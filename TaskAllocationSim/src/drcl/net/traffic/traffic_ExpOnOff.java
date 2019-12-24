// @(#)traffic_ExpOnOff.java   9/2002
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

/**
 * This class describes an On/Off traffic model.
 * <i>On</i> and <i>off</i> time intervals are exponentially distributed.
 * It defines the following parameters:
 * <dl>
 * <dt>On Time
 * <dd>Average length of <i>on</i> time (burst) intervals (second).
 *
 * <dt>Off Time
 * <dd>Average length of <i>off</i> time (idle) intervals (second).
 *
 * <dt>Burst Rate
 * <dd>Sending rate during <i>on</i> time (bps).
 *
 * <dt>Packet Size
 * <dd>Size of packets (byte).
 * </dl>
 */
public class traffic_ExpOnOff extends TrafficModel implements TrafficPeriodic
{
	public int packetSize;
	public double burstRate;  // sending rate during ON time [bps]
	public double aveOnTime;  // average length of ON time [sec]
	public double aveOffTime; // average length of OFF time [sec]
	
	public traffic_ExpOnOff()
	{}

	public traffic_ExpOnOff(int mtu_, double brate_, double ontime_,
				double offtime_)
	{ set(mtu_, brate_, ontime_, offtime_); }
	
	public void set(int mtu_, double brate_, double ontime_, double offtime_)
	{
		packetSize = mtu_;
		burstRate = brate_;
		aveOnTime = ontime_;
		aveOffTime = offtime_;		
	}
	
	public double getPeriod() 
	{ return (double)(packetSize << 3) /  burstRate; }
	
	public double getLoad() { return burstRate; }
	
	public int getBurst() { return packetSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_ExpOnOff)) return null;
		traffic_ExpOnOff thatTraffic_ = (traffic_ExpOnOff) that_;
		double load1_ = getLoad();
		double load2_ = thatTraffic_.getLoad();
		packetSize = Math.max(packetSize, thatTraffic_.packetSize);
		burstRate = Math.max(load1_, load2_);
		aveOnTime = Math.max(aveOnTime, thatTraffic_.aveOnTime);
		aveOffTime = Math.max(aveOffTime, thatTraffic_.aveOffTime);	
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_ExpOnOff)) return;
		traffic_ExpOnOff that_ = (traffic_ExpOnOff) source_;
		packetSize = that_.packetSize;
		burstRate = that_.burstRate;
		aveOnTime = that_.aveOnTime;
		aveOffTime = that_.aveOffTime;
	}
		
	public String oneline()
	{
		return  getClass().getName() + ":packetSize=" + packetSize
			+ ", burstRate=" + burstRate + ", aveOnTime=" + aveOnTime
			+ ", aveOffTime=" + aveOffTime;
	}
	
	//
	private void ___PROPERTY___() {}
	//	
	
	public void setPacketSize(int size_) { packetSize = size_; }
	public int getPacketSize() { return packetSize; }
	
	public void setBurstRate(double brate_) {burstRate = brate_; }
	public double getBurstRate() { return burstRate; }
	
	public void setAveOnTime(double ontime_) {aveOnTime = ontime_; }
	public double getAveOnTime() { return aveOnTime; }
	
	public void setAveOffTime(double offtime_) {aveOffTime = offtime_; }
	public double getAveOffTime() { return aveOffTime; }

	public int getMTU() { return packetSize; }
}
