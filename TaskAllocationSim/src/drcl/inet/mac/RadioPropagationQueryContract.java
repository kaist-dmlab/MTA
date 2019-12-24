// @(#)RadioPropagationQueryContract.java   1/2004
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

import drcl.comp.*;
import drcl.data.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.data.*;

/**
 * The RadioPropagationQueryContract contract. This class defines
 * the message formats used between <code>WirelessPhy</code> and
 * <code>RadioPropagationModel</code>.
 * @author Ye Ge
 */
public class RadioPropagationQueryContract extends Contract {
    
    public static final RadioPropagationQueryContract INSTANCE = new RadioPropagationQueryContract();
    
    public RadioPropagationQueryContract() {
        super();
    }
    
    public String getName() {
        return "Radio Propagation Model Query Contract";
    }
    
    public Object getContractContent() {
        return null;
    }
    
    /**
     *  Constructs a radio propagation query message.
     *
     *@param Lambda_ the wavelength
     *@param Xs_     the x coordinate of the sending node 
     *@param Ys_     the y coordinate of the sending node 
     *@param Zs_     the z coordinate of the sending node 
     *@param Xr_     the x coordinate of the receiving node 
     *@param Yr_     the y coordinate of the receiving node 
     *@param Zr_     the z coordinate of the receiving node 
     *
     */
    public static RadioPropagationQueryContract.Message constructQuery(double Lambda_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_ ) {
        return (new Message(Lambda_, Xs_, Ys_, Zs_, Xr_, Yr_, Zr_));
    }
    
    
    public static class Message extends drcl.comp.Message {
        double Lambda, Xs, Ys, Zs, Xr, Yr, Zr;
        long sid;  //sender's id, add by Honghai
        double loss;
        
        public Message() {}
        
        /**
         *  Constructor.
         *
         *@param Lambda_ the wavelength
         *@param Xs_     the x coordinate of the sending node 
         *@param Ys_     the y coordinate of the sending node 
         *@param Zs_     the z coordinate of the sending node 
         *@param Xr_     the x coordinate of the receiving node 
         *@param Yr_     the y coordinate of the receiving node 
         *@param Zr_     the z coordinate of the receiving node 
         *
         */
        public Message(double Lambda_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_) {
            Lambda = Lambda_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            loss = 0;
            sid = 0; 
        }
        
        //new constructor added by Honghai 
        /**
         *  Constructor.
         *
         *@param Lambda_ the wavelength
         *@param sid_    
         *@param Xs_     the x coordinate of the sending node 
         *@param Ys_     the y coordinate of the sending node 
         *@param Zs_     the z coordinate of the sending node 
         *@param Xr_     the x coordinate of the receiving node 
         *@param Yr_     the y coordinate of the receiving node 
         *@param Zr_     the z coordinate of the receiving node 
         *
         */
        public Message(long sid_, double Lambda_, double Xs_, double Ys_, double Zs_,
                        double Xr_, double Yr_, double Zr_)
        {
            sid = sid_; Lambda = Lambda_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            loss = 0;
        }

        /**
         * Constructor. 
         *
         *@param Lambda_ the wavelength
         *@param Xs_     the x coordinate of the sending node 
         *@param Ys_     the y coordinate of the sending node 
         *@param Zs_     the z coordinate of the sending node 
         *@param Xr_     the x coordinate of the receiving node 
         *@param Yr_     the y coordinate of the receiving node 
         *@param Zr_     the z coordinate of the receiving node 
         *@param loss_   the path loss in the reply
         */
        public Message(double Lambda_, double Xs_, double Ys_, double Zs_, double Xr_, double Yr_, double Zr_, double loss_) {
            Lambda = Lambda_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            loss = loss_;
            sid = 0;
        }

