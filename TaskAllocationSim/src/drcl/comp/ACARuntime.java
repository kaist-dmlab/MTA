// @(#)ACARuntime.java   2/2004
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
import drcl.util.queue.*;


/** Defines the interface of an ACA runtime. */
public abstract class ACARuntime extends ForkManager
{
	public static final ACARuntime DEFAULT_RUNTIME = new ARuntime("default");

	/** State of no thread running or waiting to be started. */
	public static final String State_INACTIVE       = "INACTIVE";
	/** State of have threads running. */
	public static final String State_RUNNING        = "RUNNING";
	/** State of system suspended. */
	public static final String State_SUSPENDED      = "SUSPENDED";

	public boolean debug = false;
	protected String name; // thread group 
	protected String state = State_INACTIVE;
	protected double timeScale = 1.0e3; 
	protected double timeScaleReciprocal = 1.0 / timeScale;

	// hooks
	LinkedList stopHookList = new LinkedList();
	LinkedList suspendHookList = new LinkedList();
	LinkedList runHookList = new LinkedList();

	/** Whether the runtime is in resetting or not.	 */
	transient public boolean resetting = false;
		
	static int RUNTIME_COUNTER = 0;

	public ACARuntime()
	{ this("default"); }
	
	public ACARuntime(String name_)
	{ name = name_ + RUNTIME_COUNTER++; }

	public String toString()
	{ return name; }
	
	public void takeover(Object[] oo_) 
	{
		if (oo_ == null) return;
		for (int i=0; i<oo_.length; i++)
			if (oo_[i] instanceof Component)
				takeover((Component)oo_[i]);
	}
	
	public void takeover(Component c_) 
	{ c_.setRuntime(this); }

	//
	private void ___TASK_MANAGEMENT___() {}
	//

	// Task?
	/**
	 * The only way to trigger new tasks to be executed.
	 */
	protected abstract void newTask(Task task_, WorkerThread current_);

	// starts multiple tasks atomicly
	void newTasks(Vector vtasks_)
	{
		newTask(new TaskSpecial(new MultipleTasks(vtasks_),
								Task.TYPE_RUNNABLE, 0.0), null);
	}

	class MultipleTasks implements Runnable {
		Vector vtasks;
		MultipleTasks(Vector v_)
		{ vtasks = v_; }

		public void run()
		{
			if (vtasks != null)
				for (int i=0; i<vtasks.size(); i++)
					newTask((Task)vtasks.elementAt(i), null);
		}

		public String toString()
		{ return vtasks == null? "<none>": vtasks.toString(); }
	}

	public boolean logenabled = false;
	protected static java.io.FileWriter tf;

	protected void _openlog()
	{
		try {tf = new java.io.FileWriter("runtime.log");}
		catch (Exception e_) {}
	}

	//
	private void ___PROPERTIES___() {}
	//
	
	/** Time scale is the ratio of wall time over system time. */
	public void setTimeScale(double e_) 
	{ 
		timeScale = e_ * 1.0e3; 
		timeScaleReciprocal = 1.0 / timeScale;
	}
	public double getTimeScale() { return timeScale / 1.0e3; }
	
	public void setName(String name_) { name = name_; }
	public String getName() { return name == null? super.toString(): name; }

	public void setMaxWorkforce(int maxwf_)
	{}
	
	/** Returns general information of this runtime. */
	public abstract String info();
	
	/** Returns more detailed information of this runtime, for diagnosis 
	 * purpose. */
	public String diag()
	{	return diag(false);	}
	
	/** Returns more detailed information of this runtime, for diagnosis 
	 * purpose. */
	public synchronized String diag(boolean listWaitingTasks_)
	{	return a_info(listWaitingTasks_);	}
	
	/** Asynchronous version of {@link #diag()}. */
	public String a_info()
	{ return a_info(false); }

