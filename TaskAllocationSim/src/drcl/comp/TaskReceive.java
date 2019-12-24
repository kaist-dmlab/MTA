// @(#)TaskReceive.java   1/2003
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

package drcl.comp;

/**
 * Defines the "receive" task.
 */
public class TaskReceive extends Task
{
	/** Creates a task to be executed immediately. */
	public TaskReceive (Port port_, Object data_)
	{
		port = port_;
		data = data_;
	}

	/** Creates a prioritized task to be conducted immediately, with a return port. */
	public TaskReceive (Port port_, Object data_, Port returnPort_)
	{
		port = port_;
		data = data_;
		returnPort = returnPort_;
	}

	/** Creates a prioritized task to be conducted at the specified time. */
	public TaskReceive (Port port_, Object data_, double time_)
	{
		port = port_;
		data = data_;
		time = time_;
	}

	public final String toString()
	{
		return "RECEIVE:" + port + "," + drcl.util.StringUtil.toString(data)
			+ ",time:" + time;
	}

	public void execute(WorkerThread thread_)
	{
		synchronized (thread_) { thread_.totalNumEvents++; }
		if (port.flagTraceData)
			port.host.trace(Component.Trace_DATA, port, data);
		if (port.host.isEnabled())
			port.host.process(data, port);
		thread_.releaseAllLocks(port.host);
			// Don't hold locks across executions!
	}
}
