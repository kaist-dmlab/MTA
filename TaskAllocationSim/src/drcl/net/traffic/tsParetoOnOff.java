// @(#)tsParetoOnOff.java   7/2003
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
import drcl.util.random.ParetoDistribution;
import drcl.net.FooPacket;

/**
 * This class implements a {@link traffic_ParetoOnOff Pareto On/Off} traffic source.
 * @see traffic_ParetoOnOff
 */
public class tsParetoOnOff extends TrafficSourceComponent
{
	double nextTime;
	double rem; // remaining "on" time in the current on period
	// interArrivalTime: packet inter-arrival time during ON time (burst)
	double interArrivalTime;
	ParetoDistribution pd1 = new ParetoDistribution();
	ParetoDistribution pd2 = new ParetoDistribution();
	traffic_ParetoOnOff traffic = null;

	public tsParetoOnOff()
	{ this(new traffic_ParetoOnOff()); }
	
	public tsParetoOnOff(String id_)
	{ super(id_); traffic = new traffic_ParetoOnOff(); reset(); }
	
	public tsParetoOnOff(traffic_ParetoOnOff traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void setSeed(long seed_)
	{
		super.setSeed(seed_);
		Random r = new Random(seed);
		pd1.setSeed(r.nextLong());
		pd2.setSeed(r.nextLong());
	}
	
	public void reset()
	{
		super.reset();
		nextTime   = 0.0;
		rem = 0;
		if (traffic != null) {
			interArrivalTime = (double)(traffic.packetSize << 3)
				/  traffic.burstRate;
		
			double p1_ = traffic.aveOnTime * (traffic.shapeParaOn - 1.0)
				/traffic.shapeParaOn;
			double p2_ = traffic.aveOffTime * (traffic.shapeParaOff - 1.0)
				/traffic.shapeParaOff;

			pd1.setScale(p1_);
			pd1.setShape(traffic.shapeParaOn);
			pd2.setScale(p2_);
			pd2.setShape(traffic.shapeParaOff);		
		}
	}
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
				+ " On Distr.= " + pd1 + "\n"
				+ "Off Distr.= " + pd2 + "\n"
				+ "State: nextTime=" + nextTime
				+ ", interArrivalTime(on)=" + interArrivalTime
				+ ", remaining_in_this_on=" + rem
				+ "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_ParetoOnOff)traffic_; reset(); }

	protected double setNextPacket(FooPacket nextpkt_)
	{ 
		double delta_ = interArrivalTime;
		
		// off period
		if (rem < interArrivalTime) {
			// remain in off when there's not enough "on" time for a pkt
			while (rem < interArrivalTime) {
				rem += pd1.nextDouble();
				delta_ += pd2.nextDouble();
			}
		}
		rem -= interArrivalTime;

		nextTime += delta_;
		
		nextpkt_.setPacketSize(traffic.packetSize);
		return nextTime;
	}

	public void setPacketSize(int size_) { traffic.packetSize = size_; }
	public int getPacketSize() { return traffic.packetSize; }
	
	public void setBurstRate(double brate_) {traffic.burstRate = brate_; }
	public double getBurstRate() { return traffic.burstRate; }
	
	public void setAveOnTime(double ontime_) {traffic.aveOnTime = ontime_; }
	public double getAveOnTime() { return traffic.aveOnTime; }
	
	public void setAveOffTime(double offtime_) {traffic.aveOffTime = offtime_; }
	public double getAveOffTime() { return traffic.aveOffTime; }

	public void setShapeParaOn(double shape_) {traffic.shapeParaOn = shape_; }
	public double getShapeParaOn() { return traffic.shapeParaOn; }

	public void setShapeParaOff(double shape_) {traffic.shapeParaOff = shape_; }
	public double getShapeParaOff() { return traffic.shapeParaOff; }

	public void set(int mtu_, double brate_, double ontime_,
			double offtime_, double shapeOn_, double shapeOff_)
	{
		traffic.set(mtu_, brate_, ontime_, offtime_, shapeOn_, shapeOff_); 
		reset();
   	}
}
