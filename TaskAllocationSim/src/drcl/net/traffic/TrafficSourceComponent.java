// @(#)TrafficSourceComponent.java   10/2003
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

package drcl.net.traffic;

import drcl.comp.*;
import drcl.data.*;
import drcl.net.Packet;
import drcl.net.PacketWrapper;
import drcl.net.FooPacket;
import drcl.util.random.*;

/**
Defines the base class for implementing a traffic source.
The output of a traffic source conforms to the associated 
{@link TrafficModel traffic model} instance.

<p>One must provide a random seed to this component via {@link #setSeed(long)}
before starting it.  The component outputs {@link drcl.net.FooPacket}'s.
If timestamping is enabled ({@link #setTimestampEnabled(boolean)}), 
the component outputs {@link TimestampedFooPacket}'s instead which is a sublcass
of <code>FooPacket</code>.

<p>Subclasses must override the {@link #setNextPacket(FooPacket)} method to 
set the size of next outputted packet and return its birth time.
In addition, a subclass should override the {@link #info(String)} to provide 
the content of
this source component.  Buffer operations are taken care of in this class.

@see TrafficModel
 */
public abstract class TrafficSourceComponent extends TrafficComponent 
	implements ActiveComponent
{
	double startTime;
	ACATimer nextPktTimer = null;
	double birthTime; // birth time of nextPkt
	
	/** The seed of the random number generator. */
	protected long seed = 0;
	int pktcount = 0;
	long bytecount = 0;

	TrafficShaper shaper;
	static final long FLAG_SEND_UNSHAPED_ENABLED  = 1L << FLAG_UNDEFINED_START;
	static final long FLAG_TIMESTAMP_ENABLED = 1L << (FLAG_UNDEFINED_START + 1);	
	public TrafficSourceComponent()
	{ super(); }

	public TrafficSourceComponent(String id_)
	{ super(id_); }

	/**
	 * Sets up next packet.
	 * Given the next packet, this method should set its packet size and return
	 * the birth time of the packet.
	 * The birth time is the time relative to the start time of this traffic 
	 * source.
	 * @return <code>Double.NaN</code> if this source has sent out all the 
	 * 		packets.
	 */
	protected abstract double setNextPacket(FooPacket pkt_) ;
	
	public void reset()
	{
		super.reset();
		setSeed(seed); // to initialize subclasses' random number generators
		pktcount = 0;
		bytecount = 0;
		nextPktTimer = null;
		startTime = 0.0;
	}

	public void setSeed(long seed_)
	{ seed = seed_; }

	public long getSeed()
	{ return seed; }
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		TrafficSourceComponent that_ = (TrafficSourceComponent) source_;
		seed = that_.seed;
	}

	/** Prints the content of this source component.
	This class prints out the start time, packet count, byte count and shaper 
	information.
	A subclass only needs to call <code>super.info(prefix_)</code> and print out
	the parameters defined in the subclass.
	@param prefix_ prefix that shoud be prepended at the beginning of each line 
		of the result.
	*/
	public String info(String prefix_)
	{
		return super.info(prefix_)
			+ prefix_ + "startTime = " + startTime + ", seed=" + seed
			+ ", pkt count=" + pktcount 
			+ ", byte count=" + bytecount 
			+ (isStarted()? (isStopped()? ", stopped": ", running"):
							", not_started_yet") + "\n"
			+ (shaper == null? "": prefix_ + "Shaper--" + shaper.info(prefix_));
	}

	
	protected void process(Object data_, drcl.comp.Port inPort_) 
	{
		if (inPort_ == timerPort) {
			if (data_ == null) {
				// do nothing
			}
			else if (data_ == shaper) {
				data_ = shaper.dequeue();
				if (shaper.getBufferLength() > 0)
					forkAt(timerPort, shaper, shaper.nextOutputTime());
				// send packet
				if (enclosingPacket == null)
					downPort.doLastSending(data_);
				else {
					PacketWrapper tmp_ = (PacketWrapper)enclosingPacket.clone();
					tmp_.wraps((Packet)data_);
					downPort.doLastSending((Object)tmp_);
				}
			}
			else {
				FooPacket nextPkt_ = (FooPacket)nextPktTimer.data;
				if (nextPkt_ == null) return; // cancelled
				nextPktTimer = null;
				if (data_ != nextPkt_)
					error(data_, "process()", inPort_, "expect " + nextPkt_
									+ ", but get " + data_);
				else
					send(nextPkt_);
			}
		}
		else error(data_, "process()", inPort_, "unknown data");
	}
	public synchronized void _start()
	{
		//reset();
		_resume();
	}
	
	/** Stops generating packets. */
	public synchronized void _stop()
	{
		double now_ = getTime();
		startTime = now_ - startTime; // record the time duration of running
		if (nextPktTimer != null) {
			cancelFork(nextPktTimer);
			birthTime -= now_; // save the time duration
		}
	}
	
	/** Resumes generating packets. */
	public synchronized void _resume()
	{
		double now_ = getTime();
		FooPacket nextPkt_ = null;
		startTime = now_ - startTime; 
			// restore the start time from current time

		if (nextPktTimer == null) {
			nextPkt_ = new FooPacket();
			birthTime = setNextPacket(nextPkt_);
			if (Double.isNaN(birthTime)) { stop(); return; }

			birthTime += startTime; // timeout duration
			nextPkt_.set(pktcount++, bytecount);
			bytecount += nextPkt_.size;
		}
		else {
			double timeoutDuration_ = birthTime;
			birthTime += now_; // exact timeout time point
			nextPkt_ = (FooPacket)nextPktTimer.data;
		}
		nextPktTimer = forkAt(timerPort, nextPkt_, birthTime);
	}

	synchronized final void send(FooPacket nextPkt_)
	{
		if (nextPkt_ == null) return; // what's going on?
		double now_ = getTime();
		boolean sendNow_ = true;
		if (shaper != null) {
			double deltaT_ = shaper.adjust(nextPkt_, now_);

			if (isDebugEnabled())
				debug("Size " + nextPkt_.size + " will be ready at " 
								+ (now_ + deltaT_));

			if (deltaT_ < 0.0) {
				if (isGarbageEnabled()) {
					if (nextPkt_.size > shaper.getTrafficModel().getMTU()) {
						drop(nextPkt_, 
							"packet is too large to enter the shaper, "
							+ nextPkt_.size + " > MTU (" 
							+ shaper.getTrafficModel().getMTU() + ")");
					}
					else
						drop(nextPkt_, "available_buffer=" 
										+ shaper.getAvailableBufferSize());
				}
				sendNow_ = false;
			}
			else if (deltaT_ > 0.0) {
				if (shaper.getBufferLength() == 1)
					fork(timerPort, shaper, deltaT_);
				sendNow_ = false;
			}
		}
		if (sendNow_) {
			if (enclosingPacket == null)
				downPort.doLastSending(nextPkt_);
			else {
				PacketWrapper tmp_ = (PacketWrapper)enclosingPacket.clone();
				tmp_.wraps(nextPkt_);
				downPort.doLastSending((Object)tmp_);
			}
		}
		if (isSendUnshapedTrafficEnabled()) {
			if (enclosingPacket == null)
				timerPort.doLastSending(nextPkt_);
			else {
				Packet tmp_ = (Packet)enclosingPacket.clone();
				tmp_.setBody(nextPkt_);
				timerPort.doLastSending(tmp_);
			}
		}
		
		// generate next packet: time and size
		if (isStopped()) return;
		if (isTimestampEnabled()) {
			nextPkt_ = new TimestampedFooPacket();
			birthTime = setNextPacket(nextPkt_);
			((TimestampedFooPacket)nextPkt_).timestamp = birthTime + startTime;
		}
		else {
			nextPkt_ = new FooPacket();
			birthTime = setNextPacket(nextPkt_);
		}
		if (Double.isNaN(birthTime)) { stop(); return; }

		birthTime += startTime; // exact timeout time point
		nextPkt_.setPacketCount(pktcount++);
		nextPkt_.setByteCount(bytecount);
		bytecount += nextPkt_.getPacketSize();
		nextPktTimer = forkAt(timerPort, nextPkt_, birthTime);
	}

	/** Adds a {@link TrafficShaper traffic shaper} to the output of this 
	 * source. */
	public void setShaper(TrafficShaper shaper_)
	{
		if (shaper != null && shaper_ != null)
			shaper_.setBufferSize(shaper.getBufferSize());
		shaper = shaper_;
	}

	/** Returns the installed {@link TrafficShaper traffic shaper} 
	 * (if there is any). */
	public TrafficShaper getShaper()
	{ return shaper; }

	/** Sets the (maximum) buffer size of the installed traffic shaper (byte).
	 */
	public void setBufferSize(int size_)
	{
		if (shaper == null)
			shaper = new tsDummy();
		shaper.setBufferSize(size_);
	}

	/** Returns the (maximum) buffer size of this traffic shaper (byte). */
	public int getBufferSize()
	{ return shaper == null? 0: shaper.getBufferSize(); }

	/** Enables/disables outputting of unshaped traffic (through the timer
	 * port). */
	public void setSendUnshapedTrafficEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_SEND_UNSHAPED_ENABLED, enabled_); }
	
	/** Returns true if outputting of unshaped traffic (through the timer
	 * port) is enabled. */
	public boolean isSendUnshapedTrafficEnabled()
	{ return getComponentFlag(FLAG_SEND_UNSHAPED_ENABLED) != 0; }

	/** Enables/disables timestamping generated packets.
	 * If it is enabled, the component outputs {@link TimestampedFooPacket}'s.
	 */
	public void setTimestampEnabled(boolean enabled_)
	{ setComponentFlag(FLAG_TIMESTAMP_ENABLED, enabled_); }

	/** Returns true if timestamping generated packets is enabled. */
	public boolean isTimestampEnabled()
	{ return getComponentFlag(FLAG_TIMESTAMP_ENABLED) != 0; }
}
