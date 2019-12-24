// @(#)QueueImpl.java   9/2002
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

/** Base class for implementing a queue. */
public abstract class QueueImpl extends drcl.DrclObj implements Queue
{
	public abstract void reset();
	
	public boolean isEmpty()
	{ return getLength() == 0; }
	
	public abstract void enqueue(double key_, Object element_);
	
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{
		throw new QueueOptionalImplException(getClass() + 
						"doesn't implement enqueueAt(int, double, Object)");
	}

	
	public boolean enqueueAfter(Object previousElement_, 
					Object element_)
	{
		throw new QueueOptionalImplException(getClass() + 
						"doesn't implement enqueueAfter(Object, Object)");
	}
	
	public void merge(Queue that_)
	{
		double[] keys_ = (double[]) that_.keys();
		Element[] elements_ = that_._retrieveAll();
		for (int i=0; i<elements_.length; i++) {
			Element e_ = elements_[i];
			enqueue(keys_[i], e_.getObject());
		}
	}
		
	public void enqueue(Object element_)
	{ enqueue(isEmpty()? 0.0: lastKey(), element_); }
	
	
	public abstract Object dequeue();
	
	public abstract Object dequeue(double key_);
	
	public abstract Object remove(Object element_);

	public abstract Object remove(double key_, Object element_);
	
	public abstract void removeAll(Object element_);
	
	public abstract void removeAll(double key_, Object element_);
	
	public abstract Object remove(int n_);
	
	public abstract Object firstElement();
	
	public abstract double firstKey();
	
	public abstract Object lastElement();
	
	public abstract double lastKey();
	
	public abstract Object retrieveAt(int n_);
	
	public abstract double retrieveKeyAt(int n_);
	
	public abstract Object retrieveBy(double key_);
	
	public abstract Object[] retrieveAll(double key_);
	
	public abstract Object[] retrieveAll();
	
	public abstract Element[] _retrieveAll();
	
	public abstract double retrieveKey(Object o_);
	
	public abstract boolean contains(Object element_);
	
	public abstract boolean containsKey(double key_);
	
	public abstract double[] keys();
	
	public abstract java.util.Enumeration getKeyEnumerator();
	
	public abstract java.util.Enumeration getElementEnumerator();
	
	public abstract int getLength();
	
	public String info(String prefix_, boolean listElement_)
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + ","
						+ getLength() + "\n");
		if (listElement_ && !isEmpty()) {
			double[] keys_ = keys();
			Object[] elements_ = retrieveAll();
			for (int i=0; i<keys_.length; i++)
				sb_.append(prefix_ + keys_[i] + "\t" + elements_[i] + "\n");
		}
		return sb_.toString();
	}

	public String info()
	{ return info("", true); }

	public String info(String prefix_)
	{ return info(prefix_, true); }

	public abstract String oneline();

	/** Prints out for diagnosis.  
	 * The default implementation invokes {@link #info()}. */
	public String diag(boolean listElement_)
	{ return info("", listElement_); }

	public void duplicate(Object source_)
	{
		QueueImpl q_ = (QueueImpl)source_;
		reset();
		if (!q_.isEmpty()) {
			double[] keys_ = q_.keys();
			Object[] elements_ = q_.retrieveAll();
			for (int i=0; i<keys_.length; i++)
				enqueue(keys_[i], elements_[i]);
		}
	}
}
