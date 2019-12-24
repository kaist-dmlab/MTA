// @(#)traffic_OnOff.java   9/2002
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
 * <dd>Fixed length of <i>on</i> time (burst) intervals (second).
 *
 * <dt>Off Time
 * <dd>Fixed length of <i>off</i> time (idle) intervals (second).
 *
 * <dt>Average Sending Rate
 * <dd>Sending rate over the period of the <i>on</i> time plus <i>off</i> time (bps).
 *
 * <dt>Packet Size
 * <dd>Size of packets (byte).
 * </dl>
 */
public class traffic_OnOff extends TrafficModel implements TrafficPeriodic
{
	public int packetSize;
	public int rate;       // avg sending rate
	public double OnTime;  // average length of ON time [sec]
	public double OffTime; // average length of OFF time [sec]
	
	public traffic_OnOff()
	{}

	public traffic_OnOff(int mtu_, int rate_, double ontime_, double offtime_)
	{ set(mtu_, rate_, ontime_, offtime_); }

	public void set(int mtu_, int rate_, double ontime_, double offtime_)
	{
		packetSize = mtu_;
		rate = rate_;
		OnTime = ontime_;
		OffTime = offtime_;		
	}
	
	public double getPeriod() 
	{ return OnTime + OffTime; }
	
	public double getLoad() { return rate; }
	
	public int getBurst() { return (int)Math.ceil(rate * (OnTime + OffTime)); }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_OnOff)) return null;
		traffic_OnOff thatTraffic_ = (traffic_OnOff) that_;
		packetSize = Math.max(packetSize, thatTraffic_.packetSize);
		if (thatTraffic_.rate > rate) rate = thatTraffic_.rate;
		OnTime = Math.min(OnTime, thatTraffic_.OnTime);
		OffTime = Math.min(OffTime, thatTraffic_.OffTime);	
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_OnOff)) return;
		traffic_OnOff that_ = (traffic_OnOff) source_;
		packetSize = that_.packetSize;
		rate = that_.rate;
		OnTime = that_.OnTime;
		OffTime = that_.OffTime;
	}
		
	public String oneline()
	{
		return getClass().getName() + ":packetSize=" + packetSize + ", rate="
			+ rate + ", OnTime=" + OnTime + ", OffTime=" + OffTime;
	}
	
	//
	private void ___PROPERTY___() {}
	//	
	
	public void setPacketSize(int size_) { packetSize = size_; }
	public int getPacketSize() { return packetSize; }
	
	public void setAvgRate(int rate_) {rate = rate_; }
	public int getAvgRate() { return rate; }
	
	public void setOnTime(double ontime_) {OnTime = ontime_; }
	public double getOnTime() { return OnTime; }
	
	public void setOffTime(double offtime_) {OffTime = offtime_; }
	public double getOffTime() { return OffTime; }

	public int getMTU() { return packetSize; }
}
