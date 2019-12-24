// @(#)Scheduler.java   9/2002
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

package drcl.intserv;

import java.util.*;

import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.util.queue.*;
import drcl.util.queue.Queue;
import drcl.util.scalar.IntSpace;

// TO_DO:
// 1. configuration
// 2. change events

//
// QUICK REFERENCE:
//
// Subclasses must override:
// - qosEnque(Packet, Spec_R)
// - qosDeque()
//
// Subclasses probably need to override:
//  - addFlowspec(Spec_Flow)
//  - setFlowspec(int handle, Spec_Flow)
//  - removeFlowspec(int handle)
// for installing new flows and/or removing flows.
// Caution: if overriding the methods, must call corresponding ones in super class to
// complete the installation/removal.

/**
 * The information unit a scheduler deals with in our architecture is "flow".
 * A flow is defined as the aggregate traffic of individual traffics originated
 * from a set of sources in the same (multicast) group.  
 * Different scheduling algorithms have their own parameters to
 * describe the resource requirement for a flow.
 * The set of parameters is called an "Rspec".  
 * 
 * Packets are classified to three types: QoS_DATA, CONTROL and BEST_EFFORTS_DATA.
 * By default, QoS_DATA has the highest priority and BEST_EFFORTS has the lowest.
 * That is, when NetworkInterface is ready to transmit a packet, the scheduler 
 * looks for available packets from QoS queue(s) first, and the queue of control
 * packets if no QoS packet is available, and then best-efforts packets.
 * However, one can install a special QoS flow for control packets with source and
 * destination set to null address, and/or for best-efforts packets with source set 
 * to any address (and destination set to null address) in order to provide 
 * hard-guaranteed QoS (lower priority packets may degrade the QoS of QoS flows
 * because packet transmission is not preemptible).
 * <br>
 * <b>Scheduler Configuration Contract</b>: Component (e.g. PacketFilter Manager) - Scheduler.<br>
 * 
 * <ul>
 * <li>Ports:
 *	<ul>
 *	<li>shaper port: port to configure the shaper.
 *	<li>config ports: ports other than the shaper port and those defined in {@link drcl.inet.core.Queue}
 *		are treated as config ports; they follow Scheduler Configuration Contract.
 *	</ul>
 * </ul>
 */
public abstract class Scheduler extends drcl.inet.core.Queue implements drcl.comp.Wrapper
{
	/** Name of the (reserved) bandwidth change event port. */
	public static final String BW_CHANGE_EVENT_PORT = ".bw";
	/** Name of the (reserved) buffer change event port. */
	public static final String BUFFER_CHANGE_EVENT_PORT = ".buffer";

	/** Name of the (reserved) bandwidth change event. */
	public static final String BW_CHANGE_EVENT_NAME = "Reserved BW";
	/** Name of the (reserved) buffer change event. */
	public static final String BUFFER_CHANGE_EVENT_NAME = "Reserved Buffers";

	// resv bandwidth change event
	Port bwEventPort = addPort(BW_CHANGE_EVENT_PORT, false);
	// resv buffer change event
	Port bufferEventPort = addPort(BUFFER_CHANGE_EVENT_PORT, false);

	Port configPort = createConfigPort();
	
	public Scheduler()
	{ super(); }
	
	public Scheduler(String id_)
	{ super(id_); }
	
	public void reset()
	{
		super.reset();
		qosResvBuffer = 0;
		qosAlloc = nonqosAlloc = 0;
		qosResvBW = 0;
		if (beQ != null) beQ.reset();
		if (ctlQ != null) ctlQ.reset();
		removeAllFlowspecs();
		if (admission != null) admission.reset();
	}
	
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		Scheduler that_ = (Scheduler)source_;
		mtu = that_.mtu;
		pktLossRate = that_.pktLossRate;
		tosMask = that_.tosMask;
		
		maxBW = that_.maxBW;
		bwRatio = that_.bwRatio;
		qosBW = that_.qosBW;
		qosResvBW = 0;
		
		maxBuffer = that_.maxBuffer;
		bufferRatio = that_.bufferRatio;
		qosBuffer = that_.qosBuffer;
		qosResvBuffer = qosAlloc = nonqosAlloc = 0;
		
		degradedQoS = that_.degradedQoS;
		
