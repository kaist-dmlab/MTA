// @(#)InfoPort.java   9/2002
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

package drcl.comp;

import java.util.*;
import drcl.data.*;
import drcl.comp.contract.*;

/**
 * Defines the dedicated infoPort in a component.
 * @see Component
 */
class InfoPort extends Port
{
	/** Constructor, default duplex port type. */
	public InfoPort()
	{ super(PortType_INOUT);	}
	
	/** Constructor, with specified port type. */
	public InfoPort(int type_)
	{ super(type_);	}
	
	/** Constructor, with specified port type and properties. */
	public InfoPort(int type_, boolean exeBoundary_)
	{ super(type_); }
	
		/*
	protected final void doReceiving(Object data_, Port peer_, WorkerThread thread_)
	{
		// speicial handling of information request
		// XXX:
		if (host != null && host.infoPort == this) {
			if (data_ == null) {
				// query for configuration
				// XXX:
				doSending(new PropertyContract.Message(Util.getAllProperties(host)));
				return; 
			}
			else if (data_ instanceof String) {
				// query for configuration
				doSending(new PropertyContract.Message(Util.getProperty(host, (String)data_)));
				return; 
			}
			else if (data_ instanceof drcl.data.IntObj) {
				int mask_ = ((drcl.data.IntObj)data_).value;
				host.setComponentFlag(mask_ >> 1, (mask_ & 1) > 0);//get bit 0
				doSending(new drcl.data.LongObj(host.getComponentFlag()));
			}
		}
		super.doReceiving(data_, peer_, thread_);
	}
		*/
	protected void doSending(Object data_, WorkerThread thread_)
	{
		if (outwire != null) {
			for (PortPack tmp_ = outwire.inports; tmp_ != null;
							tmp_ = tmp_.next) {
				Port p_ = tmp_.port;
				Component host_ = p_.host;
				if (p_ == this || !host_.isEnabled()) continue;

				String prevState_ = thread_.state;
		
				try {
					host_.process(data_, p_);
				}
				catch (Exception e_) {
					drcl.Debug.error("process() data from infoport at " + p_);
					e_.printStackTrace();
				}
		
				// restore context
				thread_.releaseAllLocks(host_);
					// Don't hold locks across components!
				thread_.state = prevState_;
			}
		}
	}
}
