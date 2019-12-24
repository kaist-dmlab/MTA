// @(#)LL.java   7/2003
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

package drcl.inet.mac;

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*; 
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

    /* 
     *  receive an IP packet from upperlayer component ( as the first step, it can be PacketDispatcher.
     *  refer to sendDown of LL.cc in ns
     *        construct the LL packet header
     *        if ( p is a broadcast ) 
     *             send the packet to the queue below
     *        else 
     *             send the packet to arp for resolve()
     *
     *  receive any packet from ARP
     *        send it down to the queue
     *
     *  receive packet from downport
     *        if ( it is a ARP packet ) 
     *             send the packet to ARP for arp.input()
     *        else
     *             send the packet to upport
     *
     *  
     *  arp.resolve()   see ARP.java
     *        look up the entry in the ARPTable
     *        if ( llinfo exists and llinfo is up )  {
     *             set the mac header of the packet 
     *             send the packet back to LL
     *        }
     *        else  {
     *             if ( llinfo doesn't exist ) 
     *                  llinfo = new ARPEntry
     *             if ( llinfo.count >= ARP_MAX_REQUEST )   {         
     *             }  
     *             
     *             drop held packet
     *             buffer current packet
     *             // need to send ARP Request
     *             arprequest(src, dst)  
     *        }    
     *
     *  arp.request(src, dst) see ARP.java
     *        construct an arp request packet
     *        sendback to LL so that it will be send down to the queue
     *        
     *  arp.input(p) 
     *  when LL receives an ARP packet from the mac layer, it will be sent to ARP and this method will be called
     *        if ( p.src is not in the ARPTable )  
     *             create an Entry
     *        else 
     *             send the current held packet corresponding to this src back to the LL so that it will be sent down
     *        if ( p is an ARP request ) {
     *             construct an ARP reply
     *             send back to LL so that it will be sent down to the queue
     *        }
     * 
     *          
     *                
     */
    
/**
 * This class implements some link layer functions. It receives IP packets from 
 * the <code>PktDispatcher</code>, encapsulates them in a <code>LLPackets</code> 
 * and sends them down to the interface queue. It also receivs InetPacket or ARPPacket
 * from the MAC layer component and passes them to <code>PktDispatcher</code> or 
 * <code>ARP</code> components respectively.
 *
 * @author Ye Ge
 */    
public class LL extends drcl.net.Module 
{
   /* configurate the ports */
    protected static final String CONFIG_PORT_ID      = ".config";
    protected static final String ARP_PORT_ID         = ".arp";           
    protected static final String MAC_PORT_ID         = ".mac";
    protected static final String EVENT_PKT_ARRIVAL_PORT_ID = ".pktarrival";
    
    protected static final String EVENT_PKT_ARRIVAL   = "PKT ARRIVAL";
     
    protected Port configPort      = addPort(CONFIG_PORT_ID, false); 
    
    /** 
     * This port should be connected to the ARP component.
     */
    protected Port arpPort         = addPort(ARP_PORT_ID, false);  // the port receiving packets from the channel 
    
    /**
     * This port should be connected to the mac component to receive incoming frames.
     */
    protected Port macPort         = addPort(MAC_PORT_ID, false); 
    
    
    protected Port pktarrival      = addEventPort(EVENT_PKT_ARRIVAL_PORT_ID);
   
    /**
     * Constructor.
     */
    public LL() {
        super();
    }

    public String getName() { return "LinkLayer"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
        LL that_ = (LL) source_;
	}    

    // tempprary solution 
    
    /**
     * The MAC address of the interface on which this  LL is working on.
     * This field is defined to avoid unnecessary communication between 
     * this component and some identity compoents (i.e. the MAC component)
     * to repeatly retrieve such kind of information. 
     */
    protected long myMacAddr;
    
    /**
     * The IP address assigned to this interface on which this LL is working on.
     * This field is defined to avoid unnecessary communication between 
     * this component and some identity compoents (i.e. Identity component)
     * to repeatly retrieve such kind of information. 
     */
    protected long myIpAddr;
    
    public long   getMacAddress() { return myMacAddr; }
    
    public long   getIpAddress()  { return myIpAddr; }
    
    /**
     * Sets the address information. This method should be called
     * in the script to explicitly pass ip address and mac address information
     * to each LL component.
     *
     *@param ipAddr_ the IP address
     *@param macAddr_ the MAC address
     * 
     */
    public void setAddresses(long ipAddr_, long macAddr_) {
        myIpAddr  = ipAddr_;
        myMacAddr = macAddr_;
    }
    
