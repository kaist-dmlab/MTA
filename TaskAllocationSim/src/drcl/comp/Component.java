// @(#)Component.java   1/2004
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
import java.io.*;
import drcl.util.StringUtil;
import drcl.util.queue.*;
import drcl.util.scalar.IntSpace;
import drcl.data.*;
import drcl.comp.contract.*;

//XXX: root component?

/**
<code>Component</code> is the base class for implementing components in the
<i>Autonomous Component Architecture</i> (ACA).

<p>A component in ACA is analogous to an IC chip in digital circuit design.
As pins to an IC chip, {@link Port ports} are the only means with which a
component is interfaced with the rest of the system.  And as the
specification in a data cookbook in digital circuit desgin, a component is
fully specified by {@link Contract contracts}.  A contract describes how a
a component expects and responds what format of data to arrive and send
at which ports.

<p>When data arrives at a component, a new thread is spawned and carries
out the {@link #process(Object, Port) process()} method to process the
data at the component.  The data processing must follow the contract(s)
that are obtained by {@link #getContract(Port)}.

There are a rich set of data structures and methods defined in a component.
Some of which are facilities that facilitate using a component in a system.
Some of which are methods defined in ACA which a subclass must implement.
Some of which are API's that writing a component subclass may take advantage of.
All these are categorized and described as follows:
<ul>
<li> Component hierarchy:<br>A component may act as a container and contain
     other components to form a component hierarchy.  Use the set of
     {@link #addComponent(Component) addComponent()} and
     {@link #removeComponent(Component) removeComponent()}
     methods to manipulate the component hierarchy.
<li> Creating/removing ports:<br>Use the set of {@link #addPort(String) addPort()}
     and {@link #removePort(String) removePort()} methods, or use specialized
	 methods ({@link #addEventPort(String)}, {@link #addForkPort(String)},
	 {@link #addServerPort(String)}) to create ports in defined port groups.
<li> System operation: {@link #reboot()} to reset the component hierarchy
     and the associated runtime.
<li> {@link ActiveComponent} operations: One may use (1) {@link #run()} to
     start all the ActiveComponents under this component hierarchy;
     (2) {@link #stop()} to stop all the ActiveComponents under this component
     hierarchy; and (3) {@link #resume()} to resume all the ActiveComponents
     under this component hierarchy.  The {@link #operate(double, String) operate()}
     method provides a unified and general form of the above three operations.
     <br>An ActiveComponent subclass should implement {@link #_start()},
     {@link #_stop()} and {@link #_resume()} to make these operations work. 
<li> Utility methods: A component subclass must implement (1) {@link #reset()}
     to reset this component to the initial state; (2) {@link #duplicate(Object)}
     to duplicate the contents of another component; and (3) {@link #info()}
     to provide the current states of this component for varifying the
     operations of this component at run-time.
<li> The information port (<code>infoport</code>): every component is
     equipped with an <code>infoport</code>.  A component may export 
     different types of messages at this port:
     <ul>
     <li> {@link ErrorContract Error message}: An error
          message is exported when some error occurs in processing some
          data.  Subclasses should use one of the {@link #error(String, Object) error()}
          methods to send out an error message.
     <li> {@link GarbageContract Garbage message}: A garbage
          message is exported when data is unwanted and discarded by the
          component due to the component's capacity or defined policy.
          Subclasses should use one of the {@link #drop(Object) drop()}
          methods to send out a garbage message.
     <li> {@link TraceContract Trace message}: A trace
          message is exported when data arrives or is sent out at a port.
          Exports of trace messages are implemented in the base class.
          Subclasses do not need to send out such messages.
     <li> {@link DebugContract Debug message}: Subclasses may
          export debug messages whenever appropriate to facilitate diagnosing
          an erroneous component.
          Subclasses should use {@link #debug(Object)} to send out a debug
          message.
     </ul>
<li> A 64-bit "register": The register contains (i) flags that control export
     of different types of messages at <code>infoport</code> and event ports.
     (ii) {@link ActiveComponent} states, and (iii) debug level bits, each
     of which can be used to enable/disable export of debug messages at a
     debug level.
<table border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td>
      <table width="100%" cellspacing="0" cellpadding="0">
        <tr> <td>63 <td align=right>32&nbsp; </tr>
      </table>
    <td>
      <table width="100%" cellspacing="0" cellpadding="0">
        <tr> <td>31 <td align=right>16&nbsp; </tr>
      </table>
    <td>
      <table width="100%" cellspacing="0" cellpadding="0">
        <tr> <td>15 <td align=right>12&nbsp; </tr>
      </table>
    <td align=center width=5%>11
    <td align=center width=5%>10
    <td align=center width=5%>9
    <td align=center width=5%>8
    <td align=center width=5%>7
    <td align=center width=5%>6
    <td align=center width=5%>5
    <td align=center width=5%>4
    <td align=center width=5%>3
    <td align=center width=5%>2
    <td align=center width=5%>1
    <td align=center width=5%>0
  </tr>
  <tr>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>(unused) </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% nowrap align=center>Debug Levels </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>(unused) </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>SP </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>ST </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100%>DOE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>PN </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>CN </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>VE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>TE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>DE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100%>GDE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>GE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>EE </tr> </table>
    <td> <table border=1 width=100%> <tr> <td width=100% align=center>E </tr> </table>
  </tr>
</table>
The flags are described in the following table.
<table border=1>
<tr> <td align=center> FLAG <td nowrap align=center> FLAG NAME <td align=center> DESCRIPTION </tr>
<tr>
   <td valign=top> E <td valign=top nowrap>Enabled
   <td> If disabled, the component does not respond to any arriving data. Default is enabled.
</tr>
<tr>
   <td valign=top> EE <td valign=top nowrap>ErrorEnabled
   <td> If disabled, the component suppresses
        {@link ErrorContract error messages}
        being sent out.  Default is enabled.
</tr>
<tr>
   <td valign=top> GE <td valign=top nowrap>GarbageEnabled
   <td> If disabled, the component suppresses
        {@link GarbageContract garbage messages}
        being sent out.  Default is disabled.
</tr>
<tr>
   <td valign=top> GDE <td valign=top nowrap>GarbageDisplayEnabled
   <td> The <i>displayable</i> flag of the
        {@link GarbageContract garbage messages}
        being sent out is set to the value of this flag.  Default is disabled.
</tr>
<tr>
   <td valign=top> DE <td valign=top nowrap>DebugEnabled
   <td> If disabled, the component suppresses
        {@link DebugContract debug messages}
        being sent out.  Default is disabled.
</tr>
<tr>
   <td valign=top> TE <td valign=top nowrap>TraceEnabled
   <td> If disabled, the component suppresses
        {@link TraceContract trace messages}
        being sent out.  Default is disabled.
</tr>
<tr>
   <td valign=top> VE <td valign=top nowrap>EventEnabled
   <td> If disabled, the component suppresses
        {@link EventContract event messages}
        being sent out.  Default is enabled.
</tr>
<tr>
   <td valign=top> CN <td valign=top nowrap>ComponentNotification
   <td> If enabled, {@link #componentAdded(Component)}/
        {@link #componentRemoved(Component)} is invoked whenever a component
		is added in/removed from this component.  Default is disabled.
</tr>
<tr>
   <td valign=top> PN <td valign=top nowrap>PortNotification
   <td> If enabled, {@link #portAdded(Port)}/{@link #portRemoved(Port)} is 
        invoked whenever a port is added to/removed from this component.
		Default is disabled.
</tr>
<tr>
   <td valign=top> DOE <td valign=top nowrap>DirectOutputEnabled
   <td> If enabled, all the above messages are sent to standard output directly
        instead of being sent out at <code>infoport</code>.
		Default is disabled.
</tr>
<tr>
   <td valign=top> ST <td valign=top nowrap>Started
   <td> This flag is set if this component is started as {@link ActiveComponent}
        (see {@link #run}).
        It is reset when the component is reset ({@link #reset()}).
        This flag is effective only if this component is an
        {@link ActiveComponent}.
</tr>
<tr>
   <td valign=top> SP <td valign=top nowrap>Stopped
   <td> This flag is set if the component is stopped (by {@link #stop()}) and
        has not been resumed ({@link #resume()}).  The flag is reset if the
		component is reset ({@link #reset()}) or resumed ({@link #resume()}).
		This flag is effective only if this component is an
        {@link ActiveComponent}.
</tr>
</table>
Each flag can be tested/set with the pair of the corresponding <i>getter</i> and
<i>setter</i> methods.  For example, use {@link #isDebugEnabled()} to check if
the debug flag is set, and {@link #setDebugEnabled(boolean)} to enable/disable
the debug flag of this component.
<br>The higher 32 bits of the register are undefined.  Subclasses may define
their own flags on those bits.
<li> Debug levels: A subclass may classify its debug messages into debug levels.
     One may use one of
     {@link #setDebugEnabledAt(boolean, int) setDebugEnabledAt()}/
     {@link #isDebugEnabledAt(int) isDebugEnabledAt()} to control/check export
     of debug messages of a debug level.
     Debug levels are only valid when the debug flag is enabled.
     To make the above methods usable, a subclass must implement
     {@link #getDebugLevelNames()}.
<li> Component/port added notification: To enable either one, a subclass should
     use {@link #setComponentNotificationEnabled(boolean)} or
     {@link #setPortNotificationEnabled(boolean)}
	 and then implement {@link #componentAdded(Component)}/
     {@link #componentRemoved(Component)}
	 or {@link #portAdded(Port)}/{@link #portRemoved(Port)}.
<li> APIs for writing a subclass: {@link #getTime()}, time control
     ({@link #sleepFor(double)}, {@link #sleepUntil(double)}), mutex
	 ({@link #lock(Object)}, {@link #unlock(Object)}), synchronization
	 ({@link #wait(Object)}, {@link #notify(Object)},
	  {@link #notifyAll(Object)}),
	 fork events ({@link #fork(Port, Object, double)},
	 {@link #forkAt(Port, Object, double)}, {@link #send(Port, Object, double)},
	 {@link #sendAt(Port, Object, double)}, and {@link #cancelFork(ACATimer)}),
	 binding contracts ({@link #setContract(Class, String, Contract)}).
</ul>
 * 
 * @see Port
 * @see Contract
 * @see ContractMultiple
 */
public class Component extends drcl.DrclObj
{
	 // Predefined port group IDs.
	 
	/** The default port group. */
	public static final String PortGroup_DEFAULT_GROUP = "";
	/** The event port group. */
	public static final String PortGroup_EVENT = "event";
	/** The service port group. */
	public static final String PortGroup_SERVICE = "service";
	
	
	static final String INFO_PORT_ID = ".info";
	
	
	/**
	 * Identification, must be unique within the parent component.
	 * ID must be set using setID() or setID(String), otherwise
	 * no check is done for components with duplicate ID.
	 */
	public String id;
	
	/**
	 * Descriptive name for the component.
	 * Default is equal to the name of the class.
	 */
	public String name = null;//StringUtil.finalPortionClassName(getClass());
	
	/** Parent component. */
	public Component parent;

	PortManager portManager = new PortManager();

	/**
	 * The information port of this component.
	 * A component sends out useful information regarding
	 * the operations of this component at this port.  Specifically, a component may send out
	 * send out the following types of messages at this port:
	 * {@link ErrorContract error},
	 * {@link GarbageContract garbage},
	 * {@link TraceContract trace}, and
	 * {@link DebugContract debug}.
	 */
	public final Port infoPort = _addPort(new InfoPort(), INFO_PORT_ID, false/* not removable*/);

