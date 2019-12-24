// @(#)LognormalParetoDistribution.java   9/2002
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

package drcl.util.random;

import java.awt.*;
import java.util.*;

public class LognormalParetoDistribution extends RandomNumberGenerator
{
	public String   getName()
	{ return "Lognormal-body Pareto-tail Distribution"; }
	
	ParetoDistribution pareto;
	double mean = 0.0, std = 1.0;
	double cutoff = 0.0;
	
	public LognormalParetoDistribution ()
	{ super(); pareto = new ParetoDistribution(); }

	public LognormalParetoDistribution (long normalSeed_)
	{ super(normalSeed_); pareto = new ParetoDistribution(); }

	public LognormalParetoDistribution (double normalMean_, double normalStd_,
			double cutoff_, double paretoScale_, double paretoShape_)
	{ this(normalMean_, normalStd_, cutoff_, paretoScale_, paretoShape_, 
					0L, 0L); }

	public LognormalParetoDistribution (double normalMean_, double normalStd_,
			double cutoff_, double paretoScale_, double paretoShape_,
			long normalSeed_, long paretoSeed_)
	{
		super(normalSeed_);
		mean = normalMean_;
		std = normalStd_;
		cutoff = (Math.log(cutoff_) - mean) / std;
		pareto = new ParetoDistribution(paretoScale_, paretoShape_,
						paretoSeed_);
	}
	
	public double nextDouble()
	{
		double tmp_ = r.nextGaussian();
		if (tmp_ < cutoff)
			return Math.exp(tmp_ * std + mean);
		else
			return pareto.nextDouble();
	}

	public int nextInt()
	{
		double tmp_ = r.nextGaussian();
		if (tmp_ < cutoff)
			return (int)Math.exp(tmp_ * std + mean);
		else
			return pareto.nextInt();
	}

	public long nextLong()
	{
		double tmp_ = r.nextGaussian();
		if (tmp_ < cutoff)
			return (long)Math.exp(tmp_ * std + mean);
		else
			return pareto.nextLong();
	}

	/** Sets the "normal" distribution mean for the lognormal distribution. */
	public void setNormalMean(double m_)
	{
		cutoff = getCutOff();
		mean = m_;
		setCutOff(cutoff);
	}

	/** Returns the "normal" distribution mean for the lognormal distribution. 
	 */
	public double getNormalMean()
	{ return mean; }
	
	/** Sets the "normal" distribution std for the lognormal distribution. */
	public void setNormalStd(double std_)
	{
		cutoff = getCutOff();
		std = std_;
		setCutOff(cutoff);
	}

	/** Returns the "normal" distribution std for the lognormal distribution. */
	public double getNormalStd()
	{ return std; }

	/** Sets the cut-off point in the lognormal distribution. */
	public void setCutOff(double cutoff_)
	{
		cutoff = (Math.log(cutoff_) - mean ) / std;
	}

	/** Returns the cut-off point in the lognormal distribution. */
	public double getCutOff()
	{
		return Math.exp(cutoff * std + mean);
	}

	public void setParetoScale(double scale_)
	{ pareto.setScale(scale_); }

	public double getParetoScale()
	{ return pareto.getScale(); }

	public void setParetoShape(double shape_)
	{ pareto.setShape(shape_); }

	public double getParetoShape()
	{ return pareto.getShape(); }

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+  prefix_ + "Normal Mean = " + mean + "\n"
			+  prefix_ + "Normal Std. = " + std + "\n"
			+  prefix_ + "Lognormal Cut-off = " + getCutOff() + "\n"
			+ pareto.info(prefix_);
	}

	public String oneline()
	{
		return super.oneline()
			+  ", normal_mean=" + mean
			+  ", normal_std=" + std
			+  ", lognormal_cutoff=" + getCutOff() + ", "
			+ pareto.oneline();
	}
}

