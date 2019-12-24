// @(#)OSPF.java   7/2003
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

package drcl.inet.protocol.ospf;

// xxx: support multiple areas (inter area routing)
// xxx: support multiple external AS routing
// xxx: support hierarchy address

import java.util.*;

import drcl.inet.core.*;
import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.inet.protocol.*;
import drcl.util.queue.TreeMapQueue;

/**
 *  The package, drcl.inet.protocol.OSPF, implements the link-state routing
 *  protocol, OSPF v2, Open Shortest Path First routing protocol and follow 
 *  the RFC2328. 
 *  In this implementation, we refered much from OSPF code in 
 *  <a href="http://www.zebra.org/">GNU Zebra project</a>, especially 
 *  <a href="ftp://ftp.zebra.org/pub/zebra/zebra-0.85.tar.gz">zebra 0.85</a>.
 *   
 *  <p>The general data structure can refer to figure 9 and section 5 RFC2328
 *  (If not specially mentioned, the following sections, figures, and etc. 
 *  can be  found in RFC 2328)
 * 
 *  @author Wei-peng Chen, Hung-ying Tyan 
 *  @see OSPF_Area 
 *  @see OSPF_DBdesc
 *  @see OSPF_Packet
 *  @see OSPF_Interface
 *  @see OSPF_LS_Database
 *  @see OSPF_LSA
 *  @see OSPF_LSA_Header
 *  @see OSPF_LSack
 *  @see OSPF_LSrequest
 *  @see OSPF_LSupdate
 *  @see OSPF_Neighbor
 *  @see OSPF_TimeOut_EVT
 */
public class OSPF extends drcl.inet.protocol.Routing implements UnicastRouting
{
	public String getName()
	{ return "ospf"; }

	public static boolean debug = false;
	public static boolean debug2 = false;

	/* OSPF packet types */
	/** A constant indicating a hello packet. */
	protected static final int HELLO = 0;
	/** A constant indicating a database description packet. */
	protected static final int DATABASE = 1;
	/** A constant indicating a link state request packet. */
	protected static final int LS_REQUEST = 2;
	/** A constant indicating a link state update packet. */
	protected static final int LS_UPDATE = 3;
	/** A constant indicating a link state acknowledge packet. */
	protected static final int LS_ACK = 4;

	static final String[] PKT_TYPES = { "HELLO", "DBDESC", "LS_REQ", "LS_UPDATE", "LS_ACK" };

	/* Neighbor state */
	final static int NBS_DOWN     = 0;
	final static int NBS_ATTEMPT  = 1; /* valid only for neighbors attached to MBMA */
	final static int NBS_INIT     = 2; /* received neighbor's hello, one way comm established */
	final static int NBS_TWOWAY   = 3;
	final static int NBS_EXSTART  = 4; /* 1st step to create adjancy between two neighbors */
	final static int NBS_EXCHANGE = 5;	
	final static int NBS_LOADING  = 6;
	final static int NBS_FULL     = 7;
	final static String[] NBR_STATES = {
		"DOWN",    "ATTEMP", "INIT",    "2WAY",
		"EXSTART", "XCHG",   "LOADING", "FULL"
	};

	/* interface state */
	final static int  IFS_NONE		= 0;
	final static int  IFS_DOWN		= 1;
	final static int  IFS_LOOPBACK	= 2;
	final static int  IFS_WAITING	= 3;
	final static int  IFS_PTOP		= 4;
	final static int  IFS_DROTHER	= 5;
	final static int  IFS_BDR		= 6;
	final static int  IFS_DR		= 7;
	final static int  IFS_MAX		= 8;
	final static String[] IF_STATES = {
		"NONE", "DOWN", "LOOPBACK", "WAITING",
		"POINT_TO_POINT", "DR_OTHER", "BDR", "DR", "MAX"
	};

	/* interface type */
	final static int  IF_PTOP		= 0;
	final static int  IF_PTOMP		= 1;
	final static int  IF_VIRTUAL	= 2;
	final static int  IF_BROADCAST	= 3;
	final static int  IF_NBMA		= 4;

	 /** acknowleadge type */
	static final int NO_ACK        = 0;
	static final int DELAYED_ACK   = 1;
	static final int DIRECT_ACK    = 2;

	final static int OSPF_MAX_LSA_SIZE	= 1500;
	final static int LS_REFRESH_TIME      = 1800;       /* 30 min */
	/** unit: second, used when a router receive two pkts with the same seq no. 
	 *  and checksum. Original value = 0, increase InfTransDelay on every hop of 
	 *  flooding procedure.  
	 */
	final static int MIN_LS_INTERVAL    =    5;
	final static int MIN_LS_ARRIVAL     =    1;
	final static int LSA_MAXAGE         = 3600;       /* 1 hour */
	final static int LSA_MAX_AGE_DIFF   =  900;       /* 15 min */

	/* LSA type */
	final static int OSPF_UNKNOWN_LSA		= -1;
	final static int OSPF_MIN_LSA			= 0;
	final static int OSPF_ROUTER_LSA		= 0;
	final static int OSPF_NETWORK_LSA		= 1;
	final static int OSPF_SUMMARY_LSA		= 2;
	final static int OSPF_SUMMARY_LSA_ASBR	= 3;
	final static int OSPF_AS_EXTERNAL_LSA	= 4;
	final static int OSPF_MAX_LSA			= 5;

	/* lsa scope */
	static final int  SCOPE_LINKLOCAL  = 0;
	static final int  SCOPE_AREA       = 2;
	static final int  SCOPE_AS         = 4;
	static final int  SCOPE_RESERVED   = 6;

	/* OSPF LSA Link Type. */
	final static int LSA_LINK_TYPE_POINTOPOINT	= 1;
	final static int LSA_LINK_TYPE_TRANSIT		= 2;
	final static int LSA_LINK_TYPE_STUB			= 3;
	final static int LSA_LINK_TYPE_VIRTUALLINK	= 4;

	final static long CONTROL_PKT_TYPE = drcl.inet.InetPacket.CONTROL; // for ToS field

	/** OSPF router ID of this node */
	protected int	router_id = Integer.MAX_VALUE;
	/** The list of OSPF areas to which this router is attached */
	protected Vector	area_list;
	/** The list of OSPF Interfaces to which this router is attached */
	protected Vector   ospf_if_list;

	/** The delayed flood event. */
	// cases of delayed lsa flood are rare, and should only occur to self-originated lsa's
	// because lsa's received from others are already constrained
	// so this object only holds an area and the area's "router_lsa_self" is the lsa to be flooded
	OSPF_TimeOut_EVT flood_EVT = new OSPF_TimeOut_EVT(OSPF_TimeOut_EVT.OSPF_TIMEOUT_DELAY_FLOOD, null);
	/** The lsa maxage reached event */
	// instead of setting up a timeout event for each lsa
	// we use a common timeout event to control event
	OSPF_TimeOut_EVT lsa_maxage_EVT = new OSPF_TimeOut_EVT(OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSMAXAGE_REACH,
		null);



	
	// xxx
	//protected OSPF_Area backbone;		/* equals to area_list[0] */

	Port ifport = createIFQueryPort();  // for Interface query service
	Port idport = createIDServicePort(); // for ID lookup service
	
	// For hello message
	{
		createIFEventPort(); // listening to neighbor events
	}

	/** Debug level to enable sampled debug messages. */
	public static final int DEBUG_SAMPLE   = 0;
	/** Debug level of neighbor state changes. */
	public static final int DEBUG_NEIGHBOR = 1;
	/** Debug level of sending ospf packets. */
	public static final int DEBUG_SEND     = 2;
	/** Debug level of calculating SPF tree and installing route entries. */
	public static final int DEBUG_SPF      = 3;
	/** Debug level of LSA refresh events. */
	public static final int DEBUG_REFRESH  = 4;
	/** Debug level of handling LSA update packets. */
	public static final int DEBUG_LSA      = 5;
	/** Debug level of handling LSA acknowledgement packets. */
	public static final int DEBUG_ACK      = 6;
	/** Debug level of handling timeout events. */
	public static final int DEBUG_TIMEOUT  = 7;
	/** Debug level of detailed messages for other levels. */
	public static final int DEBUG_DETAIL   = 8;
	static final String[] DEBUG_LEVELS = {
		"sample", "neighbor", "send", "spf", "refresh", "lsa", "ack", "timeout", "detail"
	};
	public String[] getDebugLevelNames()
	{ return DEBUG_LEVELS; }

	/** Constructor. */
	public OSPF(){
		super();
		area_list    = new Vector();
		ospf_if_list = new Vector();
	}

	public String info()
	{
		int now_ = (int)getTime();
		StringBuffer sb_ = new StringBuffer();
		sb_.append("Areas(area_list):");
		if (area_list == null || area_list.size() == 0)
			sb_.append("<none>\n");
		else {
			sb_.append("\n");
			for (int i=0; i<area_list.size(); i++) {
				OSPF_Area area_ = (OSPF_Area)area_list.elementAt(i);
				if (area_ == null) continue;
				if (area_.ls_db != null) area_.ls_db.ospf_update_age(now_);
				sb_.append("   Area" + i + ": " + area_.info("      "));
			}
		}
		sb_.append("Interfaces(ospf_if_list):");
		if (ospf_if_list == null || ospf_if_list.size() == 0)
			sb_.append("<none>\n");
		else {
			sb_.append("\n");
			for (int i=0; i<ospf_if_list.size(); i++) {
				OSPF_Interface if_ = (OSPF_Interface)ospf_if_list.elementAt(i);
				if (if_ == null) continue;
				sb_.append("   IF" + i + ": " + if_.info("      "));
			}
		}
		return sb_.toString();
	}
	
