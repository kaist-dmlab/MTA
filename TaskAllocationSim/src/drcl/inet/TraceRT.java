// @(#)TraceRT.java   12/2003
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

package drcl.inet;

import java.util.*;
import drcl.comp.*;
import drcl.data.IntObj;

/**
 * Component to initiate a trace-route command.
 * Use {@link #traceRoute(long)} to start it. 
 */
public class TraceRT extends Component
{
	Port downPort = addPort("down");
	HashMap htRequest = new HashMap(); // destAddr (long) --> #requests (int)
	HashMap htResult = new HashMap(); // destAddr (long) --> result (Object[])

	public TraceRT()
	{ super(); }
	
	public TraceRT(String id_)
	{ super(id_); }

	// possible racing between reset() and traceRoute()
	/** Interrupts all the requesting threads and resets the component. */ 
	public synchronized void reset()
	{
		super.reset();
		// wake up all the waiting threads
		for (Iterator it_ = htRequest.values().iterator(); it_.hasNext(); ) {
			Object v = it_.next();
			synchronized (v) {
				v.notifyAll();
			}
		}
		htRequest.clear();
		htResult.clear();
	}
	
	/** Get trace route result with trace route packet of size 0.
	 * @see #traceRoute(long, int) */
	public Object[] traceRoute(long destAddress_)
	{ return traceRoute(destAddress_, 0); }

	/** Returns the result of trace route in an array.
	 * The method is blocked until result is back.
	 * This requires simulation in running state. 
	 * Every two elements in the array represent time (Double) and address
	 * (Long) for the corresponding hop along the route.
	 * Returns null if destination is not reachable for any reason.
	 * @param pktSize_ packet size of the trace route packet. 
	 */
	public Object[] traceRoute(long destAddress_, int pktSize_)
	{
		IntObj numRequests_ = null;
		Long dest_ = new Long(destAddress_);
		synchronized (this) {
			numRequests_ = (IntObj)htRequest.get(dest_);
			if (numRequests_ == null) {
				numRequests_ = new IntObj(0);
				htRequest.put(dest_, numRequests_);
			}
			numRequests_.value++;
		}

		synchronized (numRequests_) {
			TraceRTPkt p = new TraceRTPkt(TraceRTPkt.RT_REQUEST, destAddress_,
						pktSize_);
			downPort.doSending(p);
			try {
				numRequests_.wait(); // wait until result comes back
			}
			catch (InterruptedException e) {
				return null;
			}
			// XXX: should set up a timer to prevent a loss

			Object[] result_ = (Object[])htResult.get(dest_);
			if (--numRequests_.value == 0) {
				htRequest.remove(dest_);
				return (Object[])htResult.remove(dest_);
			}
			else
				return (Object[])htResult.get(dest_);
		}
	}

	protected void process(Object data_, Port inPort_) 
	{
		if (!(data_ instanceof TraceRTPkt)) {
			debug("Dont know how to handle " + data_);
			return;
		}

		TraceRTPkt p = (TraceRTPkt)data_;
		if (p.getType() != TraceRTPkt.RT_RESPONSE) {
			debug("Dont know how to handle " + data_);
			return;
		}

		Long dest_ = new Long(p.getSource());
		IntObj numRequests_ = null;

		synchronized (this) {
			numRequests_ = (IntObj)htRequest.get(dest_);
			if (numRequests_ == null) {
				debug("no request corresponds to the response: " + data_);
				return;
			}
		}

		synchronized (numRequests_) {
			htResult.put(dest_, p.getList());
			numRequests_.notifyAll();
		}
	}
	
	public String info()
	{
		if (htRequest.isEmpty())
			return "No pending request.\n";

		StringBuffer sb = new StringBuffer();

		for (Iterator it_ = htRequest.keySet().iterator(); it_.hasNext(); ) {
			Object key_ = it_.next();
			Object n = htRequest.get(key_);
			Object result_ = htResult.get(key_);
			sb.append("Dest " + key_ + ": num=" + n + ", "
					+ (result_ == null? "no result yet": result_.toString()));
		}
		return sb.toString();
	}
}
