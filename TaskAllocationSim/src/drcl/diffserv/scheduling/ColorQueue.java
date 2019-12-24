// @(#)ColorQueue.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.diffserv.scheduling;

import drcl.util.random.*;
import drcl.util.queue.*;
import drcl.comp.*;
import drcl.net.Packet;
import drcl.net.PktClassifier;
import drcl.inet.InetPacket;
import drcl.diffserv.DFUtil;

/* 
  To do:
	1. extend HQS                               done!
	2. implement two abstract methods in HQS    done!
	3. replace System.out.print with error()    done!
	4. add edv, edp toString                    done!
	5. replace drop()                           
	6. monitor qlen and drop rate
	7. comments
	8. config
	9. other ports?
   10. sth is not correct while reseting the queue with qib==true

/*
 * Known problems and bug fixes:
 *    1. if use HQS, need to implement some abstract methods.
 *    2. replaced q_.retrieve(int) with q_.retrieveAt(int) 
 *    3. changed ch.getSize() to ch.getPacketSize() because getSize() is
 *       replaced by getPacketSize() now.
 *    4. fixed the error while calingrun_estimate() when yellow packet arrive
 *    5. fixed the bug while calling run_estimator() 
 *    6. need to reset() after the ColorQueue is created or param is changed in tcl file.
 *
 */

/** 
 * 
 * Three Color Queue -- an extension of RED
 * 
 * @author Ye Ge
 * @version 1.0 10/26/00 last revised by Rong Zheng
 * 
 * <p> Properties:
 * <ul>
 * <li> <code>MeanPacketsize</code>: mean packet size
 * <li> <code>Wait</code>: wait flag
 * <li> <code>CongestBit</code>: ECN enabled?
 * <li> <code>MaxThresh_RED</code>: Maximum threshhold for RED
 * <li> <code>MaxThresh_YELLOW</code>: Maximum threshold for YELLOW
 * <li> <code>MaxThresh_GREEN</code>: Maximum threshold for GREEN
 * <li> <code>MinThresh_RED</code>: Minimum threshhold for RED
 * <li> <code>MinThresh_YELLOW</code>: Minimum threshold for YELLOW
 * <li> <code>MinThresh_GREEN</code>: Minimum threshold for GREEN
 * <li> <code>DropPreference</code>: drop tail, front or random?
 * <li> <code>RedQMaxSize</code>: capcity of the virutal Red Queue
 * <li> <code>YellowQMaxSize</code>: capcity of the virutal Yellow Queue
 * <li> <code>GreenQMaxSize</code>: capcity of the virutal Green Queue
 * <li> <code>Max_P_Red_Inv</code>: max_p for RED
 * <li> <code>Max_P_Yellow_Inv</code>: max_p for YELLOW
 * <li> <code>Max_P_Green_Inv</code>: max_p for GREEN
 * <li> <code>Bandwidth</code>: bandwidth of the link
 * <li> <code>Q_w</code>: weight for estimating average queue size 
 * </ul>
 */
public class ColorQueue extends drcl.inet.core.Queue implements drcl.net.PktClassifier, drcl.diffserv.DFConstants
{ 
	public static final String DROP_TAIL = "DROP_TAIL";
	public static final String DROP_FRONT = "DROP_FRONT";
	public static final String DROP_RAND = "DROP_RAND";
	static final String[] DROP_PREFERENCES = {DROP_TAIL, DROP_FRONT, DROP_RAND};
	static final int _DROP_TAIL = 0, _DROP_FRONT = 1, _DROP_RAND = 2;

	static final int TP_RED = 0;
	static final int TP_YELLOW = 1;
	static final int TP_GREEN = 2;
	static boolean   NO_IDLE = false;

	static final int DTYPE_NONE     = 2;  	/* ok, no drop */
	static final int DTYPE_FORCED   = 0;	/* a "forced" drop */
	static final int DTYPE_UNFORCED = 1;	/* an "unforced" (random) drop */
	static final String[] DROP_TYPES = {"forced drop", "random drop"};
	
	public boolean COLOR_EWMA = true;
	public boolean COLOR_HOLT_WINTERS = false;

	public UniformDistribution ug = new UniformDistribution(); /* [0.0, 1.0] */
	
	/** early-drop params */
	public edp edp_ = new edp();
	/** early-drop variables */
	public edv edv_ = new edv();
	
   	//boolean fifo_;		/* fifo queue? */
   	
   	/** underlying (usually) FIFO queue */
    protected FiniteVSQueue q_ = new FiniteVSFIFOQueue();
	/** virtual RED profile FIFO queue */
	protected FiniteVSQueue red_q_ = new FiniteVSFIFOQueue();
	/** virtual YELLOW profile FIFO queue */
	protected FiniteVSQueue yellow_q_ = new FiniteVSFIFOQueue();
	/** virtual GREEN profile FIFO queue */
	protected FiniteVSQueue green_q_ = new FiniteVSFIFOQueue();

	/** drop preference */
	protected int drop_pref  = _DROP_TAIL;

	/* Dynamic state */
	/** queue is idle? */
	boolean idle_;		 
	boolean yellow_idle_;
	boolean green_idle_;
	
