// @(#)RTLookup.java   1/2004
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
import drcl.inet.InetPacket;
import drcl.inet.data.RTKey;

/**
The RouteLookup contract.
This contract defines the following service at the reactor:
<dl>
<dt> <code>RouteLookup</code>
<dd> The initiator sends a key ({@link drcl.inet.data.RTKey}), an 
	{@link drcl.inet.InetPacket InetPacket}, or a message that consists of
	an {@link drcl.inet.InetPacket InetPacket} and the incoming interface (int),
	and the reactor returns the outgoing interfaces (<code>int[]</code>)
	of the correspoding routing entry, the key of which has
	the <em>longest</em> match to the given key/packet.
</dl>
This class also provides the static methods
({@link #lookup(drcl.inet.data.RTKey, drcl.comp.Port) lookup(RTKey, Port)}),
({@link #lookup(drcl.inet.InetPacket, drcl.comp.Port) 
lookup(InetPacket, Port)}), and
({@link #lookup(drcl.inet.InetPacket, int drcl.comp.Port) lookup(InetPacket, 
incomingIf, Port)})
to facilitate conducting the above service from the specified port.
The method is particularly useful in implementing
a protocol that needs to look up the outgoing ports of a routing entry.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
@version 2.0, 11/2002
@see RTConfig	for the description of the longest match.
@see drcl.inet.data.RTKey
@see drcl.inet.data.RTEntry
@see drcl.inet.InetPacket
*/
public class RTLookup extends Contract
{
	public static final RTLookup INSTANCE = new RTLookup();

	public RTLookup()
	{ super(); }
	
	public RTLookup(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "RouteLookup Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static int[] lookup(RTKey key_, Port out_)
	{
		return (int[]) out_.sendReceive(key_);
	}

	/////////////////////////////////////////////////	// added by Will
	public static int[] lookup(InetPacket ipkt_, Port out_)
	{
		return (int[]) out_.sendReceive(ipkt_);
	}

	public static int[] lookup(InetPacket ipkt_, int incomingIf_, Port out_)
	{
		return (int[]) out_.sendReceive(new Message(ipkt_, incomingIf_));
	}

	public static class Message extends drcl.comp.Message
	{
		public InetPacket pkt;
		public int incomingIf;
		
		public Message ()
		{}

		public Message (InetPacket pkt_, int incomingIf_)
		{
			pkt = pkt_;
			incomingIf = incomingIf_;
		}
		
		public int getIncomingIf()
		{ return incomingIf; }

		public InetPacket getPacket()
		{ return pkt; }

		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			incomingIf = that_.incomingIf;
			pkt = that_.pkt == null? null: (InetPacket)that_.pkt.clone();
		}
	
		public Object clone()
		{
			// the contract is between two components; dont clone pkt
			return new Message(pkt, incomingIf);
		}
	
		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "RTLOOKUP:incomingIf=" + incomingIf + separator_
					+ "pkt:" + pkt;
		}
	}

}




