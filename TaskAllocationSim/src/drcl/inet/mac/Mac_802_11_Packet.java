// @(#)Mac_802_11_Packet.java   1/2004
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

// common fields for RTS, CTS, ACK and DATA Frames  
/*
------------------------------------------------------------------------------------------------------------
|    FC (2B)    | Duration (2B) |       ..................               |             FCS (4B)            |
------------------------------------------------------------------------------------------------------------
*/

/**
 * This class is the base class of IEEE802.11 RTS, CTS, ACK and Data frame classes.
 *
 * @see Mac_802_11
 * @see Mac_802_11_ACK_Frame
 * @see Mac_802_11_CTS_Frame
 * @see Mac_802_11_RTS_Frame
 * @see Mac_802_11_Data_Frame
 * @see Mac_802_11_Frame_Control
 * @author Ye Ge
 */
public abstract class Mac_802_11_Packet extends Packet
{
     /* constant definations */
	public String getName()  { return "MAC-802.11 Packet"; }
    
    boolean forcedError;       // for simulation use only
	public void setForcedError(boolean b_) {	forcedError = b_; }
	public boolean isForcedError( ) { return forcedError; }

    /* common fields in all four different frames */
    Mac_802_11_Frame_Control fc;        // 2 bytes
   	int                      duration;  // 2 bytes
	int                      fcs;       // 4 bytes
    

	public static final int ETHER_ADDR_LEN = 6;
	public static final int ETHER_TYPE_LEN = 2;
	public static final int EHTER_FCS_LEN  = 4;
	
	/** Get the frame control field (2 Byte)*/
    public Mac_802_11_Frame_Control getFc( ) { return fc; }
	/** Set the frame control field (2 Byte)
	 */
	public void         setFc(Mac_802_11_Frame_Control fc_) { fc = fc_; }
	/** Get the frame duration field (2 Byte)*/
    public int          getDuration( ) { return duration; }
	/** Set the frame duration field (2 Byte)*/
	public void         setDuration(int d_) { duration = d_; }
	/** Get the frame check sequence (4 Byte)
	  */
	public int          getFcs( ) { return fcs; }
	/** Set the frame check sequence (4 Byte)*/
	public void         setFcs(int fcs_) { fcs = fcs_; }

	/** Set the frame control flags 
	  * @param order_ -  0, 1 order field 
	  * @param wep_ - set to 1 if the frame body field contains information processed by WEP algorithm
	  * @param more_data_ - set to 1 if AP has more data to transmit to the STA
      * @param pwr_mgt_ - set to 1 if a STA is in power-saving
      * @param retry_ - set to 1 if it is a retransmission of an earlier frame
	  * @param more_frag_ - set to 1 if there is one or more fragments to follow
	  * @param from_ds_ - set to 1 if data type frames existing the DS
	  * @param to_ds_ - set to 1 if data type frames destined the DS
	  */
	 
	public void set_fc_flags(boolean order_, boolean wep_, 
                    boolean more_data_, boolean pwr_mgt_, 
                    boolean retry_, boolean more_frag_,
					boolean from_ds_, boolean to_ds_) {
		fc.set_fc_flags(order_, wep_, more_data_, pwr_mgt_, 
                        retry_, more_frag_, from_ds_, to_ds_); 
	}
    
	/** Construct a 802_11 packet */
    public Mac_802_11_Packet() {
		super();
		forcedError = false;
	}	
	
	/** Construct a 802_11 packet 
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  */
    public Mac_802_11_Packet(int hsize_, int bsize_, Object body_)
	{
		super(hsize_, bsize_, body_);
		forcedError = false;
	}
	
   	/** Construct a 802_11 packet 
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
      * @param ferror_ - indicating if the packet is corrupted
	  */
	public Mac_802_11_Packet(int hsize_, int bsize_, Object body_, boolean ferror_)
	{
		super(hsize_, bsize_, body_);
		forcedError = ferror_;
	}
    
   	/** Construct a 802_11 packet 
	  * @param hszie_ - header size
      * @param bsize_ - body size
      * @param body_ - packet body
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  */
	public Mac_802_11_Packet(int hsize_, int bsize_, Object body_, Mac_802_11_Frame_Control fc_, int duration_, int fcs_, boolean ferror_)
	{
		super(hsize_, bsize_, body_);
		fc = fc_;
		duration = duration_;
		fcs = fcs_;
        forcedError = ferror_;
	}
    
   	/** Construct a 802_11 packet with empty body 
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param fcs_ - frame check sequence
      * @param ferror_ - indicating if the packet is corrupted
	  */
    public Mac_802_11_Packet(Mac_802_11_Frame_Control fc_, int duration_, int fcs_, boolean ferror_) {
		super(); 
		forcedError = ferror_;
		fc = fc_;
		duration = duration_;
		fcs = fcs_;
	}	

   	/** Construct a non-corrupted 802_11 packet with empty body
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
	  * @param fcs_ - frame check sequence
	  */
   	public Mac_802_11_Packet(Mac_802_11_Frame_Control fc_, int duration_, int fcs_) {
		super(); 
		forcedError = false;
		fc = fc_;
		duration = duration_;
		fcs = fcs_;
	}	
    
	/*
    public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Mac_802_11_Packet that_ = (Mac_802_11_Packet)source_;
		forcedError = that_.forcedError;
        fc = (Mac_802_11_Frame_Control) that_.fc.clone();
        duration = that_.duration;  
        fcs = that_.fcs;
        setHeaderSize(that_.headerSize);   // size is adjusted accordingly also
	}

    // should be overriden by subclasses
	public Object clone()
	{
		return new Mac_802_11_Packet(headerSize, size-headerSize, body instanceof drcl.ObjectCloneable? ((drcl.ObjectCloneable)body).clone(): body, fc.clone(), duration, fcs, forcedError);
        //return null;
	}
	*/

	public String _toString(String separator_)  { return "" + separator_; }
 
}


