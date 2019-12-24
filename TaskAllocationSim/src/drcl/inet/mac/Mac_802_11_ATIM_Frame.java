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

/*
 * Mac_802_11_ATIM_Frame.java
 *
 * Created on August 26, 2002, 4:37 PM
 */

package drcl.inet.mac;

/**
 * This class defines the IEEE802.11 ATIM frame structure.
 * 
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 *
 * @author Rong Zheng
 */
public class Mac_802_11_ATIM_Frame extends Mac_802_11_Packet {
    
    long src;
    long dst;
    
    /** Creates a new instance of Mac_802_11_ATIM_Frame 
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
  	  * @param src_ - source MAC address
      * @param dst_ - destination MAC address
	  * @param fcs_ - frame check sequence
	  * @param hszie_ - header size
	  */
   public Mac_802_11_ATIM_Frame(Mac_802_11_Frame_Control fc_, int duration_, long src_, long dst_, int fcs_, int hsize_) {
        super();
        headerSize = hsize_;
        size = headerSize;
        fc = fc_;
        duration = duration_;
        src = src_;
        dst = dst_;
    	fcs = fcs_;        
    }
    
	/** Get source MAC address */
    public long getSa() {
        return src;
    }
    
	/** Get destination MAC address */
    public long getDa() {
        return dst;
    }
    
    public Object clone() {
        return new Mac_802_11_ATIM_Frame((Mac_802_11_Frame_Control)fc.clone(), duration, src, dst, fcs, size);
    }
        
    public String _toString(String separator_) {
        return "ATIM Frame" + separator_ + "sa:" + src + separator_ + "da:" + dst + separator_; 
    }    
}
