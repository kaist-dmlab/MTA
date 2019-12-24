// @(#)CBTTimingPack.java   5/2003
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

package drcl.inet.protocol.cbt;

import java.util.Random;

/** Defines all the timing parameters used by {@link CBT}.
 * Refer to draft-ietf-idmr-cbt-spec-v3-01.txt for the default values and
 * formula.  The values that are defined in the draft but not defined here
 * are [HelloInterval], [HelloPreference] and [DrTransTimer].  */
public class CBTTimingPack
{
	static final double DEFAULT_HOLDTIME = 3.0;
	static final int DEFAULT_MAX_RTX = 3;
	static final double DEFAULT_RTX_INTERVAL = 5.0;
	static final double DEFAULT_JOIN_TIMEOUT =
			3.5 * DEFAULT_RTX_INTERVAL;
	static final double DEFAULT_TRANSIENT_TIMEOUT = DEFAULT_JOIN_TIMEOUT;
	static final double DEFAULT_CHILD_DEL_TIMER = 1.5 * DEFAULT_HOLDTIME;
	static final double DEFAULT_UPSTREAM_EXPIRE_TIME =
			DEFAULT_MAX_RTX * DEFAULT_RTX_INTERVAL + DEFAULT_HOLDTIME;
	static final double DEFAULT_ECHO_INTERVAL_BASE = 60;
	static final double DEFAULT_DOWNSTREAM_EXPIRE_TIME_BASE =
			DEFAULT_ECHO_INTERVAL_BASE + DEFAULT_UPSTREAM_EXPIRE_TIME;

	double holdTime = DEFAULT_HOLDTIME;
	int maxRtx = DEFAULT_MAX_RTX;
	double rtxInterval = DEFAULT_RTX_INTERVAL;
	double joinTimeout = DEFAULT_JOIN_TIMEOUT;
	double transientTimeout = DEFAULT_TRANSIENT_TIMEOUT;
	double childDelTimer = DEFAULT_CHILD_DEL_TIMER;
	double upstreamExpireTime = DEFAULT_UPSTREAM_EXPIRE_TIME;
	double downstreamExpireTimeBase =DEFAULT_DOWNSTREAM_EXPIRE_TIME_BASE;
	double echoIntervalBase = DEFAULT_ECHO_INTERVAL_BASE;
	Random r = new Random(0);

	/** See all the getter methods for the default values. */
	public CBTTimingPack()
	{
	}

	/**
	 * Default formula for other values:
	 * <ul>
	 * <li>[JoinTimeout] = 3.5 * [RtxInterval]
	 * <li>[TransientTimeout] = [JoinTimeout]
	 * <li>[ChildDelTimer] = 1.5 * [HoldTime]
	 * <li>[UpstreamExpireTime] = [MaxRtx] * [RtxInterval] + [HoldTime]
	 * <li>[DownStreamExpireTimeBase] = [UpstreamExpireTime] +[EchoIntervalBase]
	 * </ul>
	 */
	public CBTTimingPack(double holdTime_,
					int maxRtx_,
					double rtxInterval_,
					double echoIntervalBase_)
	{
		holdTime = holdTime_;
		maxRtx = maxRtx_;
		rtxInterval = rtxInterval_;
		echoIntervalBase = echoIntervalBase_;
		joinTimeout = 3.5 * rtxInterval;
		transientTimeout = joinTimeout;
		childDelTimer = 1.5 * holdTime;
		upstreamExpireTime = maxRtx * rtxInterval + holdTime;
		downstreamExpireTimeBase = upstreamExpireTime + echoIntervalBase;
	}

	public CBTTimingPack(double holdTime_,
					int maxRtx_,
					double rtxInterval_,
					double joinTimeout_,
					double transientTimeout_,
					double childDelTimer_,
					double upstreamExpireTime_,
					double downstreamExpireTimeBase_,
					double echoIntervalBase_)
	{
		holdTime = holdTime_;
		maxRtx = maxRtx_;
		rtxInterval = rtxInterval_;
		joinTimeout = joinTimeout_;
		transientTimeout = transientTimeout_;
		childDelTimer = childDelTimer_;
		upstreamExpireTime = upstreamExpireTime_;
		downstreamExpireTimeBase = downstreamExpireTimeBase_;
		echoIntervalBase = echoIntervalBase_;
	}