	/** if idle, being idle since this time */
	double idletime_;	
	double yellow_idletime_;
	double green_idletime_;

	protected PktClassifier classifier = this;

	public ColorQueue()
	{ super(); }
	
	public ColorQueue(String id)
	{ super(id); }

	public Object firstElement()
	{ return q_ == null? null: q_.firstElement(); }
	
	/** Resets the queue. */
	public void reset() { 
		
		if (q_ != null) q_.reset();
		if (red_q_ != null) red_q_.reset();
		if (yellow_q_ != null) yellow_q_.reset();
		if (green_q_ != null) green_q_.reset();
							  
		/*
		 * If queue is measured in bytes, scale min/max thresh
		 * by the size of an average packet (which is specified by user).
		 */
		if (isByteMode() && edp_.minth_green < 50 /* FIXME: ugly*/) {
			edp_.minth_red *= edp_.mean_pktsize;
			edp_.minth_yellow *= edp_.mean_pktsize;
			edp_.minth_green *= edp_.mean_pktsize;
			edp_.maxth_red *= edp_.mean_pktsize;
			edp_.maxth_yellow *= edp_.mean_pktsize;
			edp_.maxth_green *= edp_.mean_pktsize;
		}

		/*
		 * Compute the "packet time constant" if we know the
		 * link bandwidth.  The ptc is the max number of (avg sized)
		 * pkts per second which can be placed on the link.
		 * The link bw is given in bits/sec, so scale mean psize
		 * accordingly.
		 */
		
		edp_.ptc = bw_ /(edp_.mean_pktsize << 3);

		/* The parameters for RED, YELLOW and GREEN profile are the same as the total queue. */
		edv_.reset();
		
		edv_.v_red_a    = 1 / (edp_.maxth_red    - edp_.minth_red) / edp_.max_p_red_inv;
		edv_.v_yellow_a = 1 / (edp_.maxth_yellow - edp_.minth_yellow) / edp_.max_p_red_inv;
		edv_.v_green_a  = 1 / (edp_.maxth_green  - edp_.minth_green) / edp_.max_p_red_inv;
		edv_.v_red_b    = - edp_.minth_red    * edv_.v_red_a; 
		edv_.v_yellow_b = - edp_.minth_yellow * edv_.v_yellow_a; 
		edv_.v_green_b  = - edp_.minth_green  * edv_.v_green_a; 

		idle_ = true;
		yellow_idle_ = true;
		green_idle_ = true;
		
		idletime_ = 0; // presume simulation starts at 0.0
		
		super.reset();
	}	
	
	/**
	 * Computes the average queue size.
	 * The code contains two alternate methods for this, 
	 * the plain EWMA and the Holt-Winters methods.
	 * nqueued can be bytes or packets
	 */
	private void run_estimator(long nqueued, int m, int type)
	{
		double f, f_sl, f_old, queue_w;

		if (type == TP_RED ) {
			f = edv_.v_red_ave;
			f_sl = edv_.v_red_slope;
		}
		else if (type == TP_YELLOW) {
			f = edv_.v_yellow_ave;
			f_sl = edv_.v_yellow_slope;
		}
		else {
			f = edv_.v_green_ave;
			f_sl = edv_.v_green_slope;	
		}
	  	
	  	queue_w = edp_.q_w;
	  	
		if ( COLOR_EWMA ) {
			while (m-- >= 1)
				f *= 1.0 - queue_w;
			f += queue_w * nqueued;
		} 
		if ( COLOR_HOLT_WINTERS ) {
			while (m-- >= 1) {
				f_old = f;
				f += f_sl;
				f *= 1.0 - queue_w;
				f_sl *= 1.0 - 0.5 * queue_w;
				f_sl += 0.5 * queue_w * (f - f_old);
			}
			f += queue_w * nqueued;
		}
		if ( type == TP_RED ) {
			edv_.v_red_ave = f;
			edv_.v_red_slope = f_sl;
		}
		else if (type == TP_YELLOW) { 
			edv_.v_yellow_ave = f;
			edv_.v_yellow_slope = f_sl;
		}
		else {
			edv_.v_green_ave = f;
			edv_.v_green_slope = f_sl;
		}
	}

