// @(#)LongVector.java   9/2002
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

import drcl.Debug;

public class LongVector extends drcl.DrclObj
{
	int size;  // size from user's point of view
	int inc;   // increment size
	long[] value;
	
	public LongVector()
	{
		value = new long[10];
		size = 0;
		inc = 10;
	}
	
	public LongVector(int initSize, int increment)
	{
		value = new long[initSize];
		size = 0;
		inc  = increment;
	}
	
	public void addValue(long d) 
	{ 
		if (size + 1 < value.length)
			value[size++] = d;
		else {
			int enlarge = (inc <= 0)? 1: inc;
			long[] tmp = new long[value.length + enlarge];
			System.arraycopy(value, 0, tmp, 0, value.length);
			tmp[size++] = d;
			value = tmp;
		}
	}

	public void insertValueAt (long d, int index)
	{
		if (index < 0)
			throw new IndexOutOfBoundsException(index + " < 0");
		
		if (index >= value.length)
			setSize(index + 1);
		
		if (index >= size) {
			value[index] = d;
			size = index + 1;
		}
		else {
			for (int i=size-1; i>=index; i--)
				value[i+1] = value[i];
			value[index] = d;
			size ++;
		}
	}
	
	public void removeValueAt (int index)
	{
		if (index < 0)
			throw new IndexOutOfBoundsException(index + " < 0");
		
		if (index >= size) 
			throw new IndexOutOfBoundsException(index + " >= " + size);

		for (int i=index+1; i<size; i++)
			value[i-1] = value[i];
		
		setSize(size - 1);
	}
	
	// Remove first integer = val, use with caution.
	public void removeValue (long val) {
		for (int i=0; i<size; i++) {
			if (value[i] == val) {
				removeValueAt(i);
				return;
			}
		}
	}

	public void setValueAt(long d, int pos)
	{
		if (pos >= size)
			throw new IndexOutOfBoundsException(pos + " >= " + size);
		
		value[pos] = d;
	}

	public long getValueAt(int i) 
	{ 
		if (i >= size)
			throw new IndexOutOfBoundsException(i + " >= " + size);
		
		return value[i]; 
	}

	/**
	 * Returns the index of the first occurance.
	 */
	public int indexOf(long value_)
	{
		for (int i=0; i<size; i++)
			if (value[i] == value_) return i;
		return -1;
	}

	public void setSize(int newSize) 
	{
		if (newSize == size) return;
		if (newSize < size) { size = newSize; return; }
		if (newSize <= value.length) { size = newSize; return; }
		int ns = (inc <= 0)? newSize:
							 value.length + (newSize - value.length + inc - 1) / inc * inc;
		long[] tmp = new long[ns];
		System.arraycopy(value, 0, tmp, 0, value.length);
		
		value = tmp;
	}
	
	public int size() { return size; }
	
	public long[] getLongs()
	{
		long[] longs = new long[size];
		System.arraycopy(value, 0, longs, 0, value.length);
		return longs;
	}

	public void duplicate(Object source_)
	{
		IntVector that_ = (IntVector)source_;
		size = that_.size;
		inc = that_.inc;
		if (that_.value != null) {
			value = new long[that_.value.length];
			System.arraycopy(that_.value, 0, value, 0, value.length);
		}
	}

	public void removeAll()
	{ size = 0; }
	
	public String toString()
	{
		if (size == 0) return "()";
		StringBuffer sb_ = new StringBuffer("(" + value[0]);
		for (int i=1; i<size; i++)
			sb_.append("," + value[0]);
		return sb_ + ")";
	}
}
