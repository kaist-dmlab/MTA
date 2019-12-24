// @(#)Queue.java   9/2002
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

/** Interface for implementing a queue.
 * Each element in a queue is associated with a key.
 * In general, the elements in a queue is sorted in the ascending order
 * of their keys.  
 * Specific types of queue may have different mechanisms
 * to order the elements.  "Enqueue" and "dequeue" are two common operations
 * on a queue.
 * An "enqueue" operation inserts an element to the queue based on its
 * mechanism of ordering.  
 * A "dequeue" operation removes the first element in the queue.
 *
 * <p>Base implementation is provided by {@link QueueImpl}. */
public interface Queue 
{
	/** Empties the queue. */
	public void reset();
	
	/** Returns true if the queue is empty. */
	public boolean isEmpty();
	
	/**
	 * Enqueues the element with the associated key.  
	 * The elements in queue are sorted in the ascending order of their 
	 * associated keys.  If same keys appear in the queue,
	 * the element is put right after the last element with the same key.
	 * 
	 * @param key_ the associated key.
	 * @param element_ the element to be put in the queue.
	 */
	public void enqueue(double key_, Object element_);
	
	/**
	 * Enqueues the element at the position specified with the associated key.  
	 * Note that the elements in queue are sorted in the ascending order of 
	 * their associated keys.  Enqueue fails if position and key create a 
	 * conflict to the above condition.
	 *
	 * @param pos_ the position.
	 * @param key_ the associated key.
	 * @param element_ the element to be put in the queue.
	 * @return false if key and position create a conflict.
	 */
	public boolean enqueueAt(int pos_, double key_, Object element_);

	
	/**
	 * Enqueues the element right after the <code>previousElement_</code> 
	 * element and 
	 * associates the element with a key equal to the previous element's.
	 * 
	 * @param previousElement_ the previous element.
	 * @param element_ the element to be put in the queue.
	 * @return false if <code>previousElement_</code> does not appear.
	 */
	public boolean enqueueAfter(Object previousElement_, 
					Object element_);
	
	/**
	 * Enqueues the elements in <code>that_</code> by the order of
	 * <code>that_.dequeue()</code>.
	 */
	public void merge(Queue that_);
		
	/**
	 * Associates the element with the largest key in the queue and then 
	 * enqueues the element.
	 * If the queue is originally empty, then key 0.0 is assigned.
	 */
	public void enqueue(Object element_);
	
	
	/**
	 * Dequeues and returns the element with the smallest key.
	 * If two keys are identical, then first-in-first-out.
	 * 
	 * @return the element with the smallest key, null if the queue is empty.
	 */
	public Object dequeue();
	
	/**
	 * Dequeues and returns the first element with the key matched the argument.
	 * 
	 * @return the element with the key matched the argument, null if no key 
	 * 		is matched or the queue is empty.
	 */
	public Object dequeue(double key_);
	
	/**
	 * Removes the first element that <code>equals()</code> the argument.
	 * @return the element found; null if no element is matched.
	 */
	public Object remove(Object element_);

	/**
	 * Removes the first element that has the same key and <code>equals()</code>
	 * the argument.
	 * @return the element found; null if no element is matched.
	 */
	public Object remove(double key_, Object element_);
	
	/** Removes all the elements that <code>equals()</code> the argument. */
	public void removeAll(Object element_);
	
	/** Removes all the elements that match both the argument key and element.*/
	public void removeAll(double key_, Object element_);
	
	/**
	 * Removes and returns the n<em>th</em> element in the queue.
	 * Returns null if the current size of the queue is smaller than (n+1).
	 */
	public Object remove(int n_);
	
	/**
	 * Returns the first element in the queue, no dequeue is performed.
	 * Returns null if the queue is empty.
	 */
	public Object firstElement();
	
	/**
	 * Returns the first key in the queue, no dequeue is performed.
	 * Returns <code>java.lang.Double.NaN</code> if the queue is empty.
	 */
	public double firstKey();
	
	/**
	 * Returns the last element in the queue, no dequeue is performed.
	 * Returns null if the queue is empty.
	 */
	public Object lastElement();
	
	/**
	 * Returns the last key in the queue, no dequeue is performed.
	 * Returns <code>java.lang.Double.NaN</code> if the queue is empty.
	 */
	public double lastKey();
	
	/**
	 * Returns the n<em>th</em> element in the queue, no dequeue is performed.
	 * @return null if the current length of the queue is smaller than (n+1).
	 */
	public Object retrieveAt(int n_);
	
	/**
	 * Returns the n<em>th</em> key in the queue, Double.NaN if the current 
	 * length of the queue is smaller than (n+1).
	 */
	public double retrieveKeyAt(int n_);
	
	/**
	 * Returns the first element with the key matched to the argument.  
	 * No dequeue is performed.
	 * @return <code>null</code> if no match is found.  
	 */
	public Object retrieveBy(double key_);
	
	/**
	 * Returns all the elements with the keys matched to the argument.
	 * No dequeue is performed.
	 */
	public Object[] retrieveAll(double key_);
	
	/**
	 * Returns all the elements in the queue sorted in the ascending order of 
	 * the key values and the order of enqueues.
	 */
	public Object[] retrieveAll();
	
	/**
	 * Returns all the elements in the queue sorted in the ascending order of
	 * the key values and the order of enqueues.
	 */
	public Element[] _retrieveAll();
	
	/** Returns the key of the first matched element in this queue, 
	 * Double.NaN if no match is found. */
	public double retrieveKey(Object o_);
	
	/** Returns true if the queue contains the element. */
	public boolean contains(Object element_);
	
	/** Returns true if the queue contains the key.  */
	public boolean containsKey(double key_);
	
	/** Returns all the keys in the queue.  Duplicate keys may appear. */
	public double[] keys();
	
	public java.util.Enumeration getKeyEnumerator();
	
	public java.util.Enumeration getElementEnumerator();
	
	/** Returns the current length of the queue. */
	public int getLength();
	
	/**
	 * Prints the content of the queue.
	 * @param prefix_ prefix of each line when printing.
	 */
	public String info(String prefix_, boolean listElement_);

	/** Prints the content of the queue. */
	public String info();

	/** Prints the content of the queue. */
	public String info(String prefix_);

	/** Prints the content of the queue in one line of string.  */
	public String oneline();

	/** Prints out for diagnosis. */ 
	public String diag(boolean listElement_);
}
