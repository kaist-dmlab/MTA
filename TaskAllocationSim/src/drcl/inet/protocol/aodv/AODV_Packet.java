// @(#)AODV_Packet.java   1/2004
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

package drcl.inet.protocol.aodv;

import drcl.net.Packet;
import drcl.data.*;
 
/**
  * This class implements a generic AODV packet. 
  * 
  * @author Wei-peng CHen
  */
public class AODV_Packet extends drcl.net.Packet
{
	protected static final int AODV_HEADER_LEN = 24;

  /************************* PACKET VARIABLES **************************/

	/** the type of this AODV_Pkt_header */
	private int Type;
  
	/** the identification of the router which sent this packet, chosen 
	* as the smallest of the IPaddress of all its interfaces
	*/
	private int Router_ID;
  
	/** constructs an AODV_Pkt_header with a given type, router ID */
	protected AODV_Packet(int type, int routerid)
	{
		super(AODV_HEADER_LEN);
		Type        = type;
		Router_ID   = routerid;
	}

	// Tyan: for clone()
	private AODV_Packet(int type, int routerid, int hsize_, int bsize_,
					Object body_)
	{
		super(hsize_, bsize_, body_);
		Type        = type;
		Router_ID   = routerid;
	}
  
	protected int getType() { return Type; }
	protected void setType( int type_) { Type = type_; }
	protected int getRouterID() { return Router_ID; }
	protected int getLength() { return size; }

	public String getName()
	{ return "AODV"; }

	public String _toString(String separator_)
	{
		return AODV.PKT_TYPES[Type] + separator_ + "router:" + Router_ID
			+ separator_;
	}

	/*
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		AODV_Packet that_ = (AODV_Packet)source_;
		Type = that_.Type;
		Router_ID = that_.Router_ID;
	}
	*/

	public Object clone()
	{
		// Tyan: need to clone body and pkt size info as well
		return new AODV_Packet(Type, Router_ID, headerSize, size-headerSize,
				body instanceof drcl.ObjectCloneable?
				((drcl.ObjectCloneable)body).clone(): body);
	}
}	