	/** Asynchronous version of {@link #diag(boolean)}. */
	public abstract String a_info(boolean listWaitingTasks_);

	public synchronized final void addRunnable(double later_, Runnable task_)
	{
		newTask(new TaskSpecial(task_, Task.TYPE_RUNNABLE, _getTime() + later_),
						null);
	}
	
	public synchronized final void addRunnableAt(double time_, Runnable task_)
	{
		newTask(new TaskSpecial(task_, Task.TYPE_RUNNABLE, 
								Math.max(time_, _getTime())), null);
	}
	
	public synchronized final void addRunnable(double later_, Port port_, 
					Runnable task_)
	{
		newTask(new TaskSpecial(port_, task_, Task.TYPE_RUNNABLE, 
								_getTime() + later_), null);
	}
	
	public synchronized final void addRunnableAt(double time_, Port port_, 
					Runnable task_)
	{
		newTask(new TaskSpecial(port_, task_, Task.TYPE_RUNNABLE, 
								Math.max(time_, _getTime())), null);
	}

	/** Adds the "stop" hook to the simulator. 
	 * The hook is called when the runtime runs out of events. */
	public synchronized final void addStopHook(Runnable hook_)
	{ stopHookList.add(hook_); }

	/** Removes the "stop" hook. */
	public synchronized final void removeStopHook(Runnable hook_)
	{ stopHookList.remove(hook_); }

	/** Removes all "stop" hooks. */
	public synchronized final void removeAllStopHooks()
	{ stopHookList.clear(); }

	/** Adds the "suspend" hook to the simulator. 
	 * The hook is called when the runtime is suspended. */
	public synchronized final void addSuspendHook(Runnable hook_)
	{ suspendHookList.add(hook_); }

	/** Removes the "suspend" hook. */
	public synchronized final void removeSuspendHook(Runnable hook_)
	{ suspendHookList.remove(hook_); }

	/** Removes all "suspend" hooks. */
	public synchronized final void removeAllSuspendHooks()
	{ suspendHookList.clear(); }

	/** Adds the "run" hook to the simulator. 
	 * The hook is called when the runtime starts to run or is resumed. */
	public synchronized final void addRunHook(Runnable hook_)
	{ runHookList.add(hook_); }

	/** Removes the "run" hook. */
	public synchronized final void removeRunHook(Runnable hook_)
	{ runHookList.remove(hook_); }

	/** Removes all "run" hooks. */
	public synchronized final void removeAllRunHooks()
	{ runHookList.clear(); }

	/** Executes the "stop" hooks in sequence. 
	 *  Subclasses must execute this when running out of events.
	 */
	protected final void runStopHooks()
	{
		for (Iterator it_ = stopHookList.iterator(); it_.hasNext(); )
			((Runnable)it_.next()).run();
	}

	/** Executes the "suspend" hooks in sequence. 
	 *  Subclasses must execute this when running out of events.
	 */
	protected final void runSuspendHooks()
	{
		for (Iterator it_ = suspendHookList.iterator(); it_.hasNext(); )
			((Runnable)it_.next()).run();
	}

	/** Executes the "run" hooks in sequence. 
	 *  Subclasses must execute this when running out of events.
	 */
	protected final void runRunHooks()
	{
		for (Iterator it_ = runHookList.iterator(); it_.hasNext(); )
			((Runnable)it_.next()).run();
	}

	/** Returns the number of arrival events. */
	public abstract long getNumberOfArrivalEvents();
	
	/** Returns the event processing rate of this runtime. */
	public abstract double getEventRate();

	//
	private void ___EXECUTION_CONTROL___() {}
	//
	
	/** Returns the current thread context. */
	protected WorkerThread getThread()
	{
		Thread tmp_ = Thread.currentThread();
		if (tmp_ instanceof WorkerThread)
			return (WorkerThread)tmp_;
		else
			return null;
	}

	/** Returns the system current time in second. */
	public double getTime()
	{ return _getTime(); }

