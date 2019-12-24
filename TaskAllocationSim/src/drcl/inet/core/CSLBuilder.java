// @(#)CSLBuilder.java   2/2004
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

package drcl.inet.core;

import java.util.*;
import drcl.comp.*;
import drcl.comp.queue.ActiveQueueContract;
import drcl.inet.Protocol;
import drcl.inet.contract.*;
import drcl.inet.data.*;
import drcl.net.Module;
import drcl.comp.queue.*;

// Contraints:
// - Hello, PF, Queue, NI cannot be extension
// Possible improvement of performance:
// - explore "structural copy"

/**
 * The container class for constructing a core service layer.
 */
public class CSLBuilder extends drcl.inet.CSLBuilder implements InetCoreConstants
{
	// Components in CSL:
	// pd, id: necessary
	// rt: needed when more than two down ports or one of routing service
	// 		ports exists
	// hello: needed when the IfQuery service port exists
	// pfs: needed when ConfigSwitch service port exists or one of the pf's
	// 		config port exists
	// 
	// Users need to specify PF's, (Active)Queue and NI
	
	public static final String ID_IDENTITY = "id";
	public static final String ID_PKT_DISPATCHER = "pd";
	public static final String ID_RT = "rt";
	public static final String ID_HELLO = "hello";
	public static final String ID_PKT_FILTER = "pf";
	public static final String ID_PKT_FILTER_SWITCH = "pfs";
	public static final String ID_QUEUE = "q";
	public static final String ID_NI = "ni";
	public static final String ID_IGMP = "igmp";
	
	static Component DEFAULT_QUEUE =new drcl.inet.core.queue.DropTail(ID_QUEUE);
	static Component DEFAULT_NI = new drcl.inet.core.ni.PointopointNI(ID_NI);
	static drcl.inet.core.ni.DropTailPointopointNI DEFAULT_NI2 =
		new drcl.inet.core.ni.DropTailPointopointNI(ID_NI);

	Component rt = null;
	Component id = null;
	
	public CSLBuilder()
	{ super(); }

	public CSLBuilder(String id_)
	{ super(id_); }
	
	public String info()
	{
		return super.info();
	}

	Component[] _getBank(Component target_, String bankid_)
	{
		Vector v_ = new Vector();
		Component[] cc_ = target_.getAllComponents();
		for (int i=0; i<cc_.length; i++) {
			Component c_ = cc_[i];
			if (!c_.getID().startsWith(ID_PKT_FILTER + bankid_ + "_")) continue;
			v_.addElement(c_);
		}
		cc_ = new Component[v_.size()];
		v_.copyInto(cc_);
		return cc_;
	}

	/**
	 * Extracts the structure of packet filters, queue, NI and extensions.
	 * Only works for a regular structure.
	 */
	public synchronized void extract(drcl.inet.CoreServiceLayer source_)
	{ extract(source_, "0"); }

	/**
	 * Extracts the structure of packet filters, queue, NI and extensions.
	 * Only works for a regular structure.
	 */
	public synchronized void extract(drcl.inet.CoreServiceLayer that_,
					String bankid_)
	{
		Component[] cc_ = _getBank(that_, bankid_);
		for (int i=0; i<cc_.length; i++) {
			Component c_ = (Component)cc_[i].clone();
			try {
				String id_ = c_.getID();
				String filterid_ =
						id_.substring((ID_PKT_FILTER + bankid_ + "_").length());
				c_.setID(ID_PKT_FILTER + filterid_);
				addComponent(c_);
			}
			catch (Exception e_) {
				drcl.Debug.error(this, "extract(): pkt filter " + c_
								+ " from target " + that_ + ", " + e_, false);
			}
		}
		Component q_ = that_.getComponent(ID_QUEUE + bankid_);
		if (q_ != null) {
			q_ = (Component)q_.clone();
			q_.setID(ID_QUEUE);
			addComponent(q_);
		}
		Component ni_ = that_.getComponent(ID_NI + bankid_);
		if (ni_ != null) {
			ni_ = (Component)ni_.clone();
			ni_.setID(ID_NI);
			addComponent(ni_);
		}
		
		/*
		for (int i=0; ; i++) {
			String id_ = ID_EXTENSION + i;
			Component ext_ = that_.getComponent(id_);
			if (ext_ == null) break;
			ext_ = (Component)ext_.clone();
			ext_.setID(id_);
			addComponent(ext_);
		}
		*/
	}
	
