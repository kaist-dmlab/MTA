// @(#)WirelessPhy.java   1/2004
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

import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;
import java.io.*;
import java.util.StringTokenizer;
import java.lang.Math;

/**
 * This class simulates many functions of a wireless physical card. It piggy-backs
 * various information (ie. location of the sending node, the transmission power
 * of data frame etc.) to the mac layer data frame and passes that fram to the channel.
 * While receiving a data frame from the channel component, it determines whether 
 * that frame can be decoded correctly by consulting <code>RadioPropagationModel</code>
 * and passes the decodable frame to the mac layer. It also contains a <code>EnergyModel</code>
 * component to track the energy consumption.
 * @author Ye Ge
 */
public class WirelessPhy extends drcl.net.Module implements ActiveComponent {
    
    long nid;    //this may be removed later. right now, it is same at the node id.
   
    private static String STATUS_SEND    = "SEND";
    private static String STATUS_RECEIVE = "RECEIVE";
    private static String STATUS_IDLE    = "IDLE";
    //private static int totalCachePutNum = 0;
    
    private static int numAODV = 0;
    private static int numACK = 0;
    private static int numRTS = 0;
    private static int numCTS = 0;
    private static int numUDP = 0;
    private static int numOthers = 0;
    
    ACATimer idleenergytimer_ = null;
    
    private String status;
    
    // Assume AT&T's Wavelan PCMCIA card -- Chalermek
    //	Pt = 8.5872e-4; // For 40m transmission range.
    //  Pt = 7.214e-3;  // For 100m transmission range.
    //  Pt = 0.2818; // For 250m transmission range.
    //	Pt = pow(10, 2.45) * 1e-3;         // 24.5 dbm, ~ 281.8mw
    // 	Pt_consume = 0.660;  // 1.6 W drained power for transmission
    //  Pr_consume = 0.395;  // 1.2 W drained power for reception
    //	P_idle     = 0.035;  // 1.15 W drained power for idle
    
    /** The energy model installed */
    EnergyModel em;
    
    /** Transmitting power  */
    double Pt;

    /** The last time the node sends somthing.  */
    double last_send_time;	                
    
    /** When the channel be idle again. */
    double channel_become_idle_time;	  
    /** The last time we update energy. */
    double last_energy_update_time;	        
    
    /** Frequency. */
    double freq;            // frequency
    
    /** Wavelength  (m)     */
    double Lambda;	        // wavelength (m)
    // double L_ = 1.0;   	// system loss factor
    
    /**  receive power threshold (W)   */
    double RXThresh;	// receive power threshold (W)
    /**  carrier sense threshold (W)   */
    double CSThresh;	// carrier sense threshold (W)
    /**  capture threshold (db)        */
    double CPThresh;	// capture threshold (db)
    
    /* configurate the ports */
    public static final String CONFIG_PORT_ID      = ".config";
    public static final String CHANNEL_PORT_ID     = ".channel";
    public static final String PROPAGATION_PORT_ID = ".propagation";
    public static final String MOBILITY_PORT_ID    = ".mobility";
    public static final String ENERGY_PORT_ID      = ".energy";
    public static final String ANTENNA_PORT_ID  = ".antenna"; // Chunyu
    
    protected Port configPort      = addPort(CONFIG_PORT_ID, false);
    /** the port receiving packets from the channel */
    protected Port channelPort     = addPort(CHANNEL_PORT_ID, false);  // the port receiving packets from the channel
    /** the port to query the path loss */
    protected Port propagationPort = addPort(PROPAGATION_PORT_ID, false); // the port to query the path loss
    /** the port to query the current position of myself  */
    protected Port mobilityPort    = addPort(MOBILITY_PORT_ID, false); // the port to query the current position of myself
    
    protected Port energyPort      = addPort(ENERGY_PORT_ID, false);   
    
    /** antenna port  */
    protected Port antennaPort  = addPort(ANTENNA_PORT_ID, false); //Chunyu
    
    /* antenna -- Chunyu*/
    Antenna antenna = new Antenna();
    ACATimer lockTimer;
    
    // simulate the energy model and antenna model
    double  Gt = 1.0;   // transmitting antenna gain, should be moved to attenna component later
    double  Gr = 1.0;   // receiving antenna gain, should be moved to attenna component later       

    /** bandwidth   */
    double bandwidth;

