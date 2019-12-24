// @(#)traffic_Periodic.java   9/2002
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
This class describes a periodic traffic model.
It defines the following parameters:
<dl>
<dt>C
<dd>The maximum number of bytes that are allowed in a period.

<dt>P
<dd>The period (second).

<dt>Maximum Packet Size
<dd>The maximum size of a packet (byte).
</dl>
 */
public class traffic_Periodic extends TrafficModel implements TrafficPeriodic
{
	public int maxPacketSize;
	public int C;
	public double P;

	public traffic_Periodic()
	{}

	public traffic_Periodic(int c_, double p_, int mtu_)
	{ set(c_, p_, mtu_); }

	public void set(int c_, double p_, int mtu_)
	{
		C = c_; P = p_;
		maxPacketSize = mtu_;
	}

	public double getPeriod() { return P; }
	
	public double getLoad() 
	{ return (double)(C << 3) / P; }

	public int getBurst() { return maxPacketSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_Periodic)) return null;
		
		traffic_Periodic thatTraffic_ = (traffic_Periodic) that_;
		if (C > thatTraffic_.C)	C = thatTraffic_.C;
		if (P < thatTraffic_.P)	P = thatTraffic_.P;
		if (maxPacketSize < thatTraffic_.maxPacketSize)
			maxPacketSize = thatTraffic_.maxPacketSize;
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_Periodic)) return;
		traffic_Periodic that_ = (traffic_Periodic) source_;
		C = that_.C;
		P = that_.P;
		maxPacketSize = that_.maxPacketSize;
	}
	
	public String oneline()
	{
		return getClass().getName() + ":C=" + C + ", P=" + P
			+ ", maxPacketSize=" + maxPacketSize;
	}
	
	// 
	static void ___PROPERTY___() {}
	//
	
	public void setMaxPacketSize(int size_) { maxPacketSize = size_; }
	public int getMaxPacketSize() { return maxPacketSize; }
	
	public void setC(int c_) { C = c_; }
	public int getC() { return C; }
	
	public void setPeriod(double p_) { P = p_; }
	
	public int getMTU() { return maxPacketSize; }
}
