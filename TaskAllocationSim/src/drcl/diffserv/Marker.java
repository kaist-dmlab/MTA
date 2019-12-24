// @(#)Marker.java   12/2003
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

package drcl.diffserv;

import drcl.inet.InetPacket;

/**
This class defines a marker. The exact operation by marker is decided by its
operation mode and each packet's label.

@author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
@version 1.0 10/26/2000   
  */
public class Marker extends drcl.DrclObj implements DFConstants
{
	public static String COLOR_BLIND = "COLOR_BLIND";
	public static String COLOR_AWARE = "COLOR_AWARE";
	public static String POLICER = "POLICER";
	public static String SET_EF = "SET_EF";
	public static String SET_AF11 = "SET_AF11";
	public static String DROPPER = "DROPPER";
	public static String BY_PASS = "BY_PASS";

	final static String[] MODES = {COLOR_BLIND, COLOR_AWARE, POLICER, SET_EF, SET_AF11, DROPPER, BY_PASS};

	final static int 
		_COLOR_BLIND = 0, //mark
		_COLOR_AWARE = 1, //remark
		_POLICER = 2,
		_SET_EF = 3,
		_SET_AF11 = 4, //we only implement AF11 here, extension to AFXX is trivial
		_DROPPER = 5,
		_BY_PASS = 6;
	
	int mode;
	long count; // # of packets being marked, for diagnosis
	long dropcount; // # of packets being dropped
	
	public Marker()
	{ super(); }

	public Marker(String mode_)
	{
		this();
		setMode(mode_);
	}

	/** Resets this marker. */
	public void reset()
	{ count = dropcount = 0; }
	
	public void duplicate(Object source_)
	{ mode = ((Marker)source_).mode; }

	/** Prints out the content of this marker. */
	public String info()
	{ return info(""); }

	/** Prints out the content of this marker.
	@param prefix_ prefix that should be prepended to each line of the result.
	*/
	public String info(String prefix_)
	{
		return prefix_ + "Marker of type: " + MODES[mode]
				+ ", count=" + count + ", #loss=" + dropcount + "\n";
	}

	/** Sets the operation mode of this marker.*/
	public void setMode(String mode_)
	{
		if(mode_.equals(COLOR_BLIND)) 
			mode = _COLOR_BLIND;
		else if(mode_.equals(COLOR_AWARE))
			mode = _COLOR_AWARE;
		else if(mode_.equals(POLICER))
			mode = _POLICER;
		else if(mode_.equals(SET_EF))
			mode = _SET_EF;
		else if(mode_.equals(SET_AF11))
			mode = _SET_AF11;		
		else if(mode_.equals(DROPPER))
			mode = _DROPPER;				
		else if(mode_.equals(BY_PASS))
			mode = _BY_PASS;				
		else
			drcl.Debug.error(mode_ + " is not supported.");
	}
	
	/** Returns the operation mode of this marker.*/
	public String getMode()
	{ return MODES[mode]; }
	
	/** Marks in the DSCP part of ToS in the packet header with the specified lable.
	@return false if caller should drop the packet.  */
	protected boolean markPacket(InetPacket p_, int label)
	{
		count++;
		try{
			switch(mode){
			case _SET_EF:
				DFUtil.setDSCP(p_, EF);
				break;
			case _SET_AF11:
				DFUtil.setDSCP(p_, AF11);
				break;
			case _POLICER:				
				if(label != GREEN && label != IN_PROFILE) {
					dropcount++;
					return false;
				}
				break; // Tyan: a break is missing here
			case _COLOR_BLIND:
				DFUtil.setDSCP(p_, AF11 + label);
				break;
			case _COLOR_AWARE: //FIXME: this is really a matter of policy
				DFUtil.setDSCP(p_, Math.max(AF11 + label, DFUtil.getDSCP(p_)));
				break;
			case _DROPPER :  //DROP all 
				dropcount++;
				return false;
			default:
				DFUtil.setDSCP(p_, BE);
				break;
			}
			return true;			
		}
		catch(Exception e){			
			dropcount++;
			return false;
		}		
	}
}
