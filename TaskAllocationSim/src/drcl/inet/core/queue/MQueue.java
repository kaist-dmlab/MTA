// @(#)MQueue.java   12/2003
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
 * MQueue is an <i>m</i>-level queue generalized from <i>RIO</i> and
 * <i>3-Color</i> queue.  Specifically, it is a FIFO queue with <i>m</i> queue
 * logics, one for each level.  The level-0 queue is the highest level queue
 * while the level-<code>(<i>m</i>-1)</code> queue is the lowest.
 * It uses a {@link drcl.net.PktClassifier} to
 * classify the incoming packets into <i>m</i> levels.  When a packet arrives,
 * the packet is classified into one of the <i>m</i> level.  If the packet can
 * be enqueued in that level (permitted by the corresponding queue logic), 
 * the packet is also enqueued to all the queues of lower levels without being
 * rejected.  Dequeuing follows FIFO, the dequeued packet is also dequeued from
 * the queues of its (classified) level and all the lower levels.
 *
 * The component exports an error if no level of queue is found for the packet.
 */
public class MQueue extends drcl.inet.core.Queue
		implements drcl.net.PktClassifier
{
	/** Name of the port that exports the instant queue size change events. */
	public static final String EVENT_QSIZE_PORT_ID = ".q";

	/** Flag enabling/disabling dropping packets at front of the queue,
	 * default is disabled. */
	boolean drop_front = false;

	/** The real queue that holds packets. */
	FiniteVSFIFOQueue q = null;

	/** Levels of queue logics. */
	QLogic[] qlogics;
	
	PktClassifier classifier = this;
	
	public MQueue()
	{ super(); }
	
	public MQueue(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		if (q != null) q.reset();
		if (qlogics != null) {
			for (int i=0; i<qlogics.length; i++)
				qlogics[i].reset();
		}
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		MQueue that_ = (MQueue)source_;
		drop_front = that_.drop_front;
		if (that_.q != null)
			setCapacity(that_.getCapacity());
		classifier = that_.classifier; // FIXME:??

		if (that_.qlogics == null) qlogics = null;
		else {
			try {
				qlogics = new QLogic[that_.qlogics.length];
				for (int i=0; i<qlogics.length; i++) {
					QLogic thatqlogic_ = that_.qlogics[i];
					if (thatqlogic_ == null) {
						setQLogic(i, null); continue;
					}
					QLogic qlogic_ =
							(QLogic)thatqlogic_.getClass().newInstance();
					// must sethost() before calling qlogic.duplicate() so that
					// the event ports are also duplicated
					qlogic_.setHost(this);
					qlogic_.duplicate(thatqlogic_);
					setQLogic(i, qlogic_);
				}
			} catch (Exception e_) {
				drcl.Debug.error(e_);
				e_.printStackTrace();
			}
		}
	}
	
	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.info(prefix_) + prefix_
										+ "DropFront:" + drop_front + "\n"
										+ "Classifier:" + classifier + "\n");
		if (qlogics == null)
			sb_.append(prefix_ + "No queue logic is installed.\n");
		else {
			sb_.append(prefix_ + "QueueLogics: # of levels="
							+ qlogics.length + "\n");
			for (int i=0; i<qlogics.length; i++) {
				if (qlogics[i] == null) continue;
				sb_.append(prefix_ + "   Level" + i + ":\n"
								+ qlogics[i].info(prefix_ + "      "));
			}
		}
		if (q == null)
			sb_.append(prefix_ + "Queue is empty.\n");
		else
			sb_.append(prefix_ + "Content:\n" + q.info(prefix_ + "   "));
		return sb_.toString();
	}

	/**
	 * Implements {@link drcl.net.PktClassifier#classify(drcl.net.Packet)}.
	 * It simply return the first bit of the ToS field of the INET header.
	 */
	public int classify(Packet p_)
	{ return (int) ((InetPacket)p_).getTOS() & 01; }

	public void setClassifier(PktClassifier c)
	{ classifier = c; }

	public PktClassifier getClassifier()
	{ return classifier; }

	// make sure array has a spot at the level_
	void _assureLogics(int level_)
	{
		if (level_ < 0)
			throw new IndexOutOfBoundsException("negative index:" + level_);
		if (qlogics == null) qlogics = new QLogic[level_ + 1];
		if (qlogics.length > level_) return;

		// create a larger array
		QLogic[] tmp_ = new QLogic[level_ + 1];
		System.arraycopy(qlogics, 0, tmp_, 0, qlogics.length);
		qlogics = tmp_;
	}

	/**
	 * @param level_ level index of the queue logic.
	 */
	public void setQLogic(int level_, QLogic qlogic_)
	{
		_assureLogics(level_);
		qlogics[level_] = qlogic_;
		qlogic_.setCapacity(getCapacity());
	}

	public QLogic getQLogic(int level_)
	{ return qlogics[level_]; }
	
	// enqueues the object at the level and lower
	void _enqueue(Packet p_, int psize_, int level_)
	{
		for (int i=level_; i<qlogics.length; i++)
			if (qlogics[i] != null)
				qlogics[i].enqueueHandler(p_, psize_);
		q.enqueue(p_, psize_);
	}

	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public Object enqueue(Object obj_)
	{
		if (qlogics == null) error("enqueue()", "no q logic installed");

		Packet p_ = (Packet)obj_;
		int psize_ = isByteMode()? p_.size: 1;
		int level_ = classifier.classify(p_);
		if (level_ < 0 || level_ >= qlogics.length || qlogics[level_] == null)
			error("enqueue()", "no q logic installed for " + p_);
		
		// overflow?
		if (q == null) q = new FiniteVSFIFOQueue();
		if (q.getSize() + psize_ > q.getCapacity()) {
			if (isGarbageEnabled()) drop(p_, "exceeds capacity");
			return null;
		}

		String advice_ = qlogics[level_].adviceOn(p_, psize_);
		
		if (advice_ != null) {
			if (drop_front) {
				int total_ = 0;
				while (total_ < psize_) {
					Packet tmp_ = (Packet)dequeue();
					total_ += isByteMode()? tmp_.size: 1;
					if (isGarbageEnabled())
						drop(tmp_, "<DROP_FRONT>" + advice_);
				}
				_enqueue(p_, psize_, level_);
			}
			else {
				if (isGarbageEnabled()) drop(p_, advice_);
				qlogics[level_].dropHandler(p_, psize_);
			}
		}
		else 
			_enqueue(p_, psize_, level_);
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
		if (p_ != null) {
			int level_ = classifier.classify(p_);
			for (int i=level_; i<qlogics.length; i++)
				if (qlogics[i] != null)
					qlogics[i].dequeueHandler(p_, isByteMode()? p_.size: 1);
		}
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
	{ return q == null? false: q.isFull();	}
	
	/** Return true if the queue is empty.  */
	public boolean isEmpty()
	{ return q== null? true: q.isEmpty();	}
	
	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{
		if (q == null) q = new FiniteVSFIFOQueue(capacity_);
		else q.setCapacity(capacity_);

		if (qlogics != null)
			for (int i=0; i<qlogics.length; i++)
				if (qlogics[i] != null)
					qlogics[i].setCapacity(capacity_);

	}
	
	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return q == null? Integer.MAX_VALUE: q.getCapacity(); }
	
	/** Returns the current size of the queue.  */
	public int getSize()
	{ return q == null? 0: q.getSize(); }
	
	public void setDropHeadEnabled(boolean enabled_)
	{	drop_front = enabled_;}	
	
	public boolean isDropHeadEnabled()
	{	return drop_front;}	
}
