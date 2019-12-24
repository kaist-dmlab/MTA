// @(#)EventTester.java   9/2002
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

package drcl.comp.tool;

import java.util.Vector;
import drcl.comp.*;
import drcl.util.queue.*;
import drcl.util.*;

/**
An event-based component testing engine.
Must be embedded in a component (e.g., {@link ComponentTester})
@author Hung-ying Tyan
 */
public class EventTester extends drcl.DrclObj
{
	static final String EVENT_TYPE_SEND = "send";			// time, data, port
	static final String EVENT_TYPE_RCV = "rcv";				// time, data, class, port
	static final String EVENT_TYPE_RR_REQUEST = "rr-request"; // time, data, class, port
	static final String EVENT_TYPE_RR_REPLY = "rr-reply";	// time, data, port
	static final String EVENT_TYPE_MESSAGE = "msg";		// message
	static final String EVENT_TYPE_FINISH = "finish";		// end of testing
	
	Component host;
	
	public EventTester() 
	{ super(); 	}
	
	public EventTester(Component host_)
	{
		this();
		host = host_; 
	}
	
	
	/** Flag indicates if a batch is running.  */
	transient boolean batchRunning = false;

	transient double startTime;
	transient int sendEvtIndex, rcvEvtIndex;
	transient int nEvtBeenMatched;
	transient Vector vbatch = new Vector(); // vector of events
	transient boolean allmatched, wildcardmatched;
	transient boolean stopped;
	transient boolean waitingForReply = false;
	
	double timeTol = 1e-6; // error tolerance for time matching
	
	/** Is a batch running?  */
	public boolean _isBatchRunning() { return batchRunning; }
	
	/**
	 Returns the index of the next event which the send process is waiting to 
	 execute ("send" event) or match ("receive" event).
	 */
	public int sendProgress() { return sendEvtIndex; }
	public int rcvProgress() { return rcvEvtIndex; }
	public int progress() { return nEvtBeenMatched; }
							   
	/** Removes all the events in the batch.  */
	public synchronized void clear()
	{ 
		batchRunning = false; 
		vbatch.removeAllElements(); 
	}
	
	/**
	 Reset the batch for a new start.
	 Use {@link #clear()} to clear the batch.
	 */
	public synchronized void reset()
	{
		nEvtBeenMatched = 0;
		batchRunning = false;
		stopped = false;
		allmatched = true;
		wildcardmatched = false; // wildcard match
		sendEvtIndex = rcvEvtIndex = 0;
		for (int i=0; i<vbatch.size(); i++)
			((Event)vbatch.elementAt(i)).reset();
	}
	
	/**
	 Stops the running batch.
	 A stopped batched cannot be resumed.  One must restart it.
	 */
	public void stop()
	{ stopped = true; }
	
	public String view()
	{ return view(""); }
	
