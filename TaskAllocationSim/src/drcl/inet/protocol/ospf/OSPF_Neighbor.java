// @(#)OSPF_Neighbor.java   9/2002
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

/**
 * The information exchanged with other adjacent nodes is described by a OSPF_Neighbor
 * data structure, which is bounded to a specific OSPF router interface. 
 * Ref: sec.10
 * 
 * @author Wei-peng Chen
 * @see OSPF
 * @see OSPF_Interface
 * @see OSPF_DBdesc
 * @see OSPF_LSA
 * @see OSPF_TimeOut_EVT
 */
public class OSPF_Neighbor //extends drcl.comp.Component
{
	/** The OSPF_Interface associated with this neighbor */
	OSPF_Interface     ospf_interface;
	
	/** The current state of the neighbor */
	int		state;
	
	/** dd_msbit, dd_mbit, and dd_ibit : these three bits are used to 
	 *  specify the status of the current router, not the status of the neighbor,
	 *  since these three bits will be appended in the outgoing packet to its neighbor.
	 *  They represents the current state of the originating router.
	 */
	/** dd_msbit indicates whether the current router is the master */
	int		dd_msbit;
	/** dd_mbit indicates whether the current router has more packet to send */
	int		dd_mbit;
	/** dd_ibit indicates whether the current router has sent tone packet to this neighbor */
	int		dd_ibit;
	/** DD sequence number */
	int		dd_seqnum;
	/** if this neighbor is adjacent to this router */
	boolean isAdjacent = false;
	/** Router ID of this neighbor */
	int      rtr_id;
	/** Neighbor's IP address */
	long		addr;
	/** LSA lists for this neighbor */
	/** the list of LSAs that have been flooded but not acked on this adjacency */ 
	Vector retranslist;	
	/** the list of LSAs that make up the area link-state database */
	Vector summarylist;	
	/** the list of LSAs that need to be received from this neighbor */
	Vector requestlist;	
	/** the last OSPF_DBdesc pkt received from this neighbor */
	OSPF_DBdesc last_dd;
	/** the last OSPF_DBdesc pkt sent to this neighbor */
	OSPF_DBdesc dbdesc_last_send;
	/** The timout event for monitoring whether the link is okay between this neighbor */
	OSPF_TimeOut_EVT	Nbr_Inactive_EVT;
	/**
	 * The timout event will be generated if within ospf_interface.rxmt_interval 
	 * the router does not received OSPF_DBdesc pkt from the nbr.
	 * The master is the only one allowed to retransmit Database Description Packets.
	 * It does so only	at fixed intervals, the	length of which	is the configured 
	 * per-interface constant RxmtInterval.
	 */
	OSPF_TimeOut_EVT	LSDBdesc_Retrans_EVT;
	/** LSAs on the Link state request list that have been requested,
	 *  but not yet received, are packaged into Link State Request packets
	 *  for retransmission at intervals	of RxmtInterval.
	 */
	OSPF_TimeOut_EVT	LSreq_Retrans_EVT;
	/**
	 * The	number of seconds between LSA retransmissions, for adjacencies	
	 * belonging to this interface. 
	 */
	OSPF_TimeOut_EVT	LSupdate_Retrans_EVT;
	
	/* statistics */
	int ospf_stat_bad_lsreq;
	int ospf_stat_state_changed;
	int ospf_stat_seqnum_mismatch;
	
	int ospf_stat_oneway_received;
	int  ospf_stat_inactivity_timer;
	int ospf_stat_dr_election;
	int ospf_stat_retrans_dbdesc;
	int ospf_stat_retrans_lsreq;
	int ospf_stat_retrans_lsupdate;
	int ospf_stat_received_lsa;
	int ospf_stat_received_lsupdate;

	/**
	 * Constructor
	 */
	protected OSPF_Neighbor()
	{
		Nbr_Inactive_EVT		= null;
		LSDBdesc_Retrans_EVT	= null;
		LSreq_Retrans_EVT		= null;
		LSupdate_Retrans_EVT	= null;
		retranslist				= new Vector();
		summarylist				= new Vector();
		requestlist				= new Vector();
	}
	
