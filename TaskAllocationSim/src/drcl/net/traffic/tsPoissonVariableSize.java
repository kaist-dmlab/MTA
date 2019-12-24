// @(#)tsPoissonVariableSize.java   7/2003
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
 * This class implements an {@link traffic_PoissonVariableSize On/Off traffic} 
 * source.
 * @see traffic_PoissonVariableSize
 */
public class tsPoissonVariableSize extends TrafficSourceComponent
{
	double nextTime;
	ExponentialDistribution arrival = new ExponentialDistribution();
	ExponentialDistribution pktSize = new ExponentialDistribution();
		
	traffic_PoissonVariableSize traffic = null;

	public tsPoissonVariableSize()
	{ this(new traffic_PoissonVariableSize()); }

	public tsPoissonVariableSize(String id_)
	{ super(id_); traffic = new traffic_PoissonVariableSize(); reset(); }

	public tsPoissonVariableSize(traffic_PoissonVariableSize traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void setSeed(long seed_)
	{
		super.setSeed(seed_);
		java.util.Random r = new java.util.Random(seed_);
		arrival.setSeed(r.nextLong());
		pktSize.setSeed(r.nextLong());
	}

	public void reset()
	{
		super.reset();
		nextTime = 0.0;
		if (traffic != null) {
			arrival.setRate(1.0 / traffic.getPeriod());
			pktSize.setRate(1.0 / traffic.packetSize);
		}
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
				+ prefix_ + "Arrival: " + arrival + "\n"
				+ prefix_ + "PktSize: " + pktSize + "\n"
				+ prefix_ + "State: nextTime=" + nextTime + "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_PoissonVariableSize)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_) 
	{ 
		nextTime += arrival.nextDouble();
		int pktsize_ = pktSize.nextInt();
		// XX:
		while (pktsize_ <= 0)
			pktsize_ = pktSize.nextInt();
		nextpkt_.setPacketSize(pktsize_);
		return nextTime;
	}

	public void setAvgPacketSize(int size_)
   	{ traffic.packetSize = size_; reset(); }
	public int getAvgPacketSize() { return traffic.packetSize; }
	
	public void setRate(double brate_)
   	{ traffic.rate = brate_; reset(); }
	public double getRate() { return traffic.rate; }
	
	public void set(int avgPktSize_, double brate_)
	{ traffic.set(avgPktSize_, brate_); reset(); }
}
