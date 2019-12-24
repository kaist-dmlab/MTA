// @(#)PktFilter.java   9/2002
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

import drcl.comp.Port;
import drcl.net.*;

/**
 * <ul>
 * <li>Ports: (otherwise listed in <code>drcl.net.Module</code>)
 *		<ul>
 *		<li>"up"/"down" port group: follows Below Packet Dispatcher Contract.
 *		<li>"config@service" port: follows PktFilter Configuration Contract.
 *		<li>".trace" port: packet arrival/transmission/departure, on/off events.
 *		</ul>
 * </ul>
 */
public class PktFilter extends Module implements InetCoreConstants
{
	{
		upPort.setType(Port.PortType_IN);
		downPort.setType(Port.PortType_OUT);
	}

	/** For a subclass to create the configuration port for other components to configure this component. */
	public Port createConfigPort()
	{ return addServerPort(CONFIG_PORT_ID); }
	
	public PktFilter()
	{ super(); }
	
	public PktFilter(String id_)
	{ super(id_); }
	
	/**
	 * The callback which handles the event when a packet arrives at the up port.
	 * Default behavior is relay the packet to the down port.
	 */
	protected void dataArriveAtUpPort(Object data_, Port upPort_)
	{	downPort.doLastSending(data_);	}
	
	/**
	 * The callback which handles the event when a packet arrives at the down port.
	 * Default behavior is relay the packet to the up port.
	 */
	protected void dataArriveAtDownPort(Object data_, Port downPort_)
	{	upPort.doLastSending(data_);	}
	
	
	/**
	 * Query/configure requests arrive here.
	 */
	protected void processOther(Object data_, Port inPort_)
	{
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
}




