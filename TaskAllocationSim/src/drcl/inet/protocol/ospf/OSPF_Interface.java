// @(#)OSPF_Interface.java   9/2002
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

// xxx: support multiple adjacencies over one interface

import java.util.*;
import drcl.inet.core.*;
import drcl.comp.*;

/**
 * An OSPF interface is the connection	between	a router and a network.
 * One or more router adjacencies may develop over an interface.
 * A router's LSAs	reflect	the state of its interfaces and their associated adjacencies.
 * <p>ref: sec 9
 * 
 * @author Wei-peng Chen
 * @see OSPF
 * @see OSPF_Neighbor
 * @see OSPF_TimeOut_EVT
 */
public class OSPF_Interface //extends drcl.comp.Component 
{
	/* Interface ID; same as ifindex */
	protected int if_id;

	/**A router interface belongs to one and only one area */
	protected OSPF_Area	area; 

	/* in the following data items are listed according to sec. 9 */
	/* Marked items are not implemented in the current stage */

	/** the type info from interface, default value: IF_PTOP */
	protected int type;

	/* Interface State */
	protected int	state;
  
	/* linklocal address of this I/F */ /* point to point interface do not have IP addr */
	protected long		lladdr;

	/* list of prefixes: struct in6_addr */
	/*list prefix_list;*/

	/** all routing pkts originating from this interface are labelled with this area id */
	private int area_id;

	//protected int link_id;
	
	/* Timers */
	/** the # of second between hello pkts are sent over this interface*/
	protected int		hello_interval;

	/** the # of second before the neighbor is declared to be down*/
	protected int		dead_interval;

	/** I/F transmission delay, the approximate delay over this link */
	protected int		transdelay;

	/* timeout event */
	protected OSPF_TimeOut_EVT	Hello_TimeOut_EVT;
	protected OSPF_TimeOut_EVT	Ack_Delay_Reach_EVT;
	
	/* Router Priority */
	/*u_char priority;*/

	/** list of ospf neighbor over this interface*/
	//OSPF_Neighbor[] neighbor_list;
	protected Vector	neighbor_list;
	
	/** Cost for transmitting a packet over this interface, must be greater than zero */
	private int cost;
	
	protected int  get_cost() { return cost; }
	protected void set_cost(int cost_) { cost = cost_; }

	/**
	 * The number of seconds between LSA retransmissions, for adjacencies belonging to 
	 * this interface.  Also used when retransmitting Database	Description and	
	 * Link State Request Packets.
	 */
	protected int		rxmt_interval;

	/** LSAs to Delayed Acknowledge */
	protected Vector	lsa_delayed_ack;

	/** I/F MTU */
	protected int ifmtu;

	/* statistics */
	protected int ospf_stat_dr_election;
	protected int ospf_stat_delayed_lsack;	

	/* for getting info of real interface */
	Port out_port;
	
	public String toString()
	{
		return "area:" + area_id + ",if_id:" + if_id + ",cost:" + cost + ",neighbors:" + neighbor_list;
	}
	
	public String info(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer(OSPF.IF_STATES[state] + ",area:" + area_id + ",if_id:"
			+ if_id + ",cost:" + cost + ",transdelay:" + transdelay + "\n");
		if (neighbor_list == null || neighbor_list.size() == 0)
			sb_.append(prefix_ + "Neighbors:<none>\n");
		else {
			sb_.append(prefix_ + "#Neighbors:" + neighbor_list.size() + "\n");
			for (int i=0; i<neighbor_list.size(); i++) {
				OSPF_Neighbor n_ = (OSPF_Neighbor)neighbor_list.elementAt(i);
				if (n_ == null) continue;
				sb_.append(prefix_ + "Neighbor" + i + ": " + n_.info(prefix_ + "   ") + "\n");
			}
		}
		return sb_.toString();
	}

	/**
	 * Constructor
	 */
	protected OSPF_Interface() 
	{
		Hello_TimeOut_EVT = null;
		Ack_Delay_Reach_EVT = null;
	}
	
	public void reset()
	{
		Hello_TimeOut_EVT = null;
		Ack_Delay_Reach_EVT = null;
		neighbor_list.removeAllElements();
		lsa_delayed_ack.removeAllElements();
	}
	
	/**
	 * Disable all timer
	 */
	protected void clear()
	{
		Hello_TimeOut_EVT = null;
		Ack_Delay_Reach_EVT = null;
		
		neighbor_list = null;
		lsa_delayed_ack = null;
	}
	
	/**
	 *  Constructor: Create new ospf6 interface structure 
	 *  In our implementation, this function is called when OSPF receives 
	 *  NEIGHBOR_UP event so that the state of interface is UP
	 */
	public static OSPF_Interface ospf_interface_create ( int if_index, int type_, int mtu_,
		OSPF_Area area_, Port out_)
	{
		OSPF_Interface oi = new OSPF_Interface();
		if ( oi == null ) {
			/*if (isDebugEnabled()) debug(" %%% Error %%% : Can't allocate ospf6_interface for ifindex" +  if_index);*/
			return (OSPF_Interface ) null;
		}
		oi.lladdr = 0;
		oi.area = area_;
		oi.state = OSPF.IFS_DOWN;
		oi.neighbor_list = new Vector ();
		
		/*ospf6_lsdb_init_interface (o6i);*/
		oi.lsa_delayed_ack = new Vector();
		
		oi.transdelay = 1;
		/* oi.priority = 1; */
		oi.hello_interval = 10;
		oi.dead_interval = 40;
		oi.rxmt_interval = 5;
		oi.cost	= 1; // default: hop count
		oi.ifmtu = mtu_ ;
		/*oi->prefix_list = list_init ();*/
		/*oi->lsa_seqnum_link = o6i->lsa_seqnum_network
                       = o6i->lsa_seqnum_intra_prefix
                       = INITIAL_SEQUENCE_NUMBER;*/

		// xxx:
		/* register interface list */
		/*if ( os == null )
			os = ospf6_start (); 
		ospf.list_add_node (ospf.ospf_interface_list, oi); */

		/* link both */
		/* oi.ni = ifp;*/
		oi.if_id = if_index;
		
		/*ifp.info = oi;*/
		oi.Hello_TimeOut_EVT = null;
		
		/* add a port for reference to real interfaqce info */
		oi.out_port = out_;
		
		return oi;
	}

