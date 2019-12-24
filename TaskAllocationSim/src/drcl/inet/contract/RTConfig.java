// @(#)RTConfig.java   1/2004
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

package drcl.inet.contract;

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.inet.data.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
The RouteConfiguration contract.
This contract defines five services at the reactor:
<dl>
<dt> <code>RTEntryAddition</code>
<dd> The initiator sends a request that consists of: 
	<ol>
	<li> an integer of value 0 (the "add" command), 
	<li> the key of the entry ({@link drcl.inet.data.RTKey}),
	<li> the entry ({@link drcl.inet.data.RTEntry}) and
	<li> the lifetime for the entry (<code>double</code>).
	</ol>
	The reactor adds (modifies if the entry
	already exists) the entry in the routing
	table and keeps the entry until the lifetime ends.
	The lifetime can be nonpositive or <code>Double.NaN</code> to have the
	entry <u>not</u> removed.  In the case of a modification, zero lifetime
	makes the reactor keep the previous lifetime setting.
<dt> <code>Graft</code>
<dd> The initiator sends a request that has the same format as an "add" request
	except that the first command field is an integer of 1 (the "graft" command).
	Upon receipt of a "graft" request, the reactor modifies the route entry
	as if it were an "add" request except that the reactor grafts the outgoing
	interfaces specified in the route entry in the request, instead of
	replacing with them.
<dt> <code>Prune</code>
<dd> The initiator sends a request that has the same format as an "add" request
	except that the first command field is an integer of 2 (the "prune" command).
	Upon receipt of a "prune" request, the reactor modifies the route entry
	as if it were an "add" request except that the reactor prunes the outgoing
	interfaces specified in the route entry in the request, instead of
	replacing with them.
<dt> <code>RTEntriesRemoval</code>
<dd> The initiator sends a request that consists of:
	<ol>
	<li> an integer of value 3 (the "remove" command), 
	<li> the key to the entries ({@link drcl.inet.data.RTKey}), and
	<li> the method of matching the key to the keys in the routing table.
	</ol>
	For exact match, the key in the request is used to find the entry with
		exactly the same key in the routing table. For all matches, the reactor
	finds all the route entries with their keys matched to only the value
	(the mask field is ignored) of the key in the request. For the longest
		match, the reactor finds the route entry, among all the matched entries,
	with its key being matched  to the value of the key in the request by
	most number of bits. For wildcard match, the reactor find the route
	entries with their keys' values (not including mask) matched to the
	key in the request. All the entries of the matched keys are removed
	and returned. In the cases of exact and longest matches, at most one
	entry is returned in {@link drcl.inet.data.RTEntry}. In other cases,
	an array of matched entries (drcl.inet.data.RTEntry[]) are returned.
<dt> <code>RTEntriesRetrieval</code>
<dd> The format of a retrieval request is the same as a removal except that
	the first command field is an integer of 4 (the "retrieve" command).
	The reactor will return all the matched entries in the same fashion as
	responding a removal request except that the entries are not removed from
	the routing table.
