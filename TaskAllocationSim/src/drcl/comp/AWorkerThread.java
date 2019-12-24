// @(#)AWorkerThread.java   2/2004
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

package drcl.comp;

import java.util.*;
import java.beans.*;
import drcl.util.queue.FIFOQueue;
import drcl.util.StringUtil;

/**
 * @see ARuntime
 */
public class AWorkerThread extends WorkerThread
{
	static boolean RECYCLING = true; // thread recycling?
	static final Task DUMMY_CONTEXT = Task.createNotify("DUMMY_TASK");
		// used in run()
	static final Task REQUEST_WAITING = Task.createNotify("REQUEST_WAITING");
		// used in _sleepUntil()
	static final Task GETTING_TASK = Task.createNotify("Getting_Task");
	static final Object SEND_RCV_REQUEST = Port.SEND_RCV_REQUEST;

	/** The worker runtime of this thread. */
	protected ARuntime aruntime;
	
	/** The object that this thread waits on. */
	transient protected Object sleepOn = null;
	
	Task nextTask; // set if next task is available; set by runtime

    double wakeUpTime = 0.0; // time to wake up, informational

	WaitPack waitPack;
	boolean beingWakedup = false; // for ForkManager to call wakeUp();

	// for debug
	//FIFOQueue stateQ = new FIFOQueue(10); // temporary
	Thread lastwakeupthread = null;
	Object lastsleepon = null;

	public AWorkerThread()
	{ super(); }
	
	public AWorkerThread(ThreadGroup group_, String name_)
	{ super(group_, name_);}
	
	public String info(String prefix_)
	{
		if (runtime == null)
			return StringUtil.lastSubstring(prefix_ + super.toString(), ".")
				+ "," + "<orphan>";
		StringBuffer sb_ = new StringBuffer(
			prefix_ + StringUtil.lastSubstring(super.toString(), ".") + ", "
			+ state + (sleepOn == null || sleepOn == this? "": " on " + sleepOn
			+ (waitPack != null && waitPack.target != null?
					"(waiting/locking)": ""))
			+ (state == State_SLEEPING? " until " + wakeUpTime: "") + "\n"
			+ prefix_ + "Context=" + mainContext()
			+ (mainContext == null? ", ": "\n" + prefix_)
			+ "Next_Task=" + (nextTask != null? nextTask.toString(): "<null>")
			+ "\n");
		if (currentContext != mainContext)
			sb_.append(prefix_ + "CurrentContext: " + _currentContext() + "\n");
		if (returnPort != null)
			sb_.append(prefix_ + "return_port: " + returnPort + "\n");
		if (sendingPort != null)
			sb_.append(prefix_ + "sending_port: " + sendingPort + "\n");
		sb_.append(prefix_ + "Locks: " + locks());
		sb_.append(prefix_ + "#arrivals: " + totalNumEvents + "\n");
		sb_.append(prefix_ + "last_sleepOn: " + lastsleepon + "\n");
		sb_.append(prefix_ + "last_wkUp_thread: "
				+ (lastwakeupthread== null?
						null: currentThread(lastwakeupthread)) + "\n");
		return sb_.toString();
	}

	private String currentThread(Thread t_)
	{
		if (t_ == null)
			t_ = Thread.currentThread();
		return t_.toString();
	}
	
	public String _toString()
	{
		if (runtime == null)
			return StringUtil.lastSubstring(super.toString(), ".") + ","
				+ "<orphan>";
		else
			return StringUtil.lastSubstring(super.toString(), ".") + "--"
			   + state + (sleepOn == null || sleepOn == this? "": " on "
							   + sleepOn
			   + (waitPack != null && waitPack.target != null?
					   "(waiting/locking)": ""))
			   + (state == State_SLEEPING? " until " + wakeUpTime: "") + "--"
			   + "context=" + mainContext() + "--"
			   + "next_task=" + (nextTask != null? nextTask.toString():
							   "<null>") + "--"
			   + (currentContext == mainContext? "": "--currentContext="
							   + _currentContext())
			   + (returnPort == null? "": "--return_port=" + returnPort)
			   + (sendingPort == null? "": "--sending_port=" + sendingPort);
	}
	
