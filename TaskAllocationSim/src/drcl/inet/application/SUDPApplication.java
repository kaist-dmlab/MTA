// @(#)SUDPApplication.java   9/2002
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

package drcl.inet.application;

import java.util.Hashtable;
import drcl.data.*;
import drcl.comp.*;
import drcl.inet.contract.DatagramContract;
import drcl.inet.data.RTKey;
import drcl.util.queue.FIFOQueue;
import drcl.net.*;

/**
 * Single-session (or simplified) UDP application base class.
 */
public class SUDPApplication extends drcl.net.Module
{
	static {
		setContract(SUDPApplication.class, Module.PortGroup_DOWN + "@",
			new DatagramContract(Contract.Role_PEER));
	}

	public synchronized void reset()
	{
		super.reset();
		if (incoming != null) incoming.clear();
	}
	
	public synchronized String info()
	{
		if (incoming == null || incoming.size() == 0) return "No incoming queues.\n";
		else return "Incoming queue:\n   " + incoming + "\n";
	}

    boolean connected=false;
    long src, dst, tos;
    int sport, dport;

    public SUDPApplication() {
		super();
    }
    public SUDPApplication(String id_) {
		super(id_);
    }
	
    public void open(long src, long dst, int dport) {
        open(src,dst, dport, 0);
    }
    public void open(long src, long dst, int dport, int tos) {
        // if (connected) return? exception? allowed?
        this.src = src;
        this.dst = dst;
        this.dport = dport;
        this.tos = tos;
        connected = true;
    }

    protected void sendmsg(Object data_, int size_, long dst, int dport)
	{ sendmsg(data_, size_, Address.NULL_ADDR, dst, dport, 0);    }

    protected void sendmsg(Object data_, int size_, long src, long dst, int dport)
	{ sendmsg(data_, size_, src, dst, dport, 0);    }

    protected void sendmsg(Object data_, int size_) {
        if (connected)
            sendmsg(data_, size_, src, dst, dport, 0);
    }
	
    protected void sendmsg(Object data_, int size_, long dst, int dport, long tos)
	{ sendmsg(data_, size_, Address.NULL_ADDR, dst, dport, tos);    }
	
    protected void sendmsg(Object data_, int size_, long src, long dst, int dport, long tos)
	{
        downPort.doSending(new DatagramContract.Message(data_, size_, src, dst, dport, tos));
    }

    protected DatagramContract.Message sendreceive(Object data_, int size_, long dst, int dport,
							  int ntry_, double timeout_)
	{ return sendreceive(data_, size_, Address.NULL_ADDR, dst, dport, 0, ntry_, timeout_); }

    protected DatagramContract.Message sendreceive(Object data_, int size_, long src, long dst, int dport,
							  int ntry_, double timeout_)
	{ return sendreceive(data_, size_, src, dst, dport, 0, ntry_, timeout_); }

    protected DatagramContract.Message sendreceive(Object data_, int size_, int ntry_, double timeout_) {
        if (connected)
            return sendreceive(data_, size_, src, dst, dport, 0, ntry_, timeout_);
		else
			return null;
    }

	// send and expect reply from the peer
    protected DatagramContract.Message sendreceive(Object data_, int size_,  long dst, int dport, long tos,
							  int ntry_, double timeout_)
	{ return sendreceive(data_, size_, Address.NULL_ADDR, dst, dport, tos, ntry_, timeout_); }
	
	// send and expect reply from the peer
    protected DatagramContract.Message sendreceive(Object data_, int size_, long src, long dst, int dport, long tos,
							  int ntry_, double timeout_)
	{
		for (int i=0; i<ntry_; i++) {
		    downPort.doSending(new DatagramContract.Message(data_, size_, src, dst, dport, tos));
			RTKey key_ = new RTKey(dst, 0, dport);
			ACATimer timer_ = setTimeout(key_, timeout_);
			DatagramContract.Message reply_ = recvmsg(dst, dport);
			if (reply_ != null) {
				cancelTimeout(timer_);
				return reply_;
			}
		}
		return null; // failed after ntry_ tries
    }

