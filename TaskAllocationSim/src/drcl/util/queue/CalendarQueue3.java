// @(#)CalendarQueue3.java   9/2002
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

package drcl.util.queue;

import java.util.*;

public class CalendarQueue3 extends QueueImpl
{
	boolean resizeenabled = true;
	double width;
	double oneonwidth; /* this variable is always equal 1/width
			     * we use it for a speedup (mul is cheaper than div),
			     * but we may also lose precision with it.
			     */
	double buckettop;
	double lastkey;
	double prevtop;
	int nbuckets;
	long buckbits;
	int lastbucket;
	int top_threshold;
	int bot_threshold;
	int numResize;
	int numDirectSearch;
	int numDirectSearch2;
	int numBackoff;

	_Element[] buckets;
	int qsize;
	double max;
	static final double MAX_VALUE = (double)Integer.MAX_VALUE;

	public CalendarQueue3()
	{ reset(); }


	public void enqueue(double key_, Object o_)
	{
		// (key_ * oneonwidth) needs to be less than the
		// largest integer on the machine for the mapping
		// of time to bucket to work properly.  if it is not
		// we need to re-calculate the width.
		if (key_ > max) {
			max = key_;
			if (resizeenabled && key_ * oneonwidth > MAX_VALUE)
				resize(nbuckets);
		}

		// bucket number and address
		long n = (long)(key_ * oneonwidth);
		int i = (int)(n & buckbits); 
	
		_Element e_ = new _Element(key_, o_);
		_Element p_ = buckets[i];

		// insert element in stable time sorted order
		if (p_ != null && key_ >= p_.key)
			while ((p_.next != null) && (key_ >= p_.next.key))
				p_ = p_.next;
		else
			p_ = null;
	
		if (p_ == null) {
			e_.next = buckets[i];
			buckets[i] = e_;

			// Tyan: firstKey() and firstElement may advance the parameters
			if (e_.key < lastkey) {
				numBackoff ++;
				// adjust year and resume
				lastbucket = i;
				lastkey = key_;
				buckettop = width*(n+1.5);
				prevtop = buckettop - lastbucket * width;
			}
		}
		else {
			e_.next = p_.next;
			p_.next = e_;
		}
	
		if (++qsize > top_threshold && resizeenabled)
			resize(nbuckets<<1);
	}

	// same as enqueue(key_, o_), duplicate codes for performance
	// used by resize()
	void _insert(_Element e_)
	{
		double key_ = e_.key;
		// (key_ * oneonwidth) needs to be less than the
		// largest integer on the machine for the mapping
		// of time to bucket to work properly.  if it is not
		// we need to re-calculate the width.
		if (key_ > max) {
			max = key_;
			if (resizeenabled && key_ * oneonwidth > MAX_VALUE)
				resize(nbuckets);
		}

		// bucket number and address
		int i = (int)(((long)(key_ * oneonwidth)) & buckbits); 
	
		_Element p_ = buckets[i];

		// insert event in stable time sorted order
		if (p_ != null && key_ >= p_.key)
			while ((p_.next != null) && (key_ >= p_.next.key))
				p_ = p_.next;
		else
			p_ = null;
	
		if (p_ == null) {
			e_.next = buckets[i];
			buckets[i] = e_;
		}
		else {
			e_.next = p_.next;
			p_.next = e_;
		}
	
		if (++qsize > top_threshold && resizeenabled)
			resize(nbuckets<<1);
	}

	public Object dequeue()
	{
		if (qsize == 0)
			return null;
		int i = lastbucket;
		
		// check for an event this `year'
		do {
			_Element e_ = buckets[i];
			if ((e_ != null) && (e_.key < buckettop)) {
				buckets[i] = e_.next;
				lastbucket = i;
				lastkey = e_.key;
				if (--qsize < bot_threshold && resizeenabled)
					resize(nbuckets>>1);
				return e_.obj;
			} else {
				if (++i == nbuckets) {
					i = 0;
					// at the end of each year, eliminate roundoff error in buckettop
					buckettop = prevtop + nbuckets * width;
					prevtop = buckettop;
				} else {
					buckettop+= width;
				}
			}
		} while (i != lastbucket);
			
		// or direct search for the minimum event
		numDirectSearch++;
		i = 0;
		_Element min_;
		do {
			min_ = buckets[i++];
		} while (min_ == null);
		i--;
			
		int k;
		for (k = i+1; k < nbuckets; k++) {
			_Element e_ = buckets[k];
			if ((e_ != null) && (e_.key < min_.key)) {
				min_ = e_; i = k;
			}
		}
			
		// adjust year and resume
		lastbucket = i;
		lastkey = min_.key;
		long n = (long)(min_.key * oneonwidth);
		buckettop = width*(n+1.5);
		prevtop = buckettop - lastbucket * width;

		buckets[i] = min_.next;
		if (--qsize < bot_threshold && resizeenabled)
			resize(nbuckets>>1);
		return min_.obj;
	}

