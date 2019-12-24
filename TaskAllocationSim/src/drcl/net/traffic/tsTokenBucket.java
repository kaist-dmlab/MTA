// @(#)tsTokenBucket.java   9/2002
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
 * A traffic shaper that conforms to the {@link traffic_TokenBucket token bucket}
 * traffic model.
 * @see traffic_TokenBucket
 */
public class tsTokenBucket extends TrafficShaper
{
	double lasttime; //last time when tokens start to be generated
	int acc; // number of accumulated bytes coming in since "lasttime"
	double readytime; // ready to send next packet; constrained by output rate
	traffic_TokenBucket traffic = null;

	public tsTokenBucket()
	{ this(new traffic_TokenBucket()); }

	public tsTokenBucket(traffic_TokenBucket traffic_)
	{ super(); traffic = traffic_; reset(); }

	public void reset()
	{
		super.reset();
		acc = 0;
		readytime = 0.0;
		lasttime = Double.NaN;
	}

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+ prefix_ + "State: #bytes accumulated since lasttime=" + acc
			+ ", lasttime=" + lasttime + ", readytime=" + readytime + "\n";
	}

	protected double adjust(double now_, int size_) 
	{
		double readytime_; // time to have enough tokens for size_

		// to avoid rounding error, update fulltime from lasttime
		double fulltime_ = lasttime + (double)(acc << 3) / traffic.tokenGenRate;
			// Double.NaN the first time

		if (fulltime_ > now_) {
			readytime_ = fulltime_ - (double)((traffic.bucketSize - size_) << 3)
				/ traffic.tokenGenRate;
			if (readytime_ < now_) readytime_ = now_;
			// consider integer wrap-around
			if ((Integer.MAX_VALUE >> 3) - acc < size_) {
				acc -= (int)(traffic.tokenGenRate * (now_ - lasttime) / 8.0);
				lasttime = now_;
			}
			acc += size_;
		}
		else { // goes here too if fulltime_ is Double.NaN (the first time)
			acc = size_;
			readytime_ = lasttime = now_;
		}

		if (readytime_ > readytime)
			readytime = readytime_ + (size_ << 3) / traffic.outRate;
		else
			readytime += (size_ << 3) / traffic.outRate;

		return readytime - now_;
	}

	public TrafficModel getTrafficModel()
	{ return traffic; }

	public void setTrafficModel(TrafficModel traffic_)
	{ traffic = (traffic_TokenBucket)traffic_; reset(); }

	public void setOutputRate(double rate_) { traffic.outRate = rate_; }
	public double getOutputRate() { return traffic.outRate; }
	
	public void setTokenGenRate(int rate_) { traffic.tokenGenRate = rate_; }
	public int getTokenGenRate() { return traffic.tokenGenRate; }
	
	public void setBucketSize(int size_) { traffic.bucketSize = size_; }
	public int getBucketSize() { return traffic.bucketSize; }
	
	public void setInitBucketSize(int size_) { traffic.initBucketSize = size_; }
	public int getInitBucketSize() { return traffic.initBucketSize; }
	
	public int getMTU()	{ 	return traffic.mtu;	}
	public void setMTU(int mtu_) 	{ traffic.mtu = mtu_; }

	public void set(int bsize_, int initbsize_, int trate_, double outrate_,
					int mtu_)
	{
		traffic.set(bsize_, initbsize_, trate_, outrate_, mtu_);
		reset();
	}
}
