// @(#)TruncatedParetoDistribution.java   5/2003
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

public class TruncatedParetoDistribution extends RandomNumberGenerator
{
	public String   getName()           { return "Pareto Distribution"; }

	public TruncatedParetoDistribution (double scale_, double upper_bound_,
					double shape_)
	{ this(scale_, upper_bound_, shape_, 0L); }

	public TruncatedParetoDistribution (double scale_, double upper_bound_,
					double shape_, long seed_)
	{
		this(seed_);
		scale = scale_;
		shape = -1.0 / shape_;
		upper_bound =1.0 - Math.pow(upper_bound_/scale, 1.0 / shape);
	}
	
	double upper_bound = 1.0;
	double scale = 1.0;
	double shape = - 1.0;
	
	public TruncatedParetoDistribution()
	{ super(); }

	public TruncatedParetoDistribution(long seed_)
	{ super(seed_); }

	public double getMean()
	{
		return scale / (1 + shape) 
				* (1 - Math.pow(getUpperBound()/scale, 1+1/shape));
	}
	
	public double nextDouble()
	{
		double x;
		
		do { x= r.nextDouble(); } while (x == 0.0 || x > upper_bound);

		// Mapping from Uniform to Pareto
		return scale * Math.pow(x, shape); 
	}

	public int  nextInt()
	{	return (int) nextDouble();	}

	public long nextLong()
	{	return (long) nextDouble();	}

	public void setScale(double scale_)
	{
		upper_bound = getUpperBound();
		scale = scale_;
		setUpperBound(upper_bound);
	}

	public double getScale()
	{ return scale; }
	
	public void setShape(double shape_)
	{
		upper_bound = getUpperBound();
		shape = -1.0 / shape_;
		setUpperBound(upper_bound);
	}

	public double getShape()
	{ return -1.0 / shape; }

	public void setUpperBound(double upper_bound_)
	{upper_bound = 1.0 - Math.pow(upper_bound_/scale, 1.0 / shape);}

	public double getUpperBound()
	{ return scale * Math.pow(1.0 - upper_bound, shape); }
	
	public String info(String prefix_)
	{
		return super.info(prefix_)
			+  prefix_ + "Scale = " + scale + "\n"
			+  prefix_ + "Shape = " + getShape() + "\n"
			+  prefix_ + "Upper_bound = " + getUpperBound() + "\n";
	}

	public String oneline()
	{
		return super.oneline()
			+ ", scale=" + scale
			+ ", shape=" + getShape()
			+ ", upperBound=" + getUpperBound();
	}
}

