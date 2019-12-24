// @(#)CPUAvr.java   12/2003
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

package drcl.inet.sensorsim;

import drcl.comp.*;

/** This class implements a CPU model with reasonable values for active, idle, sleep and off currents.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class CPUAvr extends CPUBase
{
  	// Values for currents of different CPU modes
	public static final double ACTIVE_CUR = 2.9e-3; // 2.9 mA
	public static final double IDLE_CUR = 2.9e-3;  	// 2.9 mA
	public static final double SLEEP_CUR = 1.9e-3;  // 1.9 mA
	public static final double OFF_CUR = 1e-6;    	// 1 microA

	public CPUAvr()
	{
		super();
		cpuMode=CPU_ACTIVE;
		activeCur=ACTIVE_CUR;
		idleCur=IDLE_CUR;
		sleepCur = SLEEP_CUR;
		offCur=OFF_CUR;
		flag=false;
	}
 
	/** Reports the current to the battery model.  */
	public void reportCurrent(double current)
	{
		BatteryContract.setConsumerCurrent(BatteryBase.CPU_MODEL, current, batteryPort);
	}

	/** Sets the CPU mode and reports the current to the battery model. */
	public int setCPUMode(int mode) 
	{
		switch (mode)
		{
			case CPU_ACTIVE:
				reportCurrent(activeCur);
				break;
			case CPU_IDLE:
				reportCurrent(idleCur);
				break;
			case CPU_SLEEP: 
				if ( flag == false )
					reportCurrent(sleepCur);
				else  
					reportCurrent(offCur);    
				break;
			case CPU_OFF:
				reportCurrent(offCur);
				break;
		}
		cpuMode = mode; 
		reportCpuMode(mode);
		return 1;
	} 
}
