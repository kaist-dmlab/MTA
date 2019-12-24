// @(#)TclComponent.java   10/2003
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

package drcl.inet;

import drcl.comp.*;
import tcl.lang.*;

/**
 * Component hook prototype for protocol development in Tcl.
 *
 * Usage:
 * (1) for each TclComponent created in the script, call
 *     {@link #init(Interp, Object, String)} to initialize the components.
 * (2) implement two Tcl procedures: "tclcomp_process" and "tclcomp_cmd",
 *     see below for details.
 *
 * tclcomp_process id comp data downPort:
 * - invoked when data arrives at the downport of a TclComponent instance 
 * - id: same id that is passed to each TclComponent instance in initialization
 * - comp: reference to the TclComponent instance
 * - data: arrived data
 * - downPort: where data been received 
 *
 * tclcomp_cmd args:
 * - user command line interface
 * - 1st argument: id that was assigned to the TclComponent in initialization
 * - 2nd argument: reference to the TclComponent instance
 * - 3rd argument: command
 * - more argument if any: arguments to the command
 *
 * In the implementation of these procedures, one may access the TclComponent
 * via the reference that is passed to the procedures.  In particular,
 * TclComponent provides a {@link #forward(Object, int, long)} to make it
 * easier for sending some data to the network.
 */
public class TclComponent extends Protocol
{
	Interp interp;
	Object lock; // lock for accessing the interpreter
	String compID;

	public TclComponent()
	{ super(); }

	public TclComponent(String id_)
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
	 * @param id_ ID assigned to this TclComponent; will be passed to the
	 * 				defined Tcl procedures.
	 */
	public void init(Interp interp_, Object lock_, String id_)
	{
		interp = interp_;
		lock = lock_;
		compID = id_;
		try {
			interp.eval("global " + compID + "_comp");
			interp.setVar(compID + "_comp", ReflectObject.newInstance(interp,
									TclComponent.class, this), 0/*flag?*/);
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}

	protected void dataArriveAtDownPort(Object data_, Port inPort_)
	{
		synchronized (lock) {
			try {
				interp.setVar("arg1_", ReflectObject.newInstance(interp,
									Object.class, data_), 0/*flag?*/);
				interp.setVar("arg2_", ReflectObject.newInstance(interp,
									Port.class, inPort_), 0/*flag?*/);
				interp.eval("tclcomp_process " + compID
						   	+ " $" + compID + "_comp $arg1_ $arg2_");
			}
			catch (Exception e_) {
				e_.printStackTrace();
			}
		}
	}

	/** Hook to execute a Tcl command implemented in "tclcomp_cmd". */
	public void exec(String cmd_)
	{
		if (cmd_ == null || cmd_.trim().length() == 0) return;
		synchronized (lock) {
			try {
				interp.eval("global " + compID + "_comp;"
								+ "tclcomp_cmd " + compID + " $" + compID
								+ "_comp " + cmd_);
			}
			catch (Exception e_) {
				e_.printStackTrace();
			}
		}
	}

	/**
	 * Sends data to the network.  Data is wrapped in drcl.inet.InetPacket.
	 * @param pkt_ packet body.
	 * @param size_ packet size. 
	 * @param dest_ destination address. 
	 */
	public void forward(Object pkt_, int size_, long dest_)
	{
		super.forward(pkt_, size_, drcl.net.Address.NULL_ADDR, dest_, 
						false/*router alert*/, 255/*ttl*/, 0/*tos*/);
	}
}
