// @(#)TreeMapQueue.java   9/2002
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
 * Queue that utilizes <code>java.util.TreeMap</code>.
 */
public class TreeMapQueue extends QueueImpl
		implements java.io.Serializable, java.util.Comparator
{
	//public static RecycleCan elementCan = new RecycleCan(_Element.class, 5, 5);
												  
	java.util.TreeMap q = new java.util.TreeMap(this);
	//java.util.TreeMap q = new java.util.TreeMap();
	int length = 0;
	
	public TreeMap getTreeMap()
	{ return q; }

	public int compare(Object obj1_, Object obj2_)
	{
		if (obj1_ == obj2_)
			return 0;
		// put obj1 behind obj2 if they have same key
		else if (((ElementSet)obj1_).key < ((ElementSet)obj2_).key)
			return -1;
		else if (((ElementSet)obj1_).key > ((ElementSet)obj2_).key)
			return 1;
		else 
			return 0;
	}
	
	public void reset()
	{
		q.clear();
		length = 0;
	}
	
	public void enqueue(double key_, Object element_)
	{
		ElementSet new_ = new ElementSet(key_, element_);
		ElementSet set_ = (ElementSet)q.get(new_);
		if (set_ == null) {
			q.put(new_, new_);
		}
		else {
			_Element newe_ = new _Element(key_, element_);
			set_.tail.next = newe_;
			set_.tail = newe_;
		}
		length++;
	}
	
	/** XXX: Not exactly. */
	public boolean enqueueAfter(Object previousElement_, Object element_)
	{
		double key_ = retrieveKey(previousElement_);
		if (Double.isNaN(key_)) return false;
		enqueue(key_, element_);
		return true;
	}
	
	/** XXX: Not exactly. */
	public boolean enqueueAt(int pos_, double key_, Object element_)
	{
		enqueue(key_, element_);
		return true;
	}
	
	
	public void enqueue(Object element_)
	{
		if (length == 0)
			enqueue(0.0, element_);
		else
			enqueue(((ElementSet)q.lastKey()).key +1.0, element_);
	}
	
	public Object dequeue()
	{
		if (length == 0) return null;
		ElementSet e_ = (ElementSet)q.firstKey();
		Object o_ = e_.head.obj;
		length--;
		if (e_.head == e_.tail) {
			q.remove(e_);
		}
		else {
			// remove e_.head:
			_Element tmp_ = e_.head;
			e_.head = e_.head.next;
			tmp_.recycle();
		}
		return o_;
	}
	
	
	public Object dequeue(double key_)
	{
		if (length == 0) return null;
		ElementSet e_ = (ElementSet)q.get(new ElementSet(key_, null));
		if (e_ == null) return null;
		length--;
		Object o_ = e_.head.obj;
		if (e_.head == e_.tail) {
			q.remove(e_);
		}
		else {
			// remove e_.head:
			_Element tmp_ = e_.head;
			e_.head = e_.head.next;
			tmp_.recycle();
		}
		return o_;
	}
	
	// remove element_ from e_
	Object _remove(ElementSet e_, Object element_)
	{
		if (e_.head.obj.equals(element_)) {
			length--;
			Object o_ = e_.head.obj;
			e_.head = e_.head.next;
			if (e_.head == null)
				q.remove(e_);
			return o_;
		}
		else {
			_Element tmp_ = e_.head;
			for (; tmp_.next != null; tmp_ = tmp_.next)
				if (tmp_.next.obj.equals(element_)) break;
			if (tmp_.next == null) return null;
			length--;
			Object o_ = tmp_.next.obj;
			tmp_.next = tmp_.next.next;
			if (tmp_.next == null)
				e_.tail = tmp_;
			return o_;
		}
	}
	
	public Object remove(double key_, Object element_)
	{
		if (length == 0) return null;
		ElementSet e_ = (ElementSet)q.get(new ElementSet(key_, null));
		if (e_ == null) return null;

		/*
		if (e_.head.obj.equals(element_)) {
			length--;
			Object o_ = e_.head.obj;
			e_.head = e_.head.next;
			if (e_.head == null)
				q.remove(e_);
			return o_;
		}
		else {
			_Element tmp_ = e_.head;
			for (; tmp_.next != null; tmp_ = tmp_.next)
				if (tmp_.next.obj.equals(element_)) break;
			if (tmp_.next == null) return null;
			length--;
			Object o_ = tmp_.next.obj;
			tmp_.next = tmp_.next.next;
			if (tmp_.next == null)
				e_.tail = tmp_;
			return o_;
		}
		*/
		return _remove(e_, element_);
	}

	public void dequeueTransfer(FIFOQueue fifo_)
	{
		if (length == 0) return;
		ElementSet es_ = (ElementSet)q.firstKey();
		q.remove(es_);
		int len_ = 0;
		for (_Element e_ = es_.head; e_ != null; e_ = e_.next) {
			len_ ++;
		}
		length -= len_;
		fifo_.length += len_;
		if (fifo_.tail == null) {
			fifo_.head.next = es_.head;
			fifo_.tail = es_.tail;
		}
		else {
			fifo_.tail.next = es_.head;
			fifo_.tail = es_.tail;
		}
	}
	
	public Object remove(Object element_)
	{
		//return remove(retrieveKey(element_), element_);
		for (Iterator it_ = q.keySet().iterator(); it_.hasNext(); ) {
			Object key_ = it_.next();
			ElementSet es_ = (ElementSet)q.get(key_);
			Object o_ = _remove(es_, element_);
			if (o_ != null) return o_;
		}
		return null;
	}
	
	public void removeAll(Object element_)
	{
		if (element_ == null) return;
		while (remove(element_) != null);
	}
	
	
	public void removeAll(double key_, Object element_)
	{
		while (remove(key_, element_) != null);
	}
	
	
	public Object remove(int n_)
	{
		if (length <= n_) return null;
		_Element e_ = ((_Element[])_retrieveAll())[n_];
		return remove(e_.key, e_.obj);
	}
	
	public Object firstElement()
	{
		if (length == 0) return null;
		_Element e_ = ((ElementSet)q.firstKey()).head;
		return e_.obj;
	}
	
	public double firstKey()
	{
		if (length == 0) return Double.NaN;
		_Element e_ = ((ElementSet)q.firstKey()).head;
		return e_.key;
	}
	
	public Object lastElement()
	{
		if (length == 0) return null;
		_Element e_ = ((ElementSet)q.lastKey()).tail;
		return e_.obj;
	}
	
	public double lastKey()
	{
		if (length == 0) return Double.NaN;
		_Element e_ = ((ElementSet)q.lastKey()).tail;
		return e_.key;
	}
	
	public Object retrieveAt(int n_)
	{
		if (length <= n_) return null;
		return ((_Element[])_retrieveAll())[n_].obj;
	}
	
	public double retrieveKeyAt(int n_)
	{
		if (length <= n_) return Double.NaN;
		return ((_Element[])_retrieveAll())[n_].key;
	}
	
	public Object retrieveBy(double key_)
	{
		if (length == 0) return null;
		ElementSet e_ = (ElementSet)q.get(new ElementSet(key_, null));
		if (e_ == null) return null;
		else return e_.head.obj;
	}
	
	public Object[] retrieveAll(double key_)
	{
		_Element[] ee_ = (_Element[])_retrieveAll();
		Vector v_ = new Vector();
		for (int i=0; i<ee_.length; i++) {
			_Element e_ = ee_[i];
			if (e_.key == key_)
				v_.addElement(e_.obj);
		}
		Object[] oo_ = new Object[v_.size()];
		v_.copyInto(oo_);
		return oo_;
	}
	
	public Object[] retrieveAll()
	{
		_Element[] ee_ = (_Element[])_retrieveAll();
		Object[] oo_ = new Object[ee_.length];
		for (int i = 0; i<oo_.length; i++)
			oo_[i] = ee_[i].obj;
		return oo_;
	}
	
	public Element[] _retrieveAll()
	{
		ElementSet[] es_ = (ElementSet[]) q.values().toArray(new ElementSet[0]);
		Vector v_ = new Vector(es_.length);
		for (int i=0; i<es_.length; i++) {
			for (_Element tmp_ = es_[i].head; tmp_ != null; tmp_ = tmp_.next)
				v_.addElement(tmp_);
		}
		_Element[] ee_ = new _Element[v_.size()];
		v_.copyInto(ee_);
		return ee_;
	}
	
	public double retrieveKey(Object o_)
	{
		_Element[] ee_ = (_Element[])_retrieveAll();
		for (int i=0; i<ee_.length; i++) {
			if (ee_[i].obj.equals(o_)) return ee_[i].key;
		}
		return Double.NaN;
	}
	
	public boolean contains(Object element_)
	{ 
		_Element[] ee_ = (_Element[])_retrieveAll();
		for (int i=0; i<ee_.length; i++) {
			if (ee_[i].obj.equals(element_)) return true;
		}
		return false;
	}
	
	public boolean containsKey(double key_)
	{ 
		return q.containsKey(new ElementSet(key_, null));
	}
	
	public double[] keys()
	{
		_Element[] ee_ = (_Element[])_retrieveAll();
		double[] keys_ = new double[length];
		for (int i=0; i<ee_.length; i++)
			keys_[i] = ee_[i].key;
		return keys_;
	}

	public Enumeration getKeyEnumerator()
	{ return new MyEnumerator(this, true); }
	
	public Enumeration getElementEnumerator()
	{ return new MyEnumerator(this, false); }
	
	static class MyEnumerator extends drcl.DrclObj implements Enumeration
	{
		boolean forKey;
		Iterator it;
		_Element current;
		
		public MyEnumerator (TreeMapQueue q_, boolean forKey_)
		{
			forKey = forKey_;
			it = q_.q.values().iterator();
		}
		
		public boolean hasMoreElements()
		{	return current != null || it.hasNext();	}
		
		public Object nextElement()
		{
			if (current == null) {
				ElementSet s_ = (ElementSet)it.next();
				if (s_ == null)
					current = null;
				else
					current = s_.head;
			}
			Object r_ = forKey? new drcl.data.DoubleObj(current.key): 
					current.obj;
			current = current.next;
			return r_;
		}
	}
		
	public int getLength()
	{	return length;	}
	
	public String oneline()
	{
		StringBuffer sb_ = new StringBuffer(super.toString() + "," 
						+ getLength());
		
		_Element[] ee_ = (_Element[])_retrieveAll();
		for (int i=0; i<ee_.length; i++)
			sb_.append("-----" + ee_[i].key + ":" + ee_[i].obj);
		
		return sb_.toString();
	}
	
	protected static class ElementSet implements Comparable
	{
		public ElementSet (double key_, Object o_)
		{
			key = key_;
			head = tail = new _Element(key_, o_);
		}
		
		// we can use this instead of TreeMapQueue.compare(), and pass nothing
		// to the constructor of TreeMap
		public int compareTo(Object obj_)
		{
			if (key < ((ElementSet)obj_).key)
				return -1;
			else if (key > ((ElementSet)obj_).key)
				return 1;
			else 
				return 0;
		}
		
		double  key;
		_Element head, tail;
	}
}
