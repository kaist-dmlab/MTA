// @(#)OSPF_Area.java   4/2003
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

import java.util.*;

// xxx: support multiple area routing (inter-area routing)

/**
 *		The area data structure contains all the information used to run the
 * basic OSPF routing algorithm. Each area maintains its own link-state
 * database. A	network	belongs	to a single area, and a	router interface
 * connects to	a single area. Each router adjacency also belongs to a single
 * area.
 *
 * <p>The OSPF backbone is the special OSPF area responsible for disseminating
 * inter-area routing information.
 *
 *	<p>	The	area link-state	database consists of the collection of router-LSAs,
 * network-LSAs and summary-LSAs that have originated from the area's routers.
 * This information is flooded throughout a single area only.
 *
 *	<p>	The list of AS-external-LSAs (see Section 5) is	also considered to be 
 * part of each area's link-state database.
 * 
 * <p>ref: sec. 6
 * 
 * @author Wei-peng Chen
 * @see OSPF
 * @see OSPF_LS_Database
 * @see OSPF_SPF_vertex
 * @see OSPF_Interface
 * @see OSPF_Neighbor
 */
public class OSPF_Area
{
	protected int area_id = Integer.MAX_VALUE;			/* Area ID. */

	/**
	 * THIS router's interfaces belonging to the area.
	 * Since each OSPF algorithm keeps a specific area data structure, 
	 * this field records the interfaces only belonging to this router in this area
	 */
	transient protected Vector if_list;
	/*protected Vector router_lsa_list;
	protected Vector network_lsa_list;
	protected Vector summary_lsa_list;*/
	
	/**
	 * Instead of using three Vector, which are router_lsa_list, network_lsa_list,
	 * and summary_lsa_list, we use a database to represent them all.
	 * Here each area has its own ls database
	 */
	transient protected OSPF_LS_Database	ls_db = null;

	/**
	 * Shortest Path Tree.
	 * The following two items are releated, spf_root is the root for the 
	 * shortest path tree
	 */ 
	transient protected OSPF_SPF_vertex spf_root;
	/** while vertex_list is the list for all verteies. Only vextex 
	 *  on the tree can be associated with spf_root, otherwise, it can be added
	 *  into vertex_list 
	 */
	transient protected Vector	vertex_list;
	
	/* Configuration variables. */
	int external_routing;                 // ExternalRoutingCapability.
	int no_summary;                       // Don't inject summaries into stub area.
	int shortcut_configured;              // Area configured as shortcut.
	int shortcut_capability;              // Other ABRs agree on S-bit
	int transit;                          // TransitCapability.

	int default_cost;                     // StubDefaultCost.
	int auth_type;                        // Authentication type.

	transient Router_LSA router_lsa_self;           // self originated LSAs.
	transient double floodLastTime = Double.NaN;    // last time flood router_lsa_self
	transient boolean delayFlood = false;           // true if a delay flood is scheduled for this area
	transient drcl.comp.ACATimer lsa_refresh_timer = null;
	/* struct route_table *summary_lsa_self; */
	/* struct route_table *summary_lsa_asbr_self; */ 

	/* Statistics field. */
	int spf_calculation;		/* SPF Calculation Count. */

	public Object clone()
	{
		OSPF_Area new_ = new OSPF_Area(area_id);
		new_.default_cost = default_cost;
		new_.auth_type = auth_type;
		new_.external_routing = external_routing;
		new_.no_summary = no_summary;
		new_.shortcut_configured = shortcut_configured;
		new_.shortcut_capability = shortcut_capability;
		new_.transit = transit;
		return new_;
	}

