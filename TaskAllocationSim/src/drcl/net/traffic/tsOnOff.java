// @(#)tsOnOff.java   5/2003
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
 * This class implements an {@link traffic_OnOff On/Off traffic} source.
 * @see traffic_OnOff
 */
public class tsOnOff extends TrafficSourceComponent
{
	double nextTime;
	int rem; // number of remaining packets in current burst
		
	// interArrivalTime: packet inter-arrival time during ON time (burst)
	// burstLength:      average number of packets in a burst
	double interArrivalTime;
	double burstLength;
	
	traffic_OnOff traffic = null;

	public tsOnOff()
	{ this(new traffic_OnOff()); }

	public tsOnOff(String id_)
	{ super(id_); traffic = new traffic_OnOff(); reset(); }

	public tsOnOff(traffic_OnOff traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void reset()
	{
		super.reset();
		rem = 0;
		
		if (traffic != null) {
			interArrivalTime = (double)(traffic.packetSize << 3)
				/ traffic.rate * (traffic.OnTime / traffic.getPeriod());
			nextTime = -interArrivalTime;
				// so the 1st pkt is created at time 0.0
			burstLength = traffic.OnTime / interArrivalTime;
			rem = (int)burstLength;
			if (rem == 0) rem = 1;
		}
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_) + prefix_
				+ "State: nextTime=" + nextTime
				+ ", interArrivalTime(on)=" + interArrivalTime
				+ ", burstLength=" + burstLength
				+ ", remaining_in_this_on=" + rem
			+ "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_OnOff)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_) 
	{ 
		double delta_ = interArrivalTime;
		
		if (rem == 0) { 
			rem = (int)burstLength;
			if (rem == 0) rem = 1; // to make sure we got at least one
			delta_ += traffic.OffTime;
		}
		rem--;
		
		nextTime += delta_;
		nextpkt_.setPacketSize(traffic.packetSize);
		return nextTime;
	}

	public void setPacketSize(int size_) { traffic.packetSize = size_; }
	public int getPacketSize() { return traffic.packetSize; }
	
	public void setAvgRate(int rate_) {traffic.rate = rate_; }
	public int getAvgRate() { return traffic.rate; }
	
	public void setOnTime(double ontime_) {traffic.OnTime = ontime_; }
	public double getOnTime() { return traffic.OnTime; }
	
	public void setOffTime(double offtime_) {traffic.OffTime = offtime_; }
	public double getOffTime() { return traffic.OffTime; }

	public void set(int mtu_, int rate_, double ontime_, double offtime_)
	{ traffic.set(mtu_, rate_, ontime_, offtime_); reset(); }
}
