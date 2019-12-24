// @(#)Globe.java   1/2004
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


import java.lang.*;
import java.io.*;     //for IOException
import java.util.*;   // for StringTokenizer
import java.text.*;   //for NumberFormat

/**
 * This class extracts the altitude of any place on earth from the Globe database. 
 <br>
          WARNING: there is some code that MAY need to be changed depending on your
                   computer system.  This is because the GLOBE data base is
                   stored in a binary format and unix systems must byte swap
                   the data.  PCs do not.  The code can be found in subroutine
                   getGlobeData.  Follow instructions in the comments.
				   This implementation has been tested on PCs with both Linux and Windows
				   operating systems.  If you are not sure if it extracts the right data
				   on your computer system, use GlobeTest in this package to test it.
 * @author Honghai Zhang 
 * @see drcl.inet.mac.GlobeTest 
 */

public class Globe {

	/**
	  * Constructs a Globe with transmitter and receiver's locations, number of sampling points 
	  * and path containing the globe database.
	 */ 
    public Globe(double tx_lat, double tx_lon, double rx_lat, double rx_lon,
           int nPts, String globepath) {
        txLat = tx_lat;
        txLon = tx_lon;
        rxLat = rx_lat;
        rxLon = rx_lon;
        nPoints = nPts;
        globePath = globepath;
        
        
        lastTile = -1;
        globeFile = null;
    }

	/**
	  * Constructs a Globe with the path containing the globe database.
	  */

    public Globe(String globepath) {
        txLat = 0;
        txLon = 0;
        rxLat = 0;
        rxLon = 0;
        nPoints = 0;
        globePath = globepath;
        
        lastTile = -1;
        globeFile = null;
        
    }

	/**
	  * Reset transmitter and receiver's locations and number of sampling points.
	  */
    
