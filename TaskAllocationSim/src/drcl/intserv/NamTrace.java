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

package drcl.intserv;

import java.util.Hashtable;
import drcl.net.Packet;
import drcl.inet.InetPacket;

/**
The class for generating NAM traces for IntServ packets.
This class overrides the following methods to complete a NAM packet event:
<ul>
<li> type: from {@link IntServToS#interpretType(drcl.inet.InetPacket)}.
<li> conversation id: the ToS field if the incoming packet is a {@link drcl.inet.InetPacket}.
<li> attribute: packets with the same ToS value share the same color ID.
</ul>
 */
public class NamTrace extends drcl.net.tool.NamTrace
{
	Hashtable htColor = new Hashtable(); // tos -> color id
	int colorCount = 0;

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
		if (p_ instanceof InetPacket)
			return String.valueOf(((InetPacket)p_).getTOS());
		else
			return super.getConversationID(p_);
	}

	public int getColorID(Packet p_)
	{
		if (p_ instanceof InetPacket) {
			Long tmp_ = new Long(((InetPacket)p_).getTOS());
			Integer colorid_ = (Integer)htColor.get(tmp_);
			if (colorid_ == null) {
				colorid_ = new Integer(colorCount++);
				htColor.put(tmp_, colorid_);
			}
			return colorid_.intValue();
		}
		else
			return super.getColorID(p_);
	}

	public String getPacketType(Packet p_)
	{
		if (p_ instanceof InetPacket)
			return IntServToS.interpretType((InetPacket)p_);
		else
			return super.getPacketType(p_);
	}
}
