// @(#)TrafficShaper.java   9/2002
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

package drcl.net.traffic;

import drcl.data.*;
import drcl.util.queue.VSFIFOQueue;
import drcl.comp.*;
import drcl.net.Packet;

/**
Defines the base class for regulating incoming packets and outputing the packets
according to the associated {@link TrafficModel traffic model}.

<p>A TrafficShaper holds a buffer that accommodates the difference of the traffic patterns
between the incoming traffic and the traffic model instance that is associated with
this component.

<p>To embed this class in a component (e.g., {@link TrafficShaperComponent}),
call {@link #adjust(drcl.net.Packet, double)} for each packet to be regulated.
The method returns the amount of time that must be delayed for outputting the packet in 
order to conform to the associated traffic model.  If the time is greater than zero, then the
packet is held in the buffer until {@link #dequeue()} is called to release the packet.
One can use {@link #nextOutputTime()} to get the absolute time 
when the next packet in the buffer can be released.

<p>Subclasses must override {@link #adjust(double, int)}.
Subclasses do not need to be concerned about the buffer as it is taken care of in
{@link #adjust(drcl.net.Packet, double)}.  The current time passed to this method is
maintained relatively to the time when this shaper instance starts.  A subclass also
needs to override {@link #duplicate(Object)}, {@link #info(String)} and {@link #reset()}.

@see TrafficModel
 */
public abstract class TrafficShaper extends drcl.DrclObj
{
	VSFIFOQueue qBuffer = new VSFIFOQueue();
	int maxsize = Integer.MAX_VALUE;
	double startTime = Double.NaN;
	
	/**
	Returns the time adjustment (relative to the current time <code>now_</code>)
	for outputing the packet.
	This is the main method a subclass must override to regulate the incoming packets.
	The current time passed to this method is maintained relatively to the time
	when this component starts.
	@param now_ current time, relative to the start time of the shaper.
	@param size_ the packet size.
	 */
	protected abstract double adjust(double now_, int size_);

	public void reset()
	{
		qBuffer.reset();
		startTime = Double.NEGATIVE_INFINITY;
	}
	
	public void duplicate(Object source_)
	{
		TrafficShaper that_ = (TrafficShaper) source_;
		maxsize = that_.maxsize;

		if (getTrafficModel() == null) {
			if (that_.getTrafficModel() != null)
				setTrafficModel((TrafficModel)that_.getTrafficModel().clone());
		}
		else if (that_.getTrafficModel() != null)
			getTrafficModel().duplicate(that_.getTrafficModel());
		else
			setTrafficModel(null);
	}

	/** Prints out the content of this traffic shaper instance.
	This class prints out the associated traffic model and the buffer.
	A subclass only needs to call <code>super.info(prefix_)</code> and
	then supply the content of the parameters defined in the subclass.
	@param prefix_ prefix_ that should be prepended at each line.
	*/
	public String info(String prefix_)
	{
		return "TrafficModel: "
			+ (getTrafficModel() == null? "<null>":
							getTrafficModel().oneline()) + "\n"
			+ prefix_ + "Buffer: capacity=" + maxsize + ", " + qBuffer.info();
	}
	
	/**
	Returns the time adjustment (relative to the current time <code>now_</code>)
	for outputing the packet.
	@return a negative value if the caller should drop the packet.
	 */
	public final synchronized double adjust(Packet p_, double now_)
	{
		// initialize startTime
		if (startTime == Double.NEGATIVE_INFINITY)
			startTime = now_;
			
		int size_ = p_.getPacketSize();
		if (size_ > getTrafficModel().getMTU()) return -1.0;
		if (qBuffer.getSize() + size_ <= maxsize) {
			double deltaT_ = adjust(now_ - startTime, size_);

			if (deltaT_ <= 0.0)
				return 0.0;
			// enqueue with key = time to output the packet
			qBuffer.enqueue(now_ + deltaT_, p_, size_);
			return deltaT_;
		}
		else
			return -1.0;
	}

	/** Releases and returns the first packet being held in the buffer.*/
	public synchronized Packet dequeue()
	{ return (Packet)qBuffer.dequeue(); }
		
	/** Returns the associated traffic model. */
	public abstract TrafficModel getTrafficModel();

	/** Sets the associated traffic model. */
	public abstract void setTrafficModel(TrafficModel traffic_);

	/** Sets the (maximum) buffer size of this traffic shaper (byte). */
	public void setBufferSize(int size_)
	{ maxsize = size_; }

	/** Returns the (maximum) buffer size of this traffic shaper (byte). */
	public int getBufferSize()
	{ return maxsize; }

	/** Returns the current buffer length of this traffic shaper (# of packets). */
	public int getBufferLength()
	{ return qBuffer.getLength(); }

	/** Returns the output time of next packet. */
	public double nextOutputTime()
	{ return qBuffer.firstKey(); }

	/** Returns the available buffer size of this traffic shaper (byte). */
	public int getAvailableBufferSize()
	{ return maxsize - qBuffer.getSize(); }
}
