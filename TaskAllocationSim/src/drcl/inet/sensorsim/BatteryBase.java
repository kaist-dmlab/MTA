// @(#)BatteryBase.java   12/2003
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

/** Abstract base class for battery models.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public abstract class BatteryBase extends drcl.comp.Component
{
  	/** Number of Maximum Components using the battery */
	public static final int MAX_COMPONENT    = 10;  
	public static final String batteryOut   = "batteryOut";
	public static final String battery = "battery" ;
	public static final String FORK_PORT = "forkPort";

	// Power Consumers
	public static final int RADIO_MODEL   	 = 0;  
	public static final int CPU_MODEL   	 = 1;  
	public static final int SENSOR_MODEL	 = 2;  	

	Port batteryOutPort = addPort(batteryOut);
	Port batteryPort = addPort(battery);
	Port forkPort = addForkPort(FORK_PORT);

	/** Initial Total Energy of the battery */
	double totalEnergy;

	/** Instantaneous Remaining Energy */
	double energy_;

	/** Battery ID */
	int batteryID;

	double [] powerRating = new double [MAX_COMPONENT];
	double [] currentRating = new double [MAX_COMPONENT];

	public void initialize()
	{
		energy_ = totalEnergy = 0.0 ;
		for ( int i = 0 ; i < MAX_COMPONENT ; i++ )
		{
			powerRating[i] = 0.0 ;
			currentRating[i] = 0.0 ;
		}
	}

	public void initialize(double energy)
	{
		energy_ = totalEnergy = energy ;
		for ( int i = 0 ; i < MAX_COMPONENT ; i++ )
		{
			powerRating[i] = 0.0 ;
			currentRating[i] = 0.0 ;
		}
	}

	public BatteryBase()
	{
		super("batteryModel");
		initialize();
	}
	
	public BatteryBase(double energy)
	{
		super("batteryModel");
		initialize(energy);
	}

	public void setBatteryID(int id)
	{	
		batteryID = id ;
	}

	// needs to be implemented by the derived class
	public int changeCurrent(double d, int i)
	{	return 1 ;	}

	/** Sets the energy level of the battery */
	public int setEnergy(double energy)
	{
		energy_ = energy;
		return 1;
	}

	/** Gets the energy */
	public double energy()
	{
		return energy_ ;
	}

	/** Gets the energy that has been spent so far */
	public double energySpent()
	{
		return (totalEnergy - energy_) ;
	}

	public double energyPercent()
	{
		return (energy_*100.0/totalEnergy) ;
	}

	public boolean isDead()
	{
		return	false ;
	}
}