	/**
	 * Clear all the timer associated with this nbr. 
	 * This Function is called when the router detect the nbr is down.
	 */
	protected void clear()
	{
		Nbr_Inactive_EVT		= null;
		LSDBdesc_Retrans_EVT	= null;
		LSreq_Retrans_EVT		= null;
		LSupdate_Retrans_EVT	= null;
		retranslist.removeAllElements();
		summarylist.removeAllElements();
		requestlist.removeAllElements();
	}
	
	/**
	 * Static function used to create an OSPF_Neighbor entity
	 */
	public OSPF_Neighbor (int src, OSPF_Interface oif)
	{
		/*OSPF_Neighbor n_ = (Neighbor)drcl.RecycleManager.reproduce(Neighbor.class);*/
		this();
		state = OSPF.NBS_DOWN;
		ospf_interface = oif;
		rtr_id = src;
	}
	
	/**
	 *  When the neighbor reaches state 'Exchange', we have to list ls summary list
	 *  for it. Prepare for dd exchange */
	protected void prepare_neighbor_lsdb ( int now_)
	{  
		/* log */
		//if (ospf.isDebugEnabled()) ospf.debug(" Making summary list for " + rtr_id );

		/* clear summary list of neighbor */
		if( summarylist != null) {
			// clear all content
			summarylist.removeAllElements();
			summarylist= null;
		}
		summarylist = new Vector();		

		/*
		// add AS-external-LSAs 
		ospf_lsdb_collect_type (nbr, htons (LST_AS_EXTERNAL_LSA), nbr.ospf6_interface.area.ospf6);
		*/
		
	    // add Router-LSAs
		ospf_lsdb_collect_type ( OSPF.OSPF_ROUTER_LSA, now_ );

		// add Network-LSAs: XXX not implemented
		//ospf_lsdb_collect_type ( OSPF_LSA_Header.OSPF_NETWORK_LSA, now_ );

		/* add Intra-Area-Prefix-LSAs */
		// xxx:
		// ospf6_lsdb_collect_type ( OSPF_LSA_Header.LST_INTRA_AREA_PREFIX_LSA);

		/* add Link-LSAs */
		// ospf6_lsdb_collect_type ( LST_LINK_LSA);

		return;
	}
	
	/* to process all as-external-lsa, *-area-prefix-lsa */
	private void ospf_lsdb_collect_type ( int type, int now_ )
	{
		int i;

		switch ( Util.translate_scope(type) ) {
			case OSPF.SCOPE_AREA:
				OSPF_Area area = (OSPF_Area) ospf_interface.area;
				for ( i = 0; i < area.ls_db.size(); i++) {
				//for (n = listhead (area->lsdb); n; nextnode (n)) {
		            OSPF_LSA lsa = ( OSPF_LSA ) area.ls_db.ls_list.elementAt(i);
			        if (lsa.header.lsh_type == type) {
						/* MaxAge LSA are added to retrans list, instead of summary list. (RFC2328, section 14) */
						if ( lsa.header.ospf_age_current( now_ ) == OSPF.LSA_MAXAGE)
							ospf_add_retrans (lsa);
						else
							ospf_add_summary (lsa);
					}
				}
				break;
			/*
			case OSPF.SCOPE_LINKLOCAL:
				// used by show_ipv6_ospf6_database_link_cmd
				OSPF_Interface oif = ( OSPF_Interface ) ospf_interface;
				for ( i = 0; i < oif.lsdb.size(); i++) {
				//for (n = listhead (o6if->lsdb); n; nextnode (n)) {
		            OSPF_LSA lsa = ( OSPF_LSA ) oif.lsdb.elementAt(i);
			        if (lsa.lsa_hdr.lsh_type == type)
						vec.addElement(lsa);
				}
				break;
			*/
			/*
			case OSPF.SCOPE_AS:
				// used by show_ipv6_ospf6_database_as_external_cmd
			    ospf6 = ( OSPF ) ospf_interface.ospf;
				for (n = listhead (ospf6->lsdb); n; nextnode (n)) {
		            lsa = (struct ospf6_lsa *) getdata (n);
				    if (lsa->lsa_hdr->lsh_type == type)
						list_add_node (l, lsa);
				}
				break;
			*/
			/*case OSPF.SCOPE_RESERVED:*/
			default:
				/*o6log.lsdb ("unsupported scope, can't collect advrtr from lsdb");*/
				break;
		}
		return;
	}

