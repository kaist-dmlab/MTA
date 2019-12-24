// @(#)BackoffTimer.java   12/2003
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

import java.math.*; 
import drcl.inet.*;
import drcl.net.*;
import drcl.comp.*;
		 

/**
 * This class simulates the backoff timer in IEEE 802.11 protocol. 
 * This class is ported from ns-2.1b7a
 *
 * @see Mac_802_11
 * @see Mac_802_11_Timer
 * @author Ye Ge
 */
public class BackoffTimer extends Mac_802_11_Timer {
	
    private double difs_wait;

    /**
     * The random number generator. The default seed is set to 111.
     * Remember to use setSeed(long seed) in the script
     * to change the random number generator seed.
     */
    static java.util.Random rand = new java.util.Random(111); // added by Will otherwise the simulation results are not duplicable because each simulation will use a different set of random number sequence.

    ACATimer timer_;
    
	public BackoffTimer(Mac_802_11 h, double s)  {
	    super(h, s);	
		difs_wait = 0.0;
		o_.setType(MacTimeoutEvt.Backoff_timeout); 
	}

    /**
     * Handles the timeout event. Called in Mac_802_11.java when this timer times out.
     */ 
	public void handle( ) {
		busy_ = false;
		paused_ = false;
		stime = 0.0;
		rtime = 0.0;
		difs_wait = 0.0;
	}	
 
    /**
     * Starts the backoff timer.
     */
	public void start(int cw, boolean idle) {
        _assert("BackoffTimer start()", "busy_ == false", (busy_ == false));
        busy_ = true;
	    paused_ = false;
	    
		stime = host_.getTime();

        
//		rtime = (Math.random() * cw) * host_.phymib_.SlotTime;   // shouldn't use random number generator in this way
                                                                 // otherwise the simulation is not repeatable

        rtime = (rand.nextDouble() * cw) * host_.phymib_.SlotTime;  //modified by Will
                                                                    // this is not a good solution either. -- Ye
        

		difs_wait = 0.0;

		if ( idle == false ) {
			paused_ = true;       
        }
		else {
            _assert("BackoffTimer start()", "rtime >= 0.0", (rtime >= 0.0));
            timer_ = host_.setTimeout(o_, rtime);
		}
	}
	
    /**
     * Pauses the backoff timer.
     */
	public void pause( ) {
		
		double rt = stime + difs_wait;
		double sr = host_.getTime() - rt;
        
        int slots = (int) (sr/host_.phymib_.SlotTime);
        if (slots < 0) slots = 0;
		
        _assert("BackoffTimer pause()", "busy_ && ! paused_", (busy_ && ! paused_));
        
	    paused_ = true;
		rtime -= (slots * host_.phymib_.SlotTime);
	    
        _assert("BackoffTimer pause()", "rtime >= 0.0", (rtime >= 0.0));
		difs_wait = 0.0;
        if ( timer_ != null ) host_.cancelTimeout(timer_);
	}

    /**
     * Resumes backoff timer after difs time.
     */
    public void resume(double difs) {
        _assert("BackoffTimer resume()", "rtime >= 0.0", (rtime >= 0.0));

		paused_ = false;
		stime = host_.getTime();

	   /*
		* The media should be idle for DIFS time before we start
		* decrementing the counter, so I add difs time in here.
		*/
		difs_wait = difs;
	
        _assert("BackoffTimer resume()", "rtime + difs_wait >= 0.0", (rtime + difs_wait >= 0.0));
		timer_ = host_.setTimeout(o_, rtime + difs_wait);
	}	

    /**
     * Sets the random number generator seed
     */
    public void setSeed(long seed) {
        rand.setSeed(seed);
    }    
}

