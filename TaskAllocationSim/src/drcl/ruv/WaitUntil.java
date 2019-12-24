// @(#)WaitUntil.java   1/2004
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

import drcl.comp.ACARuntime;
import drcl.comp.WorkerThread;

/**
 * The class provides different ways to stop the current execution until 
 * a condition is met.  Use one of the constructors to start the wait.
 */
public class WaitUntil implements Runnable
{
	ACARuntime runtime;
	boolean stopHook = false;
	Object lock = this;

	/** The execution is blocked (on itself) until the runtime stops
	 * (at the time specified).*/
	public WaitUntil(ACARuntime r, double stopAt)
		   	throws InterruptedException
	{
		runtime = r;
		synchronized (lock) {
			runtime.addRunnableAt(stopAt, this);
			runtime.resume(); // suppose the runtime is stopped at the beginning
			lock.wait(); // wait until the runtime stops
		}
	}

	/** The execution is blocked (on lock) until the runtime stops
	 * (at the time specified).*/
	public WaitUntil(ACARuntime r, double stopAt, Object lock_)
		   	throws InterruptedException
	{
		runtime = r;
		lock = lock_;
		synchronized (lock) {
			runtime.addRunnableAt(stopAt, this);
			runtime.resume(); // suppose the runtime is stopped at the beginning
			lock.wait(); // wait until the runtime stops
		}
	}

	/** The execution is blocked (on itself) until the runtime finishes
	 * all the events. */
	public WaitUntil(ACARuntime r) throws InterruptedException
	{
		runtime = r;
		synchronized (lock) {
			runtime.addStopHook(this);
			stopHook = true;
			runtime.resume(); // suppose the runtime is stopped at the beginning
			lock.wait(); // wait until the runtime stops
			runtime.removeStopHook(this);
		}
	}

	/** The execution is blocked (on lock) until the runtime finishes
	 * all the events. */
	public WaitUntil(ACARuntime r, Object lock_) throws InterruptedException
	{
		runtime = r;
		lock = lock_;
		synchronized (lock) {
			runtime.addStopHook(this);
			stopHook = true;
			runtime.resume(); // suppose the runtime is stopped at the beginning
			lock.wait(); // wait until the runtime stops
			runtime.removeStopHook(this);
		}
	}
	/** The execution is blocked (on the shell) until the command
	 * (interpreted by the shell)
	 * returns an affirmative result. */
	public WaitUntil(Shell shell_, String cmd_) throws InterruptedException
	{ this(shell_, cmd_, shell_); }

	/** The execution is blocked (on lock) until the command
	 * (interpreted by the shell)
	 * returns an affirmative result. */
	public WaitUntil(Shell shell_, String cmd_, Object lock_)
			throws InterruptedException
	{
		if (shell_ == null) return;
		//java.lang.System.out.println("Shell: " + shell_ 
		//				+ "\nwAit uNtil: " + cmd_);
		boolean done_ = false;
		try {
			synchronized (lock) {
				while (!done_) {
					lock.wait(200);
					done_ = shell_.isResultAffirmative(shell_.eval(cmd_));
				}
			}
		}
		catch (Exception e_) {
			if (e_ instanceof InterruptedException)
				throw (InterruptedException)e_;
			e_.printStackTrace();
		}
	}

	public void run()
	{
		// we should be in a simulation thread
		Thread thread = Thread.currentThread();
		if (!(thread instanceof WorkerThread)
			|| runtime != ((WorkerThread)thread).runtime) {
			return;
		}

		synchronized (lock) {
			if (!stopHook) runtime.stop();
			lock.notify(); // notify the main thread
		}
	}
}
