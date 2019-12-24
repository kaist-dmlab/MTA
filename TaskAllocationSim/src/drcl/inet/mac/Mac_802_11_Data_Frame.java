// @(#)Mac_802_11_Data_Frame.java   1/2004
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

// data frame 
/*
----------------------------------------------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |      DA (6B)   |    SA (6B)   |   BSSID (6B)   |  Seq Control (2B)  |  N/A (6B)  |  FCS (4B)   |
----------------------------------------------------------------------------------------------------------------------------------
 */

/**
 * This class defines the IEEE802.11 Data frame stucture.
 *
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 * @see Mac_802_11_CTS_Frame
 * @see Mac_802_11_RTS_Frame
 * @see Mac_802_11_ACK_Frame
 * @see Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public class Mac_802_11_Data_Frame extends Mac_802_11_Packet
{
    static final int Mac_802_11_Data_Frame_Header_Length = 34;
	
    public String getName()  { return "MAC-802.11_Data_Frame"; }
    
    /*  define the structure of Data frame, refer to Figure 22   */
	//Mac_802_11_Frame_Control fc;        // 2 bytes  
	//int                      duration;  // 2 bytes
	long                       da;        // 6 bytes    // the receiver's address
	long                       sa;        // 6 bytes    // the sender's address 
	long                       bssid;     // 6 bytes
	int                        scontrol;  // 2 bytes
	//long                     fcs;       // 4 bytes
    
   	/** Construct an uncorrupted 802_11 data packet 
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
	  * @param fcs_ - frame check sequence
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  */
	public Mac_802_11_Data_Frame(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_, 
								 int hsize_, int bsize_, Object body_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, false);	
		da = da_;
		sa = sa_;
	}	

   	/** Construct a 802_11 data packet 
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  */
   	public Mac_802_11_Data_Frame(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_, boolean ferror_,
								 int hsize_, int bsize_, Object body_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, ferror_);	
		da = da_;
		sa = sa_;
	}	

   	/** Construct a 802_11 data packet with sequence control
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param da_ - destination MAC address
	  * @param sa_ - source MAC address
	  * @param bssid_ - id of basic service set
      * @param ferror_ - indicating if the packet is corrupted
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  * @param fcs_ - frame check sequence
	  * @param scontrol_ - sequence control
	  */
   	public Mac_802_11_Data_Frame(Mac_802_11_Frame_Control fc_, int duration_, long da_, long sa_, long bssid_, int fcs_, boolean ferror_,
								 int hsize_, int bsize_, Object body_, int scontrol_) {
		super(hsize_, bsize_, body_, fc_, duration_, fcs_, ferror_);	
		da = da_;
		sa = sa_;
        scontrol = scontrol_;
	}	
    
    
	/** Get destination MAC address */
    public long getDa( ) { return da; }
	/** Set destination MAC address */
	public void setDa(long da_) { da = da_; }

	/** Get source MAC address */
	public long getSa( ) { return sa; }
	/** Set source MAC address */
	public void setSa(long sa_) { sa = sa_; }

	public Object clone()	{
	    return new Mac_802_11_Data_Frame(
						(Mac_802_11_Frame_Control) fc.clone(),
						duration,
						da,
						sa,
						bssid,
						fcs,
						forcedError,
						headerSize,
						size-headerSize,
						body instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)body).clone(): body,
						scontrol);
	}
		
	public String _toString(String separator_)	{
		return "Data Frame" + separator_ + "duration:" + duration + separator_ + "da:" + da + separator_ + "sa:" + sa + separator_ + "forcedError:" + forcedError + separator_; 
    }
}
