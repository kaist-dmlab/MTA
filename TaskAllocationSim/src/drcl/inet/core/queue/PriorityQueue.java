// @(#)PriorityQueue.java   12/2003
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
 * PriorityQueue is an <i>m</i>-level queue.  <i>m</i> is configurable.
 * Specifically, it consists of <i>m</i> FIFO queues, each associated with
 * a queue logic.
 * The level-0 queue has the highest priority
 * while the level-<code>(<i>m</i>-1)</code> queue has the lowest.
 * It uses a {@link drcl.net.PktClassifier} to
 * classify the incoming packets into <i>m</i> levels.  When a packet arrives,
 * the packet is classified into one of the <i>m</i> level and is put in that
 * FIFO queue if the corresponding queue logic permits. 
 *
 * The component treats a packet as lowest priority if no level of queue
 * is found for the packet.
 */
public class PriorityQueue extends drcl.inet.core.Queue 
	implements drcl.net.PktClassifier
{
	/** Name of the port that exports the instant queue size change events. */
	public static final String EVENT_QSIZE_PORT_ID = ".q";

	/** Flag enabling/disabling dropping packets at front of the queue, default is disabled. */
	boolean drop_front = false;

	/** The real queue that holds packets. */
	VSFIFOQueue[] qq = null;

	/** Levels of queue logics. */
	QLogic[] qlogics;
	
	PktClassifier classifier = this;

	int capacity = Integer.MAX_VALUE;
	int currentSize = 0;

	public PriorityQueue()
	{ super(); }
	
	public PriorityQueue(String id_)
	{ super(id_); }
	
	/**
	 * Implements {@link drcl.net.PktClassifier#classify(drcl.net.Packet)}.
	 * It simply return (1 - the first bit of the ToS field of the INET header)
	 * so CONTROL packets have higher priority than DATA packets. 
	 */
	public int classify(Packet p_)
	{ return 1 - (int) ((InetPacket)p_).getTOS() & 01; }

	public void setClassifier(PktClassifier pc_)
	{ classifier = pc_; }

	public void reset()
	{
		super.reset();
		if (qq != null)
			for (int i=0; i<qq.length; i++)
				qq[i].reset();
		if (qlogics != null) {
			for (int i=0; i<qlogics.length; i++)
				if (qlogics[i] != null)
					qlogics[i].reset();
		}
		currentSize = 0;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		PriorityQueue that_ = (PriorityQueue)source_;
		drop_front = that_.drop_front;
		classifier = that_.classifier; // FIXME:??
		setLevels(that_.qq.length);

		if (that_.qlogics == null) qlogics = null;
		else {
			try {
				qlogics = new QLogic[that_.qlogics.length];
				for (int i=0; i<qlogics.length; i++) {
					QLogic thatqlogic_ = that_.qlogics[i];
					if (thatqlogic_ == null) {
						setQLogic(i, null); continue;
					}
					QLogic qlogic_ = (QLogic)thatqlogic_.getClass().newInstance();
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
		try {
		StringBuffer sb_ = new StringBuffer(super.info(prefix_) + prefix_
											+ "DropFront:" + drop_front + "\n"
											+ "Classifier:" + classifier + "\n");
		if (qlogics == null)
			sb_.append(prefix_ + "No queue logic is installed.\n");
		else {
			sb_.append(prefix_ + "QueueLogics: # of levels=" + qlogics.length + "\n");
			for (int i=0; i<qlogics.length; i++) {
				if (qlogics[i] == null) continue;
				sb_.append(prefix_ + "   Level" + i + ":\n" + qlogics[i].info(prefix_ + "      "));
			}
		}
		if (qq == null || qq.length == 0)
			sb_.append(prefix_ + "No queue is installed.\n");
		else {
			for (int i=0; i<qq.length; i++)
				sb_.append(prefix_ + "Queue" + i + ": " + qq[i].info("", false)
								+ "\n");
		}
		return sb_.toString();
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return null;
		}
	}

	public Queue getQueue(int level_)
	{
		if (qq == null || level_ < 0 || qq.length <= level_)
			return null;
		else
			return qq[level_];
	}

	// make sure array has a spot at the level_
	void _assureLogics(int level_)
	{
		if (level_ < 0) throw new IndexOutOfBoundsException("negative index:" + level_);
		if (qlogics == null) qlogics = new QLogic[level_ + 1];
		if (qlogics.length > level_) return;

		// create a larger array
		QLogic[] tmp_ = new QLogic[level_ + 1];
		System.arraycopy(qlogics, 0, tmp_, 0, qlogics.length);
		qlogics = tmp_;
		if (qq.length <= level_) {
			VSFIFOQueue[] tmp2_ = new VSFIFOQueue[level_ + 1];
			System.arraycopy(qq, 0, tmp2_, 0, qq.length);
			for (int i=qq.length; i<=level_; i++)
				qq[i] = new VSFIFOQueue();
			qq = tmp2_;
		}
	}

	/**
	 * @param level_ level index of the queue logic.
	 */
	public void setQLogic(int level_, QLogic qlogic_)
	{
		_assureLogics(level_);
		qlogics[level_] = qlogic_;
	}

	public void setLevels(int nlevels_)
	{
		qq = new VSFIFOQueue[nlevels_];
		for (int i=0; i<qq.length; i++)
			qq[i] = new VSFIFOQueue();
	}

	public QLogic getQLogic(int level_)
	{ return qlogics[level_]; }
	
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
		
		if (currentSize + psize_ > capacity) {
			if (isGarbageEnabled()) drop(p_, "buffer overflow");
			if (qlogics != null && qlogics.length > level_
						&& qlogics[level_] != null)
				qlogics[level_].dropHandler(p_, psize_);
			return null;
		}

		if (qlogics != null && qlogics.length > level_
						&& qlogics[level_] != null) {
			String advice_ = qlogics[level_].adviceOn(p_, psize_);
		
			if (advice_ != null) {
				if (drop_front) {
					int total_ = 0;
					while (total_ < psize_) {
						Packet tmp_ = (Packet)qq[level_].dequeue();
						int tmpsize_ = isByteMode()? tmp_.size: 1;
						total_ += tmpsize_;
						qlogics[level_].dequeueHandler(tmp_, tmpsize_); //??
						if (isGarbageEnabled())
							drop(tmp_, "<DROP_FRONT>" + advice_);
					}
					currentSize += psize_ - total_;
					qlogics[level_].enqueueHandler(p_, psize_);
					qq[level_].enqueue(p_);
				}
				else {
					if (isGarbageEnabled()) drop(p_, advice_);
					qlogics[level_].dropHandler(p_, psize_);
				}
				return null;
			}
		}
		qq[level_].enqueue(p_);
		currentSize += psize_;
		return null;
	}
	
	/**
	 * Dequeues and returns the first object in the queue.
	 * @return the object dequeued; null if queue is empty.
	 */
	public Object dequeue()
	{
		for (int i=0; i<qq.length; i++) {
			VSFIFOQueue q = qq[i];
			if (q.isEmpty()) continue;
			Packet p_ = (Packet) q.dequeue();
			int psize_ = isByteMode()? p_.size: 1;
			if (qlogics != null && qlogics.length > i && qlogics[i] != null)
				qlogics[i].dequeueHandler(p_, psize_);
			currentSize -= psize_;
			return p_;
		}
		return null;
	}
	
	/**
	 * Retrieves but not remove the object at the position specified.
	 * @return the object; null if position is not valid.
	 */
	public Object peekAt(int pos_)
	{
		for (int i=0; i<qq.length; i++) {
			VSFIFOQueue q = qq[i];
			if (q.getSize() <= pos_) {
				pos_ -= q.getSize();
				continue;
			}
			else
				return q.retrieveAt(pos_);
		}
		return null;
	}
	
	/**
	 * Retrieves but not dequeue the first object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object firstElement()
	{
		for (int i=0; i<qq.length; i++) {
			VSFIFOQueue q = qq[i];
			if (q.isEmpty()) continue;
			return q.firstElement();
		}
		return null;
	}

	/**
	 * Retrieves but not remove the last object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object lastElement()
	{
		for (int i=qq.length-1; i>=0; i++) {
			VSFIFOQueue q = qq[i];
			if (q.isEmpty()) continue;
			return q.lastElement();
		}
		return null;
	}

	/** Return true if the queue is full. */
	public boolean isFull()
	{ return currentSize >= capacity; }
	
	/** Return true if the queue is empty.  */
	public boolean isEmpty()
	{
		for (int i=0; i<qq.length; i++) {
			VSFIFOQueue q = qq[i];
			if (!q.isEmpty()) return false;
		}
		return true;
	}
	
	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{ capacity = capacity_; }
	
	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return capacity; }
	
	/** Returns the current size of the queue.  */
	public int getSize()
	{ return currentSize; }
	
	public void setDropHeadEnabled(boolean enabled_)
	{	drop_front = enabled_;}	
	
	public boolean isDropHeadEnabled()
	{	return drop_front;}	
}
