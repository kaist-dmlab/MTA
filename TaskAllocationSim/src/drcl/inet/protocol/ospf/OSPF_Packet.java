// @(#)OSPF_Packet.java   8/2003
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

package drcl.inet.protocol.ospf;

import drcl.net.Packet;
import drcl.data.*;
 
/**
  * This class implements a generic OSPF packet. The following fields from RFC 2328
  * are not implemented:
  * <ol>
  * <li>   OSPF Version number
  * <li>   Packet length
  * <li>   Checksum
  * <li>   Authentication Type
  * <li>   Authentication
  * </ol>
  * Ref: RFC 2328 A.3.1 p.190
  * 
  * @author Wei-peng CHen
  */
public class OSPF_Packet extends drcl.net.Packet
{
	protected static final int OSPF_HEADER_LEN = 24;

  /************************* PACKET VARIABLES **************************/

	/** the type of this OSPF_Pkt_header */
	private int Type;
  
	/** the identification of the router which sent this packet, chosen 
    * as the smallest of the IPaddress of all its interfaces
    */
	private int Router_ID;
  
	/** the area to which this PACKET belongs */
	private int Area_ID;
    
	/** constructs an OSPF_Pkt_header with a given type, router ID, area ID*/
	protected OSPF_Packet(int type, int routerid, int areaid)
	{
		super(OSPF_HEADER_LEN);
		Type        = type;
		Router_ID   = routerid;
		Area_ID     = areaid;
	}
  
	protected int getType() { return Type; }
	protected int getRouterID() { return Router_ID; }
	protected int getAreaID() { return Area_ID; }
	protected int getLength() { return size; }

	public String getName()
	{ return "OSPF"; }

	public String _toString(String separator_)
	{
		return OSPF.PKT_TYPES[Type] + separator_ + "router:" + Router_ID
			+ separator_ + "area:" + Area_ID;
	}

	/*
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		OSPF_Packet that_ = (OSPF_Packet)source_;
		Type = that_.Type;
		Router_ID = that_.Router_ID;
		Area_ID = that_.Area_ID;
	}
	*/

	public Object clone()
	{ return new OSPF_Packet(Type, Router_ID, Area_ID); }
}	
