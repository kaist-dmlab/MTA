// @(#)ForkManagerLocal.java   9/2002
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

import drcl.data.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;

/**
 * Fork manager used locally in a component.
 */
class ForkManagerLocal extends ForkManager
{
	Queue qEvents;
	
	public ForkManagerLocal(String name_, ForkManager parent_, ACARuntime runtime_)
	{ super(name_); parent = parent_; runtime = runtime_; }

	public synchronized void reset()
	{ if (qEvents != null) qEvents.reset(); }
	
	public final String a_info(boolean listEvent_)
	{
		if (listEvent_)
			return name + ": parent=" + parent + "," + (qEvents == null? "no event": qEvents.info());
		else
			return name + ": parent=" + parent + "," + (qEvents == null? "no event":
				qEvents.getLength() + (qEvents.getLength() <= 1? " event": " events")) + "\n";
	}

	protected synchronized void process(WorkerThread current_, double now_)
	{
		if (current_ != Thread.currentThread()) {
			drcl.Debug.error("ForkManager.process() must be called"
							+ " in the same context as the argument");
			return;
		}
		double key_ = Double.NaN;
		ForkEvent e_ = null;
		while (!qEvents.isEmpty()) {
			key_ = qEvents.firstKey();
			if (key_ - now_ > current_.runtime.timeScaleReciprocal) break;
			e_ = (ForkEvent)qEvents.dequeue();
			if (debug) drcl.Debug.debug("localfork| " + now_ + "| executing " + e_ + "\n");
			if (e_ instanceof ForkChild)
				((ForkManager)e_.data).process(current_, now_);
			else
				((ForkEvent)e_).execute(current_);
		}
		e_ = (ForkEvent)qEvents.firstElement();
		if (e_ != null && !e_.sent()) {
			if (debug)
				drcl.Debug.debug("localfork| " + now_ + "| sent_up_" + parent
					+ ":timeout=" + key_ + "--" + qEvents.firstElement() + "\n");
			e_.setSent();
			parent.childManager(this, key_);
		}
	}
	
	public synchronized ACATimer sendAt(Port p_, Object evt_, double time_)
	{
		return goAt(new ForkSend(evt_, p_, time_));
	}

	public synchronized ACATimer send(Port p_, Object evt_, double duration_)
	{
		try {
			return goAt(new ForkSend(evt_, p_, runtime.getTime() + duration_));
		}
		catch (Exception e_) {
			if (!(Thread.currentThread() instanceof WorkerThread))
				drcl.Debug.error("attempt to invoke fork manager " + this + " in non-WorkerThread: "
					+ Thread.currentThread());
			else e_.printStackTrace();
			return null;
		}
	}
	
	public synchronized ACATimer receiveAt(Port p_, Object evt_, double time_)
	{
		return goAt(new ForkReceive(evt_, p_, time_));
	}

	public synchronized ACATimer receive(Port p_, Object evt_, double duration_)
	{
		try {
			return goAt(new ForkReceive(evt_, p_, runtime.getTime() + duration_));
		}
		catch (Exception e_) {
			if (!(Thread.currentThread() instanceof WorkerThread))
				drcl.Debug.error("attempt to invoke fork manager " + this + " in non-WorkerThread: "
					+ Thread.currentThread());
			else e_.printStackTrace();
			return null;
		}
	}
	
	ACATimer goAt(ForkEvent tevt_)
	{
		double time_ = tevt_.time;
		/*
		if (evt_ == null) {
			drcl.Debug.error(p_ + ": cannot send null to fork");
			return Double.NaN;
		}
		*/
		if (qEvents == null) qEvents = new TreeMapQueue();
		double firstKey_ = qEvents.firstKey();

		// remove previous install
		/*
		if (p_ != DUMMY_PORT) {
			Object tmp_ = qEvents.remove(tevt_);
			if (tmp_ != null) {
				tevt_ = (ForkEvent)tmp_;
				tevt_.event = evt_;
				tevt_.sent = false;
			}
		}
		*/

		if (debug)
			drcl.Debug.debug("localfork| add_new:timeout=" + time_ + "--" + tevt_ + "\n");

		qEvents.enqueue(time_, tevt_);

		double newFirst_ = qEvents.firstKey();
		if (firstKey_ != newFirst_) {
			if (debug)
				drcl.Debug.debug("localfork|  sent_up_" + parent + ":timeout="
					+ time_ + "--" + tevt_ + "\n");
			tevt_.setSent();
			parent.childManager(this, newFirst_);
		}
		return tevt_;
	}
	
	protected void childManager(ForkManager child_, double time_)
	{
		goAt(new ForkChild(child_, time_));
	}



	protected void off(ACATimer handle_)
	{
		if (qEvents == null) return;
		qEvents.remove(((ForkEvent)handle_).time, handle_);
	}
}
