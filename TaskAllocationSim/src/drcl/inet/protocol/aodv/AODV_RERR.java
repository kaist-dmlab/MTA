// @(#)AODV_RERR.java   10/2003
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
 * AODV Route Error (RERR) packet body  
 * ref: Sec. 4.3 
 * Note: type = 1  
 * @author Wei-peng Chen 
 */ 
public class AODV_RERR 
{ 
	public final static int AODV_MAX_ERRORS = 100;
	public final static int INTEGER_SIZE    = 4;

 	/** Packet Type */
	public int		re_type;        	
	/** Hop Count */
	public int		re_hop_count;           
	/** Destination IP Address */
	public long		re_dst;                 
	/** Destination Sequence Number */
	public int		re_dst_seqno;           
	/** Source IP Address */
	public long		re_src;                 
	/** Lifetime */
	public double		re_lifetime;            
	/** when corresponding REQ sent; used to compute route discovery latency */
	public double   	re_timestamp;           

	/** DestCount */
	public int		DestCount;		 	

	/** List of Unreachable destination IP addresses and sequence numbers */
	public long[]	unreachable_dst = null;

	public int[]	unreachable_dst_seqno = null;

	public int size() {  		
		int sz = 0;
		sz = (DestCount*2 + 1) * INTEGER_SIZE;
        	return sz;
	}

	public AODV_RERR() 
	{ 
		unreachable_dst = new long [AODV_MAX_ERRORS]; 
		unreachable_dst_seqno = new int [AODV_MAX_ERRORS]; 
	} 
 
	public Object clone() 
	{ 
		AODV_RERR new_ = new AODV_RERR(); 
		new_.duplicate(this); 
		return new_; 
	} 
 
	public void duplicate(Object source_) 
	{ 
		AODV_RERR that_ = (AODV_RERR)source_; 
	} 
 
	public String toString() 
	{ 
		return "type: " + re_type + " dst: " + re_dst + " dst_seq: " + re_dst_seqno + " src: " + re_src + " count: " + DestCount + returnUnreachDest() ; 
	} 
    
    private String returnUnreachDest() {
        String tmp = new String(" unreach dst #: ");
        for (int i = 0; i < DestCount; i++) {
            tmp += (" "+ i + ": " + unreachable_dst[i] + " " + "(" + unreachable_dst_seqno[i] + ")");
        }
        return tmp;
    }
} 
