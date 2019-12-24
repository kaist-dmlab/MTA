// @(#)SensorChannel.java   12/2003
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

package drcl.inet.sensorsim;

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*; 
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

/** This class implements the sensor channel in a wireless sensor network.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorChannel extends drcl.net.Module 
{
    public static final String NODE_PORT_ID    = ".node";          // connect to the mobile nodes' sensorphy   
    public static final String CONFIG_PORT_ID  = ".config";
    public static final String TRACKER_PORT_ID = ".tracker";       // connect to the tracker component
    
    protected Port nodePort = addPort(NODE_PORT_ID, false);
    protected Port configPort = addPort(CONFIG_PORT_ID, false); 
    protected Port trackerPort = addPort(TRACKER_PORT_ID, false);

    long nPort;
    
    {
        removeDefaultUpPort();
        removeDefaultDownPort();
        removeTimerPort();
    }    
        
    protected Vector vp = new Vector();         // list of ports
    double propDelay;
    
    // added for the multiple channel support
    // function as a flag whether a node has been attached to this channel
    protected boolean[] vp_flag;
    
    public SensorChannel() {
        super();
	propDelay = 0.0;
    }

	/** Sets the propagation delay of the sensor channel  */
    	public void setPropDelay(double propDelay_) { propDelay = propDelay_; }
	/** Gets the propagation delay of the sensor channel  */
    	public double getPropDelay() { return propDelay; }
    
    	/** Sets the number of nodes in the sensor network  */
    	public void setCapacity(int n) {
        	vp = new Vector(n);
        	vp_flag = new boolean[n];
        	for ( int i = 0; i < n; i ++ ) {
            		Port p_ = addPort(".toNode" + i);
            		vp.insertElementAt(p_, i);
            		vp_flag[i] = false;
        	}
    	}    
    
	/** Attaches a node to a port  */
    public void attachPort(int nid, Port port_) {
        Port p_ = (Port) vp.elementAt(nid);
        p_.connectTo(port_); 
        vp_flag[nid] = true;
    }    
    
    public String getName() { return "SensorChannel"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
	        SensorChannel that_ = (SensorChannel) source_;		
		propDelay = that_.propDelay ;
	}    
    
  	protected synchronized void processOther(Object data_, Port inPort_)
	{
        
	  String portid_ = inPort_.getID();
    
        if (portid_.equals(NODE_PORT_ID)) {
    		if (!(data_ instanceof SensorNodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            processPacket(data_);
            return;
        }
        super.processOther(data_, inPort_);
	}  

	/** Receives a packet and forwards it to sensors  */
    protected synchronized void processPacket(Object data_) {

        int i;
        long[] nodeList;
        
        SensorNodeChannelContract.Message msg = (SensorNodeChannelContract.Message) data_;
       
        double X, Y, Z;
        long   nid;
	  double Radius ;
        X = msg.getX();
        Y = msg.getY();
        Z = msg.getZ();
        nid = msg.getNid();
	  Radius = msg.getRadius();
        
        SensorNeighborQueryContract.Message msg2 = (SensorNeighborQueryContract.Message) trackerPort.sendReceive(new SensorNeighborQueryContract.Message(nid, X, Y, Z, Radius));
        
        nodeList = msg2.getNodeList();

        for ( i = 0; i < nodeList.length; i++ ) { 
            Port p_;
            if ( nid != nodeList[i] && vp_flag[(int) nodeList[i]] == true ) {
                p_ = (Port) vp.elementAt((int) nodeList[i]);

		sendAt(p_, msg.clone(), propDelay);  
		/* to send immediately, comment out above line and add the following one
		// p_.doSending(msg.clone());  
		*/
            }
        }
    }
}