	/**
	 * Retrieve the next packet in the queue for transmission.
	 * @return the retrieved packet.
	 */
	public synchronized Object dequeue()
	{
		Packet p = (Packet)q_.dequeue();
		
		int tp;
		if (p != null) { 
			tp = classifier.classify(p);
			idle_ = false;

			if ( tp == YELLOW ) {  
				yellow_idle_ = false;
				yellow_q_.dequeue();   //??????
			}
			else {
				if ( tp==GREEN ) { 
					green_idle_ = false;
					yellow_idle_ = false;
					green_q_.dequeue();    
				}
				else {
					red_q_.dequeue();
				}
			}	
		} 
		else {
			idle_ = true;
			yellow_idle_ = true;
			green_idle_ = true;
			idletime_ = this.getTime();
			yellow_idletime_ = idletime_;
			green_idletime_ = idletime_;
		}
	    return p;
	}
		
		
   /**
	* should the packet be dropped/marked due to a probabilistic drop?
	*/
	boolean drop_early(InetPacket pkt, int type)
	{
		double p, countp, u;
		
		if ( type == ColorQueue.TP_RED ) {
			p = edv_.v_red_a * edv_.v_red_ave + edv_.v_red_b;

			edv_.v_red_prob1 = p;
			if (edv_.v_red_prob1 > 1.0)  edv_.v_red_prob1 = 1.0;
			if (isByteMode())
				countp = p * (edv_.count_red_bytes/edp_.mean_pktsize);
			else
				countp = p * edv_.count_red;
		}
		else {
			if (type == ColorQueue.TP_YELLOW ){
				p = edv_.v_yellow_a * edv_.v_yellow_ave + edv_.v_yellow_b;
				
				edv_.v_yellow_prob1 = p;
				if (edv_.v_yellow_prob1 > 1.0)	edv_.v_yellow_prob1 = 1.0;
				if (isByteMode())
					countp = p * (edv_.count_yellow_bytes/edp_.mean_pktsize);
				else
					countp = p * edv_.count_yellow;
			}
			else {
				p = edv_.v_green_a * edv_.v_green_ave + edv_.v_green_b;
				
				edv_.v_green_prob1 = p;
				if (edv_.v_green_prob1 > 1.0)	edv_.v_green_prob1 = 1.0;
				if (isByteMode())
					countp = p * (edv_.count_green_bytes/edp_.mean_pktsize);
				else
					countp = p * edv_.count_green;
			}
		}	
		
		if (edp_.wait) {    //??????
			if (countp < 1.0) p = 0.0;
			else if ( countp < 2.0 ) p /= (2-countp);
			else p = 1.0;
		}	
        else {
			if (countp < 1.0) p /= 1.0 - countp;
			else p = 1.0;
		}
		
		if (isByteMode() && p < 1.0)
			p = p * pkt.size / edp_.mean_pktsize;
		
		if (p > 1.0) p = 1.0;
		
		u = ug.nextDouble();
		
		switch ( type ) {
			case ColorQueue.TP_RED:  {
				edv_.v_red_prob = p; 
				if ( u < p ) {
					edv_.count_red = 0;
					edv_.count_red_bytes = 0;
					try{
						if (edp_.setbit && pkt.isECT()) {
							pkt.setCE(true);
							// mark Congestion Experienced bit
							return (false);	// no drop
						} else {
							return (true);	// drop
						}
					}
					catch(Exception e_){ // maybe the field doesn't exist?
						error(pkt, "at drop_early()", null, e_);
						return (false);
					}
				}
				break;
			}	
			case ColorQueue.TP_YELLOW:  {
				edv_.v_yellow_prob = p; 
				if ( u < p ) {
					edv_.count_yellow = 0;
					edv_.count_yellow_bytes = 0;
					edv_.count_red = 0;
					edv_.count_red_bytes = 0;
					try{
						if (edp_.setbit && pkt.isECT()) {
							pkt.setCE(true);
							// mark Congestion Experienced bit
							return (false);	// no drop
						} else {
							return (true);	// drop
						}
					}
					catch(Exception e_){ // maybe the field doesn't exist?
						error(pkt, "at drop_early()", null, e_);
						return (false);
					}
				}	
				break;
			}	
			case ColorQueue.TP_GREEN:  {
				edv_.v_green_prob = p; 
				if ( u < p )  {
					edv_.count_green = 0;
					edv_.count_green_bytes = 0;
					edv_.count_yellow = 0;
					edv_.count_yellow_bytes = 0;
					edv_.count_red = 0;
					edv_.count_red_bytes = 0;
					try{
						if (edp_.setbit && pkt.isECT()) {
							pkt.setCE(true);
							// mark Congestion Experienced bit
							return (false);	// no drop
						} else {
							return (true);	// drop
						}
					}
					catch(Exception e_){ // maybe the field doesn't exist?
						error(pkt, "at drop_early()", null, e_);
						return (false);
					}
				}	
				break;
			}	
		}
		return false;			// no DROP/mark	
	}
		
    /**
	 * Picks packet for early congestion notification (ECN). 
	 * This packet is then marked or dropped. Having a separate 
	 * function do this is convenient for supporting derived 
	 * classes that use the standard COLOR algorithm to compute
	 * average queue size but use a different algorithm for 
	 * choosing the packet for ECN notification.
	 */
	protected Packet pickPacketForECN(Packet pkt)
    { return pkt; /* pick the packet that just arrived */ }
	
		
    /**
	 * Picks packet to drop. 
	 * By default it uses drop-tail.
	 * Having a separate function do this is convenient for
	 * supporting derived classes that use the standard three 
	 * COLOR algorithm to compute average queue size but use 
	 * a different algorithm for choosing the victim.
	protected Packet pickPacketToDrop() 
	{
		if (drop_pref == _DROP_TAIL)
			return (Packet)q_.lastElement();
		else if (drop_pref == DROP_FRONT)  
			return (Packet)q_.firstElement();
		else {
			int victim = (int)(ug.nextDouble()*q_.getLength());
			if (victim == q_.getLength())  victim = victim -1;
			return((Packet)q_.retrieveAt(victim)); 
		}
	}
	 */


