// @(#)SystemMonitor.java   1/2004
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

package drcl.ruv;

import java.util.*;
import drcl.comp.*;
import drcl.comp.contract.*;
import drcl.util.queue.*;
import drcl.util.*;

/**
 * The RUV System monitor class. 
 */
public class SystemMonitor extends drcl.comp.Extension
{
	Port defaultInPort = addPort(".in", false/*not removable*/);

	transient protected TraceManager tr =
			new TraceManager(new String[]{
					drcl.comp.Component.Trace_DATA,
					drcl.comp.Component.Trace_SEND});

	boolean tracePrintThread = false;
	boolean isRcvOn = true;
	boolean allToStderr = false; // output all msg to stderr

	{
		setComponentFlag(FLAG_ENABLED); // turn off all other flags
		setPortNotificationEnabled(true); // to act as extension
		infoPort.setType(Port.PortType_IN);
		infoPort.setExecutionBoundary(false);
		tr.setTraceEnabled(true); 
		tr.setTraceEnabledAt(drcl.comp.Component.Trace_DATA, true);
		tr.setTraceEnabledAt(drcl.comp.Component.Trace_SEND, true);
	}
	
	public SystemMonitor() 
	{ super(); 	}
	
	public SystemMonitor(String id_)
	{ super(id_); }
	
	/** Enables/disables the receipt notice.  */
	public void setRcvEnabled(boolean v_) {isRcvOn = v_; }
	public boolean isRcvEnabled() { return isRcvOn; }

	/**
	 */
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		synchronized (this) {
			if (inPort_ == infoPort) {
				try {
					//java.lang.System.out.println("receives " + data_);
					Message s_ = (Message)data_;
					if (s_ instanceof GarbageContract.Message) {
						if (!isGarbageDisplayEnabled()
							|| !((GarbageContract.Message)s_).isDisplayable())
							return;
					}
					else if (s_ instanceof TraceContract.Message) {
						if (isTraceEnabled())
							printTrace((TraceContract.Message)s_);
						return;
					}
					else if (s_ instanceof ErrorContract.Message) {
						if (isErrorNoticeEnabled())
							errpost(" ** ERROR ** " + s_.toString("| ") + "\n");
						return;
					}
					else if (s_ instanceof PropertyContract.Message)
						return; // ignore
					//else post("Unrecognized info message| " + data_ + "\n");

					post(s_.toString("| ") + "\n");
					return;
				}
				catch (Exception e_) {
					e_.printStackTrace();
				}
			}
			else if (isRcvOn) {
				// receipt notification
				if (data_ instanceof String && inPort_ == defaultInPort)
					post(data_.toString() + "\n");
				else if (inPort_ == defaultInPort)
					post("SYSTEM MONITOR| " + "| "
								+ drcl.util.StringUtil.toString(data_) + "\n");
				else {
					post(inPort_.getID() + "@" + inPort_.getGroupID() + "| "
								+ drcl.util.StringUtil.toString(data_) + "\n");
				}
			}
		}
	}
	
	//
	private void ___TRACE____() {}
	//

	public boolean isThreadPrintedInTraceEnabled()
	{ return tracePrintThread; }

	public void setThreadPrintedInTraceEnabled(boolean enabled_)
	{ tracePrintThread = enabled_; }
	
	WorkerThread getThread()
	{
		Thread t_ = Thread.currentThread();
		if (t_ instanceof WorkerThread) return (WorkerThread)t_;
		else return null;
	}

	/**
	 * For subclasses to provide their own trace mechanism,
	 * e.g., trace filtering.
	 */
	protected void printTrace(TraceContract.Message msg_)
	{
		String trace_ = msg_.getTrace();
		if (tr.isTraceEnabledAt(trace_)) {
			if (tracePrintThread)
				post(msg_.toString("| ") + "|" + getThread()._debug() + "\n");
			else
				post(msg_.toString("| ") + "\n");
		}
	}
	
	public TraceManager getTraceManager() { return tr; }
	
	public void setTraceEnabledAt(String which_, boolean enabled_)
	{ if (tr != null) tr.setTraceEnabledAt(which_, enabled_); }
	
	public boolean isTraceEnabledAt(String which_)
	{ return tr == null? false: tr.isTraceEnabledAt(which_); }
	
	/** Enables/disables directing all outputs to stderr.
	 * It is disabled by default. */
	public void setOutputAllToStderrEnabled(boolean enabled_)
	{ allToStderr = enabled_; }

	/** Returns true if directing all outputs to stderr is enabled. */
	public boolean isOutputAllToStderrEnabled()
	{ return allToStderr; }

	public void post(String msg_)
	{
		if (allToStderr) errpost(msg_);
		else java.lang.System.out.print(msg_);
	}
	
	public void errpost(String msg_)
	{ java.lang.System.err.print(msg_); }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		if (tr != null) sb_.append("Trace manager: " + tr + "\n");
		return sb_.toString();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		SystemMonitor that_ = (SystemMonitor)source_;
		tr = (TraceManager)that_.tr.clone();
	}
}
