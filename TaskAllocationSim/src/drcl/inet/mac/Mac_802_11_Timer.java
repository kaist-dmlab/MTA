// @(#)Mac_802_11_Timer.java   7/2003
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
 * This is the base class of various IEEE802.11 timer classes. 
 *
 * @see Mac_802_11
 * @author Ye Ge
 */
public class Mac_802_11_Timer {

	MacTimeoutEvt   o_;
	
	boolean     busy_;
	boolean  	paused_;
	/** Start time */
	double		stime;	
	/** Remaining time */
	double		rtime;	
	double		slottime;
	
	Mac_802_11  host_;

    ACATimer timer_;
    
    
    public Mac_802_11_Timer(Mac_802_11 h, double s) {
		busy_    = false;
		paused_  = false; 
		stime    = 0;
		rtime    = 0.0;
		slottime = s;
		
		host_     = h;
		o_ = new MacTimeoutEvt(MacTimeoutEvt.Testing_timeout);
	}
	
	public Mac_802_11_Timer(Mac_802_11 h) { 
		this(h, 1.0); 
	}	

	/**
	  * Start the timer 
	  * @param time - duration
	  * @param ctime - start timer
	  */
	public void start(double time, double ctime) {
        _assert("Mac_802_11_Timer start()", "busy_ == false", (busy_ == false));
		
      	busy_ = true;
		paused_ = false;
	    stime = ctime;
		rtime = time;
		
		_assert("Mac_802_11_Timer start()", "rtime >= 0.0", (rtime >= 0.0));
		
		timer_ = host_.setTimeout(o_, rtime);
	}

	/**
	  * Start the timer 
	  * @param time - duration 
	  */
	public void start(double time) {
        _assert("Mac_802_11_Timer start()", "busy_ == false", (busy_ == false));
		busy_ = true;
		paused_ = false;
	    stime = host_.getTime();
		rtime = time;
        _assert("Mac_802_11_Timer start()", "rtime >= 0.0", (rtime >= 0.0));
		
		timer_ = host_.setTimeout(o_, rtime);
	}
	
	public void stop( ) {
        _assert("Mac_802_11_Timer stop()", "busy_ == true", (busy_ == true));

		if (paused_ == false && timer_ != null) 
		    host_.cancelTimeout(timer_);

		busy_ = false;
		paused_ = false;
		stime = 0.0;
		rtime = 0.0;
	}	
	
	public void pause( ) {}
	public void resume( ) {}
	public void handle( ) {};

	public boolean busy( )   { return busy_;   }
	public boolean paused( ) { return paused_; }
	public double expire( ) {
		return ((stime + rtime) - host_.getTime());
	}
    
    protected void _assert(String where, String why, boolean continue_) {
        if ( continue_ == false ) 
            drcl.Debug.error(where, why, true);
        return;
    }    
}