	/*
	 * Receive a new packet arriving at the queue.
	 * The average queue size is computed.  If the average size
	 * exceeds the threshold, then the dropping probability is computed,
	 * and the newly-arriving packet is dropped with that probability.
	 * The packet is also dropped if the maximum queue size is exceeded.
	 *
	 * "Forced" drops mean a packet arrived when the underlying queue was
	 * full or when the average q size exceeded maxthresh.
	 * "Unforced" means a COLOR random drop.
	 *
	 * For forced drops, either the arriving packet is dropped or one in the
	 * queue is dropped, depending on the setting of drop_tail_.
	 * For unforced drops, the arriving packet is always the victim.
	 */
	// public boolean enque(Packet pkt) 
	
	/*
	 *  
	 */
	//public void enque(Packet pkt) 
	public synchronized Object enqueue(Object obj_)
	{ 
		InetPacket pkt = (InetPacket)obj_;
		
		//print_edv();
		/*
		 * if we were idle, we pretend that m packets arrived during
		 * the idle period.  m is set to be the ptc times the amount
		 * of time we've been idle for
		 */
		
        InetPacket pkt_copy;
        int m = 0;
		double drop_time;
	    long pktsize = pkt.size << 3; /* length of the packet in bits */
		pkt_copy = (InetPacket) pkt.clone();
		long qlen, qlen_red, qlen_yellow, qlen_green;

		int pktsize_ = isByteMode()? pkt.size: 1;
		qlen = q_.getSize();
		qlen_red = red_q_.getSize();
		qlen_yellow = yellow_q_.getSize();
		qlen_green = green_q_.getSize();
		
		int color = classifier.classify(pkt);

		if ( color == GREEN ) {  //????
			if (green_idle_) {
				double now = this.getTime();
				/* To account for the period when the queue was empty. */
				idle_ = false;
				yellow_idle_ = false;
				green_idle_ = false;
				m = (int) (edp_.ptc * (now - idletime_));
			} 
			else 
				m = 0;

		 // Run the estimator with either 1 new packet arrival, or with
		 // the scaled version above [scaled by m due to idle time]

			run_estimator(q_.getSize(), m + 1, TP_RED);       // for g+y+r
			run_estimator(green_q_.getSize()+yellow_q_.getSize(), m + 1, TP_YELLOW); // for g+y
			run_estimator(green_q_.getSize(), m + 1, TP_GREEN);   // for g

		 // count_green and count_green_bytes keeps a tally of arriving GREEN traffic
		 // that has not been dropped (i.e. how long, in terms of traffic,
		 // it has been since the last early drop)

			++edv_.count_red;
			++edv_.count_yellow;
			++edv_.count_green;
			edv_.count_red_bytes += pkt.size;
			edv_.count_yellow_bytes += pkt.size;
			edv_.count_green_bytes += pkt.size;

		/*
		 * DROP LOGIC:
		 *	q = current q size, ~q = averaged q size
		 *	1> if ~q > maxthresh, this is a FORCED drop
		 *	2> if minthresh < ~q < maxthresh, this may be an UNFORCED drop
		 *	3> if (q+1) > hard q limit, this is a FORCED drop
		 */

			double qavg_green_ = edv_.v_green_ave;
			
			int droptype_ = ColorQueue.DTYPE_NONE;
			int qlim;
			if ( isByteMode() ) qlim = (q_.getCapacity() * edp_.mean_pktsize); else qlim = q_.getCapacity();

		/* Check GREEN queue */
			if (qavg_green_ >= edp_.minth_green && qlen_green > 1) {
				if (qavg_green_ >= edp_.maxth_green) {
					droptype_ = ColorQueue.DTYPE_FORCED;
//					//debug("droptype_="+droptype_); 
				} 
				else { 
					if (edv_.old_green == 0) {
						edv_.count_green = 1;
						edv_.count_green_bytes = pkt.size;
						edv_.old_green = 1;
					} 
					else {
						if (drop_early(pkt, TP_GREEN)) {
							droptype_ = ColorQueue.DTYPE_UNFORCED;
//							//debug("droptype_="+droptype_); 
						}
					}	
				}
			} 
			else {
				edv_.v_green_prob = 0.0;
				edv_.old_green = 0;		// explain
//				//debug("droptype_="+droptype_); 
			}
			
			if (qlen >= qlim) {
				// see if we've exceeded the queue size
				droptype_ = ColorQueue.DTYPE_FORCED;
//				//debug("droptype_="+droptype_); 
			}

			switch ( droptype_ ) {
				case ColorQueue.DTYPE_NONE:
					/* not a drop: first enqueue pkt */
					if (!q_.isFull(pktsize_)) {
						q_.enqueue(pkt, pktsize_);
						green_q_.enqueue(pkt_copy, pktsize_);
					}
					else
						drop(pkt, "exceeds capacity");
					break;
				case ColorQueue.DTYPE_UNFORCED: {	
					/* pick packet for ECN, which is dropping in this case */
					Packet pkt_to_drop = pickPacketForECN(pkt);
					if ( pkt_to_drop != pkt ) {
						q_.enqueue(pkt, pktsize_);
						q_.remove(pkt_to_drop);
					}
					if ( isDebugEnabled() ) {
						debug(pkt_to_drop+" UNFORCED DROP GREEN! G_ave="+ edv_.v_green_ave+" G_qlen="+qlen_green);
					}	
					drop(pkt_to_drop, " UNFORCED DROP GREEN! G_ave="+ edv_.v_green_ave+" G_qlen="+qlen_green);
					drop_time = getTime();
					break;
				}	
				case DTYPE_FORCED: {
					/* drop a packet if we were told to */
					//if ( isDebugEnabled() ) {
						//debug(pkt+" FORCED DROP GREEN! G_ave="+ edv_.v_green_ave+" G_qlen="+qlen_green+" qlen="+qlen); 
					//}
					drop(pkt, " FORCED DROP GREEN! G_ave="+ edv_.v_green_ave+" G_qlen="+qlen_green+" qlen="+qlen);   
					drop_time = this.getTime();
					break;
				}	
			}
			return null;
		}else if ( color == YELLOW ) {  
			if ( yellow_idle_ ) {
				double now = this.getTime();
				/* To account for the period when the queue was empty. */
				idle_ = false;
				yellow_idle_ = false;
				m = (int)(edp_.ptc * (now - idletime_));
			}
			else 
				m = 0;

		/* The same enque function for IN profile packet.
		 * Change the corresponding variables to the correct value.
		 */
			run_estimator(yellow_q_.getSize()+green_q_.getSize(), m+1, ColorQueue.TP_YELLOW);
			run_estimator(q_.getSize(), m + 1, TP_RED);       // for g+y+r
			
			++edv_.count_red;
			++edv_.count_yellow;
			edv_.count_red_bytes += pkt.size;
			edv_.count_yellow_bytes += pkt.size;

			double qavg_yellow = edv_.v_yellow_ave;
			
			int droptype_ = ColorQueue.DTYPE_NONE;

			if (qavg_yellow >= edp_.minth_yellow && qlen_yellow > 1) {
				if (qavg_yellow >= edp_.maxth_yellow) {
					droptype_ = ColorQueue.DTYPE_FORCED;
				}
				else {
					if (edv_.old == 0) {
						edv_.count = 1;
						edv_.count_bytes = pkt.size;
						edv_.old = 1;
					} 
					else {
						if (drop_early(pkt, ColorQueue.TP_YELLOW)) {
							droptype_ = ColorQueue.DTYPE_UNFORCED;
						}	
					}	
				}
			} 
			else {
				edv_.v_yellow_prob = 0.0;
				edv_.old = 0;		// explain
			}
			if (qlen >= q_.getCapacity()) {
				// see if we've exceeded the queue size
				droptype_ = ColorQueue.DTYPE_FORCED;
			}
			
			switch ( droptype_ ) {
				case ColorQueue.DTYPE_NONE:
					/* not a drop: first enqueue pkt */
					if (!q_.isFull(pktsize_) ) {
						q_.enqueue(pkt, pktsize_);
						yellow_q_.enqueue(pkt_copy, pktsize_);
					}
					else
						drop(pkt, "exceeds capacity");
					break;
				case ColorQueue.DTYPE_UNFORCED: {	
					/* pick packet for ECN, which is dropping in this case */
					Packet pkt_to_drop = pickPacketForECN(pkt);
					if (pkt_to_drop != pkt) {
						q_.enqueue(pkt, pktsize_);
						q_.remove(pkt_to_drop);
					}
	    			if ( isDebugEnabled() ) {
		    			debug(pkt_to_drop+" UNFORCED DROP YELLOW! Y_ave="+ edv_.v_yellow_ave
								+" Y_qlen="+(qlen_yellow+qlen_green));
			     	}
					drop(pkt_to_drop, " UNFORCED DROP YELLOW! Y_ave="+ edv_.v_yellow_ave
							+" Y_qlen="+(qlen_yellow+qlen_green));
					drop_time = getTime();
					break;
				}	
				case ColorQueue.DTYPE_FORCED:
					/* drop a packet if we were told to */
	    			if ( isDebugEnabled() ) {
		    			debug(pkt+" FORCED DROP YELLOW! Y_ave="+ edv_.v_yellow_ave
								+" Y_qlen="+(qlen_yellow+qlen_green)+" qlen="+qlen);
			     	}
					drop(pkt, " FORCED DROP YELLOW! Y_ave="+ edv_.v_yellow_ave
							+" Y_qlen="+(qlen_yellow+qlen_green)+" qlen="+qlen);
					drop_time = getTime();
					break;
			}					   
			return null;
		} else { // RED packet
			 if (idle_) {
			 	double now = getTime();
			 	/* To account for the period when the queue was empty. */
			 	idle_ = false;
			 	m = (int)(edp_.ptc * (now - idletime_));
			 }
			 else 
			 	m = 0;

			/* The same enqueue function for IN profile packet.
			 * Change the corresponding variables to the correct value.
			 */
			 run_estimator(q_.getSize(), m+1, ColorQueue.TP_RED);
	
			 ++edv_.count_red;
			 edv_.count_red_bytes += pkt.size;
			 double qavg_ = edv_.v_red_ave;
			 int droptype_ = ColorQueue.DTYPE_NONE;
					
			 if (qavg_ >= edp_.minth_red && qlen_red > 1) {
			 	if (qavg_ >= edp_.maxth_red) {
			 		droptype_ = ColorQueue.DTYPE_FORCED;
			 	} 
			 	else  {
			 		if (edv_.old == 0) {
			 			edv_.count = 1;
			 			edv_.count_bytes = pkt.size;
			 			edv_.old = 1;
			 		} 
			 		else  {
			 			if (drop_early(pkt, TP_RED)) {
			 				droptype_ = ColorQueue.DTYPE_UNFORCED;
			 			}
			 		}	
			 	} 
			 }	
			 else  {
			 	edv_.v_red_prob = 0.0;
			 	edv_.old = 0;		// explain
			 }
					
			 if (qlen >= q_.getCapacity()) {
			 	// see if we've exceeded the queue size
			 	droptype_ = ColorQueue.DTYPE_FORCED;
			 }
			
			//debug("At "+getTime()+" got a RED packet, droptype_ = "+droptype_+ ", qsize = "+q_.getSize());
			//print_edv();
			switch (droptype_) {
			 	case ColorQueue.DTYPE_NONE:
			 		/* not a drop: first enqueue pkt */
			 		if (!q_.isFull(pktsize_) ) {
			 			q_.enqueue(pkt, pktsize_);
			 			red_q_.enqueue(pkt_copy, pktsize_);
					}
					else
						drop(pkt, "exceeds capacity");
					break;
			 	case ColorQueue.DTYPE_UNFORCED: {	
			 		/* pick packet for ECN, which is dropping in this case */
			 		Packet pkt_to_drop = pickPacketForECN(pkt);
			 		if (pkt_to_drop != pkt) {
			 			q_.enqueue(pkt, pktsize_);
			 			q_.remove(pkt_to_drop);
			 		}
			 	    drop(pkt_to_drop, " UNFORCED DROP RED! R_ave="+ edv_.v_red_ave+" qlen="+qlen);
			 		drop_time = getTime();
					break;
			 	}	
			 	case ColorQueue.DTYPE_FORCED:
			 		/* drop a packet if we were told to */
	         	    if (isDebugEnabled())
			 			debug(pkt+" FORCED DROP RED! R_ave="+ edv_.v_red_ave+" qlen="+qlen);
			 	    drop(pkt, " FORCED DROP RED! R_ave="+ edv_.v_red_ave+" qlen="+qlen);
			 		drop_time = getTime();
					break;
			 }
			 return null;
		}	
	}


	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		ColorQueue that_ = (ColorQueue)source_;
		edp_.duplicate(that_.edp_);

