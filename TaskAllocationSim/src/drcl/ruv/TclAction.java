// @(#)TclAction.java   10/2003
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

package drcl.ruv;

import drcl.comp.*;
import tcl.lang.*;
import java.util.*;

/**
 * The component executes a Tcl script upon receipt of an event.
 * One needs to call {@link #init(Interp, Object)} to initialize
 * the component with appropriate Tcl interpreter
 * and sets up actions (Tcl scripts) to take with
 * {@link #addAction(Object, String)} and/or
 * {@link #setUniversalAction(String)}.
 */
public class TclAction extends drcl.comp.Extension
{
	Interp interp;
	Object lock; // lock for accessing the interpreter
	String universalAction; // a Tcl script to execute in response to any signal
	Map actionMap; // signal (Object) -> action (Tcl script in String)

	public TclAction ()
	{ super(); }

	public TclAction(String id_)
	{ super(id_); }

	/**
	 * Initializes the component. 
	 * Usually, lock_ is the drcl.ruv.Shell object that contains the
	 * interpreter if the Shell has its own thread to accept commands from
	 * a terminal.  If the interpreter is dedicated to this purpose,
	 * then lock_ can be the interpreter itself.
	 *
	 * @param interp_ the Jacl interpreter instance.
	 * @param lock_ to make sure only one thread is accessing interp_ at a time.
	 */
	public void init(Interp interp_, Object lock_)
	{
		interp = interp_;
		lock = lock_;
	}

	/** Sets the Tcl script to execute in response to signal_ received.
	 * @param action_ the Tcl script to execute.
	 */ 
	public void addAction(Object signal_, String action_)
	{
		if (actionMap == null) actionMap = new HashMap();
		actionMap.put(signal_, action_);
	}

	/** Sets the Tcl script to execute in response to any signal received.
	 * This action is executed after the designated action (to the signal)
	 * is executed. 
	 */ 
	public void setUniversalAction(String action_)
	{
		universalAction = action_;
	}

	public String getUniversalAction()
	{ return universalAction; }

	/** Sets the action map that associates the signals with actions. */
	public void setActionMap(Map map_)
	{
		actionMap = map_;
	}

	/** Returns the action map. */
	public Map getActionMap()
	{ return actionMap; }

	protected void process(Object data_, Port inPort_)
	{
		synchronized (lock) {
			try {
				if (actionMap != null) {
					String action_ = (String)actionMap.get(data_);
					if (action_ != null) interp.eval(action_);
				}
				if (universalAction != null)
					interp.eval(universalAction);
			}
			catch (Exception e_) {
				e_.printStackTrace();
			}
		}
	}

	public String info()
	{
		return "interp = " + interp + "\nlock = " + lock
				+ "\nuniversalAction = " + universalAction
				+ "\nactionMap = " + actionMap + "\n";
	}
}
