// @(#)BitSet.java   9/2002
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

import java.io.*;

/**
 * This class implements a set of bits that grows as needed. Each 
 * bit of the set has a <code>boolean</code> value. The 
 * bits of a <code>BitSet</code> are indexed by nonnegative integers. 
 * Individual indexed bit can be examined, set, or cleared. One 
 * <code>BitSet</code> may be used to modify the contents of another 
 * <code>BitSet</code> through logical AND, logical inclusive OR, and 
 * logical exclusive OR operations.
 * <p>
 * By default, all bits in the set initially have the value 
 * <code>false</code>. 
 * <p>
 * Every bit set has a size, which is the number of bits in use 
 * by the bit set. Note that the actual size is the actual number of bits
 * that the bit set is able to represent.  The size as well as the actual
 * size is changed when the bit index of an operation exceeds the sizes.
 * Also the size can be set explicitly with <code>setSize(int)</code>.
 * The most significant bits are truncated when a smaller size is set.
 *
 * Bits are packed into arrays of subsets(<code>long</code>s).  Each subset
 * is indexed with nonnegative integers.  Index 0 refers to the subset
 * containing
 * the first 64 least significant bits. BitSet operations also apply to subsets.
 */
public class BitSet extends drcl.DrclObj 
{
    /*
     */
    //private final static int ADDRESS_BITS_PER_UNIT = 6;
    //private final static int BITS_PER_UNIT = 1 << ADDRESS_BITS_PER_UNIT;
    //private final static int BIT_INDEX_MASK = BITS_PER_UNIT - 1;

    /**
     * The subset in this BitSet.  The ith bit is stored in subset[i/64] at
     * bit position i % 64 (where bit position 0 refers to the least
     * significant bit and 63 refers to the most significant bit).
     *
     * @serial
     */
    protected long subset[];

	protected int nb; // # of bits in use (user-defined)
    protected int nsubsets; //# of subsets in use (from nb)
	protected int nbset; // # of set bits

	int[] indices; // indices of set bits
	int[] uindices; // indices of unset bits

	public BitSet (int size_, long value_)
	{
		this(size_);
		set(0, value_);
	}
	
	/**
	 * Creates and returns a bit set of the specified size given an array
	 * of long integers.  Bit 0 of <code>values_[0]</code> is stored
	 * as the least significant bit, and bit 63 of <code>values_[size-1]</code>
	 * as the most significant bit.
	 */
	public BitSet (int size_, long[] values_)
	{
		this(size_);
		if (values_ != null)
			for (int i=0; i<values_.length; i++)
				set(i, values_[i]);
	}
	
	/**
	 * Creates and returns a bit set in which the bits of the positions
	 * specified in <code>set_</code> are set to 1's.
	 * The length of the bit set is set to the maximum value in
	 * <code>set_</code>.
	 */
	public BitSet (int[] set_)
	{
		this(0);
		if (set_ != null) {
			int maxIndex_ = -1;
			for (int i=0; i<set_.length; i++)
				if (set_[i] > maxIndex_) maxIndex_ = set_[i];
			setSize(maxIndex_ + 1);
			set(set_);
		}
	}
	
    /**
     * Creates a new bit set. All subset are initially <code>false</code>.
     */
    public BitSet() 
	{this(64); }

    /**
     * Creates a bit set whose initial size is large enough to explicitly
     * represent subset with indices in the range <code>0</code> through
     * <code>nbits-1</code>. All subset are initially <code>false</code>. 
     *
     * @param     nbits   the initial size of the bit set.
     * @exception NegativeArraySizeException if the specified initial size
     *               is negative.
     */
    public BitSet(int nbits_) 
	{
		/* nbits can't be negative; size 0 is OK */
		if (nbits_ < 0)
		    throw new NegativeArraySizeException(Integer.toString(nbits_));

		nb = nbits_;
		nsubsets = (nb + 63) >> 6;
		subset = new long[nsubsets];
    }

    /**
     * Returns the "logical size" of this <code>BitSet</code>.
     *
     * @return  the logical size of this <code>BitSet</code>.
     */
    public int getSize() {   return nb;  }

	/**
	 * If <code>size_</code> is larger than the current size,
	 * the bit set expands from the most significant bit and 
	 * the new bits are cleared (set to false).
	 */
	public void setSize(int size_)
	{
		int nb_, nsubsets_;
		if (size_ > nb) {
			nb_ = nb;
			nsubsets_ = nsubsets;
			nb = size_;
			nsubsets = (nb + 63) >> 6;
			/*
			if (debug)
			System.out.println("old nb=" + nb_ + ",old nsubsets=" + nsubsets_
							+ ", nb=" + nb + ",nsubsets=" + nsubsets 
							+ "subset.length=" + subset.length);
			*/
			if (nsubsets > subset.length) enlarge(nsubsets);
			// clear the new bits
			for (int i=nsubsets_; i<nsubsets; i++) set(i, 0L);
			/*
			int nbits_ = nb_ % 64;
			if (nbits_ > 0) {
				long k = 1L;
				for (int i=1; i<nbits_; i++) k = (k << 1) | 1L;
				subset[nsubsets_ - 1] &= k;
			}
			*/
		}
		else {
			nb = nb_ = size_;
			nsubsets = nsubsets_ = (nb + 63) >> 6;
			//nbset();
			nbset = -1;
		}
		// clear the new/unused bits
		int nbits_ = nb_ % 64;
		if (nbits_ > 0)
			subset[nsubsets_ - 1] &= (1L << nbits_) - 1;
	}
	
