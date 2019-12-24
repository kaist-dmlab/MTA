// @(#)IDConfig.java   1/2004
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
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
The IdentityConfiguration contract.
This contract defines three services at the reactor:
<dl>
<dt> <code>IdentityAddition</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 0 (the "add" command),
	<li> the set of identities (<code>long[]</code>) to be added, and
	<li> the set of corresponding life periods (<code>double[]</code>).
	</ol>
	In response, the reactor adds the set of identities to its database and 
	each identity will stay for the time specified in the corresponding period
	and then removed.  One may specify a negative value or <code>Double.NaN</code>
	as the period value to have the corresponding identity <u>not</u> removed.
<dt> <code>IdentityRemoval</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 1 (the "remove" command) and
	<li> the set of identities (<code>long[]</code>) to be removed.
	</ol>
	In response, the reactor removes the set of identities from its database.
<dt> <code>IdentityTimeoutQuery</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 2 (the "query" command) and
	<li> the set of identities (<code>long[]</code>) in question.
	</ol>
	In response, the reactor sends back the set of time values (<code>double[]</code>),
	each of which corresponds to the time when the corresponding identity will
	be removed.
</dl>
This class also provides a set of static methods to faciliate conducting the
above services (<code>add(..., Port)</code>, <code>remove(..., Port)</code> and
<code>query(..., Port)</code>) from the specified port.
These methods are particularly useful in implementing
a protocol that is in charge of maintaining the identities of the node.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
*/
public class IDConfig extends Contract
{
	public static final IDConfig INSTANCE = new IDConfig();

	public IDConfig()
	{ super(); }
	
	public IDConfig(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "IdentityConfiguration Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static void add(long id_, double timeout_, Port out_)
	{ out_.sendReceive(new Message(ADD, new long[]{id_}, new double[]{timeout_})); }
	
	public static void add(long[] id_, double[] timeout_, Port out_)
	{ out_.sendReceive(new Message(ADD, id_, timeout_)); }
	
	public static void remove(long id_, Port out_)
	{ out_.sendReceive(new Message(REMOVE, new long[]{id_})); }
	
	public static void remove(long[] id_, Port out_)
	{ out_.sendReceive(new Message(REMOVE, id_)); }
	
	public static double query(long id_, Port out_)
	{
		double[] o_ = (double[])out_.sendReceive(new Message(QUERY, new long[]{id_}));
		return o_[0];
	}
	
	public static double[] query(long[] id_, Port out_)
	{ return (double[])out_.sendReceive(new Message(QUERY, id_)); }
	
	public static Object createAddRequest(long id_)
	{	return new Message(ADD, new long[]{id_}, new double[]{-1.0});	}
	
	public static Object createAddRequest(long id_, double timeout_)
	{	return new Message(ADD, new long[]{id_}, new double[]{timeout_});	}
	
	public static Object createAddRequest(long[] id_, double[] timeout_)
	{	return new Message(ADD, id_, timeout_);	}
	
	public static Object createRemoveRequest(long id_)
	{	return new Message(REMOVE, new long[]{id_});	}
	
	public static Object createRemoveRequest(long[] id_)
	{	return new Message(REMOVE, id_);	}
	
	public static Object createQueryRequest()
	{	return new Message(QUERY, null);	}
	
	public static Object createQueryRequest(long id_)
	{	return new Message(QUERY, new long[]{id_});	}
	
	public static Object createQueryRequest(long[] id_)
	{	return new Message(QUERY, id_);	}
	
	
	// type
	public static final int ADD = 0; // or modify/replace
	public static final int REMOVE = 1;
	public static final int QUERY = 2;
	static final String[] TYPES = {"add", "remove", "query"};
	
	// no setter functions are needed for this class
	public static class Message extends drcl.comp.Message
	{
		int type;
		long[] ids;
		double[] timeouts;
		
		public Message ()
		{}

		// for ADD
		public Message (int type_, long[] ids_, double[] timeout_)
		{
			type = type_;
			ids = ids_;
			timeouts = timeout_;
		}
		
		// for REMOVE and QUERY
		public Message (int type_, long[] ids_)
		{
			type = type_;
			ids = ids_;
			timeouts = null;
		}

		public int getType()
		{ return type; }

		public long[] getIDs()
		{ return ids; }

		public double[] getTimeouts()
		{ return timeouts; }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			ids = that_.ids == null? null: (long[]) that_.ids.clone();
			if (type == ADD)
				timeouts = that_.timeouts == null? null: (double[])that_.timeouts.clone();
		}
		*/
	
		public Object clone()
		{ return new Message(type, ids, timeouts); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "IDCONFIG:" + TYPES[type] + separator_ + StringUtil.toString(ids)
				+ (timeouts == null? "": separator_ + StringUtil.toString(timeouts));
		}
	}
}
