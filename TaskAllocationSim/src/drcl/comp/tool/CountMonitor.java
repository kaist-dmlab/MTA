// @(#)CountMonitor.java   1/2004
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
and outputs average rate.

It keeps a fixed-size window of objects.  The rate at current time is then
calculated by summing up the sizes of the objects in the current window
divided by the window size.  Then the results are exported 
every <code>outputInterval</code> second.  The default window size is 5 seconds
and the default output interval is one second.

<p>This component can operate in either "size" mode, "object" mode or both.
If the "size" mode is enabled, the component exports the rate, in the unit of
the size/second, at the <code>sizecount@</code> port.
If the "object" mode is enabled, it exports the rate, in object/second, at
the <code>objcount@</code> port.

<p>The first exported event is in the following format
(<code>drcl.comp.contract.EventMsg</code>):
<ul>
<li> Event name: "Average Rate" ("size" mode, see {@link #SIZE_COUNT_EVENT}) or
	"Average Object Rate" ("object" mode, see {@link #OBJ_COUNT_EVENT}).
<li> Event object: the calculated rate in <code>Double</code>.
<li> Event description: <code>null</code>.
</ul>
while the subsequent events are in <code>Double</code>.
 */
public class CountMonitor extends drcl.comp.Extension
{
	Port timerPort = addPort(new Port(Port.PortType_FORK), ".timer", 
					false/*unremovable*/);

	boolean started = false; // started when receiving first object
	
	// Measurement parameters
	double winSize = 5.0; // in second
	double interval = 1; // second;
	
	// Window information
	private FIFOQueue window = new FIFOQueue();
		// save a window of object information (size, LongObj), 
		// key is object arrival time
	long winobjcount, winsizecount;
	
	// Flag
	static final int OBJ_INFO_FLAG  = 1 << 0;
	static final int SIZE_INFO_FLAG = 1 << 1;
	static final int NOT_FIRST_TIME_OBJ_COUNT  = 1 << 2;
	static final int NOT_FIRST_TIME_SIZE_COUNT = 1 << 3;
	int flag = SIZE_INFO_FLAG; // only size info is exported by default
	
	/** ID of the port to export the object-mode events.*/
	public static final String OBJ_COUNT_PORT_ID = "objcount";
	/** ID of the port to export the size-mode events.*/
	public static final String SIZE_COUNT_PORT_ID = "sizecount";
	
	/** Name of the object-mode events.*/
	String OBJ_COUNT_EVENT = "Average Object Rate";
	/** Name of the size-mode events.*/
	String SIZE_COUNT_EVENT = "Average Rate";
	
	/** Port to output "object" count statistics. */
	protected Port objcountPort  = addEventPort(OBJ_COUNT_PORT_ID);
	/** Port to output "size" count statistics. */
	protected Port sizecountPort = addEventPort(SIZE_COUNT_PORT_ID);
	
	public CountMonitor() 
	{ super(); }
	
	public CountMonitor(String id_)
	{ super(id_); }
	
	public synchronized void reset()
	{
		super.reset();
		winobjcount = winsizecount = 0;
		started = false;
		window.reset();
		flag &= 3;
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
	}
	
	protected synchronized void process(Object data_, Port inPort_) 
	{
		// timeout from next run; just ignore
		if (!started && inPort_ == timerPort) return;
		
		// time to report
		if (inPort_ == timerPort) {
			adjustWindow();

			started = false;
			if (isObjModeEnabled()) {
				if ((flag & NOT_FIRST_TIME_OBJ_COUNT) == 0) {
					objcountPort.exportEvent(OBJ_COUNT_EVENT, 
									winobjcount / winSize, null);
					flag |= NOT_FIRST_TIME_OBJ_COUNT;
				}
				else
					objcountPort.doLastSending(new Double(winobjcount/winSize));
				started = winobjcount > 0;
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
				started = started | winsizecount > 0;
			}

			// activate next timeout
			if (started)
				fork(timerPort, this, interval);
			return;
		}

		if (data_ instanceof GarbageContract.Message)
			data_ = ((GarbageContract.Message)data_).getData();

		if (data_ instanceof drcl.data.Countable) {
			// Obtain Packet from incoming data
			drcl.data.Countable p_ = (drcl.data.Countable)data_;
			double now_ = getTime();
			
			int objsize_ = p_.getSize();
			window.enqueue(now_, new IntObj(objsize_));
			winobjcount ++;
			winsizecount += objsize_;
			
			if (!started) {
				started = true;
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
		double newStart_ = now_ - winSize; // new start time of the window
		
		while (!window.isEmpty()) {
			double time_ = window.firstKey();
			IntObj objInfo_ = (IntObj)window.firstElement();
			if (time_ < newStart_) { // out-of-window object info
				winobjcount --;
				winsizecount -= objInfo_.value;
				window.dequeue();
			}
			else break;
		}
		//if (window.getSize() > 0)
		//	System.out.println("ADJUST---" + start_ + " -> " + now_ + ", first key=" + window.firstKey());
		//else System.out.println("ADJUST---empty window");
	}
		
	public String info()
	{
		return !started? "Not receiving any object yet":
			  " Obj Event Name = " + OBJ_COUNT_EVENT + "\n"
		   	+ "Size Event Name = " + SIZE_COUNT_EVENT + "\n"
			+ "    Window size = " + winSize + " (sec)\n"
			+ "Output interval = " + interval + "(sec)\n"
			+ "   Number count = " + winobjcount + "\n"
			+ "     Size count = " + winsizecount + "\n"
			+ (winobjcount < 15? "Window:\n" + window.toString():
								 "Window starts at: " + window.firstKey());
	}
	
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

	/** Sets event names for both object/size count events. */
	public void setEventNames(String objEventName_, String sizeEventName_)
	{
		OBJ_COUNT_EVENT = objEventName_;
		SIZE_COUNT_EVENT = sizeEventName_;
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
}
