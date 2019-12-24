// @(#)SESimulator.java   2/2004
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

package drcl.sim.event;

import java.util.*;
import java.beans.*;

import drcl.comp.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;


/**
 * A sequential event simulation engine.
 * @see SEThread
 */
public class SESimulator extends ACARuntime
{
	public static final String Debug_THREAD         = "THREAD";
	public static final String Debug_THREAD_STATE   = "THREAD_STATE";
	public static final String Debug_Q              = "Q";
	public static final String Debug_RECYCLE        = "RECYCLE";
	public static final String Debug_STATE          = "STATE";
	
	/** Transitional state to SUSPENDED. */
	public static final String State_SUSPENDING     = "SUSPENDING";
	/** Transitional state to INACTIVE when system is being reset. */
	public static final String State_RESETTING      = "RESETTING";

	// the pool
	protected FIFOQueue threadPool = new FIFOQueue();
	
	// for debug, stored the SEThreads that are out working,
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
	protected TreeMapQueue qWaiting = new TreeMapQueue();

	private transient int nthreads = 0; // for debug
	private transient SEThread thread; // for debug
	protected int nthreadsWaiting = 0;
	int maxlength = 0; // max occupancy in waiting queue in history 
	boolean suspended = false;

	public SESimulator()
	{ this("SESim"); }
	
	public SESimulator(String name_)
	{	
		super(name_);
		threadGroup = new ThreadGroup(getName());
		threadGroup.setMaxPriority(Thread.MIN_PRIORITY + 2);

		// this is necessary for ForkManager to work correctly...
		setTimeScale(Double.POSITIVE_INFINITY);
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

	private String currentThread(SEThread wt_)
	{
		Thread t_ = null;
		if (wt_ == null) {
			t_ = Thread.currentThread();
			if (t_ instanceof SEThread)
				wt_ = (SEThread)t_;
		}
		return wt_==null? t_.toString(): wt_._getName();
	}

	/**
	 * The only way to trigger new tasks to be executed.
	 */
	protected synchronized void newTask(Task task_, WorkerThread currentThread_)
	{
		if (resetting || task_ == null) return;
		
		//if (debug && currentThread_ != null)
		//	System.out.println(time + ": qReady.length=" + qReady.getLength()
		//			+ ", current=" + currentThread_.getState());
		
		//System.out.println("New task: " + task_ + ", current thread = " + current_);
		SEThread current_ = null;
		try {
			current_ = (SEThread)currentThread_;
		}
		catch (ClassCastException e_) {
			current_ = null;
		}

		
		if (current_ != null && !current_.isReadyForNextTask())
			current_ = null;
		
		// to figure in the processing delay in the runtime
		if (task_.getTime() > time) {
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, current_, "to waiting-queue:" + task_);
			qWaiting.enqueue(task_.getTime(), task_);

			if (logenabled) {
				if (tf == null) _openlog();
				try{
					tf.write(("+ " + task_.getTime() + "\t\t" 
						+ qWaiting.getLength() + "\n").toCharArray());
					tf.flush();
				}
				catch (Exception e_){}
			}

			if (maxlength < qWaiting.getLength())
				maxlength = qWaiting.getLength();

			if (state == State_INACTIVE)
				_startAll();
			return;
		}
		else if (current_ != null && qReady.isEmpty()) {
			// assign next task if the current thread is finishing
			// this execution path does not require threads to sync;
			// the best path one can expect.
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, current_, "Assign task:" + task_ + " to current thread");
			current_.nextTask = task_;
			return;
		}
		else {
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, current_, "to ready-queue:" + task_);
			qReady.enqueue(task_);
			if (state == State_INACTIVE)
				_startAll();
			return;
		}
	}
	
	/** Returns a task to be executed. Workforce is deducted if needWorkforce_ is true. */
	synchronized Task getTask()
	{
		if (resetting) return null;
		//return (Task)qReady.dequeue();
		boolean jump_ = false;
		if (qReady.isEmpty()) {
			qWaiting.dequeueTransfer(qReady);
			jump_ = true;
		}

		if (qReady.isEmpty()) return null;

		Task t_ = (Task)qReady.dequeue();
		if (jump_) {
			time = t_.getTime();
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, null, 
							"from waiting-queue, <TIME_JUMP>:" + t_);
		}
		else {
			if (debug && isDebugEnabledAt(Debug_THREAD))
				println(Debug_THREAD, null, "from ready-queue:" + t_);
		}
		return t_;
	}