	public void reset()
	{
		super.reset();
		router_id = Integer.MAX_VALUE;
		for (int i=0; i<area_list.size(); i++) {
			OSPF_Area area_ = (OSPF_Area)area_list.elementAt(i);
			if (area_ != null) _area_clear(area_);
		}
		//area_list.removeAllElements();
		ospf_if_list.removeAllElements();
		if (flood_EVT.handle != null) {
			cancelTimeout(flood_EVT.handle);
			flood_EVT.setObject(null);
			flood_EVT.handle = null;
		}
		if (lsa_maxage_EVT.handle != null) {
			cancelTimeout(lsa_maxage_EVT.handle);
			lsa_maxage_EVT.setObject(null);
			lsa_maxage_EVT.handle = null;
		}
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		OSPF that_ = (OSPF)source_;
		area_list.removeAllElements();
		area_list.setSize(that_.area_list.size());
		for (int i=0; i<area_list.size(); i++) {
			OSPF_Area area_ = (OSPF_Area)that_.area_list.elementAt(i);
			area_list.setElementAt(area_.clone(), i);
		}
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
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_TIMEOUT)))
			debug("__timeout__ " + evt_);

		OSPF_Interface		oif ;
		OSPF_Neighbor		nbr = null;
		OSPF_LSA			lsa = null;
		
		int type = ((OSPF_TimeOut_EVT)evt_).EVT_Type;
		double stime = getTime();

		switch(type) {
		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSMAXAGE_REACH:
			if (evt_ != lsa_maxage_EVT) {
				error("timeout()", " ** ERROR ** where does this come from? " + evt_);
				break;
			}
			lsa = (OSPF_LSA) lsa_maxage_EVT.getObject();
			if (lsa == null) break;
			ospf_lsa_expire(lsa);
			ospf_schedule_next_maxage(stime);
			break;

		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_DELAY_FLOOD:
			if (evt_ != flood_EVT) {
				error("timeout()", " ** ERROR ** where does this come from? " + evt_);
				break;
			}
			OSPF_Area area = (OSPF_Area)flood_EVT.getObject();
			if (area.delayFlood)
				ospf_router_lsa_flood(area, false);
			ospf_schedule_next_flood(stime);
			break;

		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_HELLO:
			oif = (OSPF_Interface) ((OSPF_TimeOut_EVT)evt_).getObject();
			ospf_send_hello(oif);
			break;

		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_LS_REFRESH: 
			lsa = (OSPF_LSA) ((OSPF_TimeOut_EVT)evt_).getObject();
			lsa_refresh( lsa, (int)stime );
			break;

/*
		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSMAXAGE_REACH: 
			lsa = (OSPF_LSA) ((OSPF_TimeOut_EVT)evt_).getObject();
			ospf_lsa_expire(lsa);
			break;
			*/

		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_ACK_DELAY_REACH: 
			oif = (OSPF_Interface) ((OSPF_TimeOut_EVT)evt_).getObject();
			ospf_send_lsack_delayed(oif);
			break;
			
		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_NBR_INACTIVE:
			nbr = (OSPF_Neighbor) ((OSPF_TimeOut_EVT)evt_).getObject();
			inactivity_timer(nbr);
			break;			
		
		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_DBDESC_RETRANS: 
			nbr = (OSPF_Neighbor) ((OSPF_TimeOut_EVT)evt_).getObject();
			ospf_send_dbdesc_retrans(nbr);
			break;
		
		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSUPDATE_RETRANS: 
			nbr = (OSPF_Neighbor) ((OSPF_TimeOut_EVT)evt_).getObject();			
			ospf_send_lsupdate_retrans(nbr);
			break;

		case OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSREQ_RETRANS: 
			nbr = (OSPF_Neighbor) ((OSPF_TimeOut_EVT)evt_).getObject();			
			ospf_send_lsreq_retrans(nbr);
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
	 * According to the different packet types ( OSPF Hello pkt, Database descricption
	 * pkt,Link State update pkt, LS request pkt, or LS ack pkt), different 
	 * corresponding methods can handle the packet.
	 * Note for OSPF_Hello pkt will be received only once. The other hello maintaince
	 * is done by drcl.inet.core.Hello
	 *  
	 * @param date_ message body arriving at the down port.
	 * @param downPort_ down port at which messages arrive.
	 */
	public void dataArriveAtDownPort(Object data_, drcl.comp.Port downPort_) 
	{
		InetPacket ipkt_ = (InetPacket)data_;
		OSPF_Packet pkt_ = (OSPF_Packet)ipkt_.getBody();
		OSPF_Interface oif = null;
		int incoming_    = ipkt_.getIncomingIf();
		if (incoming_ >= ospf_if_list.size()
			|| (oif = (OSPF_Interface) ospf_if_list.elementAt(incoming_)) == null) {
			if (isErrorNoticeEnabled())
				error(data_, "dataArriveAtDownPort()", downPort_, "data received on nonexistent if");
			return;
		}

		int src_ = pkt_.getRouterID(); /* router ip addr of the source */
		
		// xxx
		/* check the OSPF heaqder */
		/*if( !hdr_.check_validity()) {
			return;
		}*/
		int pkt_type = pkt_.getType();
		int rt_id = pkt_.getRouterID();
		int area_id = pkt_.getAreaID();		

		// if the packet is OSPF pkt
		switch (pkt_type) {
			/* OSPF_Hello will not be used, since we rely on the underling
			Hello protocol to implement the hello service */
			case HELLO:
				OSPF_Hello hello =(OSPF_Hello) pkt_.getBody();
				Process_Hello(hello, oif, src_);
				break;
			case DATABASE:
				OSPF_DBdesc dbdesc =(OSPF_DBdesc) pkt_.getBody();
				Process_DBdesc(dbdesc, oif, src_ );
				break;
			case LS_REQUEST:
				OSPF_LSrequest lsreq = (OSPF_LSrequest) pkt_.getBody();
				Process_LSrequest(lsreq, oif, src_ );
				break;
			case LS_UPDATE:
				OSPF_LSupdate lsupdate = (OSPF_LSupdate) pkt_.getBody();
				Process_LSupdate(lsupdate, oif, src_ );
				break;
			case LS_ACK:
				OSPF_LSack lsack = (OSPF_LSack) pkt_.getBody();
				Process_LSack(lsack, oif, src_ );
				break;
		}
	}

	/**
	 * Called back when a neighbor up event is received.
	 * Create a OSPF_Neighbor data associated with OSPF_Interface data in this router.
	 * 
	 * @param ifindex_ index of the interface.
	 * @see drcl.inet.core.Hello
	 */
	protected void neighborUpEventHandler(int ifindex_, NetAddress neighbor_, Port inPort_)
	{
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_NEIGHBOR)))
			debug("NEIGHBOR_UP_EVENT: " + neighbor_ + " at if " + ifindex_);
		//Packet p_ = new Packet(DVnew PacketRequest(), null);
		//forward(p_, Address.NULL_ADDR, neighbor_.getAddress(), false, MAX_HOP, InetPacket.CONTROL, ifindex_);
		//setTimeout(REGULAR_UPDATE_TIMER, REGULAR_TIMEOUT);
		
		// xxx: check timeout value
		if (router_id == Integer.MAX_VALUE)
			router_id = (int) IDLookup.getDefaultID(idport);

		long src = neighbor_.getAddress();
		OSPF_Neighbor nbr = null;
		OSPF_Interface oif = null;
		if (ifindex_ < ospf_if_list.size())
			oif	= (OSPF_Interface ) ospf_if_list.elementAt( ifindex_ );
		
		if (oif != null) {
			nbr = oif.ospf_nbr_lookup_by_routerid( src );
			if( nbr != null) {
				/* this neighbor has shown up in the neighbor list */
				return;
			}
		} else {
			// xxx:  area id, this version we only implement intra area routing
			// all area id are set to be 1 temporarily
			int area_id = 1;
			int area_no = area_list.size();
			int i;
			
			InterfaceInfo iinfo_ = drcl.inet.contract.IFQuery.getInterfaceInfo( ifindex_, ifport); 
					  
			OSPF_Area area_ = null;
			for ( i = 0; i< area_no; i++) {
				area_ = (OSPF_Area) area_list.elementAt(i);
				if ( area_.area_id == area_id) {
					oif = OSPF_Interface.ospf_interface_create( ifindex_, iinfo_.getType(),
						iinfo_.getMTU(), area_, ifport);
					area_.if_list.addElement( oif );
					break;
				}
			}

			if (area_ == null) 
				error("neighborUpEventHandler()", "can not find corresponding area");

			int no = ospf_if_list.size();
			if( ifindex_ >= no) {
				for ( i = no ; i < ifindex_ ; i++)
					ospf_if_list.insertElementAt( null, i );
			} else {
				/* current position is occupied by a null */
				ospf_if_list.removeElementAt( ifindex_ );
			}
			ospf_if_list.insertElementAt( oif, ifindex_ );
		}
		// bring up the interface
		oif.Interface_Up();

		// create the new neighbor
		nbr = new OSPF_Neighbor( (int) src, oif);
		oif.add_new_neighbor(nbr);
		
		_nbr_Hello_Received(nbr, "NeighborUp");														  

		ospf_send_single_hello(oif, src);
	}

	protected void neighborDownEventHandler(int ifindex_, NetAddress neighbor_, Port inPort_)
	{
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_NEIGHBOR)))
			debug("NEIGHBOR DOWN EVENT: " + neighbor_);
		OSPF_Interface oif = (OSPF_Interface ) ospf_if_list.elementAt( ifindex_ );
		OSPF_Neighbor nbr = null;
		long src = neighbor_.getAddress();

		if (oif != null) {
			// change oif state to Down
			oif.Interface_Down();

			nbr = oif.ospf_nbr_lookup_by_routerid( src );
			if( nbr != null) {
				_nbr_state_change (nbr, NBS_DOWN, "> NeighborDown <");
				_nbr_clear(nbr);
				/* remove this neighbor from neighbor list */
				oif.remove_neighbor(nbr);
				nbr.ospf_interface.neighbor_change();
				return;
			}
		}
		return;
	}

	/**
	 * 	Process OSPF_Hello message, but will be called only once since we rely on the 
	 *  underlying Hello, drcl.inet.core.Helllo to fulfill the peridocal hello service
	 * 	The purpose of this function is to maintain the state of the neighbor
	 * 	ref: sec. 10.5
	 * 	@param hello	OSPF_Hello pkt
	 *	@param oif		OSPF_Interface from which the pkt arrives
	 *	@param src		the address of the router which originats this packet 
	 */
	private void Process_Hello(OSPF_Hello hello, OSPF_Interface oif, int src) {
		int twoway = 0;
		int i;
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("----- LS_HELLO_RECEIVED at if " + oif.if_id + " from " + src + ": " + hello);
			
		/* HelloInterval check */
		int hello_interval = hello.get_hello_interval();
		if (hello_interval!= oif.hello_interval) {
			if (isErrorNoticeEnabled())
				error("Process_Hello()", "*** Warn *** : HelloInterval mismatch");
			return;
		}

		/* RouterDeadInterval check */
		int router_dead_interval = hello.get_router_dead_interval();
		if (router_dead_interval!= oif.dead_interval) {
			if (isErrorNoticeEnabled())
				error("Process_Hello()", "*** Warn *** : RouterDeadInterval mismatch with ");
			return;
		}

		/* check options */
		/* Ebit */
		/*my_options = o6if->area->options;
		if (ospf6_opt_is_mismatch (V3OPT_E, hello->options, my_options))  {
		      zlog_warn ("Ebit mismatch with %s", rtrid_str);
		      ospf6_message_clear_buffer (MSGT_HELLO, iov);
		      return;
		}*/

		/* fot point-to-point link, the source is identified by the router id */
		/* search if the source is in the neighbor list, if not create a new one */
		OSPF_Neighbor nbr = oif.ospf_nbr_lookup_by_routerid( src );
		if ( nbr == null ) {
			// set the state of the newly created neighbor be Down
		    nbr = new OSPF_Neighbor( src, oif);
			/*nbr->prevdr = nbr->dr = hello->dr;
			nbr->prevbdr = nbr->bdr = hello->bdr;
			nbr->rtr_pri = hello->rtr_pri;
			memcpy (&nbr->hisaddr, src, sizeof (struct sockaddr_in6));*/
		}
		
		// update the state machine of the interface and the neighbor
		
		/* Examine the list of the neighbor contained in the hello packet to
		   see if it is two_way_received or one_waAy_received, TwoWay check */
		int nbr_no = hello.neighbor_no;	
		for (i = 0; i < nbr_no; i++) {
			// check if the router id in the hello pkt is the same as mine 
			if ( ((Integer)hello.neighbor_id_list.elementAt(i)).intValue() == router_id) {
				twoway = 1;
				break;
			}
		}
		
		/* execute neighbor events */
		_nbr_Hello_Received(nbr, "HelloReceived");
		//if (isDebugEnabled()) debug("Neighbor Event : *HelloReceived* from " + rtr_id );

		/* Since we rely on drcl.inet.core.Hello to do peridical hello service, the
		timer below is not used*/
		// Start the inactivity timer
/*		if (nbr.Nbr_Inactive_EVT != null) {
			cancelTimeout(nbr.Nbr_Inactive_EVT);
			nbr.Nbr_Inactive_EVT = null;
		}*/
		
		if (twoway == 1 )
			_nbr_Twoway_Received( nbr );
		/*else
			thread_execute (master, oneway_received, nbr, 0);*/

/*		OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_NBR_INACTIVE, nbr );
		nbr.Nbr_Inactive_EVT = evt_;
		setTimeout(evt_ , router_dead_interval);*/
		return ;
	}	

	/* not used, replaced by neighborDownEventHandler function */
	protected int inactivity_timer ( OSPF_Neighbor nbr)
	{
		/* statistics */
		nbr.ospf_stat_inactivity_timer++;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR)) 
			debug("Neighbor Event: *InactivityTimer*: self" + router_id + ",nbr_id" + nbr.rtr_id );

		nbr.Nbr_Inactive_EVT = null;
		/*nbr->dr = nbr->bdr = nbr->prevdr = nbr->prevbdr = 0;*/
		_nbr_state_change (nbr, NBS_DOWN,  "InactivityTimer");
		_nbr_clear(nbr);
		nbr.ospf_interface.neighbor_change();
		return 0;
	}

	/**
	 *  determine who is the master by looking at the router id 
	 *  return 1 if I am the master 
	 * */				  
	private int  dbdesc_is_master (OSPF_Neighbor nbr) {
		if (nbr.rtr_id == router_id) {
			if (isErrorNoticeEnabled())
				error("dbdesc_is_master()", "*** neighbor router id is the same of mine");
			return -1;
		} else if (nbr.rtr_id > router_id)
			return 0;
		return 1;
	}

	/**
	 *  Process_DBdesc:
	 *  Based on router id, determine I am the master or the slave during the database
	 *  exchange process.
	 *  ref: sec: 10.6
	 */
	private void Process_DBdesc (OSPF_DBdesc dbdesc, OSPF_Interface oif, long src)
	{
		int Im_master = 0;
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("----- LS_DBdesc_RECEIVED at if " + oif.if_id + " from " + src + ": " + dbdesc);

		/* find neighbor. if cannot be found, reject this message */
		OSPF_Neighbor nbr = oif.ospf_nbr_lookup_by_routerid( src );
		if ( nbr == null ) {
			/*if (IS_OSPF6_DUMP_DBDESC)
				pitchone ("neighbor not found, reject");*/
			return;
		}

		/* interface mtu check */
		/* xxx */

		/* check am I master */
		Im_master = dbdesc_is_master (nbr);
		if (Im_master < 0) {
			return; /* can't decide which is master, return */
		}

		/*if (isDebugEnabled()) debug("dbdesc_master" + "from" + nbr.rtr_id + "M:" + dbdesc.dd_mbit 
			+ "I: " + dbdesc.dd_ibit + "seq:" + dbdesc.dd_seqnum + "nbr_seq :" + nbr.dd_seqnum + "state" + nbr.state ); */
									
		switch (nbr.state) {
			case NBS_DOWN:
			case NBS_ATTEMPT:
			case NBS_TWOWAY:
			    //if (isDebugEnabled()) debug("DbDesc from " + nbr.rtr_id + " Ignored: state less than Init");
		        return;

			case NBS_INIT:
				_nbr_Twoway_Received( nbr);
				if (nbr.state != NBS_EXSTART) {
					/*if (isDebugEnabled())
						debug("DbDesc from " + nbr.rtr_id + " Ignored: state less than ExStart");*/
				    return;
			    }
				/* else fall through to ExStart */
			
			case NBS_EXSTART:
				if ( Im_master == 1) {
					// ack as master
					if (dbdesc.dd_msbit == 0 && dbdesc.dd_ibit == 0 && dbdesc.dd_seqnum == nbr.dd_seqnum) {
						// go below
					} else {
						if ( dbdesc.dd_ibit == 0) 
							if (isErrorNoticeEnabled())
								error("Process_DBdesc()", "rt_id " + router_id +" negotiation failed with "
									+ nbr.rtr_id + dbdesc + "nbr_seq :" + nbr.dd_seqnum );
						return;
					}
				}
				else {
					// ack as slave
					if ( dbdesc.dd_ibit == 1 && dbdesc.dd_mbit == 1 && dbdesc.dd_msbit == 1/*&& iov_count (iov) == 1*/) {
						// first time to receive the dbdesc pkt
						/* Master/Slave bit set to slave */
						nbr.dd_msbit = 0;
						/* Initialize bit clear */
						nbr.dd_ibit = 0;
		            
						/* sequence number set to master's */
					    nbr.dd_seqnum = dbdesc.dd_seqnum;
					} else {
						if (isErrorNoticeEnabled())
							error("Process_DBdesc()", "negotiation failed");
						return;
					}
				}
				nbr.prepare_neighbor_lsdb ( (int)getTime() );
				_nbr_state_change (nbr, NBS_EXCHANGE, "NegotiationDone");
				nbr.dd_ibit = 0;
				/*if (nbr->thread_dbdesc_retrans)
					thread_cancel (nbr->thread_dbdesc_retrans);
					nbr->thread_dbdesc_retrans = (struct thread *) NULL;
					thread_add_event (master, negotiation_done, nbr, 0); */
				break;

			case NBS_EXCHANGE: 
				/* duplicate dbdesc dropped by master */
				if ( (nbr.last_dd != null) && (OSPF_DBdesc.isduplicate( dbdesc, nbr.last_dd ) == 1) ) {
					//if (isDebugEnabled()) debug("duplicate dbdesc, drop");
					// ospf6_message_clear_buffer (MSGT_DATABASE_DESCRIPTION, iov);
					if (Im_master != 1) {
						// slave
						if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
							debug("duplicate dbdesc, retransmit dbdesc");
						ospf_send_dbdesc_retrans(nbr);
					}
					return;
				}
				
				/* check Initialize bit and Master/Slave bit */
				if ( dbdesc.dd_ibit == 1) {
					if (isErrorNoticeEnabled())
						error("Process_DBdesc()", "Initialize bit mismatch");
					//thread_add_event (master, seqnumber_mismatch, nbr, 0);
					_nbr_Seqnumber_Mismatch( nbr );
					return;
				}
				
				if ( Im_master + dbdesc.dd_msbit != 1 ) {
					if (isErrorNoticeEnabled())
						error("Process_DBdesc()", "Master/Slave bit mismatch");
					_nbr_Seqnumber_Mismatch( nbr );
					return;
				}

				/* dbdesc option check */ /* not implemented */
				/*if (memcmp (dbdesc->options, nbr->last_dd.options, sizeof (dbdesc->options))) {
					if (IS_OSPF6_DUMP_DBDESC)
						zlog_info ("dbdesc option field changed");
					thread_add_event (master, seqnumber_mismatch, nbr, 0);
					if (Im_master != 1)
            			ospf6_message_clear_buffer (MSGT_DATABASE_DESCRIPTION, iov);
					return;
				}*/

				/* dbdesc sequence number check */
				if (dbdesc.dd_seqnum != nbr.dd_seqnum + (1-Im_master)) {
					if (isErrorNoticeEnabled())
						error("Process_DBdesc()", "*** dbdesc seqnumber mismatch:"
							+ (nbr.dd_seqnum +1-Im_master) + " expected");
					_nbr_Seqnumber_Mismatch( nbr );
					return;
				}
				break;

			case NBS_LOADING:
			case NBS_FULL:
				/* duplicate dbdesc dropped by master */
				if( OSPF_DBdesc.isduplicate( dbdesc, nbr.last_dd ) == 1) {
					//if (isDebugEnabled()) debug("duplicate dbdesc, drop");
					/* duplicate dbdesc cause slave to retransmit */
					if (Im_master != 1) ospf_send_dbdesc(nbr);
					return;
				} else {
					//if (isDebugEnabled()) debug("not duplicate dbdesc in state %d" +nbr.state);
					//	thread_add_event (master, seqnumber_mismatch, nbr, 0);
					_nbr_Seqnumber_Mismatch ( nbr );
					return;
				}

			default:
				// error
				break; /* not reached */
		}

		/* process LSA headers */
		if ( _nbr_check_lsdb (nbr, dbdesc ) < 0) {
			/* one possible situation to come here is to find as-external
			  lsa found when this area is stub */
			//thread_add_event (master, seqnumber_mismatch, nbr, 0);
			_nbr_Seqnumber_Mismatch( nbr );
			return;
		}

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("   I'm " + (Im_master == 1? "master": "slave"));
		if (Im_master == 1) {
			// act as master
			/* increment dbdesc seqnum */
			nbr.dd_seqnum++;

			/* more bit check */
			if ( dbdesc.dd_mbit == 0 && nbr.dd_mbit == 0 ) {
				//thread_add_event (master, exchange_done, nbr, 0);
				_nbr_Exchange_Done( nbr );
				
				if ( nbr.LSDBdesc_Retrans_EVT != null) {
					cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
					nbr.LSDBdesc_Retrans_EVT = null;
				} 
			} else {
				// thread_add_event (master, ospf6_send_dbdesc, nbr, 0);
				ospf_send_dbdesc(nbr);
			}
			/* save last received dbdesc , and free */
			//memcpy (&nbr->last_dd, dbdesc, sizeof (struct ospf6_dbdesc));
			nbr.last_dd = dbdesc;
		}
		else {
			// act as slave
			/* set dbdesc seqnum to master's */
			nbr.dd_seqnum = dbdesc.dd_seqnum ;

			/* save last received dbdesc , and free */
			//memcpy (&nbr->last_dd, dbdesc, sizeof (struct ospf6_dbdesc));
			nbr.last_dd = dbdesc;

			//thread_add_event (master, ospf6_send_dbdesc, nbr, 0);
			ospf_send_dbdesc( nbr );
		}
	}
	
	/* ref: A.3.4 & sec. 10.7 */
	private void Process_LSrequest (OSPF_LSrequest req, OSPF_Interface oif, long src)
	{
		Object scope;
		int	DEFAULT_INTERFACE_MTU = oif.ifmtu;
		int left_len = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN;
		int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
		int lsanum = 0;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("----- LS_REQ_RECEIVED at if " + oif.if_id + " from " + src + ": " + req);
		/* find neighbor. if cannot be found, reject this message */
		OSPF_Neighbor nbr = oif.ospf_nbr_lookup_by_routerid( src );
		if (nbr == null) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
				debug("neighbor not found, reject");
			return;
		}

		/* if neighbor state less than ExChange, reject this message */
		if (nbr.state < NBS_EXCHANGE) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
				debug("neighbor state less than Exchange, reject");
			return;
		}

		/* process each request */
		int req_num = req.ls_req_num;
		int i;
		/* create an update packet */
		OSPF_LSupdate lsupdate = new OSPF_LSupdate(req_num);
		int transdelay_ = nbr.ospf_interface.transdelay;
		int now_ = (int)getTime();

		for( i = 0; i < req_num; i++) {
			OSPF_LSrequest.LSrequest_body lsreq = (OSPF_LSrequest.LSrequest_body) req.req_list.elementAt(i);

			/* get scope from request type */
			switch ( Util.translate_scope(lsreq.lsreq_type)) {
				/*case OSPF_LSA_Header.SCOPE_LINKLOCAL:
					scope = (OSPF_Interface) nbr.ospf_interface;
					break;*/
				case SCOPE_AREA	:
					scope = (OSPF_Area) nbr.ospf_interface.area;
					break;
	/*			case OSPF_LSA_Header.SCOPE_AS:
				    scope = (void *) nbr->ospf6_interface->area->ospf6;
					break;*/
				/*case OSPF_LSA_Header.SCOPE_RESERVED:*/
				default:
					if (isErrorNoticeEnabled())
						error("Process_LSrequest()", " *** Warn *** : unsupported type requested, ignore");
				    // XFREE (MTYPE_OSPF6_MESSAGE, lsreq);
					continue;
			}
			/*if (IS_OSPF6_DUMP_LSREQ)
				zlog_info ("  %s", print_lsreq (lsreq));*/

			/* find instance of database copy */
			OSPF_LSA lsa = ospf_lsdb_lookup (lsreq.lsreq_type, lsreq.lsreq_id, lsreq.lsreq_advrtr, scope);
			if (lsa == null) {
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
					debug("requested " + lsreq + " not found, BadLSReq");
				// thread_add_event (master, bad_lsreq, nbr, 0);
				_nbr_Bad_LSreq( nbr );
				return;
			}
			
			/* test if the packet space can afford one more updated lsa before adding it*/
			if (left_len >= lsa.size() ) {
				/* okay, add it */
				OSPF_LSA clone_ = (OSPF_LSA)lsa.clone();
				clone_.header.ospf_age_update_to_send(transdelay_, now_);
				lsupdate.addlsa(clone_);
				// Note: dont add these lsa's to retranslist because request will be retransmitted
				// 		if the reply is lost
				left_len -= lsa.size();
				pkt_len += lsa.size();
				lsanum++;
			} else {
				/* not enough space, must send it */
				ospf_message_send( LS_UPDATE, lsupdate, pkt_len, nbr.rtr_id, nbr.ospf_interface);
				lsupdate = new OSPF_LSupdate(req_num - lsanum);
				left_len = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN;
				pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
				lsanum = 0;	
			}
		}

		/* send the remaining LSUpdate */
		if ( lsanum > 0 ) {
			ospf_message_send( LS_UPDATE, lsupdate, pkt_len, nbr.rtr_id, nbr.ospf_interface);
	    }

		return;
	}

	private void Process_LSupdate (OSPF_LSupdate lsupdate, OSPF_Interface oif, long src)
	{
		OSPF_Neighbor nbr;

		/* assert interface */
		if ( oif == null ) {
			// error
			return;
		}
	
		/* find neighbor. if cannot be found, reject this message */
		nbr = oif.ospf_nbr_lookup_by_routerid ( src );
		if (nbr == null) {
			if (isErrorNoticeEnabled())
				error("Process_LSupdate()", "neighbor not found, reject");
			return;
		}

		/* if neighbor state less than ExChange, reject this message */
		if (nbr.state < NBS_EXCHANGE) {
			if (isErrorNoticeEnabled())
				error("Process_LSupdate()", "neighbor state less than Exchange, reject");
			return;
	    }

		/* save linkstate update info */
		long lsanum = lsupdate.lsupdate_num;

		/* statistics */
		/*nbr->ospf6_stat_received_lsa += lsanum;
		nbr->ospf6_stat_received_lsupdate++;*/

		/* process LSAs */
		int i ;
		for (i = 0; i < lsanum; i++) { 
			OSPF_LSA lsa = (OSPF_LSA) lsupdate.LSA_list.elementAt(i);
			lsa_receive ( lsa, nbr);
		}
		return;
	}
		
	/* ref: A.3.6 & Sec. 13.7 */
	private void Process_LSack ( OSPF_LSack ack, OSPF_Interface oif, long src)
	{
		Object scope;
		int	DEFAULT_INTERFACE_MTU = oif.ifmtu;
		int leftlen = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN;
		int lsanum = 0;
		OSPF_LSA_Header		lsa_hdr;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ACK))
			debug("----- LS_ACK_RECEIVED at if " + oif.if_id + " from " + src + ": " + ack);
		
		/* find neighbor. if cannot be found, reject this message */
		OSPF_Neighbor nbr = oif.ospf_nbr_lookup_by_routerid ( src );
		if (nbr == null) {
			if (isErrorNoticeEnabled())
				error("Process_LSack()", "neighbor not found, reject");
			return;
		}
		/* if neighbor state less than ExChange, reject this message */
		if (nbr.state < NBS_EXCHANGE) {
			if (isErrorNoticeEnabled())
				error("Process_LSack()", "neighbor state less than Exchange, reject");
			return;
		}
		int now_ = (int)getTime();
		int no = ack.lsack_num;
		int i;
		/* process each LSA header */
		for ( i = 0; i < no; i++) {
			/* make each LSA header treated as LSA */
			lsa_hdr = (OSPF_LSA_Header) ack.LSA_hdr_list.elementAt(i);
			OSPF_LSA lsa = new OSPF_LSA(lsa_hdr);
			ospf_set_birth(lsa, now_);
			lsa.from = nbr;

			/* get scope from request type */
			switch ( Util.translate_scope(lsa_hdr.lsh_type)) {
				/*case OSPF_LSA_Header.SCOPE_LINKLOCAL:
					scope = (OSPF_Interface) nbr.ospf_interface;
					break;*/
				case SCOPE_AREA	:
					scope = (OSPF_Area) nbr.ospf_interface.area;
					break;
	/*			case OSPF_LSA_Header.SCOPE_AS:
				    scope = (void *) nbr->ospf6_interface->area->ospf6;
					break;*/
				/*case OSPF_LSA_Header.SCOPE_RESERVED:*/
				default:
					if (isErrorNoticeEnabled())
						error("Process_LSack()", " Warn : unsupported scope acknowledge, ignored: type="
						+ lsa_hdr.lsh_type);
					//ospf6_lsa_unlock (lsa);
					continue;
			}

			OSPF_LSA copy = nbr.ospf_lookup_retrans(lsa_hdr);

			/* if not on his retrans list, just ignored */
			if ( copy  == null) {
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ACK))
					debug("not found in " + nbr.rtr_id + "'s retranslist, ack ignored:" + lsa_hdr);
				continue;
			}

			/* if the same instance, remove from retrans list. else, log and ignore */
			if ( Util.ospf_lsa_check_recent (lsa, copy, now_) == 0) {
				_nbr_remove_retrans (copy, nbr);

				// Tyan: 05/08/2001
				// should also remove the copy in ls_db if the lsa's age reaches MAXAGE
				if (copy.header.lsh_age == OSPF.LSA_MAXAGE
					&& !_nbr_in_any_retranslist(copy)) { // sec. 14
					nbr.ospf_interface.area.ls_db.ospf_lsdb_remove(copy);
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_ACK)))
						debug("remove MAXAGE lsa from " + nbr.rtr_id + "'s retranslist AND database: "
							+ copy);
				}
				else {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_ACK))
						debug("remove lsa from " + nbr.rtr_id + "'s retranslist: " + copy);
				}
			}
			else {
				/* Log the questionable acknowledgement, and examine the next one. */
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_ACK)))
					debug("*** Warn *** : questionable acknowledge: differ database copy, copy_in_db:"
						+ copy + ", copy_in_ack:" + lsa);
			}
		}
	}	

	/**
	 *  Process every LSA received in LS update packet.
	 *  Maintain the Link State database stored in the router. Compare the existing LS
	 *  in the database to the received one. Update the database if the received is more
	 *  updated. Broadcast to its neighbor if necessary. Finally ack the LS to its originator.
	 *  
	 *  ref: section 13 
	 * */
	protected void lsa_receive ( OSPF_LSA lsa, OSPF_Neighbor from)
	{
		OSPF_LSA_Header lsh = lsa.header;
		OSPF_LSA		retrive  = null;
		OSPF_LSA		received = null;
		int				scope_type;
		int				impliedack_flag = 0;
		
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
			debug("----- LSA_RECEIVED, from:" + from.rtr_id + ", lsa:" + lsa);

		/* make lsa structure for received lsa */
		int now_ = (int) getTime();
		received = lsa; // Tyan: its simulation, dont make a copy
		ospf_set_birth (received, now_);
		
		Object scope;
		/* set scope */
		switch ( (scope_type = Util.translate_scope (received.header.lsh_type)) ) {
			/*case OSPF_LSA_Header.SCOPE_LINKLOCAL:
				scope = (OSPF_Interface) from.ospf_interface;
				break;*/
			case SCOPE_AREA	:
				scope = (OSPF_Area) from.ospf_interface.area;
				break;
/*			case OSPF_LSA_Header.SCOPE_AS:
			    scope = (void *) from->ospf6_interface->area->ospf6;
				break;*/
			/*case OSPF_LSA_Header.SCOPE_RESERVED:*/
			default:
			    if (isErrorNoticeEnabled())
					error("lsa_receive()", "unsupported scope, lsa_receive() failed");
				/* always unlock before return after make_ospf6_lsa() */
				// ospf6_lsa_unlock (received);
				return;
		}
		received.from = from;
		received.scope = scope;

		/* (1) XXX, LSA Checksum */

		/* (2) XXX, should be relaxed, check LSA type, if unknown, discard it */
		if ( received.header.lsh_type < OSPF_MIN_LSA || 
			received.header.lsh_type >= OSPF_MAX_LSA ) {
			if (isErrorNoticeEnabled())
				error("lsa_receive()", "Unsupported LSA Type, Ignore" + lsh.lsh_type);
			// ospf6_lsa_unlock (received);
			return;
		}

		/* (3) XXX, Ebit Missmatch: AS-External-LSA */

		/* lookup the same database copy in lsdb */
		OSPF_LSA have = ospf_lsdb_lookup(lsh.lsh_type, lsh.lsh_id, lsh.lsh_advtr, scope);
		int  ismore_recent = -1;
		if (have != null) ismore_recent = Util.ospf_lsa_check_recent(received, have, now_);

		// Tyan: CHECK THIS FIRST!!
		/* 5(f) Self Originated LSA, section 13.4 */
		// If self-originated, take care of the following erroneous situations
		if (is_self_originated (received, from.ospf_interface.area)) {
			// 1. if database have no copy, premature aging this lsa and flood
			if (have == null) {
				ospf_premature_aging ( received, now_ );
				ospf_lsa_flood (received);
			}
			// 2. if database copy is older, then make it newer and flood the database copy
			//    (to outdate the received lsa in the routing domain)
			else if (ismore_recent < 0) {
				have.header.lsh_seqnum = received.header.lsh_seqnum;
				ospf_lsa_flood(have); // seqnum will be incremented when originated
			}
			return;
		}

		/* (4) if MaxAge LSA and if we have no instance in the database and 
			no neighbor is in states Exchange or Loading */
		if (received.header.ospf_age_current (now_) == LSA_MAXAGE) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA)) debug("MaxAge LSA...");
			if ( have == null && !_nbr_any_exchange_loading()) {
				/* log */
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) && isDebugEnabledAt(DEBUG_LSA)))
					debug("MaxAge, no database copy, and no neighbor in Exchange or Loading");
					
				/* a) Acknowledge back to neighbor (13.5) */
				/* Direct Acknowledgement */
				direct_acknowledge ( received, from.ospf_interface, now_, false );
				
				/* b) Discard */
				// ospf6_lsa_unlock (received);
				return;
			}
			// Tyan, 05/08/2001, to avoid this lsa being processed again in ospf_lsa_expire()
			received.header.birth = Integer.MIN_VALUE;
		}

		/* (5) */
		/* if no database copy or received is more recent */
		if ( have == null || ismore_recent < 0) {
			//if (isDebugEnabled()) debug("FLOOD: no database copy or received is more recent");

			/* (a) MinLSArrival check */
			/* current time */
			if ( have != null && (now_ - have.header.installed <= MIN_LS_ARRIVAL) ) {
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_LSA)))
					debug("   too frequent! last update was " + (now_-have.header.installed)
						+ " ago, drop:" + received);
				return;   /* examin next lsa */
			}

			/* (b) immediately flood */
			// note: also add the new copy to all neighbors' retranslists here
			ospf_lsa_flood ( received, scope_type);

			/* (c) remove the (old) database copy from all neighbors' retranslists */
			if (have != null)
				ospf_remove_retrans(have, from.ospf_interface.area);

			/* (d) installing lsdb, which may cause routing
              table calculation (replacing database copy) */
			// the lsa's installed will be timestamped in this method
			ospf_lsdb_install ( received, have);
			
			/* (e) possibly acknowledge */
			ospf_possible_ack( received, ismore_recent, from.ospf_interface, impliedack_flag,
				0/*duplicate flag*/, now_);
		} 
		/* (6) if there is instance on sending neighbor's request list */
		else if (from.ospf_lookup_request ( received ) != null) {
			//if (isDebugEnabled()) debug("database copy exists, received is not newer, and is on his requestlist -> BadLSReq");

			/* BadLSReq */
			//thread_add_event (master, bad_lsreq, from, 0);
			_nbr_Bad_LSreq( from );
			
			/* always unlock before return */
			// ospf6_lsa_unlock (received);
			return;
		} /* (7) if neither is more recent */
		else if (ismore_recent == 0) {
			//if (isDebugEnabled()) debug("FLOOD: the same instance from " + from.rtr_id +" advr " + received.header.lsh_advtr);

			/* (a) if on retranslist, Treat this LSA as an Ack: Implied Ack */
			if (from.ospf_lookup_retrans( received.header ) != null) {
				_nbr_remove_retrans(have, from);
				// Tyan: 05/08/2001
				// should also remove the copy in ls_db if the lsa's age reaches MAXAGE
				if (have.header.lsh_age == OSPF.LSA_MAXAGE
					&& !_nbr_in_any_retranslist(have)) { // sec. 14
					from.ospf_interface.area.ls_db.ospf_lsdb_remove(have);
					if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_LSA)))
						debug("(IMPLIED_ACK) remove MAXAGE lsa from " + from.rtr_id
							+ "'s retranslist AND database: " + have);
				}
				else if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
					debug("(IMPLIED_ACK) remove lsa from " + from.rtr_id + "'s retranslist: " + have);

				/* note occurrence of implied ack */
				impliedack_flag = 1;
			}
			/* (b) possibly acknowledge */
			ospf_possible_ack( received, ismore_recent, from.ospf_interface, impliedack_flag,
				1/*duplicate flag*/, now_);
		}
		/* (8) previous database copy is more recent */
		else {
			//if (isDebugEnabled()) debug("FLOOD: already have newer copy");

			/* XXX, check seqnumber wrapping */

			// Send the database copy DIRECTLY to this neighbor 
			// do not put in from's retranslist and do not ack (implied ack, to outdate neighbor's copy)
			OSPF_LSA clone_ = (OSPF_LSA)have.clone();
			clone_.header.ospf_age_update_to_send( from.ospf_interface.transdelay, now_ );
			OSPF_LSupdate update = new OSPF_LSupdate(clone_);
					
			int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
			pkt_len += clone_.size();
			
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
				debug("send database copy directly back to the sending neighbor: " + from.rtr_id);
			ospf_message_send( LS_UPDATE, update, pkt_len, from.rtr_id, from.ospf_interface);
		}
		// ospf6_lsa_unlock (received);
		return;
	}

	void lsa_refresh(OSPF_LSA lsa, int now_)
	{
		// Tyan: 05/08/2001
		///* this will be used later as flag to decide really originate */
		//lsa.LSA_Refresh_EVT = null;
		
		/* log */
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_REFRESH))
			debug("----- LSA_REFRESH: " + lsa);

		OSPF_LSA new_lsa = null;

		switch (lsa.header.lsh_type) {
			case OSPF_ROUTER_LSA:
				OSPF_Area area = (OSPF_Area) lsa.scope;
				new_lsa = ospf_make_router_lsa (area);
				if ( lsa == null ) {
					if (isErrorNoticeEnabled())
						error("_nbr_state_change()", "cannot make router_lsa at Area " + area.area_id);
				}
				else {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_REFRESH))
						debug("   RECONSTRUCT_LSA: " + new_lsa + " from " + lsa);
					ospf_lsdb_install (new_lsa, area); // install it like others
					ospf_router_lsa_flood (area, true/*check*/);
				}
				break;

		/*	case OSPF_LSA_Header.OSPF_NETWORK_LSA:
				long ifindex = lsa.header.lsh_id;
				
        ifp = if_lookup_by_index (ifindex);
        if (!ifp)
          {
            zlog_warn ("interface not found: index %d, "
                       "can't reconstruct", ifindex);
            return (struct ospf6_lsa *) NULL;
          }
        o6if = (struct ospf6_interface *) ifp->info;
        assert (ho6if);
        new = ospf6_make_network_lsa (o6if);
        if (!new)
          break;
        ospf6_lsa_flood (new);
        ospf6_lsdb_install (new);
        ospf6_lsa_unlock (new);
        break;
		*/
		/*	case LST_INTRA_AREA_PREFIX_LSA:
        //	 XXX, assume LS-ID has addressing semantics
        ifindex = ntohl (lsa->lsa_hdr->lsh_id);
        ifp = if_lookup_by_index (ifindex);
        if (!ifp)
          {
            zlog_warn ("interface not found: index %d, "
                       "can't reconstruct", ifindex);
            return (struct ospf6_lsa *) NULL;
          }
        o6if = (struct ospf6_interface *) ifp->info;
        assert (o6if);
        new = ospf6_make_intra_prefix_lsa (o6if);
        if (!new)
          break;
        ospf6_lsa_flood (new);
        ospf6_lsdb_install (new);
        ospf6_lsa_unlock (new);
        break;*/

      /*case LST_LINK_LSA:
        o6if = (struct ospf6_interface *) lsa->scope;
        assert (o6if);
        new = ospf6_make_link_lsa (o6if);
        if (!new)
          break;
        ospf6_lsa_flood (new);
        ospf6_lsdb_install (new);
        ospf6_lsa_unlock (new);
        break;

      case LST_AS_EXTERNAL_LSA:
        ase = (struct as_external_lsa *)(lsa->lsa_hdr + 1);
        ps = (char *)(ase + 1);
        memset (&p, 0, sizeof (p));
        p.family = AF_INET6;
        p.prefixlen = ase->ase_prefix_len;
        memcpy (&p.u.prefix, ps, sizeof (p.u.prefix6));

        // xxx 
        ospf6_lsa_create_as_external (htonl (lsa->lsa_hdr->lsh_id), 0, 0,
                                      (struct prefix_ipv6 *) &p);
        break;*/

			default:
				break;
		}
		if (new_lsa == null)
			if (isErrorNoticeEnabled())
				error("lsa_refresh()", " *** Warn *** :  Refresh LSA failed");
	}

	/* make router lsa pkt to describe its neighbor */ 
	protected Router_LSA ospf_make_router_lsa (OSPF_Area area)
	{
		// Tyan: 05/08/2001, 10/18/2001
		// cancel previous refresh event if any
		if (area.lsa_refresh_timer != null) {
			cancelTimeout(area.lsa_refresh_timer);
			area.lsa_refresh_timer = null;
		}
		
		/* get links to describe */
		int i;
		int if_no = area.if_list.size();

		/* get current database copy */
		OSPF_LSA copy = area.router_lsa_self;
		OSPF_LSA_Header header;
		if (copy == null)
			header = new OSPF_LSA_Header(OSPF_ROUTER_LSA, router_id, router_id);
		else
			header = new OSPF_LSA_Header(copy.header.lsh_seqnum+1, OSPF_ROUTER_LSA, router_id, router_id);
		// dont call ospf_set_birth() as this router creates this lsa
		header.birth = (int)getTime();

		Router_LSA lsa = new Router_LSA(header);
 		lsa.from = (OSPF_Neighbor) null;
		lsa.scope = area;
		area.router_lsa_self = lsa;

		// Tyan0504: shouldn't cancel it
		/*
		// cancel delayed flood event if any
		if (flood_EVT.getObject() == area) {
			cancelTimeout(flood_EVT);
			flood_EVT.setObject(null);
		}
		*/

		for (i = 0; i < if_no; i++) {
			OSPF_Interface oif = (OSPF_Interface) area.if_list.elementAt(i);

			/* if interface is not enabled, ignore */
			if (oif.state <= IFS_LOOPBACK)
				continue;

			/* if interface is stub, ignore */
			if ( oif.ospf_interface_count_full_nbr() != 0 ) {
				Router_LSA_Link rlsd = lsa.make_ospf_router_lsd (oif);
				lsa.link_no++;
				lsa.ls_link_list.addElement(rlsd);
			}
		}
		

// xxx: set E & B bit:
		
		// Tyan: 05/08/2001, 10/18/2001
		area.lsa_refresh_timer =
			setTimeout(new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_LS_REFRESH,
									lsa ), LS_REFRESH_TIME);

		return lsa;
	}		


	/////////////////////////////////////////////////////////////////////////////
	//  Sending Routine
	/////////////////////////////////////////////////////////////////////////////
	
	/**
	 *  Direct acknowledgement 
	 * */
	protected void direct_acknowledge (OSPF_LSA lsa, OSPF_Interface oif, int now_, boolean clone_)
	{
		/* age update and add InfTransDelay */
		OSPF_LSA_Header lsh_ = clone_? (OSPF_LSA_Header)lsa.header.clone(): lsa.header;
		lsh_.ospf_age_update_to_send (oif.transdelay, now_ );

		/* create LS ack packet */
		OSPF_LSack ack = new OSPF_LSack(lsh_);

		/* send unicast packet to neighbor's ipaddress */
		int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
		pkt_len += OSPF_LSA_Header.OSPF_LSA_HEADER_SIZE;
		//ack.setSize(pkt_len);
		
		ospf_message_send ( LS_ACK, ack, pkt_len, lsa.from.rtr_id/* dst*/, oif);
	}

	/**
	 *  Delayed  acknowledgement
	 *  Include multiple acks in one packet. Hold these acks until the timer timeout.
	 *  ref: sec. 13.5
	 * */
	protected void delayed_acknowledge (OSPF_LSA lsa)
	{
		OSPF_Interface oif = lsa.from.ospf_interface;

		/* attach delayed acknowledge list */
		// ospf6_add_delayed_ack (lsa, o6if);
		oif.lsa_delayed_ack.addElement(lsa);

		/* if not yet, schedule delayed acknowledge RxmtInterval later */
		/* timers should be *less than* (1 second here) RxmtInterval or needless retrans will ensue */
		if (oif.Ack_Delay_Reach_EVT == null) {
			OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_ACK_DELAY_REACH, oif );
			oif.Ack_Delay_Reach_EVT = evt_;
			evt_.handle = setTimeout( evt_, oif.rxmt_interval-1);
		}
		/*if (o6if->thread_send_lsack_delayed == (struct thread *) NULL)
			o6if->thread_send_lsack_delayed  = thread_add_timer (master, ospf6_send_lsack_delayed,
                          o6if, o6if->rxmt_interval - 1); */
		return;
	}

	/**
	 *  Possible ack action mentioned in sec 13 (5)(e) and (7)(b) 
	 * */
	protected void ospf_possible_ack( OSPF_LSA received, int ismore_recent, OSPF_Interface oif,
		int impliedack_flag, int duplicate_flag, int now_)
	{
		int acktype = ack_type (received, ismore_recent, impliedack_flag, duplicate_flag, now_);
		if (acktype == DIRECT_ACK) {
			/*if (isDebugEnabled()) debug("Acknowledge: direct from " + router_id
										+ " to if_id: " + oif.if_id);*/
			direct_acknowledge (received, oif, now_, true/*clone header*/);
		}
		else if (acktype == DELAYED_ACK) {
			/*if (isDebugEnabled()) debug("Acknowledge: delayedfrom " + router_id
										+ " to if_id: " + oif.if_id);*/
			delayed_acknowledge (received);
		} else {
			//if (isDebugEnabled()) debug("Acknowledge: none");
		}
		return;
	}

	/** send the pkt to specific neighbor on specific if */
	protected void ospf_message_send ( int type, Object body, int body_size, long dst, OSPF_Interface oif)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug(PKT_TYPES[type] + " sent from " + router_id + " to " + dst + ": " + body);
		/* memory allocate for protocol header */
		OSPF_Packet ospf_pkt = new OSPF_Packet(type, router_id, oif.area.area_id); 
		ospf_pkt.setBody(body, body_size);
	
		if ( oif == null)
			forward( ospf_pkt, (long) router_id, dst, false, 1, CONTROL_PKT_TYPE);
		else
			forward( ospf_pkt, (long) router_id, dst, false, 1, CONTROL_PKT_TYPE, oif.if_id);
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
	protected void ospf_message_broadcast ( int type, Object body, int body_size, OSPF_Interface oif)
	{
		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SEND))
			debug(PKT_TYPES[type] + " broadcast from " + router_id + " at if " + oif.if_id + ": " + body);
		/* memory allocate for protocol header */
		OSPF_Packet ospf_pkt = new OSPF_Packet(type, router_id, oif.area.area_id); 
		ospf_pkt.setBody(body, body_size);
	
		if( oif == null)
			forward( ospf_pkt, (long) router_id, Address.NULL_ADDR, true, 1, CONTROL_PKT_TYPE);
		else	
			forward( ospf_pkt, (long) router_id, Address.NULL_ADDR, true, 1, CONTROL_PKT_TYPE, oif.if_id );
		//ospf6_sendmsg (o6i->lladdr, dst, &ifindex, message);
	}

	/**
	 *  Sends hello pkts out at each functioning interface.
	 * 
	 *  (The method is not used anymore, we are using drcl.inet.core.Hello
	 *  as underlying Hello service provider)
	 *  Ref: sec. 9.5
	 */
	protected int ospf_send_hello ( OSPF_Interface oif )
	{
		int		n;
		OSPF_Neighbor	nbr;

		/* check interface is up */
		if (oif.state <= IFS_DOWN) {
			if (isErrorNoticeEnabled())
				error("ospf_send_hello()", " *** Warn *** :  not enabled, stop send hello at if "
					+ oif.if_id );
			return 0;
		}

		/* allocate hello header */
		OSPF_Hello hello = new OSPF_Hello(oif.hello_interval, oif.dead_interval); 
		if (hello == null) {
			if (isErrorNoticeEnabled())
				error("ospf_send_hello()", " *** Warn *** : hello alloc failed to " + oif.if_id );
			return -1;
		}

		/* set neighbor router id */
		for (n = 0; n < oif.neighbor_list.size(); n++) {
			nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(n);
			if (nbr.state < NBS_INIT)
			    continue;
			
			/* set neighbor router id */
			hello.add_neighbor_id( (((OSPF_Neighbor)oif.neighbor_list.elementAt(n)).rtr_id));
		} // end of for
		
		/* set fields */
		/* hello->rtr_pri = o6if->priority;
		memcpy (hello->options, o6if->area->options, sizeof (hello->options));
		hello->dr = o6if->dr;
		hello->bdr = o6if->bdr;  */
	
		/* send hello */
		ospf_message_broadcast( HELLO, hello, hello.size(), oif);

		/* set next timer thread */
		//  o6if->thread_send_hello = thread_add_timer (master, ospf6_send_hello,
		//                                            o6if, o6if->hello_interval);
		OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_HELLO, oif );
		oif.Hello_TimeOut_EVT = evt_;		
		evt_.handle = setTimeout( evt_,oif.hello_interval);
		return 0;
	}

	/**
	 *  used when receiving EVENT_IF_NEIGHBOR_UP, send only one hello packet to 
	 *  ensure two-way connection
    */
	protected int ospf_send_single_hello ( OSPF_Interface oif, long dst )
	{
		int		n;
		OSPF_Neighbor	nbr = null;

		/* check interface is up */

		if (oif.state <= IFS_DOWN) {
			if (isErrorNoticeEnabled())
				error("ospf_send_single_hello()", " *** Warn *** : not enabled, stop send hello at if "
					+ oif.if_id ); 
			return 0;
		}

		/* allocate hello header */
		OSPF_Hello hello = new OSPF_Hello(oif.hello_interval, oif.dead_interval); 
		if (hello == null) {
			if (isErrorNoticeEnabled())
				error("ospf_send_single_hello()", " *** Warn *** : hello alloc failed to "
					+ oif.if_id );
			return -1;
		}

		/* set neighbor router id */
		for (n = 0; n < oif.neighbor_list.size(); n++) {
			nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(n);
			if (nbr.state < NBS_INIT)
			    continue;
			
			/* set neighbor router id */
			hello.add_neighbor_id( (((OSPF_Neighbor)oif.neighbor_list.elementAt(n)).rtr_id));
		} // end of for
		
		/* send hello */
		ospf_message_send( HELLO, hello, hello.size(), dst, oif);
		return 0;
	}

	/**
	 * Send the database description to the peer
	 * 
	 * ref: 10.8
	 */
	protected int ospf_send_dbdesc (OSPF_Neighbor nbr)
	{
		int leftlen;
		int pktlen = 0;
		int	DEFAULT_INTERFACE_MTU = nbr.ospf_interface.ifmtu;
		int exchange_done_ = 0;

		/* if state less than ExStart, do nothing */
		if (nbr.state < NBS_EXSTART)
			return 0;

		/* xxx, how to limit packet length correctly? */
		/* use leftlen to make empty initial dbdesc */
		if ( nbr.dd_ibit == 1) {
			leftlen = 0;
		}
		else {
			leftlen = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN - OSPF_DBdesc.OSPF_DBDESC_FIX_SIZE;
		}
		pktlen = OSPF_DBdesc.OSPF_DBDESC_FIX_SIZE;

		/* if this is initial, set seqnum */
		if (nbr.dd_ibit == 1)
			ospf_dbdesc_seqnum_init (nbr);

		/* make dbdesc */
		OSPF_DBdesc dbdesc = new OSPF_DBdesc(nbr.dd_ibit, nbr.dd_mbit, nbr.dd_msbit, nbr.dd_seqnum);

		/* set dbdesc */
		dbdesc.ifmtu = DEFAULT_INTERFACE_MTU;

		int now_ = (int)getTime();
		int transdelay_ = nbr.ospf_interface.transdelay;
		/* move LSA from summary list to message buffer */
		while (leftlen >= OSPF_LSA_Header.OSPF_LSA_HEADER_SIZE ) {
			/* get first LSA from summary list */
			// n = listhead (nbr->summarylist);
			if ( nbr.summarylist.size() == 0 ) {
				/* no more DbDesc to transmit */
				/* check, not sure */
				/* master should se Mbit zero before than slave, i.e. 
				   slave should enter Exchange_Done before than master */
				/* slave must schedule ExchangeDone on sending, here */
				if (nbr.dd_msbit == 0) {
					if ( (nbr.last_dd != null) && (nbr.last_dd.dd_mbit == 0) ) {
		                exchange_done_ = 1;
					}
					else 
						break; // not set M-bit
		        }
				nbr.dd_mbit = 0;
				//if (isDebugEnabled()) debug(" More bit cleared");
				break;
			}
			OSPF_LSA_Header lsh = ((OSPF_LSA) nbr.summarylist.lastElement()).header;
			/* set age and add InfTransDelay */
			lsh.ospf_age_update_to_send (transdelay_, now_ );

			/* add one LSA header to dbdesc pkt */
			dbdesc.addlsaheader(lsh);

			/* left packet size */
			leftlen -= lsh.size();
			pktlen += lsh.size();
			
			/* take LSA from summary list */
			nbr.summarylist.removeElementAt (nbr.summarylist.size()-1);
		}

		/* cancel previous dbdesc retransmission thread */
		/* timer should associated with nbr */
		if ( nbr.LSDBdesc_Retrans_EVT != null) {
			cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
			nbr.LSDBdesc_Retrans_EVT = null;
		}

		/* set M-bit before sending out) */
		dbdesc.setMbit (nbr.dd_mbit) ;
		ospf_message_send ( DATABASE, dbdesc.clone(), pktlen, nbr.rtr_id, nbr.ospf_interface);

		/* set new dbdesc packet to send */
		//iov_copy_all (nbr->dbdesc_last_send, message, MAXIOVLIST);
		nbr.dbdesc_last_send = dbdesc;

		/* if master, set retransmission */
		if ( nbr.dd_msbit == 1) {
			if (nbr.LSDBdesc_Retrans_EVT != null)
				cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
			OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_DBDESC_RETRANS, nbr );
			nbr.LSDBdesc_Retrans_EVT = evt_;
			evt_.handle = setTimeout( evt_, nbr.ospf_interface.rxmt_interval);
			/*nbr->thread_dbdesc_retrans = thread_add_timer (master, ospf6_send_dbdesc_retrans,
                          nbr, nbr->ospf6_interface->);*/
		} 
		if(exchange_done_ == 1)
			_nbr_Exchange_Done( nbr );

		return 0;
	}

	/**
	 *  Retransmit the database description when
	 *  1) The DB sending timer in master is timeput, i.e. The master does not received
	 *     the DB pkt from the slave within one period of time.
	 *  2) The slave receives the same content as the previous pkt received from the 
	 *     master.
	 */
	private int ospf_send_dbdesc_retrans ( OSPF_Neighbor nbr)
	{
		/* statistics */
		nbr.ospf_stat_retrans_dbdesc++;
		/* stop the timer */
		if ( nbr.LSDBdesc_Retrans_EVT != null) {
			cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
			nbr.LSDBdesc_Retrans_EVT = null;
		}
		
		/* if state less than ExStart, do nothing */
		if (nbr.state < NBS_EXSTART)
			return 0;
		
		nbr.dbdesc_last_send.ospf_age_update_to_send(nbr.ospf_interface.transdelay, (int)getTime());

		/* send dbdesc */
		ospf_message_send ( DATABASE, nbr.dbdesc_last_send.clone(), nbr.dbdesc_last_send.size(),
			nbr.rtr_id, nbr.ospf_interface);

		/* if master, set futher retransmission */
		if( nbr.dd_msbit == 1) {
			if (nbr.LSDBdesc_Retrans_EVT != null)
				cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
			OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_DBDESC_RETRANS, nbr );
			nbr.LSDBdesc_Retrans_EVT = evt_;
			evt_.handle = setTimeout( evt_, nbr.ospf_interface.rxmt_interval);
			/* nbr->thread_dbdesc_retrans = thread_add_timer (master, ospf6_send_dbdesc_retrans,
                          nbr, nbr->ospf6_interface->rxmt_interval); */
		} else {
			if ( nbr.LSDBdesc_Retrans_EVT != null ) {
				cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
				nbr.LSDBdesc_Retrans_EVT = null;
			}
			//nbr->thread_dbdesc_retrans = (struct thread *) NULL;
		}
		return 0;
	}

	/**
	 * Retransmit the LSrequest pkt
	 * ref: sec. 10.9 
	 * */
	private void ospf_send_lsreq_retrans ( OSPF_Neighbor nbr )
	{
		nbr.LSreq_Retrans_EVT = null;
		
		/* if state less than ExStart, do nothing */
		if (nbr.state < NBS_EXCHANGE)
			return ;

		int req_num = nbr.requestlist.size();
		/* schedule loading_done if request list is empty */
		if( req_num == 0) {
			// thread_add_event (master, loading_done, nbr, 0);
			_nbr_Loading_Done(nbr);
			return ;
		}

		/* statistics */
		nbr.ospf_stat_retrans_lsreq++;
		ospf_send_lsreq(nbr);
	}
	
	/**
	 * Send the LS request pkt to its peer.
	 * Ref: sec. 10.9 
	 */
	protected int ospf_send_lsreq ( OSPF_Neighbor nbr )
	{
		OSPF_LSA lsa = null;
		int	DEFAULT_INTERFACE_MTU = nbr.ospf_interface.ifmtu;

		/* if state less than ExStart, do nothing */
		if (nbr.state < NBS_EXCHANGE)
			return 0;

		int leftlen = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN
			- OSPF_LSrequest.OSPF_LS_REQUEST_PKT_SIZE;
		int pkt_len = OSPF_LSrequest.OSPF_LS_REQUEST_PKT_SIZE;
		
		int i;
		int no = nbr.requestlist.size();
		OSPF_LSrequest lsreq = new OSPF_LSrequest( no );
		for ( i = 0; i < no ; i++) {
			if (leftlen < OSPF_LSrequest.OSPF_LS_REQUEST_PKT_SIZE)
				break;
			else {
				leftlen -= OSPF_LSrequest.OSPF_LS_REQUEST_PKT_SIZE;
				pkt_len += OSPF_LSrequest.OSPF_LS_REQUEST_PKT_SIZE;
			}
			lsa =  (OSPF_LSA) nbr.requestlist.elementAt(i);
			lsreq.addreq( lsa.header.lsh_type, lsa.header.lsh_id, lsa.header.lsh_advtr);
		}
		
		//if (isDebugEnabled()) debug("LSrequest sent from " + router_id + " to " + nbr.rtr_id + " LSA_num " + no);

		/*ospf6_message_send (MSGT_LSREQ, message, nbr.hisaddr.sin6_addr,
                      nbr.ospf_interface.interface.ifindex);*/
		ospf_message_send ( LS_REQUEST, lsreq, pkt_len, nbr.rtr_id, nbr.ospf_interface);
      
		if (nbr.LSreq_Retrans_EVT != null)
			cancelTimeout(nbr.LSreq_Retrans_EVT.handle);
		OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSREQ_RETRANS, nbr );
		nbr.LSreq_Retrans_EVT = evt_;
		evt_.handle = setTimeout( evt_, nbr.ospf_interface.rxmt_interval);
				
		/* if (nbr->thread_lsreq_retrans != NULL)
				thread_cancel (nbr->thread_lsreq_retrans);
			nbr->thread_lsreq_retrans = thread_add_timer (master, ospf6_send_lsreq_retrans,
                      nbr, nbr->ospf6_interface->rxmt_interval); */
		return 0;
	}	
	
	/**
	 *  When one LSA in the LS database expires, this function will be called via
	 *  timeout event
	 */
	void ospf_lsa_expire (OSPF_LSA lsa)
	{
		Object scope;
		
		/* assertion */
		// Tyan: what does this mean?
		//if ( (lsa.header.ospf_age_current ((int)getTime()) >= LSA_MAXAGE)
		//	|| (lsa.LSA_Refresh_EVT == null) ) {
		//	// error
		//	return;
		//}
		//if( lsa.LSA_MaxAge_Reach_EVT != null)
		//	cancelTimeout( lsa.LSA_MaxAge_Reach_EVT );
		//lsa.LSA_MaxAge_Reach_EVT = null;

		/* log */
		//if (isDebugEnabled()) debug("LSA: Expire:");

		/* reflood lsa */
		// Tyan, 05/08/2001, trigger spf recalculation
		lsa_change(lsa);
		// Tyan, 05/08/2001, set birth to MIN_VALUE to indicate that this lsa has expired and processed
		lsa.header.birth = Integer.MIN_VALUE;
		ospf_lsa_flood (lsa);
		return ;
	}

	int ospf_send_lsupdate_retrans ( OSPF_Neighbor nbr )
	{
		nbr.LSupdate_Retrans_EVT = null;
		int lsanum = 0;
		int	DEFAULT_INTERFACE_MTU = nbr.ospf_interface.ifmtu;
		int left_len = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN;
		int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
		
		if (nbr.ospf_interface.state <= IFS_WAITING)
			return -1;
		
		int i, no;
		no = nbr.retranslist.size();
		/* create an update packet */
		OSPF_LSupdate lsupdate = new OSPF_LSupdate(no);

		int transdelay_ = nbr.ospf_interface.transdelay;
		int now_ = (int)getTime();
		for ( i = 0; i < no; i++) {
			OSPF_LSA lsa = (OSPF_LSA)  nbr.retranslist.elementAt(i);
			if (left_len >= lsa.size()) {
				/* okay, add it */
				OSPF_LSA clone_ = (OSPF_LSA)lsa.clone();
				clone_.header.ospf_age_update_to_send(transdelay_, now_);
				lsupdate.addlsa(clone_);
				left_len -= lsa.size();
				pkt_len += lsa.size();
				lsanum++;
			} else {
				/* not enough space, must send it */
				/* since at any time only one LS request pkt can be originated 
				   , the unfinished part remains for the next time */
				break;
			}
		}
		if (lsanum == 0)
			return 0;

		/* statistics */
		nbr.ospf_stat_retrans_lsupdate++;
		
		ospf_message_send ( LS_UPDATE, lsupdate, pkt_len, nbr.rtr_id, nbr.ospf_interface);

		if (nbr.LSupdate_Retrans_EVT != null)
			cancelTimeout(nbr.LSupdate_Retrans_EVT.handle);
		OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSUPDATE_RETRANS, nbr );
		nbr.LSupdate_Retrans_EVT = evt_;
		evt_.handle = setTimeout( evt_, nbr.ospf_interface.rxmt_interval);
		/* o6n->send_update = thread_add_timer (master, ospf6_send_lsupdate_retrans,
                                       o6n, o6n->ospf6_interface->rxmt_interval); */
		return 0;
	}
	
	private int ospf_send_lsack_delayed ( OSPF_Interface oif)
	{
		OSPF_LSack lsack = null;
		
		int	DEFAULT_INTERFACE_MTU = oif.ifmtu;
		int leftlen = DEFAULT_INTERFACE_MTU - OSPF_Packet.OSPF_HEADER_LEN;
		int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
					 
		//o6i->thread_send_lsack_delayed = (struct thread *) NULL;
		oif.Ack_Delay_Reach_EVT = null;

		if (oif.state <= IFS_WAITING)
			return 0;
		int no = oif.lsa_delayed_ack.size();
		int i;
		if (no > 0)
			lsack = new OSPF_LSack( no );

		int transdelay_ = oif.transdelay;
		int now_ = (int)getTime();
		while (oif.lsa_delayed_ack.size() > 0) {
			OSPF_LSA lsa = (OSPF_LSA) oif.lsa_delayed_ack.elementAt(0);
			if (leftlen < OSPF_LSack.OSPF_LS_ACK_PKT_SIZE )
				break;
			else {
				leftlen -= OSPF_LSack.OSPF_LS_ACK_PKT_SIZE;
				pkt_len += OSPF_LSA_Header.OSPF_LSA_HEADER_SIZE;
			}
			OSPF_LSA_Header lsh = (OSPF_LSA_Header)lsa.header.clone();
			lsh.ospf_age_update_to_send(transdelay_, now_);
			lsack.addlsahdr( lsh );
			oif.lsa_delayed_ack.removeElement(lsa);
		}

		if ( lsack == null || lsack.lsack_num == 0)
			return 0;

		/* statistics */
		oif.ospf_stat_delayed_lsack++;

		switch (oif.state) {
			case IFS_DR:
			case IFS_BDR:
				//ospf6_message_send (MSGT_LSACK, message, &allspfrouters6.sin6_addr, o6i->if_id);
				break;
			default:
				ospf_message_broadcast ( LS_ACK, lsack, pkt_len, oif);
				break;
		}
		return 0;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////
	//  Scope Releated Routine
	///////////////////////////////////////////////////////////////////////////////////////////

	// schedule next nearest maxage reached
	void ospf_schedule_next_maxage(double now_)
	{
		// check all lsdb in all areas to find next maxage lsa
		double max_age_ = -1;
		OSPF_LSA  next_ = null;
		for (int i=0; i<area_list.size(); i++) {
			OSPF_Area area_ = (OSPF_Area)area_list.elementAt(i);
			if (area_ == null)
				continue;
			for ( int j=0; j<area_.ls_db.ls_list.size(); j++) {
				OSPF_LSA lsa_ = (OSPF_LSA) area_.ls_db.ls_list.elementAt(j);
				int lsa_age = lsa_.header.ospf_age_current( (int) now_ );
				if( lsa_age == LSA_MAXAGE) {
					if (lsa_.header.birth > Integer.MIN_VALUE) {
						if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE)
							|| isDebugEnabledAt(DEBUG_LSA)) )
							debug ("(schedule_next_maxage) lsa expire : " + lsa_);
						ospf_lsa_expire(lsa_);
					}
				} else if ( lsa_age > max_age_ ) {
					next_ = lsa_ ;
					max_age_ = lsa_age;
				}
			}
		}
		if (next_ != null) {
			lsa_maxage_EVT.setObject(next_);
			lsa_maxage_EVT.handle = setTimeout(lsa_maxage_EVT, LSA_MAXAGE - max_age_ );
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT)) {
				debug ("(schedule next maxage) set maxage timeout at time: "
					+ (now_+(LSA_MAXAGE-max_age_)) + "evt: " + lsa_maxage_EVT);
			}
		}
		else {
			//if (lsa_maxage_EVT.getObject() != null)
			//	cancelTimeout(lsa_maxage_EVT);
			lsa_maxage_EVT.setObject(null);
		}
	}

	// schedule next nearest delay flood as well as
	// perform flooding on any area whose delay flood timeout is on and expired
	void ospf_schedule_next_flood(double now_)
	{
		OSPF_Area next_ = null;
		double time_ = Double.POSITIVE_INFINITY;
		for (int i=0; i<area_list.size(); i++) {
			OSPF_Area area_ = (OSPF_Area)area_list.elementAt(i);
			if (area_ == null || !area_.delayFlood)
				continue;
			else if (now_ >= area_.floodLastTime)
				ospf_router_lsa_flood(area_, false);
			else if (area_.floodLastTime < time_) {
				next_ = area_;
				time_ = area_.floodLastTime;
			}
		}
		if (next_ != null) {
			flood_EVT.setObject(next_);
			flood_EVT.handle = setTimeoutAt(flood_EVT, time_);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT))
				debug(" set DELAY_FLOOD timeout1: at " + time_ + " area:" + next_.area_id
					+ " router lsa:" + next_.router_lsa_self);
		}
		else {
			if (flood_EVT.handle != null)
				cancelTimeout(flood_EVT.handle);
			flood_EVT.setObject(null);
			flood_EVT.handle = null;
		}
	}

	// consider MIN_LS_INTERVAL
	protected void ospf_router_lsa_flood(OSPF_Area area, boolean check_)
	{
		double now_ = getTime();
		if (check_ && now_ - area.floodLastTime < MIN_LS_INTERVAL) {
			if (!area.delayFlood) {
				area.delayFlood = true;
				area.floodLastTime += MIN_LS_INTERVAL;

				if (flood_EVT.getObject() != area)
					ospf_schedule_next_flood(now_);
			}
		}
		else {
			if (area.router_lsa_self != null) {
				ospf_lsa_flood_area(area.router_lsa_self, area);
				area.floodLastTime = now_;
			}
			area.delayFlood = false;
		}
	}
	
	/** flood ospf_lsa within appropriate scope */
	protected void ospf_lsa_flood ( OSPF_LSA lsa)
	{
		int scope_type = Util.translate_scope (lsa.header.lsh_type);
		ospf_lsa_flood(lsa, scope_type);
	}
	
	/** flood ospf_lsa within appropriate scope */
	protected void ospf_lsa_flood ( OSPF_LSA lsa, int scope_type)
	{
		OSPF_Area		area;
		OSPF_Interface	oif;
		/*OSPF_AS		as;*/
		
		switch (scope_type) {
			/*case OSPF_LS_Database.SCOPE_LINKLOCAL:
				oif = ( OSPF_Interface ) lsa.scope;
				oif.ospf6_lsa_flood_interface (lsa);
				return;*/

			case OSPF_LS_Database.SCOPE_AREA:
				area = (OSPF_Area) lsa.scope;
				ospf_lsa_flood_area (lsa, area);
				return;

			/*case OSPF_LS_Database.SCOPE_AS:
				as = (OSPF_AS) lsa.scope;
				as.ospf_lsa_flood (lsa);
				return;*/

			/*case SCOPE_RESERVED:
			default:
				o6log.dbex ("unsupported scope, can't flood");
				break;*/
		}
		return;
	}

	// sec. 13.3
	void ospf_lsa_flood_area ( OSPF_LSA lsa_, OSPF_Area area_)
	{
		/* for each eligible ospf_ifs */
		int no = area_.if_list.size();
		for (int i = 0; i < no; i++) {
			OSPF_Interface ospf6_interface = (OSPF_Interface) area_.if_list.elementAt(i);
			ospf_lsa_flood_interface (lsa_, ospf6_interface);
		}
	}

	/**
	 * for one interface, execute the flood procedure 
	 * ref: sec. 13.3
	 */
	void ospf_lsa_flood_interface ( OSPF_LSA lsa, OSPF_Interface oif )
	{
		int ismore_recent, addretrans = 0;
		/*sockaddr_in6 dst;*/

		int now_ = (int)getTime();
		int i;
		int nbr_no = oif.neighbor_list.size();
		/* (1) for each neighbor */
		for( i = 0; i < nbr_no; i++) {
			OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(i);
			/* (a) */
			if (nbr.state < OSPF.NBS_EXCHANGE)
				continue;  /* examin next neighbor */

			/* (b) */
			if ( (nbr.state == OSPF.NBS_EXCHANGE) || 
				 (nbr.state == OSPF.NBS_LOADING) ) {
				/* the request list is the list of LSAs that need to be received from this neighbor */
				OSPF_LSA req = nbr.ospf_lookup_request(lsa);
				if ( req != null ) {
					ismore_recent = Util.ospf_lsa_check_recent (lsa, req, now_);
					if (ismore_recent > 0) {
						if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
							debug("   req in neighbor " + nbr.rtr_id + "'s req list:" + req
								+ " is newer than the lsa_to_be_flooded:" + lsa
								+ ", dont flood this neighbor");
						continue; /* examin next neighbor */
				    }
					else if (ismore_recent == 0) {
						_nbr_remove_request (req, nbr);
						if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
							debug("   req in neighbor " + nbr.rtr_id + "'s req list:" + req
								+ " is same old as the lsa_to_be_flooded:" + lsa
								+ ", remove the req from req list and dont flood this neighbor");
						continue; /* examin next neighbor */
					}
					else { /* ismore_recent < 0(the new LSA is more recent) */
						_nbr_remove_request (req, nbr);
						if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
							debug("   req in neighbor " + nbr.rtr_id + "'s req list:" + req
								+ " is older than the lsa_to_be_flooded:" + lsa
								+ ", remove the req from req list");
				    }
				}
			} 
			/* (c) */
			if (lsa.from == nbr)
				continue; /* examin next neighbor */

			/* (d) add retranslist */
			/* retrans list is the list of LSAs that have been flooded but not acked yet */
			nbr.ospf_add_retrans (lsa);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
				debug("Add lsa to " + nbr.rtr_id + "'s retranslist: " + lsa);
			addretrans++;
			if ( nbr.LSupdate_Retrans_EVT == null) {
				if (nbr.LSupdate_Retrans_EVT != null)
					cancelTimeout(nbr.LSupdate_Retrans_EVT.handle);
				OSPF_TimeOut_EVT evt_ = new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_LSUPDATE_RETRANS, nbr );
				nbr.LSupdate_Retrans_EVT = evt_;
				evt_.handle = setTimeout( evt_, nbr.ospf_interface.rxmt_interval);
			}
		}

		/* (2) */
		if (addretrans == 0) {
			/*o6log.dbex ("don't flood interface %s", oif.ni.name);*/
			return; /* examin next interface */
		}  else if (lsa.from != null && lsa.from.ospf_interface == oif) {
			/*o6log.dbex ("flooding %s is floodback", oif.ni.name);*/
			// note occurence of floodback 
			lsa.floodback_flag = 1;
		} else
			 ; /*o6log.dbex ("flood %s", oif.ni.name);*/

		/* (3) */
		/*  if (lsa->from && lsa->from->ospf6_interface == o6if)
			{
		      // if from DR or BDR, don't need to flood this interface
		      if (lsa->from->rtr_id == lsa->from->ospf6_interface->dr ||
		         lsa->from->rtr_id == lsa->from->ospf6_interface->bdr)
		        return; // examin next interface
		    }
		*/
		/* (4) if I'm BDR, DR will flood this interface */
		/*  if (lsa->from && lsa->from->ospf6_interface == o6if
		      && o6if->state == IFS_BDR)
			    return; // examin next interface
		*/
		/* (5) send LinkState Update */
		/* set age */
		lsa = (OSPF_LSA)lsa.clone();
		lsa.header.ospf_age_update_to_send(oif.transdelay, now_);

		/* make LinkState Update header */
		OSPF_LSupdate lsupdate = new OSPF_LSupdate( lsa );
		
		int pkt_len = OSPF_Packet.OSPF_HEADER_LEN;
		pkt_len += lsa.size();
		
		ospf_message_broadcast( LS_UPDATE, lsupdate, pkt_len, oif);
	}

	/** ordinary lookup function */
	protected OSPF_LSA ospf_lsdb_lookup ( int type, int id, int advrtr, Object scope)
	{
		OSPF_Interface	oif;
		OSPF_Area		area;
		OSPF			ospf;
		OSPF_LSA		found = null;

		switch ( Util.translate_scope( type ) ) {
			//case SCOPE_LINKLOCAL:
			//	oif = ( OSPF_Interface ) scope;
			//	found = oif.ospf_lsdb_lookup_interface (type, id, advrtr);
			//	return found;
			case SCOPE_AREA:
				area = (OSPF_Area) scope;
				found = area.ls_db.ospf_lsdb_lookup (type, id, advrtr);
				return found;
			//case SCOPE_AS:
			//	ospf = (OSPF) scope;
			//	found = ospf6_lsdb_lookup_as (type, id, advrtr, ospf);
			//	return found;
			//case SCOPE_RESERVED:
			default:
				if (isErrorNoticeEnabled()) error("ospf_lsdb_lookup()", "unsupported lsa type: " + type);
				break;
		}
		return null;
	}

	/**
	 * When installing more recent LSA, must detach less recent database copy
	 * from LS-lists of neighbors, and attach new one.
	 * ref. sec 13.2 
	 */
	protected void ospf_lsdb_install ( OSPF_LSA new_lsa, OSPF_Area area)
	{ ospf_lsdb_install(new_lsa, area.ls_db.ospf_lsdb_lookup(new_lsa), area); }	

	/**
	 * Replaces old_lsa with new_lsa and removes old_lsa from the retranslists in area.
	 * Recalculates SPF if necessary (see 13.2).
	 */
	protected void ospf_lsdb_install ( OSPF_LSA new_lsa, OSPF_LSA old_lsa, OSPF_Area area)
	{
		//if (debug) drcl.Debug.debug("LSA Install:");
		
		// Will, 05/04
		boolean same_ = false;

		int now_ = (int)getTime();
		new_lsa.header.installed = now_;
		area.ls_db.ospf_lsdb_replace (new_lsa, old_lsa);

		if (old_lsa != null) {
			// remove old from retranslist no matter how (so that new may trigger new retrans timer)
			ospf_remove_retrans (old_lsa, area);
			// Tyan, 05/08, do this in ospf_make_router_lsa()
			//// cancel refresh event
			//if (old_lsa.LSA_Refresh_EVT != null)
			//	cancelTimeout(old_lsa.LSA_Refresh_EVT);

			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_LSA))
				debug("(lsdb_install) remove old_lsa from area"
								+ area.area_id + "'s retranslist: " + old_lsa);

			if (Util.check_is_same(new_lsa, old_lsa, now_)) {
				// update vertex.vtx_lsa only, avoid update the whole route
				int no = area.vertex_list.size();
				for (int i=0; i<no; i++) {
					OSPF_SPF_vertex v = (OSPF_SPF_vertex)area.vertex_list.elementAt(i);
					if (v.vtx_lsa == old_lsa) { v.vtx_lsa = new_lsa; break; }
				}
				// Will, 05/04
				//return;
				same_ = true;
			}
		}

		/* if differs, update route , see sec. 13.2*/
		// Will, 05/04
		if (!same_)
			lsa_change (new_lsa);

		// Will, 05/04:
		if (lsa_maxage_EVT.getObject() == null) {
			// the first existing lsa, set timer
			lsa_maxage_EVT.setObject(new_lsa);
			lsa_maxage_EVT.handle = setTimeout(lsa_maxage_EVT, LSA_MAXAGE - new_lsa.header.lsh_age);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT)) {
				debug ("lsdb_install : first maxage lsa " + new_lsa +" evt:" + lsa_maxage_EVT);
			}
		} else if (lsa_maxage_EVT.getObject() == old_lsa) { 
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT)) {
				debug ("(ospf_lsdb_install) reselect maxage lsa, old: " + old_lsa );
			}
			//cancelTimeout(lsa_maxage_EVT);
			//lsa_maxage_EVT.setObject(null);
			
			// reselect maxage lsa
			ospf_schedule_next_maxage(now_);
		} else if ( new_lsa.header.ospf_age_current(now_) >
			((OSPF_LSA) lsa_maxage_EVT.getObject()).header.ospf_age_current(now_) ) {
			//since the age of the newly added lsa could be older then 
			//the existing oldest lsa, we need to compare the age of the new_lsa
			//and the old lsa
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_TIMEOUT)) {
				debug ("(ospf_lsdb_install) change maxage lsa " + new_lsa );
			}
			cancelTimeout(lsa_maxage_EVT.handle);
			lsa_maxage_EVT.setObject(new_lsa);
			lsa_maxage_EVT.handle = setTimeout(lsa_maxage_EVT,
						LSA_MAXAGE-new_lsa.header.ospf_age_current(now_) );
		}
	}
	

	protected void ospf_lsdb_install ( OSPF_LSA new_lsa, OSPF_LSA old_lsa )
	{
		int scope_type = Util.translate_scope (new_lsa.header.lsh_type);
		ospf_lsdb_install(new_lsa, old_lsa, scope_type);
	}

	/**
	 * Install a new LSA into the database
	 */
	protected void ospf_lsdb_install ( OSPF_LSA new_lsa, OSPF_LSA old_lsa, int scope_type)
	{
		OSPF_Area		area;
		OSPF_Interface	oif;
		/*OSPF_AS		as;*/
		
		switch (scope_type) {
			/*case OSPF_LS_Database.SCOPE_LINKLOCAL:
				oif = ( OSPF_Interface ) lsa.scope;
				oif.ospf_lsdb_install (lsa);
				return;*/

			case OSPF_LS_Database.SCOPE_AREA:
				area = (OSPF_Area) new_lsa.scope;
				ospf_lsdb_install (new_lsa, old_lsa, area);
				return;

			/*case OSPF_LS_Database.SCOPE_AS:
				as = (OSPF_AS) lsa.scope;
				as.ospf_lsdb_install (lsa);
				return;*/

			/*case SCOPE_RESERVED:
			default:
				o6log.dbex ("unsupported scope, can't install");
				break;*/
		}
		return;
	}
	
	/**
	 * Timestamp at the "birth" field in the header.
	 * Must call this method on each received lsa. 
	 * MUST NOT call this for self-originated lsas.
	 * THis method does not set maxage timeout for this lsa
	 */
	void ospf_set_birth (OSPF_LSA lsa, int now_)
	{
		lsa.header.birth = now_ - lsa.header.lsh_age;
	}

	void ospf_premature_aging ( OSPF_LSA lsa, int now_ )
	{
		/* log */
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_LSA)))
			debug("LSA Premature aging: " + lsa);

		//if (lsa.LSA_MaxAge_Reach_EVT != null) {
		//	cancelTimeout(lsa.LSA_MaxAge_Reach_EVT);
		//	lsa.LSA_MaxAge_Reach_EVT = null;
		//}
		// Tyan, 05/08/2001, should not happen, a check maybe?
		//if (lsa.LSA_Refresh_EVT != null) {
		//	cancelTimeout(lsa.LSA_Refresh_EVT);
		//	lsa.LSA_Refresh_EVT = null;
		//}
		lsa.header.lsh_age = LSA_MAXAGE;
		ospf_set_birth(lsa, now_);
		// Tyan: 05/08/2001, do flood outside this method
		//ospf_lsa_expire(lsa);
	}

	
	protected OSPF_Interface ospf_if_lookup_by_addr(long addr) 
	{
		int i;
		int no = ospf_if_list.size();
		for (i = 0; i < no; i++) {
			OSPF_Interface oif = (OSPF_Interface) ospf_if_list.elementAt(i);
			// Tyan: dont do this in case the (unestablished) neighbor is either
			//       not up or not ospf-capable
			//if( oif == null)
			//	return null; // debug
			if( oif != null && oif.if_id == addr )
				return oif;
		}
		return null;
	}
	
	/**
	 * xxx: Specify the area id for this router (Needed to be modified)
	 */
	public void ospf_set_area_id( int id_) {
		if ( area_list == null)
			area_list = new Vector();
		int no = area_list.size();
		int i;
		for ( i = 0; i < no; i++) {
			OSPF_Area area = (OSPF_Area) area_list.elementAt(i);
			if ( area.area_id == id_)
				return; // this id_ has already been in area_list
		}
		OSPF_Area new_area = new OSPF_Area( id_ );
		area_list.addElement(new_area);
	}
	
	protected void ospf_dbdesc_seqnum_init ( OSPF_Neighbor nbr)
	{
		int now = (int) getTime();
		nbr.dd_seqnum = now;

		//if (isDebugEnabled()) debug("set dbdesc seqnum " + nbr.dd_seqnum + " for " + nbr.rtr_id);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//  Routing Table releated Routine
	///////////////////////////////////////////////////////////////////////////////////////////
	/*
	protected void calculate_route_table( LSA_Database ls_db) {

		// 1: calculate the shortest path tree within an area 
		// 1.1: only links between routers and transit networks are considered
		// using Dijkstra algorithm 
			
		// (1) initialization: set the SPF be only root 
		Dijkstra dijkstra = new Dijkstra (Database.RouterLink, ROUTER_NHID);
	
		if (!dijkstra.findAllShortestPaths () || 
		    !dijkstra.computed ()) return;
		else if (show_rtg_tbl) {
		}

		// The link to the stub network will be processed in the step 1.2 
		// if (ls == stub_network) 
		//	keep_read_next_one; 
		
		
		// 1.2 add the links to stub into the leaves of the SPF tree 
	}*/

	///////////////////////////////////////////////////////////////////////////////////////////
	//  MISC. UTILITY METHODS
	///////////////////////////////////////////////////////////////////////////////////////////

	/** Remove lsa from area's lsdb as well as delay-ack-list in all interfaces
	 */
	void ospf_lsdb_remove (OSPF_LSA lsa, OSPF_Area area_) {
		int if_no = area_.if_list.size();
		Vector if_list = area_.if_list;
		
		for(int i = 0; i < if_no; i++) {
			OSPF_Interface oif = (OSPF_Interface) if_list.elementAt(i);
			oif.lsa_delayed_ack.removeElement(lsa);
		}
		area_.ls_db.ospf_lsdb_remove ( lsa );
	}

	/** Remove lsa from all neighbors' retransmission list */
	void ospf_remove_retrans (OSPF_LSA lsa, OSPF_Area area_) {
		int no = area_.if_list.size();
		int i, j;
		for ( i = 0; i < no; i++) {
			OSPF_Interface oif = (OSPF_Interface) area_.if_list.elementAt(i);
			int nbr_no = oif.neighbor_list.size();
			for ( j = 0; j < nbr_no; j++) {
				OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(j);
				_nbr_remove_retrans( lsa, nbr);
			}
		}
	}

	/**
	 * lsa_changed is called when the databse add a new LSA
	 * (Also including the modification of the LSA because the old LSA will be removed
	 * and the new LSA will be added)
	 */
	int lsa_change ( OSPF_LSA lsa)
	{
		OSPF_Interface oif;
		OSPF_Area area;
		
		switch ( lsa.header.lsh_type ) {
			case OSPF_ROUTER_LSA:
			case OSPF_NETWORK_LSA:
				area = ( OSPF_Area )lsa.scope;
				// Note: area.router_lsa_self could be null if
				// handshaking is still going on for the first neighbor
				// whose first lsa update arrives sooner than last dbdesc or lsa reply
				// that finishes the handshaking
				if (area.router_lsa_self != null)
					ospf_spf_calculate ( area );
				break;
				
/*    case LST_LINK_LSA:
      o6if = (struct ospf6_interface *)lsa->scope;
      area = (struct area *) o6if->area;
      if (area->spf_calc == (struct thread *)NULL)
        area->spf_calc = thread_add_event (master, spf_calculation,
                                           area, 0);
      if (area->route_calc == (struct thread *)NULL)
        area->route_calc = thread_add_event (master, ospf6_route_calc,
                                             area, 0);
      break;
    case LST_INTRA_AREA_PREFIX_LSA:
      area = (struct area *)lsa->scope;
      if (area->route_calc == (struct thread *)NULL)
        area->route_calc = thread_add_event (master, ospf6_route_calc,
                                             area, 0);
      break;
    case LST_AS_EXTERNAL_LSA:
      ospf6 = (struct ospf6 *)lsa->scope;
      area = (struct area *)ospf6->area_list->head->data;
      if (area->route_calc == (struct thread *)NULL)
        area->route_calc = thread_add_event (master, ospf6_route_calc,
                                             area, 0);
      break;*/
			default:
				break;
		}
		return 0;
	}
	
	/**
	 * RFC2328: Table 19: Sending link state acknowledgements.
	 * 
	 * XXX, I don't remember why No Ack, when MaxAge, no instance and
	 * no neighbor ExChange or Loading. and more, the circumstance should
	 * be processed at lsa_receive() 
	 */
	int ack_type (OSPF_LSA lsa, int ismore_recent, int impliedack_flag, int duplicate_flag, int now_)
	{
		int age;
		OSPF_LSA_Header header = lsa.header;

		OSPF_Interface ospf_interface = lsa.from.ospf_interface;
		if ( lsa.floodback_flag == 1) {
			//if (isDebugEnabled()) debug(": this is flood back");
			return NO_ACK;
		} else if (ismore_recent < 0 && lsa.floodback_flag == 0) {
			/* if (ospf6_interface->state == IFS_BDR) {
				zlog_info ("    : I'm BDR");
				if (ospf6_interface->dr == this->from->rtr_id) {
					zlog_info ("    : this is from DR");
					return DELAYED_ACK;
				} else {
					zlog_info ("    : this is not from DR, do nothing");
					return NO_ACK;
				}
			} else*/ {
				return DELAYED_ACK;
			}
		} else if ( (duplicate_flag == 1) && (impliedack_flag == 1) ) {
			/*if (isDebugEnabled()) debug(": is duplicate && implied");*/
			/* if (ospf6_interface->state == IFS_BDR) {
				if (ospf6_interface->dr == this->from->rtr_id) {
					zlog_info ("    : is from DR");
					return DELAYED_ACK;
				} else {
					zlog_info ("    : is not from DR, do nothing");
					return NO_ACK;
				}
			} else */ {
				return NO_ACK;
			}
		} else if ( (duplicate_flag == 1) && (impliedack_flag == 0)) {
			return DIRECT_ACK;
		}
		// Tyan: 05/08/2001, this has been done in lsa_receive()
		/*
		else if ( (age = header.ospf_age_current( now_ )) == LSA_MAXAGE) {
			OSPF_LSA lsa_ = ospf_lsdb_lookup (header.lsh_type, header.lsh_id, header.lsh_advtr, lsa.scope);
			if ( lsa_ == null ) {
				// no current instance in lsdb
				int if_no, nbr_no;
				int i,j;
				if_no = ospf_interface.area.if_list.size();
				for( i = 0; i< if_no; i++) {
					OSPF_Interface oif = (OSPF_Interface) ospf_interface.area.if_list.elementAt(i);
					nbr_no = oif.neighbor_list.size();
					for ( j = 0; j< nbr_no; j++) {
						OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(j);
						if (nbr.state == NBS_EXCHANGE || nbr.state == NBS_LOADING)
							return NO_ACK;
					}
				}
				return DIRECT_ACK;
			}
		}
		*/
		return NO_ACK;
	}

	/**
	 * check if the newly received LSA is originated by itself
	 * ref: sec. 13.4 
	 */
	boolean is_self_originated (OSPF_LSA lsa, OSPF_Area area )
	{
		/* check router id */
		if (lsa.header.lsh_type == OSPF_NETWORK_LSA) {
			int ls_id = lsa.header.lsh_id;
			int no = area.if_list.size();
			int i;
			for ( i = 0; i < no ; i++) {
				OSPF_Interface oif = (OSPF_Interface) area.if_list.elementAt(i);
				if ( ls_id == oif.lladdr)
					return true;
			}
			return false;
		}
		else
			return lsa.header.lsh_advtr == router_id;
	}

	// clear the area data structure, including ls timeouts(XXX) and if list
	void _area_clear(OSPF_Area area_)
	{
		if (area_.if_list != null) {
			for (int i=0; i<area_.if_list.size(); i++) {
				OSPF_Interface if_ = (OSPF_Interface)area_.if_list.elementAt(i);
				if (if_ != null) _if_clear(if_);
			}
		}
		area_.reset();
	}

	// clear the interface data structure, including if timeouts and neighbor list
	void _if_clear(OSPF_Interface if_)
	{
		if( if_.Hello_TimeOut_EVT != null)
			cancelTimeout( if_.Hello_TimeOut_EVT.handle );
		if( if_.Ack_Delay_Reach_EVT != null)
			cancelTimeout( if_.Ack_Delay_Reach_EVT.handle );
		if (if_.neighbor_list != null) {
			for (int i=0; i<if_.neighbor_list.size(); i++) {
				OSPF_Neighbor nbr_ = (OSPF_Neighbor)if_.neighbor_list.elementAt(i);
				if (nbr_ != null) _nbr_clear(nbr_);
			}
		}
		if_.clear();
	}

	// Tyan: this method is moved from OSPF_Neighbor
	// 		but when to call this method?  a maxage timeout possibly?
	/** 
	 * maxage LSA remover
	 * A MaxAge LSA must be removed immediately from the router's link state database
	 * as soon as both a) it is no longer contained on any neighbor Link state 
	 * retransmission lists and b) none of the router's neighbors are in states 
	 * Exchange or Loading.
	 * ref: sec 14.
	 */
	private void ospf_lsdb_check_maxage_lsa ()
	{
		int i, j;
		int area_no = area_list.size(), lsa_no;
		
		/* if any neighbor is in state Exchange or Loading, quit */
		if (_nbr_any_exchange_loading()) return;

  // for AS LSDB
	/*  for (i = listhead (o6->lsdb); i; nextnode (i))
    {
      lsa = (struct ospf6_lsa *) getdata (i);

      if (ospf6_age_current (lsa) == MAXAGE
          && listcount (lsa->retrans_nbr) == 0)
        list_add_node (remove_list, lsa);
    } */

		/* for Area LSDB */
		int now_ = (int)getTime();
		for (i = 0; i< area_no; i++) {
			OSPF_Area oa = (OSPF_Area) area_list.elementAt(i);
			for( j = 0; j < oa.ls_db.ls_list.size(); j++) {
				OSPF_LSA lsa = (OSPF_LSA) oa.ls_db.ls_list.elementAt(j);
				if ( (lsa.header.ospf_age_current( now_ ) == LSA_MAXAGE)
					 && (lsa.from.ospf_lookup_retrans(lsa.header) == null) ) {
					// remove the lsa from area's database and interfaces'
					// delay-ack-list
					ospf_lsdb_remove (lsa, oa);
					j--;
				}
	        }
		}
		
  // for Interface LSDB
  /* for (i = listhead (o6->area_list); i; nextnode (i))
    {
      o6a = (struct area *) getdata (i);

      for (j = listhead (o6a->if_list); j; nextnode (j))
        {
          o6i = (struct ospf6_interface *) getdata (j);

          for (k = listhead (o6i->lsdb); k; nextnode (k))
            {
              lsa = (struct ospf6_lsa *) getdata (k);

              if (ospf6_age_current (lsa) == MAXAGE
                  && listcount (lsa->retrans_nbr) == 0)
                list_add_node (remove_list, lsa);
            }
        }
    }*/
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//  NEIGHBOR OPERATION 
	///////////////////////////////////////////////////////////////////////////////////////////

	/** remove lsa from request list of neighbor */
	void _nbr_remove_request (OSPF_LSA lsa, OSPF_Neighbor nbr)
	{
		if (nbr.ospf_lookup_request (lsa) == null)
			return;
		nbr.requestlist.removeElement(lsa);
		if (nbr.requestlist.size() == 0 && nbr.LSreq_Retrans_EVT != null) {
			cancelTimeout(nbr.LSreq_Retrans_EVT.handle);
			nbr.LSreq_Retrans_EVT = null;
			_nbr_Loading_Done(nbr);
		}
	}

	/** remove all lsa from retrans list of neighbor */
	void _nbr_remove_requests_all (OSPF_Neighbor nbr)
	{
		nbr.requestlist.removeAllElements();
		if (nbr.LSreq_Retrans_EVT != null) {
			cancelTimeout(nbr.LSreq_Retrans_EVT.handle);
			nbr.LSreq_Retrans_EVT = null;
			_nbr_Loading_Done(nbr);
		}
	}

	/** remove all lsa from retrans list of neighbor */
	void _nbr_remove_retrans_all (OSPF_Neighbor nbr)
	{
		nbr.retranslist.removeAllElements();
		if (nbr.LSupdate_Retrans_EVT != null) {
			cancelTimeout(nbr.LSupdate_Retrans_EVT.handle);
			nbr.LSupdate_Retrans_EVT = null;
		}
	}

	/** remove lsa from retrans list of neighbor */
	void _nbr_remove_retrans (OSPF_LSA lsa, OSPF_Neighbor nbr)
	{
		if (nbr.ospf_lookup_retrans (lsa.header) == null)
			return;
		nbr.retranslist.removeElement(lsa);
		// list_delete_by_val (lsa->retrans_nbr, nbr);
		/* ospf6_lsa_unlock (lsa); */

		// Tyan: why here? 
		/* check if any lsa which is aging & can be removed */
		//ospf_lsdb_check_maxage_lsa ( top_ospf );

		if (nbr.retranslist.size() == 0 && nbr.LSupdate_Retrans_EVT != null) {
			cancelTimeout(nbr.LSupdate_Retrans_EVT.handle);
			nbr.LSupdate_Retrans_EVT = null;
		}
	}

	void _nbr_list_cleared_of_lsa (OSPF_Neighbor nbr)
	{
		/* dd_retrans is not used */
		/*list_delete_all_node (nbr->dd_retrans);*/
		_nbr_remove_requests_all(nbr);
		_nbr_remove_retrans_all(nbr);
		nbr.ospf_remove_summary_all();
	}
	
	/**
	 *  Determine if the neighbor is adjacent to this router
	 *  ref: section 10.4 
	 */
	boolean _nbr_determineAdjacency(OSPF_Neighbor nbr)
	{
		return nbr.ospf_interface.state == IFS_PTOP
			|| nbr.ospf_interface.state == IFS_DR
			|| nbr.ospf_interface.state == IFS_BDR;
			//|| nbr.rtr_id == nbr.ospf_interface.dr
			//|| nbr.rtr_id == nbr.ospf_interface.bdr
			//|| router_id == nbr.ospf_interface.dr
			//|| router_id == nbr.ospf_interface.bdr;
	}

	void _nbr_Twoway_Received(OSPF_Neighbor nbr)
	{
		nbr.ospf_interface.neighbor_change();
		nbr.isAdjacent = _nbr_determineAdjacency(nbr);

		if (nbr.isAdjacent)
			_nbr_state_change (nbr, NBS_EXSTART, "Form_Adjacency");
		else {
			_nbr_state_change (nbr, NBS_TWOWAY, "From_2Way");
			return;
		}

		// set the MS(master), M(more) and I(initialize) bits, declare itself as the master
		nbr.dd_msbit = 1;
		nbr.dd_mbit = 1;
		nbr.dd_ibit = 1;
		
		// ref: sec. 10.8
		// thread_add_event (master, ospf6_send_dbdesc, nbr, 0);
		ospf_send_dbdesc(nbr);
	}

	void _nbr_clear(OSPF_Neighbor nbr)
	{
		if (nbr.Nbr_Inactive_EVT != null)
			cancelTimeout(nbr.Nbr_Inactive_EVT.handle);
		if (nbr.LSDBdesc_Retrans_EVT != null)
			cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
		if (nbr.LSreq_Retrans_EVT != null)
			cancelTimeout(nbr.LSreq_Retrans_EVT.handle);
		if (nbr.LSupdate_Retrans_EVT != null)
			cancelTimeout(nbr.LSupdate_Retrans_EVT.handle);
		nbr.clear();
	}

	// Tyan, 05/08/2001
	// returns true if any neighbor is in exchange or loading
	// this is one of the conditions to determine whether to keep or remove a maxage lsa
	// see section 13 and 14
	boolean _nbr_any_exchange_loading()
	{
		for (int k=0; k<area_list.size(); k++) {
			OSPF_Area area = (OSPF_Area)area_list.elementAt(k);
			int if_no = area.if_list.size();
			for(int i = 0; i< if_no; i++) {
				OSPF_Interface oif = (OSPF_Interface) area.if_list.elementAt(i);
				int nbr_no = oif.neighbor_list.size();
				for (int j = 0; j< nbr_no; j++) {
					OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(j);
					if (nbr.state == NBS_EXCHANGE || nbr.state == NBS_LOADING)
						return true;
				}
			}
		}
		return false;
	}

	// Tyan, 05/08/2001
	// returns true if any neighbor is in exchange or loading or the lsa is in any neighbor's retranslist
	// see sec. 14
	boolean _nbr_in_any_retranslist(OSPF_LSA lsa)
	{
		for (int k=0; k<area_list.size(); k++) {
			OSPF_Area area = (OSPF_Area)area_list.elementAt(k);
			int if_no = area.if_list.size();
			for(int i = 0; i< if_no; i++) {
				OSPF_Interface oif = (OSPF_Interface) area.if_list.elementAt(i);
				int nbr_no = oif.neighbor_list.size();
				for (int j = 0; j< nbr_no; j++) {
					OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(j);
					if (nbr.state == NBS_EXCHANGE || nbr.state == NBS_LOADING)
						return true;
					else if (nbr.retranslist.indexOf(lsa) >= 0)
						return true;
				}
			}
		}
		return false;
	}

	// check validity and put lsa in reqestlist if needed.
	// XXX: this function should return -1 if stub area and 
	// if as-external-lsa contained. this is not yet
	int _nbr_check_lsdb (OSPF_Neighbor nbr, OSPF_DBdesc dbdesc )
	{
		OSPF_LSA	have, received;
		have = received = null;
	
		Object	scope;
		int now_ = (int)getTime();
		int no = dbdesc.LSA_no;
		int i;
		/* for each LSA listed in DD */
		for (i = 0; i < no; i++) {
			OSPF_LSA_Header lsh = (OSPF_LSA_Header) dbdesc.LSA_hdr_list.elementAt(i);
			/* log */
			/*if (IS_OSPF6_DUMP_DBDESC) {
				ospf6_lsa_hdr_str (lsh, buf, sizeof (buf));
				zlog_info ("  %s", buf);
			}*/
			/* make lsa structure for this LSA */
			received = new OSPF_LSA(lsh); // lsh is not cloned
			ospf_set_birth(received, now_);

			/* set scope */
			switch (Util.translate_scope ( lsh.lsh_type) ) {
			/*case OSPF_LSA_Header.SCOPE_LINKLOCAL:
				scope = (void *) nbr->ospf6_interface;
				break;*/
			case SCOPE_AREA:
				scope = (OSPF_Area) nbr.ospf_interface.area;
				break;
			/* case OSPF_LSA_Header.SCOPE_AS:
				scope = (void *) nbr->ospf6_interface->area->ospf6;
				break;
			case OSPF_LSA_Header.SCOPE_RESERVED:*/
			default:
				if (isErrorNoticeEnabled())
					error("_nbr_check_lsdb()", " *** Warn *** :unsupported scope, check DD failed");
				return -1;
			}
			received.scope = scope;
			/* set sending neighbor */
			received.from = nbr;

			/* if already have newer database copy, check next LSA */
			have = ospf_lsdb_lookup (lsh.lsh_type, lsh.lsh_id, lsh.lsh_advtr, scope);
			if ( have == null ) {
				/* if we don't have database copy, add request */
				nbr.ospf_add_request (received);
			} else {
				/* if database copy is less recent, add request */
				if ( Util.ospf_lsa_check_recent (received, have, now_) < 0)
					nbr.ospf_add_request (received);
			}
		}
		return 0;
	}

	/** Change the OSPF_Neighbor state */
	void _nbr_state_change (OSPF_Neighbor nbr, int nbs_next, String reason)
	{
		int nbs_previous = nbr.state;
		if (nbs_previous == nbs_next) return;
		nbr.state = nbs_next;

		/* statistics */
		nbr.ospf_stat_state_changed++;

		/* log */
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_NEIGHBOR))) {
			if (reason == null)
				debug("Neighbor " + nbr.rtr_id + "'s state changed: " + NBR_STATES[nbs_previous] + "->"
					+ NBR_STATES[nbs_next]);
			else
				debug("Neighbor " + nbr.rtr_id + "'s state changed: " + NBR_STATES[nbs_previous] + "->"
					+ NBR_STATES[nbs_next] + ", " + reason);
		}

		OSPF_Area area = nbr.ospf_interface.area;

		if (nbs_previous == NBS_FULL || nbs_next == NBS_FULL) {
			OSPF_LSA lsa = ospf_make_router_lsa (area);
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR)) {
				debug("----- NBR_FULL_CHANGE, nbr:" + nbr.rtr_id + ", new_area_lsa:" + lsa);
			}
			if ( lsa == null ) {
				if (isErrorNoticeEnabled())
					error("_nbr_state_change()", "cannot make router_lsa at Area " + area.area_id);
			}
			else {
				ospf_lsdb_install (lsa, area); // install it like others
				ospf_router_lsa_flood(area, true/*check*/);
			}
		}

		// Tyan: ???
		///* check if any lsa which is aging & can be removed */
		//ospf_lsdb_check_maxage_lsa ( (OSPF)ospf );
	}

	/** receive the hello message from the neighbor */
	void _nbr_Hello_Received (OSPF_Neighbor nbr, String msg_)
	{
		if ( nbr.state <= NBS_DOWN ) _nbr_state_change (nbr, NBS_INIT, msg_);
	}

	/** Neighbor finish exchanging database info.
	 */
	int _nbr_Exchange_Done ( OSPF_Neighbor nbr )
	{
		if (nbr.state != NBS_EXCHANGE)
		    return 0;

		if ( nbr.LSDBdesc_Retrans_EVT != null ) {
			cancelTimeout(nbr.LSDBdesc_Retrans_EVT.handle);
			nbr.LSDBdesc_Retrans_EVT = null;
		}
		/* 	if (nbr->thread_dbdesc_retrans)
		    thread_cancel (nbr->thread_dbdesc_retrans);
		nbr->thread_dbdesc_retrans = (struct thread *) NULL; */

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("Neighbor Event : *ExchangeDone* with " + nbr.rtr_id );

		_nbr_remove_retrans_all(nbr);
		// list_delete_all_node (nbr->dd_retrans);

		if ( nbr.requestlist.size() == 0 ) {
			_nbr_state_change (nbr, NBS_FULL, "Requestlist Empty");
		}
		else {
			// thread_add_event (master, ospf6_send_lsreq, nbr, 0);
			ospf_send_lsreq(nbr);
			_nbr_state_change (nbr, NBS_LOADING, "Requestlist Not Empty");
		}
		return 0;
	}


	/**
	 * Link State Updates have been received for all out-of-date portions of the database.
	 */
	int _nbr_Loading_Done (OSPF_Neighbor nbr)
	{
		if (nbr.state != NBS_LOADING)
			return 0;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("Neighbor Event : *LoadingDone* with " + nbr.rtr_id );

		if( nbr.requestlist.size() != 0) {
			if (isErrorNoticeEnabled())
				error("_nbr_Loading_Done()", "%%% ERROR %%%: requestlist.size = 0" );
			return 0;
		}

		_nbr_state_change (nbr, NBS_FULL, "LoadingDone");
		return 0;
	}

	/** A Link State Request has been received for an LSA not contained in the database.
	 * 	This indicates an error	in the Database Exchange process.
	 */
	void _nbr_Bad_LSreq ( OSPF_Neighbor nbr )
	{
		if (nbr.state < NBS_EXCHANGE) return;

		/* statistics */
		nbr.ospf_stat_bad_lsreq++;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("Neighbor Event : *BadLSReq* from " + nbr.rtr_id );

		_nbr_state_change (nbr, NBS_EXSTART, "BadLSReq");
		nbr.dd_msbit = 1;
		nbr.dd_mbit = 1;
		nbr.dd_ibit = 1;
		_nbr_list_cleared_of_lsa (nbr);
		//thread_add_event (master, ospf6_send_dbdesc, nbr, 0);
		ospf_send_dbdesc(nbr);
	}

	/**
	 * States releated to error message 
	 * A Database Description packet has been received that either
	 * a) has an unexpected DD sequence number,
	 * b) unexpectedly has the Init bit set or	
	 * c) has an Options field	differing from the last Options field received
	 *    in a Database Description packet.
	 * Any of these conditions indicate that some error has	occurred 
	 * during adjacency establishment.
	 */	
	void _nbr_Seqnumber_Mismatch ( OSPF_Neighbor nbr )
	{
		if ( nbr.state < NBS_EXCHANGE ) return;

		/* statistics */
		nbr.ospf_stat_seqnum_mismatch++;

		if (isDebugEnabled() && isDebugEnabledAt(DEBUG_NEIGHBOR))
			debug("Neighbor Event : *SeqNumberMismatch* with " + nbr.rtr_id );

		_nbr_state_change (nbr, NBS_EXSTART, "SeqNumberMismatch");

		nbr.dd_msbit = 1;
		nbr.dd_mbit  = 1;
		nbr.dd_ibit  = 1;
		// xxx:
		_nbr_list_cleared_of_lsa (nbr);
		//thread_add_event (master, ospf6_send_dbdesc, nbr, 0);
		ospf_send_dbdesc(nbr);
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//  SPF OPERATIONS
	///////////////////////////////////////////////////////////////////////////////////////////

	/** destination type */
	final static int DTYPE_ROUTER		 = 0;
	final static int DTYPE_NETWORK		 = 1;
	final static String[] DTYPES = {"ROUTER", "NETWORK"};
	
	/** Path-types (from RFC2328 11), decreasing order of preference */
	final static int PTYPE_INTRA          = 0 ;    /* intra-area */
	final static int PTYPE_INTER          = 1 ;    /* inter-area */
	final static int PTYPE_TYPE1_EXTERNAL = 2 ;   /* type 1 external */
	final static int PTYPE_TYPE2_EXTERNAL = 3 ;   /* type 2 external */
	final static String[] PATH_TYPES = {"INTRA_AREA", "INTER_AREA", "TYPE1_EXT", "TYPE2_EXT"};

	static final int OSPF_VERTEX_ROUTER   = 0;
	static final int OSPF_VERTEX_NETWORK  = 1;
	final static String[] VERTEX_TYPES = DTYPES;

	/**
	 * Initialization of calculatinf SPF
	 */
	protected void ospf_spf_init ( OSPF_Area area)
	{
		/* Install myself as root */
		OSPF_LSA myself = area.ls_db.ospf_lsdb_lookup ( OSPF_ROUTER_LSA, router_id, router_id);
		if ( myself == null ) {
			if (isErrorNoticeEnabled())
				error("ospf_spf_init()", " *** Router-LSA of myself not found");
			return ;
		}

		OSPF_SPF_vertex v;
		if(area.spf_root != null) {
			// clear the invalid spf root
			//area.vertex_list.removeElement(area.spf_root);
			int no = area.vertex_list.size();
			for (int i=0; i<no; i++) {
				v = (OSPF_SPF_vertex)area.vertex_list.elementAt(i);
				v.reset();
			}
			v = area.spf_root;
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF)) {
				debug("   original myself: " + v.vtx_lsa);
				debug("   current  myself: " + myself);
			}
			v.vtx_lsa = myself;
			v.intree = true;
		}
		else {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF))
				debug("   create root: myself: " + myself);
			/* Create root node. */
			v = new OSPF_SPF_vertex (myself, 0, true /*intree*/);
			/* install the root into area data structure */
			area.spf_root = v;
			// add this vertex into area's spf tree list
			area.vertex_list.addElement(v);
		}
	}


	/* RFC2328 Section 16.1 (2). */
	// V: newly-added vertex
	// Add V's neighbors to candidate list (sorted by cost)
	private void ospf_spf_next(OSPF_SPF_vertex V, OSPF_Area area, TreeMapQueue candidate, int now_)
	{
		OSPF_LSA w_lsa = null;

		Router_LSA  r_lsa = (Router_LSA) V.vtx_lsa;
		int link_num = r_lsa.link_no;
		int l, i;
		int linkback;
		Router_LSA_Link currentlink = null;
		
		/* If this is a router-LSA, and bit V of the router-LSA (see Section
			A.4.2:RFC2328) is set, set Area A's TransitCapability to TRUE.  */
		/*if (this.vtx_type == OSPF_VERTEX_ROUTER) {
			if (IS_ROUTER_LSA_VIRTUAL ((struct router_lsa *) this.lsa))
				area->transit = OSPF_TRANSIT_TRUE;
		}*/
		for ( l = 0; l < link_num; l++) {
			linkback = 0;
			/* update the link info. */
			
			/* In case of V is Router-LSA. */
			if (V.vtx_lsa.header.lsh_type == OSPF_ROUTER_LSA) {
				currentlink = (Router_LSA_Link) r_lsa.ls_link_list.elementAt(l);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF))
					debug("   check link: " + currentlink);

				/* (a) If this is a link to a stub network, examine the next
				 link in V's LSA.  Links to stub networks will be
				 considered in the second stage of the shortest path
				 calculation. */
				if (currentlink.type == LSA_LINK_TYPE_STUB) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
						debug("   stub network, considered later");
					continue;
				}

				/* (b) Otherwise, W is a transit vertex (router or transit
			     network).  Look up the vertex W's LSA (router-LSA or
				 network-LSA) in Area A's link state database. */
				/* (b) examine the lsa age, and check if it link back to V */
				switch (currentlink.type) {
					case LSA_LINK_TYPE_VIRTUALLINK:
					case LSA_LINK_TYPE_POINTOPOINT:
						/*if (currentlink.type == LSA_LINK_TYPE_VIRTUALLINK)
							if (isDebugEnabled()) 
								debug("Z: looking up LSA through VL: " + currentlink.link_id);*/

						/* ref: A.4.2 explan of link id: 
						if the link type is either router or transit network,the link id will be 
						the same as the lsa link state id, in order to look up this lsa in the database */
						w_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA,
											currentlink.link_id, currentlink.link_id);
						break;
