// @(#)Antenna.java   1/2004
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
 * This class implements Antenna, which provides the common operations of
 * omni-directional antennas and two types of directional antennas:
 * SwitchedBeamAntenna and AdaptiveArrayAntenna.
 * Based on this class, the two classes for the directional antennas are developed.
 * @author Chunyu Hu
 */
public class Antenna extends drcl.comp.Component
{
/**
  * This class provides a data structure for representing 
  * antennas' direction as (azimuth, elevation) (both in degrees).
  * @author Chunyu Hu
  */
    public static class Orientation {
        int azimuth;
        int elevation;
        public Orientation (int azimuth_, int elevation_)
        {
            azimuth = normalizeAzimuthAngle(azimuth_);
            elevation = normalizeElevationAngle(elevation_);
        }
    }
    
    public static final String INITIALIZE_PORT_ID = ".init";    
    drcl.comp.Port initPort;
       
    public static int ANTENNA_ANGLE_RESOLUTION = 360;
    public static float UNINITIALIZED_GAIN_VALUE = -999;
    public static int OMNIDIRECTIONAL_PATTERN_INDEX = -1;
    
    float height = 1.5f;
    float omniGain_dBi = 0;
    int patternsNum = 0;
    float azimuthPatterns_dBi[][] = null;
    float elevationPatterns_dBi[][] = null;
    float boreSight_dBi[] = null;
    Orientation boreSight_orient[] = null;
    protected int activePattern = OMNIDIRECTIONAL_PATTERN_INDEX; 
    private boolean locked = false;
   
    public Antenna() {
        super ("antenna");
        initPort = addPort (INITIALIZE_PORT_ID, false);
    }   
    
    public String QueryType () {
        return "OMNIDIRECTIONAL ANTENNA";
    }
    
    float setHeight (float height_) 
    {
        if (height_ < 0) {
            System.out.println ("Antenna height cannot be smaller than 0!");
            return height;
        }
        height = height_;
        return height;
    } //end setHeight
    
    float setOmniGain_dBi (float omniGain_dBi_) {
        omniGain_dBi = omniGain_dBi_;
        return omniGain_dBi;
    } //end setOmniGain_dBi
    
    /**
      * Return omnidirectional gain in dBi.
      */
    public float getGain_dBi ()
    {
        return omniGain_dBi;
    }
    /**
      * Return gain in dBi in given direction.
      * To be overrided by child class.
      */
    public float getGain_dBi (Orientation orient_)
    {
        return getGain_dBi();
    }
    
    /**
      * Set the pattern index by <patternIndex_> active.
      */
    public int setActivePattern(int patternIndex_)
    {
        if ( (patternIndex_ < 0 || patternIndex_ >= patternsNum)
            && patternIndex_!= OMNIDIRECTIONAL_PATTERN_INDEX ){
            System.out.println (this + "patternIndex = " + patternIndex_ + " out of range!");
        } else
            activePattern = patternIndex_;
        
        return activePattern;
    } //end of setActivePattern
    
    public int getActivePattern()
    {
        return activePattern;
    } //end of getActivePattern   
    
    /**
      * Lock an incoming signal.
      * To be overwritten by child class -- switched beam antenna or adaptive array antenna.
      */
    public boolean lockAtSignal ()
    {
        if (locked == true) return false;        
        
        locked = true;
        return true;
        
    } //end of lockAtSignal
    
    /** 
      * Lock an incoming signal from direction <orient_>.
      * To be overriden by child class.
      */
    public boolean lockAtSignal (Orientation orient_)
    {
        return lockAtSignal();
    } //end of lockAtSignal
   
    /**
      * Unlock the current locked signal.
      */ 
    public boolean unlock ()
    {
        boolean old_status = locked;
        setActivePattern (OMNIDIRECTIONAL_PATTERN_INDEX);
        locked = false;
        
        //System.out.println ("Antenna unlock !"); //for debug
        
        return old_status;
    } //end of unlock
    
    public boolean isLocked ()
    {
        return locked;
    } //end of isLocked

