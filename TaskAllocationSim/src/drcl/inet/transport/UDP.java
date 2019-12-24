// @(#)UDP.java   9/2002
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

package drcl.inet.transport;

import drcl.comp.*;
import drcl.inet.contract.DatagramContract;
import drcl.inet.InetPacket;

/**
 * This component implements the User Datagram Protocol (RFC768).
 * @see UDPPacket
 */
public class UDP extends drcl.inet.Protocol
{
	public static final int DEFAULT_PID = 17;
	static {
		setContract(UDP.class, "*@" + drcl.net.Module.PortGroup_UP,
			new DatagramContract(Contract.Role_PEER));
	}

	public String getName()
	{ return "udp"; }

	static final String UDP_DATAGRAM_ARRIVAL_PORT_ID = ".udpdatagramarrival";
	static final String UDP_DATAGRAM_ARRIVAL = "UDP Datagram Arrival"; // event name
	
	Port udpdatagramarrival = addEventPort(UDP_DATAGRAM_ARRIVAL_PORT_ID);
	{ removeDefaultUpPort(); removeTimerPort();}
	
	int TTL = 255;
	
	public UDP()
	{ super(); }
	
	public UDP(String id_)
	{ super(id_); }
	
	public String info()
	{ return "TTL=" + TTL + "\n"; }      
	
	
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		UDP that_ = (UDP)source_;
		setTTL(that_.TTL);
	}

    /** Sets the TTL value for the UDP sessions. */
	public void setTTL(int ttl)
	{ TTL = ttl; }
	
	public int getTTL()
	{ return TTL; }

	protected void dataArriveAtUpPort(Object data_,  drcl.comp.Port upPort_) 
	{
		if (!(data_ instanceof DatagramContract.Message)) {
			error(data_, "dataArriveAtUpPort()", upPort_, "unknown object");
			return;
		}
		
		DatagramContract.Message struct_ = (DatagramContract.Message)data_;
		UDPPacket pkt_ = new UDPPacket((Integer.valueOf(upPort_.getID())).intValue(),
			struct_.getDestinationPort(), 8, struct_.getSize(), struct_.getContent()); 
			
		if (udpdatagramarrival._isEventExportEnabled())
			udpdatagramarrival.exportEvent(UDP_DATAGRAM_ARRIVAL, pkt_, "from local");
	
		// For sending packets downward, we call the forward method. 
		// forward() is defined in Protocol.java  
		// void forward(PacketBody p_, long src_, long dest_, boolean routerAlert_,
		//   		int TTL, int ToS): route-lookup forwarding 
		forward(pkt_, struct_.getSource(), struct_.getDestination(), false, TTL, struct_.getTOS());
	}	
	
	protected void dataArriveAtDownPort(Object data_, drcl.comp.Port downPort_)
	{ 
		if (!(data_ instanceof InetPacket)) {
			error(data_, "dataArriveAtDownPort", downPort_, "unrecognized data");
			return;
		}
		InetPacket ip_ = (InetPacket)data_;
		UDPPacket udp_ = (UDPPacket)ip_.getBody();
		DatagramContract.Message msg_ = new DatagramContract.Message(udp_.getBody(),
			udp_.size-udp_.headerSize, ip_.getSource(), ip_.getDestination(), udp_.getSPort(),
			ip_.getTOS());

		if (udpdatagramarrival._isEventExportEnabled())
			udpdatagramarrival.exportEvent(UDP_DATAGRAM_ARRIVAL, udp_, null);	

		// Module Class: public boolean deliver(Object data_, String portID_) 
		if (!deliver(msg_, String.valueOf(udp_.getDPort())) && isGarbageEnabled())
			drop(msg_, "UDP port:" + udp_.getDPort() + " does not exist for delivery");
	}
}



