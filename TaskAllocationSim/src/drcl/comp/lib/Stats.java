// @(#)Stats.java   5/2003
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

package drcl.comp.lib;

import drcl.comp.*;
import drcl.data.*;

/**
 * Statistical components.  
 * Receiving Double or DoubleObj, outputing mean and std.
 */
public class Stats extends Component
{
	double sum = 0.0;
	double varsum = 0.0;
	long count = 0;
	boolean outenabled = false;
	Port outmean = addPort("mean", false);
	Port outstd = addPort("std", false);
	
	public Stats()
	{ super(); }

	public Stats(String id_)
	{ super(id_); }

	public double getMean()
	{ return sum / count; }

	// ???
	public double getStd()
	{
		double mean_ = getMean();
		return Math.sqrt(varsum / count - mean_*mean_);
	}

	public boolean isOutputEnabled()
	{ return outenabled; }

	public void setOutputEnabled(boolean enabled_)
	{ outenabled = enabled_; }

	public void reset()
	{
		super.reset();
		sum = varsum = 0.0;
		count = 0;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Stats that_ = (Stats) source_;
		outenabled = that_.outenabled;
	}

	public String info()
	{
		return " Mean = " + getMean() + "\n"
				+ "  Std = " + getStd() + "\n"
				+ "Count = " + count + "\n"
				+ "Output Enabled: " + outenabled + "\n";
	}
	
	protected void process(Object data_, Port inPort_)
	{
		double v = data_ instanceof Double?
				((Double)data_).doubleValue(): ((DoubleObj)data_).value;
		sum += v;
		varsum += v*v;
		count ++;

		if (outenabled) {
			double mean_ = sum / count;
			double std_ = Math.sqrt(varsum / count - mean_*mean_);
			outmean.doSending(new double[]{count, mean_});
			outstd.doSending(new double[]{count, std_});
		}
	}
}
