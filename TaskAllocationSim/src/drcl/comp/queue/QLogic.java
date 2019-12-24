// @(#)QLogic.java   9/2002
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

package drcl.comp.queue;

import drcl.comp.Component;
import drcl.comp.Port;
import drcl.data.DoubleObj;

/**
This class is the base class for queue logic.
The purpose of this class is to separate the logic of deciding whether or
not to enqueue an object, from the actual actions of enqueuing/discarding/dequeuing
an object.  It makes it possible to construct a complex queue structure where one
can plug in different logic to form a specific queue implementation.

<p>This class defines the interfaces of that decision logic.
Specifically, a subclass must implement {@link #adviceOn(Object, int) adviceOn(data_, size_)}.
The method returns <code>null</code> if it is ok to enqueue the data of the specified size.
It returns a non-null string (advice) if it advices to discard the data.
The caller may or may not take the advice.  But regardless of which action to take, the caller
must call the corresponding event handlers ({@link #dropHandler(Object, int) dropHandler()},
{@link #enqueueHandler(Object, int) enqueueHandler()} and
{@link #dequeueHandler(Object, int) dequeueHandler()}) of this queue logic so that this queue
logic can maintain the appropriate picture of what the real queue (the caller maintains) is like
in order to give correct advices later on.

<p>This class implements a simple limited-capacity queue logic.</p>
 */
public class QLogic extends drcl.DrclObj
{
	/** Name of the instant queue size change event. */
	public static final String EVENT_QSIZE = "Instant Q Size";

	/** Default advice for replying in {@link #adviceOn(Object, int)} when the host's
	  * garbage flag is not on (for better performance). */
	protected static final String DEFAULT_ADVICE = "just drop it!"; 

	/** Port that exports the instant queue size change events. */
	protected Port qSizePort;

	/** The host component who owns this object. */
	protected Component host;

	/** The capacity of the queue. */
	public int capacity;

	/** The current queue size (each object in the queue may have different size). */
	public int qsize;

	/** The current queue length (# of objects in the queue). */
	public int qlen;

	public QLogic()
	{}

	public QLogic(Component host_)
	{ this(); host = host_; }

	public QLogic(Component host_, String qpid_)
	{ this(); host = host_; setQSizePort(qpid_); }

	/**
	 * Resets this object to the initial state.
	 * Subclasses must call <code>super.reset()</code> when
	 * overriding this method.
	 */
	public void reset()
	{ qlen = qsize = 0; }

	/**
	 * Duplicates the content of the source object to this object.
	 * If the host component is already set, this method also creates
	 * the <i>queue size change event</i> port of the same ID as that in
	 * <code>source_</code>.
	 * Subclasses must call <code>super.duplicate()</code> when
	 * overriding this method.
	 */
	public void duplicate(Object source_)
	{
		QLogic that_ = (QLogic)source_;
		capacity = that_.capacity;
		if (host != null && that_.qSizePort != null)
			setQSizePort(that_.qSizePort.getID());
	}

	/**
	 * Prints the content (states) of this queue management instance.
	 * It is equivalent to calling <code>info(null)</code>.
	 */
	public String info()
	{ return info(""); }

	/**
	 * Prints the content (states) of this queue management instance.
	 * @param prefix_ prefix of each line in the printout.
	 */
	public String info(String prefix_)
	{
		return prefix_ + drcl.util.StringUtil.finalPortionClassName(getClass())
			+ ", occupancy:" + qsize + "/" + capacity + ", Q length:" + qlen + "\n";
	}

	/**
	 * Advices the host component for deciding whether or not
	 * to drop the (arriving) object.
	 *
	 * @return the advice; null if advicing not to drop the object.
	 */
	public String adviceOn(Object obj_, int size_)
	{
		if (size_ + qsize > capacity) {
			if (!host.isGarbageEnabled())
				return DEFAULT_ADVICE; // just tell the host to drop it
			else if (host.isDebugEnabled())
				return "exceeds capacity:" + qsize + "+" + size_ + ">" + capacity;
			else
				return "exceeds capacity";
		}
		else return null;
	}

	/** Handles the event of dropping the object of the given size.  */
	public void dropHandler(Object obj_, int size_)
	{}

	/** Handles the event of enqueuing the object of the given size.  */
	public void enqueueHandler(Object obj_, int size_)
	{
		qsize += size_;
		qlen++;
		if (qSizePort != null && qSizePort._isEventExportEnabled())
			qSizePort.exportEvent(EVENT_QSIZE, new DoubleObj(qsize), null);
	}

	/** Handles the event of dequeuing the object of the given size.  */
	public void dequeueHandler(Object obj_, int size_)
	{
		qsize -= size_;
		qlen--;
		if (qSizePort != null && qSizePort._isEventExportEnabled())
			qSizePort.exportEvent(EVENT_QSIZE, new DoubleObj(qsize), null);
	}

	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{ capacity = capacity_; }

	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return capacity; }

	/** Returns the current size of the queue. */
	public int getCurrentQSize()
	{ return qsize; }

	/** Returns the current length of the queue. */
	public int getCurrentQLength()
	{ return qlen; }

	/** Returns true if the queue is full. */
	public boolean isFull()
	{ return qsize == capacity; }

	/** Returns true if the queue is empty. */
	public boolean isEmpty()
	{ return qlen == 0; }

	public void setHost(Component host_)
	{ host = host_; }

	public Component getHost()
	{ return host; }

	/**
	 * @param pid_ ID of the port.
	 */
	public void setQSizePort(String pid_)
	{ qSizePort = host.addEventPort(pid_); }

	/**
	 * @param pid_ ID of the port.
	 */
	public void set(Component host_, String pid_)
	{ setHost(host_);  setQSizePort(pid_); }
}