	public drcl.inet.CoreServiceLayer createCSL()
	{ return new CoreServiceLayer(drcl.inet.Node.ID_CSL); }

	public synchronized void build(Object[] cc_)
	{
		try {
		boolean needRT_ = false;
	   		// true if an extension needs to be connected to RT
		
		rt = getComponent(ID_RT);
		id = getComponent(ID_IDENTITY);

		Component q_ = getComponent(ID_QUEUE);
		//if (q_ == null) q_ = DEFAULT_QUEUE;
		
		Component ni_ = getComponent(ID_NI);
		Component ni2_ = ni_;
		if (ni_ == null) {
			if (q_ == null)
				ni_ = DEFAULT_NI2;
			else
				ni_ = DEFAULT_NI;
			ni2_ = DEFAULT_NI;
		}
		else if (q_ == null)
			q_ = DEFAULT_QUEUE;

		if (ni_ instanceof NI) {
			if (bw > 0.0) ((NI)ni_).setBandwidth(bw);
			if (mtu > 0) ((NI)ni_).setMTU(mtu);
			((NI)ni_).setLinkEmulationEnabled(linkEmu);
			if (linkEmu)
				((NI)ni_).setPropDelay(linkPropDelay);
			// set queue properties
			if (ni_ == DEFAULT_NI2) {
				if (bs > 0) DEFAULT_NI2.setCapacity(bs);
				DEFAULT_NI2.setMode(bufferMode);
			}
		}

		if (q_ instanceof Queue) {
			if (bs > 0) ((Queue)q_).setCapacity(bs);
			((Queue)q_).setMode(bufferMode);
		}

		// get the filter bank
		Component[] pf_ = getAllComponents();
		Vector v_ = new Vector(), vExt_ = new Vector(); // PF's
		for (int i=0; i<pf_.length; i++) {
			Component c_ = pf_[i];
			if (c_ instanceof PktFilter) v_.addElement(c_);
			//else if (c_ instanceof NI); // just leave it alone
			//else if (c_ instanceof Queue); // just leave it alone
			//else if (c_.getID().startsWith(ID_EXTENSION)) {
			//	vExt_.addElement(c_);
			//	if (c_.getPort(SERVICE_RT_PORT_ID) != null
			//		|| c_.getPort(SERVICE_RT_PORT_ID) != null)
			//		needRT_ = true;
			//}
			// ignore other components
		}
		Component[] ext_ = new Component[vExt_.size()];
		vExt_.copyInto(ext_);
		vExt_ = null;
		//System.out.println(this + " builds with " + pf_.length + " pf's");
		
		// Analyze the packet filter structure
		Vector vpfs_ = new Vector();
			// for specific interface, element is a Vector storing pfs for
			// that interface
		Vector vpf_ = new Vector(); // for general structure
		for (int i=0; i<v_.size(); i++) {
			Component c_ = (Component)v_.elementAt(i);
			String sfid_ = c_.getID();
			int index_ = sfid_.indexOf("_");
			try {
				if (index_ < 0) { // for general structure
					int fid_ = Integer.parseInt(sfid_.substring(
											ID_PKT_FILTER.length()));
					if (fid_ >= vpf_.size()) vpf_.setSize(fid_ + 1);
					vpf_.setElementAt(c_, fid_);
				} else { // specific pf at specific interface
					int fid_ = Integer.parseInt(sfid_.substring(index_ + 1));
					int bankid_ = Integer.parseInt(
							sfid_.substring(ID_PKT_FILTER.length(), index_));
					if (bankid_ >= vpfs_.size()) vpfs_.setSize(bankid_ + 1);
					Vector vtmp_ = (Vector)vpfs_.elementAt(bankid_);
					if (vtmp_ == null) {
						vtmp_ = new Vector();
						vpfs_.setElementAt(vtmp_, bankid_);
					}
					if (fid_ >= vtmp_.size()) vtmp_.setSize(fid_ + 1);
					vtmp_.setElementAt(c_, fid_);
				}
			}
			catch (Exception e_) {
				e_.printStackTrace();
				drcl.Debug.error("Ignore processing " + c_ + " due to " + e_);
			} // ignore
		}
		pf_ = new Component[vpf_.size()];
		vpf_.copyInto(pf_);
		// construct spfs_ from vpfs_ and pf_
		Component[][] spfs_ = new Component[vpfs_.size()][];
		for (int i=0; i<spfs_.length; i++) {
			Vector vtmp_ = (Vector)vpfs_.elementAt(i);
			if (vtmp_ == null) continue;
			int len_ = Math.max(pf_.length, vtmp_.size());
			Component[] tmp_ = new Component[len_];
			vtmp_.copyInto(tmp_);
			for (int j=0; j<pf_.length; j++)
				if (pf_[j] != null && tmp_[j] == null) tmp_[j] = pf_[j];
			spfs_[i] = tmp_;
		}
		vpf_ = vpfs_ = null;
		
		for (int i=0; i<cc_.length; i++) {
			if (!(cc_[i] instanceof Component)) continue;
			Component target_ = (Component)cc_[i];
			//System.out.println("   Building " + target_);
				//System.out.print(i + "(");
				//Port[] pp_ = target_.getAllPorts(Module.PortGroup_DOWN);
				//if (pp_ == null || pp_.length < 2)
				//System.out.print((pp_ == null? 0:pp_.length) + ")   ");
			
			//if (isDebugEnabled() && (i+1)%1000 == 0)
			//	debug("Making " + i + " out of " + cc_.length);
			
			try {
				// id, pd
				Component id_ = addID(target_);
				Component pd_ = addPD(target_, id_);
	
				// rt
				Port rtServicePort_ = target_.getPort(SERVICE_RT_PORT_ID);
				Port urtEventPort_ = target_.getPort(
								EVENT_RT_UCAST_CHANGED_PORT_ID);
				Port mrtEventPort_ = target_.getPort(
								EVENT_RT_MCAST_CHANGED_PORT_ID);
				Port[] pp_ = target_.getAllPorts(Module.PortGroup_DOWN);
				//System.out.print(i + "(" + pp_.length + ")   ");
				Component rt_ = null;
				if (needRT_
					|| containsComponent(ID_RT)
					|| rtServicePort_ != null
				    || urtEventPort_ != null
					|| mrtEventPort_ != null
					|| pp_ != null && pp_.length > 1) {
					rt_ = addRT(target_, pd_, rtServicePort_, urtEventPort_,
									mrtEventPort_);
				}
				
				// hello
				if (containsComponent(ID_HELLO)
				    || target_.getPort(SERVICE_IF_PORT_ID) != null
					|| target_.getPort(EVENT_IF_PORT_ID) != null
					|| target_.getPort(EVENT_VIF_PORT_ID) != null)
					addHello(target_, id_, pd_);
			
				// igmp
				if (containsComponent(ID_IGMP)
				    || target_.getPort(SERVICE_MCAST_PORT_ID) != null
					|| target_.getPort(EVENT_MCAST_HOST_PORT_ID) != null)
					addIGMP(target_, id_, pd_);
			
				// extension
				if (ext_ != null && ext_.length > 0)
					addExtensions(ext_, target_, id_, pd_, rt_);
			
				// pfs, will also check later when adding pf's
				Component pfs_ = null;
				if (containsComponent(ID_PKT_FILTER_SWITCH)
				    || target_.getPort(SERVICE_CONFIGSW_PORT_ID) != null)
					pfs_ = addPFS(target_);
			
				if (pp_ == null) continue;
				for (int k=0; k<pp_.length; k++) {
					Port p_ = pp_[k];
					//System.out.println("      build interface " + k);
					if (p_.anyClient()) continue;
					//p_.disconnectClients();
					String sbankid_ = p_.getID();
					int bankid_ = Integer.parseInt(sbankid_);
					Component last_ = null, lastup_ = null;
					Port prevOutDown_ = null, prevInDown_ = null;
					prevOutDown_ = prevInDown_ =
							pd_.addPort(Module.PortGroup_DOWN, sbankid_);

					// add filter bank
					Component[] bank_ = pf_;
					if (bankid_ < spfs_.length && spfs_[bankid_] != null)
						bank_ = spfs_[bankid_];
					for (int j=0; j<bank_.length; j++) {
						if (bank_[j] == null) continue;
						String cid_ = ID_PKT_FILTER + sbankid_ + "_" + j;
						Component c_ = target_.getComponent(cid_);
						try {
							if (c_ == null) {
								Component tmpf_ = getComponent(cid_);
								c_ = (Component)(tmpf_ == null?
											bank_[j].clone(): tmpf_.clone());
								c_.setID(ID_PKT_FILTER + sbankid_ + "_" + j);
								target_.addComponent(c_);
								commonAdd(target_, id_, c_);
							
								// config port
								if (pfs_ != null) {
									Port configPort_ = c_.getPort(
													PktFilter.CONFIG_PORT_ID);
									if (configPort_ != null)
										pfs_.addPort(
											sbankid_,
											String.valueOf(j)).connect(
												configPort_);
								}
							}

							Port pdown_ = c_.getPort(Module.PortGroup_DOWN);
							Port pup_ = c_.getPort(Module.PortGroup_UP);
					
							// previous down -> up
							if (prevOutDown_ != null
								&& pup_.getType() != Port.PortType_OUT)
								prevOutDown_.connectTo(pup_);
							if (prevInDown_ != null
								&& pup_.getType() != Port.PortType_IN)
								pup_.connectTo(prevInDown_);
							if (pdown_.getType() != Port.PortType_IN)
								prevOutDown_ = pdown_;
							if (pdown_.getType() != Port.PortType_OUT)
								prevInDown_ = pdown_;
						}
						catch (Exception e_) {
							drcl.Debug.error(this, "build(): at " + target_
									+ ", building bank " + bankid_
									+ ", connecting " + c_ + ", " + e_, false);
						}
					} // end j, loop on bank_

					// add queue and ni
					Component tmpq_ = getComponent(ID_QUEUE + sbankid_);
					if (q_ != null || tmpq_ != null) {
						try {
							Component c_ = target_.getComponent(
											ID_QUEUE + sbankid_);
							if (c_ == null) {
								c_ = (Component)(tmpq_ == null?
												q_.clone(): tmpq_.clone());
								c_.setID(ID_QUEUE + sbankid_);
								target_.addComponent(c_);
								commonAdd(target_, id_, c_);
							}
							tmpq_ = c_;
								// used to connect to ni in the following codes
							Port pdown_ = c_.getPort(
											ActiveQueueContract.OUTPUT_PORT_ID);
							Port pup_ = c_.getPort(Module.PortGroup_UP);
						
							// previous down -> up
							if (prevOutDown_ != null
								&& pup_.getType() != Port.PortType_OUT)
								prevOutDown_.connectTo(pup_);
							prevOutDown_ = pdown_;
						}
						catch (Exception e_) {
							drcl.Debug.error(this, "build(): at " + target_
								+ ", building bank " + bankid_
								+ ", connecting " + tmpq_ + ", " + e_, false);
						}
					}
					Component tmpni_ = getComponent(ID_NI + sbankid_);
					if (ni_ != null || tmpni_ != null) {
						try {
							Component c_ = target_.getComponent(
											ID_NI + sbankid_);
							if (c_ == null) {
								if (tmpni_ != null)
									c_ = (Component)tmpni_.clone();
								else 
									// if queue is not present,
									// use NI_QUEUE combo component
									c_ = (Component)(tmpq_ == null?
													ni_.clone(): ni2_.clone());
								c_.setID(ID_NI + sbankid_);
								target_.addComponent(c_);
								commonAdd(target_, id_, c_);
							}
							// MTU/fragmentation
							if (c_ instanceof NI
								&& ((NI)c_).getMTU() < DEFAULT_MTU) {
								int tmp_ = ((NI)c_).getMTU();
								if (tmp_ >= 0 || pd_ instanceof PktDispatcher) {
									((PktDispatcher)pd_).setFragmentEnabled(
																		true);
									((PktDispatcher)pd_)._setMTU(bankid_, tmp_);
								}
							}
							tmpni_ = c_;
								// used to connect to target_'s shadow down port
							Port pdown_ = c_.getPort(Module.PortGroup_DOWN);
							Port pup_ = c_.getPort(Module.PortGroup_UP);
						
							// previous down (from q) -> up
							//if (tmpq_ != null)
								prevOutDown_.connect(c_.getPort(PULL_PORT_ID));
							if (prevInDown_ != null
								&& pup_ != null
								&& pup_.getType() != Port.PortType_IN)
								pup_.connectTo(prevInDown_);
							prevOutDown_ = pdown_;
							if (pdown_.getType() != Port.PortType_OUT)
								prevInDown_ = pdown_;
						}
						catch (Exception e_) {
							drcl.Debug.error(this, "build(): at " + target_
								+ ", building bank " + bankid_
								+ ", connecting " + tmpni_ + ", " + e_, false);
						}
					}
					// p_ is target_'s shadow down port
					if (tmpni_ != null) prevOutDown_.connectTo(p_);
					if (prevInDown_ != null) p_.connectTo(prevInDown_);
				} // end for(k), loop on pp_
				
			}
			catch (Exception e_) {
				e_.printStackTrace();
				drcl.Debug.error(this, "build(): at " + target_ + ", " + e_,
								false);
			}
		} // end i, loop on cc_
		
		}
		catch (Exception e_) {
			e_.printStackTrace();
		}
	}
	
	
	// returns filter id from "pf<bank id>_<filter id>"
	static String getFilterID(String id_)
	{
		int index_ = id_.indexOf("_");
		if (index_ < 0) return null;
		return id_.substring(index_+1);
	}

