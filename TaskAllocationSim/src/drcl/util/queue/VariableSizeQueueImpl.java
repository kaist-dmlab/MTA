// @(#)VariableSizeQueueImpl.java   9/2002
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

/** Base class for implementing a queue with variable-size elements. */
public abstract class VariableSizeQueueImpl extends QueueImpl
		implements VariableSizeQueue
{
	public void enqueue(double key_, Object element_)
	{ enqueue(key_, element_, 1); }
	
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{ return enqueueAt(pos_, key_, element_, 1); }
	
	public boolean enqueueAfter(Object previousElement_, Object element_)
	{ return enqueueAfter(previousElement_, element_, 1); }
	
	public abstract void enqueue(double key_, Object element_, int size_);
	
	public boolean enqueueAt(int pos_, double key_, Object element_,
					int size_)
	{
		throw new QueueOptionalImplException(getClass() + 
					"doesn't implement enqueueAt(int, double, Object, int)");
	}
	
	public boolean enqueueAfter(Object previousElement_, 
					Object element_, int size_)
	{
		throw new QueueOptionalImplException(getClass() + 
						"doesn't implement enqueueAfter(Object, Object, int)");
	}
	
	public void merge(Queue that_)
	{
		double[] keys_ = (double[]) that_.keys();
		Element[] elements_ = that_._retrieveAll();
		for (int i=0; i<elements_.length; i++) {
			Element e_ = elements_[i];
			if (e_.getSize() == 1)
				enqueue(keys_[i], e_.getObject());
			else
				enqueue(keys_[i], e_.getObject(), e_.getSize()); 
		}
	}
		
	public void enqueue(Object element_, int size_)
	{ enqueue(isEmpty()? 0.0: lastKey(), element_, size_); }
	

	public abstract int getSize();

	public String info(String prefix_, boolean listElement_)
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + getSize() 
						+ "/" + getLength() + "\n");
		if (listElement_ && !isEmpty()) {
			double[] keys_ = keys();
			Object[] elements_ = retrieveAll();
			for (int i=0; i<keys_.length; i++)
				sb_.append(prefix_ + keys_[i] + "\t" + elements_[i] + "\n");
		}
		return sb_.toString();
	}

	public void duplicate(Object source_)
	{
		VariableSizeQueueImpl q_ = (VariableSizeQueueImpl)source_;
		reset();
		if (!q_.isEmpty()) {
			double[] keys_ = q_.keys();
			Element[] elements_ = q_._retrieveAll();
			for (int i=0; i<keys_.length; i++) {
				Element e_ = elements_[i];
				if (e_.getSize() == 1) enqueue(keys_[i], e_);
				else enqueue(keys_[i], e_, e_.getSize());
			}
		}
	}
}
