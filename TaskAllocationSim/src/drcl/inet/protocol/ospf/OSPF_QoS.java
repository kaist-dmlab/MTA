// @(#)OSPF_QoS.java   9/2002
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

package drcl.inet.protocol.ospf;

import java.util.*;
import java.lang.*;

import drcl.inet.data.*;
import drcl.net.*;
import drcl.util.queue.TreeMapQueue;

 /* xxx: Note that current implementation only supports BW calculation.
 *  Delay requirement is not supported. need to add delay to InterfaceInfo */

/**
 *  QoS extension to OSPF.
 *  Mainly based on RFC 2676 QoS Routing Mechanisms and OSPF Extensions.
 *  Without explicit indication.  The following reference is from RFC 2676.
 *  <p>As described in sec. 1,  the purpose of the extension is to 
 *  "Obtain the information needed to compute QoS paths and select a
 *  path capable of meeting the QoS requirements of a given request"
 *  In this QoS extension, We use two kinds of mechanism to calculate the
 *  QoS routes. One is the on-demand calculation modified from the standard
 *  Dijkstra's algorithm. The QoS considered here is bandwidth requirement.
 *  When doing the calculation, we only consider the link with avail. bw
 *  larger than the requirement. 
 *  
 *  <p>The second mechanism is the precomputation approach.
 *  It calculates the routes to each destination and obtains the max. bw on 
 *  route and the next hop info, which are recorded in the routing table.
 *  Because of the changes in the link metric, the precomputation needs to
 *  be done periodically or by event trigger.
 *  The dynamic changes in the link metrics would trigger the recalculation
 *  of all paths. However, this requires the cooperations of other
 *  signalling protocols such as RSVP or MPLS LDP. Hence, in the current stage,
 *  the extension does not handle the dynamics of metrics. We only left the 
 *  interface for the future exploration.
 *  
 *  <p>The implementation does support equal-cost paths for multiple nexthops
 *
 *  @author Wei-peng Chen
 *  @see OSPF
 */ 
public class OSPF_QoS extends drcl.inet.protocol.ospf.OSPF
{
	/** QoS Option bit (the least siginificant bit in the Options field)
	 , ref: sec. 3.1 */
	final static int OSPF_OPTIONS_BIT_QOS			= 1;	

	/** TOS type, ref: sec. 3.2 
	    backward compatible with OSPF v1 */
	final static int OSPF_QOS_TOS_BW			= 40;
	final static int OSPF_QOS_TOS_DELAY			= 48;
	
	/** # of bits of exponent and mantissa for BW in TOS metric field, 
	 *   exp is put in the MSB
	 */ 
	final static int OSPF_QOS_METRIC_BW_EXP_LEN		= 3;
	final static int OSPF_QOS_METRIC_BW_MANT_LEN		= 13;
	
	/** the base for the exponent part in TOS metric calculation */
	final static double OSPF_QOS_BW_BASE			= 8.0;
	final static int OSPF_QOS_BW_MANT_MASK			= 0x1FFF;
	final static double QOS_INFINITY				= Double.MAX_VALUE;
	
	/**	An example to show when to execute precomputation,
	 * not explicitly stated in RFC */
	final static int OSPF_QOS_LSA_CHANGE_TRIGGER_TIMES	= 50;
	
	/** Redefine Debug level of handling timeout events. */
	public static final int DEBUG_QOS  			= 8;
	static final String[] DEBUG_LEVELS = {
		"sample", "neighbor", "send", "spf", "refresh", "lsa", "ack", "timeout", "qos"
	};

	/**
	 * QoS options value for OSPF, ref: sec.3.2
	 */
	private int			QoS_options;
	/* bit of supporting periodical precompute */
	private boolean		periodical_precompute = false ;
	/* bit of supporting dynamic precompute */
	private boolean		dynamic_precompute = false ;
	
	/////////////////////////////////////////////////////////////////
	// Timeout Handling Functions
	private class OSPF_QoS_TimeOut_EVT extends drcl.inet.protocol.ospf.OSPF_TimeOut_EVT {
		/* 0~8 are standard timeout event in OSPF */
		public static final int OSPF_QOS_TIMEOUT_PRECOMPUTE	= 9; 
		final String[] TIMEOUT_TYPES = {
			"HELLO", "LS_REFRESH", "MAXAGE_REACHED", "DELAY_ACK",
			"NBR_INACTIVE", "DBDESC_RETX", "LSA_RETX", "LSREQ_RETX", "DELAY_FLOOD", "PRECOMPUTE" };
			
