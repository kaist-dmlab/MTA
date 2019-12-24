// @(#)DatagramContract.java   1/2004
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

/**
The datagram sending/delivery contract.
This contract defines the data exchange formats between two protocol layers
that use datagrams.  Specifically, the contract defines the following message format:
<ol>
<li> the packet to be sent (<code>Object</code>),
<li> the source address (<code>long</code>),
<li> the destination address (<code>long</code>),
<li> the port (<code>int</code>), and
<li> the type of service (<code>long</code>).
</ol>
When sending a datagram, one may specify the source address as 
{@link Address#NULL_ADDR} to have the lower
layer to put in an appropriate address for the upper layer.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
*/
public class DatagramContract extends Contract
{
	public static final DatagramContract INSTANCE = new DatagramContract();

	public DatagramContract()
	{ super(); }
	
	public DatagramContract(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "Datagram Contract"; }
	
	public Object getContractContent()
	{ return null; }
	
	public static class Message extends drcl.comp.Message
	{
		Object pkt;
		long src, dest, tos;
		int port, pktsize;
		
		public Message ()
		{}

		/**
		 * Datagram sending contract between application and transport.
		 * 
		 * @param data_ the datagram.
		 * @param src_ source.
		 * @param dest_ destination.
		 * @param destPort_ destination port number.
		 * @param tos_ type of service.
		 */
		public Message (Object data_, int size_, long src_, long dest_,
									  int port_, long tos_)
		{
			pkt = data_;
			pktsize = size_;
			src = src_;
			dest = dest_;
			port = port_;
			tos = tos_;
		}
		
		public Object getContent()
		{ return pkt; }
	
		public int getSize()
		{ return pktsize; }
	
		public long getSource()
		{ return src; }
	
		public void setSource(long src_)
		{ src = src_; }
	
		public long getDestination()
		{ return dest; }
	
		public long getTOS()
		{ return tos; }
	
		public int getDestinationPort()
		{ return port; }
	
		public int getSourcePort()
		{ return port; }

		public int getPort()
		{ return port; }

		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			pkt = that_.pkt instanceof drcl.ObjectCloneable?
				((drcl.ObjectCloneable)that_.pkt).clone(): that_.pkt;
			pktsize = that_.pktsize;
			src = that_.src;
			dest = that_.dest;
			port = that_.port;
			tos = that_.tos;
		}
		*/
	
		public Object clone()
		{
			// the contract is between two components; dont clone pkt
			return new Message(pkt, pktsize, src, dest, port, tos);
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "DATAGRAM" + separator_ + "src:" + src
				+ separator_ + "dest:" + dest + separator_ + "port:" + port
				+ separator_ + "tos:" + tos + separator_ + "content:" + pktsize + ","
				+ drcl.util.StringUtil.toString(pkt);
		}
	}
}
