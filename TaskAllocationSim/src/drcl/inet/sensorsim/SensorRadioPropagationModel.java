// @(#)SensorRadioPropagationModel.java   12/2003
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

/** Abstract base class for propagation models over the sensor channel.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public abstract class SensorRadioPropagationModel extends drcl.net.Module
{
    /**  Port that receives signal strength query */
    protected Port queryPort = this.addServerPort(".query");

	{
        	removeDefaultUpPort();
        	removeDefaultDownPort();
        	removeTimerPort();
	}

    public SensorRadioPropagationModel()
	{
		super();
	}

    public SensorRadioPropagationModel(String id_)
	{
		super(id_);
	}

 	protected synchronized void processOther(Object data_, Port inPort_)
	{		
        if (inPort_ != queryPort) {
			super.processOther(data_, inPort_);
			return;
		}
        
        if ( ! (data_ instanceof SensorRadioPropagationQueryContract.Message) ) { 
            error(data_, "processOther()", inPort_, "unrecognized data type");
			return;
		}
        processReceivedSignalStrengthQuery(data_);
    }    

	/** Replies with the calculated received signal strength  */
    protected synchronized void processReceivedSignalStrengthQuery(Object data_) {
        double Pr = calculateReceivedSignalStrength(data_);
        queryPort.doSending(new SensorRadioPropagationQueryContract.Message(Pr));
    }
    
    /** Calculates the received signal strength  */
    // to be overridden by the derived classes
    protected synchronized double calculateReceivedSignalStrength(Object data_) 
    {
        return 1.0;
    }    
    
   	public String info() { 
        return "SensorRadioPropagationModel" + "\n"; 
    }      

}