	String mainContext()
	{
		if (mainContext == null) return "<null>";
		return (mainContext.port != null? mainContext.port + ",": "")
			+ (mainContext.data != null? mainContext.data.toString(): "<null>")
			+ (returnPort == null? "": ",return_port:" + returnPort)
			+ (sendingPort == null? "": ",sending_port:" + sendingPort);
	}
	
	/**
	 * Delay the starting until the time specified later(second).
	 */
	public void start()
	{
		//if (port != null) System.out.println(this + " starts: " + data);
		//runtime.startRegister(this);

		if (aruntime.isSuspend()) return;
		
		if (isAlive()) {
			//__wakeUp();
			synchronized (this) {
				lastsleepon = this;
				lastwakeupthread = Thread.currentThread();
		
				try {
					this.notify();
				}
				catch (Exception e_) {
					drcl.Debug.error(this, "start()| " + e_, false);
				}
			}
		}
		else {
			// the thread is newly created
			setState(State_INACTIVE);
			super.start();
		}
	}
	
	final void setWakeupTime(double time_)
	{ wakeUpTime = time_; }
	
	final double getWakeupTime()
	{ return wakeUpTime; }
	
	/**
	 */
	protected final void __sleepOn(Object sleepOn_, String prestate_,
					String poststate_)
	{
		sleepOn = sleepOn_;
		if (prestate_ != null) {
			if (poststate_ == null) poststate_ = state;
			setState(prestate_);
		}
		synchronized (sleepOn) {
			try {
				sleepOn.wait();
			}
			catch (InterruptedException e_) { // worker runtime is being reset
				throw new WorkerThreadInterruptedException();
			}
		}
		if (poststate_ != null) setState(poststate_);
        sleepOn = null;
	}
	
