// @(#)BooleanObj.java   9/2002
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

package drcl.data;

/**
 * Class that is similar to <code>Boolean</code> but is mutable.
 */
public class BooleanObj extends drcl.DrclObj
{
	public boolean value;

	public BooleanObj() {}
	public BooleanObj(boolean v)
	{	value = v;	}

	public void setValue(boolean v) { this.value = v; }
	public boolean  getValue()      { return value; }

	public boolean equals(Object o)
	{
		if (this == o) return true;
		else if (!(o instanceof BooleanObj)) return false;
		return value == ((BooleanObj)o).value;
	}

	public int hashCode() { return value? 1:0; }
	
	public void duplicate(Object source_)
	{
		value = ((BooleanObj)source_).value;
	}
	
	/** Flips the boolean value and returns the new one.  */
	public boolean flip()
	{ 
		value = !value; 
		return value; 
	}
	
	public String toString() { return value+""; }
}

