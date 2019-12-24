// @(#)RadioPropagationModel.java   7/2003
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

package drcl.inet.mac;


import drcl.comp.Port;
import drcl.comp.Contract;
import drcl.inet.mac.*;

/*
 * Free space model 
 *       
 *      Pr(d) = Pt*Gt*Gr*Lambda^2/((4Pi)^2*d^2*L
 * 
 *      loss(d) = Lambda^2/(4*pi*d)^2
 *
 * Two-ray ground model
 *
 *      Pr(d) = Pt*Gt*Gr*ht^2*hr^2/(d^4*L)
 *
 *      loss(d) = (ht*hr)^2/d^4
 *  
 *  dc = (4*Pi*ht*hr)/Lambda
 *
 *  if ( d < dc ) 
 *      loss(d) = Lambda^2/(4*pi*d)^2
 *  else
 *      loss(d) = (ht*hr)^2/d^4
 * 
 *  dc:      cross-over distance
 *  Lambda:  wavelength
 *  d:       distance between the transmitter and the receiver
 *  ht:      the height of the anttenna
 *  hr:      the height of the anttenna
 *  L:       system loss, L == 1
 */

/**
 * This is the base class of all different radio propagation models.
 * @author Ye Ge
 */
public class RadioPropagationModel extends drcl.net.Module
{
    /**
     * the port receiving path loss query 
     */
    protected Port queryPort = this.addServerPort(".query");

    protected static boolean isCartesian = true;

    /** 
     * Constructor.
     */
    public RadioPropagationModel()
	{
		super();
	}

    /** 
     * Constructor.
     */
    public RadioPropagationModel(String id_)
	{
		super(id_);
	}

    /**
     * Sets the flag to indicate whether to use Cartesian coordinates or not.
     */
    public static void useCartesianCoordinates(boolean b) {
        isCartesian = b;
    }
    /**
     * Gets the flag of using Cartesian coordinates or not.
     */
    public static boolean isCartesianCoordinates() {
        return isCartesian ;
    }

 	protected synchronized void processOther(Object data_, Port inPort_)
	{
		
        if (inPort_ != queryPort) {
			super.processOther(data_, inPort_);
			return;
		}
        
        if ( ! (data_ instanceof RadioPropagationQueryContract.Message) ) { 
            error(data_, "processOther()", inPort_, "unrecognized data type");
			return;
		}
        processPathLossQuery(data_);
    }    

    /** Precesses a process path loss query and sends back reply through the query port.  */
    protected void processPathLossQuery(Object data_) {
        double loss;
        
        RadioPropagationQueryContract.Message msg = (RadioPropagationQueryContract.Message) data_;
        
        loss = calculatePathLoss();
        queryPort.doSending(new RadioPropagationQueryContract.Message(loss));
    }
    
    /**
     * This should be override by its sub class.
     */
    protected double calculatePathLoss() {
        return 1.0;
    }    
    
   	public String info() { 
        return "RadioPropagationModel" + "\n"; 
    }      

}

