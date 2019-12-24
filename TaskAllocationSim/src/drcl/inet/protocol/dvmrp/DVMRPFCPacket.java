// @(#)DVMRPFCPacket.java   12/2002
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

package drcl.inet.protocol.dvmrp;

/**
Defines the format of the packets used by {@link DVMRP} to exchange forwarding table
with neighbors.
 */
public class DVMRPFCPacket extends drcl.DrclObj
{ 
	static String[] CMDS = {"PRUNE", "GRAFT", "GRAFT-ACK"};

	// commands
	static final int PRUNE = 0;
	static final int GRAFT = 1;
	static final int GRAFT_ACK = 2;
	static final int PRUNE_SIZE = 24; // header size 24: DVMRPv3 internet draft
	static final int GRAFT_SIZE = 20; // header size 20: DVMRPv3 internet draft
	
	int cmd, version;
	long src, group, srcmask;
	int pruneLifetime;

	public DVMRPFCPacket()
	{ super(); }
	
	/**
	 * Creates a prune packet
	 * @param pruneLifetime_ in seconds.
	 */
	public DVMRPFCPacket (int version_, long src_, long group_,
											long srcmask_, int pruneLifetime_)
	{
		cmd = PRUNE;
		version = version_;
		src = src_;
		group = group_;
		srcmask = srcmask_;
		pruneLifetime = pruneLifetime_;
	}
	
	/** Creates a graft(-ack) packet. */
	public DVMRPFCPacket (int version_, long src_, long group_,
											long srcmask_, boolean ack_)
	{
		cmd = ack_? GRAFT_ACK: GRAFT;
		version = version_;
		src = src_;
		group = group_;
		srcmask = srcmask_;
	}

	private DVMRPFCPacket (int cmd_, int version_, long src_, long group_,
											long srcmask_, int pruneLifetime_)
	{
		cmd = cmd_;
		version = version_;
		src = src_;
		group = group_;
		srcmask = srcmask_;
		pruneLifetime = pruneLifetime_;
	}
	
	public int getCommand()
	{	return cmd; }
	
	public int getVersion()
	{	return version;	}
	
	public boolean isPrune()
	{ return cmd == PRUNE; }
	
	public boolean isGraft()
	{ return cmd == GRAFT; }
	
	public boolean isGraftAck()
	{ return cmd == GRAFT_ACK; }
	
	/** Retrieves the source field. */
	public long getSource()
	{	return src;	}
	
	/** Retrieves the group field. */
	public long getGroup()
	{	return group;	}
	
	/** Retrieves the source network mask field. */
	public long getSourceMask()
	{	return srcmask;	}
	
	/** Retrieves the prune lifetime field. */
	public int getPruneLifetime()
	{ return pruneLifetime; }
	
	public void setCommand(int value_)
	{ cmd = value_; }
	
	public void setVersion(int value_)
	{ version = value_; }
	
	public void setSource(long value_)
	{ src = value_; }
	
	public void setSourceMask(long value_)
	{ srcmask = value_; }
	
	public void setGroup(long value_)
	{ group = value_; }
	
	public void setPruneLifetime(int value_)
	{ pruneLifetime = value_; }
	
	public void duplicate(Object source_)
	{
		DVMRPFCPacket that_ = (DVMRPFCPacket)source_;
		cmd = that_.cmd;
		version = that_.version;
		src = that_.src;
		group = that_.group;
		srcmask = that_.srcmask;
		if (cmd == PRUNE) pruneLifetime = that_.pruneLifetime;
	}

	public Object clone()
	{ return new DVMRPFCPacket (cmd, version, src, group, srcmask, pruneLifetime); }

	public String toString()
	{
		return "DVMRPv" + version + "_" + CMDS[cmd] + ",src:" + src + "/" + srcmask
			+ ",group:" + group + (cmd == PRUNE? ",pruneLifetime:" + pruneLifetime: "");
	}
}
