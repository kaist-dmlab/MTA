// @(#)LongSpace.java   7/2003
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
 * A long integer space.
 */
public class LongSpace extends drcl.DrclObj
{
	Vector vLongInterval;
	
	public LongSpace()
	{ reset(0, Long.MAX_VALUE); }
	
	public LongSpace(long start_, long end_)
	{ reset(start_, end_); }
	
	public void reset()
	{ reset(0, Long.MAX_VALUE); }
	
	public void reset(long start, long end)
	{
		if (vLongInterval == null)
			vLongInterval = new Vector(3,3);
		else
			vLongInterval.removeAllElements();
		
		if (start < end)
			vLongInterval.addElement(new LongInterval(start, end));
	}
	
	public void clear()
	{ vLongInterval.removeAllElements(); }
	
	public void duplicate(Object source_)
	{
		LongSpace that_ = (LongSpace)source_;
		vLongInterval.setSize(that_.vLongInterval.size());
		for (int i=0; i<that_.vLongInterval.size(); i++) {
			LongInterval g_ = (LongInterval)that_.vLongInterval.elementAt(i);
			vLongInterval.setElementAt(new LongInterval(g_.start, g_.end), i);
		}
	}
	
	public String toString()
	{
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<vLongInterval.size(); i++) {
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
		for (int i=0; i<vLongInterval.size(); i++) {
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			sb_.append("LongInterval ");
			sb_.append(i);
			sb_.append(": (");
			sb_.append(g_.start);
			sb_.append(", ");
			sb_.append(g_.end-1);
			sb_.append(")\n");
		}
		sb_.append(vLongInterval.size() + " LongIntervals\n");
		return sb_.toString();
	}

	public int numOfLongIntervals() { return vLongInterval.size(); }
	
	/**
	 * Returns the first available long integer in the space.
	 * @return <code>java.lang.Long.MIN_VALUE</code> if the set is empty.
	 */
	public long getSmallest()
	{
		if (vLongInterval.size() == 0) return Long.MIN_VALUE;
		else return ((LongInterval)vLongInterval.firstElement()).start;
	}
	
	public LongInterval[] getLongIntervals()
	{
		LongInterval[] ii_ = new LongInterval[vLongInterval.size()];
		vLongInterval.copyInto(ii_);
		return ii_;
	}

	public LongInterval getLongInterval(int index_)
	{
		return (LongInterval) vLongInterval.elementAt(index_);
	}
	
	/**
	 * Check out the first available long integer from the space.
	 * @return <code>java.lang.Long.MIN_VALUE</code> if the set is empty.
	 */
	public long checkout()
	{
		if (vLongInterval.size() == 0)
			return Long.MIN_VALUE;
		
		LongInterval g_ = (LongInterval)vLongInterval.firstElement();
		long v_ = g_.start++;
		if (g_.start == g_.end)
			vLongInterval.removeElementAt(0);
		return v_;
	}
	
	/**
	 * Check out a long integer, given a preference.
	 * @return <code>java.lang.Long.MIN_VALUE</code> if <code>code_</code> is not in the space.
	 */
	public long checkout(long code_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start > code_) return Long.MIN_VALUE;
			else if (code_ < g_.end) {
				if (g_.end - g_.start == 1) {
					vLongInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						LongInterval tmp_ = new LongInterval(code_ + 1, g_.end);
						vLongInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Long.MIN_VALUE;
	}
	
	/**
	 * Check out a range of long integers.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkout(long start_, long end_)
	{
		// special cases
		if (start_ >= end_) return;
		if (vLongInterval.size() == 0)
			return;
		else {
			LongInterval g_ = (LongInterval) vLongInterval.lastElement();
			if (g_.end <= start_) {
				return;
			}
		}
		
		// general cases
		LongInterval gStart_ = null, gEnd_;
		int i, gStartIndex_ = 0, gEndIndex_;
		for (i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
					vLongInterval.insertElementAt(new LongInterval(end_, g_.end), i+1);
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
							vLongInterval.removeElementAt(i);
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
		for (i++; i<vLongInterval.size(); i++) {
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
		for (i = gEndIndex_ + 1; i<vLongInterval.size(); i++) {
			vLongInterval.setElementAt(vLongInterval.elementAt(i), i-shift_);
		}
		vLongInterval.setSize(vLongInterval.size()-shift_);
	}
	
	
	/**
	 * Check out all long integers before the specified one.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkoutUntil(long end_)
	{
		int i = getLongIntervalIndex(0/*start index*/, end_);
		if (i < 0) { // checkout all!
			vLongInterval.removeAllElements();
			return;
		}
		
