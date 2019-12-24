// @(#)SwitchedBeamAntenna.java   1/2004
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

import java.io.*;
import java.lang.*;
import java.util.StringTokenizer;

/**
 * This class is derived from class Antenna.java
 * Before using, it has to be initialized with pattern files. A simple example:
 * "mkdir drcl.inet.mac.WirelessPhy phy
 *  inject "create SwitchedBeam Antenna" phy/.antenna@
 *  inject "azimuthpatterns = azimuth_file" phy/.antenna@ (required)
 *  inject "elevationpatterns = elevation_file" phy/.antenna@ (optional, if occur, has to be after the above line)"
 * @author Chunyu Hu
 */
public class SwitchedBeamAntenna extends drcl.inet.mac.Antenna {
    
    public static final String INITIALIZE_PORT_ID = ".test";
    drcl.comp.Port testPort;    
    
    /** Creates a new instance of SwitchedBeamAntenna */
    public SwitchedBeamAntenna() {
        testPort = addPort (INITIALIZE_PORT_ID, false);        
    }
    
    public String QueryType () {
        return "switched beam antenna";
    }
    
    /**
      * Get the gain using the current activePattern at the given angle.
      */
    
    public float getGain_dBi(Orientation orient_)
    {
        if (activePattern == OMNIDIRECTIONAL_PATTERN_INDEX) 
            return omniGain_dBi;
        
        return getGain_dBi_wPatternX_atOrientX (activePattern, orient_);
    } //end of getGain

    /**
      * Get the gain of pattern <patternIndex_> at direction <orient_>.
      */ 
    public float getGain_dBi_wPatternX_atOrientX(int patternIndex_, Orientation orient_)
    {
        if (patternIndex_ <0 || patternIndex_ >= patternsNum) {
            System.out.println (this + "patternIndex = " + patternIndex_ + " out of range!");
            return UNINITIALIZED_GAIN_VALUE;        
        } //endif
        
        if (super.azimuthPatterns_dBi == null) {
            System.out.println (this + ":: patterns uninitialized !");
            return UNINITIALIZED_GAIN_VALUE;
        } //endif
        
        float gain_dBi = azimuthPatterns_dBi[patternIndex_][orient_.azimuth];
        
        if (super.elevationPatterns_dBi!= null) {
            gain_dBi += elevationPatterns_dBi[patternIndex_][orient_.elevation];
        } //endif
        
        return gain_dBi;
    } //end of getGain_atPatternX_atOrientX
    
    /**
      * Find the pattern using which the antenna can receive a signal
      * in the arriving direction with maximum gain.
      */
    public int getPattern_withMaxGain (Orientation orient_)
    {
        int pattern = OMNIDIRECTIONAL_PATTERN_INDEX;
        float gain_dBi = UNINITIALIZED_GAIN_VALUE, tmp;
        
        for (int i=0; i< patternsNum; i++) {
            tmp = this.getGain_dBi_wPatternX_atOrientX (i, orient_);
            if (tmp > gain_dBi) {
                gain_dBi = tmp;
                pattern = i;
            } //endif
        } //endfor
        if (gain_dBi < omniGain_dBi){
            gain_dBi = omniGain_dBi;
            pattern = OMNIDIRECTIONAL_PATTERN_INDEX;
        }
        return pattern;        
    } //end of getPattern_withMaxGain
    
 	/** Lock an incoming signal from direction */
    public boolean lockAtSignal (Orientation orient_)
    {
        if (super.lockAtSignal() == true) {
            setActivePattern (getPattern_withMaxGain(orient_));
            return true;
        } else
            return false;
        
    } //end of lockAtSignal

    
	/** Process incoming data */
    public void process(Object data_, drcl.comp.Port inPort_)
    {
        String args = ((String)data_).toLowerCase(), value;
        
        if (inPort_ == initPort) {
            super.process(data_, inPort_);
            System.out.println (this + "has " + patternsNum + " patterns.");
            return;
        }
        
        if (inPort_ == testPort) {
                StringTokenizer st = new StringTokenizer(args);
                int index = Integer.valueOf(st.nextToken()).intValue();
                int pattern, azimuth;
                float gain;
                switch (index) {
                    case 0:
                        System.out.println (" ");
                        System.out.println ("0 ---- this menu");
                        System.out.println ("1 ---- getGain_dBi()");
                        System.out.println ("2 ---- setActivePattern()");
                        System.out.println ("3 ---- getActivePattern()");
                        System.out.println ("4 ---- getGain_dBi__atPatternX_atOrientX()");
                        System.out.println ("5 ---- getPattern_withMaxGain()");
                        break;
                    case 1: //getGain
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getGain_dBi(" + azimuth + ") = " + getGain_dBi(new Orientation(azimuth, 0)));
                        break;
                    case 2: //setActivePattern
                        pattern = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("setActivePatterns("+pattern+") :: "+ setActivePattern(pattern) );
                        break;
                    case 3: //getActivePattern
                        System.out.println ("getActivePattern() = " + activePattern);
                        break;
                    case 4: //getGain_atPatternX_atOrientX
                        pattern = Integer.valueOf(st.nextToken()).intValue();
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getGain_dBi_wPatternX_atOrientX("+pattern+", "+azimuth+") = "+
                                            getGain_dBi_wPatternX_atOrientX(pattern, new Orientation(azimuth, 0)));
                        break;
                    case 5: //getPattern_withMaxGain
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getPattern_withMaxGain("+azimuth+") = "+
                                            getPattern_withMaxGain(new Orientation(azimuth, 0)));
                        break;
                    default:
                        System.out.println ("Wrong test index!");
                        break;
                } //endswitch            
                return;
        } //endif 
        
        System.out.println (this + " unrecognized port!");
    } // end of process

}
