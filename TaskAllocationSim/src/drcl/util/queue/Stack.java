// @(#)Stack.java   9/2002
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

import java.util.Vector;

/**
 * A <i>first-in-last-out</i> queue.
 */
public class Stack extends SimpleQueue
{
	public Stack()
	{ super(); }
	
	public void enqueue(double key_, Object element_)
	{
		_Element e_ = head;
		_Element new_ = new _Element(key_, element_);
		// insert before e_.next
		new_.next = e_.next;
		e_.next = new_;
		length ++;
		if (new_.next == null) tail = new_;
	}
	
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{
		_Element e_= head;
		for (int i=0; i<pos_ && e_.next != null; i++, e_ = e_.next);
		
		// insert after e_
		_Element new_ = new _Element(key_, element_);
		new_.next = e_.next;
		e_.next = new_;
		length ++;
		if (new_.next == null) tail = new_;
		return true;
	}
	
	public void merge(Queue that_)
	{
		double[] keys_ = (double[]) that_.keys();
		Element[] elements_ = that_._retrieveAll();
		
		_Element e_ = head;
		
		for (int i=0; i<keys_.length; i++)	{
			
			_Element new_ = new _Element(keys_[i], elements_[i]);
		
			// insert after e_
			new_.next = e_.next;
			e_.next = new_;
			length ++;
			if (new_.next == null) tail = new_;
			e_ = new_;
		}
	}
	
	
	public void enqueue(Object element_)
	{
		_Element e_ = head;
		_Element new_ = new _Element(e_.next == null? 0.0: e_.next.key, 
						element_);
		// insert before e_.next
		new_.next = e_.next;
		e_.next = new_;
		length++;
		if (new_.next == null) tail = new_;
	}
	
	
	public Object retrieveBy(double key_)
	{
		for (_Element e_ = head; e_.next != null; e_ = e_.next)
			if (e_.next.key == key_) return e_.next.obj;
		return null;
	}
	
	
	
	public Object[] retrieveAll(double key_)
	{
		Vector v_ = new Vector();
		for (_Element e_ = head; e_.next != null; e_ = e_.next)	
			if (e_.next.key == key_) v_.addElement(e_.next.obj);
		Object[] oo_ = new Object[v_.size()];
		v_.copyInto(oo_);
		return oo_;
	}
	
	
	public boolean containsKey(double key_)
	{ 
		for (_Element e_ = head; e_.next != null; e_ = e_.next)
			if (e_.next.key == key_) return true;
		return false;
	}
}
