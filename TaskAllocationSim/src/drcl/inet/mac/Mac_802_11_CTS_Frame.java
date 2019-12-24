// @(#)Mac_802_11_CTS_Frame.java   1/2004
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
---------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |             RA  (6B)          |         FCS (4B)          |
---------------------------------------------------------------------------------------------
*/

/** 
 * This class defines the IEEE802.11 CTS frame structure.
 *
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 * @see Mac_802_11_ACK_Frame
 * @see Mac_802_11_RTS_Frame
 * @see Mac_802_11_Data_Frame
 * @see Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public class Mac_802_11_CTS_Frame extends Mac_802_11_Packet
{
    static final int Mac_802_11_CTS_Frame_Header_Length = 14;
	
    public String getName()  { return "MAC-802.11_CTS_Frame"; }
    
    /**  define the structure of  frame, refer to Figure 17   */
	long                       ra;        // 6 bytes    // the address of the STA this cts is responding to, copied from the TA field of the RTS frame


	/** Construct a CTS frame
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param fcs_ - frame check sequence
	  */
	public Mac_802_11_CTS_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_, int fcs_) {
		super();
        headerSize = hsize_;	
		size = headerSize;
        fc = fc_;
		duration = duration_;
		ra = ra_;
		fcs = fcs_;
	}	

	/** Construct a CTS frame
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  */
	public Mac_802_11_CTS_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_, int fcs_, boolean forcedError_) {
		super();
		headerSize = hsize_;	
		size = headerSize;
		fc = fc_;
		duration = duration_;
		ra = ra_;
		fcs = fcs_;
		forcedError = forcedError_;
	}	
	
	/** Get receiver's address */
	public long getRa( ) { return ra; }
	/** Set receiver's address */
	public void setRa(long ra_) { ra = ra_; }

	public Object clone() {
	    return new Mac_802_11_CTS_Frame(headerSize, (Mac_802_11_Frame_Control) fc.clone(), duration, ra, fcs, forcedError);
	}
		
	public String _toString(String separator_) { 
        return "CTS Frame" + separator_ + "duration:" + duration + separator_ + "ra:" + ra; 
        // return "CTS Frame" + separator_ + "frame control:" + this.fc._toString(separator_) + separator_ + "duration:" + duration + separator_ + "ra:" + ra;
    }
}