	// same as dequeue() except no resizing, used by _newwidth()
	public _Element _dequeue()
	{
		int i = lastbucket;
		_Element min_;
		
		// check for an event this `year'
		do {
			min_ = buckets[i];
			if ((min_ != null) && (min_.key < buckettop)) {
				buckets[i] = min_.next;
				lastbucket = i;
				lastkey = min_.key;
				return min_;
			} else {
				if (++i == nbuckets) {
					i = 0;
					// at the end of each year, eliminate roundoff error in buckettop
					buckettop = prevtop + nbuckets * width;
					prevtop = buckettop;
					
				} else {
					buckettop+= width;
				}
			}
		} while (i != lastbucket);
			
		// or direct search for the minimum event
		numDirectSearch++;
		i = 0;
		do {
			min_ = buckets[i++];
		} while (min_ == null);
		i--;
		
		for (int k = i+1; k < nbuckets; k++) {
			_Element e_ = buckets[k];
			if ((e_ != null) && (e_.key < min_.key)) {
				min_ = e_; i = k;
			}
		}
		
		// adjust year and resume
		lastbucket = i;
		lastkey = min_.key;
		long n = (long)(min_.key * oneonwidth);
		buckettop = width*(n+1.5);
		prevtop = buckettop - lastbucket * width;

		buckets[i] = min_.next;
		return min_;
	}

	// same as dequeue() except no actual deqeue
	public double firstKey()
	{
		if (qsize == 0)
			return Double.NaN;
		int i = lastbucket;
		do {
			_Element e_ = buckets[i];
			if ((e_ != null) && (e_.key < buckettop)) {
				lastbucket = i;
				lastkey = e_.key;
				return e_.key;
			} else {
				if (++i == nbuckets) {
					i = 0;
					// at the end of each year, eliminate roundoff error in buckettop
					buckettop = prevtop + nbuckets * width;
					prevtop = buckettop;
					
				} else {
					buckettop+= width;
				}
			}
		} while (i != lastbucket);
			
		// or direct search for the minimum event
		numDirectSearch2++;
		i = 0;
		_Element min_;
		do {
			min_ = buckets[i++];
		} while (min_ == null);
		i--;
			
		int k;
		for (k = i+1; k < nbuckets; k++) {
			_Element e_ = buckets[k];
			if ((e_ != null) && (e_.key < min_.key)) {
				min_ = e_; i = k;
			}
		}
		// adjust year and resume
		lastbucket = i;
		lastkey = min_.key;
		long n = (long)(min_.key * oneonwidth);
		buckettop = width*(n+1.5);
		return min_.key;
	}

	// same as firstKey()
	public Object firstElement()
	{
		if (qsize == 0)
			return null;
		int i = lastbucket;
		do {
			_Element e_ = buckets[i];
			if ((e_ != null) && (e_.key < buckettop)) {
				lastbucket = i;
				lastkey = e_.key;
				return e_.obj;
			} else {
				if (++i == nbuckets) {
					i = 0;
					// at the end of each year, eliminate roundoff error in buckettop
					buckettop = prevtop + nbuckets * width;
					prevtop = buckettop;
					
				} else {
					buckettop+= width;
				}
			}
		} while (i != lastbucket);
			
		// or direct search for the minimum event
		numDirectSearch2++;
		i = 0;
		_Element min_;
		do {
			min_ = buckets[i++];
		} while (min_ == null);
		i--;
			
		int k;
		for (k = i+1; k < nbuckets; k++) {
			_Element e_ = buckets[k];
			if ((e_ != null) && (e_.key < min_.key)) {
				min_ = e_; i = k;
			}
		}

		// adjust year and resume
		lastbucket = i;
		lastkey = min_.key;
		long n = (long)(min_.key * oneonwidth);
		buckettop = width*(n+1.5);
		return min_.obj;
	}

