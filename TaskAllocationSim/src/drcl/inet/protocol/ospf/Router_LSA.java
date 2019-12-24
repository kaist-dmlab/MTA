// @(#)Router_LSA.java   9/2002
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
 * OSPF Router-LSAs structure.
 * Router LSAs are flooded through its area and no further.
 * Ref: sec 12.4.1
 * 
 * @author Wei-peng Chen
 */
public class Router_LSA extends OSPF_LSA
{  
	protected final int ROUTER_LSA_LINK_LEN		= 4;

	/* the length of Router LSA packet body excluding each link info. */
	protected final static int ROUTER_LSA_FIELD_LEN		= 4;

	/* private int Vbit; */ /* indicate if the area is used as transit area */
	private int	 Ebit; /* indicate if the router is AS boundary router */
	private int	 Bbit; /* indicate if the router is area border router */

	/** Link # described in this LSA */
	protected int	 link_no; /* # of links */
	/** Link list included in this LSA */
	Vector	ls_link_list;

	public String toString()
	{
		return super.toString() + ",#links:" + link_no + ",links:" + ls_link_list;
	}

	/** Constructor */
	protected Router_LSA()
	{ this(new OSPF_LSA_Header()); }

	/** Constructor */
	protected Router_LSA(OSPF_LSA_Header header_)
	{
		super(header_);
		link_no = 0;
		ls_link_list = new Vector();
	}
	
	/**
	 * return the total size of the Router_LSA, including the header length
	 */
	protected int size() 
	{
		int size_ = OSPF_LSA_Header.OSPF_LSA_HEADER_SIZE + ROUTER_LSA_FIELD_LEN;
		for (int i = 0; i < link_no; i++) 
			size_ += ( (Router_LSA_Link) ls_link_list.elementAt(i)).size();
		return size_;
	}
	
	/**
	 * duplicate the router lsa
	 */
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Router_LSA that_ = (Router_LSA)source_;
		Ebit = that_.Ebit;
		Bbit = that_.Bbit;
		link_no = that_.link_no;
		//ls_link_list = (Vector) that_.ls_link_list.clone();
		// FIXME: do we really need to clone the vector?
		ls_link_list = that_.ls_link_list;
	}

	public Object clone()
	{
		Router_LSA new_ = new Router_LSA((OSPF_LSA_Header)header.clone());
		new_.duplicate(this);
		return new_;
	}
	
	/**
	 *  Make a new router lsa link according to oif 
	 */
	protected Router_LSA_Link make_ospf_router_lsd ( OSPF_Interface oif)
	{
		Router_LSA_Link rlsd = new Router_LSA_Link( );
		/* common field for each link type */
		rlsd.metric = oif.get_cost();
		rlsd.link_id = oif.if_id;

		/* set LS description for each link type */
		if (oif.if_is_pointopoint () == 1) {
			OSPF_Neighbor nbr = (OSPF_Neighbor) oif.neighbor_list.elementAt(0);
			// assert (nbr && nbr->state == NBS_FULL);
			rlsd.type = OSPF.LSA_LINK_TYPE_POINTOPOINT;
			// rlsd.rlsd_neighbor_interface_id = htonl (nbr->ifid);
			rlsd.link_id = nbr.rtr_id;
			rlsd.link_data = oif.if_id;
		}
		// if (if_is_broadcast (o6if->interface))
		/* else  {
			// else, assume this is broadcast. other types not supported
			rlsd->rlsd_type = LSDT_TRANSIT_NETWORK;

			// different neighbor field between DR and others
			if (o6if->state == IFS_DR) {
				rlsd->rlsd_neighbor_interface_id = htonl (o6if->if_id);
				rlsd->rlsd_neighbor_router_id = o6if->area->ospf6->router_id;
			} else {
				// find DR
				struct neighbor *dr;
				dr = nbr_lookup (o6if->dr, o6if);
				assert (dr);
				rlsd->rlsd_neighbor_interface_id = htonl (dr->ifid);
				rlsd->rlsd_neighbor_router_id = dr->rtr_id;
			}
	    } */
		return rlsd;
	}
	
	/** Returns true if the link info are the same. */
	protected boolean check_same_link(Router_LSA a_rlsa)
	{
		// assume no duplicate link, so 1-to-1 correspondence
		int no = a_rlsa.link_no;
		if (no != link_no) return false;
		if (ls_link_list == a_rlsa.ls_link_list) return true; // shortcut

		Router_LSA_Link		a_rlsd;
		Router_LSA_Link		rlsd;
		int i, j;
		
		for ( i = 0; i < a_rlsa.link_no; i++) {
			boolean same = true;
			a_rlsd = (Router_LSA_Link) a_rlsa.ls_link_list.elementAt(i);
			for ( j = 0; j< link_no; j++)
				if (ls_link_list.elementAt(j).equals(a_rlsd)) break;

			if ( j == link_no ) return false; // can not find the same corresponding lsd
		}
		return true; // all link are the same
	}
} // End of class Router_LSA
