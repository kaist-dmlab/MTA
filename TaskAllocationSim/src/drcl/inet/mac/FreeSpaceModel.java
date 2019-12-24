// @(#)FreeSpaceModel.java   1/2004
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

package drcl.inet.mac;

import drcl.comp.Port;
import drcl.comp.Contract;

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
 * This class implements the free space radio propagation model.
 * @author Ye Ge
 */
public class FreeSpaceModel extends drcl.inet.mac.RadioPropagationModel
{
    
    
    public FreeSpaceModel() {
        super();
    }

    
    /**
     * Processes the path loss query message which is sent from the WirelessPhy component.
     * The calculation result is sent back through the queryPort.
     */
    protected synchronized void processPathLossQuery(Object data_) {
        double loss;
        double ht, hr, Lambda, d;
        RadioPropagationQueryContract.Message msg = (RadioPropagationQueryContract.Message) data_;
        
        Lambda = msg.getLambda();
        if ( isCartesian ) 
            d    = msg.getDxyz();
        else 
            d    = msg.getDxyz2();
        loss = calculatePathLoss(Lambda, d);
        queryPort.doSending(new RadioPropagationQueryContract.Message(loss));
    }

    /**
     * Calculate the path loss.
     *
     *@param Lamda_ the wavelength. 
     *@param d_     the distance between the two points. 
     */
    protected double calculatePathLoss(double Lambda_, double d_) {
        return FreeSpace(Lambda_, d_);
    }
    
    
    /**
     * Calculates the path loss using free space model.
     *
     *@param Lamda_ the wavelength. 
     *@param d_     the distance between the two points. 
     *
     */
    protected double FreeSpace(double Lambda_, double d_)  {
        double loss;
        loss = Friis(Lambda_, d_);
        return loss;
    }
    
    
    
    protected double Friis(double Lambda_, double d_)  {
        /*
         * Friis free space equation:
         *
         *       Pt * Gt * Gr * (lambda^2)
         *   P = --------------------------
         *       (4 * pi * d)^2 * L
         */
        
        double M = Lambda_ / (4 * Math.PI * d_);
        return M*M;
    }

   	public String info() { 
        return "FreeSpaceModel" + "\n"; 
    }
    
}
