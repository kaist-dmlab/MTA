// @(#)RadioBase.java   12/2003
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

/* base class of Radio models of a sensor*/

import drcl.comp.*;
import drcl.data.IntObj ;

/** Abstract base class for radio models.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public abstract class RadioBase extends drcl.comp.Component
{
  	// Different Status of Radio
	public static final int RADIO_IDLE = 0;  
	public static final int RADIO_SLEEP = 1;  
	public static final int RADIO_OFF = 2;  
	public static final int RADIO_TRANSMIT = 3;  
	public static final int RADIO_RECEIVE = 4;  

	int radioMode;
	double txCur; //Transmit Power
	double rxCur;
	double dataRate;  // DataRate

	Port batteryInPort = addPort("batteryIn");
	Port batteryPort = addPort("battery");
	Port reportRadioModePort = addPort("reportRadioMode");

	public RadioBase()
	{
		super("radioModel");
		radioMode=RADIO_IDLE;
		txCur=1.0;
		rxCur=1.0;
		dataRate = 1.0;
	}
	
	/** Gets the transmission current. */
	public double getTxCur() {return txCur;}
	
	/** Gets the receiving current. */
	public double getRxCur() {return rxCur;}
	
	/** Gets the data rate. */
	public double getDataRate() {return dataRate;}
	
	/** Gets the radio mode. */
	public int getRadioMode() { return radioMode; }

	/** Gets the remaining energy.  */
	public double getRemainingEnergy() 
	{ 
		double e_ = BatteryContract.getRemainingEnergy(batteryPort);
		return e_;
	}

	/** Sets the transmission current. */
	public double setTxCur(double a) {return txCur=a;}
	
	/** Sets the receiving current. */
	public double setRxCur(double a) {return txCur=a;}
	
	/** Sets the data rate. */
	public double setDataRate(double a) {return dataRate=a;}

	/* attachApp() is to be called explicitly from tcl to attach the reportRadioModePort to the corresponding port in SensorApp, which is passed as the parameter port_ */
	/** Connects to the sensor application layer. */
	public void attachApp(Port port_) {
        	reportRadioModePort.connectTo(port_); 
		reportRadioMode(radioMode);
	}

	/** Reports the CPU mode to the sensor application layer. */
	public void reportRadioMode (int mode) 
	{
		reportRadioModePort.doSending(new IntObj(mode));
	} 

	/** Returns true if the radio is sleep or off. */
	public boolean isDown()
	{  
	    if ( (radioMode == RADIO_SLEEP) || (radioMode == RADIO_OFF) ) 
		      return true;
	    else 
		      return false;
	}

	/** Returns true if the radio is off. */
	public boolean isOff()
	{ 
	    if ( radioMode == RADIO_OFF ) 
		      return true;
	    else 
		      return false;
	}
	
	/** Returns true if the radio is sleep. */
	public boolean isSleep(){ 
	    if ( radioMode == RADIO_SLEEP ) 
		      return true;
	    else 
		      return false;
	}

	// needs to be implemented by the derived class
	/** Sets the radio mode. */
	public int setRadioMode(int a) { radioMode = a; return 1; } 

	protected synchronized void process(Object data_, Port inPort_)
	{
		if ( inPort_ == batteryInPort )
		{
			String msg = new String ((String) data_);
			if ( msg.equals("BATTERY_OUT") )
			{
				setRadioMode(RADIO_OFF);
			}
		}
		else if ( inPort_ == reportRadioModePort )
		{
			setRadioMode(((IntObj)data_).getValue());
		}
	}
}
