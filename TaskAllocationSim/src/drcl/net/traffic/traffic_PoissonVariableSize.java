// @(#)traffic_PoissonVariableSize.java   7/2003
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

/**
 * This class describes the traffic model with Poisson arrival process
 * and packet size with exponential distribution.
 * It defines the following parameters:
 * <dl>
 * <dt>Rate
 * <dd>Average sending rate (bps).
 *
 * <dt>AvgPacketSize
 * <dd>Average packet size (byte).
 * </dl>
 */
public class traffic_PoissonVariableSize extends TrafficModel implements TrafficPeriodic
{
	public int packetSize;
	public double rate;  // avg sending rate (bps)

	public traffic_PoissonVariableSize()
	{}

	public traffic_PoissonVariableSize(int avgPktSize_, double brate_)
	{ set(avgPktSize_, brate_); }
	
	public void set(int avgPktSize_, double brate_)
	{
		packetSize = avgPktSize_;
		rate = brate_;
	}
	
	public double getPeriod() 
	{ return (double)(packetSize << 3) / rate; }
	
	public double getLoad() { return rate; }
	
	public int getBurst() { return packetSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_PoissonVariableSize)) return null;
		traffic_PoissonVariableSize thatTraffic_
				= (traffic_PoissonVariableSize) that_;
		double load1_ = getLoad();
		double load2_ = thatTraffic_.getLoad();
		packetSize = Math.max(packetSize, thatTraffic_.packetSize);
		rate = Math.max(load1_, load2_);
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_PoissonVariableSize)) return;
		traffic_PoissonVariableSize that_
				= (traffic_PoissonVariableSize) source_;
		packetSize = that_.packetSize;
		rate = that_.rate;
	}
		
	public String oneline()
	{
		return  getClass().getName() + ":AvgPacketSize(B)=" + packetSize
			+ ", Rate(bps)=" + rate;
	}
	
	//
	private void ___PROPERTY___() {}
	//	
	
	public void setAvgPacketSize(int size_) { packetSize = size_; }
	public int getAvgPacketSize() { return packetSize; }
	
	public void setRate(double brate_) {rate = brate_; }
	public double getRate() { return rate; }
	
	public int getMTU() { return Integer.MAX_VALUE; }
}
