// @(#)TCP_full.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.socket;

import java.util.Vector;
import java.util.HashMap;
import drcl.comp.*;
import drcl.inet.InetPacket;
import drcl.inet.transport.TCPPacket;
import drcl.net.Address;
import drcl.util.scalar.IntSpace;
import drcl.util.queue.FiniteFIFOQueue;

// XXX: current implementation does not reclaim resource when nonblocking 
//      connect or accept fails.
/** This is a complete implementation of TCP.  In addition to what is done in 
 * {@link TCP_socket}, it handles multiplexing and demultiplexing, and manages
 * TCP sessions.
 */
public class TCP_full extends drcl.inet.Protocol
	implements drcl.inet.transport.TCPConstants
{ 
	/** The smallest valid connection ID. */
	public static final int MIN_CONNECTION_ID = 10;

	/** Default request buffer. */
	public int DEFAULT_REQUEST_BUFFER = 5;

	//{ upPort.setType(Port.PortType_SERVER); }
	// Note: server port does not work with nonblocking socket calls
	// need to allocate one control port (in UP group) for each application
	// 'upPort' is created by default

	public String getName()
	{ return "tcp"; }

	HashMap mapTCP;
		// 4-tuple (localaddr, srcport, remoteaddr, remoteport)
		// --> TCP_socket, for demux
	HashMap mapHalfTCP;
		// 2-tuple (localaddr, localport) --> TCP_socket, for demux
	Vector vectorTCP;
		// (connectionID - MIN_CONNECTION_ID) -->
		// 		TCP_socket, for signaling from application
	IntSpace portNumSpace;
		// for assigning port #: "accept" ports occupy negative space
	
	public TCP_full()
	{ super(); }

	public TCP_full(String id)
	{ super(id); }	

	public synchronized void reset()
	{
		if (mapTCP != null) {
			Object[] oo_ = mapTCP.values().toArray();
			for (int i=0; i<oo_.length; i++) {
				if (oo_[i] == null) continue;
				TCP_socket tcp_ = (TCP_socket)oo_[i];
				tcp_.removeAll();
			}
			mapTCP.clear();
		}
		if (mapHalfTCP != null) {
			Object[] oo_ = mapHalfTCP.values().toArray();
			for (int i=0; i<oo_.length; i++) {
				if (oo_[i] == null) continue;
				if (oo_[i] instanceof TCP_socket) {
					TCP_socket tcp_ = (TCP_socket)oo_[i];
					tcp_.removeAll();
				}
				else
					((FiniteFIFOQueue)oo_[i]).reset();
			}
			mapHalfTCP.clear();
		}
		if (vectorTCP != null) {
			vectorTCP.removeAllElements();
			vectorTCP = null;
		}
		if (portNumSpace != null)
			portNumSpace.reset(Integer.MIN_VALUE+1, Integer.MAX_VALUE-1);

		disconnectAllPorts(PortGroup_UP);
		removeAllPorts(PortGroup_UP);
		removeAllComponents();

		//upPort.disconnect();
		super.reset();
	}

	public synchronized String info()
	{
		StringBuffer sb_ = new StringBuffer("Full Connections: ");
		if (mapTCP == null || mapTCP.size() == 0)
			sb_.append("none\n");
		else {
			sb_.append("\n");
			Object[] oo_ = mapTCP.values().toArray();
			for (int i=0; i<oo_.length; i++) {
				if (oo_[i] == null) continue;
				TCP_socket tcp_ = (TCP_socket)oo_[i];
				sb_.append("   " + tcp_ + "/" + tcp_.localAddr + ":"
					+ tcp_.localPort + " <--> "
					+ tcp_.getPeer() + ":" + tcp_.remotePort + "\t\t"
					+ STATES[tcp_.getState()] + "\n");
			}
		}
		sb_.append("Half Connections: ");
		if (mapHalfTCP == null || mapHalfTCP.size() == 0)
			sb_.append("none\n");
		else {
			sb_.append("\n");
			HalfTCPKey[] oo_ = new HalfTCPKey[mapHalfTCP.size()];
			mapHalfTCP.keySet().toArray(oo_);
			for (int i=0; i<oo_.length; i++) {
				Object v = mapHalfTCP.get(oo_[i]);
				if (v == null) continue;
				if (v instanceof TCP_socket) {
					TCP_socket tcp_ = (TCP_socket)v;
					sb_.append("   " + tcp_ + "/" + tcp_.localAddr + ":"
						+ tcp_.localPort + "\t\t"
						+ STATES[tcp_.getState()] + "\n");
				}
				else 
					sb_.append("   " + oo_[i].localAddr + ":"
						+ oo_[i].localPort + "--"
						+ ((FiniteFIFOQueue)v).oneline() + "\n");
			}
		}
		sb_.append("Available connection IDs: ");
		if (vectorTCP == null)
			sb_.append("(" + MIN_CONNECTION_ID + ", MAX)\n");
		else {
			int prev_ = 0;
			boolean in_ = false;
			for (int i=0; i< vectorTCP.size(); i++) {
				Object tcp_ = vectorTCP.elementAt(i);
				if (tcp_ != null) {
					if (in_) continue;
					if (i > prev_)
						sb_.append("(" + (MIN_CONNECTION_ID + prev_) + ", "
									+ (MIN_CONNECTION_ID + i-1) + ")");
					prev_ = i;
					in_ = true; // start of an "in" period
				}
				else {
					if (!in_) continue;
					// start of an "out" period
					prev_ = i;
					in_ = false;
				}
			}
			if (in_)
				sb_.append("(" + (vectorTCP.size() + MIN_CONNECTION_ID)
								+ ", MAX)\n");
			else
				sb_.append("(" + (prev_ + MIN_CONNECTION_ID) + ", MAX)\n");
		}

		if (portNumSpace == null)
			sb_.append("Available port numbers: (-MAX, MAX)\n");
		else
			sb_.append("Available port numbers: " + portNumSpace + "\n");
		return sb_.toString();
	}

	int allocateConnectionID(TCP_socket tcp_)
	{
		if (vectorTCP == null)
			vectorTCP = new Vector();
		int id_ = 0;
		for (; id_<vectorTCP.size(); id_++) {
			if (vectorTCP.elementAt(id_) == null)
				break;
		}
		if (id_ == vectorTCP.size())
			vectorTCP.addElement(tcp_);
		else
			vectorTCP.setElementAt(tcp_, id_);
		return id_ + MIN_CONNECTION_ID;
	}

	// create and set up a TCP_socket, 
	// return the TCP_socket instance 
	TCP_socket allocateTCP(int localport_, boolean accept_)
	{
		if (portNumSpace == null)
			portNumSpace = new IntSpace(Integer.MIN_VALUE+1,
							Integer.MAX_VALUE-1);
		if (localport_ <= 0) // automatic assigning a port #
			localport_ = portNumSpace.checkoutGreater(10);
		else {
			if (accept_) {
				if (!portNumSpace.contains(-localport_))
					return null;
				portNumSpace.checkout(-localport_);
			}
			else
				portNumSpace.checkout(localport_);
		}

		TCP_socket tcp_ = new TCP_socket();
		tcp_.connectionID = allocateConnectionID(tcp_);

		tcp_.setID("tcp" + tcp_.connectionID);
		addComponent(tcp_);
		tcp_.setDebugEnabled(isDebugEnabled());
		tcp_.setTraceEnabled(isTraceEnabled());
		Port dataPort_ =
			(Port)addPort(PortGroup_UP, String.valueOf(tcp_.connectionID));
		tcp_.upPort.connect(dataPort_);
		// cannot make this.downPort a shadow because we need it to demux
		tcp_.setDownPort(this.downPort);
		tcp_.localPort = localport_;
		return tcp_;
	}

	// e.g., close
	TCP_socket getTCP(int connectionID_)
	{
		if (vectorTCP == null || connectionID_ < MIN_CONNECTION_ID)
			return null;
		return (TCP_socket)vectorTCP.elementAt(connectionID_
						- MIN_CONNECTION_ID);
	}

	void removeTCP(TCP_socket tcp_, boolean halfOpen_)
	{
		if (halfOpen_)
			mapHalfTCP.remove(new HalfTCPKey(tcp_.localAddr, tcp_.localPort));
		else if (mapTCP != null)
			mapTCP.remove(new FullTCPKey(tcp_.localAddr,
						tcp_.localPort, tcp_.getPeer(),
						tcp_.getRemotePort()));
	}

	FiniteFIFOQueue getRequestBuffer(long localAddr_, int localPort_)
	{
		if (mapHalfTCP == null)
			return null;
		FiniteFIFOQueue q = (FiniteFIFOQueue)mapHalfTCP.get(
						new HalfTCPKey(localAddr_, localPort_, true));
		if (q == null)
			q = (FiniteFIFOQueue)mapHalfTCP.get(
						new HalfTCPKey(Address.NULL_ADDR, localPort_, true));
		return q;
	}

	// for demux to a half-open tcp or closing an "accepting" socket
	TCP_socket getTCP(long localAddr_, int localPort_)
	{
		if (mapHalfTCP == null)
			return null;
		TCP_socket tcp_ = (TCP_socket)mapHalfTCP.get(
						new HalfTCPKey(localAddr_, localPort_));
		if (tcp_ == null)
			tcp_ = (TCP_socket)mapHalfTCP.get(
						new HalfTCPKey(Address.NULL_ADDR, localPort_));
		return tcp_;
	}

	// demux
	TCP_socket getTCP(long localAddr_, int localPort_, long remoteAddr_,
					int remotePort_)
	{
		if (mapTCP == null)
			return null;
		TCP_socket tcp_ = (TCP_socket) mapTCP.get(new FullTCPKey(localAddr_,
						localPort_, remoteAddr_, remotePort_));

		return tcp_;
	}

	// e.g., in demux, getTCP(long, int, long, int)
	// key (3 long's): localAddr, remoteAddr, (remotePort | localPort)
	drcl.data.BitSet _createBitSet(long localAddr_, int localPort_,
					long remoteAddr_, int remotePort_)
	{
		long[] value_ = new long[]{localAddr_, remoteAddr_,
			((long)localPort_ & 0x0FFFFFFFFL) | ((long)remotePort_ << 32)};
		return new drcl.data.BitSet(192, value_);
	}

	// key to find TCP_socket entity in mapTCP
	class FullTCPKey
	{
		long localAddr, remoteAddr;
		int localPort, remotePort;

		FullTCPKey(long localAddr_, int localPort_, long remoteAddr_, 
						int remotePort_)
		{
			localAddr = localAddr_;
			localPort = localPort_;
			remoteAddr = remoteAddr_;
			remotePort = remotePort_;
		}

		public int hashCode()
		{ return (int)remoteAddr; }

		public boolean equals(Object that_)
		{
			return ((FullTCPKey)that_).localAddr == localAddr
					&& ((FullTCPKey)that_).remoteAddr == remoteAddr
					&& ((FullTCPKey)that_).localPort == localPort
					&& ((FullTCPKey)that_).remotePort == remotePort;
		}
	}

	// key to find TCP_socket entity in mapHalfTCP
	class HalfTCPKey
	{
		long localAddr;
		int localPort;
		boolean forBuffer = false;

		HalfTCPKey(long localAddr_, int localPort_)
		{
			localAddr = localAddr_;
			localPort = localPort_;
		}

		HalfTCPKey(long localAddr_, int localPort_, boolean forBuffer_)
		{
			localAddr = localAddr_;
			localPort = localPort_;
			forBuffer = forBuffer_;
		}

		HalfTCPKey(int localPort_)
		{
			localAddr = Address.NULL_ADDR;
			localPort = localPort_;
		}

		HalfTCPKey(int localPort_, boolean forBuffer_)
		{
			localAddr = Address.NULL_ADDR;
			localPort = localPort_;
			forBuffer = forBuffer_;
		}

		public int hashCode()
		{ return localPort; }

		public boolean equals(Object that_)
		{
			return ((HalfTCPKey)that_).localAddr == localAddr
					&& ((HalfTCPKey)that_).localPort == localPort
					&& ((HalfTCPKey)that_).forBuffer == forBuffer;
		}
	}

	void setRequestBuffer(long localAddr_, int localPort_, int bufferSize_)
	{
		if (mapHalfTCP == null)
			mapHalfTCP = new HashMap();
		HalfTCPKey k = new HalfTCPKey(localAddr_, localPort_, true);

		FiniteFIFOQueue q = (FiniteFIFOQueue)mapHalfTCP.get(k);
		if (q == null) {
			q = new FiniteFIFOQueue(bufferSize_);
			mapHalfTCP.put(k, q);
		}
		else {
			q.setCapacity(bufferSize_);
			// if capacity reduced, requests may be discarded
		}
	}

	// add "half open" tcp for demux
	void addTCP(TCP_socket tcp_, long localAddr_, int localPort_)
	{
		if (mapHalfTCP == null)
			mapHalfTCP = new HashMap();
		mapHalfTCP.put(new HalfTCPKey(localAddr_, localPort_), tcp_);
		tcp_.localAddr = localAddr_;

		// set default request buffer
		HalfTCPKey k = new HalfTCPKey(localAddr_, localPort_, true);
		FiniteFIFOQueue q = (FiniteFIFOQueue)mapHalfTCP.get(k);
		if (q == null) {
			q = new FiniteFIFOQueue(DEFAULT_REQUEST_BUFFER);
			mapHalfTCP.put(k, q);
		}
	}

	// add "full open" tcp for demux
	// returns true if another tcp exists with same connection
	void addTCP(TCP_socket tcp_, long localAddr_, int localPort_,
					long remoteAddr_, int remotePort_)
	{
		if (mapTCP == null)
			mapTCP = new HashMap();
		FullTCPKey key_ = new FullTCPKey(localAddr_, localPort_, remoteAddr_,
						remotePort_);
		mapTCP.put(key_, tcp_);
		tcp_.setPeer(remoteAddr_);
		tcp_.remotePort = remotePort_;
		tcp_.localAddr = localAddr_;
	}

	// remove tcp_ from map, return port and connectionID
	void returnResource(TCP_socket tcp_, boolean halfOpen_)
	{
		Port dataPort_ = getPort(PortGroup_UP,
						String.valueOf(tcp_.connectionID));
		if (dataPort_ != null) {
			dataPort_.disconnect();
			removePort(dataPort_);
		}
		tcp_.upPort.disconnect();
		removeTCP(tcp_, halfOpen_);
		if (tcp_.connectionID >= MIN_CONNECTION_ID) {
			vectorTCP.setElementAt(null, tcp_.connectionID - MIN_CONNECTION_ID);
			// XX: shrink vectorTCP?
		}
		// check share port # only when full open
		if (halfOpen_) {
			portNumSpace.checkin(-tcp_.localPort);
		}
		else {
			boolean sharePortNum_ = false;
			Object[] oo_ = mapTCP.values().toArray();
			for (int i=0; i<oo_.length; i++)
				if (((TCP_socket)oo_[i]).localPort == tcp_.localPort) {
					sharePortNum_ = true;
					break;
				}
			if (!sharePortNum_)
				portNumSpace.checkin(tcp_.localPort);
		}

		if (isDebugEnabled()) {
			// keep tcp_ for debugging
			String id_ = "x_" + tcp_.getID();
			while (containsComponent(id_))
				id_ = "x" + id_;
			tcp_.setID(id_);
		}
		else
			removeComponent(tcp_);
	}

	void moveToFullOpen(TCP_socket tcp_)
	{
		removeTCP(tcp_, true); // true: half open
		portNumSpace.checkin(-tcp_.getLocalPort());
		portNumSpace.checkout(tcp_.getLocalPort());
		addTCP(tcp_, tcp_.localAddr, tcp_.localPort,
						tcp_.getPeer(), tcp_.remotePort);
	}

	protected void dataArriveAtUpPort(Object data_, Port upPort_)
	{
		try {
			SocketContract.Message request_ = (SocketContract.Message)data_;
			long localAddr_ = request_.getLocalAddr();
			long remoteAddr_ = request_.getRemoteAddr();
			int localPort_ = request_.getLocalPort();
			int remotePort_ = request_.getRemotePort();
			Object msgID_ = request_.getMessageID();
			TCP_socket tcp_ = null;

			if (request_.isAccept()) {
				synchronized (this) {
					tcp_ = allocateTCP(localPort_, true);
					if (tcp_ == null) {
						SocketContract.error(localPort_
							+ " is used by another socket",
							-1, Address.NULL_ADDR, -1, Address.NULL_ADDR, -1,
							msgID_, upPort_);
						return;
					}
					// add half TCP
					addTCP(tcp_, localAddr_, localPort_);

					// check if any request is waiting
					FiniteFIFOQueue q = getRequestBuffer(
										localAddr_, localPort_);
					if (!q.isEmpty()) {
						// loop back the request to connect the first request
						// to this accepting session
						//
						// [NOTE]  need to set PRE_LISTEN first so that
						// if multiple threads are allowed and fork() goes
						// earlier than this thread, tcp_ can handles the
						// loop-back SYN correctly 
						tcp_.state = tcp_.PRE_LISTEN;	
						fork(downPort, q.firstElement(), 0.0);

						if (isDebugEnabled())
							debug("loop back request: " + q.firstElement());

						// this causes deadlock, use PRE_LISTEN instead
						//// avoid racing between this thread and the one that
						//// handles the looped-back request
						//lock(tcp_);
					}
				}

				// note: use this's upPort_
				// blocked until connection is established or closed
				// 	if request_.getMessageID() is null
				tcp_.dataArriveAtUpPort(data_,upPort_);

				//unlock(tcp_);

				if (tcp_.state == CLOSED)
					returnResource(tcp_, false); // false: full open
			}

			else if (request_.isConnect()) {
				synchronized (this) {
					if ((tcp_ = getTCP(localAddr_, localPort_, remoteAddr_,
						remotePort_)) != null) {
						if (tcp_.upPort.anyPeer()) {
							SocketContract.error(localAddr_ + ":"
								+ localPort_ + "," + remoteAddr_ + ":"
								+ remotePort_ + " is used by another socket",
								-1, Address.NULL_ADDR, -1, Address.NULL_ADDR,
								-1, msgID_, upPort_);
							return;
						}
						else
							returnResource(tcp_, false); // false: full open
					}
					tcp_ = allocateTCP(localPort_, false);
					localPort_ = tcp_.localPort; // assigned a new one
					addTCP(tcp_, localAddr_, localPort_,
						remoteAddr_, remotePort_);
					request_.setLocalPort(localPort_);
				}

				// note: use this's upPort_
				// blocked until connection is established or closed
				// 	if request_.getMessageID() is null
				tcp_.dataArriveAtUpPort(data_,upPort_);

				if (tcp_.state == CLOSED)
					returnResource(tcp_, false); // false: full open
			}
			
			else if (request_.isClose()) {
				int connectionID_ = request_.getConnectionID();
				synchronized (this) {
					if (connectionID_ >= MIN_CONNECTION_ID)
						tcp_ = getTCP(connectionID_);
					else
						tcp_ = getTCP(localAddr_, localPort_);
				}
				if (tcp_ == null) {
					SocketContract.error("no connection to close",
							-1, Address.NULL_ADDR, -1, Address.NULL_ADDR, -1,
							msgID_, upPort_);
					return;
				}

				// XX: should verify who is closing the connection...

				//changeContext(tcp_.infoPort);
				// use this's upPort_
				tcp_.dataArriveAtUpPort(data_,upPort_);
				//changeContext(upPort_);

				synchronized (this) {
					if (tcp_.state == CLOSED)
						returnResource(tcp_, false);
					else if (tcp_.state == FIN_WAIT_2)
						;// do nothing
					else if (msgID_ == null)
						// should not be here
						error(data_, "dataArriveAtUpPort()", upPort_, 
										"couldn't close TCP: state=" + STATES[tcp_.state]); 
				}
			}

			else if (request_.isListen()) {
				synchronized (this) {
					setRequestBuffer(localAddr_, localPort_,
								request_.getBufferSize());
				}
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error(data_, "dataArriveAtUpPort()", upPort_, e_); 
    	}
	}
	
	
	protected void dataArriveAtDownPort(Object data_, Port downPort_)
	{
		try {
			InetPacket ipkt_ = (InetPacket)data_;
			TCPPacket pkt_ = (TCPPacket)ipkt_.getBody();
		
			long remoteAddr_ = ipkt_.getSource();
			long localAddr_ = ipkt_.getDestination();
	    	int remotePort_ = pkt_.getSPort();
	    	int localPort_ = pkt_.getDPort();

			TCP_socket tcp_;

			// this causes deadlock, use PRE_LISTEN instead
			//// need to lock tcp instance to avoid racing
			////boolean needLock_ = false;

			// synchronized here to avoid racing for full open 4-tuple between:
			// 1. a SYN comes in and the half open tcp becomes full open
			// 2. a CONNECT command comes in at an up port
			synchronized (this) {

			tcp_ = getTCP(localAddr_, localPort_, remoteAddr_, remotePort_);

			if (tcp_ == null) {
				if (pkt_.isSYN()) {
					// no full tcp
					// check request queue first
					FiniteFIFOQueue q = getRequestBuffer(
										localAddr_, localPort_);
					if (q == null) {
						if (isDebugEnabled())
							error(data_, "dataArriveAtDownPort()", downPort_,
									"connection does not exist: "
									+ localAddr_ + ":" + localPort_ + "--"
									+ remoteAddr_ + ":" + remotePort_);
						// XXX: send back error?
						return; 
					} 
					else {
						if (!q.isEmpty()) {
							if (q.firstElement() != ipkt_) {
								// not a loopback
								if (q.isFull()) {
									if (isDebugEnabled())
										error(data_, "dataArriveAtDownPort()",
											downPort_, "request buffer full:"
											+ localAddr_ + ":" + localPort_
											+ "--" + q);
									return;
								}
								else {
									// put the request in the queue
									_enqueueRequest(q, ipkt_);
									return;
								}
							}
							else {
								// remove the request & continue to connect
								q.dequeue();
								if (isDebugEnabled())
									debug("loop back request connected: "
													+ ipkt_);
								//needLock_ = true;
							}
						}
						tcp_ = getTCP(localAddr_, localPort_);
						if (tcp_ == null) {
							// application not issue an accept yet
							_enqueueRequest(q, ipkt_);
							return;
						}
						else {
							if (tcp_.localAddr != Address.NULL_ADDR && 
								tcp_.localAddr != ipkt_.getDestination()) {
								// this tcp_ listens only on a specific address
								// drop the packet
								return;
							}

							// move tcp_ to full open
							tcp_.setPeer(ipkt_.getSource());
							tcp_.remotePort = pkt_.getSPort();
							moveToFullOpen(tcp_);

							//if (needLock_) lock(tcp_);
						}
					}
				}
				else if (pkt_.isFIN()) {
					// no full tcp
					// send back an ACK, as a courtesy
					forward(new TCPPacket(localPort_, remotePort_, -1, 0,
							0, true,false,false,-1.0,-1.0,20,0,null),
							localAddr_, remoteAddr_, false, 255, 0);
					return;
				}
				else
					// just drop it
					return;
			}
			} // synchronized

			if (isDebugEnabled())
				debug(tcp_ + "/" + tcp_.localAddr + ":"
					+ tcp_.localPort + " <--> "
					+ tcp_.getPeer() + ":" + tcp_.remotePort + "  "
					+ STATES[tcp_.getState()]);
			//changeContext(tcp_.infoPort);
			tcp_.dataArriveAtDownPort(data_,downPort_);
			//changeContext(downPort_);

			//if (needLock_) unlock(tcp_);
			
			// if tcp_ is fully closed (ie, bi-direction)
			if (tcp_.state == CLOSED)
				synchronized (this) {
					returnResource(tcp_, false);
				}
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error(data_, "dataArriveAtDownPort()", downPort_,
							"unrecognized data: " + e_);
		}
	}

	void _enqueueRequest(FiniteFIFOQueue q, InetPacket ipkt_)
	{
		TCPPacket pkt_ = (TCPPacket)ipkt_.getBody();
		long remoteAddr_ = ipkt_.getSource();
		long localAddr_ = ipkt_.getDestination();
	    int remotePort_ = pkt_.getSPort();
	    int localPort_ = pkt_.getDPort();

		// avoid duplicate; check if the request is already in the queue
		Object[] oo = q.retrieveAll();
		for (int i=0 ;i<oo.length; i++) {
			InetPacket ipktthat_ = (InetPacket)oo[i];
			TCPPacket pktthat_ = (TCPPacket)ipktthat_.getBody();
			long remoteAddrthat_ = ipktthat_.getSource();
			long localAddrthat_ = ipktthat_.getDestination();
	    	int remotePortthat_ = pktthat_.getSPort();
	    	int localPortthat_ = pktthat_.getDPort();
			if (remoteAddr_ == remoteAddrthat_
				&& remotePort_ == remotePortthat_
				&& localAddr_ == localAddrthat_
				&& localPort_ == localPortthat_) {
				// drop the duplicate
				if (isDebugEnabled())
					debug("duplicate request: " + ipkt_);
				return;
			}
		}
		q.enqueue(ipkt_);
		if (isDebugEnabled())
			debug("enqueue request: " + ipkt_);
	}
}


