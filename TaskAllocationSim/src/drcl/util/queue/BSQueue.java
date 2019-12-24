// @(#)BSQueue.java   9/2002
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

/**
 * Queue with double type of keys.
 * Although PriorityQueue implements Serializable, for the class to be it,
 * the stored objects must be serializable also.
 */
public class BSQueue extends QueueImpl implements java.io.Serializable
{
	//public static RecycleCan elementCan = new RecycleCan(__Element.class, 5, 5);
												  
	__Element head = new __Element(Double.NaN, null);
	int length;

	{ head.next = head.prev = head; }
	
	public void reset()
	{
		/*
		// recycle all the Elements
		__Element e_= head.next;
		while (e_ != null) {
			__Element tmp_ = e_.next;
			e_.recycle();
			e_ = tmp_;
		}
		*/
		head.next = head.prev = head;
		length = 0;
	}
	
	// codes are almost identical to enqueue(double, Object, int)
	public void enqueue(double key_, Object element_)
	{
		__Element e_ = head;
		__Element new_ = new __Element(key_, element_);
		for (; e_.prev != head; e_ = e_.prev) 
			if (e_.prev.key <= key_) break;
		
		// insert after e_.prev: e_.prev <-> new_ <-> e_
		new_.prev = e_.prev;
		e_.prev = new_;
		new_.prev.next = new_;
		new_.next = e_;
		length ++;
	}
	
	public boolean enqueueAfter(Object previousElement_, Object element_)
	{
		double key_ = retrieveKey(previousElement_);
		if (Double.isNaN(key_)) return false;
		
		__Element new_ = new __Element(key_, element_);
		__Element e_ = head.next;
		for (; e_.obj != previousElement_; e_ = e_.next);
		
		// insert after e_: e_ <-> new_ <-> e_.next
		new_.next = e_.next;
		e_.next = new_;
		new_.prev = e_;
		new_.next.prev = new_;
		length ++;
		return true;
	}
	
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{
		__Element e_= head;
		for (int i=0; i<pos_ && e_.next != head; i++, e_ = e_.next);
		if (e_.key > key_) return false;
		if (e_.next != head && e_.next.key < key_) return false;
		
		// insert after e_: e_ <-> new_ <-> e_.next
		__Element new_ = new __Element(key_, element_);
		new_.next = e_.next;
		e_.next = new_;
		new_.prev = e_;
		new_.next.prev = new_;
		length ++;
		return true;
	}
	
	
	public void enqueue(Object element_)
	{
		__Element new_ = new __Element(length == 0? 0.0: head.prev.key, 
						element_);
		new_.prev = head.prev;
		head.prev = new_;
		new_.prev.next = new_;
		new_.next = head;
		length ++;
	}
	
	public Object dequeue()
	{
		if (length == 0) return null;
		__Element e_ = head.next;
		Object o_ = e_.obj;
		head.next = e_.next;
		e_.next.prev = head;
		length --;
		//e_.recycle();
		return o_;
	}
	