    /**
     * Processes packets arriving from upper layer.
     */
    protected synchronized void dataArriveAtUpPort(Object data_,  drcl.comp.Port upPort_) 
	{
        if ( !( data_ instanceof InetPacket) )  {
            error("dataArriveAtUpPort", "LL only expects InetPackets from upPort.");
            return;
        }    

        /* the PktDispatcher may send a message down instead of an InetPacket */
        /* this part will be modified later according to the implementation of ad-hoc routing */
        InetPacket pkt = (InetPacket) data_;

        if (pktarrival._isEventExportEnabled())
			pktarrival.exportEvent(EVENT_PKT_ARRIVAL, pkt, "from local up port " + upPort_.getID()); 
        
        long dst, src;
        
        ////////////////////////////////////////////////////////////////////////////////////
    	// Modified by Will
    	//dst = pkt.getDestination();   // temporary solution,
        // the correct destination ipaddr should be the next hop node's ip address instead of the final destination of this pkt
        dst = pkt.getNextHop();   // correct solution,
        //debug("LL recv new pkt from upport: " + pkt + " nexthop "+ dst);
        ////////////////////////////////////////////////////////////////////////////////////

        //XXX Ye
        // fixing the bug in PktSending.getBcastPacket()        
        
        dst = pkt.getDestination();
        if ( dst != Address.ANY_ADDR ) 
            dst = pkt.getNextHop(); 

    	src = pkt.getSource();
        
        long src_macaddr, dst_macaddr;
        //System.out.println("src = " + src + ",  dst = " + dst);        
        if ( dst == Address.ANY_ADDR )  {    // any address
            src_macaddr = myMacAddr;  // temporary solution for performance reason, no need to query the mac address.
            dst_macaddr = Mac_802_11.MAC_BROADCAST;
            //System.out.println("gocha  dst_macaddr = " + Mac_802_11.MAC_BROADCAST);        
            LLPacket llpkt = new LLPacket(dst_macaddr, src_macaddr, pkt.size, pkt);
            downPort.doSending(llpkt);
    	    //debug("LL pkt " + llpkt);
        }
        else {
            src_macaddr = myMacAddr;  // temporary solution for performance reason, no need to query the mac address.
            dst_macaddr = 0;
            LLPacket llpkt = new LLPacket(dst_macaddr, src_macaddr, pkt.size, pkt);
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_Resolve, dst, llpkt); 
            arpPort.doSending(msg);   // need to do ARP resolve
        }    
    }
    
    /**
     * Processes packets arriving from lower layer (MAC layer).
     */
	protected synchronized void dataArriveAtMacPort(Object data_) 
    {
        if ( data_ instanceof InetPacket ) {
            //debug("LL recv new pkt from macport: " + ((InetPacket) data_));
            upPort.doSending(data_);
        }   
        else if ( data_ instanceof ARPPacket ) {  // has received an ARP request or reply packet.
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_Input, data_);
            arpPort.doSending(msg);  
        }
		return;
	}
    
    /**
     * Processes packets received from the arpPort.
     */
    protected synchronized void dataArriveAtArpPort(Object data_) 
    {
        ARPContract.Message msg = (ARPContract.Message) data_;
        if ( msg.getOpCode() == ARPContract.Message.ARP_ResolveReply || msg.getOpCode() == ARPContract.Message.ARP_Send_Hold 
             || msg.getOpCode() == ARPContract.Message.ARP_Send_Reply || msg.getOpCode() == ARPContract.Message.ARP_Send_Request )
            downPort.doSending(msg.getPkt());
		return;
	}
    
  	protected synchronized void processOther(Object data_, Port inPort_)
	{
		String portid_ = inPort_.getID();
    
        if (portid_.equals(MAC_PORT_ID)) {
    	    if (!(data_ instanceof ARPPacket || data_ instanceof InetPacket)) {
               	error(data_, "processOther()", inPort_, "unknown object at node " + this.myMacAddr);
               	return;
            }
            dataArriveAtMacPort(data_);
            return;
        }
        else {
            if (portid_.equals(ARP_PORT_ID)) {
                if (!(data_ instanceof ARPContract.Message)) {
                    error(data_, "processOther()", inPort_, "unknown object");
                    return;
                }
                dataArriveAtArpPort(data_);
                return;
            }    
        }
        super.processOther(data_, inPort_);
	}  
}    
    
    