	/**
	 * Returns the number of bits that are set true in this bit set.
	 */
	public int getNumSetBits()
	{
		if (nbset < 0) nbset(); // update it
		return nbset;
	}

	// XXX: this could speed up getSetBitIndices() and getUnsetBitIndices...
	// unfinished
	static int[][] INDICES = {
		{},
		{0},
		{1},
		{0,1},
		{2},
		{0,2},
		{1,2},
		{0,1,2},
		{3},
		{0,3},
		{1,3},
		{0,1,3},
		{2,3},
		{0,2,3},
		{1,2,3},
		{0,1,2,3},
		{4},
		{0,4},
		{1,4},
		{0,1,4},
		{2,4},
		{0,2,4},
		{1,2,4},
		{0,1,2,4},
		{3,4},
		{0,3,4},
		{1,3,4},
		{0,1,3,4},
		{2,3,4},
		{0,2,3,4},
		{1,2,3,4},
		{0,1,2,3,4},
		{5},
		{0,5},
		{1,5},
		{0,1,5},
		{2,5},
		{0,2,5},
		{1,2,5},
		{0,1,2,5},
		{3,5},
		{0,3,5},
		{1,3,5},
		{0,1,3,5},
		{2,3,5},
		{0,2,3,5},
		{1,2,3,5},
		{0,1,2,3,5},
		{4,5},
		{0,4,5},
		{1,4,5},
		{0,1,4,5},
		{2,4,5},
		{0,2,4,5},
		{1,2,4,5},
		{0,1,2,4,5},
		{3,4,5},
		{0,3,4,5},
		{1,3,4,5},
		{0,1,3,4,5},
		{2,3,4,5},
		{0,2,3,4,5},
		{1,2,3,4,5},
		{0,1,2,3,4,5},
		{6},
		{0,6},
		{1,6},
		{0,1,6},
		{2,6},
		{0,2,6},
		{1,2,6},
		{0,1,2,6},
		{3,6},
		{0,3,6},
		{1,3,6},
		{0,1,3,6},
		{2,3,6},
		{0,2,3,6},
		{1,2,3,6},
		{0,1,2,3,6},
		{4,6},
		{0,4,6},
		{1,4,6},
		{0,1,4,6},
		{2,4,6},
		{0,2,4,6},
		{1,2,4,6},
		{0,1,2,4,6},
		{3,4,6},
		{0,3,4,6},
		{1,3,4,6},
		{0,1,3,4,6},
		{2,3,4,6},
		{0,2,3,4,6},
		{1,2,3,4,6},
		{0,1,2,3,4,6},
		{5,6},
		{0,5,6},
		{1,5,6},
		{0,1,5,6},
		{2,5,6},
		{0,2,5,6},
		{1,2,5,6},
		{0,1,2,5,6},
		{3,5,6},
		{0,3,5,6},
		{1,3,5,6},
		{0,1,3,5,6},
		{2,3,5,6},
		{0,2,3,5,6},
		{1,2,3,5,6},
		{0,1,2,3,5,6},
		{4,5,6},
		{0,4,5,6},
		{1,4,5,6},
		{0,1,4,5,6},
		{2,4,5,6},
		{0,2,4,5,6},
		{1,2,4,5,6},
		{0,1,2,4,5,6},
		{3,4,5,6},
		{0,3,4,5,6},
		{1,3,4,5,6},
		{0,1,3,4,5,6},
		{2,3,4,5,6},
		{0,2,3,4,5,6},
		{1,2,3,4,5,6},
		{0,1,2,3,4,5,6},
		{7},
		{0,7},
		{1,7},
		{0,1,7},
		{2,7},
		{0,2,7},
		{1,2,7},
		{0,1,2,7},
		{3,7},
		{0,3,7},
		{1,3,7},
		{0,1,3,7},
		{2,3,7},
		{0,2,3,7},
		{1,2,3,7},
		{0,1,2,3,7},
		{4,7},
		{0,4,7},
		{1,4,7},
		{0,1,4,7},
		{2,4,7},
		{0,2,4,7},
		{1,2,4,7},
		{0,1,2,4,7},
		{3,4,7},
		{0,3,4,7},
		{1,3,4,7},
		{0,1,3,4,7},
		{2,3,4,7},
		{0,2,3,4,7},
		{1,2,3,4,7},
		{0,1,2,3,4,7},
		{5,7},
		{0,5,7},
		{1,5,7},
		{0,1,5,7},
		{2,5,7},
		{0,2,5,7},
		{1,2,5,7},
		{0,1,2,5,7},
		{3,5,7},
		{0,3,5,7},
		{1,3,5,7},
		{0,1,3,5,7},
		{2,3,5,7},
		{0,2,3,5,7},
		{1,2,3,5,7},
		{0,1,2,3,5,7},
		{4,5,7},
		{0,4,5,7},
		{1,4,5,7},
		{0,1,4,5,7},
		{2,4,5,7},
		{0,2,4,5,7},
		{1,2,4,5,7},
		{0,1,2,4,5,7},
		{3,4,5,7},
		{0,3,4,5,7},
		{1,3,4,5,7},
		{0,1,3,4,5,7},
		{2,3,4,5,7},
		{0,2,3,4,5,7},
		{1,2,3,4,5,7},
		{0,1,2,3,4,5,7},
		{6,7},
		{0,6,7},
		{1,6,7},
		{0,1,6,7},
		{2,6,7},
		{0,2,6,7},
		{1,2,6,7},
		{0,1,2,6,7},
		{3,6,7},
		{0,3,6,7},
		{1,3,6,7},
		{0,1,3,6,7},
		{2,3,6,7},
		{0,2,3,6,7},
		{1,2,3,6,7},
		{0,1,2,3,6,7},
		{4,6,7},
		{0,4,6,7},
		{1,4,6,7},
		{0,1,4,6,7},
		{2,4,6,7},
		{0,2,4,6,7},
		{1,2,4,6,7},
		{0,1,2,4,6,7},
		{3,4,6,7},
		{0,3,4,6,7},
		{1,3,4,6,7},
		{0,1,3,4,6,7},
		{2,3,4,6,7},
		{0,2,3,4,6,7},
		{1,2,3,4,6,7},
		{0,1,2,3,4,6,7},
		{5,6,7},
		{0,5,6,7},
		{1,5,6,7},
		{0,1,5,6,7},
		{2,5,6,7},
		{0,2,5,6,7},
		{1,2,5,6,7},
		{0,1,2,5,6,7},
		{3,5,6,7},
		{0,3,5,6,7},
		{1,3,5,6,7},
		{0,1,3,5,6,7},
		{2,3,5,6,7},
		{0,2,3,5,6,7},
		{1,2,3,5,6,7},
		{0,1,2,3,5,6,7},
		{4,5,6,7},
		{0,4,5,6,7},
		{1,4,5,6,7},
		{0,1,4,5,6,7},
		{2,4,5,6,7},
		{0,2,4,5,6,7},
		{1,2,4,5,6,7},
		{0,1,2,4,5,6,7},
		{3,4,5,6,7},
		{0,3,4,5,6,7},
		{1,3,4,5,6,7},
		{0,1,3,4,5,6,7},
		{2,3,4,5,6,7},
		{0,2,3,4,5,6,7},
		{1,2,3,4,5,6,7},
		{0,1,2,3,4,5,6,7},
/*
		{},
		{0},
		{1},
		{0,1},
		{2},
		{0,2},
		{1,2},
		{0,1,2},
		{3},
		{0,3},
		{1,3},
		{0,1,3},
		{2,3},
		{0,2,3},
		{1,2,3},
		{0,1,2,3},
		*/
	};
	
