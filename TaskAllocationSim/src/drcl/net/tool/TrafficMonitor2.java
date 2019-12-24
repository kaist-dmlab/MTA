// @(#)TrafficMonitor2.java   1/2004
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

import drcl.comp.tool.CountMonitor2;
/**
This component monitors the incoming traffic and outputs its throughput and 
packet loss rate.  The implementation is moved to {@link CountMonitor2} for
general applications.  This component remains for backward compatibility.

<p>This component works the same as {@link TrafficMonitor} except that it also 
calculates packet loss rate in the current window, provided that the packets
contain correct ordering information.  It calculates the loss rate by summing
up the "gaps" found between the ordering information in the packets in the 
current window.  The calculation may not be correct if packets may arrive out
of order in different windows.

<p>Same as {@link TrafficMonitor}, this component is configured by two
parameters: the window size (default one second) and the output interval
(default 0.5 second).
It can operate in the "byte" mode, the "packet" mode, or both.
The throughput events are exported at either the <code>bytecount@</code>
port or the <code>pktcount@</code> port, and the loss rate exported at either
the <code>byteloss@</code> port or the <code>pktloss@</code> port, both in 
percentage(%).
 */
public class TrafficMonitor2 extends CountMonitor2
{

	/** ID of the port to export the packet-mode throughput events. */
	public static final String PKT_COUNT_PORT_ID = "pktcount";
	/** ID of the port to export the packet-mode packet-loss-rate events. */
	public static final String PKT_LOSS_PORT_ID = "pktloss";
	/** ID of the port to export the byte-mode throughput events. */
	public static final String BYTE_COUNT_PORT_ID = "bytecount";
	/** ID of the port to export the byte-mode packet-loss-rate events. */
	public static final String BYTE_LOSS_PORT_ID = "byteloss";
	
	/** Name of the packet-mode throughput events. */
	public static final String PKT_COUNT_EVENT = "Throughput (packet count)";
	/** Name of the packet-mode packet-loss-rate events. */
	public static final String PKT_LOSS_EVENT ="Packet Loss Rate(packet count)";
	/** Name of the byte-mode throughput events. */
	public static final String BYTE_COUNT_EVENT = "Throughput";
	/** Name of the byte-mode packet-loss-rate events. */
	public static final String BYTE_LOSS_EVENT = "Packet Loss (byte count)";
	
	{
		setEventNames(PKT_COUNT_EVENT, PKT_LOSS_EVENT,
						BYTE_COUNT_EVENT, BYTE_LOSS_EVENT);
		objcountPort.setID(PKT_COUNT_PORT_ID);
		objlossPort.setID(PKT_LOSS_PORT_ID);
		sizecountPort.setID(BYTE_COUNT_PORT_ID);
		sizelossPort.setID(BYTE_LOSS_PORT_ID);
	}
	
	public TrafficMonitor2() 
	{ super(); }
	
	public TrafficMonitor2(String id_)
	{ super(id_); }
}