	//
	private  void ___INFO___() {}
	//
	
	// flags
	static final int FLAG_DEBUG_LEVEL_START               = 16;
	/** Bit mask of the enabled flag. */
	public static final int FLAG_ENABLED                  = 1 << 0;
	/** Bit mask of the enabled flag. */
	public static final int FLAG_ERROR_ENABLED            = 1 << 1;
	public static final int FLAG_GARBAGE_ENABLED          = 1 << 2;
	public static final int FLAG_GARBAGE_DISPLAY_ENABLED  = 1 << 3;
	public static final int FLAG_DEBUG_ENABLED            = 1 << 4;
	public static final int FLAG_TRACE_ENABLED            = 1 << 5;
	public static final int FLAG_EVENT_ENABLED            = 1 << 6;
	public static final int FLAG_COMPONENT_NOTIFICATION   = 1 << 7;
	public static final int FLAG_PORT_NOTIFICATION        = 1 << 8;
	public static final int FLAG_DIRECT_OUTPUT_ENABLED    = 1 << 9;
	public static final int FLAG_STARTED                  = 1 << 10;
	public static final int FLAG_STOPPED                  = 1 << 11;
	//public static final int FLAG_EXPOSED                  = 1 << 12;
	//public static final int FLAG_HAS_EXPOSED_CHILD        = 1 << 13;
	static final int FLAG_OMIT_DUPLICATE             = 1 << 14; // internal use
	static final int FLAG_GARBAGE_ENABLEDS                = FLAG_GARBAGE_DISPLAY_ENABLED | FLAG_GARBAGE_ENABLED;
	static final int NUM_DEBUG_LEVELS                     = 16;
	public static final int FLAG_UNDEFINED_START          = FLAG_DEBUG_LEVEL_START + NUM_DEBUG_LEVELS;
	static final int DEBUG_FLAG_BASE            = 4; // 2 ^ DEBUG_FLAG_BASE must >= NUM_DEBUG_LEVELS
	// By default, only error is turned on.
	long flag = FLAG_ENABLED | FLAG_ERROR_ENABLED | FLAG_EVENT_ENABLED;
	
	/** Data arrival trace type. */
	public static final String Trace_DATA	= "DATA";
	/** Data sending trace type. */
	public static final String Trace_SEND	= "SEND";
	
	// Stores child components with ID as the index.
	Hashtable hchild;
	
	ACARuntime runtime = null;
	ForkManager fm = null;
	
	
	//
	private  void ___CONSTRUCTOR_INIT___() {}
	//
	
	public Component() 
	{	this((String)null);	}
	
	/** @param id_ the component ID. */
	public Component(String id_)
	{	setID(id_);	}
	
