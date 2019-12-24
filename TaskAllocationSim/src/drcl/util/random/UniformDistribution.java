// @(#)UniformDistribution.java   5/2003
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


public class UniformDistribution extends RandomNumberGenerator
{
	public String   getName()           { return "Uniform distribution for double number"; }
	
	public UniformDistribution (double min_, double max_)
	{ this(min_, max_, 0L); }

	public UniformDistribution (double min_, double max_, long seed_)
	{
		this(seed_);
		min = min_;
		max = max_;
	}

	public UniformDistribution()
	{ super(); }

	public UniformDistribution(long seed_)
	{ super(seed_); }
	
	double max = 1.0, min = 0.0;
	
	public double nextDouble()
	{ return r.nextDouble() * (max - min) + min;	}

	public int    nextInt()
	{ return (int) nextDouble(); }
	
	public long nextLong()
	{ return (long) nextDouble(); }
	
	public double getMax() { return max; }
	public void setMax(double max_) { max = max_; }
	
	public double getMin() { return min; }
	public void setMin(double min_) { min = min_; }

	public double getMean()
	{ return (max + min) / 2; }

	public double getStd()
	{ return Math.sqrt((max-min)*(max-min)/12); }

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+  prefix_ + "Max. = " + max + "\n"
			+  prefix_ + "Min. = " + min + "\n";
	}

	public String oneline()
	{ return super.oneline() + ", max=" + max + ", min=" + min; }
}

