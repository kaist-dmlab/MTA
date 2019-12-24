// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
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

import drcl.*;
import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.mac.*;
import drcl.util.scalar.IntSpace;
import java.util.*;

/**
 * This class implements IEEE802.11 protocol. Part of this class is ported from ns-2.1b7a
 * implementation. 
 *
 * @author Ye Ge
 */
public class Mac_802_11 extends Module implements ActiveComponent {
    double last_time = 0.0;
    
    /**
     * Flag to enable and disable tracing all events.
     */
    public boolean MAC_TRACE_ALL_ENABLED    = false;
    
    /**
     * Flag to enable and disable tracing packet arriving and departal events.
     */
    public boolean MAC_TRACE_PACKET_ENABLED = false;
    
    /**
     * Flag to enable and disable tracing changes of CW.
     */
    public boolean MAC_TRACE_CW_ENABLED     = false;
    
    /**
     * Flag to enable and disable tracing certain special events.
     */
    public boolean MAC_TRACE_EVENT_ENABLED  = false;
    
    /**
     * Flag to enable and disable tracing timer events.
     */
    public boolean MAC_TRACE_TIMER_ENABLED  = false;

    /** Sets the MAC_TRACE_ALL_ENABLED flag.  */
    public void set_MAC_TRACE_ALL_ENABLED(boolean b_)    { MAC_TRACE_ALL_ENABLED    = b_; };
    /** Sets the MAC_TRACE_PACKET_ENABLED flag.  */
    public void set_MAC_TRACE_PACKET_ENABLED(boolean b_) { MAC_TRACE_PACKET_ENABLED = b_; };
    /** Sets the MAC_TRACE_CW_ENABLED flag.  */
    public void set_MAC_TRACE_CW_ENABLED(boolean b_)     { MAC_TRACE_CW_ENABLED     = b_; };
    /** Sets the MAC_TRACE_EVENT_ENABLED flag.  */
    public void set_MAC_TRACE_EVENT_ENABLED(boolean b_)  { MAC_TRACE_EVENT_ENABLED  = b_; };
    /** Sets the MAC_TRACE_TIMER_ENABLED flag.  */
    public void set_MAC_TRACE_TIMER_ENABLED(boolean b_)  { MAC_TRACE_TIMER_ENABLED  = b_; };
    
	/** Set PSM mode */
    public void set_MAC_PSM(boolean b_) {psm_enabled_ = b_; if(psm_enabled_) psm_mode_ = PSM_PWR_SAVE;}
	/** Enable PSM mode and set psm_mode_ to PSM_PWR_SAVE */
    public void enable_PSM() {psm_enabled_ = true; psm_mode_ = PSM_PWR_SAVE;}
	/** Disable PSM mode */
    public void disable_PSM() {psm_enabled_ = false;}

    /** Sets the is_malicious flag. */ 
    public void setMalicious(boolean b_) {is_malicious_ = b_;}
    
    /** Turns on the MAC_TRACE_ALL_ENABLED flag. */ 
    public void enable_MAC_TRACE_ALL( )    { MAC_TRACE_ALL_ENABLED    = true; };
    /** Turns on the MAC_TRACE_PACKET_ENABLED flag. */ 
    public void enable_MAC_TRACE_PACKET( ) { MAC_TRACE_PACKET_ENABLED = true; };
    /** Turns on the MAC_TRACE_CW_ENABLED flag. */ 
    public void enable_MAC_TRACE_CW( )     { MAC_TRACE_CW_ENABLED     = true; };
    /** Turns on the MAC_TRACE_EVENT_ENABLED flag. */ 
    public void enable_MAC_TRACE_EVENT( )  { MAC_TRACE_EVENT_ENABLED  = true; };
    /** Turns on the MAC_TRACE_TIMER_ENABLED flag. */ 
    public void enable_MAC_TRACE_TIMER( )  { MAC_TRACE_TIMER_ENABLED  = true; };
    
    /** Turns off the MAC_TRACE_ALL_ENABLED flag. */ 
    public void disable_MAC_TRACE_ALL( )    { MAC_TRACE_ALL_ENABLED    = false; };
    /** Turns off the MAC_TRACE_PACKET_ENABLED flag. */ 
    public void disable_MAC_TRACE_PACKET( ) { MAC_TRACE_PACKET_ENABLED = false; };
    /** Turns off the MAC_TRACE_CW_ENABLED flag. */ 
    public void disable_MAC_TRACE_CW( )     { MAC_TRACE_CW_ENABLED     = false; };
    /** Turns off the MAC_TRACE_EVENT_ENABLED flag. */ 
    public void disable_MAC_TRACE_EVENT( )  { MAC_TRACE_EVENT_ENABLED  = false; };
    /** Turns off the MAC_TRACE_TIMER_ENABLED flag. */ 
    public void disable_MAC_TRACE_TIMER( )  { MAC_TRACE_TIMER_ENABLED  = false; };
    
    protected static final String LL_PORT_ID           = ".linklayer";
    protected static final String MAC_TRACE_PORT_ID    = ".mactrace";
    protected static final String ENERGY_PORT_ID       = ".energy";
    
    protected Port llPort      = addPort(LL_PORT_ID, false);
    protected Port tracePort   = addPort(MAC_TRACE_PORT_ID);
    
    /**
     * interface to the energy module
     */
    protected Port energyPort  = addPort(ENERGY_PORT_ID, false); 
    
    /** 
     * The event type of the link broken event. 
     */
    public static final String EVENT_LINK_BROKEN = "Link Broken";
    
    protected static final String EVENT_LINK_BROKEN_PORT_ID = ".linkbroken";
    Port brokenPort = addEventPort(EVENT_LINK_BROKEN_PORT_ID);
    
    /* constants for packet drop reasons */
    private static final String DROP_END_OF_SIMULATION        = "END";
    private static final String DROP_MAC_COLLISION            = "COL";
    private static final String DROP_MAC_DUPLICATE            = "DUP";
    private static final String DROP_MAC_PACKET_ERROR         = "ERR";
    private static final String DROP_MAC_RETRY_COUNT_EXCEEDED = "RET";
    private static final String DROP_MAC_INVALID_STATE        = "STA";
    private static final String DROP_MAC_BUSY                 = "BSY";
    private static final String DROP_MAC_ATIM_RETRY_COUNT     = "MRC";
    
    private static final String DROP_RTR_NO_ROUTE             = "NRTE";  // no route
    private static final String DROP_RTR_ROUTE_LOOP           = "LOOP";  // routing loop
    private static final String DROP_RTR_TTL                  = "TTL";   // ttl reached zero
    private static final String DROP_RTR_QFULL                = "IFQ";   // queue full
    private static final String DROP_RTR_QTIMEOUT             = "TOUT";  // packet expired
    private static final String DROP_RTR_MAC_CALLBACK         = "CBK";   // MAC callback
    
    private static final String DROP_IFQ_QFULL                = "IFQ";   // no buffer space in IFQ
    private static final String DROP_IFQ_ARP_FULL             = "ARP";   // dropped by ARP
    private static final String DROP_OUTSIDE_SUBNET           = "OUT";   // dropped by base stations if received rtg updates from nodes outside its domain.
    private static final String DROP_MAC_BUFFER_FULL          = "BUF";
    
    /** Broadcast mac address. */
    public static final long MAC_BROADCAST	 = -1;
    
    
    /** Broadcast mac address. Used in LL_Demo. */
    //public static final long BCAST_ADDR      = -1;  // deleted after modifying LL_Demo.
    
    public static final String[] MAC_STATE = {"MAC_IDLE",
                                              "MAC_POLLING",
                                              "MAC_RECV",
                                              "MAC_SEND",
                                              "MAC_RTS",
                                              "MAC_CTS",
                                              "MAC_ACK",
                                              "MAC_COLL"};
    
	/** Idle state */
    public static final int MAC_IDLE     = 0x0000;
	/** Polling state */
    public static final int MAC_POLLING  = 0x0001;
	/** Recving state */
    public static final int MAC_RECV     = 0x0010;
	/** Transmitting state */
    public static final int MAC_SEND     = 0x0100;
	/** RTS sent */
    public static final int MAC_RTS      = 0x0200;
	/** CTS sent */
    public static final int MAC_CTS      = 0x0400;
	/** ACK sent */
    public static final int MAC_ACK      = 0x0800;
	/** Collision state */
    public static final int MAC_COLL     = 0x1000;
	/** Beacon transmitted */
    public static final int MAC_BEACON   = 0x2000;
	/** Inside ATIM window */
    public static final int MAC_ATIM     = 0x4000;
    
	/** beaconing */
    public static final int MF_BEASON   = 0x0008; 
	/** used as mask for control frame */
    public static final int MF_CONTROL  = 0x0010; 
	/** Announce slot open for contension */
    public static final int MF_SLOTS    = 0x001a; 
	/** Request to send */
    public static final int MF_RTS      = 0x001b; 
	/** Clear to send */
    public static final int MF_CTS      = 0x001c; 
	/** Acknowledgement */
    public static final int MF_ACK      = 0x001d; 
	/** contention free period end */
    public static final int MF_CF_END   = 0x001e; 
	/** Polling */
    public static final int MF_POLL     = 0x001f; 
	/** Used as a mask for data frame */
    public static final int MF_DATA     = 0x0020; 
	/** Ack for data frame */
    public static final int MF_DATA_ACK = 0x0021; 
    
    /**
     * class for buffered packets in IEEE 802.11 PSM
     */
    class BUFFER_ENTRY {
        Packet packet_;
        long addr_;
        boolean sent_atim_;
        boolean recvd_ack_;
        boolean sent_data_;
        boolean pwr_mgt_;
        boolean pwr_mgt_updated_;
        int age_;
        
        public BUFFER_ENTRY(Packet packet, long addr, boolean sent_atim,
        boolean recvd_ack, boolean sent_data, int age) {
            packet_ = packet;
            addr_ = addr;
            
            sent_atim_ = sent_atim;
            recvd_ack_ = recvd_ack;
            sent_data_ = sent_data;
            pwr_mgt_ = true;
            pwr_mgt_updated_ = false;
            age_ = age;
        }
        
        public long get_addr() {
            if (packet_ != null) {
                return addr_;
            }
            return -1;
        }
    }
    
    /**
     * class which is defined for the MAC testing purpose
     */
    class DummyEnergyModel {
        boolean is_sleep = false;
        boolean is_on = true;
        double residual_energy;
        
        public DummyEnergyModel() {
        }
        
        boolean if_on() {
            is_on = ((EnergyModel)energyPort.sendReceive(null)).getOn();
            return is_on;
        }
        
        boolean if_sleep() {
            is_sleep = ((EnergyModel)energyPort.sendReceive(null)).getSleep();
            return is_sleep;
        }
        
        double residual_energy() {
            residual_energy = ((EnergyModel)energyPort.sendReceive(null)).getEnergy();
            return residual_energy;
        }
        
        void setSleep(boolean sleep_) {
            is_sleep = sleep_;
            energyPort.doSending(new BooleanObj(sleep_));
        }
    }
    
    /**
     * Physical Layer Management Information Base.
     */
    class PHY_MIB {
        /* static */
        int    DSSS_CWMin                 = 31;
        int    DSSS_CWMax                 = 1023;
        double DSSS_SlotTime              = 0.000020;	// 20us
        double DSSS_CCATime	              = 0.000015;	// 15us
        double DSSS_RxTxTurnaroundTime    = 0.000005;	// 5us
        double DSSS_SIFSTime			  = 0.000010;	// 10us
        int    DSSS_PreambleLength		  = 144;	    // 144 bits
        int    DSSS_PLCPHeaderLength	  = 48;	        // 48 bits
        
        int           CWMin;
        int           CWMax;
        double	      SlotTime;
        double	      CCATime;
        double	      RxTxTurnaroundTime;
        double	      SIFSTime;
        int           PreambleLength;
        int           PLCPHeaderLength;
        