	void _init(int nbuck_, double bwidth_, double start_)
	{
		buckets = new _Element[nbuck_];
		width = bwidth_;
		oneonwidth = 1.0 / width;
		nbuckets = nbuck_;
		buckbits = nbuckets-1;
		qsize = 0;
		lastkey = start_;
		long n = (long)(start_ * oneonwidth);
		lastbucket = (int)(n & buckbits);
		buckettop = width*(n+1.5);
		prevtop = buckettop - lastbucket * width;
		bot_threshold = (nbuckets >> 1) - 2;
		top_threshold = (nbuckets << 1);
	}

	// no ``resizeenabled'' check
	public void resize(int newsize_)
	{
		numResize++;
		double bwidth_ = _newwidth();
		_Element[] oldb_ = buckets;
		int oldn_ = nbuckets;

		// copy events to new buckets
		_init(newsize_, bwidth_, lastkey);
		for (int i = oldn_-1; i >= 0; i--) {
			_Element e_ = oldb_[i];
			while (e_ != null) {
				_Element en_ = e_.next;
				_insert(e_);
				e_ = en_;
			}
		}
	}

	static int MAX_HOLD = 25;
	static double MIN_WIDTH = 1.0e-6;

	double _newwidth()
	{
		int nsamples_;

		if (qsize < 2) return 1.0;
		if (qsize < 5) nsamples_ = qsize;
		else nsamples_ = 5 + qsize / 10;
		if (nsamples_ > MAX_HOLD) nsamples_ = MAX_HOLD;

		_Element[] hold_ = new _Element[MAX_HOLD];

		// dequue and requeue sample events to gauge width
		double buckettop_ = buckettop;
		int lastbucket_ = lastbucket;
		double prevtop_ = prevtop;
		double lastkey_ = lastkey;

		resizeenabled = false;
		for (int i = 0; i < nsamples_; i++)
			hold_[i] = _dequeue();
		// insert in the inverse order and using insert2 to take care of same-time events.
		for (int j = nsamples_-1; j >= 0; j--)
			_insert2(hold_[j]);
		resizeenabled = true;

		buckettop = buckettop_;
		prevtop = prevtop_;
		lastbucket = lastbucket_;
		lastkey = lastkey_;

		// try to work out average cluster separation
		double asep_ = (hold_[nsamples_-1].key - hold_[0].key) / (nsamples_-1);
		double asep2_ = 0.0;
		int count_ = 0;

		for (int k = 1; k < nsamples_; k++) {
			double diff_ = hold_[k].key - hold_[k-1].key;
			if (diff_ < 2.0*asep_) { asep2_ += diff_; count_++; }
		}

		// but don't let things get too small for numerical stability
		double nw_ = count_ > 0 ? 3.0*(asep2_/count_) : asep_;
		if (nw_ < MIN_WIDTH) nw_ = MIN_WIDTH;

		// need to make sure that time_/width can be represented as
		// an int.  see the comment at the start of enqueue().
		if (max/nw_ > MAX_VALUE)
			nw_ = max/MAX_VALUE;
		return nw_;
	}


	// Same as _insert(), but for inserting e_ *before* any same-time-events, if
	//   there should be any.  Since it is used only by _newwidth(),
	//   some important checks present in _insert() need not be performed.
	void _insert2(_Element e_)
	{
		double key_ = e_.key;
		// bucket number and address
		int i = (int)(((long)(key_ * oneonwidth)) & buckbits); 
	
		_Element p_ = buckets[i];

		// insert event in stable time sorted order
		if (p_ != null && key_ > p_.key)
			while ((p_.next != null) && (key_ > p_.next.key))
				p_ = p_.next;
		else
			p_ = null;
	
		if (p_ == null) {
			e_.next = buckets[i];
			buckets[i] = e_;
		}
		else {
			e_.next = p_.next;
			p_.next = e_;
		}
	
		++qsize;
	}

	public Object remove(double key_, Object obj_)
	{
		int i = (int)(((long)(key_ * oneonwidth)) & buckbits);
	
		_Element p_ = buckets[i];
		if (p_ == null) return null;
		Object o_ = null;
		if (p_.obj.equals(obj_)) {
			o_ = p_.obj;
			buckets[i] = p_.next;
		}
		else
			for (; p_.next != null; p_ = p_.next)
				if (p_.next.obj.equals(obj_)) {
					o_ = p_.next.obj;
					p_.next = p_.next.next;
					break; 
				}
		if (--qsize < bot_threshold && resizeenabled)
			resize(nbuckets>>1);
		return o_;
	}

