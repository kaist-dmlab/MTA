// @(#)PktMonitor.java   12/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.tool;

import drcl.comp.*;
import drcl.net.Packet;
import drcl.inet.transport.TCPPacket;

/**
 * Tracking a TCP packet of a certain sequence number. 
 */
public class PktMonitor extends Extension
{
	long tcpseq = -1;
	
	public PktMonitor() { super(); }
	
	public PktMonitor(String id_) { super(id_); }
	
	protected void process(Object data_, Port inPort_) 
	{
		Object original_ = data_;
		while (!(data_ instanceof TCPPacket)) {
			if (!(data_ instanceof Packet)) return;
			data_ = ((Packet)data_).getBody();
		}
		TCPPacket p = (TCPPacket)data_;
		if (p.getSeqNo() == tcpseq)
			debug(inPort_.getID() + ": " + original_);
	}

	public void setSeqNo(long seq_)
	{ tcpseq = seq_; }

	public String info()
	{
		if (tcpseq >= 0) return "tracking " + tcpseq + "\n";
		else return "tracking nothing\n";
	}
}
