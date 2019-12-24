// @(#)OSPF_Hello.java   9/2002
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

/**
 * OSPF Hello packet body.
 * 
 * <p>ref: A.3.2.
 * Note: type = 1
 * 
 * @author Wei-peng Chen
 */
import java.util.*;

public class OSPF_Hello extends drcl.DrclObj
{
	private int	hello_interval;
	private int	router_dead_interval;
	/* The OSPF Options field is present in OSPF Hello packets, Database
           Description packets and all LSAs. */
	private int     options;
	
	/* this field does not exist in the specification, for implementation simplicity */
	protected int	neighbor_no;
	protected Vector		neighbor_id_list; /* router id of neighbors */

	public Object clone()
	{
		OSPF_Hello new_ = new OSPF_Hello(hello_interval, router_dead_interval);
		new_.duplicate(this);
		return new_;
	}

	public void duplicate(Object source_)
	{
		OSPF_Hello that_ = (OSPF_Hello)source_;
		neighbor_no = that_.neighbor_no;
		neighbor_id_list.removeAllElements();
		neighbor_id_list.setSize(neighbor_no);
		for (int i=0; i<neighbor_no; i++)
			neighbor_id_list.setElementAt(that_.neighbor_id_list.elementAt(i), i); // FIXME: dont clone
	}
	
	OSPF_Hello( int hello_interval_, int dead_interval)
	{
		neighbor_id_list = new Vector();
		hello_interval = hello_interval_;
		router_dead_interval = dead_interval;
	}
	
	protected int get_hello_interval() { return hello_interval; }
	protected int get_router_dead_interval() { return router_dead_interval; }
  
	protected void set_neighbor_id(Vector list) {
		neighbor_no = list.size();
		neighbor_id_list = list;
	}

	protected void add_neighbor_id( int id ) {
		neighbor_no++ ;
		neighbor_id_list.addElement( new Integer(id));
	}

	private static final int FIX_HELLO_LEN = 20;
	
	/**
	 * return the packet length of Hello Body (in byte) , which excludes the OSPF header length
	 * Network Mask: 4, HelloInterval:2, Options:1, Rtr Pri:1, RouterDeadInterval: 4
	 * Designated Router: 4, Backup: 4, the sum of the above items is assigned to 
	 * FIX_HELLO_LEN = 20, Neighbor: each 4 byte
	 */
	protected int size()
	{
		return FIX_HELLO_LEN + neighbor_no *4;
	}
}