	public Object dequeue(double key_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next)	
			if (e_.next.key == key_) {
				__Element out_ = e_.next;
				Object o_ = out_.obj;
				e_.next = out_.next;
				out_.next.prev = e_;
				length --;
				//out_.recycle();
				return o_;
			}
		return null;
	}
	
	
	public Object remove(Object element_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next) {
			__Element tmp_ = e_.next;
			Object o_ = tmp_.obj;
			if (o_ == element_ || o_ != null && o_.equals(element_)) {
				e_.next = tmp_.next;
				tmp_.next.prev = e_;
				length --;
				//tmp_.recycle();
				return o_;
			}
		}
		return null;
	}
	
	public Object remove(double key_, Object element_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next) {
			__Element tmp_ = e_.next;
			Object o_ = tmp_.obj;
			if (tmp_.key == key_ && (o_ == element_ || o_ != null && o_.equals(element_))) {
				e_.next = tmp_.next;
				tmp_.next.prev = e_;
				length --;
				//tmp_.recycle();
				return o_;
			}
		}
		return null;
	}
	
	public void removeAll(Object element_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next) {
			__Element tmp_ = e_.next;
			Object o_ = tmp_.obj;
			if (o_ == element_ || o_ != null && o_.equals(element_)) {
				e_.next = tmp_.next;
				tmp_.next.prev = e_;
				length --;
				//tmp_.recycle();
			}
		}
	}
	
	
	public void removeAll(double key_, Object element_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next) {
			__Element tmp_ = e_.next;
			Object o_ = tmp_.obj;
			if (tmp_.key == key_ &&	(o_ == element_ || o_ != null && o_.equals(element_)) )	{
				e_.next = tmp_.next;
				tmp_.next.prev = e_;
				length --;
				//tmp_.recycle();
			}
			else if (tmp_.key > key_) break;
		}
	}
	
	
	public Object remove(int n_)
	{
		__Element e_= head;
		for (int i=0; i<n_ && e_.next != head; i++, e_ = e_.next);
		
		if (e_.next == head) return null;
		__Element tmp_ = e_.next;
		Object o_ = tmp_.obj;
		e_.next = tmp_.next;
		tmp_.next.prev = e_;
		length --;
		//tmp_.recycle();
		
		return o_;
	}
	
	public Object firstElement()
	{ return length == 0? null: head.next.obj; }
	
	public double firstKey()
	{ return length == 0? Double.NaN: head.next.key; }
	
	public Object lastElement()
	{ return length == 0? null: head.prev.obj; }
	
	public double lastKey()
	{ return length == 0? Double.NaN: head.prev.key; }
	
	public Object retrieveAt(int n_)
	{
		__Element e_= head.next;
		for (int i=0; i<n_ && e_ != head; i++, e_ = e_.next);
		
		return e_ == head? null: e_.obj; 
	}
	
	public double retrieveKeyAt(int n_)
	{
		__Element e_= head.next;
		for (int i=0; i<n_ && e_ != head; i++, e_ = e_.next);
		
		return e_ == head? Double.NaN: e_.key; 
	}
	
	public Object retrieveBy(double key_)
	{
		for (__Element e_ = head; e_.next != head; e_ = e_.next)
			if (e_.next.key == key_) return e_.next.obj;
			else if (e_.next.key > key_) return null;
		return null;
	}
	
	public Object[] retrieveAll(double key_)
	{
		Vector v_ = new Vector();
		for (__Element e_ = head; e_.next != head; e_ = e_.next)	
			if (e_.next.key == key_) v_.addElement(e_.next.obj);
			else if (e_.next.key > key_) break;
		Object[] oo_ = new Object[v_.size()];
		v_.copyInto(oo_);
		return oo_;
	}
	
	public Object[] retrieveAll()
	{
		Object[] all_ = new Object[length];
		__Element e_ = head.next;
		for (int i=0; i<all_.length; i++) {
			all_[i] = e_.obj;
			e_ = e_.next;
		}
		return all_;
	}
	
	public Element[] _retrieveAll()
	{
		Element[] all_ = new Element[length];
		__Element e_ = head.next;
		for (int i=0; i<all_.length; i++) {
			all_[i] = e_;
			e_ = e_.next;
		}
		return all_;
	}
	
	public double retrieveKey(Object o_)
	{
		for (__Element e_ = head.next; e_ != head; e_ = e_.next)
			if (e_.obj.equals(o_)) return e_.key;
		return Double.NaN;
	}
	
	public boolean contains(Object element_)
	{ 
		for (__Element e_ = head.next; e_ != head; e_ = e_.next)
			if (e_.obj.equals(element_)) return true;
		return false;
	}
	
	public boolean containsKey(double key_)
	{ 
		for (__Element e_ = head; e_.next != head; e_ = e_.next)
			if (e_.next.key == key_) return true;
			else if (e_.next.key > key_) return false;
		return false;
	}
	
	public double[] keys()
	{
		double[] keys_ = new double[length];
		__Element e_ = head.next;
		for (int i=0; i<keys_.length; i++) {
			keys_[i] = e_.key;
			e_ = e_.next;
		}
		return keys_;
	}

	public Enumeration getKeyEnumerator()
	{ return new MyEnumerator(this, true); }
	
	public Enumeration getElementEnumerator()
	{ return new MyEnumerator(this, false); }
	
	static class MyEnumerator extends drcl.DrclObj implements Enumeration
	{
		boolean forKey;
		__Element current;
		BSQueue q;
		
		public MyEnumerator (BSQueue q_, boolean forKey_)
		{
			forKey = forKey_;
			current = q_.head;
			q = q_;
		}
		
		public boolean hasMoreElements()
		{	return current.next != q.head;	}
		
		public Object nextElement()
		{
			if (current.next == q.head) return null;
			Object r_ = forKey? new drcl.data.DoubleObj(current.next.key): current.next.obj;
			current = current.next;
			return r_;
		}
	}
		
	public int getLength()
	{	return length;	}
	
	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," + 
						getLength());
		
		for (__Element e_ = head.next; e_ != head; e_ = e_.next)
			sb_.append("-----" + e_.key + ":" + e_.obj);
		
		return sb_.toString();
	}
	
	protected static class __Element extends drcl.DrclObj implements Element
	{
		public __Element (double key_, Object o_)
		{
			key = key_;
			obj = o_;
			next = null;
		}
		
		public void recycle()
		{
			obj = null;
			next = null;
			//elementCan.recycle(this);
		}
		
		public Object getObject()
		{ return obj; }
		
		public int getSize()
		{ return 1; }
		
		double  key;
		Object  obj;
		__Element next, prev;
	}
}
