// @(#)Identity.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.core;

import java.util.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.net.Address;
import drcl.inet.contract.IDConfig;
import drcl.inet.contract.IDLookup;

/**
 * The component that manages the identities of the network node.
 * 
 * <p>Contracts:
 * <ul>
 * <li> <code>.service_id@</code>: {@link drcl.inet.contract.IDLookup IDLookup}.
 * <li> <code>.service_id@</code>: {@link drcl.inet.contract.IDConfig IDConfig}.
 * </ul>
 * 
 * <p>Events:
 * <ul>
 * <li> <code>.id@</code>: Type "Identity Added", Object is the identities
 * being added in
 *		<code>drcl.data.LongObj</code> or <code>long[]</code>.
 * <li> <code>.id@</code>: Type "Identity Removed", Object is the identity
 * being removed in
 *		<code>drcl.data.LongObj</code> or <code>long[]</code>.
 * </ul>
 * 
 * <p>Properties:
 * <ul>
 * <li> <code>defaultID</code>: the default identity of the node.
 * <li> <code>IDs</code>: array of the static identities (won't be timed out)
 * of the node.
 * </ul>
 * 
 * @author Hung-ying Tyan
 * @version 1.0, 10/17/2000
 * @see drcl.inet.contract.IDLookup
 * @see drcl.inet.contract.IDConfig
 */
public class Identity extends drcl.comp.Component implements InetCoreConstants
{ 
	static {
		Contract c1_ = new IDLookup(Contract.Role_REACTOR);
		Contract c2_ = new IDConfig(Contract.Role_REACTOR);
		setContract(Identity.class, SERVICE_ID_PORT_ID + "@"
						+ PortGroup_SERVICE, new ContractMultiple(c1_, c2_));
	}

	static final ACATimer NEVER_TIMED_OUT = new ACATimer();
	
	long defaultIdentity = Address.NULL_ADDR;
	Hashtable groups = new Hashtable(); // identity (long) -> timer
	Port idchange = addEventPort(EVENT_ID_CHANGED_PORT_ID);
	Port timerPort = addForkPort(".timer");

   
	{
		addServerPort(SERVICE_ID_PORT_ID);
	}
	
	public Identity()
	{ super(); }
	