	Component addID(Component target_)
	{
		Component id_ = target_.getComponent(ID_IDENTITY);
		if (id_ == null) {
			if (id == null)
				id_ = new Identity(ID_IDENTITY);
			else
				id_ = (Component)id.clone();
			target_.addComponent(id_);
		}
		//System.out.println("add " + id_);
		Port p_ = target_.addPort(SERVICE_ID_PORT_ID);
		//System.out.println("connect " + p_.info());
	   	//Port p2_ = id_.getPort(SERVICE_ID_PORT_ID);
		//if (p2_ != null) System.out.println("connect " + p2_.info());
		if (p_ != null) id_.addPort(SERVICE_ID_PORT_ID).connect(p_);
		p_ = target_.getPort(EVENT_ID_CHANGED_PORT_ID);
		//System.out.println("connect " + p_);
		if (p_ != null) id_.addPort(EVENT_ID_CHANGED_PORT_ID).connectTo(p_);
		return id_;
	}
	

	void commonAdd(Component target_, Component id_, Component c_)
	{
		if (id_ != null) {
			Port p_ = c_.getPort(SERVICE_ID_PORT_ID);
			try {
				if (p_ != null) p_.connect(id_.getPort(SERVICE_ID_PORT_ID));
			}
			catch (Exception e_) {}
		}
	}
	
