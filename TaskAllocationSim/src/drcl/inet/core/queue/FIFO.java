// @(#)FIFO.java   12/2003
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

/**
 * Implements a FIFO queue structure that one may plug in a different queue
 * logic to form a differnt FIFO implementation.  For example, one may plug in
 * the {@link RED} queue logic to form a random-early-detection (RED) queue;
 * plug in the {@link FRED} queue logic to form a fair RED (FRED) queue;
 * plug in the {@link SRED} queue logic to form a stablized RED (SRED) queue.
 *
 * Without any queue logic, this component works as a simple drop-tail
 * (by default) or drop-head queue.
 */
public class FIFO extends drcl.inet.core.Queue
{
	/** Name of the port that exports the instant queue size change events. */
	public static final String EVENT_QSIZE_PORT_ID = ".q";

	protected boolean drop_front = false;	/* drop-from-front (rather than from tail) */
	protected VSFIFOQueue q = null;

	QLogic qlogic;
	
	public FIFO()
	{ super(); }
	
	public FIFO(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		if (q != null) q.reset();
		if (qlogic != null) qlogic.reset();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		FIFO that_ = (FIFO)source_;
		if (that_.qlogic == null) setQLogic(null);
		else {
			try {
				qlogic = (QLogic)that_.qlogic.getClass().newInstance();
				// must sethost() before calling qlogic.duplicate() so that
				// the event ports are also duplicated
				qlogic.setHost(this);
				qlogic.duplicate(that_.qlogic);
			} catch (Exception e_) {
				drcl.Debug.error(e_);
				e_.printStackTrace();
			}
		}
		drop_front = that_.drop_front;
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
			   + prefix_ + "DropFront:" + drop_front + "\n"
			   + (qlogic == null? "": prefix_ + "QueueLogic:\n" + qlogic.info(prefix_ + "   ") + "\n")
			   + prefix_ + (q == null? "Queue is empty.\n": "Content:\n" + q.info(prefix_ + "   "));
	}

	public void setQLogic(QLogic qlogic_)
	{
		if (qlogic != null && qlogic_ != null) qlogic_.capacity = qlogic.capacity;
		qlogic = qlogic_;
		if (qlogic != null) qlogic.set(this, EVENT_QSIZE_PORT_ID);
	}

	public QLogic getQLogic()
	{ return qlogic; }
	
	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public Object enqueue(Object obj_)
	{
		Packet p_ = (Packet)obj_;
		int psize_ = isByteMode()? p_.size: 1;
		
		if (q == null) {
			if (qlogic == null) {
				setQLogic(new QLogic());
				qlogic.capacity = 65536; // bytes; default size
			}
			q = new VSFIFOQueue();
		}

		String advice_ = qlogic.adviceOn(p_, psize_);
		
		if (advice_ != null) {
			if (drop_front) {
				int total_ = 0;
				while (total_ < psize_) {
					Packet tmp_ = (Packet)dequeue();
					total_ += isByteMode()? tmp_.size: 1;
					if (isGarbageEnabled())
						drop(tmp_, "<DROP_FRONT>" + advice_);
				}
				q.enqueue(p_, psize_);
				qlogic.enqueueHandler(p_, psize_);
			}
			else {
				if (isGarbageEnabled())
					drop(p_, advice_);
				qlogic.dropHandler(p_, psize_);
			}
		}
		else {
			q.enqueue(obj_, psize_);
			qlogic.enqueueHandler(p_, psize_);
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
		if (p_ != null)
			qlogic.dequeueHandler(p_, isByteMode()? p_.size: 1);
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
	{ return qlogic == null? false: qlogic.isFull();	}
	
	/** Return true if the queue is empty.  */
	public boolean isEmpty()
	{ return qlogic == null? true: qlogic.isEmpty();	}
	
	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{
		if (qlogic == null) setQLogic(new QLogic()); // to save the capacity
		qlogic.capacity = capacity_;
	}
	
	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return qlogic == null? 0: qlogic.capacity; }
	
	/** Returns the current size of the queue.  */
	public int getSize()
	{ return qlogic == null? 0: qlogic.qsize; }
	
	public void setDropHeadEnabled(boolean enabled_)
	{	drop_front = enabled_;}	
	
	public boolean isDropHeadEnabled()
	{	return drop_front;}	
}
