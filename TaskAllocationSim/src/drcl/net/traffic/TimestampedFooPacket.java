// @(#)TimestampedFooPacket.java   10/2003
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

/** A packet class that keeps a timestamp in addition to what is in the 
 * superclass. */
public class TimestampedFooPacket extends drcl.net.FooPacket
{
	double timestamp;

	public TimestampedFooPacket()
	{ super(); }

	public TimestampedFooPacket(int pktsize_, int pktcnt_, long bytecnt_)
	{ super(pktsize_, pktcnt_, bytecnt_); }

	public TimestampedFooPacket(double time_, int pktsize_, int pktcnt_,
					long bytecnt_)
	{ super(pktsize_, pktcnt_, bytecnt_); timestamp = time_; }

	public String getName()
	{ return "TimedFOO"; }

	public void setTimestamp(double time_)
	{ timestamp = time_; }

	public double getTimestamp()
	{ return timestamp; }

	/** Returns true always. */
	public boolean isTimestampSupported()
	{ return true; }

	/*
	public void duplicate(Object p_)
	{
		super.duplicate(p_);
		timestamp = ((TimestampedFooPacket)p_).timestamp;
	}
	*/

	public Object clone()
	{ return new TimestampedFooPacket(timestamp, size, getPacketCount(),
					getByteCount()); }

	public String _toString(String separator_)
	{ return "timestamp:" + timestamp + separator_
			+ super._toString(separator_); }
}
