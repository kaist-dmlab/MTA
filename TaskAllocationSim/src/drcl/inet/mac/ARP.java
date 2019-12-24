// @(#)ARP.java   1/2004
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

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*; 
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

/**
 * This component partially implements the Address Resolution Protocol (ARP).
 * In this release, the stalled ARP entries are not periodically purged.
 *
 * @see ARPContract
 * @see LL
 *
 * @author Ye Ge
 */
public class ARP extends drcl.net.Module
{

    private static final int ARP_MAX_REQUEST_COUNT = 3;

    /** 
     * The MAC address of this interface card.
     */
    protected long myMacAddr;
    
    /** 
     * The IP address of this interface card. This is a temporary solution so 
     * that the connection between <code>ARP</code> and <code>Identity</code> 
     * components can be avoided to speed up the simulation.
     */
    protected long myIpAddr;
    
    /** 
     * Configurates the IP address and the MAC address of this interface. 
     */
    public void setAddresses(long ipAddr_, long macAddr_) {
        myIpAddr  = ipAddr_;
        myMacAddr = macAddr_;
    }
    
    /** 
     * This is a flag which the user can choose to bypass the ARP operation to speed up the 
     * simulation. If this flag is set to false,  
     */
    public boolean bypassARP = false;
    
    /**
     * sets the bypassarp flag.
     */
    public void setBypassARP(boolean b_) { bypassARP = b_; }
    
    /**
     * get mac address
     */
    public long getMacAddr() { return myMacAddr; }
    
    /**
     * get IP address
     */
    public long getIpAddr()  { return myIpAddr;  }
    
    private static final String ARP_PORT_ID = ".arp";    
     
    private static final String CONFIG_PORT_ID = ".config";
    
    /**
     * The port to be connected to <code>LL</code> component's <code>arpPort</code>. 
     */
    Port arpPort    = addPort(ARP_PORT_ID, false);

    Port configPort = addPort(CONFIG_PORT_ID, false); 

    { 
        removeDefaultUpPort();
        removeDefaultDownPort();
        // removeTimerPort();      // here it is not removed because we need this timer port to refresh the arp entry in later release
    }  

    /**
     * ARP Table 
     */
    Vector ARPTable = new Vector(); 
    
	public ARP()
	{ 
        super(); 
        ARPTable = new Vector();
    }
	
	public ARP(String id_)
    {
        super(id_);
        ARPTable = new Vector();
    }
	
	public String info()
	{
        return ARPTable.toString();
    }      
	
    public String toString() {
       String str;
       str = "";
       for ( int i = 0; i < ARPTable.size(); i++ ) {
            ARPEntry o_ = (ARPEntry) ARPTable.elementAt(i);
            str = str + " <" + o_.toString() + "> " ;
        }    
       return str;
        
    }
    
