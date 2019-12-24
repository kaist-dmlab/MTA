// @(#)CPUBase.java   12/2003
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
import drcl.data.IntObj ;

/** Abstract base class for CPU models.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public abstract class CPUBase extends drcl.comp.Component
{
  	// Different Status of CPU
	public static final int CPU_IDLE    = 0;  
	public static final int CPU_SLEEP   = 1;  
	public static final int CPU_ACTIVE  = 2;  
	public static final int CPU_OFF     = 3;  

	int cpuMode;
	double activeCur; 
	double idleCur;
	double sleepCur; 
	double offCur;
	double clockSpeed;
	boolean flag;

	Port batteryInPort = addPort("batteryIn");
	Port batteryPort = addPort("battery");
	Port reportCPUModePort = addPort("reportCPUMode");
	
	public CPUBase()
	{
		super("cpuModel");
		cpuMode=CPU_IDLE;
		activeCur=1.0;
		idleCur=1.0;
		sleepCur = 1.0;
		clockSpeed=1.0;
		flag=false;
	}
 
	/* attachApp() is to be called explicitly from tcl to attach the reportCPUModePort to the corresponding port in SensorApp, which is passed as the parameter port_ */
	/** Connects to the sensor application layer. */
	public void attachApp(Port port_) {
        	reportCPUModePort.connectTo(port_); 
		reportCpuMode(cpuMode);
	}

	/** Reports the CPU mode to the sensor application layer. */
	public void reportCpuMode(int mode) 
	{
		reportCPUModePort.doSending(new IntObj(mode));
	} 

	/** Gets the active current. */
	public double getActiveCur() {return activeCur;}

	/** Gets the idle current. */
	public double getIdleCur() {return idleCur;}
	
	/** Gets the sleep current. */
	public double getSleepCur() { return sleepCur; }
	
	/** Gets the OFF current. */
	public double getOffCur() { return offCur; }
	
	/** Gets the clock speed.  */
	public double getClockSpeed() { return clockSpeed; }
	
	/** Gets the CPU mode.  */
	public int getCPUMode() { return cpuMode; }
	
	/** Gets the remaining energy.  */
	public double getRemainingEnergy() 
	{
		double e_ = BatteryContract.getRemainingEnergy(batteryPort);
		return e_;
	}
	
	/** Sets the active current. */
	public double setActiveCur(double a) {return activeCur=a;}
	
	/** Sets the idle current. */
	public double setIdleCur(double a) {return idleCur=a;}
	
	/** Sets the sleep current. */
	public double setSleepCur(double a) {return sleepCur=a;}
	
	/** Sets the OFF current. */
	public double setOffCur(double a) {return offCur =a; }
	
	/** Returns true if the CPU is OFF. */
	public boolean isOff(){ return ( cpuMode == CPU_OFF )?true:false;}
	/** Returns true if the CPU is sleep. */
	public boolean isSleep(){ return ( cpuMode==CPU_SLEEP)?true:false;}
	/** Returns true if the CPU is idle. */
	public boolean isIdle(){ return (cpuMode==CPU_IDLE)?true:false;}
	/** Returns true if the CPU is active. */
	public boolean isActive(){ return ( cpuMode==CPU_ACTIVE)?true:false;}
	
	public void toggleFlag() { flag=(flag)?false:true; }
	public boolean flagIsSet() { return flag; }

	// needs to be implemented by the derived class
	/** Sets the CPU mode. */
	public int setCPUMode(int a) { cpuMode = a; return 1; } 
	/** Sets the clock speed. */
	public void setClockSpeed(double a) { clockSpeed=a; }

	protected synchronized void process(Object data_, Port inPort_)
	{
		if ( inPort_ == batteryInPort )
		{
			String msg = new String ((String) data_);
			if ( msg.equals("BATTERY_OUT") )
			{
				setCPUMode(CPU_OFF);
			}
		}
		else if ( inPort_ == reportCPUModePort )
		{
			setCPUMode(((IntObj)data_).getValue());
		}
	}
}