        public PHY_MIB() {
            CWMin = DSSS_CWMin;
            CWMax = DSSS_CWMax;
            SlotTime = DSSS_SlotTime;
            CCATime =  DSSS_CCATime;
            RxTxTurnaroundTime = DSSS_RxTxTurnaroundTime;
            SIFSTime = DSSS_SIFSTime;
            PreambleLength = DSSS_PreambleLength;
            PLCPHeaderLength = DSSS_PLCPHeaderLength;
        }
    }
    
    /**
     * MAC Layer Management Information Base.
     */
    class MAC_MIB {
       /*
        * IEEE 802.11 Spec, section 11.4.4.2
        *      - default values for the MAC Attributes
        */
        int MAC_RTSThreshold	        = 3000;		// bytes
        int MAC_ShortRetryLimit		    = 7;   		// retransmittions
        int MAC_LongRetryLimit		    = 4;	   	// retransmissions
        int MAC_FragmentationThreshold	= 2346;		// bytes
        int MAC_MaxTransmitMSDULifetime	= 512;		// time units
        int MAC_MaxReceiveLifetime		= 512;		// time units
		double MAC_BeaconPeriod = 0.100; // in second
		double MAC_ATIMWindow = 0.008; //in second
        
        int MAC_ATIMRetryLimit = 7;
        //     MACAddress;
        //	   GroupAddresses;
        int    RTSThreshold;
        int    ShortRetryLimit;
        int    LongRetryLimit;
        int    FragmentationThreshold;
        int    MaxTransmitMSDULifetime;
        int    MaxReceiveLifetime;
        //	   ManufacturerID;
        //	   ProductID;
        
        int    TransmittedFragmentCount = 0;
        int    MulticastTransmittedFrameCount = 0;
        int    FailedCount = 0;
        int    RetryCount = 0;
        int    MultipleRetryCount = 0;
        int    FrameDuplicateCount = 0;
        int    RTSSuccessCount = 0;
        int    RTSFailureCount = 0;
        int    ACKFailureCount = 0;
        int    ReceivedFragmentCount = 0;
        int    MulticastReceivedFrameCount = 0;
        int    FCSErrorCount = 0;
        
        double BeaconPeriod = 0;
        double ATIMWindow   = 0;
        
        public MAC_MIB() {
			RTSThreshold            = MAC_RTSThreshold;
			ShortRetryLimit         = MAC_ShortRetryLimit;
			LongRetryLimit          = MAC_LongRetryLimit;
			FragmentationThreshold  = MAC_FragmentationThreshold;
			MaxTransmitMSDULifetime = MAC_MaxTransmitMSDULifetime;
			MaxReceiveLifetime      = MAC_MaxReceiveLifetime;
			BeaconPeriod 			= MAC_BeaconPeriod;
			ATIMWindow   			= MAC_ATIMWindow;
        }
    }
    
	/** Set the RTS threshold (size of packet to transmit RTS) */
    public void setRTSThreshold(int rstthreshold_) {
        macmib_.RTSThreshold = rstthreshold_;
    }
    
	/** Set the length of beacon interval */
    public void setBeaconInterval(double beacon_) {
        macmib_.BeaconPeriod = beacon_;
    }

	/** Set the size of ATIM window */
    public void setATIMWindow(double atim_) {
        macmib_.ATIMWindow = atim_;
    }
    /**
     * Calculate the transmission time of given bytes.
     */
    double txtime(int bytes) {
        return (8. * bytes / bandwidth_);
    }
    
    /**
     * Calculates the transmission time of a given packet.
     */
    double txtime(Packet p) {
        return 8. * (p.size) / bandwidth_;
    }

    /**
     * My MAC address
     */
    long   macaddr_;		
    
    /**
     * Channel bit rate.
     */
    double bandwidth_;      
    
    /**
     * MAC overhead.
     */
    double delay_;		    
    
    /**
     * Sets the channel bandwidth and calculates all related variables.
     *
     * @param bw_ channel bandwidth (bps)
     */
    public void   setBandwidth(double bw_) {
        bandwidth_ = bw_;
        
        sifs_ = phymib_.SIFSTime;
        pifs_ = sifs_ + phymib_.SlotTime;
        difs_ = sifs_ + 2*phymib_.SlotTime;
        eifs_ = sifs_ + difs_ + DATA_Time(ETHER_ACK_LEN + phymib_.PreambleLength/8 + phymib_.PLCPHeaderLength/8);
        tx_sifs_ = sifs_ - phymib_.RxTxTurnaroundTime;
        tx_pifs_ = tx_sifs_ + phymib_.SlotTime;
        tx_difs_ = tx_sifs_ + 2 * phymib_.SlotTime;
        
        setEtherHdrLen();
        setEtherRTSLen();
        setEtherCTSLen();
        setEtherACKLen();
        setRTSTime();
        setCTSTime();
        setACKTime();
        setBeaconTime();
        setCTSTimeout();
        setNAVTimeout();
    }
    
    /**
     * Gets the channel bandwidth.
     *
     */
    public double bandwidth() { return bandwidth_; }

    /**
     * Set the MAC address 
     *
     *@param addr_  the MAC address
	 */
    public void   setMacAddress(long addr_) { macaddr_ = addr_; }
    
    /**
     * Get the Mac address
	 */
    public long   getMacAddress( ) { return macaddr_; }
    
    private   int  ETHER_HDR_LEN;
    private   int  ETHER_RTS_LEN;
    private   int  ETHER_CTS_LEN;
    private   int  ETHER_ACK_LEN;
    private   int  ETHER_BEACON_LEN;
    
    private   void setEtherHdrLen() {
        ETHER_HDR_LEN = (phymib_.PreambleLength>>3)+(phymib_.PLCPHeaderLength>>3)
        +Mac_802_11_Data_Frame.Mac_802_11_Data_Frame_Header_Length;
    }
    private   void setEtherRTSLen() {
        ETHER_RTS_LEN = (phymib_.PreambleLength>>3)+(phymib_.PLCPHeaderLength>>3)
        +Mac_802_11_RTS_Frame.Mac_802_11_RTS_Frame_Header_Length;
    }
    private   void setEtherCTSLen() {
        ETHER_CTS_LEN = (phymib_.PreambleLength>>3)+(phymib_.PLCPHeaderLength>>3)
        +Mac_802_11_CTS_Frame.Mac_802_11_CTS_Frame_Header_Length;
    }
    private   void setEtherACKLen() {
        ETHER_ACK_LEN = (phymib_.PreambleLength>>3)+(phymib_.PLCPHeaderLength>>3)
        +Mac_802_11_ACK_Frame.Mac_802_11_ACK_Frame_Header_Length;
    }
    
    private void setEtherBeaconLen() {
        ETHER_BEACON_LEN = (phymib_.PreambleLength>>3)+(phymib_.PLCPHeaderLength>>3)
        + Mac_802_11_Beacon_Frame.Mac_802_11_Beacon_Frame_Header_Length;
    }
    
    private   double RTS_Time;    //  seconds
    private   double CTS_Time;    //  seconds
    private   double ACK_Time;    //  seconds
    private   double Beacon_Time; //  seconds
    
    /* while calling DATA_Time(), len_ has already counted the extra overhead bits in */
    private   double DATA_Time(int len_) {        // seconds
        return 8 * len_ / bandwidth_;
    }
    
    protected double calDataTime(int len_) {      // seconds
        return 8 * len_ / bandwidth_;
    }
    
    protected void setRTSTime() {                 // seconds
        setEtherRTSLen();
        RTS_Time = 8*ETHER_RTS_LEN/bandwidth_;
    }
    
    protected void setCTSTime() {                 // seconds
        setEtherCTSLen();
        CTS_Time = 8*ETHER_CTS_LEN/bandwidth_;
    }
    
    protected void setACKTime() {                 // seconds
        setEtherACKLen();
        ACK_Time = 8 * ETHER_ACK_LEN / bandwidth_;
    }
    
    protected void setBeaconTime() {                 // seconds
        setEtherBeaconLen();
        Beacon_Time = 8 * ETHER_BEACON_LEN / bandwidth_;
    }
    
    
    private   double CTSTimeout;
    protected void setCTSTimeout() {
        CTSTimeout = (RTS_Time + CTS_Time) + 2 * sifs_;
    }
    private   double ACKTimeout(int len_) {
        return (DATA_Time(len_) + ACK_Time + sifs_ + difs_);
    }
    protected double calACKTimeout(int len_) {
        return (calDataTime(len_) + ACK_Time + sifs_ + difs_);
    }
    private   double NAVTimeout;
    protected void setNAVTimeout() {
        NAVTimeout = (2 * phymib_.SIFSTime + CTS_Time + 2 * phymib_.SlotTime);
    }
    
    private   int  RTS_DURATION(Packet pkt) {       // micro seconds
        return usec(sifs_ + CTS_Time + sifs_ + TX_Time(pkt) + sifs_ + ACK_Time);
    }
    protected int calRTSDuration(Packet pkt) {
        return usec(sifs_ + CTS_Time + sifs_ + TX_Time(pkt) + sifs_ + ACK_Time);
    }
    private   int  CTS_DURATION(int dur_) {
        return usec((dur_*1e-6) - (CTS_Time + sifs_));
    }
    protected int calCTSDuration(double dur_) {  //XXX dur_ in usec?
        return usec((dur_*1e-6) - (CTS_Time + sifs_));
    }
    private   int  DATA_DURATION( ) {
        return usec(ACK_Time + sifs_);
    }
    private   int  ACK_DURATION( ) {
        return 0x00;                // we are not doing fragments now
    }
    
    protected double TX_Time(Packet p) {
        
        _assert("TX_Time", "p != null", p != null);
        double t = DATA_Time(p.size);
        return t;
    }
    
    private void inc_cw( ) {
        cw_ = (cw_ << 1) + 1;
        if(cw_ > phymib_.CWMax)  cw_ = phymib_.CWMax;
        trace("inc_cw()   cw_ = " + cw_, "CW");
    }
    
    private void rst_cw( ) {
        cw_ = phymib_.CWMin;
        trace("rst_cw()   cw_ = " + cw_, "CW");
    }
    
    private int usec(double t) {
        int us = (int) Math.ceil(t * 1e6);
        return us;
    }
    
    private void set_nav(int us) {     
        double now = getTime();
        double t = us * 1e-6;
        
        if((now + t) > nav_) {
            
            nav_ = now + t;
            if(nav_timer_.busy())
                nav_timer_.stop();
            nav_timer_.start(t);
        }
    }
    
    protected PHY_MIB phymib_;
    protected MAC_MIB macmib_;
    
    IFTimer       if_timer_;		// interface timer, timeout when the transmission is finished
    NavTimer      nav_timer_;	    // NAV timer
    RxTimer       rx_timer_;		// incoming packets
    TxTimer       tx_timer_;		// outgoing packets, timeout when no expected response received
    
    DeferTimer    df_timer_;        // defer timer
    BackoffTimer  bf_timer_;        // backoff timer
    
    ATIMEndTimer atim_timer_;
    TBTTTimer    tbtt_timer_;
    BeaconTimer  beacon_timer_;
    TSFTimer     tsf_timer_;
    /* ============================================================
       Internal MAC State
       ============================================================ */
    /**
     * Network Allocation Vector.
     */
    double		nav_;		
    
    /**
     * Incoming state (MAC_RECV or MAC_IDLE).
     */
    int         rx_state_;	
    
    /**
     * Outgoing state.
     */
    int         tx_state_;	
    
    /**
     * Transmitter is ACTIVE or not.
     */ 
    boolean    	tx_active_;	 
    
    /**
     * MAC's current state.
     */
    int         state_;	
    
    // these three variables are defined here to keep some information pased from wirelessphy related to the packet being received.
    
    /**
     * Whether this received packet is of error.
     */
    boolean txinfo_pktRx_error;    
    
    /**
     * The received power of this packet.
     */
    double  txinfo_pktRx_RxPr;     
    
    /**
     * Capture threshhold of the wireless physical layer.
     */
    double  txinfo_pktRx_CPThresh;  
    