    Hashtable incoming = new Hashtable(); // RTKey -> FIFOQueue
	
    protected void dataArriveAtDownPort(Object data_, Port downPort_)
	{
		if (!(data_ instanceof DatagramContract.Message)) {
			error(data_, "dataArrivedAtDownPort", downPort_, "unrecognized data");
			return;
		}
		DatagramContract.Message s_ = (DatagramContract.Message) data_;
			
		FIFOQueue q_ = getAnonymousQueue(false);
		if (q_ == null) q_ = getQueue(s_.getSource(), s_.getPort());
			
		synchronized (q_) {
		    q_.enqueue(s_);
			notify(q_);
		}
    }

	protected void timeout(Object data_)
	{
		try {
			RTKey key_ = (RTKey) data_;
			long src_ = key_.getSource();
			int srcPort_ = key_.getIncomingIf(); // encoded
			
			FIFOQueue q_ = getQueue(src_, srcPort_);
			
			synchronized (q_) {
				notify(q_);
			}
		}
		catch (Exception e_) {
			if (isErrorNoticeEnabled())
				error(data_, "timeout()", timerPort, "unrecognized data");
		}
	}
	
	// anonymous queue
	synchronized FIFOQueue getAnonymousQueue(boolean createIt_)
	{
		RTKey key_ = new RTKey(Address.NULL_ADDR, 0, -1);
		FIFOQueue q_ = (FIFOQueue)incoming.get(key_);
		if (q_ == null && createIt_) {
			q_ = new FIFOQueue();
			incoming.put(key_, q_);
		}
		return q_;
	}
	
	synchronized void removeAnonymousQueue()
	{
		RTKey key_ = new RTKey(Address.NULL_ADDR, 0, -1);
		incoming.remove(key_);
	}
	
	synchronized FIFOQueue getQueue(long src_, int srcPort_)
	{
		RTKey key_ = new RTKey(src_, 0, srcPort_);
		FIFOQueue q_ = (FIFOQueue)incoming.get(key_);
		if (q_ == null) {
			q_ = new FIFOQueue();
			incoming.put(key_, q_);
		}
		return q_;
	}
	
    protected DatagramContract.Message recvmsg()
	{
		FIFOQueue q_ = getAnonymousQueue(true);
        DatagramContract.Message s_ = null;
		
        synchronized (q_) {
            if (q_.isEmpty()) wait(q_);
			s_ = (DatagramContract.Message)q_.dequeue();
        }
        return s_;
	}
	
    protected DatagramContract.Message recvmsg(long src_, int srcPort_)
	{
		FIFOQueue q_ = null;
        DatagramContract.Message s_ = null;
		
		synchronized (this) {
			removeAnonymousQueue();
			q_ = getQueue(src_, srcPort_);
		}
		
        synchronized (q_) {
            if (q_.isEmpty()) wait(q_);
			s_ = (DatagramContract.Message)q_.dequeue();
        }
        return s_;
    }
	
	// send and expect reply from the peer
    protected DatagramContract.Message recvmsg(long src_, int sport_, double timeout_)
	{
		RTKey key_ = new RTKey(src_, 0, sport_);
		ACATimer timer_ = setTimeout(key_, timeout_);
		DatagramContract.Message reply_ = recvmsg(src_, sport_);
		if (reply_ != null) {
			cancelTimeout(timer_);
			return reply_;
		}
		else
			return null; // failed, been timed out
    }

	/** Retrieves the peer address from the datagram. */
	protected long getPeerAddress(Object data_)
	{ return ((DatagramContract.Message)data_).getSource(); }

	/** Retrieves the peer port from the datagram. */
	protected int getPeerPort(Object data_)
	{ return ((DatagramContract.Message)data_).getSourcePort(); }

	/** Retrieves the content in the datagram. */
	protected Object getContent(Object data_)
	{ return ((DatagramContract.Message)data_).getContent(); }
}
