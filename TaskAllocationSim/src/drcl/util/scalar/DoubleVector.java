// @(#)DoubleVector.java   9/2002
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

package drcl.util.scalar;

import java.util.*;
import drcl.Debug;

public class DoubleVector extends Vector implements java.io.Serializable
{
	public DoubleVector() { super(); }
	public DoubleVector(int initSize_) { super(initSize_); }
	
	public double getValueAt(int i) 
	{ 
		if (i<this.size()) return ((Double)elementAt(i)).doubleValue(); 
		else 
			Debug.error(this, "DoubleVector: Array index out of bound. size="+size()+", index="+i);
		
		return Double.NaN;
	}

	public void addValue(double d) 
	{ 
		addElement(new Double(d)); 
	}

	public void insertValueAt (double d, int index) 
	{
		insertElementAt(new Double(d), index);
	}

	public void setValueAt(double value, int i) 
	{
		Double d = new Double(value);
		if (i>=size()) this.setSize(i+1);
		setElementAt(d, i);
	}
	
	public void duplicate(Object source_)
	{
		DoubleVector that_ = (DoubleVector)source_;
		for (int i=0; i<that_.size(); i++)
			addElement(new Double(that_.getValueAt(i)));
	}
	
	public Object clone()
	{
		DoubleVector that_ = new DoubleVector(this.size());
		that_.duplicate(this);
		return that_;
	}
}
