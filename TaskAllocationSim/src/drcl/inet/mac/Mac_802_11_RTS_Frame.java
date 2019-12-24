// @(#)Mac_802_11_RTS_Frame.java   1/2004
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


// RTS Frame  
/*
--------------------------------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |          RA  (6B)       |          TA (6B)           |         FCS (4B)          |
--------------------------------------------------------------------------------------------------------------------
*/

/** 
 * This class defines the IEEE802.11 CTS frame structure.
 *
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 * @see Mac_802_11_ACK_Frame
 * @see Mac_802_11_CTS_Frame
 * @see Mac_802_11_Data_Frame
 * @see Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public class Mac_802_11_RTS_Frame extends Mac_802_11_Packet
{
    static final int Mac_802_11_RTS_Frame_Header_Length = 20;
	
    public String getName()  { return "MAC-802.11_RTS_Frame"; }
    
    /*  define the structure of RTS frame, refer to Figure 16   */
	//Mac_802_11_Frame_Control fc;        // 2 bytes  
	//int                      duration;  // 2 bytes
	long                       ra;        // 6 bytes           // the macaddr of the intended immediate recipient of the pending directed data or management frame
    long                       ta;        // 6 bytes           // the address of the STA transmitting this RTS frame
	//long                     fcs;       // 4 bytes
	
	/** Construct RTS frame 
	  * @param hszie_ - header size
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param ra_ - address of intended recipient
	  * @param ta_ - address of this STA
	  * @param fcs_ - frame check sequence
	  */
	public Mac_802_11_RTS_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_,long ta_, int fcs_) {
		super();

        headerSize = hsize_;
		size = headerSize;
		
        fc = fc_;
		duration = duration_;
		ra = ra_;
        ta = ta_;
		fcs = fcs_;
	}	

	/** Construct RTS frame 
	  * @param hszie_ - header size
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param ra_ - address of intended recipient
	  * @param ta_ - address of this STA
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  */

	public Mac_802_11_RTS_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_, long ta_, int fcs_, boolean forcedError_) {
		super();
		headerSize = hsize_;	
		size = headerSize;
		fc = fc_;
		duration = duration_;
		ra = ra_;
        ta = ta_;
		fcs = fcs_;
		forcedError = forcedError_;
	}	
	
	/** Get address of intended recipient */
	public long getRa( ) { return ra; }
	/** Set address of intended recipient */
	public void setRa(long ra_) { ra = ra_; }

	/** Get address of source station */
	public long getTa( ) { return ta; }
	/** Set address of source station */
	public void setTa(long ta_) { ta = ta_; }


	public Object clone()
	{
	    return new Mac_802_11_RTS_Frame(headerSize, (Mac_802_11_Frame_Control) fc.clone(), duration, ra, ta, fcs, forcedError);
	}
		
	public String _toString(String separator_)
	{ 
        return "RTS Frame" + separator_ + "duration:" + duration + separator_ + "ra:" + ra + separator_ + "ta:" + ta; 
        // return "frame control:" + this.fc._toString(separator_) + separator_ + "duration:" + duration + separator_ + "ra:" + ra + separator_ + "ta:" + ta; 
    }
	
}
