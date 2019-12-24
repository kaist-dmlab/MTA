// @(#)sIGMP.java   12/2002
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

package drcl.inet.core;

import java.util.Vector;
import drcl.comp.*;
import drcl.inet.InetPacket;
import drcl.inet.contract.IDLookup;
import drcl.inet.contract.IDConfig;
import drcl.inet.contract.McastHostEvent;
import drcl.util.scalar.LongVector;

/**
A very preliminary IGMP protocol.

This component only implements REPORT and LEAVE.  REPORT retransmission and QUERY
are not implemented.  And this component can be used in both a host and a router.

<p>Contracts:
<ul>
<li> <code>.service_id@</code>: {@link drcl.inet.contract.IDLookup} and {@link drcl.inet.contract.IDConfig} as client.
<li> <code>.mcast_service@</code> (for host): {@link drcl.inet.contract.IDConfig} as reactor.
<li> <code>.mcastHost@</code> (for router): {@link drcl.inet.contract.McastHostEvent} as initiator.
<li> <code>down@</code>: {@link drcl.inet.contract.PktSending} and
	{@link drcl.inet.contract.PktDelivery} as client.
</ul>

<p>Reference:
<ul>
<li> <a href="http://www.google.com/search?hl=en&safe=off&q=rfc2236">RFC2236</a>.
</ul>

@author Hung-ying Tyan
@version 1.0, 7/9/2001
 */
public class sIGMP extends drcl.inet.Protocol
{	
	/** Membership Query message type (RFC2236). */
	public static final int QUERY = 0x11;
	/** Version 2 Membership Report message type (RFC2236). */
	public static final int REPORT = 0x16;
	/** Leave Group message type (RFC2236). */
	public static final int LEAVE = 0x17;
	/** Version 1 Membership Report message type (RFC2236). */
	public static final int REPORT1 = 0x12;
	/** IGMP message size (RFC2236). */
	public static final int IGMP_MESSAGE_SIZE = 8;

	public String getName()
	{ return "igmp"; }
	
	static {
		setContract(sIGMP.class, EVENT_MCAST_HOST_PORT_ID + "@", new McastHostEvent(Contract.Role_INITIATOR));
		setContract(sIGMP.class, SERVICE_MCAST_PORT_ID + "@", new IDConfig(Contract.Role_REACTOR));
	}

	{ // remove upPort and timerPort
		upPort.setRemovable(true); removePort(upPort); upPort = null;
		timerPort.setRemovable(true); removePort(timerPort); timerPort = null;
	}

	Port idport = createIDServicePort(); // for ID configuration service
	Port mcastServicePort = addServerPort(SERVICE_MCAST_PORT_ID);
	Port mcastHostPort = addEventPort(EVENT_MCAST_HOST_PORT_ID);

	// a quick solution: for interface i, groups[i] stores groups that have
	// members on the interface; the number of duplicate copies of a group
	// in groups[i] is equal to the number of members of that group
	// on interface i.  this is not scalable if a large number of members are
	// in a subnet.
	// used by router
	LongVector[] groups;
	// groups that this node joins, see groups
	// used by end host
	LongVector localGroups;

	public sIGMP()
	{ super(); }

	public sIGMP(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		// clone each LongVector element as well
		groups = (LongVector[])drcl.util.ObjectUtil.clone(((sIGMP)source_).groups);
	}
	
