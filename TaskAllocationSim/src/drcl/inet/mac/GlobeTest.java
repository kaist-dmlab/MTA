// @(#)GlobeTest.java   7/2003
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


import java.io.*;
import java.text.*;

/**
 * This is a program to test extraction of the GLOBE data base.
 * @author Honghai Zhang
 * @see drcl.inet.mac.Globe
 */

public class GlobeTest {

	/**
	  * Main function running a test for Globe extracting routines.
	  */
    public static void main(String [] args) {
        
                   
        double [] sample = {
            1585., 1593.,1597.,1595.,1589.,1573.,1557.,1545.,1534.,1532.,
            1526.,1508.,1492.,1486.,1481.,1477.,1477.,1486.,1497.,1506.,
            1519.,1528.,1534.,1532.,1528.,1535.,1554.,1551.,1524.,1500.,
            1494.,1509.,1523.,1538.,1554.,1540.,1518.,1517.,1497.,1484.,
            1496.,1503.,1505.,1523.,1524.,1517.,1500.,1496.,1479.,1481.,
            1487.,1485.,1482.,1480.,1480.,1490.,1494.,1497.,1504.,1511.,
            1520.,1523.,1531.,1539.,1546.,1552.,1564.,1567.,1577.,1580.,
            1586.,1593.,1599.,1605.,1620.,1628.,1637.,1641.,1651.,1662.,
            1671.,1679.,1683.,1694.,1705.,1715.,1724.,1733.,1744.,1749.,
            1764.,1803.,1839.,1839.,1860.,1871.,1890.,1900.,1916.,1943.,
            1975.}; 
          
      int nPoints = 101;
      double txLat, txLon;
      double rxLat, rxLon;
      double delta = 1111.77;
      int maxnPoints = 5001;
      double [] pfl = new double[maxnPoints+2]; 
      String globePath = "../../../../globedat";
      
      Globe globe = new Globe(globePath);
      double d = globe.distance(0, 125, 0, 124);
      System.out.println("distance = "+d);
      
      System.out.println("Sample input: 101 40 -105 41 -105");
      System.out.println("Should produce the results:");
      printData(nPoints, delta, sample,0);
      System.out.println("Please compare your results to those above");
      
	  nPoints = 101;
                
      txLat = 40;
      txLon = -105;
      rxLat = 41;
      rxLon = -105;
            
        globe = new Globe(txLat, txLon, rxLat, rxLon, nPoints, globePath);
        int result = globe.get_pfl(pfl);
        if (result != 0) {
            System.out.println("Wrong globe data or extraction routine " + result);
			return;
        }

        System.out.println("Here is your result:");
        printData((int) pfl[0], pfl[1], pfl,2);
		

		boolean res = true;
		for (int i = 0; i < nPoints; i++)
			if (Math.abs(sample[i]- pfl[i+2]) > 1) 
			{
				res = false;
				System.out.println("The " + i + "th result is different, difference: "
						+ (sample[i]- pfl[i+2]));
			}
				 
		if (res) 
			System.out.println("The result is correct");
        
      
    }
    
    static void printData(int npoints, double delta, double [] data, int sIndex) { 
      NumberFormat nf = NumberFormat.getNumberInstance();
      System.out.println(npoints +" points found, delta between points= "+ nf.format(delta) + " meters");
      nf.setMaximumFractionDigits(0);
      for (int i = 0; i < npoints; i++) {
          System.out.print(nf.format(data[sIndex+i])+ " ");
          if (i % 10 == 9) System.out.println();
      }
      System.out.println();
    }
    
   
    
   
}
