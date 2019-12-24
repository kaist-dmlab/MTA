// @(#)FiniteVSSimpleQueue.java   9/2002
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
//import drcl.RecycleCan;

/** Variable-size version of {@link SimpleQueue}.  */
public class FiniteVSSimpleQueue extends VSSimpleQueue implements FiniteVSQueue
{
	int capacity = Integer.MAX_VALUE;

	public void enqueue(double key_, Object element_, int size_)
	{
		if (isFull(size_)) return;
		super.enqueue(key_, element_, size_);
	}
	
	public boolean enqueueAfter(Object previousElement_, Object element_, 
					int size_)
	{
		if (isFull(size_)) return false;
		return enqueueAfter(previousElement_, element_, size_);
	}
	
	public boolean enqueueAt(int pos_, double key_, Object element_, int size_)
	{
		if (isFull(size_)) return false;
		return enqueueAt(pos_, key_, element_, size_);
	}
	
	public void merge(Queue that_)
	{
		double[] keys_ = (double[]) that_.keys();
		Element[] elements_ = that_._retrieveAll();
		
		_Element e_ = head;
		
		for (int i=0; i<keys_.length; i++)	{
			int size_ = elements_[i].getSize();
			if (isFull(size_)) return;

			_Element new_ = _Element.create(keys_[i], elements_[i].getObject(), size_);
			for (; e_.next != null; e_ = e_.next)
				if (e_.next.key > keys_[i])	break;
		
			// insert after e_
			new_.next = e_.next;
			e_.next = new_;
			size += size_; length ++;
			if (new_.next == null) tail = new_;
			e_ = new_;
		}
	}
	
	public void enqueue(Object element_, int size_)
	{
		if (isFull(size_)) return;
		super.enqueue(element_, size_);
	}
	
	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + getSize()
						+ "/" + capacity + "," + getLength());
		
		for (_Element e_ = head.next; e_ != null; e_ = e_.next)
			sb_.append("-----" + e_.key + ":" + e_.obj);
		
		return sb_.toString();
	}

	public int getCapacity()
	{ return capacity; }

	public void setCapacity(int cap_)
	{ capacity = cap_; }

	public boolean isFull()
	{ return getSize() == capacity; }

	public boolean isFull(int size_)
	{ return getSize() + size_ > capacity; }
}
