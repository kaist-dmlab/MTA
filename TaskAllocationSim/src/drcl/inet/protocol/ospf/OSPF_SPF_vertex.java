// @(#)OSPF_SPF_vertex.java   9/2002
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

// xxx: support netweork (dr)

import java.util.*;

// Tyan: for saving space, this class is also used as route entry extension
/**
 * Data structure of vertex in the shortest path tree (SPF).
 * Ref: sec. 16.1
 * 
 * @author Wei-peng Chen
 */
public class OSPF_SPF_vertex //extends drcl.comp.Component /* Transit Vertex */
{
	/**
	 * vtx_id together with	the vertex type	(router or network)	uniquely 
	 * identifies the vertex. For router vertices the Vertex ID is the router's 
	 * OSPF Router ID.  For network vertices, it is the	IP address of the network's
	 * Designated Router.
	*/
	protected long			vtx_id;	
	
	/** "router" or "network"	 */
	protected int			vtx_type;

	/**
	 * Each transit vertex	has an associated LSA.	For router vertices, 
	 * this is a router-LSA. For transit networks, this is a network-LSA 
	 * (which is actually originated by the network's Designated Router).
	 * In any case,	the LSA's Link State ID is always equal to the above Vertex ID.
	 */
	protected OSPF_LSA		vtx_lsa;
	

	/** The list of next hops from the root to this vertex */
	protected Vector		nexthops; /* For ECMP */
	
	/** Distance (cost) from the root to this vertex */
	protected int			vtx_distance; 

	protected OSPF_SPF_vertex parent;
	protected OSPF_Interface ifp; // parent's interface that leads to this vertex
	
	/** flag to indicate if this vertex is on tree */
	protected boolean		intree;
		
	//Vector      vtx_path;     /* Lower node */
	//Vector      vtx_parent;   /* for vertex on candidate list */
	//char        vtx_depth;    /* for vertex on spf tree */
	int path_type;	// Tyan: for acting like route entry
	
	
	/** constructor */
	public OSPF_SPF_vertex ()
	{
		nexthops = new Vector();
	}
	
	/** constructor */
	public OSPF_SPF_vertex ( OSPF_LSA lsa_, int dist_, boolean intree_)
	{
		this();
		set(lsa_, dist_, intree_);
	}

	public void set( OSPF_LSA lsa_, int dist_, boolean intree_)
	{
		vtx_type = lsa_.header.lsh_type;
		vtx_id   = lsa_.header.lsh_id;
		vtx_lsa = lsa_;
		vtx_distance = dist_;
		intree = intree_;
	}

	public void reset()
	{
		parent = null;
		nexthops.removeAllElements();
		intree = false;
		vtx_distance = 0;
	}

	// prints as a route entry
	public String toString()
	{
		return OSPF.DTYPES[vtx_type] + "," + OSPF.PATH_TYPES[path_type] + ",cost(" + vtx_distance
			+ "),seq(" + (vtx_lsa.header == null? "-),age(-)":
				Util.printLSHSeqNum(vtx_lsa.header.lsh_seqnum) + "),age(" + vtx_lsa.header.lsh_age + ")");
	}

	public String _toString()
	{
		StringBuffer sb_ = new StringBuffer();
		sb_.append("id:" + vtx_id + ",type:" + vtx_type + ",distance:" + vtx_distance
			+ ",lsa:<" + _print_lsa(vtx_lsa) + ">" + ",parent:"
			+ (parent == null? "<none>": parent.vtx_id+"") + ",nexthops:");
		if (nexthops == null || nexthops.size() == 0)
			sb_.append("<none>");
		else {
			for (int i=0; i<nexthops.size(); i++)
				sb_.append(((OSPF_SPF_vertex)nexthops.elementAt(i)).vtx_id + " ");
		}
		return sb_.toString();
	}

	String _print_lsa(OSPF_LSA lsa_)
	{
		return "from:" + (lsa_.from == null? "-": lsa_.from.rtr_id+"") + ",origin:"
			+ (lsa_.header == null? "-": lsa_.header.lsh_advtr + ",age:"
				+ lsa_.header.lsh_age + ",seq#:" + Util.printLSHSeqNum(lsa_.header.lsh_seqnum));
	}

	public boolean equals(Object that_)
	{
		if (this == that_) return true;
		OSPF_SPF_vertex b = (OSPF_SPF_vertex) that_;
		
		if ( vtx_type == b.vtx_type && vtx_distance == b.vtx_distance
			&& vtx_lsa == b.vtx_lsa && nexthops.size() == b.nexthops.size() ) {
			for (int i = 0; i < nexthops.size(); i++) {
				OSPF_SPF_vertex na = (OSPF_SPF_vertex) nexthops.elementAt(i);
				OSPF_SPF_vertex nb = (OSPF_SPF_vertex) b.nexthops.elementAt(i);
				if (na.ifp == null ^ nb.ifp == null) return false;
				if( ( na.ifp != null && na.ifp.if_id != nb.ifp.if_id ) || (na.vtx_id !=nb.vtx_id))
					return false;
			}
			return true;
		}
		else
			return false;
	}

	public int getDistance()
	{ return vtx_distance; }
}
