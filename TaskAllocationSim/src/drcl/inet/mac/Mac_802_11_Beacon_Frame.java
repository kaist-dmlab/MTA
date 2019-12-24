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

package drcl.inet.mac;

/**
 * This class defines the IEEE802.11 Beacon frame structure.
 *
 * @see Mac_802_11
 * @see Mac_802_11_Packet
 *
 * @author Rong Zheng
 *
 */
public class Mac_802_11_Beacon_Frame extends Mac_802_11_Packet {
	public final static int Mac_802_11_Beacon_Frame_Header_Length = 55;

	//Mac_802_11_Frame_Control fc;        // 2 bytes 
	long sa;                              // 6 bytes
	long bb_ts;                           // included in Beacon_body
	//Mac_802_11_Beacon_Body bh;          // 43 bytes
	//long                     fcs;       // 4 bytes

	/** Creates a Mac_802_11_Beacon_Frame 
	  * @param fc_ - MAC frame control
	  * @param duration_ - duration
      * @param sa_ - source MAC address
	  * @param fcs_ - frame check sequence
	  * @param hszie_ - header size
      * @param bb_ts_ - time stamp
	  */
	public Mac_802_11_Beacon_Frame(Mac_802_11_Frame_Control fc_, int duration_, long sa_, int fcs_, int hsize_, long bb_ts_) {
		super();
		headerSize = hsize_;
        size = headerSize;
        fc = fc_;
        duration = duration_;
		sa = sa_;
		fcs = fcs_;
		bb_ts = bb_ts_;
	}

	/** Set the source MAC address of the beacon frame */
	public void setSa(long sa_) {
		sa = sa_;
	}

	/** Get the source MAC address of the beacon frame */
	public long getSa() {
		return sa;
	}

	/** Set the TSF timer of the beacon frame */
	public void setTSF(long timer) {
		bb_ts = timer;
	}

	/** Get the TSF timer of the beacon frame */
	public long getTSF() {
		return bb_ts;
	}
        
    public Object clone() {
        return new Mac_802_11_Beacon_Frame((Mac_802_11_Frame_Control)fc.clone(), duration, sa, fcs, size, bb_ts);
    }
        
    public String _toString(String separator_) {
        return "Beacon Frame" + separator_ + "sa:" + sa + separator_ + "forcedError:" + forcedError + separator_; 
    }
}	