    public void resetPosition(double tx_lat, double tx_lon, double rx_lat, double rx_lon, int nPts)
    {
        txLat = tx_lat;
        txLon = tx_lon;
        rxLat = rx_lat;
        rxLon = rx_lon;
        nPoints = nPts;
    }
    
/******************************************************************
           Extract a path profile array from Tx to Rx from the GLOBE data base.
           Elevation points are in meters.  The great circle SHORTEST path
           will be extracted from the data base.  Longitude values should be
           in the range [-180 to +180 degrees], with +lon=East. <br>
           pfl = array to fill (result will be in meters)
          <blockquote>     
				 pfl(0) = number of points to get between Tx and Rx<br>
                 pfl(1) = distance between points (meters)
                          thus, (pfl(0)-1)*pfl(1)=distance between Tx and Rx <br>
                 pfl(2) = Tx elevation in meters <br>
                 pfl(npoints+1) = Rx elevation in meters.  
			</blockquote>
           Return positive if no GLOBE data exists or error in Globe data file. <br>
           Written for the NOAA's Globe Version 1.0 elevation data. 
*/
    public int get_pfl(double [] pfl) 
    {
        double delta;
        
        ztht = GlobeElevation(txLon,txLat); // height at Tx
        if (ztht < -500) return 1;          //error in Globe data file
        zrht = GlobeElevation(rxLon, rxLat); //height at Rx
        if (zrht < -500) return 2;
        ztlat = txLat;
        ztlon = txLon;
        zrlat = rxLat;
        zrlon = rxLon;
        
        dazel(0);    //calc ztaz & zdgc
        pfl[0] = nPoints;
        delta = zdgc/(double)(nPoints -1);
        pfl[1] = delta * 1000;
        pfl[2] = ztht;
        pfl[nPoints+1] = zrht;
        NumberFormat nf = NumberFormat.getNumberInstance();
        //System.out.println("true  = "+ zdgc);
        
        for (int i = 3; i <= nPoints; i++)
        {
            zdgc = (double)(i-2) * delta;
            dazel(1);                           // calc zrlat, zrlon
            
            //System.out.println(i+ " " + nf.format(zrlon) + " " + nf.format(zrlat) );
            double zz = GlobeElevation(zrlon, zrlat);  // height  at point
            
            if (zz < -500) return i;
            pfl[i] = zz;
        }
               
        return 0;
    }
    
/**
  void DAZEL(MODE)             Great circle calculations.

     TWO MODES--   0   INPUT LAT AND LON OF END POINT
                       RETURN DISTANCE AND AZIMUTH TO END PT WITH ELEVATIONS
                   1   INPUT BEARING (AZIMUTH) OF END POINT
                       RETURN LAT AND LON OF END POINT WITH ELEVATIONS

   MODE 0
   INPUT PARAMETERS (THESE DEFINE LOCATION OF POINTS T (TRANSMITTER)
     AND R (RECEIVER) RELATIVE TO A SPHERICAL EARTH.
     ZTLAT - LATITUDE (DECIMAL DEGREES NORTH OF EQUATOR) OF POINT T
     ZTLON - LONGITUDE (DECIMAL DEGREES EAST OF PRIME (GREENWICH)
            MERIDIAN) OF POINT T
     ZTHT  - HEIGHT (METERS ABOVE MEAN SEA LEVEL) OF POINT T
     ZRLAT - LATITUDE (DECIMAL DEGREES NORTH OF EQUATOR) OF POINT R
     ZRLON - LONGITUDE (DECIMAL DEGREES EAST OF PRIME MERIDIAN OF POINT R
     ZRHT  - HEIGHT (METERS ABOVE MEAN SEA LEVEL) OF POINT R

   OUTPUT PARAMETERS
     ZTAZ  - AZUMUTH (DECIMAL DEGREES CLOCKWISE FROM NORTH) AT T OF R
     ZRAZ  - AZIMUTH (DECIMAL DEGREES CLOCKWISE FROM NORTH) AT R OF T
     ZTELV - ELEVATION ANGLE (DECIMAL DEGREES ABOVE HORIZONTAL AT T
            OF STRAIGHT LINE BETWEEN T AND R
     ZRELV - ELEVATION ANGLE (DECIMAL DEGREES ABOVE HORIZONTAL AT R)
            OF STRAIGHT LINE BETWEEN T AND R
     ZTAKOF - TAKE-OFF ANGLE (DECIMAL DEGREES ABOVE HORIZONTAL AT T)
            OF REFRACTED RAY BETWEEN T AND R (ASSUMED 4/3 EARTH RADIUS)
     ZRAKOF - TAKE-OFF ANGLE (DECIMAL DEGREES ABOVE HORIZONTAL AT R)
            OF REFRACTED RAY BETWEEN T AND R (ASSUMED 4/3 EARTH RADIUS)
     ZD    - STRAIGHT LINE DISTANCE (KILOMETERS) BETWEEN T AND R
     ZDGC  - GREAT CIRCLE DISTANCE (KILOMETERS) BETWEEN T AND R

   MODE 1
   INPUT PARAMETERS                    OUTPUT PARAMETERS
     ZTLAT                                ZRLAT
     ZTLON                                ZRLON
     ZTAZ                                 RELEV,ZRAKOF
     ZDGC                                 TELEV,ZTAKOF


     ALL OF THE ABOVE PARAMETERS START WITH THE LETTER Z AND ARE SINGLE
     PRECISION.  ALL PROGRAM VARIABLES ARE DOUBLE PRECISION.
     PROGRAM IS UNPREDICTABLE FOR SEPARATIONS LESS THAN 0.00005 DEGREES,
     ABOUT 5 METERS.

*/
    void dazel(int mode)
    {
        double pi = 3.141592653589793238462643;
        double rerth = 6370;
        double dtor = 0.01745329252;
        double rtod = 57.29577951;
        
        //temporary variables
        double tlats, tlons,thts, rlats, rlons, rhts;
        double delat, delon, adlat,adlon,delht ;
        double gc, sgc, d;
        double p;
        
        if (mode == 0) {
            tlats = ztlat;
            tlons = ztlon;
            thts = ztht * 1.E-3;
            rlats = zrlat;
            rlons = zrlon;
            rhts = zrht * 1.0E-3;
            
            if (tlats <= - 90.0) tlats = -89.99;
            if (tlats >= 90.0 )  tlats = 89.99;
            if (rlats <= -90.0)  rlats = -89.99;
            if (rlats >= 90.0 )  rlats = 89.99;
            
            delat = rlats - tlats;
            adlat = Math.abs(delat);
            delon = rlons - tlons;
            while (delon < -180.0) delon += 360;
            while (delon > 180.0) delon -= 360;
            
            adlon = Math.abs(delon);
            delht = rhts - thts;
            if (adlon <= 1.0E-5) {
                if (adlat <= 1.E-5) {
                    //point T and R have the same coordinate
                    ztaz = 0;
                    zraz = 0;
                    if (delht < 0) {
                        ztelv = -90;
                        zrelv = 90;
                        zd = - delht;
                        zdgc = 0.0;
                    }
                    else if (delht == 0.0) {
                        ztelv = 0.0;
                        zrelv = 0.0;
                        zd = 0.0;
                        zdgc = 0.0;
                    }
                    else {
                        ztelv = 90;
                        zrelv = -90;
                        zd  = delht;
                        zdgc = 0.0;
                    }
                    return;
                }
                else {
                    //point T and R has same longitude, distinct latitudes
                    if (delat <= 0.0) {
                        ztaz = 180.0;
                        zraz = 0.0;
                    }
                    else {
                        ztaz = 0.0;
                        zraz = 180;
                    }
                    gc = adlat * dtor;
                    sgc = Math.sin(0.5*gc);
                    d = Math.sqrt(delht * delht + 4.0 * (rerth + thts) * (rerth + rhts) * sgc * sgc);
                    zd = d;
                    zdgc = gc * rerth;
                }
            }
            else {
                //point R and T have distince longtitudes
                double wlat, elat;
                if (delon <= 0.0) {
                    wlat = rlats * dtor;
                    elat = tlats * dtor;
                }
                else {
                    wlat = tlats * dtor;
                    elat = rlats * dtor;
                }
                
                //calculate azimuths at points W and E
                double sdlat, sdlon, sadln, celat,cwlat;
                double cwaz, swaz, waz,ceaz, seaz, eaz;
                sdlat = Math.sin(0.5 * adlat *dtor);
                sdlon = Math.sin(0.5 * adlon*dtor);
                sadln = Math.sin(adlon * dtor);
                cwlat = Math.cos(wlat);
                celat = Math.cos(elat);
                p = 2.0 * (sdlat * sdlat + sdlon * sdlon * cwlat * celat );
                sgc = Math.sqrt(p * (2.0-p));
                sdlat = Math.sin(elat - wlat);
                cwaz = (2.0 * celat * Math.sin(wlat) * sdlon * sdlon + sdlat)/sgc;
                swaz = sadln * celat/sgc;
                waz = Math.atan2(swaz, cwaz) * rtod;
                ceaz = (2.0 * cwlat * Math.sin(elat) * sdlon * sdlon - sdlat)/sgc;
                seaz = sadln * cwlat/sgc;
                eaz = Math.atan2(seaz, ceaz) * rtod; 
                eaz = 360.0 - eaz;
                if (delon <= 0.0) {
                    ztaz = eaz;
                    zraz = waz;
                }
                else {
                    ztaz = waz;
                    zraz = eaz;
                }
                
                //compute the straight line distance and great circle angle between T and R
                double cgc;
                d = Math.sqrt(delht * delht + 2.0 * (rerth + thts) * (rerth + rhts) * p);
                zd =d;
                cgc = 1.0 - p;
                gc = Math.atan2(sgc, cgc);
                zdgc = gc * rerth;
            }
        }
        else  {  //mode == 1 
            double tlatr, tlonr, tazr, colat, cosco, sinco;
            double cosgc, singc, cosb;
            double arg, b, arc;
            double rdlon, drlat;
            
            tlatr = ztlat * dtor;
            tlonr = ztlon * dtor;
            tazr = ztaz * dtor;
            gc = zdgc / rerth;
            colat = pi/2.0 - tlatr;
            cosco = Math.cos(colat);
            sinco = Math.sin(colat);
            cosgc = Math.cos(gc);
            singc = Math.sin(gc);
            cosb = cosco * cosgc + sinco * singc * Math.cos(tazr);
            arg = Math.max(0, (1- cosb * cosb));
            b = Math.atan2(Math.sqrt(arg), cosb);
            arc = (cosgc - cosco* cosb) / (sinco * Math.sin(b));
            arg = Math.max(0, (1.0 - arc * arc));
            rdlon = Math.atan2(Math.sqrt(arg), arc);
            zrlat = (pi/2 - Math.abs(b)) * rtod;
            drlat = zrlat ;
            zrlat = Math.abs(drlat) * cosb / Math.abs(cosb);
            zrlon = ztlon + Math.abs(rdlon) * rtod;
            
            if (ztaz > 180) zrlon = ztlon - (Math.abs(rdlon) * rtod);
            thts = ztht * 1.0E-3;
            rhts = zrht * 1.0E-3;
            delht = rhts - thts;
            sgc = Math.sin(0.5* gc);
            d = Math.sqrt(delht * delht + 4.0 * (rerth + thts) * (rerth + rhts) * sgc * sgc);
        }
            
        //code for both mode
        double aht, bht;
        if (delht < 0) {
            aht = thts;
            bht = rhts;
        }
        else {
            aht = rhts;
            bht = thts;
        }
        
        double saelv, arg, aelv,belv;
        saelv = 0.5 * (d * d + Math.abs(delht) * (rerth + aht + rerth + bht))/(d* (rerth + aht) );
        arg = Math.max(0, (1- saelv * saelv));
        aelv = Math.atan2(saelv, Math.sqrt(arg));
        belv = (aelv - gc)* rtod;
        aelv = - aelv * rtod;
        
        //compute take-off angels assuming 4/3 earth radius;
        double r4thd, aalt, balt, da, atakof, btakof;
        r4thd = rerth * 4.0/3.0;
        gc = 0.75 * gc;
        sgc = Math.sin(0.5 * gc);
        p = 2.0 * sgc * sgc;
        aalt = r4thd + aht;
        balt = r4thd + bht;
        da = Math.sqrt(delht * delht + 2.0 * aalt * balt * p);
        saelv = 0.5 * (da * da + Math.abs(delht) * (aalt + balt))/(da * aalt);
        arg = Math.max(0, (1 - saelv * saelv));
        atakof = Math.atan(saelv/Math.sqrt(arg));
        btakof = (atakof - gc) * rtod;
        atakof = - atakof * rtod;
        
        if (delht < 0) {
            ztelv = aelv;
            zrelv = belv;
            ztakof = atakof;
            zrakof = btakof;
        }
        else {
            ztelv = belv;
            zrelv = aelv;
            ztakof = btakof;
            zrakof = atakof;
        }
    }  
        
/**
          extract the GLOBE elevation for (xxlon,xxlat)
          GLOBE_elevation= elevation in meters of point
                     = < -500 = file does not exist
                     This should only happen if your data files
                     are not in the directory specified by path.
          NOTE: GLOBE flags ocean values as -500.
                These routines change any -500 value to 0.
               If you wish to identify ocean values, you should modify
                the code in get_GLOBE_data to suit your needs.
************************************************************
          The elevation of the 4 points that contain the 
          (xxlon,xxlat) are found and the elevation is interpolated.
          The 4 points are:
                   2   3
                   1   4
************************************************************
          In order to get the same value at Latitude=-90
          regardless of longitude, any Latitude below -89.99167
          has been forced to = 2777 meters elevation.
          This is because the lowest latitude data record
          corresponds to latitude=-89.9916666666...,
          which is NOT the South Pole, and the values at
          different longitude are slightly different.
*************************************************************/
    