	/**
	 * Standard Thread.run().
	 */
	public /*final*/ void run()
	{
        if (Thread.currentThread() != this) {
			throw new WorkerThreadException(
					"not run() by the owning thread.  Current thread:"
					+ Thread.currentThread() + "---owning thread:" + this);
		}
        
		try 
		{
			if (aruntime.debug &&
							aruntime.isDebugEnabledAt(ARuntime.Debug_THREAD))
				aruntime.println(ARuntime.Debug_THREAD, this, "THREAD STARTS");
			
			for (;;) {
				if (nextTask != null && (mainContext == null
										|| mainContext == DUMMY_CONTEXT)) {
					mainContext = nextTask;
					nextTask = null;
				}
				// error checking
				else if (nextTask != null) {
					drcl.Debug.systemFatalError(
						"Incorrectly assign task to occupied thread:" + this);
				}
					
				while (true) {
					synchronized (aruntime) {
						if (mainContext == null
										|| mainContext == DUMMY_CONTEXT) {
							// grab a task from worker pool
							nextTask = GETTING_TASK;
								// XXX: temporary for debug
							nextTask = aruntime.getTask(mainContext == null);
							if (nextTask != null) {
								//so null wont override DUMMY_CONTEXT
								mainContext = nextTask;
								nextTask = null;
							}
							else {
								setState(State_RECYCLING);
								sleepOn = this;
								aruntime.recycle(this);
									// return workforce in recycle()
							}
						}
					}

					if (state == State_RECYCLING) {
						setState(State_INACTIVE);
						// BOOKMARK1:
						// At this point, other threads may assign a new task
						// to this thread and start this thread, so check if
						// mainContext is being assigned within
						// the following synchronized claus
						synchronized (this) {
							if (mainContext == DUMMY_CONTEXT)
								mainContext = null;

							if (mainContext == null)
								__sleepOn(this, State_INACTIVE, State_INACTIVE);
							else
								sleepOn = null;
						}
					}
					else if (runtime.resetting
							|| !(mainContext instanceof TaskNotify))
						break; // the while loop
					else {
						// mainContext is TaskNotify:
						// the object to be notified on is in "data"
						synchronized (mainContext.data) {
							if (aruntime.debug && aruntime.isDebugEnabledAt(
														ARuntime.Debug_THREAD))
								aruntime.println(ARuntime.Debug_THREAD,
												this, "EXECUTING notify:"
												+ mainContext.data + ","
												+ System.currentTimeMillis());
							mainContext.data.notify();
							aruntime.nthreadsWaiting --;
						}
						mainContext = null;
							// workforce transfered to the waked up thread
					}

					/* 2004/02/16:
					 * the following is old code and produces a race condition
					 * during the time after this thread cannot get more tasks
					 * from runtime but before it goes to recycle() to return
					 * workforce, other threads may produce more tasks in the
					 * ready queue without this thread knowing it. 
					 * solution: getTask() and recycle() must happen atomically 
					 * like the new code above 
					if (mainContext == null || mainContext == DUMMY_CONTEXT) {
						// grab a task from worker pool
						nextTask = GETTING_TASK;
							// XXX: temporary for debug
						nextTask = aruntime.getTask(mainContext == null);
						if (nextTask != null) mainContext = nextTask;
							//so null wont override DUMMY_CONTEXT
						nextTask = null;
					}
					
					if (mainContext != null && mainContext != DUMMY_CONTEXT) {
						if (mainContext instanceof TaskNotify) {
							// the object to be notified on is in "data"
							synchronized (mainContext.data) {
								if (aruntime.debug && aruntime.isDebugEnabledAt(
														ARuntime.Debug_THREAD))
									aruntime.println(ARuntime.Debug_THREAD,
												this, "EXECUTING notify:"
												+ mainContext.data + ","
												+ System.currentTimeMillis());
								mainContext.data.notify();
								aruntime.nthreadsWaiting --;
							}
							mainContext = null;
								// workforce transfered to the waked up thread
						}
						else {
							//yield();
								// expensive; produce about 100% more of
								// the overhead
							//System.out.println("continue: cwf = "
							//	+ runtime.cwf + ", " + this + ", " + task_);
							break;
						}
					}
					else if (!runtime.resetting) {
						setState(State_RECYCLING);
						setState(State_INACTIVE);
						sleepOn = this;
							// important!
							// for other thread to wakeUp() after
							// runtime.recycle()
						aruntime.recycle(this);
							// return workforce in recycle()
						// BOOKMARK1:
						// At this point, other threads may assign a new task
						// to this thread and start this thread, so check if
						// mainContext is being assigned within
						// the following synchronized claus
						synchronized (this) {
							if (mainContext == DUMMY_CONTEXT)
								mainContext = null;

							if (mainContext == null)
								__sleepOn(this, State_INACTIVE, State_INACTIVE);
							else
								sleepOn = null;
						}
					}
					else {
						// runtime is being reset
						break;
					}
					*/
				} // while

				if (aruntime.debug &&
							aruntime.isDebugEnabledAt(ARuntime.Debug_THREAD))
					aruntime.println(ARuntime.Debug_THREAD, this,
									"EXECUTING:" + mainContext);

				synchronized (this) { // avoid racing with runtime.stop()
					if (aruntime.isSuspend())
						// go into sleep if runtime is suspended
						__sleepOn(this, State_PREACTIVE, State_ACTIVE);
					else if (runtime.resetting)
						throw new WorkerThreadInterruptedException();
				}
				
				setState(State_ACTIVE);
				
				//runtime.runCheck(this);
				String method_ = "process()";
				currentContext = mainContext;
				returnPort = currentContext.returnPort;
					// from server port, new context
				try {
					mainContext.execute(this);
				}
				catch (NullPointerException e_) {
					if (runtime == null) return;
					e_.printStackTrace();
					drcl.Debug.error(info());
				}
				catch (Exception e_) {
					e_.printStackTrace();
					drcl.Debug.error(info());
				}
				catch (SendReceiveException e_) {
					e_.printStackTrace();
					drcl.Debug.error(info());
				}
				currentContext.port = null;
				currentContext.data = null;
					
				finishing();
				mainContext = DUMMY_CONTEXT;
					// to maintain the ownership of workforce
				
				if (!RECYCLING) {
					aruntime.remove(this);
					break;
				}
			} // for(;;)
			drcl.Debug.systemFatalError("Unexpected finish at " + this);
		}
		catch (WorkerThreadInterruptedException e_) {
			//if (!runtime.resetting) {
			//	e_.printStackTrace();
			//	drcl.Debug.systemFatalError(
			//		"AWorkerThread terminates abnormally, " + this
			//			+ "\nManager: " + runtime.diag());
			//}
			// Will releaseAllLocks()/cancelAllWaits() clean up the lock
			// 	structure in components?
			// Not if the thread comes across multiple components...
			if (mainContext != null && mainContext.port != null)
				releaseAllLocks(mainContext.port.host);
			aruntime.remove(this);
		}
		catch (NullPointerException e_) {
			if (runtime != null) {
				e_.printStackTrace();
				drcl.Debug.error(info());
			}
		}
		runtime = null; // become an orphan
		aruntime = null;
	}
	
