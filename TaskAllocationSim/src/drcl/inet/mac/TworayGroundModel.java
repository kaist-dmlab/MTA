// @(#)TworayGroundModel.java   1/2004
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
 * This class implements the Two-ray Ground radio propagation model.
 *
 * @see RadioPropagationModel
 * @author Ye Ge
 */
public class TworayGroundModel extends drcl.inet.mac.RadioPropagationModel
{
    
    public TworayGroundModel() {
        super();
    }


    protected synchronized void processPathLossQuery(Object data_) {
        double loss;
        double ht, hr, Lambda, d;
        RadioPropagationQueryContract.Message msg = (RadioPropagationQueryContract.Message) data_;
        
        ht     = msg.getZs();
        hr     = msg.getZr();
        Lambda = msg.getLambda();

        if ( isCartesian ) 
            d    = msg.getDxy();   // it has to be Cartesian Coordinates
        else 
            d    = msg.getDxyz2();

        loss = calculatePathLoss(ht, hr, Lambda, d);
        queryPort.doSending(new RadioPropagationQueryContract.Message(loss));
    }

    /**
     * Calculates path loss according to Tworay Ground model. 
     *
     *@param ht_  the hight of transmitting antenna.
     *@param hr_  the hight of the receiving antenna.
     *@param Lambda_  the wavelength
     *@param d_       the distance on the X-Y plane.
     *
     */
    protected double calculatePathLoss(double ht_, double hr_, double Lambda_, double d_) {
        return TworayGround(ht_, hr_, Lambda_, d_);
    }
    
    
    /**
     * Calculates path loss according to Tworay Ground model. 
     *
     *@param ht_  the hight of transmitting antenna.
     *@param hr_  the hight of the receiving antenna.
     *@param Lambda_  the wavelength
     *@param d_       the distance on the X-Y plane.
     *
     */
    protected double TworayGround(double ht_, double hr_, double Lambda_, double d_)  {
        
        double crossover_dist;
        double loss;
        
        crossover_dist = (4 * Math.PI * ht_ * hr_) / Lambda_;
        
       /*
        *  If the transmitter is within the cross-over range , use the
        *  Friis equation.  Otherwise, use the two-ray
        *  ground reflection model.
        */

        if ( d_ <= crossover_dist ) {
            loss = Friis(Lambda_, d_);
        }
        else {
            loss = Tworay(ht_, hr_, d_);
        }    
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

    protected double Tworay(double ht_, double hr_, double d_)  {
        /*
         *  Two-ray ground reflection model.
         *
         *	     Pt * Gt * Gr * (ht^2 * hr^2)
         *  Pr = ----------------------------
         *           d^4 * L
         *
         * The original equation in Rappaport's book assumes L = 1.
         * To be consistant with the free space equation, L is added here.
         */
        return (hr_ * hr_ * ht_ * ht_) / (d_ * d_ * d_ * d_ );
    }

   	public String info() { 
        return "TworayGroundModel" + "\n"; 
    }
    
}