    double GlobeElevation(double xxLon, double xxLat)  
    {
        String indexFileName = globePath + File.separator + "globe.dat";
        
       
        if (ionce == 0) {
            try {
            BufferedReader in = new BufferedReader(new 
                FileReader(indexFileName) );
            String line = in.readLine();
       
            for (int i = 0; i < 16;i ++)
            {
                line = in.readLine();
                StringTokenizer t = new StringTokenizer(line);
                tiles[i] = t.nextToken();
            }
            in.close();
            ionce = 1;
            }
            catch(IOException e) {
                System.out.println("File " + indexFileName+" not exists " + e.getMessage());
                System.exit(1);
            }
        }
        
        if (xxLat < -89.99167) {
            return 2777;   //south pole: 2777 meters
        }
        
        if (xxLon < 0) xxLon = xxLon + 360;
        
        Location loc = new Location();
        Fraction frac = new Fraction();
        FPosition fpos = new FPosition();
        
        double [] elev = new double[4];
        double elevation = 0;
        GlobeIndex(xxLat, xxLon, loc, frac);
        
        int y1 = loc.y+1;
        if (y1 > 21599) y1 = 21599;
        int x1 = loc.x + 1;
        if (x1 > 43199) x1 = 0;
        
        
        try {
        
           
        elev[0] = getElevation(loc);
        loc.y ++ ;
        elev[1] = getElevation(loc);
        loc.x++;
        elev[2] = getElevation(loc);
        loc.y--;
        elev[3] = getElevation(loc);
        
        
        elevation = GlobeInterp(frac, elev); //interpolate to find elevation at (xlat, xlon).
        } catch (IOException e) {
            System.out.println("Open or read Globe data error " + e.getMessage());
            System.exit(1);
        }
                
        return elevation;
    }   

