// @(#)WorkerThread.java   9/2002
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

import java.util.*;
import java.beans.*;
import drcl.util.queue.FIFOQueue;
import drcl.util.StringUtil;

/**
 * Carries out data processing at a component or special runtime tasks.
 *
 * @see ACARuntime
 */
public abstract class WorkerThread extends Thread {
	/** The worker runtime of this thread. */
	public ACARuntime runtime;

	// currentContext is created when thread executes across multiple
	// components.
	// the original host component is kept in the main context ("mainContext").
	public Task mainContext, currentContext;

	protected String state = State_INACTIVE;

	public long totalNumEvents = 0; // total # of events

	// for server port, set and used by Port, may go across threads
	protected Port returnPort = null;

	// for sendReceive, set and used by Port
	Object sendingPort = null;

	public WorkerThread() {
		super();
	}

	public WorkerThread(ThreadGroup group_, String name_) {
		super(group_, name_);
	}

	public WorkerThread(String name_) {
		super(name_);
	}

	public String info() {
		return info("");
	}

	public abstract String info(String prefix_);

	public String _getName() {
		return StringUtil.lastSubstring(super.toString(), ".");
	}

	public String toString() {
		return StringUtil.lastSubstring(super.toString(), ".");
	}

	// for debugging
	public String _debug() {
		return _getName() + ",returnport:" + returnPort;
	}

	protected void startComponent(Component c_) {
		c_.setStarted(true);
		c_._start();
	}

	protected void stopComponent(Component c_) {
		c_.setStopped(true);
		c_._stop();
	}

	protected void resumeComponent(Component c_) {
		c_.setStopped(false);
		c_._resume();
	}

	public double getTime() {
		return runtime.getTime();
	}

	//
	private void ___API_FOR_COMPONENT___() {
	}

	//

	protected abstract void sleepFor(double time_);

	protected abstract void sleepUntil(double time_);

	/*
	 * protected int getTaskPriority() { return priority; }
	 * 
	 * protected void setTaskPriority(int priority_) { priority = priority_; }
	 */

	public final boolean isOrphan() {
		return runtime == null;
	}

	//
	private void ___EVENTS___() {
	}

	// Identify the events to make it easier to do simulation extension.
	//

	/** Data processing has come to an end. */
	protected void finishing() {
		setState(State_FINISHING);
	}

	public long getNumEvents() {
		return totalNumEvents;
	}

	//
	private void ___SYNC_APIS___() {
	}

	//

	protected abstract void releaseAllLocks(Component host_);

	protected abstract void lock(Component host_, Object o_);

	protected void unlock(Component host_, Object o_) {
		unlock(host_, o_, false);
	}

	protected abstract void unlock(Component host_, Object o_, boolean release_);

	// Make thread wait on the object until being notified by another thread.
	protected abstract void wait(Component host_, Object o_);

	// Notifies the first thread waiting on the object.
	protected abstract void notify(Component host_, Object o_);

	// Notifies all the threads waiting on the object.
	protected abstract void notifyAll(Component host_, Object o_);

	// Yields to check if runtime is still in running state
	// and also yields to other threads to perform other tasks.
	// Used by component which may hold the thread for a long time
	// or forever in an infinite loop.
	protected abstract void yieldToRuntime();

	//
	private void ___STATE___() {
	}

	//

	public final String getState2() {
		return state;
	}

	protected void setState(String new_) {
		state = new_;
	}

	public final boolean isWaiting() {
		return state == State_WAITING;
	}

	/** State of not executing a task. */
	public static final String State_INACTIVE = "INACTIVE";
	/** State of executing a task. */
	public static final String State_ACTIVE = "ACTIVE";
	/** State of sleeping for finite time. */
	public static final String State_SLEEPING = "SLEEPING";
	/** State of waiting on an object. */
	public static final String State_WAITING = "WAITING";
	/** State of finishing up, ready to accept next task. */
	public static final String State_FINISHING = "FINISHING";
}
