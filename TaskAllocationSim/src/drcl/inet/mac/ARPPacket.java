// @(#)ARPPacket.java   1/2004
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

package drcl.inet.mac;

import drcl.net.*;

/**
 * This class defines ARP packet.
 * @author Ye Ge
 */
public class ARPPacket extends Packet
{
    
    static final int ARP_PACKET_SIZE = 28; 
    
    static final int ARPHRD_ETHER     = 1;     // pp. 686
    static final int ETHERTYPE_IP     = 0x0800;
    static final int ETHERTYPE_ARP    = 0x0806;
    static final int ETHERTYPE_REVARP = 0x8035;

    static final int ARPOP_REQUEST    = 1;
    static final int ARPOP_REPLY      = 2;
    static final int ARPOP_REVREQUEST = 3;
    static final int ARPOP_REVREPLY   = 4;
    
    int ar_hrd;    // hardware type, should be set to ARPHRD_ETHER
    int ar_pro;    // protocol type, should be set to ETHERTYPE_IP
    int ar_hln;    // hardware length, should be set to 6
    int ar_pln;    // protocol length, should be set to 4
    
    int  arp_op;    //
    
    /**
     * sender Ethernet addr
     */
    long arp_sha;   
    
    /**
     * sender IP addr
     */
    long arp_spa;   
    
    /**
     * target Ethernet addr
     */
    long arp_tha;   
    
    /**
     * target IP addr
     */
    long arp_tpa;   
    
    public String getName()  { return "ARP Packet"; }

    ARPPacket(int op_, long sha_, long spa_, long tha_, long tpa_)  {
        super(ARP_PACKET_SIZE, 0, null);
        
        arp_op   = op_;
        arp_sha  = sha_;
        arp_spa  = spa_;
        arp_tha  = tha_;
        arp_tpa  = tpa_;
        
        ar_hrd = ARPHRD_ETHER;
        ar_pro = ETHERTYPE_IP;
        ar_hln = 6;
        ar_pln = 4;
    }
    
	/*
    public void duplicate(Object source_)
	{
		super.duplicate(source_);
		ARPPacket that_ = (ARPPacket)source_;
        arp_op   = that_.arp_op;
        arp_sha  = that_.arp_sha;
        arp_spa  = that_.arp_spa;
        arp_tha  = that_.arp_tha;
        arp_tpa  = that_.arp_tpa;
        
        ar_hrd = that_.ar_hrd;
        ar_pro = that_.ar_pro;
        ar_hln = that_.ar_hln;
        ar_pln = that_.ar_pln;
	}
	*/

	public Object clone()
	{
		return new ARPPacket(arp_op, arp_sha, arp_spa, arp_tha, arp_tpa);
	}

	public String _toString(String separator_)  { 
        return getName() + separator_ + "arp_op="  + arp_op  + separator_ + 
                                        "arp_sha=" + arp_sha + separator_ + 
                                        "arp_spa=" + arp_spa + separator_ + 
                                        "arp_tha=" + arp_tha + separator_ + 
                                        "arp_tpa=" + arp_tpa;
                                        
    }
 
}