		if (that_.admission != null) {
			admission = (Admission)that_.admission.clone();
			admission.scheduler = this; 
		}
	}

	/** Returns the admission object wrapped in this scheduler component. */
	public Object getObject()
	{ return admission; }
	
	//
	void ___PROCESS_PACKET() {}
	//
	
	// Queue for best efforts traffic if no qos flow is installed to handle it
	private transient Queue beQ = new FIFOQueue();
	
	// Queue for control packets if no qos flow is installed to handle it
	private transient Queue ctlQ = new FIFOQueue();
	
	public synchronized Object enqueue(Object o_)
	{
		try {
			InetPacket p_ = (InetPacket)o_;
			long tos_ = p_.getTOS() & tosMask;
			if (isDebugEnabled())
				debug("Masked tos of incoming pkt: " + tos_ + ", pkt: " + p_);
			// first look for reservation for the tos
			SpecFlow f_ = getFlowspec(tos_);
		
			// second, look for reservation based on packet type
			if (f_ == null || f_.rspec == null || !f_.rspec.activated) {
				int tosType_ = (int)tos_ & IntServToS.TYPE_MASK;
				if (isDebugEnabled())
					debug("type(mask:" + IntServToS.TYPE_MASK + ") in tos(" + tos_ + "): " + tosType_);
				f_ = getFlowspec((long)tos_);
			
				// last, resort to default handler for best-efforts and control
				if (f_ == null || f_.rspec == null || !f_.rspec.activated) {
					if (tosType_ == IntServToS.CONTROL)
						doControl(p_); // default handling of control packets
					else if (degradedQoS || tosType_ == IntServToS.BEST_EFFORTS_DATA)
						doBestEffort(p_); // default handling of best-efforts packets
					else if (isGarbageEnabled())
						drop(p_, "no reservation for the " + IntServToS.interpretType(tosType_) + " packet");
					return null;
				}
			}
			
			doQoS(p_, f_.rspec);
		}
		catch (Exception e_) {
			e_.printStackTrace();
			error(o_, "enque()", infoPort, e_);
		}
		return null;
	}
	
	// treat the packet with QoS
	private void doQoS(Packet p_, SpecR rspec_)
	{
		if (isDebugEnabled()) debug("QoS packet");
		if (allocateBuffer(p_.size, IntServToS.QoS_DATA))
			qosEnque(p_, rspec_);
		else if (isGarbageEnabled())
			drop(p_, "buffer overflow");
	}
	
	// treat the packet with best efforts
	private void doBestEffort(Packet p_)
	{
		if (isDebugEnabled()) debug("Best-efforts packet");
		if (allocateBuffer(p_.size, IntServToS.BEST_EFFORTS_DATA))
			beQ.enqueue(p_);
		else if (isGarbageEnabled())
			drop(p_, "buffer overflow");
	}
	
	// control packets
	private void doControl(Packet p_)
	{
		if (isDebugEnabled()) debug("Control packet");
		if (allocateBuffer(p_.size, IntServToS.CONTROL))
			ctlQ.enqueue(p_);
		else if (isGarbageEnabled())
			drop(p_, "buffer overflow");
	}
	
	public synchronized Object dequeue()
	{
		Packet p_ = qosDeque();
		if (p_ != null) {
			freeBuffer(p_.size, IntServToS.QoS_DATA);
		}
		else if (!ctlQ.isEmpty()) {
			// control packets
			p_ = (Packet)ctlQ.dequeue();
			if (p_ != null) freeBuffer(p_.size, IntServToS.CONTROL);
		}
		else if (!beQ.isEmpty()) {
			// best-efforts traffic
			p_ = (Packet)beQ.dequeue();
			if (p_ != null) freeBuffer(p_.size, IntServToS.BEST_EFFORTS_DATA);
		}
		
		if (isDebugEnabled()) debug("dequeue(): isEmpty?" + isEmpty() + ", pkt: " + p_);
		return p_;
	}
	
	// For subclass implementation to drop qos packets;
	// buffer is released for the packet; this PI seems unnecessary.
	//protected void dropQoS(Packet p_, String description_)
	//{
	//	freeBuffer(p_.size, IntServToS.QoS_DATA);
	//	drop(p_, description_);
	//}
	
	/**
	 * Subclasses must override this method to provide its own
	 * enqueuing scheme.
	 */
	protected abstract void qosEnque(Packet p_, SpecR rspec_);
	

	/**
	 * Subclasses must override this method to provide its own
	 * dequeuing scheme.
	 */
	protected abstract Packet qosDeque();

	//
	void ___CONFIGURATION___() {}
	//
	
	protected synchronized void process(Object data_, drcl.comp.Port inPort_)
	{
		if(inPort_ != configPort) {
			super.process(data_, inPort_); 
			return; 
		}

		if (data_ instanceof SpecAd) {
			// advertisement
			if (admission != null)
				inPort_.doLastSending(admission.advertisement((SpecAd) data_));
			else
				inPort_.doLastSending(null); //not capable of updating adspec
			return;
		}

		if (!(data_ instanceof SchedulerConfig.Message)) {
			error(data_, "process()", inPort_, "unrecognized data");
			return;
		}

		SchedulerConfig.Message req_ = (SchedulerConfig.Message)data_;
		
		switch(req_.getType()) {
		case SchedulerConfig.ADD:
		case SchedulerConfig.MODIFY:
			SpecFlow fspec_ = req_.getFlowspec();
			if (admission != null)
				inPort_.doLastSending(admission.setFlowspec(fspec_.getHandle(), req_.getToS(), req_.getToSMask(), fspec_));
			else {
				// install this flowspec unconditionally
				setFlowspec(fspec_.getHandle(), req_.getToS(), req_.getToSMask(), fspec_);
				inPort_.doLastSending(null);
			}
			break;
		case SchedulerConfig.REMOVE:
			if (admission != null)
				inPort_.doLastSending(admission.removeFlowspec(req_.getHandle()));
			else
				inPort_.doLastSending(removeFlowspec(req_.getHandle()));
			break;
		case SchedulerConfig.QUERY:
			int handle_ = req_.getHandle();
			if (handle_ >= 0)
				inPort_.doLastSending(getFlowspec(handle_));
			else
				inPort_.doLastSending(getAllFlowspecs());
			break;
		default:
			error(data_, "process()", inPort_, "unrecognized request");
			inPort_.doLastSending(null);
			break;
		}
	}
		
	/** Returns the buffer size available for reservation. */
	public int getAvailableCapacity()
	{	return (int) (maxBuffer - qosAlloc - nonqosAlloc); }
	
	// XX: packet mode? byte mode?

	public int getCapacity()
	{ return (int) maxBuffer; }
	
	public synchronized void setCapacity(int size_)
	{
		maxBuffer = size_;
		qosBuffer = (int) (size_ * bufferRatio);
	}
	
	public int mtu = Integer.MAX_VALUE;
	
	public void setMTU(int mtu_) { mtu = mtu_; }
	public int getMTU() { return mtu; }
	
	// between 0.0 and 1.0
	public double pktLossRate = 0.0;
	
	public void setPktLossRate(double v_) { pktLossRate = v_; }
	public double getPktLossRate() { return pktLossRate; }
	
	/** The admission module equipped on this scheduler. */
	Admission admission;
	
	public void setAdmission(Admission adm_)
	{ admission = adm_; adm_.scheduler = this; }
	
	public Admission getAdmission()
	{ return admission; }
	
	long tosMask = -1L;
	
	/**
	 * Sets the ToS mask.
	 * The ToS of an incoming packet is masked with this mask and
	 * then the scheduler obtains the reservation for the masked ToS.
	 */
	public void setToSMask(long mask_) { tosMask = mask_; }
	public long getToSMask() { return tosMask; }
	
	boolean degradedQoS = false;
	
	/**
	 * Enables/disables graceful QoS degradation.
	 * If disabled, QoS packets are dropped in absense of an appropriate reservation.
	 * Otherwise, the packets are treated with best efforts.
	 */
	public void setDegradedQoSEnabled(boolean dqos_)
	{degradedQoS = dqos_; }

	public boolean isDegradedQoSEnabled()
	{ return degradedQoS; }
	
	public String info()
	{
		StringBuffer sb_ = new StringBuffer(super.info());
		// bandwidth
		sb_.append("Bandwidth=" + maxBW + ", QoS portion=" + qosBW + "(" 
				   + (bwRatio * 100.0) + "%)" 
				   + ", Resvd=" + qosResvBW + "\n");
		// buffer
		sb_.append("Buffer=" + maxBuffer + ", QoS portion=" + qosBuffer + "(" 
				   + (bufferRatio * 100.0) + "%)" 
				   + ", Resvd=" + qosResvBuffer + "\n");
		sb_.append("Buffer allocation: qos=" + qosAlloc + ", nonqos=" + nonqosAlloc + "\n");
		
		// Reservations
		if (admission == null) 
			sb_.append("No admission\n" + resvinfo());
		else
			sb_.append("Admission: " + admission.getClass() + "\n" + admission.info());
		return sb_.toString();
	}
	
	/** Displays the reservations installed in this scheduler. */
	public String resvinfo()
	{
		if (vFlowspec == null || fsize == 0) return "No flowspec installed.\n";
		StringBuffer sb_ = new StringBuffer();
		int nresv_ = 0;
		if (vTSHandle == null) {
			sb_.append("Handle -> SpecFlow\n");
			for (int i=0; i<vFlowspec.size(); i++) {
				SpecFlow s_ = (SpecFlow)vFlowspec.elementAt(i);
				if (s_ != null) {
					nresv_ ++;
					sb_.append("   " + i + ": " + getToS(s_) + ", " + s_ + "\n");
				}
			}
		}
		else {
			sb_.append("Handle -> SpecFlow, shaper handle\n");
			for (int i=0; i<vFlowspec.size(); i++) {
				SpecFlow s_ = (SpecFlow)vFlowspec.elementAt(i);
				if (s_ != null) {
					nresv_ ++;
					if (i < vTSHandle.size())
						sb_.append("   " + i + ": " + s_ + ", " + vTSHandle.elementAt(i) + "\n");
					else
						sb_.append("   " + i + ": " + s_ + ", no shaper\n");
				}
			}
		}
		if (nresv_ == 0) return "No flowspec installed.\n";
		
		/*
		// ToS -> handle
		if (htFlowspec != null && htFlowspec.size() > 0) {
			sb_.append("ToS -> handle\n");
			for (Enumeration e_ = htFlowspec.keys(); e_.hasMoreElements(); ) {
				Object key_ = e_.nextElement();
				Object s_ = htFlowspec.get(key_);
				if (s_ != null) sb_.append("   " + key_ + ": " + vFlowspec.indexOf(s_) + "\n");
			}
		}
		*/
		
		// ToS, mask -> handle
		if (mapFlowspec != null) {
			MapKey[] keys_ = mapFlowspec.getAllKeys();
			if (keys_ != null && keys_.length > 0) {
				sb_.append("ToS(mask) -> handle\n");
				for (int i=0; i<keys_.length; i++) 
					if (keys_[i] != null) {
						Object tmp_ = mapFlowspec.get(keys_[i], drcl.data.Map.MATCH_EXACT);
						if (tmp_ != null) 
							sb_.append("   " + keys_[i].numberRepresentation() + ": " 
									   + vFlowspec.indexOf(tmp_) + "\n");
					}
			}
		}
		return "Number of reservations: " + nresv_ + "\n" + sb_.toString();
	}
	
	//
	private void ___FLOWSPEC___() {}
	//
	
	// Flowspec is retrieved 
	// (1) when a packet of qos type arrives; using tos
	// (2) when a new flow is established and admission test is performed;
	//     (all flowspecs are retrieved)
	// (3) when a flowspec is replaced (retrieved by a handle)
	
	// flow id (tos) -> flowspec.
	private Hashtable htFlowspec;
	
	// flow id -> flowspec
	// Stores only flowspecs w/ ID masked.
	private drcl.data.Map mapFlowspec;
	
	// Handle -> flowspec.
	Vector vFlowspec;
	
	// Handle -> handle of traffic shaper installed in drcl.inet.filter.Shaper
	private Vector vTSHandle;

	// Available handles to be assigned to new flowspecs.
	private IntSpace isHandle = new IntSpace();
	
	private transient SpecR[]    rArray = new SpecR[0]; // compact list of flows
	private transient SpecFlow[] fArray = new SpecFlow[0]; // compact list of reservations
	transient int fsize = 0; // size of fArray/rArray

	/**
	 * Adds a flowspec.
	 * Subclasses do not need to override this method.
	 * @return a handle for this reservation.
	 */
	public final int addFlowspec(long tos_, SpecFlow fspec_)
	{ return setFlowspec(-1, new long[]{tos_}, null, fspec_); }
	
	/**
	 * Adds a flowspec.
	 * Subclasses do not need to override this method.
	 * @return a handle for this reservation.
	 */
	public final int addFlowspec(long tos_, long tosmask_, SpecFlow fspec_)
	{ return setFlowspec(-1, new long[]{tos_}, new long[]{tosmask_}, fspec_); }
	
	/**
	 * Adds a flowspec.
	 * Subclasses do not need to override this method.
	 * @return a handle for this reservation.
	 */
	public final int addFlowspec(long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{ return setFlowspec(-1, tos_, tosmask_, fspec_); }
	
	/**
	 * Adds/modifies the flowspec with the specified handle.
	 * If no flowspec exists for the handle, the flowspec
	 * is added.  Otherwise, the flowspec replaces the original one.
	 * No admission control is performed in this method.
	 * A subclass may need to override this method to create its own
	 * data structure for the successfully installed flow.
	 * @return the handle.
	 */
	public synchronized int setFlowspec(int handle_, long[] tos_, long[] tosmask_, SpecFlow fspec_)
	{
		if (fspec_ == null || fspec_.rspec == null
			|| (handle_ < 0 && (tos_ == null || tos_.length == 0))) return -1;
		
		fArray = null; // constructed on demand, see getAllFlows()

		// put in vFlowspec
		if (vFlowspec == null) vFlowspec = new Vector(3, 3);
		if (handle_ < 0) handle_ = isHandle.checkout();
		
		SpecFlow oldfspec_ = vFlowspec.size() <= handle_? 
							 null: (SpecFlow) vFlowspec.elementAt(handle_);
		
		if (oldfspec_ != null) {
			if (oldfspec_ == fspec_) return handle_;
		}
		else fsize ++;
		
		// put in htFlowspec
		// Note that tos must be unique even it is a masked value.
		if (htFlowspec == null) htFlowspec = new Hashtable(3);
		if (tos_ != null)
			for (int i=0; i<tos_.length; i++) {
				htFlowspec.put(new LongObj(tos_[i]), fspec_);
				if (tosmask_ != null && tosmask_[i] != -1) {
					// add to map
					if (mapFlowspec == null) mapFlowspec = new drcl.data.Map();
					MapKey mapkey_ = new MapKey(tosmask_[i], tos_[i]);
					mapFlowspec.addEntry(mapkey_, fspec_);
				}
			}
		// else is replacement, do nothing
		
		// put in vFlowspec
		fspec_.handle = fspec_.rspec.handle = handle_;
		if (handle_ >= vFlowspec.size())
			vFlowspec.setSize(handle_ + 1);
		vFlowspec.setElementAt(fspec_, handle_);
		//stateReport();

		return handle_;
	}

	/**
	 * Removes a flowspec.
	 * A subclass may need to override this method to remove its own
	 * data structure for the installed flow.
	 */
	public synchronized SpecFlow removeFlowspec(int handle_)
	{
		if (vFlowspec == null) return null;
		
		if (handle_ >= vFlowspec.size() && handle_ < 0) return null;
		SpecFlow fspec_ = (SpecFlow) vFlowspec.elementAt(handle_);
		if (fspec_ == null) return null;
		vFlowspec.setElementAt(null, handle_);
		isHandle.checkin(handle_);

		// remove flowspec from hashtable
		if (htFlowspec != null) {
			Enumeration keys_ = htFlowspec.keys();
			for (Enumeration e_ = htFlowspec.elements(); e_.hasMoreElements(); ) {
				Object key_ = keys_.nextElement();
				Object tmp_ = e_.nextElement();
				if (tmp_ == fspec_) htFlowspec.remove(key_);
			}
		}
		
		if (mapFlowspec != null) mapFlowspec.removeEntry(fspec_);// remove entries in map
		fArray = null; // reconstructed on demand, see getAllFlows()
		fsize --;
	
		return fspec_;
	}

	/** Returns the reservation specified by the handle. */
	public final synchronized SpecFlow getFlowspec(int handle_)
	{
		if (vFlowspec == null) return null;
		
		if (handle_ >= vFlowspec.size() || handle_ < 0) return null;
		else
			return (SpecFlow) vFlowspec.elementAt(handle_);
	}

	/** Returns the all reservations installed in this link. */
	public final synchronized SpecFlow[] getAllFlowspecs()
	{
		if (fArray == null) createResvArrays();

		return fArray;
	}

	// returns ToS of the flow with handle_
	// returns Long.MIN_VALUE if N/A
	long getToS(SpecFlow s_)
	{
		if (htFlowspec != null && htFlowspec.size() > 0) {
			for (Enumeration e_ = htFlowspec.keys(); e_.hasMoreElements(); ) {
				Object key_ = e_.nextElement();
				if (s_ == htFlowspec.get(key_)) return ((LongObj)key_).value;
			}
		}
		return Long.MIN_VALUE;
	}

	/** Returns the flow specified by the handle. */
	public final synchronized SpecR getRspec(int handle_)
	{
		if (vFlowspec == null) return null;
		
		if (handle_ >= vFlowspec.size() || handle_ < 0) return null;
		else {
			SpecFlow tmp_ = (SpecFlow) vFlowspec.elementAt(handle_);
			return tmp_ == null? null: tmp_.rspec;
		}
	}

	/** Returns all the Rspecs installed. */
	public final synchronized SpecR[] getAllRspecs()
	{
		if (fArray == null) createResvArrays();

		return rArray;
	}

	private void createResvArrays()
	{
		if (vFlowspec == null) return;
		
		fArray = new SpecFlow[fsize];
		rArray = new SpecR[fsize];
		int i=0;
		for (int j=0; j<vFlowspec.size(); j++) {
			Object o_ = vFlowspec.elementAt(j);
			if (o_ != null) {
				fArray[i] = (SpecFlow) o_;
				rArray[i++] = ((SpecFlow)o_).rspec;
			}
		}
		if (i != fsize)
			error("createResvArrays()", "Inconsistency in vFlowspec and fsize (" + i + " != " + fsize + ")");
	}

	//private MapKey reuseKey = new MapKey(); // avoid frequent mem allocation 
	
	/** Returns the rspec of the flow that best matches the ToS value. */
	public final SpecR getRspec(long tos_)
	{
		if (vFlowspec == null) return null;
		
		LongObj key_ = new LongObj(tos_);
		SpecFlow f_ = (SpecFlow)htFlowspec.get(key_);
		if (f_ == null && mapFlowspec != null) {
			// search in map
			drcl.data.BitSet bs_ = new drcl.data.BitSet(64, tos_);
			f_ = (SpecFlow)mapFlowspec.get(bs_, drcl.data.Map.MATCH_LONGEST);
		}
		return f_.rspec;
	}

	/** Returns the the flow that best matches the ToS value. */
	public final SpecFlow getFlowspec(long tos_)
	{
		if (vFlowspec == null) return null;
		
		LongObj key_ = new LongObj(tos_);
		SpecFlow f_ = (SpecFlow)htFlowspec.get(key_);
		if (f_ == null && mapFlowspec != null) {
			// search in map
			drcl.data.BitSet bs_ = new drcl.data.BitSet(64, tos_);
			f_ = (SpecFlow)mapFlowspec.get(bs_, drcl.data.Map.MATCH_LONGEST);
		}
		return f_;
	}

	public void removeAllFlowspecs()
	{
		if (htFlowspec != null) htFlowspec.clear();
		if (vFlowspec != null) vFlowspec.removeAllElements();
		if (vTSHandle != null) vTSHandle.removeAllElements();
		if (mapFlowspec != null) mapFlowspec.reset();
		if (isHandle != null) isHandle.reset();
		rArray = null;
		fArray = null;
		fsize = 0;
	}
	
	// XXX:
	/**
	 * Adds a shaper in front of this to complete the reservation.  
	 * Not all schedulers need to do this.
	 * @param handle_ is the one for the reservation.
	 */
	public void addShaper(int handle_, long[] tos_, long[] tosmask_, 
						  drcl.net.traffic.TrafficShaper shaper_)
	{
		/*
		if (shaperPort == null) return;
		try {
			IntObj hobj_ = (IntObj)shaperPort.sendReceive(Struct.create(tos_, tosmask_, shaper_));
			if (hobj_ != null) {
				int tsHandle_ = hobj_.value;
				if (vTSHandle == null) vTSHandle = new Vector(2, 3);
				if (vTSHandle.size() <= tsHandle_) vTSHandle.setSize(tsHandle_ + 1);
				vTSHandle.setElementAt(hobj_, tsHandle_);
			}
		}
		catch (Exception e_) {	}
		*/
	}
	
	/**
	 * Removes the shaper in a reservation.
	 * Not all schedulers need to do this.
	 * @param handle_ is the one for the reservation.
	 */
	public void removeShaper(int handle_)
	{
	/*
		if (shaperPort == null) return;
		if (vTSHandle == null || handle_ < 0 || handle_ >= vTSHandle.size()) return;
		IntObj hobj_ = (IntObj) vTSHandle.elementAt(handle_);
		vTSHandle.setElementAt(null, handle_);
		if (hobj_ != null) shaperPort.doSending(hobj_);
		*/
	}

	//
	void ___BW_ALLOC___() {}
	//
	
	protected int maxBW;
	protected double bwRatio = 0.9; // qos/all (qos + control & b.e.)
	protected int qosBW;     // qos portion of bandwidth
	protected transient int qosResvBW; // reserved portion of qosBW
	
	/** Allocates bandwidth (for a certain flow). */
	public final synchronized boolean resvBW(int amount_)
	{
		if (qosResvBW + amount_ > qosBW) return false;
		else 
			qosResvBW += amount_;
		//XXX: bw change
		//stateReport();
		return true;
	}

	/** Releases bandwidth allocation (from a certain flow). */
	public final synchronized void releaseBW(int amount_)
	{
		int old_ = qosResvBW;
		qosResvBW -= amount_;
		if (Math.abs(qosResvBW - qosBW) < 1e-6) // consider rounding error
			qosResvBW = qosBW;
		else if (qosResvBW - qosBW > 1e-6)	{ // consider rounding error
			error("releaseBW()", "BW: freed bw > max qos bw (" + qosResvBW + ">" + qosBW + ")");
			qosResvBW = qosBW;
		}
		//XXX: bw change
		//stateReport();
	}

	/** Acquires available bandwidth. */
	public int getAvailableBW()
	{ return qosBW - qosResvBW; }

	/** Returns the bandwidth of the interface. */
	public int getBW()
	{ return maxBW; }

	/** Sets the bandwidth of the interface. */
	public void setBW(int cap_) 
	{ 
		maxBW = cap_; 
		qosBW = (int)(maxBW * bwRatio);
		//stateReport();
	}
	
	public double getBWRatio()
	{ return bwRatio; }
	
	public synchronized void setBWRatio(double ratio_)
	{
		qosBW = (int)(maxBW * ratio_);
		bwRatio = ratio_;
		//stateReport();
	}

	public int getCurrentLoad()
	{	return qosResvBW;	}

	public double getCurrentLoadByPercentage()
	{	return (double)qosResvBW / maxBW;	}

	void bwChange(int old_)
	{
		bwEventPort.exportEvent(BW_CHANGE_EVENT_NAME, new int[]{old_, qosResvBW}, null);
	}

	//
	void ___BUFFER_ALLOC___() {}
	//
	
	// Buffer
	// Note: although qos buffer can be reserved, 
	// buffer is allocated to any packet as long as buffer is sufficient
	// When buffer overflows, qos packets may preempt nonqos packets.
	protected int maxBuffer;
	protected double bufferRatio = 0.5; // qos/all (qos + control & b.e.)
	protected int qosBuffer;     // qos portion of buffer
	protected transient int qosResvBuffer; // reserved portion of qosBuffer
	protected transient int qosAlloc = 0;    // buffer allocated for qos packets
	protected transient int nonqosAlloc = 0; // buffer that has allocated for non-qos packets

	// allocate buffer for packet type: QoS, CONTROL or BEST_EFFORTS
	// returns true if succeeds
	public synchronized boolean allocateBuffer(int amount_, int type_)
	{
		if (amount_ + qosAlloc + nonqosAlloc <= maxBuffer) {
			if (type_ == IntServToS.QoS_DATA)
				qosAlloc += amount_;
			else
				nonqosAlloc += amount_;
			// XXX: available buffer change
			//stateReport();
			return true;
		}
		else if (type_ == IntServToS.QoS_DATA) 
		{
			if (amount_ + qosAlloc > qosBuffer) return false;
			
			// preempt nonqos packets
			int available_ = maxBuffer - qosAlloc - nonqosAlloc;
			while (available_ < amount_) {
				Queue q_ = beQ.isEmpty()? ctlQ: beQ;
				Packet p_ = (Packet)q_.remove(q_.getLength() - 1);
				int size_ = p_.size;
				nonqosAlloc -= size_;
				available_ += size_;
				if (isGarbageEnabled()) drop(p_, "being preempted");
			}
			qosAlloc += amount_;
			//XXX: available buffer change
			//stateReport();
			return true;
		}
		else return false;
	}

	/** Frees buffer from a departing packet. */
	public synchronized void freeBuffer(int amount_, int type_)
	{
		if (type_ == IntServToS.QoS_DATA)
			qosAlloc -= amount_;
		else
			nonqosAlloc -= amount_;
		// XXX: available buffer change
		//stateReport();
	}

	/** Allocates buffer for a QoS flow. */
	public synchronized boolean resvQoSBuffer(int amount_)
	{
		if (qosResvBuffer + amount_ > qosBuffer) return false;
		else 
			qosResvBuffer += amount_;
		//XXX: resv buffer change
		//stateReport();
		return true;
	}

	/** Frees buffer from a QoS flow. */
	public synchronized void releaseQoSBuffer(int amount_)
	{
		qosResvBuffer -= amount_;
		if (Math.abs(qosResvBuffer - qosBuffer) < 1e-8) // consider rounding error
			qosResvBuffer = qosBuffer;
		else if (qosResvBuffer > qosBuffer)
		{
			error("releaseQoSBuffer()", " QoS buffer: freed buffer size > max buffer size ("
				+ qosResvBuffer + ">" + qosBuffer + ")");
			qosResvBuffer = qosBuffer;
		}
		// XXX: resv buffer change
		//stateReport();
	}
	
	/** Returns the remaining buffer for QoS flow reservation. */
	public int getAvailableQoSBuffer()
	{ return qosBuffer - qosResvBuffer; }
	
	/** Returns the remaining buffer for packet allocation. */
	public int getAvailableBuffer()
	{ return maxBuffer - qosAlloc - nonqosAlloc; }
	
	/** Returns the buffer ratio (%QoS portion). */
	public double getBufferRatio()
	{ return bufferRatio; }
	
	/** Sets the buffer ratio (%QoS portion). */
	public synchronized void setBufferRatio(double ratio_)
	{
		qosBuffer = (int)(maxBuffer * ratio_);
		bufferRatio = ratio_;
		//stateReport();
	}

	// listening options
	public static final int
		AVAILABLE_ALLOCATED_QoS = 1,
		AVAILABLE_ACTUAL_QoS    = 2,
		AVAILABLE_BE            = 4;

	Hashtable bufferListeners; // keyed by MyInteger(option), element is a Vector of listeners

	IntObj bReuseKey = new IntObj();

	// XXX: should export events
	// Notify buffer change listeners.
	void bufferChange(int whichBuffer)
	{
	}
	
	// should obtain this from ifquery port
	public double getPropDelay() { return 0.0; }

	// superclass abstract methods
	public int getSize()
	{ return getCapacity() - getAvailableBuffer(); }

	public boolean isEmpty()
	{ return getAvailableBuffer() == getCapacity(); }

	public boolean isFull()
	{ return getAvailableBuffer() == 0; }

	/**
	 * Configures the resources (bandwidth and buffer) governed by this scheduler.
	 * @param bwRatio_ the percentage of bandwidth allocated to QoS flows.
	 * @param bufferRatio_ the percentage of buffer allocated to QoS flows.
	 */
	public void configure(int bw_, double bwRatio_, int buffer_, double bufferRatio_)
	{
		if (bw_ > 0) maxBW = bw_; 
		if (bwRatio_ >= 0.0 && bwRatio_ <= 1.0) bwRatio = bwRatio_;
		if (buffer_ > 0) maxBuffer = buffer_;
		if (bufferRatio_ >= 0.0 && bufferRatio_ <= 1.0) bufferRatio = bufferRatio_;
		qosBW = (int)(maxBW * bwRatio);
		qosBuffer = (int)(maxBuffer * bufferRatio);
	}
}
