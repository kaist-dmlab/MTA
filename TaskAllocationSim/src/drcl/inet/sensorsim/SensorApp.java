// @(#)SensorApp.java   12/2003
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

import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

/** This class implements the sensor application layer.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorApp extends drcl.net.Module {
	/*
		This class receives information about target node through the sensor protocol stack.
		If this sensor is not the sink (i.e., data collector), it forwards the data as unicast packet to the sink node.
	*/

	public static final int BYTE_FOR_ID 	= 2048; // number of bytes required for identification
	public static final int COHERENT	= 0; 
	public static final int NON_COHERENT	= 1; 
	public static final int SUPPRESS	= 2; 

	public static final boolean SUPPRESS_ON	= false; 

	public static final String SNR_EVENT = "SNR";

	double radioEventTime ;
	double cpuEventTime ;
	int radioMode ;
	int cpuMode ;
	double coherentThreshold;
	double lastSeenSNR;
	int lastSeenDataSize;
	ACATimer rTimer ;
	int running_ ;
	int eID ;

	long nid ;	/* id of the sensor node. added for cosmotic reasons only: namely to make System.out.println() statements*/

	long sink_nid;	/* id of the sink node to which collected data about target nodes should be sent along the wireless protocol stack */

	long first_target_nid; 	/* added for cosmotic reasons only: namely to make a graph of the data pertaining to each target. This assumes that the targets are the high-numbered nodes in the simulation starting from first_target_nid */

    /* configure the ports */
    public static final String CONFIG_PORT_ID      = ".config";
    public static final String FROM_SENSOR_AGENT_PORT_ID = ".fromSensorAgent";
    public static final String FROM_WIRELESS_AGENT_PORT_ID = ".fromWirelessAgent";
    public static final String RADIO_PORT_ID = ".radio";
    public static final String CPU_PORT_ID = ".cpu";

    public static final String SNR_PORT_ID = ".snr";

	/** Creates ports on which the sink node generates collected information  */
	public void createSnrPorts(int node_num, int target_node_num) 
	{
		first_target_nid = node_num - target_node_num ;

	        for ( int i = 0; i < target_node_num; i ++ ) {
		    addEventPort(SNR_PORT_ID + i);
       		}    
    	}

    protected Port configPort      = addPort(CONFIG_PORT_ID, false);
    protected Port fromSensorAgentPort = addPort(FROM_SENSOR_AGENT_PORT_ID, false);      
    protected Port fromWirelessAgentPort = addPort(FROM_WIRELESS_AGENT_PORT_ID, false);      

	protected Port radioPort = addPort(RADIO_PORT_ID, false); 
	protected Port cpuPort = addPort(CPU_PORT_ID, false); 

	{
		removeDefaultUpPort() ;
		removeTimerPort() ;
	}

	public SensorApp ()
	{
		super();
		radioEventTime = 0.0 ;
		cpuEventTime = 0.0 ;
		radioMode = -1 ;
		cpuMode = -1 ;
		coherentThreshold = 20.0 ; 
		lastSeenSNR = 0.0;
		lastSeenDataSize = 0;
		nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
		sink_nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
		first_target_nid = Integer.MIN_VALUE ; /* assuming no node has id Integer.MIN_VALUE */
		rTimer = null ;
		running_ = 0 ;
		eID = 0 ;
		setEventExportEnabled(true); 
	}

    public String getName() { return "SensorApp"; }
    public void setNid(long nid_)		{ nid = nid_; }
   	
	/** Sets the ID of the sink node to which information should be forwarded */
	public void setSinkNid(long sink_nid_)	{ sink_nid = sink_nid_; }

    public void setCoherentThreshold(double coherentThreshold_)
		{ coherentThreshold = coherentThreshold_; }
    public double getCoherentThreshold() {return  coherentThreshold; }

    public void duplicate(Object source_) 
    {
        super.duplicate(source_);
        SensorApp that_ = (SensorApp) source_;
        radioEventTime = that_.radioEventTime;
	cpuEventTime = that_.cpuEventTime;
        radioMode = that_.radioMode;
        cpuMode = that_.cpuMode;
	coherentThreshold = that_.coherentThreshold;
	nid = that_.nid;
	sink_nid = that_.sink_nid;
	first_target_nid = that_.first_target_nid;
	lastSeenSNR = that_.lastSeenSNR ;
	lastSeenDataSize = that_.lastSeenDataSize ;
	running_ = that_.running_ ;
	eID = that_.eID ;
    }

	/** Gets the new radio mode */
    protected synchronized void radioModeChanged(int mode)
    {
	radioEventTime = this.getTime() ;
	radioMode = mode ;
    }

	/** Gets the new CPU mode */
    protected synchronized void cpuModeChanged(int mode)
    {
	cpuEventTime = this.getTime() ;
	cpuMode = mode ;
    }

	/** Sets the radio mode  */
	protected synchronized void setRadioMode(int mode)
	{
		radioPort.doSending(new IntObj(mode));
	}

	/** Sets the CPU mode */
	protected synchronized void setCPUMode(int mode)
	{
		cpuPort.doSending(new IntObj(mode));
	}

	/** Handles information received over the sensor channel */
    protected synchronized void recvSensorEvent(Object data_)
    {
	SensorAppAgentContract.Message msg = (SensorAppAgentContract.Message) data_;
 	lastSeenSNR = msg.getSNR();
	lastSeenDataSize = msg.getDataSize();
	long target_nid = msg.getTargetNid();
	
	if ( nid != sink_nid )
	{
	}
	else
	{
		Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(target_nid - first_target_nid));
		if ( snrPort.anyOutConnection() )
			snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)lastSeenSNR), null);
	}

	if ( SUPPRESS_ON )
	{
		/* it is up to the application designer to see if this timer makes sense if there are more than one target in the simulation. In that case, the data included in a sensor packet might not be pertaining to the target_nid of that packet. In future releases, we can solve this problem by keeping a buffer of SensorPackets to be transmitted. */
		rTimer = setTimeout("SendPacket", (5.0*Math.log(10.0))/Math.log(lastSeenSNR));
		running_ = 1 ;
	} 
	else
	{
		if ( lastSeenSNR > coherentThreshold )
		{
			if ( nid != sink_nid )
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, (int)(100), COHERENT, lastSeenSNR, eID, target_nid)) ;
				eID = eID + 1 ;
			}
		}
		else
		{
			if ( nid != sink_nid )
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, lastSeenDataSize, NON_COHERENT, lastSeenSNR, eID, target_nid)) ;
				eID = eID + 1 ;
			}
		} // end if ( lastSeenSNR > coherentThreshold )
	} // end if ( SUPPRESS_ON )
    }

	/** Handles information received over the wireless channel  */
	/* this function is called whenever a packet (SensorPacket) is received through the wireless protocol stack */
    protected synchronized void recvSensorPacket(Object data_)
    {
	SensorPacket spkt = (SensorPacket) data_;
	if ( spkt.pktType == SUPPRESS )
	{
		if ( (running_==1) && (rTimer != null) )
			cancelTimeout(rTimer);
		running_ = 0;
	}
	else
	{
		if ( nid != sink_nid )
		{
		}
		else
		{
			lastSeenSNR = spkt.getMaxSnr();
			lastSeenDataSize = spkt.getDataSize();

			Port snrPort = (Port) getPort(SNR_PORT_ID + (int)(spkt.getTargetNid() - first_target_nid));
			if ( snrPort.anyOutConnection() )
				snrPort.exportEvent(SNR_EVENT, new DoubleObj((double)spkt.getMaxSnr()), null);
		}
	}
    }

    protected synchronized void processOther(Object data_, Port inPort_) {
        String portid_ = inPort_.getID();
        
        if (portid_.equals(RADIO_PORT_ID)) {
            if (!(data_ instanceof IntObj)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            radioModeChanged(((IntObj)data_).getValue());
            return;
        } else if (portid_.equals(CPU_PORT_ID)) {
            if (!(data_ instanceof IntObj)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            cpuModeChanged(((IntObj)data_).getValue());
            return;
        } else if (portid_.equals(FROM_SENSOR_AGENT_PORT_ID)) {
            if (!(data_ instanceof SensorAppAgentContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
	    recvSensorEvent(data_);
            return;
        } else if (portid_.equals(FROM_WIRELESS_AGENT_PORT_ID)) {
            if (!(data_ instanceof SensorPacket)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
	    recvSensorPacket(data_);
            return;
        }
        
        super.processOther(data_, inPort_);
    }

	protected synchronized void timeout(Object data_) {
	        if ( data_.equals("SendPacket") ) 
		{
			running_ = 0 ;
			
			if ( lastSeenSNR > coherentThreshold )
			{
				downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.BROADCAST_SENSOR_PACKET, SUPPRESS, lastSeenSNR, eID)) ;

				if ( nid != sink_nid )
				{
					downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, (int)(100), COHERENT, lastSeenSNR, eID, Integer.MIN_VALUE)) ; /* because we set a timer, the target information was lost, a temporary solution to pass Integer.MIN_VALUE */
					eID = eID + 1 ;
				}
			}
			else
			{
				if ( nid != sink_nid )
				{
					downPort.doSending(new SensorAppWirelessAgentContract.Message(SensorAppWirelessAgentContract.UNICAST_SENSOR_PACKET, sink_nid, lastSeenDataSize, NON_COHERENT, lastSeenSNR, eID, Integer.MIN_VALUE)) ; /* because we set a timer, the target information was lost, a temporary solution to pass Integer.MIN_VALUE */
					eID = eID + 1 ; 
				}
			} // end if ( lastSeenSNR > coherentThreshold )
	        }    
	}
}
