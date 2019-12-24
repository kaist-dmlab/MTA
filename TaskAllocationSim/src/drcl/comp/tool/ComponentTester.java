// @(#)ComponentTester.java   7/2003
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

import java.util.*;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.event.*;

import drcl.comp.*;
import drcl.comp.contract.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;
import drcl.util.*;

/**
This <i>tester</i> component is designed for testing a component.
Testing of a component using this tester component is similar to testing of
an IC chip where the IC chip is put in a testing circuitry.  
One can program the testing circuitry to generate certain signals and
then monitor if the tested chip responds with correct signals.

<p>To test a component, one must "program" this tester by setting up a
sequence of events.  Two types of events are essential in testing a component:
<ul>
<li> Sending event: one may set up such an event to send data from this tester
	component to the tested component at specified time.
<li> Receiving event: one may set up such an event to expect data sent from
	the tested component to this tester component at specified time.
</ul>
Two more types of events are derived from the above two, specifically for
handling "sendReceive" events initiated by the tested component:
<ul>
<li> "rr-request" event: one may set up such an event to expect a "sendReceive"
	request sent from the tested component to this tester component at
	specified time.
<li> "rr-reply" event: one may set up such an event to send a "sendReceive"
	reply from this tester component to the tested component at specified time.
</ul>
The reason why the above two types of events are distinguished from the 
sending and receiving events is because the thread of sending a "sendReceive"
request carries across component boundary and expects to get the reply within
the same thread context, while the threads for processing the sending and 
receiving events are different and independent.  As a result, a "rr-reply"
event must be set up as a pair with a "rr-request" event.
 
<p>To set up an event, use one of the <code>addEvent()</code> or the
<code>insertEvent()</code> methods.  To remove an event, use the {@link #removeEvent(int)}
method.

Run this tester component (as well as the tested component if necessary) to
start the test as the tester component is an {@link ActiveComponent}.

@author Hung-ying Tyan
*/
public class ComponentTester extends drcl.comp.WrapperComponent implements drcl.comp.ActiveComponent
{
	/** Flag for receipt notification.  Default is off.  */
	transient boolean isRcvOn = false;
	
	EventTester evtTester = new EventTester(this);
	{
		setComponentFlag(FLAG_ENABLED); // turn off all other flags
		infoPort.setExecutionBoundary(false); // so that info don't come in out of order
		infoPort.setType(Port.PortType_IN);
		setObject(evtTester);	
	}
	
	public ComponentTester() 
	{ super(); 	}
	
	public ComponentTester(String id_)
	{ super(id_); }
	
	/** Enables/disables printing received data.  */
	public void setRcvEnabled(boolean v_) {isRcvOn = v_; }

	/** Returns true if printing received data is enabled.  */
	public boolean isRcvEnabled() { return isRcvOn; }

	/** Returns the underlying testing engine. */
	public EventTester getEventTester()
	{ return evtTester; }
	
	/**
	 */
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		synchronized (this) {
			if (isRcvOn) {
				if (data_ instanceof String) post(data_.toString());
				else post(data_ + "\n");
			}
		}
		