	//
	static void ___SUBCLASS_OVERRIDES___() {}
	//
	
	void _sleepUntil(double time_, String poststate_)
	{
		if (poststate_ == null) poststate_ = state;

		setWakeupTime(time_);
		// set the following two lines before calling
		// runtime.threadRequestsWaiting() to avoid racing
		// (see codes below)
		if (sleepOn == null) sleepOn = this;
			// for wait(Object, double), sleepOn is preset
		synchronized (sleepOn) {
			if (nextTask != null) _semanticsError("sleep()");
			if (aruntime.threadRequestsSleeping(this, time_)) {
				__sleepOn(sleepOn, State_SLEEPING, State_AWAKENING);
				//runtime.threadAwakeFromSleeping(this);
				if (aruntime.debug &&
								aruntime.isDebugEnabledAt(aruntime.Debug_Q))
					aruntime.println(aruntime.Debug_Q, this,
						"awaked from finite sleep, " + _currentContext());
			}
			setWakeupTime(Double.NaN);
		}
		setState(poststate_);
	}

	void _sleepOn(Object sleepOn_, String prestate_, String poststate_)
	{
		if (poststate_ == null) poststate_ = state;

		sleepOn = sleepOn_; 
		synchronized (sleepOn) {
			if (nextTask != null) _semanticsError("wait()");
			aruntime.threadBecomesWaiting(this);
			__sleepOn(sleepOn_, prestate_, State_AWAKENING);
			//runtime.threadAwakeFromWaiting(this);
			if (aruntime.debug && aruntime.isDebugEnabledAt(aruntime.Debug_Q))
				aruntime.println(aruntime.Debug_Q, this,
					"awaked from indefinite sleep: " + _currentContext());
		}
		setState(poststate_);
	}
	
	//protected void _awakened()
	//{}
	
	//
	static void ___API_FOR_COMPONENT___() {}
	//
	
	protected final void sleepFor(double time_)
	{
		//if (nextTask != null) _semanticsError("sleepFor(double)");
		_sleepUntil(runtime.getTime() + time_, State_AWAKENING);
		//_awakened();
		setState(State_ACTIVE);
	}
	
	protected final void sleepUntil(double time_)
	{
		//if (nextTask != null) _semanticsError("sleepUntil(double)");
		_sleepUntil(time_, State_AWAKENING);
		//_awakened();
		setState(State_ACTIVE);
	}

	/** Let go of the thread from sleep in idle. */
	public void kill()
	{
		if (isAlive())
			interrupt();
		else
			aruntime.remove(this);
	}
	
	//
	static void ___EVENTS___() {}
	// Identify the events to make it easier to do simulation extension.
	//
	
	protected void changeCurrentContext(Port port_, Object data_, String state_)
	{
		//mainContext = new Task(port_, data_, mainContext.type, 0.0);
		if (currentContext == mainContext || currentContext == null)
			currentContext = new TaskReceive(port_, data_);
		else {
			currentContext.data = data_;
			currentContext.port = port_;
		}
		if (state_ != null) state = state_;;
		if (aruntime.debug && aruntime.isDebugEnabledAt(ARuntime.Debug_THREAD))
			aruntime.println(ARuntime.Debug_THREAD, this, "currentContext ---> "
				+ "state:" + state + "," + _currentContext());
	}
	
	String _currentContext()
	{
		if (currentContext == null || currentContext.data == null
						&& currentContext.port == null)
			return "<null>";
		else
			return "PORT:" + currentContext.port + ",DATA:"
				+ drcl.util.StringUtil.toString(currentContext.data);
	}