	/**
	 * Returns the array of indices of the bits that are set true in this bit
	 * set.
	 * 
	 * @return the array of indices of the set bits.
	 */
	public int[] getSetBitIndices() 
	{
		if (nbset < 0) nbset(); // update it
		if (indices != null) {
			int[] indices_ = new int[nbset];
			System.arraycopy(indices, 0, indices_, 0, nbset);
			return indices_;
		}
		int[] indices_ = new int[nbset];
		/* use get(i)
		int j = 0;
		for (int i=0; i<nb; i++) 
			if (get(i)) indices_[j++] = i;
		*/
		/* moves get() code here and simplifies it, not much improvement
		int j = 0;
		int index_ = 0;
		for (int i=0; i<subset.length; i++) {
			for (long mask_ = 1L; mask_ != 0; mask_ <<= 1) {
				if ((subset[i] & mask_) != 0) indices_[j++] = index_;
				index_++;
			}
		}
		*/
		// this is much better, use only 1/8 of the time for the above
		int j = 0, shift_ = 0;
		//if (debug) System.out.println("nsubsets = " + nsubsets);
		for (int i=0; i<nsubsets; i++) {
			long subset_ = subset[i];
			for (int k=0; k<64; k+=8) {
				int[] tmp_ = INDICES[(int)((subset_ >> k) & 0x0FFL)];
				if (tmp_.length > 0) {
					/*
					if (debug)
						System.out.println(shift_ + " >> "
									+ drcl.util.StringUtil.toString(
									tmp_, ",", Integer.MAX_VALUE));
					*/
					try {
					System.arraycopy(tmp_, 0, indices_, j, tmp_.length);
					}
					catch (ArrayIndexOutOfBoundsException e_) {
						e_.printStackTrace();
						System.out.println("subset_=" + subset_ + ", " + shift_ + " >> "
									+ drcl.util.StringUtil.toString(
									tmp_, ",", Integer.MAX_VALUE));
						System.out.println("nb=" + nb + ", j=" + j + ", nbset=" + nbset);

					}
					int m = j;
					j += tmp_.length;
					for (; m < j; m++) indices_[m] += shift_;
					/*
					if (debug)
						System.out.println(drcl.util.StringUtil.toString(
									indices_, ",", Integer.MAX_VALUE));
					*/
				}
				shift_ += 8;
			}
		}
		indices = new int[nbset];
		System.arraycopy(indices_, 0, indices, 0, nbset);
		return indices_;
	}
	
