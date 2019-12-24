// @(#)IntServToS.java   9/2002
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

package drcl.intserv;

import java.util.*;
import drcl.net.*;
import drcl.inet.InetPacket;

public class IntServToS
{
	public static String interpretType(InetPacket h_) 
	{
		try {
			switch((int)h_.getTOS() & TYPE_MASK)	{
			case QoS_DATA:			return "QoS";
			case CONTROL:			return "CTL";
			case BEST_EFFORTS_DATA:	return "BE";
			default:				return "unknown " + h_.getTOS();
			}
		} catch (Exception e_) {
			return "No ToS: " + e_ + "| packet:" + h_;
		}
	}
	
	public static String interpretType(int type_) 
	{
		switch(type_)	{
		case QoS_DATA:			return "QoS";
		case CONTROL:			return "CTL";
		case BEST_EFFORTS_DATA:	return "BE";
		default:				return "unknow " + type_; 
		}
	}
	
	//
	static void ___ToS___() {}
	//
	
	// For IntServ, ToS and connection id is one-to-one mapping.
	// ToS:
	// 63-48: source
	// 47-32: dest
	// 31-20: src port#
	// 19-8:  dest port#
	// 1-0:  packet type
	static final int SRC_START		= 48;
	static final int SRC_LENGTH		= 16; // 16 bits
	static final int DEST_START		= 32;
	static final int DEST_LENGTH	= 16; // 16 bits
	static final int SPORT_START	= 20;
	static final int SPORT_LENGTH	= 12; // 12 bits
	static final int DPORT_START	= 8;
	static final int DPORT_LENGTH	= 12; // 12 bits
									
	public static final int	QoS_DATA			= 0x3;
	public static final int	CONTROL				= 0x1;
	public static final int	BEST_EFFORTS_DATA	= 0x0;
	
	public static final int TYPE_MASK		= 0x3;
	public static final long SRC_MASK		= (1 << SRC_LENGTH   - 1) << SRC_START;
	public static final long DEST_MASK		= (1 << DEST_LENGTH  - 1) << DEST_START;
	public static final long SRCULP_MASK	= (1 << SPORT_LENGTH - 1) << SPORT_START;
	public static final long DESTULP_MASK	= (1 << DPORT_LENGTH - 1) << DPORT_START;
	
	public static long getToS(long src_, int srcUlp_, long dest_, int destUlp_, int type_)
	{
		return (src_  << SRC_START) | 
			   ((dest_ << DEST_START) & DEST_MASK) |
			   (((long)srcUlp_  << SPORT_START) & SRCULP_MASK) |
			   (((long)destUlp_ << DPORT_START) & DESTULP_MASK) |
			   (long)type_;
	}
	
	public static long getSource(long tos_) { return (tos_ & SRC_MASK) >> SRC_START; }
	public static long getDest(long tos_) { return (tos_ & DEST_MASK) >> DEST_START; }
	public static int getSrcUlp(long tos_) { return (int)((tos_ & SRCULP_MASK) >> SPORT_START); }
	public static int getDestUlp(long tos_) { return (int)((tos_ & DESTULP_MASK) >> DPORT_START); }
	public static int getType(long tos_) { return (int)(tos_ & TYPE_MASK); }
}
