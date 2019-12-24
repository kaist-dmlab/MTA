// @(#)DFProfile.java   9/2002
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

package drcl.diffserv;

import drcl.data.*;
import drcl.comp.*;

/**
This class defines a profile for Diffserv.
It is simply a container of a {@link Meter} and a {@link Marker}.

@author Rong Zheng
@version 1.0 07/16/00   
 */
public class DFProfile extends drcl.DrclObj
{
	Marker marker = null;
	Meter meter = null;
	
	public DFProfile()
	{ super(); }
	
	/** Resets the installed marker and meter. */
	public void reset()
	{
		if (marker != null) marker.reset();
		if (meter != null) meter.reset();
	}

	/** Prints out the content of the installed marker and meter. */
	public String info(String prefix_) 
	{
		return prefix_ + "Marker: " + (marker == null? "<null>\n": marker.info())
			 + prefix_ + "Meter: " + (meter == null? "<null>\n": meter.info());
	}
	
	/** Duplicates the marker and meter from <code>source_</code>.*/
	public void duplicate(Object source_)
	{
		super.duplicate(source_);
		DFProfile that_ = (DFProfile)source_;
		if (that_.marker == null) marker = null;
		else marker = (Marker)that_.marker.clone();
		if (that_.meter == null) meter = null;
		else meter = (Meter)that_.meter.clone();
	}

	/** Intalls the marker and meter to this profile. */
	public void set(Marker marker_, Meter meter_)
	{ marker = marker_; meter = meter_; }
	
	/** Intalls the marker to this profile. */
	public void setMarker(Marker m_)
	{ marker = m_; }

	/** Intalls the meter to this profile. */
	public void setMeter(Meter m_)
	{ meter = m_; }

	/** Returns the installed meter. */
	public Meter getMeter()
	{ return meter; }
	
	/** Returns the installed marker. */
	public Marker getMarker()
	{ return marker; }
}
