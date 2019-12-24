// @(#)SeismicProp.java   12/2003
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

import drcl.comp.Port;
import drcl.comp.Contract;

/** This class implements the seismic propagation model.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SeismicProp extends SensorRadioPropagationModel
{
	double d0 ;
	/** Attenuation Factor  */
	double atnFactor ;

	public SeismicProp()
	{
		super();
		d0 = 0.1 ;
		atnFactor = 1.0 ;
	}

 	public SeismicProp(String id_)
	{
		super(id_);
		d0 = 0.1 ;
		atnFactor = 1.0 ;
	}

	/** Sets the minimum value of the distance between two nodes */
	public void setD0(double d0_)
	{
		d0 = d0_ ;
	}

	/** Sets the attenuation factor  */
	public void setAtnFactor(double atnFactor_)
	{
		atnFactor = atnFactor_ ;
	}

	/** Calculates the signal strength of a received signal based on the distance between the transmitter and the receiver and also based on the attenuation factor  */
    protected synchronized double calculateReceivedSignalStrength(Object data_) {
        SensorRadioPropagationQueryContract.Message msg = (SensorRadioPropagationQueryContract.Message) data_;

        double Pt; // power with which the packet was sent
		   // Pt is included in the request (i.e., query)
	double Xs, Ys, Zs; // location of sender
	double Xr, Yr, Zr; // location of receiver
        double Pr; // power with which the packet was received
		   // Pr is included in the reply

	double d;			// distance between sender and receiver

	Xs = msg.getXs(); 
        Ys = msg.getYs(); 
        Zs = msg.getZs(); 
        Xr = msg.getXr();
        Yr = msg.getYr(); 
        Zr = msg.getZr();
        Pt = msg.getPt();
  
	d = Math.sqrt((Xs - Xr) * (Xs - Xr)  
	   + (Ys - Yr) * (Ys - Yr) 
	   + (Zs - Zr) * (Zs - Zr)); 
 
	d = Math.max(d, d0);

	Pr = Pt/ (Math.pow(d, atnFactor));
	return Pr;        
    }
       	public String info() { 
        return "SeismicProp" + "\n"; 
    }
}
