// @(#)RED.java   9/2002
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

package drcl.inet.core.queue;

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.InetPacket;
import drcl.data.DoubleObj;
import drcl.util.random.*;

/**
 * The <i>Random Early Detection</i> (RED) queue logic.
 */
public class RED extends drcl.comp.queue.QLogic
{
	public static boolean RED_EWMA = true;
	public static boolean RED_HOLT_WINTERS = false;
	protected static final String	DTYPE_NONE	= null;	/* ok, no drop */
	protected static final String	DTYPE_FORCED =	"forced drop";	/* a "forced" drop */
	protected static final String DTYPE_UNFORCED = "random drop";	/* an "unforced" (random) drop */

	/** Name of the <i>average queue size change</i> event. */
	public static final String EVENT_AVG_QSIZE = "Avg Q Size";

	/** Port to output <i>average queue size change</i> event */
	protected Port avgQSizePort;
	
	/* fixed parameters */
	/** Mean packet size (byte), for computing {@link #ptc}. */
	protected int mean_pktsize = 500;
	/** Link bandwidth (bps), for computing {@link #ptc}. */
	protected double bw = 0.0;
	/**	Minimum threshold of average queue size. */
	protected double th_min=0.0;
	/**	Maximum threshold of average queue size. */
	protected double th_max=0.0;
	/** Maximum dropping probability. */
	protected double max_p=1.0/50.0;
	/**	Weight for computing average queue size given to current queue size sample. */
	protected double qw=0.002;
	/** The wait flag, true for waiting between dropped packets. */
	protected boolean wait = false;		
	/** The mark flag, true for marking CE bit instead of early dropping. */
	protected boolean mark = false;

	// precomputed values from the fixed parameters, for better performance
	/** Output rate of the link in packets/second. */
	protected double ptc;
	/** For computing dropping probability 
	 * (<code>=max_p*(qavg - min_th)/(max_th-min_th) = v_a * qavg + v_b</code>). */
	protected double v_a = 0.0;
	/** For computing dropping probability 
	 * (<code>=max_p*(qavg - min_th)/(max_th-min_th) = v_a * qavg + v_b</code>). */
	protected double v_b = 0.0;

	/* internal states/variables  */
	/** Computed average queue size. */
	protected double qavg = 0.0;
	/** Variable used in HOLT WINTERS filter for computing average queue size. */
	protected double v_slope = 0.0;	
	/** Computed dropping probability for the incoming packet, for diagnosis purpose. */
	protected double v_prob = 0.0;
	/** # of packets since last early drop. */
	protected long count = 0;
	/** 0 when average queue first exceeds thresh. */
	protected int old = 0;
	/** Start of the current idle period. Is <code>Double.NaN</code> if not idle. */
	protected double idletime = Double.NaN;
	/** The random number generator used in this RED. */
	protected RandomNumberGenerator ug = null;
	
	public RED()
	{ super();	}

	/**
	 * @param avgpid_ ID of the average queue size change event port that will be created at
	 * 		the host component.
	 */
	public RED(Component host_, String avgpid_)
	{ super(host_);	setAvgQSizePort(avgpid_); }

	/**
	 * @param avgpid_ ID of the average queue size change event port that will be created at
	 * 		the host component.
	 * @param qpid_ ID of the instant queue size change event port that will be created at
	 * 		the host component.
	 */
	public RED(Component host_, String avgpid_, String qpid_)
	{ super(host_, qpid_);	setAvgQSizePort(avgpid_); }

