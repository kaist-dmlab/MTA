// @(#)TrafficComponent.java   9/2002
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
import drcl.util.queue.FIFOQueue;
import drcl.comp.*;
import drcl.net.PacketWrapper;

/**
Defines the base class for generating/regulating/outputing packets
according to the associated {@link TrafficModel traffic model}.

<p>One may further encapsulate the generated/regulated packets in a packet wrapper
by installing a packet wrapper using {@link #setPacketWrapper(drcl.net.PacketWrapper)}.

@see TrafficModel
 */
public abstract class TrafficComponent extends Component
{
	PacketWrapper enclosingPacket;
	protected Port downPort = addPort("down", false);
	{ downPort.setType(Port.PortType_OUT); }
	protected Port timerPort = addForkPort(".timer");

	public TrafficComponent()
	{ super(); }

	public TrafficComponent(String id_)
	{ super(id_); }

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TrafficComponent that_ = (TrafficComponent) source_;
		if (getTrafficModel() == null) {
			if (that_.getTrafficModel() != null)
				setTrafficModel((TrafficModel)that_.getTrafficModel().clone());
		}
		else if (that_.getTrafficModel() != null)
			getTrafficModel().duplicate(that_.getTrafficModel());
		else
			setTrafficModel(null);

		if (that_.enclosingPacket != null)
			enclosingPacket = (PacketWrapper)that_.enclosingPacket.clone();
		else
			enclosingPacket = null;
	}

	public String info()
	{ return info(""); }

	public String info(String prefix_)
	{
		return prefix_ + "TrafficModel: "
			+ (getTrafficModel() == null? "<null>": getTrafficModel().oneline()) + "\n"
			+ (enclosingPacket == null? "": prefix_ + "EnclosingPkt: " + (Object)enclosingPacket + "\n");
	}

	/** Returns the associated traffic model. */
	public abstract TrafficModel getTrafficModel();

	/** Sets the associated traffic model. */
	public abstract void setTrafficModel(TrafficModel traffic_);
	
	/** Installs a packet wrapper to this traffic component. */
	public void setPacketWrapper(PacketWrapper pkt_)
	{ enclosingPacket = pkt_; }

	/** Returns the installed packet wrapper. */
	public PacketWrapper getPacketWrapper()
	{ return enclosingPacket; }
}
