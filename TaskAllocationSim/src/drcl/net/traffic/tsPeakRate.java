// @(#)tsPeakRate.java   7/2003
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

import java.util.Random;
import drcl.util.random.RandomNumberGenerator;
import drcl.net.FooPacket;

/**
 * This class implements a traffic source that conforms to the 
 * {@link traffic_PeakRate} peak rate
 * traffic model.
 * @see traffic_PeakRate
 */
public class tsPeakRate extends TrafficSourceComponent
{
	double nextTime;
	traffic_PeakRate traffic = null;
	Random randomSize = new Random();
	Random randomInterval = new Random();

	public tsPeakRate()
	{ this(new traffic_PeakRate()); }

	public tsPeakRate(String id_)
	{ super(id_); traffic = new traffic_PeakRate(); reset(); }

	public tsPeakRate(traffic_PeakRate traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void setSeed(long seed_)
	{
		super.setSeed(seed_);
		Random r = new Random(seed_);
		randomSize.setSeed(r.nextLong());
		randomInterval.setSeed(r.nextLong());
	}

	public void reset()
	{
		super.reset();
		nextTime   = 0.0;
	}

	public String info(String prefix_)
	{
		return super.info(prefix_) + prefix_ + "State: nextTime=" + nextTime
			+ "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_PeakRate)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_)
	{ 
		int size_ = (int)((double)(traffic.maxPktSize - traffic.minPktSize)
					* randomSize.nextDouble()) + traffic.minPktSize;
		if (size_ < 1) size_ = 1;

		double delta_ = (traffic.maxIntArrivalTime - traffic.minIntArrivalTime)
					* randomInterval.nextDouble() + traffic.minIntArrivalTime;
		//if (delta_ > .001)// XXX: temporary, for readability
		//	nextTime += (double)((long)delta_);
		double time_ = nextTime;
		nextTime += delta_;

		nextpkt_.setPacketSize(size_);
		return time_;
	}

	public void setMaxPktSize(int size_) { traffic.maxPktSize = size_; }
	public int getMaxPktSize() { return traffic.maxPktSize; }
	
	public void setMinPktSize(int size_) { traffic.minPktSize = size_; }
	public int getMinPktSize() { return traffic.minPktSize; }
	
	public void setMaxIntArrivalTime(double time_)
	{traffic.maxIntArrivalTime = time_; }
	public double getMaxIntArrivalTime() { return traffic.maxIntArrivalTime; }
	
	public void setMinIntArrivalTime(double time_)
	{ traffic.minIntArrivalTime = time_; }
	public double getMinIntArrivalTime() { return traffic.minIntArrivalTime; }

	public void set(int min_, int max_, double miniat_, double maxiat_)
	{ traffic.set(min_, max_, miniat_, maxiat_); reset(); }
}
