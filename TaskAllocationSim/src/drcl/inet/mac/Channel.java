// @(#)Channel.java   7/2003
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

/**
 * This class simulates a shared wireless channel. A <code>Channel</code> component contains
 * a vector of <code>Ports</code> as well as an array of boolean flags.
 * If a mobile node's wireless interface card is working on a given channel, the <code>downPort</code> 
 * of the <code>WirelessPhy</code> component is connected to the corresponding port of the <code>Channel</code> component 
 * and the flag is set correspondingly. 
 * 
 * The <code>Channel</code> component also contains a <code>trackerPort</code> which should be connected to the <code>channelPort</code> 
 * of a <code>NodePositionTracker</code> component. 
 * 
 * When a data packet is sent from a wireless interface card to the <code>Channel</code> component, the <code>Channel</code> 
 * component consults the <code>NodePositionTracker</code> to decide to which others nodes
 * that data packet should be sent. How the neighbouring nodes are decided can be find in 
 * {@link NodePositionTracker}. Whether that data packet should be really 
 * received(decoded) by the receivers shall be further determined at each <code>WirelessPhy</code> component respectively. 
 * In this way, a shared medium is simulated without unnecessarily passing each data packet to all nodes listenning to that channel.
 *
 * @see NodePositionTracker
 * @see NodeChannelContract
 * @see WirelessPhy
 * @author Ye Ge
 */  
public class Channel extends drcl.net.Module 
{
    
    /* configurate the ports */
    private static final String NODE_PORT_ID    = ".node";          // connect to the mobile nodes' wirelessphy   
    private static final String CONFIG_PORT_ID  = ".config";
    private static final String TRACKER_PORT_ID = ".tracker";       // connect to the tracker component
    
    // public static int totalMessage = 0;  // deleted
    
    /**
     * This port should be connected to a mobile node' wirelessphy if that node is operating on this channel.
     */
    protected Port nodePort = addPort(NODE_PORT_ID, false);
    
    /**
     * The configuration port. 
     */
    protected Port configPort = addPort(CONFIG_PORT_ID, false); 
    
    /**
     * This port should be connected to a NodePositionTracker component.    
     */
    protected Port trackerPort = addPort(TRACKER_PORT_ID, false);

   /**
    * A variable indicating what is the maximum distance if an subarea is
    * considered as the neightbouring grid to the current position of the 
    * sender node.
    */
    public int nGrids = 1;
    
    long nPort;
    
    {
        removeDefaultUpPort();
        removeDefaultDownPort();
        removeTimerPort();
    }    
    
    // when each wirelessphy is connected to the channel, the channel saves its reference
    // this doesn't comply to the ACA concept very well, but hope it can boost the performance 
    // in this special case. (In this release, we didn't adopt this implementation scheme.)
    // protected Vector WIfs = new Vector();    
    // public void addWirelessInterface(Wirelessphy wif_, long nid_)  {
    //     WIfs.setElementAt(wif_, nid_);
    // }   
    
    /** 
     * vector of <code>Port</code> components.
     */
    protected Vector vp = new Vector();         
    
    /**
     * A flag array. Functions as a flag to indicate whether a node has been 
     * attached to this channel or not. 
     * This is implemented to support simulation of multiple channels. 
     */
    protected boolean[] vp_flag;
    
    /**
     * Constructor.
     */
    public Channel() {
        super();
    }

   /**
    * Set nGrids variable. All nodes within n-grid subarea are considered neighbors
    * @param n - grid size
    */
    public void setnGrids(int n) {
        nGrids = n;
    }    
    
    /**
     * Sets the capacity (the maximal number of wireless interface cards 
     * to communicate on this channel). We need this information from the user
     * to decide the size of the vp_flag array.
     */
    public void setCapacity(int n) {
        vp = new Vector(n);
        vp_flag = new boolean[n];
        for ( int i = 0; i < n; i ++ ) {
            Port p_ = addPort(".toNode" + i);
            vp.insertElementAt(p_, i);
            vp_flag[i] = false;
        }    
    }    
    
    /**
     * Attaches the nid-th element in the port vector to the Port <code>port_</code>.
     *
     * @param nid the node id of the node whose wireless interface card (WirelessPhy) is working on this channel.
     * @param port_ the downport of the <code>WirelessPhy</code> component which is working on this channel.
     */
    public void attachPort(int nid, Port port_) {
        Port p_ = (Port) vp.elementAt(nid);
        p_.connectTo(port_); 
        vp_flag[nid] = true;
    }    
    
    public String getName() { return "Channel"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
        // no reason to duplicate nPort and vp
	}    

    
  	protected synchronized void processOther(Object data_, Port inPort_)
	{
        
		String portid_ = inPort_.getID();
    
        if (portid_.equals(NODE_PORT_ID)) {
    		if (!(data_ instanceof NodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            processPacket(data_);
            return;
        }
        super.processOther(data_, inPort_);
	}  

    /**
     * Processes the data packet sent down from any node by retrieving out the sender node's position 
     * and consulting the <code>NodePositionTracker</code> to find out the "neighbours" of the sender.
     * Then to each "neighbouring" node, a duplicated data packet is sent out.
     */
    protected synchronized void processPacket(Object data_) {

        int i;
        long[] nodeList;
        
        NodeChannelContract.Message msg = (NodeChannelContract.Message) data_;
       
        double X, Y, Z;
        long   nid;
        X = msg.getX();
        Y = msg.getY();
        Z = msg.getZ();
        nid = msg.getNid();

        /**
         * Queries the <code>NodePositionTracker</code> component to decide the "neighbours" of the sender of the data packet.
         */
        NeighborQueryContract.Message msg2 = (NeighborQueryContract.Message)  trackerPort.sendReceive(new NeighborQueryContract.Message(nid, X, Y, Z, nGrids));
        
        nodeList = msg2.getNodeList();
        
        for ( i = 0; i < nodeList.length; i++ ) { 
            Port p_;
            if ( nid != nodeList[i] && vp_flag[(int) nodeList[i]] == true ) {
                p_ = (Port) vp.elementAt((int) nodeList[i]);
                
                /**
                 * Sends out a duplicated data packet to each "neighbouring" node.
                 */
                p_.doSending(msg.clone());  
            }
            // possible solution to improve the performance?
            //WirelessPhy wif = WIfs.elementAt(i);
            //wif.doReceiving(msg.clone());
        }
    }
}