		public OSPF_QoS_TimeOut_EVT(int tp_, Object obj_) {
			super(tp_, obj_) ;
		} 
		public String toString()
		{ return TIMEOUT_TYPES[EVT_Type] + ", " + EVT_Obj; }

	}
	OSPF_QoS_TimeOut_EVT precompute_EVT = null;
	
	/** precomputation period (unit: second) */
	private int precompute_period;
	
	protected void timeout(Object evt_)
	{
		super.timeout(evt_);
		int type = ((OSPF_TimeOut_EVT)evt_).EVT_Type;
		switch(type) {
			case OSPF_QoS_TimeOut_EVT.OSPF_QOS_TIMEOUT_PRECOMPUTE:
				if (evt_ != precompute_EVT) {
					error("QoS timeout()", " ** ERROR ** where does this come from? " + evt_);
					break;
				}
				OSPF_Area area_ = (OSPF_Area) precompute_EVT.getObject();
				ospf_QoS_spf_precompute ( area_ ); 
				break;
		}
		return;
	}

	////////////////////////////////////////////////////////////////
	// private variables for precomputation only
	private Vector QoS_V_list	= null ; /* set of vertices, router id are stored */
  
	private class TopologyTable {
		/* maximum available bandwidth (as known thus far) on a path of 
		at most h hops between vertices s and n*/
		double			bw;
		/* nexthop is the first hop on that path (a neighbor of s). Storing type: long */
		Vector			nexthops;
		/* the interfqace to the next hop from the source */
		OSPF_Interface 	ifp;
		
		TopologyTable() { bw=0; nexthops = new Vector(); ifp = null; }
		
		TopologyTable(double bw_, Vector nexthops_, OSPF_Interface ifp_) 
		{ bw=bw_; nexthops = (Vector) nexthops_.clone(); ifp = ifp_; }
		
		protected void copy( TopologyTable src) {
			bw=src.bw; nexthops = (Vector) src.nexthops.clone(); ifp = src.ifp;
		}
		protected Object clone()
		{
			TopologyTable t = new TopologyTable(bw, nexthops, ifp);
			t.bw = bw;
			t.ifp = ifp;
			return t;
		}
	}
	/** QoS_RT[1..N, 1..H]: topology table entry, whose (n,h) entry is a tab_entry 
	 *  It is created when doing precomputation 
	 */
	private TopologyTable[][] QoS_RT	= null; 
	
	/* record the lsa change times in order to trigger a precomputation */
	private int lsa_change_times			= 0;
	
	//////////////////////////////////////////////////////////////////////////////////////
	/** Constructor. */
	public OSPF_QoS()
	{
		super();

		// initialize QoS_V_list
		QoS_V_list = new Vector();
		QoS_V_list.removeAllElements();
	}

	public String info()
	{
		int now_ = (int)getTime();
		StringBuffer sb_ = new StringBuffer();
		sb_.append("Router id:" + super.router_id + "\n");
		sb_.append("QoS_V_list:");
		if (QoS_V_list == null || QoS_V_list.size() == 0)
			sb_.append("<none>\n");
		else {
			sb_.append("\n");
			for (int i=0; i<QoS_V_list.size(); i++) {
				int vtx = ((Integer) QoS_V_list.elementAt(i)).intValue();
				sb_.append("   vtx" + i + ": " + vtx);
			}
		}
		sb_.append("QoS_RT: ");
		if( QoS_RT == null) {
			sb_.append("<none>\n");
		} else {
			sb_.append("\n");
			for (int i=0; i<QoS_V_list.size(); i++) {
				int vtx = ((Integer) QoS_V_list.elementAt(i)).intValue();
				for (int j=0; j< 14; j++) {
					if(  QoS_RT[i][j] != null) {
						sb_.append(" [" + i + "][" + j + "] " + vtx +" bw " + QoS_RT[i][j].bw + " next ");
						for( int k = 0; k < QoS_RT[i][j].nexthops.size(); k++) {
							sb_.append( ((Long) QoS_RT[i][j].nexthops.elementAt(k)).longValue() + " " );
						}
					}
				}	
				sb_.append("\n");
			}
		}
		return sb_.toString();
	}
	