	public Identity(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		if (groups != null) {
			for (Enumeration e = groups.keys() ; e.hasMoreElements() ;)  {
				LongObj key_ = (LongObj)e.nextElement();
				if (key_ == null) continue;
				ACATimer timer_ = (ACATimer)groups.get(key_);
				if (timer_ != NEVER_TIMED_OUT)
					remove(key_.value);
			}
		}
	}
	
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		Identity that_ = (Identity)source_;
		defaultIdentity = that_.defaultIdentity;
		if (that_.groups != null) {
			if (groups == null) groups = new Hashtable();
			groups.clear();
			for (Enumeration e = that_.groups.keys() ; e.hasMoreElements() ;)  {
				LongObj key_ = (LongObj)e.nextElement();
				if (key_ == null) continue;
				ACATimer timer_ = (ACATimer)that_.groups.get(key_);
				if (timer_ == NEVER_TIMED_OUT)
					groups.put(key_.clone(), timer_);
				else {
					LongObj newKey_ = new LongObj(key_.value);
					groups.put(newKey_, forkAt(timerPort, newKey_,
											timer_.getTime()));
				}
			}
		}
		else
			groups = null;
	}
	
	protected synchronized void process(Object data_, drcl.comp.Port inPort_) 
	{ 
		if (inPort_ == timerPort) {// handle timeout
			LongObj key_ = (LongObj)data_;
			boolean isDefault_ = key_.value == defaultIdentity;
			key_ = _remove(key_);
			if (key_ != null && idchange._isEventExportEnabled())
				idchange.exportEvent(EVENT_IDENTITY_REMOVED, key_,
							_getRemoveEventDescription(key_, true, isDefault_));
			return;
		}
		
		// config
		if (data_ instanceof IDConfig.Message) 
		{
			//if (isDebugEnabled())
			//	debug("IDConfig: " + drcl.util.StringUtil.toString(data_));
			IDConfig.Message struct_ = (IDConfig.Message)data_;
			int cm_ = struct_.getType();
			
			if (cm_ == IDConfig.ADD)  {
				// add/replace/query
				long[] ids_ = struct_.getIDs();
				double[] timeouts_ = struct_.getTimeouts();
				for (int i=0; i<ids_.length; i++) add(ids_, timeouts_);
				inPort_.doLastSending(null);
			}
			else { // remove or query
				long[] ids_ = struct_.getIDs();
				if (cm_ == IDConfig.REMOVE) {
					remove(ids_);
					inPort_.doLastSending(null);
				}
				else if (ids_ != null) {
					inPort_.doLastSending(queryTimeout(ids_));
				}
				else {// query all
					long[] all_ = _getAll();
					Object[] reply_ = new Object[]{all_, queryTimeout(all_)};
					inPort_.doLastSending(reply_);
				}
			}
			return;
		}
		
		// lookup
		try {
			//if (isDebugEnabled())
			//	debug("IDLookup: " + drcl.util.StringUtil.toString(data_));
			if ( data_ instanceof IntObj )  {
				int type_ = ((IntObj)data_).value;
				if (type_ == IDLookup.GET_DEFAULT)
					inPort_.doLastSending(new LongObj(getDefaultID()));
				else
					inPort_.doLastSending(_getAll());
			}
			else {
				inPort_.doLastSending(query((long[])data_));
			}
		}
		catch (Exception e_) {
			error(data_, "process()", inPort_, "lookup error: " + e_);
			inPort_.doLastSending(null);
		}
	}
	//
	private void ___SCRIPT___() {}
	//
	
	/**
	 * Sets the default identity.  This method is also the setter method
	 * of the "defaultID" property.
	 * @param defaultIdentity_ the default identity.
	 */
	public synchronized void setDefaultID(long defaultIdentity_ )  {
		add(defaultIdentity_, -1.0/*no timeout*/);
	    defaultIdentity = defaultIdentity_;
	}
	
	/** Returns the default identity. */
	public synchronized long getDefaultID()  
	{ return defaultIdentity; }
	
	/** Adds the identities to the database. */
	public synchronized void setIDs(long[] ids_)
	{
		if (ids_ == null || ids_.length == 0) return;
		for (int i=0; i<ids_.length; i++)
			add(ids_[i], -1.0);
	}
	
	/** Returns all the static identities stored in this component. */
	public synchronized long[] getIDs()
	{
		try {
		long[] all_ = new long[groups.size()];
		int i = 0;
		for (Enumeration e = groups.keys() ; e.hasMoreElements() ;)  {
			LongObj key_ = (LongObj)e.nextElement();
			ACATimer timer_ = (ACATimer)groups.get(key_);
			if (timer_ != NEVER_TIMED_OUT) continue;
			all_[i++] = key_.value;
		}
		long[] result_ = new long[i];
		for (int j=0; j<i; j++) result_[j] = all_[j];
		return result_;
		}
		catch (Exception e_) {
			e_.printStackTrace();
			return new long[0];
		}
	}
	
	/** Adds a new identity without timeout. */
	public synchronized void add(long newID_)
	{ add(newID_, -1.0/*not timed out*/);	}
	
	/**
	 * Adds a new identity with timeout.
	 * @param newID_ new identiy to be added.
	 * @param timeouDuration_ timeout duration 
	 */
	public synchronized void add(long newID_, double timeoutDuration_)
	{
		LongObj key_ = _add(newID_, timeoutDuration_);
		if (key_ == null ||!idchange._isEventExportEnabled()) return;
		idchange.exportEvent(EVENT_IDENTITY_ADDED, key_,
							 _getAddEventDescription(key_));
	}
	
	/**
	 * Adds a list of identities with timeouts.
	 * @param newID_ an array of identities to add.
	 * @param timeouDuration_ an array of timeout durations. 
	 */
	public synchronized void add(long[] ids_, double[] timeouts_)
	{
		if (ids_ == null) return;
		
		Vector v_ = new Vector();
		for (int i=0; i<ids_.length; i++) {
			Object o_ = _add(ids_[i], timeouts_[i]);
			if (o_ != null) v_.addElement(o_);
		}
		
		if (v_.size() == 0 || !idchange._isEventExportEnabled()) return;
		ids_ = new long[v_.size()];
		StringBuffer s_ = new StringBuffer();
		for (int i=0; i<ids_.length; i++) {
			LongObj key_ = (LongObj)v_.elementAt(i);
			ids_[i] = key_.value;
			s_.append(_getAddEventDescription(key_) + ".\n");
		}
		idchange.exportEvent(EVENT_IDENTITY_ADDED, ids_, s_.toString());
	}
	
	String _getAddEventDescription(LongObj key_)
	{
		ACATimer timer_ = (ACATimer)groups.get(key_);
		if (key_.value == defaultIdentity)
			if (timer_ == NEVER_TIMED_OUT)
				return "Default identity is set to " + key_;
			else
				return "Default identity is set to " + key_
					   + " and will be timed out at " + timer_.getTime();
		else {
			if (timer_ == NEVER_TIMED_OUT)
				return "Identity:" + key_ + " is added";
			else
				return "Identity:" + key_
					+ " is added, and will be timed out at " + timer_.getTime();
		}
	}
	
	// returns Key if the id is newly added to this component
	// returns null if the id already exists.
	LongObj _add(long newID_, double timeoutDuration_)
	{
		LongObj key_ = new LongObj(newID_);
		double timeout_ = Double.NaN;
		
		// check if previous installation exists

		if (groups.containsKey(key_)) {
			// get old copy
			ACATimer timer_ = (ACATimer)groups.get(key_);
			if (timeoutDuration_ >= 0.0) {
				timeout_ = timeoutDuration_ + getTime();
				if (timer_ == NEVER_TIMED_OUT) 
					groups.put(key_, forkAt(timerPort, key_, timeout_));
				else if (timeout_ != timer_.getTime()) {
					// reset timer
					cancelFork(timer_);
					groups.put(key_, forkAt(timerPort, key_, timeout_));
				}
			}
			else if (timer_ != NEVER_TIMED_OUT) {
				cancelFork(timer_);
				groups.put(key_, NEVER_TIMED_OUT);
			}
			return null;
		}
		else {
			// new id
			if (timeoutDuration_ >= 0.0) {
				timeout_ = getTime() + timeoutDuration_;
				groups.put(key_, forkAt(timerPort, key_, timeout_));
			}
			else
				groups.put(key_, NEVER_TIMED_OUT);

			// set the first identity to be the default
			if (groups.size() == 1)
				defaultIdentity = newID_;
			return key_;
		}
	}
	
	
	/** Removes an identity from the identity database. */
	public void remove(long id_) 
	{
		boolean isDefault_ = id_ == defaultIdentity;
		LongObj key_ = _remove(new LongObj(id_));
		if (key_ == null || !idchange._isEventExportEnabled()) return;
		idchange.exportEvent(EVENT_IDENTITY_REMOVED, key_,
						_getRemoveEventDescription(key_, false, isDefault_));
	}
	
	/** Removes multiple identities. */
	public synchronized void remove(long[] ids_) 
	{
		if (ids_ == null) return;
		
		boolean eventEnabled_ = idchange._isEventExportEnabled();
		Vector v_ = new Vector(); // save ids that are gonna be removed
		int index_ = -1; // index (in v_) of default id
		for (int i=0; i<ids_.length; i++) {
			boolean isDefault_ = ids_[i] == defaultIdentity;
			LongObj key_ = _remove(new LongObj(ids_[i]));
			if (eventEnabled_ && key_ != null) {
				v_.addElement(key_);
				if (isDefault_) index_ = v_.size()-1;
			}
		}
		if (v_.size() == 0 || !eventEnabled_) return;
		// construct event message
		ids_ = new long[v_.size()];
		StringBuffer s_ = new StringBuffer();
		for (int i=0; i<ids_.length; i++) {
			LongObj key_ = (LongObj)v_.elementAt(i);
			ids_[i] = key_.value;
			s_.append(_getRemoveEventDescription(key_, false, i == index_)
							+ ".\n");
		}
		idchange.exportEvent(EVENT_IDENTITY_REMOVED, ids_, s_.toString());
	}
	
	LongObj _remove(LongObj key_)
	{
		if (key_.value == defaultIdentity)
			defaultIdentity = Address.NULL_ADDR;
		ACATimer timer_ = (ACATimer)groups.remove(key_);
		if (timer_ != null && timer_ != NEVER_TIMED_OUT)
			cancelFork(timer_);
		return timer_ != null? key_: null;
	}
	
	String _getRemoveEventDescription(LongObj key_, boolean timeout_,
									  boolean isDefault_)
	{
		return (isDefault_? "Default identity:": "Identity:")
			   + key_ + " is removed"
			   + (timeout_? " due to timeout": "");
	}
	
	/** Queries the existence of an identity in this database. */
	public synchronized boolean query(long id_) 
	{
		return defaultIdentity == id_ 
			   || groups.containsKey(new LongObj(id_)); 
	}
	
	/**
	 * Queries the existence of multiple identities in this database.
	 * @param ids_ the identities in question.
	 * @return array of results.  The result is true if the corresponding
	 * identity exists.
	 */
	public synchronized boolean[] query(long[] ids_) 
	{
		if (ids_ == null) return new boolean[0];
		boolean[] result_ = new boolean[ids_.length];
		for (int i=0; i<result_.length; i++)
			result_[i] = query(ids_[i]);
		return result_;
	}
	
	/** Queries the time instance when the identity will be timed out.
     * Returns the time instance; Double.NaN if the identity does not exist.
     */	
	public synchronized double queryTimeout(long id_)
	{
		ACATimer timer_ = (ACATimer)groups.get(new LongObj(id_));
		return timer_ == null || timer_ == NEVER_TIMED_OUT?
				Double.NaN: timer_.getTime();
	}
	
	/**
     * Queries the time instances when the identities will be timed out.
     * Returns array of the time instances.  A time instance may be Double.NaN
	 * if the corresponding identity does not exist.
     */	
	public synchronized double[] queryTimeout(long[] ids_)
	{
		if (ids_ == null) return new double[0];
		
		double[] times_ = new double[ids_.length];
		LongObj dummy_ = new LongObj();
		for (int i=0; i<ids_.length; i++) {
			dummy_.value = ids_[i];
			ACATimer timer_ = (ACATimer)groups.get(dummy_);
			times_[i] = timer_ == null || timer_ == NEVER_TIMED_OUT?
					Double.NaN: timer_.getTime();
		}
		return times_;
	}
	
	/** Returns all the identities stored in this database. */
	public synchronized long[] _getAll()  
	{
		long[] ids_ = new long[groups.size()];
		int i = 0;
		for (Enumeration e = groups.keys() ; e.hasMoreElements() ;) 
			ids_[i++] = ((LongObj)e.nextElement()).value;
		return ids_;
	}
	
	/** Outputs the identities stored in this database and their timeout
	 * instances. */
	public String info()
	{ return info(""); }

	/** Outputs the identities stored in this database and their timeout
	 * instances. */
	synchronized String info(String prefix_)
	{
		long[] ids_ = _getAll();
		if (ids_.length == 0) return "No identity\n";
		
		StringBuffer sb_ = new StringBuffer("#Identities=" + groups.size()
						+ "\n");
		Enumeration keys_ = groups.keys();
		Enumeration elements_ = groups.elements();
		for(; keys_.hasMoreElements(); ) {
			ACATimer timer_ = (ACATimer)elements_.nextElement();
			long key_ = ((LongObj)keys_.nextElement()).value;
			String longFormat_ = drcl.inet.InetConfig.Addr.ltos(key_);
			if (!longFormat_.equals(String.valueOf(key_)))
				longFormat_ = longFormat_ + " (" + key_ + ")";
			sb_.append(prefix_ + longFormat_ + "\ttimeout "
				+ (timer_ == NEVER_TIMED_OUT?
						"--": String.valueOf(timer_.getTime())) + "\n");
		}
		return sb_.toString();
	}
}
