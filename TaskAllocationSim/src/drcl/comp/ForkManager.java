// @(#)ForkManager.java   9/2002
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

/**
 * Defines the base class that manages "fork" events for a component system.
 * One may organize multiple fork managers in a runtime in a hierarchical structure.
 */
public abstract class ForkManager extends drcl.DrclObj
{
	protected boolean debug = false;
	protected String name;

	/** The associated runtime instance. */
	protected ACARuntime runtime;

	/** Parent manager in the manager hierarchy. */
	protected ForkManager parent;

	public ForkManager()
	{ this("FM_NO_NAME"); }
	
	public ForkManager(String name_)
	{ super(); name = name_;}

	public String toString()
	{ return name; }
	
	public void setDebugEnabled(boolean debug_)
	{ debug = debug_; }
	
	public boolean isDebugEnabled()
	{ return debug; }

	public String getName()
	{ return name; }

	public void setName(String name_)
	{ name = name_; }
	
	/**
	 * Takes over as the fork manager of the component system under <code>c_</code>.
	 */
	public void takeover(Component c_) 
	{ takeover(c_, false); }

	/**
	 * Takes over as the fork manager of the component system under <code>c_</code>.
	 */
	public void takeover(Component c_, boolean asParent_) 
	{
		if (!asParent_ || c_.fm == null) {
			c_.fm = this;
			Component[] children_ = c_.getAllComponents();
			for (int i=0; i<children_.length; i++)
				takeover(children_[i], asParent_);
		}
		else
			c_.fm.setParent(this);
			// stop recursively taking over because
			// child components' fork manager should be c_.fm or a child of it
	}

	public void reset()
	{}
	
	/** Lists the fork event queue and all other information regarding this manager. */
	public final synchronized String list()
	{ return a_info(true); }
	
	/** Lists the fork event queue (optional) and all other information
	 * regarding this manager. */
	public final synchronized String info(boolean listEvent_)
	{ return a_info(listEvent_); }

	/** Asynchronous version of {@link #list()}. */
	public final String a_list()
	{ return a_info(true); }
	
	/** Asynchronous version of {@link #info(boolean)}. */
	public abstract String a_info(boolean listEvent_);
	
	/** Sets up a "receive" event.
	 * @return a handle to cancel the event. */
	protected abstract ACATimer receive(Port p_, Object evt_, double duration_);
	
	/** Sets up a "receive" event.
	 * @return a handle to cancel the event. */
	protected abstract ACATimer receiveAt(Port p_, Object evt_, double time_);
	
	/** Sets up a "send" event.
	 * @return a handle to cancel the event. */
	protected abstract ACATimer send(Port p_, Object evt_, double duration_);
	
	/** Sets up a "send" event.
	 * @return a handle to cancel the event. */
	protected abstract ACATimer sendAt(Port p_, Object evt_, double time_);
	
	/** Sets up an event for childManager.  A child manager calls this method of its parent
	 * to sets itself up in parent's event queue. */
	protected abstract void childManager(ForkManager child_, double time_);

	/** Cancels a fork event. */
	protected abstract void off(ACATimer handle_);

	/** For a parent fork manager to notify its child manager
	 *  of processing expired fork events. */
	protected abstract void process(WorkerThread current_, double now_);

	public ForkManager getParent()
	{ return parent; }

	public void setParent(ForkManager parent_)
	{ parent = parent_; }

	public void setRuntime(ACARuntime runtime_)
	{
		if (parent == runtime) parent = runtime_;
		runtime = runtime_;
	}

	public ACARuntime getRuntime()
	{ return runtime; }
}