		try {
			//System.out.println("ComponentTester got from " + inPort_ + ", " + data_);
			if (data_ instanceof TraceContract.Message && !isTraceEnabled())
				return;
			else if (data_ instanceof DebugContract.Message && !isDebugEnabled())
				return;
			else if (data_ instanceof GarbageContract.Message && !isGarbageEnabled())
				return;
			else if (evtTester._isBatchRunning()) evtTester.match(getTime(), data_, inPort_);
		}
		catch (Exception e_) {
			post("Error: EventTester.match(): " + e_ + "\n");
			evtTester.stop();
		}
		//if (isStorageOn) addData(data_, inPort_);
	}
	
	/** Resets the tester component.  The events are not removed. */
	public void reset()
	{
		super.reset();
		evtTester.reset();
		clearStorage();
	}
	
	/** Starts a test.  Do not call this method directly.  Use run() instead.  */
	protected void _start()
	{ evtTester.start(); }
	

	//
	private void ___EVENT_MGT___() {}
	//
	
	/**
	 Adds a "sending"/"rr-reply" event.  Must specify time, object to send and the port on 
	 which the object is to send.  The time is relative to the time when the 
	 test starts.
	 */
	public Object addEvent(String evtType_, double time_, Object toSend_, Port p_)
	{ return evtTester.addEvent(evtType_, time_, toSend_, p_);	}
	
	/** Adds a "rr-reply" event, corresponding to last event.  */
	public Object addEvent(String evtType_, double time_, Object toSend_)
	{ return evtTester.addEvent(evtType_, time_, toSend_);	}
	
	/**
	 Adds a "receiving" event. 
	 @see #addEvent(String, double, Object, String, String) 
	 */
	public Object addEvent(String evtType_, double time_, Object toRcv_, String portDescription_)
	{ return evtTester.addEvent(evtType_, time_, toRcv_, null, portDescription_);	}
	
	/**
	 Adds a "receiving"/"rr-request" event.  
	 When a data is received, time, data itself, class of data and port are matched
	 with the event.  In the event, if time is less than 0, time always matches.
	 Similarly, data always matches if null is specified.
	 Class always matches if null is specified.
	 The time is relative to the time when the test starts.
	 */
	public Object addEvent(String evtType_, double time_, Object toRcv_, String classExpected_, Port p_)
	{ return evtTester.addEvent(evtType_, time_, toRcv_, classExpected_, p_);	}
	
	/**
	 Adds a "receiving" event.  
	 Exactly the same as the other add-recv-event method except that port is
	 specified in group id and port id.  Group id always matches if null is
	 specified.  Port id always matches if null is specified.
	 */
	public Object addEvent(String evtType_, double time_, Object toRcv_, String classExpected_, String portDescription_)
	{ return evtTester.addEvent(evtType_, time_, toRcv_, classExpected_, portDescription_);	}
	
	/**
	 Adds a finish event.  The event is to finish the test no matter 
	 whether there are still events behind.
	 */
	public Object addEvent(String evtType_, double time_)
	{ return evtTester.addEvent(evtType_, time_);	}
	
	/**
	 Adds a message event.  Executing the event results in the message being
	 printed to the out channels.
	 */
	public Object addEvent(String evtType_, double time_, String msg_)
	{ return evtTester.addEvent(evtType_, time_, msg_);	}
	
	/**
	 Adds a message event.  Executing the event results in the message being
	 printed to the out channels.
	 */
	public Object addEvent(String msg_)
	{ return evtTester.addEvent(msg_);	}
	
	/**
	 Removes the index'th event.
	 Use {@link #viewBatch} to view the list of events in the batch.
	 */
	public Object removeEvent(int index_)
	{ return evtTester.removeEvent(index_);	}
	
	/**
	 Inserts a "sending" event
	 Use {@link #viewBatch()} to view the list of events in the batch.
	 @see #addEvent(String, double, Object, Port)
	 */
	public Object insertEvent(int index_, String evtType_, double time_, Object toSend_, Port p_)
	{ return evtTester.insertEvent(index_, evtType_, time_, toSend_, p_);	}
	
	/**
	 Inserts a "receiving" event
	 Use {@link #viewBatch()} to view the list of events in the batch.
	 @see #addEvent(String, double, Object, String, Port)
	 */
	public Object insertEvent(int index_, String evtType_, double time_, Object toRcv_, String classExpected_, Port p_)
	{ return evtTester.insertEvent(index_, evtType_, time_, toRcv_, classExpected_, p_);	}
	
	/**
	 Inserts a "receiving" event.
	 Use {@link #viewBatch()} to view the list of events in the batch.
	 @see #addEvent(String, double, Object, String, String)
	 */
	public Object insertEvent(int index_, String evtType_, double time_, Object toRcv_, String classExpected_, String portDescription_)
	{ return evtTester.insertEvent(index_, evtType_, time_, toRcv_, classExpected_, portDescription_);	}
	
	/**
	 Inserts a finish event.
	 Use {@link #viewBatch()} to view the list of events in the batch.
	 @see #addEvent(String, double)
	 */
	public Object insertEvent(int index_, String evtType_, double time_)
	{ return evtTester.insertEvent(index_, evtType_, time_);	}
	
	/**
	 Inserts a message event.
	 Use {@link #viewBatch()} to view the list of events in the batch.
	 @see #addEvent(String, double, String)
	 */
	public Object insertEvent(int index_, String evtType_, double time_, String msg_)
	{ return evtTester.insertEvent(index_, evtType_, time_, msg_);	}
	
	public String viewBatch()
	{ return evtTester.view(); }
	
	public void clearBatch()
	{ evtTester.clear(); }
	
	public void resetBatch()
	{ evtTester.reset(); }

	//
	private void ___INCOMING_DATA_MANAGEMGT___() {}
	//
	
	transient Hashtable htStorage = new Hashtable(); // port -> queue of events sorted in time
	boolean isStorageOn = true;
	
	public void setStorageEnabled(boolean v_) { isStorageOn = v_; }
	public boolean isStorageEnabled() { return isStorageOn; }
	
	// Buffers the incoming data.
	synchronized void addData(Object data_, Port port_)
	{
		if (port_ == null) return;
		
		double time_ = getTime();
		Object[] datapack_ = new Object[]{new Double(time_), data_, port_};
		Queue q_ = (Queue) htStorage.get(port_);
		if (q_ == null) {
			q_ = new FIFOQueue();//QueueAssistant.getBest();
			htStorage.put(port_, q_);
		}
		q_.enqueue(time_, datapack_);
	}
	
	/**
	 View the list of data that are buffered in the tester. 
	 Use {@link #getData(Port)} to retrieve them.
	 */
	public synchronized String viewStorage()
	{
		if (!isStorageOn) {
			return "Storage is off.\n";
		}
		StringBuffer sb_ = new StringBuffer();
		for (Enumeration e_ = htStorage.keys(); e_.hasMoreElements(); ) {
			Port p_ = (Port)e_.nextElement();
			sb_.append("Port " + p_ + ": ");
			Queue q_ = (Queue) htStorage.get(p_);
			if (q_ == null) continue;
			if (q_.isEmpty()) sb_.append("no data\n");
			else if (q_.getLength() == 1)sb_.append("one data:\n");
			else sb_.append(q_.getLength() + " data:\n");
			
			Object[] oo_ = q_.retrieveAll();
			for (int i=0; i<oo_.length; i++) {
				Object[] datapack_ = (Object[])oo_[i];
				sb_.append("Data " + i + ": time=" + datapack_[0] + ", from " + datapack_[2] + ", " + datapack_[1] + "\n");
			}
		}
		if (sb_.length() == 0) return "No data in the storage.\n";
		return sb_.toString();
	}
	
	/** Returns the first data in the queue, coming in from the port specified.  */
	public synchronized Object getData(Port port_)
	{
		if (port_ == null) return null;
		Queue q_ = (Queue) htStorage.get(port_);
		if (q_ == null) return null;
		Object[] datapack_ = (Object[])q_.dequeue();
		if (datapack_ == null) return null;
		Object data_ = datapack_[1];
		post("Data comes at time " + datapack_[0] + "\n");
		return data_;
	}
	
	/** Returns the first data in the queue, coming in from the port at the time specified. */
	public synchronized Object getData(Object port_, double time_)
	{
		if (port_ == null) return null;
		Queue q_ = (Queue) htStorage.get(port_);
		if (q_ == null) return null;
		Object[] datapack_ = (Object[])q_.dequeue(time_);
		if (datapack_ == null) return null;
		Object data_ = datapack_[1];
		return data_;
	}
	
	/** Clears all the data buffered in the tester.  */
	public synchronized void clearStorage()
	{
		htStorage.clear(); // fastest and laziest
	}
	
	//
	private void ___MONITOR___() {}
	//
	
	public ComponentMonitor monitor(Component comp_)
	{
		if (comp_.id == null) {
			post("Can't monitor for component without id!\n");
			return null;
		}
		String id_ = Util.getFullID(comp_);
		if (containsComponent(id_)) {
			post("Monitor for " + id_ + " exists!\n");
			return null;
		}
		ComponentMonitor monitor_ = new ComponentMonitor(id_);
		if (containsComponent(id_)) {
			post("Strange...\n");
			return null;
		}
		comp_.infoPort.connect(monitor_.infoPort);
		Frame f_ = new Frame(id_ + " Monitor");
		f_.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e_) {
				Frame tmp_ = (Frame)e_.getSource();
				String idtmp_ = tmp_.getTitle();
				idtmp_ = idtmp_.substring(0, idtmp_.length() - 8);
				if (idtmp_ != null) {
					ComponentMonitor m_ = (ComponentMonitor)getComponent(idtmp_);
					if (m_ == null) post("No monitor for '" + idtmp_ + "'\n");
					else {
						java.awt.Component c_ = m_.getDisplay();
						synchronized (c_) {	c_.notifyAll(); }
						m_.disconnectAll();
						removeComponent(m_);
					}
				}
				tmp_.dispose();
			}
		});
		f_.add(monitor_.getDisplay(), BorderLayout.CENTER);
		f_.pack();
		f_.show();
		//comp_.infoPort.doReceiving(Component.Info.STATE_REPORT);
		return monitor_;
	}
	
	//
	private void ___MISC___() {}
	//
	
	// post a message to all out channels.
	public void post(String msg_)
	{ drcl.Debug.debug(msg_); }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		sb_.append("EventTester:\n");
		sb_.append(evtTester.info());
		sb_.append(viewStorage());
		return sb_.toString();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		// no need to duplicate other things.
		//ComponentTester that_ = (ComponentTester)source_;
	}
}
