// @(#)traffic_CDSmooth.java   9/2002
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
 * This class describes the (<i>C</i>,<i>D</i>)-smooth traffic model.
 * It defines the following parameters:
 * <dl>
 * <dt><i>D</i>
 * <dd>Time interval in interest (second).
 *
 * <dt><i>C</i>
 * <dd>Maximum number of bytes that can be served in every time interval of D.
 *
 * <dt>Maximum Transmit Unit (MTU)
 * <dd>The maximum size of a packet (byte).
 * </dl>
 */
public class traffic_CDSmooth extends TrafficModel implements TrafficPeriodic
{
	public int maxPacketSize;
	public int C;
	public double D;

	public traffic_CDSmooth()
	{}
	
	public traffic_CDSmooth(int c_, double d_, int mtu_)
	{
		C = c_; D = d_;
		maxPacketSize = mtu_;
	}
		
	public void set(int c_, double d_, int mtu_)
	{
		C = c_; D = d_;
		maxPacketSize = mtu_;
	}
		
	public double getPeriod()
	{ return D; }
	
	public double getLoad() 
	{ return (double)(C << 3) / D; }
	
	public int getBurst()
	{ return C; }

	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_CDSmooth)) return null;
		
		traffic_CDSmooth thatTraffic_ = (traffic_CDSmooth) that_;
		if (C > thatTraffic_.C)	C = thatTraffic_.C;
		if (D < thatTraffic_.D)	D = thatTraffic_.D;
		if (maxPacketSize < thatTraffic_.maxPacketSize)
			maxPacketSize = thatTraffic_.maxPacketSize;
		return this;
	}
	
	public void duplicate(Object source_)
	{
		traffic_CDSmooth that_ = (traffic_CDSmooth) source_;
		C = that_.C;
		D = that_.D;
		maxPacketSize = that_.maxPacketSize;
	}
	
	public String oneline()
	{
		return getClass().getName() + ":C=" + C + ", D=" + D
			+ ", maxPacketSize=" + maxPacketSize;
	}
	
	// 
	private void ___PROPERTY___() {}
	//
	
	public void setMaxPacketSize(int size_) { maxPacketSize = size_; }
	public int getMaxPacketSize() { return maxPacketSize; }
	
	public void setC(int c_) { C = c_; }
	public int getC() { return C; }
	
	public void setD(double d_) { D = d_; }
	public double getD() { return D; }
	
	public int getMTU()
	{ return maxPacketSize; }
}