    double interferencePwr = 0.0; //Rong: sum-up all the inference signals
    boolean is_malicious_ = false; //Rong: want to simulate a malicious node that does not do carrier sense
    
    
    /**
     * Receivd IEEE802.11 frame.
     */ 
    Mac_802_11_Packet pktRx_;
    
    /**
     * IEEE802.11 frame to be transmited.
     */ 
    Mac_802_11_Packet pktTx_;
    
    /**
     * Outgoing RTS packet.
     */
    Mac_802_11_Packet pktRTS_;     
    
    /**
     * Outgoing non-RTS packet.
     */
    Mac_802_11_Packet pktCTRL_;    
    
    /**
     * Contention Window.
     */
    int         cw_;		// Contention Window
    
    /** STA Short Retry Count. */
    int         ssrc_;		
    /** STA Long Retry Count. */
    int         slrc_;		
    /** Short Interface Space. */
    double		sifs_;		
    /** PCF Interframe Space. */
    double		pifs_;		
    /** DCF Interframe Space. */
    double		difs_;		
    /** Extended Interframe Space. */
    double		eifs_;		
    
    double		tx_sifs_;
    double		tx_pifs_;
    double		tx_difs_;
    
    int	        min_frame_len_;
    
    //Rong: added for power management 08/16/02
    boolean got_beacon_ = false;
    boolean psm_enabled_ = true;
    
   
    //WirelessPhy netif_;
    DummyEnergyModel netif_;
    
    /* 
       psm mode is decided by mac layer,
       at physical layer only three states are possible: active, doze, shut-down
     */

    int psm_mode_;
	/** PSM enabled and in power saving */
    public final static int PSM_PWR_SAVE = 0;
	/** PSM enabled but not in power saving */
    public final static int PSM_PWR_AWAKE = 1;
    
    boolean recvd_atim_   = false;
	int initial_wake_count = 10;
	int tx_bcast_atim_ = 0;
    int recvd_bcast_atim_ = 0;
    int recvd_ucast_atim_ = 0;
    int has_packet_tx_    = 0;
    int atimrc            = 0;
    long last_atim_da_    = 0;
    
    Mac_802_11_Beacon_Frame pktBeacon_ = null;
    Mac_802_11_ATIM_Frame pktATIM_ = null;
    BUFFER_ENTRY entryATIM_ = null;
    
    Vector psm_buffer;
    
    protected void _start() {

        // release_1208    
        //if ( isDebugEnabled() == true )    debug("_start");

        if (psm_enabled_) {
            psm_mode_ = PSM_PWR_SAVE;
            netif_.setSleep(true);
            tbtt_timer_.start(2*macmib_.BeaconPeriod - Math.IEEEremainder(tsf_timer_.getTSF(), macmib_.BeaconPeriod*1e6)/1e6);
            trace("start beacon timer", "TIMER");
        }
    }
    
    /* ============================================================
       Duplicate Detection state
       ============================================================ */
    int    sta_seqno_;	// next seqno that I'll use
    Hashtable cache_;   //hashtable is used to record the recently received mac frame sequence number to detect the duplicate frames
    
    public Mac_802_11() {
        super();
        phymib_ = new PHY_MIB();
        macmib_ = new MAC_MIB();
        
        nav_ = 0.0;
        
        tx_state_ = MAC_IDLE;
        rx_state_ = MAC_IDLE;
        tx_active_ = false;
        
        pktRTS_ = null;
        pktCTRL_ = null;
        
        cw_ = phymib_.CWMin;
        
        ssrc_ = 0;  slrc_ = 0;
        
        setBandwidth(2000000.0);  // 2M bps
        
        sta_seqno_ = 1;
        
        cache_ = new Hashtable();
       
        if_timer_  = new IFTimer(this);		// interface timer
        nav_timer_ = new NavTimer(this);    // NAV timer
        rx_timer_  = new RxTimer(this);		// incoming packets
        tx_timer_  = new TxTimer(this);		// outgoing packets
        
        df_timer_  = new DeferTimer(this, phymib_.SlotTime);          // defer timer
        bf_timer_  = new BackoffTimer(this, phymib_.SlotTime);        // backoff timer
        
        tbtt_timer_   = new TBTTTimer(this);
        atim_timer_   = new ATIMEndTimer(this);
        beacon_timer_ = new BeaconTimer(this);
        tsf_timer_    = new TSFTimer(this, 0);
        psm_buffer    = new Vector();
        
        //XXX: for now
        netif_ = new DummyEnergyModel();
    }
    
    /**
     * Handling timeout event.
     * @param evt_ event object got from the timer port
     */
    protected synchronized void timeout(Object evt_) {

        if (evt_ instanceof MacPhyContract.Message) {
            // _assert("Mac_802_11 timeout", "tx_active_ == false", tx_active_ == false);
            MacPhyContract.Message msg = (MacPhyContract.Message) evt_;
            
            interferencePwr -= msg.getRxPr();
            if (interferencePwr < msg.getCSThresh()/1000)
                interferencePwr = 0.0;
            //System.out.println("timeout: interference ="+ interferencePwr);
            if (rx_state_ == MAC_RECV) {
                if (interferencePwr < msg.getCSThresh() && pktRx_ == null) {
                    rx_state_ = MAC_IDLE;
                    CHECK_BACKOFF_TIMER();  
                }    
            }
            return;
        }
        
        int type_ = ((MacTimeoutEvt)evt_).evt_type;
        
        switch(type_) {
            case MacTimeoutEvt.Nav_timeout:
                handleNavTimeout();
                return;
            case MacTimeoutEvt.IF_timeout:
                handleIfTimeout();
                return;
            case MacTimeoutEvt.Rx_timeout:
                handleRxTimeout();
                return;
            case MacTimeoutEvt.Tx_timeout:
                handleTxTimeout();
                return;
            case MacTimeoutEvt.Defer_timeout:
                handleDeferTimeout();
                return;
            case MacTimeoutEvt.Backoff_timeout:
                handleBackoffTimeout();
                return;
            case MacTimeoutEvt.Beacon_timeout:
                handleBeaconTimeout();
                return;
            case MacTimeoutEvt.TBTT_timeout:
                handleTBTTTimeout();
                return;
            case MacTimeoutEvt.ATIMEnd_timeout:
                handleATIMEndTimeout();
                return;
        }
    }
    
    
    private void _assert(String where, String why, boolean continue_) {
        if ( continue_ == false )
            drcl.Debug.error(where, why, true);
        return;
    }
    
    /** Handles BackoffTimer Timeout Event. */
    protected void handleBackoffTimeout() {  
        bf_timer_.handle();
        
        trace("bf_timer_ timeout", "TIMER");
        
        if (atim_timer_.busy()) {
            if ( pktCTRL_ != null ) {
                _assert("Mac_802_11 handleBackoffTimeout()", "(tx_timer_.busy() || df_timer_.busy()) "+ getTime(), ( tx_timer_.busy() || df_timer_.busy() ) );
                return;
            }
            
            if(!got_beacon_) {
                if (check_pktBEACON() == 0)
                    return;
                return;
            }
            
            atim_scan();
            
            if (check_pktATIM() == 0) {
                if (entryATIM_ != null)
                    entryATIM_.sent_atim_ = true;
					if (entryATIM_.get_addr() == MAC_BROADCAST)
						tx_bcast_atim_++;
                return;
            }
        }
        else {
            // check whether there is some packet waiting to transmit
            if ( pktCTRL_ != null ) {
                _assert("Mac_802_11 handleBackoffTimeout()", "(tx_timer_.busy() || df_timer_.busy())", ( tx_timer_.busy() || df_timer_.busy() ) );
                return;
            }
            
            if ( check_pktRTS() == 0 )  {
                return;
            }
            if ( check_pktTx() == 0 )   {
                return;
            }
            
            tx_scan();
            
            if ( check_pktRTS() == 0 )  {
                return;
            }
            if ( check_pktTx() == 0 )   {
                return;
            }
            else
                has_packet_tx_ = 0;
        }
    }
    
    
    /** Handles DeferTimer Timeout Event. */
    protected void handleDeferTimeout() {    

        df_timer_.handle();
        //trace("df_timer_ timeout", "TIMER");
        
        if (atim_timer_.busy()) {
            if (check_pktCTRL() == 0)
                return;
            
            if (got_beacon_) {
                atim_scan();
                if(check_pktATIM() == 0) {
                    if (entryATIM_ != null)
                        entryATIM_.sent_atim_ = true;
                    return;
                }
            }
        } else {
            if ( check_pktCTRL() == 0 )
                return;
            
            _assert("Mac_802_11 handleDeferTimeout()", "(bf_timer_.busy() == 0)", (bf_timer_.busy() == false) );
            
            if ( check_pktRTS() == 0 )
                return;
            
            if ( check_pktTx() == 0 )
                return;
            
            tx_resume("defer timeout");
        }
    }
    
    /** Handles NavTimer Timeout Event. */
    protected void handleNavTimeout() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleNavTimeout");
        
        nav_timer_.handle();
        trace("nav_timer_ timeout", "TIMER");
        