	/** add lsa to summary list of neighbor */
	protected void ospf_add_summary ( OSPF_LSA lsa)
	{
		if (summarylist.indexOf(lsa) != -1)
		    return;
		summarylist.addElement( lsa );
		//lsa.summary_nbr.addElement( nbr );
		// xxx:
		// ospf6_lsa_lock (lsa);
		return;
	}

	/** remove lsa from summary list of neighbor */
	protected void ospf_remove_summary ( OSPF_LSA lsa)
	{
		if ( summarylist.indexOf(lsa) == -1)
			return;
		summarylist.removeElement( lsa );
		// lsa.summary_nbr.removeElement( nbr );
		//ospf6_lsa_unlock (lsa);
		return;
	}
	
	/** remove all lsa from retrans list of neighbor */
	protected void ospf_remove_summary_all ()
	{
		summarylist.removeAllElements();
	}

	/** add lsa to summary list of neighbor */
	protected void ospf_add_request ( OSPF_LSA lsa)
	{
		if (requestlist.indexOf(lsa) < 0) requestlist.addElement( lsa );
	}

	/** lookup lsa on retrans list of neighbor */
	protected OSPF_LSA ospf_lookup_retrans (OSPF_LSA_Header h)
	{
		int i;
		int no = retranslist.size();
		for ( i = 0 ; i < no; i++) {
			OSPF_LSA p = (OSPF_LSA) retranslist.elementAt(i);
			if ( OSPF_LSA_Header.ospf_lsa_issame (p.header, h) == 1 )
				return p;
		}
		return null;
	}
	
	/** add lsa to retrans list of neighbor */
	protected void ospf_add_retrans (OSPF_LSA lsa)
	{
		if (retranslist.indexOf(lsa) != -1)
		    return;
		retranslist.addElement( lsa );
		// lsa.retrans_nbr.addElement( nbr );
		// ospf6_lsa_lock (lsa);
	}
	
	/**
	 * Lookup lsa on request list of neighbor 
	 * This lookup is different from others, because this lookup is to find
	 * the same LSA instance of different memory space 
	 */
	protected OSPF_LSA ospf_lookup_request (OSPF_LSA lsa)
	{
		int i;
		int no = requestlist.size();
		for ( i = 0 ; i < no; i++) {
			OSPF_LSA p = (OSPF_LSA) requestlist.elementAt(i);
			if ( OSPF_LSA_Header.ospf_lsa_issame (p.header, lsa.header) == 1 ) {
				/* #ifndef NDEBUG
					if (!list_lookup_node (p->request_nbr, nbr))
					assert (0);
					#endif */ /* NDEBUG */
				return p;
			}
		}
		return null;
	}
	
	public void reset()
	{
		retranslist.removeAllElements();
		summarylist.removeAllElements();
		requestlist.removeAllElements();
	}

	public String toString()
	{
		if (retranslist == null || retranslist.size() == 0)
			return rtr_id + (isAdjacent? ",adjacent": "") + ",no_retrans";
		else if (retranslist.size() < 5)
			return rtr_id + (isAdjacent? ",adjacent": "") + ",#lsa_retrans:" + retranslist.size()
				+ ",lsa_retrans:" + retranslist;
		else
			return rtr_id + (isAdjacent? ",adjacent": "") + ",#lsa_retrans:" + retranslist.size();
	}

	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(rtr_id + ", " + OSPF.NBR_STATES[state] + ", "
			+ (isAdjacent? "": "NOT_") +"ADJACENT\n");
		sb_.append(prefix_ + "#lsa_retrans:" + retranslist.size() + "\n");
		for (int i=0; i<retranslist.size(); i++) {
			if (i == 10) { sb_.append(prefix_ + "   ...\n"); break; }
			sb_.append(prefix_ + "   " + retranslist.elementAt(i) + "\n");
		}
		sb_.append(prefix_ + "#lsreq_retrans:" + requestlist.size() + "\n");
		for (int i=0; i<requestlist.size(); i++) {
			if (i == 10) { sb_.append(prefix_ + "   ...\n"); break; }
			sb_.append(prefix_ + "   " + requestlist.elementAt(i) + "\n");
		}
		sb_.append(prefix_ + "#in_summry:" + summarylist.size() + "\n");
		for (int i=0; i<summarylist.size(); i++) {
			if (i == 10) { sb_.append(prefix_ + "   ...\n"); break; }
			sb_.append(prefix_ + "   " + summarylist.elementAt(i) + "\n");
		}
		return sb_.toString();
	}
}