    int getElevation(Location loc) throws IOException {
        
        Integer elevation = (Integer) elevationTable.get(loc);
        if (elevation != null  )  {
            return elevation.intValue();
        }
        else {
            FPosition fpos = new FPosition();;
            GlobeRecord(loc.x, loc.y, fpos);
            int elev = getGlobeData(fpos);
            elevationTable.put(loc.clone(), new Integer(elev));
            return elev;
        }
    }

/***********************************************************************
           Open a GLOBE data file and get the elevation corresponding
           to a particular cell corner. <br>
           Return elevation value in meters. <br>
                   The GLOBE database contains -500 to signify ocean.
                   That value is converted to 0 in this routine.
                   If you wish to do something different, do so in the routine.
*/ 
    int getGlobeData(FPosition fpos) throws IOException 
    {
                
        //do we need to open a new data file?
        if (lastTile != fpos.tile ) {  // open a new Globe file
            if (lastTile != -1) globeFile.close();
            String fileName = globePath + File.separator +  tiles[fpos.tile-1];
            globeFile = new RandomAccessFile(fileName, "r");
            lastTile = fpos.tile;
        }
        
        globeFile.seek(fpos.record*2);
        int data = globeFile.readUnsignedShort(); //important to readUnsignedShort 
        //swap the byte ordering, //I guess it is needed here because the file is stored using small-endian
        int tmp = data  & 255;
        data = (short)(tmp << 8) + (data >>8); //important to convert type first
        if (data == -500) data = 0;
        
        
        
        return data;
    }

/**  
	Given: (ix,iy) - the Globe cell location
    Find:  fpos(tile - tile index containing (x,y)
                record  - record number within itile containing elevation
 */
    void GlobeRecord(int ix,int iy, FPosition fpos)
    {
        int icol = ix % 10800;
        int jx = ix /10800 + 1;
        int jy;
        int irow;
        if (iy >= 16800) {
            jy = 0;
            irow = 21600 - iy;
        }
        else if (iy >= 10800) {
            jy = 1;
            irow = 16800 - iy;
        }
        else if (iy >= 4800) {
            jy = 2;
            irow = 10800 - iy;
        }
        else if (iy >= 0) {
            jy = 3;
            irow = 4800 - iy;
        }
        else {
            System.out.println("wrong parameters in Globe.GlobeRecord");
            jy = -1;
            irow = 0;
        }
        fpos.tile = jx + jy * 4;
        fpos.record = (irow -1) * 10800 + icol ; //no plus 1 because java start from 0
        return;
    }

/**    
          given: (xlat,xlon)
          find:  Location loc(x,y) of lower left corner of cell containing (xlat,xlon)
                         x will range from [0 to 43199].
                             0 = -179.99583 longitude
                         y will range from [0 to 21599].
                             0 = -89.99583
                 Fraction frac(dx,dy) is used to interpolate. It is the fraction of cell
                         where (xlat,xlon) is located.
          Note: Globe data base cell size = 30 seconds = 1/120 degree = .0083333
*/  
    void GlobeIndex(double xlat, double xlon, Location loc, Fraction frac)
    {
        double dlat, dlon, x,y;
        dlat = xlat + 90;
        y = dlat * 120 - 0.5;
        loc.y = (int) y;
        dlon = xlon;
        if (xlon < 0) dlon = dlon+360;
        if (dlon >= 180) {
            x = (dlon - 180) * 120 - 0.5;
            if (x < 0) x += 43200;
        }
        else {
            x = (dlon + 180) * 120 - 0.5;
        }
        loc.x = (int) x;
        frac.dx = x - loc.x;
        frac.dy = y - loc.y;
        
        if ( frac.dy < 0)  {
            loc.y = 0;
            frac.dy = 0;
        }
        
        return;
    }
    
