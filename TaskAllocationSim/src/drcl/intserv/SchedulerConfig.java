// @(#)SchedulerConfig.java   1/2004
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

package drcl.intserv;

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.util.ObjectUtil;
import drcl.util.StringUtil;

/**
The SchedulerConfiguration contract.
This contract defines three services at the reactor:
<dl>
<dt> <code>AdspecProcessing</code>
<dd> The initiator sends a {@link SpecAd} and in response, the reactor sends back
     the upated adspec.
<dt> <code>FlowspecAddition</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 0 (the "add" command);
	<li> the set of ToS values (<code>long[]</code>) associated with the flowspec, could be null;
	<li> the set of ToS mask values (<code>long[]</code>) associated with the ToS values, could be null, and
	<li> the flowspec ({@link SpecFlow}).
	</ol>
     In response, the reactor returns null and the handle number is set in the flowspec
     if the flowspec is sucessfully installed, or returns another flowspec
     that reflects the availability of the resources if failed.
<dt> <code>FlowspecModification</code>
<dd> The initiator sends a message that has the same format as a <code>FlowspecAddition</code>
     except that the first field is an integer of value 1 (the "modify" command).
     The second and the third fields are not used.
     The response is the same as that of <code>FlowspecAddition</code>.
     In case that the request is failed, the previous installation is intact.
<dt> <code>FlowspecRemoval</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> an integer of value 2 (the "remove" command);
	<li> the handle of the flowspec to be removed
	</ol>
     In response, the reactor removes the flowspec and returns the removed flowspec.
<dt> <code>FlowspecQuery</code>
<dd> The initiator sends a message that has the same format as a <code>FlowspecRemoval</code>
     except that the first field is an integer of value 3 (the "query" command).
	 Use negative handle to query all the installed flowspecs.
     In response, the reactor returns the queried flowspec(s).
</dl>

@author Hung-ying Tyan
@version 1.0, 6/2001
*/
public class SchedulerConfig extends Contract
{
	public static final SchedulerConfig INSTANCE = new SchedulerConfig();

	public SchedulerConfig()
	{ super(); }
	
	public SchedulerConfig(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "SchedulerConfiguration Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static SpecFlow add(long[] tos_, long[] tosmask_, SpecFlow fspec_, Port out_)
	{ return (SpecFlow)out_.sendReceive(new Message(ADD, tos_, tosmask_, fspec_)); }
	
	public static SpecFlow modify(SpecFlow fspec_, Port out_)
	{ return (SpecFlow)out_.sendReceive(new Message(MODIFY, null, null, fspec_)); }
	
	public static SpecFlow remove(int handle_, Port out_)
	{ return (SpecFlow)out_.sendReceive(new Message(REMOVE, handle_)); }
	
	public static SpecFlow query(int handle_, Port out_)
	{ return (SpecFlow)out_.sendReceive(new Message(QUERY, handle_)); }
	
	public static SpecFlow[] queryAll(Port out_)
	{ return (SpecFlow[])out_.sendReceive(new Message(QUERY, -1)); }
	
	
	// type
	public static final int ADD = 0; // or modify/replace
	public static final int MODIFY = 1;
	public static final int REMOVE = 2;
	public static final int QUERY = 3;
	static final String[] TYPES = {"add", "modify", "remove", "query"};
	
	// no setter functions are needed for this class
	public static class Message extends drcl.comp.Message
	{
		int type;
		long[] tos;
		long[] tosmask;
		SpecFlow fspec;
		int handle;
		
		public Message ()
		{}

		// for ADD/MODIFY
		public Message (int type_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
		{
			type = type_;
			tos = tos_;
			tosmask = tosmask_;
			fspec = fspec_;
		}
		
		// for REMOVE and QUERY
		public Message (int type_, int handle_)
		{
			type = type_;
			handle = handle_;
		}

		private Message (int type_, long[] tos_, long[] tosmask_, SpecFlow fspec_, int handle_)
		{
			type = type_;
			tos = tos_;
			tosmask = tosmask_;
			fspec = fspec_;
			handle = handle_;
		}

		public int getType()
		{ return type; }

		public long[] getToS()
		{ return tos; }

		public long[] getToSMask()
		{ return tosmask; }

		public SpecFlow getFlowspec()
		{ return fspec; }

		public int getHandle()
		{ return handle; }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			tos = that_.tos == null? null: (long[]) that_.tos.clone();
			tosmask = that_.tosmask == null? null: (long[]) that_.tosmask.clone();
			fspec = that_.fspec == null? null: (SpecFlow)fspec.clone();
			handle = that_.handle;
		}
		*/
	
		public Object clone()
		{ return new Message(type, tos, tosmask, fspec, handle); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			switch (type) {
			case ADD:
				return "SCHEDULER_CONFIG:add" + separator_ + StringUtil.toString(tos)
					+ "/" + StringUtil.toString(tosmask) + separator_ + fspec;
			case MODIFY:
				return "SCHEDULER_CONFIG:modify " + fspec;
			case REMOVE:
				return "SCHEDULER_CONFIG:remove " + handle;
			case QUERY:
				return "SCHEDULER_CONFIG:query " + handle;
			default:
				return "SCHEDULER_CONFIG:unknown";
			}
		}
	}
}
