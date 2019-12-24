// @(#)FiniteFIFOQueue.java   11/2003
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

package drcl.util.queue;

import java.util.*;
//import drcl.RecycleCan;

/**
 * Finite-length version of {@link FIFOQueue}.
 */
public class FiniteFIFOQueue extends FIFOQueue implements FiniteQueue
{
	//public static RecycleCan elementCan =new RecycleCan(_Element.class, 5, 5);
	int capacity = Integer.MAX_VALUE;

	public FiniteFIFOQueue()
	{}
												  
	public FiniteFIFOQueue(int capacity_)
	{ capacity = capacity_; }
												  
	public void enqueue(double key_, Object element_)
	{
		if (isFull()) return;
		super.enqueue(key_, element_);
	}
	
	public boolean enqueueAfter(Object previousElement_, Object element_)
	{
		if (isFull()) return false;
		return enqueueAfter(previousElement_, element_);
	}
	
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{
		if (isFull()) return false;
		return enqueueAt(pos_, key_, element_);
	}
	
	
	public void enqueue(Object element_)
	{
		if (isFull()) return;
		super.enqueue(element_);
	}
	
	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(
						super.toString() + "," + getLength() + "/" + capacity);
		
		for (_Element e_ = head.next; e_ != null; e_ = e_.next)
			sb_.append("-----" + e_.key + ":" + e_.obj);
		
		return sb_.toString();
	}

	public int getCapacity()
	{ return capacity; }

	public void setCapacity(int cap_)
	{
		capacity = cap_;
		if (capacity <= 0) {
			capacity = 0;
			head.next = tail = null;
			length = 0;
		}
		else if (getLength() > capacity) {
			_Element e_= head;
			for (int i=0; i<capacity; i++, e_ = e_.next);
		
			e_.next = null;
			length = capacity;
			tail = e_;
		}
	}

	public boolean isFull()
	{ return getLength() == capacity; }
}