        /**
         * Constructor. 
         *
         *@param Lambda_ the wavelength
         *@param sid_
         *@param Xs_     the x coordinate of the sending node 
         *@param Ys_     the y coordinate of the sending node 
         *@param Zs_     the z coordinate of the sending node 
         *@param Xr_     the x coordinate of the receiving node 
         *@param Yr_     the y coordinate of the receiving node 
         *@param Zr_     the z coordinate of the receiving node 
         *@param loss_   the path loss in the reply
         */
        public Message(long sid_, double Lambda_, double Xs_, double Ys_, double Zs_, 
                        double Xr_, double Yr_, double Zr_, double loss_) {
            Lambda = Lambda_;
            Xs = Xs_; Ys = Ys_; Zs = Zs_;
            Xr = Xr_; Yr = Yr_; Zr = Zr_;
            loss = loss_;
            sid = sid_;
        }

        /**
         * Constructor.
         *
         *@param loss_  the path loss
         */
        public Message(double loss_) {
            loss = loss_;
            Xs = 0; Ys = 0; Zs = 0; Xr = 0; Yr = 0;  Zr = 0;
            Lambda = 0; sid = 0;
        }
        
		/*
        public void duplicate(Object source_)  {
            Message that_ = (Message)source_;
            Xs = that_.Xs; Ys = that_.Ys; Zs = that_.Zs;
            Xr = that_.Xr; Yr = that_.Yr; Zr = that_.Zr;
            loss = that_.loss; sid = that_.sid;
            Lambda = that_.Lambda; sid = that_.sid;
        }
		*/
        
        public Object clone() {
            return new Message(sid, Lambda, Xs, Ys, Zs, Xr, Yr, Zr, loss);
        }
        
        public Contract getContract()  {
            return INSTANCE;
        }
        
        public long   getSid() {return sid; }
        public double getXs() { return Xs; }
        public double getYs() { return Ys; }
        public double getZs() { return Zs; }
        public double getXr() { return Xr; }
        public double getYr() { return Yr; }
        public double getZr() { return Zr; }
        public double getLambda() { return Lambda; }
        
        public double getLoss() { return loss; }
        
        /**
         * calculate the distance of two nodes on the X-Y plane, assuming X and Y are the Cartesian coordinates.  
         */
        public double getDxy() { return Math.sqrt((Xs-Xr)*(Xs-Xr) + (Ys-Yr)*(Ys-Yr)); }

        /**
         * calculate the distance of two nodes. Assume X, Y and Z are the Cartesian coordinates. 
         */
        public double getDxyz() { return Math.sqrt((Xs-Xr)*(Xs-Xr) + (Ys-Yr)*(Ys-Yr) + (Zs-Zr)*(Zs-Zr)); }

        //In the following, we assume x represent longitude, y represent latitude, z altitude.
        /**
         * Calculate the distance of two nodes. Assume x represent longitude, y represent latitude, z represent altitude.
         */
        public double getDxyz2() {
            double radiusOfEarth = 6378000;
            double pi = 3.1415926;
            double degreeToRads = pi / 180; //covert degrees to rads
            double long1 = Xs * degreeToRads;
            double long2 = Xr * degreeToRads;
            double lat1 = Ys * degreeToRads;
            double lat2 = Yr * degreeToRads;
            
            double diffLong = long2 - long1;
            double t1 = Math.sin(lat1) * Math.sin(lat2);
            double t2 = Math.cos(lat1) * Math.cos(lat2) * Math.cos(diffLong);
            double angle = 0;
            if ( (lat1 != lat2) || (long1 != long2) )
                angle = Math.acos(t1 + t2 );
            double minAlt = Math.min(Zs, Zr);
            double d1 = (radiusOfEarth + minAlt) * angle;
            double distance = Math.sqrt(d1 * d1 + (Zs -Zr) * (Zs - Zr));
            
            
            return distance;
        }
            
        public String toString(String separator_)  {
            return "RadioPropagationQuery: " + separator_ + "(" + Xs + "," + Ys + "," + Zs + ")" + separator_ + " (" + Xr + "," + Yr + "," + Zr + ")";
        }
    }
}