	public Object remove(Object obj_)
	{
		for (int i=0; i<buckets.length; i++) {
			_Element p_ = buckets[i];
			if (p_ == null) continue;
			if (p_.obj.equals(obj_)) {
				buckets[i] = p_.next;
				if (--qsize < bot_threshold && resizeenabled)
					resize(nbuckets>>1);
				return p_.obj;
			}
			else
				for (; p_.next != null; p_ = p_.next)
					if (p_.next.obj.equals(obj_)) {
						obj_ = p_.next.obj;
						p_.next = p_.next.next;
						if (--qsize < bot_threshold && resizeenabled)
							resize(nbuckets>>1);
						return obj_;
					}
		}
		return null;
	}


	public boolean isEmpty()
	{ return qsize == 0; }

	public int getSize()
	{ return qsize; }

	public int getLength()
	{ return qsize; }

	_Element[] _all()
	{
		if (qsize == 0)
			return new _Element[0];
			
		// save everything that is changed in _dequeue()
		double buckettop_ = buckettop;
		double lastkey_ = lastkey;
		double prevtop_ = prevtop;
		int lastbucket_ = lastbucket;
		_Element[] buckets_ = new _Element[nbuckets];
		int qsize_ = qsize;
		System.arraycopy(buckets, 0, buckets_, 0, nbuckets);

		_Element[] all_ = new _Element[qsize];
		for (int i=0; i< qsize_; i++)
			all_[i] = _dequeue();

		// restore everything
		buckettop = buckettop_;
		lastkey = lastkey_;
		prevtop = prevtop_;
		lastbucket = lastbucket_;
		qsize = qsize_;
		System.arraycopy(buckets_, 0, buckets, 0, nbuckets);

		return all_;
	}

	public double[] keys()
	{
		_Element[] all_ = _all();
		double[] keys_ = new double[qsize];
		for (int i=0; i< qsize; i++)
			keys_[i] = all_[i].key;
		return keys_;
	}

	public Object[] retrieveAll()
	{
		_Element[] all_ = _all();
		Object[] oo_ = new Object[qsize];
		for (int i=0; i< qsize; i++)
			oo_[i] = all_[i].obj;
		return oo_;
	}

	public void reset()
	{
		_init(2, 1.0, 0.0);
		max = 0.0;
		numBackoff = numDirectSearch2 = numDirectSearch = numResize = 0;
	}

