// @(#)AODV_RREP.java   10/2003
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
 * AODV Route Reply (RREP) packet body  
 * ref: Sec. 4.2 
 * Note: type = 1  
 * @author Wei-peng Chen 
 */ 
public class AODV_RREP  
{ 
	public final static int INTEGER_SIZE    = 4;

	/** Packet Type */	
	public int	rp_type;       
	/** Hop Count */
	public int	rp_hop_count;
	/** Destination IP Address */
	public long	rp_dst; 
	/** Destination Sequence Number */
	public int	rp_dst_seqno;   
	/** Source IP Address */
	public long	rp_src; 
	/** Lifetime */
	public double	rp_lifetime; 
	/** when corresponding REQ sent; used to compute route discovery latency */
	public double   rp_timestamp; 
				
	/** return the size of the payload in the RREP packet */		
	public int size() { 
		int sz = 5 * INTEGER_SIZE; //based on AODV draft version 13 
		return sz;
	}

	public AODV_RREP() 
	{} 
 
	public Object clone() 
	{ 
		AODV_RREP new_ = new AODV_RREP(); 
		new_.duplicate(this); 
		return new_; 
	} 
 
	public void duplicate(Object source_) 
	{ 
		AODV_RREP that_ = (AODV_RREP)source_; 
	        rp_type = that_.rp_type;
                rp_hop_count = that_.rp_hop_count;
                rp_dst = that_.rp_dst;
                rp_dst_seqno = that_.rp_dst_seqno;
                rp_src = that_.rp_src;
                rp_lifetime = that_.rp_lifetime;
                rp_timestamp = that_.rp_timestamp;
	} 
 
	public String toString() 
	{ 
		return "type: " + rp_type + " dst: " + rp_dst + " dst_seq: " + rp_dst_seqno + " src: " + rp_src + " time: " + rp_timestamp + " life: " + rp_lifetime + " hops: " + rp_hop_count;	
	} 
} 
