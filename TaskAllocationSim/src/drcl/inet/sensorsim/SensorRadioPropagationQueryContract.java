// @(#)SensorRadioPropagationQueryContract.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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
import drcl.data.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.data.*;

/** This class implements the contract between the sensor physical layer and the propagation model.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorRadioPropagationQueryContract extends Contract 
{    
    public static final SensorRadioPropagationQueryContract INSTANCE = new SensorRadioPropagationQueryContract();
    
    public SensorRadioPropagationQueryContract() {
        super();
    }
    
    public String getName() {
        return "Sensor Radio Propagation Query Contract";
    }
    
    public Object getContractContent() {
        return null;
    }
    
	/** Constructs the query message */
    public static SensorRadioPropagationQueryContract.Message constructQuery(double Pt_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_ ) 
    {
	// Pt is included in the query, but Pr is not.
        return (new Message(Pt_, Xs_, Ys_, Zs_, Xr_, Yr_, Zr_));
    }
   
	/** This class implements the underlying message of the contract. */ 
    	public static class Message extends drcl.comp.Message 
	{
        double Pt; // power with which the packet was sent
		   // Pt is included in the request (i.e., query)
	double Xs, Ys, Zs; // location of sender
	double Xr, Yr, Zr; // location of receiver
        double Pr; // power with which the packet was received
		   // Pr is included in the reply
        
        public Message() {}
        
	/** Constructs the query message */
        public Message(double Pt_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_) {
            Pt = Pt_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            Pr = 0.0;
        }
                
        public Message(double Pt_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_, double Pr_) {
            Pt = Pt_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            Pr = Pr_;
        }

	/** Constructs the reply message */       
        public Message(double Pr_) {
            Pr = Pr_;
            Xs = 0.00; Ys = 0; Zs = 0; Xr = 0; Yr = 0;  Zr = 0;
            Pt = 0;
        }
        
		/*
        public void duplicate(Object source_)  {
            Message that_ = (Message)source_;
            Pt = that_.Pt; 
            Xs = that_.Xs; Ys = that_.Ys; Zs = that_.Zs;
            Xr = that_.Xr; Yr = that_.Yr; Zr = that_.Zr;
            Pr = that_.Pr; 
        }
		*/
        
        public Object clone() {
            return new Message(Pt, Xs, Ys, Zs, Xr, Yr, Zr, Pr);
        }
        
        public Contract getContract()  {
            return INSTANCE;
        }
        
        public double getXs() { return Xs; }
        public double getYs() { return Ys; }
        public double getZs() { return Zs; }
        public double getXr() { return Xr; }
        public double getYr() { return Yr; }
        public double getZr() { return Zr; }
        public double getPt() { return Pt; }
        
        public double getPr() { return Pr; }
                
        public String toString(String separator_)  {
            return "SensorRadioPropagationQuery: " + separator_ + "(" + Xs + "," + Ys + "," + Zs + ")" + separator_ + " (" + Xr + "," + Yr + "," + Zr + ")";
        }
    }
}
