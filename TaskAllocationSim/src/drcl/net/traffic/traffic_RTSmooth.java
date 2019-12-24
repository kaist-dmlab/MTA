// @(#)traffic_RTSmooth.java   9/2002
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
This class describes the (r,T)-smooth traffic model.
It defines the following parameters:
<dl>
<dt>Frame Length
<dd>Length of frames (second).  The time axis is sliced into frames.

<dt>Time Origin
<dd>The start time of the first frame (second) .

<dt>Maximum Allowance Per Frame
<dd>Maximum number of bits that can be served in a frame.

<dt>Maximum Transmit Unit (MTU)
<dd>The maximum size of a packet (byte).
</dl>
 */
public class traffic_RTSmooth extends TrafficModel implements TrafficPeriodic
{
	public double frameLength; // second
	public int nbits; // max amount of bits can be served in a frame
	public double origin; // time origin of frame
	public int mtu; // byte
	
	public traffic_RTSmooth()
	{}

	public traffic_RTSmooth(int nbits_, double flen_, double forigin_, int mtu_)
	{ set(nbits_, flen_, forigin_, mtu_); }

	public void set(int nbits_, double flen_, double forigin_, int mtu_)
	{
		nbits = nbits_;
		frameLength = flen_;
		origin = forigin_;
		mtu = nbits_ >= (mtu_ << 3)? mtu_: (nbits_ >> 3)
			+ ((nbits_ & 7) > 0? 1: 0);
	}
		
	public double getPeriod() { return frameLength; }
	
	public double getLoad() 
	{ return (double)nbits / frameLength; }

	public int getBurst() { return nbits >> 3; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_RTSmooth)) return null;
		
		traffic_RTSmooth thatTraffic_ = (traffic_RTSmooth) that_;
		if (frameLength > thatTraffic_.frameLength)
			frameLength = thatTraffic_.frameLength;
		if (nbits < thatTraffic_.nbits)	nbits = thatTraffic_.nbits;
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_RTSmooth)) return;
		traffic_RTSmooth that_ = (traffic_RTSmooth) source_;
		frameLength = that_.frameLength;
		nbits = that_.nbits;
		origin = that_.origin;
	}
	
	public String oneline()
	{
		return getClass().getName() + ":frameLength=" + frameLength
			+ "), #bits/frame=" + nbits + ", frameOrigin=" + origin;
	}
	
	//
	private void ___PROPERTY___() {}
	//
	
	public void setFrameLength(double time_) { frameLength = time_; }
	public double getFrameLength() { return frameLength; }
	
	public void setNBitsPerFrame(int nbits_) { nbits = nbits_; }
	public int getNBitsPerFrame() { return nbits; }
	
	public void setOrigin(double time_) { origin = time_; }
	public double getOrigin() { return origin; }
	
	public int getMTU()	{ 	return mtu;	}
	public void setMTU(int mtu_) 	{ mtu = mtu_; }
}
