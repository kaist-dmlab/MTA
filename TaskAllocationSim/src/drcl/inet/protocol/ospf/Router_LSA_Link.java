// @(#)Router_LSA_Link.java   9/2002
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
 * OSPF Router-LSAs link data structure.
 * 
 * In the first step, link type 1 is implemented.
 * link type = 1, point to point link, link id = neighbor router id.
 * Ref: table 18
 * <table border=1>
 * <tr>
 * <td>type	<td>description					<td>link id								<td>link data
 * <tr>
 * <td>1	<td>point-to-point				<td>neighbor's router id				<td>(1) (numbered)
 * <tr>
 * <td>2	<td>link to transit network		<td>interface addr of designated router	<td>(1)
 * <tr>
 * <td>3	<td>link to stub network		<td>IP network number					<td>(2)
 * <tr>
 * <td>4	<td>virtual link				<td>neighbor's router id				<td>(1)
 * </table>
 * (1) IP interface addr of the associated router interface.
 * (2) stub's network's IP addr mask.
 * 
 * For field descriptions, refer to A.4.2
 *
 * @author Wei-peng Chen
 */
class Router_LSA_Link
{
	protected int link_id;
	protected long link_data;
	protected int type;
	protected int tos_no;
	protected int metric;
	
	/* the basic size, not including tos length */
	private final int ROUTER_LSA_LINK_BASE_LEN		= 4;

	/**
	 *  tos_list: list of tos type
	 */
	protected Vector tos_list;
	/**
	 *  tos_metric_list: list of tos metric value
	 */
	protected Vector tos_metric_list;
	
	/** Constructor */
	protected Router_LSA_Link( )
	{
		tos_no = 0;
		tos_list = new Vector();
		tos_metric_list = new Vector();
	}

	/**
	 * return the size of one link data
	 */
	protected int size() 
	{
		/* one tos occupies one byte */
		return (ROUTER_LSA_LINK_BASE_LEN + tos_no );
	}
	
	/**
	 *  add one tos metric info.
	 */
	protected void add_tos_metric( int tos, int tos_metric )
	{
		tos_list.addElement( new Integer(tos) );
		tos_metric_list.addElement( new Integer(tos_metric) );
		tos_no++;
	}

	/**
	 *  given the tos type, return the tos metric value.
	 */
	protected int get_tos_metric( int tos )
	{
		int tos_index_ = tos_list.indexOf( new Integer(tos) );
		return ( ((Integer) (tos_metric_list.elementAt(tos_index_))).intValue() );
	}

	public boolean equals(Object that_)
	{
		Router_LSA_Link lsd = (Router_LSA_Link) that_;
		return link_id == lsd.link_id && link_data == lsd.link_data;
	}

	public String toString()
	{
		return "<neighbor:" + link_id + ",metric:" + metric + ">";
	}
}