	public final boolean isReadyForNextTask()
	{
		return (state == State_FINISHING || state == State_INACTIVE)
			&& nextTask == null;
	}
	
	//
	static void ___INFO___() {}
	//
	
	/*
	public final Component getHost()
	{ 
		return currentContext == null? null:
				(currentContext.port == null?
				 (currentContext.data instanceof Component?
				  (Component)currentContext.data: null):
				  currentContext.port.host);	
	}
	*/
	
	//
	static void ___SYNC_APIS___() {}
	//
	
	private void _semanticsError(String method_)
	{
		drcl.Debug.fatalError("Calling " + method_
				+ " after doLastSending()/finishing() is prohibited.\n"
				+ "Current_thread:" + this + "\n");
	}

	//String msg = "<nothing>";  // temporary
	//public String msg() { return msg; } // temporary
	
	public final String locks()
	{
		// XXX
		return "<not implemented yet>\n";
	}
	
	/*
	public static class LockPack implements Component.Locks
	{
		public Object target;
		public WorkerThread holder;
		public int counter; // # of times "holder" grabs the lock of "target"
		public int lockReqCount; // # of threads waiting for lock of "target"
		public int waitCount; // # of threads waiting on "target"
		public LockPack next;
		
		public LockPack(Object target_)
		{
			target = target_;
		}

		public String printAll()
		{
			LockPack p_ = this;
			StringBuffer sb_ = new StringBuffer();
			while (p_ != null) {
				sb_.append("   " + p_.target + ": " + p_.holder + ", count="
					+ p_.counter + ", waitCount=" + p_.waitCount + "\n");
				p_ = p_.next;
			}
			return sb_.toString();
		}

		public String toString()
		{ return "LockPack: " + target + ", holder=" + ", " + counter
				+ ", " + lockReqCount + ", " + waitCount; }
	}
	*/
	
	// store counterparts in LockPack that are reset when this thread
	// 'wait()' on something
	class WaitPack
	{
		Object target;
		//WorkerThread next; // next thread in waiting chain
		int counter;
		
		public WaitPack()
		{}
	}
	
	// Looking for LockPack in host.
	// Create one if necessary.
	private LockPack lookforLock(Component host_, Object o_)
	{
		LockPack p_ = null;
		//if (host_.locks != null) {
		//	p_ = (LockPack)host_.locks;
		//	while (p_ != null && p_.target != o_) p_ = p_.next;
		//}
		//if (p_ == null) {
			// First time the object is locked.
			// Search again in the synchronized claus to avoid racing.
			// Create one if it is still not there.
			synchronized (host_) {
				if (host_.locks != null) {
					p_ = (LockPack)host_.locks;
					while (p_ != null && p_.target != o_) p_ = p_.next;
				}
				if (p_ == null) {
					p_ = new LockPack(o_);
					p_.next = (LockPack)host_.locks;
					host_.locks = p_;
				}
			}
		//}
		return p_;
	}
	
	protected final void lock(Component host_, Object o_)
	{
		// looking for LockPack in host
		LockPack p_ = lookforLock(host_, o_);
		
		// No need to grab p_'s monitor if thread is already the lock holder.
		if (p_.holder == this) {
			p_.counter ++;
			return;
		}
		
		String old_ = state;
		synchronized (p_) {
			// the lock holder may be an orphan from previous run
			//if (p_.holder == null || p_.holder.isOrphan()) {
			if (p_.counter == 0
				|| (p_.holder != null && p_.holder.isOrphan())) {
				//thread becomes the lock holder	
				p_.holder = this;
				p_.counter = 1;
				return;
			}

			//if (nextTask != null) _semanticsError("lock(Object)");
		
			// Else:
			// 1. add thread itself to the waiting queue;
			// 2. going into sleep;
			// 3. getting the lock after awake;
		
			//p_.qWaiting.enqueue(this); 
			p_.lockReqCount ++;
			if (waitPack == null) waitPack = new WaitPack();
			waitPack.target = p_.target; // for debugging only
			_sleepOn(p_, State_LOCKING, null); // wait for lock
			waitPack.target = null;
			//p_.qWaiting.remove(this);
			if (isOrphan()) {
				p_.notify();
				return;
			}
			p_.holder = this;
			p_.counter = 1;
			//_awakened();
		}
	}
	
