// @(#)TrafficShaperComponent.java   9/2002
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

import drcl.comp.*;
import drcl.net.Packet;
import drcl.net.PacketWrapper;

/**
This component is the base class for regulating incoming packets and outputing the packets
according to the associated {@link TrafficModel traffic model}.

<p>A TrafficShaperComponent expects input data in {@link drcl.net.Packet} and outputs either the incoming
packet or an enclosing packet that encapsulates the incoming packet.

<p>A TrafficShaperComponent holds a {@link TrafficShaper} object that does the real job.
One may plug in a different {@link TrafficShaper} object to create a different traffic shaper component.

@see TrafficShaper
 */
public class TrafficShaperComponent extends TrafficComponent
{
	// add an "up" port
	Port upPort = addPort("up", false);
	{ upPort.setType(Port.PortType_IN); }

	TrafficShaper shaper;
	
	public TrafficShaperComponent()
	{}

	public TrafficShaperComponent(TrafficShaper shaper_)
	{ shaper = shaper_; }

	public TrafficShaperComponent(String id_)
	{ super(id_); }

	public void reset()
	{
		super.reset();
		shaper.reset();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TrafficShaperComponent that_ = (TrafficShaperComponent) source_;
		if (that_.shaper == null) shaper = null;
		else
			shaper = (TrafficShaper)that_.shaper.clone();
	}
	
	public String info(String prefix_)
	{
		return prefix_ + shaper.info(prefix_)
			+ (enclosingPacket == null? "": prefix_ + "EnclosingPkt: " + (Object)enclosingPacket + "\n");
	}
	
	protected synchronized void process(Object data_, drcl.comp.Port inPort_)
	{
		if (inPort_ == upPort) {
			if (getTrafficModel() == null) { downPort.doLastSending(data_); return; }
			if (!(data_ instanceof Packet)) {
				error(data_, "process()", inPort_, "unrecognized data");
				return;
			}
			
			double deltaT_ = shaper.adjust((Packet)data_, getTime());

			if (isDebugEnabled())
				debug("Size " + ((Packet)data_).size + " will be ready at " + (getTime() + deltaT_));

			if (deltaT_ < 0.0) {
				if (isGarbageEnabled()) {
					if (((Packet)data_).size > getTrafficModel().getMTU()) {
						drop(data_, "packet is too large to enter the shaper, "
							+ ((Packet)data_).size + " > MTU (" + getTrafficModel().getMTU() + ")");
					}
					else
						drop(data_, "available_buffer=" + shaper.getAvailableBufferSize());
				}
				return;
			}
			else if (deltaT_ > 0.0) {
				if (shaper.getBufferLength() == 1)
					fork(timerPort, shaper, deltaT_);
				return;
			}
		}
		else if (inPort_ == timerPort) {
			data_ = shaper.dequeue();
			if (shaper.getBufferLength() > 0)
				forkAt(timerPort, shaper, shaper.nextOutputTime());
		}
		else {
			error(data_, "process()", inPort_, "unknown data");
			return;
		}

		// send data_
		if (enclosingPacket == null)
			downPort.doLastSending(data_);
		else {
			PacketWrapper tmp_ = (PacketWrapper)enclosingPacket.clone();
			tmp_.wraps((Packet)data_);
			downPort.doLastSending((Object)tmp_);
		}
	}

	/** Installs the {@link TrafficShaper} instance. */
	public void setShaper(TrafficShaper shaper_)
	{ shaper = shaper_; }

	/** Returns the associated {@link TrafficShaper} instance. */
	public TrafficShaper getShaper()
	{ return shaper; }

	public TrafficModel getTrafficModel()
	{ return shaper.getTrafficModel(); }

	public void setTrafficModel(TrafficModel traffic_)
	{ shaper.setTrafficModel(traffic_); }

	/** Sets the (maximum) buffer size of this traffic shaper (byte). */
	public void setBufferSize(int size_)
	{ shaper.setBufferSize(size_); }

	/** Returns the (maximum) buffer size of this traffic shaper (byte). */
	public int getBufferSize()
	{ return shaper.getBufferSize(); }
}