	/**
	 * Prints the content of the queue.
	 * @param prefix_ prefix of each line when printing.
	 */
	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + qsize + "\n");
		if (!isEmpty()) {
			double[] keys_ = keys();
			Object[] elements_ = retrieveAll();
			for (int i=0; i<keys_.length; i++)
				sb_.append(prefix_ + keys_[i] + "\t" + elements_[i] + "\n");
		}
		return sb_.toString();
	}

	/** Prints the content of the queue. */
	public String info()
	{ return info(""); }

	/** Prints the content of the queue in one line of string.  */
	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + qsize);
		
		_Element[] all_ = _all();
		for (int i=0; i<all_.length; i++)
			sb_.append("-----" + all_[i].key + ":" + all_[i].obj);
		
		return sb_.toString();
	}

	public String diag(boolean listElement_)
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + qsize + "\n");
		sb_.append("    resizable? = " + resizeenabled + "\n");
		sb_.append("  bucket_width = " + width + "\n");
		sb_.append("     buckettop = " + buckettop + "\n");
		sb_.append("       prevtop = " + prevtop + "\n");
		sb_.append("       lastkey = " + lastkey + "\n");
		sb_.append("    lastbucket = " + lastbucket + "\n");
		sb_.append("    bucketbits = " + buckbits + "\n");
		sb_.append("      nbuckets = " + nbuckets + "\n");
		sb_.append(" top_threshold = " + top_threshold + "\n");
		sb_.append(" bot_threshold = " + bot_threshold + "\n");
		sb_.append("           max = " + max + "\n");
		sb_.append("     numResize = " + numResize + "\n");
		sb_.append("numDirectSearch = " + numDirectSearch + "\n");
		sb_.append("numDirectSearch2= " + numDirectSearch2 + "\n");
		sb_.append("     numBackoff = " + numBackoff + "\n");
		if (listElement_) {
			sb_.append(" --- buckets --- " + "\n");
			for (int i=0; i<buckets.length; i++) {
				sb_.append("   bucket_" + i + ": " + "\n");
				_Element p_ = buckets[i];
				while (p_ != null) {
					sb_.append("      " + p_.key + " ----- " + p_.obj + "\n");
					p_ = p_.next;
				}
			}
		}
		return sb_.toString();
	}

	public Enumeration getElementEnumerator()
	{
		throw new QueueOptionalImplException(getClass()
						+ " doesn't implement getElementEnumerator()");
	}
	
	public Enumeration getKeyEnumerator()
	{
		throw new QueueOptionalImplException(getClass() + 
						"doesn't implement getKeyEnumerator()");
	}

	public void enqueue(Object o_)
	{ enqueue(max, o_); }

	public Object[] retrieveAll(double key_)
	{
		if (qsize == 0) return null;
		_Element[] all_ = _all();
		Vector v_ = new Vector();
		for (int i=0; i<all_.length; i++)
			if (all_[i].key == key_) v_.addElement(all_[i].obj);
		Object[] tmp_ = new Object[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	public double retrieveKey(Object o_)
	{
		if (qsize == 0) return Double.NaN;
		_Element[] all_ = _all();
		for (int i=0; i<all_.length; i++)
			if (all_[i].obj.equals(o_)) return all_[i].key;
		return Double.NaN;
	}

	public Object dequeue(double key_)
	{
		if (qsize == 0) return null;
		_Element[] all_ = _all();
		for (int i=0; i<all_.length; i++)
			if (all_[i].key == key_) {
				remove(key_, all_[i].obj);
				return all_[i].obj;
			}
		return null;
	}


	public Object retrieveBy(double key_)
	{
		if (qsize == 0) return null;
		_Element[] all_ = _all();
		for (int i=0; i<all_.length; i++)
			if (all_[i].key == key_) return all_[i].obj;
		return null;
	}

	public void removeAll(Object o_)
	{
		if (qsize == 0) return;
		_Element[] all_ = _all();
		for (int i=0; i<all_.length; i++)
			if (all_[i].obj.equals(o_))
				remove(all_[i].key, o_);
	}

	public void removeAll(double key_, Object o_)
	{
		int i = (int)(((long)(key_ * oneonwidth)) & buckbits);
	
		_Element p_ = buckets[i];
		if (p_.obj.equals(o_))
			buckets[i] = p_.next;
		else {
			for (; p_.next != null; )
				if (p_.next.obj.equals(o_)) {
					p_.next = p_.next.next;
					qsize--;
				}
				else
					p_ = p_.next;
		}
		if (resizeenabled && qsize < bot_threshold)
			resize(nbuckets>>1);
	}

	public double lastKey()
	{
		if (qsize == 0) return Double.NaN;
		_Element[] all_ = _all();
		return all_[qsize-1].key;
	}

	public Object lastElement()
	{
		if (qsize == 0) return null;
		_Element[] all_ = _all();
		return all_[qsize-1].obj;
	}

	public double retrieveKeyAt(int n_)
	{
		if (n_ >= qsize) return Double.NaN;
		_Element[] all_ = _all();
		return all_[n_].key;
	}

	public Object retrieveAt(int n_)
	{
		if (n_ >= qsize) return null;
		_Element[] all_ = _all();
		return all_[n_].obj;
	}

	public Object remove(int n_)
	{
		if (n_ >= qsize) return null;
		_Element e_ = _all()[n_];
		remove(e_.key, e_.obj);
		return e_.obj;
	}

	public boolean containsKey(double key_)
	{
		int i = (int)(((long)(key_ * oneonwidth)) & buckbits); 
		_Element p_ = buckets[i];
		while (p_ != null) {
			if (p_.key == key_) return true;
			p_ = p_.next;
		}
		return false;
	}

	public Element[] _retrieveAll()
	{ return _all(); }


	public boolean contains(Object o_)
	{
		for (int i=0; i<buckets.length; i++) {
			_Element p_ = buckets[i];
			while (p_ != null) {
				if (p_.obj == o_) return true;
				p_ = p_.next;
			}
		}
		return false;
	}
}
