// @(#)tsRTSmooth.java   9/2002
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

public class tsRTSmooth extends TrafficShaper
{
	int amountServed;	// amount of bits served in the current frame
	double fbegin;		// Begin time of current frame
	double fend;		// Ending time of current frame
	traffic_RTSmooth traffic = null;

	public tsRTSmooth()
	{ this(new traffic_RTSmooth()); }

	public tsRTSmooth(traffic_RTSmooth traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void reset()
	{
		super.reset();
		if (traffic != null) {
			fbegin = traffic.origin;
			fend = fbegin + traffic.frameLength;
		}
		amountServed = 0;
	}

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+ prefix_ + "State: amountServed/frame=" + amountServed
			+ ", frameStart=" + fbegin + ", frameEnd=" + fend + "\n";
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_RTSmooth)traffic_; reset(); }

	protected double adjust(double now_, int size_) 
	{
		size_ = size_ << 3; // byte -> bits
		
		if (now_ >= fend) {
			// start of a frame
			amountServed = size_;
			fbegin = (int)((now_ - traffic.origin) / traffic.frameLength)
				* traffic.frameLength + traffic.origin;
			fend = fbegin + traffic.frameLength;
			return fbegin - now_;
		}
		else if (amountServed + size_ > traffic.nbits) {
			// start of a frame
			amountServed = size_;
			fbegin = fend;
			fend = fbegin + traffic.frameLength;
			return fbegin - now_;
		}
		else {
			amountServed += size_;
			if (now_ < fbegin) return fbegin - now_;
			else return 0.0;
		}
	}

	public void setFrameLength(double time_) { traffic.frameLength = time_; }
	public double getFrameLength() { return traffic.frameLength; }
	
	public void setNBitsPerFrame(int nbits_) { traffic.nbits = nbits_; }
	public int getNBitsPerFrame() { return traffic.nbits; }
	
	public void setOrigin(double time_) { traffic.origin = time_; }
	public double getOrigin() { return traffic.origin; }
	
	public int getMTU()	{ 	return traffic.mtu;	}
	public void setMTU(int mtu_) 	{ traffic.mtu = mtu_; }

	public void set(int nbits_, double flen_, double forigin_, int mtu_)
	{ traffic.set(nbits_, flen_, forigin_, mtu_); reset(); }
}
