// @(#)Mac_802_11_ACK_Frame.java   1/2004
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


// ACK Frame  
/*
------------------------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |              RA  (6B)                  |             FCS (4B)            |
------------------------------------------------------------------------------------------------------------
*/

/**
 * This class defines the IEEE 802.11 ACF frame structure. 
 * 
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 * @see Mac_802_11_CTS_Frame
 * @see Mac_802_11_RTS_Frame
 * @see Mac_802_11_Data_Frame
 * @see Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public class Mac_802_11_ACK_Frame extends Mac_802_11_Packet
{
    static final int Mac_802_11_ACK_Frame_Header_Length = 14;
	
    public String getName()  { return "MAC-802.11_ACK_Frame"; }
    
    //  define the structure of ACK frame, refer to Figure 18   
	//Mac_802_11_Frame_Control fc;        // 2 bytes  
	//int                      duration;  // 2 bytes
	long                       ra;        // 6 bytes      // the macaddr the ack frame is sent to. actually it is the sender's macaddr. myself is the receiver.  
	//long                     fcs;       // 4 bytes

   	/** Construct a 802_11 ACK Frame 
	  * @param hszie_ - header size
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
      * @param ra_ - destination address
	  * @param fcs_ - frame check sequence
	  */
	public Mac_802_11_ACK_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_, int fcs_) {
		super();
        headerSize = hsize_;	
		size = headerSize;
        fc = fc_;
		duration = duration_;
		ra = ra_;
		fcs = fcs_;
	}	

   	/** Construct a 802_11 ACK Frame 
	  * @param hszie_ - header size
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
      * @param ra_ - destination address
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  */
	public Mac_802_11_ACK_Frame(int hsize_, Mac_802_11_Frame_Control fc_, int duration_, long ra_, int fcs_, boolean forcedError_) {
		super();
		headerSize = hsize_;	
		size = headerSize;
		fc = fc_;
		duration = duration_;
		ra = ra_;
		fcs = fcs_;
		forcedError = forcedError_;
	}	
	
	/** Get receiver MAC address */
	public long getRa( ) { return ra; }
	/** Set receiver MAC address */
	public void setRa(long ra_) { ra = ra_; }

	/*
	public void duplicate(Object source_) {
        super.duplicate(source_);

        Mac_802_11_ACK_Frame that_ = (Mac_802_11_ACK_Frame) source_;
		ra       = that_.ra;

        // done in super.duplicate()
        //fc = (Mac_802_11_Frame_Control) that_.fc.clone();
		//duration = that_.duration;
        //fcs      = that_.fcs;
		//forcedError = that_.forcedError;
        //setHeaderSize(that_.headerSize);
	}	
	*/
	
	public Object clone() {
	    return new Mac_802_11_ACK_Frame(headerSize, (Mac_802_11_Frame_Control) fc.clone(), duration, ra, fcs, forcedError);
	}
		
	public String _toString(String separator_) { 
        return "ACK Frame" + separator_ + "duration:" + duration + separator_ + "ra:" + ra + separator_ + "forcedError=" + forcedError; 
        // return "ACK Frame" + separator_ + "frame control:" + this.fc._toString(separator_) + separator_ + "duration:" + duration + separator_ + "ra:" + ra + separator_ + "forcedError=" + forcedError; 
    }
}

