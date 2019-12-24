// @(#)NamTrace.java   9/2002
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

package drcl.net.tool;

import drcl.comp.Port;
import drcl.comp.contract.ComponentMessage;
import drcl.comp.contract.GarbageContract;
import drcl.net.Packet;

/**
The base class for generating NAM traces for incoming packets.
The NAM traces are output at the "output@" port.  One may connect it to a file
component to actually write the traces to a file.
Specifically, it recognizes the following five NAM packet events by the ID of 
the port at which packets come: <em>hop</em>, <em>receive</em>, <em>drop</em>, 
<em>enque</em> and <em>deque</em>.
(For <em>drop</em> events, this component expects to receive
{@link drcl.comp.contract.GarbageContract.Message}.)

<p>Below are the ways it obtains pieces of information that constitutes a NAM 
packet event:
<ul>
<li> source and destination: also in the ID of the port at which packets arrive.
<li> time: from {@link #getTime()}.
<li> type: from {@link #getPacketType(Packet)}, by default,
	it uses {@link Packet#getPacketType()}.
<li> extent: packet size from {@link Packet#getPacketSize()}.
<li> conversation id: from {@link #getConversationID(Packet)},
	a subclass should override this method
	to provide this information, by default, this method returns null and this 
	component does not output this field.
<li> id: uses {@link Packet#id} (the field is maintained by NamTrace).
<li> attribute: from {@link #getColorID(Packet)},
	a subclass should override this method to provide this information, 
	by default, it always returns 0.
</ul>

<p>In addition, this class provides a set of methods to facilitate outputting 
node, link, and queue events as well as configuring colors.
 */
public class NamTrace extends drcl.comp.Extension
{
	Port out = addPort("output");
	int hop, receive, drop, enque, deque;
	boolean accounting = true;
	boolean componentMessageFeedback = false;
	Object lastComponentMessage = null; // to avoid loop back
	long idcount = 0;

	public NamTrace()
	{	this(null);	}
	
	public NamTrace(String id_)
	{	super(id_);	}
	
	public void reset()
	{
		super.reset();
		hop = receive = enque = deque = drop = 0;
		idcount = 0;
	}

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		NamTrace that_ = (NamTrace)source_;
	}

	public String info()
	{
		if (accounting)
			return "   # enque events = " + enque + "\n"
			     + "   # deque events = " + deque + "\n"
			     + "     # hop events = " + hop + "\n"
    			 + " # receive events = " + receive + "\n"
    			 + "    # drop events = " + drop + "\n"
    			 + "packet id counter = " + idcount + "\n";
		else
			return "Accounting is disabled.";
	}