	Component addPD(Component target_, Component id_)
	{
		//System.out.println("add pd");
		Component pd_ = target_.getComponent(ID_PKT_DISPATCHER);
		if (pd_ == null) {
			pd_ = getComponent(ID_PKT_DISPATCHER);
			if (pd_ == null)
				pd_ = new PktDispatcher(ID_PKT_DISPATCHER);
			else
				pd_ = (PktDispatcher)pd_.clone();
			target_.addComponent(pd_);
		}

		if (pd_ instanceof PktDispatcher)
			((PktDispatcher)pd_).resetCache();
		
		//System.out.println("add " + pd_);
		try {
			pd_.getPort(EVENT_PKT_ARRIVAL_PORT_ID).connectTo(
							target_.getPort(EVENT_PKT_ARRIVAL_PORT_ID));
		} catch (Exception e_) {} // ignored
		try {
			pd_.getPort(MCAST_QUERY_PORT_ID).connect(
							target_.getPort(MCAST_QUERY_PORT_ID));
		} catch (Exception e_) {} // ignored
		try {
			pd_.getPort(UCAST_QUERY_PORT_ID).connect(
							target_.getPort(UCAST_QUERY_PORT_ID));
		} catch (Exception e_) {} // ignored
		
		// up ports
		Port[] pp_ = target_.getAllPorts(Module.PortGroup_UP);

		if (pp_ != null)
			for (int i=0; i<pp_.length; i++) {
				Port p_ = pp_[i];
				p_.connect(pd_.addPort(Module.PortGroup_UP, p_.getID()));
			}
		//commonAdd(target_, id_, pd_);
		((PktDispatcher)pd_).bind((Identity)id_);
		return pd_;
	}
	
