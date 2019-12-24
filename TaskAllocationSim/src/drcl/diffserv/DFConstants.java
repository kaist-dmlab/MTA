// @(#)DFConstants.java   9/2002
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

package drcl.diffserv;

import drcl.net.Packet;
import drcl.inet.InetPacket;

/** A collection of constants that are used in the Diffserv architecture.  */
public interface DFConstants
{
	/** The IN_PROFILE value of the two-label AF class. */
	public static final int IN_PROFILE  = 0;
	/** The OUT_PROFILE value of the two-label AF class. */
	public static final int OUT_PROFILE = 2;
	/** The GREEN value of the three-label AF class. */
	public static final int GREEN       = 0;
	/** The YELLOW value of the three-label AF class. */
	public static final int YELLOW      = 2;
	/** The RED value of the three-label AF class. */
	public static final int RED         = 4;
	
    public static final String SINGLE_RATE = "SINGLE_RATE";
    public static final String TWO_RATE = "TWO_RATE";
    public static final String[] MODES = {SINGLE_RATE, TWO_RATE};
    public static final int _SINGLE_RATE = 0, _TWO_RATE = 1;

	/** The DSCP of the AF11 class. */
	public static final int AF11 = 012;
	/** The DSCP of the AF12 class. */
	public static final int AF12 = 014;
	/** The DSCP of the AF13 class. */
	public static final int AF13 = 016;
	/** The DSCP of the AF21 class. */
	public static final int AF21 = 022;
	/** The DSCP of the AF22 class. */
	public static final int AF22 = 024;
	/** The DSCP of the AF23 class. */
	public static final int AF23 = 026;
	/** The DSCP of the AF31 class. */
	public static final int AF31 = 032;
	/** The DSCP of the AF32 class. */
	public static final int AF32 = 034;
	/** The DSCP of the AF33 class. */
	public static final int AF33 = 036;
	/** The DSCP of the EF class. */
	public static final int EF   = 077;
	/** The DSCP of the BE class. */
	public static final int BE   = 000;
	/** The DSCP of the AF1x class. */
	public static final int AF1x = 010;
	/** The DSCP of the AF2x class. */
	public static final int AF2x = 020;
	/** The DSCP of the AF3x class. */
	public static final int AF3x = 030;

	/** Name array for AFxx, usage: _AF_CLASSES[dscp - 010]. */
	public static final String[] _AF_CLASSES = {
		"AF1x", "unknown", "AF11", "unknown", "AF12", "unknown", "AF13", "unknown",
		"AF2x", "unknown", "AF21", "unknown", "AF22", "unknown", "AF23", "unknown",
		"AF3x", "unknown", "AF31", "unknown", "AF32", "unknown", "AF33", "unknown"
	};

	/** Bit shift of DSCP in INET ToS */
	public static final int  DSCPShift = 3;
	/** Mask for masking out DSCP in INET ToS */
	public static final long DSCPMask = 077 << DSCPShift;

	/** The INET TOS of the AF11 class. */
	public static final long AF11_TOS = AF11 << DSCPShift;
	/** The INET TOS of the AF12 class. */
	public static final long AF12_TOS = AF12 << DSCPShift;
	/** The INET TOS of the AF13 class. */
	public static final long AF13_TOS = AF13 << DSCPShift;
	/** The INET TOS of the AF21 class. */
	public static final long AF21_TOS = AF21 << DSCPShift;
	/** The INET TOS of the AF22 class. */
	public static final long AF22_TOS = AF22 << DSCPShift;
	/** The INET TOS of the AF23 class. */
	public static final long AF23_TOS = AF23 << DSCPShift;
	/** The INET TOS of the AF31 class. */
	public static final long AF31_TOS = AF31 << DSCPShift;
	/** The INET TOS of the AF32 class. */
	public static final long AF32_TOS = AF32 << DSCPShift;
	/** The INET TOS of the AF33 class. */
	public static final long AF33_TOS = AF33 << DSCPShift;
	/** The INET TOS of the EF class. */
	public static final long EF_TOS   = EF << DSCPShift;
	/** The INET TOS of the BE class. */
	public static final long BE_TOS   = BE << DSCPShift;

	/** The INET TOS mask to distinguish AF1x, AF2x, AF3x, EF and BE packets. */
	public static final long DFCLASS_MASK = 070 << DSCPShift;
	/** The INET TOS of the AF1x class. */
	public static final long AF1x_TOS  = AF1x << DSCPShift;
	/** The INET TOS of the AF2x class. */
	public static final long AF2x_TOS  = AF2x << DSCPShift;
	/** The INET TOS of the AF3x class. */
	public static final long AF3x_TOS  = AF3x << DSCPShift;
}
