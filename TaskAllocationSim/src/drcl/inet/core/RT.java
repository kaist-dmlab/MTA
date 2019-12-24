// @(#)RT.java   1/2004
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
import drcl.data.*;
import drcl.comp.*;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.inet.InetConfig;
import drcl.inet.*;

/**
 * The component that manages the routing table of a network node.
 * It does not distinguish unicast and multicast addresses.
 * To be exact, it does not distinguish the types of addresses at all.
 * Basically, all it does is to maintain the mapping from a set of
 * {@link drcl.inet.data.RTKey RTKeys} to the corresponding 
 * {@link drcl.inet.data.RTEntry RTEntries}.
 * 
 * <p>As a service provider, this component provides the services that
 * are described by the following contracts (contract classes) and the bound 
 * ports:
 * <table border=1>
 * <tr> <td> SERVICE <td> PORT </tr>
 * <tr>
 *		<td nowrap>Route Lookup Service<br>({@link drcl.inet.contract.RTLookup 
 *		RTLookup})
 *		<td valign=top><i>.service_rt@</i>
 * </tr>
 * <tr>
 * 		<td>Configuration Service<br>({@link drcl.inet.contract.RTConfig 
 * 		RTConfig})
 *		<td valign=top><i>.service_rt@</i>
 * </tr>
 * </table>
 * 
 * <p>In addition, this component may export the following events:
 * <table border=1>
 * <tr> <td> EVENT <td> PORT <td> DESCRIPTION </tr>
 * <tr>
 *		<td valign=top nowrap>RT Entry Added
 *		<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
 *		<td valign=top>Event object is the entry or array of entries being added.
 * </tr>
 * <tr>
 *		<td valign=top nowrap>RT Entry Removed
 *		<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
 *		<td valign=top>Event object is the entry or array of entries being 
 *		removed.
 * </tr>
 * <tr>
 *		<td valign=top nowrap>RT Entry Modified
 *		<td valign=top nowrap><i>.rt_ucast@</i><br><i>.rt_mcast@</i>
 *		<td valign=top>Event object is a two-element array that consists of 
 *		the old and the new entries.
 * </tr>
 * </table>
 *
 * <p>Properties:
 * <table border=1>
 * <tr> <td> NAME <td> DESCRIPTION </tr>
 * <tr>
 * 		<td valign=top nowrap><i>staticEntries</i>
 *		<td valign=top>Array of the static route entries (never timed out) of 
 *		the node.
 * </tr>
 * </table>
 * 
 * @author Hung-ying Tyan
 * @author Wei-peng Chen
 * @version 1.1, 08/2002
 * @see drcl.inet.contract.RTLookup
 * @see drcl.inet.contract.RTConfig
 * @see drcl.inet.data.RTKey
 * @see drcl.inet.data.RTEntry
 */
public class RT extends drcl.comp.Component implements InetCoreConstants
{
	static {
		Contract c1_ = new RTLookup(Contract.Role_REACTOR);
		Contract c2_ = new RTConfig(Contract.Role_REACTOR);
		setContract(RT.class, SERVICE_RT_PORT_ID + "@" + PortGroup_SERVICE, 
						new ContractMultiple(c1_, c2_));
	}
	Port timerPort = addForkPort(".timer");
	{
		addServerPort(SERVICE_RT_PORT_ID);
	}

	static final long FLAG_RETAIN_STATIC_ENTRIES_ON_RESET  =
			1L << FLAG_UNDEFINED_START;

	public static final String RADIX_TREE = "radix";
	public static final String MULTIPLE_FIELD = "multi-field";
	public static String IMPLEMENTATION = MULTIPLE_FIELD;
	
	drcl.data.Map map = IMPLEMENTATION == RADIX_TREE?
		new drcl.data.RadixMap(): new drcl.data.Map();
	Port urtchange = addEventPort(EVENT_RT_UCAST_CHANGED_PORT_ID);
	Port mrtchange = addEventPort(EVENT_RT_MCAST_CHANGED_PORT_ID);

	public RT() 
	{ super(); }
	
	public RT(String id_)
	{ super(id_); }
	