    /** use this cache to speed up the simulation by avoiding unnecessary propagation loss calculation  */
    private Hashtable pathLossCache;    // use this cache to speed up the simulation by avoiding unnecessary propagation loss calculation
    private double Xc = 0.0;            // my previous position 
    private double Yc = 0.0;
    private double Zc = 0.0; 
    private double tc = -1.0;           // my cached position and the last time I consult the mobility model.
    private static double tp = -1.0;    //last time I print the statistics 

    /** Below are the tolerance of coordinate for hashing,
      * i.e., if any distance change (in any one dimension) exceeds the 
      * tolerance below, the path loss will be recalculated,
      * otherwise, it will be just pick from the cache.  
      * Notice the tolerance for Cardesian system and longitude-latitude
      * system is different.  */ 
    // At the equator, the circumference of the earth is 40,003 kilometers
    // (10.0/40003000.0)*360.0 = 9e-5
    private double xyz_tol = 10.0; 
    private double long_lat_tol = 0.00009;
    
    /** A sample card  */
    static class SampleCard {
        static double freq       = 900000000;
        static double bandwidth  = 2000000.0;
        static double Pt         = 0.2818;    // for 260m transmission range?
        static double Pt_consume = 0.660;
        static double Pr_consume = 0.395;
        static double P_idle     = 0.0;
        static double P_sleep    = 0.130;
        static double P_off      = 0.043;
        static double RXThresh   = 0.2818 * (1/100.0) * (1/100.0) * (1/100.0) * (1/100.0);
        static double CSThresh   = 0.2818 * (1/100.0) * (1/100.0) * (1/100.0) * (1/100.0) / 8.0;
        // static double RXThresh   = Math.pow(10.0, -20.0/10.0) * 0.001;;
        // static double CSThresh   = Math.pow(10.0, -30.0/10.0) * 0.001;;
        static double CPThresh   = 10;       // (db) 
        SampleCard() {};
    }
    
    /** tank to soldier card for NMS demo  */
    static class Demo_TSCard {   // tank to soldier card for NMS demo
        static double freq       = 2400000000.0;
        static double bandwidth  = 6000000.0;
        static double Pt         = Math.pow(10.0, (21.94 + 0.15 )/10.0) * 0.001;    // for 260m transmission range?
                                            // added 0.15 is Gt - cable loss
        static double Pt_consume = 0.660;
        static double Pr_consume = 0.395;
        static double P_idle     = 0.0;
        static double P_sleep    = 0.130;
        static double P_off      = 0.043;
        
        static double RXThresh   = Math.pow(10.0, -68.0/10.0) * 0.001;;
        static double CSThresh   = Math.pow(10.0, -78.0/10.0) * 0.001;;
        // static double RXThresh   = Math.pow(10.0, -20.0/10.0) * 0.001;;
        // static double CSThresh   = Math.pow(10.0, -30.0/10.0) * 0.001;;
        static double CPThresh   = 10;       // (db) 
        Demo_TSCard() {};
    }
    
    /** tank to tank card for NMS demo */
    static class Demo_TTCard {     // tank to tank card for NMS demo
        static double freq       = 2400000000.0;
        static double bandwidth  = 2000000.0;
        static double Pt         = Math.pow(10.0, (40.0 + 0.15)/10.0) * 0.001;    // for 260m transmission range?
                                            //the added 0.15 is Gt - cableloss = 2.15 - 2.0
        static double Pt_consume = 0.660;
        static double Pr_consume = 0.395;
        static double P_idle     = 0.0;
        static double P_sleep    = 0.130;
        static double P_off      = 0.043;
        
        static double RXThresh   = Math.pow(10.0, -68.0/10.0) * 0.001;;
        static double CSThresh   = Math.pow(10.0, -78.0/10.0) * 0.001;;
        // static double RXThresh   = Math.pow(10.0, -20.0/10.0) * 0.001;;
        // static double CSThresh   = Math.pow(10.0, -30.0/10.0) * 0.001;;
        static double CPThresh   = 10;       // (db) 
        Demo_TTCard() {};
    }
    
