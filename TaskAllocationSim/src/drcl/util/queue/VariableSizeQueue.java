// @(#)VariableSizeQueue.java   9/2002
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

/** Inteface for implementing a queue with variable-size elements. 
 * Base implementation is provided by {@link VariableSizeQueueImpl}. */
public interface VariableSizeQueue extends Queue
{
	/**
	 * Enqueues the element with the associated key.
	 * The elements in queue are sorted in the ascending order of their
	 * associated keys.  If same keys appear in the queue,
	 * the element is put right after the last element with the same key.
	 * 
	 * @param key_		the associated key.
	 * @param element_	the element to be put in the queue.
	 * @param size_		size of the element.
	 */
	public void enqueue(double key_, Object element_, int size_);
	
	/**
	 * Enqueues the element at the position specified with the associated key.  
	 * Note that the elements in queue are sorted in the ascending order of 
	 * their associated keys.  Enqueue fails if position and key create a 
	 * conflict to the above condition.
	 *
	 * @param pos_ the position.
	 * @param key_ the associated key.
	 * @param element_ the element to be put in the queue.
	 * @param size_		size of the element.
	 * @return false if key and position create a conflict.
	 */
	public boolean enqueueAt(int pos_, double key_, Object element_,
					int size_);
	
	/**
	 * Enqueues the element right after the <code>previousElement_</code>
	 * element and associates the element with a key equal to the previous
	 * element's.
	 * 
	 * @param previousElement_ the previous element.
	 * @param element_ the element to be put in the queue.
	 * @param size_		size of the element.
	 * @return false if <code>previousElement_</code> does not appear.
	 */
	public boolean enqueueAfter(Object previousElement_, 
					Object element_, int size_);
	
	/**
	 * Associates the element with the largest key in the queue and then 
	 * enqueues the element.
	 * If the queue is originally empty, then key 0.0 is assigned.
	 * 
	 * @param size_		size of the element.
	 */
	public void enqueue(Object element_, int size_);
	

	/** Returns the current size of the queue.  */
	public int getSize();
}