/*	case LSA_LINK_TYPE_TRANSIT:		*/
/*						 zlog_info ("Z: Looking up Network LSA, ID: %s", currentlink.link_id);
						 w_lsa = ospf_lsa_lookup_by_id (area, OSPF_NETWORK_LSA, l->link_id);
						 break;*/
					default:
						if (isErrorNoticeEnabled())
							error("ospf_spf_next()", " *** Warn *** : Invalid LSA link type "
								+ currentlink.type);
						continue;
				} /* end of switch */
			} /* else {
// In case of V is Network-LSA. 
				r = (struct in_addr *) p ;
				p += sizeof (struct in_addr);

				// Lookup the vertex W's LSA. 
				w_lsa = ospf_lsa_lookup_by_id (area, OSPF_ROUTER_LSA, *r); 
			}*/

			/* (b cont.) If the LSA does not exist, or its LS age is equal
			 to MaxAge, or it does not have a link back to vertex V,
			 examine the next link in V's LSA. */
			if (w_lsa== null || w_lsa.header.ospf_age_current (now_) == LSA_MAXAGE) {
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF)) {
					if (w_lsa == null)
						debug("   LSA does not exist");
					else
						debug("   LS's age is max: " + w_lsa);
				}
				continue ; // next link
			}

			/* Examin if this LSA have link back to V */
			if ( ospf_lsa_has_link (w_lsa, V.vtx_lsa) == 0 ) {
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF)) {
					debug("   node doesn't have a link back (no w-->v)");
					debug("      w:" + w_lsa);
					debug("      v:" + V.vtx_lsa);
				}
				continue;
			}

			/* (c) If vertex W is already on the shortest-path tree, examine
				the next link in the LSA. */
			if( vertex_in_tree( currentlink.link_id, area.vertex_list ) ) {
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF))
					debug("   node already in-tree");
				continue;
			}
			
			/* (d) Calculate the link state cost D of the resulting path
			 from the root to vertex W.  D is equal to the sum of the link
			 state cost of the (already calculated) shortest path to
			 vertex V and the advertised cost of the link between vertices
			 V and W. */

			/* calculate link cost D. */
			int dist;
			if (V.vtx_lsa.header.lsh_type == OSPF_ROUTER_LSA)
				dist = V.vtx_distance + currentlink.metric;
			else
				dist = V.vtx_distance;

			/* Is there already vertex W in candidate list? */
			// Tyan: link type and vtx type are different stories.
			OSPF_SPF_vertex CW = ospf_vertex_lookup (candidate, currentlink.link_id);//, currentlink.type);
			if ( CW == null) {
				/* Calculate nexthop to W. */
				OSPF_SPF_vertex W = ospf_vertex_lookup (area.vertex_list, currentlink.link_id);
				if (W == null) {
					W = new OSPF_SPF_vertex ();
					area.vertex_list.addElement(W);
				}
				W.set(w_lsa, dist, false);
				W.parent = V; // Tyan
				ospf_nexthop_calculation (area, V, W);
				ospf_install_candidate (candidate, W);
				if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF))
					debug("   add candidate: " + w_lsa);
			} else {
				/* if D is greater than. */
				if (CW.vtx_distance < dist) {
					if (isDebugEnabled() && isDebugEnabledAt(DEBUG_DETAIL) && isDebugEnabledAt(DEBUG_SPF))
						debug("   original's distance is smaller");
					continue;
				}
				/* equal to. */
				else if ( CW.vtx_distance == dist) {
					/* make vertex W. */
					OSPF_SPF_vertex W = new OSPF_SPF_vertex (w_lsa, dist, false);
					/* Calculate nexthop to W. */
					ospf_nexthop_calculation (area, V, W);
					/* add next hop list from W to CW */
					int hop_no = W.nexthops.size();
					for ( i = 0; i < hop_no; i++) {
						Object tmp_ = W.nexthops.elementAt(i);
						if (CW.nexthops.indexOf(tmp_) < 0)
							CW.nexthops.addElement(tmp_);
					}
					W.nexthops.removeAllElements();
				}
				/* less than. */
				else {
					/* make vertex W. */
					OSPF_SPF_vertex W = new OSPF_SPF_vertex (w_lsa, dist, false);

					/* Calculate nexthop. */
					ospf_nexthop_calculation (area, V, W);
					// Remove old vertex from candidate list. 
					// and replace it with new in vertex_list
					candidate.remove(CW);
					area.vertex_list.setElementAt(W, area.vertex_list.indexOf(CW));

					/* Install new to candidate. */
					ospf_install_candidate (candidate, W);
				}
			}
		} /* end of for each link */
	}

	/** Calculate nexthop from root to vertex W.  Ref: sec. 16.1.1 */
	protected void ospf_nexthop_calculation ( OSPF_Area area, OSPF_SPF_vertex v, OSPF_SPF_vertex w)
	{
		int no;
		OSPF_Interface oif = null;
		OSPF_Neighbor  nbr = null;
		OSPF_SPF_vertex nh;
		
		/* W's parent is root. */
		if (v == area.spf_root) {
			// Tyan: root's nexthop to w is w itself
			nh = w; // v.ospf_nexthop_new ();
			long addr = ospf_nexthop_out_if_addr (v, w);

			if ( addr != -1 )
				oif = ospf_if_lookup_by_addr (addr);

			if (oif != null) {
				nh.ifp = oif;

				/* Tyan: unnecessary
				if (w.vtx_type == OSPF_VERTEX_ROUTER) {
					nbr = oif.ospf_nbr_lookup_by_routerid (w.vtx_id);
					if (nbr != null)
						nh.vtx_id = nbr.rtr_id;
					else
						if (isDebugEnabled()) debug("Z: couldn't find the nbr");
				} 
				*/
			}

			if (w.vtx_type == OSPF_VERTEX_NETWORK)
				nh.vtx_id = 0; 

			/*if (isDebugEnabled()) debug("Z: resolved next hop: int: %s, next hop: %s",nh.ifp.name, nh.vtx_id);*/
			w.nexthops.addElement(nh);
			
			// Tyan: ??
			//ospf_lsa_has_link (w.vtx_lsa, v.vtx_lsa);

			//if (isDebugEnabled()) debug("Z: we use " + w.vtx_id);
			//if (isDebugEnabled()) debug("Z: to reach rtr " + w.vtx_lsa.header.lsh_id);
			return;
		}
		/* In case of W's parent is network connected to root. */
		else if (v.vtx_type == OSPF_VERTEX_NETWORK) {
			no = v.nexthops.size();
			for (int i = 0; i < no; i++) {
				OSPF_SPF_vertex x = (OSPF_SPF_vertex) v.nexthops.elementAt(i);
				if (x.parent == area.spf_root) {
					// Tyan: root's nexthop to w is w itself
					nh = w;//v.ospf_nexthop_new();
					long addr = ospf_nexthop_out_if_addr (v, w);
					nh.ifp = x.ifp;
					//nh.vtx_id = addr;
					w.nexthops.addElement(nh);
					return;
				}
			}
		}
		/* here the parent is not the root */
		/* W Inherit V's nexthop. */
		w.nexthops.setSize(v.nexthops.size());
		for (int i=0; i<w.nexthops.size(); i++)
			w.nexthops.setElementAt(v.nexthops.elementAt(i), i);
		/* Tyan: what!?
		no = w.nexthops.size();
		for (int i = 0; i < no; i++) {
			OSPF_SPF_vertex next = (OSPF_SPF_vertex) w.nexthops.elementAt(i);
			next.parent = v;
		}
		*/
	}

	/* Add vertex V to SPF tree. */
	// Tyan: not used anymore
	private void ospf_spf_install (OSPF_SPF_vertex V, OSPF_Area area, int now_)
	{
		if ( V.intree == true ) {
			error("ospf_spf_install()", "?! Check who set intree ");
			return;
		} else
			V.intree = true;
		
		transit_vertex_rtable_install (V, area, now_);
		// add this vertex into area's spf tree list
		area.vertex_list.addElement(V);
	}
		
	/**
	 * Calculating the shortest-path tree for an area. 
	 */
	void ospf_spf_calculate ( OSPF_Area area)
	{
		OSPF_SPF_vertex V;		
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_SPF)))
			debug("   ospf_spf_calculate: running Dijkstra for area " + area.area_id);

		/* Check router-lsa-self.  If self-router-lsa is not yet allocated,
			return this area's calculation. */
		if (area.ls_db.size() == 0 ) {
			/*if (isDebugEnabled()) debug("ospf_spf_calculate:Skip area " + area.area_id +"'s calculation due to empty router_lsa_self" );*/
			return;
		}

		/* RFC2328 16.1. (1). */
		/* Initialize the algorithm's data structures. */ 
		/* Clear the list of candidate vertices. */ 
		TreeMapQueue candidate = new TreeMapQueue();

		/* Initialize the shortest-path tree to only the root (which is the
			router doing the calculation). */
		ospf_spf_init (area);
					
		V = area.spf_root;

		/* Set Area A's TransitCapability to FALSE. */
		/*area->transit = OSPF_TRANSIT_FALSE;
		  area->shortcut_capability = 1; */

		int now_ = (int)getTime();
		for (;;) {
			/* RFC2328 16.1. (2). */
			
			if (debug) System.out.println("calculate candidates");
			ospf_spf_next(V, area, candidate, now_);
			
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   #candidates = " + candidate.getLength());
			

			/* RFC2328 16.1. (3). */
			/* If at this step the candidate list is empty, the shortest-
			path tree (of transit vertices) has been completely built and
			this stage of the procedure terminates. */
			if (candidate.isEmpty())
				break;

			/* Otherwise, choose the vertex belonging to the candidate list
			that is closest to the root, and add it to the shortest-path
			tree (removing it from the candidate list in the
			process). */ 
			V = (OSPF_SPF_vertex) candidate.dequeue();

			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   install vertex to SPF:" + V.vtx_id);
			/* Add to SPF tree. */
			/* RFC2328 16.1. (4). possible modify the routing table */
			// Tyan: all vertexes are now in vertex_list when created,
			//		make vertex intree now but modify routing table later
			//ospf_spf_install (V, area, now_);
			V.intree = true;

			/* Note that when there is a choice of vertices closest to the
		         root, network vertices must be chosen before router vertices
				 in order to necessarily find all equal-cost paths. */
			/* We don't do this at this moment, we should add the treatment
			     above codes. -- kunihiro. */

			/* RFC2328 16.1. (5). */
			/* Iterate the algorithm by returning to Step 2. */
		}
		
		// Tyan: clean up vertex_list and modify routing table here
		ospf_area_rt_install(area);

		/* Second stage of SPF calculation procedure's  */
		if (debug2) System.out.println(router_id + " process_stubs");
		//ospf_spf_process_stubs (area_spf_root, area/*, new_table*/);
		if (debug2) System.out.println(router_id + " end process_stubs");

		/* Increment SPF Calculation Counter. */
		area.spf_calculation++;
	}

	/**
	 * Install area's vertex_list to routing table
	 */
	protected void ospf_area_rt_install ( OSPF_Area area)
	{
		// vertex_list(0) is myself, don't add myself to routing table
		for (int i=1; i<area.vertex_list.size(); i++) {
			OSPF_SPF_vertex  new_en = (OSPF_SPF_vertex)area.vertex_list.elementAt(i);
			long destination_ = new_en.vtx_id;
			// XXX: should consider network mask
			RTKey key = new RTKey(0, 0, destination_, -1, 0, 0);
			RTEntry entry_ = null;

			/* set path type */
			if (new_en.intree)
				new_en.path_type = PTYPE_INTRA;
			else {
				entry_ = (RTEntry) removeRTEntry(key, RTConfig.MATCH_EXACT);		
				if (entry_ != null) {
					if( isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_SPF)))
						debug("Removes route entry to " + destination_);
				}
				continue;
			}
	
			/* add new_en to routing table */

			if (new_en.nexthops.size() == 0) {
				if (isErrorNoticeEnabled())
					error("ospf_area_rt_install()", "intree vertex does not have next hop");
				new_en.intree = false;
				i--; continue;
			}
			OSPF_SPF_vertex vnexthop_ = (OSPF_SPF_vertex)new_en.nexthops.firstElement();
			long nexthop_ = vnexthop_.vtx_id;
			int if_index = vnexthop_.ifp.if_id;
			entry_ = (RTEntry) retrieveRTEntry(key, RTConfig.MATCH_EXACT);		
			int[] ifs_ = null;
			OSPF_SPF_vertex  cur_en = null;
			if (entry_ != null && entry_.getExtension() instanceof OSPF_SPF_vertex)
				cur_en = (OSPF_SPF_vertex)entry_.getExtension();

			if (cur_en != null && cur_en != new_en) {
				if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
					debug("   *** impossible *** ospf_area_rt_install(): cur_en != new_en, ignored");
			}

			if (entry_ == null || cur_en == null) {
				addRTEntry(key, nexthop_, if_index, new_en, -1);
				if( isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_SPF))) {
					debug(router_id + " Add Route Entry " + destination_ + ", " + nexthop_
						+ "_{" + if_index + "}, cost " + new_en.vtx_distance
						+ ", lsa " + new_en.vtx_lsa);
				}
			}
			else if (entry_.getNextHop() != nexthop_ || (ifs_ = entry_._getOutIfs()) == null
				|| ifs_.length != 1 || ifs_[0] != if_index) {
				addRTEntry(key, nexthop_, if_index, new_en, -1);
				if( isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_SPF))) {
					debug(router_id + " Update Route Entry to " + destination_
						+ ": " + nexthop_ + "-{" + if_index + "}, cost " + new_en.vtx_distance
						+ ", lsa " + new_en.vtx_lsa);
				}
			}
		}
		for (int i=area.vertex_list.size()-1; i>=0; i--) {
			OSPF_SPF_vertex  v = (OSPF_SPF_vertex)area.vertex_list.elementAt(i);
			if (!v.intree) area.vertex_list.removeElementAt(i);
		}
	}

	/**
	 * Hold the intermediate results on area routing table
	 * Install a new entry into routing table
	 */
	// Tyan: to save space, use vertex as route entry
	// Tyan: not used anymore
	protected void transit_vertex_rtable_install ( OSPF_SPF_vertex v, OSPF_Area area, int now_)
	{
		//OSPF_RoutingEntry	en = null;
//		RTEntry			en = null ;
		long			prefix = Address.NULL_ADDR;
		int				dtype = 0;

		/* set router id */
		prefix = v.vtx_id;

		if ( v.vtx_type == OSPF_VERTEX_ROUTER) {
			Router_LSA rlsa = null;
			rlsa = (Router_LSA) v.vtx_lsa;
			dtype = DTYPE_ROUTER;
		} else if (v.vtx_type == OSPF_VERTEX_NETWORK ) {
	        dtype = DTYPE_NETWORK;
			prefix = v.vtx_id;
		} else {
			if (isErrorNoticeEnabled())
				error("transit_vertex_rtable_install()", " %%% ERROR %%% : unkown vertex type");
		}
		/* set routing entry */
		//en = new OSPF_RoutingEntry( dtype, area, PTYPE_INTRA, v.vtx_distance, v.vtx_lsa, v.nexthops);
		v.path_type = PTYPE_INTRA;

		/* add area table */
		//OSPF_SPF_vertex nh =  (OSPF_SPF_vertex) v.nexthops.elementAt(0);
		//ospf_route_add (prefix, en, nh, area);
		ospf_route_add (prefix, v, area, now_);
	}


		
	/**
	 * Add a routing entry into RoutingTable and synchronize with drcl.inet.core.RT
	 */
	// Tyan: not used anymore
	protected void ospf_route_add ( long dst, OSPF_SPF_vertex new_en, OSPF_Area area, int now_)
	//protected void ospf_route_add ( long dst, OSPF_RoutingEntry new_en, OSPF_SPF_vertex vnexthop_,
	//	OSPF_Area area )
	{
		//OSPF_RoutingEntry  cur_en = null;
		OSPF_SPF_vertex  cur_en = null;
		OSPF_SPF_vertex vnexthop_ = (OSPF_SPF_vertex)new_en.nexthops.firstElement();
		long nexthop_ = vnexthop_.vtx_id;
		int if_index = vnexthop_.ifp.if_id;
		OSPF_LSA lsa;
			
		// xxx: mask:
		drcl.inet.data.RTKey key = new drcl.inet.data.RTKey(0, 0, dst, -1, 0, 0);

		// Tyan XXX: shortcut, force to replace all the route entries
		if (nexthop_ > 0) {
			addRTEntry(key, nexthop_, if_index, new_en, -1);
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF)) {
				debug(router_id + " Add Route Entry " + dst + ", " + nexthop_
					+ "_{" + if_index + "}, cost " + new_en.vtx_distance//cost
					+ ", lsa " + new_en.vtx_lsa);//ls_origin.from);
			}
		}

		/* get current entry. if not exists, make entry in table */
		Object obj_ = retrieveRTEntry(key, RTConfig.MATCH_EXACT);		
		if(obj_ == null)
			cur_en = null;
		else
			//cur_en = (OSPF_RoutingEntry) ((RTEntry)obj_).getExtension();
			cur_en = (OSPF_SPF_vertex) ((RTEntry)obj_).getExtension();

