// @(#)tsPoisson.java   7/2003
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

package drcl.net.traffic;

import drcl.util.random.ExponentialDistribution;
import drcl.net.FooPacket;

/**
 * This class implements an {@link traffic_Poisson On/Off traffic} source.
 * @see traffic_Poisson
 */
public class tsPoisson extends TrafficSourceComponent
{
	double nextTime;
	ExponentialDistribution ed = new ExponentialDistribution();
		
	traffic_Poisson traffic = null;

	public tsPoisson()
	{ this(new traffic_Poisson()); }

	public tsPoisson(String id_)
	{ super(id_); traffic = new traffic_Poisson(); reset(); }

	public tsPoisson(traffic_Poisson traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void setSeed(long seed_)
	{
		super.setSeed(seed_);
		ed.setSeed(seed_);
	}

	public void reset()
	{
		super.reset();
		nextTime = 0.0;
		if (traffic != null)
			ed.setRate(1.0 / traffic.getPeriod());
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
				+ prefix_ + "Arrival: " + ed + "\n"
				+ prefix_ + "State: nextTime=" + nextTime + "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_Poisson)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_) 
	{ 
		nextTime += ed.nextDouble();
		nextpkt_.setPacketSize(traffic.packetSize);
		return nextTime;
	}

	public void setPacketSize(int size_)
   	{ traffic.packetSize = size_; reset(); }
	public int getPacketSize() { return traffic.packetSize; }
	
	public void setRate(double brate_)
   	{ traffic.rate = brate_; reset(); }
	public double getRate() { return traffic.rate; }
	
	public void set(int mtu_, double brate_)
	{ traffic.set(mtu_, brate_); reset(); }
}
