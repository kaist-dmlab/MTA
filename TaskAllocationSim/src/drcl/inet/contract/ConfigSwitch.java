// @(#)ConfigSwitch.java   1/2004
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

/**
The ConfigurationSwitch contract.
This contract defines the following service at the reactor:
<dl>
<dt> <code>PacketFilterConfiguration</code>
<dd> The initiator sends a message that consists of:
	<ol>
	<li> the filter bank ID (<code>int</code>),
	<li> the filter ID in the bank (<code>int</code>), and 
	<li> the request (<code>Object</code>).
	</ol>
	The reactor will dispatch the request to the corresponding packet filter
	in the {@link drcl.inet.CoreServiceLayer core service layer}.
</dl>
This class also provides a static method
({@link #configure(int, int, Object, drcl.comp.Port) configure(...)})
to facilitate conducting the above service from the specified port.
The method is particularly useful in implementing
a (signaling) protocol that needs to configure the packet filters
in the core service layer.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
@see drcl.inet.CoreServiceLayer
*/
public class ConfigSwitch extends Contract
{
	public static final ConfigSwitch INSTANCE = new ConfigSwitch();

	/**
	 * Sends a configuration request through the configuration switch service of
	 * the CoreServiceLayer.
	 * @return the result.  The format of the result depends on the PktFilter
	 * that has been interacted with.
	 */
	public static Object configure(int bankID_, int filterID_, Object req_, Port out_)
	{
		return out_.sendReceive(new Message(bankID_, filterID_, req_));
	}
	
	public ConfigSwitch()
	{ super(); }
	
	public ConfigSwitch(int role_)
	{ super(role_); }
	
	public String getName()
	{ return "ConfigurationSwitch Contract"; }

	public Object getContractContent()
	{ return null; }

	// no setter functions are needed for this class
	public static class Message extends drcl.comp.Message
	{
		int bankID, filterID;
		Object request;
		
		public Message ()
		{}

		// for ADD
		public Message (int bankID_, int filterID_, Object request_)
		{
			bankID = bankID_;
			filterID = filterID_;
			request = request_;
		}

		public int getBankID()
		{ return bankID; }

		public int getFilterID()
		{ return filterID; }

		public Object getRequest()
		{ return request; }
		
		/*
		public void duplicate(Object source_)
		{
			Message that_ = (Message)source_;
			bankID = that_.bankID;
			filterID = that_.filterID;
			request = drcl.util.ObjectUtil.clone(that_.request);
		}
		*/
	
		public Object clone()
		{ return new Message(bankID, filterID, request); }

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
		{
			return "CONFIG_SWITCH_REQ" + separator_ + "(" + bankID + "," + filterID + ")"
				+ separator_ + drcl.util.StringUtil.toString(request);
		}
	}
}