	public synchronized void reset()
	{
		super.reset();
		if (isRetainStaticEntriesOnResetEnabled()) {
			RTEntry[] all_ = _getAll();
			// remove entries that are dynamically created
			for (int i=0; i<all_.length; i++) {
				Object ext_ = all_[i].getExtension();
				if (all_[i]._getTimeout() >= 0.0)
					_remove(all_[i].getKey(), drcl.data.Map.MATCH_EXACT);
			}
		}
		else if (map != null)
			map.reset(); // remove all entries
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		RT that_ = (RT) source_;
		// duplicate map
		if (that_.map != null)
			map = (drcl.data.Map)that_.map.clone();
	}
	
	public String info()
	{ return info(""); }

	synchronized String info(String prefix_)
	{
		MapKey[] keys_ = map == null? null: map.getAllKeys();
		if (keys_ == null || keys_.length == 0)
			return "No route entries\n";
		StringBuffer sb_ = new StringBuffer("#Entries=" + keys_.length + "\n");
		Object[] entries_ = map.getAllEntries();
		for (int i=0; i<keys_.length; i++) {
			RTEntry entry_ = (RTEntry)entries_[i];
			RTKey key_ = (RTKey)keys_[i];
			Object extension_ = entry_.getExtension();
			if (key_ != null)
				if (entry_ != null) {
					long nexthop_ = entry_.getNextHop();
					sb_.append(prefix_ + key_.print(InetConfig.Addr) + "   \t"
							+ InetConfig.Addr.ltos(nexthop_) + "-"
							//+ (nexthop_ == drcl.net.Address.NULL_ADDR? "??-": 
							//		nexthop_ + "-")
							+ (entry_.getOutIf()==null? "{}": "{" 
									+ entry_.getOutIf().toString() + "}")
							+ (extension_ == null? "": "\t" + extension_) 
							+ "\ttimeout:" + entry_._getTimeout() + "\n");
				}
				else sb_.append(prefix_ + key_.print(InetConfig.Addr)
								+ "\tnull entry\n");
		}
			
		return sb_.toString();
	}

	public void setRetainStaticEntriesOnResetEnabled(boolean enabled_)
	{
		setComponentFlag(FLAG_RETAIN_STATIC_ENTRIES_ON_RESET, enabled_);		
	}

	public boolean isRetainStaticEntriesOnResetEnabled()
	{ return getComponentFlag(FLAG_RETAIN_STATIC_ENTRIES_ON_RESET) != 0; }

	public synchronized String diag()
	{ return map.diag(); }
	
	void _exportEvent(String eventName_, long destination_, Object target_,
					String description_)
	{
		if (InetConfig.Addr.isUnicast(destination_)) {
			if (urtchange._isEventExportEnabled())
				urtchange.exportEvent(eventName_, target_, description_);
		}
		else {
			if (mrtchange._isEventExportEnabled())
				mrtchange.exportEvent(eventName_, target_, description_);
		}
	}
	
	// RT Config contract:
	// 1. type, RTKey, match_type: remove/retrieve
	// 2. type, RTKey, RTEntry, timeout: add/modify/graft/prune
	// 
	// RT Lookup contract: 
	// 1. query: RTKey
	// 2. result: int[]
	// 
	protected void process(Object data_, Port inPort_) 
	{
		if (inPort_ == timerPort) {
			// handle timeout
			RTEntry entry_ = (RTEntry)get((RTKey)data_, RTConfig.MATCH_EXACT);
			if (entry_ == null) return;

			double timeout_ = entry_._getTimeout();
			if (timeout_ > 0.0) {
				// XX: rounding error?
				if (timeout_ <= getTime()) {
					_remove((RTKey)data_, RTConfig.MATCH_EXACT);

					if (_isEventExportEnabled(((RTKey)data_).getDestination()))
						_exportEvent(EVENT_RT_ENTRY_REMOVED, 
							((RTKey)data_).getDestination(), entry_,
							"due to timeout");
				}
				else
					// restart the timer
					entry_.handle = forkAt(timerPort, (RTKey)data_, timeout_);
			}
			return;
		}
		
		// look up
		if (data_ instanceof RTKey) {
			RTEntry e_ = (RTEntry)get((RTKey)data_, RTConfig.MATCH_LONGEST);
			if (e_ == null) inPort_.doLastSending(e_);
			else inPort_.doLastSending(e_._getOutIfs());
			return;
		}
		
		if (!(data_ instanceof RTConfig.Message)) {
			error(data_, "process()", inPort_, "unrecognized data");
			inPort_.doLastSending(null);
			return;
		}
		
		// config 
		
		RTConfig.Message req_ = (RTConfig.Message)data_;
		int type_ = req_.getType();
		RTKey key_ = req_.getKey();
		
		// remove
		if (type_ == RTConfig.REMOVE) {
			inPort_.doLastSending(remove(key_, req_.getMatchtype()));
			return;
		}
		
		// retrieve
		if (type_ == RTConfig.RETRIEVE) {
			inPort_.doLastSending(get(key_, req_.getMatchtype()));
			return;
		}
		
		RTEntry entry_ = req_.getEntry();
		double timeout_ = req_.getTimeout();
		
		// add/modify
		if (type_ == RTConfig.ADD) {
			add(key_, entry_, timeout_);
			inPort_.doLastSending(null);
			return;
		}
			
		if (type_ == RTConfig.GRAFT) {
			_graftprune(true, key_, entry_.getOutIf(), entry_.getExtension(), 
							timeout_);
			inPort_.doLastSending(null);
			return;
		}
			
		if (type_ == RTConfig.PRUNE) {
			_graftprune(false, key_, entry_.getOutIf(), entry_.getExtension(), 
							timeout_);
			inPort_.doLastSending(null);
			return;
		}
			
		error(data_, "process()", inPort_, "unrecognized RT config request");
		inPort_.doLastSending(null);
	}
	
