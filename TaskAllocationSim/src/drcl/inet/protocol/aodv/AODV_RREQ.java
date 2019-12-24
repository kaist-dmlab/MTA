// @(#)AODV_RREQ.java   10/2003
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
import drcl.net.*; 
 
/** 
 * AODV Route Request (RREQ) packet body  
 * ref: Sec. 4.1 
 * Note: type = 1  
 * @author Wei-peng Chen 
 */ 
public class AODV_RREQ 
{ 
	public final static int	RREQ_GRAT_RREP = 0x80;
	public final static int INTEGER_SIZE    = 4;
	
	public int	rq_type;	/* Packet Type */	
	/* XXX: unimplemented */
	public int	rq_j_flag;		
	/* XXX: unimplemented */
	public int	rq_r_flag;	
	/* XXX: unimplemented */
	public int	rq_g_flag;	
	/* XXX: unimplemented */
	public int	rq_d_flag;  
	/* XXX: unimplemented */
 	public int	rq_u_flag; 
	/* Hop Count */
	public int	rq_hop_count;  
	/** Broadcast ID */ 
 	public int	rq_bcast_id;    
	/** Destination IP Address */
	public long	rq_dst;
	/** Destination Sequence Number */ 
	public int	rq_dst_seqno;
	/** Source IP Address */ 
	public long	rq_src; 
	/** Source Sequence Number*/ 
	public int	rq_src_seqno;
	/** when REQUEST sent; used to compute route discovery latency */
	public double	rq_timestamp;

	public int size() {  		
		int sz = 0; 
		sz = 6 * INTEGER_SIZE; // besed on AODV draft version 13
		return sz;
	}
 
	public int ls_req_num; 
	public Vector req_list = new Vector(); 
 
	public AODV_RREQ() 
	{} 
 
	public Object clone() 
	{ 
		AODV_RREQ new_ = new AODV_RREQ(); 
		new_.duplicate(this); 
		return new_; 
	} 
 
	public void duplicate(Object source_) 
	{ 
		AODV_RREQ that_ = (AODV_RREQ)source_; 
		rq_type = that_.rq_type;
		rq_hop_count = that_.rq_hop_count;
		rq_bcast_id = that_.rq_bcast_id;
		rq_dst = that_.rq_dst;
		rq_dst_seqno = that_.rq_dst_seqno;
		rq_src = that_.rq_src;
		rq_src_seqno = that_.rq_src_seqno;
		rq_timestamp = that_.rq_timestamp;
	} 
 
	public String toString() 
	{ 
		return "type: " + rq_type + " dst: " + rq_dst + " dst_seq: " + rq_dst_seqno + " src: " + rq_src + " src_seq: " + rq_src_seqno + "bcast_id: " + rq_bcast_id + " time: " + rq_timestamp + " hops: " + rq_hop_count; 
	} 
} 
