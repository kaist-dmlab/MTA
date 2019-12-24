// @(#)traffic_TokenBucket.java   9/2002
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
 * This class describes the token bucket traffic model.
 * It is defined by the following parameters:
 * <dl>
 * <dt>Bucket Size
 * <dd>The size of the token bucket (byte).
 *
 * <dt>Initial Bucket Size
 * <dd>The number of tokens that are available in the bucket initially (byte).
 *
 * <dt>Token Generation Rate
 * <dd>Rate of tokens being generated and filled in the bucket (bps).
 *
 * <dt>Output Rate
 * <dd>The rate of outputing packets out of the bucket (bps).
 *
 * <dt>Maximum Transmit Unit (MTU)
 * <dd>The maximum size of a packet (byte).
 * </dl>
 */
public class traffic_TokenBucket extends TrafficModel implements TrafficPeriodic
{
	public double outRate = Double.POSITIVE_INFINITY;
		// output rate of this shaper; bps
	public int tokenGenRate; // token generation rate; bps
	public int bucketSize;  // byte
	public int initBucketSize; // byte
	public int mtu; // byte

	public traffic_TokenBucket()
	{}

	public traffic_TokenBucket(int bsize_, int initbsize_, int trate_,
				double outrate_, int mtu_)
	{ set(bsize_, initbsize_, trate_, outrate_, mtu_); }

	public void set(int bsize_, int initbsize_, int trate_, double outrate_,
				int mtu_)
	{
		tokenGenRate = trate_;
		bucketSize = bsize_;
		initBucketSize = initbsize_;
		outRate = outrate_;
		mtu = Math.min(bsize_, mtu_);
	}
		
	public double getPeriod() { return (double)(bucketSize << 3) / tokenGenRate; }
	
	public double getLoad() { return tokenGenRate; }

	public int getBurst() { return bucketSize; }
	
	public TrafficModel merge(TrafficModel that_)
	{
		if (!(that_ instanceof traffic_TokenBucket)) return null;
		
		traffic_TokenBucket thatTraffic_ = (traffic_TokenBucket) that_;
		if (bucketSize < thatTraffic_.bucketSize) bucketSize = thatTraffic_.bucketSize;
		if (initBucketSize < thatTraffic_.initBucketSize) initBucketSize = thatTraffic_.initBucketSize;
		if (outRate < thatTraffic_.outRate) outRate = thatTraffic_.outRate;
		if (tokenGenRate < thatTraffic_.tokenGenRate) tokenGenRate = thatTraffic_.tokenGenRate;
		return this;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof traffic_TokenBucket)) return;
		traffic_TokenBucket that_ = (traffic_TokenBucket) source_;
		outRate = that_.outRate;
		tokenGenRate = that_.tokenGenRate;
		bucketSize = that_.bucketSize;
		initBucketSize = that_.initBucketSize;
	}
	
	public String oneline()
	{
		return getClass().getName() + ":bucketSize=" + bucketSize + "(init "
			+ initBucketSize + "), tokenGenRate=" + tokenGenRate + ", outRate="
			+ outRate;
	}
	
	//
	static void ___PROPERTY___() {}
	//
	
	public void setOutputRate(double rate_) { outRate = rate_; }
	public double getOutputRate() { return outRate; }
	
	public void setTokenGenRate(int rate_) { tokenGenRate = rate_; }
	public int getTokenGenRate() { return tokenGenRate; }
	
	public void setBucketSize(int size_) { bucketSize = size_; }
	public int getBucketSize() { return bucketSize; }
	
	public void setInitBucketSize(int size_) { initBucketSize = size_; }
	public int getInitBucketSize() { return initBucketSize; }
	
	public int getMTU()	{ 	return mtu;	}
	public void setMTU(int mtu_) 	{ mtu = mtu_; }
}
