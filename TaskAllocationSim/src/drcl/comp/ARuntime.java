// @(#)ARuntime.java   2/2004
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
import drcl.util.queue.Queue;


/**
 * A realization of ACA runtime.
 * @see AWorkerThread
 */
public class ARuntime extends ACARuntime
{
	public static final String Debug_THREAD         = "THREAD";
	public static final String Debug_THREAD_STATE   = "THREAD_STATE";
	public static final String Debug_WORKFORCE      = "WF";
	public static final String Debug_Q              = "Q";
	public static final String Debug_RECYCLE        = "RECYCLE";
	public static final String Debug_STATE          = "STATE";
	
	/** Transitional state to SUSPENDED. */
	public static final String State_SUSPENDING     = "SUSPENDING";
	/** Transitional state to INACTIVE when system is being reset. */
	public static final String State_RESETTING      = "RESETTING";

	// amount of work force
	// wf is the workforce assigned to the runtime (always >= 0.0)
	// -cwf (yes, the minus sign) is the workforce being used (cwf <= 0.0 always)
	protected int wf = Integer.MAX_VALUE, cwf = 0;
	
	// the pool
	protected FIFOQueue threadPool = new FIFOQueue();
	
	// for debug, stored the AWorkerThreads that are out working,
	// either haven't finished or never returned.
	Vector vWorking = new Vector();
	
	// statistics
	protected int total = 0; // number of threads created
	protected long totalThreadRequests = 0;
	protected long startTime = 0; // to calculate "grab rate"
	protected long ltime = 0; // current time in long (ms)
	protected double time = 0.0; // current time in double (s) to record the time when pool is suspended.
	protected ThreadGroup threadGroup;
	
	/** Used to store tasks. */
	protected FIFOQueue qReady = new FIFOQueue();

	private transient int nthreads = 0; // for debug
	private transient AWorkerThread thread; // for debug
	protected WakeupThread wakeupThread;
	protected int nthreadsWaiting = 0;
	int maxlength = 0; // max occupancy in waiting queue in history 

	public ARuntime()
	{ this("default"); }
	
	public ARuntime(String name_)
	{	
		super(name_);	
		threadGroup = new ThreadGroup(getName());
		threadGroup.setMaxPriority(Thread.MIN_PRIORITY + 2);
		wakeupThread = new WakeupThread("WK_ " + name_);
		wakeupThread.start();
	}

	//
	void ___TASK_MANAGEMENT___() {}
	//
	/*
	public void addRunnable(double delay_, Runnable task_)
	{
		if (pool == null) pool = WorkerPool.defaultPool;
		pool.addRunnable(delay_, task_);
	}
	*/

	private String currentThread(AWorkerThread wt_)
	{
		Thread t_ = null;
		if (wt_ == null) {
			t_ = Thread.currentThread();
			if (t_ instanceof AWorkerThread)
				wt_ = (AWorkerThread)t_;
		}
		return wt_==null? t_.toString(): wt_._getName();
	}

	/**
	 * The only way to trigger new tasks to be executed.
	 */
	protected synchronized void newTask(Task task_, WorkerThread currentThread_)
	{
		if (resetting || task_ == null) return;
		
		
		//System.out.println("New task: " + task_ + ", current thread = " + current_);
		AWorkerThread current_ = (AWorkerThread)currentThread_;

		if (debug && current_ != null)
			System.out.println(_getTime() + ": qReady.length=" + qReady.getLength()
					+ ", current=" + current_.state);
		if (current_ != null && !current_.isReadyForNextTask())
			current_ = null;
		
		// XXX: qReady has unlimited buffer for now...
		
		// to figure in the processing delay in the runtime
		if (task_.time > _getTime()) {
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, current_, "to waiting-queue:" + task_);
			wakeupThread.Q.enqueue(task_.time, task_);
			if (logenabled) {
				if (tf == null) _openlog();
				try{tf.write(("+ " + task_.time + "\t\t" + wakeupThread.Q.getLength() + "\n").toCharArray()); tf.flush();} catch (Exception e_){}
			}
			if (maxlength < wakeupThread.Q.getLength()) maxlength = wakeupThread.Q.getLength();
			if (timeScaleReciprocal > 0.0 && wakeupThread.Q.firstElement() == task_)
				triggerWakeupThread();
			if (state == State_INACTIVE)
				_startAll(null);
			return;
		}
			
		// assign next task if the current thread is finishing
		// this execution path does not require threads to sync;
		// the best path one can expect.
		if (current_ != null && qReady.isEmpty()) {
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, current_, "Assign task:" + task_ + " to current thread");
			current_.nextTask = task_;
			return;
		}

		synchronized (this) {
			if (!qReady.isEmpty() || (current_ == null && !getWorkforce())) {
				if (debug && isDebugEnabledAt(Debug_THREAD))
					println(Debug_THREAD, current_, "Enqueue task:" + task_);
				qReady.enqueue(task_);
				return;
			}
			
			if (current_ != null) {
				if (debug && isDebugEnabledAt(Debug_THREAD))
					println(Debug_THREAD, current_, "Assign task:" + task_ + " to current thread");
				current_.nextTask = task_;
			}
			else {
				current_ = grabOne();
				if (debug && isDebugEnabledAt(Debug_THREAD))
					println(Debug_THREAD, null, "Assign task:" + task_ + " to inactive thread:" + current_._getName());
				//System.out.println("Grab one: cwf = " + cwf + ", " + current_ + ", " + task_ + ", " + Thread.currentThread());
		// BOOKMARK 1: synchronized here to avoid racing between here and current_ which may 
		// be recycling itself in AWorkerThread.run(), look for "BOOKMARK 1" in AWorkerThread
				synchronized (current_) {
					current_.mainContext = task_;
					current_.start();
				}
			}
		}
	}
	
	/** Returns a task to be executed. Workforce is deducted if needWorkforce_ is true. */
	synchronized Task getTask(boolean needWorkforce_)
	{
		//if (resetting) return null;
		if (!qReady.isEmpty() && (!needWorkforce_ || getWorkforce()))
			return (Task)qReady.dequeue();
		else 
			return null;
	}

