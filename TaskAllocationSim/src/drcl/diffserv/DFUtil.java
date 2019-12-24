// @(#)DFUtil.java   12/2003
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

package drcl.diffserv;

import drcl.net.Packet;
import drcl.inet.InetPacket;
import drcl.data.MapKey;

/**
 This class provides some utility functions for 
 accessing the diffserv code points (DSCP) in the ToS field of a
{@link drcl.inet.InetPacket}.
 
@author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
@version 1.0, 07/16/00
*/
public class DFUtil implements DFConstants
{
	/** Classifies the packet by the ToS value.
	 * Returns "AF11", AF12", "AF13", "AF21", "AF22", "AF23", "AF31",
	 * "AF32", "AF33", "EF" or "BE". */
	public static String classify(long tos_)
	{
		int dscp_ = (int)((tos_ & DSCPMask) >> DSCPShift);
		if (dscp_ >= 010 && dscp_ < 040)
			return _AF_CLASSES[dscp_ - 010];
		else if (dscp_ == EF)
			return "EF";
		else if (dscp_ == BE)
			return "BE";
		else
			return "unknown";
	}

	/** Classifies the packet by the ToS value.
	 * Returns "AF11", AF12", "AF13", "AF21", "AF22", "AF23", "AF31",
	 * "AF32", "AF33", "EF" or "BE". */
	public static String classify(InetPacket p_)
	{
		int dscp_ = (int)((p_.getTOS() & DSCPMask) >> DSCPShift);
		if (dscp_ >= 010 && dscp_ < 040)
			return _AF_CLASSES[dscp_ - 010];
		else if (dscp_ == EF)
			return "EF";
		else if (dscp_ == BE)
			return "BE";
		else
			return "unknown";
	}

	/** Classifies the packet by the ToS value and the label (for AF classes).
	 * Returns "AF11", AF12", "AF13", "AF21", "AF22", "AF23", "AF31",
	 * "AF32", "AF33", "EF" or "BE". */
	public static String classify(InetPacket p_, int label_)
	{
		int dscp_ = (int)((p_.getTOS() & DSCPMask) >> DSCPShift);
		if (dscp_ + label_ >= 010 && dscp_ + label_ < 040)
			return _AF_CLASSES[dscp_ + label_ - 010];
		else if (dscp_ == EF)
			return "EF";
		else if (dscp_ == BE)
			return "BE";
		else
			return "unknown";
	}

	/** Prints the ToS value in the hex form according to the ToS encoding. */
	public static String printProfileKeyValue(MapKey key_)
	{
		return "src=" + key_.value.getSubset(2)
			+ ",dest=" + key_.value.getSubset(1)
			+ "," + classify(key_.value.getSubset(0) & DSCPMask);
	}

	/** Prints the mask in the hex form according to the ToS encoding. */
	public static String printProfileKeyMask(MapKey key_)
	{
		return "#" + drcl.util.StringUtil.toHex(key_.mask.getSubset(2)) + "/#"
			+ drcl.util.StringUtil.toHex(key_.mask.getSubset(1)) + "/#"
			+ drcl.util.StringUtil.toHex(key_.mask.getSubset(0), DSCPShift, 6);
	}
	/** Prints the ToS and mask in the hex form according to the ToS encoding. */
	public static String printProfileKey(MapKey key_)
	{
		return printProfileKeyValue(key_) + "(" + printProfileKeyMask(key_) + ")";
	}
	
	/** Retrieves the DSCP field from the ToS field of the INET packet header. */
	public static int getDSCP(InetPacket p_) {
		return (int)((p_.getTOS() & DSCPMask) >> DSCPShift);
	}

	/** Sets the DSCP field in the ToS field of the INET packet header. */
	public static void setDSCP(InetPacket p_, long code) {
		p_.setTOS((p_.getTOS() & ~DSCPMask) | (code << DSCPShift));
	}
}
