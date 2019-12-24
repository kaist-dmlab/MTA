// @(#)CountMonitor2.java   1/2004
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

package drcl.comp.tool;

import java.util.Enumeration;
import drcl.data.IntObj;
import drcl.comp.Port;
import drcl.comp.contract.GarbageContract;
import drcl.util.scalar.IntSpace;
import drcl.util.scalar.LongSpace;
import drcl.util.queue.FIFOQueue;
/**
This component monitors an "object stream" (defined by
{@link drcl.data.Countable})
and outputs its average rate and object loss rate.
This component works the same as {@link CountMonitor} except that it also 
calculates object loss rate in the current time window, provided that the
objects contain correct "sequence" information as defined in
{@link drcl.data.Countable}.
It calculates the loss rate by summing up the "gaps" found between the
ordering information in the objects in the current time window.
The calculation may not be correct if objects may arrive out
of order in different windows.

<p>Same as {@link CountMonitor}, this component is configured by two
parameters: the window size (default one second) and the output interval
(default 0.5 second).
It can operate in the "size" mode, the "object" mode, or both.
The throughput events are exported at either the <code>sizecount@</code>
port or the <code>objcount@</code> port, and the loss rate exported at either
the <code>sizeloss@</code> port or the <code>objloss@</code> port, both in 
percentage(%).
 */
public class CountMonitor2 extends drcl.comp.Extension
{
	Port timerPort = addForkPort(".timer");
	private IntSpace unreceivedObj = new IntSpace(); // not received sequence #
	private LongSpace unreceivedSize = new LongSpace(); // not received sizes
	boolean active = false; // active when have something to report
	int nextobjcount;
	long nextsizecount;
		// in case window has no object, we can still use these values to
		// calculate loss rate
	
	// Measurement parameters
	double winSize = 1.0; // in second
	double interval = 0.5; // second;
	
	// Window information
	private FIFOQueue window = new FIFOQueue();
		// save a window of object information (size, LongObj), key is object 
		// arrival time
	long winobjcount, winsizecount;
	
	// Flag
	static final int OBJ_INFO_FLAG  = 1 << 0;
	static final int SIZE_INFO_FLAG = 1 << 1;
	static final int NOT_FIRST_TIME_OBJ_COUNT  = 1 << 2;
	static final int NOT_FIRST_TIME_OBJ_LOSS   = 1 << 3;
	static final int NOT_FIRST_TIME_SIZE_COUNT = 1 << 4;
	static final int NOT_FIRST_TIME_SIZE_LOSS  = 1 << 5;
	int flag = SIZE_INFO_FLAG; // only size info is exported by default
	
	/** ID of the port to export the object-mode throughput events. */
	public static final String OBJ_COUNT_PORT_ID = "objcount";
	/** ID of the port to export the object-mode object-loss-rate events. */
	public static final String OBJ_LOSS_PORT_ID = "objloss";
	/** ID of the port to export the size-mode throughput events. */
	public static final String SIZE_COUNT_PORT_ID = "sizecount";
	/** ID of the port to export the size-mode object-loss-rate events. */
	public static final String SIZE_LOSS_PORT_ID = "sizeloss";
	
	/** Name of the object-mode rate events. */
	String OBJ_COUNT_EVENT = "Average Object Rate";
	/** Name of the object-mode object-loss-rate events. */
	String OBJ_LOSS_EVENT ="Average Object Loss Rate";
	/** Name of the size-mode rate events. */
	String SIZE_COUNT_EVENT = "Average Rate";
	/** Name of the size-mode loss-rate events. */
	String SIZE_LOSS_EVENT = "Average Loss Rate";
	
	/** Port to output "object" count events. */
	protected Port objcountPort = addEventPort(OBJ_COUNT_PORT_ID);
	/** Port to output "object" loss events. */
	protected Port objlossPort = addEventPort(OBJ_LOSS_PORT_ID);
	/** Port to output "size" count events. */
	protected Port sizecountPort = addEventPort(SIZE_COUNT_PORT_ID);
	/** Port to output "size" loss events. */
	protected Port sizelossPort = addEventPort(SIZE_LOSS_PORT_ID);
	
	public CountMonitor2() 
	{ super(); }
	
	public CountMonitor2(String id_)
	{ super(id_); }
	
	public synchronized void reset()
	{
		super.reset();
		winobjcount = winsizecount = 0;
		active = false;
		unreceivedObj.reset();
		unreceivedSize.reset();
		window.reset();
		flag &= 3;
	}
	