	boolean _isEventExportEnabled(long dest_)
	{
		if (InetConfig.Addr.isUnicast(dest_))
			return urtchange.anyPeer();
		else
			return mrtchange.anyPeer();
	}

	// for PktDispatcher
	// the next hop of the packet will be filled
	public int[] lookup(InetPacket pkt_, long src_, long dest_, int incomingIf_)
	{
		RTKey key_ = new RTKey(src_, dest_, incomingIf_);
		RTEntry e_ = (RTEntry)get(key_, RTConfig.MATCH_LONGEST);
			
		//debug("pkt " + data_ + " entry " + e_ +" outif "
		//	+ ((e_ == null) ? "null" : ("" + e_._getOutIfs())));
			 
		if (e_ == null)
			return null;
		else {
			pkt_.setNextHop(e_.getNextHop());
			return e_._getOutIfs();
		}
	}

	/** Adds a key-entry pair with timeout to the routing table. */
	public void add(RTKey key_, RTEntry entry_, double timeout_)
	{
		entry_.setKey(key_);
		/*
		if (timeout_ > 0.0) {
			entry_._setTimeout(fork(timerPort, key_, timeout_));
		}
		else if (timeout_ == 0.0) {
			entry_._setTimeout(0.0);
		}
		else if (Double.isNaN(timeout_) || timeout_ < 0.0) {
			entry_._setTimeout(Double.NaN);
			cancelFork(timerPort, key_);
		}
		*/
		if (timeout_ > 0.0)
			entry_._setTimeout(getTime() + timeout_);
		else
			entry_._setTimeout(Double.NaN);
		_add(key_, entry_);
	}

	/** Adds a key-entry pair to the routing table. */
	public void add(RTKey key_, RTEntry entry_)
	{
		entry_.setKey(key_);
		entry_._setTimeout(Double.NaN);
		//cancelFork(timerPort, key_);
		_add(key_, entry_);
	}