	/** Asynchronized version of getTime(), for diagnosis.
	 *  Subclasses should override this method to provide its own
	 *  time mapping function. */
	protected abstract double _getTime();

	/** Stops the system.
	 * The state must be SUSPENDED when the method returns.
	 */
	public final void stop()
	{
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread t_ = (WorkerThread)Thread.currentThread();
			if (t_.runtime == this) _stop(false);
			else _stop(true);
		}
		else
			_stop(true);
	}

	protected abstract void _stop(boolean block_);

	/**
	 * Resumes the system.
	 * The state must advance to RUNNING when this method returns.
	 */
	public abstract void resume();
		
	/** Stops the system at the time specified later.  */
	public synchronized final void stop(double later_)
	{
		if (later_ <= 0.0)
			_stop(true); // blocked
		else
			newTask(new TaskSpecial(stopExe, Task.TYPE_RUNNABLE, 
									_getTime() + later_), null);
	}
	
	/** Stops the system at the time specified. */
	public synchronized final void stopAt(double time_)
	{
		if (time_ <= _getTime())
			_stop(true); // blocked
		else
			newTask(new TaskSpecial(stopExe, Task.TYPE_RUNNABLE, 
									Math.max(time_, _getTime())), null);
	}
	
	/**
	 * Resumes the system and lets it run for the time duration specified.
	 * The state must advance to RUNNING when this method returns.
	 */
	public synchronized final void resumeFor(double later_)
	{
		double time_ = _getTime() + later_;
		stopAt(time_);
		resume();
	}
	
	/**
	 * Resumes the system and lets it run for the time duration specified.
	 * The state must advance to RUNNING when this method returns.
	 */
	public synchronized final void resumeTo(double time_)
	{
		stopAt(time_);
		resume();
	}
	
	public abstract void reset();

	/** Returns the actual time (in ms) for which this runtime has 
	 * participated. */
	public abstract long getWallTimeElapsed();

	public String t_info()
	{ return t_info(""); }

	protected abstract String t_info(String prefix_);

	// XXX: reboot? reset?
	public void reboot()
	{ reset(); }
	
	protected transient Vector vStateListener = null;
				  
	public synchronized void addStateListener(
					java.beans.PropertyChangeListener l_)
	{
		if (vStateListener== null) vStateListener = new java.util.Vector();
		if (vStateListener.indexOf(l_) < 0) {
			vStateListener.addElement(l_);
			l_.propertyChange(new PropertyChangeEvent(this, "State", null,
									state));
		}
	}
	
	public synchronized void removeStateListener(
					java.beans.PropertyChangeListener l_)
	{
		if (vStateListener != null && vStateListener.indexOf(l_) < 0)
		{
			vStateListener.removeElement(l_);
			if (vStateListener.size() == 0) vStateListener = null; 
		}
	}
	
	public PropertyChangeListener[] getAllStateListeners()
	{
		PropertyChangeListener[] ll_ = 
			new PropertyChangeListener[vStateListener == null?
				0: vStateListener.size()];
		if (vStateListener != null) vStateListener.copyInto(ll_);
		return ll_;
	}
	
	public void notifyStateListeners(PropertyChangeEvent e_)
	{
		if (vStateListener != null) 
			for (int i=0; i<vStateListener.size(); i++) 
				((PropertyChangeListener)vStateListener.elementAt(i)).
					propertyChange(e_);
	}
	
	public String getState() { return state; }
	protected void setState(String new_)
	{
		if (state == new_) return;
		
		// notify listeners
		if (vStateListener != null) 
			notifyStateListeners(new PropertyChangeEvent(this, "State", 
									state, new_));

		state = new_;
	}
	
	/** Returns true if the runtime is running. */
	public boolean isRunning() 
	{	return state == State_RUNNING;	}
	
	/** Returns true if the runtime is either inactive or suspended. */
	public boolean isStopped()
	{ return state == State_SUSPENDED || state == State_INACTIVE; }
	
	/** Returns true if the runtime is suspended. */
	public boolean isSuspended()
	{ return state == State_SUSPENDED; }
	
	/** Returns true if the runtime is stopped (inactive or suspended)
	 * or running but all working threads are waiting. */
	public boolean isIdle()
	{ return state == State_SUSPENDED || state == State_INACTIVE; }
	
	Runnable stopExe = new Runnable() {
		public void run()
		{ _stop(false/* dont block */); }

		public String toString()
		{ return "StopExe"; }
	};
	
	
	//
	private void ___TRACE___() {}
	//
	
	protected drcl.util.TraceManager tr = new drcl.util.TraceManager();

	public String tr_info()
	{ return tr.toString(); }

	public void setDebugEnabled(boolean enabled_)
	{ debug = enabled_; }

	public boolean isDebugEnabled()
	{ return debug; }
	
	public boolean isDebugEnabledAt(String which_)
	{ return debug && tr.isTraceEnabledAt(which_); }
	
	public String[] getAllDebugLevels()
	{ return tr.getAllTraces(); }

	public void setDebugLevels(String[] traces_)
	{ tr.setTraces(traces_); }

	public void addDebugLevel(String trace_)
	{ tr.addTrace(trace_); }

	public void removeDebugLevel(String trace_)
	{ tr.removeTrace(trace_); }

	public boolean containsDebugLevel(String trace_)
	{ return tr.containsTrace(trace_); }

	public void setDebugEnabledAt(String level_, boolean enabled_)
	{
		if (level_ == null) return;
		String[] ss_ = drcl.util.StringUtil.substrings(level_);
		for (int i=0; i<ss_.length; i++)
			tr.setTraceEnabledAt(ss_[i], enabled_);
	}

	public String debug_info()
	{ return "Debug enabled: " + debug + "\n" + tr.info(); }

	protected void finalized()
	{ reset(); }

	//
	// ForkManager API
	//

	protected synchronized ACATimer receive(Port p_, Object evt_, 
					double duration_)
	{
		Task task_ = new TaskReceive(p_, evt_, 
						duration_ <= 0.0? 0.0: duration_ + _getTime());
		newTask(task_, null);
		return task_;
	}
	
	// synchronized is not necessary here as newTask() has it
	protected ACATimer receiveAt(Port p_, Object evt_, double time_)
	{
		Task task_ = new TaskReceive(p_, evt_, time_);
		newTask(task_, null);
		return task_;
	}
	
	protected synchronized ACATimer send(Port p_, Object evt_, double duration_)
	{
		Task task_ = new TaskSend(p_, evt_, 
						duration_ <= 0.0? 0.0: duration_ + _getTime());
		newTask(task_, null);
		return task_;
	}
	
	// synchronized is not necessary here as newTask() has it
	protected ACATimer sendAt(Port p_, Object evt_, double time_)
	{
		Task task_ = new TaskSend(p_, evt_, time_);
		newTask(task_, null);
		return task_;
	}

	// synchronized is not necessary here as newTask() has it
	protected void childManager(ForkManager child_, double time_)
	{ newTask(new TaskFork(child_, time_), null); }
	
	protected void process(WorkerThread current_, double now_)
	{ drcl.Debug.error("ACARuntime.process(WorkerThread) should not be invoked"); }

	public ForkManager getParent()
	{ return this; }

	public void setParent(ForkManager parent_)
	{ drcl.Debug.error("ACARuntime.setParent(ACARuntime) should not be invoked"); }

	public void setRuntime(ACARuntime runtime_)
	{
		if (runtime_ != this)
			drcl.Debug.error("ACARuntime.setRuntime(ACARuntime) should not be invoked");
	}

	protected abstract void off(ACATimer handle_);

	public abstract Object getEventQueue();
}
