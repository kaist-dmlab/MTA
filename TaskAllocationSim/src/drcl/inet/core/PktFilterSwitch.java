// @(#)PktFilterSwitch.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

import drcl.data.*;
import drcl.comp.*;
import drcl.inet.contract.ConfigSwitch;

/**
 * The component which routes packet filter configuration request to appropriate packet filters.
 * The switch has one port for each packet filter.
 * All these ports are divided into "filter banks".
 * The following describes what filter banks are and the formation of the port
 * ID.
 * A filter bank consists of one or more packet filters and is formed such that
 * if a packet has to go through one of them, it must go through all of them
 * in the filter bank.  A packet filter must belong to one or more filter bank.
 * 
 * One port group in the switch is dedicated to one filter bank and the port 
 * group IDs are simply numbered from 0 to (number of banks -1).  
 * The IDs of the ports (filter ports) which connect to the packet filters 
 * of a bank are simply numbered from 0 to (number of filters -1).  
 * For example, filter port 1@0 connects to filter 1 
 * of the bank 0.  Filter ports of the same ID are expected to connect to
 * filters of the same class.
 * 
 * <ul>
 * <li>Ports:
 *		<ul>
 *		<li>"config" port group: follows PacketFilter Switch Contracts.
 *		<li> filter bank port groups: ports connecting to fiters follow PacketFilter Configuration Contracts.
 *		</ul>
 * </ul>
 */
public class PktFilterSwitch extends drcl.comp.Component 
	implements drcl.inet.InetConstants
{
	static {
		setContract(PktFilterSwitch.class, 
			SERVICE_CONFIGSW_PORT_ID + "@" + PortGroup_SERVICE,
			new ConfigSwitch(Contract.Role_REACTOR));
	}
	
	{
		addServerPort(SERVICE_CONFIGSW_PORT_ID);
	}
	
	public PktFilterSwitch()
	{ super(); }
	
	public PktFilterSwitch(String id_)
	{ super(id_); }
	
	protected void process(Object data_, drcl.comp.Port inPort_)
	{
		//XXX: wildcard spec
		if (!(data_ instanceof ConfigSwitch.Message)) {
			error(data_, "process()", inPort_, "unrecognized data");
			return;
		}
		ConfigSwitch.Message req_ = (ConfigSwitch.Message)data_;
		int bankID_ = req_.getBankID();
		int filterID_ = req_.getFilterID();
		Port p_ = getPort(String.valueOf(bankID_), String.valueOf(filterID_));
		Object request_ = req_.getRequest();
		Object reply_ = p_.sendReceive(request_);
		inPort_.doLastSending(reply_);
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	/** A utility method to connect between the manager and the packet filter 
	 * bank. */
	public void connect(int bankID_, Component[] ff_)
	{
		String pgid_ = String.valueOf(bankID_);
		for (int i=0; i<ff_.length; i++) {
			Port peer_ = ff_[i].getPort(PktFilter.CONFIG_PORT_ID);
			if (peer_ == null) continue;
			addPort(pgid_, String.valueOf(i)).connect(peer_);
		}
	}
}