	/**
	 * Provide multicast group membership information on each interface.
	 */
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		if (groups != null)
			for (int i=0; i<groups.length; i++)
				if (groups[i] != null)
					sb_.append("IF" + i + ": " + groups[i] + "\n");
		String sb2_ = localGroups == null || localGroups.size() == 0? "": "Groups (local): " + localGroups + "\n";
		if (sb_.length() == 0 && sb2_.length() == 0)
			return "None.\n";
		else
			return (sb_.length() == 0? "": "Groups (subnets):\n" + sb_.toString()) + sb2_;
	}
	
	public void reset()
	{
		super.reset();
		if (groups != null)
			for (int i=0; i<groups.length; i++)
				if (groups[i] != null)
					groups[i].removeAll();
	}
	
	protected void process(Object data_, drcl.comp.Port inPort_)
	{
		lock(this);
		super.process(data_, inPort_);
		unlock(this);
	}

	// from .mcast_service
	protected void processOther(Object data_, drcl.comp.Port inPort_)
	{ 
		if(inPort_ != mcastServicePort) {
			super.processOther(data_,inPort_);
			return;
		}
		if (data_ instanceof IDConfig.Message) {
			// end host: from mcast application
			IDConfig.Message msg_ = (IDConfig.Message)data_;
			long[] groups_ = msg_.getIDs();
			if (groups_ == null) {
				inPort_.doLastSending(null);
				return;
			}
			if (msg_.getType() == IDConfig.QUERY) {
				// relay to idport and relay the result back to inPort_
				inPort_.doLastSending(idport.sendReceive(data_));
				return;
			}
			if (msg_.getType() == IDConfig.ADD)
				for (int i=0; i<groups_.length; i++)
					join(groups_[i]);
			else
				for (int i=0; i<groups_.length; i++)
					leave(groups_[i]);
			inPort_.doLastSending(null);
		}
		else {
			// router:
			// only expect REMOVE msg, which indicates something
			// wrong with the multicast group, the router is forced
			// to leave the group; notify end hosts
			McastHostEvent.Message s = (McastHostEvent.Message)data_;
			long group_ = s.getGroup();
			int ifindex_ = s.getIfIndex();
			_sendResult(s.isJoin(), group_, ifindex_);
		}
	}

	/** Makes this host joins the multicast group.*/
	public void join(long group_)
	{
		if (localGroups == null) localGroups = new LongVector();
		if (localGroups.indexOf(group_) < 0) {
			// if first request, send a report to the router
			IDConfig.add(group_, Double.NaN, idport);
			forward(new IGMPPacket(REPORT, 0, group_), IGMP_MESSAGE_SIZE,
				drcl.net.Address.NULL_ADDR, group_, true/*router alert*/,
				1/*TTL*/, InetPacket.CONTROL);
		}
		localGroups.addValue(group_);
	}

	/** Makes this host leaves the multicast group.*/
	public void leave(long group_)
	{
		if (localGroups == null || localGroups.indexOf(group_) < 0) 
			return;
		localGroups.removeValue(group_);
		if (localGroups.indexOf(group_) < 0) {
			// if all leaves, send a leave msg to the router
			IDConfig.remove(group_, idport);
			forward(new IGMPPacket(LEAVE, 0, group_), IGMP_MESSAGE_SIZE,
				drcl.net.Address.NULL_ADDR, group_, true/*router alert*/,
				1/*TTL*/, InetPacket.CONTROL);
		}
	}
	
	// take care of bookkeeping
	void _leave(long group_)
	{
		while (localGroups.indexOf(group_) >= 0)
			localGroups.removeValue(group_);
		IDConfig.remove(group_, idport);
	}

	// send result back to end host
	void _sendResult(boolean succeeded_, long group_, int if_)
	{
		if (!succeeded_) {
			for (int i=0; i<groups.length; i++) {
				if (groups[i] == null) continue;
				LongVector v = groups[i];
				while (groups[i].indexOf(group_) >= 0)
					groups[i].removeValue(group_);
			}
		}
		forward(new IGMPPacket(succeeded_? REPORT: LEAVE, 0, group_),
						IGMP_MESSAGE_SIZE, drcl.net.Address.NULL_ADDR,
						group_, true/*router alert*/, 1/*TTL*/,
						InetPacket.CONTROL, if_);
	}

	protected void dataArriveAtDownPort(Object msg_, Port downPort_) 
	{
		InetPacket p_ = (InetPacket)msg_;
		IGMPPacket igmp_ = (IGMPPacket)p_.getBody();
		
		if (isDebugEnabled())
			debug("RECEIVED: " + msg_);
		
		if (!_isHost() && !mcastHostPort.anyPeer()) return; // ignore the msg

		int incomingIf_ = p_.getIncomingIf();
		long group_ = igmp_.getGroup();
		
		switch (igmp_.getType()) {
			case QUERY:
				return; // not supported
			case REPORT:
				if (_isHost()) {
					// membership join finished from router
					// broadcast to all mcast applications
					mcastServicePort.doSending(
										McastHostEvent.createJoinEvent(
												group_, incomingIf_));
					break;
				}
				if (groups == null)
					groups = new LongVector[incomingIf_ + 1];
				if (groups.length <= incomingIf_) {
					LongVector[] tmp_ = new LongVector[incomingIf_ + 1];
					System.arraycopy(groups, 0, tmp_, 0, groups.length);
					groups = tmp_;
				}
				if (groups[incomingIf_] == null)
					groups[incomingIf_] = new LongVector();
				if (groups[incomingIf_].indexOf(group_) < 0)
					mcastHostPort.doLastSending(McastHostEvent.createJoinEvent(group_, incomingIf_));
				groups[incomingIf_].addValue(group_);
				break;
			case LEAVE:
				if (_isHost()) {
					// membership error from router
					// broadcast to all mcast applications
					mcastServicePort.doSending(
										McastHostEvent.createLeaveEvent(
												group_, incomingIf_));
					_leave(group_);
				}
				else {
					// router: send McastHost event
					if (groups == null || groups.length <= incomingIf_
						|| groups[incomingIf_] == null
						|| groups[incomingIf_].indexOf(group_) < 0)
						break;
					groups[incomingIf_].removeValue(group_);
					if (groups[incomingIf_].indexOf(group_) < 0)
						mcastHostPort.doLastSending(
										McastHostEvent.createLeaveEvent(
												group_, incomingIf_));
				}
			case REPORT1:
				return; // not supported
			default:
				return; // discarded silently
		}
	}

	// returns true if this node is a router
	boolean _isRouter()
	{ return groups != null; }

	// returns true if this node is an end host
	boolean _isHost()
	{ return localGroups != null; }

	public static class IGMPPacket extends drcl.DrclObj
	{
		int type;
		int maxResponseTime;
		long group;

		public IGMPPacket(int type_, int maxResponseTime_, long group_)
		{
			type = type_;
			maxResponseTime = maxResponseTime_;
			group = group_;
		}

		public void duplicate(Object source_)
		{
			type = ((IGMPPacket)source_).type;
			maxResponseTime = ((IGMPPacket)source_).maxResponseTime;
			group = ((IGMPPacket)source_).group;
		}

		public Object clone()
		{ return new IGMPPacket(type, maxResponseTime, group); }

		public int getType()
		{ return type; }

		public int getMaxResponseTime()
		{ return maxResponseTime; }

		public long getGroup()
		{ return group; }

		public String toString()
		{
			switch (type) {
			case QUERY:
				return "IGMP_QUERY:" + group + ",max_resp_time=" + maxResponseTime;
			case REPORT:
				return "IGMP_REPORT2:" + group;
			case LEAVE:
				return "IGMP_LEAVE:" + group;
			case REPORT1:
				return "IGMP_REPORT1:" + group;
			default:
				return "IGMP_unknown:" + type + ",max_resp_time=" + maxResponseTime
					+ ",group=" + group;
			}
		}
	}
}