	protected final void unlock(Component host_, Object o_)
	{ unlock(host_, o_, false); }
	
	protected void unlock(Component host_, Object o_, boolean release_)
	{
		LockPack p_ = lookforLock(host_, o_);
		
		String old_ = state;
		setState(State_UNLOCKING);
		synchronized (p_) {
			if (release_ || --p_.counter < 0)
				p_.counter = 0;

			if (p_.counter == 0) {
				if (p_.lockReqCount == 0) {
					p_.holder = null;
					return;
				}
				// Note 1:
				// what if another thread grabs the lock before
				// the one being notified below?
				// p_.counter is not zero so other threads cannot just grab it
				p_.lockReqCount--;
				p_.counter = -1;
				p_.holder = null;
				synchronized (aruntime) {
					if (aruntime.getWorkforce()) {
						p_.notify(); // dont know who's going to wake up
						aruntime.nthreadsWaiting --;
					}
					else {
						aruntime.newTask(Task.createNotify(p_), this);
					}
				}
				//---
			}
		}
		setState(old_);
	}
	
	protected final void releaseAllLocks(Component host_)
	{
		String old_ = state;
		setState(State_UNLOCKING);
		for (LockPack p_ = (LockPack)host_.locks; p_ != null; p_ = p_.next)
			if (p_.holder == this) {
				//unlock(host_, p_.target, true);
				//below is copied from unlock()
				synchronized (p_) {
					p_.holder = null;
					p_.counter = -1;
					// p_.counter is not zero, see Note 1 above
					if (p_.lockReqCount == 0) {
						continue;
					}
					p_.lockReqCount--;
					synchronized (aruntime) {
						if (aruntime.getWorkforce()) {
							p_.notify(); // dont know who's going to wake up
							aruntime.nthreadsWaiting --;
						}
						else
							aruntime.newTask(Task.createNotify(p_), this);
					}
				}
			}
		setState(old_);
	}

	// Make thread wait on the object until being notified by another thread.
	protected final void wait(Component host_, Object o_)
	{
		//if (nextTask != null) _semanticsError("wait(Object)");
		// looking for LockPack in host
		LockPack p_ = lookforLock(host_, o_);
		
		String old_ = state;
		setState(State_WAITING);
		synchronized (o_) {
			// Records the status if the thread holds the lock.
			if (waitPack == null) waitPack = new WaitPack();
			waitPack.target = p_.target;
			if (p_.holder == this) {
				waitPack.counter = p_.counter;
				//unlock(host_, o_, true/* release all grabs */);
				//below is copied from unlock()
				synchronized (p_) {
					p_.holder = null;
					if (p_.lockReqCount == 0)
						p_.counter = 0;
					else {
						p_.counter = -1;
							// p_.counter is not zero, see Note 1 above
						p_.lockReqCount--;
						synchronized (aruntime) {
							if (aruntime.getWorkforce()) {
								p_.notify(); // dont know who's going to wake up
								aruntime.nthreadsWaiting --;
							}
							else
								aruntime.newTask(Task.createNotify(p_), this);
						}
					}
				}
			}
			p_.waitCount++;
			
			// waited to be notified
			_sleepOn(o_, State_WAITING, null);
	
			if (isOrphan())
				throw new WorkerThreadInterruptedException("Orphan thread");

			//_awakened();

			// re-grabbing the lock if necessary.
			if (waitPack.counter > 0) {
				synchronized (p_) {
					if (p_.holder != null)
						lock(host_, p_.target);
					else {
						p_.holder = this;
						p_.counter = waitPack.counter;
					}
				}
			}
		}
		
		if (p_ != null) {
			waitPack.counter = 0;
			waitPack.target = null;
		}
	}
			
