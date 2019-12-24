// @(#)TSW_meter.java   9/2002
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
import drcl.util.random.*;

/**
Time Sliding Window Meter.
 
Refer to "Explicit Allocation of Best Effort Packet Delivery Service"
by D. Clark and Wenjia Fang 
at http://diffserv.lcs.mit.edu/Papers/exp-alloc-ddc-wf.pdf

<p> Properities:
<ul>
<li> <code>tgrate_</code>: target rate
</ul>

@author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
@version 1.0 10/26/00   
 */
public class TSW_meter extends Meter implements DFConstants
{
	double target_rate;
	double	win_length = 0.2;
	boolean wait;
	transient double	avg_rate = 0.0;
	transient double	lasttime = Double.NaN;
	transient int     count = 0;
	
	UniformDistribution ug = new UniformDistribution(0L);
	
	public TSW_meter()
	{ super(); }
	
	public TSW_meter(double win_len_, double tgrate_, boolean wait_, long seed_)
	{
		this();
		win_length = win_len_;
		target_rate = tgrate_/8;
		wait = wait_;
		avg_rate = 0.0;
		ug.setSeed(seed_);
	}
	
	public void duplicate(Object source_)
	{
		TSW_meter that_ = (TSW_meter)source_;
		win_length = that_.win_length;
		target_rate = that_.target_rate;
		wait = that_.wait;
		ug.setSeed(that_.ug.getSeed());
	}
	
	public void reset()
	{
		lasttime = Double.NaN;
		count = 0;
		avg_rate = 0.0;
		ug.reset();
	}

	public void setTargetRate(double v_)
	{ target_rate = v_; }
	
	public double getTargetRate()
	{ return target_rate; }
	
	public void setWinLength(double v_)
	{ win_length = v_; }
	
	public double getWinLength()
	{ return win_length; }
	
	public void setWait(boolean wait_)
	{ wait = wait_; }

	public boolean getWait()
	{ return wait; }

	protected int measure(Packet p_, double now_)
	{
		double bytes_in_tsw = avg_rate * win_length;
		double new_bytes    = bytes_in_tsw + p_.size;
		if (Double.isNaN(lasttime)) lasttime = now_;
		avg_rate = new_bytes / (now_ - lasttime + win_length);
		lasttime  = now_;
				
		boolean	inprofile_ = true;
		if (avg_rate > target_rate) {
			double prob = (avg_rate - target_rate) / avg_rate;
			double countp = count * prob;

			// mark the IN prfile smoothly. 
			if (wait) {
				if (countp < 1) prob = 0;
				else if (countp < 2) prob /= (2 - countp);
				else prob = 1;
			}
			else {
				if (countp < 1) prob /= (1 - countp);
				else prob = 1;
			}
			if (ug.nextDouble() < prob) 
			    inprofile_ = false;
		}

		if (inprofile_){
		    ++count;
			return IN_PROFILE;
		}
		else {
		    count = 0;
			return OUT_PROFILE;
		}
	}
	
	public void config(double tgrate_, double win_, boolean wait_, long seed_)
	{
		target_rate = tgrate_/8; //in bytes
		win_length = win_;
		wait = wait_;
		ug.setSeed(seed_);
	}
	
	public String info(String prefix_)
	{
		return prefix_ + "TSW_Meter:\n" 
			+ prefix_ + "  target_rate = " + target_rate + "\n"
			+ prefix_ + "   win_length = " + win_length + "\n"
			+ prefix_ + "         wait = " + wait + "\n"
			+ prefix_ + "uniform dist. = " + ug + "\n"
			+ prefix_ + "     avg_rate = " + avg_rate + "\n"
			+ prefix_ + "     lasttime = " + lasttime + "\n"
			+ prefix_ + "        count = " + count + "\n";
	}
}
