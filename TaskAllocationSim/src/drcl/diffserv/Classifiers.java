// @(#)Classifiers.java   12/2003
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
import drcl.net.PktClassifier;
import drcl.inet.InetPacket;
import drcl.diffserv.DFUtil;
import drcl.diffserv.DFConstants;


/** 
 * Collection of packet classifiers to be used in queue classes.
 * Examples of queue classes that use {@link PktClassifier packet classifier}
 * are {@link drcl.diffserv.scheduling.ColorQueue} and
 * {@link drcl.inet.core.queue.MQueue}. 
 */
public class Classifiers implements DFConstants
{ 
	/* The classifier returns 0 for AF11, 1 for AF12/AF13. */
	public static PktClassifier getAF1xClassifier2()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return DFUtil.getDSCP((InetPacket)pkt_) == AF11? 0: 1; }
		};
	}

	/* The classifier returns 0 for AF21, 1 for AF22/AF23. */
	public static PktClassifier getAF2xClassifier2()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return DFUtil.getDSCP((InetPacket)pkt_) == AF21? 0: 1; }
		};
	}

	/* The classifier returns 0 for AF31, 1 for AF32/AF33. */
	public static PktClassifier getAF3xClassifier2()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return DFUtil.getDSCP((InetPacket)pkt_) == AF31? 0: 1; }
		};
	}

	/* The classifier returns 0 for AF11, 1 for AF12 and 2 for AF13. */
	public static PktClassifier getAF1xClassifier3()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return (DFUtil.getDSCP((InetPacket)pkt_)-AF11)/2; }
		};
	}

	/* The classifier returns 0 for AF21, 1 for AF22 and 2 for AF23. */
	public static PktClassifier getAF2xClassifier3()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return (DFUtil.getDSCP((InetPacket)pkt_)-AF21)/2; }
		};
	}

	/* The classifier returns 0 for AF31, 1 for AF32 and 2 for AF33. */
	public static PktClassifier getAF3xClassifier3()
	{
		return new PktClassifier() {
			public int classify(Packet pkt_)
			{ return (DFUtil.getDSCP((InetPacket)pkt_)-AF31)/2; }
		};
	}
}