	public void reset()
	{
		super.reset();
		if (ug != null) ug.reset();
		edv_reset();
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		RED that_ = (RED)source_;
		edp_duplicate(that_);
		wait = that_.wait;
		mark = that_.mark;
		setSeed(that_.getSeed());
		if (host != null && that_.avgQSizePort != null)
			setAvgQSizePort(that_.avgQSizePort.getID());
	}

	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.info(prefix_));
		sb_.append(prefix_ + "mark instead of early drop: " + mark + "\n");
		sb_.append(prefix_ + "RNG: " + (ug != null? ug.oneline(): "<none>") + "\n");
		sb_.append(edp_print(prefix_) + edv_print(prefix_));
		return sb_.toString();
	}

	/**
	 * Computes the average queue size.
	 * The code contains two alternate methods for this, the plain EWMA
	 * and the Holt-Winters method.
	 */
	protected void run_estimator(boolean advice_)
	{
		// if we were idle, we pretend that m packets arrived during
		// the idle period.  m is set to be the ptc times the amount
		// of time we've been idle for
		int m = 0;
		if (!Double.isNaN(idletime)) {
			double now_ = host.getTime();
			m = (int)(ptc * (now_ - idletime));
			idletime = Double.NaN;
		}
		if (!advice_) m++;
		if (m == 0) return;

		if(RED_EWMA){
			while (m-- >= 1)
				qavg *= 1.0 - qw;
			// qavg = pow(1.0 - qw, (double)m);
			qavg += qw * qsize;
		}
			
			
		if(RED_HOLT_WINTERS){
			double f_sl = v_slope;
			while (m-- >= 1) {
				double f_old = qavg;
				qavg = (qavg + f_sl) * (1.0 - qw);
				f_sl *= 1.0 - 0.5 * qw;
				f_sl += 0.5 * qw * (qavg - f_old);
			}
			qavg += qw * qsize;
			v_slope = f_sl;
		}
		if (avgQSizePort != null && avgQSizePort._isEventExportEnabled())
			avgQSizePort.exportEvent(EVENT_AVG_QSIZE, new DoubleObj(qavg), null);
	}

	/*
	 * Returns true if the packet should be dropped/marked.
	 */
	protected boolean drop_early(Packet pkt_)
	{
		v_prob = v_a * qavg + v_b;
		double countp_ = count * v_prob;
		
		if (wait) {
			if (countp_ < 1.0) v_prob = 0.0;
			else if (countp_ < 2.0) v_prob /= (2 - countp_);
			else v_prob = 1.0;
		} else {
			if (countp_ < 1.0) v_prob /= (1.0 - countp_);
			else v_prob = 1.0;
		}
		
		if (v_prob > 1.0 || v_prob < 0.0) v_prob = 1.0;

		if (ug == null) ug = new UniformDistribution(0);

		// drop probability is computed, pick random number and act
		if (v_prob == 1.0 || ug.nextDouble() <= v_prob) {
			// DROP or MARK
			if (mark && ((InetPacket)pkt_).isECT()) {
				((InetPacket)pkt_).setCE(true); // mark Congestion Experienced bit
				return false;	// no drop
			}
			else
				return true;	// drop
		}
		return false;			// no DROP/mark
	}

	/**
	 * Returns advice (in String) on whether or not to drop the packet.
	 * Returns false if not to drop the packet.
	 */
	public String adviceOn(Object obj_, int psize_)
	{
		run_estimator(true);
		Packet pkt_ = (Packet)obj_;

		/*
		 * DROP LOGIC:
		 *	q = current q size, ~q = averaged q size
		 *	1> if ~q > maxthresh, this is a FORCED drop
		 *	2> if minthresh < ~q < maxthresh, this may be an UNFORCED drop
		 *	3> if (q+1) > hard q limit, this is a FORCED drop
		 */

		String droptype = DTYPE_NONE;

		if (qavg >= th_min && qlen > 1) {
			if (qavg >= th_max) {
				droptype = DTYPE_FORCED;
			} else if (old == 0) {
				// SALLY: would like a comment here
				count = 1;
				old = 1;
			} else if (drop_early(pkt_)) {
				droptype = DTYPE_UNFORCED;
			}
		} else {
			v_prob = 0.0;
			old = 0;		// explain
		}
		
		if (droptype == DTYPE_NONE) {
			if (qsize + psize_ > capacity) {
				if (host.isGarbageEnabled() && host.isDebugEnabled())
					return "exceeds capacity:" + qsize + "+" + psize_ + ">" + capacity;
				else
					return "exceeds capacity";
			}
			else
				return null;
		}
		else { // droptype == DTYPE_UNFORCED || droptype == DTYPE_FORCED
			if (host.isGarbageEnabled() && host.isDebugEnabled()) {
				if (droptype == DTYPE_FORCED)
					droptype = droptype + ": avg_q=" + qavg + ", max_th=" + th_max;
				else
					droptype = droptype + ": drop_prob=" + v_prob + ", count=" + count;
			}
			count = 0;
			return droptype;
		}
	}

	public void enqueueHandler(Object obj_, int psize_)
	{
		super.enqueueHandler(obj_, psize_);
		run_estimator(false);
		// # of packets not being dropped since last early drop.
		++count;
	}
	
	public void dequeueHandler(Object obj_, int psize_)
	{
		super.dequeueHandler(obj_, psize_);
		if (qsize == 0) // becomes idle
			idletime = host.getTime();
	}

	public void setWait(boolean enabled_){wait = enabled_;}   //wait 
	public boolean isWait() { return wait; }
	
	/** If enabled, RED marks the congestion bit of, instead of advising to drop, packets.  */
	public void setMarkEnabled(boolean enabled_){mark = enabled_;}
	public boolean isMarkEnabled() { return mark; }
	
	public void setMeanPacketSize(int size_)
	{
		mean_pktsize = size_==0? 500: size_;
		ptc = bw / (mean_pktsize << 3);
	}
	public int getMeanPacketSize(){return mean_pktsize;}
	
	public void setMaxThresh(double max)
	{ th_max = max==0? capacity*2/3: max; _updatePrecomputed(); }
	public double getMaxThresh() { return th_max; }
	
	public void setMinThresh(double min)
	{ th_min = min==0? capacity/3: min; _updatePrecomputed(); }
	public double getMinThresh() { return th_min; }
	
	public void setInvProb(double value)
	{ max_p = value==0? 1.0/30.0: 1.0/value; _updatePrecomputed(); }
	public double getInvProb() { return 1.0/max_p; }
	
	public void setWeight(double weight)
	{ qw = weight==0? .002: weight; }
	public double getWeight() { return qw; }
	
	public double getBandwidth() { return bw; }
	public void setBandwidth(double bw_)
	{ bw = bw_; ptc = bw / (mean_pktsize << 3); }

	/**
	 * Sets up all RED parameters.
	 * For each parameter, the default value is used if zero is given.
	 */
	public void setREDParam(int psize_, double bw_, double th_max_,
				double th_min_, double inv_max_p_, double qw_)
	{
		mean_pktsize = psize_==0? 500: psize_;
		bw = bw_;
		th_max = th_max_==0.0? capacity/2: th_max_;
		th_min = th_min_==0.0? capacity/4: th_min_;
		max_p = inv_max_p_==0? 1.0/50.0: 1.0/inv_max_p_;
		qw = qw_==0? .002: qw_;

		_updatePrecomputed();
		ptc = bw / (mean_pktsize << 3);
	}

	public void setSeed(long seed_)
	{
		if (ug == null) ug = new UniformDistribution(seed_);
		else ug.setSeed(seed_);
	}

	public long getSeed()
	{ return ug == null? 0L: ug.getSeed(); }

	public void setRandomNumberGenerator(RandomNumberGenerator ug_)
	{
		if (ug != null && ug_ != null) ug_.setSeed(ug.getSeed());
		ug = ug_;
	}

	public void setRandomNumberGenerator(RandomNumberGenerator ug_, long seed_)
	{ ug = ug_;  ug.setSeed(seed_); }

	/**
	 * @param pid_ ID of the port.
	 */
	public void setAvgQSizePort(String avgqspid_)
	{ avgQSizePort = host.addEventPort(avgqspid_); }
	
	// reset states/variables
	void edv_reset()
	{
		qavg = v_slope = v_prob = 0.0;
		count = 0;
		old = 0;
		idletime = Double.NaN;
	}
	
	// duplicate fixed parameters
	void edp_duplicate(RED source_)
	{
		mean_pktsize = source_.mean_pktsize;
		th_min = source_.th_min;
		th_max = source_.th_max;
		max_p = source_.max_p;
		qw = source_.qw;
		bw = source_.bw;
	}

	// print out fixed parameters
	String edp_print(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(prefix_ + "RED Parameters:\n");
		sb_.append(prefix_ + "   bw=" + bw + ", mean_pktsize=" + mean_pktsize  + ", ptc=" + ptc + "\n");
		sb_.append(prefix_ + "   minth=" + th_min + ",  maxth=" + th_max + "\n");
		sb_.append(prefix_ + "   1/max_p=" + (1.0/max_p) +",  1/qw=" + (1.0/qw) + ",  wait_flag=" + wait + "\n"); 
		return sb_.toString();
	}

	// print out states/variables
	protected String edv_print(String prefix_)
	{
		return prefix_ + "EDV:\n"
			+  prefix_ + "     qavg=" + qavg + ",  idletime=" + idletime + "\n"
			+  prefix_ + "     count=" + count + ",  last_drop_prob=" + v_prob + "\n";
	}
	
	
	// update precomputed variables
	void _updatePrecomputed()
	{
		v_a = max_p / (th_max - th_min);
		v_b = - th_min * max_p / (th_max - th_min);
	}
}