        if ( is_idle() && bf_timer_.paused() ) {
            bf_timer_.resume(difs_);
        }
    }
    
    /** Handles IfTimer Timeout Event. */
    protected void handleIfTimeout() {    // clear tx_active_ after tx is finished

        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleIFTimeout");

        
        if_timer_.handle();
        trace("if_timer_ timeout", "TIMER");
        tx_active_ = false;   
    }
    
    /** Handles RxTimer Timeout Event. */
    protected void handleRxTimeout() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleRxTimeout");
        
        rx_timer_.handle();
        trace("rx_timer_ timeout", "TIMER");
        recv_timer();         
    }
    
    /** Handles TxTimer Timeout Event. */
    protected void handleTxTimeout() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleTxTimeout");
        
        tx_timer_.handle();
        trace("tx_timer_ timeout", "TIMER");
        send_timer();         
    }
    
    /** Handles BeaconTimer Timeout Event. */
    protected void handleBeaconTimeout() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleBeaconTimeout");
        
        beacon_timer_.handle();
        
        //trace("beacon timer timeout", "TIMER");
        
        sendBeacon();
        if (tx_state_ == MAC_IDLE && !tx_timer_.busy() && !bf_timer_.busy() && !df_timer_.busy())
            bf_timer_.start(phymib_.CWMin*2, is_idle());
    }
    
    /** Handles TBTTTimer Timeout Event. */
    protected void handleTBTTTimeout() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleTBTTTimeout");
        
        
        tbtt_timer_.handle();
        
        trace("tbtt timer timeout", "TIMER");

		if (initial_wake_count > 0)
			initial_wake_count--;
        
        if (atim_timer_.busy())
            atim_timer_.stop();
        
        atim_timer_.start(macmib_.ATIMWindow);
        
        // immediately wake up if not already awake
        if (netif_.if_sleep()) {
            netif_.setSleep(false);
        }
        
        // our next TBTT is exactly one BeaconPeriod away
        double tbtt = macmib_.BeaconPeriod - Math.IEEEremainder(tsf_timer_.getTSF(), macmib_.BeaconPeriod * 1e6) / 1e6;
        
        if (tbtt < macmib_.BeaconPeriod / 3)
            tbtt += macmib_.BeaconPeriod;
        
        _assert("handleTBTTTimeout()", "!tbtt_timer_.busy()", !tbtt_timer_.busy());
        tbtt_timer_.start(tbtt);
        _assert("handleTBTTTimeout()", "!beacon_timer_.busy()", !beacon_timer_.busy());
        beacon_timer_.start(0.0001);
        
        got_beacon_ = false;
        recvd_atim_ = false;
		tx_bcast_atim_ = 0;
        recvd_bcast_atim_ = 0;
        recvd_ucast_atim_ = 0;
        has_packet_tx_ = 0;
        
        BUFFER_ENTRY this_entry;
        
        Enumeration e = psm_buffer.elements();
        
        while(e != null && e.hasMoreElements()) {
            this_entry = (BUFFER_ENTRY)e.nextElement();

            if (psm_buffer.size() > 20 && this_entry.age_ >= 2 && !(this_entry.get_addr() == MAC_BROADCAST && tx_bcast_atim_ > 0)) {
            	psm_buffer.remove(this_entry);

                if(this_entry.packet_ != null){
                    drop(this_entry.packet_, DROP_MAC_BUFFER_FULL);
		            debug(DROP_MAC_BUFFER_FULL + " drop due to PSM" + " < " + this_entry.packet_.toString() + " >");
		        }    

                if (this_entry == entryATIM_) {
                    entryATIM_ = null;
                    this_entry = null;
                }
            }
            else {
                // retry ATIM transmission at the beginning of the ATIM window, but
                // don't reset pwr_mgt_, so we don't send extra ATIMs.
                this_entry.sent_atim_ = this_entry.recvd_ack_ = false;
                this_entry.pwr_mgt_updated_ = false;
                this_entry.age_++;
            }
        }
    }
    
    /*
       End of ATIM window,
       1. check if there are data to send, if so backoff
       2. check if there will be pending unicast/broadcast data to recieve, if not, switch to power saving state
     */
    
   /** Handles ATIM END Event. */
    protected void handleATIMEndTimeout() {

        // release_1208    
        if ( isDebugEnabled() == true )
            debug("handleATIMEndTimeout");
        
        atim_timer_.handle();
        
 		atimrc = 0;       
		if (pktBeacon_ != null) 
            pktBeacon_ = null;
        
        
        if(pktATIM_ != null) {
            entryATIM_ = null;
            pktATIM_ = null;
        }
        
        double atimPendingCorrection = 0.0;
        
        if (tx_state_ == MAC_ATIM)
            atimPendingCorrection = Math.max(tx_timer_.expire(), if_timer_.expire() + 0.000001);
        
        if (tx_scan() || (pktTx_ != null)) {
            if (!tx_timer_.busy() && !bf_timer_.busy() && !df_timer_.busy())
                df_timer_.start(atimPendingCorrection);
        } else if (!recvd_atim_ && has_packet_tx_ == 0 && initial_wake_count == 0)
            goto_sleep();
    }
    
    /* =======================================================
       The "real" Timer Handler Routine
       ======================================================= */
    private void send_timer() {
        
        // release_1208    
        //if ( isDebugEnabled() == true )   debug("send_timer");
        
        switch(tx_state_) {
            case MAC_RTS:  //Sent a RTS, but did not receive a CTS.
                //trace("RetransmitRTS()", "EVENT");
                RetransmitRTS();
                break;
            case MAC_CTS:  // Sent a CTS, but did not receive a DATA packet.
                _assert("Mac_802_11 send_timer()", "case MAC_CTS: (pktCTRL_ != null)", (pktCTRL_ != null));
                pktCTRL_ = null;
                break;
            case MAC_SEND: // Sent DATA, but did not receive an ACK packet.
                //trace("RetransmitDATA()", "EVENT");
                RetransmitDATA();
                break;
            case MAC_ACK:  // Sent an ACK, and now ready to resume transmission.
                _assert("Mac_802_11 send_timer()", "case MAC_ACK: (pktCTRL_ != null)", (pktCTRL_ != null));
                pktCTRL_ = null;
                break;
            case MAC_BEACON:
                if (pktBeacon_ != null) {
                    pktBeacon_ = null;
                }
                break;
                
            case MAC_ATIM:
                RetransmitATIM();
                break;
                
            case MAC_IDLE:
                break;
            default:
                _assert("Mac_802_11 send_timer()", "should not reach here", false);
        }
        tx_resume("send timer: "+tx_state_);
    }
    
    private void CHECK_BACKOFF_TIMER() {
        if ( is_idle() && bf_timer_.paused() ) {
            bf_timer_.resume(difs_);
        }
        if ( !is_idle() && bf_timer_.busy() && !bf_timer_.paused() ) {
            bf_timer_.pause();
        }
    }
    
    private void TRANSMIT(Mac_802_11_Packet p, double t)  {
        
        // release_1208    
        //if ( isDebugEnabled() == true )  debug("TRANSMIT");
        
        
        tx_active_ = true;
        /*
         * If I'm transmitting without doing CS, such as when
         * sending an ACK, any incoming packet will be "missed"
         * and hence, must be discarded.
         */
        if ( rx_state_ != MAC_IDLE && pktRx_ != null) {
            
            _assert("Mac_802_11 TRANSMIT()", "(p instanceof Mac_802_11_ACK_Frame) && pktRx_ != null)",
            ( p instanceof Mac_802_11_ACK_Frame) && pktRx_ != null );
            
            pktRx_.setForcedError(true);     /* force packet discard */
        }
        
        /*
         * pass the packet on the "interface" which will in turn
         * place the packet on the channel.
         *
         */
        
        /* in case of beacon, record the tsf timer */
        if (p.getFc().get_fc_type() == Mac_802_11_Frame_Control.MAC_Type_Management &&
            p.getFc().get_fc_subtype() == Mac_802_11_Frame_Control.MAC_Subtype_Beacon) {
            ((Mac_802_11_Beacon_Frame)p).setTSF(tsf_timer_.getTSF());
        }
        
        if_timer_.start(TX_Time(p));
        tx_timer_.start(t);             // if this timer expires, check why no response is received
        downPort.doSending(p);           

		if ((p.getFc().get_fc_type() == Mac_802_11_Frame_Control.MAC_Type_Management &&
		     p.getFc().get_fc_subtype() == Mac_802_11_Frame_Control.MAC_Subtype_ATIM) ||
		    (p.getFc().get_fc_type() == Mac_802_11_Frame_Control.MAC_Type_Control &&
		     p.getFc().get_fc_subtype() == Mac_802_11_Frame_Control.MAC_Subtype_ACK)) {
		    trace("TRANSMIT  <" + p.toString() + ">", "PACKET");
		}
        
        
    }
    
    private void SET_RX_STATE(int x) {
        rx_state_ = x;
        CHECK_BACKOFF_TIMER();
    }
    
    private void SET_TX_STATE(int x) {
        tx_state_ = x;
        CHECK_BACKOFF_TIMER();
    }
    
    
    /* ======================================================================
       Misc Routines
       ====================================================================== */
    private boolean is_idle() {
        if (rx_state_ != MAC_IDLE)
            return false;
        if (tx_state_ != MAC_IDLE)
            return false;
        if (nav_ > this.getTime())
            return false;
        return true;
    }
    
    
    /* need to explicitly release p from outside out this function
     * therefore after calling this method, need to explicitly "discard" 
     * the packet if necessary
     */
    private void discard(Mac_802_11_Packet p, String why) {
        
        /* if the rcvd pkt contains errors, a real MAC layer couldn't
           necessarily read any data from it, so we just toss it now */
        if ( p.isForcedError() ) {
            return;
        }
        
        switch ( p.getFc().get_fc_type() ) {
            case Mac_802_11_Frame_Control.MAC_Type_Management:
			    switch( p.getFc().get_fc_subtype() ) {
			    case Mac_802_11_Frame_Control.MAC_Subtype_Beacon:                
					trace(why + " drop Beacon frame" + " < " + ((Mac_802_11_Beacon_Frame)p).toString() + " >", "PACKET");
					break;
	    		case Mac_802_11_Frame_Control.MAC_Subtype_ATIM:                
					trace(why + " drop ATIM frame" + " < " + ((Mac_802_11_ATIM_Frame)p).toString() + " >", "PACKET");
					break;
	    		default:
					break;
	    		}
                return;
            case Mac_802_11_Frame_Control.MAC_Type_Control:
                switch( p.getFc().get_fc_subtype() ) {
                    case Mac_802_11_Frame_Control.MAC_Subtype_RTS:
                        if ( ((Mac_802_11_RTS_Frame)p).getTa() == this.macaddr_ ||
                        ((Mac_802_11_RTS_Frame)p).getRa() == this.macaddr_ ) {
                            trace(why + " drop RTS frame" + " < " + ((Mac_802_11_RTS_Frame)p).toString() + " >", "PACKET");
                            return;
                        }
                        break;
                    case Mac_802_11_Frame_Control.MAC_Subtype_CTS:
                        if ( ((Mac_802_11_CTS_Frame)p).getRa() == this.macaddr_ ) {
                            trace(why + " drop CTS frame" + " < " + ((Mac_802_11_CTS_Frame)p).toString() + " >", "PACKET");
                            return;
                        }
                        break;
                    case Mac_802_11_Frame_Control.MAC_Subtype_ACK:
                        if ( ((Mac_802_11_ACK_Frame)p).getRa() == this.macaddr_ ) {
                            trace(why + " drop ACK frame" + " < " + ((Mac_802_11_ACK_Frame)p).toString() + " >", "PACKET");
                            return;
                        }
                        break;
                    default:
                        drcl.Debug.error("Mac_802_11 discard()", "invalid MAC Control subtype", true);
                }
                break;
            case Mac_802_11_Frame_Control.MAC_Type_Data:
                switch( p.getFc().get_fc_subtype() ) {
                    case Mac_802_11_Frame_Control.MAC_Subtype_Data:
                        if ( ((Mac_802_11_Data_Frame)p).getDa() == this.macaddr_ ||
                        ((Mac_802_11_Data_Frame)p).getSa() == this.macaddr_ ||
                        ((Mac_802_11_Data_Frame)p).getDa() == this.MAC_BROADCAST ) {
                            trace(why + " drop DATA frame" + " < " + ((Mac_802_11_Data_Frame)p).toString() + " >", "PACKET");
                            return;
                        }
                        break;
                    default:
                        drcl.Debug.error("Mac_802_11", "invalid MAC Data subtype", true);
                }
                break;
            default:
                drcl.Debug.error("Mac_802_11", "invalid MAC type", true);
        }
        // p = null;   // don't forget to release this packet after calling this method
        return;
    }
    
    private void capture(Mac_802_11_Packet p) {  // remember to release p after calling this method if necessary
        /*
         * Update the NAV so that this does not screw
         * up carrier sense.
         */
        set_nav(usec(eifs_ + TX_Time(p)));
        //p = null;
    }
    
    /*      need to explicitly release p from outside out this function
     *      set p as null won't release that packet
     */
    void collision(Mac_802_11_Packet p) {
        
        // release_1208    
        //if ( isDebugEnabled() == true )  debug("collision");

        switch(rx_state_) {
            case MAC_RECV:
                SET_RX_STATE(MAC_COLL);
                /* fall through */
            case MAC_COLL:               
                _assert("Mac_802_11 collision()", "(pktRx_ != null)", (pktRx_ != null));
                _assert("Mac_802_11 collision()", "rx_timer_.busy()", rx_timer_.busy());
                
                /*
                 *  Since a collision has occurred, figure out
                 *  which packet that caused the collision will
                 *  "last" the longest.  Make this packet,
                 *  pktRx_ and reset the Recv Timer if necessary.
                 */
                if (TX_Time(p) > rx_timer_.expire()) {
                    rx_timer_.stop();
                    discard(pktRx_, DROP_MAC_COLLISION);   // pktRx_ is set to p, so no need to explicitly "free" pktRx_
                    pktRx_ = p;
                    rx_timer_.start(TX_Time(pktRx_));
                }
                else {
                    discard(p, DROP_MAC_COLLISION);
                    // p = null;
                }
                break;
            default:
                _assert("mAC_802_11 collision()", "invalid rx_state", false);
        }
    }
    
    private synchronized void tx_resume(String caller_) {

        _assert("Mac_802_11 tx_resume()", "tx_timer_.busy() == false", (tx_timer_.busy() == false));
        
        if (atim_timer_.busy() && got_beacon_) {//inside ATIM window and got a beacon
            boolean active = false;
            
            if (pktCTRL_ != null) {
                if (!df_timer_.busy())
                    df_timer_.start(sifs_);
                active = true;
            }
            
            if (pktTx_ != null)
                active = true;
            
            if (!active) {
                atim_scan();
                if (pktBeacon_ != null || pktATIM_ != null) {
                    if (!bf_timer_.busy()) {
                        if (!df_timer_.busy())
                            df_timer_.start(difs_);
                    }
                } else {
                    _assert(" tx_resume:0", "node"+macaddr_+" "+pktCTRL_ + "|"+pktRTS_+"|"+"pktATIM_"+"|"+pktTx_+"|"+pktBeacon_, ((pktCTRL_ == null) && (pktRTS_ == null) && (pktBeacon_ == null) && (pktATIM_ == null)) && (pktTx_ == null));
                    upPort.doSending(null);
                }
            }
        } else if (atim_timer_.busy()){ //inside ATIM window, no beacon yet
            if (pktCTRL_ != null) {
                if (!df_timer_.busy())
                    df_timer_.start(sifs_);
            }
        } else {
            boolean active = false;
            
            if ( pktCTRL_ != null ) {
               /*
                *  Need to send a CTS or ACK.
                */
                if(!df_timer_.busy())
                    df_timer_.start(sifs_);
                active = true;
            } else if ( pktRTS_ != null ) {
                
                if ( !bf_timer_.busy())
                    if(!df_timer_.busy())
                        df_timer_.start(difs_);
                
                active = true;
            } else if ( pktTx_ != null ) {
                if ( !bf_timer_.busy()) {
                    if(!df_timer_.busy())
                        df_timer_.start(difs_);
                }
                active = true;
            }
            
            if (psm_enabled_ && !active) {
                tx_scan();
                
                if (pktTx_ != null || pktRTS_ != null) {
                    if (!bf_timer_.busy()) {
                        if (is_idle()) {
                            /* If we are already deferring, there is no need to reset the Defer timer. */
                            if (!df_timer_.busy())
                                df_timer_.start(difs_);
                        }
                            /* If the medium is NOT IDLE, then we start the backoff timer. */
                        else {
                            if(bf_timer_.busy())
                                bf_timer_.stop();
                            bf_timer_.start(cw_, is_idle());
                        }
                    }
                }
                else { //if no packet to transmit, go to sleep conditionally
                    if (recvd_bcast_atim_ == 0 && recvd_ucast_atim_ == 0 && psm_mode_ == PSM_PWR_SAVE && tx_bcast_atim_ == 0 && initial_wake_count == 0) {
                        goto_sleep();
                    }    
                    else {
                        _assert(" tx_resume:1", "node"+macaddr_+" "+pktCTRL_ + "|"+pktRTS_+"|"+pktATIM_+"|"+pktTx_+"|"+pktBeacon_, ((pktCTRL_ == null) && (pktRTS_ == null) && (pktBeacon_ == null) && (pktATIM_ == null)) && (pktTx_ == null));
                        upPort.doSending(null);
                    }
                }
            }
            else if(!active) {
                _assert(" tx_resume:2", "node"+macaddr_+" "+pktCTRL_ + "|"+pktRTS_+"|"+pktATIM_+"|"+pktTx_+"|"+pktBeacon_, ((pktCTRL_ == null) && (pktRTS_ == null) && (pktBeacon_ == null) && (pktATIM_ == null)) && (pktTx_ == null));
                upPort.doSending(null);  // asking uplayer for next packet, refer to ActiveQueue.java
                                
                
            }
            else {
            }
            
            // according to the activequeue contract,
            // sending an null back means asking for new packets
        }
        
        SET_TX_STATE(MAC_IDLE);
    }
    
    private synchronized void rx_resume() {
        // release_1208    
        //if ( isDebugEnabled() == true )  debug("rx_resume");

        _assert("Mac_802_11 tx_resume()", "pktRx_ == null", (pktRx_ == null));
        _assert("Mac_802_11 tx_resume()", "rx_timer_.busy() == false", (rx_timer_.busy() == false));
        SET_RX_STATE(MAC_IDLE);
    }
    
    private boolean tx_scan() {
        if (pktTx_ != null)
            return false;
        
        boolean remove = false;
        BUFFER_ENTRY this_entry = null;
        
        // try to find packet with an ACK, or broadcast packets
        if (!remove) {
            Enumeration e = psm_buffer.elements();
            
            while(e != null && e.hasMoreElements()) {
                this_entry = (BUFFER_ENTRY)e.nextElement();
                
                long dst = this_entry.get_addr();
                
                if (this_entry.packet_ == null ||
                (!this_entry.recvd_ack_ && dst != MAC_BROADCAST) ||
                (dst == MAC_BROADCAST && !this_entry.sent_atim_))
                    continue;
                
                sendDATA(this_entry.packet_, dst);
                sendRTS(dst);
                
                psm_buffer.remove(this_entry);
                remove = true;
                break;
            }
        }
        
        if (remove) {
            if (this_entry == entryATIM_) entryATIM_ = null;
            this_entry = null;
            return true;
        }
        return false;
    }
    
    
    private void goto_sleep() {
        if (psm_buffer.size() < 5)
            netif_.setSleep(true);
    }
    
    /* ======================================================================
           Outgoing Packet Routines
       ====================================================================== */
    private int check_pktCTRL() {
        Mac_802_11_Packet pktctrl_ = null;
        
        double timeout = 0;
        Mac_802_11_Frame_Control fc_;
        
        if (pktCTRL_ == null)
            return -1;
        if (tx_state_ == MAC_CTS || tx_state_ == MAC_ACK)
            return -1;
        
        fc_ = pktCTRL_.getFc();
        
        switch(fc_.get_fc_subtype()) {
           /*
            *  If the medium is not IDLE, don't send the CTS.
            */
            case Mac_802_11_Frame_Control.MAC_Subtype_CTS:
                if(!is_idle()) {
                    discard(pktCTRL_, DROP_MAC_BUSY);
                    pktCTRL_ = null;   // don't expect discard to release the packet
                    return 0;
                }
                SET_TX_STATE(MAC_CTS);
                timeout = ((Mac_802_11_CTS_Frame)pktCTRL_).getDuration() * 1e-6 + CTS_Time;
                pktctrl_ = (Mac_802_11_Packet) ((Mac_802_11_CTS_Frame) pktCTRL_).clone();
                trace("TRANSMIT  CTS Packet" + " < " + ((Mac_802_11_CTS_Frame) pktctrl_).toString() + " > ", "PACKET");
                break;

           /*
            * IEEE 802.11 specs, section 9.2.8
            * Acknowledments are sent after an SIFS, without regard to
            * the busy/idle state of the medium.
            */
            case Mac_802_11_Frame_Control.MAC_Subtype_ACK:
                SET_TX_STATE(MAC_ACK);
                timeout = ACK_Time;
                pktctrl_ = (Mac_802_11_Packet) ((Mac_802_11_ACK_Frame) pktCTRL_).clone();
                trace("TRANSMIT  ACK Packet" + " < " + ((Mac_802_11_ACK_Frame) pktctrl_).toString() + " > ", "PACKET");
                break;
            default:
                drcl.Debug.error("Mac_802_11 check_pktCTRL()", "Invalid MAC Control subtype", true);
        }
        TRANSMIT(pktctrl_, timeout);   // send the duplicated frame
        return 0;
    }
    
    private int check_pktRTS() {
        
        Mac_802_11_Packet pktrts_ = null;
        
        double timeout = 0.0;
        
        Mac_802_11_Frame_Control fc_;
        
        _assert("Mac_802_11 check_pktRTS()", "bf_timer_.busy() == false", (bf_timer_.busy() == false));
        
        if (pktRTS_ == null)
            return -1;
        
        fc_ = pktRTS_.getFc();
        switch(fc_.get_fc_subtype()) {
            case Mac_802_11_Frame_Control.MAC_Subtype_RTS:
                if(! is_idle()) {
                    inc_cw();
                    bf_timer_.start(cw_, is_idle());
                    return 0;
                }
                SET_TX_STATE(MAC_RTS);
                timeout = CTSTimeout;
                pktrts_ = (Mac_802_11_Packet) ((Mac_802_11_RTS_Frame) pktRTS_).clone();
                trace("TRANSMIT  RTS Packet" + " < " + ((Mac_802_11_RTS_Frame) pktrts_).toString() + " > ", "PACKET");
                break;
            default:
                drcl.Debug.error("Mac_802_11 check_pktRTS()", "Invalid MAC Control subtype", true);
        }
        // TRANSMIT(pktRTS_, timeout);
        TRANSMIT(pktrts_, timeout);   // send the duplicated frame
        return 0;
    }
    
    private int check_pktTx() {
        
        Mac_802_11_Packet pkttx_ = null;
        
        double timeout = 0.0;
        Mac_802_11_Frame_Control fc_;
        
        _assert("Mac_802_11 check_pktTx()", "bf_timer_.busy() == false", (bf_timer_.busy() == false));
        
        if (pktTx_ == null)
            return -1;
        
        fc_ = pktTx_.getFc();
        int len = pktTx_.size;
        
        switch(fc_.get_fc_subtype()) {
            case Mac_802_11_Frame_Control.MAC_Subtype_Data:
                if(! is_idle()) {
                    sendRTS(((Mac_802_11_Data_Frame)pktTx_).getDa());
                    inc_cw();
                    bf_timer_.start(cw_, is_idle());
                    return 0;
                }
                pkttx_ = (Mac_802_11_Packet) ((Mac_802_11_Data_Frame) pktTx_).clone();
                SET_TX_STATE(MAC_SEND);
                if (((Mac_802_11_Data_Frame)pktTx_).getDa() != MAC_BROADCAST )
                    timeout = ACKTimeout(len);
                else
                    timeout = TX_Time(pktTx_);
                trace("TRANSMIT  DATA Frame" + " < " + ((Mac_802_11_Data_Frame) pkttx_).toString() + " > ", "PACKET");
                break;
            default:
                drcl.Debug.error("Mac_802_11 check_pktTx()", "Invalid MAC Control subtype", true);
        }
        TRANSMIT(pkttx_, timeout);   // send the duplicated frame
        return 0;
    }
    
    private int check_pktBEACON() {
                
        Mac_802_11_Packet pktbeacon_;
        
        if (pktBeacon_ == null)
            return -1;
        
        if (tx_state_ != MAC_IDLE)
            return -1;
        
        
        pktbeacon_ = (Mac_802_11_Packet)(pktBeacon_.clone());
        SET_TX_STATE(MAC_BEACON);
        
        got_beacon_ = true;
        
        TRANSMIT(pktbeacon_, TX_Time(pktbeacon_));
        return 0;
    }
    
    private int check_pktATIM() {
        Mac_802_11_Packet pktatim_;
        
        _assert("check_pktATIM()", "!bf_timer_.busy()", !bf_timer_.busy());
        
        if (tx_state_ != MAC_IDLE)
            return -1;
        
        if (pktATIM_ == null)
            return -1;
        
        if (entryATIM_ == null) {
            pktATIM_ = null;
            return -1;
        }
        
        if (!is_idle()) {
            if (!bf_timer_.busy()) {
                inc_cw();
                bf_timer_.start(cw_, is_idle());
            }
            return -1;
        } else {
            SET_TX_STATE(MAC_ATIM);
            
            double timeout;
            if (pktATIM_.getDa() != MAC_BROADCAST)
                timeout = ACKTimeout(pktATIM_.size);
            else
                timeout = TX_Time(pktATIM_);
            
            // store the address of the STA we last transmitted an ATIM to
            last_atim_da_ = pktATIM_.getDa();
            pktatim_ =(Mac_802_11_Packet) pktATIM_.clone();
            
            trace("TRANSMIT  ATIM Packet" + " < " + ((Mac_802_11_ATIM_Frame) pktatim_).toString() + " > ", "PACKET");
            
            TRANSMIT(pktatim_, timeout);
        }
        return 0;
    }
    
    /*
     * Low-level transmit functions that actually place the packet onto
     * the channel.
     */
    private void sendRTS(long dst) {
        
        Mac_802_11_RTS_Frame rf;
        Mac_802_11_Frame_Control fc;
        
        _assert("Mac_802_11 sendRTS()", "pktTx_ != null", (pktTx_ != null));
        _assert("Mac_802_11 sendRTS()", "pktRTS_ == null", (pktRTS_ == null));
        
        /*
         *  If the size of the packet is larger than the
         *  RTSThreshold, then perform the RTS/CTS exchange.
         *
         *  also skip if destination is a broadcast
         */
        if( pktTx_.size < macmib_.RTSThreshold || dst == this.MAC_BROADCAST ) {
            return;
        }
        
        fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_RTS,
        Mac_802_11_Frame_Control.MAC_Type_Control,
        Mac_802_11_Frame_Control.MAC_ProtocolVersion);
        
        fc.set_fc_flags(false, false, false, false, false, false, false, false);
        
        rf = new Mac_802_11_RTS_Frame(ETHER_RTS_LEN, fc, this.RTS_DURATION(pktTx_), dst, macaddr_, 0, false);
        pktRTS_ = rf;
    }
    
    private void sendCTS(long dst, int rts_duration)	{
        
        Mac_802_11_CTS_Frame cf;
        Mac_802_11_Frame_Control fc;
        
        _assert("Mac_802_11 sendCTS()", "pktCTRL_ == null", (pktCTRL_ == null));
        
        fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_CTS,
        Mac_802_11_Frame_Control.MAC_Type_Control,
        Mac_802_11_Frame_Control.MAC_ProtocolVersion);
        
        fc.set_fc_flags(false, false, false, false, false, false, false, false);
        
        cf = new Mac_802_11_CTS_Frame(ETHER_CTS_LEN, fc, CTS_DURATION(rts_duration), dst, 0, false);
        pktCTRL_ = cf;
    }
    
    private void sendACK(long dst) {
        Mac_802_11_ACK_Frame af;
        Mac_802_11_Frame_Control fc;
        
        _assert("Mac_802_11 sendACK()", "pktCTRL_ == null", (pktCTRL_ == null));
        fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_ACK,
        Mac_802_11_Frame_Control.MAC_Type_Control,
        Mac_802_11_Frame_Control.MAC_ProtocolVersion);
        
        
        fc.set_fc_flags(false, false, false, false, false, false, false, false);
        
        af = new Mac_802_11_ACK_Frame(ETHER_ACK_LEN, fc, this.ACK_DURATION(), dst, 0, false);
        pktCTRL_ = af;
        trace("sendACK()" + pktCTRL_.toString(), "EVENT");
    }
    
    private void sendDATA(Packet p, long dst) {  
        
        Mac_802_11_Data_Frame df;
        Mac_802_11_Frame_Control fc;

        _assert("Mac_802_11 addr:" + this.macaddr_ + "  sendDATA() "+getTime() + " "+ pktTx_, "pktTx_ == null", (pktTx_ == null));
        
        fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_Data,
        Mac_802_11_Frame_Control.MAC_Type_Data,
        Mac_802_11_Frame_Control.MAC_ProtocolVersion);
        
        
        fc.set_fc_flags(false, false, false, false, false, false, false, false);
        
        if ( dst != this.MAC_BROADCAST )
            df = new Mac_802_11_Data_Frame(fc, DATA_DURATION(), dst, macaddr_, 0, 0, false, ETHER_HDR_LEN, p.size, p);
        else
            df = new Mac_802_11_Data_Frame(fc, 0, dst, macaddr_, 0, 0, false, ETHER_HDR_LEN, p.size, p);
        pktTx_ = df;
        
        ((Mac_802_11_Data_Frame) pktTx_).scontrol = sta_seqno_;
        sta_seqno_ = sta_seqno_ + 1;
    }
    
    private void sendBeacon() { 
        Mac_802_11_Beacon_Frame bf;
        Mac_802_11_Frame_Control fc;
        
        _assert("Mac_802_11 sendBeacon()", "pktBeacon__ == null", (pktBeacon_ == null));
        
        fc = new Mac_802_11_Frame_Control(Mac_802_11_Frame_Control.MAC_Subtype_Beacon,
        Mac_802_11_Frame_Control.MAC_Type_Management,
        Mac_802_11_Frame_Control.MAC_ProtocolVersion);
        
        fc.set_fc_flags(false, false, false, false, false, false, false, false);
        
        //bh = new Mac_802_11_Beacon_Header(macmib_.BeaconPeriod*1e3, macmib_.ATIMWindow*1e3);
        //Rong: skip specification of atim window size, beacon period in the beacon message
        //I don't really have a beacon body
        bf = new Mac_802_11_Beacon_Frame(fc, 0, macaddr_, 0, ETHER_BEACON_LEN, 0);
        
        pktBeacon_ = bf;
    }
    
    /* ======================================================================
           Retransmission Routines
       ====================================================================== */
    private void RetransmitRTS() {
        _assert("Mac_802_11 RetransmitRTS()", "pktTx_ != null", (pktTx_ != null));
        _assert("Mac_802_11 RetransmitRTS()", "pktRTS_ != null", (pktRTS_ != null));
        
		if (bf_timer_.busy() == true) 
			debug("\n" + pktBeacon_ + pktATIM_ + "\n");
			
        _assert("Mac_802_11 RetransmitRTS()", "bf_timer_.busy() == false", (bf_timer_.busy() == false));
        
        macmib_.RTSFailureCount++;
        
        ssrc_ = ssrc_ + 1;			// STA Short Retry Count
        
        if (ssrc_ >= macmib_.ShortRetryLimit) {
            discard(pktRTS_, DROP_MAC_RETRY_COUNT_EXCEEDED);
            pktRTS_ = null;    // have to explicitly set pktRTS_ to null because discard() can not "free" it

            /* notify ad-hoc routing that the send operation failed. */
            if(brokenPort._isEventExportEnabled())
                brokenPort.exportEvent(EVENT_LINK_BROKEN, ((Packet) pktTx_.getBody()).clone(), null);
            
            discard(pktTx_, DROP_MAC_RETRY_COUNT_EXCEEDED);
            pktTx_ = null;  // have to explicitly set pktTx_ to null because discard() can not "free" it
            ssrc_ = 0;
            rst_cw();
        } else {
            ((Mac_802_11_RTS_Frame)pktRTS_).fc.set_retry(true);
            inc_cw();
            bf_timer_.start(cw_, is_idle());
        }
    }
    
    private void RetransmitDATA() {
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("RetransmitDATA");

        int rcount, thresh;
        
        Mac_802_11_Data_Frame df;
        Mac_802_11_Frame_Control fc;
        
        _assert("Mac_802_11 RetransmitDATA()", "bf_timer_.busy() == false", (bf_timer_.busy() == false));
        _assert("Mac_802_11 RetransmitDATA()", "pktTx_ != null", (pktTx_ != null));
        _assert("Mac_802_11 RetransmitDATA()", "pktRTS_ == null", (pktRTS_ == null));
        
        /*
         *  Broadcast packets don't get ACKed and therefore
         *  are never retransmitted.
         */
        
        fc = ((Mac_802_11_Data_Frame)pktTx_).getFc();
        
        if (((Mac_802_11_Data_Frame)pktTx_).getDa() == MAC_BROADCAST) {
            pktTx_ = null;
            
            /*
             * Backoff at end of TX.
             */
            rst_cw();
            bf_timer_.start(cw_, is_idle());
            
            return;
        }
        
        macmib_.ACKFailureCount++;
        
        boolean useShortCount;

        if(pktTx_.size <= macmib_.RTSThreshold) {
            ssrc_ ++;
            rcount = ssrc_;
            useShortCount = true;
            thresh = macmib_.ShortRetryLimit;
        }
        else {
            slrc_++;
            rcount = slrc_;
            useShortCount = false;
            thresh = macmib_.LongRetryLimit;
        }
        
        if(rcount > thresh) {
            macmib_.FailedCount++;
            
            if(brokenPort._isEventExportEnabled())
                brokenPort.exportEvent(EVENT_LINK_BROKEN, ((Packet) pktTx_.getBody()).clone(), null);
            discard(pktTx_, DROP_MAC_RETRY_COUNT_EXCEEDED);
            pktTx_ = null; // have to explicitly set pktTx_ to null because discard() can not "free" it
            rcount = 0;
            if ( useShortCount == true )
                ssrc_ = 0;
            else
                slrc_ = 0;
            
            rst_cw();
        }
        else {
            ((Mac_802_11_Data_Frame)pktTx_).fc.set_retry(true);
            sendRTS(((Mac_802_11_Data_Frame)pktTx_).getDa());
            inc_cw();
            bf_timer_.start(cw_, is_idle());
        }
    }
    
    private void RetransmitATIM() {
        _assert("RetransmitATIM()", "!bf_timer_.busy()", !bf_timer_.busy());
        
        if (!atim_timer_.busy() || pktATIM_.getDa() == MAC_BROADCAST) {
            pktATIM_ = null;
            //rst_cw();
            //bf_timer_.start(cw_, is_idle());
            return;
        }

        _assert("RetransmitATIM()", "pktATIM != 0", pktATIM_ != null);
        long dst = pktATIM_.getDa();
        // look to see if this is still necessary
        BUFFER_ENTRY this_entry;
        Enumeration e = psm_buffer.elements();;
        while(e != null && e.hasMoreElements()) {
            this_entry = (BUFFER_ENTRY)e.nextElement();
            
            if (this_entry.get_addr() == dst && this_entry.recvd_ack_) {
                pktATIM_ = null;
                rst_cw();
                bf_timer_.start(cw_, is_idle());
                return;
            }
        }
        
        macmib_.ACKFailureCount++;
        atimrc++;
        if (atimrc > macmib_.MAC_ATIMRetryLimit) {
			Packet removePkt_ = null;

            macmib_.FailedCount++;
            discard(pktATIM_, DROP_MAC_ATIM_RETRY_COUNT);
            pktATIM_ = null;
            atimrc = 0;
            rst_cw();
            
            while(e != null && e.hasMoreElements()) {
                this_entry = (BUFFER_ENTRY)e.nextElement();
                
                if (this_entry.get_addr() == dst) {
                    psm_buffer.remove(this_entry);
                    //XXX: call back?

					removePkt_ = this_entry.packet_;
                    
                    drop(this_entry.packet_, DROP_MAC_RETRY_COUNT_EXCEEDED);
                    if (this_entry == entryATIM_)
                        entryATIM_ = null;
                    this_entry = null;
	            }
	            
               	if(removePkt_ != null && brokenPort._isEventExportEnabled())
                	brokenPort.exportEvent(EVENT_LINK_BROKEN, removePkt_.clone(), null);

            }
        } else {
            inc_cw();
            bf_timer_.start(cw_, is_idle());
        }
    }
    
    protected void recv_timer() {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("recv_timer");
        
        long   src = 0, dst = 0;
        Mac_802_11_Frame_Control fc;
        
        fc = pktRx_.getFc();
        
        int type = fc.get_fc_type();
        int subtype = fc.get_fc_subtype();
        
        _assert("Mac_802_11 RetransmitDATA()", "pktRx_ != null", (pktRx_ != null));
        _assert("Mac_802_11 RetransmitDATA()", "rx_state_ == MAC_RECV || rx_state_ == MAC_COLL", (rx_state_ == MAC_RECV || rx_state_ == MAC_COLL));
        
        /*
         *  If the interface is in TRANSMIT mode when this packet
         *  "arrives", then I would never have seen it and should
         *  do a silent discard without adjusting the NAV.
         */
        if ( tx_active_ ) {
            //trace("recv_timer timeout, i am transmitting, discard pktRx_  <" + pktRx_.toString() + ">", "EVENT");
            pktRx_ = null;
            rx_resume();
            return;
        }
        
        /*
         * Handle collisions.
         */
        if ( rx_state_ == MAC_COLL ) {
            discard(pktRx_, DROP_MAC_COLLISION);
            set_nav(usec(eifs_));
            pktRx_ = null;  // "free" pktRx_
            rx_resume();
            return;
        }
        
        /*
         * Check to see if this packet was received with enough
         * bit errors that the current level of FEC still could not
         * fix all of the problems - ie; after FEC, the checksum still
         * failed.
         */
        if( pktRx_.forcedError == true ) {
            trace("recv_timer timeout, forced error, discard pktRx_  <" + pktRx_.toString() + ">", "EVENT");
            set_nav(usec(eifs_));
            pktRx_ = null;
            rx_resume();
            return;
        }
        
        /*
         * IEEE 802.11 specs, section 9.2.5.6
         *	- update the NAV (Network Allocation Vector)
         */
        if ( pktRx_ instanceof Mac_802_11_RTS_Frame ) {
            src = ((Mac_802_11_RTS_Frame)pktRx_).getTa();
            dst = ((Mac_802_11_RTS_Frame)pktRx_).getRa();
        }
        else if ( pktRx_ instanceof Mac_802_11_CTS_Frame ) {
            dst = ((Mac_802_11_CTS_Frame)pktRx_).getRa();
            if (pktRTS_ != null)
                src = ((Mac_802_11_RTS_Frame)pktRTS_).getTa();
        }
        else if ( pktRx_ instanceof Mac_802_11_ACK_Frame ) {
            dst = ((Mac_802_11_ACK_Frame)pktRx_).getRa();
            if (pktTx_ != null)
                src = ((Mac_802_11_Data_Frame)pktTx_).getSa();
        }
        else if ( pktRx_ instanceof Mac_802_11_Data_Frame ) {
            dst = ((Mac_802_11_Data_Frame)pktRx_).getDa();
            src = ((Mac_802_11_Data_Frame)pktRx_).getSa();
        }
        else {
            if (psm_enabled_) {
                if ( pktRx_ instanceof Mac_802_11_Beacon_Frame) {
                    src = ((Mac_802_11_Beacon_Frame)pktRx_).getSa();
                    dst = MAC_BROADCAST;
                }
                else if (pktRx_.getFc().get_fc_subtype() == Mac_802_11_Frame_Control.MAC_Subtype_ATIM) {
                    src = ((Mac_802_11_ATIM_Frame)pktRx_).getSa();
                    dst = ((Mac_802_11_ATIM_Frame)pktRx_).getDa();
                }
            } else {
                dst = 0;
                _assert("Mac_802_11 recv_timer()", "incorrect packet type", false);   // should never reach here
            }
        }
        
        if ( dst != macaddr_ ) {
            set_nav(pktRx_.getDuration());
        }
        
        if (psm_enabled_) {
            Enumeration e = psm_buffer.elements();
            while(e != null && e.hasMoreElements()) {
                BUFFER_ENTRY this_entry = (BUFFER_ENTRY)e.nextElement();
                if (src == this_entry.get_addr()) {
                    this_entry.pwr_mgt_ = ((((Mac_802_11_Packet)pktRx_).getFc()).get_pwr_mgt(true) == true);
                }
            }
        }
        
        /*
         * Address Filtering
         */
        if(dst != macaddr_ && dst != MAC_BROADCAST) {
            /*
             *  We don't want to log this event, so we just free
             *  the packet instead of calling the drop routine.
             */
            trace("recv_timer timeout, , dst != macaddr_ && dst != MAC_BROADCAST, discard pktRx_  <" + pktRx_.toString() + ">", "EVENT");
            discard(pktRx_, "---");
            pktRx_ = null;
            rx_resume();
            return;
        }
        
        switch(type) {
            case Mac_802_11_Frame_Control.MAC_Type_Management:
                if (!psm_enabled_) {
                    discard(pktRx_, this.DROP_MAC_PACKET_ERROR);
                    // goto done;
                    
                    pktRx_ = null;
                    rx_resume();

               		debug(macaddr_+" PSM disabled\n");

                    return;
                }
                else {
                    switch(subtype) {
                        case Mac_802_11_Frame_Control.MAC_Subtype_Beacon:
                            recvBeacon(pktRx_);
                            break;
                        case Mac_802_11_Frame_Control.MAC_Subtype_ATIM:
                            recvATIM(pktRx_);

                   		    trace("received ATIM frame" + " < " + ((Mac_802_11_ATIM_Frame)pktRx_).toString() + " > ", "PACKET");

                            break;
                        default:
                            discard(pktRx_, this.DROP_MAC_PACKET_ERROR);
                            
                            pktRx_ = null;
                            rx_resume();
                            return;
                    }
                }
                break;
            case Mac_802_11_Frame_Control.MAC_Type_Control:
                switch(subtype) {
                    case Mac_802_11_Frame_Control.MAC_Subtype_RTS:
                        trace("received RTS frame" + " < " + ((Mac_802_11_RTS_Frame)pktRx_).toString() + " > ", "PACKET");
                        recvRTS(pktRx_);
                        break;
                    case Mac_802_11_Frame_Control.MAC_Subtype_CTS:
                        trace("received CTS frame" + " < " + ((Mac_802_11_CTS_Frame)pktRx_).toString() + " > ", "PACKET");
                        recvCTS(pktRx_);
                        break;
                    case Mac_802_11_Frame_Control.MAC_Subtype_ACK:
                        trace("received ACK frame" + " < " + ((Mac_802_11_ACK_Frame)pktRx_).toString() + " > ", "PACKET");
                        recvACK(pktRx_);
                        break;
                    default:
                        drcl.Debug.error("Mac_802_11 recv_timer()", "Invalid MAC Control Subtype", true);
                }
                break;
            case Mac_802_11_Frame_Control.MAC_Type_Data:
                switch(subtype) {
                    case Mac_802_11_Frame_Control.MAC_Subtype_Data:
                        trace("received DATA frame" + " { " + pktRx_.toString() + " } ", "PACKET");
                        recvDATA(pktRx_);
                        break;
                    default:
                        drcl.Debug.error("Mac_802_11 recv_timer()", "Invalid MAC Data Subtype", true);
                }
                break;
            default:
                drcl.Debug.error("Mac_802_11 recv_timer()", "Invalid MAC Type", true);
        }
        done:
            pktRx_ = null;   // pktRx_ is reset to null, so no need to "free" pktRx_ in recvRTS_(), recvCTS(), recvACK() and recvDATA()
            rx_resume();
    }
    
    
    // refer to recv() and send() in ns2
    /**
     * Processing the frames arriving from the up port.
     */
    protected synchronized void dataArriveAtUpPort(Object data_, drcl.comp.Port upPort_) {
        Packet p = (Packet) ((LLPacket) data_).getBody();
        
        trace("recevie LLPacket from upper layer " + " < " + ((LLPacket) data_).toString() + " > ", "PACKET");
        
        long src_macaddr, dst_macaddr;
        src_macaddr = ((LLPacket) data_).getSrcMacAddr();
        dst_macaddr = ((LLPacket) data_).getDstMacAddr();
        
        boolean fastroute = false; //reserved for future use
        boolean to_transmit = false;

        /*
            the logic of PSM stuff here is as follows:
            1. if the node is powered-off, drop the packet
            2. if the node is in low-power state, buffer the packet, return
            3. check the current state
                3.1 inside ATIM window and got beacon, check the entry in the buffer
                    to decide whether this packet's next hop has acknowledged to wait
                    for pending transmission, buffer the packet afterwards
                3.2 outside the ATIM window, if the packet's next hop has acknowledged or
                    fastroute is turned on, transmit the packet immediately
                3.3 inside the ATIM window, no ack has been received and fastroute is off,
                    buffer the packet
         */
        
        if (psm_enabled_) {
            
            if (netif_.if_sleep() || (atim_timer_.busy() && !got_beacon_) || pktTx_ != null) {
                psm_buffer.addElement(new BUFFER_ENTRY(p, dst_macaddr, false, false, false, 0));
                return;
            }
            
            if (atim_timer_.busy() && got_beacon_) {
                boolean found = false;
                BUFFER_ENTRY new_entry = new BUFFER_ENTRY(p, dst_macaddr, false, false, false, 0);
                Enumeration e = psm_buffer.elements();
                while(e != null && e.hasMoreElements()) {
                    BUFFER_ENTRY this_entry = (BUFFER_ENTRY)e.nextElement();
                    
	                  if (new_entry.get_addr() == this_entry.get_addr() && this_entry.sent_atim_) {
	                    	
                        new_entry.sent_atim_ = this_entry.sent_atim_;
                        new_entry.recvd_ack_ = this_entry.recvd_ack_;
                        found = true;
                        break;
                    }
                }
                psm_buffer.addElement(new_entry);
                
                //send an ATIM is not found
                if (!found && pktATIM_ == null) {
                    atim_scan();
                }
            } 
			else {
            
				if (!atim_timer_.busy()) { //outside ATIM window
					Enumeration e = psm_buffer.elements();
					while(e != null && e.hasMoreElements()) {
						BUFFER_ENTRY this_entry = (BUFFER_ENTRY)e.nextElement();
						if (dst_macaddr == this_entry.get_addr() && (this_entry.recvd_ack_ ||
									(dst_macaddr == MAC_BROADCAST && this_entry.sent_atim_))) {
							to_transmit = true;
							break;
						}
					}
				}

				if(!to_transmit) { //inside ATIM window w/o beacon or outside ATIM window with receiver sleeping
					BUFFER_ENTRY new_entry = new BUFFER_ENTRY(p, dst_macaddr, false, false, false, 0);

					Enumeration e = psm_buffer.elements();
					while(e != null && e.hasMoreElements()) {
						BUFFER_ENTRY this_entry = (BUFFER_ENTRY)e.nextElement();
						if (new_entry.get_addr() == this_entry.get_addr()) {
							new_entry.sent_atim_ = this_entry.sent_atim_;
							new_entry.recvd_ack_ = this_entry.recvd_ack_;
							break;
						}
					}


					psm_buffer.addElement(new_entry);
				}
			}
        }
        
        if (!psm_enabled_ || fastroute || to_transmit) {
            // sendDATA(p, dst_macaddr, src_macaddr);   // if support promiscous arp
            sendDATA(p, dst_macaddr);
            sendRTS(dst_macaddr);
            
            // If the medium is IDLE, we must wait for a DIFS Space before transmitting.
            if ( bf_timer_.busy() == false ) {
                if ( is_idle() ) {
                   /*
                    * If we are already deferring, there is no need to reset the Defer timer
                    */
                    if ( df_timer_.busy() == false ) df_timer_.start(difs_);
                }
               /*
                * If the medium is NOT IDLE, then we start the backoff timer.
                */
                else {
                    bf_timer_.start(cw_, is_idle());
                }
            }
        }
    }
    
    /**
     * Processing the data from the down port.
     */
    protected synchronized void dataArriveAtDownPort(Object data_, drcl.comp.Port downPort_) {
        /*
         * If the interfacec is currently in transmit mode, then it probably won't even see this packet.
         * However, the "air" arround me is BUSY so I need to let the packet proceed. Just set the error
         * flag in the header so that this packet will get thrown away
         */
        
        /* Rong: to turn off the carrier sensing to be a malicious user */
        if (is_malicious_) {
            data_ = null;
            return;
        }
        
        MacPhyContract.Message msg = ( MacPhyContract.Message ) data_;
        
        trace("recevie the first bit of a Mac_802_11_Packet from lower layer"+msg.getPkt(), "EVENT");
        // if in transmission state, drop the signal
        if ( this.tx_active_ && msg.getError() == false ) {
            msg.setError(true);
            ((Mac_802_11_Packet) msg.getPkt()).setForcedError(true);
        }
        
        //Rong: the physial layer should pass me all the signal that has been received
        if ( rx_state_ == MAC_IDLE ) {
            //if it is greater than the receiving threshold
            
            if (msg.getRxPr() >= msg.getRXThresh()) {
                
                SET_RX_STATE(MAC_RECV);
                pktRx_ = (Mac_802_11_Packet) (msg.getPkt());
                
                txinfo_pktRx_error = msg.getError();
                txinfo_pktRx_RxPr  = msg.getRxPr();
                txinfo_pktRx_CPThresh = msg.getCPThresh();
                rx_timer_.start(TX_Time(pktRx_));  // schedule when the last bit will be received
            }
            else {
                if (interferencePwr + msg.getRxPr() > msg.getCSThresh()) {
                    //if greater than the carrier sense range
                    SET_RX_STATE(MAC_RECV);
                    //System.out.println("Below RXThresh, over CSThresh, interference =" + (interferencePwr+msg.getRxPr())+"CSThresh =" + msg.getCSThresh());
                }
                interferencePwr += msg.getRxPr();
                pktRx_ = null;
                setTimeout((Object)msg, TX_Time((Mac_802_11_Packet)msg.getPkt()));
            }
        }
        else {
           /*
            * If the power of the incoming packet is smaller than the power of the packet currently being received by at
            * least the capture threshold, then we ignore the new packet.
            */
            if (pktRx_ == null) {//interference
                interferencePwr += msg.getRxPr();
                setTimeout((Object)msg, TX_Time((Mac_802_11_Packet)msg.getPkt()));
                //System.out.println("Overlapping, interference =" + interferencePwr);
            } 
            else {
                if ( txinfo_pktRx_RxPr / msg.getRxPr() >= msg.getCPThresh() ) {
                    capture((Mac_802_11_Packet) msg.getPkt());
                    interferencePwr += msg.getRxPr();
                    setTimeout((Object)msg, TX_Time((Mac_802_11_Packet)msg.getPkt()));
                    //System.out.println("Capture, interference =" + interferencePwr);
                }
                else {
                    trace("COLLISION"+pktRx_+msg.getPkt(), "EVENT");
                    collision((Mac_802_11_Packet) msg.getPkt());
                    //interferencePwr += msg.getRxPr();
                    // no need to "free" this packet here
                }
            }
        }
    }
    
    
    private void recvRTS(Mac_802_11_Packet p) {
        Mac_802_11_Frame_Control fc;
        
        fc = p.getFc();
        
        if(tx_state_ != MAC_IDLE) {
            discard(p, DROP_MAC_BUSY);
            return;
        }
        
       /*
        *  If I'm responding to someone else, discard this RTS.
        */
        if (pktCTRL_ != null) {
            discard(p, DROP_MAC_BUSY);
            return;
        }

        sendCTS(((Mac_802_11_RTS_Frame)p).getTa(), p.getDuration());
        
       /*
        *  Stop deferring - will be reset in tx_resume().
        */
        if (df_timer_.busy()) df_timer_.stop();
        
        tx_resume("recvRTS");
        
    }
    
    
    private void recvCTS(Mac_802_11_Packet p) {
        if(tx_state_ != MAC_RTS) {
            discard(p, DROP_MAC_INVALID_STATE);
            return;
        }

        _assert("Mac_802_11 recvCTS()", "pktRTS_ != null", (pktRTS_ != null));
        pktRTS_ = null;
        
        _assert("Mac_802_11 recvCTS()", "pktTx_ != null", (pktTx_ != null));
        
        tx_timer_.stop();
        
       /*
        * The successful reception of this CTS packet implies
        * that our RTS was successful.  Hence, we can reset
        * the Short Retry Count and the CW.
        */
        ssrc_ = 0;
        rst_cw();
        
        tx_resume("recvCTS");
        
    }
    
    private void recvDATA(Mac_802_11_Packet p) {
        long dst, src;
        int  size;
        
        dst = ((Mac_802_11_Data_Frame)p).getDa();
        src = ((Mac_802_11_Data_Frame)p).getSa();
        
        size = p.size;
        
       /*
        *  If we sent a CTS, clean up...
        */
        if (dst != MAC_BROADCAST) {
            if (size >= macmib_.RTSThreshold) {
                if (tx_state_ == MAC_CTS) {
                    _assert("Mac_802_11 recvCTS()", "pktCTRL_ != null", (pktCTRL_ != null));
                    pktCTRL_ = null;
                    tx_timer_.stop();
                   /*
                    * Our CTS got through.
                    */
                    ssrc_ = 0;
                    rst_cw();
                }
                else {
                    discard(p, DROP_MAC_BUSY);
                    return;
                }
                sendACK(src);
                tx_resume("recvDATA 1");
            }
           
           /*
            *  We did not send a CTS and there's no
            *  room to buffer an ACK.
            */
            else {
                if(pktCTRL_ != null) {
                    discard(p, DROP_MAC_BUSY);
                    return;
                }
                sendACK(src);
                if(tx_timer_.busy() == false)
                    tx_resume("recvDATA 2");
            }
        }
        
       /* ============================================================
          Make/update an entry in our sequence number cache.
          ============================================================ */
        if(dst != MAC_BROADCAST) {
            Integer hseqno = (Integer)cache_.get(new Long(src));
            
            if( hseqno != null && (hseqno.intValue() == ((Mac_802_11_Data_Frame)p).scontrol) ) {
                discard(p, DROP_MAC_DUPLICATE);
                return;
            }
            cache_.put(new Long(src), new Integer(((Mac_802_11_Data_Frame)p).scontrol));
        }
        
        Packet p2 = (Packet) p.getBody();
        // two possible packets: InetPacket or ARPPacket
        
        llPort.doSending(p2);     // the contract from MAC to LL is really simple here, no Message is defined
        if ( p2 instanceof InetPacket )
            trace("sending Packet to upper layer " + " < " + ((InetPacket) p2).toString() + " > ", "EVENT");
        else if ( p2 instanceof ARPPacket )
            trace("sending Packet to upper layer " + " < " + ((ARPPacket) p2).toString() + " > ", "EVENT");
        else
            trace("sending Packet to upper layer " + " < " + p2.toString() + " > ", "EVENT");
    }
    
    
    private void recvACK(Mac_802_11_Packet p) {
        _assert("recvACK", "tx_state_ != MAC_SEND || !(psm_enabled_ && tx_state_ == MAC_ATIM)", tx_state_ == MAC_SEND || (psm_enabled_ && tx_state_ == MAC_ATIM));

        //  if recvd an ACK, mark the corresponding entry
        if (tx_state_ == MAC_ATIM) {
            atimrc = 0;
            Enumeration e = psm_buffer.elements();
            
            while(e != null && e.hasMoreElements()) {
                BUFFER_ENTRY this_entry = (BUFFER_ENTRY)e.nextElement();
                
                if (last_atim_da_ == this_entry.get_addr()) {

                    this_entry.sent_atim_ = true;
                    this_entry.recvd_ack_ = true;
                    has_packet_tx_++;
                }
                
            }
        }
        
        if (tx_state_ == MAC_SEND) {
            _assert("Mac_802_11 recvACK()", "pktTx_ != null ", (pktTx_ != null));
            pktTx_ = null;
        }
        else {
            _assert("Mac_802_11 recvACK()", "tx_state_ == MAC_ATIM", (tx_state_ == MAC_ATIM));
            pktATIM_ = null;
        }
        
        tx_timer_.stop();
        
        trace("received ACK frame" + " < " + ((Mac_802_11_ACK_Frame)p).toString() + " > ", "PACKET");

        
       /*
        * The successful reception of this ACK packet implies
        * that our DATA transmission was successful.  Hence,
        * we can reset the Short/Long Retry Count and the CW.
        */
        if (p.size  <= macmib_.RTSThreshold)
            ssrc_ = 0;
        else
            slrc_ = 0;
        
        // Backoff before sending again.
        rst_cw();

		if (bf_timer_.busy() == true) {
			debug("Mac state :" + tx_state_ + p+"\n");
		}
        
        
        _assert("Mac_802_11 recvACK()", "bf_timer_.busy() == false", (bf_timer_.busy() == false));
        bf_timer_.start(cw_, is_idle());
        
        tx_resume("recv ACK: "+ tx_state_);
    }
    
     /*
       Upon receiving a beacon message,
       1. cancel the pending beacon message, set got_beacon_ to true
       2. get the tsftimer in the beacon message and update
     */
    private void recvBeacon(Mac_802_11_Packet p) {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("recvBeacon");
        
        got_beacon_ = true;
        
        //cancel pending beacon timer
        if (pktBeacon_ != null)
            pktBeacon_ = null;
        
        if (beacon_timer_.busy())
            beacon_timer_.stop();
        
        long tsf = tsf_timer_.getTSF();
        long time_stamp_ = ((Mac_802_11_Beacon_Frame)p).getTSF();
        
        if (time_stamp_ > tsf + 10000) {
            tsf_timer_.setTSF(time_stamp_);
            
            if(tbtt_timer_.busy())
                tbtt_timer_.stop();
            
            //to do: backoff to send the first ATIM
            double tbtt = macmib_.BeaconPeriod - (tsf%(macmib_.BeaconPeriod*1e6))/1e6;
            if (tbtt < macmib_.BeaconPeriod/3) {
                tbtt_timer_.start(tbtt + macmib_.BeaconPeriod);
            } else {
                tbtt_timer_.start(tbtt);
            }
        }
        
        if (!tx_timer_.busy())
            tx_resume("recv beacon");
    }
    
    private void recvATIM(Mac_802_11_Packet p) {
        
        // release_1208    
        if ( isDebugEnabled() == true )
            debug("recvATIM");
        
        
        recvd_atim_ = true;
        
        long src = ((Mac_802_11_ATIM_Frame)p).getSa();
        long dst = ((Mac_802_11_ATIM_Frame)p).getDa();
        
        trace("RECEIVE  ATIM Packet" + " < " + ((Mac_802_11_ATIM_Frame) p).toString() + " > ", "PACKET");
        
        if(dst == MAC_BROADCAST)
            recvd_bcast_atim_++;
        else {
            recvd_ucast_atim_++;
            // send an ACK
            if (pktCTRL_ == null) {
                sendACK(src);
                if (!tx_timer_.busy())
                    tx_resume("recv ATIM");
            }
        }
    }
    
    private void atim_scan() {

        // release_1208    
        //if ( isDebugEnabled() == true )   debug("atim_scan");
        
        if (pktCTRL_ != null)
            return;
        
        if (pktATIM_ != null) return;
        
        BUFFER_ENTRY this_entry;
        
        Enumeration e = psm_buffer.elements();
        while(e != null && e.hasMoreElements()) {
            this_entry = (BUFFER_ENTRY)e.nextElement();
            if ((!this_entry.recvd_ack_ && this_entry.get_addr() != MAC_BROADCAST) || (tx_bcast_atim_ == 0 && this_entry.get_addr() == MAC_BROADCAST)) {
                entryATIM_ = this_entry;
                
                Mac_802_11_Frame_Control fc = new Mac_802_11_Frame_Control();
                fc.set_fc_type(Mac_802_11_Frame_Control.MAC_Type_Management);
                fc.set_fc_subtype(Mac_802_11_Frame_Control.MAC_Subtype_ATIM);
                fc.set_pwr_mgt(psm_enabled_ && psm_mode_ == PSM_PWR_SAVE);
                
                pktATIM_ = new Mac_802_11_ATIM_Frame(fc,  DATA_DURATION(), macaddr_, this_entry.get_addr(), 0, ETHER_HDR_LEN);
                
                break;
            }
        }
    }
    
    private void trace(String what_, String type_) {
        if ( type_.equals("PACKET") )  {
            if ( MAC_TRACE_ALL_ENABLED || MAC_TRACE_PACKET_ENABLED )
                tracePort.doSending("At " + getTime() + " : MAC addr:" + this.macaddr_ + "--- " + what_ + "\n");
        }
        else if ( type_.equals("CW") ) {
            if ( MAC_TRACE_ALL_ENABLED || MAC_TRACE_CW_ENABLED )
                tracePort.doSending("At " + getTime() + " : MAC addr:" + this.macaddr_ + "--- " + what_ + "\n");
        }
        else if ( type_.equals("TIMER") ) {
            if ( MAC_TRACE_ALL_ENABLED || MAC_TRACE_TIMER_ENABLED )
                tracePort.doSending("At " + getTime() + " : MAC addr:" + this.macaddr_ + "--- " + what_ + "\n");
        }
        else if ( type_.equals("EVENT") ) {
            if ( MAC_TRACE_ALL_ENABLED || MAC_TRACE_EVENT_ENABLED )
                tracePort.doSending("At " + getTime() + " : MAC addr:" + this.macaddr_ + "--- " + what_ + "\n");
        }
    }
    
    /** 
     * Set the random number generator seed. This method is used in simulation 
     * setup script files.
     */
    public void setSeed(long seed) 
    {
        bf_timer_.setSeed(seed);
    }    
    
}