	/**
	 *  count number of full neighbor adjacent to this interface
	 */
	protected int ospf_interface_count_full_nbr ()
	{
		int count = 0;
		int i;
		
		int no = neighbor_list.size();
		for (i = 0; i < no; i++) {
			OSPF_Neighbor nbr = (OSPF_Neighbor) neighbor_list.elementAt(i);
			if (nbr.state == OSPF.NBS_FULL)
				count++;
		}
		return count;
	}


	/* Interface State Machine */
	/**
	 * Never called in this implemented since we use CSL hello service
	 * Interface_UP event is trigger be NEIGHBOR_UP event 
	 */
	protected int Interface_Up ()
	{
		/* ifid of this interface */
		/*  ospf6_interface->if_id = ospf6_interface->interface->ifindex;*/

		/* Join AllSPFRouters */
		//ospf6_join_allspfrouters (ospf6_interface->interface->ifindex);

		/* set socket options */
		// ospf6_reset_mcastloop ();
		// ospf6_set_pktinfo ();
		// ospf6_set_checksum ();

		/* decide next interface state */
		if ( (type == OSPF.IF_PTOP) || (type == OSPF.IF_PTOMP) || (type == OSPF.IF_VIRTUAL))
			ifs_change (OSPF.IFS_PTOP, "IF Type PointToPoint");
		/*else if (ospf6_interface->priority == 0)
			ifs_change (IFS_DROTHER, "Router Priority = 0", ospf6_interface);
		else {
			ifs_change (IFS_WAITING, "Priority > 0", ospf6_interface);
			thread_add_timer (master, wait_timer, ospf6_interface, ospf6_interface->dead_interval);
		}*/

		/* construct LSAs */
		/*ospf6_lsa_originate_link (ospf6_interface);
		ospf6_lsa_originate_intraprefix (ospf6_interface);*/

		return 0;
	}

	protected int Interface_Down ()
	{

		/*o6log.ism ("I/F [%s] InterfaceDown", ospf6_interface->interface->name);*/
		if ( state == OSPF.IFS_NONE)
			return 1;

		ifs_change (OSPF.IFS_DOWN, "Configured");

		return 0;
	}

	/**
	 * Oe of the neighbors associated with this interface change its state
	 */
	protected int neighbor_change ( )
	{
		if ( state != OSPF.IFS_DROTHER && state != OSPF.IFS_BDR && state != OSPF.IFS_DR)
			return 0;
		/*o6log.ism ("I/F [%s] NeighborChange", ospf6_interface.name);*/
		// xxx: dr_election in the next stage
		/*ifs_change (dr_election (ospf6_interface), "NeighborChange:DR Election", ospf6_interface);*/
		return 0;
	}

	/**
	 * interface state change
	 */
	private int ifs_change (int ifs_next, String reason)
	{
		int ifs_prev;

		ifs_prev = state;

		/*if (IS_OSPF6_DUMP_INTERFACE)
			zlog_info ("I/F [%s] %d -> %d (%s)", ni.name,ifs_prev, ifs_next, reason);*/

		/*
		switch (ifs_prev) {
		    case OSPF.IFS_DR:
		    case OSPF.IFS_BDR:
				switch (ifs_next) {
					case OSPF.IFS_DR:
					case OSPF.IFS_BDR:
						break;
					default:
						ospf6_leave_alldrouters (ni.ifindex);
						break;
				}
				break;
			default:
				switch (ifs_next) {
					case OSPF.IFS_DR:
					case OSPF.IFS_BDR:
						ospf6_join_alldrouters (ospf6_interface->interface->ifindex);
						break;
					default:
						break;
				}
				break;
		} */
		state = ifs_next;

		return 0;
	}

	/**
	 * Lookup the neighbor associated with this interface with router id
	 */
	protected OSPF_Neighbor ospf_nbr_lookup_by_routerid ( long id)
	{
		int i;
		int no = neighbor_list.size();
		for ( i = 0; i < no; i++) {
			OSPF_Neighbor nbr = (OSPF_Neighbor) neighbor_list.elementAt(i);
			if (nbr.rtr_id == id) {
				return nbr;
			}
		}
		return null;
	}
	
	/**
	 * Check whther this interface is the type of point-to-point
	 */
	protected int if_is_pointopoint()
	{
		// temp stage, only consider point to point
		return 1;
	}
	
	/**
	 * Add one new neighbor to this interface
	 */
	protected void add_new_neighbor( OSPF_Neighbor nbr )
	{
		if (neighbor_list == null)
			neighbor_list = new Vector();
		neighbor_list.addElement(nbr);
	}

	/**
	 * Remove one new neighbor to this interface
	 */
	protected void remove_neighbor( OSPF_Neighbor nbr )
	{ neighbor_list.removeElement(nbr); }			
}