	public void duplicate(Object source_)
	{
		if (!(source_ instanceof CountMonitor2)) return;
	}
	
	protected synchronized void process(Object data_, Port inPort_) 
	{
		// timeout from next run; just ignore
		if (!active && inPort_ == timerPort) return;
		
		// time to report
		if (inPort_ == timerPort) {
			adjustWindow();
			active = false;
			if (isObjModeEnabled()) {
				if ((flag & NOT_FIRST_TIME_OBJ_COUNT) == 0) {
					objcountPort.exportEvent(OBJ_COUNT_EVENT, 
									winobjcount / winSize, null);
					flag |= NOT_FIRST_TIME_OBJ_COUNT;
				}
				else
					objcountPort.doLastSending(
									new Double(winobjcount / winSize));
				active = winobjcount > 0;
				if (objlossPort._isEventExportEnabled()) {
					long losscount_ = unreceivedObj.getSizeUpTo(
									(int)nextobjcount);
					double lossrate_ = losscount_ == 0? 0.0:
							100.0 * losscount_ / (winobjcount + losscount_);
					if ((flag & NOT_FIRST_TIME_OBJ_LOSS) == 0) {
						objlossPort.exportEvent(OBJ_LOSS_EVENT,
										lossrate_, null);
						flag |= NOT_FIRST_TIME_OBJ_LOSS;
					}
					else
						objlossPort.doLastSending(new Double(lossrate_));
					active = active | losscount_ > 0;
				}
			}
				
			if (isSizeModeEnabled()) {
				if ((flag & NOT_FIRST_TIME_SIZE_COUNT) == 0) {
					sizecountPort.exportEvent(SIZE_COUNT_EVENT,
									(winsizecount << 3) / winSize, null);
					flag |= NOT_FIRST_TIME_SIZE_COUNT;
				}
				else
					sizecountPort.doLastSending(
									new Double((winsizecount << 3) / winSize));
				active = active | winsizecount > 0;
				if (sizelossPort._isEventExportEnabled()) {
					long losscount_ = unreceivedSize.getSizeUpTo(nextsizecount);
					double lossrate_ = losscount_ == 0? 0.0:
							100.0 * losscount_ / (winsizecount + losscount_);
					if ((flag & NOT_FIRST_TIME_SIZE_LOSS) == 0) {
						sizelossPort.exportEvent(SIZE_LOSS_EVENT,
										lossrate_, null);
						flag |= NOT_FIRST_TIME_SIZE_LOSS;
					}
					else
						sizelossPort.doLastSending(new Double(lossrate_));
					active = active | losscount_ > 0;
				}
			}

			// activate next timeout
			if (active)
				fork(timerPort, this, interval);
			return;
		}

		if (data_ instanceof GarbageContract.Message)
			data_ = ((GarbageContract.Message)data_).getData();

		if (data_ instanceof drcl.data.Countable) { 
			drcl.data.Countable p_ = (drcl.data.Countable)data_;
			double now_ = getTime();
			
			int objsize_ = p_.getSize();
			nextobjcount = p_.getNumberCount();
			nextsizecount = p_.getSizeCount() + objsize_;
			window.enqueue(now_,
						new ObjInfo(objsize_, nextobjcount, nextsizecount));
			winobjcount ++;
			winsizecount += objsize_;
			unreceivedObj.checkout((int)nextobjcount-1);
			unreceivedSize.checkout(nextsizecount - objsize_, nextsizecount);	
			
			if (!active) {
				active = true;
				// make output times predictable (not dependent on the time
				// when the first data comes in
				//fork(timerPort, this, interval);
				forkAt(timerPort, this, Math.ceil(now_/winSize)*winSize);
			}
		}
		else if (isErrorNoticeEnabled())
			error(data_, "process()", inPort_, "unknown data");
	}
	
	/** Adjusts all the window information with respect to the current time. */
	void adjustWindow()
	{
		double now_ = getTime();
		double newStart_ = now_ - winSize; // start time of the window
		
		ObjInfo objInfo_ = null;
		while (!window.isEmpty()) {
			double time_ = window.firstKey();
			objInfo_ = (ObjInfo)window.firstElement();
			if (time_ < newStart_) { // out-of-window object info
				winobjcount --;
				winsizecount -= objInfo_.size;
				window.dequeue();
			}
			else break;
		}
		//if (window.getSize() > 0)
		//	System.out.println("ADJUST---" + start_ + " -> " + now_
		//		+ ", first key=" + window.firstKey());
		//else System.out.println("ADJUST---empty window");
		
		if (objInfo_ == null) {
			// check window parameters
			if (winsizecount != 0 || winobjcount != 0 || !window.isEmpty())
				error("adjustWindow()", "window should contain no object");
		}
		else {
			// adjust unreceived spaces for calculating loss
			unreceivedObj.checkoutUntil(objInfo_.count);
			unreceivedSize.checkoutUntil(objInfo_.sizeCount);
		}
	}
		
