// @(#)TaskSpecial.java   9/2002
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

package drcl.comp;

/**
 * Defines the "special" runtime task performed by a {@link WorkerThread}.
 */
public class TaskSpecial extends Task
{
	int type;

	/** Creates a notify/runnable task to be executed at the specified time. */
	public TaskSpecial (Object data_, int type_, double time_)
	{
		data = data_;
		type = type_;
		time = time_;
	}
	
	/** Creates a start/stop/resume task to be executed at the specified time. */
	public TaskSpecial (Port port_, int type_, double time_)
	{
		port = port_;
		type = type_;
		time = time_;
	}

	/** Creates a notify/runnable task to be executed at the specified time. */
	public TaskSpecial (Port port_, Object data_, int type_, double time_)
	{
		port = port_;
		data = data_;
		type = type_;
		time = time_;
	}

	public final static String printType(int type_)
	{
		switch (type_) {
		case TYPE_START: return "START";
		case TYPE_STOP: return "STOP";
		case TYPE_RESUME: return "RESUME";
		case TYPE_RUNNABLE: return "RUNNABLE";
		default:
			return "unknown";
		}
	}
		
	public final String toString()
	{
		return printType(type) + ":" + port + ","
			+ drcl.util.StringUtil.toString(data) + ",time:" + time;
	}

	public void execute(WorkerThread thread_)
	{
		switch (type) {
		case TYPE_RUNNABLE:
			((Runnable)data).run();
			break;
		case TYPE_START:
			synchronized (thread_) { thread_.totalNumEvents++; }
			if (!port.host.isStarted() || port.host instanceof RestartableComponent) {
				port.host.setStarted(true);
				port.host._start();
				thread_.releaseAllLocks(port.host); // Don't hold locks across executions!
			}
			break;
		case TYPE_STOP:
			synchronized (thread_) { thread_.totalNumEvents++; }
			if (port.host.isStarted() && !port.host.isStopped()) {
				port.host.setStopped(true);
				port.host._stop();
				thread_.releaseAllLocks(port.host); // Don't hold locks across executions!
			}
			break;
		case TYPE_RESUME:
			//System.out.println(mainContext.data + " " + mainContext.port.host);
			synchronized (thread_) { thread_.totalNumEvents++; }
			if (port.host.isStarted() && port.host.isStopped()) {
				port.host.setStopped(false);
				port.host._resume();
				thread_.releaseAllLocks(port.host); // Don't hold locks across executions!
			}
			break;
		default:
			drcl.Debug.error("unknown task: " + this);
		}
	}
}
