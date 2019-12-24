// @(#)Mac_802_11_Frame_Control.java   7/2003
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

package drcl.inet.mac;

/**
 * This class defines the IEEE802.11 frame control field structure.
 *
 * @see Mac_802_11
 * @see Mac_802_11_Data_Frame
 * @author Ye Ge
 */
public class Mac_802_11_Frame_Control implements Cloneable 
{
    // two bytes 
    
	public static final int MAC_ProtocolVersion = 0x00;
    
    public static final int MAC_Type_Management	= 0x00;
	public static final int MAC_Type_Control    = 0x01;
	public static final int MAC_Type_Data       = 0x02;
    public static final int MAC_Type_Reserved	= 0x03;
   
 	public static final int MAC_Subtype_ATIM    = 0x09;
	public static final int MAC_Subtype_Beacon  = 0x0A;
	public static final int MAC_Subtype_RTS	    = 0x0B;
	public static final int MAC_Subtype_CTS	    = 0x0C;
    public static final int MAC_Subtype_ACK	    = 0x0D;
    public static final int MAC_Subtype_Data    = 0x00;	
		
    /*	field definations, refer to Figure 15  */
    /*  
        ------------------------------------------------------------------------------------------ 
        | Pro. Ver.|   Type   |       Subtype      |                 fc_flags                    |
        |   2b     |    2b    |         4b         |                 8 bits                      |
        ------------------------------------------------------------------------------------------     
    */  
	int     fc_subtype;                // 4 bits
	int     fc_type;                   // 2 bits     
	int     fc_protocol_version;       // 2 bits
	
	boolean fc_order;                  // 1 bit  
	boolean fc_wep;                    // 1 bit
	boolean fc_more_data;              // 1 bit
	boolean fc_pwr_mgt;                // 1 bit
	boolean fc_retry;                  // 1 bit
	boolean fc_more_frag;              // 1 bit
	boolean fc_from_ds;                // 1 bit
	boolean fc_to_ds;                  // 1 bit 
		
	public Mac_802_11_Frame_Control( ) { }
    
	public Mac_802_11_Frame_Control(int subtype_, int type_, int protocol_version_) {
		fc_subtype          = subtype_;
		fc_type             = type_;
		fc_protocol_version = protocol_version_;
	}

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
		fc_order      =  order_;
	    fc_wep        =  wep_;
	    fc_more_data  =  more_data_;
	    fc_pwr_mgt    =  pwr_mgt_;
	    fc_retry      =  retry_;
	    fc_more_frag  =  more_frag_; 
	    fc_from_ds    =  from_ds_;
	    fc_to_ds      =  to_ds_;
	}
	
	/** Set frame control subtype */
	public void set_fc_subtype(int st_) { fc_subtype = st_; }
	/** Set frame control type */
	public void set_fc_type(int t_) { fc_type = t_; }
	/** Set frame control protocol version */
	public void set_fc_protocol_version(int pv_) { fc_protocol_version = pv_; }
	/** Set frame control order field */
	public void set_fc_order(boolean b_) { fc_order = b_; }
	/** Set frame control WEP field */
	public void set_fc_wep(boolean b_) { fc_wep = b_; }
	/** Set frame control more_data field */
	public void set_more_data(boolean b_) { fc_more_data = b_; }
	/** Set frame control power management field */
	public void set_pwr_mgt(boolean b_) { fc_pwr_mgt = b_; }
	/** Set frame control retry field */
	public void set_retry(boolean b_) { fc_retry = b_; }
	/** Set frame control fragmentation field */
	public void set_more_frag(boolean b_) { fc_more_frag = b_; }
	/** Set frame control frome ds field */
	public void set_from_ds(boolean b_) { fc_from_ds = b_; }
	/** Set frame control to ds field */
	public void set_to_ds(boolean b_) { fc_to_ds = b_; }
	
	/** Get frame control subtype */
	public int get_fc_subtype( ) { return fc_subtype; }
	/** Get frame control type */
	public int get_fc_type( ) { return fc_type; }
	/** Get frame control protocol version */
	public int get_fc_protocol_version( ) { return fc_protocol_version; }
	/** Get frame control order field */
	public boolean get_fc_order(boolean b_) { return fc_order; }
	public boolean get_fc_wep(boolean b_) { return fc_wep; }
	/** Get frame control WEP field */
	public boolean get_more_data(boolean b_) { return fc_more_data; }
	/** Get frame control power management field */
	public boolean get_pwr_mgt(boolean b_) { return fc_pwr_mgt; }
	/** Get frame control retry field */
	public boolean get_retry(boolean b_) { return fc_retry; }
	/** Get frame control fragmentation field */
	public boolean get_more_frag(boolean b_) { return fc_more_frag; }
	/** Get frame control frome ds field */
	public boolean get_from_ds(boolean b_) { return fc_from_ds; }
	/** Get frame control to ds field */
	public boolean get_to_ds(boolean b_) { return fc_to_ds; }

	void duplicate(Object fc_) {
		Mac_802_11_Frame_Control fc = (Mac_802_11_Frame_Control) fc_;
	    fc_subtype          = fc.fc_subtype;
	    fc_type             = fc.fc_type;
	    fc_protocol_version = fc.fc_protocol_version;
	    
		fc_order     = fc.fc_order;
	    fc_wep       = fc.fc_wep;
	    fc_more_data = fc.fc_more_data;
	    fc_pwr_mgt   = fc.fc_pwr_mgt;
	    fc_retry     = fc.fc_retry;
	    fc_more_frag = fc.fc_more_frag;
	    fc_from_ds   = fc.fc_from_ds;
	    fc_to_ds     = fc.fc_to_ds;
	}	
	
	public Object clone() {
	    Mac_802_11_Frame_Control fc_;
		fc_ = new Mac_802_11_Frame_Control(); 
		fc_.duplicate(this);
		return fc_;
	}

	public String _toString(String separator_)
	{ return ""; 
      // return "fc_type:" + fc_type + separator_ + "fc_subtype:" + fc_subtype; 
    }

    public String _toString( )
	{ return _toString("--"); }
    
}	


	


