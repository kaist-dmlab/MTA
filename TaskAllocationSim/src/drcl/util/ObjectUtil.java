// @(#)ObjectUtil.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.util;

import java.util.*;
import java.lang.reflect.*;

public class ObjectUtil
{
	/**
	 * Returns the clone of <code>o_</code>.
	 * It calls <code>o_.clone()</code>, if possible, and
	 * returns null if fails.
	 */
	public static Object clone(Object o_)
	{ return clone(o_, false);	}
	
	/**
	 * Returns the clone of <code>o_</code>.
	 * It calls <code>o_.clone()</code>, if possible, and
	 * either raises an exception or returns null if fails.
	 * 
	 * @param raiseException_ set to true if one wishes to get exception
	 *	instead of receiving null when this method fails to call
	 *  <code>o_.clone()</code>.
	 */
	public static Object clone(Object o_, boolean raiseException_)
	{
		if (o_ == null) return null;
		if (o_ instanceof String) return o_;
		
		try {
			if (o_ instanceof drcl.ObjectCloneable)
				return ((drcl.ObjectCloneable)o_).clone();
			if (o_.getClass().isArray()) {
				int length_ = Array.getLength(o_);
				Class componentType_ = o_.getClass().getComponentType();
				Object that_ = Array.newInstance(componentType_, length_);
				if (componentType_.isPrimitive())
					System.arraycopy(o_, 0, that_, 0, length_);
				else {
					for (int i=0; i<length_; i++)
						Array.set(that_, i, clone(Array.get(o_, i), raiseException_));
				}
				return that_;
			}
			Method m_ = o_.getClass().getMethod("clone", null);
			return m_.invoke(o_, null);
		}
		catch (Exception e_) {
			if (raiseException_) {
				Thread t_ = Thread.currentThread();
				t_.getThreadGroup().uncaughtException(t_, e_);
			}
			return null;
		}
	}
	
	/**
	 * Returns true if <code>o1_</code> is equal to <code>o2_</code>.
	 * It is mainly for comparing arrays of objects.
	 * The <code>equals()</code> of a Java array does not compare
	 * the content of array.  This method checks the content (recursively)
	 * in the array and returns true if all the content are equal under
	 * this method.
	 * 
	 * If the arguments are not array, then the method simply returns
	 * <code>o1_.equals(o2_)</code>.
	 */
	public static boolean equals(Object o1_, Object o2_)
	{
		if (o1_ == null && o2_ == null) return true;
		if (o1_ == null ^ o2_ == null) return false;
		
		if (o1_ instanceof Object[]) {
			if (!(o2_ instanceof Object[])) return false;
			Object[] a1_ = (Object[])o1_;
			Object[] a2_ = (Object[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (!equals(a1_[i], a2_[i])) return false;
			return true;
		}
		if (o1_ instanceof long[]) {
			if (!(o2_ instanceof long[])) return false;
			long[] a1_ = (long[])o1_;
			long[] a2_ = (long[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof int[]) {
			if (!(o2_ instanceof int[])) return false;
			int[] a1_ = (int[])o1_;
			int[] a2_ = (int[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof boolean[]) {
			if (!(o2_ instanceof boolean[])) return false;
			boolean[] a1_ = (boolean[])o1_;
			boolean[] a2_ = (boolean[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof double[]) {
			if (!(o2_ instanceof double[])) return false;
			double[] a1_ = (double[])o1_;
			double[] a2_ = (double[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof byte[]) {
			if (!(o2_ instanceof byte[])) return false;
			byte[] a1_ = (byte[])o1_;
			byte[] a2_ = (byte[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof short[]) {
			if (!(o2_ instanceof short[])) return false;
			short[] a1_ = (short[])o1_;
			short[] a2_ = (short[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		if (o1_ instanceof float[]) {
			if (!(o2_ instanceof float[])) return false;
			float[] a1_ = (float[])o1_;
			float[] a2_ = (float[])o2_;
			if (a1_.length != a2_.length) return false;
			for (int i=0; i<a1_.length; i++)
				if (a1_[i] !=  a2_[i]) return false;
			return true;
		}
		return o1_.equals(o2_);
	}
}