		// remove the first i LongIntervals
		LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
		if (g_.end == end_) i++; // remove the ith as well
		else if (end_ > g_.start) g_.start = end_;
		
		if (i == vLongInterval.size()) vLongInterval.removeAllElements();
		else {
			for (int j=i; j<vLongInterval.size(); j++)
				// move jth element from jth to (j-i)th position
				vLongInterval.setElementAt(vLongInterval.elementAt(j), j-i);
			vLongInterval.setSize(vLongInterval.size() - i);
		}
	}
	
	/**
	 * Check out the smallest long integer that is greater than or equal to
	 * <code>code_<code>.
	 * @return <code>java.lang.Long.MIN_VALUE</code> if none is available.
	 */
	public long checkoutGreater(long code_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start > code_)
				code_ = g_.start;
			
			if (code_ < g_.end) {
				if (g_.end - g_.start == 1) {
					vLongInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						LongInterval tmp_ = new LongInterval(code_ + 1, g_.end);
						vLongInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Long.MIN_VALUE;
	}
	
	/**
	 * Check out the greatest long integer that is smaller than or equal to
	 * <code>code_<code>.
	 * @return <code>java.lang.Long.MAX_VALUE</code> if none is available.
	 */
	public long checkoutSmaller(long code_)
	{
		for (int i=vLongInterval.size()-1; i>=0; i--)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.end <= code_)
				code_ = g_.end-1;
			
			if (code_ >= g_.start) {
				if (g_.end - g_.start == 1) {
					vLongInterval.removeElementAt(i);
				}
				else if (g_.start < code_) {
					if (code_ < g_.end -1) {
						LongInterval tmp_ = new LongInterval(code_ + 1, g_.end);
						vLongInterval.insertElementAt(tmp_, i+1);
					}
					g_.end = code_;
				}
				else
					g_.start++;
				return code_;
			}
		}
		return Long.MAX_VALUE;
	}
	
	public void checkin(long which_)
	{
		if (vLongInterval.size() == 0) {
			vLongInterval.addElement(new LongInterval(which_, which_+1));
			return;
		}
		for (int i=0; i<vLongInterval.size(); i++)
		{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start <= which_ && g_.end > which_) return;
			else if (g_.end == which_) {
				g_.end++;
				// check if this interval can be merged with next
				if (i+1 < vLongInterval.size()) {
					LongInterval next_ = (LongInterval)vLongInterval.elementAt(i+1);
					if (next_.start == g_.end) {
						g_.end = next_.end;
						vLongInterval.removeElementAt(i+1);
					}
				}
				return;
			}
			else if (g_.start -1 == which_) {
				g_.start--;
				return;
			}
			else if (g_.start -1 > which_) {
				LongInterval new_ = new LongInterval(which_, which_+1);
				vLongInterval.insertElementAt(new_, i);
				return;
			}
		}
	}
	
	/**
	 * Check in a range of long integers.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public void checkin(long start_, long end_)
	{
		// special cases
		if (start_ >= end_) return;
		if (vLongInterval == null)
			vLongInterval = new Vector(3,3);
		if (vLongInterval.size() == 0) {
			vLongInterval.addElement(new LongInterval(start_, end_));
			return;
		}
		else {
			LongInterval g_ = (LongInterval) vLongInterval.lastElement();
			if (g_.end < start_) {
				vLongInterval.addElement(new LongInterval(start_, end_));
				return;
			}
			else if (g_.end == start_) {
				g_.end = end_;
				return;
			}
		}
		
		// general cases
		LongInterval gStart_ = null, gEnd_;
		int i, gStartIndex_ = 0, gEndIndex_;
		for (i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
					LongInterval new_ = new LongInterval(start_, end_);
					vLongInterval.insertElementAt(new_, i);
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
		for (i++; i<vLongInterval.size(); i++) {
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
		for (i = gEndIndex_ + 1; i<vLongInterval.size(); i++) {
			vLongInterval.setElementAt(vLongInterval.elementAt(i), i-shift_);
		}
		vLongInterval.setSize(vLongInterval.size()-shift_);
	}
	
	public void takesUnionWith(LongSpace that_)
	{
		LongInterval[] ii_ = that_.getLongIntervals();
		for (int i=0; i<ii_.length; i++)
			checkin(ii_[i].start, ii_[i].end);
	}
	
	public void excludes(LongSpace that_)
	{
		LongInterval[] ii_ = that_.getLongIntervals();
		for (int i=0; i<ii_.length; i++)
			checkout(ii_[i].start, ii_[i].end);
	}
	
	public void intersectedWith(LongSpace that_)
	{
		LongSpace excluded_ = (LongSpace)clone();
		excluded_.excludes(that_);
		excludes(excluded_);
	}
	
	/** Shifts all the intervals in the space by <code>shift_</code>.  */
	public void shiftedBy(long shift_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			g_.start += shift_;
			g_.end += shift_;
			if (g_.start >= g_.end)
				g_.end = Long.MAX_VALUE;
		}
	}
	
	/** Returns next interval starting greater than or equal to <code>start_</code>.
	The returned interval may be shared by this space, modifying it alters this space.
	@return null if no such an interval exisits. */
	public LongInterval nextInterval(long start_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.end > start_) {
				if (g_.start >= start_)
					return g_;
				else
					return new LongInterval(start_, g_.end);
			}
		}
		return null;
	}
	
	/** Returns next gap starting greater than or equal to <code>start_</code>. */
	public LongInterval nextGap(long start_)
	{
		LongInterval prev_ = null;
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start > start_) {
				if (i == 0 || prev_.end <= start_)
					return new LongInterval(start_, g_.start);
				else
					return new LongInterval(prev_.end, g_.start);
			}
			prev_ = g_;
		}
		if (prev_ == null || prev_.end <= start_)
			return new LongInterval(start_, Long.MAX_VALUE);
		else
			return null;
	}
	
	/** Returns true if the long integer is in the space.  */
	public boolean contains(long code_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start <= code_ && code_ < g_.end) return true;
		}
		return false;
	}
	
	/** Returns true if the range is covered in the space.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public boolean contains(long start_, long end_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start <= start_ && end_ <= g_.end) return true;
		}
		return false;
	}
	
	/** Returns true if the range is covered in this space but not one of the constituent blocks.
	@param start_ start of the range, inclusive.
	@param end_ end of the range, exclusive.
	 */
	public boolean strictlyContains(long start_, long end_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start <= start_ && end_ <= g_.end)
				return g_.start != start_ || end_ != g_.end;
		}
		return false;
	}
	
	/** Returns true if the intersection of the range and this space is not empty.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public boolean isIntersectedWith(long start_, long end_)
	{
		if (end_ <= start_) return false;
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start <= start_ && start_ < g_.end) return true;
			if (g_.start < end_ && end_ <= g_.end) return true;
		}
		return false;
	}
	
	// return the index of the first interval (from startIndex_) with the end of
	// the interval (exclusive) greater than or equal to code_
	// return -1 if all the intervals are before code_
	int getLongIntervalIndex(int startIndex_, long code_)
	{
		for (int i=startIndex_; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (code_ <= g_.end) return i;
		}
		return -1;
	}
	
	/** Returns the number of long integers in this space. */
	public long getSize()
	{ return getSize(Long.MIN_VALUE, Long.MAX_VALUE); }
	
	/**
	 * Returns the number of long integers in this space that are in the specified range.
	 * @param start_ start of the range, inclusive.
	 * @param end_ end of the range, exclusive.
	 */
	public long getSize(long start_, long end_)
	{
		if (start_ >= end_) return 0;
		long count_ = 0;
		int j = 0;
		for (; j<vLongInterval.size(); j++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(j);
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
		
		for (; j<vLongInterval.size(); j++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(j);
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
	 * Returns the number of long integers in this space that are smaller than the
	 * specified one.
	 * @param end_ end of the range, exclusive.
	 */
	public long getSizeUpTo(long end_)
	{
		if (vLongInterval.size() == 0) return 0;
		else return getSize(Long.MIN_VALUE, end_);
	}
	
	/** Returns the iterator that iterates the intervals in this space. */
	public Iterator getIntervalIterator()
	{ return new IntervalIterator(-1, Long.MIN_VALUE); }

	/** Returns the iterator that iterates the intervals in this space.
	The interval strictly containing <code>start_</code> does not count. */
	public Iterator getIntervalIterator(long start_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
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
	public Iterator getGapIterator(long start_)
	{
		for (int i=0; i<vLongInterval.size(); i++)	{
			LongInterval g_ = (LongInterval) vLongInterval.elementAt(i);
			if (g_.start > start_)
				return new GapIterator(i-1, start_);
		}
		return new GapIterator(vLongInterval.size()-1, start_);
	}

	// Iterates every interval starting from index
	class IntervalIterator implements Iterator
	{
		int index;
		long start;
		boolean nextCalled = false;

		IntervalIterator(int index_, long start_)
		{
			index = index_;
			start = start_;
		}

		public boolean hasNext()
		{ return vLongInterval != null && (index+1) < vLongInterval.size(); }

		public Object next()
		{
			nextCalled = false;
			if (!hasNext()) throw new NoSuchElementException();
			nextCalled = true;
			LongInterval interval_ = (LongInterval)vLongInterval.elementAt(++index);
			if (interval_.start < start)
				return new LongInterval(start, interval_.end);
			else
				return interval_;
		}

		public void remove()
		{
			if (!nextCalled) throw new IllegalStateException();
			vLongInterval.removeElementAt(index--);
			nextCalled = false;
		}

		public String toString()
		{ return super.toString() + ",index=" + index; }
	}

	// Iterates every gap starting from index
	class GapIterator implements Iterator
	{
		int index;
		long start;
		boolean nextCalled = false;
		boolean firstTime = true;
		LongInterval currentGap = null;

		GapIterator(int index_, long start_)
		{
			index = index_;
			start = start_;
		}

		public boolean hasNext()
		{
			return vLongInterval != null && ((index+1) < vLongInterval.size()
				|| ((LongInterval)vLongInterval.lastElement()).end < Long.MAX_VALUE);
		}

		public Object next()
		{
			nextCalled = false;
			if (!hasNext()) throw new NoSuchElementException();
			nextCalled = true;

			if (++index < vLongInterval.size()) {// not last interval
				if (firstTime) {
					firstTime = false;
					if (index == 0)
						currentGap = new LongInterval(start,
							((LongInterval) vLongInterval.elementAt(index)).start);
					else
						currentGap = new LongInterval(
							Math.max(start, ((LongInterval) vLongInterval.elementAt(index-1)).end),
							((LongInterval) vLongInterval.elementAt(index)).start);
				}
				else
					currentGap = new LongInterval(
						((LongInterval) vLongInterval.elementAt(index-1)).end,
						((LongInterval) vLongInterval.elementAt(index)).start);
			}
			else {
				currentGap = new LongInterval(
					Math.max(start, ((LongInterval) vLongInterval.lastElement()).end),
					Long.MAX_VALUE);
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
