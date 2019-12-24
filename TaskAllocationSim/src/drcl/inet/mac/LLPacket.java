// @(#)LLPacket.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.mac;

import drcl.net.*;


/**
 * This class defines the link layer frame format.
 *
 * @see LL
 * @see ARP
 * @see Mac_802_11
 * @see LLMacContract
 * @author Ye Ge
 */
public class LLPacket extends Packet {
    
    /**
     * destination mac address.
     */
    long dst_macaddr;
    
    /**
     *source mac address.
     */
    long src_macaddr;
    
    public String getName()  { return "LL Packet"; }
    
    /** 
     * Construct LL Packet
     * @param dst_macaddr_ - destination MAC address
     * @param src_macaddr_ - source MAC address
     * @param bsize_ - size of packet body
     * @param body_ - packet body
     */
    public LLPacket(long dst_macaddr_, long src_macaddr_, int bsize_, Object body_) {
        super(0, bsize_, body_);
        dst_macaddr = dst_macaddr_;
        src_macaddr = src_macaddr_;
    }
   /* 
    public void duplicate(Object source_) {
        super.duplicate(source_);
        
        LLPacket that_ = (LLPacket)source_;
        dst_macaddr = that_.dst_macaddr;
        src_macaddr = that_.src_macaddr;
    }
	*/
    
    /**
     * Sets destination MAC address.
     */
    public void setDstMacAddr(long dst_macaddr_) { dst_macaddr = dst_macaddr_; }
    
    /**
     * Sets source MAC address.
     */
    public void setSrcMacAddr(long src_macaddr_) { src_macaddr = src_macaddr_; }
    
    /**
     * Gets destination MAC address.
     */
    public long getDstMacAddr() { return dst_macaddr; }
    
    /**
     * Gets source MAC address.
     */
    public long getSrcMacAddr() { return src_macaddr; }

    public Object clone() {
        return new LLPacket(dst_macaddr, src_macaddr, size - headerSize, body instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)body).clone(): body);
    }
    
    public String _toString(String separator_)  {
		if (body instanceof Packet) {
			return getName() + separator_ + "src_macaddr=" + src_macaddr + separator_ + "dst_macaddr=" + dst_macaddr + separator_ + "__<" + ((Packet)body).toString(separator_) + ">__";
        }
        else if (body != null)  {
			return getName() + separator_ + "src_macaddr=" + src_macaddr + separator_ + "dst_macaddr=" + dst_macaddr + separator_ + _toString(separator_) + "__<" + drcl.util.StringUtil.toString(body) + ">__";
        }
        else
			return getName() + separator_ + "src_macaddr=" + src_macaddr + separator_ + "dst_macaddr=" + dst_macaddr + separator_ + _toString(separator_) + "__<EMPTY_BODY>__";
	}
}