/*
	// notify "nthreads_" threads that wait on "o_"
	// # of threads being waked up depends on workforce available
	final void _notifyMany(Object o_, int nthreads_, SEThread current_)
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
	
	
	String _currentContext()
	{
		Thread tmp_ = Thread.currentThread();
		if (tmp_ instanceof SEThread)
			return ((SEThread)tmp_)._currentContext();
		else
			return tmp_.toString();
	}

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
		sb_.append(" --- State:" + state + "\n");
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
		sb_.append(" --- State:" + state + "\n");
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

		sb_.append(q_info("", listWaitingTasks_));
		if (isRTEnabled())
			sb_.append("Realtime(RT) enabled.\nRT performance: " + getRTEvaluation() + "%\n");

		return sb_.toString();
	}
	
	public String a_info(boolean listWaitingTasks_)
	{ return a_info(listWaitingTasks_, false, false); }

	/** Asynchronous version of {@link #diag()}. */
	public String a_info(boolean listWaitingTasks_, 
					boolean listReadyTasks_, 
					boolean listWorkingThreads_)
	{
		StringBuffer sb_ = new StringBuffer(s_info(listWaitingTasks_));

		if (listWorkingThreads_) {
		if (vWorking.size() > 0) sb_.append("Working threads:\n");
			for (int i=0; i<vWorking.size(); i++) {
				SEThread thread_ = (SEThread)vWorking.elementAt(i);
				sb_.append("   thread " + i + " " + thread_._toString() + "\n");
			}
		}
		if (listReadyTasks_)
			sb_.append("Task queue: "
					+ (qReady.isEmpty()? "empty.\n": "\n" + qReady.info()));

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
				SEThread thread_ = (SEThread)vWorking.elementAt(i);
				sb_.append("THREAD_" + (j++) + ":\n" + thread_.info("   "));
			}
			Object[] tt_ = threadPool.retrieveAll();
			for (int i=0; i<tt_.length; i++) {
				SEThread t_ = (SEThread)tt_[i];
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
				SEThread thread_ = (SEThread)vWorking.elementAt(i);
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
	
	/** Grabs a SEThread from recycling pool, creates one if necessary.  */
	protected synchronized SEThread grabOne() 
	{
		if (state == State_INACTIVE) {
			setState(State_RUNNING);
			adjustTime();
		}
		SEThread thread_ = null;
		
		while (!threadPool.isEmpty()) {
			Object o_ = threadPool.dequeue();
			if (o_ instanceof SEThread) {
				thread_ = (SEThread)o_;
				if (thread_.getState2() == SEThread.State_INACTIVE) break;
				drcl.Debug.systemFatalError(thread_ + " in inactive thread pool!?");
				//thread_ = null;
			}
		}
		
		if (thread_ == null) {
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
	protected SEThread newThread() 
	{
		return new SEThread(threadGroup, String.valueOf(total)); // with lower priority
		//return new SEThread();
	}

	/** Used by SEThread to return itself to the pool. */
	protected synchronized void recycle(SEThread thread_)
	{
		if (resetting) return;

		if (vWorking.size() == nthreadsWaiting+1) {
			systemBecomesInactive();
			if (debug && isDebugEnabledAt(Debug_RECYCLE))
				println(Debug_RECYCLE, thread_, "working/waiting threads:"
					+ vWorking.size() + "/" + nthreadsWaiting
					+ ", --> RECYCLED and SIM STOPPED");
		}
		else if (debug && isDebugEnabledAt(Debug_RECYCLE))
			println(Debug_RECYCLE, thread_, "working/waiting threads:"
					+ vWorking.size() + "/" + nthreadsWaiting + ", RECYCLED");

		vWorking.removeElement(thread_);
		threadPool.enqueue(thread_);
	}

	// returns false if no task to execute
	private boolean _startAll()
	{
		Task task_ = getTask();
		if (task_ == null) 
			return false;
		SEThread t_ = grabOne();
		if (debug && isDebugEnabledAt(Debug_THREAD))
			println(Debug_THREAD, null, "Assign to inactive thread, task:" + task_ + ", thread:" + t_);
		
		// BOOKMARK1: synchronized here to avoid racing between here and t_
		// which may be recycling itself in SEThread.run(), look for 
		// "BOOKMARK1" in SEThread.java
		synchronized (t_) {
			t_.mainContext = task_;
			t_.start();
		}
		return true;
	}
	
	synchronized void remove(SEThread thread_)
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
	public SEThread getWorkingThread(int index_)
	{
		if (index_ < 0) return null;
		return index_ >= vWorking.size()? null: (SEThread)vWorking.elementAt(index_);
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
			SEThread thread_ = (SEThread)vWorking.elementAt(i);
			synchronized (thread_) {
				totalGrabs_ += thread_.totalNumEvents;
			}
		}
		Object[] tt_ = threadPool.retrieveAll();
		for (int i=0; i<tt_.length; i++) {
			SEThread t_ = (SEThread)tt_[i];
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
	
	protected SEThread[] getWorkingThreads()
	{
		SEThread[] tt_ = new SEThread[vWorking.size()];
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
	{ return time; }
	
	/** Adjust {@link #startTime} according to wall time and {@link #ltime}
	 * Called when simulation started and resumed. */
	protected void adjustTime()
	{ startTime = System.currentTimeMillis() - ltime; }
	
	// record "ltime" and "time"
	protected void recordTime()
	{
		if (state == State_INACTIVE && startTime == 0)
			startTime = System.currentTimeMillis();
		else
			if (state != State_SUSPENDED) ltime = System.currentTimeMillis() - startTime;
	}
	
	protected synchronized void _stop(boolean block_) 
	{
		recordTime();
		setState(State_SUSPENDING);
		suspended = true;

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
				SEThread thread_ =  (SEThread)vWorking.elementAt(i);
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
		suspended = false;
		setState(State_RUNNING); // must be set before waking up each thread
		// if ready queue is not empty, then the first case must hold
		if (vWorking.size() > nthreadsWaiting) {
			for (int i=0; i<vWorking.size(); i++) {
				SEThread thread_ =  (SEThread)vWorking.elementAt(i);
				if (thread_.isInActive()) thread_.start();
			}
			runRunHooks();
		}
		else if (!_startAll())
			systemBecomesInactive();
		else
			runRunHooks();
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
		// SOLVED!  In SEThread.start(), just halt if thread's runtime is in suspension
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
		SEThread special_ = null, allThreads_[];
		//System.out.println("--------------------- " + nthreads + " threads to release");
		while (true) {
			synchronized (this) {
				if (vWorking.size() == 0) break;
				allThreads_ = new SEThread[vWorking.size()];
				vWorking.copyInto(allThreads_);
			}
			for (int i=0; i<allThreads_.length; i++) {
				SEThread thread_ = allThreads_[i];
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
				//else if (thread_.state == SEThread.State.INACTIVE 
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
			if (tt_[i] instanceof SEThread && tt_[i] != current_)
				if (tt_[i].isAlive())
					((SEThread)tt_[i]).kill();
				else
					zero_ ++;
			else
				zero_ ++;
		}
		
		setState("WAIT_FOR_THREADS_TO_DIE");
		// try wait for most of idle threads dying away
		for (int i=0; i<tt_.length; i++)
			if (tt_[i] instanceof SEThread && tt_[i] != current_) {
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
		qReady.reset();
		qWaiting.reset();
		resetting = false;
		nthreadsWaiting = 0;
		maxlength = 0;
		suspended = false;

		runStopHooks();
		
		} // for debug
		catch (Exception e_) {
			e_.printStackTrace();
			drcl.Debug.systemFatalError("Error in SESimulator.reset().");
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
	{ return suspended; }
	
	/** Returns true if the runtime is in resetting. */
	public boolean isResetting()
	{	return resetting || state == State_RESETTING;	}
	
	/** Returns true if the runtime is stopped (inactive or suspended)
	 * or running but all working threads are waiting. */
	public boolean isIdle()
	{
		if (state == State_SUSPENDED || state == State_INACTIVE)
			return true;

		return nthreadsWaiting == vWorking.size();
	}
	
	//
	private void ___TRACE___() {}
	//
	
	protected void threadStateChange(SEThread thread_, String oldState_, String newState_)
	{
		if (debug && isDebugEnabledAt(Debug_THREAD_STATE))
			println(Debug_THREAD, null, oldState_ + " --> " + newState_ + ", " + thread_);
	}
	
	{
		tr.setTraces(new String[]{Debug_THREAD, Debug_Q, Debug_RECYCLE, Debug_STATE, Debug_THREAD_STATE});
	}
	
	// stuff spaces before trace names in print()/println()
	private static String[] SPACES = {null, "      ", "     ", "    ", "   ", "  ", " ", ""};

	public void print(String which_, SEThread current_, String msg_)
	{
		if (which_.length() < SPACES.length)
			drcl.Debug.debug(SPACES[which_.length()] + which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_.getName():
						Thread.currentThread().getName())
				+ (msg_ == null? "": "| " + msg_));
		else
			drcl.Debug.debug(which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_.getName():
						Thread.currentThread().getName())
				+ (msg_ == null? "": "| " + msg_));
	}

	public void println(String which_, SEThread current_, String msg_)
	{
		if (which_.length() < SPACES.length)
			drcl.Debug.debug(SPACES[which_.length()] + which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_.getName():
						Thread.currentThread().getName())
				+ (msg_ == null? "": "| " + msg_) + "\n");
		else
			drcl.Debug.debug(which_ + "| " + this + "," + _getTime() + ","
				+ (current_ != null? current_.getName():
						Thread.currentThread().getName())
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

	public Queue getQ() { return qWaiting; }
	public Object getEventQueue() { return qWaiting; }

	protected synchronized void threadBecomesWaiting(SEThread exe_)
	{
		nthreadsWaiting ++;
		if (!_startAll())
			systemBecomesInactive();
	}

	/**
	 * Subclasses should override this method to handle the event when a workerthread
	 * requests to become waiting.
	 * @return false if current thread should continue execution (the request is rejected).
	 */
	protected synchronized boolean threadRequestsSleeping(SEThread exe_, double time_)
	{
		if (resetting) return true;

		if (nthreadsWaiting+1 == vWorking.size() && qReady.isEmpty()
			&& (qWaiting.isEmpty() || qWaiting.firstKey() > time_)) {
			// advance time, skip the sleep
			time = time_;
			return false;
		}

		nthreadsWaiting ++;
		qWaiting.enqueue(time_, Task.createNotify(exe_.sleepOn, time_));
		if (debug && isDebugEnabledAt(Debug_THREAD))
			println(Debug_THREAD, exe_, "to waiting-queue for waking up at "
							+ time_);
		if (maxlength < qWaiting.getLength()) maxlength = qWaiting.getLength();

		_startAll();
		return true;
	}
	
	/*
	synchronized void threadAwakeFromSleeping(SEThread exe_)
	{
		if (debug && isDebugEnabledAt(Debug_Q))
			println(Debug_Q, exe_, "awaked from finite sleep, " + exe_._currentContext());
		qWaiting.remove(exe_.sleepOn);
	}
	*/
	
	/*
	synchronized void threadAwakeFromWaiting(SEThread exe_)
	{
		if (debug && isDebugEnabledAt(Debug_Q))
			println(Debug_Q, exe_, "awaked from indefinite sleep: " + exe_._currentContext());
	}
	*/
	
	/**
	 * Called when system becomes inactive, that is,
	 * no thread is active or all working threads
	 * are waiting, and no task is in the waiting queue.
	 */
	void systemBecomesInactive()
	{
		recordTime();
		if (state == State_RUNNING) {
			setState(State_INACTIVE);
			runStopHooks();
		}
	}

	/** Returns information of the event queue.  */
	public String q_info(String prefix_, boolean listElement_)
	{
		if (qWaiting == null)
			return prefix_ + "qWaiting: empty\n";
		else if (qWaiting.isEmpty())
			return prefix_ + "qWaiting: empty, maxlength = "
					+ maxlength + "\n";
		else {
			StringBuffer sb_ = new StringBuffer();
			int qsize_ = qWaiting.getLength();
			if (qsize_ > 1)
				sb_.append(prefix_ + "qWaiting: " + qsize_ 
							+ " events, maxlength = " + maxlength + "\n");
			else 
				sb_.append(prefix_ + "qWaiting: one event, maxlength = "
							+ maxlength + "\n");
	
			if (listElement_) {
				Object[] oo_ = qWaiting.retrieveAll();
				double[] dd_ = qWaiting.keys();
				for (int i=0; i<oo_.length; i++) {
					sb_.append(prefix_ + "   " + i + "," + dd_[i]
								+ ": " + oo_[i] + "\n");
				}
			}
			return sb_.toString();
		}
	}

	//
	// ForkManager API
	//

	/** Cancels a fork event. */
	protected void off(ACATimer handle_)
	{ qWaiting.remove(((Task)handle_).getTime(), handle_); }
}