	//public static boolean debug = false;

	/**
	 * Returns the array of indices of the bits that are set false in this bit 
	 * set.
	 * 
	 * @return the array of indices of the unset bits.
	 */
	public int[] getUnsetBitIndices() 
	{
		/*
		if (nbset < 0) nbset(); // update it
		int[] indices_ = new int[getSize() - nbset];
		int j = 0;
		for (int i=0; i<nb; i++) 
			if (!get(i)) indices_[j++] = i;
		return indices_;
		*/
		/*
		BitSet clone_ = (BitSet)clone();
		clone_.not();
		return clone_.getSetBitIndices();
		*/
		if (nbset < 0) nbset(); // update it
		if (uindices != null) {
			int[] indices_ = new int[uindices.length];
			System.arraycopy(uindices, 0, indices_, 0, uindices.length);
			return indices_;
		}
		BitSet clone_ = (BitSet)clone();
		clone_.not();
		int[] indices_ = clone_.getSetBitIndices();
		uindices = new int[indices_.length];
		System.arraycopy(indices_, 0, uindices, 0, indices_.length);
		return indices_;
	}
	
    /**
     * Returns the number of bits actually used by this 
     * <code>BitSet</code> to represent bit values. 
     * The maximum element in the set is the size - 1st element.
     *
     * @return  the number of bits currently in this <code>BitSet</code>.
     */
    public int getActualSize() { return subset.length << 6;   }

	//
	void ___BIT_OP___() {}
	//
	
 	/**
	 * Sets all the bits.
	 */
	public void set()
	{
		for (int i=0; i<nsubsets; i++)
			subset[i] |= -1L;
		nbset = nb;
		indices = null;
		uindices = null;
	}

