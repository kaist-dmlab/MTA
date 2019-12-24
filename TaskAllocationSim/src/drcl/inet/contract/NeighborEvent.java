// @(#)NeighborEvent.java   1/2004
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
The NeighborEvent contract.
This contract defines the object format in a neighbor event
(neighbor-up/neighbor-down).
The event object is a message consisting of
<ol>
<li> the index of the interface where this event occurs (<code>int</code>), and
<li> the neighbor information ({@link drcl.inet.data.NetAddress NetAddress}).
</ol>

@author Hung-ying Tyan
@version 1.0, 10/17/2000
@see drcl.inet.data.NetAddress
*/
public class NeighborEvent extends Contract
{
	public static final NeighborEvent INSTANCE = new NeighborEvent();

	public NeighborEvent()
	{ super(); }
	
	public NeighborEvent(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "NeighborEvent Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static class Message extends drcl.comp.Message
	{
		int ifindex;
		NetAddress neighbor;
		
		public Message ()
		{}

		public Message (int ifindex_, NetAddress neighbor_)
		{
			ifindex = ifindex_;
			neighbor = neighbor_;
		}
		
		/** Returns the interface index in the event object. */
		public int getIfIndex()
		{ return ifindex; }
	
		/** Returns the neighbor address in the event struct. */
		public NetAddress getNeighbor()
		{ return neighbor; }
	
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			ifindex = that_.ifindex;
			neighbor = that_.neighbor == null?
				null: (NetAddress)that_.neighbor.clone();
		}
		*/
	
		public Object clone()
		{
			return new Message(ifindex, neighbor);
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{ return "NEIGHBOR:" + neighbor + separator_ + "if:" + ifindex; }
	}
}
