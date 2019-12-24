// @(#)LongObj.java   7/2003
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

package drcl.data;

/**
 * Class that is similar to <code>Long</code> but is mutable.
 */
public class LongObj extends NumberObj
{
	public long value;

	public LongObj() {}
	public LongObj(long v)
	{	value = v;	}

	public void setValue(long v) { value = v; }
	public long  getValue()      { return value; }

	public boolean equals(Object o)
	{
		if (this == o) return true;
		else if (!(o instanceof LongObj)) return false;
		return value == ((LongObj)o).value;
	}

	public int hashCode() { return (int)value; }
	
	public void duplicate(Object source_)
	{
		value = ((LongObj)source_).value;
	}
	
	
	/** Increases the long by delta_ and returns the new value. */
	public long inc(long delta_)
	{ 
		value += delta_; 
		return value;
	}
	
	public String toString() { return value+""; }
	public byte byteValue() { return (byte)value; }
	public float floatValue() { return (float)value; }
	public double doubleValue() { return (double)value; }
	public int intValue() { return (int)value; }
	public long longValue() { return (long)value; }
	public short shortValue() { return (short)value; }
}

