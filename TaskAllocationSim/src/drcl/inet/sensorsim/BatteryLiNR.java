// @(#)BatteryLiNR.java   12/2003
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
import drcl.data.DoubleObj;

/** This class implements an LiNR battery.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class BatteryLiNR extends drcl.inet.sensorsim.BatteryBase
{
	public static final double VOLTAGE = 1.0; 

	double lastTimeOut ; // last time updateEnergy() was called
	BatteryTable myTable ;
	ACATimer batteryOutEventTimer ;
	 /** Initializes the current-capacity table */
	public void initializeLiNR()
	{
		lastTimeOut=0.0; 
		myTable = new BatteryTable () ;
		batteryOutEventTimer = null ;
		// Data from Newman model
		myTable.inputData(0, 0.1, 1.0);
		myTable.inputData(1, 0.2, 0.947);
		myTable.inputData(2, 0.3, 0.930);
		myTable.inputData(3, 0.4, 0.925);
		myTable.inputData(4, 0.5, 0.924);
		myTable.inputData(5, 0.6, 0.923);
		myTable.inputData(6, 0.7, 0.922);
		myTable.inputData(7, 0.8, 0.921);
		myTable.inputData(8, 0.9, 0.920);
		myTable.inputData(9, 1.0, 0.919);
		myTable.inputData(10, 1.1, 0.918);
		myTable.inputData(11, 1.2, 0.917);
		myTable.inputData(12, 1.3, 0.916);
		myTable.inputData(13, 1.4, 0.915);
		myTable.inputData(14, 1.5, 0.888);
		myTable.inputData(15, 1.6, 0.772);
		myTable.inputData(16, 1.7, 0.643);
		myTable.inputData(17, 1.8, 0.602);
		myTable.inputData(18, 1.9, 0.564);
		myTable.inputData(19, 2.0, 0.533);
		myTable.inputData(20, 2.1, 0.498);
		myTable.inputData(21, 2.2, 0.466);
		myTable.inputData(22, 2.3, 0.429);
		myTable.inputData(23, 2.4, 0.380);
		myTable.inputData(24, 2.5, 0.327);
	}

	public BatteryLiNR()
	{
		super();
		initializeLiNR();
	}

	public BatteryLiNR(double energy)
	{
		super(energy);
		initializeLiNR();
	}
	
	/** Calculates the new value of the energy. */
	public void updateEnergy()
	{
		double now = getTime() ;
		int i;

		if ( energy_ <= 0.0 )
		  	return; 
	
		//subtract the energy spent from the last call to change power 
		double totalPower=0.0;
		for (i=0; i< MAX_COMPONENT; i++)
			totalPower += (currentRating[i]*VOLTAGE);
		energy_ -= (totalPower)*(now-lastTimeOut)/myTable.getCapacity(totalPower*1e3/4);
		if ( energy_ <= 0.0 )
		  	energy_ = 0.0; 

		lastTimeOut=now;
	}

	/** Gets the energy. */
	public double energy()
	{
		updateEnergy() ;
		return energy_ ;
	}

	/** Gets the percentage of the energy that has been spent so far. */
	public double energyPercent()
	{
		updateEnergy() ;
		return (energy_*100.0/totalEnergy) ;
	}

	/** Gets the energy that has been spent so far. */
	public double energySpent()
	{
		updateEnergy() ;
		return (totalEnergy - energy_) ;
	}

	/** Returns true if all of the energy has been consumed.  */
	public boolean isDead()
	{
		return	(energy_ > 0.0) ? false : true ;
	}

	/** Changes the current drained by a power consumer. */
	public int changeCurrent(double cur, int componentID) 
	{ 
		int i=0;
		double now = getTime() ;

		if ( (componentID < 0) || (componentID >= MAX_COMPONENT) ) 
		{
		    return -1; // component ID out of range
		}

		updateEnergy();

		currentRating[componentID] = cur;
  
		// Compute the time when the battery will be drained with
		// the current rate of power drain.  Then schedule an event 
		// to handle the case of battery completely being drained
		double totalPower=0.0;
		for (i=0; i < MAX_COMPONENT; i++)
			totalPower += (currentRating[i]*VOLTAGE);

		if (totalPower != 0.0 ) 
		{
			if ( !isDead() ) 
			{
				double timeToZero = energy_*myTable.getCapacity(totalPower*1e3/4)/totalPower; 
				if ( batteryOutEventTimer != null )
					cancelFork(batteryOutEventTimer) ;
				batteryOutEventTimer = fork(forkPort, "BATTERY_OUT", timeToZero);
			}
		}
		else
			if ( batteryOutEventTimer != null )
				cancelFork(batteryOutEventTimer) ;

		return 1; 
	}

	/** Sets the energy of the battery. */
	public int setEnergy(double newEnergyValue) 
	{ 
		int i=0;
		double now = getTime() ;
		
		// Compute the time when the battery will be drained with
		// the current rate of power drain.  Then schedule an event 
		// to handle the case of battery completely being drained
		double totalPower=0.0;
		for (i=0; i< MAX_COMPONENT; i++)
			totalPower += ( currentRating[i] * VOLTAGE );
		newEnergyValue = (newEnergyValue < 0.0)?0.0:newEnergyValue;
		newEnergyValue = (newEnergyValue > 100.0)?100.0:newEnergyValue;
		energy_= (newEnergyValue/100.0) * totalEnergy;

		if ( isDead() ) 
		{
			batteryOutPort.doSending("BATTERY_OUT");
			energy_ = 0.0 ;
			return 1;
		}

		if (totalPower != 0.0 ) 
		{  // cannot be divided by 0
			double timeToZero = energy_*myTable.getCapacity(totalPower*1e3/4)/totalPower;
			if ( batteryOutEventTimer != null )
				cancelFork(batteryOutEventTimer) ;
			batteryOutEventTimer = fork(forkPort, "BATTERY_OUT", timeToZero);
		}
		else
			if ( batteryOutEventTimer != null )
				cancelFork(batteryOutEventTimer) ;
		return 1; 
	}

	protected synchronized void process(Object data_, Port inPort_)
	{
		if ( inPort_ == forkPort )
		{
			String msg = new String ((String) data_);
			if ( msg.equals("BATTERY_OUT") )
			{
				batteryOutPort.doSending("BATTERY_OUT");
				energy_ = 0.0 ;
			}
		} else if ( inPort_ == batteryPort )
		{
			if (data_ instanceof BatteryContract.Message) 
			{
				BatteryContract.Message struct_ = (BatteryContract.Message)data_;
				int type = struct_.getType();
			
				if (type == BatteryContract.GET_REMAINING_ENERGY)  
				{
					batteryPort.doSending(new DoubleObj(energy_));
				} else if (type == BatteryContract.SET_CONSUMER_CURRENT)  
				{
				int consumer_id = struct_.getConsumerID();
				double current = struct_.getCurrent();
				changeCurrent(current, consumer_id);
				} // end else if
			} // end if 
		} // end else if
	} // end process()
} // end class