	synchronized void _add(RTKey key_, RTEntry entry_)
	{
		if (entry_ == null)
			return;
		RTEntry old_ = (RTEntry)map.get(key_, drcl.data.Map.MATCH_EXACT);
		boolean eventEnabled_ = _isEventExportEnabled(key_.getDestination());
		if (old_ != null) {
			// modify: compare each field of the two entries
			RTEntry copy_ = eventEnabled_? (RTEntry)old_.clone(): null;
			boolean changed_ = false;
			// outgoing interfaces
			drcl.data.BitSet outIf_ = entry_.getOutIf();
			if (outIf_ != RTConfig.OUT_IFS_NO_CHANGE 
				&& !drcl.util.ObjectUtil.equals(outIf_, old_.getOutIf())) {
				old_.setOutIf(outIf_);
				changed_ = true;
			}
			// extension
			Object extension_ = entry_.getExtension();
			if (extension_ != RTConfig.EXTENSION_NO_CHANGE 
				&& !drcl.util.ObjectUtil.equals(extension_, 
						old_.getExtension())) {
				old_.setExtension(extension_);
				changed_ = true;
			}
			// timeout
			double newTimeout_ = entry_._getTimeout();
			double oldTimeout_ = old_._getTimeout();
			if (newTimeout_ == RTConfig.TIMEOUT_NO_CHANGE) {
				// no change
			}
			else {
				if (newTimeout_ > 0.0 && newTimeout_ < oldTimeout_) {
					cancelFork(old_.handle);
					old_.handle = forkAt(timerPort, key_, newTimeout_);
				}
				else if (newTimeout_ <= 0.0) 
					newTimeout_ = Double.NaN;
				old_._setTimeout(newTimeout_);
				changed_ = true;
				// timeout operation is taken care of at the caller
			}
			// next hop:
			if (entry_.getNextHop() != RTConfig.NEXT_HOP_NO_CHANGE 
				&& old_.getNextHop() != entry_.getNextHop()) {
				old_.setNextHop(entry_.getNextHop());
				changed_ = true;
			}
			// export an event if there's any change
			if (changed_ && eventEnabled_)
				_exportEvent(EVENT_RT_ENTRY_MODIFIED, key_.getDestination(), 
								new RTEntry[]{copy_, old_}, "");
		}
		else { // add
			double newTimeout_ = entry_._getTimeout();
			if (newTimeout_ == RTConfig.TIMEOUT_NO_CHANGE)
				entry_._setTimeout(newTimeout_ = Double.NaN);
			else if (newTimeout_ > 0.0)
				entry_.handle = forkAt(timerPort, key_, newTimeout_);
			if (entry_.getNextHop() == RTConfig.NEXT_HOP_NO_CHANGE)
				entry_.setNextHop(drcl.net.Address.NULL_ADDR);
			if (key_ != null) map.addEntry(key_, entry_);
			if (eventEnabled_)
				_exportEvent(EVENT_RT_ENTRY_ADDED, key_.getDestination(), 
								entry_, "");
		}
	}
	
	/** Grafts the interfaces to the entry exactly matched by the given key. */
	public void graft(RTKey key_, drcl.data.BitSet bs_)
	{ _graftprune(true, key_, bs_, RTConfig.EXTENSION_NO_CHANGE, -1.0); }
	
	/** Grafts the interfaces to the entry exactly matched by the given key. */
	public void graft(RTKey key_, drcl.data.BitSet bs_, double timeout_)
	{ _graftprune(true, key_, bs_, RTConfig.EXTENSION_NO_CHANGE, timeout_); }
	
	public synchronized void _graftprune(boolean graft_, RTKey key_, 
					drcl.data.BitSet bs_, Object ext_,
		double timeout_)
	{
		if (bs_ == null || bs_.getNumSetBits() == 0) return;
		RTEntry old_ = (RTEntry) get(key_, drcl.data.Map.MATCH_EXACT);
		if (old_ == null) return;
		
		boolean eventEnabled_ = _isEventExportEnabled(key_.getDestination());
		RTEntry copy_ = eventEnabled_? (RTEntry)old_.clone(): null;
		drcl.data.BitSet outIf_ = old_.getOutIf();
		if (graft_)	outIf_.or(bs_);
		else outIf_.clearBy(bs_);
		
		boolean changed_ = false;
		// extension
		if (ext_ != RTConfig.EXTENSION_NO_CHANGE 
			&& !drcl.util.ObjectUtil.equals(ext_, old_.getExtension())) {
			old_.setExtension(ext_);
			changed_ = true;
		}
		
		if (timeout_ != RTConfig.TIMEOUT_NO_CHANGE) {
			if (timeout_ > 0.0) {
				timeout_ = getTime() + timeout_;
				double oldTimeout_ = old_._getTimeout();
				changed_ = oldTimeout_ != timeout_;
				old_._setTimeout(timeout_);
				if (Double.isNaN(oldTimeout_))
					old_.handle = forkAt(timerPort, key_, timeout_);
			}
			else {
				changed_ = old_._getTimeout() > 0.0;
				old_._setTimeout(Double.NaN);
			}
		}
		// export event
		if (eventEnabled_ && (changed_ || !outIf_.equals(copy_.getOutIf())))
			_exportEvent(EVENT_RT_ENTRY_MODIFIED, key_.getDestination(),
							new RTEntry[]{copy_, old_},
							graft_? "(graft)": "(prune)");
	}
	
	/** Prunes the interfaces from the entry exactly matched by the given key.
	 */
	public void prune(RTKey key_, drcl.data.BitSet bs_)
	{ _graftprune(false, key_, bs_, RTConfig.EXTENSION_NO_CHANGE, -1.0); }