/*
According to the NAM documentation, here is the format of a Packet Event line: 

<type> -t <time> -e <extent> -s <src_addr> -d <dst_addr> 
       -c <conv> -i <id>     -a <attr>     -p <pkt_type>

<type> is one of:
h - Hop, packet started transmission from src_addt to dst_addr
r - Receive, packet finished transmission and started to be received at the
	destination 
d - Drop, packets was dropped from the queue or link from src_addr to dst_addr 
+ - Enqueue, packet entered the queue from src_addr to dst_addr 
- - Dequeue, packet left the queue from src_addr to dst_addr 

-t <time> is the time at which the event occured 
-e <extent> is the packet size in bytes 
-s <src_addr> is the originating node 
-d <dst_addr> is the destination node 
-a <attr> sets the packet attribute (currently used as colour id) 
-c <conv> is the conversion ID (???) 
-i <id> is the ID of the packet in the transmission 
-p <pkt_type> is the type of the packet, e.g. TCP, ACK, NACK, SRM 
*/
// src and dest are end node IDs of a link.

	protected synchronized void process(Object data_, drcl.comp.Port inPort_) 
	{
		Packet p_;
		if (data_ instanceof Packet)
			p_ = (Packet) data_;
		else if (data_ instanceof GarbageContract.Message) {
			if (componentMessageFeedback && data_ != lastComponentMessage) {
				lastComponentMessage = data_;
				infoPort.doSending(data_);
					// deliver the intercepted component messages
			}
			p_ = (Packet)((GarbageContract.Message)data_).getData();
		}
		else {
			if (componentMessageFeedback && data_ != lastComponentMessage
				&& data_ instanceof ComponentMessage) {
				lastComponentMessage = data_;
				infoPort.doSending(data_);
					// deliver the intercepted component messages
			}
			return;
		}

		if (accounting) {
			switch (inPort_.id.charAt(0)) {
			case 'r':
				receive++; break;
			case 'd':
				drop++; break;
			case '+':
				enque++; break;
			case '-':
				deque++; break;
			case 'h':
				hop++; break;
			default:
				error("process()", "unrecognized event '"
								+ inPort_.id.charAt(0) + "'");
				return;
			}
		}
		if (p_.id == 0) p_.id = ++idcount;
		String conversationID_ = getConversationID(p_);
		out.doSending(inPort_.id 
				  + " -t " + getTime()
				  + " -p " + getPacketType(p_)
				  + " -e " + p_.size
				  + (conversationID_ == null? "": " -c " + conversationID_)
				  + " -i " + p_.id
				  + " -a " + getColorID(p_) + "\n");
	}

	public String getConversationID(Packet p_)
	{ return null; }

	public int getColorID(Packet p_)
	{ return 0; }

	public String getPacketType(Packet p_)
	{ return p_.getPacketType(); }

	/** Sets true to enable accounting of each type of event. */
	public void setAccountingEnabled(boolean enabled_)
	{ accounting = enabled_; }

	/** Returns true if accounting of each type of event is enabled. */
	public boolean isAccountingEnabled()
	{ return accounting; }

	/** Sets true to enable component message feedback through this 
	 * component's infoport. */
	public void setComponentMessageFeedbackEnabled(boolean enabled_)
	{ componentMessageFeedback = enabled_; }

	/** Returns true if component message feedback through this component's 
	 * infoport is enabled. */
	public boolean isComponentMessageFeedbackEnabled()
	{ return componentMessageFeedback; }

	
	/** Adds a link event (complete form). */
	public void addLink(double time_, long source_, long dest_, String state_,
		String color_, String bandwidth_, String propagationDelay_,
		String orientation_)
	{
		out.doSending("l -t " + time_ + " -s " + source_ + " -d " + dest_
			+ " -S " + state_ + " -c " + color_ + " -r " + bandwidth_ + " -D "
			+ propagationDelay_ + " -o " + orientation_ + "\n");
	}

	/** Adds a (initial) link event. */
	public void addLink(long source_, long dest_, String state_,
		String bandwidth_, String propagationDelay_, String orientation_)
	{
		out.doSending("l -t * -s " + source_ + " -d " + dest_ + " -S " + state_
			+ " -r " + bandwidth_ + " -D " + propagationDelay_
			+ (orientation_ == null? "": " -o " + orientation_)
			+ "\n");
	}

	/** Adds a link (state changed) event. */
	public void addLink(double time_, long source_, long dest_, String state_,
					String color_)
	{
		out.doSending("l -t " + time_ + " -s " + source_ + " -d " + dest_
			+ " -S " + state_ + " -c " + color_ + "\n");
	}

	/** Adds a node event (complete form). */
	public void addNode(double time_, long source_, long dest_, String state_,
				String shape_, String color_, String prevColor_, String label_)
	{
		// NAM does not accept -A flag now...
		out.doSending("n -t " + time_ + " -s " + source_ + " -d " + dest_
			+ " -S " + state_
			//+ " -v " + shape_ + " -c " + color_ + " -o " + prevColor_ + " -A " + label_ + "\n");
			+ " -v " + shape_ + " -c " + color_ + " -o " + prevColor_ + "\n");
	}

	/** Adds a (initial) node event. */
	public void addNode(long source_, String state_, String shape_,
					String color_, String label_)
	{
		// NAM does not accept -A flag now...
		out.doSending("n -t * -s " + source_ + " -S " + state_
			//+ " -v " + shape_ + " -c " + color_ + " -A " + label_ + "\n");
			+ " -v " + shape_ + " -c " + color_ + "\n");
	}

	/** Adds a node (state changed) event. */
	public void addNode(double time_, long source_, String state_,
					String color_)
	{
		out.doSending("n -t " + time_ + " -s " + source_ + " -S " + state_ 
						+ " -c " + color_ + "\n");
	}

	/** Adds a node (state changed) event. */
	public void addNode(double time_, long source_, String state_,
			String shape_, String color_, String prevColor_, String label_)
	{
		// NAM does not accept -A flag now...
		out.doSending("n -t " + time_ + " -s " + source_ + " -S " + state_
			//+ " -v " + shape_ + " -c " + color_ + " -o " + prevColor_ + " -A " + label_ + "\n");
			+ " -v " + shape_ + " -c " + color_ + " -o " + prevColor_ + "\n");
	}

	/** Adds a queue event. */
	public void addQueue(double time_, long source_, long dest_, 
					String attribute_)
	{
		out.doSending("n -t " + time_ + " -s " + source_ + " -d " + dest_
						+ " -a " + attribute_ + "\n");
	}

	/** Adds a queue event. */
	public void addQueue(long source_, long dest_, String attribute_)
	{
		out.doSending("q -t * -s " + source_ + " -d " + dest_ + " -a " 
						+ attribute_ + "\n");
	}

	/** Adds a color configuration. */
	public void addColor(double time_, int colorid_, String colorName_)
	{
		out.doSending("c -t " + time_ + " -i " + colorid_ + " -n " 
						+ colorName_ + "\n");
	}

	/** Adds a color configuration. */
	public void addColor(int colorid_, String colorName_)
	{
		out.doSending("c -t * -i " + colorid_ + " -n " + colorName_ + "\n");
	}

	/** Adds a set of colors . */
	public void addColors(String[] colorNames_)
	{
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<colorNames_.length; i++)
			sb_.append("c -t * -i " + i + " -n " + colorNames_[i] + "\n");
		out.doSending(sb_.toString());
	}

	/** Adds a set of preconfigured colors . */
	public void addColors()
	{
		out.doSending(
			"c -t * -i 0 -n red\n"
			+ "c -t * -i 1 -n blue\n"
			+ "c -t * -i 2 -n yellow\n"
			+ "c -t * -i 3 -n green\n"
			+ "c -t * -i 4 -n black\n"
			+ "c -t * -i 5 -n orange\n"
		);
	}
}
