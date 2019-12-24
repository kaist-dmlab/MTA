// @(#)ParetoDistribution.java   5/2003
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

/**
 * This class implements a pareto distribution.
 * Parameters: scale, shape
*/

public class ParetoDistribution extends RandomNumberGenerator
{
	public String   getName()           { return "Pareto Distribution"; }

	public ParetoDistribution (double scale_, double shape_)
	{ this(scale_, shape_, 0L); }

	public ParetoDistribution (double scale_, double shape_, long seed_)
	{
		this(seed_);
		scale = scale_;
		shape = -1.0 / shape_;
	}
	
	double scale = 1.0;
	double shape = - 1.0;
	
	public ParetoDistribution()
	{ super(); }

	public ParetoDistribution(long seed_)
	{ super(seed_); }
	
	public double getMean()
	{ return scale / (1 + shape); }

	public double nextDouble()
	{
		double x;
		
		do { x= r.nextDouble(); } while (x == 0.0);

		// Mapping from Uniform to Pareto
		return scale * Math.pow(x, shape); 
	}

	public int  nextInt()
	{	return (int) nextDouble();	}

	public long nextLong()
	{	return (long) nextDouble();	}

	public void setScale(double scale_)
	{ scale = scale_; }

	public double getScale()
	{ return scale; }
	
	public void setShape(double shape_)
	{ shape = -1.0 / shape_; }

	public double getShape()
	{ return -1.0 / shape; }

	public String info(String prefix_)
	{
		return super.info(prefix_)
			+  prefix_ + "Scale = " + scale + "\n"
			+  prefix_ + "Shape = " + getShape() + "\n";
	}

	public String oneline()
	{
		return super.oneline()
			+ ", scale=" + scale
			+ ", shape=" + getShape();
	}
}