   /**
     * Sets the bit specified by the index to <code>true</code>.
     *
     * @param     bitIndex_   a bit index.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void set(int bitIndex_) 
	{
		if (bitIndex_ < 0)
		    throw new IndexOutOfBoundsException(Integer.toString(bitIndex_));

	    int subsetIndex_ = bitIndex_ >> 6;

		if (bitIndex_ >= nb) {
			setSize(bitIndex_ + 1);
			/*
			nb = bitIndex_ + 1;
			if (nsubsets <= subsetIndex_) {
				nsubsets = subsetIndex_ + 1;
				enlarge(nsubsets);
			}
			*/
		}
		long mask_ = 1L << (bitIndex_ & 63);
		if ((subset[subsetIndex_] & mask_) == 0) {
			subset[subsetIndex_] |= mask_;
			if (nbset >= 0) nbset ++;
			indices = null;
			uindices = null;
		}
	}
	
   /**
     * Sets the bits specified by the indices to <code>true</code>.
     *
     * @param     bitIndices_   array of bit indices.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void set(int[] bitIndices_) 
	{
		for (int i=0; i<bitIndices_.length; i++) {
			int bitIndex_ = bitIndices_[i];
			if (bitIndex_ < 0)
			    throw new IndexOutOfBoundsException(Integer.toString(bitIndex_));

			int subsetIndex_ = bitIndex_ >> 6;

			if (bitIndex_ >= nb) {
				setSize(bitIndex_ + 1);
				/*
				nb = bitIndex_ + 1;
				if (nsubsets <= subsetIndex_) {
					nsubsets = subsetIndex_ + 1;
					enlarge(nsubsets);
				}
				*/
			}
			long mask_ = 1L << (bitIndex_ & 63);
			if ((subset[subsetIndex_] & mask_) == 0) {
				subset[subsetIndex_] |= mask_;
				if (nbset >= 0) nbset ++;
				indices = null;
				uindices = null;
			}
		}
	}
	
     /**
     * Sets the subset specified by the index to the argument subset.
     * This operation does not change the size of the bit set.
     *
     * @param     subsetIndex_ the subset index.
     * @param     subset_ the subset with which to mask the corresponding 
     *            subset in this <code>BitSet</code>.
     */
    public void set(int subsetIndex_, long subset_) 
	{
		if (subsetIndex_ >= subset.length) {
			setSize((subsetIndex_ + 1) << 6);
			//enlarge(subsetIndex_ + 1);
		}
		subset[subsetIndex_] = subset_;
		//nbset();
		nbset = -1;
    }

 	/**
	 * Clears all the bits.
	 */
	public void clear()
	{
		for (int i=0; i<nsubsets; i++)
			subset[i] = 0;
		nbset = 0;
		indices = null;
		uindices = null;
	}

   /**
     * Sets the bit specified by the index to <code>false</code>.
     *
     * @param     bitIndex_   the index of the bit to be cleared.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void clear(int bitIndex_) 
	{
		if (bitIndex_ < 0)
		    throw new IndexOutOfBoundsException(Integer.toString(bitIndex_));
		
		if (bitIndex_ >= nb) return;
		int subsetIndex_ = bitIndex_ >> 6;
		long mask_ = 1L << (bitIndex_ & 63);
		if ((subset[subsetIndex_] & mask_) != 0) {
			subset[subsetIndex_] &= ~mask_;
			if (nbset >= 0) nbset --;
			indices = null;
			uindices = null;
		}
    }

   /**
     * Sets the bits specified by the indices to <code>false</code>.
     *
     * @param     bitIndices_   array of bit indices.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public void clear(int[] bitIndices_) 
	{
		for (int i=0; i<bitIndices_.length; i++) {
			int bitIndex_ = bitIndices_[i];
			if (bitIndex_ < 0)
			    throw new IndexOutOfBoundsException(
								Integer.toString(bitIndex_));
		
			if (bitIndex_ >= nb) return;
			int subsetIndex_ = bitIndex_ >> 6;
			long mask_ = 1L << (bitIndex_ & 63);
			if ((subset[subsetIndex_] & mask_) != 0) {
				subset[subsetIndex_] &= ~mask_;
				if (nbset >= 0) nbset --;
				indices = null;
				uindices = null;
			}
		}
    }

    /**
     * Clears all of the subset in this <code>BitSet</code> whose corresponding
     * bit is set in the specified <code>BitSet</code>.
     *
     * @param     set_ the <code>BitSet</code> with which to mask this
     *            <code>BitSet</code>.
     */
    public void clearBy(BitSet set_) 
	{
		int nsubsets_ = nsubsets > set_.nsubsets? set_.nsubsets: nsubsets;

		// perform logical (a & !b) on subset in common
        for (int i=0; i<nsubsets_; i++) 
			subset[i] &= ~set_.subset[i];
		
		//nbset();
		nbset = -1;
    }

     /**
     * Clears the subset in this <code>BitSet</code> whose corresponding
     * bit is set in the specified subset.
     * This operation does not change the size of the bit set.
     *
     * @param     subsetIndex_ the subset index.
     * @param     subset_ the subset with which to mask the corresponding 
     *            subset in this <code>BitSet</code>.
     */
    public void clearBy(int subsetIndex_, long subset_) 
	{
		if (subsetIndex_ >= subset.length) enlarge(subsetIndex_ + 1);
		// perform logical (a & !b) on subset in common
		subset[subsetIndex_] &= ~subset_;
		//nbset();
		nbset = -1;
    }

   /**
     * Returns the value of the bit with the specified index. The value 
     * is <code>true</code> if the bit with the index <code>bitIndex_</code> 
     * is currently set in this <code>BitSet</code>; otherwise, the result 
     * is <code>false</code>.
     *
     * @param     bitIndex_   the bit index.
     * @return    the value of the bit with the specified index.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
    public boolean get(int bitIndex_) 
	{
		if (bitIndex_ < 0)
		    throw new IndexOutOfBoundsException(Integer.toString(bitIndex_));

		if (bitIndex_ >= nb) return false;
		int subsetIndex_ = bitIndex_ >> 6;
		return ((subset[subsetIndex_] & (1L << (bitIndex_ & 63))) != 0);
    }

     /**
     * Returns the subset specified by the index to the argument subset.
     *
     * @param     subsetIndex_ the subset index.
     * @return	  the subset withe the specified index.
     * @exception IndexOutOfBoundsException if the specified index is negative.
     */
	public long getSubset(int subsetIndex_)
	{
		if (subsetIndex_ < 0)
			throw new IndexOutOfBoundsException(Integer.toString(subsetIndex_));
		
		if (subsetIndex_ >= subset.length) return 0;
		else return subset[subsetIndex_];
	}
	
    /**
     * Performs a logical <b>AND</b> of this target bit set with the 
     * argument bit set. This bit set is modified so that each bit in it 
     * has the value <code>true</code> if and only if it both initially 
     * had the value <code>true</code> and the corresponding bit in the 
     * bit set argument also had the value <code>true</code>. 
     *
     * @param   set_   a bit set. 
     */
    public void and(BitSet set_) 
	{
		if (set_ == this) return;
		
		int nsubsets_ = nsubsets > set_.nsubsets? set_.nsubsets: nsubsets;
		int i;
		for(i=0; i<nsubsets_; i++)  subset[i] &= set_.subset[i];
		for (; i<nsubsets; i++) subset[i] = 0;
		//nbset();
		nbset = -1;
    }
	
     /**
     * Performs a logical <b>AND</b> of the target subset with the 
     * argument subset. This subset is modified so that each bit in it 
     * has the value <code>true</code> if and only if it both initially 
     * had the value <code>true</code> and the corresponding bit in the 
     * subset argument also had the value <code>true</code>. 
     * This operation does not change the size of the bit set.
     *
     * @param     subsetIndex_ the subset index.
     * @param     subset_ the subset with which to mask the corresponding 
     *            subset in this <code>BitSet</code>.
     */
    public void and(int subsetIndex_, long subset_) 
	{
		if (subsetIndex_ >= subset.length) enlarge(subsetIndex_ + 1);
		// perform logical (a & !b) on subset in common
		subset[subsetIndex_] &= subset_;
		//nbset();
		nbset = -1;
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set 
     * argument. This bit set is modified so that a bit in it has the 
     * value <code>true</code> if and only if it either already had the 
     * value <code>true</code> or the corresponding bit in the bit set 
     * argument has the value <code>true</code>.
     * This operation does not change the size of the bit set.
     *
     * @param   set_   a bit set.
     */
    public void or(BitSet set_) 
	{
		if (set_ == this) return;
		int nsubsets_ = nsubsets > set_.nsubsets? set_.nsubsets: nsubsets;
		if (nb < set_.nb) {
			nb = set_.nb;
			nsubsets = (nb + 63) >> 6;
			if (nsubsets > subset.length) enlarge(nsubsets);
		}
		int i;
		for(i=0; i<nsubsets_; i++)  subset[i] |= set_.subset[i];
		if (i < set_.nsubsets)
			for (; i<nsubsets; i++) subset[i] = set_.subset[i];
		//nbset();
		nbset = -1;
    }

     /**
     * Performs a logical <b>OR</b> of the target subset with the 
     * argument subset. This subset is modified so that each bit in it 
     * has the value <code>true</code> if and only if it both initially 
     * had the value <code>true</code> or the corresponding bit in the 
     * subset argument also had the value <code>true</code>. 
     *
     * @param     subsetIndex_ the subset index.
     * @param     subset_ the subset with which to mask the corresponding 
     *            subset in this <code>BitSet</code>.
     */
    public void or(int subsetIndex_, long subset_) 
	{
		if (subsetIndex_ >= subset.length) enlarge(subsetIndex_ + 1);
		// perform logical (a & !b) on subset in common
		subset[subsetIndex_] |= subset_;
		//nbset();
		nbset = -1;
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set 
     * argument. This bit set is modified so that a bit in it has the 
     * value <code>true</code> if and only if one of the following 
     * statements holds: 
     * <ul>
     * <li>The bit initially has the value <code>true</code>, and the 
     *     corresponding bit in the argument has the value <code>false</code>.
     * <li>The bit initially has the value <code>false</code>, and the 
     *     corresponding bit in the argument has the value <code>true</code>. 
     * </ul>
     *
     * @param   set_   a bit set.
     */
    public void xor(BitSet set_) 
	{
		if (set_ == this) { 
			for (int i=0; i<nsubsets; i++)	subset[i] = 0;
			return; 
		}
		
		int nsubsets_ = nsubsets > set_.nsubsets? set_.nsubsets: nsubsets;
		if (nb < set_.nb) {
			nb = set_.nb;
			nsubsets = (nb + 63) >> 6;
			if (nsubsets > subset.length) enlarge(nsubsets);
		}
		int i;
		for(i=0; i<nsubsets_; i++)  subset[i] ^= set_.subset[i];
		if (i < set_.nsubsets)
			for (; i<nsubsets; i++) subset[i] = set_.subset[i];
		//nbset();
		nbset = -1;
   }

     /**
     * Performs a logical <b>XOR</b> of the target subset with the 
     * argument subset. This subset is modified so that a bit in it has the
     * value <code>true</code> if and only if one of the following 
     * statements holds: 
     * <ul>
     * <li>The bit initially has the value <code>true</code>, and the 
     *     corresponding bit in the argument has the value <code>false</code>.
     * <li>The bit initially has the value <code>false</code>, and the 
     *     corresponding bit in the argument has the value <code>true</code>. 
     * </ul>
     * This operation does not change the size of the bit set.
     *
     * @param     subsetIndex_ the subset index.
     * @param     subset_ the subset with which to mask the corresponding 
     *            subset in this <code>BitSet</code>.
     */
    public void xor(int subsetIndex_, long subset_) 
	{
		if (subsetIndex_ >= subset.length) enlarge(subsetIndex_ + 1);
		// perform logical (a & !b) on subset in common
		subset[subsetIndex_] ^= subset_;
		//nbset();
		nbset = -1;
    }
	
    /**
     * Performs a logical <b>NOT</b> of the this bitset.
     * This operation does not change the size of the bit set.
     */
    public void not() 
	{
		for(int i=0; i<nsubsets; i++) subset[i] ^= -1L;
		if (nbset >= 0) nbset = nb - nbset;
		int nbits_ = nb % 64;
		if (nbits_ > 0)
			subset[nsubsets - 1] &= (1L << nbits_) - 1;
		indices = null;
		uindices = null;
    }
	

	//
	void ___MISC___() {}
	//
	
	static int[] NUM_SET_BITS = {
	};

	/**
	 * Recalculates the number of set bits.
	 */
	private void nbset()
	{
		indices = uindices = null;
		nbset = 0;
		int i;
		for (i=0; i<nsubsets - 1; i++) {
			long k = 1L;
			for (int j=0; j<64; j++) {
				if ((subset[i] & k) != 0) nbset ++;
				k = k << 1;
			}
		}
		
		int remaining_ = nb - ((nsubsets - 1) << 6);
		long k = 1L;
		for (int j=0; j<remaining_; j++) {
			if ((subset[i] & k) != 0) nbset ++;
			k = k << 1;
		}
	}
	
	private static int nbset(long subset_)
	{
		int nbset_ = 0;
		long k = 1L;
		for (int j=0; j<64; j++) {
			if ((subset_ & k) != 0) nbset_ ++;
			k = k << 1;
		}
		return nbset_;
	}
	
    /**
     * Enlarges the bitset to the size required.
     * @param	required_ the required number of subsets.
     */
    private void enlarge(int required_) 
	{
		if (subset == null || subset.length < required_) {
		    long[] new_ = new long[required_];
			if (subset != null)
				System.arraycopy(subset, 0, new_, 0, subset.length);
		    subset = new_;
		}
    }

    /**
     * Returns a hash code value for this bit set. The has code 
     * depends only on which subset have been set within this 
     * <code>BitSet</code>. The algorithm used to compute it may 
     * be described as follows.<p>
     * Suppose the subset in the <code>BitSet</code> were to be stored 
     * in an array of <code>long</code> integers called, say, 
     * <code>subset</code>, in such a manner that bit <code>k</code> is 
     * set in the <code>BitSet</code> (for nonnegative values of 
     * <code>k</code>) if and only if the expression 
     * <pre>((k&gt;&gt;6) &lt; subset.length) && ((subset[k&gt;&gt;6] & (1L &lt;&lt; (bit & 0x3F))) != 0)</pre>
     * is true. Then the following definition of the <code>hashCode</code> 
     * method would be a correct implementation of the actual algorithm:
     * <pre>
     * public synchronized int hashCode() {
     *      long h = 1234;
     *      for (int i = subset.length; --i &gt;= 0; ) {
     *           h ^= subset[i] * (i + 1);
     *      }
     *      return (int)((h &gt;&gt; 32) ^ h);
     * }</pre>
     * Note that the hash code values change if the set of subset is altered.
     * <p>Overrides the <code>hashCode</code> method of <code>Object</code>.
     *
     * @return  a hash code value for this bit set.
     */
    public int hashCode() 
	{
		long h = 1234;
		for (int i = subset.length; --i >= 0; )
		        h ^= subset[i] * (i + 1);

		return (int)((h >> 32) ^ h);
    }
    
    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is 
     * not <code>null</code> and is a <code>Bitset</code> object that has 
     * exactly the same set of subset set to <code>true</code> as this bit 
     * set. That is, for every nonnegative <code>int</code> index 
	 * <code>k</code>, 
     * <pre>((BitSet)obj).get(k) == this.get(k)</pre>
     * must be true. The current sizes of the two bit sets are not compared. 
     * <p>Overrides the <code>equals</code> method of <code>Object</code>.
     *
     * @param   that_   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object that_) 
	{
		if (this == that_) return true;
		if (!(that_ instanceof drcl.data.BitSet))  return false;

		BitSet set_ = (BitSet) that_;
		int nsubsets_ = nsubsets > set_.nsubsets? set_.nsubsets: nsubsets;
		// Check subsets in use by both BitSets
		for (int i = 0; i < nsubsets_; i++)
		    if (subset[i] != set_.subset[i]) return false;

		// Check any subsets in use by only one BitSet (must be 0 in other)
		if (nsubsets > nsubsets_) {
		    for (int i = nsubsets_; i<nsubsets; i++)
				if (subset[i] != 0) return false;
		} 
		else 
		    for (int i = nsubsets_; i<set_.nsubsets; i++)
				if (set_.subset[i] != 0) return false;

		return true;
    }

    /**
     */
	public void duplicate(Object source_)
	{
		BitSet that_ = (BitSet)source_;
		if (that_.subset == null) subset = null;
		else {
			if (subset == null || subset.length < that_.subset.length) 
				enlarge(that_.subset.length);
			else clear();
			nsubsets = that_.nsubsets;
			System.arraycopy(that_.subset, 0, subset, 0, nsubsets);
		}
		nb = that_.nb;
		nbset = that_.nbset;
	}

	public Object clone()
	{
		BitSet that_ = new BitSet();
		that_.nb = nb;
		that_.nsubsets = nsubsets;
		that_.nbset = nbset;
		that_.subset = new long[nsubsets];
		System.arraycopy(subset, 0, that_.subset, 0, nsubsets);
		that_.indices = indices;
		return that_;
	}
	
	/**
	 * Prints out this bit set by the set of indices of 1's in this bit set.
     * Overrides the <code>toString</code> method of <code>Object</code>.
     * <p>Example:
     * <pre>
     * BitSet bs_ = new BitSet();</pre>
     * Now <code>bs_.toString()</code> returns "<code>-0-</code>"
	 * (empty set).<p>
     * <pre>
     * bs_.set(2);</pre>
     * Now <code>bs_.toString()</code> returns "<code>2</code>".<p>
     * <pre>
     * bs_.set(4);
     * bs_.set(10);</pre>
     * Now <code>bs_.toString()</code> returns "<code>2,4,10</code>".
     *
     * @return  a string representation of this bit set.
     */
    public String toString() 
	{
		if (nb == 0) return "-0-";
		StringBuffer sb_ = new StringBuffer(8*nb + 2);
		String separator = "";

		for (int i = 0 ; i < nb; i++) 
		    if (get(i)) {
				sb_.append(separator + i);
				separator = ",";
		    }
		return sb_.toString();
    }
	
	/**
	 * Returns the long integer representation of this bit set.
	 */
	public String numberRepresentation()
	{
		if (nb == 0) return "-0-";
		StringBuffer sb_ = new StringBuffer();
		if (subset != null && subset.length > 0) {
			// print from most significant subset
			for (int i=subset.length-1; i>0; i--)
				sb_.append(subset[i] + ",");
			sb_.append(subset[0]);
		}
		return sb_.toString();
	}

	/** Returns the binary representation of this bit set. */
    public String binaryRepresentation() 
	{ return binaryRepresentation(false); }

	/**
	 * Returns the binary representation of this bit set.
	 * @param skipLeadingZeros_ if true, the leading zeros in the resulting 
	 * 		binary represenation is not printed.
	 */
    public String binaryRepresentation(boolean skipLeadingZeros_) 
	{
		if (nb == 0) return "-0-";
		StringBuffer sb_ = new StringBuffer(nb);

		int len_ = nb;
		int pos_ = len_%64;
		if (pos_ == 0) pos_ = 64;
		pos_ --;
		boolean hasOne_ = false;
		for (int i=subset.length-1; i>=0; i--) {
			long v_ = subset[i];
			long probe_ = 1L << pos_;
			for (int j=pos_; j>=0; j--) {
				if ((v_ & probe_) != 0) {
					sb_.append('1');
					hasOne_ = true;
				}
				else {
					if (!skipLeadingZeros_ || hasOne_)
						sb_.append('0');
				}
				if (j == 63)
					// probe_ = 1000... (a negative long),
					// 	shift right makes it 11000...
					probe_ = (probe_ >> 1) - probe_;
				else
					probe_ >>= 1; 
			}
			pos_ = 63;
		}
		//for (int i = 0 ; i < nb; i++) 
		//	sb_.append(get(i)? '1': '0');
		if (sb_.length() == 0) return "0";
		else return sb_.toString();
    }

	/**
	 * Returns the binary representation of the bit set in the specified 
	 * number of binary characters.
	 * It stuffs zeros if the bit set is not large enough.
	 * The length of the result may exceed the specified length if the bit set 
	 * is too large.
	 * @param length_  expected number of binary characters.
	 */
    public String binaryRepresentation(int length_) 
	{
		String result_ = binaryRepresentation(false);
		if (result_.length() == length_) return result_;
		if (result_.length() > length_) {
			int len_ = result_.length() - length_;
			for (int i=0; i<len_; i++)
				if (result_.charAt(i) != '0') return result_.substring(i);
			return result_.substring(len_);
		}
		else {
			int len_ = length_ - result_.length();
			// stuff leading zeros
			StringBuffer sb_ = new StringBuffer(len_);
			for (int i=0; i<len_; i++) sb_.append('0');
			return sb_ + result_;
		}
	}

	static char[] HEX = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	/** Returns the hex representation of this bit set. */
    public String hexRepresentation() 
    { return hexRepresentation(false); }

	/**
	 * Returns the hex representation of this bit set.
	 * @param skipLeadingZeros_ if true, the leading zeros in the resulting 
	 * 		binary represenation is not printed.
	 */
    public String hexRepresentation(boolean skipLeadingZeros_) 
	{
		try {
		if (nb == 0) return "-0-";
		StringBuffer sb_ = new StringBuffer(nb/4);

		int len_ = nb;
		long mask_ = 0x0f;
		for (int i=0; i<subset.length; i++) {
			long v_ = subset[i];
			int len2_ = len_ >= 64? 64: len_;
			len_ -= len2_;
			for (int j=0; j<len2_; j+=4) {
				int maskedV_ = (int) (v_ & mask_);
				if (len2_ - j < 4) {
					// last hex
					maskedV_ = (int) (v_ & ((1 << (len2_ - j)) - 1));
				}
				sb_.append(HEX[maskedV_]);
				v_ >>= 4;
			}
		}
		if (skipLeadingZeros_) {
			int i=sb_.length()-1;
			for (; i>=0; i--)
				if (sb_.charAt(i) != '0') break;
			if (i < 0) return "0";
			sb_.reverse();
			return sb_.substring(sb_.length() - i - 1);
		}
		sb_.reverse();
		return sb_.toString();
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return null;
		}
    }

	/**
	 * Returns the hex representation of the bit set in the specified number 
	 * of hex characters.
	 * It stuffs zeros if the bit set is not large enough.
	 * The length of the result may exceed the specified length if the bit set 
	 * is too large.
	 * @param length_  expected number of hex characters.
	 */
    public String hexRepresentation(int length_) 
	{
		String result_ = hexRepresentation(false);
		if (result_.length() == length_) return result_;
		if (result_.length() > length_) {
			int len_ = result_.length() - length_;
			for (int i=0; i<len_; i++)
				if (result_.charAt(i) != '0') return result_.substring(i);
			return result_.substring(len_);
		}
		else {
			int len_ = length_ - result_.length();
			// stuff leading zeros
			StringBuffer sb_ = new StringBuffer(len_);
			for (int i=0; i<len_; i++) sb_.append('0');
			return sb_ + result_;
		}
	}
}
