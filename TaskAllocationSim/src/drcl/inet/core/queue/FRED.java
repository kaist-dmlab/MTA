// @(#)FRED.java   9/2002
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
import drcl.inet.core.Queue;
import drcl.data.DoubleObj;
import drcl.util.random.*;

/**
 * The <i>Fair Random Early Detection</i> queue logic.
 */
public class FRED extends RED implements drcl.net.PktClassifier
{
	public static boolean SANITY_CHECK = true;
	static final int MAXFLOWS = 200, MINQ = 2;

	/* internal states/variables  */
	int maxq;
	int minq = 0;
	int nactive = 0;
	FlowState[] flows = new FlowState[MAXFLOWS]; 
	PktClassifier classifier = this;

	public FRED()
	{ super();	}

	/**
	 * @param avgpid_ ID of the average queue size change event port that will be created at
	 * 		the host component.
	 */
	public FRED(Component host_, String avgpid_)
	{ super(host_, avgpid_); }

	public void reset()
	{
		super.reset();
		minq = 0; // for setting it up the first time in adviceOn()
		nactive = 0;
		for (int i=0; i<flows.length; i++)
			if (flows[i] != null) flows[i].reset();
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		FRED that_ = (FRED)source_;
		minq = that_.minq;
	}

	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.info(prefix_));
		sb_.append(prefix_ + "max_q = " + maxq + ",  min_q=" + minq + "\n");
		sb_.append(prefix_ + "Active flows: " + nactive + "\n");
		for (int i=0; i<flows.length; i++)
			if (flows[i] != null && flows[i].qlen > 0)
				sb_.append(prefix_ + "   flow " + i + ": " + flows[i] + "\n");
		return sb_.toString();
	}

	FlowState getFlow(Packet pkt_)
	{
		int id_ = classifier.classify(pkt_);
		if (flows[id_] == null) flows[id_] = new FlowState();
		return flows[id_];
	}

	public int classify(Packet pkt_)
	{
		// form flow id from a hash of src, dest, protocol.
		return (int)((((InetPacket)pkt_).getSource() + (((InetPacket)pkt_).getDestination()<< 16L)
					+((long)((InetPacket)pkt_).getProtocol())<<32L) % (MAXFLOWS-1));
	}

	public void setClassifier(PktClassifier c_)
	{ classifier = c_; }

	public PktClassifier getClassifier()
	{ return classifier; }

	/**
	 * Returns advice (in String) on whether or not to drop the packet.
	 * Returns false if not to drop the packet.
	 */
	public String adviceOn(Object obj_, int psize_)
	{
		// set up minq the first time 
		if (minq == 0) {
			if (psize_ == 1) // packet mode? just an ugly but quick check
				minq = MINQ;
			else
				minq = MINQ * mean_pktsize;
		}

		if (SANITY_CHECK) SanityCheck(); // FRED
		Packet pkt_ = (Packet)obj_;

		// FRED: distinguish flow
		FlowState flow_ = getFlow(pkt_);

		run_estimator(true);

		//FRED:
		// block A in FRED pseudocode (SIGCOMM'97)
		if (qavg >=  th_max) {
			if (psize_ == 1) // packet mode? ugly but quick check
				maxq = 2;
			else
				maxq = mean_pktsize << 1;
		}
		else
			maxq = (int)th_min;

		/* 
		 * FRED DROP LOGIC:
		 */

		double avgcq_ = nactive > 0? qavg/nactive: qavg;
		if (avgcq_ < 1.0) avgcq_ = 1.0;

		String droptype = DTYPE_NONE;

		// identify and manage non-adaptive flows
		if (flow_.qlen >= maxq ||
			// the next two lines represent line B in the FRED pseudocode (SIGCOMM'97) 
			(qavg >= th_max && flow_.qlen > 2*avgcq_) ||
			(flow_.qlen >= avgcq_ && flow_.strike > 1)) {
				++flow_.strike;
				droptype = DTYPE_FORCED;
		} else {
			/* operate in random drop mode */
			if (qavg < th_min) {
				// no drop mode:
				count = -1;
			}
			else if (qavg >= th_max) { 
				// drop-tail mode; the following is block C in the FRED pseudocde
				droptype = DTYPE_FORCED;
			}
			else {
	         	//th_min <= qavg < th_max
				// drop from robust flows only
				// drop_early() is from RED (superclass)
				if (flow_.qlen >= Math.max(minq, avgcq_) && drop_early(pkt_))
					droptype = DTYPE_UNFORCED;
			}
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
						droptype += ": flow:" + flow_ + ", maxq=" + maxq;
					else
						droptype += ": drop_prob=" + v_prob + ", count=" + count;
			}
			count = 0;
			return droptype;
		}
	}
	
	public void dequeueHandler(Object obj_, int psize_)
	{
		if (SANITY_CHECK) SanityCheck();
		super.dequeueHandler(obj_, psize_);
		FlowState flow_ = getFlow((Packet)obj_);
		flow_.qlen -= psize_;
		if (flow_.qlen == 0) {
			nactive--; flow_.strike = 0;
		}
		run_estimator(false);
	}

	public void enqueueHandler(Object obj_, int psize_)
	{
		super.enqueueHandler(obj_, psize_);
		FlowState flow_ = getFlow((Packet)obj_);
		if (flow_.qlen == 0) nactive++;
		flow_.qlen += psize_;
		run_estimator(false);
		count++;
	}

	/* check state consistency for all flows */
	void SanityCheck()
	{
		int total_ = 0;
	  
		for (int i = 0; i < MAXFLOWS; i++) {
			if (flows[i] == null) continue;
			if (flows[i].qlen != 0)
				total_ += flows[i].qlen;
			else if (flows[i].strike != 0)
				drcl.Debug.error("Error: invalid strike, flow " + i + ": " + flows[i]);
		}
	  
		if (total_ != qsize)
			drcl.Debug.error("Error: qsize=" + qsize + " total qlen in flows=" + total_);
	}

	class FlowState
	{
		int qlen = 0;        
		int strike = 0;      
		
		public void reset()
		{ qlen = 0; strike = 0; }
		
		public String toString()
		{ return "qlen=" + qlen + ",strike=" + strike; }
	}
}
