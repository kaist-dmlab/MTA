// @(#)RandomNumberGenerator.java   5/2003
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

import java.util.Random;

public abstract class RandomNumberGenerator extends drcl.DrclObj
{
	// the followings must be included in child class

	public abstract String   getName();

	public abstract double nextDouble();
	public abstract int    nextInt();
	public abstract long   nextLong();

	
	protected Random r;
	protected long seed = 0;

	public RandomNumberGenerator()
	{ this(0); }

	public RandomNumberGenerator(long seed_)
	{ super(); seed = seed_; r = new Random(seed); }

	/**
	 * Resets the generator .
	 */
	public void reset() 
	{
		r.setSeed(seed);
	}

	public void duplicate(Object source_)
	{
		seed = ((RandomNumberGenerator)source_).seed;
		r    = new Random(seed);
	}
	
	public void setSeed(long seed_)
	{
		seed = seed_;
		r.setSeed(seed);
	}
	
	public long getSeed() { return seed; }

	public String info()
	{ return info(""); }

	public double getMean()
	{ return Double.NaN; }

	public double getStd()
	{ return Double.NaN; }

	public String info(String prefix_)
	{
		return prefix_ + drcl.util.StringUtil.finalPortionClassName(getClass())
				+ "\n"
				+ prefix_ + "Seed = " + seed + "\n"
				+ prefix_ + "Mean = " + getMean() + "\n"
				+ prefix_ + " Std = " + getStd() + "\n";
	}

	public String toString()
	{ return oneline(); }

	public String oneline()
	{
		return drcl.util.StringUtil.finalPortionClassName(getClass())
				+ ", Seed=" + seed
				+ ", Mean = " + getMean()
				+ ", Std = " + getStd();
	}
}