	// Notifies the first thread waiting on the object.
	protected final void notify(Component host_, Object o_)
	{
		if (o_ == null) return;
		String old_ = state;
		setState(State_NOTIFYING);
		//sleepOn = o_;
		LockPack p_ = lookforLock(host_, o_);
		synchronized (o_) {
			//setState(State_NOTIFYING2);
			//sleepOn = null;
			if (p_.waitCount == 0) return;
			p_.waitCount--;
			synchronized (aruntime) {
				if (aruntime.getWorkforce()) {
					o_.notify(); // dont know who's going to wake up
					aruntime.nthreadsWaiting --;
				}
				else {
					aruntime.newTask(Task.createNotify(o_), this);
				}
			}
		}
		setState(old_);
	}
	
	// Notifies all the threads waiting on the object.
	protected final void notifyAll(Component host_, Object o_)
	{
		if (o_ == null) return;
		String old_ = state;
		setState(State_NOTIFYING);
		//sleepOn = o_;
		LockPack p_ = lookforLock(host_, o_);
		synchronized (o_) {
			//setState(State_NOTIFYING2);
			//sleepOn = null;
			if (p_.waitCount == 0) return;
			//runtime._notifyMany(o_, p_.waitCount, this);
			synchronized (aruntime) {
				if (aruntime.getWorkforce(p_.waitCount)) {
					o_.notifyAll(); // dont know who's going to wake up
					aruntime.nthreadsWaiting -= p_.waitCount;
				}
				else {
					int remainingWorkforce_ = aruntime.getRemainingWorkforce();
					// notify "remainingWorkforce_" threads
					for (int i=0; i<remainingWorkforce_; i++) o_.notify();
					aruntime.nthreadsWaiting -= remainingWorkforce_;
					for (int i=p_.waitCount-remainingWorkforce_; i>0; i--)
						aruntime.newTask(Task.createNotify(o_), this);
				}
			}
			p_.waitCount = 0;
		}
		setState(old_);
	}
	
	/**
	 * Returns the thread that is holding the lock of the target object.
	 * The returned thread may be an orphan thread from previous run.
	final WorkerThread getHolder(Object o_)
	{
		Component host_ = getHost();
		if (host_ == null) return null;
		LockPack p_ = lookforLock(host_, o_);
		return p_ == null? null: p_.holder;
	}
	 */
	
	protected void yieldToRuntime()
	{
		synchronized (this) { // avoid racing with runtime.stop()
			if (aruntime.isSuspend())
				// go into sleep if runtime is suspended
				__sleepOn(this, State_PREACTIVE, state);
			else if (runtime.resetting)
				throw new WorkerThreadInterruptedException();

			aruntime.newTask(Task.createNotify(this), this);
			_sleepOn(this, State_YIELD, null);
		}
	}

	//
	static void ___STATE___() {}
	//
	
	protected final void setState(String new_)
	{
		if (state == new_) return;
		
		if (aruntime.debug) aruntime.threadStateChange(this, state, new_);
		
		//if (stateQ.isFull()) stateQ.dequeue(); // temporary
		//stateQ.enqueue(state); // temporary
		state = new_;
	}

	// for ARuntime to stop()
	final boolean isActive()
	{
		return !(state == State_INACTIVE || state == State_SLEEPING
			|| state == State_PREACTIVE || state == State_AWAKENING_WAITING
			|| state == State_LOCKING || state == State_WAITING
			|| state == State_YIELD);
	}

	// for ARuntime to resume()
	final boolean isInActive()
	{
		return state == State_INACTIVE || state == State_PREACTIVE;
	}

	// XX: Transient states, for debugging purpose.

	/** State of acquiring a lock. */
	static final String State_LOCKING	= "LOCKING";
	// state of performing recycling tasks before runtime.recycle()
	static final String State_RECYCLING	= "RECYCLING";
	// state of sleep before active due to suspended runtime
	static final String State_PREACTIVE		= "PREACTIVE";
	// state of being awakened, but waiting for available workforce.
	static final String State_AWAKENING_WAITING	= "AWAKENING-WAITING";
	// state of notifying on an object
	static final String State_NOTIFYING	= "NOTIFYING(debug)";
	static final String State_NOTIFYING2= "NOTIFYING2(debug)";
	// state of being awakened, re-grabbing workforce.
	static final String State_AWAKENING	= "AWAKENING";
	// state of releasing a lock
	static final String State_UNLOCKING	= "UNLOCKING(debug)";
	// state of yielding to other threads
	static final String State_YIELD	= "YIELD(debug)";
}