    /**
     * Function: 
     *  Parse the pattern file, write the patterns into patterns_dBi_
     *  and return number_of_patterns.
     * Parameters: 
     *  index = 0, read patterns for azimuthPatterns_dBi[][]
     *  index = 1, ................. elevationPatterns_dBi[][]    
     * Return: 
     *  -1 fail in parsing the file, either beacause failing access or format error ..
     *  else number_of_patterns read into patterns_dBi_.
     */
    private int readPatterns(String filename_, Antenna ant_, int index)
    {
        int patterns = 0;
        float patterns_dBi[][];
        
        try {
            BufferedReader in = 
                new BufferedReader (new FileReader(filename_));
            String s_in, s_proc;
            boolean flag = false;            
            
            // 1. "number_of_patterns = ..."
            while (!flag && (s_in = in.readLine())!=null ) {
                s_proc = s_in.toLowerCase();
                if ( s_proc.indexOf ("number")!= -1 && s_proc.indexOf ("patterns")!=-1 ) {
                    flag = true;
                    s_proc = s_proc.substring(s_proc.lastIndexOf('=') + 1);
                    if (s_proc.equalsIgnoreCase(null)) {
                        System.out.println (this + ":: error in formatting number_of_patterns!");
                        in.close();
                        return -1;
                    } //endif
                    
                    StringTokenizer st = new StringTokenizer(s_proc.trim());
                    patterns = Integer.valueOf(st.nextToken()).intValue();
                } //endif
            } //end of while

            // check what we get with "number_of_patterns"
            if (!flag) {
                System.out.println (this + ":: <number_of_patterns> not found!");
                in.close();
                return -1;
            } //endif            
            if (patterns <= 0) {
                System.out.println (this + ":: <number_of_patterns> = "+ patterns);
                in.close();
                return patterns;
            } //endif          
            //System.out.println (this + "number_of_patterns = " + patterns);            
            
            // initialize the pattern array
            patterns_dBi = new float [patterns][ANTENNA_ANGLE_RESOLUTION];
            for (int i=0; i< patterns; i++) for (int j=0; j< ANTENNA_ANGLE_RESOLUTION; j++)
                patterns_dBi[i][j] = UNINITIALIZED_GAIN_VALUE;
            
            // fill out the pattern array
            int patternIndex = 0, angleIndex = 0;
            float gain_dBi;
            String token;
            while ( (s_in= in.readLine())!=null ) {
                if (s_in.length()==0) continue;
                
                StringTokenizer tz = new StringTokenizer(s_in);
                patternIndex = Integer.valueOf(tz.nextToken()).intValue();
                angleIndex = Integer.valueOf(tz.nextToken()).intValue();
                //if (index == 1) angleIndex += ANTENNA_ANGLE_RESOLUTION/2;
                gain_dBi = Float.valueOf(tz.nextToken()).floatValue();
                
                // for debug
                // System.out.println ("patternIndex = "+ patternIndex + ", angleIndex = " + angleIndex + ", gain_dBi = " + gain_dBi + ".");
                 if ( patternIndex <0 || patternIndex >= patterns || angleIndex <0 || angleIndex >= ANTENNA_ANGLE_RESOLUTION) {
                     System.out.println ("<patternIndex, angleIndex> out of scopes!");
                     in.close();
                     return -1;
                 } //endif
                 
                 patterns_dBi[patternIndex][angleIndex] = gain_dBi;
            } //endwhile
            in.close();
            
            // fill up the gap between discontinuity of angles
            int last_angle;
            for (int i=0; i < patterns; i++ ) {
                last_angle = 0;
                for (int j=0; j < ANTENNA_ANGLE_RESOLUTION; j++) {
                    while (j< ANTENNA_ANGLE_RESOLUTION && patterns_dBi[i][j] == UNINITIALIZED_GAIN_VALUE) j++;
                    if (last_angle == 0 && j == ANTENNA_ANGLE_RESOLUTION) {
                        System.out.println (this + ":: Pattern " + i + " uninitialized!!");
                        in.close();
                        return -1;
                    } //endif
                    for (int k= last_angle; k<j; k++) { // fill the gap
                        patterns_dBi[i][k] = patterns_dBi[i][last_angle];
                    }//endfor k
                    last_angle = j;                    
                } // endfor j                
            } //endfor i            
            
        }catch (java.io.IOException e) {
            System.out.println (this + ":: error in reading " + filename_);
            return -1;
        }
        
        if (index == 0) ant_.azimuthPatterns_dBi = patterns_dBi;
        else ant_.elevationPatterns_dBi = patterns_dBi;
               
        return patterns;
    } //end of readPatterns
    
    public boolean initAzimuthPatterns(String filename_)
    {  
        int num = readPatterns (filename_, this, 0);
        if (num <= 0 ) return false;

        /* for debug
        for (int i=0; i< num; i++) for (int j=0; j< ANTENNA_ANGLE_RESOLUTION; j++)
            System.out.println ("PATTERN " + i + " ANGLE " + j + ": " + azimuthPatterns_dBi[i][j]);
         */
        patternsNum = num;
        
        // initialize boresight
        boreSight_dBi = new float [patternsNum];
        boreSight_orient = new Antenna.Orientation [patternsNum];

        float highestGain;
        int highestAzimuth;
        for (int i=0; i<patternsNum; i++) {
            highestGain = UNINITIALIZED_GAIN_VALUE;
            highestAzimuth = 0;
            for (int j=0; j< ANTENNA_ANGLE_RESOLUTION; j++) {
                if (azimuthPatterns_dBi[i][j] > highestGain) {
                    highestGain = azimuthPatterns_dBi[i][j];
                    highestAzimuth = j;
                } //endif
            }//endfor j
            if (highestGain == UNINITIALIZED_GAIN_VALUE) {
                System.out.println (this + "error in initializing boreSight[][] (fcn. initAzimuthPatterns)");
                return false;
            } //endif

            boreSight_dBi[i] = highestGain;
            boreSight_orient[i] = new Antenna.Orientation(highestAzimuth, 0);
	    //System.out.println ("Pattern "+i + " boreSight_dBi= "+ boreSight_dBi[i] + " azimuth= " + boreSight_orient[i].azimuth);
    
        } //endfor i        

        return true;
    } //end of initAzimuthPatterns
    
