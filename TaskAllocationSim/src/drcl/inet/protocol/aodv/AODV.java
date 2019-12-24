// @(#)AODV.java   12/2003
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

package drcl.inet.protocol.aodv;

import java.util.*;
import drcl.inet.core.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.inet.protocol.*;

/* Bug Fix for inconsistency of RT 
 * Be careful about the expire time setting. We need to keep consistency btw RT and AODV RT.
 * Whenever calling set_expire() or rt_update or refer to rt.rt_expire,
 * we also need to update the RTEntry in RT using addRTEntry.*/

/**
 * AODV.java: the main part of AODV protocols. 
 * The software is refered to:
 * 1. AODV Draft (version. 11)
 * 2. AODV class in ns-2.
 * In ns-2, the AODV code developed by the 
 * CMU/MONARCH group was optimized and tuned by Samir Das and Mahesh Marina,
 * University of Cincinnati. The work was partially done in Sun Microsystems. 
 *  
 * @author Wei-peng Chen 
 */
public class AODV extends drcl.inet.protocol.Routing implements UnicastRouting, ActiveComponent
{
	public String getName()
	{ return "aodv"; }

	public static boolean debug = false;
	public static boolean debug2 = false;

	/* AODV packet types */
	protected static final int AODVTYPE_RREQ  = 1;
	protected static final int AODVTYPE_RREP  = 2;
	protected static final int AODVTYPE_RERR  = 3;
	protected static final int AODVTYPE_RREP_ACK = 4;
	protected static final int AODVTYPE_HELLO = 5;

	/**************************************************************************************/
	/* configuration parameters, the following value are suggested in Sec. 9 */
	final static double	ACTIVE_ROUTE_TIMEOUT	= 30.0; // 50 seconds
	final static int	ALLOWED_HELLO_LOSS	= 3;  /* packets */
	final static double	ARP_DELAY		= 0.01; /* fixed delay to keep arp happy */
	final static double	DELAY			= 1.0; /* fixed delay to keep arp happy */
	final static int	BAD_LINK_LIFETIME	= 3; /* 3000 ms */
	final static int	BCAST_ID_SAVE		= 6; /* 3 seconds */
	final static double	HELLO_INTERVAL		= 1.0;  /* 1000 ms */
	final static double	MaxHelloInterval	= 1.25 * HELLO_INTERVAL;  /* 1250 ms */
	final static double	MinHelloInterval	= 0.75 * HELLO_INTERVAL;  /* 750 ms */
	// This should be somewhat related to arp timeout
	final static double	LOCAL_REPAIR_WAIT_TIME	= 0.15; //sec
	// timeout after doing network-wide search RREQ_RETRIES times
	final static int	MAX_RREQ_TIMEOUT	= 10; //sec
	final static double	MY_ROUTE_TIMEOUT	= 30.0; // 100 seconds
	final static double	NODE_TRAVERSAL_TIME	= 0.03; // 30 ms
	/* Should be set by the user using best guess (conservative) */
	final static int	NETWORK_DIAMETER = 35; // 35 hops
	// Must be larger than the time difference between a node propagates a route 
	// request and gets the route reply back.
	final static int	RREP_WAIT_TIME		= 1;  // sec
	final static int	REV_ROUTE_LIFE		= 6;	// 5  seconds
	/* No. of times to do network-wide search before timing out for MAX_RREQ_TIMEOUT sec.*/ 
	final static int	RREQ_RETRIES = 3;  
	/* Various constants used for the expanding ring search */
	final static int	TTL_START = 1;
	final static int	TTL_INCREMENT = 2;
	final static int	TTL_THRESHOLD = 7;

	final static double	ROUTE_CACHE_FREQUENCY = 0.5; // sec
	/* The maximum number of packets that we allow a routing protocol to buffer.*/
	final static int	AODV_RTQ_MAX_LEN		= 64; // packets
	/* The maximum period of time that a routing protocol is allowed to buffer a packet for.*/
	final static int	AODV_RTQ_TIMEOUT     = 30; // seconds
	static final String[] PKT_TYPES = { "NULL", "RREQ", "RREP", "RERR", "RREP_ACK", "HELLO" };

	final static long CONTROL_PKT_TYPE = drcl.inet.InetPacket.CONTROL; // for ToS field

	/* seed setting from dcript */
    	static java.util.Random rand = new java.util.Random(7777);
    
	/** AODV router ID of this node */
	protected int	router_id = Integer.MAX_VALUE;

	protected int		seqno ;	/* initial value = 2 */
	private int		bid ;	/* broadcast id */

	private boolean	AODV_link_layer_detection = false;
	
	//////////////////////////////////////////////////////////////////////////////
	// Inner Class
	/** AODV_BroadcastID.java
	 * 
	 * @author Wei-peng Chen
	 * @see AODV 
	 */
	class AODV_BroadcastID
	{	
		public long	src;
		public int	id;		
		public double expire; // now + BCAST_ID_SAVE s
	
		/**
		 * Constructor
		 * @param ospf_: backward OSPF reference
		 */
		AODV_BroadcastID(long src_) { src = src_; }		
		AODV_BroadcastID(long src_, int id_) { src = src_; id = id_;  }
	}

	class AODV_Buffered_pkt 
	{
		protected InetPacket 	ipkt;
		protected double	expire;
		
		AODV_Buffered_pkt(InetPacket ipkt_, double expire_) {
			ipkt = (InetPacket) ipkt_.clone();
			expire = expire_;
		}
	}
		
	//////////////////////////////////////////////////////////////////////////////
	// List variables
	
	/** BroadcastID List */
	protected Vector bcast_id_list = null ;

	/** AODV Routing Entry List */
	protected Vector aodv_rt_entry_list = null ;

	/** AODV Neighbor list */
	protected Vector aodv_nbr_list = null ;
	
	/** AODV pkt queue list */
	protected Vector pkt_list;
	
	protected int		pkt_queue_limit_;
	protected double	pkt_queue_timeout_;

	AODV_TimeOut_EVT bcast_id_EVT	= null;
	AODV_TimeOut_EVT hello_EVT	= null;
	AODV_TimeOut_EVT nbr_EVT	= null;
	AODV_TimeOut_EVT route_EVT	= null;
	AODV_TimeOut_EVT local_repair_EVT = null;

	Port ifport = createIFQueryPort();  // for Interface query service
	Port idport = createIDServicePort(); // for ID lookup service
	
	/* add one port connected diecctly to Mac module, Once a link broken
	   event happens, Mac module will directly notify AODV and ADOV will
	   handle it immediately */
	{
		createLinkBrokenEventPort();
	}

