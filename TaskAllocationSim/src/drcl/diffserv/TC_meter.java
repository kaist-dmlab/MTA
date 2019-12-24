// @(#)TC_meter.java   9/2002
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

import drcl.net.Packet;

/**
  * This class implementes a three-color meter. 
  * it can operate in two modes, single-rate or two rate
  * refer to RFC 2697, 2698 for detail
  * 
  * @author Rong Zheng (zhengr@ee.eng.ohio-state.edu)
  * @version 1.0 07/16/2000
  * 
  * <p> Properties:
  * <ul> 
  * <li> <code>PBS</code>: peak burst size
  * <li> <code>PIR</code>: peak information rate
  * <li> <code>CBS</code>: committed burst size
  * <li> <code>CIR</code>: committed information rate
  * <li> <code>mode</code>: can be signle rate or two rate
  * </ul>
  * 
  */
public class TC_meter extends Meter implements DFConstants
{
	int mode;   //single rate, two rate
	long PIR; //Peak Information Rate
	long PBS; //Peak Burst Size
	long CIR; //Committed Information Rate
	long CBS; //Commiteted Burst Size
	transient long Tp = 0;  //Token Bucket for PIR
	transient long Tc = 0;  //Token Bucket for CIR	
	transient double lastimep = Double.NaN;
	transient double lastimec = Double.NaN;
	transient double idletime = Double.NaN, time = 0.0; // for debug

	public TC_meter()
	{ super(); }
	
	public TC_meter(String mode_, long cbs, long cir, long pbs, long pir)
	{
		this();
		setMode(mode_);
		PIR = pir;
		PBS = pbs;
		CIR = cir;
		CBS = cbs;
	}

	public void reset()
	{
		Tp = 0;  
		Tc = 0;  
		lastimep = Double.NaN;
		time  = 0.0;
	}
	
	public void duplicate(Object source_)
	{
		TC_meter that_ = (TC_meter)source_;
		PIR = that_.PIR;
		PBS = that_.PBS;
		CIR = that_.CIR;
		CBS = that_.CBS;
	}
	
	public String info(String prefix_)
	{
		return prefix_ + "TC_meter: " + MODES[mode] + ", cbucket=" + Tc + "/"
			+ CBS + " cir=" + CIR + " lastimec=" + lastimec + "; pbucket=" + Tp + "/" + PBS + " pir="
			+ PIR + " lastimep=" + lastimep
			+ (isDebugEnabled()? ", token utilization="
				+ (100.0 - idletime*100.0/time) + "%": "") +  "\n";
	}

	public void setPeakInformationRate(long pir)
	{ PIR = pir; }
	
	public long getPeakInformationRate()
	{ return PIR; }

	public void setPeakBurstSize(long pbs)
	{ PBS = pbs; }
	
	public long getPeakBurstSize()
	{ return PBS; }

	public void setCommittedInformationRate(long cir)
	{ CIR = cir; }

	public long getCommittedInformationRate()
	{ return CIR; }

	public void setCommittedBurstSize(long cbs)
	{ CBS = cbs; }

	public long getCommittedBurstSize()
	{ return CBS; }

	public void setDebugEnabled(boolean enabled_)
	{
		if (enabled_ && Double.isNaN(idletime))
			idletime = 0.0;
		else if (!enabled_) idletime = Double.NaN;
	}

	public boolean isDebugEnabled()
	{ return !Double.isNaN(idletime); }

	public void setMode(String mode_)
	{
		if(mode_.equals(SINGLE_RATE)) 
			mode = _SINGLE_RATE;
		else if(mode_.equals(TWO_RATE))
			mode = _TWO_RATE;
		else 
			drcl.Debug.error(mode_ + " not supported");
	}

	public String getMode()
	{ return MODES[mode]; }
	
	protected int measure(Packet p_, double now_)
	{
		if (Double.isNaN(lastimep))
			lastimep = lastimec = now_;
		double durationp_ = now_ - lastimep;
		double durationc_ = now_ - lastimec;
		long num_of_bit = p_.size<<3;

		if (isDebugEnabled()) time = now_;
		
		/* update counter */
		// two rate
		if (mode == _TWO_RATE) {
			long Tp_now_ = Tp + (long)(durationp_*PIR);
			if(Tp_now_ > PBS) {
				Tp = PBS;
				lastimep = now_;
			}
			long Tc_now_ = Tc + (long)(durationc_*CIR);
			if(Tc_now_ > CBS) {
				if (isDebugEnabled()) idletime += ((double)(Tc-CBS)/CIR);
				Tc = CBS;
				lastimec = now_;
			}

			if(Tc >= num_of_bit) {
				Tp -= num_of_bit;
				Tc -= num_of_bit;
				return GREEN;
			}
			else if(Tp >= num_of_bit){
				Tp -= num_of_bit;			
				return YELLOW;
			}
			else
				return RED;
		}
		else{
			//single rate
			long Tc_now_ = Tc + (long)(durationc_*CIR);
			if(Tc_now_ > CBS){
				Tp += (Tc_now_ - CBS); Tc = CBS; lastimec = now_;
				if(Tp > PBS) {
					if (isDebugEnabled()) idletime += ((double)(Tp-PBS)/CIR);
					Tp = PBS;
				}
			}
			if(Tc >= num_of_bit){	
				Tc -= num_of_bit;
				return GREEN;
			}
			else if(Tp >= num_of_bit){
				Tp -= num_of_bit;
				return YELLOW;
			}
			return RED;
		}
	}

	public void config(String mode, long cbs, long cir,  long pbs, long pir)
	{
		setMode(mode);
		PIR = pir;
		PBS = pbs;
		CIR = cir;
		CBS = cbs;
	}
}
