// @(#)Receiver.java   9/2002
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

package drcl.net.tool;

import drcl.net.*;
import drcl.util.scalar.LongSpace;

/**
A generic receiving component.  This component uses the <code>getPacketCount()</code> and 
<code>getByteCount()</code> methods of {@link drcl.net.Packet} to record the packet and
byte "gaps" (unreceived packets and bytes) of the connection.
One can use the info() method of the component to print out the gaps.
*/
public class Receiver extends Module
{
	LongSpace unreceived = new LongSpace(); // not received sequence #
	LongSpace unreceivedBytes = new LongSpace(); // not received sequence #

	{ removeDefaultUpPort(); removeTimerPort(); }
	
	public Receiver() 
	{ super(); }
	
	public Receiver(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		unreceived.reset();
		unreceivedBytes.reset();
	}
	
	public void duplicate(Object source_)
	{ super.duplicate(source_); }
	
	public String info()
	{ return "Unreceived packets: " + unreceived + "\nUnreceived bytes: " + unreceivedBytes + "\n"; }
	
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		if (!(data_ instanceof Packet)) {
			error(data_, "process()", inPort_, "unknown data");
			return;
		}
		Packet pkt_ = (Packet)data_;
		if (pkt_.isTimestampSupported()) {
			if (isDebugEnabled())
				debug("latency=" + (getTime() - pkt_.getTimestamp()) + ": " 
						+ pkt_);
		}
		if (pkt_.isPacketCountSupported())
			unreceived.checkout(pkt_.getPacketCount());
		if (pkt_.isByteCountSupported())
			unreceivedBytes.checkout(pkt_.getByteCount(),
							pkt_.getByteCount() + pkt_.getPacketSize());
	}
}
