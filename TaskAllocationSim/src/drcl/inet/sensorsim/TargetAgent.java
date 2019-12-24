// @(#)TargetAgent.java   12/2003
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
import drcl.inet.data.*;
import drcl.net.*;
import drcl.util.random.*;

/** This class implements a target node in a wireless sensor network. 
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class TargetAgent extends drcl.net.Module implements drcl.comp.ActiveComponent
{
	public static final int TARGET_GAUSSIAN		= 0 ; 
	public static final int TARGET_SINUSOIDAL	= 1 ; 

	public static final double BCAST_RATE	= 0.01 ;  // every 0.01 secs
	public static final double SAMPLE_RATE	= 400.0 ; // at 400 Hz sampling rate
	public static final double MEAN		= 0.1 ; // gaussian mean
	public static final double VAR		= 0.5 ; // gaussian standard deviation
	public static final double SIN_FREQ	= 40.0 ; // 40 Hz

	public static final double M_PI 	= Math.PI ; /* = 3.14159265358979323846 */

	public static final long SEED_RNG = 10 ;

	ACATimer rTimer;
	int bufIndex;
	double sampleRate;
	double bcastRate;
	double gaussianVar;
	double sinFreq;
	int targetType;
	double sinTime;	
	GaussianDistribution gen;

	public TargetAgent() {
        	super();
		rTimer = null ;
		bufIndex=0;
		sampleRate = SAMPLE_RATE;
		bcastRate = BCAST_RATE;
		sinFreq = SIN_FREQ;
		setTargetType("gaussian", VAR);
		sinTime=0.0;
		gen = null ;
		removeDefaultUpPort();
	}

    public String getName() { return "TargetAgent"; }
    
    public void duplicate(Object source_) 
    {
        super.duplicate(source_);
        TargetAgent that_ = (TargetAgent) source_;
	bufIndex = that_.bufIndex;
	sampleRate = that_.sampleRate;
	bcastRate = that_.bcastRate;
	gaussianVar = that_.gaussianVar;
	sinFreq = that_.sinFreq;
	targetType = that_.targetType;
	sinTime = that_.sinTime;	
	gen = that_.gen;
    }

	/** Sets the sample rate with which the target node generates stimuli. */
	public void setSampleRate(double sampleRate_)
	{	sampleRate = sampleRate_ ; }

	/** Sets the broadcast rate with which the target node generates stimuli. */
	public void setBcastRate(double bcastRate_)
	{	bcastRate = bcastRate_ ; }

	/** Sets how stimuli are generated: gaussian or sinusoidal. */
	public int setTargetType(String type_, double value)
	{
		long s ;
	        if ( type_.equals("gaussian") ) {
			targetType = TARGET_GAUSSIAN ;
			gaussianVar = value ;
			s = SEED_RNG ;
			// Uncomment the following line to get different results in every run
			// s = (long)(Math.abs(java.lang.System.currentTimeMillis()*Math.random()*100)) ;
			gen = new GaussianDistribution(MEAN, gaussianVar, s);
			return 1 ;
	        } else if ( type_.equals("sinusoidal") ) {
			targetType = TARGET_SINUSOIDAL ;
			sinFreq = value ;
			return 1 ;
	        } else {
			System.out.println("TargetAgent.setTargetType ERROR: UNDEFINED TARGET TYPE");
			return -1 ;
	        }
	}

	protected void _start ()  {
		long s ;
		s = SEED_RNG ;
                // Uncomment the following line to get different results in every run
                // s = (long)(Math.abs(java.lang.System.currentTimeMillis()*Math.random()*100)) ;
		gen = new GaussianDistribution(MEAN, gaussianVar, s);
		sendPacket() ;
	}

	protected void _stop()  {
	        if (rTimer != null)
			cancelTimeout(rTimer);
	}

	protected void _resume() {
		rTimer = setTimeout("SendPacket", bcastRate);
	}

	protected synchronized void sendPacket() {
		int sampleSize = (int)(sampleRate * bcastRate) ;

		double payload [] = new double [sampleSize] ;
		double waveFreq ;
		
		for ( int i=0; i< sampleSize; i++){
		    switch ( targetType ) {
			case TARGET_GAUSSIAN:
			      payload[i] = gen.nextDouble();
			      break;
			case TARGET_SINUSOIDAL:
			      waveFreq = 2*M_PI*sinFreq/sampleRate;
			      sinTime = sinTime + (bcastRate/sampleSize);
			      payload[i]=Math.sin(waveFreq*sinTime);
			      break;
			default:
			      break;
		    }
		}	

		downPort.doSending(new TargetPacket(sampleSize, payload)) ;

		// Reset the timer to send a new TargetPacket after bcastRate
		// If commented, the target node will send only one packet	
		rTimer = setTimeout("SendPacket", bcastRate);
	}

	protected synchronized void timeout(Object data_) {
	        if ( data_.equals("SendPacket") ) {
			sendPacket() ;
	        }    
	}
}
