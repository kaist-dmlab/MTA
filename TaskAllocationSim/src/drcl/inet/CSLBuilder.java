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

package drcl.inet;

import drcl.comp.*;
import drcl.net.Module;

/**
The base class for building a {@link CoreServiceLayer core service layer}.
An implementation of a core service layer should come with a CSLBuilder class.
In particular, a CSLBuilder subclass provides {@link #createCSL()} to create
an instance of the {@link CoreServiceLayer} that is known to this builder.

@author Hung-ying Tyan
@version 1.0, 10/17/2000
 */
public abstract class CSLBuilder extends Component implements InetConstants
{
	/** The default CSL builder. */
	public static CSLBuilder DEFAULT_BUILDER = new drcl.inet.core.CSLBuilder(
					Node.ID_CSL);
	
	public CSLBuilder()
	{ super(); }

	public CSLBuilder(String id_)
	{ super(id_); }
	
	public void duplicate(Object source_) 
	{
		super.duplicate(source_);
		CSLBuilder that_ = (CSLBuilder)source_;
		bw = that_.bw;
		bs = that_.bs;
		mtu = that_.mtu;
	}
	
	public String info()
	{
		String mtu_ = mtu > 0? String.valueOf(mtu): mtu == 0?
				"disabled": "default value (depending on CSL implementation)";
		return   "   Bandwidth = " + (bw > 0.0?
				String.valueOf(bw):
				"default value (depending on CSL implementation)") + "\n"
				+ " Buffer size = "
				+ (bs > 0.0? String.valueOf(bs):
					"default value (depending on CSL implementation)") + "\n"
				+ "Fragmentation: " + mtu_ + "\n";
	}

	/** Extracts the structure of an existing CoreServiceLayer to
	 * the builder. */
	public abstract void extract(CoreServiceLayer csl_);
	
	/** Builds the core service layers inside the specified containers. */
	public abstract void build(Object[] cc_);
	
	/**
	 * Creates and returns an instance of the CoreServiceLayer class
	 * which is known to this builder.
	 */
	public abstract CoreServiceLayer createCSL();

	public void build(Object c_)
	{ build(new Object[]{c_}); }
	
	public Port addUpPort(int pid_)
	{
		return addPort(Module.PortGroup_UP, String.valueOf(pid_));
	}
	
	public Port addDownPort(int ifindex_)
	{
		return addPort(Module.PortGroup_DOWN, String.valueOf(ifindex_));
	}
	
	/** Global bandwidth setting (in bps). */
	protected double bw;
	
	/** Global buffer size setting (in bytes). */
	protected int bs;
	
	/** Fragmentation size.  Default is 0 (no fragmentation).*/
	protected int mtu = 0;
	
	protected String bufferMode = "byte";

	/** True if link emulation is enabled. */
	protected boolean linkEmu = false;

	/** Global link propagation delay, used with link emulation enabled. */
	protected double linkPropDelay = Double.NaN;
	
	/** Sets the bandwidth (in bps) for all the interfaces. */
	public void setBandwidth(double bw_)
	{ bw = bw_;	}
	
	public double getBandwidth()
	{ return bw; }
	
	/** Sets the buffer size (in bytes) for all the interfaces. */
	public void setBufferSize(int bs_)
	{ bs = bs_;	}
	
	public int getBufferSize()
	{ return bs; }
	
	/**
	 * Sets the maximum transmission unit (MTU) for all interfaces.
	 * A positive value also enables the fragmentation in the core service layer.
	 * Zero disables it.  The default value applies if a negative value is given.
	 */
	public void setMTU(int mtu_)
	{ mtu = mtu_; }

	public int getMTU()
	{ return mtu; }

	/**
	 * Sets the buffer mode of all the interfaces.
	 * @param mode_ can be either "packet" or "byte".
	 */
	public void setBufferMode(String mode_)
	{ bufferMode = mode_; }

	public String getBufferMode()
	{ return bufferMode; }

	public boolean isLinkEmulationEnabled()
	{ return linkEmu; }

	public void setLinkEmulationEnabled(boolean enabled_)
	{ linkEmu = enabled_; }

	/** Returns the (global) emulated link propagation delay.
	 * Used with link emulation enabled. */
	public double getLinkPropDelay()
	{ return linkPropDelay; }

	/** Sets the (global) emulated link propagation delay.
	 * Used with link emulation enabled. */
	public void setLinkPropDelay(double delay_)
	{ linkPropDelay = delay_; }
}
