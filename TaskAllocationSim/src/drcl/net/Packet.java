// @(#)Packet.java   10/2003
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

package drcl.net;

/**
This class defines the base class for implementing packets.
A packet consists of a header and a body.
The header structure should be further defined in subclasses.
This class defines the interface a subclass should/would implement,
and implements the setter/getter methods for the encapsulated body object,
the packet size and the header size.
 */
public abstract class Packet //extends drcl.DrclObj
	implements PacketWrapper, drcl.data.Countable
{
	public Object	body;
	public int		size;
	public int		headerSize;

	/** The packet ID that is used and maintained by instrument components,
	    should be globally and uniquely recognized.  */
	public long     id;
	
	public Packet()
	{}
	
	public Packet (int packetSize_)
	{
		body = null;
		size = headerSize = packetSize_;
	}	
	
	public Packet (int headerSize_, int bodySize_, Object body_)
	{
		body = body_;
		headerSize = headerSize_;
		size = headerSize + bodySize_;
	}	
	
	/*
	public void duplicate(Object source_)
	{
		Packet p_ = (Packet) source_;
		size = p_.size;
		headerSize = p_.headerSize;
		body = p_.body instanceof Cloneable?
			((Cloneable)p_.body).clone(): p_.body;
	}
	*/

	public abstract Object clone();

	/** Returns the name of this packet. */
	public abstract String getName();
	
	/** Returns the encapsulated object of this packet. */
	public Object getBody()
	{ return body; }

	/** Sets the encapsulated object of this packet with another packet. */
	public void setBody(Packet b_)
	{
		size = headerSize + b_.size;
		body = b_; 
	}

	/** Wraps the argument packet as the body of this packet. */
	public void wraps(Packet p_)
	{ setBody(p_); }

	/** Sets the encapsulated object of this packet. */
	public void setBody(Object b_, int bodySize_) 
	{ 
		size = headerSize + bodySize_;
		body = b_; 
	}
	
	/** Returns the packet size of this packet. */
	public int getPacketSize()
	{ return size; }

	/** Returns the packet size of this packet. */
	public int getSize()
	{ return size; }

	/** Sets the packet size of this packet. */
	public void setPacketSize(int packetSize_) 
	{
		if (body instanceof Packet)
			((Packet)body).setPacketSize(packetSize_ - headerSize);
		size = packetSize_;
	}

	/** Sets the header size of this packet. */
	public void setHeaderSize(int hsize_)
	{
		size = (size - headerSize) + hsize_;
		headerSize = hsize_;
	}

	/** Returns the header size of this packet. */
	public int getHeaderSize()
	{ return headerSize; }

	/** Returns the type of packet.
	 By default, it delegates the call to the encapsulated packet, and
	 returns {@link #getName} otherwise.
	 */
	public String getPacketType()
	{
		if (body instanceof Packet && ((Packet)body).getPacketType() != null)
			return ((Packet)body).getPacketType();
		else
			return getName();
	}

	/**
	 * Returns the count of this packet in the connection to which this packet
	 * belongs.
	 * By default, it delegates the call to the encapsulated packet, and throws
	 * a {@link PacketException} if the encapsulated object is not a packet.
	 */
	public int getPacketCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getPacketCount();
		else
			throw new PacketException("No packet count info: " + this);
	}

	/** @see #getPacketCount() */
	public int getNumberCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getPacketCount();
		else
			throw new PacketException("No packet count info: " + this);
	}

	public boolean isPacketCountSupported()
	{
		if (body instanceof Packet)
			return ((Packet)body).isPacketCountSupported();
		else
			return false;
	}

	/**
	 * Returns the byte count of this packet in the connection to which this
	 * packet belongs.
	 * By default, it delegates the call to the encapsulated packet, and throws
	 * a {@link PacketException} if the encapsulated object is not a packet.
	 * This method approximates the byte count with the byte count obtained from
	 * the encapsulated packet plus the header size times the packet count.
	 */
	public long getByteCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getByteCount() + headerSize* getPacketCount();
		else
			throw new PacketException("No packet count info: " + this);
	}

	/** @see #getByteCount() */
	public long getSizeCount()
	{
		if (body instanceof Packet)
			return ((Packet)body).getByteCount() + headerSize* getPacketCount();
		else
			throw new PacketException("No packet count info: " + this);
	}

	public boolean isByteCountSupported()
	{
		if (body instanceof Packet)
			return ((Packet)body).isByteCountSupported();
		else
			return false;
	}

	public double getTimestamp()
	{
		if (body instanceof Packet)
			return ((Packet)body).getTimestamp();
		else
			throw new PacketException("Timestamp is not supported: " + this);
	}

	public void setTimestamp(double time_)
	{
		if (body instanceof Packet)
			((Packet)body).setTimestamp(time_);
		else
			throw new PacketException("Timestamp is not supported: " + this);
	}

	public boolean isTimestampSupported()
	{
		if (body instanceof Packet)
			return ((Packet)body).isTimestampSupported();
		else
			return false;
	}

	/** Sets the packet and header sizes of this packet. */
	public void setSize(int packetSize_, int headerSize_)
	{
		headerSize = headerSize_;
		size = packetSize_;
		if (body instanceof Packet)
			((Packet)body).setPacketSize(size - headerSize);
	}
	
	/** Prints the content of this packet; subclasses should override
	 * {@link #_toString(String)}. */
	public String toString() 
	{ return toString("--"); }

	// should go recursively if content is a packet?
	/** Prints the content of this packet; subclasses should override
	 * {@link #_toString(String)}. */
	public String toString(String separator_) 
	{ 
		if (body instanceof Packet)
			return "sz" + size + "(" + getName() + ")sz" + headerSize
				+ separator_ + _toString(separator_) + "__<"
				+ ((Packet)body).toString(separator_) + ">__";
		else if (body != null)
			return "sz" + size + "(" + getName() + ")sz" + headerSize
				+ separator_ + _toString(separator_) + "__<"
				+ drcl.util.StringUtil.toString(body) + ">__";
		else
			return "sz" + size + "(" + getName() + ")sz" + headerSize
				+ separator_ + _toString(separator_) + "__<EMPTY_BODY>__";
	}

	/** Prints the packet header. */
	public String _toString(String separator_) 
	{ return ""; }
	
	/** Returns true if <code>that_</code> is a packet, and its packet size,
	 * header size and the encapsulated object are all equal to those of this
	 * packet. */
	public boolean equals(Object that_)
	{
		if (this == that_) return true;
		if (!(that_ instanceof Packet)) return false;
		Packet b_ = (Packet)that_;
		return size == b_.size && headerSize == b_.headerSize &&
			   (body == b_.body || body != null && body.equals(b_.body));
	}
}
