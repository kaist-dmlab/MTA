// @(#)McastHostEvent.java   1/2004
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
import drcl.util.ObjectUtil;
import drcl.inet.data.*;

/**
The McastHostEvent contract.
This contract defines the message format in a multicast host event
(a join or leave event).  The event is delivered to a multicast routing
protocol component to handle the event when a host joins or leaves a 
multicast group.

<p>The event object is a message consisting of:
<ol>
<li> Event type: (<code>JOIN</code> or <code>LEAVE</code>),
<li> Multicast group: the host network joins/leaves,
<li> Index of the interface: at which that host network is connected to this router.
	The index may be less than zero if the router is multihomed and it joins/leaves
	itself.
</ol>

<p>This class provides a set of static methods to faciliate constructing
the event object ({@link #createJoinEvent(long, int) createJoinEvent()s} and
{@link #createLeaveEvent(long, int) createLeaveEvent()}s).

@author Hung-ying Tyan
@version 1.0, 12/03/2000
*/
public class McastHostEvent extends Contract
{
	public static final McastHostEvent INSTANCE = new McastHostEvent();

	public McastHostEvent()
	{ super(); }
	
	public McastHostEvent(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "McastHostEvent Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	/** Returns the JOIN event message. */
	public static Message createJoinEvent(long group_, int ifindex_)
	{ return new Message(JOIN, 0, 0, group_, ifindex_); }
	
	/** Returns the JOIN event message. */
	public static Message createJoinEvent(long src_, long group_, int ifindex_)
	{ return new Message(JOIN, src_, -1, group_, ifindex_); }
	
	/** Returns the JOIN event message. */
	public static Message createJoinEvent(long src_, long srcmask_, long group_, int ifindex_)
	{ return new Message(JOIN, src_, srcmask_, group_, ifindex_); }
	
	/** Returns the LEAVE event message. */
	public static Message createLeaveEvent(long group_, int ifindex_)
	{ return new Message(LEAVE, 0, 0, group_, ifindex_); }
	
	/** Returns the LEAVE event message. */
	public static Message createLeaveEvent(long src_, long group_, int ifindex_)
	{ return new Message(LEAVE, src_, -1, group_, ifindex_); }
	
	/** Returns the LEAVE event message. */
	public static Message createLeaveEvent(long src_, long srcmask_, long group_, int ifindex_)
	{ return new Message(LEAVE, src_, srcmask_, group_, ifindex_); }
	
	public static final int JOIN  = 0;
	public static final int LEAVE = 1;
	static final String[] TYPES = {"join", "leave"};
	
	public static class Message extends drcl.comp.Message
	{
		int type, ifindex;
		long group, src, srcmask;
		
		public Message ()
		{}
		
		public Message (int type_, long src_, long srcmask_, long group_, int ifindex_)
		{
			type = type_;
			group = group_;
			src = src_;
			srcmask = srcmask_;
			ifindex = ifindex_;
		}
		
		/** Returns true if the message is a join event. */
		public boolean isJoin()
		{ return type == JOIN; }
	
		/** Returns true if the event struct is a leave event. */
		public boolean isLeave()
		{ return type == LEAVE; }

		/** Returns the group field from the event struct. */
		public long getGroup()
		{ return group; }
	
		/** Returns the source field from the event struct. */
		public long getSource()
		{ return src; }
	
		/** Returns the source field from the event struct. */
		public long getSourceMask()
		{ return srcmask; }
	
		/** Returns the interface index from the event struct. */
		public int getIfIndex()
		{ return ifindex; }
	
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			type = that_.type;
			group = that_.group;
			src = that_.src;
			srcmask = that_.srcmask;
			ifindex = that_.ifindex;
		}
		*/
	
		public Object clone()
		{ return new Message(type, src, srcmask, group, ifindex); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "MCAST_HOST:" + TYPES[type] + separator_ + "src:" + src
				+ separator_ + "srcmask:" + srcmask + separator_ + "group:" + group
				+ separator_ + "if:" + ifindex;
		}
	}
}
