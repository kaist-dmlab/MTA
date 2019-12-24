// @(#)pq.java   9/2002
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
  * A Simple Priority Queue Scheduler
  * NOTE: for convenience, assume that entry is ordered by weight in descending
  * order, i.e., when you do addQueueSet in script file, you should add queue with 
  * higher priority first. This makes sense for static configuration
  * 
  * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
  * @version 1.0  07/16/2000
  * @version 1.1  05/22/2002
  * 
  */

import drcl.inet.core.Queue;
import drcl.util.queue.SimpleQueue;
import java.util.Enumeration;

public class pq extends drcl.diffserv.HQS
{
	SimpleQueue qq;

	public pq()
	{ super(); }

	public pq(String id)
	{ super(id); }
			
	protected Queue pickEligibleQueue(boolean dequeue_)
	{
		if (qq == null) return null;

		for (Enumeration e_ = qq.getElementEnumerator(); e_.hasMoreElements(); ) {
			Queue q_ = (Queue) e_.nextElement();
			if (!q_.isEmpty()) return q_;
		}
		
		return null;
	}

	/**
	 * Adds the child queue with the same priority as (the currently lowest priority 
	 * in this HQS + 1).
	 */
	public void addQueueSet(Queue child_, long classMask_, long classId_) 
	{
		super.addQueueSet(child_, classMask_, classId_);
		if (qq == null) qq = new SimpleQueue();
		double lastKey_ = qq.lastKey();
		if (Double.isNaN(lastKey_)) lastKey_ = 0.0;
		qq.enqueue(lastKey_ + 1.0, child_);
	}

	/**
	 * Adds the child queue with the specified priority.
	 * The smaller value of <code>priority_</code> corresponds to higher priority.
	 */
	public void addQueueSet(Queue child_, long classMask_, long classId_, int priority_) 
	{
		super.addQueueSet(child_, classMask_, classId_);
		if (qq == null) qq = new SimpleQueue();
		qq.enqueue((double)priority_, child_);
	}
	
	public void removeQueueSet(Queue child_)
	{
		super.removeQueueSet(child_);
		if (qq != null) qq.remove(child_);
	}

	protected String configInfo(String prefix_, Queue q_)
	{
		if (qq == null) return "";
		double priority_ = qq.retrieveKey(q_);
		if (Double.isNaN(priority_))
			return prefix_ + "priority: NaN";
		else
			return prefix_ + "priority: " + ((int)priority_);
	}
}
