// @(#)scheduler_WF2Q.java   9/2002
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

package drcl.intserv.scheduler;

import java.util.Vector;
import java.util.Enumeration;
import drcl.net.Packet;
import drcl.intserv.*;
import drcl.util.queue.*;

/**
The Worse-case fair weighted fair queueing scheduler.
Accepts any type of Rspec.
 */
public class scheduler_WF2Q extends Scheduler
{
	Queue pq = QueueAssistant.getBest(); // Only one queue is used for all sessions, store vparam instead of packet.
	                              // When qosDeque(), we search for flow with the first packet in queue
	                              // that has started transmission under GPS with smallest virtual finishing
	                             // time.  See qosEnque() and qosDeque() for details.
	Vector vflow = new Vector(3, 3); // handle -> vparam
	
	// GPS busy period;
	// In our implementation, we only update these parameters when a packet arrives (qosEnque()).
	// It is sufficient theoretically.
	double bsLast; // last time when the busy set is changed, in real time 
	double vbsLast; // last time when the busy set is changed, in virutal time, V(bsLast) = vbsLast
	long activeLoad; // sum of bandwidth of active flows (w/ backlog), 
	                   // used to map virtual time to real time.
	Queue vpq = QueueAssistant.getBest(); // Packet queue for departure under GPS;
	                               // sorted by virtual finishing time, which is used to calculate 
	                               // next GPS departure event in real time.  The object stored in
	                               // the queue is the <code>vparam</code> of the corresponding flow.
	
	public scheduler_WF2Q()
	{ super(); }
	
	public scheduler_WF2Q(String id_) 
	{ super(id_); }
	