    /** tank to uav card for NMS demo  */
    static class Demo_TUCard {     // tank to uav card for NMS demo
        static double freq       = 2400000000.0;
        static double bandwidth  = 3000000.0;
        static double Pt         = Math.pow(10.0, 42.0/10.0) * 0.001;    // for 260m transmission range?
        static double Pt_consume = 0.660;
        static double Pr_consume = 0.395;
        static double P_idle     = 0.0;
        static double P_sleep    = 0.130;
        static double P_off      = 0.043;
        static double RXThresh   = Math.pow(10.0, -68.0/10.0) * 0.001;;
        static double CSThresh   = Math.pow(10.0, -78.0/10.0) * 0.001;;
        // static double RXThresh   = Math.pow(10.0, -20.0/10.0) * 0.001;;
        // static double CSThresh   = Math.pow(10.0, -30.0/10.0) * 0.001;;
        static double CPThresh   = 10;       // (db) xxxxxxxxok
        Demo_TUCard() {};
    }
    
    public void _start() {
        idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 10.0);
    }
    
    /**
     * Constructor. Sets some parameters according to a simple card. 
     */
    public WirelessPhy() {
        super();
        pathLossCache = new Hashtable();
        freq       = SampleCard.freq;
        Pt         = SampleCard.Pt;
        bandwidth  = SampleCard.bandwidth;
        Lambda = 300000000.0 / freq;   // wavelength (m)
        RXThresh   = SampleCard.RXThresh;
        CSThresh   = SampleCard.CSThresh;	    // carrier sense threshold (W)  //
        CPThresh   = SampleCard.CPThresh;       // capture threshold (db)
        last_send_time = 0.0;	            // the last time the node sends somthing.
        channel_become_idle_time = 0.0;	    // channel idle time.
        last_energy_update_time = 0.0;	    // the last time we update energy.
        em = new EnergyModel();
    }
    
    /**
     * Configures the card parameters.
     */
    public void configureCard(String card_) {
        if ( card_.equals("Demo_TSCard") ) {
            freq       = Demo_TSCard.freq;
            Pt         = Demo_TSCard.Pt;
            bandwidth  = Demo_TSCard.bandwidth;
            RXThresh   = Demo_TSCard.RXThresh;
            CSThresh   = Demo_TSCard.CSThresh;	    // carrier sense threshold (W)  //
            CPThresh   = Demo_TSCard.CPThresh;       // capture threshold (db)
            Lambda     = 300000000.0 / freq;   // wavelength (m)
        }
        else if ( card_.equals("Demo_TTCard") ) {
            freq       = Demo_TTCard.freq;
            Pt         = Demo_TTCard.Pt;
            bandwidth  = Demo_TTCard.bandwidth;
            RXThresh   = Demo_TTCard.RXThresh;
            CSThresh   = Demo_TTCard.CSThresh;	    // carrier sense threshold (W)  //
            CPThresh   = Demo_TTCard.CPThresh;       // capture threshold (db)
            Lambda     = 300000000.0 / freq;   // wavelength (m)
        }
        else if ( card_.equals("Demo_TUCard") ) {
            freq       = Demo_TUCard.freq;
            Pt         = Demo_TUCard.Pt;
            bandwidth  = Demo_TUCard.bandwidth;
            RXThresh   = Demo_TUCard.RXThresh;
            CSThresh   = Demo_TUCard.CSThresh;	    // carrier sense threshold (W)  //
            CPThresh   = Demo_TUCard.CPThresh;       // capture threshold (db)
            Lambda     = 300000000.0 / freq;   // wavelength (m)
        }
    }
    
    public String getName() { return "WirelessPhy"; }
    
    public void duplicate(Object source_) {
        super.duplicate(source_);
        WirelessPhy that_ = (WirelessPhy) source_;
        Pt = that_.Pt;
        RXThresh = that_.RXThresh;
        CSThresh = that_.CSThresh;
        CPThresh = that_.CPThresh;
        freq     = that_.freq;
        Lambda   = that_.Lambda;
        bandwidth  = that_.bandwidth;
        
        // need to duplicate the energy model?
    }
    
    public Port getChannelPort() { return channelPort; }
    
    /**
     * Sets the node id. 
     */
    public void setNid(long nid_) { nid = nid_; }
    
    /**
     * Gets the node id.
     */
    public long getNid(long nid_) { return nid; }
    
    /** Sets the transmission power */
    public void setPt(double Pt_) { Pt = Pt_; }
   
    /** Sets the power level.  */
    public void setPwl(int pwl_) {Pt = Pt/pwl_;} /* power level in decending order, 1 - maximum */
    
    public void setRxThresh(double RXThresh_) { RXThresh = RXThresh_; }
    public void setCSThresh(double CSThresh_) { CSThresh = CSThresh_; }
    public void setCPThresh(double CPThresh_) { CPThresh = CPThresh_; }
    
    /** Sets the frequency */
    public void setFreq(double freq_ ) { freq = freq_; Lambda = 300000000.0 / freq; }
    
    /**
     * Processes data frame coming from MAC component.
     */
    protected synchronized void dataArriveAtUpPort(Object data_,  drcl.comp.Port upPort_) {
        
        if ( !em.getOn() || em.getSleep() )
            return;     //  packet can not be transmitted, drop siliently
        
        //Decreases node's energy
        if ( em.energy > 0 ) {
            double txtime = ((Packet) data_).size * 8.0 / bandwidth;
            double start_time = Math.max(channel_become_idle_time, getTime());
            double end_time = Math.max(channel_become_idle_time, getTime()+txtime);
            double actual_txtime = end_time - start_time;
            
            // decrease the energy consumed during the period from the last energy updating time to the start of the this transmission
            if (start_time > last_energy_update_time) {
                em.updateIdleEnergy(start_time - last_energy_update_time);
                last_energy_update_time = start_time;
            }
            double temp = Math.max(getTime(), last_send_time);
            double begin_adjust_time = Math.min(channel_become_idle_time, temp);
            double finish_adjust_time = Math.min(channel_become_idle_time, getTime()+txtime);
            double gap_adjust_time = finish_adjust_time - begin_adjust_time;
            if (gap_adjust_time < 0.0) {
                drcl.Debug.error("Negative gap time. Check WirelessPhy.java! \n");
            }
            if ((gap_adjust_time > 0.0) && (status == STATUS_RECEIVE)) {
                em.updateTxEnergy(-gap_adjust_time);
                em.updateRxEnergy(gap_adjust_time);
            }
            em.updateTxEnergy(actual_txtime);
            if (end_time > channel_become_idle_time) {
                status = STATUS_SEND;
            }
            last_send_time = getTime() + txtime;
            channel_become_idle_time = end_time;
            last_energy_update_time = end_time;
            if (!em.getOn()) {
                // logEnergy();
            }
        }
        else {
            // siliently discards the packet 
            return;
        }

        double t;
        t = this.getTime();
        if ( Math.abs(t - tc) > 1.0 ) {  // the least position check interval is one second to speed up simulation
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
           
        }    
        downPort.doSending(new NodeChannelContract.Message(nid, Xc, Yc, Zc, Pt, Gt, data_));
        
        //Add by Honghai for debugging
        if (isDebugEnabled()  ) {
        	if (t - tp > 1.0)  {
         		printPktStat();
        		tp = t;
        	}
        	String pktType = ((Packet)data_).getPacketType();
        	if (pktType.equals("AODV") ) numAODV ++;
        	else if (pktType.equals("MAC-802.11_ACK_Frame") ) numACK++;
        	else if (pktType.equals("MAC-802.11_RTS_Frame") ) numRTS++;
        	else if (pktType.equals("MAC-802.11_CTS_Frame") ) numCTS++;
        	else if (pktType.equals("UDP") ) numUDP++;
        	else {
        		numOthers++;
        		System.out.println("type <" + pktType + ">" );
			}
		}
    }

	void printPktStat() {
		StringBuffer sb_ = new StringBuffer(toString());
		sb_.append("AODV packet: " + numAODV);
		sb_.append("\tRTS packet: " + numRTS);
		sb_.append("\tCTS packet: " + numCTS);
		sb_.append("\tACK packet: " + numACK);
		sb_.append("\tUDP packet: " + numUDP);
		sb_.append("\tOther packet: " + numOthers);
		debug(sb_.toString() );
	}
    
    void logEnergy() {
        ;//drcl.Debug.debug("At time: "+ getTime() + " Node" + nid + " remaining energy = " + em.getEnergy() + "\n");
    }

    /**
     * Processes the received frame.
     */
    protected synchronized void dataArriveAtChannelPort(Object data_) {
        double Pr;
        double Loss;
        double Pt_received;    // Pt of the received packet
        double Gt_received;    // Gt of the received packet
        double Xs, Ys, Zs;     // position of the sender
        
        boolean incorrect = false;
        
        Packet pkt;
        
        double t;
        t = this.getTime();
        if ( Math.abs(t - tc) > 1.0 ) {      // the least position check interval is one second to speed up simulation
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
        }
        
        NodeChannelContract.Message msg2 = (NodeChannelContract.Message) data_;
        Xs = msg2.getX();
        Ys = msg2.getY();
        Zs = msg2.getZ();
        Gt_received = msg2.getGt();
        Pt_received = msg2.getPt();
       
        String type = antenna.QueryType();
        Antenna.Orientation incomingOrient = new Antenna.Orientation(0, 0);
        if (!type.equals("OMNIDIRECTIONAL ANTENNA")) { 
                // add by Chunyu -- calculate the gain from uni-/omni-directional antenna gain
                // 1. calculate the incoming angle
                incomingOrient = CalcOrient (Xc, Yc, Zc, Xs, Ys, Zs);
                // 2. get the antenna gain in dBi and convert it to absolute value
                Gr = Math.exp (0.1 * antenna.getGain_dBi(incomingOrient) );
        }
        
        Long sid = new Long(msg2.getNid());
        
        
        boolean cacheHit = false;
        Loss = 1.0;        // Loss will be set to proper value below, here I set it to 1.0 to avoid the compiler's complaint
        
        if ( pathLossCache.containsKey(sid) ) {
            CachedPathLoss c = (CachedPathLoss) (pathLossCache.get(sid));
	    	if (RadioPropagationModel.isCartesianCoordinates()) {
				if (Math.abs(c.xs - Xs) <= xyz_tol &&
					Math.abs(c.ys - Ys) <= xyz_tol &&
					Math.abs(c.zs - Zs) <= xyz_tol && 
					Math.abs(c.xr - Xc) <= xyz_tol &&
					Math.abs(c.yr - Yc) <= xyz_tol &&
					Math.abs(c.zr - Zc) <= xyz_tol )   {
						cacheHit = true;
						Loss = c.loss;  
				}
			} else {
				if (Math.abs(c.xs - Xs) <= long_lat_tol &&
					Math.abs(c.ys - Ys) <= long_lat_tol &&
					Math.abs(c.zs - Zs) <= xyz_tol && 
					Math.abs(c.xr - Xc) <= long_lat_tol &&
					Math.abs(c.yr - Yc) <= long_lat_tol &&
					Math.abs(c.zr - Zc) <= xyz_tol )  	{
				 		cacheHit = true;
                		Loss = c.loss;  
				}	
			}
        }
        
        if ( cacheHit == false ) {
            RadioPropagationQueryContract.Message msg3 = (RadioPropagationQueryContract.Message) propagationPort.sendReceive(new RadioPropagationQueryContract.Message(Lambda, Xs, Ys, Zs, Xc, Yc, Zc ));
            Loss = msg3.getLoss();
            CachedPathLoss c = new CachedPathLoss(Xc, Yc, Zc, Xs, Ys, Zs, Loss);
            pathLossCache.put(sid, c);         
        }    
  
        Pr = Pt_received * Gt_received * Gr * Loss;
        
        // if the node is in sleeping mode, drop the packet simply
        //Rong's comment: it is possible that the interface overheard other's communication
        if ( !em.getOn() ) {
            //debug("Packet is dropped at node" + nid + " because the node is not on");
            return;
        }
        if ( em.getSleep() ) {
            //debug("Packet is dropped at node" + nid + " because the node is sleeping");
            return;
        }

        pkt = (Packet) msg2.getPkt();
       
        if ( Pr < CSThresh/1000 ) {
            return;
        }
        
        if ( Pr < RXThresh) {
            // can detect, but not successfully receive this packet.
            // marks the packet erro;
            incorrect = true;
        } 

        //if modulation is simulated, mark packek decoding error here
        
        /*
         * The MAC layer must be notified of the packet reception
         * now - ie; when the first bit has been detected - so that
         * it can properly do Collision Avoidance / Detection.
         */
        
        /*
         * Decrease energy if packet successfully received
         */
        double rcvtime = (8. * ((Packet) pkt).size) / bandwidth;
        // no way to reach here if the energy level < 0
        
        double start_time = Math.max(channel_become_idle_time, getTime());
        double end_time = Math.max(channel_become_idle_time, getTime() + rcvtime);
        double actual_rcvtime = end_time-start_time;
        
        if (start_time > last_energy_update_time) {
            em.updateIdleEnergy(start_time-last_energy_update_time);
            last_energy_update_time = start_time;
        }

        em.updateRxEnergy(actual_rcvtime);
        if (end_time > channel_become_idle_time) {
            status = STATUS_RECEIVE;
        }
        
        channel_become_idle_time = end_time;
        last_energy_update_time = end_time;
        
        if (em.getOn()) {
            // logEnergy();
        }
        
        /* added by Chunyu Aug. 05, 2002
         *  1. lock on the signal if the antenna is not locked
         *  2. if lock succeeds, recalculate Graphics= antenna.getGain() and Pr
         *  3. set a timer to unlock the antenna, which times out at end_time
         */
        if ( !type.equals("OMNIDIRECTIONAL ANTENNA") 
	     && Pr >= RXThresh && !antenna.isLocked()) {
             antenna.lockAtSignal (incomingOrient);
             Gr = Math.exp (0.1 * antenna.getGain_dBi (incomingOrient) );
             //	incomingOrient.azimuth + ". Gr = " + Gr); //for debug             
             Pr = Pt_received * Gt_received * Gr * Loss;
             lockTimer = setTimeout ("AntennaLockSignal_TimeOut", end_time-getTime());
         } //endif
        

        // MacPhyContract.Message is defined to convey all necessary information to the MAC component.
        MacPhyContract.Message msg4 = new MacPhyContract.Message(incorrect, Pr, CPThresh, CSThresh, RXThresh, pkt);
        upPort.doSending(msg4);
    }

    /* added by Chunyu -- 08.08.2002
     * Calculate the orientation of the sender (Xt, Yt, Zt) in regards to 
     * the receiver's position (Xr, Yr, Zr) 
     */
    /**
     * Calculates the orientation of the sender (Xt, Yt, Zt) in regards to 
     * the receiver's position (Xr, Yr, Zr) 
     *
     */
    protected Antenna.Orientation CalcOrient (
        double Xr, double Yr, double Zr, 
        double Xt, double Yt, double Zt) 
    {
        double delta_x, delta_y, delta_z, delta_xy;
        double alfa = 0, beta = 0;
        Antenna.Orientation orient;

        delta_x = Xt-Xr; 
        delta_y = Yt-Yr; 
        delta_z = Zt-Zr;
        delta_xy = Math.sqrt(delta_x*delta_x + delta_y*delta_y);
        
        if (delta_x==0) {
            if (delta_y==0) alfa = 0;
            else if(delta_y>0) alfa = 90;
            else alfa = 270;
        }else {
            alfa = Math.toDegrees (Math.abs (Math.atan(delta_y/delta_x)));
            if (delta_x>0 && delta_y>=0) ;
            else if (delta_x<0 && delta_y >=0) alfa = 180-alfa;
            else if (delta_x<0 && delta_y < 0) alfa = 180+alfa;
            else if (delta_x>0 && delta_y < 0) alfa = 360-alfa;
        }

        if (delta_xy==0){
            if (delta_z==0) beta = 0;
            else if (delta_z>0) beta = 90;
            else beta = 270;
        } else {
            beta = Math.toDegrees (Math.abs (Math.atan(delta_z/delta_xy)));
            if (delta_xy>0 && delta_z>=0) ;
            else if (delta_xy<0 && delta_z >=0) beta = 180 - beta;
            else if (delta_xy<0 && delta_z >=0) beta = 180 + beta;
            else if (delta_xy>0 && delta_z < 0) beta = 360 - beta;
        }
                        
        return new Antenna.Orientation ((int)alfa, (int)beta);
    } //end CalcOrient
        
    /**
     * Configures the node's antenna from assigned port.
     */
    protected void configAntenna (Object data_)
    {
        String args = ((String)data_).toLowerCase(), value;

        /* create an antenna */
        if (args.startsWith("create")) {
            String ant = args.substring (args.indexOf("create")+ 6);
            ant = ant.trim();

            if (ant.equals("antenna")) {
                //antenna = new Antenna();
                return;
            } 

            if (ant.equals("switchedbeam antenna")) {
                antenna = new SwitchedBeamAntenna();
                return;
            }

            if (ant.equals("adaptive antenna")) {
                antenna = new AdaptiveAntenna();
                return;
            }

            System.out.println ("FORMAT erorr! shall be <create antenna/switchedbeam antenna/adaptive antenna>");
            return;
        } //endif "create"

        /* Query the antenna type*/
        if (args.startsWith ("querytype")) {
            System.out.println (antenna.QueryType());
            return;
        } //endif "QueryType"

        /* initialization work */
        int index;
        if ( (index = args.indexOf ('=')) != -1) {                            
            value = (args.substring(index+1)).trim();            
            if (value.equals(null)) {
            System.out.println (this + ":: pls. use the format such as <height = 1.5>");
            return;               
        } //endif value

        if (args.indexOf ("height")!=-1) {
            float height = Float.parseFloat(value);
            antenna.setHeight (height);
            System.out.println ("set height = " + antenna.setHeight(height));
            return;
        } //endif "height"

        if (args.indexOf ("omnigain_dbi")!=-1) {
            float omniGain_dBi = Float.parseFloat(value);
            antenna.setOmniGain_dBi(omniGain_dBi);
            System.out.println ("set omniGain_dBi = " + antenna.getGain_dBi());
            return;
        } //endif "omniGain_dBi"

        if (args.indexOf ("azimuthpatterns")!=-1) {
            try {
                BufferedReader in = 
                    new BufferedReader (new FileReader(value));
                in.close();
            } catch (java.io.IOException e) {
                System.out.println (this + ":: error in opening " + value);
                return;
            } //endtry

            if (antenna.initAzimuthPatterns (value))
                ;//System.out.println (this + "Successfully initialize the azimuth pattern file!");
            else
                System.out.println (this + "Failure in initializing the azimuth pattern file!");
            return;
        } //endif "azimuthPatterns"

        if (args.indexOf ("elevationpatterns")!=-1) {
            try {
                BufferedReader in = 
                    new BufferedReader (new FileReader(value));
                in.close();
            } catch (java.io.IOException e) {
                System.out.println (this + ":: error in opening " + value);
                return;
            } //endtry

            if (antenna.initElevationPatterns (value))
                System.out.println (this + "Successfully initialize the elevation pattern file!");                    
            else
                System.out.println (this + "Failure in initializing the evlation pattern file!");
            // antenna transmission gain, should be moved to attenna component later
            return;
        } //endif "elevationPatterns"                

        System.out.println (" Wrong format: no such initialization item!");

        } //endif ' ... = ...'

        System.out.println ("Wrong format to communicate with the Antenna component!");
            
    } //end configAntenna
    
    protected synchronized void processOther(Object data_, Port inPort_) {
        String portid_ = inPort_.getID();
        
        if (portid_.equals(CHANNEL_PORT_ID)) {
            if (!(data_ instanceof NodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            dataArriveAtChannelPort(data_);  // a packet is "heard" from the channel
            return;
        }
        
        if (portid_.equals(ENERGY_PORT_ID)) {
            if (data_ == null) // this is a query message
                energyPort.doSending(em);
            else if (data_ instanceof BooleanObj) {
                em.setSleep(((BooleanObj) data_).getValue());
                updateIdleEnergy();
            }
            else
                drcl.Debug.error("WirelessPhy.processOther()", "Unrecognized contract", true);
            return;
        }
        
        /* antenna -- Chunyu */
        if (portid_.equals (ANTENNA_PORT_ID)) {
            configAntenna (data_);
            return;
        }
        
        super.processOther(data_, inPort_);
    }
    
    /**
     *  Preriodically timeout to update energy consumption even if it is in idle state.
     */
    public synchronized void timeout(Object data_) {
        if ( data_ instanceof String ){
            if ( ((String) data_).equals("IdleEnergyUpdateTimeout") ) {
                if ( em.getEnergy() > 0 )  logEnergy();
                updateIdleEnergy();
                idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 10.0);
            }
            else if ( ((String) data_).equals("AntennaLockSignal_TimeOut") ) {
                antenna.unlock();
            }
        }
    }
    
    /**
     * updates energy consumption during the idle state.
     */
    protected void updateIdleEnergy() {
        if ( getTime() > last_energy_update_time) {
            if(em.getOn()) {
                if(em.getSleep())
                    em.updateSleepEnergy(getTime() - last_energy_update_time);
                else
                    em.updateIdleEnergy(getTime() - last_energy_update_time);
            }
            last_energy_update_time = getTime();
        }
        //if ( em.getEnergy() > 0 )  logEnergy();
    }
}



