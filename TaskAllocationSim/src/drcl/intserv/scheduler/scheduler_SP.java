// @(#)scheduler_SP.java   9/2002
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

import java.util.*;

import drcl.net.*;
import drcl.intserv.*;
import drcl.util.scalar.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;

/**
The Static priority scheduler.
It uses RM to schedule the incoming packets.
Accepts any <code>SpecR</code> that implements <code>SpecR_SP</code>.
@see SpecR_SP
 */
public class scheduler_SP extends drcl.intserv.Scheduler
{
	Queue pq = QueueAssistant.getBest();		// QoS Packet queue vector
	
	public void reset() 
	{
		super.reset();
		pq.reset();
	}

	protected void qosEnque(Packet p_, SpecR rspec_)
    {
		if (!(rspec_ instanceof SpecR_SP)) {
			drop(p_, "unknown rspec format");
			return;
		}
		pq.enqueue(((SpecR_SP)rspec_).getPriority(), p_);
	}

	protected Packet qosDeque()
    { return (Packet)pq.dequeue(); }

	public synchronized int setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{	
		handle_ = super.setFlowspec(handle_, tos_, tosmask_, fspec_);
		if (handle_ == -1) return -1;
		return handle_;
	}

	public synchronized SpecFlow removeFlowspec(int handle_)
	{	
		return super.removeFlowspec(handle_);
	}
}