	String view(String prefix_)
	{
		StringBuffer sb_ = new StringBuffer();
		if (vbatch.size() == 0) sb_.append(prefix_ + "No event in the batch.\n");
		else if (vbatch.size() == 1)sb_.append(prefix_ + "One event in the batch:\n");
		else sb_.append(prefix_ + vbatch.size() + " events in the batch:\n");
		for (int i=0; i<vbatch.size(); i++) {
			Event evt_ = (Event)vbatch.elementAt(i);
			sb_.append(prefix_ + "Event " + i + "| " + evt_ + "\n");
		}
		return sb_.toString();
	}
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer();
		sb_.append("   Running: " + _isBatchRunning() + "\n");
		if (_isBatchRunning()) {
			sb_.append("    Send progress: " + sendProgress() + "/" + (vbatch.size()-1) 
					   + (waitingForReply? " (waiting for reply)": "") + ".\n");
			sb_.append("     Rcv progress: " + rcvProgress() + "/" + (vbatch.size()-1) + ".\n");
			sb_.append("   Total progress: " + progress() + "/" + vbatch.size() + ".\n");
		}
		sb_.append(view("   "));
		return sb_.toString();
	}
	
	/**
	 Verifies rr-request and rr-reply pairs in the batch.
	 @return false if fails to find some pair.
	 */
	public boolean verify()
	{
		boolean failed_ = false;
		for (int i=0; i<vbatch.size(); i++) {
			Event evt_ = (Event)vbatch.elementAt(i);
			if (evt_.type == EVENT_TYPE_RR_REQUEST || evt_.type == EVENT_TYPE_RR_REPLY) {
				if (evt_.related == null) { // no matched event
					post("Error: No paired '" + (evt_.type == EVENT_TYPE_RR_REQUEST? EVENT_TYPE_RR_REPLY: EVENT_TYPE_RR_REQUEST)
						 + "' event for Event " + i + " (" + evt_.type + ").\n");
					failed_ = true;
				}
				else if (evt_.related.related != evt_) { // incorrect match
					Event tmp_ = evt_.related.related;
					post("Error: Event " + i + " (" + evt_.type + ") is paired to Event "
						 + vbatch.indexOf(evt_.related) + " which is paired to "
						 + (tmp_ == null? "null": "Event " + vbatch.indexOf(tmp_))
						 + ".\n");
					failed_ = true;
				}
				else if (evt_.type == evt_.related.type) {
					post("Error: Event " + i + " (" + evt_.type + ") is paired to Event "
						 + vbatch.indexOf(evt_.related) + " which has the same event type.\n");
					failed_ = true;
				}
			}
		}
		return !failed_;
	}
	
	/**
	 Starts a batch run.  Do not call this method directly.  Use run() instead,
	 it will create an appropriate thread to start() the component.
	 */
	public void start()
	{
		reset();
		if (vbatch.size() == 0) {
			post("No event in the batch.\n");
			return;
		}
		if (!(Thread.currentThread() instanceof WorkerThread)) {
			post("EventTester must be started in a workerthread.  Test is stopped.\n");
			return;
		}
		
		if (!verify()) return;
		
		WorkerThread thread_ = (WorkerThread)Thread.currentThread();
		
		post("--------- TEST STARTS ---------\n");
		if (vbatch.size() == 1) post("One event in the batch.\n");
		else post(vbatch.size() + " events in the batch.\n");
		
		batchRunning = true;
		allmatched = true;
		wildcardmatched = false; // wildcard match
		startTime = thread_.getTime();
		
		// loop through send/finish events, rcv events are handled in matchEvent()
		// from process() callback
		for (sendEvtIndex=0; sendEvtIndex<vbatch.size(); sendEvtIndex++) {
			Event e_ = (Event)vbatch.elementAt(sendEvtIndex);
			double now_ = thread_.getTime();
			if (e_.time < 0.0) e_.timeIn = now_;
			String type_ = e_.type;
			if (type_ == EVENT_TYPE_MESSAGE || type_ == EVENT_TYPE_FINISH) {
				host.sleepFor(e_.time + startTime - now_);
				e_.isMatchedYet = true;
				synchronized (this) {
					post("Event " + sendEvtIndex + "| " + e_ + "\n");
					nEvtBeenMatched ++;
				}
			}
			else if (type_ == EVENT_TYPE_SEND) {
				host.sleepFor(e_.time + startTime - now_);
				e_.isMatchedYet = true;
				synchronized (this) {
					post("Event " + sendEvtIndex + "| " + e_ + "\n");
					nEvtBeenMatched ++;
				}
				e_.port.doSending(e_.data);
			}
			else if (type_ ==  EVENT_TYPE_RR_REPLY) {
				host.sleepFor(e_.time + startTime - now_);
				synchronized (e_) {
					if (e_.rrFlag) { // rr-request thread is waiting
						host.notify(e_); // notify the rr-request thread that are waiting on the reply
						if (host.isDebugEnabled()) post("notified " + e_ + "\n");
					}
					else e_.rrFlag = true;
				}
				
				e_.isMatchedYet = true;
				synchronized (this) {
					post("Event " + sendEvtIndex + "| " + e_ + "\n");
					nEvtBeenMatched ++;
				}
			}
			else {
				synchronized (this) {
					while (!e_.isMatchedYet) {
						waitingForReply = true;
						host.wait(this); // until the rcv event is met.
					}
					waitingForReply = false;
				}
			}
		} // for loop on sendEvtIndex
		if (progress() == vbatch.size()) {
			batchRunning = false;
			if (allmatched)	{
				if (wildcardmatched) post("------- (" + EVENT_MATCH_WILDCARD + ") Test finished --------\n");
				else post("------- (" + EVENT_MATCH_MATCHED + ") Test finished --------\n");
			}
			else post("------- (" + EVENT_MATCH_UNMATCHED + ") Test finished --------\n");
		}
	}
	
	static final Event NULL_EVENT = new Event();
	static final Object MATCH_LOCK = new Object();
	
	/** Matches a receipt event.  */
	public void match(double time_, Object data_, Port p_)
	{
		if (!batchRunning) return;
		
		// enter critical region
		host.lock(MATCH_LOCK);
		
		time_ -= startTime;
		String datainfo_ = StringUtil.toString(data_);
		boolean advancing_ = true; // still good to advance rcvEvtIndex
		Event matchedEvt_ = null;
		
		for (int i = rcvEvtIndex; i < vbatch.size(); i++) {
			Event e_ = (Event)vbatch.elementAt(i);
			String type_ = e_.type;
			if (type_ == EVENT_TYPE_MESSAGE ||
				type_ == EVENT_TYPE_FINISH ||
				type_ == EVENT_TYPE_SEND ||
				type_ == EVENT_TYPE_RR_REPLY) {
				if (advancing_) rcvEvtIndex = i+1;
			}
			else { // EVENT_TYPE_RCV, EVENT_TYPE_RR_REQUEST
				if (e_.isMatchedYet) {
					if (advancing_) rcvEvtIndex = i+1;
				}
				else if (time_ + timeTol < e_.time) {
					//System.out.println("trying to match: time " + time_ + " < next expected " + e_.time);
					matchedEvt_ = NULL_EVENT; // break the loop, don't match more
				}
				else {
					e_.match(time_, data_, p_, timeTol);
					//System.out.println("trying to match: (" + e_.matchEvent + ") " + e_);
					if (e_.matchEvent != EVENT_MATCH_UNMATCHED) {
						e_.isMatchedYet = true;
						if (advancing_) rcvEvtIndex = i+1;
						matchedEvt_ = e_;
						post("Event " + i + "| " + e_ + "\n");
						if (e_.matchEvent == EVENT_MATCH_WILDCARD) wildcardmatched = true;
					}
					else advancing_ = false;
				}
			}
			if (matchedEvt_ != null) break;
		}
		
		if (matchedEvt_ == null || matchedEvt_ == NULL_EVENT) {
			// cannot find match
			post("Event ??| (" + EVENT_MATCH_UNMATCHED + ")Cannot find match for: " + 
				 drcl.util.StringUtil.toString(data_) + " arriving at " + p_ + ".\n");
			// leave criticl region
			host.unlock(MATCH_LOCK);
			return;
		}
		
		synchronized (this) {
			nEvtBeenMatched ++;
		}
		/*
		// advance rcvEvtIndex to the next not yet matched rcv-event
		if (advancing_) {
			for (int i = rcvEvtIndex; i < vbatch.size(); i++) {
				Event e_ = (Event)vbatch.elementAt(i);
				String type_ = e_.type;
				if (type_ == EVENT_TYPE_MESSAGE ||
					type_ == EVENT_TYPE_FINISH ||
					type_ == EVENT_TYPE_SEND ||
					type_ == EVENT_TYPE_RR_REPLY) {
					rcvEvtIndex = i+1;
				}
				else { // EVENT_TYPE_RCV, EVENT_TYPE_RR_REQUEST
					if (e_.isMatchedYet) rcvEvtIndex = i+1;
					else advancing_ = false;
				}
				if (!advancing_) break;
			}
		}
		*/
		
		// leave criticl region
		host.unlock(MATCH_LOCK);
		
		// hold the thread until rr-reply notifies
		if (matchedEvt_.type == EVENT_TYPE_RR_REQUEST) {
			Event replyEvt_ = matchedEvt_.related;
			synchronized (replyEvt_) {
				// notify send-thread if it is blocked
				synchronized(this) {
					host.notify(this);
				}
				if (!replyEvt_.rrFlag) {
					replyEvt_.rrFlag = true;
					if (host.isDebugEnabled()) post("wait on " + replyEvt_ + "\n");
					host.wait(replyEvt_);
				}
			}
			
			//System.out.println("Got reply: " + replyEvt_.data);
			// reply must be sent by rr-request thread
			replyEvt_.port.doSending(replyEvt_.data);
		}
		else {
			// notify send-thread if it is blocked
			host.notify(this);
		}
	}
	
	//
	private void ___EVENT_MGT___() {}
	//
	
	/**
	 Adds a "send"/"rr-reply" event.  Must specify time, object to send and the port on 
	 which the object is to send.  The time is relative to the time when the 
	 batch starts.
	 */
	public synchronized Event addEvent(String evtType_, double time_, Object toSend_, Port p_)
	{
		Event evt_ = Event.create(evtType_, time_, toSend_, p_);
		if (evt_ == null) {
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
					  + ", toSend=" + toSend_ + ", port=" + p_ + ".\n");
		}
		else {
			if (evt_.type == EVENT_TYPE_RR_REPLY) {
				Event last_ = findMatchedRRRequestEvent(vbatch.size()-1, p_);
				if (last_ == null) {
					drcl.Debug.error("Last event does not exist for reference.  Failed to add the event.\n");
					return null;
				}
				evt_.related = last_;
				last_.related = evt_;
			}
			vbatch.addElement(evt_);
		}
		return evt_;
	}
	
	Event findMatchedRRRequestEvent(int startingIndex_, Port port_)
	{
		// look for last rr-request event
		for (int i=startingIndex_; i>=0; i--) {
			Event tmp_ = (Event)vbatch.elementAt(i);
			if (tmp_.type == EVENT_TYPE_RR_REQUEST && tmp_.related == null
				&& (port_ == null || port_ == tmp_.port))
				return tmp_;
		}
		return null;
	}
	
	/** Adds a "rr-reply" event, corresponding to last event.  */
	public synchronized Event addEvent(String evtType_, double time_, Object toSend_)
	{
		Event last_ = findMatchedRRRequestEvent(vbatch.size()-1, null);
		if (last_ == null) {
			drcl.Debug.error("Last event does not exist for reference.  Failed to add the event.\n");
			return null;
		}
		Event evt_ = Event.create(evtType_, time_, toSend_, last_.port);
		if (evt_ == null) {
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
						  + ", toSend=" + toSend_ + ".\n");
		}
		else vbatch.addElement(evt_);
		evt_.related = last_;
		last_.related = evt_;
		return evt_;
	}
	
	/**
	 Adds a "receive"/"rr-request" event.  
	 When a data is received, time, data itself, class of data and port are matched
	 with the event.  In the event, if time is less than 0, time always matches.
	 Similarly, data always matches if null is specified.
	 Class always matches if null is specified.
	 The time is relative to the time when the batch starts.
	 */
	public synchronized Event addEvent(String evtType_, double time_, Object toRcv_, String classExpected_, Port p_)
	{
		Event evt_ = Event.create(evtType_, time_, toRcv_, classExpected_, p_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
					  + ", toRcv=" + toRcv_ + ", classExpected=" + classExpected_
					  + ", port=" + p_ + ".\n");
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Adds a "receive" event.  
	 Exactly the same as the other add-recv-event method except that port is
	 specified in group id and port id.  Group id always matches if null is
	 specified.  Port id always matches if null is specified.
	 */
	public synchronized Event addEvent(String evtType_, double time_, Object toRcv_, String classExpected_, String portDescription_)
	{
		Event evt_ = Event.create(evtType_, time_, toRcv_, classExpected_, portDescription_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
					  + ", toRcv=" + toRcv_ + ", classExpected=" + classExpected_
					  + ", port=" + portDescription_ + ".\n");
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Adds a finish event.  The event is to finish the batch run no matter 
	 whether there are still events behind.
	 */
	public synchronized Event addEvent(String evtType_, double time_)
	{
		Event evt_ = Event.create(evtType_, time_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ + ".\n");
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Adds a message event.  Executing the event results in the message being
	 printed to the debug channels.
	 */
	public synchronized Event addEvent(String evtType_, double time_, String msg_)
	{
		Event evt_ = Event.create(evtType_, time_, msg_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ 
					  + ", time=" + time_ + ", msg=" + msg_ + ".\n");
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Adds a message event.  Executing the event results in the message being
	 printed to the debug channels.
	 */
	public synchronized Event addEvent(String msg_)
	{
		Event evt_ = Event.create(EVENT_TYPE_MESSAGE, -1.0, msg_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for:  msg=" + msg_ + ".\n");
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Removes the index'th event in the batch.
	 */
	public synchronized Event removeEvent(int index_)
	{
		if (index_ < vbatch.size())	{
			Event e_ = (Event)vbatch.elementAt(index_);
			vbatch.removeElementAt(index_);
			return e_;
		}
		else return null;
	}
	
	/**
	 Inserts a "send"/"rr-reply" event
	 @see #addEvent(String, double, Object, Port)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_, Object toSend_, Port p_)
	{
		Event evt_ = Event.create(evtType_, time_, toSend_, p_);
		if (evt_ == null) {
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
					  + ", toSend=" + toSend_ + ", port=" + p_ + ".\n");
			return null;
		}
		if (evt_.type == EVENT_TYPE_RR_REPLY) {
			Event last_ = findMatchedRRRequestEvent(index_-1, p_);
			if (last_ == null) {
				drcl.Debug.error("Last event does not exist for reference.  Failed to add the event.\n");
				return null;
			}
			evt_.related = last_;
			last_.related = evt_;
		}
		if (index_ < vbatch.size())
			vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Inserts a "rr-reply" event, corresponding to last event.
	 @see #addEvent(String, double, Object)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_, Object toSend_)
	{
		Event last_ = findMatchedRRRequestEvent(index_-1, null);
		if (last_ == null) {
			drcl.Debug.error("Last event does not exist for reference.  Failed to add the event.\n");
			return null;
		}
		Event evt_ = Event.create(evtType_, time_, toSend_, last_.port);
		if (evt_ == null) {
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
						  + ", toSend=" + toSend_ + ".\n");
		}
		else if (index_ < vbatch.size())
			vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		evt_.related = last_;
		last_.related = evt_;
		return evt_;
	}
	
	/**
	 Inserts a "receive" event
	 @see #addEvent(String, double, Object, String, Port)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_, Object toRcv_, String classExpected_, Port p_)
	{
		Event evt_ = Event.create(evtType_, time_, toRcv_, classExpected_, p_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ + ", time=" + time_ 
					  + ", toRcv=" + toRcv_ + ", classExpected=" + classExpected_
					  + ", port=" + p_ + ".\n");
		else if (index_ < vbatch.size())	vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Inserts a "receive" event.
	 @see #addEvent(String, double, Object, String, String)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_, Object toRcv_, String classExpected_, String portDescription_)
	{
		Event evt_ = Event.create(evtType_, time_, toRcv_, classExpected_, portDescription_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ 
					  + ", time=" + time_ + ", toRcv=" + toRcv_ + ", classExpected=" + classExpected_
					  + ", port=" + portDescription_ + ".\n");
		else if (index_ < vbatch.size())	vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Inserts a finish event.
	 @see #addEvent(String, double)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_)
	{
		Event evt_ = Event.create(evtType_, time_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ 
					  + ", time=" + time_ + ".\n");
		else if (index_ < vbatch.size())	vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Inserts a message event.
	 @see #addEvent(String, double, String)
	 */
	public synchronized Event insertEvent(int index_, String evtType_, double time_, String msg_)
	{
		Event evt_ = Event.create(evtType_, time_, msg_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: type=" + evtType_ 
					  + ", time=" + time_ + ", msg=" + msg_ + ".\n");
		else if (index_ < vbatch.size())	vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	/**
	 Inserts a message event.
	 @see #addEvent(String, double, String)
	 */
	public synchronized Event insertEvent(int index_, String msg_)
	{
		Event evt_ = Event.create(EVENT_TYPE_MESSAGE, -1.0, msg_);
		if (evt_ == null)
			drcl.Debug.error("Event does not create for: msg=" + msg_ + ".\n");
		else if (index_ < vbatch.size())	vbatch.insertElementAt(evt_, index_);
		else vbatch.addElement(evt_);
		return evt_;
	}
	
	//
	private void ___MISC___() {}
	//
	
	public void duplicate(Object source_)
	{
	}
	
	public void setTimeDiffTolerance(double d_) 
	{ timeTol = d_; }
	
	public double getTimeDiffTolerance() 
	{ return timeTol; }
	
	void post(String msg_)
	{	drcl.Debug.debug(msg_);	}
	
	//
	private void ___EVENT___() {}
	//
	
	static final String EVENT_MATCH_MATCHED = "v";
	static final String EVENT_MATCH_UNMATCHED = "x";
	static final String EVENT_MATCH_WILDCARD = "?";

	static class Event extends drcl.DrclObj
	{
		String type; // see EVENT_TYPE_XXX
		double time; // may be Double.NaN
		
		// either of the followings for rcv events, send events must use data
		Object data;
		Class classExpected;
		Object reply;
		
		// either of the followings for rcv events, send events must use port
		Port port;
		String portDescription; // XX@XX, allows wildcard spec
		
		Event related;
		// 1. used by SR_SEND to get the corresponding SR_RCV event
		// 2. used by RR_REPLY to notify the corresponding RR_REQUEST event (which is blocked)
		
		boolean isMatchedYet = false; // has this event been matched yet?
		String matchTime; // data is matched?
		String matchData; // data is matched?
		String matchClass; // class is matched?
		String matchPort; // port is matched?
		String matchEvent = EVENT_MATCH_MATCHED; // the event is completely matched?
		
		// if doesn't match, the followings are incoming objects
		double timeIn;
		Object dataIn;
		Class classIn;
		Port portIn;

		// for sync between rr-request and rr-reply threads
		// whoever first reaches will set this flag to let the other know
		boolean rrFlag = false;
		
		public void reset()
		{
			dataIn = null; classIn = null; portIn = null;
			isMatchedYet = false;
			rrFlag = false;
		}
		
		public static Event create(String evtType_, double time_, Object target_, Port p_)
		{
			String type_ = evtType_ != null? evtType_.toLowerCase(): null;
			
			if (type_ != null && (type_.equals(EVENT_TYPE_RCV) || type_.equals(EVENT_TYPE_RR_REQUEST)))
				return create(type_, time_, target_, null, p_);
			
			if (type_ == null || (!type_.equals(EVENT_TYPE_SEND) && !type_.equals(EVENT_TYPE_RR_REPLY))) {
				drcl.Debug.error("Unrecognized event format for type:'" + evtType_ + "'.\nEvent type must be '" + EVENT_TYPE_SEND +  "' or '" + EVENT_TYPE_RR_REPLY + "'.\n");
				return null;
			}
		
			if (time_ < 0.0) {
				drcl.Debug.error("Must specify time for a send-event.\n");
				return null;
			}
			Event evt_ = new Event();
			evt_.type = type_.intern();
			evt_.time = time_;
			evt_.data = target_;
			evt_.port = p_;
			return evt_;
		}
		
		public static Event create(String evtType_, double time_, Object target_, String classExpected_, 
								   Port p_)
		{
			String type_ = evtType_ != null? evtType_.toLowerCase(): null;
			if (type_ == null || (!type_.equals(EVENT_TYPE_RCV) && !type_.equals(EVENT_TYPE_RR_REQUEST))) {
				drcl.Debug.error("Unrecognized event format for type:'" + evtType_ + "'.\nEvent type must be '" + EVENT_TYPE_RCV + "' or '" + EVENT_TYPE_RR_REQUEST + "'.\n");
				return null;
			}
			Event evt_ = new Event();
			evt_.type = type_.intern();
			evt_.time = time_;
			evt_.data = target_;
			if (classExpected_ != null && target_ != null) evt_.classExpected = target_.getClass();
			else
				try {
					evt_.classExpected = classExpected_ == null? null: Class.forName(classExpected_);
				}
				catch (Exception e_) {
					drcl.Debug.error("EventTester.Event.create(): " + e_ + "\n");
					return null;
				}
			evt_.port = p_;
			return evt_;
		}
		
		public static Event create(String evtType_, double time_, Object target_, String classExpected_, 
								   String portDescription_)
		{
			String type_ = evtType_ != null? evtType_.toLowerCase(): null;
			if (type_ == null || !type_.equals(EVENT_TYPE_RCV)) {
				drcl.Debug.error("Unrecognized event format for type:'" + evtType_ + "'.\nEvent type must be '" + EVENT_TYPE_RCV + "'.\n");
				return null;
			}
			Event evt_ = new Event(); 
			evt_.type = type_.intern();
			evt_.time = time_;
			evt_.data = target_;
			if (classExpected_ != null && target_ != null) evt_.classExpected = target_.getClass();
			else
				try {
					evt_.classExpected = classExpected_ == null? null: Class.forName(classExpected_);
				}
				catch (Exception e_) {
					drcl.Debug.error("EventTester.Event.create(): " + e_ + "\n");
					return null;
				}
			evt_.port = null;
			evt_.portDescription = portDescription_;
			return evt_;
		}
		
		public static Event create(String evtType_, double time_, String msg_)
		{
			String type_ = evtType_ != null? evtType_.toLowerCase(): null;
			if (type_ == null || !type_.equals(EVENT_TYPE_MESSAGE)) {
				drcl.Debug.error("Unrecognized event format for type:'" + evtType_ + "'.\nEvent type must be '" + EVENT_TYPE_MESSAGE + "'.\n");
				return null;
			}
			Event evt_ = new Event(); 
			evt_.type = type_.intern();
			evt_.time = time_;
			evt_.data = msg_;
			return evt_;
		}
		
		public static Event create(String evtType_, double time_)
		{
			String type_ = evtType_ != null? evtType_.toLowerCase(): null;
			if (type_ == null || !type_.equals(EVENT_TYPE_FINISH)) {
				drcl.Debug.error("Unrecognized event format for type:'" + evtType_
					+ "'.\nEvent type must be '" + EVENT_TYPE_FINISH + "'.\n");
				return null;
			}
			Event evt_ = new Event(); 
			evt_.type = type_.intern();
			evt_.time = time_;
			return evt_;
		}
		
		public void match(double time_, Object data_, Port p_, double timeTol_)
		{
			timeIn = time_;
			dataIn = data_;
			portIn = p_;
			
			boolean match_ = true, wildcardmatch_ = false;
			// match time
			if (time < 0.0 || Double.isNaN(time)) {
				matchTime = EVENT_MATCH_WILDCARD;
				wildcardmatch_ = true;
			}
			else if (time >= 0.0 && Math.abs(time_ - time) > timeTol_) {
				match_ = false;
				matchTime = EVENT_MATCH_UNMATCHED;
			}
			else matchTime = EVENT_MATCH_MATCHED;
				
			// match port
			if (port == null && portDescription == null) {
				// no port spec
				matchPort = EVENT_MATCH_WILDCARD;
				wildcardmatch_ = true;
			}
			else if (port != null) {
				if (p_ != port) {
					match_ = false;
					matchPort = EVENT_MATCH_UNMATCHED;
				}
				else matchPort = EVENT_MATCH_MATCHED;
			}
			else {
				if (drcl.util.StringUtil.match2(p_.getID() + "@" + p_.getGroupID(), portDescription)) {
					matchPort = EVENT_MATCH_MATCHED;
				}
				else {
					match_ = false;
					matchPort = EVENT_MATCH_UNMATCHED;
				}
			}
				
			// match class
			classIn = data_ == null? null: data_.getClass();
			if (classExpected != null) {
				if (classIn == null || !classExpected.isAssignableFrom(classIn)) {
					match_ = false;
					matchClass = EVENT_MATCH_UNMATCHED;
				}
				else matchClass = EVENT_MATCH_MATCHED;
			}
			
			// match object
			if (data == null && data_ != null) {
				matchData = EVENT_MATCH_WILDCARD;
				wildcardmatch_ = true;
			}
			else if (!ObjectUtil.equals(data, data_)) {
				match_ = false;
				matchData = EVENT_MATCH_UNMATCHED;
			}
			else matchData = EVENT_MATCH_MATCHED;
			
			if (match_)
				matchEvent = wildcardmatch_? EVENT_MATCH_WILDCARD: EVENT_MATCH_MATCHED;
			else
				matchEvent = EVENT_MATCH_UNMATCHED;
		}
		
		public String toString()
		{
			StringBuffer sb_ = new StringBuffer();
			if (isMatchedYet) sb_.append("(" + matchEvent + ")");
			if (type != null) sb_.append(type + "| ");
			if (type == EVENT_TYPE_FINISH)
				return sb_.toString() + (time >= 0.0? time+"":"");
			else if (type == EVENT_TYPE_MESSAGE)
				return sb_.toString() + data;
			else if (type == EVENT_TYPE_SEND || type == EVENT_TYPE_RR_REPLY)
				return sb_.toString() + (time>=0.0? time: timeIn) + "| " + port + "| " + drcl.util.StringUtil.toString(data);
			else { // EVENT_TYPE_RCV, EVENT_TYPE_RR_REQUEST:
				if (isMatchedYet) {
					// time
					sb_.append("(" + matchTime + ")");
					if (matchTime == EVENT_MATCH_MATCHED)
						sb_.append(time);
					else if (matchTime == EVENT_MATCH_UNMATCHED)
						sb_.append(time + "(" + timeIn + ")");
					else
						sb_.append(timeIn);
					
					// port
					sb_.append("| (" + matchPort + ")");
					String tmp_ = port != null? port.toString(): portDescription;
					if (matchPort == EVENT_MATCH_MATCHED)
						sb_.append(tmp_);
					else if (matchPort == EVENT_MATCH_UNMATCHED)
						sb_.append(tmp_ + "(" + portIn + ")");
					else
						sb_.append(portIn);
					
					// class
					if (classExpected != null) {
						sb_.append("| (" + matchClass + ")");
						if (matchClass == EVENT_MATCH_MATCHED)
							sb_.append(drcl.util.StringUtil.toString(classExpected));
						else if (matchClass == EVENT_MATCH_UNMATCHED)
							sb_.append(drcl.util.StringUtil.toString(classExpected) + "("
									   + drcl.util.StringUtil.toString(classIn) + ")");
					}
					
					// data
					sb_.append("| (" + matchData + ")");
					if (matchData != EVENT_MATCH_UNMATCHED)
						sb_.append(drcl.util.StringUtil.toString(dataIn));
					else
						sb_.append(drcl.util.StringUtil.toString(data) + "(" 
								   + drcl.util.StringUtil.toString(dataIn) + ")");
					
					return sb_.toString();
				}
				else
					return sb_ + " expected time:" + (time < 0.0? "*": time+"")
						   + "| expected port:" + (port==null&&portDescription== null? "*": (port==null?portDescription: port.toString()))
						   + (classExpected == null? "": "| expected class:" + classExpected.getName())
						   + "| expected data:" + drcl.util.StringUtil.toString(data);
			}
		}
	}
}
