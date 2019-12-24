// @(#)TrafficMonitor.java   1/2004
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

package drcl.net.tool;

import drcl.comp.tool.CountMonitor;
/**
This component monitors incoming traffic (packets) and outputs throughput of
the traffic.  The implementation is moved to {@link CountMonitor} for general
applications.  This component remains for backward compatibility.

It keeps a fixed-size window of packets.  The throughput at current
time is then calculated by summing up the sizes of the packets in the current
window divided by the window size.  Then the results are exported 
every <code>outputInterval</code> second.  The default window size is 5 seconds
and the default output interval is one second.

<p>This component can operate in either "byte" mode, "packet" mode or both.
If the "byte" mode is enabled, the component exports the throughput, in the
unit of bit/second, or bps, at the <code>bytecount@</code> port.
If the "packet" mode is enabled, it exports the throughput, in packet/second, at
the <code>pktcount@</code> port.

<p>The first exported event is in the following format (<code>drcl.comp.contract.EventMsg</code>):
<ul>
<li> Event name: "Throughput" ("byte" mode, see {@link #BYTE_COUNT_EVENT}) or
		"Throughput (packet count)" ("packet" mode, see {@link #PKT_COUNT_EVENT}).
<li> Event object: the calculated throughput in <code>Double</code>.
<li> Event description: <code>null</code>.
</ul>
while the subsequent events are in <code>Double</code>.
 */
public class TrafficMonitor extends CountMonitor
{
	/** ID of the port to export the packet-mode events.*/
	public static final String PKT_COUNT_PORT_ID = "pktcount";
	/** ID of the port to export the byte-mode events.*/
	public static final String BYTE_COUNT_PORT_ID = "bytecount";
	/** Name of the packet-mode events.*/
	public static final String PKT_COUNT_EVENT = "Throughput (packet count)";
	/** Name of the byte-mode events.*/
	public static final String BYTE_COUNT_EVENT = "Throughput";
	
	{
		setEventNames(PKT_COUNT_EVENT, BYTE_COUNT_EVENT);
		objcountPort.setID(PKT_COUNT_PORT_ID);
		sizecountPort.setID(BYTE_COUNT_PORT_ID);
	}
	
	public TrafficMonitor() 
	{ super(); }
	
	public TrafficMonitor(String id_)
	{ super(id_); }
}