	/**
	 * Resets the component for being used anew.
	 * All the internal variables must be set to the initial state.
	 * All the ports and child components are reset as well.
	 */
	public synchronized void reset()
	{
		//System.out.println("reset component: " + this);
		portManager.reset();
		setComponentFlag(FLAG_STARTED | FLAG_STOPPED, false);
		if (fm != null) fm.reset();
		locks = null;

		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) cc_[i].reset();
	}
	
	/**
	 * Resets the component and the associated runtime.
	 * This method is usually used to reset a component system that
	 * is associated with a runtime.
	 * All the internal variables must be set to the initial state.
	 * All the ports and child components are reset as well.
	 */
	public synchronized void reboot()
	{
		//System.out.println("reboot from: " + this);
		try {
			runtime.reboot();
			reset();
		}
		catch (Throwable e_) {
			e_.printStackTrace();
		}
	}
	
	
	
	/**
	 * This method is invoked in a new thread context when {@link #run()} is called.
	 * An {@link ActiveComponent} subclass must override this method to start this component.
	 */
	protected void _start()	{}
	
	/**
	 * This method is invoked in a new thread context when {@link #stop()} is called.
	 * An {@link ActiveComponent} subclass must override this method to stop this component.
	 */
	protected void _stop()	{}
	
	/**
	 * This method is invoked in a new thread context when {@link #resume()} is called.
	 * An {@link ActiveComponent} subclass must override this method to resume this component.
	 */
	protected void _resume()	{}
	
	
	/** Starts all the active components under this component hierarchy. */
	public void run()
	{ operate(0.0, Util.OP_START); }
	
	/** Stops all the active components under this component hierarchy. */
	public final void stop()
	{ operate(0.0, Util.OP_STOP); }
	
	/** Resumes all the active components under this component hierarchy. */
	public final void resume()
	{ operate(0.0, Util.OP_RESUME); }
	
	/**
	 * Operates all the active components under this component hierarchy.
	 * Available operators are "start", "stop" and "resume".  This is the 
	 * general form of {@link #run()}, {@link #stop()} and {@link #resume()}.
	 * @param time_ operating the component at the specified time later.
	 * @param op_ the operator.
	 */
	public final void operate(double time_, String operation_)
	{
		int op_ = operation_.equals(Util.OP_START)? Task.TYPE_START:
			operation_.equals(Util.OP_STOP)? Task.TYPE_STOP:
			operation_.equals(Util.OP_RESUME)?
			Task.TYPE_RESUME: Task.TYPE_UNKNOWN;
		if (op_ == Task.TYPE_UNKNOWN) {
			drcl.Debug.error("Unknown operation: " + op_);
			return;
		}
		Vector vtasks_ = Util._operate(this, time_, op_, null);
		if (vtasks_ != null && vtasks_.size() > 0)
			runtime.newTasks(vtasks_);
	}

	//
	private  void ___PROPERTIES___() {}
	//
	
	/** Returns the parent component. */
	public Component getParent() { return parent; }
	
	/**
	 * Set the parent component.  Just changes the parent property,
	 * does not check the unqueness of the id.  Usually called when the component
	 * is added to the parent by parent.addComponent() method.
	 * All the dirty work is done there.
	 */
	void setParent(Component parent_) 
	{
		parent = parent_;
		// FIXME: this is ugly
		if (fm != null)
			fm.setName("FM_" + this);
	}
	
	/**
	 * Sets the identification of the component.
	 * It returns false if same id exists for some component
	 * within the same parent.  Id can be assigned before the component
	 * is added to a parent component.
	 */
	public void setID(String newID_) 
	{
		if (parent == null) {
			id = newID_ == null? null: newID_.intern();
			return;
		}
		if (newID_ == null) {
			throw new SetIDException("Trying to set null ID!");
		}
		newID_ = newID_.intern();
		Component c_ = (Component)parent.hchild.get(newID_);
		if (c_ == this) return;
		else if (c_ != null) {
			throw new SetIDException(parent, newID_);
		}
			
		parent.hchild.remove(id);
		parent.hchild.put(newID_, this);
		id = newID_;
	}

	// avoid using String.intern()
	void _setID(String newID_) 
	{
		if (parent == null) {
			id = newID_;
			return;
		}
		if (newID_ == null) {
			throw new SetIDException("Trying to set null ID!");
		}
		Component c_ = (Component)parent.hchild.get(newID_);
		if (c_ == this) return;
		else if (c_ != null) {
			throw new SetIDException(parent, newID_);
		}
			
		parent.hchild.remove(id);
		parent.hchild.put(newID_, this);
		id = newID_;
	}
	
	/**
	 * This method automatically finds a valid ID in the parent component
	 * for this component.
	 * The naming rule and search order is <code>class_name</code>,
	 * <code>class_name(2)</code>,
	 * <code>class_name(3)</code> and so on.
	 */
	public String setID() 
	{
		String prefix_ = StringUtil.finalPortionClassName(
						getClass()).toLowerCase();
		String id_;
		for (int i=1; ;i++)	{
			id_ = prefix_ + (i==1? "": "(" + i + ")");
			try {
				setID(id_);
				return id_;
			}
			catch (SetIDException e_) {}
		}
	}
	
	/** Returns the identification of the component. */
	public String getID()
	{ return id; }
	
	/** Sets the (informational) name of this component.
	 * By default, the name of a component is set to its class name. */
	public void setName(String name_)
	{ name = name_; }
	
	/** Returns the (informational) name of the component. */
	public String getName()
	{ return name == null? StringUtil.finalPortionClassName(getClass()): name; }
		
	/** Returns the full path of the component. */
	public String toString() { return Util.getFullID(this); }
	
	/** Returns true if this component is an {@link ActiveComponent} and
	 * has started to run (using the {@link #run()} method).
	 * A started component
	 * cannot be started again until being reset ({@link #reset()}).
	 */
	public final boolean isStarted() 
	{ return getComponentFlag(FLAG_STARTED) != 0; }
	
	final void setStarted(boolean active_)
	{	setComponentFlag(FLAG_STARTED, active_);	}
	
	public final boolean isStopped() 
	{ return getComponentFlag(FLAG_STOPPED) != 0; }
	
	final void setStopped(boolean active_)
	{	setComponentFlag(FLAG_STOPPED, active_);	}
	
	//
	private  void ___FOR_PORT___() {}
	//
	
	/**
	 * The main callback method of a component.
	 * Being invoked when data arrives at a port of its.
	 * @param data_ the arrival data.
	 * @param inPort_ the port where the data arrives.
	 */
	protected void process(Object data_, Port inPort_) 
	{ 
		// process data...
	}
	
	//
	private  void ___PORT_MANIPULATION___() {}
	//
	
	/** Adds an event port to the default port group of this component. */
	public final Port addEventPort(String pid_)
	{
		return _addPort(new Port(Port.PortType_EVENT), pid_.intern(),
						false/*not removable*/);
	}

	/** Adds an event port to the specified port group of this component. */
	public final Port addEventPort(String pgid_, String pid_)
	{
		return _addPort(new Port(Port.PortType_EVENT), pgid_.intern(),
						pid_.intern(), false/*not removable*/);
	}

	/** Adds a server port to the default port group of this component. */
	public final Port addServerPort(String pid_)
	{
		return _addPort(new Port(Port.PortType_SERVER), pid_.intern(),
						false/*not removable*/);
	}

	/** Adds a server port to the specified port group of this component. */
	public final Port addServerPort(String pgid_, String pid_)
	{
		return _addPort(new Port(Port.PortType_SERVER), pgid_.intern(),
						pid_.intern(), false/*not removable*/);
	}

	/** Adds a fork port to the default port group of this component. */
	public final Port addForkPort(String pid_)
	{ return _addPort(new Port(Port.PortType_FORK), pid_.intern(),
					false/*not removable*/); }

	// called when a port is added to this component, handles exposed child
	// components
	void _portAdded(Port p_)
	{
		Port tmp_ = null;
		/*
		if (getComponentFlag(FLAG_HAS_EXPOSED_CHILD) != 0) {
			Component[] cc_ = getAllComponents();
			for (int i=0; i<cc_.length; i++) {
				Component c_ = cc_[i];
				if (c_.getComponentFlag(FLAG_EXPOSED) != 0)
					c_._addPort(p_.groupID, p_.id).connect(p_);
			}
		}
		*/
		if (getComponentFlag(FLAG_TRACE_ENABLED) != 0)
			p_.setTraceEnabled(true);
		if (getComponentFlag(FLAG_EVENT_ENABLED) == 0)
			p_.setEventExportEnabled(false);
		if (getComponentFlag(FLAG_PORT_NOTIFICATION) != 0)
			portAdded(p_);
	}
	
	void _portRemoved(Port p_)
	{
		/*
		if (p_.isShadow() && getComponentFlag(FLAG_HAS_EXPOSED_CHILD) != 0) {
			// FIXME: what's this for???
			Port[] pp_ = p_.getClients();
			if (pp_ != null)
				for (int i=0; i<pp_.length; i++)
					if (pp_[i] != null) {
						Port tmp_ = pp_[i];
						tmp_.host.removePort(tmp_);
						tmp_.disconnect(); 
					}
		}
		*/
		if (getComponentFlag(FLAG_PORT_NOTIFICATION) != 0)
			portRemoved(p_);
	}
	
	/** Returns the port given the group ID and the port ID. */
	public final synchronized Port getPort(String groupID_, String id_)
	{	return portManager.getPort(groupID_, id_); }
	
	/** Returns the port in the default group given the port ID. */
	public final synchronized Port getPort(String id_)
	{ return portManager.getPort(PortGroup_DEFAULT_GROUP, id_); }
	
	/** Returns all the ports of this component.  */
	public final synchronized Port[] getAllPorts()
	{ return portManager.getAllPorts();	}
	
	/** Returns all the ports of the port group in this component. */
	public final synchronized Port[] getAllPorts(String groupID_)
	{ return portManager.getAllPorts(groupID_);	}
	
	/** Removes all the ports. */
	public final synchronized void removeAllPorts()
	{ portManager.removeAllPorts();	}
	
	/** Removes all the ports of the port group. */
	public final synchronized void removeAllPorts(String groupID_)
	{ portManager.removeAllPorts(groupID_);	}
	
	/**
	 * Removes the port given the group id and the port id.  It does not
	 * disconnect the port.  The other <code>removePort()</code> methods
	 * end up with calling this method.
	 * 
	 * @param groupID_ group ID of the port.
	 * @param id_ ID of the port.
	 * @return the removed port.
	 */
	public final synchronized Port removePort(String groupID_, String id_)
	{ return portManager.removePort(groupID_, id_);	}

	/**
	 * Removes the port.  It does not disconnect the port.
	 * 
	 * @param p_ the port to be removed from this component.
	 * @return the port.
	 */
	public final synchronized Port removePort(Port p_)
	{ return portManager.removePort(p_.groupID, p_.id); }
	
	/**
	 * Removes the port in the default port group.  It does not disconnect
	 * the port.
	 * 
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port removePort(String id_)
	{ return portManager.removePort(PortGroup_DEFAULT_GROUP, id_);	}
	
	/**
	 * Replaces the port, the given group id and the port id, with
	 * <code>newPort</code>.  It does not disconnect the replaced port.
	 */
	public final synchronized void setPort(Port newPort_, String groupID_,
					String id_)
	{ portManager.setPort(newPort_, groupID_, id_); }
	
	/**
	 * Replaces the port in the default port group, given the port id, 
	 * with <code>newPort</code>.
	 * It does not disconnect the replaced port.
	 */
	public final synchronized void setPort(Port newPort_, String id_)
	{ portManager.setPort(newPort_, PortGroup_DEFAULT_GROUP, id_); }
	
	/**
	 * Adds a port with the given group id and the port id.
	 * It assigns an ID if the given id is null.
	 * 
	 * @param new_ the port to be added to this component.
	 * @param groupID_ group ID of the port.
	 * @param newID_ ID of the port.
	 * @return new_ or null if port of the same ID in the same group exists.
	 */
	public final synchronized Port addPort(Port new_, String groupID_,
					String newID_)
	{ return portManager.addPort(new_, groupID_.intern(), newID_.intern(),
					true);	}

	synchronized Port _addPort(Port new_, String groupID_, String newID_)
	{ return portManager.addPort(new_, groupID_, newID_, true);	}
	
	/**
	 * Adds a port with the given group id and the port id.
	 * It assigns an ID if the given id is null.
	 * 
	 * @param new_ the port to be added to this component.
	 * @param groupID_ group ID of the port.
	 * @param newID_ ID of the port.
	 * @return new_ or null if port of the same ID in the same group exists.
	 */
	public final synchronized Port addPort(Port new_, String groupID_,
					String newID_, boolean isRemovable_)
	{ return portManager.addPort(new_, groupID_.intern(), newID_.intern(),
					isRemovable_);	}

	synchronized Port _addPort(Port new_, String groupID_, String newID_,
										   boolean isRemovable_)
	{ return portManager.addPort(new_, groupID_, newID_, isRemovable_);	}
	
	/**
	 * Adds a port to the default port group with the given port id,
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(Port new_, String newID_)
	{ return portManager.addPort(new_, PortGroup_DEFAULT_GROUP,
					newID_.intern(), true);	}
	
	final synchronized Port _addPort(Port new_, String newID_)
	{ return portManager.addPort(new_, PortGroup_DEFAULT_GROUP, newID_,
					true);	}

	/**
	 * Adds a port to the default port group with the given port id,
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(Port new_, String newID_,
					boolean isRemovable_)
	{ return portManager.addPort(new_, PortGroup_DEFAULT_GROUP,
					newID_.intern(), isRemovable_);	}
	
	synchronized Port _addPort(Port new_, String newID_, boolean isRemovable_)
	{ return portManager.addPort(new_, PortGroup_DEFAULT_GROUP, newID_,
					isRemovable_);	}

	/**
	 * Creates a port in the default port group with the given port id,
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(String id_)
	{
		return portManager.addPort(new Port(), PortGroup_DEFAULT_GROUP,
						id_.intern(), true);
	}

	synchronized Port _addPort(String id_)
	{
		Port p_ = portManager.getPort(PortGroup_DEFAULT_GROUP, id_);
		if (p_ != null) return p_;
		else return portManager.addPort(new Port(), PortGroup_DEFAULT_GROUP,
						id_, true);
	}
	
	/**
	 * Creates a port in the default port group with the given port id,
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(String id_, boolean isRemovable_)
	{
		return portManager.addPort(new Port(), PortGroup_DEFAULT_GROUP,
						id_.intern(), isRemovable_);
	}
	
	synchronized Port _addPort(String id_, boolean isRemovable_)
	{
		Port p_ = portManager.getPort(PortGroup_DEFAULT_GROUP, id_);
		if (p_ != null) { setPortRemovable(p_, isRemovable_); return p_; }
		else return portManager.addPort(new Port(), PortGroup_DEFAULT_GROUP,
						id_, isRemovable_);
	}
	/**
	 * Creates a port with the given group id and the port id.
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param groupID_ group ID of the port.
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(String groupID_, String id_)
	{
		return portManager.addPort(new Port(), groupID_.intern(),
						id_.intern(), true);
	}
	
	synchronized Port _addPort(String groupID_, String id_)
	{
		Port p_ = portManager.getPort(groupID_, id_);
		if (p_ != null) return p_;
		return portManager.addPort(new Port(), groupID_, id_, true);
	}
	
	/**
	 * Creates a port with the given group id and the port id.
	 * It assigns an ID if the given id is null.
	 * If the port already exists, the port is returned.
	 * 
	 * @param groupID_ group ID of the port.
	 * @param id_ ID of the port.
	 * @return the port.
	 */
	public final synchronized Port addPort(String groupID_, String id_,
					boolean isRemovable_)
	{
		return portManager.addPort(new Port(), groupID_.intern(),
						id_.intern(), isRemovable_);
	}
	
	synchronized Port _addPort(String groupID_, String id_,
					boolean isRemovable_)
	{
		Port p_ = portManager.getPort(groupID_, id_);
		if (p_ != null) { setPortRemovable(p_, isRemovable_); return p_; }
		return portManager.addPort(new Port(), groupID_, id_, isRemovable_);
	}
	
	/** Returns true if the component owns the port. */
	public final synchronized boolean containsPort(Port p_)
	{ return portManager.containsPort(p_);	}
	
	/** Returns true if the port is removable from this component. */
	public final boolean isPortRemovable(Port p_)
	{ return portManager.isPortRemovable(p_);	}
	
	/** Sets the port to be (un)removable. */
	protected final void setPortRemovable(Port p_, boolean v_)
	{ portManager.setPortRemovable(p_, v_); }
	
	/**
	 stablishes a two-way connection between this and the specified component.
	 his method uses <code>findAvailable()</code> to get ports from
	 he components for connection.  By default, the method returns an
	 vailable port in the default port group.  Subclasses should override 
	 <code>findAvailable()</code> if should behave otherwise.
	 @param shared_ set to true if both directions of the connection should
	 share the same wire.
	 see Component.findAvailable()
	 see Component.findAvailable(String)
	 */
	public boolean connect(Component c_, boolean shared_)
	{
		if (c_ == null) return false;
		if (shared_) return findAvailable().connect(c_.findAvailable());
		else {
			Port mine_ = findAvailable();
			Port peer_ = c_.findAvailable();
			if (!mine_.connectTo(peer_)) return false;
			return peer_.connectTo(mine_);
		}
	}
	
	/**
	 * Returns an unconnected port from the default connection port group
	 * of this component.  By default, it uses the default port group.
	 * Subclasses should override this method if behave otherwise.
	 * @see Component#connect(Component, boolean)
	 * @see Component#findAvailable(String)
	 */
	public Port findAvailable()
	{ return findAvailable(PortGroup_DEFAULT_GROUP, Port.PortType_INOUT);	}
	
	/** Returns an unconnected port from the specified port group. */
	public final synchronized Port findAvailable(String groupID_)
	{ return findAvailable(groupID_, Port.PortType_INOUT); }
	
	/** Returns an unconnected port of the given port type from the specified
	 * port group. */
	public final synchronized Port findAvailable(String groupID_, int portType_)
	{
		Port p_ = null;
		Port[] pp_ = getAllPorts(groupID_);
		if (pp_ != null) {
			for (int i=0; i<pp_.length; i++)
				if (pp_[i] != infoPort && pp_[i] != null 
					&& pp_[i].getType() == portType_
					&& !pp_[i].anyPeer()) return pp_[i];
			IntSpace is_ = new IntSpace();
			//(IntSpace)drcl.RecycleManager.reproduce(IntSpace.class);
			for (int i=0; i<pp_.length; i++) {
				try {
					is_.checkout(Integer.parseInt(pp_[i].id));
				}
				catch (Exception e_) {}
			}
			//p_ = new Port();
			//p_.set(groupID_, String.valueOf(is_.checkout()));
			p_ = addPort(new Port(portType_), groupID_,
							String.valueOf(is_.checkout()));
			//drcl.RecycleManager.recycle(is_);
		}
		else {
			//p_ = new Port();
			//p_.set(groupID_, "0");
			p_ = addPort(groupID_, "0");
		}
		
		return p_;
	}
	/**
	 * Creates a port that proxies for the client port, specified by
	 * the child component, group id and port id.
	 * The method attempts to create the proxy port in the same port group
	 * (default group) with the same port id as those of the child port.
	 * If failed, it assigns an unused id to the proxy port.
	 */
	public Port exposePort(Component child_, String pid_)
	{
		return exposePort(child_, PortGroup_DEFAULT_GROUP, pid_,
						PortGroup_DEFAULT_GROUP, pid_);
	}
	
	/**
	 * Creates a shadow port for a port of the child component's, specified by
	 * the child component, the port group id and the port id.
	 * The method attempts to create the shadow port in the same port group with
	 * the same port id as those of the port of the child component.
	 * If the attempt fails, it assigns an unused id to the shadow port.
	 */
	public Port exposePort(Component child_, String groupID_, String id_)
	{
		return exposePort(child_, groupID_, id_, groupID_, id_);
	}
	
	/**
	 * Creates a shadow port, with the given group id and the port id,
	 * for a port of the child component, specified by the child component,
	 * the port group id and the port id.
	 */
	public Port exposePort(Component child_, String groupID_, String id_, 
						   String mygroupID_, String myID_)
	{
		Port client_ = child_.getPort(groupID_, id_);
		if (client_ == null) return null;
		
		Port p_ = addPort(mygroupID_, myID_);
		if (p_ == null) p_ = addPort(mygroupID_, null);
		if (p_ == null) return null;
		
		p_.connect(client_);
		p_.setType(client_.getType());
		return p_;
	}
	
	/**
	 * Creates a shadow port for the client port and makes it have the same
	 * group ID and port ID as the client's.
	 */
	public Port exposePort(Port client_)
	{	// same as exposePort(client_, client_.groupID, client_.id)
		if (client_.host.parent != this) return null;
		
		Port p_ = _addPort(client_.groupID, client_.id);
		if (p_ == null) p_ = addPort(client_.groupID, null);
		if (p_ == null) return null;
		
		p_.connect(client_);
		p_.setType(client_.getType());
		return p_;
	}
	
	/**
	 * Creates a shadow port, with the given group id and the port id,
	 * for the client port. 
	 */
	public Port exposePort(Port client_, String mygroupID_, String myID_)
	{
		if (client_.host.parent != this) return null;
		
		Port p_ = addPort(mygroupID_, myID_);
		if (p_ == null) p_ = addPort(mygroupID_, null);
		if (p_ == null) return null;
		
		p_.connect(client_);
		p_.setType(client_.getType());
		return p_;
	}
		
	/**
	 * Disconnects this component from the rest of the system.
	 * The internal connections remain.
	 */
	public synchronized void disconnectAllPeers()
	{
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++) {
			if (pp_[i] == infoPort) {
				if (infoPort.getType() != Port.PortType_IN)
						// not a probing component
					infoPort.outwire = null;
				infoPort.disconnect();
			}
			else
				pp_[i].disconnectPeers();
		}
	}
	
	// 
	private  void ___CONTAINER___() {}
	//

	/**
	 * Adds a child component.
	 * @throws AddComponentException if the component has null or duplicate ID.
	 */
	public synchronized void addComponent(Component c_) 
	{ addComponent(c_, true);	}
	
	/**
	 * Adds a child component.
	 * @throws AddComponentException if the component has null or duplicate ID.
	 */
	public synchronized void addComponent(Component c_, boolean inheritRuntime_) 
	{
		if (c_ == null) {
			throw new AddComponentException("Trying to add null in " + this
							+ "!");
		}
		
		if (c_.getID() == null) {
			throw new AddComponentException("Trying to add a component "
							+ "with null ID in " + this + "!");
		}
			
		if (hchild == null) {
			hchild = new Hashtable();
		}
		if (c_ == null || hchild.contains(c_)) return;
		if (c_.getParent() != null) {
			throw new AddComponentException(c_ + " is currently in "
							+ c_.getParent() + ", needs to remove it first.");
		}
		
		String id_ = c_.getID();
		if (hchild.containsKey(id_))
			throw new AddComponentException(this, id_);
		hchild.put(id_, c_);
		
		c_.setParent(this);
		if (inheritRuntime_) {
			if (runtime != null) runtime.takeover(c_);
			if (fm != null && fm != c_.fm) fm.takeover(c_, true/*as parent*/);
		}
		
		// connect c_'s info port to parent (this component)
		if (c_.infoPort.getType() == Port.PortType_IN) // probing component
			Root.infoPort.outwire.joinIn(c_.infoPort);
		else
			c_.infoPort.outwire = Root.infoPort.outwire;
		//else
		//	c_.infoPort.connectTo(infoPort);
		
		if (getComponentFlag(FLAG_COMPONENT_NOTIFICATION) != 0)
			componentAdded(c_);
	}
	
	/** Returns true if the specified component is an immediate child of this
	 * component. */
	public synchronized boolean containsComponent(Component c_)
	{ return c_.parent == this; }
	
	/**
	 * Returns true if the component hierarchy rooted at this component
	 * contains the specified component.
	 */
	public synchronized boolean isAncestorOf(Component c_)
	{
		c_ = c_.parent;
		while (c_ != null) {
			if (c_ == this) return true;
			c_ = c_.parent;
		}
		return false;
	}

	/** Returns true if one component contains the other's. */
	public synchronized boolean isDirectlyRelatedTo(Component c_)
	{ return this == c_ || isAncestorOf(c_) || c_.isAncestorOf(this); }
	
	/** Returns true if the ID matches one of its child components'. */
	public synchronized boolean containsComponent(String id_)
	{
		return hchild == null? false: hchild.containsKey(id_);
	}
	
	/**
	 * Removes a child from this component.
	 * This method does not disconnect the child component.
	 * @return the removed child component; null if this component does not
	 * contain the child.
	 */
	public synchronized Component removeComponent(Component c_) 
	{
		if (hchild == null) return null;
		if (hchild.contains(c_)) {
			//c_.disconnectAllPeers();
			hchild.remove(c_.getID());
			c_.parent = null;
			if (getComponentFlag(FLAG_COMPONENT_NOTIFICATION) != 0)
				componentRemoved(c_);
			return c_;
		}
		else return null;
	}
	
	/**
	 * Removes a child given the component ID.
	 * This method does not disconnect the child component.
	 * @return the removed child component; null if this component does not
	 * contain the child.
	 */
	public synchronized Component removeComponent(String id_)
	{
		Component child_ = getComponent(id_);
		if (child_ != null) removeComponent(child_);
		if (getComponentFlag(FLAG_COMPONENT_NOTIFICATION) != 0)
			componentRemoved(child_);
		return child_;
	}
	
	/** Returns all the child components. */
	public synchronized Component[] getAllComponents()
	{
		if (hchild == null) return new Component[0];
		Component[] children_ = new Component[hchild.size()];
		int i = 0;
		for (Enumeration e_ = hchild.elements(); e_.hasMoreElements(); )
			children_[i++] = (Component)e_.nextElement();
		return children_;
	}
	
	/** Returns true if this component is a container. */
	public synchronized boolean isContainer()
	{	return hchild != null && hchild.size() > 0;	}
	
	/**
	 * Removes all the child components.
	 * This method does not disconnect the child components.
	 */
	public synchronized void removeAllComponents()
	{
		/*
		Component[] children_ = getAllComponents();
		for (int i=0; i<children_.length; i++) {
			children_[i].disconnectAllPeers();
		}
		if (hchild != null) {
			hchild.clear();
		}
		*/ 
		Component[] children_ = getAllComponents();
		for (int i=0; i<children_.length; i++) {
			removeComponent(children_[i]);
		}
		//setComponentFlag(FLAG_HAS_EXPOSED_CHILD, false);
	}
	
	/** Returns the child given the component ID. */
	public synchronized Component getComponent(String id_)
	{
		if (hchild == null) return null;
		else return (Component)hchild.get(id_);
	}
	
	/** Removes all the children and ports.  The method does not disconnect
	 * them.
	 */
	public synchronized void removeAll()
	{
		removeAllComponents();
		removeAllPorts();
	}
	
	/**
	 * Makes the child component receives all the data as this parent component
	 * does.  All the child's ports are exposed at this time instant.
	 * Creation of all the ports at the parent (this) later creates a
	 * corresponding port at the child, but NOT vice versa.
	 * All ports created at this moment are subject to removal when
	 * {@link #unexpose(Component)} is called.
	 */
	public synchronized void expose(Component child_)
	{
		if (!containsComponent(child_)) return;
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++) {
			Port p_ = child_.getPort(pp_[i].groupID, pp_[i].id);
			if (p_ == null) p_ = child_._addPort(pp_[i].groupID, pp_[i].id);
			pp_[i].connect(p_);
		}
		
		pp_ = child_.getAllPorts();
		for (int i=0; i<pp_.length; i++) {
			Port p_ = getPort(pp_[i].groupID, pp_[i].id);
			if (p_ == null) {
				p_ = _addPort(pp_[i].groupID, pp_[i].id);
				pp_[i].connect(p_);
			}
		}

		//setComponentFlag(FLAG_HAS_EXPOSED_CHILD, true);
		//child_.setComponentFlag(FLAG_EXPOSED, true);
	}
	
	/**
	 * Undoes what {@link #expose(Component)} did.
	 * If a port was connected to the exposed child 
	 * but does not have any connection after unexposure, the port is
	 * removed.
	 */
	public synchronized void unexpose(Component child_)
	{
		if (!containsComponent(child_))
			//|| getComponentFlag(FLAG_HAS_EXPOSED_CHILD) == 0
			//|| child_.getComponentFlag(FLAG_EXPOSED) == 0)
			return;
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++) {
			Port p_ = child_.getPort(pp_[i].groupID, pp_[i].id);
			if (p_ != null) {
				p_.disconnectWithParent();
				if (!pp_[i].anyConnection()) removePort(pp_[i]);
			}
		}
		
		// if no child is exposed, turn off HAS_EXPOSED_CHILD flag
		/* 
		child_.setComponentFlag(FLAG_EXPOSED, false);
		Component[] cc_ = getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			if (cc_[i].getComponentFlag(FLAG_EXPOSED) != 0)
				return;
		}
		setComponentFlag(FLAG_HAS_EXPOSED_CHILD, false);
		*/
	}
	
	/** Exposes the event ports of the given child component. */
	public void exposeEventPorts(Component c_)
	{
		Port[] pp_ = c_.getAllPorts(PortGroup_EVENT);
		if (pp_ != null) {
			for (int i=0; i<pp_.length; i++) exposePort(pp_[i]);
		}
	}
	
	/**
	 * Returns all the wires that the ports of this component are
	 * attached to.
	 */
	public Wire[] getAllWiresOut()
	{
		Port[] all_ = getAllPorts();
		Hashtable ht_ = new Hashtable();
		Vector v_ = new Vector();
		for (int i=0; i<all_.length; i++) {
			Port p_ = all_[i];
			if (p_ == null || p_.inwire == null && p_.outwire == null) continue;
			if (p_.inwire != null && !ht_.containsKey(p_.inwire)) {
				ht_.put(p_.inwire, p_);
				v_.addElement(p_.inwire);
			}
			if (p_.outwire != null && !ht_.containsKey(p_.outwire)) {
				ht_.put(p_.outwire, p_);
				v_.addElement(p_.outwire);
			}
		}
		Wire[] result_ = new Wire[v_.size()];
		v_.copyInto(result_);
		return result_;
	}
	
	/**
	 * Returns all the wires that the ports of the child components are 
	 * attached to.
	 */
	public Wire[] getAllWiresInside()
	{
		Component[] child_ = getAllComponents();
		Hashtable ht_ = new Hashtable();
		Vector v_ = new Vector();
		for (int i=0; i<child_.length; i++) {
			Wire[] tmp_ = child_[i].getAllWiresOut();
			for (int j=0; j<tmp_.length; j++) 
				if (!ht_.containsKey(tmp_[j])) {
					ht_.put(tmp_[j], this);
					v_.addElement(tmp_[j]);
				}
		}
		Wire[] tmp_ = new Wire[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	/**
	 * Returns all the wires that the ports of this component and the child 
	 * components are attached to.
	 */
	public Wire[] getAllWiresInsideOut()
	{
		Hashtable ht_ = new Hashtable();
		Vector v_ = new Vector();
		Wire[] tmp_ = getAllWiresOut();
		for (int j=0; j<tmp_.length; j++) 
			if (!ht_.containsKey(tmp_[j])) {
				ht_.put(tmp_[j], this);
				v_.addElement(tmp_[j]);
			}
		tmp_ = getAllWiresInside();
		for (int j=0; j<tmp_.length; j++) 
			if (!ht_.containsKey(tmp_[j])) {
				ht_.put(tmp_[j], this);
				v_.addElement(tmp_[j]);
			}
		tmp_ = new Wire[v_.size()];
		v_.copyInto(tmp_);
		return tmp_;
	}
	
	//
	private  void ___MISC____() {}
	//
	
	/**
	 * The hookup method for subclasses to handle the event when a child 
	 * component is added.
	 */
	protected void componentAdded(Component c_)
	{}
	
	/**
	 * The hookup method for subclasses to handle the event when a child 
	 * component is removed.
	 */
	protected void componentRemoved(Component c_)
	{}
	
	
	/**
	 * The hookup method for subclasses to handle the event when the port 
	 * <code>p_</code> is added to the component.
	 */
	protected void portAdded(Port p_)
	{}
	
	/**
	 * The hookup method for subclasses to handle the event when the port 
	 * <code>p_</code> is removed from the component.
	 */
	protected void portRemoved(Port p_)
	{
	}
	
	/** Returns information regarding this component.
	 * Subclasses should override this method to provide useful information at 
	 * run time. */
	public String info()
	{ return "No extra info is provided in " + getClass().getName() + "\n";	}
	

	/**
	 * Duplicates the content of source_, including ID, name, child components 
	 * and connections among them, to this component.
	 * Subclasses need to override this method to copy its own variables.
	 * @throws SetIDException if <code>source_</code> and this component share 
	 * 		the	same parent component.
	 */
	public void duplicate(Object source_)
	{
		// the codes are tuned to best performace by using as few APIs
		// as possible.
		Component that_ = (Component)source_;
		
		_setID(that_.id);
		name = that_.name == null? null: that_.name;
		flag = that_.flag;
		
		if (getComponentFlag(FLAG_OMIT_DUPLICATE) == 0)
			sduplicate(that_);
	}
	
	// duplicate all components and ports under this component hierarchy
	// wires are not duplicated but discovered and put in "wiremap_"
	// addThisPorts_: true to add this's ports to portmap_
	void _duplicate(Component that_, Hashtable childmap_, Hashtable portmap_,
					Hashtable wiremap_, boolean addThisPorts_)
	{
		long cflag_ = getComponentFlag(FLAG_COMPONENT_NOTIFICATION);
		long pflag_ = getComponentFlag(FLAG_PORT_NOTIFICATION);
		setComponentFlag(FLAG_COMPONENT_NOTIFICATION | FLAG_PORT_NOTIFICATION,
						false);

		// duplicate ports and add to wiremap_
		if (addThisPorts_) {
		Port[] pp_ = that_.getAllPorts();
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				if (p_ == p_.host.infoPort) continue;
				Port pclone_ = _addPort(p_.groupID, p_.id);
				pclone_.duplicate(p_);
				portmap_.put(p_, pclone_);
				if (p_.inwire != null && !wiremap_.containsKey(p_.inwire))
					wiremap_.put(p_.inwire, wiremap_);
				if (p_.outwire != null && p_.outwire != p_.inwire
					&& !wiremap_.containsKey(p_.outwire))
					wiremap_.put(p_.outwire, wiremap_);
			}
		}

		// creates all the child components
		try {
			childmap_.put(that_, this);
			Component[] cc_ = that_.getAllComponents();
			for (int i=0; i<cc_.length; i++) {
				Component c_ = cc_[i];
				Component cclone_ = null;
				if (containsComponent(c_.id))
					cclone_ = getComponent(c_.id);
				else {
					cclone_ = (Component)c_.getClass().newInstance();
					cclone_.setComponentFlag(FLAG_OMIT_DUPLICATE, true);
					cclone_.duplicate(c_);
					cclone_.setComponentFlag(FLAG_OMIT_DUPLICATE, false);
					addComponent(cclone_);
				}
				cclone_._duplicate(c_, childmap_, portmap_, wiremap_, true);
			}
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
		if (cflag_ != 0)
			setComponentFlag(FLAG_COMPONENT_NOTIFICATION, true);
		if (pflag_ != 0)
			setComponentFlag(FLAG_PORT_NOTIFICATION, true);
	}

	PortPack _duplicateIn(PortPack pp_, Hashtable portmap_,
					Wire w_, Wire wclone_)
	{
		PortPack new_ = null;
		for (; pp_ != null; pp_ = pp_.next) {
			Port p_ = pp_.port;
			Port pclone_ =  (Port)portmap_.get(p_);
			if (pclone_ == null) continue;
			new_ = new PortPack(pclone_);
			if (p_.inwire == w_) {
				// remove whatever internal wiring pclone_ may have
				// internal rewiring is redone in iduplicate()/sduplicate()
				if (pclone_.inwire != null)
					pclone_.inwire.disconnect();
				pclone_.inwire = wclone_;
			}
			pp_ = pp_.next;
			break;
		}
		for (PortPack tmp_ = new_; pp_ != null; pp_ = pp_.next) {
			Port p_ = pp_.port;
			Port pclone_ =  (Port)portmap_.get(p_);
			if (pclone_ == null) continue;
			tmp_.next = new PortPack(pclone_);
			tmp_ = tmp_.next;
			if (p_.inwire == w_) {
				// remove whatever internal wiring pclone_ may have
				// internal rewiring is redone in iduplicate()/sduplicate()
				if (pclone_.inwire != null)
					pclone_.inwire.disconnect();
				pclone_.inwire = wclone_;
			}
		}
		return new_;
	}

	PortPack _duplicateOut(PortPack pp_, Hashtable portmap_,
					Wire w_, Wire wclone_)
	{
		PortPack new_ = null;
		for (; pp_ != null; pp_ = pp_.next) {
			Port p_ = pp_.port;
			Port pclone_ =  (Port)portmap_.get(p_);
			if (pclone_ == null) continue;
			new_ = new PortPack(pclone_);
			if (p_.outwire == w_) {
				// remove whatever internal wiring pclone_ may have
				// internal rewiring is redone in iduplicate()/sduplicate()
				if (pclone_.outwire != null)
					pclone_.outwire.disconnect();
				pclone_.outwire = wclone_;
			}
			pp_ = pp_.next;
			break;
		}
		for (PortPack tmp_ = new_; pp_ != null; pp_ = pp_.next) {
			Port p_ = pp_.port;
			Port pclone_ =  (Port)portmap_.get(p_);
			if (pclone_ == null) continue;
			tmp_.next = new PortPack(pclone_);
			tmp_ = tmp_.next;
			if (p_.outwire == w_) {
				// remove whatever internal wiring pclone_ may have
				// internal rewiring is redone in iduplicate()/sduplicate()
				if (pclone_.outwire != null)
					pclone_.outwire.disconnect();
				pclone_.outwire = wclone_;
			}
		}
		return new_;
	}

	PortPack _duplicatePP(PortPack pp_, Hashtable portmap_)
	{
		PortPack new_ = null;
		for (; pp_ != null; pp_ = pp_.next) {
			Port pclone_ =  (Port)portmap_.get(pp_.port);
			if (pclone_ == null) continue;
			new_ = new PortPack(pclone_);
			pp_ = pp_.next;
			break;
		}
		for (PortPack tmp_ = new_; pp_ != null; pp_ = pp_.next) {
			Port pclone_ =  (Port)portmap_.get(pp_.port);
			if (pclone_ == null) continue;
			tmp_.next = new PortPack(pclone_);
			tmp_ = tmp_.next;
		}
		return new_;
	}

	/** Duplicates the child components and the structure (including the shadow connections)
	 * from the source component.  */
	public final void sduplicate(Component that_)
	{
		Hashtable childmap_ = new Hashtable();
		Hashtable portmap_ = new Hashtable();
		Hashtable wiremap_ = new Hashtable();
		_duplicate(that_, childmap_, portmap_, wiremap_, true);

		//System.out.println("clone " + getClass().getName());
		// clone ports of this component
		
		// set up all the wires (in wiremap_)
		for (Enumeration e_ = wiremap_.keys(); e_.hasMoreElements(); ) {
			Wire w_ = (Wire)e_.nextElement();
			Wire wclone_ = new Wire();
			if (w_.inports != null)
				wclone_.inports = _duplicateIn(w_.inports, portmap_, w_,
								wclone_);
			if (w_.outports != null)
				wclone_.outports = _duplicateOut(w_.outports, portmap_, w_,
								wclone_);
			if (w_.shadowInports != null)
				wclone_.shadowInports = _duplicateIn(w_.shadowInports,
								portmap_, w_, wclone_);
			if (w_.shadowOutports != null)
				wclone_.shadowOutports = _duplicateOut(w_.shadowOutports,
								portmap_, w_, wclone_);
			if (w_.inEvtListeners != null)
				wclone_.inEvtListeners =  _duplicatePP(w_.inEvtListeners,
								portmap_);
			if (w_.outEvtListeners != null)
				wclone_.outEvtListeners =  _duplicatePP(w_.outEvtListeners,
								portmap_);
		}
	}
	
	
	/**
	 * Duplicates the internal structure of the source component.
	 * The difference from sduplicate() is that iduplicate() does not
	 * duplicate the (shadow) connections of this component's ports.
	 */
	public final void iduplicate(Component that_)
	{
		Hashtable childmap_ = new Hashtable();
		Hashtable portmap_ = new Hashtable();
		Hashtable wiremap_ = new Hashtable();
		_duplicate(that_, childmap_, portmap_, wiremap_, false);

		//System.out.println("clone " + getClass().getName());
		// clone ports of this component
		
		// set up all the wires (in wiremap_)
		for (Enumeration e_ = wiremap_.keys(); e_.hasMoreElements(); ) {
			Wire w_ = (Wire)e_.nextElement();
			Wire wclone_ = new Wire();
			if (w_.inports != null)
				wclone_.inports = _duplicateIn(w_.inports, portmap_, w_,
								wclone_);
			if (w_.outports != null)
				wclone_.outports = _duplicateOut(w_.outports, portmap_, w_,
								wclone_);
			if (w_.shadowInports != null)
				wclone_.shadowInports = _duplicateIn(w_.shadowInports,
								portmap_, w_, wclone_);
			if (w_.shadowOutports != null)
				wclone_.shadowOutports = _duplicateOut(w_.shadowOutports,
								portmap_, w_, wclone_);
			if (w_.inEvtListeners != null)
				wclone_.inEvtListeners =  _duplicatePP(w_.inEvtListeners,
								portmap_);
			if (w_.outEvtListeners != null)
				wclone_.outEvtListeners =  _duplicatePP(w_.outEvtListeners,
								portmap_);
		}
	}

	/** Returns the root component of the component system. */
	public Component getRoot()
	{
		//XX: temporary
		return Root;
	}
	
	/**
	 * Disconnects all the ports of this component.
	 * @see Port#disconnect().
	 */
	public void disconnectAll()
	{
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++) pp_[i].disconnect();
		if (parent != null) parent.unexpose(this);
	}
	
	/**
	 * Disconnects all the ports in the port group.
	 * @see Port#disconnect().
	 */
	public void disconnectAllPorts(String groupID_)
	{
		Port[] pp_ = getAllPorts(groupID_);
		if (pp_ == null) return;
		for (int i=0; i<pp_.length; i++)
			pp_[i].disconnect();
	}
	
	public void setExecutionBoundary(boolean v_)
	{
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++) pp_[i].setExecutionBoundary(v_);
	}
	
	//
	private  void ___INFO_METHODS___() {}
	//
	
	/** Sets the info flag of this component. */
	public void setComponentFlag(long flag_)
	{ flag = flag_; }
	
	/** Returns the info flag of this component. */
	public final long getComponentFlag()
	{ return flag; }
	
	/** Sets the info flag at the 1's in <code>mask_</code>. */
	public void setComponentFlag(long mask_, boolean v_)
	{
		if (v_) flag |= mask_; 
		else flag &= ~mask_;
	}
	
	/** Returns portion of the info flag masked by <code>mask_</code> */
	public final long getComponentFlag(long mask_)
	{ return flag & mask_; }
	
	/** Enables/disables the component (and child components) for receiving data. */
	public final void setEnabled(boolean v_)
	{
		setComponentFlag(FLAG_ENABLED, v_);
		Component[] oo_ = getAllComponents();
		for (int i=0; i<oo_.length; i++)
			oo_[i].setEnabled(v_);
	}
	
	/** Returns true if the component is enabled for receiving data. */
	public final boolean isEnabled() 
	{ return getComponentFlag(FLAG_ENABLED) != 0; }
	
	/** Turns on/off the debug flag. */
	public final void setDebugEnabled(boolean v_) 
	{ setComponentFlag(FLAG_DEBUG_ENABLED, v_);	}
	
	/** Turns on/off the port-added notification flag. */
	protected final void setPortNotificationEnabled(boolean v_) 
	{ setComponentFlag(FLAG_PORT_NOTIFICATION, v_);	}

	/** Returns true if the port-added notification flag is enabled. */
	protected final boolean isPortNotificationEnabled() 
	{ return getComponentFlag(FLAG_PORT_NOTIFICATION) != 0;	}

	/** Turns on/off the component-added notification flag. */
	protected final void setComponentNotificationEnabled(boolean v_) 
	{ setComponentFlag(FLAG_COMPONENT_NOTIFICATION, v_);	}

	/** Returns true if the component-added notification flag is enabled. */
	protected final boolean isComponentNotificationEnabled() 
	{ return getComponentFlag(FLAG_COMPONENT_NOTIFICATION) != 0;	}

	/** Turns on/off the debug flag recursively. */
	public final void setDebugEnabled(boolean v_, boolean recursive_)
	{
		setComponentFlag(FLAG_DEBUG_ENABLED, v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setDebugEnabled(v_, true);
		}
	}
	
	/** Returns true if the debug flag is on. */
	public final boolean isDebugEnabled() 
	{ return getComponentFlag(FLAG_DEBUG_ENABLED) != 0; }
	
	/**
	 * Enables/disables one or more debug levels.
	 * At most 16 debug levels can be defined in a component.
	 * @param level_ 0 &lt= level_ &lt 16 for a single level;
	 *		or a bit mask with 4-bit shift (2^4 &lt level_ &lt 2^20) for multiple levels;
	 */
	public final void setDebugEnabledAt(boolean v_, int level_)
	{
		if (level_ >=0 && level_ < NUM_DEBUG_LEVELS)
			setComponentFlag(1 << FLAG_DEBUG_LEVEL_START << level_, v_);
		else {
			long mask_ = (1 << NUM_DEBUG_LEVELS) - 1;
			setComponentFlag(((level_ >> DEBUG_FLAG_BASE) & mask_) << FLAG_DEBUG_LEVEL_START, v_);
		}
	}
	
	/**
	 * Enables/disables one or more debug levels.
	 * At most 16 debug levels can be defined in a component.
	 * @param level_ 0 &lt= level_ &lt 16 for a single level;
	 *		or a bit mask with 4-bit shift (2^4 &lt level_ &lt 2^20) for multiple levels;
	 */
	public final void setDebugEnabledAt(boolean v_, int[] levels_)
	{
		if (levels_ == null) return;
		long level_ = 0;
		long mask_ = (1 << NUM_DEBUG_LEVELS) - 1;
		for (int i=0; i<levels_.length; i++) {
			int tmp_ = levels_[i];
			if (tmp_ >=0 && tmp_ < NUM_DEBUG_LEVELS)
				level_ |= 1L << tmp_;
			else
				level_ |= (tmp_ >> DEBUG_FLAG_BASE) & mask_;
		}
		
		setComponentFlag(level_ << FLAG_DEBUG_LEVEL_START, v_);
	}
	
	/**
	 * Enables/disables one or more debug levels given the debug level name(s).
	 * The names of debug levels should be consistent with those obtained
	 * by {@link #getDebugLevelNames()}.
	 */
	public final void setDebugEnabledAt(boolean v_, String[] levels_)
	{
		if (levels_ == null) return;
		String[] levelNames_ = getDebugLevelNames();
		if (levelNames_ == null || levelNames_.length == 0) return;
		for (int i=0; i<levelNames_.length; i++)
			levelNames_[i] = levelNames_[i].toLowerCase();
		long level_ = 0;
		for (int i=0; i<levels_.length; i++) {
			String tmp_ = levels_[i].toLowerCase();
			for (int j=0; j<levelNames_.length; j++)
				if (tmp_.equals(levelNames_[j]))
					level_ |= 1L << j;
		}
		
		setComponentFlag(level_ << FLAG_DEBUG_LEVEL_START, v_);
	}
	
	/**
	 * Returns true if the flag(s) is(are) enabled at the specified debug level(s).
	 * At most 16 debug levels can be defined in a component.
	 * @param level_ 0 &lt= level_ &lt 16 for a single level;
	 *		or a bit mask with 4-bit shift (2^4 &lt level_ &lt 2^20) for multiple levels;
	 */
	public final boolean isDebugEnabledAt(int level_)
	{
		if (level_ >=0 && level_ < NUM_DEBUG_LEVELS)
			return getComponentFlag(1L << FLAG_DEBUG_LEVEL_START << level_) > 0;
		else {
			String[] levelNames_ = getDebugLevelNames();
			if (levelNames_ == null) levelNames_ = new String[0];
			long mask_ = (1 << levelNames_.length) - 1;
			long tmp_ = (level_ >> DEBUG_FLAG_BASE) & mask_;
			return getComponentFlag(tmp_ << FLAG_DEBUG_LEVEL_START) == (tmp_ << FLAG_DEBUG_LEVEL_START);
		}
	}
	
	/** Prints the debug level flags as bitset; 1 as enabled and 0 disabled. */
	public final String getDebugFlagsInBinary()
	{
		return StringUtil.toBinary(getComponentFlag(), FLAG_DEBUG_LEVEL_START,
											 NUM_DEBUG_LEVELS);
	}
	
	/** Returns the names of defined debug levels; subclasses should override
	this method if debug levels are defined. */
	public String[] getDebugLevelNames()
	{ return null; }

	/** Returns true if the trace flag is on. */
	public boolean isTraceEnabled() 
	{ return getComponentFlag(FLAG_TRACE_ENABLED) != 0; }

	/**
	 * Turns on/off the component trace flag and
	 * all ports' (except <code>infoPort</code>) trace flags.
	 * The component trace flag is used to turn on/off the trace flags of the
	 * ports that are added to this component.
	 * The trace flags of ports can be individually turned on/off
	 * after they are added to a component.
	 */
	public void setTraceEnabled(boolean v_) 
	{
		setComponentFlag(FLAG_TRACE_ENABLED, v_);
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i] != infoPort) pp_[i].setTraceEnabled(v_);
	}
	
	/** Turns on/off the trace flags recursively.
	 * @see #setTraceEnabled(boolean). */
	public final void setTraceEnabled(boolean v_, boolean recursive_)
	{
		setTraceEnabled(v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setTraceEnabled(v_, true);
		}
	}
	
	/**
	 * Turns on/off the garbage flag.
	 * Turning off the garbage flag implies turning off the garbage display flag.
	 */
	public final void setGarbageEnabled(boolean v_) 
	{
		if (v_) setComponentFlag(FLAG_GARBAGE_ENABLED, v_);
		else setComponentFlag(FLAG_GARBAGE_ENABLEDS, v_);
	}
	
	/** Turns on/off the garbage flag recursively. */
	public final void setGarbageEnabled(boolean v_, boolean recursive_)
	{
		if (v_) setComponentFlag(FLAG_GARBAGE_ENABLED, v_);
		else setComponentFlag(FLAG_GARBAGE_ENABLEDS, v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setGarbageEnabled(v_, true);
		}
	}
	
	/**
	 * Turns on/off the garbage display flag.
	 * Turning on the garbage display flag implies turning on the garbage flag.
	 */
	public final void setGarbageDisplayEnabled(boolean v_) 
	{
		if (v_) setComponentFlag(FLAG_GARBAGE_ENABLEDS, v_);
		else setComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED, v_);
	}
	
	/** Turns on/off the garbage display flag recursively. */
	public final void setGarbageDisplayEnabled(boolean v_, boolean recursive_)
	{
		if (v_) setComponentFlag(FLAG_GARBAGE_ENABLEDS, v_);
		else setComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED, v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setGarbageDisplayEnabled(v_, true);
		}
	}
	
	/** Returns true if the garbage flag is on. */
	public final boolean isGarbageEnabled() 
	{ return getComponentFlag(FLAG_GARBAGE_ENABLED) != 0; }
	
	/** Returns true if the garbage display flag is on. */
	public final boolean isGarbageDisplayEnabled() 
	{ return getComponentFlag(FLAG_GARBAGE_ENABLEDS) == FLAG_GARBAGE_ENABLEDS; }
	
	/** Turns on/off the error notice flag. */
	public final void setErrorNoticeEnabled(boolean v_) 
	{ setComponentFlag(FLAG_ERROR_ENABLED, v_);	}
	
	/** Turns on/off the error notice flag recursively. */
	public final void setErrorNoticeEnabled(boolean v_, boolean recursive_)
	{
		setComponentFlag(FLAG_ERROR_ENABLED, v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setErrorNoticeEnabled(v_, true);
		}
	}
	
	/** Returns true if the error notice flag is on. */
	public final boolean isErrorNoticeEnabled() 
	{ return getComponentFlag(FLAG_ERROR_ENABLED) != 0; }
	
	/** Turns on/off the state report flag. */
	public final void setEventExportEnabled(boolean v_) 
	{
		setComponentFlag(FLAG_EVENT_ENABLED, v_);
		Port[] pp_ = getAllPorts();
		for (int i=0; i<pp_.length; i++)
			if (pp_[i] != infoPort) pp_[i].setEventExportEnabled(v_);
	}
	
	/** Turns on/off the state report flag recursively. */
	public final void setEventExportEnabled(boolean v_, boolean recursive_)
	{
		setEventExportEnabled(v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setEventExportEnabled(v_, true);
		}
	}
	
	/** Turns on/off the direct-output flag. */
	public final void setDirectOutputEnabled(boolean v_) 
	{ setComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED, v_);	}
	
	/** Turns on/off the direct-output flag recursively. */
	public final void setDirectOutputEnabled(boolean v_, boolean recursive_)
	{
		setComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED, v_);
		if (recursive_) {
			Component[] oo_ = getAllComponents();
			for (int i=0; i<oo_.length; i++)
				oo_[i].setDirectOutputEnabled(v_, true);
		}
	}
	
	/** Returns true if the error notice flag is on. */
	public final boolean isDirectOutputEnabled() 
	{ return getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0; }
	
	/** Returns true if the state report flag is on. */
	public final boolean isEventExportEnabled() 
	{ return getComponentFlag(FLAG_EVENT_ENABLED) != 0; }
	
	/** Send debug information at <code>infoport</code>. */
	public final void debug(Object info_)
	{
		if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
			System.out.println(new DebugContract.Message(getTime(), this, info_).toString("| "));
		else
			infoPort.doSending(new DebugContract.Message(getTime(), this, info_));
	}
	
	/** Send trace information at <code>infoport</code>. */
	final void trace(String trace_, Object where_, Object data_)
	{	
		if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
			System.out.println(trace_ + "| " + getTime() + "| " 
							   + where_ + "| " + StringUtil.toString(data_));
		else
			infoPort.doSending(new TraceContract.Message(trace_, getTime(), where_, data_));
	}
	
	/** Send trace information at <code>infoport</code>. */
	final void trace(String trace_, Object where_, Object data_, String comment_)
	{	
		if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
			System.out.println(trace_ + "| " + getTime() + "| " 
							   + where_ + "| " + StringUtil.toString(data_)
							   + "| " + comment_);
		else
			infoPort.doSending(new TraceContract.Message(trace_, getTime(), where_,
									StringUtil.toString(data_) + "| " + comment_));
	}
	
	/** Drops the garbage at <code>infoport</code>. */
	public final void drop(Object o_)
	{ 
		if (isGarbageEnabled()) {
			if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
				System.out.println(new GarbageContract.Message(getTime(), toString(), o_, getComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED) != 0).toString("| "));
			else
				infoPort.doSending(new GarbageContract.Message(getTime(), toString(), o_, getComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED) != 0));
		}
	}
	
	/** Drops the garbage with description at <code>infoport</code>. */
	public final void drop(Object o_, String description_)
	{ 
		if (isGarbageEnabled()) {
			if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
				System.out.println(new GarbageContract.Message(getTime(), toString(), o_, description_, getComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED) != 0).toString("| "));
			else
				infoPort.doSending(new GarbageContract.Message(getTime(), toString(), o_, description_, getComponentFlag(FLAG_GARBAGE_DISPLAY_ENABLED) != 0));
		}
	}
	
	/** Sends an error message at <code>infoport</code>. */
	public void error(Object data_, String method_, Port p_, Object error_)
	{ 
		if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
			System.out.println(new ErrorContract.Message(getTime(), p_, 
											   data_,
											   (method_ == null || method_.length() == 0?
												"": getClass() + "." + method_),
											   error_).toString("| "));
		else
			infoPort.doSending(new ErrorContract.Message(getTime(), p_, 
											   data_,
											   (method_ == null || method_.length() == 0?
												"": getClass() + "." + method_),
											   error_));
	}
	
	/** Sends an error message at <code>infoport</code>. */
	public void error(String method_, Object error_)
	{ 
		if (getComponentFlag(FLAG_DIRECT_OUTPUT_ENABLED) != 0)
			System.out.println(new ErrorContract.Message(getTime(), infoPort, 
											   null,
											   (method_ == null || method_.length() == 0?
												"": getClass() + "." + method_),
											   error_).toString("| "));
		else
			infoPort.doSending(new ErrorContract.Message(getTime(), infoPort, 
											   null,
											   (method_ == null || method_.length() == 0?
												"": getClass() + "." + method_),
											   error_));
	}
	
	//
	private  void ___TIME_CONTROL___() {}
	// programming interfaces for time control
	
	/** Sleeps for <em>time</em> seconds. */
	public void sleepFor(double time_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null) throw new WorkerThreadException("sleepFor() must be called in a WorkerThread context");
        else if (time_ > 0.0)
			t_.sleepFor(time_);
	}
	
	/** Sleeps util the time specified. */
	public void sleepUntil(double time_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null) throw new WorkerThreadException("sleepUntil() must be called in a WorkerThread context");
        else
			t_.sleepUntil(time_);
	}
	
	/** Returns the system time. */
	public double getTime() 
	{
		// use runtime instead of current thread's runtime
		return runtime.getTime();
	}
	
	//
	private void ___ASYNCHRONOUS___() {}
	//
	
	/** Creates and associates a local fork runtime with this component. */
	public final void useLocalForkManager()
	{
		new ForkManagerLocal("LFM_" + this, fm == null? runtime: fm,
						runtime).takeover(this, false/* not as parent*/);
	}

	/** Creates and associates a local fork runtime with this component. */
	public final void useLocalForkManager(boolean simple_)
	{
		new ForkManagerLocal("LFM_" + this, fm == null? runtime: fm,
						runtime).takeover(this, false/* not as parent*/);
	}

	/** Returns the fork runtime associated with this component. */
	protected final ForkManager getForkManager()
	{ return fm; }
	
	/**
	 * Sets up a delayed send event.
	 * Returns the timer object which can be used to cancel the event.
	 */
	public ACATimer send(Port which_, Object data_, double duration_)
	{
		//if (isDebugEnabled()) debug("fork| after " + duration_ + " at "
		//							+ which_ + " will arrive " + data_);
		if (which_ == null)
			return null;
		else if (duration_ <= 0.0) {
			which_.doSending(data_);
			return null;
		}
		else if (fm == null)
			return runtime.send(which_, data_, duration_);
		else
			return fm.send(which_, data_, duration_);
	}
	
	/**
	 * Sets up a delayed send event.
	 * Returns the timer object which can be used to cancel the event.
	 */
	public ACATimer sendAt(Port which_, Object data_, double time_)
	{
		//if (isDebugEnabled()) debug("fork| after " + duration_ + " at "
		//							+ which_ + " will arrive " + data_);
		if (fm == null)
			return runtime.sendAt(which_, data_, time_);
		else
			return fm.sendAt(which_, data_, time_);
	}
	
	/**
	 * Sets up a fork event.
	 * With setup of such an event, the data is scheduled to arrive at
	 * the specified port (of this component) at the specified time later.
	 * Returns the timer object which can be used to cancel the event.
	 */
	public ACATimer fork(Port which_, Object data_, double duration_)
	{
		//if (isDebugEnabled()) debug("fork| after " + duration_ + " at "
		//							+ which_ + " will arrive " + data_);
		if (which_ == null)
			return null;
		else if (duration_ <= 0.0) {
			//which_.doReceiving(data_, null, runtime.getThread());
			//if (which_.isDataTraceEnabled()) 
			//	trace(Component.Trace_DATA, which_, data_, "(create context)");
			// note: trace taken care of in TaskReceive	
			runtime.newTask(new TaskReceive(which_, data_),
							runtime.getThread());
			return null;
		}
		else if (fm == null)
			return runtime.receive(which_, data_, duration_);
		else
			return fm.receive(which_, data_, duration_);
	}
	
	/**
	 * Sets up a fork event.
	 * With setup of such an event, the data is scheduled to arrive at
	 * the specified port (of this component) at the specified time.
	 * Returns the timer object which can be used to cancel the event.
	 */
	public ACATimer forkAt(Port which_, Object data_, double time_)
	{
		//if (isDebugEnabled()) debug("fork| after " + duration_ + " at "
		//							+ which_ + " will arrive " + data_);
		if (fm == null)
			return runtime.receiveAt(which_, data_, time_);
		else
			return fm.receiveAt(which_, data_, time_);
	}
	
	/** Cancels a fork event. */
	public void cancelFork(ACATimer handle_)
	{
		if (fm == null)
			runtime.off(handle_);
		else
			fm.off(handle_);
	}
	
	// 
	private  void ___MULTITHREADED_API___() {}
	//
	
	/** Data structure for holding all the locks and waiting threads
	 * in a component. */
	public static interface Locks
	{
		/** Returns all the locks' and waiting threads' information. */
		public String printAll();
	}

	/** For holding all the locks and waiting threads in this component.*/
	public Locks locks = null;
	
	// for simulation extension
	
	/** Grabs the lock of the object. This method is blocked until the current
	 * execution
	 * context is the owner of the lock. */
	public void lock(Object o_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null)
			throw new WorkerThreadException(
						"lock() must be called in a WorkerThread context");
		t_.lock(this, o_);
	}
	
	/** Releases the lock of the object. */
	public void unlock(Object o_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null)
			throw new WorkerThreadException(
						"unlock() must be called in a WorkerThread context");
		t_.unlock(this, o_);
	}
	
	/** Waits on the object until being notified. */
	public void wait(Object o_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null) {
			//throw new WorkerThreadException(
			//	"wait() must be called in a WorkerThread context");
			try { o_.wait(); }
			catch (Exception e_) {
				throw new WorkerThreadException("wait() fails"); }
		}
		else
			t_.wait(this, o_);
	}

	/** Notifies on the object.
	 * The first execution context that waits on this object is waked up. */
	public void notify(Object o_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null) {
			//throw new WorkerThreadException(
			//	"notify() must be called in a WorkerThread context");
			try {
				o_.notify();
			}
			catch (Exception e_) {
				throw new WorkerThreadException("notify() fails");
			}
		}
		else
			t_.notify(this, o_);
	}
	
	/** Do not use this method unless you know what you are doing.
	protected void changeContext(Port p_)
	{
		WorkerThread t_ = runtime.getThread();
		t_.currentContext.port = p_;
	}
	 */

	/** Notifies all the execution contexts that wait on the object. */
	public void notifyAll(Object o_)
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ == null) {
			//throw new WorkerThreadException(
			//	"notifyAll() must be called in a WorkerThread context");
			try {
				o_.notifyAll();
			}
			catch (Exception e_) {
				throw new WorkerThreadException("notifyAll() fails");
			}
		}
		else
			t_.notifyAll(this, o_);
	}

	/** Makes the current thread stop and allows other threads to execute.
	 * This is particularly important when an ActiveComponent starts in an
	 * infinite loop, or in general, a component may hold a thread for a
	 * long time in a loop.  
	 *
	 * <p>** Warning **<br>
	 * A thread may block other threads if no yield(), wait() or sleep() in an
	 * infinite loop in a component's {@link #process(Object, Port)} or
	 * {@link #_start()} method.  Even yield() is used, it is strongly
	 * recommended to avoid an infinite loop in {@link #process(Object, Port)}
	 * or {@link #_start()}.
	 * In many cases, an infinite loop can be replaced with asynchronous
	 * events which is more efficient in terms of performance. 
	 * */
	protected void yield()
	{
		WorkerThread t_ = runtime.getThread();
		if (t_ != null) t_.yieldToRuntime();
	}
	
	//
	private  void ___WORKER_THREAD___() {}
	
	/** Sets the runtime associated with this component.
	 * The runtime is propagated to all the child components. */
	protected void setRuntime(ACARuntime m_)
	{
		runtime = m_;
		if (fm != null) fm.setRuntime(m_);
		Component[] children_ = getAllComponents();
		for (int i=0; i<children_.length; i++)
			if (children_[i].runtime != m_)
				children_[i].setRuntime(m_);
	}
	
	/** Returns the runtime associated with this component. */
	protected ACARuntime getRuntime()
	{ return runtime; }
	
	/* * Returns the current thread context.
	WorkerThread getThread()
	{ return runtime.getThread(); }
	*/
	
	/** Notifies of the end of data processing (for performance-aware
	 * components). */
	protected void finishing()
	{
		Thread tmp_ = Thread.currentThread();
		if (tmp_ instanceof WorkerThread)
			((WorkerThread)tmp_).setState(WorkerThread.State_FINISHING);
	}
			
	/** Sets the priority of the current thread.
	final protected void setTaskPriority(int priority_)
	{
		WorkerThread thread_ = getThread();
		if (thread_ != null) thread_.setTaskPriority(priority_);
	}
	*/
	
	/** Returns the priority of the current thread.
	final protected int getTaskPriority()
	{
		WorkerThread thread_ = getThread();
		if (thread_ != null) return thread_.getTaskPriority();
		else return Task.TYPE_NORMAL;
	}
	*/
	
	//
	private  void ___PORT_MANAGER___() {}
	//
	
	class PortManager implements java.io.Serializable
	{
		/**
		 * Use group ID (String) as index, elements are 
		 * portGroupClass.  Each port has an ID
		 * used to retrieve itself from the group.
		 */
		Hashtable htPortGroups = new Hashtable(3);

		public void reset()
		{
			for (Enumeration e_ = htPortGroups.elements();
						e_.hasMoreElements(); ) {
				Object next_ = e_.nextElement();
				//System.out.println("reset port group: " + next_);
				((PortGroupClass)next_).reset();
			}
		}
		
		/**
		 * Returns the port given group ID and port ID.
		 */
		Port getPort(String groupID_, String id_)
		{
			PortGroupClass pg_ = (PortGroupClass) htPortGroups.get(groupID_);
			return pg_ == null? null: pg_.getPort(id_);
		}
	
		/**
		 * Returns all the ports of the component.
		 */
		Port[] getAllPorts()
		{
			int np_ = 0; // total # of ports
			PortGroupClass[] pgs_ = new PortGroupClass[htPortGroups.size()];
			
			for (Enumeration e_ = htPortGroups.elements();
							e_.hasMoreElements(); ) 
				np_ += ((PortGroupClass)e_.nextElement()).size();
			
			Port[] all_ = new Port[np_];
			np_ = 0;
			for (Enumeration e_ = htPortGroups.elements();
							e_.hasMoreElements(); ) {
				PortGroupClass pg_ = (PortGroupClass)e_.nextElement();
				System.arraycopy(pg_.getAll(), 0, all_, np_, pg_.size());
				np_ += pg_.size();
			}
			return all_;
		}
		
		Port[] getAllPorts(String groupID_)
		{
			PortGroupClass pg_ = (PortGroupClass) htPortGroups.get(groupID_);
			return pg_ == null? null: pg_.getAll();
		}
	
		/**
		 * Removes all the ports.
		 */
		void removeAllPorts()
		{
			for (Enumeration e_ = htPortGroups.elements();
							e_.hasMoreElements(); ) {
				PortGroupClass pg_ = (PortGroupClass)e_.nextElement();
				removeAllPorts(pg_.name);
			}
		}
	
		/**
		 * Removes all the ports.
		 */
		void removeAllPorts(String groupID_)
		{
			Port[] pp_ = getAllPorts(groupID_);
			if (pp_ == null) return;
			for (int i=0; i<pp_.length; i++)
				if (isPortRemovable(pp_[i]))
					removePort(pp_[i].groupID, pp_[i].id);
		}
	
		/**
		 * Removes the port given group id and port id.  It does not disconnect
		 * the port.  The other <code>removePort()</code> methods end up with
		 * calling this method.
		 * 
		 * @param groupID_ group ID of the port.
		 * @param id_ ID of the port.
		 * @return the port.
		 */
		Port removePort(String groupID_, String id_)
		{
			PortGroupClass pg_ = (PortGroupClass) htPortGroups.get(groupID_);
			if (pg_ != null) {
				Port p_ = pg_.getPort(id_);
				if (p_ == null || !isPortRemovable(p_)) return null;
				pg_.removePort(id_);
				_portRemoved(p_);
				if (pg_.size() == 0) htPortGroups.remove(groupID_);
				return p_;
			}
			else return null;
		}

		/**
		 * Replaces the port, given group id and port id, with 
		 * <code>newPort</code>.  It does not disconnect the replaced port.
		 */
		void setPort(Port newPort_, String groupID_, String id_)
		{
			if (newPort_ == null) return;
			Port old_ = getPort(groupID_, id_);
			boolean isRemovable_ = true;
			if (old_ != null) isRemovable_ = isPortRemovable(old_);
			removePort(groupID_, id_);
			addPort(newPort_, groupID_, id_, isRemovable_);
		}
	
		/**
		 * Adds a port with the given group id and port id.
		 * It assigns an ID if the given id is null.
		 * The other <code>addPort()</code> methods end up with calling this
		 * method.
		 * 
		 * @param new_ the port to be added to this component.
		 * @param groupID_ group ID of the port.
		 * @param newID_ ID of the port.
		 * @return new_ or null if port of the same ID in the same group exists.
		 */
		Port addPort(Port new_, String groupID_, String newID_, 
					boolean isRemovable_)
		{
			//groupID_ = groupID_.intern();
			//newID_ = newID_.intern();
			PortGroupClass pg_ = (PortGroupClass) htPortGroups.get(groupID_);
			if (pg_ == null) {
				pg_ = new PortGroupClass();
				pg_.name = groupID_;
				htPortGroups.put(groupID_, pg_);
			}
			
			if (pg_.addPort(new_, newID_) != null) {
				new_.setRemovable(isRemovable_);
				new_.setHost(Component.this);
				_portAdded(new_);
				return new_;
			}
			else return pg_.getPort(newID_);
		}
	
		/**
		 * Returns true if the component owns the port.
		 */
		boolean containsPort(Port p_)
		{
			PortGroupClass pg_ = (PortGroupClass) htPortGroups.get(p_.groupID);
			return pg_ != null && pg_.containsPort(p_);
		}
		
		/**
		 * Returns true if the port is removable.
		 */
		boolean isPortRemovable(Port p_)
		{ return p_.isRemovable();	}
	
		void setPortRemovable(Port p_, boolean v_)
		{ p_.setRemovable(v_); }
	}
	
	/** Class to manage port groups. */
	static class PortGroupClass implements java.io.Serializable 
	{
		//Port[] all;
		Hashtable all;
		int size = 0;
		String name; // group name
		
		// # of ports in this group
		public synchronized int size() { return size; }
		
		public synchronized Port getPort(String id_)
		{
			if (all == null) return null;
			/*
			for (int i=0; i<all.length; i++)
				if (all[i] != null && all[i].getID().equals(id_)) return all[i];
			return null;
			*/
			return (Port)all.get(id_);
		}
		
		// return a valid port id
		public synchronized String findAvailable()
		{
			//Port[] pp_ = all;
			Port[] pp_ = (Port[])all.values().toArray(new Port[0]);
			if (pp_ != null) {
				IntSpace is_ = new IntSpace();
				for (int i=0; i<pp_.length; i++) {
					if (pp_[i] == null) continue;
					try {
						is_.checkout(Integer.parseInt(pp_[i].id));
					}
					catch (Exception e_) {}
				}
				int id_ = is_.checkout();
				return String.valueOf(id_);
			}
			else
				return "0";
		}
		
		public synchronized Port addPort(Port p_, String id_)
		{
			if (id_ == null || id_.length() == 0) {
				//System.out.println("add: " + p_);
				id_ = findAvailable();
			}
			else if (getPort(id_) != null) return null;
			
			p_.groupID = name;
			p_.id = id_;
			size++;
			/*
			if (all == null) all = new Port[3];
			for (int i=0; i<all.length; i++)
				if (all[i] == null) {
					all[i] = p_; return p_;
				}
			
			Port[] tmp_ = new Port[all.length + 3];
			System.arraycopy(all, 0, tmp_, 0, all.length);
			tmp_[all.length] = p_;
			all = tmp_;
			*/
			if (all == null) all = new Hashtable(3,3);
			all.put(id_, p_);
			return p_;
		}
		
		public synchronized Port removePort(String id_)
		{
			if (id_ == null || id_.length() == 0) return null;
			
			if (all == null) return null;
			/*
			for (int i=0; i<all.length; i++)
				if (all[i] != null && all[i].getID().equals(id_)) {
					Port p_ = all[i];
					all[i] = null;
					size--;
					return p_;
				}
			return null;
			*/
			Port p_ = (Port)all.remove(id_);
			if (p_ != null) size--;
			return p_;
		}
		
		public synchronized boolean containsPort(Port p_)
		{
			if (all == null) return false;
			/*
			for (int i=0; i<all.length; i++)
				if (all[i] == p_) return true;
			return false;
			*/
			return all.containsKey(p_.id);
		}
		
		public synchronized Port[] getAll()
		{
			/*
			Port[] pp_ = new Port[size];
			if (all == null) return pp_;
			int i = 0;
			for (int j=0; j<all.length; j++)
				if (all[j] != null) pp_[i++] = all[j];
			return pp_;
			*/
			if (all == null) return new Port[0];
			return (Port[])all.values().toArray(new Port[0]);
		}
		
		public synchronized void removeAll()
		{
			all = null;
			size = 0;
		}
		
		public synchronized void reset()
		{
			if (all == null) return;
			/*
			for (int i=0; i<all.length; i++)
				if (all[i] != null) all[i].reset();
			*/
			for (Enumeration e_=all.elements(); e_.hasMoreElements();)
				((Port)e_.nextElement()).reset();
		}
		
		public String toString()
		{ 
			return name +"@";	
		}
	}

	
	//
	private  void ___PORT_CONTRACT___() {}
	//
	
	static Hashtable htContract = null;
	
	/**
	 * Associates a port or a set of ports with the contract.
	 * This method recognizes the wildcard expression for port ID.
	 * @param id_	port group id or (port id + @ + port group id).
	 */
	protected static void setContract(Class class_, String id_, Contract c_)
	{
		if (class_ == null || id_ == null)
			throw new NullPointerException((class_ == null? "class":"ID") + " is null.");

		if (htContract == null) htContract = new Hashtable();
		Hashtable ht_ = (Hashtable)htContract.get(class_);
		if (ht_ == null) {
			ht_ = new Hashtable();
			htContract.put(class_, ht_);
		}
		
		ht_.put(id_, c_);
	}
	
	/*
	 * Returns the contract that is associated with the port(s).
	 * This method recognizes the wildcard expression for port ID.
	 * @param id_	port group id or (port id + @ + port group id).
	 */
	private static Contract getContract(Class class_, String id_)
	{
		try {
			Hashtable ht_ = (Hashtable)htContract.get(class_);
			Contract c_ = (Contract) ht_.get(id_);
			if (c_ != null) return c_;
			for (Enumeration e_ = ht_.keys(); e_.hasMoreElements(); ) {
				String key_ = (String)e_.nextElement();
				if (key_.equals("*") /*short cut*/
					|| StringUtil.match(id_, key_))
					return (Contract)ht_.get(key_);
			}
			
			class_ = class_.getSuperclass();
			while (class_ != null) {
				c_ = getContract(class_, id_);
				if (c_ != null) return c_;
				class_ = class_.getSuperclass();
			}
			return null;
		}
		catch (Exception e_) {
			return null;
		}
	}
	
	/** For debugging purposes. */
	public static Hashtable getContractHT(String name_)
	{
		try {
			Class class_ = Class.forName(name_);
			return (Hashtable)htContract.get(class_);
		}
		catch (Exception e_) {
			e_.printStackTrace();
			System.err.println("Component.getContractHT()| " + e_);
			return new Hashtable();
		}
	}
	
	/** For debugging purposes. */
	public static Hashtable getContractHT()
	{ return htContract; }
	
	/** Returns the contract associated with the port. */
	public static Contract getContract(Port p_)
	{
		try {
			Contract c_ = getContract(p_.getHost().getClass(), 
									  p_.getID() + "@" + p_.getGroupID());
			return c_ != null? c_: ContractAny.INSTANCE;
		}
		catch (Exception e_) {
			return ContractAny.INSTANCE;
		}
	}
	
	// avoid confliction due to cycling dependence, do this after all static 
	// fields are created.
	/** The root of the component system. */
	public static Component Root;
	static {
		Root = new Component("___ROOT___");
		Root.infoPort.outwire = new Wire();
		Root.runtime = ACARuntime.DEFAULT_RUNTIME;
	}
}