  	public String toString()
	{
		return "id:" + area_id + ",#ls=" + (ls_db == null? 0: ls_db.size())
			+ ",#ifs:" + (if_list == null? 0: if_list.size())
			+ ",#vertex:" + (vertex_list == null? 0: vertex_list.size());
	}

	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(toString() + "\n");
		sb_.append(prefix_ + "router_lsa_self: " + router_lsa_self + "\n");
		sb_.append(prefix_ + "#spf_calculations: " + spf_calculation + "\n");
		// LS's
		sb_.append(prefix_ + "LS's:");
		if (ls_db == null || ls_db.ls_list.size() == 0) sb_.append("<none>\n");
		else {
			sb_.append(" #LS's=" + ls_db.ls_list.size() + "\n");
			for (int i=0; i<ls_db.ls_list.size(); i++) {
				OSPF_LSA lsa_ = (OSPF_LSA)ls_db.ls_list.elementAt(i);
				if (lsa_ == null) continue;
				sb_.append(prefix_ + "   " + lsa_ + "\n");
			}
		}
		// IF's
		sb_.append(prefix_ + "IF's = {");
		if (if_list == null || if_list.size() == 0) sb_.append("<none>\n");
		else {
			for (int i=0; i<if_list.size(); i++) {
				OSPF_Interface if_ = (OSPF_Interface)if_list.elementAt(i);
				if (if_ == null) continue;
				sb_.append(if_.if_id + " ");
			}
			sb_.append("}\n");
		}
		/*
		// VERTEX's
		if (vertex_list == null || vertex_list.size() == 0) sb_.append("<none>\n");
		else {
			sb_.append(" #vertex=" + vertex_list.size() + "\n");
			for (int i=0; i<vertex_list.size(); i++) {
				OSPF_SPF_vertex v_ = (OSPF_SPF_vertex)vertex_list.elementAt(i);
				if (v_ == null) continue;
				sb_.append(prefix_ + "   " + v_._toString() + "\n");
			}
		}
		*/
		return sb_.toString();
	}

	/** Constructor */
	protected OSPF_Area()
	{
		if_list		= new Vector();
		vertex_list	= new Vector();
	}

	/**
	 * Constructor
	 * 
	 * @param id_ : Area id
	 */
	protected OSPF_Area( int id_)
	{
		this();
		area_id = id_;
		ls_db		= new OSPF_LS_Database();
	}

	public void reset()
	{
		//area_id = Integer.MAX_VALUE;			/* Area ID. */
		if_list.removeAllElements();
		ls_db.reset();
		spf_root = null;
		vertex_list.removeAllElements();
		router_lsa_self = null;
		floodLastTime = Double.NaN;
		delayFlood = false;
		spf_calculation = 0;
		lsa_refresh_timer = null;
	}

	/**
	 * Count the # of neighbors to which belong to this area have the same state as
	 * the "state" from the argument 
	 */
	protected int count_nbr_in_state ( int state )
	{
		OSPF_Interface oif;
		int count = 0;
		int i, j; 
		int if_no, nbr_no;
		
		if_no = if_list.size();
		for ( i = 0; i < if_no; i++) {
			oif = (OSPF_Interface) if_list.elementAt(i);
			nbr_no = oif.neighbor_list.size();
			for ( j = 0; j < nbr_no; j++) {
				OSPF_Neighbor  nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(j);
				if (nbr.state == state)
					count++;
			}
		}
		return count;
	}


	/**
	 * remove lsa from the area LS database
	protected void ospf_lsdb_remove ( OSPF_LSA lsa)
	{
		// LSA going to be removed from lsdb should not be (delayed)
		// acknowledged. I must prevent from being delayed acknowledged here
		int if_no = if_list.size();
		int i;
		
		for( i = 0; i < if_no; i++) {
			OSPF_Interface oif = (OSPF_Interface) if_list.elementAt(i);
			oif.lsa_delayed_ack.removeElement(lsa);
		}
		ls_db.ospf_lsdb_remove ( lsa );
		return;
	}
	 */
	
	/**
	 * Check whether lsa in the area LS database is outdated
	protected boolean osdf_lsdb_outdated( OSPF_LSA lsa )
	{
		OSPF_LSA_Header hdr = lsa.header;
		OSPF_LSA cur_lsa = ospf_lsdb_lookup(hdr.lsh_type, hdr.lsh_id, hdr.lsh_advtr);
		if(cur_lsa == null)
			return true; // no longer exist in the LS database
		
		return true;
		//switch ( lsa.header.lsh_type ) {
		//	case OSPF_LSA_Header.OSPF_ROUTER_LSA:
		//		Router_LSA rlsa = (Router_LSA) lsa;
		//		Router_LSA cur_rlsa = (Router_LSA) cur_lsa;
		//		if ( hdr.ospf_age_current(now_) > cur_lsa.header.ospf_age_current(now_)) {
		//			// the lsa_origin is older than the existing one
		//			if (cur_rlsa.link_no != rlsa.link_no)
		//				return true;
		//			else {
		//				// check each link
		//				return( rlsa.check_same_link( cur_rlsa ) );
		//			}	
		//		} else {
		//			// the ls_origin is more updfated
		//			return false;
		//		}	
		//}
		//return false;
	}
	*/
}