    public boolean initElevationPatterns(String filename_)
    {
        if (boreSight_dBi==null) {
            System.out.println (this + " must initialize azimuth patterns first!");
	    return false;
        } //endif

        int num = readPatterns (filename_, this, 1);
        if (num <= 0) return false;
        if (num!= patternsNum) {
            elevationPatterns_dBi = null;
            System.out.println (this + " unequal numbers of azimuth_patterns and elevation_patterns!\n");
            return false;
        }       
        
        float highestGain;
        int highestElevation;
        
        for (int i=0; i< patternsNum; i++) {
            highestGain = UNINITIALIZED_GAIN_VALUE;
            highestElevation = 0;
            for (int j=0; j< ANTENNA_ANGLE_RESOLUTION; j++) {
                if (elevationPatterns_dBi[i][j] >highestGain) {
                    highestGain = elevationPatterns_dBi[i][j];
                    highestElevation = j;
                } //endif
            } //endfor j
            if (highestGain == UNINITIALIZED_GAIN_VALUE) {
                System.out.println (this + "error in initializing boreSight[][] (fcn. initElevationPatterns)");
                return false;
            } //endif            
            boreSight_dBi[i] += highestGain;
            boreSight_orient[i].elevation = highestElevation;
            //System.out.println ("Pattern "+i + " boreSight_dBi= " + boreSight_dBi[i] + " elevation= "+ boreSight_orient[i].elevation);
        } //endfor i
        
        return true;
    } //end of initElevationPatterns


/**
 * Normalize azimuth angle to 360 degree
 */
    public static int normalizeAzimuthAngle (int angle)
    {
        while (angle < 0) angle += ANTENNA_ANGLE_RESOLUTION;
        
        if (angle > ANTENNA_ANGLE_RESOLUTION)
            angle %= ANTENNA_ANGLE_RESOLUTION;
        
        return angle;
        
    } //end of normalizeAzimuthAngle
    
/**
 * Normalize elevation angle to 360 (not +/-180 any more) degree
 */    
    public static int normalizeElevationAngle (int angle)
    {
         while (angle < 0) angle += ANTENNA_ANGLE_RESOLUTION;
        
        if (angle > ANTENNA_ANGLE_RESOLUTION)
            angle %= ANTENNA_ANGLE_RESOLUTION;
        
        return angle;
    
    } 
    
        /** Process incoming data
	 	 * @param data_ = name of object to be initialized. 
         * 1. data_ = "height = ...", case insensitive         
         * 2. data_ = "omniGain_dBi = ...", case insensitive
         * 3. data_ = "azimuthPatterns = <filename_>", case insensitive
         * 4. data)->item = "elevationPatterns = <filename_>", case insensitive
         */            
    public void process(Object data_, drcl.comp.Port inPort_)
    {
        String args = ((String)data_).toLowerCase(), value;
        int index;
        
        if (inPort_ == initPort){
            
            if ( (index = args.indexOf('=')) == -1) {
                System.out.println (this + ":: pls. use the format such as <height = 1.5>");
                return;
            } //endif index
            
            value = (args.substring(index+1)).trim();            
            if (value.equals(null)) {
                System.out.println (this + ":: pls. use the format such as <height = 1.5>");
                return;               
            } //endif value
            
            if (args.indexOf ("height")!=-1) {
                height = Float.parseFloat(value);
                System.out.println ("set height = " + height);
                return;
            } //endif "height"
            
            if (args.indexOf ("omnigain_dbi")!=-1) {
                omniGain_dBi = Float.parseFloat(value);
                System.out.println ("set omniGain_dBi = " + omniGain_dBi);
                return;
            } //endif "omniGain_dBi"
            
            if (args.indexOf ("azimuthpatterns")!=-1) {
                try {
                    BufferedReader in = 
                        new BufferedReader (new FileReader(value));
                    in.close();
                } catch (java.io.IOException e) {
                    System.out.println (this + ":: error in opening " + value);
                    return;
                } //endtry
                
                if (initAzimuthPatterns (value))
                    //System.out.println (this + "Successfully initialize the azimuth pattern file!");
                return;
            } //endif "azimuthPatterns"
            
            if (args.indexOf ("elevationpatterns")!=-1) {
                try {
                    BufferedReader in = 
                        new BufferedReader (new FileReader(value));
                    in.close();
                } catch (java.io.IOException e) {
                    System.out.println (this + ":: error in opening " + value);
                    return;
                } //endtry
                
                if (initElevationPatterns (value)) {
                    System.out.println (this + "Successfully initialize the elevation pattern file!");                    
                }
                return;
            } //endif "elevationPatterns"
            
            System.out.println (this + ":: wrong format input!");            
        }
    } //end of process
    
} //end of class Antenna
