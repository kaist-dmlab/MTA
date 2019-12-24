// @(#)RadioSimple.java   12/2003
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

/** This class implements a radio model with reasonable values for transmit, receive and sleep currents.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class RadioSimple extends RadioBase
{
  	// Values for currents of different Radio modes
	public static final double TX_CUR = 5.2e-3; // Transmit mode current draw = 12 mA
	public static final double RX_CUR = 4.1e-3;   // Receiver mode current draw   1.8 mA
	public static final double SLEEP_CUR = 5e-6; // Sleep mode current draw  5 microA
	public static final double DATARATE = 19200;  // Data Rate 19.2 kbps

	public RadioSimple() 
	{
		super();
		txCur = TX_CUR;
		rxCur = RX_CUR;
		dataRate = DATARATE;
		radioMode = RADIO_RECEIVE;
	}

	/** Reports the current to the battery model. */
	public void reportCurrent(double current)
	{
		BatteryContract.setConsumerCurrent(BatteryBase.RADIO_MODEL, current, batteryPort);
	}

	/** Sets the radio mode and reports the current to the battery model. */
	public int setRadioMode(int mode) 
	{
		switch (mode)
		{
			case RADIO_TRANSMIT:
				if ( radioMode != RADIO_OFF )
					reportCurrent(txCur) ;
				else
					return 0;
				break;
			case RADIO_IDLE:
			case RADIO_RECEIVE:
				if ( radioMode != RADIO_OFF )
					reportCurrent(rxCur);
				break;
			case RADIO_SLEEP:
				if ( radioMode != RADIO_OFF)
					reportCurrent(SLEEP_CUR);
				break;
			case RADIO_OFF:
				reportCurrent(0.0);
				break;
		} // end switch
		
		radioMode = mode;  
		reportRadioMode(mode);  // report it to the application
		return 1;
	} 

	public void turnOnTransmit() {
		setRadioMode(RADIO_TRANSMIT);
	}

	public void turnOffTransmit() { 
		setRadioMode(RADIO_RECEIVE);
	}
 
	public void sleep() {  
		setRadioMode(RADIO_SLEEP);
	}

	public void wakeup() { 
		setRadioMode(RADIO_RECEIVE);
	}
}
