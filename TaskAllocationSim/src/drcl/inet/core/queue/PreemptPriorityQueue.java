// @(#)PreemptPriorityQueue.java   12/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

import java.util.Vector;
import drcl.data.*;
import drcl.util.queue.*;
import drcl.comp.*;
import drcl.comp.queue.QLogic;
import drcl.net.*;
import drcl.inet.InetPacket;

/**
 * PreemptPriorityQueue is the same as {@link PriorityQueue} except
 * that an arrived higher-priority packet can preempt (and drop)
 * a lower-priority packet when the queue is full.
 */
public class PreemptPriorityQueue extends PriorityQueue
{
	public PreemptPriorityQueue()
	{ super(); }
	
	public PreemptPriorityQueue(String id_)
	{ super(id_); }
	
	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public Object enqueue(Object obj_)
	{
		Packet p_ = (Packet)obj_;
		int psize_ = isByteMode()? p_.size: 1;
		int level_ = classifier.classify(p_);
		if (level_ < 0 || level_ >= qq.length)
			level_ = qq.length-1; // lowest priority
		
		String advice_ = null;
		if (qlogics != null && qlogics.length > level_
						&& qlogics[level_] != null)
			advice_ = qlogics[level_].adviceOn(p_, psize_);

		if (advice_ != null || qq[level_].getSize() + psize_ > capacity) {
			if (drop_front) {
				int total_ = 0;
				while (total_ < psize_) {
					Packet tmp_ = (Packet)qq[level_].dequeue();
					int tmpsize_ = isByteMode()? tmp_.size: 1;
					total_ += tmpsize_;
					if (advice_ != null)
						qlogics[level_].dequeueHandler(tmp_, tmpsize_); //??
					if (isGarbageEnabled()) {
						if (advice_ != null)
							drop(tmp_, "<DROP_FRONT>" + advice_);
						else
							drop(tmp_, "<DROP_FRONT> buffer overflow");
					}
				}
				currentSize += psize_ - total_;
				if (advice_ != null) qlogics[level_].enqueueHandler(p_, psize_);
				qq[level_].enqueue(p_);
			}
			else {
				if (isGarbageEnabled()) {
					if (advice_ != null)
						drop(p_, advice_);
					else
						drop(p_, "buffer overflow");
				}

				if (advice_ != null)
					qlogics[level_].dropHandler(p_, psize_);
			}
			return null;
		}

		// put pkt to corresponding queue

		currentSize += psize_;
		if (isByteMode())
			qq[level_].enqueue(p_, psize_);
		else
			qq[level_].enqueue(p_);

		if (currentSize > capacity) {
			// preempt lower-priority pkt
			for (int i = qq.length-1; i>level_; i--) {
				VSFIFOQueue q = qq[i];
				if (q.isEmpty()) continue;

				while (!q.isEmpty()) {
					Packet tmp_ = drop_front?
							(Packet)q.dequeue():
							(Packet)q.remove(q.getLength()-1);

					int tmpsize_ = isByteMode()? tmp_.size: 1;
					currentSize -= tmpsize_;
					if (isGarbageEnabled()) drop(tmp_, "preempted");
					if (currentSize <= capacity) break;
				}
				if (currentSize <= capacity) break;
			}
		}

		return null;
	}
}
