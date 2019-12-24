// @(#)ActiveQueue.java   7/2003
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

package drcl.comp.queue;

import drcl.comp.*;
import drcl.data.*;

/**
An <code>ActiveQueue</code> is a queue and it is designed to interact with
a <em>data pulling</em> component.

It accepts a <code>null</code> signal from the data pulling component 
which triggers a dequeue and the dequeued data is sent at the
<code>output@</code> port.  If the queue is empty, this component remembers
that the pulling component is available for receiving data, and when new data
arrives, this component <em>actively</em> sends out the data at the
<code>output@</code> port without the pulling component having to do
the pulling again.
Multiple pullings when the queue is empty results in only <em>one time</em> of
active sending.
 */
public abstract class ActiveQueue extends Component
{
	public static final String OUTPUT_PORT_ID = 
			ActiveQueueContract.OUTPUT_PORT_ID;
	static {
		setContract(ActiveQueue.class, "*", 
						new ActiveQueueContract(Contract.Role_REACTOR));
	}
	
	protected boolean requesting = false; // from the pulling component
	private long enqueCounter = 0;
	
	protected Port outport = addPort(OUTPUT_PORT_ID, false/*not removable*/);
	
	public ActiveQueue()
	{ super(); }
	
	public ActiveQueue(String id_)
	{ super(id_); }

	public void reset()
	{
		super.reset();
		enqueCounter = 0;
		requesting = false;
	}

	public String info()
	{ return info(""); }

	public String info(String prefix_)
	{
		return prefix_ + "Enque count: " + enqueCounter
			+ ", buffering: " + getSize() + "/" + getCapacity()
			+ ", pending pulling req=" + requesting + "\n";
	}

	protected synchronized void process(Object data_, Port inPort_) 
	{
		if (data_ == null) { // dequeue signaling from the pulling component
			if (!isEmpty())
				outport.doLastSending(pull());
			else
				requesting = true;
		}
		else if (!(data_ instanceof String)) { // enqueue
			enqueCounter++;
			if (requesting) {
				outport.doLastSending(data_);
				requesting = false;
			}
			else {
				Object dropped_ = enqueue(data_);
				if (dropped_ != null && isGarbageEnabled())
					drop(dropped_, "due to queue capacity/policy");
			}
			/*
			Object dropped_ = enqueue(data_);
			if (dropped_ != null && isGarbageEnabled())
				drop(dropped_, "due to queue capacity");
			if (requesting) {
				data_ = pull(); // 'requesting' off in pull()
				if (data_ != null) outport.doLastSending(data_);
			}
			*/
		}
		else if (data_ instanceof IntObj) {
			setCapacity(((IntObj)data_).value);
		}
		else if (data_ == ActiveQueueContract.DEQUEUE) {
			inPort_.doLastSending(dequeue());
		}
		else if (data_ == ActiveQueueContract.PEEK) {
			inPort_.doLastSending(peekAt(0));
		}
		else if (data_ == ActiveQueueContract.IS_FULL) {
			inPort_.doLastSending(new BooleanObj(isFull()));
		}
		else if (data_ == ActiveQueueContract.IS_EMPTY) {
			inPort_.doLastSending(new BooleanObj(isEmpty()));
		}
		else if (data_ == ActiveQueueContract.GET_CAPACITY) {
			inPort_.doLastSending(new IntObj(getCapacity()));
		}
		else if (data_ == ActiveQueueContract.GET_SIZE) {
			inPort_.doLastSending(new IntObj(getSize()));
		}
	}

	protected final long getEnqueCount()
	{ return enqueCounter; }

	/** Increases the enqueue counter, for diagnosis purpose.  */
	public final void increaseEnqueCount()
	{ enqueCounter++; }
	
	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public abstract Object enqueue(Object obj_);
	
	/**
	 * Enqueues the object at the position specified.
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	public Object enqueueAt(Object obj_, int pos_)
	{ drcl.Debug.error("N/A"); return null; }
	
	/**
	 * Dequeues and returns the first object in the queue.
	 * @return the object dequeued; null if position is not valid.
	 */
	public abstract Object dequeue();
	
	/**
	 * Dequeues the object at the position specified.
	 * @return the object dequeued; null if position is not valid.
	 */
	public Object retrieveAt(int pos_)
	{ drcl.Debug.error("N/A"); return null; }
	
	/**
	 * Retrieves but not dequeue the object at the position specified.
	 * @return the object; null if position is not valid.
	 */
	public Object peekAt(int pos_)
	{ drcl.Debug.error("N/A"); return null; }
	
	/**
	 * Retrieves but not dequeue the first object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object firstElement()
	{ drcl.Debug.error("N/A"); return null; }
	
	/**
	 * Retrieves but not remove the last object in the queue.
	 * @return the object; null if queue is empty.
	 */
	public Object lastElement()
	{ drcl.Debug.error("N/A"); return null; }
	
	/** Return true if the queue is full.  */
	public abstract boolean isFull();
	
	/** Return true if the queue is empty.  */
	public abstract boolean isEmpty();
	
	/** Sets the capacity of the queue.  */
	public abstract void setCapacity(int capacity_);
	
	/** Returns the capacity of the queue. */
	public abstract int getCapacity();

	/** Returns the current size of the queue. */
	public abstract int getSize();

	/** Returns the available size of the queue.  */
	public int getAvailableSize()
	{ return getCapacity() - getSize(); }
	
	protected final void _setRequesting(boolean requesting_)
	{ requesting = requesting_; }
	
	protected final boolean _isRequesting()
	{ return requesting; }
	
	/** Returns the first available data in the queue. */
	protected final Object pull()
	{
		if (!isEmpty()) requesting = false; // request served
		return dequeue();
	}
}
