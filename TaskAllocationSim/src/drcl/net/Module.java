// @(#)Module.java   1/2004
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

package drcl.net;

import drcl.comp.*;

/**
A module is a component which implements a protocol.  The ports of a module are 
categorized into "up" and "down" groups.  "Up" and "down" indicate the direction
of data flow in the protocol stack of a node.
By default, it contains one "up" port (<code>up@</code>)
and one "down" port (<code>down@</code>).

<p>A module is also equipped with a timer port (<code>.timer@</code>) and defines
a set of methods to set up and cancel timeout events
({@link #setTimeout(Object, double)}, {@link #setTimeoutAt(Object, double)}, and
{@link #cancelTimeout(drcl.comp.ACATimer)}.  The first two methods return an
{@link drcl.comp.ACATimer ACATimer}
object which contains the event object and the time when the timer expires.
The timer object can be used to cancel the timeout event using
{@link #cancelTimeout(ACATimer)}.

<p>Incoming data is dispatched in {@link #process(Object, drcl.comp.Port)
process(Object, Port)} to four handlers by the port at which data arrives:
{@link #dataArriveAtUpPort(Object, drcl.comp.Port) dataArriveAtUpPort(Object, Port)},
{@link #dataArriveAtDownPort(Object, drcl.comp.Port) dataArriveAtDownPort(Object, Port)},
{@link #timeout(Object)},
and {@link #processOther(Object, drcl.comp.Port) processOther(Object, Port)}.
A subclass should override one or more of the handlers to handle the incoming data
as necessary.
 */
public class Module extends drcl.comp.Component
{
	/** The ID of the "up" port group. */
	public static final String PortGroup_UP   = "up";

	/** The ID of the "down" port group. */
	public static final String PortGroup_DOWN = "down";
	
	/** The default "down" port. */
	public Port downPort = addPort(PortGroup_DOWN, false/*not removable*/);
	/** The default "up" port. */
	public Port upPort = addPort(PortGroup_UP, false/*not removable*/);
	
	/** The port at which the timeout events come.
	@see #setTimeout(Object, double)
	@see #setTimeoutAt(Object, double)
	 */
	protected Port timerPort = addForkPort(".timer");

	public Module()
	{ super(null); }
	
	public Module(String id_)
	{ super(id_); }

	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	/**
	 * Delivers <code>data_</code> at the (up) port specified.
	 * Returns false if failed (e.g. port does not exist).
	 */
	protected final boolean deliver(Object data_, String portID_) 
	{
		try {
			getPort(PortGroup_UP, portID_).doSending(data_);
			return true;
		}
		catch (Exception e_) {
			return false;
		}	
	}
	
	/**
	 The handler invoked when a packet arrives at a "down" port.
	 Subclasses should override it to handle such an event.
	 */
	protected void dataArriveAtDownPort(Object data_, Port downPort_) 
	{
		if (isErrorNoticeEnabled())
			error(data_, "processOther()", downPort_, "data not processed");
	}
	
	/**
	 The handler invoked when a packet arrives at an "up" port.
	 Subclasses should override it to handle such an event.
	 */
	protected void dataArriveAtUpPort(Object data_,  Port upPort_) 
	{
		if (isErrorNoticeEnabled())
			error(data_, "processOther()", upPort_, "data not processed");
	}
	
	/**
	 The handler invoked when a timeout event occurs.
	 Subclasses should override it to handle such an event.
	 @see #setTimeout(Object, double)
	 @see #setTimeoutAt(Object, double)
	 */
	protected void timeout(Object data_)
	{
		if (isErrorNoticeEnabled())
			error(data_, "timeout()", timerPort, "data not processed");
	}
	
	/**
	 The handler invoked when a packet arrived at a port other than the
	 "up", "down" and timer ports. 
	 */
	protected void processOther(Object data_, Port inPort_)
	{
		if (isErrorNoticeEnabled())
			error(data_, "processOther()", inPort_, "data not processed");
	}
	
	/** This method classifies <code>inPort_</code> and delegates process of data
	to the appropriate handler.  If a subclass decides to override this method,
	then it should call <code>super.process(data_, inPort_)</code> to make those
	handlers effective.
	@see #dataArriveAtUpPort(Object, drcl.comp.Port)
	@see #dataArriveAtDownPort(Object, drcl.comp.Port)
	@see #processOther(Object, drcl.comp.Port)
	*/
	protected /*final*/ void process(Object data_, drcl.comp.Port inPort_) 
	{
		String gname_ = inPort_.groupID;
		
		if (inPort_ == downPort)
			dataArriveAtDownPort(data_, inPort_);
		else if (inPort_ == upPort)
			dataArriveAtUpPort(data_, inPort_);
		else if (gname_.equals(PortGroup_DOWN))
			dataArriveAtDownPort(data_, inPort_);
		else if (gname_.equals(PortGroup_UP))
			dataArriveAtUpPort(data_, inPort_);
		else if (inPort_ == timerPort)
			timeout(data_);
		else processOther(data_, inPort_);
	}
	
	/** Sets up a timeout event at the specified absolute time.  
	 * Returns a timer object that can be used to cancel the event. 
	 * @see #timeout(Object) */
	public final ACATimer setTimeoutAt(Object evt_, double time_)
	{
		if (timerPort == null) {
			error(evt_, "setTimeoutAt()", infoPort, "no timerPort is defined");
			return null;
		}
		return forkAt(timerPort, evt_, time_);
	}
	
	/** Sets up a timeout event at the specified time later.
	 * Returns a timer object that can be used to cancel the event. 
	 * @see #timeout(Object) */
	public final ACATimer setTimeout(Object evt_, double duration_)
	{
		if (timerPort == null) {
			error(evt_, "setTimeout()", infoPort, "no timerPort is defined");
			return null;
		}
		return fork(timerPort, evt_, duration_);
	}
	
	/** Cancels a timeout event. */
	public final void cancelTimeout(ACATimer handle_)
	{
		if (timerPort == null) {
			error(handle_, "cancelTimeout()", infoPort, "no timerPort is defined");
			return;
		}
		cancelFork(handle_);
	}
	
	/** Removes the equipped down port (<code>down@</code>), if it is not used by this module.
	This method should be called in the construction code block. */
	protected void removeDefaultDownPort()
	{ downPort.setRemovable(true); removePort(downPort); downPort = null; }

	/** Removes the equipped up port (<code>up@</code>), if it is not used by this module.
	This method should be called in the construction code block. */
	protected void removeDefaultUpPort()
	{ upPort.setRemovable(true); removePort(upPort); upPort = null; }

	/** Removes the equipped timer port (<code>.timer@</code>), if it is not used by this module.
	This method should be called in the construction code block. */
	protected void removeTimerPort()
	{ timerPort.setRemovable(true); removePort(timerPort); timerPort = null; }
}
