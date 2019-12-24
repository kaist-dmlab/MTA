// @(#)SensorPhy.java   12/2003
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
import drcl.util.random.*;

/** This class implements the sensor physical layer.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorPhy extends drcl.net.Module {

	public static final double TARGET_STRENGTH	= 50000.0; //initial target strength
	public static final double NOISE_STRENGTH	= 100.0; // initial noise strength
	public static final double NOISE_MEAN		= 0.0;
	public static final double NOISE_VAR		= 0.5;

	public static final long SEED_RNG = 10 ;

	long nid;
	double Radius ; // transmission radius.
	double  Pt ; // transmission power, same as targetStrength
	double noiseStrength;
	double lastNoisePower; // Power of the noise last seen by the phy layer
	GaussianDistribution gen;	
	double RxThresh ;

    /* configure the ports */
    public static final String CONFIG_PORT_ID      = ".config";
    public static final String CHANNEL_PORT_ID     = ".channel";
    public static final String PROPAGATION_PORT_ID = ".propagation";
    public static final String MOBILITY_PORT_ID    = ".mobility";
    public static final String TO_AGENT_PORT_ID    = ".toAgent";
    
    protected Port configPort      = addPort(CONFIG_PORT_ID, false);
    protected Port channelPort     = addPort(CHANNEL_PORT_ID, false);  // the port receiving packets from the channel
    protected Port propagationPort = addPort(PROPAGATION_PORT_ID, false); // the port to query the propagation module
    protected Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); // the port to query the current position of myself
    protected Port toAgentPort    = addPort(TO_AGENT_PORT_ID, false); // the port to forward received data to the sensor agent
        
	public SensorPhy (long nid_, double Radius_)
	{
		super();
		nid = nid_;
		Radius = Radius_;
		Pt = TARGET_STRENGTH ;
		noiseStrength = NOISE_STRENGTH ;
		lastNoisePower = 1.0 ;
		gen = null ;
		RxThresh = 200.0 ; /* value used in ns2 */
	}

	public SensorPhy ()
	{
		super();
		Pt = TARGET_STRENGTH ;
		noiseStrength = NOISE_STRENGTH ;
		lastNoisePower = 1.0 ;
		gen = null ;
		RxThresh = 200.0 ; /* value used in ns2 */
	}

    public String getName() { return "SensorPhy"; }
    
    public void duplicate(Object source_) 
    {
        super.duplicate(source_);
        SensorPhy that_ = (SensorPhy) source_;
        nid = that_.nid;
	Radius = that_.Radius;
        Pt = that_.Pt;
	noiseStrength = that_.noiseStrength;
	lastNoisePower = that_.lastNoisePower;
	gen = that_.gen ;
	RxThresh = that_.RxThresh ;
    }
    
	/** Gets the port that connects to the sensor channel */
    	public Port getChannelPort() { return channelPort; }
	/** Sets the ID of the node */
    	public void setNid(long nid_) { nid = nid_; }
	/** Gets the ID of the node */
    	public long getNid(long nid_) { return nid; }    
	/** Sets the transmission radius  */
    	public void setRadius(double Radius_) { Radius = Radius_; }
	/** Gets the transmission radius  */
    	public double getRadius() { return Radius; }
	/** Sets the receiving threshold  */
    	public void setRxThresh(double RxThresh_) { RxThresh = RxThresh_; }
    	/** Gets the receiving threshold  */
	public double getRxThresh() { return RxThresh; }
	/** Gets the noise power  */
	public double getNoisePower()	{	return lastNoisePower;	}
	/** Sets the noise strength */
	public void setNoiseStrength(double noiseStrength_)
	{	noiseStrength = noiseStrength_;		}
	/** Sets the target power  */
	public void setTargetPower(double Pt_)
	{	setPt(Pt_);	}
	public void setPt(double Pt_)	
	{	Pt = Pt_; 	}
    	/** Gets the target power  */
	public double getTargetPower()	
	{	return getPt();	}
	public double getPt()	
	{	return Pt; }

	/*
		Sensors do not send through the sensor protocol stack. 		Hence, dataArriveAtUpPort() should be called only 
		in a target node.
	*/
	/** Handles data arriving at UP port */
    	protected synchronized void dataArriveAtUpPort(Object data_, drcl.comp.Port upPort_) {
            if (!(data_ instanceof TargetPacket)) {
                error(data_, "dataArriveAtUpPort()", upPort_, "unknown object");
                return;
            }

	    // get most up-to-date location from SensorMobilityModel
            SensorPositionReportContract.Message msg = new SensorPositionReportContract.Message();
            msg = (SensorPositionReportContract.Message) mobilityPort.sendReceive(msg);
            double Xc = msg.getX();
            double Yc = msg.getY();
            double Zc = msg.getZ();

	/* Forward the packet received from the TargetAgent to the channel
	   so that other neighboring sensors can receive it */
	
	System.out.println("Target " + nid + ": generating signal at time " + getTime() + " loc: " + Xc + ", " + Yc + ", " + Zc);

        downPort.doSending(new SensorNodeChannelContract.Message(nid, Xc, Yc, Zc, Pt, Radius,new TargetPacket(((TargetPacket)data_).size, 
			        ((TargetPacket)data_).data))) ;
    }
    
	/** Handles data arriving at Channel port  */
    protected synchronized void dataArriveAtChannelPort(Object data_) 
	{
		long s ;
		if ( gen == null )
		{
                        s = SEED_RNG ;
                        // Uncomment the following line to get different results in every run
                        // s = (long)(Math.abs(java.lang.System.currentTimeMillis()*Math.random()*100)) ;
			gen = new GaussianDistribution(NOISE_MEAN, NOISE_VAR, s);
		}
              
	// get most up-to-date location of the node (i.e., the receiver
	// of the message) from SensorMobilityModel
            SensorPositionReportContract.Message msg = new SensorPositionReportContract.Message();
            msg = (SensorPositionReportContract.Message) mobilityPort.sendReceive(msg);
            double Xc = msg.getX();
            double Yc = msg.getY();
            double Zc = msg.getZ();

       	// extract the location of the sender from the message being received
        double Xs, Ys, Zs;     // position of the sender
        SensorNodeChannelContract.Message msg2 = (SensorNodeChannelContract.Message) data_;
        Xs = msg2.getX();
        Ys = msg2.getY();
        Zs = msg2.getZ();

	// extract also the power with which the packet was sent
        double Pt_received;    // Pt of the received packet
        Pt_received = msg2.getPt();

	long target_nid = msg2.getNid() ;
        
	// make up a SensorRadioPropagationContract to ask the 
	// propagation model to reply with the received signal strength
	SensorRadioPropagationQueryContract.Message msg3 = (SensorRadioPropagationQueryContract.Message) propagationPort.sendReceive(new SensorRadioPropagationQueryContract.Message(Pt_received, Xs, Ys, Zs, Xc, Yc, Zc));
        double Pr = msg3.getPr();

	if ( Pr < RxThresh )
	{
		System.out.println("SensorPhy nid=" + nid + " Packet was discarded because Pr = " + Pr + " < " + RxThresh);
	}
	else
	{
		double af = Pr ; // attenuation factor
		int size = ((TargetPacket)msg2.getPkt()).size ;
		TargetPacket sensorPkt = new TargetPacket(size, ((TargetPacket)msg2.getPkt()).data);
		lastNoisePower = 0.0 ;
		double rd ;
		for ( int k = 0 ; k < sensorPkt.size; k++)
		{
			sensorPkt.data[k] = sensorPkt.data[k] * af ; // attenuate the signal
			rd = gen.nextDouble() ;
			double noise = noiseStrength * rd ;
			sensorPkt.data[k] = sensorPkt.data[k] + noise;
			lastNoisePower = lastNoisePower + (noise * noise);
		} // end for

		lastNoisePower = lastNoisePower / ((double)size);

		// Forward the data packet up to the sensor agent
	        toAgentPort.doSending(new SensorAgentPhyContract.Message(lastNoisePower, new TargetPacket(size, sensorPkt.data), target_nid)) ;
	} // end else
    } // end dataArriveAtChannelPort
    
    protected synchronized void processOther(Object data_, Port inPort_) {
        String portid_ = inPort_.getID();
        
        if (portid_.equals(CHANNEL_PORT_ID)) {
            if (!(data_ instanceof SensorNodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            dataArriveAtChannelPort(data_);
            return;
        }
        
        super.processOther(data_, inPort_);
    }
}