	/** Prunes the interfaces from the entry exactly matched by the given key. 
	 */
	public void prune(RTKey key_, drcl.data.BitSet bs_, double timeout_)
	{ _graftprune(false, key_, bs_, RTConfig.EXTENSION_NO_CHANGE, timeout_); }

	/**
	 * Returns the matched entry(-ies).
	 *
	 * @param key_ the key to match.
	 * @param matchType_ {@link drcl.data.Map#MATCH_EXACT},
	 * 		{@link drcl.data.Map#MATCH_LONGEST},
	 *		{@link drcl.data.Map#MATCH_ALL} or
	 *		{@link drcl.data.Map#MATCH_WILDCARD}.
	 * @return the matched entry or array of the matched entries.
	 */
	public synchronized Object get(RTKey key_, String matchType_)
	{
		Object o_ = map.get(key_, matchType_);
		if (o_ instanceof Object[]) {
			Object[] oo_ = (Object[])o_;
			RTEntry[] ee_ = new RTEntry[oo_.length];
			System.arraycopy(oo_, 0, ee_, 0, oo_.length);
			return ee_;
		}
		else
			return o_;
	}
	
	/** Returns all the route entry(-ies) stored in this component. */
	public synchronized RTEntry[] _getAll()
	{
		Object[] oo_ = (Object[])map.get(new RTKey(0,0,0,0,0,0), 
						drcl.data.Map.MATCH_WILDCARD);
		RTEntry[] ee_ = new RTEntry[oo_.length];
		System.arraycopy(oo_, 0, ee_, 0, oo_.length);
		return ee_;
	}
	
	/**
	 * Removes the matched entry(-ies).
	 *
	 * @param key_ the key to match.
	 * @param matchType_ {@link drcl.data.Map#MATCH_EXACT},
	 * 		{@link drcl.data.Map#MATCH_LONGEST},
	 *		{@link drcl.data.Map#MATCH_ALL} or
	 *		{@link drcl.data.Map#MATCH_WILDCARD}.
	 * @return the matched entry or array of the matched entries.
	 */
	public synchronized Object remove(RTKey key_, String matchType_)
	{
		Object o_ = _remove(key_, matchType_);
		if (o_ != null && _isEventExportEnabled(key_.getDestination())) 
			_exportEvent(EVENT_RT_ENTRY_REMOVED, key_.getDestination(), o_, "");
		return o_;
	}
	
	/** Removes and returns all the route entries stored in this component. */
	public synchronized Object clear()
	{
		return remove(new RTKey(0,0,0,0,0,0), drcl.data.Map.MATCH_WILDCARD);
	}
	
	synchronized Object _remove(RTKey key_, String matchType_)
	{
		Object o_ = map.remove(key_, matchType_);
		// cancel timeouts if necessary
		if (o_ == null) return null;
		else if (o_ instanceof RTEntry) {
			RTEntry e_ = (RTEntry)o_;
			if (e_.handle != null) {
				cancelFork(e_.handle);
				e_.handle = null;
			}
			return o_;
		}
		else {
			Object[] oo_ = (Object[])o_;
			if (oo_.length == 0) return null;
			RTEntry[] ee_ = new RTEntry[oo_.length];
			System.arraycopy(oo_, 0, ee_, 0, oo_.length);
			for (int i=0; i<ee_.length; i++)
				if (ee_[i].handle != null) {
					cancelFork(ee_[i].handle);
					ee_[i].handle = null;
				}
			return ee_;
		}
	}
	
	/** Returns the static route entries (entries never timed out). */
	public synchronized RTEntry[] getStaticEntries()
	{
		RTEntry[] all_ = _getAll();
		Vector v_ = new Vector();
		for (int i=0; i<all_.length; i++) {
			if (all_[i]._getTimeout() >= 0.0) continue;
			Object ext_ = all_[i].getExtension();
			if (ext_ == null || ext_ == HOST_ENTRY_EXT)
				v_.addElement(all_[i]);
		}
		all_ = new RTEntry[v_.size()];
		v_.copyInto(all_);
		return all_;
	}
	
	/** Sets the static route entries (entries never timed out). */
	public synchronized void setStaticEntries(RTEntry[] all_)
	{
		for (int i=0; i<all_.length; i++)
			add(all_[i].getKey(), all_[i]);
	}
}