</dl>
This class also provides a set of static methods to faciliate conducting the
above services
({@link #add(drcl.inet.data.RTKey, drcl.inet.data.RTEntry, double, drcl.comp.Port) add(...)},
{@link #graft(drcl.inet.data.RTKey, drcl.data.BitSet, Object, double, drcl.comp.Port) graft(...)},
{@link #prune(drcl.inet.data.RTKey, drcl.data.BitSet, Object, double, drcl.comp.Port) prune(...)},
{@link #remove(drcl.inet.data.RTKey, String, drcl.comp.Port) remove(...)} and
{@link #retrieve(drcl.inet.data.RTKey, String, drcl.comp.Port) retrieve(...)}) from the specified port.
In addition, it provides a 
convenient call {@link #getAllEntries(drcl.comp.Port)} to get all the routing entries from
a specified port.
These methods are particularly useful in implementing
a (routing) protocol that is in charge of maintaining the routing table of
the node.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
@see drcl.inet.data.RTKey
@see drcl.inet.data.RTEntry
*/
public class RTConfig extends Contract
{
	public static final RTConfig INSTANCE = new RTConfig();

	public static final long NEXT_HOP_NO_CHANGE = Address.NULL_ADDR-1;
	public static final drcl.data.BitSet OUT_IFS_NO_CHANGE = new drcl.data.BitSet();
	public static final Object EXTENSION_NO_CHANGE = "DONT_CHANGE_EXTENSION";
	public static final double TIMEOUT_NO_CHANGE = 0.0;
																				  
	public RTConfig()
	{ super(); }
	
	public RTConfig(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "RouteConfiguration Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static void add(RTKey key_, RTEntry entry_, double timeout_, Port out_)
	{ out_.sendReceive(new Message(ADD, key_, entry_, timeout_)); }
	
	public static void graft(RTKey key_, drcl.data.BitSet bs_, Object extension_, double timeout_, Port out_)
	{ out_.sendReceive(new Message(GRAFT, key_, new RTEntry(bs_, extension_), timeout_)); }
	
	public static void prune(RTKey key_, drcl.data.BitSet bs_, Object extension_, double timeout_, Port out_)
	{ out_.sendReceive(new Message(PRUNE, key_, new RTEntry(bs_, extension_), timeout_)); }
	
	public static Object remove(RTKey key_, String match_, Port out_)
	{ return out_.sendReceive(new Message(REMOVE, key_, match_)); }
	
	public static Object retrieve(RTKey key_, String match_, Port out_)
	{ return out_.sendReceive(new Message(RETRIEVE, key_, match_)); }
	
	public static Object getAllEntries(Port out_)
	{
		return out_.sendReceive(new Message(RETRIEVE, new RTKey(0,0,0,0,0,0), drcl.data.Map.MATCH_WILDCARD));
	}
	
	public static Object createAddRequest(RTKey key_, RTEntry entry_, double timeout_)
	{ return new Message(ADD, key_, entry_, timeout_);	}
	
	public static Object createGraftRequest(RTKey key_, drcl.data.BitSet bs_, double timeout_)
	{ return new Message(GRAFT, key_, new RTEntry(bs_, EXTENSION_NO_CHANGE), timeout_);	}
	
	public static Object createGraftRequest(RTKey key_, drcl.data.BitSet bs_, Object extension_,
		double timeout_)
	{ return new Message(GRAFT, key_, new RTEntry(bs_, extension_), timeout_);	}
	
	public static Object createPruneRequest(RTKey key_, drcl.data.BitSet bs_, double timeout_)
	{	return new Message(PRUNE, key_, new RTEntry(bs_, EXTENSION_NO_CHANGE), timeout_);	}
	
	public static Object createPruneRequest(RTKey key_, drcl.data.BitSet bs_, Object extension_,
		double timeout_)
	{	return new Message(PRUNE, key_, new RTEntry(bs_, extension_), timeout_);	}
	
	public static Object createRemoveRequest(RTKey key_, String match_)
	{	return new Message(REMOVE, key_, match_);	}
	
	public static Object createRetrieveRequest(RTKey key_, String match_)
	{	return new Message(RETRIEVE, key_, match_);	}

	public static Object createGetAllRequest()
	{	return new Message(RETRIEVE, new RTKey(0,0,0,0,0,0), drcl.data.Map.MATCH_WILDCARD);	}
	
	
	// type
	/** The "add" command used in the "add an entry" service. */
	public static final int ADD = 0; // or modify/replace
	/** The "graft" command used in the "graft" service. */
	public static final int GRAFT = 1;
	/** The "prune" command used in the "prune" service. */
	public static final int PRUNE = 2;
	/** The "remove" command used in the "remove entries" service. */
	public static final int REMOVE = 3;
	/** The "retrieve" command used in the "retrieve entries" service. */
	public static final int RETRIEVE = 4;
	static final String[] TYPES = {"add", "graft", "prune", "remove", "retrieve"};
	
	// index
	public static final int TYPE = 0;
	public static final int KEY = 1;
	public static final int ENTRY = 2;
	public static final int TIMEOUT = 3;
	public static final int MATCH_TYPE = 2;
	
	// match type
	/**
	 * The exact match argument used in the "remove" or "retrieve" service.
	 * In this type of match,
	 * the key is used to find exactly the same key in the routing table.
	 * Exactly one key or none in the routing table will be matched to the given
	 * key.
	 */
	public static final String MATCH_EXACT = Map.MATCH_EXACT;
	/**
	 * The longest match argument used in the "remove" or "retrieve" service.
	 * In this type of match,
	 * the value of the key is used to 
	 * find the <em>longest</em> match among all matched keys.
	 * Exactly one key or none in the routing table will be matched to the given
	 * key.
	 * @see #MATCH_ALL
	 */
	public static final String MATCH_LONGEST = Map.MATCH_LONGEST;
	/**
	 * The match-all argument used in the "remove" or "retrieve" service.
	 * In this type of match,
	 * only the value of the key is used to find all the matched keys in the
	 * routing table.
	 * There may be more than one key or none at all in the routing table matched to the
	 * given key.
	 */
	public static final String MATCH_ALL = Map.MATCH_ALL;
	/**
	 * The wildcard match argument used in the "remove" or "retrieve" service.
	 * In this type of match,
	 * only the values of the keys in the routing table is used to match
	 * the given key.
	 * There may be more than one key or none at all in the routing table matched to the
	 * given key.  The given key with value 0 and mask 0 will be wildcard matched
	 * to all possible keys.
	 */
	public static final String MATCH_WILDCARD = Map.MATCH_WILDCARD;
	
	public static class Message extends drcl.comp.Message
	{
		int type;
		RTKey key;
		RTEntry entry;
		double timeout;
		String matchtype;
		
		public Message ()
		{}

		public Message (int type_, RTKey key_, RTEntry entry_, double timeout_)
		{
			type = type_;
			key = key_;
			entry = entry_;
			timeout = timeout_;
		}
		
		public Message (int type_, RTKey key_, String matchtype_)
		{
			type = type_;
			key = key_;
			matchtype = matchtype_.toLowerCase().intern();
		}

		private Message (int type_, RTKey key_, RTEntry entry_, double timeout_, String matchType_)
		{
			type = type_;
			key = key_;
			entry = entry_;
			timeout = timeout_;
			matchtype = matchType_;
		}

		public int getType()
		{ return type; }

		public RTKey getKey()
		{ return key; }

		public RTEntry getEntry()
		{ return entry; }

		public double getTimeout()
		{ return timeout; }

		public String getMatchtype()
		{ return matchtype; }
		
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			key = that_.key == null? null: (RTKey)that_.key.clone();
			entry = (RTEntry)ObjectUtil.clone(that_.entry);
			timeout = that_.timeout;
			matchtype = that_.matchtype;
		}
		*/
	
		public Object clone()
		{
			// the contract is between two components; dont clone key and entry
			return new Message(type, key, entry, timeout, matchtype);
		}
	
		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			if (type == REMOVE || type == RETRIEVE)
				return "RTCONFIG:" + TYPES[type] + separator_ + "key:" + key
					+ separator_ + "match:" + matchtype;
			else
				return "RTCONFIG:" + TYPES[type] + separator_ + "key:" + key
					+ separator_ + "entry:" + entry + separator_ + "timeout:" + timeout;
		}
	}
}