	Component addRT(Component target_, Component pd_,
					Port rtServicePort_, Port urtEventPort_, Port mrtEventPort_)
	{
		Component rt_ = target_.getComponent(ID_RT);
		if (rt_ == null) {
			if (rt == null)
				rt_ = new RT(ID_RT);
			else
				rt_ = (Component)rt.clone();
			target_.addComponent(rt_);
		}
		//System.out.println("add " + rt_);
		Port servicePort_ = rt_.addPort(SERVICE_RT_PORT_ID);
		if (rtServicePort_ != null) servicePort_.connect(rtServicePort_);
		if (urtEventPort_ != null)
			rt_.addPort(EVENT_RT_UCAST_CHANGED_PORT_ID).connectTo(
							urtEventPort_);
		if (mrtEventPort_ != null)
			rt_.addPort(EVENT_RT_MCAST_CHANGED_PORT_ID).connectTo(
							mrtEventPort_);
		
		//Port p_ = pd_.getPort(SERVICE_RT_PORT_ID);
		//if (p_ != null) p_.connect(servicePort_);
		((PktDispatcher)pd_).bind((RT)rt_);
		return rt_;
	}
	
	void addHello(Component target_, Component id_, Component pd_)
	{
		Port p_ = target_.getPort(EVENT_VIF_PORT_ID);
		Component hello_ = target_.getComponent(ID_HELLO);
		if (hello_ == null) {
			hello_ = p_ == null? new Hello(ID_HELLO): new Hellov(ID_HELLO);
			target_.addComponent(hello_);
		}
		//System.out.println("   add " + hello_);
		Port servicePort_ = hello_.addPort(SERVICE_IF_PORT_ID);
		if (servicePort_ == null) return;
		if (pd_ != null && hello_ instanceof Protocol) {
			hello_.getPort(Module.PortGroup_DOWN).connect(
				pd_.addPort(Module.PortGroup_UP, String.valueOf(PID_HELLO)));
			servicePort_.connect(target_.addPort(SERVICE_IF_PORT_ID));
		}
		commonAdd(target_, id_, hello_);
		
		if (p_ != null) hello_.getPort(EVENT_VIF_PORT_ID).connectTo(p_);
		p_ = target_.getPort(EVENT_IF_PORT_ID);
		if (p_ != null) hello_.getPort(EVENT_IF_PORT_ID).connectTo(p_);
	}
	
