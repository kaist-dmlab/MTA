// @(#)OSPF_RoutingEntry.java   9/2002
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

import java.util.Vector;

// xxx: support network routing entry
// xxx: multiple area

/**
 * OSPF routing table entry.
 * Ref: RFC 2328 sec.11 & Table 12, 13
 * The fields which should be included in the route table are:
 * DestType, DestID, Area, PathType, Cost, NextHop, AdvRoute
 * 
 * @author Wei-peng Chen, Hung-ying Tyan
 * @see OSPF_SPF_vertex
 * @see OSPF_LS_Database
 */
class OSPF_RoutingEntry extends drcl.DrclObj
{
	/** destination type, 'router' or 'network' */
	protected int			type;
	
	/** destination id, for 'router', it's the destination's IP address */
	protected int			dest_id;
	
	/**
	 * Only defined for networks.  The	network's IP address together
	 * with its address mask defines a	range of IP addresses.	For IP
	 * subnets, the address mask is referred to as the	subnet mask.
	 * For host routes, the mask is "all ones"	(0xffffffff).
	 */
	protected int			addr_mask;
	
	/**
	 * The entry's associated area. For sets of AS external	paths, this field
	 * is not defined.  For "router", there	may be separate	sets of	paths 
	 * (and therefore separate routing table entries) associated with each of 
	 * several areas. For example, the area border routers.For "network",
	 * only the set of	paths associated with the best area (the one providing
	 * the preferred route) is	kept.
	 */
	protected OSPF_Area		area; 
	
	/* The following fields describes the set of paths to the	destination.*/ 
	/**
	 * Four types of paths used to route traffic to	the destination,
	 * intra-area, inter-area, type 1 external	or type	2 external.
	 */
	protected int			path_type; 
	
	/** The link state cost of the path	to the destination.	 */
	protected int			cost;
	
	/**
	 * Valid only for intra-area paths, this field indicates the LSA
	 * (router-LSA or network-LSA) that directly references the	destination.
	 */
	protected OSPF_LSA		ls_origin;  /* Link State Origin, for MOSPF */
	
	/**
	 * The outgoing router interface to use when forwarding traffic to
	 * the destination.  On broadcast,	Point-to-MultiPoint and	NBMA
	 * networks, the next hop also includes the IP address of the next
	 * router (if any)	in the path towards the	destination.
	 */
	protected Vector		next_hops;
	
	/**
	 * 	Valid only for inter-area and AS external paths.  This field
	 * 	indicates the Router ID	of the router advertising the summary-LSA
	 *  or AS-external-LSA that led	to this	path.
	 */
	protected Vector		adv_router;

	/**
	 * Constructor
	 */
	public OSPF_RoutingEntry()
	{
//		super();
		next_hops = new Vector();
	}
	
	/**
	 * Constructor
	 */
	protected OSPF_RoutingEntry( int _dtype, OSPF_Area _area, int _ptype, int _cost, 
						 OSPF_LSA _lsa, Vector _nexthops )
	{
//		super();
		type = _dtype;
		area = _area;
		path_type = _ptype;
		cost = _cost;
		ls_origin = _lsa;
		next_hops = _nexthops;
	}
	
	/**
	 * Check if the Routing entry itself is the saem as routing entry b
	 */
	protected int isSame( OSPF_RoutingEntry b)
	{
		int no, i;
		
		if ( ( type == b.type) && (  cost == b.cost) && ( (no = next_hops.size()) == b.next_hops.size() ) )
		{
			for ( i = 0; i < no; i++) {
				OSPF_SPF_vertex na = (OSPF_SPF_vertex) next_hops.elementAt(i);
				OSPF_SPF_vertex nb = (OSPF_SPF_vertex) b.next_hops.elementAt(i);
				if( ( na.ifp.if_id != nb.ifp.if_id ) || (na.vtx_id !=nb.vtx_id))
					return 0;
			}
			return 1;
		} else
			return 0;
	}

	public String toString()
	{
		return "type(" + type + "),path_type(" + path_type + "),cost(" + cost
			+ "),age(" + (ls_origin.header == null? "-)": ls_origin.header.lsh_age + ")");
	}
}
