// @(#)tsPacketTrain.java   9/2002
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

import drcl.util.random.RandomNumberGenerator;
import drcl.net.FooPacket;

/**
 * This class implements a traffic shaper that conforms to the {@link traffic_PacketTrain
 * packet train} traffic model.
 * @see traffic_PacketTrain
 */
public class tsPacketTrain extends TrafficSourceComponent
{
	double nextTime;
	traffic_PacketTrain traffic = null;

	public tsPacketTrain()
	{ this(new traffic_PacketTrain()); }

	public tsPacketTrain(String id_)
	{ super(id_); traffic = new traffic_PacketTrain(); reset(); }

	public tsPacketTrain(traffic_PacketTrain traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void reset()
	{
		super.reset();
		nextTime   = 0.0;
	}
	
	public String info(String prefix_)
	{ return super.info(prefix_) + "State: nextTime=" + nextTime + "\n"; }

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_PacketTrain)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_) 
	{ 
		double time_ = nextTime;
		nextTime += traffic.interArrivalTime;
		nextpkt_.setPacketSize(traffic.packetSize);
		return time_;
	}

	public void setPacketSize(int size_)
	{ traffic.setPacketSize(size_); }

	public int getPacketSize()
	{ return traffic.getPacketSize(); }
	
	public void setIntArrivalTime(double t_)
	{ traffic.setIntArrivalTime(t_); }

	public double getIntArrivalTime()
	{ return traffic.getIntArrivalTime(); }

	public void set(int mtu_, double iat_)
	{ traffic.set(mtu_, iat_); reset(); }
}
