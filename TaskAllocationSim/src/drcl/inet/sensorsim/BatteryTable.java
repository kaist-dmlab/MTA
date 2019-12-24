// @(#)BatteryTable.java   12/2003
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

/** This class implements the table that specifies the capacity of a battery as a function of its current.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class BatteryTable extends drcl.DrclObj
{
	public static final int MAX_TABLE_SIZE = 25;  	
	public static final int CURRENT = 0;  
	public static final int CAPACITY = 1;  
	public static final int SEC_IN_HOUR = 3600;  
	public static final int MAX_CAPACITY = 10;  	
	
	double maxCapacity ; // in mAh
	double curCapacity ; 

	double [] [] data = new double [2] [MAX_TABLE_SIZE];

	public BatteryTable()
	{
		for ( int i = 0 ; i < MAX_TABLE_SIZE; i++ )
		{
			data[CURRENT][i] = 0.0 ;
			data[CAPACITY][i] = 0.0 ;			
		}
	}

	/** Sets the maximum capacity */
	public void setMaxCapacity(double a)
	{
		maxCapacity = a ; // in mAh
		curCapacity = a ; 
	}
	
	/** Gets the current capacity */
	public double getCurCapacity()
	{
		return curCapacity ;
	}

	/** Computes the new value of the capacity based on a given value for current and after a certain given time duration */
	public double computeNewCapacity(double current, double duration)
	{
		double newCap;
		double effCap = getCapacity(current);
		newCap = curCapacity - (current*duration/SEC_IN_HOUR)/effCap;
		curCapacity = newCap;
		return ( curCapacity );		
	}

	/** Gets the interpolated capacity at a given value of current */
	public double getCapacity(double current)
	{
		int i;
		for (i=0; i < MAX_TABLE_SIZE; i++) {
			if ( data[CURRENT][i] >= current )
				break;
		} // end for
		
		if ( i == 0 )  // current smaller than 0.1
			return 1.0 ;
		
		if ( i >= MAX_TABLE_SIZE ) // current out of range
			return 0.0 ;

		// find the interpolated capacity
		double midPoint, capRange;
		midPoint = (current - data[CURRENT][i-1]) / ( data[CURRENT][i] - data[CURRENT][i-1] ) ;
		capRange = data[CAPACITY][i]-data[CAPACITY][i-1];
		return ((capRange*midPoint)+data[CAPACITY][i-1]);
	}

	/** Inputs an entry in the table */
	public int inputData(int i, double current, double capacity)
	{
		if ( (i < 0) || (i >= MAX_TABLE_SIZE) )
		{
			return -1 ;
		} 
		else 
		{
			data[CURRENT][i] = current ;
			data[CAPACITY][i] = capacity ;
			return 1 ;
		}
	}
}
