// @(#)MapKey.java   9/2002
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
 * <code>MapKey</code> is a pair of value and mask <code>BitSet</code>s.
 * A <code>MapKey</code> <em>matches</em> a <code>BitSet</code> if the value
 * masked from the bitset by the mask of the key is equal to the masked value
 * of the key.  Also two keys are <em>matched exactly</em> if both the values
 * and masks are matched.
 * 
 * A key is said to be matched with the other key in the <em>wildcard</em>
 * manner if the value masked from the masked value of the first key by the
 * mask of the second key is equal to the value masked from the masked value
 * of the second key by the mask of the first key.
 * 
 * @see BitSet  
 * @see Map
 */
public class MapKey extends drcl.DrclObj
{
	public drcl.data.BitSet mask;
	public drcl.data.BitSet value;
	
	public MapKey() {}

	/**
	 * Creates an <code>MapKey</code> given mask and value.
	 * 
	 * @param mask_		mask.
	 * @param value_	value.
	 */
	public MapKey (drcl.data.BitSet mask_, drcl.data.BitSet value_)
	{
		set(mask_, value_);
	}
	
	/**
	 * Creates an <code>MapKey</code> given mask and value as long integers.
	 * 
	 * @param mask_		mask.
	 * @param value_	value.
	 */
	public MapKey (long mask_, long value_)
	{
		drcl.data.BitSet bsMask_  = new drcl.data.BitSet(64, mask_);
		drcl.data.BitSet bsValue_ = new drcl.data.BitSet(64, value_);
		set(bsMask_, bsValue_);
	}
	
	/**
	 * Creates an <code>MapKey</code> given mask and value as arrays of long
	 * integers.
	 * The arrays are first converted to bitsets.
	 * The least significant bit of a bit set is at bit 0 of element 0 in the
	 * array.
	 * 
	 * @param mask_		mask.
	 * @param value_	value.
	 */
	public MapKey (long[] mask_, long[] value_)
	{
		drcl.data.BitSet bsMask_  = new drcl.data.BitSet(mask_ == null?
						0: mask_.length << 6, mask_);
		drcl.data.BitSet bsValue_ = new drcl.data.BitSet(value_ == null?
						0: value_.length << 6, value_);
		set(bsMask_, bsValue_);
	}
	
	/**
	 * Set this <code>MapKey</code> with the given mask and value.
	 * 
	 * @param mask_		mask.
	 * @param value_	value.
	 */
	public void set(drcl.data.BitSet mask_, drcl.data.BitSet value_)
	{
		super.duplicate(value_);
		if (mask == null) mask = (drcl.data.BitSet)mask_.clone();
		else mask.duplicate(mask_);
		if (value == null) value = (drcl.data.BitSet)value_.clone();
		else value.duplicate(value_);
		value.and(mask_);
	}
	
	public void setValue(drcl.data.BitSet bs_) 
	{
		if (value != null) value.duplicate(bs_);
		else value = (drcl.data.BitSet)bs_.clone();
	}
	
	public drcl.data.BitSet getValue() { return value; }
	
	public void setMask(drcl.data.BitSet bs_) 
	{
		if (mask != null) mask.duplicate(bs_);
		else mask = (drcl.data.BitSet)bs_.clone();
	}
	
	public drcl.data.BitSet getMask() { return mask; }
	
	
	// 
	private void ___MATCH___() {}
	//
	
	/**
	 * Masks the value of <code>bs_</code> with the mask of this
	 * <code>MapKey</code> and returns true if the masked value is equal to
	 * the masked value of this key.
	 * 
	 * @param that_ the key to be matched.
	 * @return true if the masked value of <code>that_</code> using the mask
	 * 		of this key is equal to the masked value of this key.
	 */
	public boolean match(drcl.data.BitSet bs_)
	{
		drcl.data.BitSet value_ = (drcl.data.BitSet)bs_.clone();
		value_.and(mask);
		boolean answer_ = value.equals(value_);
		return answer_;
	}
	
	/**
	 * Returns true only if the masks and the masked values of
	 * <code>that_</code> and this key are equal.
	 * 
	 * @param that_ the key to be matched.
	 * @return true if the masks and the masked values of <code>that_</code>
	 * 		and this key are equal.
	 */
	public boolean exactMatch(MapKey that_)
	{
		return (mask == null && that_.mask == null || mask != null
				&& mask.equals(that_.mask)) && 
			   (value == null && that_.value == null || value != null
				&& value.equals(that_.value));
	}
	