    public String getName() { return "arp"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		ARP that_ = (ARP)source_;
		ARPTable = new Vector();
        for ( int i = 0; i < that_.ARPTable.size(); i++ ) {
            Object o_ = that_.ARPTable.elementAt(i);
            ARPTable.addElement(o_ instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)o_).clone() : o_);
        }    
	}
   
    /** 
     * This class implements the ARP table entry in the <code>ARP</code> component.
     */
    public class ARPEntry 
    {
        boolean  isup;
        long     ipaddr;
        long     macaddr;
        
        /**
         * Buffering one packet whose mac address has not been resolved and a request has been sent out.
         * It is only the most recent packet to the same IP address is buffered and the older packets are
         * discarded after this packet arrives.  
         */
        Object   hold;     // hold only the current packet if its mac address is not resolved and a request has been sent out
        int      count;
        
        public ARPEntry(long ipaddr_, long macaddr_, boolean isup_, Object hold_, int count_) {
            ipaddr   = ipaddr_;
            macaddr  = macaddr_;
            isup     = isup_;
            count    = count_;
            // hold = hold_ instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)hold_).clone(): hold_;
            hold     = hold_;
        }
        
        protected void increaseCount() { count = count + 1; }
        protected void setCount(int count_)      { count = count_;     }
        protected void setIsUp(boolean isup_)    { isup = isup_;       }
        protected void setIPAddr(long ipaddr_)   { ipaddr = ipaddr_;   }
        protected void setMacAddr(long macaddr_) { macaddr = macaddr_; }
        
        /**
         * Buffers the packet.
         */
        protected void setHold(Object hold_)     { hold = hold_;       }
        
        protected int     getCount()   { return count;   }
        protected boolean isUp()       { return isup;    }
        protected long    getIPAddr()  { return ipaddr;  }
        protected long    getMACAddr() { return macaddr; }
        
        /**
         * Retrieves the buffered packet.
         */
        protected Object  getHold()    { return hold;    }
        
        public String toString(String seperator_) {
            return "ARP Entry" + seperator_ + "ipaddr=" + ipaddr + seperator_ + "macaddr=" + macaddr + seperator_ + "isup=" + isup + seperator_ + "count=" + count;
        }    
        
    }     
    
    /**
     * Processes the packet coming from arpPort and configPort.
     */
    protected synchronized void processOther(Object data_, Port inPort_)
	{
		String portid_ = inPort_.getID();
        
        if (portid_.equals(ARP_PORT_ID)) {
    		if (!(data_ instanceof ARPContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            processARP(data_);
        }    
        else 
            if (portid_.equals(CONFIG_PORT_ID)) {
                if (!(data_ instanceof ARPContract.Message)) {
                    error(data_, "processOther()", inPort_, "unknown object");
                    return;
                 }
                processConfig(data_);
            }    
            else 
                super.processOther(data_, inPort_);
	}  
        
    /**
     * Processes the ARP resolve, request or reply packets.
     */
    private synchronized void processARP(Object data_) {
        
        ARPContract.Message msg = (ARPContract.Message) data_;
        
        int opCode = msg.getOpCode();
        if (opCode == ARPContract.Message.ARP_Resolve) {
            arpResolve(msg.getDst(), msg.getPkt());            // arp resolve   
        }    
        else if (opCode == ARPContract.Message.ARP_Input) {    // arp request or reply are received
            arpInput(msg.getPkt());
        }    
    }    

    /**
     * Processes ARP configuration packet. 
     *
     */ 
    private synchronized void processConfig(Object data_) {
        ARPContract.Message msg = (ARPContract.Message) data_;
        int opCode = msg.getOpCode();
        if (opCode == ARPContract.Message.ARP_Config) {
            // arp_config();     // ARP configuration function is not implemented yet.
        }    
    }    

    /**
     * Processes the ARP resolve packet and fill the destination mac address field of the
     * <code>LLPacket</code> component if possible. If <code>bypassARP</code> is set to true, by default the mac adress and IP address
     * of each interface card are of the same value, therefore no arp requesting/replying is involved
     * and ARP fills in the destination mac address into that <code>LLPacket</code> immediately.
     * If <code>bypassARP</code> is set to false, 
     *
     * @param dst_  the destination ip address to be resolved.
     * @param data_  the <code>LLPacket</code> to be sent down to mac layer.
     */
    synchronized void arpResolve(long dst_, Object data_) {
        
        if ( bypassARP == true ) {
            LLPacket llpkt = (LLPacket) data_;
            llpkt.setSrcMacAddr(myMacAddr);     // set the src mac address field, redumdent operation
            llpkt.setDstMacAddr(dst_);          // set the dst mac address field
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_ResolveReply, llpkt);

            arpPort.doSending(msg);                     // send the LLPacket back;
            return;
        }    
        
        ARPEntry llinfo = null;
	
        for (int i = 0; i < ARPTable.size(); i++) {
            ARPEntry t = (ARPEntry) ARPTable.elementAt(i);
            if ( t.ipaddr == dst_ ) 
                llinfo = t;
        }    
       
        if ( llinfo != null && llinfo.isup == true ) {
            LLPacket llpkt = (LLPacket) data_;
            llpkt.setSrcMacAddr(myMacAddr);             // set the src mac address field, redumdent operation
            llpkt.setDstMacAddr(llinfo.getMACAddr());   // set the dst mac address field
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_ResolveReply, llpkt);

            arpPort.doSending(msg);                     // send the LLPacket back;
            return;
        }    
	
        if(llinfo ==  null) {
            //  Create a new ARP entry
            llinfo = new ARPEntry(dst_ /*dst ip addr */, -1 /* macAddr */, false /* isup */, null /* hold */, 0 /* count */);
            ARPTable.insertElementAt(llinfo, 0);
            debug("ARP Entry is added.  " + llinfo.toString());
        }
            
        // If there are more than three ARP requests are received for the same destination IP address, 
        // the current packet is also dropped along with the buffered packet.
        if(llinfo.count >= ARP_MAX_REQUEST_COUNT) {
            if ( llinfo.getHold() != null ) {
                drop(llinfo.getHold(), "DROP_IFQ_ARP_FULL");
            }    
            drop(data_, "DROP_INF_ARP_FULL");
            llinfo.setCount(0);
            llinfo.setHold(null);
            return;
        }

	    llinfo.increaseCount();
        
        // only one LLpacket is buffered here 
        if (llinfo.getHold() != null)
            drop(llinfo.getHold(), "DROP_IFQ_ARP_FULL");
        llinfo.setHold(data_);

         
        // We don't have a MAC address for this node.  Send an ARP Request.
        // The rate of sending ARP request should be limited. ( not implemented )
        // Right now, we keep the IP address of this host in 
        // the ARP component, therefore it is not necessarily to consult the Identity compoent
        // to get the IP address.
        arpRequest(myIpAddr, dst_);
    }    

    /**
     * Constructs an ARP request packet and imcapsulate it into a LLPacket and send it back through
     * the arpPort. 
     *
     * @param src_ sender's ip address
     * @param dst_ receiver's ip address
     */
    synchronized void arpRequest(long src_, long dst_) {
        long sha_ = myMacAddr; // sender Ethernet addr
        long spa_ = src_;      // sender IP addr
        long tha_ = 0;         // target Ethernet addr
        long tpa_ = dst_;      // target IP addr
        ARPPacket ap = new ARPPacket(ARPPacket.ARPOP_REQUEST, sha_, spa_, tha_, tpa_);
        
        LLPacket llpkt = new LLPacket(Mac_802_11.MAC_BROADCAST, sha_, ap.size, ap);

        ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_Send_Request, llpkt);   
        arpPort.doSending(msg);
    }    
     
    /** 
     * When a LL component receives an ARP packet from the mac layer, 
     * it will pass that ARP packet to ARP. When an ARP component recieves 
     * any ARP request or reply packet, this method will be called to process it.
     *
     * @param pkt_ the ARP request or reply packet
     */    
    synchronized void arpInput(Object pkt_)  {
        if ( !(pkt_ instanceof ARPPacket) )  {
            error("arpInput()", "unknown object");
            return;
        }    
            
        ARPPacket pkt = (ARPPacket) pkt_;
        
        ARPEntry llinfo = null;
	
        // look up the ARP table
        for (int i = 0; i < ARPTable.size(); i++) {
            ARPEntry t = (ARPEntry) ARPTable.elementAt(i);
            if ( t.ipaddr == pkt.arp_spa ) 
                llinfo = t;
        }    
        
        // no entry is found. Add a new entry.
        if ( llinfo == null ) {
            llinfo = new ARPEntry(pkt.arp_spa, -1, false, null, 0);
            ARPTable.insertElementAt(llinfo,0);
            debug("ARP Entry is added.  " + llinfo.toString());
    	}
        
        llinfo.setMacAddr(pkt.arp_sha);
        llinfo.setIsUp(true);
        debug("ARP Entry is added.  " + llinfo.toString());
        
        // There is one buffered packet. Send it out.
        if ( llinfo.getHold() != null ) {
            LLPacket llpkt = (LLPacket) llinfo.getHold();
            llpkt.setDstMacAddr(llinfo.getMACAddr());   // set the dst mac address field
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_Send_Hold, 0, llpkt);
            arpPort.doSending(msg);
            llinfo.setHold(null);
        }    

        // Send back the ARP reply.
        if ( pkt.arp_op == ARPPacket.ARPOP_REQUEST && pkt.arp_tpa == myIpAddr ) {
            long sha_ = myMacAddr;                   // sender Ethernet addr
            long spa_ = myIpAddr;                    // sender IP addr
            long tha_ = llinfo.getMACAddr();         // target Ethernet addr
            long tpa_ = llinfo.getIPAddr();          // target IP addr
            ARPPacket ap = new ARPPacket(ARPPacket.ARPOP_REPLY, sha_, spa_, tha_, tpa_);
        
            LLPacket llpkt = new LLPacket(tha_, sha_, ap.size, ap);
            
            ARPContract.Message msg = new ARPContract.Message(ARPContract.Message.ARP_Send_Reply, 0, llpkt);
            arpPort.doSending(msg);
            llinfo.setHold(null);
        } 
    }    
}           