	/** Generic response interval.  Default: 3 seconds. */
	public double getHoldtime()
	{ return holdTime; }

	/** Default maximum number of retransmissions.  Default: 3. */
	public int getMaxRtx()
	{ return maxRtx; }

	/** Message retransmission time.  Default: 5 seconds. */
	public double getRtxInterval()
	{ return rtxInterval; }

	/** Raise exception deu to tree join failure. 
	 * Default: (3.5 * [RtxInterval]) seconds. */
	public double getJoinTimeout()
	{ return joinTimeout; }

	/** delete (unconfirmed) transient state.  
	 * Default: [JoinTimeout] seconds. */
	public double getTransientTimeout()
	{ return transientTimeout; }

	/** Remove child interface from forwarding cache.
	 * Default: (1.5*[HoldTime]) seconds. */
	public double getChildDelTimer()
	{ return childDelTimer; }

	/** Time to send a QUIT-NOTIFICATION to the non-responding parent.
	 * Default: ([MaxRtx]*[RtxInterval] + [HoldTime]) seconds. */
	public double getUpstreamExpireTime()
	{ return upstreamExpireTime; }

	/** Not heard from child, base time to remove child interface.
	 * Default: ([EchoIntervalBase] + [UpstreamExpireTime]) seconds. */
	public double getDownstreamExpireTimeBase()
	{ return downstreamExpireTimeBase; }

	/** Base interval between sending ECHO_REQUEST to parent routers.
	 * Default: 60 seconds. */
	public double getEchoIntervalBase()
	{ return echoIntervalBase; }

	/** Not heard from child, time to remove child interface.
	 * Default: ([EchoInterval] + [UpstreamExpireTime]) seconds. */
	public double getDownstreamExpireTime()
	{ return downstreamExpireTimeBase + r.nextInt((int)holdTime); }

	/** Interval between sending ECHO_REQUEST to parent routers.
	 * Default: (60 + rnd) seconds, where rnd is between 0 and
	 * [HoldTime] seconds. */
	public double getEchoInterval()
	{ return echoIntervalBase + r.nextInt((int)holdTime); }

	/** Echo request rtx timer.
	 * Default: [HoldTime] seconds. */
	public double getEchoRtxTime()
	{ return holdTime; }

	/** Random seed for generating rnd for [EchoInterval].
	 * @see #getEchoInterval() */
	public void setRandomSeed(long seed_)
	{ r.setSeed(seed_); }

	public void setHoldtime(double holdtime_)
	{ holdTime = holdtime_; }

	public void setMaxRtx(int v_)
	{ maxRtx = v_; }

	public void setRtxInterval(double v_)
	{ rtxInterval = v_; }

	public void setJoinTimeout(double v_)
	{ joinTimeout = v_; }

	public void setTransientTimeout(double v_)
	{ transientTimeout = v_; }

	public void setChildDelTimer(double v_)
	{ childDelTimer = v_; }

	public void setUpstreamExpireTime(double v_)
	{ upstreamExpireTime = v_; }

	public void setDownstreamExpireTimeBase(double v_)
	{ downstreamExpireTimeBase = v_; }

	public void setEchoIntervalBase(double v_)
	{ echoIntervalBase = v_; }

	public String info()
	{
		return    "                HoldTime = " + holdTime + " sec.\n"
				+ "                  MaxRtx = " + maxRtx + "\n"
				+ "             RtxInterval = " + rtxInterval + " sec.\n"
				+ "             JoinTimeout = " + joinTimeout + " sec.\n"
				+ "        TransientTimeout = " + transientTimeout + " sec.\n"
				+ "           ChildDelTimer = " + childDelTimer + " sec.\n"
				+ "      UpstreamExpireTime = " + upstreamExpireTime + " sec.\n"
				+ "DownstreamExpireTimeBase = " + downstreamExpireTimeBase
					+ " sec.\n"
				+ "        EchoIntervalBase = " + echoIntervalBase + " sec.\n";
	}
}
