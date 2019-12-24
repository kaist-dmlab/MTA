// @(#)TB_meter.java   9/2002
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

package drcl.diffserv;
 
import drcl.net.Packet;

/**
  * This class implements a token bucket meter. 
  * 
  * <p> Properties:
  * <ul>
  * <li> <code>burst</code>: burst size 
  * <li> <code>tgrate</code>: token generation rate
  * </ul>
  * 
  * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
  * @version 1.0 10/27/00   
  */
public class TB_meter extends Meter implements DFConstants
{
	/* token bucket parameters */	
	long burst; // in byte
	long tgrate; // in byte/second
	transient double lasttime = Double.NaN; // last time when buffer is filled up
	transient long counter; // in byte

	public TB_meter()
	{ super(); }
	
	/**
	@param burst_ in bit
	@param rate_ in bit/second
	 */
	public TB_meter(long burst_, long rate_)
	{
		super();
		config(burst_, rate_);
	}
	
	public void reset()
	{
		lasttime = Double.NaN;
		counter = burst;
	}
	
	public String info(String prefix_)
	{
		return prefix_ + "(TB_Meter) burst=" + (burst<<3) + ", tg_rate=" + (tgrate<<3)
			+ ", # of tokens=" + (counter<<3) + " at time " + lasttime + "\n";
	}

	public void duplicate(Object source_)
	{
		TB_meter that_ = (TB_meter)source_;
		burst = counter = that_.burst;
		tgrate = that_.tgrate;
	}
	
	/**
	@param rate_ in bit/second
	@param burst_ in bit
	 */
	public void config(long burst_, long rate_)
	{
		burst = counter = burst_ >> 3;
		tgrate = rate_ >> 3;
	}

	/** Sets the token bucket size (bits). */
	public void setBurst(long burst_)
	{ burst = counter = burst_<<3; }
	
	/** Sets the token generation rate (bps). */
	public void setTokenGenerationRate(long rate_)
	{ tgrate = rate_<<3; }

	/** Returns the token bucket size (bits). */
	public long getBurst()
	{ return burst>>3; }

	/** Returns the token generation rate (bps). */
	public long getTokenGenerationRate()
	{ return tgrate>>3; }

	protected int measure(Packet p, double now_)
	{ 
		if (Double.isNaN(lasttime)) lasttime = now_;

		long counterNow_ = counter + (long)((now_ - lasttime) * tgrate);
		if(counterNow_ > burst) {
			counter = counterNow_ = burst;
			lasttime = now_;
		}

		if(counterNow_ <  p.size)
			return OUT_PROFILE;
	 	else {
	 		counter -= p.size;
			return IN_PROFILE;
		}
	}
}