	/* Debug level to enable sampled debug messages. */
	public static final int DEBUG_SAMPLE   = 0;
	public static final int DEBUG_AODV     = 1;
	/* Debug level of sending AODV packets. */
	/** Debug level of sending AODV packets. */
	public static final int DEBUG_SEND     = 2;
	/** Debug level of showing all RREQ packets. */
	public static final int DEBUG_RREQ     = 3;
	/** Debug level of showing all RREP packets. */
	public static final int DEBUG_RREP     = 4;
	/** Debug level of showing all RERR packets. */
	public static final int DEBUG_RERR     = 5;
	public static final int DEBUG_HELLO    = 6;
	public static final int DEBUG_TIMEOUT  = 7;
	public static final int DEBUG_DATA     = 8;
	public static final int DEBUG_ROUTE    = 9;
	static final String[] DEBUG_LEVELS = {
		"sample", "aodv", "send", "rreq", "rrep", "rerr", "hello", "timeout", "data", "route"
	};
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVELS; }

	/** Constructor. */
	public AODV(){
		super();
		bcast_id_list		= new Vector();
		aodv_rt_entry_list	= new Vector();
		aodv_nbr_list		= new Vector();	
		pkt_list		= new Vector();
		pkt_queue_limit_ = AODV_RTQ_MAX_LEN;
		pkt_queue_timeout_ = AODV_RTQ_TIMEOUT;
		
		seqno = 2;
		bid = 1;
	}

	// xxx:
	public String info()
	{
		int now_ = (int)getTime();
		StringBuffer sb_ = new StringBuffer();
		return sb_.toString();
	}
	
	public void reset()
	{
		super.reset();
		router_id = Integer.MAX_VALUE;
		bcast_id_list.removeAllElements();
		aodv_rt_entry_list.removeAllElements();
		aodv_nbr_list.removeAllElements();	
		pkt_list.removeAllElements();

		if (bcast_id_EVT.handle != null) {
			cancelTimeout(bcast_id_EVT.handle);
			bcast_id_EVT.setObject(null);
			bcast_id_EVT.handle = null;
		}
		if (hello_EVT.handle != null) {
			cancelTimeout(hello_EVT.handle);
			hello_EVT.setObject(null);
			hello_EVT.handle = null;
		}
		if (nbr_EVT.handle != null) {
			cancelTimeout(nbr_EVT.handle);
			nbr_EVT.setObject(null);
			nbr_EVT.handle = null;
		}
		if (route_EVT.handle != null) {
			cancelTimeout(route_EVT.handle);
			route_EVT.setObject(null);
			route_EVT.handle = null;
		}
	}

	// xxx:
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		AODV that_ = (AODV)source_;
	}

	protected void _start()
	{
		if (router_id == Integer.MAX_VALUE) {
			router_id = (int) IDLookup.getDefaultID(idport);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_AODV))
				debug(" is constructed");
		}
        
		// whether need to start hello timer (and then trigger nb_timer
		// if the link layer provides the ability of link broken detection (link 802.11),
		// we don't need hello and nbr timer
		if (AODV_link_layer_detection == false ) {
			hello_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_HELLO, null);
			double interval = MinHelloInterval + ((MaxHelloInterval - MinHelloInterval) * rand.nextDouble());
			hello_EVT.handle = setTimeout(hello_EVT, interval);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug("setTimeout " + hello_EVT + " time " + interval);
		}
	}

	/** If calling this function in the script, that means the underlying link layer
	 *  provides the ability of detecting link broken, and then hello timer and neighbor
	 *  timer will be never used.
	 */
	public void enable_link_detection() { AODV_link_layer_detection = true; }
	public void disable_link_detection() { AODV_link_layer_detection = false; }
	
	///////////////////////////////////////////////////////////////////////
	// Event Handler
	///////////////////////////////////////////////////////////////////////
	
	/* Only non-data packets and Broadcast Packets are need to be processed */
	/** This routine is invoked when the link-layer reports a route failed. 
	 * When this function is called, there are two cases:
	 * (1) local repair => send RREQ and not send RERR => wait for reply 
	 *  => if not recv RREP, send RERR and call rt_down
	 * (2) no local repair => nb_delete => handle_link_failure => rt_down & send RERR
	 */
	public void  LinkBrokenEventHandler(InetPacket p, Port inPort_) {
		AODV_RTEntry rt;
		long broken_nbr = p.getNextHop();		
		if((rt = rt_lookup(p.getDestination())) == null) {
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
				debug("LinkBrokenEventHandler: no route, drop pkt " + p);
			}
			if(isGarbageEnabled()) drop(p, "DROP_RTR_MAC_CALLBACK");
			return;
		}

		/* if the broken link is closer to the dest than source,
 		attempt a local repair. Otherwise, bring down the route. */
		if (p.getHops() > rt.rt_hops) {
			local_rt_repair(rt, p); // local repair
			/* retrieve all the packets in the ifq using this link, queue the packets for which local repair is done, */
			/* hold the RERR, do not let src know it right now */
			return;	
		} else {
			if(isGarbageEnabled()) drop(p, "DROP_RTR_MAC_CALLBACK");
			/*Do the same thing for other packets in the interface queue using the broken link -Mahesh */
			/*while((p = ifqueue.filter(broken_nbr))) {	
				drop(p, "DROP_RTR_MAC_CALLBACK");
			}*/
			/* in nb_delete(), this node will send a RRER to the src */
			if (AODV_link_layer_detection == false) nb_delete(broken_nbr);
			handle_link_failure(broken_nbr);
		}
	}
	
	//protected int[] ucastQueryHandler(InetPacket req_, Port inPort_)
	public int[] routeQueryHandler(InetPacket req_, int incomingIf_, Port inPort_)
	{
		AODV_RTEntry aodv_rt;
		aodv_rt = rt_lookup(req_.getDestination());
		if(aodv_rt == null) {
			aodv_rt = rt_add(req_.getDestination());
		}

		/* If the route is up, forward the packet */
		if(aodv_rt.rt_flags == AODV_RTEntry.RTF_UP) {
			Object rt_obj = retrieveRTEntryDest(aodv_rt.rt_dst);
			if ( rt_obj!= null && rt_obj instanceof RTEntry) { 
				RTEntry rt = (RTEntry) rt_obj;
				return rt._getOutIfs();
			} else if ( rt_obj!= null && rt_obj instanceof RTEntry[] && ((RTEntry[])rt_obj).length > 0) { 
                                RTEntry[] mul_rt = (RTEntry[]) rt_obj;
				debug("mul_rt length " + mul_rt.length );
                                // xxx: always return the first one
                                return mul_rt[0]._getOutIfs();
                         }
 			 debug("ERROR! inconsistency btw RT(empty) len: " + ((RTEntry[])rt_obj).length + " and aodv_RT " + aodv_rt + " pkt: " + req_  );
                         aodv_rt.rt_down();
		}

		/* if I am the source of the packet, then do a Route Request.*/
		if(req_.getSource() == router_id) {
			aodv_pkt_enque(req_);
			/*debug("sendRequest from ucastQueryHandler" + aodv_rt.rt_dst + "pkt " + req_);*/
			sendRequest(aodv_rt.rt_dst);
		}
		/* A local repair is in progress. Buffer the packet. */
		else if (aodv_rt.rt_flags == AODV_RTEntry.RTF_IN_REPAIR) {
			aodv_pkt_enque(req_);
		}
		/* I am trying to forward a packet for someone else to which
		 *  I don't have a route. */
		else {
			AODV_RERR re = new AODV_RERR();
			/* For now, drop the packet and send error upstream.
			 * Now the route errors are broadcast to upstream
			 * neighbors - Mahesh 09/11/99 */	
			re.DestCount = 0;
			re.unreachable_dst[re.DestCount] = aodv_rt.rt_dst;
			re.unreachable_dst_seqno[re.DestCount] = aodv_rt.rt_seqno;
			re.DestCount += 1;
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_AODV) || isDebugEnabledAt(DEBUG_RERR))) {
				debug("sending RERR: " + re + " pkt:" + req_);
				if (aodv_rt != null)
					debug("RERR RT: " + aodv_rt );
				
			}
			sendError(re, false);
			if(isGarbageEnabled()) drop(req_, "DROP_RTR_NO_ROUTE");
		}
		return null;
	}
	
	///////////////////////////////////////////////////////////////////////
	//  Receiving Routine
	///////////////////////////////////////////////////////////////////////
	/** 
	 * Handling timeout event. According to the timeout event 
	 * which can be obtained from evt_, there are different 
	 * handling functions.
	 * @param evt_ event object got from the timer port
	 */
	protected void timeout(Object evt_)
	{
		InetPacket pkt;
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_TIMEOUT)))
			debug("__timeout__ " + evt_);

		int type = ((AODV_TimeOut_EVT)evt_).EVT_Type;
		double stime = getTime();

		switch(type) {
			case AODV_TimeOut_EVT.AODV_TIMEOUT_BCAST_ID:
				if (evt_ != bcast_id_EVT) {
					error("timeout()", " ** ERROR ** where does this come from? " + evt_);
					break;
				}
				id_purge();
				bcast_id_EVT.handle = setTimeout(bcast_id_EVT, BCAST_ID_SAVE);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
					debug("Time: " + stime + " set BCAST timeout: " + BCAST_ID_SAVE);
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_HELLO:
				if (evt_ != hello_EVT) {
					error("timeout()", " ** ERROR ** where does this come from? " + evt_);
					break;
				}
				sendHello();
				double interval = MinHelloInterval + ((MaxHelloInterval - MinHelloInterval) * rand.nextDouble());
				hello_EVT.handle = setTimeout(hello_EVT, interval);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
					debug("Time: " + stime + " set HELLO timeout: " + interval);
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_NBR:
				if (evt_ != nbr_EVT) {
					error("timeout()", " ** ERROR ** where does this come from? " + evt_);
					break;
				}
				nb_purge();
				nbr_EVT.handle = setTimeout(nbr_EVT, HELLO_INTERVAL);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
					debug("Time: " + stime + " set NBR timeout: " + HELLO_INTERVAL);
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_ROUTE:
				if (evt_ != route_EVT) {
					error("timeout()", " ** ERROR ** where does this come from? " + evt_);
					break;
				}
				rt_purge();
				route_EVT.handle = setTimeout(route_EVT, ROUTE_CACHE_FREQUENCY);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
					debug("Time: " + stime + " set Route timeout: " + ROUTE_CACHE_FREQUENCY);
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_LOCAL_REPAIR: 
				pkt = (InetPacket) ((AODV_TimeOut_EVT)evt_).getObject();
				AODV_RTEntry rt = rt_lookup(pkt.getDestination());
	
				if (rt != null && rt.rt_flags != AODV_RTEntry.RTF_UP) {
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_TIMEOUT)) ) {
						debug("Time: " + stime + " failed local repair; dst: " + rt.rt_dst);
						debug(" local repair timeout" + "nexthop: " + rt.rt_nexthop + " pkt: " + pkt);
					}
					if (AODV_link_layer_detection == false) nb_delete(pkt.getDestination());
					handle_link_failure(pkt.getDestination());
				}
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_DELAY_FORWARD: 
				pkt = (InetPacket) ((AODV_TimeOut_EVT)evt_).getObject();
				downPort.doSending(pkt);
				break;
			case AODV_TimeOut_EVT.AODV_TIMEOUT_DELAY_BROADCAST: 
				AODV_Packet aodv_pkt = (AODV_Packet) ((AODV_TimeOut_EVT)evt_).getObject();
				broadcast( aodv_pkt, (long) router_id, Address.ANY_ADDR, true, 1/*ttl*/, CONTROL_PKT_TYPE, Address.ANY_ADDR /*nexthop*/);
				break;
		}
		return;
	}

	protected void process(Object data_, drcl.comp.Port inPort_)
	{
		lock(this);
		super.process(data_, inPort_);
		unlock(this);
	}
	
  	/**
	 * Handle data arriving at the down port.
	 * According to the different packet types ( AODV Hello pkt, Database descricption
	 * pkt,Link State update pkt, LS request pkt, or LS ack pkt), different 
	 * corresponding methods can handle the packet.
	 * Note for AODV_Hello pkt will be received only once. The other hello maintaince
	 * is done by drcl.inet.core.Hello
	 *  
	 * @param date_ message body arriving at the down port.
	 * @param downPort_ down port at which messages arrive.
	 */
	public void dataArriveAtDownPort(Object data_, drcl.comp.Port downPort_) 
	{
		InetPacket ipkt_ = (InetPacket)data_;
		AODV_Packet pkt_ = (AODV_Packet)ipkt_.getBody();

		int src_ = pkt_.getRouterID(); /* router ip addr of the source */
		int rt_id = pkt_.getRouterID();
		int pkt_type = pkt_.getType();

		// if the packet is AODV pkt
		switch (pkt_type) {
			case AODVTYPE_RREQ:
				AODV_RREQ req = (AODV_RREQ) pkt_.getBody();
				recvRequest(req, ipkt_);
				break;
			case AODVTYPE_RREP:
				AODV_RREP rep = (AODV_RREP) pkt_.getBody();
				recvReply(rep, ipkt_);
				break;
			case AODVTYPE_RERR:
				AODV_RERR err = (AODV_RERR) pkt_.getBody();
				recvError(err, ipkt_);
				break;
			case AODVTYPE_HELLO:
				AODV_RREP rep_ = (AODV_RREP) pkt_.getBody();
				recvHello(rep_, ipkt_);
				break;
			default:		
				if (isDebugEnabled()) {
					debug(" Invalid AODV type" + pkt_type);
				}
				System.exit(1);
		}
	}
	
	/* ref: 8.3.1 */
	protected void recvRequest(AODV_RREQ rq, InetPacket ipkt_) {
            	int inIf_ = ipkt_.getIncomingIf();

		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_AODV))) {
			debug(" aodv: receive RREQ: " + rq + " ipkt " + ipkt_ + " from " + ipkt_.getIncomingIf());
		}
		/* Drop if:		*  - I'm the source
		*  - I recently heard this request.
		*/
		if(rq.rq_src == router_id) {
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_AODV))) {
				debug(" got my own REQUEST: " + rq);
			}	
			return;
		}	
		// receive duplicated RREQ
		if ( id_lookup(rq.rq_src, rq.rq_bcast_id) == true ) {
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_AODV))){ 
				debug(" discarding request: " + rq);
			}
			return;
		}

		/* Cache the broadcast ID */
		id_insert(rq.rq_src, rq.rq_bcast_id);

		/* We are either going to forward the REQUEST or generate a
		* REPLY. Before we do anything, we make sure that the REVERSE
		* route is in the route table.		*/
		double now = getTime();
		AODV_RTEntry rev_rt; // rev_rt is the reverse route 
   
		rev_rt = rt_lookup(rq.rq_src);
		if(rev_rt == null) { 
			/* if not in the route table */
			// create an entry for the reverse route.
			rev_rt = rt_add(rq.rq_src);
		}
                /* set_expire return true if it really modify rt_expire */
                if ( rev_rt.set_expire( now + REV_ROUTE_LIFE) == true) {
                    //aodv_addRTEntry(rev_rt.rt_dst, ipkt_.getSource(), REV_ROUTE_LIFE );
                    aodv_addRTEntry(rev_rt.rt_dst, ipkt_.getSource(), REV_ROUTE_LIFE, inIf_ );
                }
                
		// If we have a fresher seq no. or lesser #hops for the 
		// same seq no., update the rt entry. Else don't bother.
		if ((rq.rq_src_seqno > rev_rt.rt_seqno ) ||
    		((rq.rq_src_seqno == rev_rt.rt_seqno) && (rq.rq_hop_count < rev_rt.rt_hops)) ) {
			// update the info. of the next hop field in the reverse route
			rev_rt.rt_update(rq.rq_src_seqno, rq.rq_hop_count, ipkt_.getSource());			
			if (rev_rt.rt_req_timeout > 0.0) {
				/* Reset the soft state and  Set expiry time to CURRENT_TIME + ACTIVE_ROUTE_TIMEOUT. This is because route is used in the forward direction,  but only sources get benefited by this change */
				rev_rt.rt_req_cnt = 0;
				rev_rt.rt_req_timeout = 0.0; 
				rev_rt.rt_req_last_ttl = rq.rq_hop_count;
				rev_rt.rt_expire = now + ACTIVE_ROUTE_TIMEOUT;
			}
			/* update RT info */	
			//aodv_addRTEntry(rq.rq_src, ipkt_.getSource(), ACTIVE_ROUTE_TIMEOUT);
			aodv_addRTEntry(rq.rq_src, ipkt_.getSource(), ACTIVE_ROUTE_TIMEOUT, inIf_);
			/* Find out whether any buffered packet can benefit from the reverse route. May need some change in the following code - Mahesh 09/11/99 */
			InetPacket buffered_pkt;
			while ((buffered_pkt = aodv_pkt_deque(rev_rt.rt_dst)) != null) {
				if ( rev_rt.rt_flags == AODV_RTEntry.RTF_UP ) {
					aodv_delay_forward(rev_rt, buffered_pkt, 0, false);
				}
			}
		} // End for putting reverse route in rt table

		/* We have taken care of the reverse route stuff.
		* Now see whether we can send a route reply. 
		*/
		AODV_RTEntry rt = rt_lookup(rq.rq_dst);

		// First check if I am the destination ..
		if(rq.rq_dst == router_id) {
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_AODV))){ 
				debug(" destination sending reply: " );
			}
			/*Just to be safe, I use the max. Somebody may have incremented the dst seqno. */
			seqno = (seqno > rq.rq_dst_seqno) ? (seqno + 1) : (rq.rq_dst_seqno + 1);
			if (seqno%2 == 1) seqno++;

			sendReply(rq.rq_src,	/* IP Destination */
             			 	  1,	/* Hop Count */
				  router_id,	/* Dest IP Address */
             			      seqno,	/* Dest Sequence Num */
		           MY_ROUTE_TIMEOUT,	/* Lifetime */
             		rq.rq_timestamp);	/* timestamp */
		}
		/* I am not the destination, but I may have a fresh enough route. */
		else if ( rt != null && (rt.rt_hops != Integer.MAX_VALUE) && (rt.rt_seqno >= rq.rq_dst_seqno) ) {
			sendReply(rq.rq_src,
             		     rt.rt_hops + 1,
		                  rq.rq_dst,
		                rt.rt_seqno,
		       (rt.rt_expire - now),
			   rq.rq_timestamp);	
		/* Insert nexthops to RREQ source and RREQ destination in the precursor lists of destination and source respectively */
			rt.pc_insert(rev_rt.rt_nexthop); // nexthop to RREQ source
			rev_rt.pc_insert(rt.rt_nexthop); // nexthop to RREQ destination
			// xxx: send grat RREP to dst if G flag set in RREQ
		}
		/* Can't reply. So forward the  Route Request */
		else {
			/* create a new RREQ */
			AODV_RREQ new_rq = (AODV_RREQ) rq.clone();

			ipkt_.setSource(router_id);
			ipkt_.setDestination(Address.ANY_ADDR);
			new_rq.rq_hop_count += 1;
			// Maximum sequence number seen en route
			if (rt != null) new_rq.rq_dst_seqno = (rt.rt_seqno > new_rq.rq_dst_seqno)? rt.rt_seqno : new_rq.rq_dst_seqno ;
			// replace it with broadcast
			aodv_delay_broadcast (  AODVTYPE_RREQ, new_rq, new_rq.size(), 1, null, DELAY * rand.nextDouble(), true);
			//aodv_delay_forward (  AODVTYPE_RREQ, new_rq, new_rq.size(), 1, null, DELAY * rand.nextDouble(), true);
		}
	}
		
	protected void recvReply(AODV_RREP rp, InetPacket ipkt_) {
            	int inIf_ = ipkt_.getIncomingIf();
                
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_AODV))) {	
			debug( " recv RREP: " + rp + "ipkt: " + ipkt_);
		}
		AODV_RTEntry rt;
		boolean suppress_reply = false;
		double delay = 0.0;
		double now = getTime();
	
		/*  Got a reply. So reset the "soft state" maintained for 
		*  route requests in the request table. We don't really have
		*  have a separate request table. It is just a part of the
		*  routing table itself. 
		*/
		// Note that rp_dst is the dest of the data packets, not the
		// the dest of the reply, which is the src of the data packets.
		rt = rt_lookup(rp.rp_dst);
        
		/* If I don't have a rt entry to this host... adding */
		if(rt == null) {
			rt = rt_add(rp.rp_dst);
		}

		/* Add a forward route table entry... here I am following
		* Perkins-Royer AODV paper almost literally - SRD 5/99	*/
		if ( (rt.rt_seqno < rp.rp_dst_seqno) ||   /* newer route */ 
		((rt.rt_seqno == rp.rp_dst_seqno) && (rt.rt_hops > rp.rp_hop_count)) ) { 
			/* shorter or better route */
			/* Update the rt entry, including the next hop and expire info. */
			rt.rt_update(rp.rp_dst_seqno, rp.rp_hop_count, ipkt_.getSource(), now + rp.rp_lifetime);
			/* update RT info */
			aodv_addRTEntry(rp.rp_dst, ipkt_.getSource(), rp.rp_lifetime, inIf_);
			//aodv_addRTEntry(rp.rp_dst, ipkt_.getSource(), ACTIVE_ROUTE_TIMEOUT);
			/* reset the soft state */
			rt.rt_req_cnt = 0;
			rt.rt_req_timeout = 0.0; 
			rt.rt_req_last_ttl = rp.rp_hop_count;
  
			if (ipkt_.getDestination() == router_id) { 
				/* If I am the original source
				 * Update the route discovery latency statistics
				 * rp.rp_timestamp is the time of request origination */
				rt.rt_disc_latency[rt.hist_indx] = (now - rp.rp_timestamp) / (double) rp.rp_hop_count;
				/* increment indx for next time */
				rt.hist_indx = (rt.hist_indx + 1) % AODV_RTEntry.MAX_HISTORY;
			}

			/* Send all packets queued in the sendbuffer destined for this destination. 
			* XXX - observe the "second" use of p.
			*/
			InetPacket buf_pkt;
			while ((buf_pkt = aodv_pkt_deque(rt.rt_dst)) != null) {
				if(rt.rt_hops != Integer.MAX_VALUE ) {
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_DATA) || isDebugEnabledAt(DEBUG_AODV))) {	
						debug( "ready to send buffered pkt " + buf_pkt + " rt:" + rt);
					}
					/* Delay them a little to help ARP. Otherwise ARP 
					 * may drop packets. -SRD 5/23/99 */
					aodv_delay_forward(rt, buf_pkt, delay, false);
					delay += ARP_DELAY;
				}
			}
		} else {	
			suppress_reply = true;		
		}

		/* If reply is not for me, forward it. */
		if(ipkt_.getDestination() != router_id && suppress_reply == false ) {
			/* create a new RREP */
			AODV_RREP new_rp = (AODV_RREP) rp.clone();
			
			 /* Find the rt entry */
			AODV_RTEntry rt0 = rt_lookup(ipkt_.getDestination());
			/* If the rt is up, forward */
			if( rt0 != null && (rt0.rt_hops != Integer.MAX_VALUE)) {
				new_rp.rp_hop_count += 1;
				new_rp.rp_src = router_id;
				aodv_delay_forward (  AODVTYPE_RREP, new_rp, new_rp.size(), 1, rt0, 0/* no delay*/, false);
				/* Insert the nexthop towards the RREQ source to
				 * the precursor list of the RREQ destination */
				rt.pc_insert(rt0.rt_nexthop); /* nexthop to RREQ source */
			} else {
			// I don't know how to forward .. drop the reply. 
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_AODV))) {
					debug(" dropping Route Reply: " + rp);
				}
				if(isGarbageEnabled()) drop(ipkt_, "DROP_RTR_NO_ROUTE");
			}
		}
	}

	/** receive a packet with RERR type, indicating the information of a broken link */
	public void recvError(AODV_RERR re, InetPacket ipkt_) {
        if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {	
			debug( " recv RERR: " + re + " ipkt: " + ipkt_);
		}

		AODV_RTEntry rt;		
		double now = getTime();
		AODV_RERR nre = new AODV_RERR();
		nre.DestCount = 0;
		for (int i=0; i < re.DestCount; i++) {
			/* For each unreachable destination*/
			rt = rt_lookup(re.unreachable_dst[i]);
			if ( rt != null && (rt.rt_hops != Integer.MAX_VALUE) &&	(rt.rt_nexthop == ipkt_.getSource()) &&	(rt.rt_seqno <= re.unreachable_dst_seqno[i]) ) {
				/* is the seqno even? */
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
					debug( "dst: " + rt.rt_dst + " seq: " + rt.rt_seqno + " nexthop: " + rt.rt_nexthop + " unreach dst: " + re.unreachable_dst[i] + " unreach dst seq: " + re.unreachable_dst_seqno[i] + " src: " + ipkt_.getSource());
				}
				rt.rt_seqno = re.unreachable_dst_seqno[i];
				rt.rt_down();
				/* remove the RTEntry in RT*/
				aodv_removeRTEntry(rt.rt_dst);

				/* if precursor list non-empty add to RERR and delete the precursor list*/
				if (rt.pc_empty() == false) {
					nre.unreachable_dst[nre.DestCount] = rt.rt_dst;
					nre.unreachable_dst_seqno[nre.DestCount] = rt.rt_seqno;
					nre.DestCount += 1;
					rt.pc_delete();
				}
			}
		}

		if (nre.DestCount > 0) {
			sendError(nre, true);
		} 
	}

	/** receive a hello packet to maintain the neighbor relationship, Hello packet is only used when the 
	 *  link layer does not provide the link broken detection function */ 
	public void recvHello(AODV_RREP rp, InetPacket ipkt_) {
		double now = getTime();
		AODV_BroadcastID nb = nb_lookup(rp.rp_dst);
		if(nb == null) {
			nb_insert(rp.rp_dst);
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_HELLO) || isDebugEnabledAt(DEBUG_AODV))) {	
				debug( " recv Hello from new nbr: " + rp);
			}
		} else {
			nb.expire = now + (1.5 * ALLOWED_HELLO_LOSS * HELLO_INTERVAL);
		}
	}


	/////////////////////////////////////////////////////////////////////////////
	//  Sending Routine
	/////////////////////////////////////////////////////////////////////////////
	/**
	 * broadcast the RREQ pkt	
	 * ref: sec. 5.3
	 */	
	protected void sendRequest(long dst) {
		int ttl_;
		double now = getTime();
		/* Allocate a RREQ packet */
		AODV_RREQ rq = new AODV_RREQ( );
		AODV_RTEntry rt = rt_lookup(dst);
		/* Rate limit sending of Route Requests. We are very conservative about sending out route requests. */
		if (rt.rt_flags == AODV_RTEntry.RTF_UP) {
			return;
		}
		if (rt.rt_req_timeout > now) {
			return;
		}

		/* rt_req_cnt is the no. of times we did network-wide broadcast
		 * RREQ_RETRIES is the maximum number we will allow before going to a long timeout. */
		if (rt.rt_req_cnt > RREQ_RETRIES) {
			rt.rt_req_timeout = now + MAX_RREQ_TIMEOUT;
			rt.rt_req_cnt = 0;
			InetPacket buf_pkt;
			while ((buf_pkt = aodv_pkt_deque(rt.rt_dst)) != null) {
				if(isGarbageEnabled()) drop(buf_pkt, "DROP_RTR_NO_ROUTE");
			}	
			return;
		}
		// Determine the TTL to be used this time. 
		// Dynamic TTL evaluation - SRD
		rt.rt_req_last_ttl = (rt.rt_req_last_ttl > rt.rt_last_hop_count) ? rt.rt_req_last_ttl : rt.rt_last_hop_count;

		if (rt.rt_req_last_ttl == 0) {
			/* first time query broadcast */
			ttl_ = TTL_START;
		} else {
			// Expanding ring search.
			if (rt.rt_req_last_ttl < TTL_THRESHOLD) {
				ttl_ = rt.rt_req_last_ttl + TTL_INCREMENT;
			} else {
				// network-wide broadcast
				ttl_ = NETWORK_DIAMETER;
				rt.rt_req_cnt += 1;
			}
		}

		// remember the TTL used  for the next time
		rt.rt_req_last_ttl = ttl_;

		// PerHopTime is the roundtrip time per hop for route requests.
		// The factor 2.0 is just to be safe .. SRD 5/22/99
		// Also note that we are making timeouts to be larger if we have 
		// done network wide broadcast before. 
		rt.rt_req_timeout = 2.0 * (double) ttl_ * PerHopTime(rt); 
		if (rt.rt_req_cnt > 0) {
			rt.rt_req_timeout *= rt.rt_req_cnt;	
		}
		rt.rt_req_timeout += now;

		// Don't let the timeout to be too large, however .. SRD 6/8/99
		if (rt.rt_req_timeout > now + MAX_RREQ_TIMEOUT) {
			rt.rt_req_timeout = now + MAX_RREQ_TIMEOUT;
		}
		rt.rt_expire = 0;
		// Fill up some more fields. 
		rq.rq_type = AODVTYPE_RREQ;
		rq.rq_hop_count = 1;
		rq.rq_bcast_id = bid++;
		rq.rq_dst = dst;
		rq.rq_dst_seqno = (rt == null ? 0: rt.rt_seqno);
		rq.rq_src = router_id;
		seqno += 2;
		rq.rq_src_seqno = seqno;
		rq.rq_timestamp = now;
		
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREQ) || isDebugEnabledAt(DEBUG_AODV))){ 
			debug(" sending Route Request, dst:" + dst + " timeout: " + (rt.rt_req_timeout - now) + " rt: " + rt.rt_req_last_ttl +" rq: " + rq);
		}

		aodv_message_broadcast( AODVTYPE_RREQ, rq, rq.size(), ttl_);
	}
	
	protected void sendReply(long ipdst, int hop_count, long rpdst, int rpseq, double lifetime, double timestamp) {
		int ttl_;
		double now = getTime();
		/* Allocate a RREP packet */
		AODV_RREP rp = new AODV_RREP( );
		AODV_RTEntry rt = rt_lookup(ipdst);
		if ( rt == null ) {
			if (isDebugEnabled()){ 
				debug( "ERROR! reverse rt is null");		 	
			}
			return;
		}			
		rp.rp_type = AODVTYPE_RREP;
		rp.rp_hop_count = hop_count; /* if I am dest, rp_hop_count = 1 */
		rp.rp_dst = rpdst;
		rp.rp_dst_seqno = rpseq;
		rp.rp_src = router_id;
		rp.rp_lifetime = lifetime;
		rp.rp_timestamp = timestamp;
		ttl_ = NETWORK_DIAMETER;
		
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RREP) || isDebugEnabledAt(DEBUG_AODV))){ 
			debug(" sending Reply: " + rp  + " dst " + ipdst );
		}

		aodv_message_send ( AODVTYPE_RREP, rp, rp.size(), ipdst, ttl_);
	}
	
	protected void sendError(AODV_RERR re, boolean jitter) {
		int ttl_;	
		double now = getTime();
    
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))){ 
			debug(" sending RERR: " + re);
		}
		re.re_type = AODVTYPE_RERR;

		aodv_message_broadcast( AODVTYPE_RERR, re, re.size(), 1);
	}
	
	/** ref: sec. 8.7 Hello Message */
	protected void sendHello() {
		int ttl_;
		double now = getTime();
		/* Allocate a RREP packet */		
		AODV_RREP rh = new AODV_RREP( );
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_HELLO) || isDebugEnabledAt(DEBUG_AODV))){ 
			debug(" sending Hello: " + rh);
		}
		rh.rp_type = AODVTYPE_HELLO;
		rh.rp_hop_count = 1;
		rh.rp_dst = router_id;
		rh.rp_dst_seqno = seqno;
		rh.rp_lifetime = (1 + ALLOWED_HELLO_LOSS) * HELLO_INTERVAL;
		aodv_message_broadcast( AODVTYPE_HELLO, rh, rh.size(), 1);
	}

	/** send the pkt to specific neighbor on specific if */
	protected void aodv_message_send ( int type, Object body, int body_size, long dst, int ttl)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug(PKT_TYPES[type] + " sent from " + router_id + " to " + dst + ": " + body);
		/* memory allocate for protocol header */
		AODV_Packet aodv_pkt = new AODV_Packet(type, router_id); 
		aodv_pkt.setBody(body, body_size);

		// should enable router alert since the reply should be forwarded by intermediate nodes	
		forward( aodv_pkt, (long) router_id, dst, true, ttl, CONTROL_PKT_TYPE);
	}

	/**
	* Send the packet to every neighbor connected to this interfac. 
	*
	* For sending packets downward, we call the forward(), defined in Protocol.java  
	* void forward(PacketBody p_, long src_, long dest_, int dest_ulp_, boolean
	* routerAlert_, int TTL, int ToS, int link_id)
	* route-lookup forwarding routerAlert is set to be true, in which the packet is sent
	* in only one hop. In this case, whatever dst is irrevelent
	 */
	protected void aodv_message_broadcast ( int type, Object body, int body_size, int ttl)
	{
		/* memory allocate for protocol header */
		AODV_Packet aodv_pkt = new AODV_Packet(type, router_id); 
		aodv_pkt.setBody(body, body_size);
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug(PKT_TYPES[type] + " broadcast from " + router_id + ": " + body + "ttl " + ttl );
		/* if route alert is true, pd ignore ttl */
		broadcast( aodv_pkt, (long) router_id, Address.ANY_ADDR, true, ttl, CONTROL_PKT_TYPE, Address.ANY_ADDR /*nexthop*/);
	}

	// for non ip pkt only, created ipkt first then call standard aodv_delay_broadcast()
	protected void aodv_delay_broadcast ( int type, Object body, int body_size, int ttl, AODV_RTEntry rt, double delay, boolean bcast)
	{
		InetPacket ipkt_;
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug("delay** " + PKT_TYPES[type] + " broadcast from " + router_id + ": " + body +"for time " + delay);
		/* memory allocate for protocol header */
		AODV_Packet aodv_pkt = new AODV_Packet(type, router_id); 
		aodv_pkt.setBody(body, body_size);
		if (bcast == true || delay != 0.0) {
			AODV_TimeOut_EVT aodv_bcast_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_DELAY_BROADCAST, aodv_pkt);
			aodv_bcast_EVT.handle = setTimeout( aodv_bcast_EVT, delay);
		} else {
			broadcast( aodv_pkt, (long) router_id, Address.ANY_ADDR, true, ttl, CONTROL_PKT_TYPE, Address.ANY_ADDR /*nexthop*/);
		}
	}

	// for non ip pkt only, created ipkt first then call standard aodv_delay_forward()
	protected void aodv_delay_forward ( int type, Object body, int body_size, int ttl, AODV_RTEntry rt, double delay, boolean bcast)
	{
		InetPacket ipkt_;
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug("delay* " + PKT_TYPES[type] + " broadcast from " + router_id + ": " + body +"for time " + delay);
		/* memory allocate for protocol header */
		AODV_Packet aodv_pkt = new AODV_Packet(type, router_id); 
		aodv_pkt.setBody(body, body_size);
		if (bcast == true) {
			ipkt_ = new InetPacket((long) router_id, Address.ANY_ADDR, 0/*protocol*/, ttl, 0/*hops*/, true /*routerAlert_*/, CONTROL_PKT_TYPE/*tos_*/, 0/*id*/, 0/*flag*/, 0/*frag offset*/, aodv_pkt, aodv_pkt.size, Address.ANY_ADDR /*next_hop*/);
		} else {
			ipkt_ = new InetPacket((long) router_id, rt.rt_dst, 0/*protocol*/, ttl, 0/*hops*/, true /*routerAlert_*/, CONTROL_PKT_TYPE/*tos_*/, 0/*id*/, 0/*flag*/, 0/*frag offset*/, aodv_pkt, aodv_pkt.size);

		}
		aodv_delay_forward(rt, ipkt_, delay, bcast);
	}

	/* Packet Transmission Routines */
	protected void aodv_delay_forward(AODV_RTEntry rt, InetPacket p, double delay, boolean bcast) {
		double now = getTime();
		if(p.getTTL() == 0) {
			if(isGarbageEnabled()) drop(p, "DROP_RTR_TTL");
			return;
		}
		if (rt != null) {
			p.setNextHop( rt.rt_nexthop);
		} else { // if it is a broadcast packet, do nothing
		}
		if (bcast == true) {
			AODV_TimeOut_EVT aodv_bcast_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_DELAY_FORWARD, p);
			aodv_bcast_EVT.handle = setTimeout( aodv_bcast_EVT, delay);
		} else {
			if (delay > 0) {
				AODV_TimeOut_EVT aodv_forword_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_DELAY_FORWARD, p);
				aodv_forword_EVT.handle = setTimeout( aodv_forword_EVT, delay);
			} else {
				// no delay, sendout immediately
				downPort.doSending(p);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////
	// AODV Neighbor Management  Functions	
	//////////////////////////////////////////////////////////////////////////
	private void nb_insert(long id) {
		double now = getTime();
		if(nbr_EVT == null) {
			nbr_EVT	= new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_NBR, null);
			nbr_EVT.handle = setTimeout(nbr_EVT, HELLO_INTERVAL);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug(" set NBR timeout: " + HELLO_INTERVAL);
		}
		AODV_BroadcastID b = new AODV_BroadcastID(id);
		b.expire = now + (1.5 * ALLOWED_HELLO_LOSS * HELLO_INTERVAL);
		aodv_nbr_list.addElement(b);
		seqno += 2;             // set of neighbors changed
	}

	private AODV_BroadcastID nb_lookup(long id) {
		int no = aodv_nbr_list.size();
		for (int i = 0; i < no; i++) {
			AODV_BroadcastID nbr = (AODV_BroadcastID) aodv_nbr_list.elementAt(i);
			if (nbr.src == id)
				return nbr;
		}
		return null;
	}
	
	/* Purges all timed-out Neighbor Entries - runs every
	* HELLO_INTERVAL * 1.5 seconds.
	*/
	private void nb_purge() {
		double now = getTime();
		for (int i = 0; i < aodv_nbr_list.size(); i++) {
			AODV_BroadcastID nbr = (AODV_BroadcastID) aodv_nbr_list.elementAt(i);
			if (nbr.expire <= now)
				nb_delete(nbr.src);
			else
				i++;
		}
	}

	/** Called when we receive *explicit* notification that a Neighbor
	* is no longer reachable.
	* nb_delete is called by :
	* (1) LinkBrokenEventHandler (@ no local repair)
	* (2) nb_purge (@nbr.expire <= now) 
`	*/
	public void nb_delete(long id) {
		seqno += 2;     // Set of neighbors changed
		int no = aodv_nbr_list.size();
		for (int i = 0; i < no; i++) {
			AODV_BroadcastID nbr = (AODV_BroadcastID) aodv_nbr_list.elementAt(i);
			if (nbr.src == id) {
				aodv_nbr_list.removeElement(nbr);
				break;
			}
		}
	}

	/* called by nb_delete, which periodically check whether nbr is valid */
	private void handle_link_failure(long id) {
		double now = getTime();
		AODV_RERR re = new AODV_RERR();
		re.DestCount = 0;
		int no = aodv_rt_entry_list.size();
		for (int i = 0; i < no; i++) {
			AODV_RTEntry rt = (AODV_RTEntry) aodv_rt_entry_list.elementAt(i);
			if ((rt.rt_hops != Integer.MAX_VALUE) && (rt.rt_nexthop == id) ) {
				rt.rt_seqno++;
				re.unreachable_dst[re.DestCount] = rt.rt_dst;
				re.unreachable_dst_seqno[re.DestCount] = rt.rt_seqno;
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
					debug(" unreach dst: " + re.unreachable_dst[re.DestCount] + " unreach dst seq: " + re.unreachable_dst_seqno[re.DestCount] + "nexthop: " + rt.rt_nexthop);
				}
				re.DestCount += 1;
				rt.rt_down();
				/* remove the RTEntry in RT*/
				aodv_removeRTEntry(rt.rt_dst);
			}
			/* remove the lost neighbor from all the precursor lists */
			rt.pc_delete(id);
		}
		if (re.DestCount > 0) {
			sendError(re, false);
		}
	}
	
	private void local_rt_repair(AODV_RTEntry rt, InetPacket p) {
		double now = getTime();
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
			debug(" repair route; dst: " + rt.rt_dst);
		}
		// Buffer the packet 
		aodv_pkt_enque(p);
		// mark the route as under repair 
		rt.rt_flags = AODV_RTEntry.RTF_IN_REPAIR;
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
			debug("sendRequest from local_rt_repair" + rt.rt_dst);
		}
		sendRequest(rt.rt_dst);
		// set up a timer interrupt
		local_repair_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_LOCAL_REPAIR, p);
		local_repair_EVT.handle = setTimeout( local_repair_EVT, rt.rt_req_timeout);
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Broadcast ID Management  Functions	
	//////////////////////////////////////////////////////////////////////////	
	protected void id_insert(long id_, int bid_) {
		double now = getTime();
		if (bcast_id_EVT == null) {
			bcast_id_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_BCAST_ID, null);
			if (bcast_id_EVT != null) {
				//System.out.println("bcast_id_Evt " + bcast_id_EVT);
				bcast_id_EVT.handle = setTimeout(bcast_id_EVT, BCAST_ID_SAVE);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
					debug(" set BCAST timeout: " + BCAST_ID_SAVE);
			} else 
				System.out.println("Error! Null bcast_id_EVT");
		}
		AODV_BroadcastID b = new AODV_BroadcastID(id_, bid_);
		b.expire = now + BCAST_ID_SAVE;
		bcast_id_list.addElement(b);
	}

	/* SRD */
	protected boolean id_lookup(long src_, int id_) {
		int no = bcast_id_list.size();
		for (int i = 0; i < no; i++) {
			AODV_BroadcastID id_entry = (AODV_BroadcastID) bcast_id_list.elementAt(i);
			if ((id_entry.src == src_) && (id_entry.id == id_))
				return true;
		}
		return false;
	}

	private void id_purge() {
		double now = getTime();
		for (int i = 0; i < bcast_id_list.size(); ) {
			AODV_BroadcastID id_entry = (AODV_BroadcastID) bcast_id_list.elementAt(i);
			if (id_entry.expire <= now)
				bcast_id_list.removeElement(id_entry);
			else
				i++;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////
	// The Routing Table Functions
	//////////////////////////////////////////////////////////////////////////
	protected AODV_RTEntry rt_lookup(long id_) {
		int no = aodv_rt_entry_list.size();
		for (int i = 0; i < no; i++) {
			AODV_RTEntry rt_entry = (AODV_RTEntry) aodv_rt_entry_list.elementAt(i);
			if (rt_entry.rt_dst == id_)
				return rt_entry;
		}
		return null;
	}

	private void rt_delete(long id_) {
		AODV_RTEntry rt = rt_lookup(id_);
		if(rt != null) {
			aodv_rt_entry_list.removeElement(rt);
		}
	}
	
	/* the setting of rt_expire is the responsibility of the caller */
	protected AODV_RTEntry rt_add(long id_) {
		double now = getTime();
		if ( route_EVT == null ) {
			route_EVT = new AODV_TimeOut_EVT(AODV_TimeOut_EVT.AODV_TIMEOUT_ROUTE, null);
			route_EVT.handle = setTimeout(route_EVT, ROUTE_CACHE_FREQUENCY);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug(" set Route timeout: " + ROUTE_CACHE_FREQUENCY + route_EVT);
		}
		AODV_RTEntry rt = rt_lookup(id_);
		if(rt != null) {
			return rt;
		}
		rt = new AODV_RTEntry(id_);
		aodv_rt_entry_list.addElement(rt);	
		return rt;
	}
	
	private void rt_purge() {
		double now = getTime();
		double delay = 0.0;

		int no = aodv_rt_entry_list.size();
		for (int i = 0; i < no; i++) {
			AODV_RTEntry rt = (AODV_RTEntry) aodv_rt_entry_list.elementAt(i);
			if ((rt.rt_flags == AODV_RTEntry.RTF_UP) && (rt.rt_expire < now)) {
				/* if a valid route has expired, purge all packets from send buffer and invalidate the route. */
				InetPacket pkt;
				while ((pkt = aodv_pkt_deque(rt.rt_dst)) != null) {
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_RERR) || isDebugEnabledAt(DEBUG_AODV))) {
						debug(" drop pkt dst: " + rt.rt_dst);
					}
					if(isGarbageEnabled()) drop(pkt, "DROP_RTR_NO_ROUTE");
				}
				rt.rt_seqno++;
				rt.rt_down();
				
				/* remove the RTEntry in RT*/
				aodv_removeRTEntry(rt.rt_dst);
			} else if (rt.rt_flags == AODV_RTEntry.RTF_UP) {
				/* If the route is not expired, and there are packets in the sendbuffer waiting, forward them. This should not be needed, but this extra check does no harm. */
				InetPacket buffered_pkt;
				while ((buffered_pkt = aodv_pkt_deque(rt.rt_dst)) != null) {
					aodv_delay_forward(rt, buffered_pkt, delay, false);
					delay += ARP_DELAY;
				}
			} else if (aodv_pkt_find(rt.rt_dst) == true) {
				/* If the route is down and there is a packet for this destination waiting in the sendbuffer, then send out route request. sendRequest will check whether it is time to really send out request or not.	This may not be crucial to do it here, as each generated packet will do a sendRequest anyway. */
				/*debug("sendRequest from rt_purge " + rt.rt_dst); */
				sendRequest(rt.rt_dst); 
			}
		}
	}

	/** add a routing entry with specification of distance, nect hop and the timeout value */
	public void aodv_addRTEntry(long dst_, long nexthop_, double timeout_)
	{
		aodv_addRTEntry(dst_, nexthop_, timeout_, 0 /*interface */);
	}

	/** add a routing entry with specification of distance, nect hop ,the timeout value and the interface id*/
	public void aodv_addRTEntry(long dst_, long nexthop_, double timeout_, int interface_)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE) || isDebugEnabledAt(DEBUG_AODV))
			debug("addRTEntry: dst" + dst_ +" nexthop " + nexthop_ + " timeout " + timeout_ + " if: " + interface_);
		// update the routing info. into RT
		RTKey key = new RTKey(0, 0, dst_, -1, 0, 0);
		//RTKey key = new RTKey(-1, 0, dst_, -1, -1, 0);
		AODV_RTEntry new_en = rt_lookup(dst_);
		// xxx: check whether interface is set correctly 
                /* Here we use longer timeout in RT since RT usually purges entries on time
                 * But in AODV, we check rt_purge only on every ROUTE_CACHE_FREQUENCY time,
                 * So, ther might cause inconsistency if we do not use longer timeout in RT since
                 * the entries in AODV RT might not be purged */
		addRTEntry(key, nexthop_, interface_ , new_en, timeout_ + 2*ROUTE_CACHE_FREQUENCY);
	}

	/** each rt_down is associated with an aodv_removeRTEntry, rt_down is called when:
	* (1) local repair timeout which then call nb_delete
	* (2) recvError
	* (3) handle_link_failure (which is called by nb_delete)
	* (4) rt_purge 
	*/
	public Object aodv_removeRTEntry(long dst) {
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ROUTE) || isDebugEnabledAt(DEBUG_AODV))
			debug("removeRTEntry: dst" + dst);
		return removeRTEntry(dst);
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Packet Queue Management Functions
	/////////////////////////////////////////////////////////////////////////
	private void aodv_pkt_enque(InetPacket p) {
		double now = getTime();
		/* Purge any packets that have timed out.*/
		aodv_pkt_purge();
 
		if (pkt_list.size() == pkt_queue_limit_) {
			AODV_Buffered_pkt pkt = aodv_pkt_remove_head();	// decrements len_
			if (pkt.expire > now ) {
				if(isGarbageEnabled()) drop(pkt, "DROP_RTR_NO_ROUTE");
			} else {
				if(isGarbageEnabled()) drop(pkt, "DROP_RTR_QTIMEOUT");
			}
		}
		AODV_Buffered_pkt new_pkt = new AODV_Buffered_pkt(p, now + pkt_queue_timeout_);	
		pkt_list.addElement(new_pkt);
	}

	protected InetPacket aodv_pkt_deque() {
		/* Purge any packets that have timed out.*/
		aodv_pkt_purge();

		AODV_Buffered_pkt p = (AODV_Buffered_pkt) pkt_list.firstElement();
		pkt_list.removeElementAt(0);
		return p.ipkt;
	}

	protected InetPacket aodv_pkt_deque(long dst) {
		/* Purge any packets that have timed out.*/
		aodv_pkt_purge();
		int no = pkt_list.size();
		for (int i = 0; i < no; i++) {	
			AODV_Buffered_pkt pkt_ = (AODV_Buffered_pkt) pkt_list.elementAt(i);
			if (pkt_.ipkt.getDestination() == dst) {
				pkt_list.removeElementAt(i);	
				return pkt_.ipkt;	
			}
		}
		return null;
	}

	private boolean aodv_pkt_find(long dst) {
		int no = pkt_list.size();
		for (int i = 0; i < no; i++) {	
			AODV_Buffered_pkt pkt_ = (AODV_Buffered_pkt) pkt_list.elementAt(i);
			if (pkt_.ipkt.getDestination() == dst) {	
				return true;	
			}
		}
		return false;
	}

	private AODV_Buffered_pkt aodv_pkt_remove_head() {
		if (pkt_list.size() == 0) return null;
		AODV_Buffered_pkt p = (AODV_Buffered_pkt) pkt_list.firstElement();
		pkt_list.removeElementAt(0);
		return p;
	}	

	private void aodv_pkt_purge() {
		double now = getTime();
		for (int i = 0; i < pkt_list.size(); ) {
			AODV_Buffered_pkt p = (AODV_Buffered_pkt) pkt_list.elementAt(i);
			if (p.expire <= now)
				pkt_list.removeElement(p);
			else
				i++;
		}
	}
	
	/////////////////////////////////////////////////////////////////////////	
	// Helper Functions
	//////////////////////////////////////////////////////////////////////////
	private double PerHopTime(AODV_RTEntry rt) {
		int num_non_zero = 0;
		double total_latency = 0.0;

		if (rt == null) return ((double) NODE_TRAVERSAL_TIME );
		for (int i=0; i < AODV_RTEntry.MAX_HISTORY; i++) {
			if (rt.rt_disc_latency[i] > 0.0) {
				num_non_zero++;
				total_latency += rt.rt_disc_latency[i];
			}
		}
		if (num_non_zero > 0)
			return(total_latency / (double) num_non_zero);
		else
			return((double) NODE_TRAVERSAL_TIME);
	}
    
    /**
     * Sets the seed of the random number generator.
     */
    public void setSeed(long seed) {
        rand.setSeed(seed);
    } 
    
}
