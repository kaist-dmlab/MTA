// @(#)AdaptiveAntenna.java   1/2004
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
 *  inject "create Adaptive Antenna" phy/.antenna@
 *  inject "azimuthpatterns = azimuth_file" phy/.antenna@ (required)
 *  inject "elevationpatterns = elevation_file" phy/.antenna@ (optional, if occur, has to be after the above
 line)"
 * @author Chunyu Hu
 */

public class AdaptiveAntenna extends drcl.inet.mac.Antenna {
    
    public static final String INITIALIZE_PORT_ID = ".test";
    drcl.comp.Port testPort;    
       
    private Antenna.Orientation steeringOrient = new Orientation (0, 0);
     
    /* Creates a new instance of AdaptiveAntenna */
    public AdaptiveAntenna() {
        testPort = addPort (INITIALIZE_PORT_ID, false);
    }
    
    public String QueryType () {    
        return "adaptive array antenna";
    }
 
    /**
      * Get the gain using current activePattern at given angle.
      */
    
    public float getGain_dBi(Orientation orient_)
    {
        if (activePattern == OMNIDIRECTIONAL_PATTERN_INDEX) 
            return omniGain_dBi;
        
        return getGain_dBi_wPatternXsteerOrientX_atOrientX (activePattern, getSteeringOrient(), orient_);
    } //end of getGain
  
    /**
      * Get the gain of pattern <patternIndex_> at direction <orient_>.
      */
    
    public float getGain_dBi_wPatternXsteerOrientX_atOrientX (
            int patternIndex_, Orientation steeringOrient_, Antenna.Orientation orient_)
    {
        if (patternIndex_ <0 || patternIndex_ >= patternsNum) {
            System.out.println (this + "patternIndex = " + patternIndex_ + " out of range!");
            return UNINITIALIZED_GAIN_VALUE;        
        } //endif
        
        if (super.azimuthPatterns_dBi == null) {
            System.out.println (this + ":: patterns uninitialized !");
            return UNINITIALIZED_GAIN_VALUE;
        } //endif
        
        Orientation adjusted_orient = new Antenna.Orientation (
                                        orient_.azimuth - steeringOrient_.azimuth,
                                        orient_.elevation - steeringOrient_.elevation);
        
        float gain_dBi = super.azimuthPatterns_dBi[patternIndex_][adjusted_orient.azimuth];
        
        if (super.elevationPatterns_dBi!= null) {
            gain_dBi += elevationPatterns_dBi[patternIndex_][adjusted_orient.elevation];
        } //endif
        
        return gain_dBi;        
    } //end of getGain_atPatternX_atOrientationX    
    
    /** 
      * Steer the antenna to the desired direction <newOrient_>
      */    
    Antenna.Orientation setSteeringOrient(Orientation newOrient_) 
    {
        Orientation orient = new Antenna.Orientation (newOrient_.azimuth, newOrient_.elevation);
        
        steeringOrient.azimuth = orient.azimuth;
        steeringOrient.elevation = orient.elevation;
        
        return steeringOrient;
    } //end of setSteeringOrient
    
    Antenna.Orientation getSteeringOrient() 
    {
        return steeringOrient;
    } //end of getSteeringOrient

    /** 
     * Find the pattern using which the antenna can receive a signal
     * in the arriving direction with maximum gain.
     */
    public int getPattern_withMaxGain (Orientation orient_)
    {
        int pattern = OMNIDIRECTIONAL_PATTERN_INDEX;
        float gain_dBi = UNINITIALIZED_GAIN_VALUE, tmp;

        for (int i=0; i< patternsNum; i++) {
            if (boreSight_dBi[i] > gain_dBi){
                gain_dBi = boreSight_dBi[i];
                pattern = i;
            } //endif
        } //endfor
        
        if (gain_dBi < this.omniGain_dBi) {
            gain_dBi = omniGain_dBi;
            pattern = OMNIDIRECTIONAL_PATTERN_INDEX;
        }
        
        return pattern;        
    } //end of getPattern_withMaxGain
    