	/**
	 * Returns true if the masks and the masked values of <code>that_</code>
	 * 	and this key are equal.
	 * @param that_ the key to be matched.
	 * @return true if the masks and the masked values of <code>that_</code>
	 * 	and this key are equal.
	 */
	public boolean wildcardMatch(MapKey that_)
	{
		drcl.data.BitSet value1_ = (drcl.data.BitSet)value.clone();
		value1_.and(that_.mask);
		drcl.data.BitSet value2_ = (drcl.data.BitSet)that_.value.clone();
		//value2_.and(mask);
		value2_.and(that_.mask);
		return value1_.equals(value2_);
	}
	
	//
	private void ___MISC___() {}
	//
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof MapKey)) return;
		MapKey that_ = (MapKey)source_;
		if (that_.mask == null) 
			mask = null;
		else {
			if (mask == null) mask = (drcl.data.BitSet)that_.mask.clone();
			else mask.duplicate(that_.mask);
		}
		
		if (that_.value == null) 
			value = null;
		else {
			if (value == null) value = (drcl.data.BitSet)that_.value.clone();
			else value.duplicate(that_.value);
		}
	}
		
	public boolean equals(Object obj_)
	{
		if (this == obj_) return true;
		if (!(obj_ instanceof MapKey) ) return false;
		MapKey that_ = (MapKey)obj_;
		return (mask == null && that_.mask == null
						|| mask != null && mask.equals(that_.mask)) && 
			   (value == null && that_.value == null || value != null
				&& value.equals(that_.value));
	}
		
	public int hashCode()
	{
		return value.hashCode();
	}
	
	/**
	 * Prints out the value and the mask of this map key by
	 * the set of indices of the bits of 1's in the value and the mask.
	 * @see BitSet#toString().
	 */
	public String toString()
	{
		return value + "(" + mask + ")";
	}
	
	/** Returns the long integer representation of this map key.  */
	public String numberRepresentation()
	{
		return value.numberRepresentation() 
			   + "(" + mask.numberRepresentation() + ")";
	}

	/** Returns the binary representation of this map key.  */
    public String binaryRepresentation() 
	{ return binaryRepresentation(false); }
	
 	/**
	 * Returns the binary representation of this map key.
	 * @param skipLeadingZeros_ if true, the leading zeros in the resulting
	 * 		binary represenation is not printed.
	 * @see BitSet#binaryRepresentation(boolean).
	 */
    public String binaryRepresentation(boolean skipLeadingZeros_) 
	{
		return "$" + value.binaryRepresentation(skipLeadingZeros_) 
			   + "($" + mask.binaryRepresentation(skipLeadingZeros_) + ")";
	}
	
	/**
	 * Returns the binary representation of the value and the mask of the map
	 * key in the specified number of binary characters.
	 * It stuffs zeros if the value or the mask is not large enough.
	 * The length of the result may exceed the specified length if the value
	 * or mask is too large.
	 * @param length_  expected number of binary characters.
	 * @see BitSet#binaryRepresentation(int).
	 */
    public String binaryRepresentation(int length_) 
	{
		return "$" + value.binaryRepresentation(length_) 
			   + "($" + mask.binaryRepresentation(length_) + ")";
	}
		
	/** Returns the hex representation of this map key. */
    public String hexRepresentation() 
	{ return hexRepresentation(false); }
	
 	/**
	 * Returns the hex representation of this map key.
	 * @param skipLeadingZeros_ if true, the leading zeros in the resulting
	 * 		binary represenation is not printed.
	 * @see BitSet#hexRepresentation(boolean).
	 */
   public String hexRepresentation(boolean skipLeadingZeros_) 
	{
		return "#" + value.hexRepresentation(skipLeadingZeros_) 
			   + "(#" + mask.hexRepresentation(skipLeadingZeros_) + ")";
	}
	
	/**
	 * Returns the hex representation of the value and the mask of the map key
	 * in the specified number of hex characters.
	 * It stuffs zeros if the value or the mask is not large enough.
	 * The length of the result may exceed the specified length if the value
	 * or mask is too large.
	 * @param length_  expected number of hex characters.
	 * @see BitSet#hexRepresentation(int).
	 */
    public String hexRepresentation(int length_) 
	{
		return "#" + value.hexRepresentation(length_) 
			   + "(#" + mask.hexRepresentation(length_) + ")";
	}
}
