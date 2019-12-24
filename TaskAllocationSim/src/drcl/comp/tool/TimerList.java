// @(#)TimerList.java   7/2003
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

package drcl.comp.tool;

import java.util.HashMap;
import drcl.comp.*;
import drcl.util.queue.TreeMapQueue;

/** Maintains a set of timers. */
public class TimerList
{ 
	HashMap map; // key -> TimerStruct
	TreeMapQueue qkey; // time -> key
	ACATimer current;

	public TimerList()
	{}

	/** Returns the number of timers in the list. */
	public int size()
	{
		if (map == null) return 0;
		else return map.size();
	}

	/** Sets a timer.  Use <code>timer_</code> as key. 
	 * If the key exists, the timer is rescheduled. */
	public void set(Port timerPort_, Object timer_, double time_)
	{ set(timerPort_, timer_, timer_, time_); }

	/** Sets a timer.  If key_ exists, the timer is rescheduled. */
	public synchronized void set(Port timerPort_, Object key_, Object timer_,
					double time_)
	{
		if (map == null) {
			map = new HashMap();
			qkey = new TreeMapQueue();
		}

		boolean set_ = (qkey.isEmpty() || qkey.firstKey() > time_);

		TimerStruct ts_ = (TimerStruct)map.get(key_);
		if (ts_ != null) {
			// reschedule
			ts_.time = time_;
			ts_.timer = timer_;
		}
		else {
			ts_ = new TimerStruct(time_, timer_);
			map.put(key_, ts_);
			qkey.enqueue(time_, key_);
		}

		if (set_) {
			if (current != null)
				timerPort_.host.cancelFork(current);
			current = timerPort_.host.forkAt(timerPort_, this, time_);
		}
	}

	/** Retrieves the timer object. */
	public synchronized Object get(Object key_)
	{
		if (map == null) return null;
		TimerStruct ts_ = (TimerStruct)map.get(key_);
		if (ts_ == null) return null;
		else return ts_.timer;
	}

	/** Returns true if the timer list contains the key. */
	public synchronized boolean containsKey(Object key_)
	{
		if (map == null) return false;
		return map.containsKey(key_);
	}

	/** Returns the object of the timer that expires and activates next
	 * if available.
	 * Returns null if it is a false alarm. */
	public synchronized Object timeout(Port timerPort_, double now_)
	{
		Object timer_ = null;
		if (!qkey.isEmpty() && qkey.firstKey() <= now_) {
			Object key_ = qkey.dequeue();
			TimerStruct ts_ = (TimerStruct)map.remove(key_);
			if (ts_.time <= now_)
				timer_ = ts_.timer;
			else {
				// put it back to timer list
				map.put(key_, ts_);
				qkey.enqueue(ts_.time, key_);
			}
		}
		if (!qkey.isEmpty())
			current = timerPort_.host.forkAt(timerPort_, this, qkey.firstKey());
		else
			current = null;
		return timer_;
	}

	/** Cancels all the timers. */
	public synchronized void cancelAll(Port timerPort_)
	{
		if (qkey == null) return;
		qkey.reset();
		map.clear();
	}

	/** Returns the object of the timer. */
	public synchronized Object cancel(Port timerPort_, Object key_)
	{
		if (qkey == null) return null;
		TimerStruct ts_ = (TimerStruct)map.remove(key_);
		if (ts_ == null)
			return null;
		qkey.remove(key_);
		return ts_.timer;
	}

	public String info(String prefix_)
	{
		if (map == null) return prefix_ + "none\n";
		double[] timeouts_ = qkey.keys();
		Object[] keys_ = qkey.retrieveAll();
		StringBuffer sb_ = new StringBuffer();
		for (int i=0; i<timeouts_.length; i++) {
			TimerStruct ts_ = (TimerStruct)map.get(keys_[i]);
			sb_.append(prefix_);
			sb_.append(timeouts_[i]);
			sb_.append(":");
			sb_.append(ts_.time);
			sb_.append("--");
			sb_.append(ts_.timer);
			sb_.append("\n");
		}
		return sb_ + prefix_ + "activated = " + current + "\n";
	}

	class TimerStruct
	{
		double time;
		Object timer;
		boolean beenSet; // true if has been set; reserved

		TimerStruct(double time_, Object timer_)
		{
			time = time_;
			timer = timer_;
		}

		public String toString()
		{
			return time + ":" + (beenSet? "set,":"") + timer;
		}
	}
}