	public void reset() 
	{
		super.reset();
		vpq.reset();
		pq.reset();
		vflow.removeAllElements();
		activeLoad = 0;
		bsLast = vbsLast = 0.0; // not necessary
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof scheduler_WF2Q)) return;
		super.duplicate(source_);
		scheduler_WF2Q that_ = (scheduler_WF2Q)source_;
	}
	
	protected void qosEnque(Packet p_, SpecR rspec_)
	{
		vparam vp_ = (vparam)vflow.elementAt(rspec_.handle);
		if (vp_ == null) {
			drcl.Debug.error(this, "no flow info is installed for " + p_);
			return;
		}
		
		double now_ = getTime();
		update(now_); // update busy period paramaters until now_
		
		double s_ = vp_.F;
		if (vp_.backlog++ == 0) { 
			if (isDebugEnabled()) debug("Start a busy period");
			// first packet of the flow in the current busy period
			s_ = vbsLast = activeLoad == 0.0? now_: // start of a new busy period
											  (now_ - bsLast) * maxBW / activeLoad + vbsLast;
			bsLast = now_;
			vp_.F = s_ + (double)(p_.size<<3) / vp_.fei;
			activeLoad += vp_.fei;
		}
		else {
			// busy set does not change
			vp_.F += (double)(p_.size<<3) / vp_.fei;
		}

		// queues for real scheduling
		if (vp_.pq.isEmpty())
 			pq.enqueue(vp_.F, vp_); 
		vp_.pq.enqueue(s_, p_); // fifo enqueue with key = virtual starting (of transmission) time
		
		// queue for GPS
		vpq.enqueue(vp_.F, vp_);
   }

	// Updates busy period parameters until now_, from bsLast.
	// This is the same as in PGPS
	void update(double now_)
	{
		while (!vpq.isEmpty()) {
			double vdtime_ = vpq.firstKey(); // next departure time under GPS in virtual time
			double dtime_ = (vdtime_ - vbsLast) * activeLoad / maxBW + bsLast; // next departure time under GPS in real time
			if (dtime_ > now_) break;
			bsLast = dtime_;
			vbsLast = vdtime_;
			
			vparam vp_ = (vparam)vpq.dequeue();
			if (--vp_.backlog == 0) {
				// no more packet for the flow, under GPS
				// busy set changes
				activeLoad -= vp_.fei;
				//if (Math.abs(activeLoad) < 1e-6) activeLoad = 0.0; // rounding error
			}
			else {
				// busy set does not change; do nothing
			}
		}
	}
	
	protected Packet qosDeque()
	{
		// Note 1: packet departure under WF2Q means nothing to GPS reference system.
		// Note 2: (XX) can check if the packet's rspec still exists, but what the heck.
		if (pq.isEmpty()) return null;
		
		// Since pq is not empty, now is still in a busy period under GPS
		// activeLoad must > 0.0 and bsLast and vbsLast must be valid.
		// => the following code block is valid
		
		double now_ = getTime();
		update(now_);
		double vnow_ = (now_ - bsLast) * maxBW / activeLoad + vbsLast; // virtual current time
		if (isDebugEnabled())
			debug("Now: " + now_ + " <--> virtual now: " + vnow_);
		int count_ = 0;
		for (Enumeration e_ = pq.getElementEnumerator(); e_.hasMoreElements(); ) {
			vparam vp_ = (vparam)e_.nextElement();
			double s_ = vp_.pq.firstKey(); // virtual starting time of the first packet in queue
			if (s_ <= vnow_ || Math.abs(s_-vnow_) < 1.0e-6) {
				Packet p_ = (Packet)vp_.pq.dequeue();
				// update pq:
				pq.remove(count_); // remove vp_ from pq
				if (!vp_.pq.isEmpty()) {
					// re-enqueue vp_ to pq
					double nexts_ = vp_.pq.firstKey();
					Packet next_ = (Packet)vp_.pq.firstElement();
					pq.enqueue(nexts_ + (double)(next_.size<<3) / vp_.fei, vp_);
				}
				return p_;
			}
			count_ ++;
		}
		drcl.Debug.error(this, "qosDeque()| should not reach here");
		return null;
	}

	public synchronized int setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{	
		handle_ = super.setFlowspec(handle_, tos_, tosmask_, fspec_);
		if (handle_ == -1) return -1;
		vparam vp_ = new vparam(handle_, fspec_.rspec.getBW());
		
		// XX: should we discard the packets that are still queued and belong to the discarded flow?
		if (vflow.size() <= handle_) vflow.setSize(handle_ + 1);
		vflow.setElementAt(vp_, handle_);
		return handle_;
	}

	public synchronized SpecFlow removeFlowspec(int handle_)
	{	
		SpecFlow fspec_ = super.removeFlowspec(handle_);
		if (fspec_ == null) return null;
		
		vparam vp_ = (vparam)vflow.elementAt(handle_);
		vflow.setElementAt(null, handle_);
		
		// packets in the queue still get transmitted, see qosDeque()
		return fspec_;
	}

	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<vflow.size(); i++) {
			vparam vp_ = (vparam)vflow.elementAt(i);
			if (vp_ != null) sb_.append("   " + i + ": " + vp_ + "\n");
		}
		if (sb_.length() == 0) sb_.append(super.info());
		else sb_.insert(0, super.info() + "Handle -> flow param\n");

		sb_.append("Busy set changed last at " + bsLast + " (virtual:" + vbsLast + ")\n");
		sb_.append("Current load: " + activeLoad + " (sum of bandwidths of backlogged flows)\n");
		if (pq.isEmpty())
			sb_.append("Packet_Q: <empty>\n");
		else
			sb_.append("Packet_Q: " + pq.info("   "));
		if (vpq.isEmpty())
			sb_.append("GPS_Packet_Q: <empty>\n");
		else
			sb_.append("GPS_Packet_Q: " + vpq.info("   "));
		return sb_.toString();
	}
		
	// for keeping parameters of a flow under GPS
	static class vparam extends drcl.DrclObj
	{
		double	F;				// virtual finishing time
		int	fei;			// service share = rspec.getBW()
		int		backlog;		// backlog
		Queue	pq = new FIFOQueue(); // store real packets for departure in real system;
		                              // irrelevant to GPS
		int		handle;			// for debugging

		vparam (int handle_, int fei_)
		{
			F		= 0.0;
			fei		= fei_;
			backlog	= 0;
			handle = handle_;
		}

		public String toString()
		{
			return "handle:" + handle + ", fei=" + fei + ", backlog=" + backlog
				+ ", VF=" + F + ", pq:" + pq.oneline();
		}
	}
}
