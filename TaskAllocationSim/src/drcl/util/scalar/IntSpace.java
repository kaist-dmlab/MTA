// @(#)IntSpace.java   7/2003
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

package drcl.util.scalar;

import java.util.Vector;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An integer space.
 */
public class IntSpace extends drcl.DrclObj
{
	Vector vIntInterval;
	
	public IntSpace()
	{ reset(0, Integer.MAX_VALUE); }
	
	public IntSpace(int start_, int end_)
	{ reset(start_, end_); }
	
	public void reset()
	{ reset(0, Integer.MAX_VALUE); }
	
	public void reset(int start, int end)
	{
		if (vIntInterval == null)
			vIntInterval = new Vector(3,3);
		else
			vIntInterval.removeAllElements();
		
		if (start < end)
			vIntInterval.addElement(new IntInterval(start, end));
	}
	
	public void clear()
	{ vIntInterval.removeAllElements(); }
	
	public void duplicate(Object source_)
	{
		IntSpace that_ = (IntSpace)source_;
		vIntInterval.setSize(that_.vIntInterval.size());
		for (int i=0; i<that_.vIntInterval.size(); i++) {
			IntInterval g_ = (IntInterval)that_.vIntInterval.elementAt(i);
			vIntInterval.setElementAt(new IntInterval(g_.start, g_.end), i);
		}
	}
	