//		int if_index = IFQuery.getIndexByPeerAddr( dst, ifport);

		if ( cur_en == null) {
			/* if not installed, simply set info */
			addRTEntry(key, nexthop_, if_index, new_en, -1);
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF)) {
				debug(router_id + " Add Route Entry " + dst + ", " + nexthop_
					+ "_{" + if_index + "}, cost " + new_en.vtx_distance//cost
					+ ", lsa " + new_en.vtx_lsa);//ls_origin.from);
			}
			return;
		}
		
		/* there's already entry. */
		//if ( cur_en.isSame ( new_en) == 1)
		else if (cur_en.equals( new_en)) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   same vertex in the routing table");
			return;
		}
			
		/*		if ( ((Router_LSA) cur_en.ls_origin).ls_link_list.size() < 
			 ((Router_LSA) new_en.ls_origin).ls_link_list.size() ) {
			removeRTEntry(key, RTConfig.MATCH_EXACT);
			addRTEntry(key, nexthop_, if_index, new_en, -1);
			if( isDebugEnabled()) {
				debug(router_id + " Included Route Entry " + dst + " if: " + if_index + " cost " + new_en.cost + "Old Entry Outdated");
			}
			return;
		}*/

		/* check if cur_en.vtx_lsa still exists in LS database and up-to-date */
		//if (area.ls_db.ospf_lsdb_lookup(cur_en.ls_origin) == null) {
		else if ( (lsa = area.ls_db.ospf_lsdb_lookup(cur_en.vtx_lsa)) == null
			|| Util.ospf_lsa_check_recent(cur_en.vtx_lsa, lsa, now_) == 1) {
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   old route entry's lsa does not exist any more");
			//removeRTEntry(key, RTConfig.MATCH_EXACT);
			addRTEntry(key, nexthop_, if_index, new_en, -1);
		}
			
		/* path type preference */
		else if ( new_en.path_type < cur_en.path_type) {
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   new entry has preferable path type");
			/* replace */
			/*Vector en_v = en.next_hops.*/
			//removeRTEntry(key, RTConfig.MATCH_EXACT);
			addRTEntry(key, nexthop_, if_index, new_en, -1);
		}
		else if ( new_en.path_type > cur_en.path_type ) {
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   old entry has preferable path type");
			/* preferable entry already exists, don't add */
					/* check if the assoicated lsa is out-dated */
			return;
		}

		/* cost preference for the same path type */
		//else if ( new_en.cost < cur_en.cost) {
		else if ( new_en.vtx_distance < cur_en.vtx_distance) {
			if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   new entry has smaller cost");
			/* replace */
			//removeRTEntry(key, RTConfig.MATCH_EXACT);
			addRTEntry(key, nexthop_, if_index, new_en, -1);
		}
		//else if ( new_en.cost > cur_en.cost ) {
		else if ( new_en.vtx_distance > cur_en.vtx_distance) {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   old route entry has smaller cost");
			/* preferable entry already exists, don't add */
			return;
		}
		else {
			if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF))
				debug("   route does not change(?)");
			// Tyan: dont know what to do about these codes, just cross them out
			/* the same cost, merge nexthops to current entry
			int no = new_en.next_hops.size();
			int i;
			for ( i = 0; i < no; i++) {
				OSPF_SPF_vertex nh = (OSPF_SPF_vertex) new_en.next_hops.elementAt(i);
				if ( cur_en.next_hops.contains(nh) == true )
					continue;
				cur_en.next_hops.addElement(nh);
			} */
			return;
		}
		if( isDebugEnabled() && isDebugEnabledAt(DEBUG_SPF)) {
			debug(router_id + " Update Route Entry to " + dst
				+ ": " + nexthop_ + "-{" + if_index + "}, cost " + new_en.vtx_distance//cost
				+ ", lsa " + new_en.vtx_lsa);//ls_origin.from);
		}
	}

	private long ospf_nexthop_out_if_addr ( OSPF_SPF_vertex v, OSPF_SPF_vertex w)
	{
		long addr = -1;
		int i, no;
		
		Router_LSA r_lsa = (Router_LSA) v.vtx_lsa;
		no = r_lsa.link_no;
		for ( i = 0; i < no; i++) {
			Router_LSA_Link lsd = (Router_LSA_Link) r_lsa.ls_link_list.elementAt(i);
			if (lsd.type == LSA_LINK_TYPE_STUB )
				continue;

			/* Defer NH calculation via VLs until summaries from
				 transit areas area confidered             */
			if (lsd.type == LSA_LINK_TYPE_VIRTUALLINK )
				continue;
			if (lsd.link_id == w.vtx_id) {
				addr = lsd.link_data;
				return addr;
			}
		}
		return addr ;
	}
	
	/* Second stage of SPF calculation. */
	private void ospf_spf_process_stubs (OSPF_SPF_vertex V, OSPF_Area area)
	{
		OSPF_SPF_vertex child_vtx;
		int i;

		//if (isDebugEnabled()) debug("Z: ospf_process_stub():processing stubs for area " + area.area_id);

		if (V.vtx_type == OSPF_VERTEX_ROUTER ) {
			//if (isDebugEnabled()) debug("Z: ospf_process_stub():processing router LSA, id: " + this.vtx_lsa.header.lsh_id);

			Router_LSA rlsa = ( Router_LSA ) V.vtx_lsa;
			//if (isDebugEnabled()) debug("Z: ospf_process_stub(): we have %d links to process" + rlsa.link_no);
			int link_num = rlsa.link_no;
			for ( i = 0; i < link_num; i++) {
				Router_LSA_Link l = (Router_LSA_Link) rlsa.ls_link_list.elementAt(i);
				if (l.type == LSA_LINK_TYPE_STUB )
// xxx:
// ospf_intra_add_stub (rt, l, this, area);
					;
			}
		}
		//if (isDebugEnabled()) debug("Z: children of V:");
		int child_num = V.nexthops.size();
		for ( i = 0; i < child_num; i++) {
			child_vtx = (OSPF_SPF_vertex) V.nexthops.elementAt (i);
			ospf_spf_process_stubs (child_vtx, area);
		}
	}
	
	/**
	 * Test whether a vertex exists in the list 
	 * xxx: a hashtable would help the performance
	 */
	protected static OSPF_SPF_vertex ospf_vertex_lookup (Vector list, long id)//, int type)
	{
		for (int i = 0; i < list.size(); i++) {
			OSPF_SPF_vertex v = (OSPF_SPF_vertex) list.elementAt(i);
			if ( v.vtx_id == id)// && v.vtx_type == type )
				return v;
		}
		return null;
	}

	/**
	 * Test whether a vertex exists in the list 
	 * xxx: a hashtable would help the performance
	 */
	protected static OSPF_SPF_vertex ospf_vertex_lookup (TreeMapQueue list, long id)//, int type)
	{
		Object[] oo_ = list.retrieveAll();
		for (int i = 0; i < oo_.length; i++) {
			OSPF_SPF_vertex v = (OSPF_SPF_vertex) oo_[i];
			if ( v.vtx_id == id)// && v.vtx_type == type )
				return v;
		}
		return null;
	}

	/**
	 * Install vertex w into candidate list
	 * the candidate list is sorted from the min cost to max cost
	 */
	protected void ospf_install_candidate (TreeMapQueue candidate, OSPF_SPF_vertex w)
	{
		candidate.enqueue(w.vtx_distance, w);
	}

	/**
	 * Test whether there is a link in LSA w pointer to vertex v
	 * returns 1 if a link exists in w --> v
	 */
	protected int ospf_lsa_has_link ( OSPF_LSA w, OSPF_LSA v)
	{
		int	i;
		int	length;
		Router_LSA		rl;
		
		/*
		Network_LSA		nl;
		// In case of W is Network LSA. 
		if (w.lsh_type == OSPF_LSA_Header.OSPF_NETWORK_LSA) {
			if (v.lsh_type == OSPF_LSA_Header.OSPF_NETWORK_LSA)
				return 0;

			nl = ( Network_LSA ) w;

			length = nl.routers.size();
			for (i = 0; i < length; i++)
				if ( nl.routers.elementAt(i) == v.lsh_id )
					return 1;
			return 0;
		}*/

		/* In case of W is Router LSA. */
		if (w.header.lsh_type == OSPF_ROUTER_LSA ) {
			rl = ( Router_LSA ) w;
			length = rl.link_no;
			for (i = 0; i < length; i++) {
				Router_LSA_Link lsd = (Router_LSA_Link) rl.ls_link_list.elementAt(i);
				switch (lsd.type) {
					case LSA_LINK_TYPE_POINTOPOINT:
					case LSA_LINK_TYPE_VIRTUALLINK:
						/* Router LSA ID. */
						if (v.header.lsh_type == OSPF_ROUTER_LSA && lsd.link_id == v.header.lsh_id ) {
							return 1;
						}
						break;
					case LSA_LINK_TYPE_TRANSIT:
						/* Network LSA ID. */
						if (v.header.lsh_type == OSPF_NETWORK_LSA && lsd.link_id == v.header.lsh_id ) {
							return 1;
						}
						break;
					case LSA_LINK_TYPE_STUB:
						/* Not take into count? */
						continue;
		            default:
						break;
				}
		    }
		}
		return 0;
	}

	/** Check whether one vertex with link_id in the vertex tree exists */
	boolean vertex_in_tree( int link_id, Vector vertex_list)
	{
		int no = vertex_list.size();
		for (int i = 0; i < no; i++) {
			OSPF_SPF_vertex vex = (OSPF_SPF_vertex) vertex_list.elementAt(i);
			if ( vex.vtx_id == link_id && vex.intree == true)
				return true;
		}
		return false;
	}
}