    //bilinear interpolation routine
    double GlobeInterp(Fraction frac, double [] elev)
    {
        double z01 = elev[0] + (elev[1] - elev[0]) * frac.dy;
        double z23 = elev[3] + (elev[2] - elev[3] ) * frac.dy;
        double zp = z01 + (z23 - z01) * frac.dx;
        return zp;
    }
    
/**
  Release the open file  resources.
 */ 
    public void dispose()
    {
        try {
        if (globeFile != null) globeFile.close();
        }
        catch (IOException e) {
        	System.err.println("globeFile close error " + e.getMessage());
        
        }
    }
    
	//careful when use this function because it modifies the sender and receiver's location,
    //should only be used for calculating altitude.
    double distance(double tLat, double tLon, double rLat, double rLon)
    {
        ztlat = tLat;
        ztlon = tLon;
        zrlat = rLat;
        zrlon = rLon;
        dazel(0);
        return zdgc * 1000;
    }
    
    double txLat;      //transmitter latitude
    double txLon;      //transmitter longitude
    double rxLat;      //receiver latitude
    double rxLon;      //receiver longitude
    int nPoints;       // number of points from source to destination
    String globePath;  //path to the globe data.

    //variable used by dazel to calculate great circle paths.
    double ztlat, ztlon, ztht, zrlat, zrlon, zrht, ztaz, zraz;
    double ztelv, zrelv, zd, zdgc, ztakof, zrakof;
    
    //variable used for globe initiation
    static String [] tiles = new String[16];
    static int ionce = 0; //indicate whether the file globe.dat has been read.
   
    //used for access the data file
    int lastTile;
    RandomAccessFile  globeFile;
    
    class Fraction {
        public double dx, dy; //dx, dy is the fraction of cell where xlat, xlon is located
    };
    static class FPosition {
        public int tile; // the index containing location(x,y),
        public int record; //record number within itile containing elevation.
            
    };
    
    static Hashtable elevationTable = new Hashtable(1001);
}


    class Location {
        public int x,y;
        
        Location () {
            x = y = 0;
        }
        Location (int x_, int y_) {
            x = x_;
            y = y_;
        }
        Location (Location that) {
            x = that.x;
            y = that.y;
        }
        public boolean equals(Object that_obj) {
            Location that_  = (Location) that_obj;
            
            boolean v = ( (x == that_.x)  && (y == that_.y));
            return v;
        }
        public int hashCode() {
            return x ^ ((y << 16) + (y >>16) ) ;
        }
        protected Object clone() {
            return new Location(x,y);
        }
    }
