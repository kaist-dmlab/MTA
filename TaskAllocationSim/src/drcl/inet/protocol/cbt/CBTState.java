// @(#)CBTState.java   7/2003
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

package drcl.inet.protocol.cbt;

public class CBTState
{ 
	public int state = 0; // INIT
	public long group, core;

	public CBTInterface[] ifs;
	public int upstreamIf = -1; // index of the upstream if
	public int ntries = 0; // # of tries of sending a join/quit

	public CBTState()
	{}

	public CBTState(long group_, long core_)
	{
		group = group_;
		core = core_;
	}

	public void reset()
	{ 
		state = CBTConstants.INIT;
		removeAllIfs();
		ntries = 0;
	}

	public void removeAllIfs()
	{
		ifs = null;
		upstreamIf = -1;
	}

	/** Adds an interface. */
	public CBTInterface addIf(int if_, boolean upstream_, boolean broadcast_,
					boolean hostIf_, boolean routerIf_, Object extension_)
	{
		assert !upstream_ || (upstream_ && upstreamIf < 0);
		if (ifs == null)
			ifs = new CBTInterface[if_ + 1];
		else if (ifs.length <= if_) {
			CBTInterface[] tmp_ = new CBTInterface[if_ + 1];
			System.arraycopy(ifs, 0, tmp_, 0, ifs.length);
			ifs = tmp_;
		}
		assert ifs[if_] == null;
		ifs[if_] = new CBTInterface(upstream_, broadcast_, hostIf_, routerIf_,
						extension_);
		if (upstream_)
			upstreamIf = if_;
		return ifs[if_];
	}

	/** Removes an interface. */
	public void removeIf(int if_)
	{
		if (if_ == upstreamIf)
			upstreamIf = -1;
		ifs[if_] = null;
	}

	/** Retrieves an interface. */
	public CBTInterface getIf(int if_)
	{
		if (ifs == null || ifs.length <= if_)
			return null;
		else
			return ifs[if_];
	}

	/** Retrieves the "upstream" interface. */
	public CBTInterface getUpstreamIf()
	{
		if (upstreamIf < 0)
			return null;
		else
			return ifs[upstreamIf];
	}

	/** Returns true if the state includes at least one down stream interface.*/
	public boolean anyDownstreamIf()
	{
		if (ifs == null) return false;
		for (int i=0; i<ifs.length; i++) {
			CBTInterface if_ = ifs[i];
			if (if_ != null && !if_.isUpstream) return true;
		}
		return false;
	}

	/** Returns true if the interface exists and is a downstream. */
	public boolean isDownstreamIf(int if_)
	{
		if (ifs == null || ifs.length <= if_ || ifs[if_] == null)
			return false;
		else
			return !ifs[if_].isUpstream;
	}

	/** Returns true if the interface exists and is the upstream. */
	public boolean isUptreamIf(int if_)
	{ return !isDownstreamIf(if_); }

	public String toString()
	{
		StringBuffer sb_ = new StringBuffer(
				CBTConstants.STATES[state]
				+ (ntries > 0? ntries+"": "")
				+ "," + group + "," + core
				+ ",up=" + upstreamIf
				+ ",ifs=");
		boolean any_ = false;
		if (ifs != null)
			for (int i=0; i<ifs.length; i++) {
				if (ifs[i] == null) continue;
				any_ = true;
				sb_.append("|" + i + ":" + ifs[i]);
			}
		if (!any_)
			sb_.append("none");
		return sb_.toString();
	}
}