	public String info()
	{
		return "Actively Reporting: " + active + "\n"
			+ " Obj Count Event = " + OBJ_COUNT_EVENT + "\n"
			+ " Obj  Loss Event = " + OBJ_LOSS_EVENT + "\n"
		   	+ "Size Count Event = " + SIZE_COUNT_EVENT + "\n"
		   	+ "Size  Loss Event = " + SIZE_LOSS_EVENT + "\n"
			+ "     Window size = " + winSize + " (sec)\n"
			+ " Output interval = " + interval + "(sec)\n"
			+ "    Number count = " + winobjcount + "\n"
			+ "      Size count = " + winsizecount + "\n"
			+ "Unreceived objects:\n" + unreceivedObj + "\n"
			+ "Unreceived sizes:\n" + unreceivedSize + "\n"
			+ (winobjcount < 15? "Window:\n" + window.toString():
								 "Window starts at: " + window.firstKey());
	}
	
	private void ___PROPERTY___() {}
	
	/**
	 * Configures this traffic monitor.
	 * @param wsize_ the window size in second.
	 * @param uint_ the output interval in second.
	 */
	public void configure(double wsize_, double uint_)
	{
		winSize = wsize_;
		interval = uint_;
	}

	/** Sets the event names to be outputted with the report events. */
	public void setEventNames(String objCountEvent_, String objLossEvent_,
				   	String sizeCountEvent_, String sizeLossEvent_)
	{
		OBJ_COUNT_EVENT = objCountEvent_;
		OBJ_LOSS_EVENT = objLossEvent_;
		SIZE_COUNT_EVENT = sizeCountEvent_;
		SIZE_LOSS_EVENT = sizeLossEvent_;
	}

	/** Sets the "object" event names to be outputted with the report events. */
	public void setObjectEventNames(String objCountEvent_, String objLossEvent_)
	{
		OBJ_COUNT_EVENT = objCountEvent_;
		OBJ_LOSS_EVENT = objLossEvent_;
	}

	/** Sets the "size" event names to be outputted with the report events. */
	public void setSizeEventNames(String sizeCountEvent_, String sizeLossEvent_)
	{
		SIZE_COUNT_EVENT = sizeCountEvent_;
		SIZE_LOSS_EVENT = sizeLossEvent_;
	}

	/** Sets the output interval (in second). */
	public void setOutputInterval(double int_)
	{ interval = int_; }
	
	/** Returns the output interval (in second). */
	public double getOutputInterval()
	{ return interval; }
	
	/** Sets the size of the measurement window (in second). */
	public void setWindowSize(double size_)
	{ winSize = size_; }
	
	/** Returns the size of the measurement window (in second). */
	public double getWindowSize()
	{ return winSize; }
	
	/** Enables/disables the "object" mode. */
	public void setObjModeEnabled(boolean enabled_)
	{
		if (enabled_) flag |= OBJ_INFO_FLAG; 
		else flag &= ~OBJ_INFO_FLAG;
	}
	
	/** Returns true if the "object" mode is enabled. */
	public boolean isObjModeEnabled()
	{ return (flag & OBJ_INFO_FLAG) > 0; }
	
	/** Enables/disables the "size" mode. */
	public void setSizeModeEnabled(boolean enabled_)
	{
		if (enabled_) flag |= SIZE_INFO_FLAG; 
		else flag &= ~SIZE_INFO_FLAG;
	}
	
	/** Returns true if the "size" mode is enabled. */
	public boolean isSizeModeEnabled()
	{ return (flag & SIZE_INFO_FLAG) > 0; }

	class ObjInfo
	{
		int size;
		int count;
		long sizeCount;

		ObjInfo(int size_, int count_, long sizeCount_)
		{
			size = size_;
			count = count_;
			sizeCount = sizeCount_;
		}

		public String toString()
		{
			return "size=" + size + ",count=" + count
					+ ",sizeCount=" + sizeCount;
		}
	}
}