    /* return orient_ on parameter error */
    public Antenna.Orientation getOrient_withMaxGain_atPatternX (Orientation orient_, int patternIndex_)
    {
        if (patternIndex_ <0 || patternIndex_ >= patternsNum) {
            System.out.println (this + "patternIndex = " + patternIndex_ + " out of range!");
            return orient_;
        } //endif

        int azimuth = orient_.azimuth - boreSight_orient[patternIndex_].azimuth;
        int elevation = orient_.elevation - boreSight_orient[patternIndex_].elevation;
        
        return new Antenna.Orientation(azimuth, elevation);
    } //end of getOrient_withMaxGain_atPatternX
    
    public boolean lockAtSignal (Orientation orient_)
    {
        if (super.lockAtSignal() == true) {
            setActivePattern (getPattern_withMaxGain(orient_));
            setSteeringOrient (orient_);
            return true;
        } else
            return false;
            
    } //end of lockAtSignal
    
    public boolean unlock ()
    {
        setSteeringOrient (new Antenna.Orientation(0, 0));
        return super.unlock();
        
    } //end of unlock
    /** Process incoming data*/
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
                        System.out.println ("1 ---- getGain()");
                        System.out.println ("2 ---- setActivePattern()");
                        System.out.println ("3 ---- getActivePattern()");
                        System.out.println ("4 ---- setSteeringOrient()");
                        System.out.println ("5 ---- getSteeringOrient()");
                        System.out.println ("6 ---- getGain_atPatternX_atOrientX()");
                        System.out.println ("7 ---- getPattern_withMaxGain()");
                        System.out.println ("8 ---- getOrient_withMaxGain_atPatternX()");
                        break;
                    case 1: //getGain
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getGain_dBi(" + azimuth + ") = " + getGain_dBi(new Orientation(azimuth, 0)));
                        break;
                    case 2: //setActivePattern
                        pattern = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("setActivePattern("+pattern+") :: "+ setActivePattern(pattern) );
                        break;
                    case 3: //getActivePattern
                        System.out.println ("getActivePattern() = " + activePattern);
                        break;
                    case 4: //setSteeringOrient
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("setSteeringOrient("+azimuth+") = " +
                            setSteeringOrient(new Orientation(azimuth, 0)).azimuth);
                        break;
                    case 5: //getSteeringOrient
                        System.out.println ("getSteeringOrient() = (" + 
                                getSteeringOrient().azimuth + ", "+getSteeringOrient().elevation+")");
                        break;
                    case 6: //getGain_wPatternXsteerOrientX_atOrientX
                        pattern = Integer.valueOf(st.nextToken()).intValue();
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getGain_wPatternXsteerOrientX_atOrientX("+pattern+", "+azimuth+") = "+
                                            getGain_dBi_wPatternXsteerOrientX_atOrientX(
                                            pattern, 
                                            new Orientation(azimuth, 0),
                                            new Orientation(0, 0))
                                            );
                        break;
                    case 7: //getPattern_withMaxGain
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        System.out.println ("getPattern_withMaxGain("+azimuth+") = "+
                                            getPattern_withMaxGain(new Orientation(azimuth, 0)));
                        break;
                    case 8: //getOrient_withMaxGain_atPatternX
                        pattern = Integer.valueOf(st.nextToken()).intValue();
                        azimuth = Integer.valueOf(st.nextToken()).intValue();
                        Orientation orient = getOrient_withMaxGain_atPatternX(new Orientation(azimuth, 0), pattern);
                        System.out.println ("getOrient_withMaxGain_atPatternX() = (" +
                                orient.azimuth + ", "+ orient.elevation + ")");
                        break;
                    default:
                        System.out.println ("Wrong test index!");
                        break;
                } //endswitch            
                return;
        } //endif 
        
        System.out.println (this + " unrecognized port!");        
        
    } //end process

    
}
