// @(#)wrr.java   9/2002
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

/**
  * A Deficit Weighted Round Robin Scheduler
  * Base on M. Shreedhar and G. Varghese, "Efficient fair queuing using deficit round robin," 
  * Proc. of ACM SIGCOMM '95, Aug. 1995
  * 
  * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
  * @version 1.0 10/26/2000
  * @version 1.1 05/22/2002, Hung-ying Tyan (tyanh@ieee.org)
  * 
  */

import java.util.*;
import drcl.inet.core.Queue;
import drcl.net.*;

public class wrr extends drcl.diffserv.HQS
{
	int	lastServed = 0;
	Vector vQInfo;
	Hashtable htQToQInfo;
	
	public wrr()
	{ super(); }

	public wrr(String id)
	{ super(id);}
	
	public void reset()
	{
		super.reset(); lastServed = 0;
		if (vQInfo != null) 
			for (int i = 0; i < vQInfo.size(); i++) {
				QInfo qi_ = (QInfo)vQInfo.elementAt(i);
				if (qi_ == null) continue;
				qi_.reset();
			}
	}
	
	protected Queue pickEligibleQueue(boolean dequeue_)
	{
		if (vQInfo == null) return null;
		Packet  p_ = null;

		/*
		boolean hasPacket_ = false;
		for (int nturn_ = 0; nturn_ < vQInfo.size() || hasPacket_;
			nturn_++, lastServed = (lastServed + 1) % vQInfo.size()) {
			QInfo qi_ = (QInfo)vQInfo.elementAt(lastServed);
			if (qi_ == null) continue;
			Queue q_ = qi_.q;
			if(qi_.turn == 0){
				qi_.counter += qi_.weight;
				qi_.turn = 1;
			}
			p_ = (Packet)q_.firstElement();
			if (p_ != null) { 
				hasPacket_ = true;
				if(p_.size <= qi_.counter) {
					qi_.counter -= p_.size;
					if(isDebugEnabled())
						debug("nturn:" + nturn_ + ", lastServed:" + lastServed+ ", QInfo:" + qi_ + ", pkt=" + p_);
					return q_;
				} else {
					qi_.turn = 0;
					//if(isDebugEnabled())
					//	debug("not enough counter, lastServed:" + lastServed+ ", QInfo:" + qi_);
				}
			} else { 
				// dont accumulate credit when queue is empty
				qi_.turn = 0;
				qi_.counter = 0;
				
				//if(isDebugEnabled())				
				//	debug("empty queue, lastServed:" + lastServed+ ", QInfo:" + qi_);
			}
		}
		return null;
		}
		*/
		int lastserved_ = lastServed;
		int minnturn_ = Integer.MAX_VALUE;
		QInfo best_ = null;

		Packet pkt_ = null; // the first packet in the best child queue

		for (int i = 0; i < vQInfo.size(); i++,
						lastserved_ = (lastserved_ + 1) % vQInfo.size()) {
			QInfo qi_ = (QInfo)vQInfo.elementAt(lastserved_);
			if (qi_ == null) continue;
			p_ = (Packet)qi_.q.firstElement();
			if (p_ != null) { 
				// calculate # of turns needed for this child queue to be
				// 	eligible
				qi_.turn = true;
				int tmp_ = (p_.size - qi_.counter + qi_.weight - 1) / qi_.weight;
				if (tmp_ < minnturn_) {
					best_ = qi_;
					minnturn_ = tmp_;
					if (dequeue_) {
						lastServed = lastserved_;
						pkt_ = p_;
					}
				}
			}
			else
				qi_.turn = false;
		}
		if (best_ != null && dequeue_) {
			for (int i = 0; i < vQInfo.size(); i++) {
				QInfo qi_ = (QInfo)vQInfo.elementAt(i);
				if (qi_ == null || !qi_.turn) continue;
				qi_.counter += qi_.weight * minnturn_;
				if (qi_ == best_) {
					qi_.counter -= pkt_.size;
					if(isDebugEnabled())
						debug("nturn:" + minnturn_ + ", lastServed:" + lastServed
										+ ", QInfo:" + qi_ + ", pkt=" + pkt_);
				}
			}
		}
		return best_ == null? null: best_.q;
	}
	
	public void addQueueSet(Queue child_, long classMask_, long classId_) 
	{ drcl.Debug.error("Should use addQueueSet(Queue, long, long, int) to specify weight"); }

	public void addQueueSet(Queue child_, long classMask_, long classId_, int weight_) 
	{
		super.addQueueSet(child_, classMask_, classId_);
		if (vQInfo == null) {
			vQInfo = new Vector(3);
			htQToQInfo = new Hashtable(3);
		}
		QInfo new_ = new QInfo(weight_, child_);
		vQInfo.addElement(new_);
		htQToQInfo.put(child_, new_);
	}
	
	public void removeQueueSet(Queue child_)
	{
		super.removeQueueSet(child_);
		if (vQInfo != null) {
			QInfo qi_ = (QInfo)htQToQInfo.remove(child_);
			if (qi_ != null) vQInfo.removeElement(qi_);
		}
	}

	protected String configInfo(String prefix_)
	{
		if (vQInfo == null) return "";
		Queue lastServed_ = ((QInfo)vQInfo.elementAt(lastServed)).q;
		return prefix_ + "LastServed: " + lastServed_.getID() + "\n";
	}

	protected String configInfo(String prefix_, Queue q_)
	{
		QInfo qi_ = (QInfo)htQToQInfo.get(q_);
		return prefix_ + qi_;
	}

	class QInfo {
		int weight;
		int counter = 0;
		boolean turn = false;
		Queue q;

		public QInfo(int w_, Queue q_)
		{
			weight = w_;
			q = q_;
		}

		void reset()
		{
			counter = 0;
			turn = false;
		}

		public String toString()
		//{ return "weight=" + weight + ", counter=" + counter + ", turn=" + turn; }
		{ return "weight=" + weight + ", counter=" + counter; }
	}
}