	public void reset()
	{
		super.reset();
		if (QoS_V_list != null) {
			QoS_V_list.removeAllElements();
			QoS_V_list = null;
		}
		QoS_RT = null;
		lsa_change_times = 0;
		
		if (periodical_precompute == true && precompute_EVT.handle != null) {
			cancelTimeout(precompute_EVT.handle);
			precompute_EVT.setObject(null);
			precompute_EVT.handle = null;
		}

	}

	/////////////////////////////////////////////////////////////////////////////
	// Interface to the public, some are temporarily creatred for demo
	/** set the tos type value for QoS extension, BW=40, DELAY=48 */

	/** Set value of TOS type, either 40( for bw) or 48 (for delay) */	
	public void set_QoS_options (int options_) 
	{
		QoS_options = options_;
	}
	
	public void show_QoS_options ( ) 
	{
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			debug( " QoS option " + QoS_options );
		}
	}

	/** Set the bit whether to enable precomputation periodically */	
	public void set_periodical_precompute_options (boolean options_, int period) 
	{
		periodical_precompute = options_;
		precompute_period = period;
		if( options_ == true) {
			// since we only support area, the following area object use the first index
			// if multiple areas are supported, this must be modified
			precompute_EVT = new OSPF_QoS_TimeOut_EVT(OSPF_QoS_TimeOut_EVT.OSPF_QOS_TIMEOUT_PRECOMPUTE, (OSPF_Area) area_list.elementAt(0));
			precompute_EVT.handle = setTimeout( precompute_EVT, precompute_period);
		}
	}
	
	/** Set the bit whether to execute precomputation according to dynamic changes,
	 * In this example, received lsa # is the reference */	
	public void set_dynamic_precompute_options (boolean options_) 
	{
		dynamic_precompute = options_;
	}
	
	/** given a destination and the required the bw, print the nexthops, on-demand version */
	public void show_QoS_ondemand_nexthop_by_bw ( int dest, double bw_) {
		int n_index = QoS_V_list.indexOf( new Integer(dest));
		if ( (n_index < 0) && (isDebugEnabled()) ) {
			debug("   ospf_qos_on_demand: have not received lsa from dest " + dest );
			return;
		}
		
		Vector nexthops_ = ospf_QoS_spf_on_demand_calculate ( (OSPF_Area) area_list.elementAt(0), dest, bw_);
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			if( nexthops_ == null) {
				debug("   ospf_qos_on_demand: no satisfied nexthop for dest " + dest + " bw: " + bw_ );
			} else {
				StringBuffer sb_ = new StringBuffer();
				sb_.append("   ospf_qos_on_demand: dest " + dest + " bw: " + bw_ +" next hops: " );
				for (int i = 0; i < nexthops_.size(); i++) {
					sb_.append( ((OSPF_SPF_vertex)nexthops_.elementAt(i)).vtx_id + " ");
				}
				debug(sb_.toString());
			}
		}
	}

	/** given a destination and the required the bw, print the nexthops, precompute version */
	public void show_QoS_precompute_nexthop_by_bw ( int dest, double bw_) {
		int tot_node_num = QoS_V_list.size();
		StringBuffer sb_ = new StringBuffer();
		
		int n_index = QoS_V_list.indexOf( new Integer(dest));
		if ( (n_index < 0) && (isDebugEnabled()) ) {
			debug("   ospf_qos_precompute: have not received lsa from dest " + dest );
			return;
		}
		for (int h = 1; h < tot_node_num; h++) {
			// find the min hop path satisfying bw_
			if( QoS_RT[n_index][h].bw >= bw_ ) {
				if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
					sb_.append("   ospf_qos_precompute: dest " + dest + " bw: " + bw_ + " hop: " + h + " next hop: " );
					for( int k = 0; k < QoS_RT[n_index][h].nexthops.size(); k++) {
						sb_.append( ((Long) QoS_RT[n_index][h].nexthops.elementAt(k)).longValue() + " " );
					}
					debug(sb_.toString());
					return;
				}
			}
		}
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			debug("   ospf_qos_precompute: no satisfied nexthop for dest " + dest + " bw: " + bw_ );
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////
	// QoS metrics composition
	//////////////////////////////////////////////////////////////////////////////
	/**
	 *  make router lsa pkt to describe its neighbor 
	 *  LSA which supports QoS may carry more than one tos metric
	 */ 
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
		
		InterfaceInfo iinfo_ ;
		for (i = 0; i < if_no; i++) {
			OSPF_Interface oif = (OSPF_Interface) area.if_list.elementAt(i);

			iinfo_ = drcl.inet.contract.IFQuery.getInterfaceInfo( oif.if_id, oif.out_port ); 

			/* if interface is not enabled, ignore */
			if (oif.state <= IFS_LOOPBACK)
				continue;

			/* if interface is stub, ignore */
			if ( oif.ospf_interface_count_full_nbr() != 0 ) {
				Router_LSA_Link rlsd = lsa.make_ospf_router_lsd (oif);
				
				// changes for QoS only
				//////////////////////////////////////////////////////////////
				if (QoS_options == OSPF_QOS_TOS_BW ) {
					double bw_ = iinfo_.getBandwidth();
					/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
						debug(" bw " + bw_ );*/

					int encrypt_bw = bw2metric(bw_);
					/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
						debug(" en_bw " + encrypt_bw );*/

					rlsd.add_tos_metric(OSPF_QOS_TOS_BW, encrypt_bw);
				} else if (QoS_options == OSPF_QOS_TOS_DELAY ) {
					// xxx: iinfo needs to add delay info
				}
				//////////////////////////////////////////////////////////////
				
				lsa.link_no++;
				lsa.ls_link_list.addElement(rlsd);
			}
		}
		
