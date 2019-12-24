// @(#)OSPF_LSA.java   9/2002
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

// xxx: type 2-5 LSA

import java.util.*;
import drcl.net.*;

/**
 * Link State Advertisement.
 * Ref: RFC 2328 sec. 4.3 Table 9
 * <table border=1>
 * <tr>
 *   <td>Type <td nowrap>LSA Name          <td nowrap>Originate from      
 *   <td>Content
 *   <td>Extent		<td>Originator
 * <tr>
 *   <td valign=top>1    <td valign=top nowrap>Router-LSAs       <td valign=top nowrap>all routers
 *   <td valign=top>states of the router's interface to an area
 *   <td valign=top nowrap>intra-area	<td valign=top>Router
 * <tr>
 *   <td valign=top>2    <td valign=top nowrap>Network-LSAs      <td valign=top nowrap>brocast and MBMA
 *   <td valign=top>list of routers connected to the network
 *   <td valign=top nowrap>intra-area	<td>network's desiginated router
 * <tr>
 *   <td valign=top nowrap>3,4  <td valign=top nowrap>Summary-LSAs      <td valign=top>area border router
 *   <td valign=top nowrap>3: router to networks outside this area<br>
 *       4: router to AS boundary router
 *   <td valign=top nowrap>inter-area
 *   <td valign=top>area border router
 *											  
 * <tr>
 *   <td valign=top>5    <td valign=top>As-external-LSAa  <td valign=top>AS boundary router 
 *   <td valign=top>a route to a dest. in another AS             
 *   <td valign=top nowrap>inter-AS	<td valign=top>AS boundary router
 * </table>
 * Except type 5, all other four types are flooded through a single area only.
 * Type 5 is flooded through the entire AS.
 *
 * @author Wei-peng Chen	
 * @see OSPF_LSA_Header
 * @see Router_LSA
 */
public class OSPF_LSA extends drcl.DrclObj
{
	protected final static int OSPF_LSA_HEADER_SIZE	= 20;

	// Tyan: 05/08/2001, remove flags to save space
	protected int			floodback_flag	;
	//protected int			duplicate_flag	;
	//protected int			impliedack_flag ;

	/* timeout event */
	//protected OSPF_TimeOut_EVT		LSA_Refresh_EVT;
	//protected OSPF_TimeOut_EVT		LSA_MaxAge_Reach_EVT;
	
	// for implementation
	// protected int from; // the router id of the originating router which sends this lsa
	protected OSPF_Neighbor	from;
	protected Object			scope;
	
	protected OSPF_LSA_Header header;
	protected OSPF_LSA_Header getHeader() { return header; }
	
	public String toString()
	{ return _toString(); }

	public String _toString()
	{
		return "from:" + (from == null? "-": from.rtr_id+"") + ",header:"
			+ (header == null? "-": "<" + header + ">");
	}

	/** Constructor */
	protected OSPF_LSA() 
	{ this(new OSPF_LSA_Header()); }
	
	/** Constructor */
	protected OSPF_LSA(OSPF_LSA_Header header_) 
	{
		//LSA_Refresh_EVT = null;
		header = header_;
	}
	
	/** return the size of the LSA */
	protected int size()
	{
		switch(header.lsh_type) {
			case OSPF.OSPF_ROUTER_LSA:
				return ((Router_LSA) this).size();
			default:
				return Integer.MAX_VALUE; // error
		}		
	}
	
	// clone fields that are really in OSPF packet
	public Object clone()
	{ return new OSPF_LSA((OSPF_LSA_Header)header.clone()); }
} // End of LSA
