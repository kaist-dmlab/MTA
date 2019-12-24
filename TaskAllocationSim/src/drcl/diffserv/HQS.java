// @(#)HQS.java   9/2002
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

package drcl.diffserv;

import drcl.inet.InetPacket;
import drcl.inet.core.Queue;
import drcl.net.Packet;
import drcl.comp.*;
import drcl.data.Map;
import drcl.data.MapKey;

/**
  * This component defines the base class for a hierachical queue set.
  * 
  * <p> Propertes:
  * <ul> 
  * <li> <code>classIDMask</code>: TOS mask for classifying incoming packets
  * </ul>
  *
  * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
  * @version 1.0 10/26/2000   
  * @version 1.1 05/21/2002, Hung-ying Tyan (tyanh@ieee.org)   
  */
public abstract class HQS extends drcl.inet.core.Queue	
{
	protected Map qs;
	protected long classIDMask;
	
	public HQS()
	{ super(); }
	
	public HQS(String id)
	{ super(id); }

	public void duplicate(Object source_) {
		super.duplicate(source_);
		HQS that_ = (HQS)source_;
		classIDMask = that_.classIDMask;
		
		if (that_.qs == null) return;
		if (qs == null) qs = new Map();
		else qs.reset();
		MapKey[] keys_ = that_.qs.getAllKeys();
		for (int i=0; i<keys_.length; i++) {
			MapKey key_ = keys_[i]; // no clone
			Component c_ = (Component)that_.qs.get(key_, Map.MATCH_LONGEST);
			if (c_ == null) continue;
			c_ = getComponent(c_.getID());
			if (c_ == null) continue;
			qs.addEntry(key_, c_);
		}
	}

	/**
	 * Prints the configuration information of this HQS.
	 * Subclasses should override this method to print out their own
	 * parameters.
	 */
	protected String configInfo(String prefix_)
	{ return ""; }

	/**
	 * Prints out the configuration information associated with
	 * the child queue.
	 * Subclasses should override this method to print out their own
	 * parameters.
	 */
	protected String configInfo(String prefix_, Queue q_)
	{ return ""; }

	public synchronized String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(super.info(prefix_));
		sb_.append(prefix_ + "ClassIDMask: #" + drcl.util.StringUtil.toHex(classIDMask, true) + "\n");
		sb_.append(configInfo(prefix_));
		if (qs != null && qs.getSize() > 0) {
			sb_.append(prefix_ + "QueueSet: size " + qs.getSize() + "\n");
			MapKey[] keys_ = qs.getAllKeys();
			for (int i=0; i<keys_.length; i++) {
				MapKey key_ = keys_[i];
				Queue c_ = (Queue)qs.get(key_, Map.MATCH_LONGEST);
				if (c_ == null) continue;
				sb_.append(prefix_ + "   " + DFUtil.printProfileKey(key_)
							+ " ---> " + c_.getID() + configInfo(", ", c_) + "\n");
				sb_.append(c_.info(prefix_ + "      "));
			}
		}
		else
			sb_.append(prefix_ + "No QueueSet is set up.\n");
		return sb_.toString();
	}
			
	void ___Data_Manipulation___(){}

	public synchronized Object enqueue(Object obj_)
	{
		if (qs == null) {
			error("enqueue()", "no QueueSet is set up");
			return null;
		}
		InetPacket pkt = (InetPacket)obj_;
		//classification
		long classID_ = pkt.getTOS() & classIDMask;
		Queue lower_ = (Queue)qs.get(new drcl.data.BitSet(64, classID_), Map.MATCH_LONGEST);
		
		if(lower_ == null){
			error("enqueue()", "no matched HQS for pkt: " + pkt);
			return null;
		}
		else {
			lower_.increaseEnqueCount();
			return lower_.enqueue(pkt);
		}
	}
	
	public synchronized Object dequeue() 
	{
		if (qs == null) return null;
		Queue lower_ = pickEligibleQueue(true);
		return lower_.dequeue();
	}
	
	public Object firstElement()
	{
		Queue q_ = pickEligibleQueue(false);
		if(q_ != null) return q_.firstElement();
		return null;
	}

	void ___QueueSet_Management___(){}
	
	public synchronized void addQueueSet(Queue child_, long classMask, long classId) 
	{
		if (qs == null) qs = new Map();
		MapKey key_ = new MapKey(classMask, classId);
		qs.addEntry(key_, child_);
		addComponent(child_);
	}
	
	public synchronized Queue getQueueSet(long classID)
	{
		if (qs == null) return null;
		return (Queue)qs.get(new drcl.data.BitSet(64, classID), Map.MATCH_LONGEST);
	}
	
	public synchronized void removeQueueSet(Queue leaf_) {
		if (qs == null) return;
		qs.removeEntry(leaf_);
		removeComponent(leaf_);
	}
	
	/**
	 * This method is called when this HQS needs to pick a child queue to
	 * perform dequeue() or firstElement().  Subclasses must implement this method.
	 */
	protected abstract Queue pickEligibleQueue(boolean dequeue_);	

	public void setClassIDMask(long mask_)
	{ classIDMask = mask_; }

	public long getClassIDMask()
	{ return classIDMask; }
	
	public synchronized boolean isEmpty()
	{
		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Queue)) continue;
			Queue q_ = (Queue)cc_[i];
			if (!q_.isEmpty()) return false;
		}
		return true;
	}
	
	public synchronized boolean isFull()
	{
		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Queue)) continue;
			Queue q_ = (Queue)cc_[i];
			if (!q_.isFull()) return false;
		}
		return true;
	}

	public synchronized int getSize()
	{
		int size_ = 0;
		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Queue)) continue;
			Queue q_ = (Queue)cc_[i];
			size_ += q_.getSize();
		}
		return size_;
	}

	public synchronized int getCapacity()
	{
		int size_ = 0;
		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Queue)) continue;
			Queue q_ = (Queue)cc_[i];
			size_ += q_.getCapacity();
		}
		return size_;
	}

	/** Not applicable to this class. */
	public void setCapacity(int size)
	{ drcl.Debug.error("N/A, should set capacity at the leaf queue."); }
}
