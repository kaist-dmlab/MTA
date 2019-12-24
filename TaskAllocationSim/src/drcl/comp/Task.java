// @(#)Task.java   9/2002
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

/** Defines a runtime task performed by a {@link WorkerThread}.
 * A specific type of task is further defined in a subclass.
 */
public abstract class Task extends ACATimer
{
	public static final int TYPE_UNKNOWN = Integer.MIN_VALUE;
	public static final int TYPE_START = Integer.MIN_VALUE + 1;
	public static final int TYPE_STOP = Integer.MIN_VALUE + 2;
	public static final int TYPE_RESUME = Integer.MIN_VALUE + 3;
	public static final int TYPE_RUNNABLE = Integer.MIN_VALUE + 4;

	/** Port to which data is delivered. */
	public Port port;

	/** The return port for server port operation. */
	public Port returnPort;

	// by WorkerThread to queue notified object
	/** Creates a special task to notify on the targeted object. */
	public static Task createNotify(Object target_)
	{ return new TaskNotify(target_, 0.0); }

	/** Creates a special task to notify on the targeted object. */
	public static Task createNotify(Object target_, double time_)
	{ return new TaskNotify(target_, time_); }

	/** Creates a special task to start the component system. */
	public static Task createStart(Component c_, double time_)
	{ return new TaskSpecial(c_.infoPort, null, TYPE_START, time_); }

	/** Creates a special task to stop the component system. */
	public static Task createStop(Component c_, double time_)
	{ return new TaskSpecial(c_.infoPort, null, TYPE_STOP, time_); }

	/** Creates a special task to resume the component system. */
	public static Task createResume(Component c_, double time_)
	{ return new TaskSpecial(c_.infoPort, null, TYPE_RESUME, time_); }

	/** Creates a special task to execute the Runnable as a normal Java thread. */
	public static Task createRunnable(Runnable r_, double time_)
	{ return new TaskSpecial(r_, TYPE_RUNNABLE, time_); }

	/** Executes the task in <code>thread_</code>. */
	public abstract void execute(WorkerThread thread_);
}
