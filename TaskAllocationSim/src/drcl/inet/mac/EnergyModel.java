// @(#)EnergyModel.java   1/2004
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

/**
 * The class implements a simple energy model.
 * @author Rong Zheng
 */
public class EnergyModel extends drcl.DrclObj {
    /** Creates a new instance of EnergyInfo */
    double Pt = 0.2818;		        // transmitter signal power (W)
    double Pt_consume = 0.660;	    // power consumption for transmission (W)
    double Pr_consume = 0.395;	    // power consumption for reception (W)
    double P_idle  = 0.0;          // idle power consumption (W)
    double P_off = 0.043;
    double P_sleep = 0.130;
    
    boolean isOn = true;
    boolean isSleep = false;
    
    double energy = 1000;
    public EnergyModel() {
    }
    
	/** Set energy consumption 
	  * @param Pt_ signal power
	  * @param Pt_consume_ power consumption of transmission
	  * @param Pr_consume_ power consumption for reception
	  * @param P_idle_ idle power consumption
	  * @param P_off_  shutdown energy consumption
	  */
    public void setEnergyConsumption(double Pt_, double Pt_consume_, double Pr_consume_, double P_idle_, double P_off_) {
        Pt = Pt_;
        Pt_consume = Pt_consume_;
        Pr_consume = Pr_consume_;
        P_idle = P_idle_;
        P_off = P_off_;
    }

	public String toString()
	{
		return "EnergyModel:t_signal_power=" + Pt + ",t_consume=" + Pt_consume + ",r_consume=" + Pr_consume + ",idle_consume=" + P_idle + ",P_off=" + P_off;
	}
    
    protected void setOn(boolean is_on) {
        isOn = is_on;
    }
    
    protected void setSleep(boolean is_sleep) {
        isSleep = is_sleep;
    }
    
    protected void setEnergy(double energy_) {
        energy = energy_;
    }
    
    protected boolean updateIdleEnergy(double time) {
        energy -= P_idle*time;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        
        return isOn;
    }
    
    protected boolean updateTxEnergy(double time) {
        energy -= Pt_consume*time;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        
        return isOn;
    }
    
    protected boolean updateRxEnergy(double time) {
        energy -= Pr_consume*time;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        
        return isOn;
    }
    
    protected boolean updateSleepEnergy(double time) {
        energy -= P_sleep*time;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        
        return isOn;
    }
    
    protected boolean getOn() {
        return isOn;
    }
    
    protected boolean getSleep() {
        return isSleep;
    }
    
    protected double getEnergy() {
        return energy;
    }
}
