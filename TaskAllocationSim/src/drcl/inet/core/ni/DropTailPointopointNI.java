// @(#)DropTailPointopointNI.java   8/2003
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

package drcl.inet.core.ni;

import drcl.data.*;
import drcl.comp.Port;
import drcl.net.*;
import drcl.util.queue.VSFIFOQueue;

/**
The class implements the point-to-point network interface and emulates the physical
link propagation.
It is fully specified by the bandwidth, MTU and propagation delay.  
As a network interface, only one packet can be transmitted at a time.
However as an emulated link, multiple packets may be outstanding, the number of which
dependes on the propagation delay, bandwidth and packet size.
 */
public class DropTailPointopointNI extends drcl.inet.core.QueueNI
{
	{ downPort.setType(Port.PortType_OUT); }
	{ pullPort.setType(Port.PortType_IN); }
	
	/** Time ready to transmit next packet. */
	protected int ready = 1;
	
	public static final String EVENT_QLEN = "Instant Q Length";
	protected Port qLenPort = addEventPort(".q"); 

	protected VSFIFOQueue q = null;
	protected int capacity = DEFAULT_BUFFER_SIZE; // default in bytes
	int maxLength = 0; // stats

	public DropTailPointopointNI()
	{	super();	}
	
	public DropTailPointopointNI(String id_)
	{	super(id_);	}
	
	protected synchronized void process(Object data_, Port inPort_)
	{
		if (data_ == null) return;

		if (data_ == this) {
			data_ = dequeue();
			if (data_ == null) {
				ready++;
				return;
			}
		}
		else if (ready == 0) {
			enqueue((Packet)data_);
			return;
		}

		Packet pkt_ = (Packet)data_;
		if (pkt_.size > mtu) {
			if (isGarbageEnabled()) drop(data_, "pkt size > mtu(" + mtu + ")");
			return;
		}
	
		ready = 0;
		transmit(pkt_);
	}
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		DropTailPointopointNI that_ = (DropTailPointopointNI)source_;
		capacity = that_.capacity;
	}
	
	
	public void reset()
	{
		super.reset();
		ready = 1;
		if (q != null) q.reset();
		maxLength = 0;
	}
	
	/** Returns true if the interface is ready to transmit more packets. */
	public boolean isReady() 
	{ return ready > 0; }

	public String info()
	{
		return super.info()
			+ (q == null? "": "Content:" + q.info("   "));
	}

	/**
	 * Enqueues the object at the end of the queue
	 * @return the object being dropped due to the enqueue; null otherwise.
	 */
	Packet enqueue(Packet p_)
	{
		int psize_ = byteMode? p_.size: 1;
		
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
			q.enqueue(p_, psize_);
			//if (isDebugEnabled())
			//	debug("qsize=" + q.getSize() + ", enqueue " + p_);
			if (qLenPort._isEventExportEnabled())
				qLenPort.exportEvent(EVENT_QLEN, (double)q.getSize(), null);
			if (maxLength < q.getLength()) maxLength++;
		}
		return null;
	}

	/**
	 * Dequeues and returns the first object in the queue.
	 * @return the object dequeued; null if queue is empty.
	 */
	Packet dequeue()
	{
		if (q == null || q.isEmpty()) return null;
		Packet p_ = (Packet) q.dequeue();
		if (qLenPort._isEventExportEnabled())
			qLenPort.exportEvent(EVENT_QLEN, (double)q.getSize(), null);
		return p_;
	}

	/** Sets the capacity of the queue. */
	public void setCapacity(int capacity_)
	{ capacity = capacity_; }
	
	/** Returns the capacity of the queue. */
	public int getCapacity()
	{ return capacity; }
	
	/** Returns the current size of the queue. */
	public int getSize()
	{ return q == null? 0: q.getSize(); }

	public int getMaxLength()
	{ return maxLength; }
}
