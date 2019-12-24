// @(#)DropTail.java   7/2003
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
import drcl.net.*;

public class DropTail extends drcl.inet.core.Queue
{
	public static final String EVENT_QLEN = "Instant Q Length";
	protected Port qLenPort = addEventPort(".q");

	protected VSFIFOQueue q = null;
	protected int capacity = DEFAULT_BUFFER_SIZE; // default in bytes
	
	public DropTail()
	{ super(); }
	
	public DropTail(String id_)
	{ super(id_); }
	
	public void reset()
	{
		if (q != null) q.reset();
		super.reset();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		DropTail that_ = (DropTail)source_;
		capacity = that_.capacity;
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
			   + prefix_ + (q == null? "Queue is empty.\n": "Content:\n" + q.info(prefix_ + "   "));
	}
	
	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public Object enqueue(Object obj_)
	{
		Packet p_ = (Packet)obj_;
		int psize_ = isByteMode()? p_.size: 1;
		
		if (psize_ > capacity) {
			if (isGarbageEnabled()) {
				if (isDebugEnabled()) drop(p_, "pkt too large: " + psize_
					+ ">" + capacity);
				else drop(p_, "pkt too large");
			}
			return null;
		}
		
		if (q == null) q = new VSFIFOQueue();
		if (q.getSize() + psize_ > capacity) {
			if (isGarbageEnabled()) {
				if (isDebugEnabled()) drop(p_, "exceeds capacity: " + psize_
					+ "+" + q.getSize() + ">" + capacity);
				else drop(p_, "exceeds capacity");
			}
		}
		else {
			q.enqueue(obj_, psize_);
			//if (isDebugEnabled())
			//	debug("qsize=" + q.getSize() + ", enqueue " + p_);
			if (qLenPort._isEventExportEnabled())
				qLenPort.exportEvent(EVENT_QLEN, (double)q.getSize(), null);
		}
		return null;
	}
	
	/**
	 * Dequeues and returns the first object in the queue.
	 * @return the object dequeued; null if queue is empty.
	 */
	public Object dequeue()
	{
		if (q == null || q.isEmpty()) return null;
		Packet p_ = (Packet) q.dequeue();
		if (qLenPort._isEventExportEnabled())
			qLenPort.exportEvent(EVENT_QLEN, (double)q.getSize(), null);
		return p_;
	}
	
	/**
	 * Retrieves but not remove the object at the position specified.
	 * @return the object; null if position is not valid.
	 */
	public Object peekAt(int pos_)
	{ return  q == null? null: q.retrieveAt(pos_); }
	
	/**
	 * Retrieves but not dequeue the first object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object firstElement()
	{ return  q == null? null: q.firstElement(); }

	/**
	 * Retrieves but not remove the last object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object lastElement()
	{ return  q == null? null: q.lastElement(); }

	/** Return true if the queue is full. */
	public boolean isFull()
	{ return q == null? false: q.getSize() == capacity;	}
	
	/** Return true if the queue is empty. */
	public boolean isEmpty()
	{ return q == null? true: q.isEmpty();	}
	
	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{ capacity = capacity_; }
	
	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return capacity; }
	
	/** Returns the current size of the queue. */
	public int getSize()
	{ return q == null? 0: q.getSize(); }
}
