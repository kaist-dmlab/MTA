// @(#)SEThreadOld.java   9/2002
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

package drcl.sim.event;

import java.util.*;
import java.beans.*;
import drcl.comp.*;
import drcl.util.queue.FIFOQueue;
import drcl.util.StringUtil;

/**
 * @see SESimulatorOld
 */
public class SEThreadOld extends WorkerThread
{
	static boolean RECYCLING = true; // thread recycling?

	/** The worker runtime of this thread. */
	protected SESimulatorOld aruntime;
	
	/** The object that this thread waits on. */
	transient protected Object sleepOn = null;
	
	WaitPack waitPack = null; // for debug
	double wakeupTime = Double.NaN; // informational

	// for debug
	//FIFOQueue stateQ = new FIFOQueue(10); // temporary

	public SEThreadOld()
	{ super(); }
	
	public SEThreadOld(String name_)
	{ super(name_);}
	
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
			+ (state == State_SLEEPING? " until " + wakeupTime: "") + "\n"
			+ prefix_ + "Context=" + mainContext
			+ (mainContext == null? ", ": "\n" + prefix_));
		if (returnPort != null)
			sb_.append(prefix_ + "return_port: " + returnPort + "\n");
		return sb_.toString();
	}

	public String _toString()
	{
		if (runtime == null)
			return StringUtil.lastSubstring(super.toString(), ".") + ","
				+ "<orphan>";
		else
			return StringUtil.lastSubstring(super.toString(), ".") + "--"
			   + state
			   + ", current_context = " + _currentContext()
			   + (returnPort == null? "": "--return_port=" + returnPort);
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
			if (aruntime.debug)
				aruntime.println(getName() + " STARTS");
			

			setState(State_ACTIVE);
			for (;;) {

				// avoid racing with aruntime.newTask() from another thread
				synchronized (aruntime) {
					// check suspension before getting task because it may
					// be the previous task that stops the runtime
					if (aruntime.suspended) {
						// go into sleep if runtime is suspended
						setState(State_PREACTIVE);
						aruntime.notify(); // notify stopping thread
						try { aruntime.wait(); } catch (Exception e_) {}
						setState(State_ACTIVE);
					}

					mainContext = aruntime.getTask();

					if (mainContext == null) {
						aruntime.threadRetired();
						break;
					}
					else if (mainContext instanceof TaskNotify) {
						aruntime.totalNumEvents += totalNumEvents;
						totalNumEvents = 0;
						// the object to be notified on is in "data"
						synchronized (mainContext.data) {
							mainContext.data.notify();
						}
						if (aruntime.debug)
							aruntime.println(this + " RETIRED");
						break;
					}
					
					if (aruntime.resetting) {
						aruntime.notify();
						throw new WorkerThreadInterruptedException();
					}
				}

				if (aruntime.debug)
					aruntime.println(getName() + " EXECUTING: "
									+ _currentContext());

				String method_ = "process()";
				returnPort = mainContext.returnPort;
					// from server port, new context
				try {
					mainContext.execute(this);
				}
				catch (NullPointerException e_) {
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
			} // for(;;)
		}
		catch (WorkerThreadInterruptedException e_) {
			//if (!runtime.resetting) {
			//	e_.printStackTrace();
			//	drcl.Debug.systemFatalError(
			//		"SEThreadOld terminates abnormally, " + this
			//		+ "\nManager: " + runtime.diag());
			//}
			// Will releaseAllLocks()/cancelAllWaits() clean up
			// 	the lock structure in components?
			// Not if the thread comes across multiple components...
		}
		catch (NullPointerException e_) {
			if (runtime != null) {
				e_.printStackTrace();
				drcl.Debug.error(info());
			}
		}

		state = State_INACTIVE;
		runtime = null; // become an orphan
		aruntime = null;
	}
	
	//
	static void ___API_FOR_COMPONENT___() {}
	//
	
	public final void sleepFor(double time_)
	{
		String old_ = state;
		setState(State_SLEEPING);
		wakeupTime = aruntime.time + time_;
		aruntime.newTask(Task.createNotify(this, wakeupTime), null);
		synchronized (this) {
			aruntime.totalNumEvents += totalNumEvents;
			totalNumEvents = 0;
			aruntime.startNewThread();
			try { this.wait(); } catch (Exception e_) {}
			aruntime.mainThread = this; // reclaim as mainThread
		}
		wakeupTime = Double.NaN;
		setState(old_);
	}
	
	public final void sleepUntil(double time_)
	{
		String old_ = state;
		setState(State_SLEEPING);
		wakeupTime = time_;
		aruntime.newTask(Task.createNotify(this, time_), null);
		synchronized (this) {
			aruntime.totalNumEvents += totalNumEvents;
			totalNumEvents = 0;
			aruntime.startNewThread();
			try { this.wait(); } catch (Exception e_) {}
			aruntime.mainThread = this; // reclaim as mainThread
		}
		wakeupTime = Double.NaN;
		setState(old_);
	}

	//
	private void ___EVENTS___() {}
	// Identify the events to make it easier to do simulation extension.
	//
	
	/*
	protected void changeCurrentContext(Port port_, Object data_, String state_)
	{
		mainContext.data = data_;
		mainContext.port = port_;
		if (state_ != null) state = state_;;
		if (aruntime.debug)
			aruntime.println("currentContext ---> " + "state:" + state + ","
				+ _currentContext());
	}
	*/
	
	String _currentContext()
	{
		return "PORT:" + mainContext.port + ",DATA:"
			+ drcl.util.StringUtil.toString(mainContext.data);
	}

	
	//
	private void ___SYNC_APIS___() {}
	//


	// store counterparts in LockPack that are reset when this thread
	// 'wait()' on something
	class WaitPack
	{
		Object target;
		int counter;
		
		public WaitPack()
		{}
	}
	
	// Looking for LockPack in host.
	// Create one if necessary.
	private LockPack lookforLock(Component host_, Object o_)
	{
		LockPack p_ = null;
		if (host_.locks != null) {
			p_ = (LockPack)host_.locks;
			while (p_ != null && p_.target != o_) p_ = p_.next;
		}
		if (p_ == null) {
			p_ = new LockPack(o_);
			p_.next = (LockPack)host_.locks;
			host_.locks = p_;
		}
		return p_;
	}
	
	protected final void lock(Component host_, Object o_)
	{
		// looking for LockPack in host
		LockPack p_ = lookforLock(host_, o_);
		
		// No need to grab p_'s monitor if thread is already the lock holder.
		if (p_.holder == this)
			p_.counter ++;
		//else if (p_.holder == null) {
		else if (p_.counter == 0) {
			//thread becomes the lock holder	
			p_.holder = this;
			p_.counter = 1;
		}
		else {
			// wait until lock is released
			p_.lockReqCount ++;
			if (waitPack == null) waitPack = new WaitPack();
			waitPack.target = p_.target; // for debugging only
			synchronized (p_) {
				// wait to be notified
				aruntime.totalNumEvents += totalNumEvents;
				totalNumEvents = 0;
				aruntime.startNewThread();
				String prestate_ = state;
				setState(State_LOCKING);
				try { p_.wait(); } catch (Exception e_) {}
				setState(prestate_);
				aruntime.mainThread = this; // reclaim as mainThread
			}
			waitPack.target = null;
			p_.holder = this;
			p_.counter = 1;
		}
	}
	
	protected void unlock(Component host_, Object o_, boolean release_)
	{
		LockPack p_ = lookforLock(host_, o_);
		
		if (release_ || --p_.counter < 0)
			p_.counter = 0;

		if (p_.counter == 0) {
			p_.holder = null;
			if (p_.lockReqCount == 0)
				return;
			// Note 1:
			// what if another thread grabs the lock before
			// the one being notified below?
			// p_.counter is not zero so other threads cannot just grab it
			p_.counter = -1;
			p_.lockReqCount--;
			aruntime.newTask(Task.createNotify(p_), this);
		}
	}
	
    protected void releaseAllLocks(Component host_)
	{
		for (LockPack p_ = (LockPack)host_.locks; p_ != null; p_ = p_.next)
			if (p_.holder == this) {
				//unlock(host_, p_.target, true);
				p_.holder = null;
				if (p_.lockReqCount > 0) {
					p_.lockReqCount--;
					p_.counter = -1;
					aruntime.newTask(Task.createNotify(p_), this);
					// p_.holder is not nulled, see Note 1 above
				}
				else {
					p_.counter = 0;
				}
			}
	}

	// Make thread wait on the object until being notified by another thread.
	protected final void wait(Component host_, Object o_)
	{
		String old_ = state;
		LockPack p_ = lookforLock(host_, o_);
		
		setState(State_WAITING);
		// Records the status if the thread holds the lock.
		if (waitPack == null) waitPack = new WaitPack();
		waitPack.target = p_.target;
		if (p_.holder == this) {
			waitPack.counter = p_.counter;
			p_.holder = null;
			//unlock(host_, o_, true); // true: release all grabs 
			if (p_.lockReqCount > 0) {
				p_.lockReqCount--;
				p_.counter = -1;
				aruntime.newTask(Task.createNotify(p_), this);
				// p_.counter is not zero, see Note 1 above
			}
			else
				p_.counter = 0;
		}
		p_.waitCount++;
			
		synchronized (o_) {
			// wait to be notified
			aruntime.totalNumEvents += totalNumEvents;
			totalNumEvents = 0;
			aruntime.startNewThread();
			try { o_.wait(); } catch (Exception e_) {}
			aruntime.mainThread = this; // reclaim as mainThread
		}
	
		// re-grabbing the lock if necessary.
		if (waitPack.counter > 0) {
			if (p_.holder != null)
				lock(host_, p_.target);
			else
				p_.holder = this;
				p_.counter = waitPack.counter;
		}
		
		waitPack.counter = 0;
		waitPack.target = null;
		setState(old_);
	}
			
	// Notifies the first thread waiting on the object.
	protected void notify(Component host_, Object o_)
	{
		if (o_ == null) return;
		LockPack p_ = lookforLock(host_, o_);
		if (p_.waitCount == 0) return;
		p_.waitCount--;
		aruntime.newTask(Task.createNotify(o_), this);
	}
	
	// Notifies all the threads waiting on the object.
	protected final void notifyAll(Component host_, Object o_)
	{
		if (o_ == null) return;
		LockPack p_ = lookforLock(host_, o_);
		for (; p_.waitCount>0; p_.waitCount--)
			aruntime.newTask(Task.createNotify(o_), this);
		p_.waitCount = 0;
	}

	protected void yieldToRuntime()
	{
		synchronized (aruntime) {
			if (aruntime.suspended) {
				// go into sleep if runtime is suspended
				setState(State_PREACTIVE);
				aruntime.notify(); // notify stopping thread
				try { aruntime.wait(); } catch (Exception e_) {}
				setState(State_ACTIVE);
			}
			if (runtime.resetting) {
				aruntime.notify(); // notify resetting thread
				throw new WorkerThreadInterruptedException();
			}
		}

		synchronized (this) {
			aruntime.newTask(Task.createNotify(this), this);
			aruntime.totalNumEvents += totalNumEvents;
			totalNumEvents = 0;
			aruntime.startNewThread();
			try { this.wait(); } catch (Exception e_) {}
			aruntime.mainThread = this; // reclaim as mainThread
		}
	}
	
	//
	private void ___STATE___() {}
	//
	
	protected final void setState(String new_)
	{
		if (state == new_) return;
		
		//if (aruntime.debug) aruntime.threadStateChange(this, state, new_);
		
		//if (stateQ.isFull()) stateQ.dequeue(); // temporary
		//stateQ.enqueue(state); // temporary
		state = new_;
	}

	// XX: Transient states, for debugging purpose.

	/** State of acquiring a lock. */
	static final String State_LOCKING	= "LOCKING";
	// state of sleep before active due to suspended runtime
	static final String State_PREACTIVE		= "PREACTIVE";
}
