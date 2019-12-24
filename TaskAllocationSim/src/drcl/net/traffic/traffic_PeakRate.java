// @(#)traffic_PeakRate.java   9/2002
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
 * This class describes the peak rate traffic model.
 * It defines the following parameters:
 * <dl>
 * <dt>Maximum Inte-rarrival Time
 * <dd>Maximum packet inter-arrival time (second).
 *
 * <dt>Minimum Inte-rarrival Time
 * <dd>Minimum packet inter-arrival time (second).
 *
 * <dt>Minimum Transmit Unit
 * <dd>The minimum size of a packet (byte).
 *
 * <dt>Maximum Transmit Unit (MTU)
 * <dd>The maximum size of a packet (byte).
 * </dl>
 */
public class traffic_PeakRate extends TrafficModel implements TrafficPeriodic
{
	public int maxPktSize;
	public int minPktSize;
	public double maxIntArrivalTime;
	public double minIntArrivalTime;
	
	public traffic_PeakRate()
	{}

	public traffic_PeakRate(int min_, int max_, double miniat_, double maxiat_)
	{ set(min_, max_, miniat_, maxiat_); }
		
	public void set(int min_, int max_, double miniat_, double maxiat_)
	{
		minPktSize = min_;
		maxPktSize = max_;
		minIntArrivalTime = miniat_;
		maxIntArrivalTime = maxiat_;
	}
		
	public double getPeriod() 
	{ return (maxIntArrivalTime + minIntArrivalTime) / 2.0; }
	
	public double getLoad() 
	{ return ((maxPktSize + minPktSize) << 3) / (maxIntArrivalTime
						+ minIntArrivalTime); }
	
	public int getBurst() { return maxPktSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_PeakRate)) return null;
		traffic_PeakRate thatTraffic_ = (traffic_PeakRate) that_;
		if (maxPktSize < thatTraffic_.maxPktSize)	maxPktSize = thatTraffic_.maxPktSize;
		if (minPktSize > thatTraffic_.minPktSize)	minPktSize = thatTraffic_.minPktSize;
		if (maxIntArrivalTime < thatTraffic_.maxIntArrivalTime)	maxIntArrivalTime = thatTraffic_.maxIntArrivalTime;
		if (minIntArrivalTime > thatTraffic_.minIntArrivalTime)	minIntArrivalTime = thatTraffic_.minIntArrivalTime;
		return this;
	}
	
	public void duplicate(Object source_)
	{
		traffic_PeakRate that_ = (traffic_PeakRate) source_;
		maxPktSize = that_.maxPktSize;
		minPktSize = that_.minPktSize;
		maxIntArrivalTime = that_.maxIntArrivalTime;
		minIntArrivalTime = that_.minIntArrivalTime;
	}
		
	public String oneline()
	{
		return	getClass().getName() + ":packetSize=" + minPktSize
			+ "-" + maxPktSize + ", interArrivalTime=" + minIntArrivalTime
			+ "-" + maxIntArrivalTime;
	}
	
	//
	private void ___PROPERTY___() {}
	//
	
	public void setMaxPktSize(int size_) { maxPktSize = size_; }
	public int getMaxPktSize() { return maxPktSize; }
	
	public void setMinPktSize(int size_) { minPktSize = size_; }
	public int getMinPktSize() { return minPktSize; }
	
	public void setMaxIntArrivalTime(double time_) {maxIntArrivalTime = time_; }
	public double getMaxIntArrivalTime() { return maxIntArrivalTime; }
	
	public void setMinIntArrivalTime(double time_) { minIntArrivalTime = time_; }
	public double getMinIntArrivalTime() { return minIntArrivalTime; }
	
	public int getMTU() { return maxPktSize; }
}