	public String toString()
	{
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<vIntInterval.size(); i++) {
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			sb_.append("(");
			sb_.append(g_.start);
			sb_.append(",");
			sb_.append(g_.end-1);
			sb_.append(") ");
		}
		return sb_.toString();
	}

	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<vIntInterval.size(); i++) {
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			sb_.append("IntInterval ");
			sb_.append(i);
			sb_.append(": (");
			sb_.append(g_.start);
			sb_.append(", ");
			sb_.append(g_.end-1);
			sb_.append(")\n");
		}
		sb_.append(vIntInterval.size() + " IntIntervals\n");
		return sb_.toString();
	}

	public int numOfIntIntervals() { return vIntInterval.size(); }
	
	/**
	 * Returns the first available integer in the space.
	 * @return <code>java.lang.Integer.MIN_VALUE</code> if the set is empty.
	 */
	public int getSmallest()
	{
		if (vIntInterval.size() == 0) return Integer.MIN_VALUE;
		else return ((IntInterval)vIntInterval.firstElement()).start;
	}
	
	public IntInterval[] getIntIntervals()
	{
		IntInterval[] ii_ = new IntInterval[vIntInterval.size()];
		vIntInterval.copyInto(ii_);
		return ii_;
	}

	public IntInterval getIntInterval(int index_)
	{
		return (IntInterval)vIntInterval.elementAt(index_);
	}
	
	/**
	 * Check out the first available integer from the space.
	 * @return <code>java.lang.Integer.MIN_VALUE</code> if the set is empty.
	 */
	public int checkout()
	{
		if (vIntInterval.size() == 0)
			return Integer.MIN_VALUE;
		
		IntInterval g_ = (IntInterval)vIntInterval.firstElement();
		int v_ = g_.start++;
		if (g_.start == g_.end)
			vIntInterval.removeElementAt(0);
		return v_;
	}
	
	/**
	 * Check out an integer, given a preference.
	 * @return <code>java.lang.Integer.MIN_VALUE</code> if <code>code_</code> is not in the space.
	 */
	public int checkout(int code_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start > code_) return Integer.MIN_VALUE;
			else if (code_ < g_.end) {
				if (g_.end - g_.start == 1) {
					vIntInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						IntInterval tmp_ = new IntInterval(code_ + 1, g_.end);
						vIntInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Integer.MIN_VALUE;
	}
	
	/**
	 * Check out a range of integers.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkout(int start_, int end_)
	{
		// special cases
		if (start_ >= end_) return;
		if (vIntInterval.size() == 0)
			return;
		else {
			IntInterval g_ = (IntInterval) vIntInterval.lastElement();
			if (g_.end <= start_) {
				return;
			}
		}
		
		// general cases
		IntInterval gStart_ = null, gEnd_;
		int i, gStartIndex_ = 0, gEndIndex_;
		for (i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.end <= start_)
				//   g +--+
				// arg    ...+----...
				continue;
			else if (g_.start < start_) {
				//   g +------+...
				// arg   +----...
				if (g_.end == end_) {
					g_.end = start_;
					return;
				}
				else if (g_.end > end_) {
					vIntInterval.insertElementAt(new IntInterval(end_, g_.end), i+1);
					g_.end = start_;
					return;
				}
				g_.end = start_;
			}
			else {
				//   g ...+------...
				// arg +----...
				if (g_.start >= end_) {
					//   g    ...+------...
					// arg +--+
					return;
				}
				else {
					//   g    +------...
					// arg +---+...
					if (g_.end >= end_) {
						if (g_.end == end_)
							vIntInterval.removeElementAt(i);
						else 
							g_.start = end_;
						return;
					}
					gStart_ = g_;
					gStartIndex_ = i;
					break;
				}
			}
		}
		
		gEnd_ = gStart_;
		gEndIndex_ = gStartIndex_;
		for (i++; i<vIntInterval.size(); i++) {
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (end_ <= g_.start) break;
			else if (g_.end <= end_) {
				//   g    +---+
				// arg +------+...
				gEnd_ = g_;
				gEndIndex_ = i;
				if (g_.end == end_) break;
			}
			else {
				//   g    +-----+
				// arg +------+
				g_.start = end_;
				break;
			}
		}
		
		// remove gStart_ to gEnd_
		int shift_ = gEndIndex_ - gStartIndex_ + 1;
		for (i = gEndIndex_ + 1; i<vIntInterval.size(); i++) {
			vIntInterval.setElementAt(vIntInterval.elementAt(i), i-shift_);
		}
		vIntInterval.setSize(vIntInterval.size()-shift_);
	}
	
	
	/**
	 * Check out all integers before the specified one.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkoutUntil(int end_)
	{
		int i = getIntIntervalIndex(0/*start index*/, end_);
		if (i < 0) { // checkout all!
			vIntInterval.removeAllElements();
			return;
		}
		
		// remove the first i IntIntervals
		IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
		if (g_.end == end_) i++; // remove the ith as well
		else if (end_ > g_.start) g_.start = end_;
		
		if (i == vIntInterval.size()) vIntInterval.removeAllElements();
		else {
			for (int j=i; j<vIntInterval.size(); j++)
				// move jth element from jth to (j-i)th position
				vIntInterval.setElementAt(vIntInterval.elementAt(j), j-i);
			vIntInterval.setSize(vIntInterval.size() - i);
		}
	}
	
	/**
	 * Check out the smallest integer that is greater than or equal to
	 * <code>code_<code>.
	 * @return <code>java.lang.Integer.MIN_VALUE</code> if none is available.
	 */
	public int checkoutGreater(int code_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start > code_)
				code_ = g_.start;
			
			if (code_ < g_.end) {
				if (g_.end - g_.start == 1) {
					vIntInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						IntInterval tmp_ = new IntInterval(code_ + 1, g_.end);
						vIntInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Integer.MIN_VALUE;
	}
	
	/**
	 * Check out the greatest integer that is smaller than or equal to
	 * <code>code_<code>.
	 * @return <code>java.lang.Integer.MAX_VALUE</code> if none is available.
	 */
	public int checkoutSmaller(int code_)
	{
		for (int i=vIntInterval.size()-1; i>=0; i--)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.end <= code_)
				code_ = g_.end-1;
			
			if (code_ >= g_.start) {
				if (g_.end - g_.start == 1) {
					vIntInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						IntInterval tmp_ = new IntInterval(code_ + 1, g_.end);
						vIntInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Integer.MAX_VALUE;
	}
	
	public void checkin(int which_)
	{
		if (vIntInterval == null)
			vIntInterval = new Vector(3,3);
		if (vIntInterval.size() == 0) {
			vIntInterval.addElement(new IntInterval(which_, which_+1));
			return;
		}
		for (int i=0; i<vIntInterval.size(); i++)
		{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start <= which_ && g_.end > which_) return;
			else if (g_.end == which_) {
				g_.end++;
				// check if this interval can be merged with next
				if (i+1 < vIntInterval.size()) {
					IntInterval next_ = (IntInterval)vIntInterval.elementAt(i+1);
					if (next_.start == g_.end) {
						g_.end = next_.end;
						vIntInterval.removeElementAt(i+1);
					}
				}
				return;
			}
			else if (g_.start -1 == which_) {
				g_.start--;
				return;
			}
			else if (g_.start -1 > which_) {
				IntInterval new_ = new IntInterval(which_, which_+1);
				vIntInterval.insertElementAt(new_, i);
				return;
			}
		}
	}
	
	/**
	 * Check in a range of integers.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkin(int start_, int end_)
	{
		// special cases
		if (start_ >= end_) return;
		if (vIntInterval == null)
			vIntInterval = new Vector(3,3);
		if (vIntInterval.size() == 0) {
			vIntInterval.addElement(new IntInterval(start_, end_));
			return;
		}
		else {
			IntInterval g_ = (IntInterval) vIntInterval.lastElement();
			if (g_.end < start_) {
				vIntInterval.addElement(new IntInterval(start_, end_));
				return;
			}
			else if (g_.end == start_) {
				g_.end = end_;
				return;
			}
		}
		
		// general cases
		IntInterval gStart_ = null, gEnd_;
		int i, gStartIndex_ = 0, gEndIndex_;
		for (i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.end < start_)
				//   g +--+
				// arg      +----...
				continue;
			else if (g_.start <= start_) {
				//   g +------+...
				// arg ...+----...
				if (g_.end >= end_) return;
				gStart_ = g_;
				gStartIndex_ = i;
				break;
			}
			else {
				//   g   +------...
				// arg +----...
				if (g_.start > end_) {
					//   g      +------...
					// arg +--+
					IntInterval new_ = new IntInterval(start_, end_);
					vIntInterval.insertElementAt(new_, i);
					return;
				}
				else {
					//   g     +------...
					// arg +---+...
					g_.start = start_;
					if (g_.end >= end_)	return;
					gStart_ = g_;
					gStartIndex_ = i;
					break;
				}
			}
		}
		
		gEnd_ = gStart_;
		gEnd_.end = end_;
		gEndIndex_ = gStartIndex_;
		for (i++; i<vIntInterval.size(); i++) {
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (end_ < g_.start) break;
			else {
				gEnd_ = g_;
				gEndIndex_ = i;
				if (end_ <= g_.end) break;
				g_.end = end_;
			}
		}
		
		// merge gStart_ to gEnd_
		if (gStart_ == gEnd_) return;
		gStart_.end = gEnd_.end;
		int shift_ = gEndIndex_ - gStartIndex_;
		for (i = gEndIndex_ + 1; i<vIntInterval.size(); i++) {
			vIntInterval.setElementAt(vIntInterval.elementAt(i), i-shift_);
		}
		vIntInterval.setSize(vIntInterval.size()-shift_);
	}
	
	public void takesUnionWith(IntSpace that_)
	{
		IntInterval[] ii_ = that_.getIntIntervals();
		for (int i=0; i<ii_.length; i++)
			checkin(ii_[i].start, ii_[i].end);
	}
	
	public void excludes(IntSpace that_)
	{
		IntInterval[] ii_ = that_.getIntIntervals();
		for (int i=0; i<ii_.length; i++)
			checkout(ii_[i].start, ii_[i].end);
	}
	
	public void intersectedWith(IntSpace that_)
	{
		IntSpace excluded_ = (IntSpace)clone();
		excluded_.excludes(that_);
		excludes(excluded_);
	}
	
	/**
	 * Shifts all the intervals in the space by <code>shift_</code>.
	 */
	public void shiftedBy(int shift_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			g_.start += shift_;
			g_.end += shift_;
			if (g_.start >= g_.end)
				throw new WrapAroundException("Shift " + shift_ + " on " + this);
		}
	}
	
	/** Returns next interval starting greater than or equal to <code>start_</code>.
	The returned interval may be shared by this space, modifying it alters this space.
	@return null if no such an interval exisits. */
	public IntInterval nextInterval(int start_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.end > start_) {
				if (g_.start >= start_)
					return g_;
				else
					return new IntInterval(start_, g_.end);
			}
		}
		return null;
	}
	
	/** Returns next gap starting greater than or equal to <code>start_</code>. */
	public IntInterval nextGap(int start_)
	{
		IntInterval prev_ = null;
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start > start_) {
				if (i == 0 || prev_.end <= start_)
					return new IntInterval(start_, g_.start);
				else
					return new IntInterval(prev_.end, g_.start);
			}
			prev_ = g_;
		}
		if (prev_ == null || prev_.end <= start_)
			return new IntInterval(start_, Integer.MAX_VALUE);
		else
			return null;
	}
	
	/** Returns true if the integer is in the space. */
	public boolean contains(int code_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start <= code_ && code_ < g_.end) return true;
		}
		return false;
	}
	
	/** Returns true if the range is covered in the space.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public boolean contains(int start_, int end_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start <= start_ && end_ <= g_.end) return true;
		}
		return false;
	}
	
	/** Returns true if the range is covered in the space but is not
	one of the constituent blocks.
	@param start_ start of the range, inclusive.
	@param end_ end of the range, exclusive.
	 */
	public boolean strictlyContains(int start_, int end_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start <= start_ && end_ <= g_.end)
				return g_.start != start_ || g_.end != end_;
		}
		return false;
	}
	
	/** Returns true if the intersection of the range and this space is not empty.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public boolean isIntersectedWith(int start_, int end_)
	{
		if (end_ <= start_) return false;
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start <= start_ && start_ < g_.end) return true;
			if (g_.start < end_ && end_ <= g_.end) return true;
		}
		return false;
	}
	
	// return the index of the first interval (from startIndex_) with the end of
	// the interval (exclusive) greater than or equal to code_
	// return -1 if all the intervals are before code_
	int getIntIntervalIndex(int startIndex_, int code_)
	{
		for (int i=startIndex_; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (code_ <= g_.end) return i;
		}
		return -1;
	}
	
	/**
	 * Returns the number of integers in this space.
	 */
	public int getSize()
	{ return getSize(Integer.MIN_VALUE, Integer.MAX_VALUE); }
	
	/**
	 * Returns the number of integers in this space that are in the specified range.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public int getSize(int start_, int end_)
	{
		if (start_ >= end_) return 0;
		int count_ = 0;
		int j = 0;
		for (; j<vIntInterval.size(); j++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(j);
			if (start_ >= g_.end) continue;
			if (start_ < g_.start) {
				break;
			}
			else {
				count_ = Math.min(g_.end, end_) - start_;
				j++;
				break;
			}
		}
		
		for (; j<vIntInterval.size(); j++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(j);
			if (g_.start >= end_) break;
			if (end_ > g_.end) {
				count_ += g_.end - g_.start;
			}
			else {
				count_ += end_ - g_.start;
				break;
			}
		}
		
		return count_;
	}
	
	/**
	 * Returns the number of integers in this space that are smaller than the
	 * specified one.
	 * @param end_ end of the range, exclusive.
	 */
	public int getSizeUpTo(int end_)
	{
		if (vIntInterval.size() == 0) return 0;
		else return getSize(Integer.MIN_VALUE, end_);
	}
	
	/** Returns the iterator that iterates the intervals in this space. */
	public Iterator getIntervalIterator()
	{ return new IntervalIterator(-1, Integer.MIN_VALUE); }

	/** Returns the iterator that iterates the intervals in this space.
	The interval strictly containing <code>start_</code> does not count. */
	public Iterator getIntervalIterator(int start_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.end > start_)
				return new IntervalIterator(i-1, start_);
		}
		return new IntervalIterator(Integer.MAX_VALUE-1, start_);
	}

	/** Returns the iterator that iterates the gaps in this space. */
	public Iterator getGapIterator()
	{ return new GapIterator(-1, getSmallest()); }

	/** Returns the iterator that iterates the gaps in this space.
	The gap strictly containing <code>start_</code> does not count. */
	public Iterator getGapIterator(int start_)
	{
		for (int i=0; i<vIntInterval.size(); i++)	{
			IntInterval g_ = (IntInterval) vIntInterval.elementAt(i);
			if (g_.start > start_)
				return new GapIterator(i-1, start_);
		}
		return new GapIterator(vIntInterval.size()-1, start_);
	}

	// Iterates every interval starting from index
	class IntervalIterator implements Iterator
	{
		int index;
		int start;
		boolean nextCalled = false;

		IntervalIterator(int index_, int start_)
		{
			index = index_;
			start = start_;
		}

		public boolean hasNext()
		{ return vIntInterval != null && (index+1) < vIntInterval.size(); }

		public Object next()
		{
			nextCalled = false;
			if (!hasNext()) throw new NoSuchElementException();
			nextCalled = true;
			IntInterval interval_ = (IntInterval)vIntInterval.elementAt(++index);
			if (interval_.start < start)
				return new IntInterval(start, interval_.end);
			else
				return interval_;
		}

		public void remove()
		{
			if (!nextCalled) throw new IllegalStateException();
			vIntInterval.removeElementAt(index--);
			nextCalled = false;
		}

		public String toString()
		{ return super.toString() + ",index=" + index; }
	}

	// Iterates every gap starting from index
	class GapIterator implements Iterator
	{
		int index;
		int start;
		boolean nextCalled = false;
		boolean firstTime = true;
		IntInterval currentGap = null;

		GapIterator(int index_, int start_)
		{
			index = index_;
			start = start_;
		}

		public boolean hasNext()
		{
			return vIntInterval != null && ((index+1) < vIntInterval.size()
				|| ((IntInterval)vIntInterval.lastElement()).end < Integer.MAX_VALUE);
		}

		public Object next()
		{
			nextCalled = false;
			if (!hasNext()) throw new NoSuchElementException();
			nextCalled = true;

			if (++index < vIntInterval.size()) {// not last interval
				if (firstTime) {
					firstTime = false;
					if (index == 0)
						currentGap = new IntInterval(start,
							((IntInterval) vIntInterval.elementAt(index)).start);
					else
						currentGap = new IntInterval(
							Math.max(start, ((IntInterval) vIntInterval.elementAt(index-1)).end),
							((IntInterval) vIntInterval.elementAt(index)).start);
				}
				else
					currentGap = new IntInterval(
						((IntInterval) vIntInterval.elementAt(index-1)).end,
						((IntInterval) vIntInterval.elementAt(index)).start);
			}
			else {
				currentGap = new IntInterval(
					Math.max(start, ((IntInterval) vIntInterval.lastElement()).end),
					Integer.MAX_VALUE);
			}
			return currentGap.clone();
		}

		public void remove()
		{
			if (!nextCalled) throw new IllegalStateException();
			checkin(currentGap.start, currentGap.end);
		}

		public String toString()
		{ return super.toString() + ",index=" + index; }
	}
}
