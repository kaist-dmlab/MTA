// @(#)ExponentialDistribution.java   5/2003
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

package drcl.util.random;

import java.awt.*;
import java.util.*;


public class ExponentialDistribution extends RandomNumberGenerator
{
	public String   getName()           { return "Exponential Distribution"; }

	public ExponentialDistribution (double rate_)
	{ this(rate_, 0L); }

	public ExponentialDistribution (double rate_, long seed_)
	{
		this(seed_);
		_rate = -1.0 / rate_;
	}
	
	double _rate = -1.0;
	
	public ExponentialDistribution()
	{ super(); }

	public ExponentialDistribution(long seed_)
	{ super(seed_); }
	
	public double nextDouble()
	{
		double x;
		do { x= r.nextDouble(); } while (x == 0.0);

		// Mapping from Uniform to Exponential
		return _rate*Math.log(x); 

	}

	public int  nextInt()
	{	return (int) nextDouble();	}

	public long nextLong()
	{	return (long) nextDouble();	}

	public void setRate(double rate_) { _rate = -1.0 / rate_; }
	public double getRate() { return -1.0 / _rate; }

	public double getMean()
	{ return -_rate; }

	public double getStd()
	{ return -_rate; }

	public String info(String prefix_)
	{ return super.info(prefix_) + prefix_ + "Rate = " + getRate() + "\n"; }

	public String oneline()
	{ return super.oneline() + ", rate=" + getRate(); }
}