		q_ = QueueAssistant.getBestFiniteVS();			    // underlying queue
		red_q_ = QueueAssistant.getBestFiniteVS();
	    yellow_q_ = QueueAssistant.getBestFiniteVS();
	    green_q_ = QueueAssistant.getBestFiniteVS();
		edv_ = new edv();
		ug = new UniformDistribution(); /* [0.0, 1.0] */
		reset();
	}

	public void setSeed(long seed_)
	{ ug.setSeed(seed_); }
	
	public long getSeed()
	{ return ug.getSeed(); }
	
   /**
	 * Interface for user to supply parameters
	 */
	public void setMeanPacketsize(int size){edp_.mean_pktsize=size;}
	public void setWait(boolean wait_){edp_.wait = wait_;}   //wait 
	public void setCongestBit(boolean cb_){edp_.setbit = cb_;} //congestion bit?

	public void setMaxThresh_RED(double max){edp_.maxth_red = max;}
	public void setMaxThresh_YELLOW(double max){edp_.maxth_yellow = max;}
	public void setMaxThresh_GREEN(double max){edp_.maxth_green = max;}
	
	public void setMinThresh_RED(double max){edp_.minth_red = max;}
	public void setMinThresh_YELLOW(double max){edp_.minth_yellow = max;}
	public void setMinThresh_GREEN(double max){edp_.minth_green = max;}
	
	public void setDropPreference(String pref_)
	{
		if (pref_.equals(DROP_TAIL)) drop_pref = _DROP_TAIL;
		else if (pref_.equals(DROP_FRONT)) drop_pref = _DROP_FRONT;
		else if (pref_.equals(DROP_RAND)) drop_pref = _DROP_RAND;
		else drcl.Debug.error(pref_ + " is not supported.");
	}
	
	public int getDropPreference()
	{ return drop_pref; }
	
	public void setCapacity(int size){q_.setCapacity(size);}
	public void setRedQMaxSize(int size){red_q_.setCapacity(size);}
	public void setYellowQMaxSize(int size){yellow_q_.setCapacity(size);}
	public void setGreenQMaxSize(int size){green_q_.setCapacity(size);}
	
	public int  getSize(){return q_.getSize();}
	public int  getGreenQSize(){return green_q_.getSize();}
	public int  getYellowQSize(){return yellow_q_.getSize();}
	public int  getRedQSize(){return red_q_.getSize();}
	
	public int getCapacity(){return q_.getCapacity();}
	public int getMaxGreenQSize(){return green_q_.getCapacity();}
	public int getMaxYellowQSize(){return yellow_q_.getCapacity();}
	public int getMaxRedQSize(){return red_q_.getCapacity();}

	public void setMax_P_Red_Inv(double p_) { edp_.max_p_red_inv = p_; }
	public void setMax_P_Yellow_Inv(double p_) { edp_.max_p_yellow_inv = p_; }
	public void setMax_P_Green_Inv(double p_) { edp_.max_p_green_inv = p_; }

	public void config(double minth_g, double maxth_g, double imaxp_g,
					   double minth_y, double maxth_y, double imaxp_y,
					   double minth_r, double maxth_r, double imaxp_r,
					   double qw_, double bw, int meanpsize_)
	{
		edp_.minth_green  = minth_g; edp_.maxth_green  = maxth_g; edp_.max_p_green_inv  = imaxp_g;
		edp_.minth_yellow = minth_y; edp_.maxth_yellow = maxth_y; edp_.max_p_yellow_inv = imaxp_y;
		edp_.minth_red    = minth_r; edp_.maxth_red    = maxth_r; edp_.max_p_red_inv    = imaxp_r;
		edp_.q_w = qw_;
		bw_ = bw;
		edp_.mean_pktsize = meanpsize_;
		setPtc();
	}
	
	double bw_ = 1000.0; //????
	public double getBandwidth()
	{ return bw_; }

	public void setBandwidth(double v_)
	{ 
		bw_ = v_; 
		edp_.ptc = edp_.ptc = bw_ /(8. * edp_.mean_pktsize);
	}

	public void setPtc()
	{ edp_.ptc = bw_ /(8. * edp_.mean_pktsize); }
	
	public void setQ_w(double q_w_)
	{ edp_.q_w = q_w_; }
	
	public void setClassifier(PktClassifier c_)
	{ classifier = c_; }

	public PktClassifier getClassifier()
	{ return classifier; }

	public int classify(Packet pkt_)
	{ return DFUtil.getDSCP((InetPacket)pkt_)-AF11; }

	public void setDefault()
	{
		setDropPreference(DROP_TAIL);
		setMeanPacketsize(500);
	    setWait(false); 
	    setCongestBit(false); 

		setMaxThresh_RED(30);
		setMinThresh_RED(10);
		setMax_P_Red_Inv(5);

		setMaxThresh_YELLOW(20);
		setMinThresh_YELLOW(7);
		setMax_P_Yellow_Inv(10);

		setMaxThresh_GREEN(15);
		setMinThresh_GREEN(5);
		setMax_P_Green_Inv(20);
		
		setBandwidth(1000.0);
		
		setPtc();
		setQ_w(0.02);
	}	
	
	public void print_edp()
	{ System.out.println(edp_.toString()); }

	public void print_edv()
	{ System.out.println(edv_.toString()); }

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+ prefix_ + "EDP:\n" + edp_.info(prefix_ + "   ")
			+ prefix_ + "EDV:\n" + edv_.info(prefix_ + "   ")
			+ prefix_ + "Q's:\n"
			+ prefix_ + "    green: " + green_q_.getSize() + "\n"
			+ (green_q_.isEmpty()? "": green_q_.info(prefix_ + "           "))
			+ prefix_ + "   yellow: " + yellow_q_.getSize() + "\n"
			+ (yellow_q_.isEmpty()? "": yellow_q_.info(prefix_ + "           "))
			+ prefix_ + "      red: " + red_q_.getSize() + "\n"
			+ (red_q_.isEmpty()? "": red_q_.info(prefix_ + "           "));
	}

	public boolean isEmpty()
	{ return q_ == null? true: q_.isEmpty(); }

	public boolean isFull()
	{ return q_ == null? false: q_.isFull(); }

	/* Early drop parameters, supplied by user */
	class edp
	{
		/* User supplied. */
		int mean_pktsize = 500;	/* avg pkt size */
		boolean wait = true;		/* true for waiting between dropped packets */
		boolean setbit = false;		/* true to set congestion indication bit */

		double minth_red;	/* minimum threshold for RED packets. */
		double minth_yellow;	/* minimum threshold for YELLOW packets. */
		double minth_green;	/* minimum threshold for GREEN packets. */

		double maxth_red;	/* maximum threshold for RED packets. */
		double maxth_yellow;	/* maximum threshold for YELLOW packets. */
		double maxth_green;	/* maximum threshold for GREEN packets. */
		
		double max_p_red_inv;	/* 1/max_p_red, for max_p_red = maximum RED prob.  */
		double max_p_yellow_inv;	/* 1/max_p_yellow, for max_p_yellow = maximum YELLOW prob. */
		double max_p_green_inv; /* 1/max_p_green, for max_p_green = maximum GREEN prob.  */
		
		double q_w;		/* queue weight given to cur q size sample */

		/* packet time constant in packets/second */
		double ptc;		// Computed as a function of user supplied paramters.
		
		void duplicate(edp source_) { 
			mean_pktsize = source_.mean_pktsize; 
			wait = source_.wait;
			setbit = source_.setbit;
			minth_red = source_.minth_red;
			minth_yellow = source_.minth_yellow;
			minth_green = source_.minth_green;
			maxth_red = source_.maxth_red;	
			maxth_yellow = source_.maxth_yellow;	
			maxth_green = source_.maxth_green;	
			max_p_red_inv = source_.max_p_red_inv;	
			max_p_yellow_inv = source_.max_p_yellow_inv;	
			max_p_green_inv = source_.max_p_green_inv; 
			q_w = source_.q_w;	
		}
		
		edp(){}

		public String info(String prefix_)
		{
		    return prefix_ + " green: minth=" + minth_green + ", maxth=" + maxth_green + ", 1/maxp=" + max_p_green_inv + "\n"
		    	 + prefix_ + "yellow: minth=" + minth_yellow + ", maxth=" + maxth_yellow + ", 1/maxp=" + max_p_yellow_inv + "\n"
		    	 + prefix_ + "   red: minth=" + minth_red + ", maxth=" + maxth_red + ", 1/maxp=" + max_p_red_inv + "\n"
				 + prefix_ + "1/q_w=" + (1.0/q_w) + ", ptc=" + ptc + "\n";
		}

		public String toString()
		{ 
			return "mean packet size = " + mean_pktsize + 
			   ", minth_red = " + minth_red +
			   ", minth_yellow = " + minth_yellow + 
			   ", minth_green = " + minth_green +
			   ", maxth_red = " + maxth_red + 
			   ", maxth_yellow = " + maxth_yellow + 
			   ", maxth_green = " + maxth_green + 
			   ", max_p_red_inv = " + max_p_red_inv +
			   ", max_p_yellow_inv = " + max_p_yellow_inv + 
			   ", max_p_green_inv = " + max_p_green_inv +
			   ", q_w = " + q_w +
			   ", ptc = " + ptc;
		}
	}

	/* Early drop variables, maintained by COLOR */
	class edv
	{
		double v_red_ave = 0.0;		/* average RED queue size */
		double v_yellow_ave = 0.0;	/* average YELLOW queue size */
		double v_green_ave = 0.0; 	/* average GREEN queue size */
		double v_red_prob1 = 0.0;	/* prob. of RED packet drop before "count". */
		double v_yellow_prob1 = 0.0;	/* prob. of YELLOW packet drop before "count". */
		double v_green_prob1 = 0.0;     /* prob. of GREEN packet drop before "count". */
		double v_red_slope = 0.0;		/* used in computing average RED queue size */
		double v_yellow_slope = 0.0;	/* used in computing average YELLOW queue size */
		double v_green_slope = 0.0;     /* used in computing average GREEN queue size */
		double v_red_prob = 0.0;		/* prob. of RED packet drop */
		double v_yellow_prob = 0.0;		/* prob. of YELLOW packet drop */
		double v_green_prob = 0.0;      /* prob. of GREEN packet drop */
		double v_red_a = 0.0;			/* ??v_red_prob = v_out_a * v_ave + v_out_b */
		double v_yellow_a = 0.0;		/* ??v_yellow_prob = v_in_a * v_in_ave + v_in_b */
		double v_green_a = 0.0;         /* ??v_green_prob = v_out_a * v_ave + v_out_b */
		double v_red_b = 0.0;
		double v_yellow_b = 0.0;
		double v_green_b = 0.0;
		long   count;
		long   count_red = 0;			/* # of RED packets since last drop */
		long   count_yellow = 0;		/* # of YELLOW packets since last drop */
		long   count_green = 0;                /* # of GREEN packets since last drop */
		long   count_bytes;
		long   count_red_bytes = 0;		/* # of RED bytes since last drop ? */
		long   count_yellow_bytes = 0;		/* # of YELLOW bytes since last drop */
		long   count_green_bytes = 0;    	/* # of GREEN bytes since last drop ? */
		long   old = 0;
		long   old_red = 0;			/* 0 when average total queue first exceeds thresh */
		long   old_yellow = 0;            	/* 0 when average YELLOW and GREEN queue first exceeds thresh */
		long   old_green = 0;			/* 0 when average GREEN queue first exceeds thresh */

		public edv(){}

		protected void reset()
		{
			v_red_ave = 0.0;    v_yellow_ave = 0.0;     v_green_ave = 0.0; 	
			v_red_prob1 = 0.0;	v_yellow_prob1 = 0.0;   v_green_prob1 = 0.0;    
			v_red_slope = 0.0;	v_yellow_slope = 0.0;   v_green_slope = 0.0;    
			v_red_prob = 0.0;	v_yellow_prob = 0.0;	v_green_prob = 0.0;     
			v_red_a = 0.0;		v_yellow_a = 0.0;		v_green_a = 0.0;        
			v_red_b = 0.0;      v_yellow_b = 0.0;		v_green_b = 0.0;
			count = 0;			
			count_red = 0;		count_yellow = 0;		count_green = 0;    count_bytes = 0;		
			count_red_bytes = 0;count_yellow_bytes = 0;	count_green_bytes = 0;  
			old = 0;
			old_red = 0;		old_yellow = 0;         old_green = 0;			
		}
		
		public String info(String prefix_)
		{
		    return prefix_ + " green: qavg=" + v_green_ave + ", prob=" + v_green_prob + ", cnt=" + count_green + "\n"
		    	 + prefix_ + "yellow: qavg=" + v_yellow_ave + ", prob=" + v_yellow_prob + ", cnt=" + count_yellow + "\n"
		    	 + prefix_ + "   red: qavg=" + v_red_ave + ", prob=" + v_red_prob + ", cnt=" + count_red + "\n";
		}
	}
}