/*
	// notify "nthreads_" threads that wait on "o_"
	// # of threads being waked up depends on workforce available
	final void _notifyMany(Object o_, int nthreads_, AWorkerThread current_)
	{
		if (o_ == null) return;
		synchronized (o_) {
			synchronized (this) {
				if (getWorkforce(nthreads_))
					o_.notifyAll(); // dont know who's going to wake up
				else {
					int remainingWorkforce_ = getRemainingWorkforce();
					// notify "remainingWorkforce_" threads
					for (int i=0; i<remainingWorkforce_; i++) o_.notify();
					for (int i=nthreads_-remainingWorkforce_; i>0; i--)
						newTask(Task.createNotify(o_), current_);
				}
				nthreadsWaiting -= nthreads_;
			}
		}
	}
	*/
	
	//
	void ___WORK_FORCE___() {}
	//
	
	String _currentContext()
	{
		Thread tmp_ = Thread.currentThread();
		if (tmp_ instanceof AWorkerThread)
			return ((AWorkerThread)tmp_)._currentContext();
		else
			return tmp_.toString();
	}

	synchronized void returnWorkforce()
	{
		cwf ++;
		if (debug && isDebugEnabledAt(Debug_WORKFORCE))
			println(Debug_WORKFORCE, null, "R1 ---> " + cwf + ", " + _currentContext());
	}
	
	synchronized void returnWorkforce(int amount_)
	{
		cwf += amount_;
		if (debug && isDebugEnabledAt(Debug_WORKFORCE))
			println(Debug_WORKFORCE, null, "R" + amount_ + " ---> " + cwf + ", " + _currentContext());
	}
	
	synchronized boolean getWorkforce()
	{
		if (1 <= wf+cwf) {
			cwf --;
			if (debug && isDebugEnabledAt(Debug_WORKFORCE))
				println(Debug_WORKFORCE, null, "G1 ---> " + cwf + ", " + _currentContext());
			return true;
		}
		else return false;
	}
	
	synchronized boolean getWorkforce(int amount_)
	{
		if (amount_ <= wf+cwf) {
			cwf -= amount_;
			if (debug && isDebugEnabledAt(Debug_WORKFORCE))
				println(Debug_WORKFORCE, null, "G" + amount_ + " ---> " + cwf + ", " + _currentContext());
			return true;
		}
		else return false;
	}

	// Gets remaining workforce
	synchronized int getRemainingWorkforce()
	{
		int remaining_ = wf + cwf;
		cwf = -wf;
		return remaining_;
	}
	
	/**
	 * Set the amount for all.
	 */
	public void setMaxWorkforce(int value_)
	{ 
		double old_ = wf;
		wf = value_ >= 0? value_: Integer.MAX_VALUE;
	}
	
	/** Returns the maximum workforce managed by this runtime. */
	public int getMaxWorkforce()
	{ return wf; }
	
	/** Returns the amount of available workforce in this runtime.  */
	public synchronized int getAvailableWorkforce()
	{	return wf + cwf;	}
	
	//
	private void ___PROPERTIES___() {}
	//
	
	public ThreadGroup getThreadGroup() { return threadGroup; }
	public void listThreads() 
	{ 
		if (threadGroup == null) System.out.println("No thread group defined");
		else threadGroup.list(); 
	}
	
	/** Returns general information of this runtime. */
	public synchronized String info()
	{	return s_info2();	}
	
	/** Synchronized version of {@link #s_info()}. */
	public synchronized String ss_info()
	{	return s_info();	}
	
	// Returns general info of this runtime.
	String s_info2()
	{
		long numberOfArrivalEvents_ = getNumberOfArrivalEvents();
		long numOfThreadsCreated_ = getNumberOfThreadsCreated();
		StringBuffer sb_ = new StringBuffer(toString());
		if (state != State_INACTIVE && state != State_SUSPENDED)
			ltime = System.currentTimeMillis() - startTime;
		sb_.append(" --- State:" + state + " --- Workforce:" + wf + "\n");
		sb_.append("# of threads created:   " + numOfThreadsCreated_ + "\n");
		sb_.append("# of events:            " + numberOfArrivalEvents_ + "\n");
		sb_.append("Event processing rate:  " + _getEventRate(numberOfArrivalEvents_) + "(#/s)\n");
		if (resetting) {
			sb_.append("Resetting..." + nthreads + " threads to go\n");
			if (thread != null) sb_.append("     could be stuck on " + thread + "\n");
		}
		sb_.append("Time:  " + _getTime() + "\n");
		sb_.append("Wall time elapsed: ");
		sb_.append(((double)ltime / 1000.0) + " sec.\n");
	
		if (isRTEnabled())
			sb_.append("Realtime(RT) enabled.\nRT performance: " + getRTEvaluation() + "%\n");

		return sb_.toString();
	}

	/** Returns statistics of this runtime. */
	public String s_info()
	{ return s_info(false); }

	/** Returns statistics of this runtime. */
	public String s_info(boolean listWaitingTasks_)
	{
		long numberOfArrivalEvents_ = getNumberOfArrivalEvents();
		long numOfThreadRequests_ = getNumberOfThreadRequests();
		long numOfThreadsCreated_ = getNumberOfThreadsCreated();
		StringBuffer sb_ = new StringBuffer(toString());
		if (state != State_INACTIVE && state != State_SUSPENDED)
			ltime = System.currentTimeMillis() - startTime;
		sb_.append(" --- State:" + state + " --- Workforce:" + (wf+cwf) + "/" + wf + "\n");
		sb_.append("# of threads created:  " + numOfThreadsCreated_ + "\n");
		sb_.append("# of working threads:  " + vWorking.size() + "\n");
		sb_.append("# of waiting threads:  " + nthreadsWaiting + "\n");
		sb_.append("# of idle threads:     " + getNumberOfIdleThreads() + "\n");
		sb_.append("# of arrival events:   " + numberOfArrivalEvents_ + "\n");
		sb_.append("# of thread requests:  " + numOfThreadRequests_ + "\n");
		sb_.append("Event request rate:    " + _getEventRate(numberOfArrivalEvents_) + "(#/s)\n");
		sb_.append("Thread request rate:   " + getThreadRequestRate() + "(#/s)\n");
		sb_.append("# thread requests/#created:  " + ((double)numOfThreadRequests_/numOfThreadsCreated_) + "\n");
		sb_.append("# arrivals/#created:         " + ((double)numberOfArrivalEvents_/numOfThreadsCreated_) + "\n");
		sb_.append("# arrivals/# thread requests:" + ((double)numberOfArrivalEvents_/numOfThreadRequests_) + "\n");
		if (resetting) {
			sb_.append("Resetting..." + nthreads + " threads to go\n");
			if (thread != null) sb_.append("     could be stuck on " + thread + "\n");
		}
		sb_.append("Time:  " + _getTime() + "\n");
		sb_.append("Wall time elapsed: ");
		sb_.append(((double)ltime / 1000.0) + " sec.\n");
		//sb_.append("ltime = " + ltime + "\n");
		//sb_.append("startTime = " + startTime + "\n");
		//sb_.append("real time = " + System.currentTimeMillis() + "\n");
	
		sb_.append("#Tasks_queued: " + qReady.getLength() + "\n");

		sb_.append("WAKEUP THREAD:  " + wakeupThreadInfo("", listWaitingTasks_));
		if (isRTEnabled())
			sb_.append("Realtime(RT) enabled.\nRT performance: " + getRTEvaluation() + "%\n");

		return sb_.toString();
	}
	
	/** Asynchronous version of {@link #diag()}. */
	public String a_info(boolean listWaitingTasks_)
	{
		StringBuffer sb_ = new StringBuffer(s_info(listWaitingTasks_));
		if (vWorking.size() > 0) sb_.append("Working threads:\n");
		for (int i=0; i<vWorking.size(); i++) {
			AWorkerThread thread_ = (AWorkerThread)vWorking.elementAt(i);
			sb_.append("   thread " + i + " " + thread_._toString() + "\n");
		}
		sb_.append("Task queue: " + (qReady.isEmpty()? "empty.\n": "\n" + qReady.info()));

		return sb_.toString();
	}

	public synchronized String sthreads()
	{ return threads(); }

	/** Returns information of the associate thread group. */
	public String threads()
	{
		try {
			StringBuffer sb_ = new StringBuffer("THREAD_GROUP: " + threadGroup + "\n");
			int j = 0;
			for (int i=0; i<vWorking.size(); i++) {
				AWorkerThread thread_ = (AWorkerThread)vWorking.elementAt(i);
				sb_.append("THREAD_" + (j++) + ":\n" + thread_.info("   "));
			}
			Object[] tt_ = threadPool.retrieveAll();
			for (int i=0; i<tt_.length; i++) {
				AWorkerThread t_ = (AWorkerThread)tt_[i];
				sb_.append("THREAD_" + (j++) + ":\n" + t_.info("   "));
			}
			sb_.append("Total: " + j + " worker thread" + (j <= 1? "": "s") + ".\n");
			return sb_.toString();
		}
		catch (Throwable e_) {
			e_.printStackTrace();
			return null;
		}
	}

	
	/** Synchronized version of {@link #wthreads()}. */
	public synchronized String swthreads()
	{ return wthreads(); }

	/** Returns information of working threads. */
	public String wthreads()
	{
		try {
			StringBuffer sb_ = new StringBuffer();
			int j = 0;
			for (int i=0; i<vWorking.size(); i++) {
				AWorkerThread thread_ = (AWorkerThread)vWorking.elementAt(i);
				sb_.append("Thread " + (j++) + ":\n" + thread_.info("   "));
			}
			sb_.append("Total: " + j + " worker thread" + (j <= 1? "": "s") + ".\n");
			return sb_.toString();
		}
		catch (Throwable e_) {
			e_.printStackTrace();
			return null;
		}
	}

	/** Returns information of the task queue. */
	public String tasks()
	{
		return "Task queue: " + (qReady == null || qReady.isEmpty()?
								"<empty>.\n": "\n" + qReady.info());
	}
	
	//
	private void ___THREAD___() {}
	//
	
	/** Grabs a AWorkerThread from recycling pool, creates one if necessary.  */
	protected synchronized AWorkerThread grabOne() 
	{
		if (state == State_INACTIVE) {
			setState(State_RUNNING);
			adjustTime();
		}
		AWorkerThread thread_ = null;
		
		/*
		while (!threadPool.isEmpty()) {
			Object o_ = threadPool.dequeue();
			if (o_ instanceof AWorkerThread) {
				thread_ = (AWorkerThread)o_;
				if (thread_.getState() == AWorkerThread.State_INACTIVE) break;
				drcl.Debug.systemFatalError(thread_ + " in inactive thread pool!?");
				//thread_ = null;
			}
		}

		if (thread_ == null) {
			thread_ = newThread();
			total++;
			thread_.runtime = thread_.aruntime = this;
		}
		*/

		if (!threadPool.isEmpty()) {
			thread_ = (AWorkerThread)threadPool.dequeue();
		}
		else {
			thread_ = newThread();
			total++;
			thread_.runtime = thread_.aruntime = this;
		}
		
		totalThreadRequests++;
		vWorking.addElement(thread_);
		
		if (debug && isDebugEnabledAt(Debug_THREAD))
			println(Debug_THREAD, null, "bring up thread:" + thread_);
		return thread_;
	}
	
	/** For subclasses to provide its own worker thread class. */
	protected AWorkerThread newThread() 
	{
		return new AWorkerThread(threadGroup, String.valueOf(total)); // with lower priority
		//return new AWorkerThread();
	}

	/** Used by AWorkerThread to return itself to the pool. */
	protected synchronized void recycle(AWorkerThread thread_)
	{
		if (resetting) return;

		if (vWorking.size() == nthreadsWaiting+1) {
			// thread_ is the last active working thread
			if (wakeupThread.Q.isEmpty() && qReady.isEmpty())
				systemBecomesInactive();
			else {
				if (debug && isDebugEnabledAt(Debug_RECYCLE))
					println(Debug_RECYCLE, thread_, "working/waiting threads:" + vWorking.size() 
							+ "/" + nthreadsWaiting + ", _startAll()");
				_startAll(thread_);
			}
		}

		if (thread_.mainContext == null || thread_.mainContext == AWorkerThread.DUMMY_CONTEXT) {
			if (debug && isDebugEnabledAt(Debug_RECYCLE))
				println(Debug_RECYCLE, thread_, "working/waiting threads:" + vWorking.size() 
						+ "/" + nthreadsWaiting + ", RECYCLED");
			vWorking.removeElement(thread_);
			threadPool.enqueue(thread_);
			if (thread_.mainContext != null) {
				//returnWorkforce();
				cwf ++;
				if (debug && isDebugEnabledAt(Debug_WORKFORCE))
					println(Debug_WORKFORCE, null, "R1 ---> " + cwf + ", " + _currentContext());
			}
		}
	}

	// try to start as many tasks as workforce is allowed
	// the first task will be assigned to current_ if current_ is not null
	private void _startAll(AWorkerThread current_)
	{
		if (qReady.isEmpty()) {
			systemBecomesIdle(current_);
			// if time advances, move all the expired waiting tasks from waiting queue to ready queue
			// here we are doing the wakeupThread's job to avoid thread synchronization with wakeupThread
			// which could gain performance by a magnitude
			if (!wakeupThread.Q.isEmpty()) {
				boolean changed_ = false;
				while (wakeupThread.Q.firstKey() - _getTime() <= timeScaleReciprocal) {
					Object o_ = wakeupThread.Q.dequeue();
			if (logenabled) {
				if (tf == null) _openlog();
				try {tf.write(("-\t\t\t" + wakeupThread.Q.getLength() + "\n").toCharArray()); tf.flush();} catch (Exception e_){}
			}
					changed_ = true;
					if (o_ instanceof Task) {// assign task to recycled thread right away
						//((Task)o_).time = 0.0;
						newTask((Task)o_, current_);
					}
					else
						newTask(Task.createNotify(o_), current_);

					// check if new task is assigned to current_.nextTask:
					if (current_ != null && current_.nextTask != null) {
						if (current_.mainContext == null) {
							if (!getWorkforce())
								drcl.Debug.systemFatalError("No workforce for even one thread!?");
						}
						current_.mainContext = current_.nextTask;
						current_.nextTask = null;
						current_ = null;
					}
				}
				if (timeScaleReciprocal > 0.0 && changed_ && !wakeupThread.Q.isEmpty())
					triggerWakeupThread();
			}
		}

		Task task_ = null;
		if (current_ != null) {
			task_ = getTask(current_.mainContext == null/*need workforce*/);
			if (task_ != null) current_.mainContext = task_;
		}

		// start as many tasks as possible
		while ((task_ = getTask(true/*need workforce*/)) != null) {
			AWorkerThread t_ = grabOne();
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, null, "Assign to inactive thread, task:" + task_ + ", thread:" + t_);
		
			// BOOKMARK1: synchronized here to avoid racing between here and t_ which may 
			// be recycling itself in AWorkerThread.run(), look for "BOOKMARK1" in AWorkerThread.java
			synchronized (t_) {
				t_.mainContext = task_;
				t_.start();
			}
		}
	}
	
	synchronized void remove(AWorkerThread thread_)
	{
		if (debug && isDebugEnabledAt(Debug_THREAD))
			println(Debug_THREAD, thread_, "REMOVED itself");
		threadPool.remove(thread_);
		vWorking.removeElement(thread_);
	}
	
	/** Forces to reset this runtime. */
	public void forceReset()
	{
		resetting = false; 
		reset();
	}
	
	/** Returns the worker thread, for diagnosis */
	public AWorkerThread getWorkingThread(int index_)
	{
		if (index_ < 0) return null;
		return index_ >= vWorking.size()? null: (AWorkerThread)vWorking.elementAt(index_);
	}
	
	//
	public void ___PROFILE___() {}
	// 
	
	/** Returns the number of worker threads being created. */
	public int getNumberOfThreadsCreated()
	{ return total; }
	
	/** Returns the number of [idle] worker threads in the recycling pool. */
	public int getNumberOfIdleThreads()
	{ return threadPool.getLength(); }
	
	/** Returns the number of requests for worker threads. */
	public long getNumberOfThreadRequests() 
	{ return totalThreadRequests; }
	
	/** Returns the number of arrival events. */
	public long getNumberOfArrivalEvents() 
	{
		long totalGrabs_ = 0;

		for (int i=0; i<vWorking.size(); i++) {
			AWorkerThread thread_ = (AWorkerThread)vWorking.elementAt(i);
			synchronized (thread_) {
				totalGrabs_ += thread_.totalNumEvents;
			}
		}
		Object[] tt_ = threadPool.retrieveAll();
		for (int i=0; i<tt_.length; i++) {
			AWorkerThread t_ = (AWorkerThread)tt_[i];
			synchronized (t_) {
				totalGrabs_ += t_.totalNumEvents;
			}
		}
		return totalGrabs_; 
	}
	
	/**
	 * Returns the efficiency index of thread recycling.
	 * Calculated by {@link #getNumberOfThreadRequests()} over
	 * {@link #getNumberOfThreadsCreated()}.
	 */
	public double getEfficiencyIndex() 
	{ return total == 0? Double.NaN: (double)getNumberOfThreadRequests() / total; }
	
	public double getEventEfficiencyIndex(long numberOfArrivalEvents_) 
	{ return total == 0? Double.NaN: (double)numberOfArrivalEvents_ / total; }
	
	/** Returns the number of worker threads that are currently executing some tasks. */
	public int getNumberOfWorkingThreads() { return vWorking.size(); }
	
	/** Returns the event processing rate of this runtime. */
	public double getEventRate() 
	{ return _getEventRate(getNumberOfArrivalEvents()); }

	protected double _getEventRate(long numArrivals_) 
	{
		if (state == State_SUSPENDED || state == State_INACTIVE)
			return (double)numArrivals_ / ltime * 1000.0;
		else 
			return (double)numArrivals_ / (System.currentTimeMillis() - startTime) * 1000.0; 
	}

	public double getThreadRequestRate() 
	{
		if (state == State_SUSPENDED || state == State_INACTIVE)
			return (double)getNumberOfThreadRequests() / ltime * 1000.0;
		else 
			return (double) getNumberOfThreadRequests() / (System.currentTimeMillis() - startTime) * 1000.0; 
	}
	
	protected AWorkerThread[] getWorkingThreads()
	{
		AWorkerThread[] tt_ = new AWorkerThread[vWorking.size()];
		vWorking.copyInto(tt_);
		return tt_;
	}
	
	//
	void ___EXECUTION_CONTROL___() {}
	//
	
	/** Asynchronized version of getTime(), for diagnosis.
	 *  Subclasses should override this method to provide its own
	 *  time mapping function. */
	protected double _getTime() 
	{
		if (state == State_SUSPENDED || state == State_INACTIVE) return time;
		ltime = System.currentTimeMillis() - startTime;
		time = (double)ltime * timeScaleReciprocal; 
		//return System.currentTimeMillis() - startTime;// * timeScaleReciprocal; 
		return time;
	}
	
	/** Adjust {@link #startTime} according to wall time and {@link #ltime}
	 * Called when simulation started and resumed. */
	protected void adjustTime()
	{ startTime = System.currentTimeMillis() - ltime; }
	
	// record "ltime" and "time"
	protected void recordTime()
	{
		if (state == State_INACTIVE && startTime == 0)
			startTime = System.currentTimeMillis();
		else {
			if (state != State_SUSPENDED) ltime = System.currentTimeMillis() - startTime;
			_getTime(); // record the time when pool is stopped.
		}
	}
	
	protected synchronized void _stop(boolean block_) 
	{
		recordTime();
		setState(State_SUSPENDING);
		if (timeScaleReciprocal > 0.0)
			triggerWakeupThread();

		if (!block_) {
			setState(State_SUSPENDED);
			runSuspendHooks();
			return;
		}
		
		int nactive_ = vWorking.size();
		int ntimes_ = 0;
		Thread current_ = Thread.currentThread();
		//int priority_ = current_.getPriority();
		//current_.setPriority(Thread.MIN_PRIORITY);
		
		// wait until all threads become inactive or sleeping
		while (true) {
			//System.out.println("----------------------------------------");
			int tmp_ = 0;
			for (int i=0; i<vWorking.size(); i++) {
				AWorkerThread thread_ =  (AWorkerThread)vWorking.elementAt(i);
				if (thread_.isActive()) tmp_ ++;
			}
			if (tmp_ == 0) break;
			if (tmp_ == nactive_) {
				ntimes_++;
				if (ntimes_ == 30) {
					System.out.println("stop(): some threads just don't finish, quit waiting now!!!");
					System.out.println(nactive_ + " threads to go.");
					return;
				}
			}
			else {
				nactive_ = tmp_;
				ntimes_ = 0;
			}
			try {
				wait(100);
			}
			catch (Exception e_) {
				drcl.Debug.debug(this + ": current thread can't sleep");
				current_.yield();
			}
		}
		setState(State_SUSPENDED);
		runSuspendHooks();
		//current_.setPriority(priority_);
	}
	
	/**
	 * Resumes the system.
	 * The state must advance to RUNNING when this method returns.
	 */
	public synchronized void resume()
	{
		adjustTime();
		setState(State_RUNNING); // must be set before waking up each thread
		// if ready queue is not empty, then the first case must hold
		if (vWorking.size() > nthreadsWaiting) {
			for (int i=0; i<vWorking.size(); i++) {
				AWorkerThread thread_ =  (AWorkerThread)vWorking.elementAt(i);
				if (thread_.isInActive()) thread_.start();
			}
			if (timeScaleReciprocal > 0.0 && !wakeupThread.Q.isEmpty())
				triggerWakeupThread();
			runRunHooks();
		}
		else if (wakeupThread.Q.isEmpty())
			systemBecomesInactive();
		else {
			// start as many tasks as possible
			_startAll(null);
			if (timeScaleReciprocal > 0.0 && !wakeupThread.Q.isEmpty())
				triggerWakeupThread();
			runRunHooks();
		}
	}
		
	/**
	 * Resumes the system and lets it run for the time duration specified.
	 * The state must advance to RUNNING when this method returns.
	public synchronized final void resumeFor(double later_)
	{
		// XXX: the order of stopAt() and resume() is important 
		// If stopAt() first and then resume(), then the thread for stopAt() cannot start
		// because the current thread holds the monitor of this object.
		// It works if resume() first and then stopAt(), but the result may not be what
		// is expected if there is a big jump in time right after resume() that passes
		// the stopAt() time.
		// ------------------------------
		// A dirty solution:
		//     stopAt() first and then resume(), insert a wait() for this thread to give
		//     up holding the monitor of this object so that the thread for stopAt() has
		//     a chance to start.
		//     But it still does not guarantee it will work all the time...
		// ------------------------------
		// SOLVED!  In AWorkerThread.start(), just halt if thread's runtime is in suspension
		double time_ = _getTime() + later_;
		stopAt(time_);
		resume();
	}
	 */
	
	/**
	 * The workerpool enters a transitional period when reset() is issued.
	 * It waits for all the working thread to either finishes the job or
	 * goes into sleep.  After all the threads finish jobs, it releases all the
	 * threads by kill()ing them.
	 * 
	 * <p>
	 * CAUTION: 1. This method is better be executed by a thread that is not created
	 * from this worker pool, unless you know what you're doing.
	 * 
	 * 2. Current implementation does not work if a thread executes indefinitely.
	 */
	public void reset()
	{
		try { // for debug
		if (resetting) return;
		resetting = true;
		synchronized (this) { setState(State_RESETTING); }
		
		wakeupThread.reset();

		cwf = 0;
		Thread current_ = Thread.currentThread();
		synchronized (this) {
			nthreads = vWorking.size();
		}
		setState("LET_GO_THREADS");
		// let running threads finish
		while (true) {
			try {
				current_.sleep(100);
			}
			catch (Exception e_) {
				System.out.println("reset()| " + e_.toString());
			}
			synchronized (this) {
				if (nthreads <= vWorking.size()) break;
				nthreads = vWorking.size();
			}
		}
		
		setState("KILL_THREADS");
		int j=0;
		AWorkerThread special_ = null, allThreads_[];
		//System.out.println("--------------------- " + nthreads + " threads to release");
		while (true) {
			synchronized (this) {
				if (vWorking.size() == 0) break;
				allThreads_ = new AWorkerThread[vWorking.size()];
				vWorking.copyInto(allThreads_);
			}
			for (int i=0; i<allThreads_.length; i++) {
				AWorkerThread thread_ = allThreads_[i];
				if (thread_ == null) break;
				if (thread_ == current_) { 
					// If the thread is one of the guys, best we can do is kill() it
					// at the end and just leave it out here.
					// Should be fine if the thread does nothing afterwards, 
					// otherwise the behavior is unpredictable.
					synchronized (this) {
						vWorking.removeElement(thread_); 
					}
					special_ = thread_;
					special_.totalNumEvents = 1;
					continue;
				}
				//else if (thread_.state == AWorkerThread.State.INACTIVE 
				//		 && !thread_.isAlive()) {
				//	thread_.interrupt();
					// do nothing; somebody will start it!
				//}
				else {
					//System.out.print("--------------------- " + thread_.sleepOn + ": wake up " + thread_);
					thread = thread_;
					//thread_.wakeUp();
					thread.kill();
					thread = null;
					//System.out.println(" Done ");
				}
			}
			// shouldn't need the following codes
			// it's more robust when bugs are present
			try {
				current_.sleep(300);
			}
			catch (Exception e_) {
				System.out.println("reset()| " + e_.toString());
			}
			synchronized (this) {
				if (nthreads == vWorking.size()) {
					if (++j == 10) {
						System.out.println("reset(): can't clean up working threads in " + this + " in 3 seconds, quit now!!!");
						//resetting = false;
						return;
					}
				}
				//else System.out.println("--------------------- # of threads: " + nthreads);
				nthreads = vWorking.size();
			}
		}
		
		int zero_ = 0;
		Thread[] tt_ = null;
		synchronized (this) {
			setState("KILL_THREADS2");
			nthreads = threadGroup.activeCount();
			tt_ = new Thread[nthreads];
			threadGroup.enumerate(tt_);
		}
		for (int i=0; i<tt_.length; i++) {
			if (tt_[i] instanceof AWorkerThread && tt_[i] != current_)
				if (tt_[i].isAlive())
					((AWorkerThread)tt_[i]).kill();
				else
					zero_ ++;
			else
				zero_ ++;
		}
		
		setState("WAIT_FOR_THREADS_TO_DIE");
		// try wait for most of idle threads dying away
		for (int i=0; i<tt_.length; i++)
			if (tt_[i] instanceof AWorkerThread && tt_[i] != current_) {
				try {
					tt_[i].join(500); // wait for 500 ms
				}
				catch (Exception e_) {}
			}
		
		for (int i=0; i<10; i++) {
			nthreads = threadGroup.activeCount() - zero_;
			if (nthreads > 0) {
				try { current_.sleep(300); } catch (Exception e_) { current_.yield(); }
			}
		}
		
		if (nthreads > 0) {
			System.out.println("reset(): can't clean up idle threads in " + this + " in 3 seconds, quit now!!!");
			System.out.println(nthreads + " to go; zero_=" + zero_);
			return;
		}

		setState(State_INACTIVE);
		total = special_ == null? 0: 1;
		totalThreadRequests = 0;
		startTime = 0;
		ltime = 0;
		time = 0.0;
		cwf = special_ == null? 0: -1;
		qReady.reset();
		resetting = false;
		nthreadsWaiting = 0;
		maxlength = 0;

		runStopHooks();
		
		} // for debug
		catch (Exception e_) {
			e_.printStackTrace();
			drcl.Debug.systemFatalError("Error in ARuntime.reset().");
		}
	}

	/** Returns the actual time (in ms) this runtime has run for. */
	public long getWallTimeElapsed()
	{
		if (state != State_INACTIVE && state != State_SUSPENDED)
			ltime = System.currentTimeMillis() - startTime;
		return ltime;
	}

	public String t_info()
	{ return t_info(""); }

	protected String t_info(String prefix_)
	{
		if (state != State_INACTIVE && state != State_SUSPENDED)
			ltime = System.currentTimeMillis() - startTime;
		StringBuffer sb_ = new StringBuffer();
		sb_.append(prefix_ + "timeScale     = " + (timeScale/1.0e3) + " (wall time/virtual time)\n");
		sb_.append(prefix_ + "1.0/timeScale = " + (timeScaleReciprocal*1.0e3) + " (virtual time/wall time)\n");
		sb_.append(prefix_ + "startTime     = " + startTime + "\n");
		sb_.append(prefix_ + "ltime         = " + ltime + "\n");
		sb_.append(prefix_ + "currentTime   = " + _getTime() + "\n");
		return sb_.toString();
	}

	protected void setState(String new_)
	{
		if (state == new_) return;
		
		// notify listeners
		if (vStateListener != null) 
			notifyStateListeners(new PropertyChangeEvent(this, "State", state, new_));

		if (debug && isDebugEnabledAt(Debug_STATE))
			println(Debug_STATE, null, state + " --> " + new_);
		
		state = new_;
	}
	
	boolean isSuspend()
	{ return state == State_SUSPENDING || state == State_SUSPENDED; }
	
	/** Returns true if the runtime is in resetting. */
	public boolean isResetting()
	{	return resetting || state == State_RESETTING;	}
	
	/** Returns true if the runtime is stopped (inactive or suspended)
	 * or running but all working threads are waiting. */
	public boolean isIdle()
	{
		if (state == State_SUSPENDED || state == State_INACTIVE)
			return true;
		//for (int i=0; i<vWorking.size(); i++) {
		//	AWorkerThread thread_ = (AWorkerThread)vWorking.elementAt(i);
		//	if (!thread_.isWaiting()) return false;
		//}
		//return true;
		return nthreadsWaiting == vWorking.size();
	}
	
	//
	private void ___TRACE___() {}
	//
	
	protected void threadStateChange(AWorkerThread thread_, String oldState_, String newState_)
	{
		if (debug && isDebugEnabledAt(Debug_THREAD_STATE))
			println(Debug_THREAD, null, oldState_ + " --> " + newState_ + ", " + thread_);
	}
	
	{
		tr.setTraces(new String[]{Debug_THREAD, Debug_WORKFORCE, Debug_Q, Debug_RECYCLE, Debug_STATE, Debug_THREAD_STATE});
	}
	
	// stuff spaces before trace names in print()/println()
	private static String[] SPACES = {null, "      ", "     ", "    ", "   ", "  ", " ", ""};

	public void print(String which_, AWorkerThread current_, String msg_)
	{
		if (which_.length() < SPACES.length)
			drcl.Debug.debug(SPACES[which_.length()] + which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_: Thread.currentThread())
				+ (msg_ == null? "": "| " + msg_));
		else
			drcl.Debug.debug(which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_: Thread.currentThread())
				+ (msg_ == null? "": "| " + msg_));
	}

	public void println(String which_, AWorkerThread current_, String msg_)
	{
		if (which_.length() < SPACES.length)
			drcl.Debug.debug(SPACES[which_.length()] + which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_: Thread.currentThread())
				+ (msg_ == null? "": "| " + msg_) + "\n");
		else
			drcl.Debug.debug(which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_: Thread.currentThread())
				+ (msg_ == null? "": "| " + msg_) + "\n");
	}

	// XXX: work-in-progress
	private void ___REAL_TIME_EVALUATION___() {}
	
	protected boolean rtEnabled = false;
	protected long nLags = 0, nEvents; // # lagging events
	
	/**
	 * Can only be set when INACTIVE <i>(not implemented)</i>.
	 */
	public void setRTEnabled(boolean value_) 
	{
		if (state == State_INACTIVE)
			rtEnabled = value_; 
		else
			drcl.Debug.error(this, "setRTEnabled(): can only be set when INACTIVE.");
	}
	
	public boolean isRTEnabled() { return rtEnabled; }
	
	public long getNumLaggingEvents() { return nLags; }

	/**
	 * Returns the percentage where events are processed in real time (within tolerance
	 * specified by setRTTolerance() <i>(not implemented)</i>.
	 */
	public double getRTEvaluation() 
	{ return 100.0 - (double) 100.0 * nLags / nEvents; }
	
	protected long rtTol = 50; // ms
	
	public void setRTTolerance(long v_) { rtTol = v_; }
	public long getRTTolerance() { return rtTol; }



	//
	private void ___WAKE_UP_PACK___() {}
	//

	public Queue getQ() { return wakeupThread.Q; }
	public Object getEventQueue() { return wakeupThread.Q; }

	protected synchronized void threadBecomesWaiting(AWorkerThread exe_)
	{
		nthreadsWaiting ++;
		returnWorkforce();
		if (nthreadsWaiting == vWorking.size()) {
			if (qReady.isEmpty() && wakeupThread.Q.isEmpty())
				systemBecomesInactive();
			else
				_startAll(null);
		}
	}

	/**
	 * Subclasses should override this method to handle the event when a workerthread
	 * requests to become waiting.
	 * @return false if current thread should continue execution (the request is rejected).
	 */
	protected synchronized boolean threadRequestsSleeping(AWorkerThread exe_, double time_)
	{
		if (resetting) return true;

		nthreadsWaiting ++;
		wakeupThread.Q.enqueue(time_, exe_.sleepOn);
			if (logenabled) {
				if (tf == null) _openlog();
				try{tf.write(("+ " + time_ + "\t\t" + wakeupThread.Q.getLength() + "\n").toCharArray()); tf.flush();} catch (Exception e_){}
			}
		if (maxlength < wakeupThread.Q.getLength()) maxlength = wakeupThread.Q.getLength();
		if (debug && isDebugEnabledAt(Debug_Q))
			println(Debug_Q, exe_, "Enqueue thread(threadRequestsSleeping()), sleepOn:" + exe_.sleepOn);

		if (nthreadsWaiting == vWorking.size()) {
			if (qReady.isEmpty()
				//&& time_ - wakeupThread.Q.firstKey() <= timeScaleReciprocal) {
				&& wakeupThread.Q.firstElement() == exe_.sleepOn) {
				systemBecomesIdle(exe_);
				if (time_ - _getTime() <= timeScaleReciprocal) { // system time is advanced
					if (debug && isDebugEnabledAt(Debug_Q))
						println(Debug_Q, null, "DONT_SLEEP (remove from Q):" + exe_.sleepOn);
					//wakeupThread.Q.remove(exe_.sleepOn);
					wakeupThread.Q.dequeue();
			if (logenabled) {
				if (tf == null) _openlog();
				try{tf.write(("-\t\t\t" + wakeupThread.Q.getLength() + "\n").toCharArray()); tf.flush();} catch (Exception e_){}
			}
					nthreadsWaiting --;
					return false;
				}
			}
		}
		returnWorkforce();
		// start as many tasks as possible
		_startAll(null);
		if (timeScaleReciprocal > 0.0 && wakeupThread.Q.firstElement() == exe_.sleepOn)
			triggerWakeupThread();
		return true;
	}
	
	/*
	synchronized void threadAwakeFromSleeping(AWorkerThread exe_)
	{
		if (debug && isDebugEnabledAt(Debug_WORKFORCE))
			println(Debug_Q, exe_, "awaked from finite sleep, " + exe_._currentContext());
		wakeupThread.Q.remove(exe_.sleepOn);
	}
	*/
	
	/*
	synchronized void threadAwakeFromWaiting(AWorkerThread exe_)
	{
		if (debug && isDebugEnabledAt(Debug_WORKFORCE))
			println(Debug_Q, exe_, "awaked from indefinite sleep: " + exe_._currentContext());
	}
	*/
	
	/**
	 * Subclasses should override this method to handle the event when all workerthreads
	 * in the system are either waiting or idle but there are still tasks in the waiting queue.
	 * @param exe_ current thread, requests waiting and leads the system to the idle state.
	 */
	protected void systemBecomesIdle(AWorkerThread exe_)
	{}

	/**
	 * Called when system becomes inactive, that is, no thread is active or all working threads
	 * are waiting, and no task is in the waiting queue.  In most cases, subclasses do not need
	 * to override this method.
	 */
	protected void systemBecomesInactive()
	{
		recordTime();
		if (state == State_RUNNING) {
			setState(State_INACTIVE);
			runStopHooks();
		}
	}
	

	public String wakeupThreadInfo()
	{ return wakeupThread.info("", false); }

	String wakeupThreadInfo(String prefix_, boolean listWaitingTasks_)
	{ return wakeupThread.info(prefix_, listWaitingTasks_); }

	protected void triggerWakeupThread()
	{
		if (debug && isDebugEnabledAt(Debug_Q))
			println(Debug_Q, null, "trigger wakeupthread");
		synchronized (this) {
			this.notify();
		}
	}

	static double MAX_SLEEP_TIME_THAT_MAKES_SENSE = 2592000000.0; // ms ~ one month

	class WakeupThread extends Thread
	{
		// Threads that will resume execution at future times.
		// Entry is thread, key is the time to resume the thread execution.
		//Queue Q = QueueAssistant.getBest(); // or "waiting" queue
		//Queue Q = new CalendarQueue(); // or "waiting" queue
		Queue Q = new TreeMapQueue(); // or "waiting" queue
		//Queue Q = new BSQueue(); // or "waiting" queue
		//Queue Q = new SimpleQueue(); // or "waiting" queue
		String wakeupThreadState = null;
		ARuntime runtime;
		Object next; // for debug
		double realTime; // 

		WakeupThread(String id_)
		{
			super(id_);
			runtime = ARuntime.this;
		}

		void reset()
		{
			Q.reset();
			triggerWakeupThread();
			wakeupThreadState = null;
			next = null;
		}

		public void run()
		{
			if (Thread.currentThread() != this) return;
			//if (true) return;
			
			try {
				synchronized (runtime) {
					for (;;) {
						while (Q.isEmpty() || isSuspend()) {
							wakeupThreadState = isSuspend()? "suspended": "wait";
							runtime.wait();
						}
						next = null;
						double time_ = Q.firstKey();
						double now_ = runtime._getTime();
						if (time_ - now_ <= timeScaleReciprocal) {
							next = Q.dequeue();
			if (logenabled) {
				if (tf == null) _openlog();
				try{tf.write(("-\t\t\t" + wakeupThread.Q.getLength() + "\n").toCharArray()); tf.flush();} catch (Exception e_){}
			}
							if (debug && isDebugEnabledAt(Debug_Q))
								println(Debug_Q, null, "Dequeue sleepOn:" + next);
						}
				
						if (next != null) {
							if (next instanceof Task) {
								((Task)next).time = 0.0;
								newTask((Task)next, null);
							}
							else
								newTask(Task.createNotify(next), null);
						}
						else if (!isSuspend()) {
							wakeupThreadState = "sleep_until:" + time_;
							realTime = timeScale * (time_ - now_);
							if (realTime > MAX_SLEEP_TIME_THAT_MAKES_SENSE)
								runtime.wait((long)MAX_SLEEP_TIME_THAT_MAKES_SENSE);
							else
								runtime.wait((long)realTime);
						}
					}
				} // synchronized
			}
			catch (Throwable e_) {
				e_.printStackTrace();
				drcl.Debug.systemFatalError("ARuntime.wakeupThread is corrupted, "
					+ "forcing JavaSim to exit!");
			}
		}

		public String info(String prefix_, boolean listWaitingTasks_)
		{
			return prefix_ + "State:" + wakeupThreadState + ", next:" + next + "\n"
					+ q_info(prefix_, listWaitingTasks_);
		}

		/**
		 * @return information of the sleeping thread queue.
		 */
		public String q_info(String prefix_, boolean listElement_)
		{
			if (Q == null)
				return prefix_ + "Q: empty\n";
			else if (Q.isEmpty())
				return prefix_ + "Q: empty, maxlength = " + maxlength + "\n";
			else {
				StringBuffer sb_ = new StringBuffer();
				int qsize_ = Q.getLength();
				if (qsize_ > 1)
					sb_.append(prefix_ + "Q: " + qsize_ + " events, maxlength = " + maxlength + "\n");
				else 
					sb_.append(prefix_ + "Q: one event, maxlength = " + maxlength + "\n");
	
				if (listElement_) {
					Object[] oo_ = Q.retrieveAll();
					double[] dd_ = Q.keys();
					for (int i=0; i<oo_.length; i++) {
						sb_.append(prefix_ + "   " + i + "," + dd_[i] + ": " + oo_[i] + "\n");
					}
				}
				return sb_.toString();
			}
		}
	}

	//
	// ForkManager API
	//

	/** Cancels a fork event. */
	protected void off(ACATimer handle_)
	{ wakeupThread.Q.remove(((Task)handle_).time, handle_); }
}