	void addIGMP(Component target_, Component id_, Component pd_)
	{
		Component igmp_ = target_.getComponent(ID_IGMP);
		if (igmp_ == null) {
			igmp_ = new sIGMP(ID_IGMP);
			target_.addComponent(igmp_);
		}
		Port servicePort_ = igmp_.addPort(SERVICE_MCAST_PORT_ID);
		if (servicePort_ == null) return;
		if (pd_ != null && igmp_ instanceof Protocol) {
			igmp_.getPort(Module.PortGroup_DOWN).connect(
					pd_.addPort(Module.PortGroup_UP, String.valueOf(PID_IGMP)));
			servicePort_.connect(target_.addPort(SERVICE_MCAST_PORT_ID));
		}
		commonAdd(target_, id_, igmp_);
		
		Port p_ = target_.getPort(EVENT_MCAST_HOST_PORT_ID);
		if (p_ != null) igmp_.getPort(EVENT_MCAST_HOST_PORT_ID).connectTo(p_);
	}

	void addExtensions(Component[] originalExts_, Component target_,
					Component id_, Component pd_, Component rt_)
	{
		Component[] exts_ = new Component[originalExts_.length];
		Port rtservice_ = null;
		//Port pdport_ = pd_.addPort(PktDispatcher.EXTENSION_PORT_ID);
		if (rt_ != null) {
			rtservice_ = rt_.getPort(SERVICE_RT_PORT_ID);
		}
		
		/*
		//System.out.println("   add " + hello_);
		// Tasks:
		// 1. form a loop on those extension ports
		// 2. connect extension's other ports to id_/rt_ if necessary
		Port prev_ = pdport_;
		for (int i=0; i<exts_.length; i++) {
			Component ext_ = exts_[i] = (Component)originalExts_[i].clone();
			target_.addComponent(ext_);
			Port current_ = ext_.getPort(PktDispatcher.EXTENSION_PORT_ID);
			prev_.connectTo(current_);
			prev_ = current_;
			commonAdd(target_, id_, ext_);
			
			// rt ports
			if (rt_ != null) {
				Port p_ = ext_.getPort(SERVICE_RT_PORT_ID);
				if (p_ != null) p_.connect(rtservice_);
			}
		}
		prev_.connectTo(pdport_); // complete the loop
		*/
	}
	
	Component addPFS(Component target_)
	{
		Component pfs_ = target_.getComponent(ID_PKT_FILTER_SWITCH);
		if (pfs_ == null) {
			pfs_ = new PktFilterSwitch(ID_PKT_FILTER_SWITCH);
			target_.addComponent(pfs_);
		}
		// expose service port
		Port servicePort_ = pfs_.getPort(SERVICE_CONFIGSW_PORT_ID);
		if (servicePort_ == null) return null;
		servicePort_.connect(target_.addPort(SERVICE_CONFIGSW_PORT_ID));
		return pfs_;
	}
}
