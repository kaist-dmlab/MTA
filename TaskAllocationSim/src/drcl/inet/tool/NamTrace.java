// @(#)NamTrace.java   9/2002
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

package drcl.inet.tool;

import java.util.Hashtable;
import drcl.net.Packet;
import drcl.inet.InetPacket;
import drcl.inet.transport.TCPPacket;

/**
The class for generating NAM traces for {@link InetPacket INET packets}.
This class overrides the following method to complete a NAM packet event:
<ul>
<li> {@link #getColorID(Packet)}: packets with the same pair of (source, destination) share the same color ID.
<li> {@link #getConversationID(Packet)}: the current implementation only recognizes {@link TCPPacket TCP packets}.
</ul>
 */
public class NamTrace extends drcl.net.tool.NamTrace
{
	Hashtable htColor = new Hashtable(); // tos -> color id
	int colorCount = 0;
	int MSS = 512;
	boolean mssEnabled = true;

	public NamTrace()
	{ super(); }
	
	public NamTrace(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		colorCount = 0;
		htColor.clear();
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		NamTrace that_ = (NamTrace)source_;
	}

	public String info()
	{
		return super.info()
    		 + "#colors used: " + colorCount + "\n";
	}

	public String getConversationID(Packet p_)
	{
		if (p_ instanceof InetPacket) {
			if (p_.getBody() instanceof TCPPacket) {
				TCPPacket tcp_ = (TCPPacket)p_.getBody();
				if (mssEnabled) {
					if (tcp_.isACK())
						return "sn" + (tcp_.getSeqNo()/MSS) + ",asn" + (tcp_.getAckNo()/MSS);
					else
						return "sn" + tcp_.getSeqNo()/MSS;
				}
				else {
					if (tcp_.isACK())
						return "sn" + tcp_.getSeqNo() + ",asn" + tcp_.getAckNo();
					else
						return "sn" + tcp_.getSeqNo();
				}
			}
		}
		return super.getConversationID(p_);
	}

	public int getColorID(Packet p_)
	{
		if (p_ instanceof InetPacket) {
			Long tmp_ = new Long((((InetPacket)p_).getSource() << 16) ^ ((InetPacket)p_).getDestination());
			Integer colorid_ = (Integer)htColor.get(tmp_);
			if (colorid_ == null) {
				colorid_ = new Integer(colorCount++);
				htColor.put(tmp_, colorid_);
				tmp_ = new Long((((InetPacket)p_).getDestination() << 16) ^ ((InetPacket)p_).getSource());
				htColor.put(tmp_, colorid_);
			}
			return colorid_.intValue();
		}
		else
			return super.getColorID(p_);
	}

	public void setMSSEnabled(boolean enabled_)
	{ mssEnabled = enabled_; }

	public boolean isMSSEnabled()
	{ return mssEnabled; }

	public void setMSS(int mss_)
	{ MSS = mss_; }

	public int getMSS()
	{ return MSS; }
}