// xxx: set E & B bit:
		
		// Tyan: 05/08/2001, 10/18/2001
		area.lsa_refresh_timer =
			setTimeout(new OSPF_TimeOut_EVT( OSPF_TimeOut_EVT.OSPF_TIMEOUT_LS_REFRESH,
									lsa ), LS_REFRESH_TIME);

		// changes for QoS only: set options bit (the least siginificant bit in the Options field)
		lsa.getHeader().set_options( (lsa.getHeader().get_options() | (0x01 << (OSPF_OPTIONS_BIT_QOS-1))) );
		
		/*
		if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			debug("   *** ospf_make_router_lsa: options " + lsa.getHeader().get_options() );
		}*/
		
		return lsa;
	}		

	/**
	 * Besides the normal operations, record the special metrics recorded in LSA
	 * for QoS path calculation 
	 */
	protected void ospf_lsdb_install ( OSPF_LSA new_lsa, OSPF_LSA old_lsa, OSPF_Area area)
	{
		super.ospf_lsdb_install( new_lsa, old_lsa, area);
		// check whether the Options value is correct
		int options_ = new_lsa.getHeader().get_options();
		/*if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			debug("   *** ospf_lsdb_install: add " + new_lsa.getHeader().lsh_id + " to QoS_V_list "
				+ " options " + options_);
		}*/

		if ( (options_ & (0x01 << (OSPF_OPTIONS_BIT_QOS-1))) == 0 ) {
			// unsupported options
			return;
		}
		// record the node to QoS_V_list, if it's new
		if ( QoS_V_list.indexOf( new Integer( new_lsa.getHeader().lsh_id )) < 0) {
			QoS_V_list.addElement( new Integer( new_lsa.getHeader().lsh_id ));
			if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS))) {
			//if (isDebugEnabled() && isDebugEnabledAt(DEBUG_QOS)) {
				debug("   ospf_lsdb_install: add " + new_lsa.getHeader().lsh_id + " to QoS_V_list size()" + QoS_V_list.size());
			}
		}
		
		// set up an example to trigger the precomputation condition
		if ( (dynamic_precompute == true) && (lsa_change_times >= OSPF_QOS_LSA_CHANGE_TRIGGER_TIMES) &&
			 (new_lsa.header.lsh_type == OSPF_ROUTER_LSA) ) {
			lsa_change_times = 0;
			ospf_QoS_spf_precompute ( (OSPF_Area) new_lsa.scope ); 
		} else 
			lsa_change_times++;
	}
	

	////////////////////////////////////////////////////////////////////////////
	// QoS On-demand Path calculation
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 *  RFC2328 Section 16.1 (2). 
	 *  V: newly-added vertex
	 *  Add V's neighbors to candidate list (sorted by cost) 
	 *  QoS extension: remove the links with BW less than the requirements
	 *  currently support the BW option only
	 */
	private void ospf_QoS_spf_next (OSPF_SPF_vertex V, OSPF_Area area, TreeMapQueue candidate, int now_, double bw_)
	{
		OSPF_LSA w_lsa = null;

		Router_LSA  r_lsa = (Router_LSA) V.vtx_lsa;
		int link_num = r_lsa.link_no;
		int l, i;
		Router_LSA_Link currentlink = null;

		/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
			debug(" link_num  " + link_num  );*/
		
		for ( l = 0; l < link_num; l++) {
			/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
				debug(" (" + l + ")  type " + V.vtx_lsa.header.lsh_type  );*/

			/* In case of V is Router-LSA. */
			if (V.vtx_lsa.header.lsh_type == OSPF_ROUTER_LSA) {
				currentlink = (Router_LSA_Link) r_lsa.ls_link_list.elementAt(l);

				/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
					debug("id " + currentlink.link_id );*/

				/////////////////////////////////////////////////////////////////////////////
				// QoS BW requirements: delete links with avail. BW less than the requirement
				/////////////////////////////////////////////////////////////////////////////
				int encrypt_bw_ = currentlink.get_tos_metric(OSPF_QOS_TOS_BW);
				
				/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
					debug(" en_bw " + encrypt_bw_ );*/

				double curr_bw_ = metric2bw(encrypt_bw_);
				
				/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
					debug(" en_bw " + encrypt_bw_ + " bw " + curr_bw_ );*/

				if ( curr_bw_ < bw_ )
					continue;
				/////////////////////////////////////////////////////////////////////////////
				
				/* (a) If this is a link to a stub network, examine the next
				 link in V's LSA.  Links to stub networks will be
				 considered in the second stage of the shortest path
				 calculation. */
				if (currentlink.type == LSA_LINK_TYPE_STUB)
					continue;

				/* (b) Otherwise, W is a transit vertex (router or transit
			     network).  Look up the vertex W's LSA (router-LSA or
				 network-LSA) in Area A's link state database. */
				/* (b) examine the lsa age, and check if it link back to V */
				switch (currentlink.type) {
					case LSA_LINK_TYPE_VIRTUALLINK:
					case LSA_LINK_TYPE_POINTOPOINT:
						/* ref: A.4.2 explan of link id: 
						if the link type is either router or transit network,the link id will be 
						the same as the lsa link state id, in order to look up this lsa in the database */
						w_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA,
											currentlink.link_id, currentlink.link_id);
						break;
					default:
						if (isErrorNoticeEnabled())
							error("ospf_spf_next()", " *** Warn *** : Invalid LSA link type "
								+ currentlink.type);
						continue;
				} /* end of switch */
			} 

			/* (b cont.) If the LSA does not exist, or its LS age is equal
			 to MaxAge, or it does not have a link back to vertex V,
			 examine the next link in V's LSA. */
			if (w_lsa== null || w_lsa.header.ospf_age_current (now_) == LSA_MAXAGE) {
				continue ; // next link
			}

			/* Examine if this LSA have link back to V */
			if ( ospf_lsa_has_link (w_lsa, V.vtx_lsa) == 0 ) {
				//if (isDebugEnabled()) 
				//	debug("      Z: The LSA doesn't have a link back: " + w_lsa);
				continue;
			}

			/* (c) If vertex W is already on the shortest-path tree, examine
				the next link in the LSA. */
			if( vertex_in_tree( currentlink.link_id, area.vertex_list ) )
				continue;
						
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
				W.set(w_lsa, dist, false);				W.parent = V; // Tyan
				ospf_nexthop_calculation (area, V, W);
				ospf_install_candidate (candidate, W);
			} else {
				/* if D is greater than. */
				if (CW.vtx_distance < dist) {
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

	/**
	 * On-demand version of calculating a path satisying specified requirements 
	 * to a destination. 
	 * return value: the nexthop list of the destination (interface from the src)
	 * Note that the type of elements in Vector is OSPF_SPF_vertex
	 */
	public Vector ospf_QoS_spf_on_demand_calculate ( OSPF_Area area, int dest, double bw_)
	{
		OSPF_SPF_vertex V;
		/*if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_SPF)))
			debug("   ospf_QoS_spf_on_demand_calculate: running Dijkstra for area " + area.area_id);*/

		/* Check router-lsa-self.  If self-router-lsa is not yet allocated,
			return this area's calculation. */
		if (area.ls_db.size() == 0 ) {
			if (isDebugEnabled()) debug("ospf_spf_calculate:Skip area " + area.area_id +"'s calculation due to empty router_lsa_self" );
			return null;
		}

		/* RFC2328 16.1. (1). */
		/* Initialize the algorithm's data structures. */ 
		/* Clear the list of candidate vertices. */ 
		TreeMapQueue candidate = new TreeMapQueue();

		/* Initialize the shortest-path tree to only the root (which is the
			router doing the calculation). */
		ospf_spf_init (area);
					
		V = area.spf_root;

		/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
			debug("V " + V._toString() );*/
			
		int now_ = (int)getTime();
		// Modified from OSPF: only need to reach the dest
		while ( V.vtx_id != dest ) {
			/* RFC2328 16.1. (2). */
			
			/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
				debug("calculate candidate V " + V.vtx_id  );*/
			ospf_QoS_spf_next (V, area, candidate, now_, bw_);
			
			/*if (isDebugEnabled() && isDebugEnabledAt(DEBUG_SAMPLE))
				debug("   #candidates = " + candidate.size());*/
			

			/* RFC2328 16.1. (3). */
			/* If at this step the candidate list is empty, the shortest-
			path tree (of transit vertices) has been completely built and
			this stage of the procedure terminates. */
			if (candidate.isEmpty())
				return null;

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
		return V.nexthops;
	}

	/////////////////////////////////////////////////////////////////////
	// QoS Precomputation
	///////////////////////////////////////////////////////////////////
	/**
	 * Appendex A. Pseudocode for the BF Based Pre-Computation Algorithm
	 * - assumes a hop-by-hop forwarding approach
	 * - does not handle equal cost multi-paths for simplicity
	 * - ***Note***: Here we only consider the bidirectional links.
	 *   During the LSA exchange process, if we have received a LSA from
	 *   a node, say A, we don't know its existence and would not calculate
	 *   a route to it.
	 * 
	 *  Local Variable:
	 *  - TT[n][h]: n:index in QoS_V_list; h:hop num
	 *  storing the max. bw and the next hop info. to this node n in the 
	 *  hth iteration. After calculation TT will be stored in QoS_RT 	
	 *  - S_prev: list of vertices that changed a bw value in the TT table
	 *  in the previous iteration.
	 *  - S_new: list of vertices that changed a bw value (in the TT table 
	 *  etc.) in the current iteration.
	 *
	 *  Each link info. (n,m) is required during the computation
	 *  can be obtained by first retrieve the router LSA from n
	 *  check each LSA link in this router LSA and get the link info.
	 *  To ensure the bidirection connectivity, we need to check the router LSA from m
	 */
	protected void ospf_QoS_spf_precompute ( OSPF_Area area )
	{
		int tot_node_num = QoS_V_list.size();
		
		if (tot_node_num == 0)
			return;

		/*if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS)))
			debug("   ospf_QoS_spf_precompute " + tot_node_num);*/
			
		///////////////////////////////////////////////////////////////////////////////////////////
		/* Fisrt Step: Initialization */
		// allocate an N*N table, here we assume the max hop # is N for simplicity
		TopologyTable[][] TT = new TopologyTable [tot_node_num][tot_node_num];
		for ( int i = 0; i < tot_node_num; i++ ) {
			for ( int j = 0; j < tot_node_num; j++ ) {
				TT[i][j] = new TopologyTable();
			}
		}
		TT[0][0].bw = QOS_INFINITY;
		
		// S_prev store the index in QoS_V_list, not the router id. Therefore, 
		// after retrieving an index from S_prev, need to map it to QoS_V_list to get router id
		Vector S_prev = new Vector();
		
		Router_LSA curr_lsa = null;
		Router_LSA next_lsa = null;
		Router_LSA_Link currentlink = null;
		double curr_bw_;
		
		// first curr_lsa is itself
		curr_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA, super.router_id, super.router_id);
		int link_num = curr_lsa.link_no;
		
		for ( int l = 0; l < link_num; l++) {
			currentlink = (Router_LSA_Link) curr_lsa.ls_link_list.elementAt(l);

			int encrypt_bw_ = currentlink.get_tos_metric(OSPF_QOS_TOS_BW);
			curr_bw_ = metric2bw(encrypt_bw_);
				
			/* (b) examine the lsa age, and check if it link back to V */
			switch (currentlink.type) {
				case LSA_LINK_TYPE_VIRTUALLINK:
				case LSA_LINK_TYPE_POINTOPOINT:
					next_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA,
											currentlink.link_id, currentlink.link_id);
					break;
				default:
					if (isErrorNoticeEnabled())
						error("ospf_QoS_spf_precompute()", " *** Warn *** : Invalid LSA link type "
												 + currentlink.type);
					continue;
			} /* end of switch */
			int now_ = (int) getTime();
			if (next_lsa== null || next_lsa.header.ospf_age_current (now_) == LSA_MAXAGE) {
				continue ; // next link
			}

			/* Examine if this LSA have link back to V */
			if ( ospf_lsa_has_link (next_lsa, curr_lsa) == 0 ) {
				continue;
			}
			// finally, set the values in TT
			// find the index in QoS_V_list
			int n_index = QoS_V_list.indexOf( new Integer(currentlink.link_id));
			if (n_index < 0) {
				if (isErrorNoticeEnabled())
					error("ospf_QoS_spf_precompute()", " unmatched node id " +
									super.router_id + " with " + currentlink.type );
			}
			TT[n_index][1].bw = Math.max( TT[n_index][1].bw, curr_bw_);
			if (TT[n_index][1].bw == curr_bw_ ) {
				TT[n_index][1].nexthops.removeAllElements();
				TT[n_index][1].nexthops.addElement(new Long(currentlink.link_id));
				TT[n_index][1].ifp = ospf_if_lookup_by_addr(currentlink.link_data);
			}
			/* update the S_prev */
			if (S_prev.indexOf(new Integer (n_index)) < 0) {
				S_prev.addElement( new Integer (n_index) );
			}
		}
		
		///////////////////////////////////////////////////////////////////////////////////////////
		// 2nd step: iterate for each hop, consider all possible number of hops
		for( int h=2; h < tot_node_num; h++) {
			Vector S_new = new Vector();
			for ( int m = 0; m < tot_node_num; m++) {
				TT[m][h].copy( TT[m][h-1]);
			}
			int prev_num = S_prev.size();
			for (int n = 0; n < prev_num; n++) {
				int n_index = ((Integer) S_prev.elementAt(n)).intValue();
				int r_id = ((Integer) QoS_V_list.elementAt(n_index)).intValue();
				curr_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA, r_id, r_id);
				
				link_num = curr_lsa.link_no;
				for ( int l = 0; l < link_num; l++) {
					currentlink = (Router_LSA_Link) curr_lsa.ls_link_list.elementAt(l);

					int encrypt_bw_ = currentlink.get_tos_metric(OSPF_QOS_TOS_BW);
					curr_bw_ = metric2bw(encrypt_bw_);
				
					/* (b) examine the lsa age, and check if it link back to V */
					switch (currentlink.type) {
						case LSA_LINK_TYPE_VIRTUALLINK:
						case LSA_LINK_TYPE_POINTOPOINT:
							next_lsa = (Router_LSA) area.ls_db.ospf_lsdb_lookup (OSPF_ROUTER_LSA,
											currentlink.link_id, currentlink.link_id);
							break;
						default:
							if (isErrorNoticeEnabled())
								error("ospf_QoS_spf_precompute()", " *** Warn *** : Invalid LSA link type "
												 + currentlink.type);
							continue;
					} /* end of switch */
					int now_ = (int) getTime();
					if (next_lsa == null || next_lsa.header.ospf_age_current (now_) == LSA_MAXAGE) {
						if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS)))
							debug("   ospf_QoS_spf_precompute null next_lsa: cur " + r_id + " next :" + currentlink.link_id );
						continue ; // next link
					}

					/* Examine if this LSA have link back to V */
					if ( ospf_lsa_has_link (next_lsa, curr_lsa) == 0 ) {
						if (isDebugEnabled() && (isDebugEnabledAt(DEBUG_SAMPLE) || isDebugEnabledAt(DEBUG_QOS)))
							debug("   ospf_QoS_spf_precompute not bi-direction: cur " + r_id + " next :" + currentlink.link_id );
						continue;
					}
					int m_index = QoS_V_list.indexOf( new Integer(currentlink.link_id));
					if (m_index < 0) {
						if (isErrorNoticeEnabled())
							error("ospf_QoS_spf_precompute()", " unmatched node id " +
									super.router_id + " with " + currentlink.type );
					}
				
					if ( Math.min( TT[n_index][h-1].bw, curr_bw_ ) > TT[m_index][h].bw ) {
						TT[m_index][h].bw	= Math.min( TT[n_index][h-1].bw, curr_bw_ );
						TT[m_index][h].nexthops	= (Vector) TT[n_index][h-1].nexthops.clone();
						TT[m_index][h].ifp	= TT[n_index][h-1].ifp;
					} // when multiple equal-cost paths exist
					else if ( Math.min( TT[n_index][h-1].bw, curr_bw_ ) == TT[m_index][h].bw ) {			
						for (int i = 0; i < TT[n_index][h-1].nexthops.size(); i++) {
							if ( TT[m_index][h].nexthops.indexOf( new Long( ((Long) TT[n_index][h-1].nexthops.elementAt(i)).longValue() )) < 0 ) {	
								TT[m_index][h].nexthops.addElement( new Long( ((Long) TT[n_index][h-1].nexthops.elementAt(i)).longValue() ));
							}
						}					
					}
					
					if (S_new.indexOf(new Integer (m_index)) < 0) {
						S_new.addElement( new Integer (m_index) );
					}
				}
			}
			S_prev = (Vector) S_new.clone();
		}
		
		///////////////////////////////////////////////////////////////////////////////////////////		
		// 3rd step: after calculating all hops, we can store it to the routing table
		/**
		 * We can have two design choices. One is to keep TT table alive all the time.
		 * Thus, whenever a request arrives, we can find the min. hop path satisfying
		 * the bw requirement. Keep in mind that a route with max. bw might not be 
		 * the min. hop route. The second design choice is to record the max. bw route
		 * as well as the min. hop route in the routing table. 
		 * In the first choice we keep two kinds of routing table but can provide 
		 * more options for different bw requirementsthe while the second approach
		 * only keep one table but only provide one choice. Since in the second
		 * choice we need to modify the structure of routing entry, RTEntry.
		 * In this implementation, we choose the first approach for the simplicity. 
		 */
		if( QoS_RT != null) QoS_RT = null;
		QoS_RT = new TopologyTable [tot_node_num][tot_node_num];
		for (int i=1; i < tot_node_num; i++) {
			for (int j=1; j < tot_node_num; j++) 
				QoS_RT[i][j] = (TopologyTable) TT[i][j].clone();
		}		

	}
	////////////////////////////////////////////////////////////////////////////////////////
	// Supporting Functions
	
	/** transfer the value of the TOS metric (16 bits) to bandwidth value,
	    units: bits/sec, ref: sec3.2.1 */
	public double metric2bw ( int metric) 
	{
		double exp_val = (double) (metric >> OSPF_QOS_METRIC_BW_MANT_LEN);
		return( 8*(metric & OSPF_QOS_BW_MANT_MASK) * Math.pow(OSPF_QOS_BW_BASE, exp_val) );
	}
	/* input units: bits/sec */ 
	public int bw2metric ( double bw) 
	{
		int i;
		double bytes_val = bw / 8;
		int max_pow = 1 << OSPF_QOS_METRIC_BW_EXP_LEN;
		double max_base = Math.pow(2.0, (double) OSPF_QOS_METRIC_BW_MANT_LEN) -1;
		
		// check whether the input value is too high
		if ( bytes_val > (max_base * Math.pow(OSPF_QOS_BW_BASE, max_pow)) ) {
			if (isErrorNoticeEnabled())
				error("bw2metric()", " *** Warn *** : the input value exceeds the max. allowed value");
		}
		
		for (i = 0; i < max_pow; i++) {
			if ( bytes_val <= (max_base * Math.pow(OSPF_QOS_BW_BASE, (double) i)) ) {
				break;
			}
		}
		int exp_val  = i;
		int mant_val = (int) (bytes_val / Math.pow(OSPF_QOS_BW_BASE, (double) i));
		return ( (int) (exp_val << OSPF_QOS_METRIC_BW_MANT_LEN) | mant_val );
	}
}	
