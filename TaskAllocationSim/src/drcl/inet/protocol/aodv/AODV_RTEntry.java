// @(#)AODV_RTEntry.java   1/2003
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
 
/** 
 * AODV_RTEntry.java 
 * AODV routing Entry dedicated for AODV usage. 
 * Note the routing entry is kept in AODV classes for some special
 * operations for AODV such as local repairs. The content of the routing
 * table should be consistent with the routing table kept in the core (core.RT). 
 *
 * @author Wei-peng Chen 
 * @see AODV  
 */ 
public class AODV_RTEntry 
{	 
	final static int RTF_DOWN	= 0;
	final static int RTF_UP		= 1;
	final static int RTF_IN_REPAIR	= 2;  	
	final static int MAX_HISTORY	= 3;
	
	/*public int	src; 	public int	id; 	public double expire; // now + BCAST_ID_SAVE s
	*/ 
	/** when I can send another req */
 	public double	rt_req_timeout; 	
	/** number of route requests */
	public int	rt_req_cnt;
	/** destination of the route entry*/
	protected long	rt_dst;
	/** route sequence number */
 	protected int	rt_seqno;
	/** hop count */
	protected int	rt_hops;
	/** last valid hop count */
	protected int	rt_last_hop_count; 
	/** next hop IP address */
	protected long	rt_nexthop;
	/** when entry expires */
	protected double rt_expire; 
	/** flag of the route entry, indicate up or down status */
	protected int	rt_flags;
 	/** list of precursors */ 
	protected Vector	rt_pclist;
	/** a list of neighbors that are using this route.*/
	protected Vector	rt_nblist;
	protected double[] 	rt_disc_latency = null;
	protected int 		hist_indx;
	/** last ttl value used */ 
	protected int		rt_req_last_ttl;
 
	/* Constructor */ 
	AODV_RTEntry(long id_) {  		
		rt_dst = id_;
	 	rt_req_timeout = 0.0;
		rt_req_cnt = 0;

		rt_seqno = 0;
		rt_hops = rt_last_hop_count = Integer.MAX_VALUE;
		rt_nexthop = 0;
		rt_pclist = new Vector();
 		rt_nblist = new Vector();
		rt_expire = 0.0;
		rt_flags = RTF_DOWN;
		rt_disc_latency = new double [MAX_HISTORY];
 		for (int i=0; i < MAX_HISTORY; i++) { 
			rt_disc_latency[i] = 0.0;
		}
		hist_indx = 0;
		rt_req_last_ttl = 0; 	
	}
	
	public void pc_insert(long id) {
		if (pc_lookup(id) == -1) {
			Long pc = new Long(id);
			rt_pclist.addElement(pc);
 		}
	} 	
	
	public long pc_lookup(long id) { 
		for ( int i = 0; i < rt_pclist.size(); i++) { 	
			long elm = ((Long) rt_pclist.elementAt(i)).longValue();
			if( elm == id) {
				return elm;
			}
 		}
		return -1;
	}

	public void pc_delete(long id) {
		for ( int i = 0; i < rt_pclist.size(); i++) { 	
			long elm = ((Long) rt_pclist.elementAt(i)).longValue();
			if( elm == id) {
 				rt_pclist.removeElementAt(i);
				break;
			} 		
		}
	}

	public void pc_delete() {
		rt_pclist.removeAllElements(); 	
	}	

	public boolean pc_empty() { 		
		return (rt_pclist.isEmpty());
	}	

	/** Disable the routing entry */
 	public void rt_down( ) {
 		/* Make sure that you don't "down" a route more than once. */
		if(rt_flags == RTF_DOWN) {
			return;
		}
		// assert (rt->rt_seqno%2); // is the seqno odd?
		rt_last_hop_count = rt_hops; 
		rt_hops = Integer.MAX_VALUE; 	
		rt_flags = RTF_DOWN; 		
		rt_nexthop = 0;
 		rt_expire = 0;
	} 

	/** rt_update: update the routing entry */
 	public void	rt_update(int seqnum, int metric, long nexthop, double expire_time) {
		rt_seqno = seqnum;
		rt_hops = metric;
		rt_flags = RTF_UP;
		rt_nexthop = nexthop;
		rt_expire = expire_time;
	}

	/** rt_update: update the routing entry */
 	public void	rt_update(int seqnum, int metric, long nexthop) {
		rt_update(seqnum, metric, nexthop, rt_expire);
	}
 	
        /** return true, if it really modifies rt_expire */
	public boolean set_expire (double new_expire) { 
		if ( rt_expire < new_expire ) {
			rt_expire = new_expire;
                        return true;
		}
                return false;
	}
	
	/** express the content of the routing entry */
	public String toString()
	{
		return "dst: " + rt_dst + " " + "h " + rt_hops + " "+  rt_req_last_ttl + " " + rt_req_timeout + " next:" + rt_nexthop + "flag: " + rt_flags;
	}
} 
