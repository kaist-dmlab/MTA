// @(#)IrregularTerrainModel.java   1/2004
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

import drcl.comp.Port;
import drcl.comp.Contract;
import drcl.util.*;
import drcl.util.scalar.*;

import java.lang.*;
import java.text.*;
import java.util.*;

/*
 * IrregularTerrainModel.java
 *
 * Created on July 19, 2002, 5:34 PM
 */

/**
 * This class implements the Irregular Terrain Model.
 * The details of this model can be found in <a href="http://elbert.its.bldrdoc.gov/itm.html">ITM </a> 
 * 
 * @author  Honghai Zhang 
 * @see drcl.inet.mac.RadioPropagationModel
 */

// *************************************
//               IrregularTerrainModel
// *************************************
// Routines for this program are taken from
// a tranlation of the FORTRAN code written by
// U.S. Department of Commerce NTIA/ITS
// Institute for Telecommunication Sciences
// *****************
// Irregular Terrain Model (ITM) (Longley-Rice)
// *************************************


//import java.lang.*;

public class IrregularTerrainModel extends drcl.inet.mac.RadioPropagationModel{

    static class prop_type { 
      
      public double aref;
      public double dist;
      public double hg[] = new double[2];
      public double wn;
      public double dh;
      public double ens;
      public double gme;
      public double zgndreal;
      public double zgndimag;
      public double he [] = new double[2];
      public double dl[] = new double[2];
      public double the[] = new double[2];
      public int kwx;
      public int mdp;
      
    };

    static class propv_type
    { double sgc;
      int lvar;
      int mdvar;
      int klim;
    };

    static class propa_type
    { double dlsa;
      double dx;
      double ael;
      double ak1;
      double ak2;
      double aed;
      double emd;
      double aes;
      double ems;
      double dls[] = new double[2];
      double dla;
      double tha;
    };

    static class couple {
        double x, y;
    };
    
    /**
	 * Class Location is used to record self address,
	 */
    static class Location {
        
        protected double lon, lat,height;  //lon, lat is in degrees, height is in meters
        
        public Location()
        {
            lon = 0; lat = 0; height = 0; 
        }
        public Location(double lon_, double lat_, double height_)
        {
            lon = lon_; lat = lat_; height = height_; 
        }
        public boolean isMoved(double lon_, double lat_, double height_)
        {
            double delon = Math.abs(lon - lon_);
            double delat = Math.abs(lat_ - lat);
            double deheight = Math.abs(height - height_);
            return (delon > 9.E-5) || (delat > 9.E-5) || (deheight >10);
            //averagely for movement to be 10 meters, delon  =10/40000000 * 360 = 9 E-5
        }
        
        public void setLocation(double lon_, double lat_, double height_)
        {
            lon = lon_;
            lat = lat_;
            height = height_;
        }
       
        
    };
    
    /** 
	 * Class LocLoss is used to record the loss from a given sender
	 */
    static class LocLoss extends Location 
    {
        protected double loss;
        
        public LocLoss(double lon_, double lat_, double height_, double loss_)
        {
            setLocLoss(lon_, lat_, height_, loss_);
        }
        
        public void setLocLoss(double lon_, double lat_, double height_, double loss_)
        {
            lon = lon_; lat = lat_; height = height_; loss = loss_;
        }
    }


	/**
	  * A port for tracing path loss. 
	  */
	protected Port tracePort = addPort(".proptrace");

	void trace(String what_)
	{
                tracePort.doSending("At " + getTime() + " "+  what_ + "\n");

	}
        
    
    //from adiff
    double wd1, xd1, afo, qk, aht, xht;
    
    //from ascat
    double ad, rr, etq, h0s;
    //from alos
    double wls;
    
    //from lrprop
    boolean wlos, wscat;
    double dmin, xae;
   
    //from avar
    int kdv;
    double dexa, de, vmd, vs0, sgl, sgtm, sgtp, sgtd, tgtd,
                    gm, gp, cv1, cv2, yv1, yv2, yv3, csm1, csm2, ysm1, ysm2,
                    ysm3, csp1, csp2, ysp1, ysp2, ysp3, csd1, zd, cfm1, cfm2,
                    cfm3, cfp1, cfp2, cfp3;
    boolean ws, w1;
    
    static final double THIRD = 1.0/3.0;
    
    //global variable used for calculate radio attenuation.
    //the path to the global database, the following are default values
    //assuming the whole simulation use the same set of variables below.
    //Otherwise those parameters should not be defined as static
    static final double DB = 8.68589; /*= 20 /ln10 */
    //static String globePath = "/home/jhou/hzhang3/disk2/terrain/globedat";
	//static String globePath = "C:\\terrain\\globedat\\";
    static int polarity ;     // 0--horizontal, 1 -- vertical
    static int radioClimate ;
    static double surfRef ;   //surface reflectivity
    static double dielectric ;
    static double conductivity ;
    
	//It seems ITM does not handle the case when both sender and
	//receive are at altitude 0, we add all height by an antennaHeight.
	static double antennaHeight = 2.0; //meters 

    int nPoints ;      //number of points in the terrain profile between sender and receiver

    // elevations of the points between sender and receiver, need to be re-allocated when nPoints changes
    // the first two elements is the nPoints and delta (the distance divided by nPoints-1.
    double [] dblelev;   
    
    Location loc;
    HashMap lossCache;
    Globe globe;

    
    
    /**
	 * Creates new IrregularTerrainModel.
	 */
    public IrregularTerrainModel() 
    {
        super();
        
        //default values
        nPoints = 20;
        dblelev = new double[nPoints+2];
        polarity = 1;
        radioClimate = 4;
        surfRef = 280.0;
        dielectric = 15.0;
        conductivity = 0.005;
        
        lossCache = new HashMap();
        loc = new Location();
        
        //globe = new Globe(globePath);
    }

	/**
	  * Set the path to the GLOBE database.
	  */
	public void setGlobePath(String path)
	{
		globe = new Globe(path);
	}
	/**
	  * Set the number of sampling points between transmitter and receiver.
	  */
   	public void setNumPoints(int nPts) { 
        if (nPts != nPoints) {
            nPoints = nPts; 
            dblelev = new double [nPoints +2];
        }
   	}
	/** Set radio polarity: 0 or 1. */
   	public void setPolarity(int pol) { polarity = (pol == 0)?0:1 ;}
	/** Get radio polarity. */
   	public int 	getPolarity() { return polarity  ;}
	/** Set Radio Climate: 1 to 7.  */
   	public void setRadioClimate(int climate) {radioClimate = climate; }
	/** Get Radio Climate.  */
   	public int 	getRadioClimate() {return radioClimate ; }
	/** Set Surface Reflectivity. */
   	public void setSurfRef(double ref) { surfRef = ref; }
	/** Get Surface Reflectivity. */
   	public double getSurfRef() { return surfRef; }
	/** Set Dielectric. */
   	public void setDielectric(double diel) { dielectric =  diel;}
	/** Get Dielectric. */
   	public double getDielectric() { return dielectric ;}
	/** Set Ground Conductivity. */
   	public void setConductivity(double cond) {conductivity = cond; }
	/** Get Ground Conductivity. */
   	public double getConductivity() {return conductivity ; }
   
   
        
    
    
	/**
	  * Release open file  resources.
	  */
    public void dispose()
    {
        globe.dispose();
    }
     
	/**
	  * Process the query of path loss from the query port and send back the path loss.
	  */
    
    protected synchronized void processPathLossQuery(Object data_) {
        double loss;
        double  Lambda;
        double txHeight, txLon, txLat;
        double rxHeight, rxLon, rxLat;
        long senderId; 
        
        
        RadioPropagationQueryContract.Message msg = (RadioPropagationQueryContract.Message) data_;
        
        txHeight     = msg.getZs() + antennaHeight;  //get transmitter's height
        txLon   = msg.getXs();   //get transmitter's longitude
        txLat   = msg.getYs();   //get transmitter's latitude
        
        rxHeight = msg.getZr() + antennaHeight;
        rxLon   = msg.getXr();
        rxLat   = msg.getYr();
        
        Lambda = msg.getLambda();
        senderId = msg.getSid();
        
        loss = calculatePathLoss(senderId, txLon, txLat, txHeight, rxLon, rxLat,rxHeight, Lambda, 0.5, 0.5);
		trace("(" + txLon + "," + txLat + "," + txHeight + ")---> (" + rxLon + "," + rxLat + "," + rxHeight + ") loss = " + loss );
        queryPort.doSending(new RadioPropagationQueryContract.Message(loss));
    }


    
    //pr = 0.5, pc = 0.5 for default. lamda = C/F, C = 2.998x10^8. F is carrier frequence 
    //sid set to -1 so far 
	/**
	  * Return the path loss from transmitter to receiver.
	  */
    public double calculatePathLoss(long sid,  double txLon, double txLat, double txHeight,
                           double rxLon, double rxLat, double rxHeight, double Lambda, 
                           double pr, double pc) 
    {
  
  // mPoints:  has the number of points in the terrain profile
  // delta: has the distance between points in meters.
  // dblelev[]: floating point values of the terrain (as returned from GLOBE)
  // txHeight: has the transmitting antenna height in meters
  // rxHeight: has the receiving antenna height in meters
  // polarity: has value of 0 for Horizontal Polarization, or 1 for Vertical Polarization
  // radioClimate: has the value of
  //     1 for Equatorial
  //     2 for Continental Subtropical
  //     3 for Maritime Tropical
  //     4 for Desert
  //     5 for Continental Temperate
  //     6 for Maritime Temperate, Over Land
  //     7 for Maritime Temperate, Over Sea
  // dielectric: has value of dielectric constant of ground
  // conductivity: has value conductivity of ground
  // freq: has the frequency in MHz
  // surfRef: has the surface refractivity
  
      //check in the cache to see whether the loss has been calculated
        /* comment this cache because Ye has implemented another layer of cache in WirelessPhy
        LocLoss locLoss = null;
        if (loc.isMoved(rxLon, rxLat, rxHeight) )  {  //clear all mapping if recievier's location is moved
          lossCache.clear();
          loc.setLocation(rxLon, rxLat, rxHeight);
        }
        else if (sid != -1) {
            locLoss = (LocLoss) lossCache.get(new Long(sid));
            if (locLoss != null  &&  (! locLoss.isMoved(txLon, txLat, txHeight) ))
                return locLoss.loss;
        }
        */
        
        
      //not found in the cache, calculate from the globe database       
      //extract data from globe database
      globe.resetPosition(txLat, txLon, rxLat, rxLon, nPoints);
      int result = globe.get_pfl(dblelev);
      
      if (result != 0) {
            drcl.Debug.error("Wrong globe data or extraction routine " + result);
      }
                    
      dblelev[0]--;
      
      //prepare for data
      double np, eno, enso, q, zsys, fs;
      
      prop_type prop = new prop_type();
      propv_type propv = new propv_type();
      propa_type propa = new propa_type();
      
      prop.hg[0] = txHeight;
      prop.hg[1] = rxHeight;
      propv.klim = radioClimate;
      prop.kwx = 0;
      propv.lvar = 5;
      prop.mdp = -1;
      
      double eps = dielectric;
      double sgm = conductivity;
      double freq = 300 / Lambda; // frequency in MHz
      
      double zc, zr;
      double ja, jb;
      int i;
      
      //pc = 0.5;
      zc = qerfi(pc);
      //pr = 0.5;
      zr = qerfi(pr);
      
      np = dblelev[0];
      eno = surfRef;
      enso = 0.0;
      q = enso;
      zsys = 0.0;
        if(q<=0)
        { 
            ja = 3.0 + 0.1 * dblelev[0];
            jb = np - ja + 6.0;
            for(i=(int) ja;i<=jb;++i)
            zsys += dblelev[i-1];
            zsys /= (jb - ja + 1);
            q = eno;
        }
        propv.mdvar = 12;
        qlrps(freq,zsys,q,polarity,eps,sgm,prop);
        qlrpfl(dblelev,radioClimate,propv.mdvar,prop,propa,propv);
        fs = 32.45 + DB * Math.log(freq *prop.dist/1000.0);
     
        double xlb = fs + avar(zr,0.0, zc, prop, propv);
        
        double loss = Math.pow(10,-xlb/10);
        
        //add the new loc-loss to the cache
        //disable this cache because Ye has implemented another layer of cache in WirelessPhy.
        //lossCache.put(new Long(sid), new LocLoss(txLon, txLat, txHeight, loss));
        
        return loss;
      
    }
    
	/**
	  * Return the name of this component.
	  */

    public String info() { 
        return "IrregularTerrainModel" + "\n"; 
    }
    
        
        
    double FORTRAN_DIM( double x, double y)
    { // This performs the FORTRAN DIM function.
      // result is x-y if x is greater than y; otherwise result is 0.0
        if(x>y)
            return x-y;
        else
        return 0.0;
    }

    double aknfe(double v2)
    { 
        double a;
        if(v2<5.76)
        a=6.02+9.11 * Math.sqrt(v2)-1.27*v2;
        else
        a=12.953+4.343*Math.log(v2);
        return a;
    }

    double fht(double  x, double  pk)
    { 
        double w, fhtv;
        if(x<200.0)
        {   
            w=-Math.log(pk);
	        if( pk < 1e-5 || x*Math.pow(w,3.0) > 5495.0 )
	        { 
                fhtv=-117.0;
		        if(x>1.0)
		        fhtv=17.372*Math.log(x)+fhtv;
            }
	        else
            fhtv=2.5e-5*x*x/pk-8.686*w-15.0;
	    }
        else
	    {
            fhtv=0.05751*x-4.343*Math.log(x);
	        if(x<2000.0)
	        { 
                w=0.0134*x*Math.exp(-0.005*x);
                fhtv=(1.0-w)*fhtv+w*(17.372*Math.log(x)-117.0);
            }
	    }
        return fhtv;
    }

    double h0f(double r, double et)
    { 
        double a[]={25.0, 80.0, 177.0, 395.0, 705.0};
        double b[]={24.0, 45.0,  68.0,  80.0, 105.0};
        double q, x;
        int it;
        double h0fv;
        it=(int)et;
        if(it<=0)
        {   
            it=1;
            q=0.0;
        }
        else if(it>=5)
        {    
            it=5;
            q=0.0;
        }
        else
            q=et-it;
        x=Math.pow(1.0/r,2.0);
        h0fv=4.343*Math.log((a[it-1]*x+b[it-1])*x+1.0);
        if(q!=0.0)
            h0fv=(1.0-q)*h0fv+q*4.343*Math.log((a[it]*x+b[it])*x+1.0);
        return h0fv;
    }

    double ahd(double td)
    { 
        int i;
        double a[] = {   133.4,    104.6,     71.8};
        double b[] = {0.332e-3, 0.212e-3, 0.157e-3};
        double c[] = {  -4.343,   -1.086,    2.171};
        if(td<=10e3)
            i=0;
        else if(td<=70e3)
            i=1;
        else
            i=2;
        return a[i]+b[i]*td+c[i]*Math.log(td);
    }

    double  adiff( double d, prop_type prop, propa_type propa)
    { 

        Complex prop_zgnd = new Complex(prop.zgndreal,prop.zgndimag);
        double a, q, pk, ds, th, wa, ar, wd, adiffv;
        if (d==0)
	    {
		  q=prop.hg[0]*prop.hg[1];
		  qk=prop.he[0]*prop.he[1]-q;
	      if(prop.mdp<0.0)
		    q+=10.0;
		  wd1=Math.sqrt(1.0+qk/q);
		  xd1=propa.dla+propa.tha/prop.gme;
		  q=(1.0-0.8*Math.exp(-propa.dlsa/50e3))*prop.dh;
		  q*=0.78*Math.exp(-Math.pow(q/16.0,0.25));
	      afo=Math.min(15.0,2.171*Math.log(1.0+4.77e-4*prop.hg[0]*prop.hg[1] *
			  prop.wn*q));
		  qk=1.0/Complex.abs(prop_zgnd);
		  aht=20.0;
		  xht=0.0;
		  for(int j=0;j<2;++j)
		    {
              a=0.5*Math.pow(prop.dl[j],2.0)/prop.he[j];
			  wa=Math.pow(a*prop.wn,THIRD);
			  pk=qk/wa;
			  q=(1.607-pk)*151.0*wa*prop.dl[j]/a;
			  xht+=q;
			  aht+=fht(q,pk);
	    	}
		  adiffv=0.0;
		}
        else
        { th=propa.tha+d*prop.gme;
          ds=d-propa.dla;
          q=0.0795775*prop.wn*ds*Math.pow(th,2.0);
          adiffv=aknfe(q*prop.dl[0]/(ds+prop.dl[0]))+aknfe(q*prop.dl[1]/(ds+prop.dl[1]));
          a=ds/th;
          wa=Math.pow(a*prop.wn,THIRD);
          pk=qk/wa;
          q=(1.607-pk)*151.0*wa*th+xht;
          ar=0.05751*q-4.343*Math.log(q)-aht;
          q=(wd1+xd1/d)*Math.min(((1.0-0.8*Math.exp(-d/50e3))*prop.dh*prop.wn),6283.2);
          wd=25.1/(25.1+Math.sqrt(q));
          adiffv=ar*wd+(1.0-wd)*adiffv+afo;
        }
        return adiffv;
    }

    double  ascat( double d, prop_type prop, propa_type propa)
    { 
        //complex prop_zgnd = new complex(prop.zgndreal,prop.zgndimag);
        //no use
        
        double h0, r1, r2, z0, ss, et, ett, th, q;
        double ascatv;
        if(d==0.0)
        { ad=prop.dl[0]-prop.dl[1];
          rr=prop.he[1]/prop.he[0];
          if(ad<0.0)
            { ad=-ad;
              rr=1.0/rr;
            }
          etq=(5.67e-6*prop.ens-2.32e-3)*prop.ens+0.031;
          h0s=-15.0;
          ascatv=0.0;
        }
        else {
            if(h0s>15.0)
              h0=h0s;
            else
            { th=prop.the[0]+prop.the[1]+d*prop.gme;
              r2=2.0*prop.wn*th;
              r1=r2*prop.he[0];
              r2*=prop.he[1];
              if(r1<0.2 && r2<0.2)
                return 1001.0;  // <==== early return
              ss=(d-ad)/(d+ad);
              q=rr/ss;
              ss=Math.max(0.1,ss);
              q=Math.min(Math.max(0.1,q),10.0);
              z0=(d-ad)*(d+ad)*th*0.25/d;
              et=(etq*Math.exp(-Math.pow(Math.min(1.7,z0/8.0e3),6.0))+1.0)*z0/1.7556e3;
              ett=Math.max(et,1.0);
              h0=(h0f(r1,ett)+h0f(r2,ett))*0.5;
              h0+=Math.min(h0,(1.38-Math.log(ett))*Math.log(ss)*Math.log(q)*0.49);
              h0=FORTRAN_DIM(h0,0.0);
              if(et<1.0)
                h0=et*h0+(1.0-et)*4.343*Math.log(Math.pow((1.0+1.4142/r1) *
                   (1.0+1.4142/r2),2.0)*(r1+r2)/(r1+r2+2.8284));
              if(h0>15.0 && h0s>=0.0)
                h0=h0s;
            }
          h0s=h0;
          th=propa.tha+d*prop.gme;
          ascatv=ahd(th*d)+4.343*Math.log(47.7*prop.wn*Math.pow(th,4.0))-0.1 *
                 (prop.ens-301.0)*Math.exp(-th*d/40e3)+h0;
        }
      return ascatv;
    }

    double qerfi( double q )
    { double x, t, v;
      double c0  = 2.515516698;
      double c1  = 0.802853;
      double c2  = 0.010328;
      double d1  = 1.432788;
      double d2  = 0.189269;
      double d3  = 0.001308;

      x = 0.5 - q;
      t = Math.max(0.5 - Math.abs(x), 0.000001);
      t = Math.sqrt(-2.0 * Math.log(t));
      v = t - ((c2 * t + c1) * t + c0) / (((d3 * t + d2) * t + d1) * t + 1.0);
      if (x < 0.0) v = -v;
      return v;
    }

    
    double abq_alos (Complex  r)
    { return r.real()*r.real()+r.imag()*r.imag(); }

    double  alos( double d, prop_type prop, propa_type propa)
    { Complex prop_zgnd = new Complex(prop.zgndreal,prop.zgndimag);
      
      Complex r;
      double s, sps, q;
      double alosv;
      if(d==0.0)
        { wls=0.021/(0.021+prop.wn*prop.dh/Math.max(10e3,propa.dlsa));
          alosv=0.0;
        }
      else
        { q=(1.0-0.8*Math.exp(-d/50e3))*prop.dh;
          s=0.78*q*Math.exp(-Math.pow(q/16.0,0.25));
          q=prop.he[0]+prop.he[1];
          sps=q/Math.sqrt(d*d+q*q);
          r=Complex.divide(Complex.subtract(sps,prop_zgnd),Complex.add(sps,prop_zgnd));
          r = Complex.multiply(r, Math.exp(-Math.min(10.0,prop.wn*s*sps)));
          q=abq_alos(r);
          if(q<0.25 || q<sps)
            r= Complex.multiply(r,Math.sqrt(sps/q));
          alosv=propa.emd*d+propa.aed;
          q=prop.wn*prop.he[0]*prop.he[1]*2.0/d;
          if(q>1.57)
            q=3.14-2.4649/q;
          
          Complex t = new Complex(Math.cos(q),-Math.sin(q));
          alosv = (-4.343*Math.log(abq_alos(Complex.add(t,r)))-alosv) * wls + alosv;
         }
      return alosv;
    }

    void qlrps( double fmhz, double zsys, double en0,
              int ipol, double eps, double sgm, prop_type prop)
    {
	  double gma=157e-9;
      prop.wn=fmhz/47.7;
      prop.ens=en0;
      if(zsys!=0.0)
        prop.ens*=Math.exp(-zsys/9460.0);
      prop.gme=gma*(1.0-0.04665*Math.exp(prop.ens/179.3));
      Complex prop_zgnd; //not used = new Complex(prop.zgndreal,prop.zgndimag);
      Complex zq= new Complex(eps,376.62*sgm/prop.wn);
      prop_zgnd=Complex.sqrt(Complex.subtract(zq,1.0));
      if(ipol!=0.0)
        prop_zgnd = Complex.divide(prop_zgnd,zq);

      prop.zgndreal=prop_zgnd.real();  
      prop.zgndimag=prop_zgnd.imag();
    }

    void qlra( int [] kst, int klimx, int mdvarx,
              prop_type prop, propv_type propv)
    { //Complex prop_zgnd = new Complex(prop.zgndreal,prop.zgndimag);
      //not used
      double q;
      for(int j=0;j<2;++j)
        { if(kst[j]<=0)
            prop.he[j]=prop.hg[j];
          else
            { q=4.0;
              if(kst[j]!=1)
                q=9.0;
              if(prop.hg[j]<5.0)
                q*=Math.sin(0.3141593*prop.hg[j]);
              prop.he[j]=prop.hg[j]+(1.0+q)*Math.exp(-Math.min(20.0,2.0*prop.hg[j]/Math.max(1e-3,prop.dh)));
            }
          q=Math.sqrt(2.0*prop.he[j]/prop.gme);
          prop.dl[j]=q*Math.exp(-0.07*Math.sqrt(prop.dh/Math.max(prop.he[j],5.0)));
          prop.the[j]=(0.65*prop.dh*(q/prop.dl[j]-1.0)-2.0*prop.he[j])/q;
        }
      prop.mdp=1;
      propv.lvar=Math.max(propv.lvar,3);
      if(mdvarx>=0)
        { propv.mdvar=mdvarx;
          propv.lvar=Math.max(propv.lvar,4);
        }
      if(klimx>0)
        { propv.klim=klimx;
          propv.lvar=5;
        }
    }

    void lrprop (double d,
              prop_type prop, propa_type propa) // paul_m_lrprop
    { 
      Complex prop_zgnd = new Complex(prop.zgndreal,prop.zgndimag);
      
      double a0, a1, a2, a3, a4, a5, a6;
      double d0, d1, d2, d3, d4, d5, d6;
      boolean wq;
      double q;
      int j;

      if(prop.mdp!=0)
        {
          for(j=0;j<2;j++)
            propa.dls[j]=Math.sqrt(2.0*prop.he[j]/prop.gme);
          propa.dlsa=propa.dls[0]+propa.dls[1];
          propa.dla=prop.dl[0]+prop.dl[1];
          propa.tha=Math.max(prop.the[0]+prop.the[1],-propa.dla*prop.gme);
          wlos=false;
          wscat=false;
          if(prop.wn<0.838 || prop.wn>210.0)
                { prop.kwx=Math.max(prop.kwx,1);
            }
          for(j=0;j<2;j++)
            if(prop.hg[j]<1.0 || prop.hg[j]>1000.0)
                { prop.kwx=Math.max(prop.kwx,1);
            }
          for(j=0;j<2;j++)
            if( Math.abs(prop.the[j]) >200e-3 || prop.dl[j]<0.1*propa.dls[j] ||
               prop.dl[j]>3.0*propa.dls[j] )
            { 
                prop.kwx=Math.max(prop.kwx,3);
            }
          if( prop.ens < 250.0   || prop.ens > 400.0  || 
              prop.gme < 75e-9 || prop.gme > 250e-9 || 
              prop_zgnd.real() <= Math.abs(prop_zgnd.imag()) || 
              prop.wn  < 0.419   || prop.wn  > 420.0 )
                { prop.kwx=4;
                }
              for(j=0;j<2;j++)
            if(prop.hg[j]<0.5 || prop.hg[j]>3000.0)
            { prop.kwx=4;
            }
          dmin=Math.abs(prop.he[0]-prop.he[1])/200e-3;
          q=adiff(0.0,prop,propa);
          xae=Math.pow(prop.wn*Math.pow(prop.gme,2),-THIRD);
          d3=Math.max(propa.dlsa,1.3787*xae+propa.dla);
          d4=d3+2.7574*xae;
          a3=adiff(d3,prop,propa);
          a4=adiff(d4,prop,propa);
          propa.emd=(a4-a3)/(d4-d3);
          propa.aed=a3-propa.emd*d3;
         }
      if(prop.mdp>=0)
        {	prop.mdp=0;
        prop.dist=d;
        }
      if(prop.dist>0.0)
        {
          if(prop.dist>1000e3)
            { prop.kwx=Math.max(prop.kwx,1);
            }
          if(prop.dist<dmin)
            { prop.kwx=Math.max(prop.kwx,3);
            }
          if(prop.dist<1e3 || prop.dist>2000e3)
            { prop.kwx=4;
            }
        }
      if(prop.dist<propa.dlsa)
        {
          if(!wlos)
            {
            q=alos(0.0,prop,propa);
            d2=propa.dlsa;
            a2=propa.aed+d2*propa.emd;
            d0=1.908*prop.wn*prop.he[0]*prop.he[1];
                if(propa.aed>=0.0)
                { d0=Math.min(d0,0.5*propa.dla);
                  d1=d0+0.25*(propa.dla-d0);
                }
            else
                d1=Math.max(-propa.aed/propa.emd,0.25*propa.dla);
            a1=alos(d1,prop,propa);
            wq=false;
            if(d0<d1)
            {
                a0=alos(d0,prop,propa);
                q=Math.log(d2/d0);
                propa.ak2=Math.max(0.0,((d2-d0)*(a1-a0)-(d1-d0)*(a2-a0)) /
                                    ((d2-d0)*Math.log(d1/d0)-(d1-d0)*q));
                wq=propa.aed>=0.0 || propa.ak2>0.0;
                if(wq)
                    { 
                    propa.ak1=(a2-a0-propa.ak2*q)/(d2-d0);
                    if(propa.ak1<0.0)
                        { propa.ak1=0.0;
                                  propa.ak2=FORTRAN_DIM(a2,a0)/q;
                      if(propa.ak2==0.0)
                            propa.ak1=propa.emd;
                                }
                }
                else
                {
                    propa.ak2=0.0;
                    propa.ak1=(a2-a1)/(d2-d1);
                    if(propa.ak1<=0.0)
                        propa.ak1=propa.emd;
                }
                }
              else
                {	propa.ak1=(a2-a1)/(d2-d1);
                propa.ak2=0.0;
                if(propa.ak1<=0.0)
                    propa.ak1=propa.emd;
                }
              propa.ael=a2-propa.ak1*d2-propa.ak2*Math.log(d2);
              wlos=true;
            }
          if(prop.dist>0.0)
            prop.aref=propa.ael+propa.ak1*prop.dist +
                       propa.ak2*Math.log(prop.dist);
        }
      if(prop.dist<=0.0 || prop.dist>=propa.dlsa)
        { if(!wscat)
            { 
              q=ascat(0.0,prop,propa);
              d5=propa.dla+200e3;
              d6=d5+200e3;
              a6=ascat(d6,prop,propa);
              a5=ascat(d5,prop,propa);
              if(a5<1000.0)
                { propa.ems=(a6-a5)/200e3;
                  propa.dx=Math.max(propa.dlsa,Math.max(propa.dla+0.3*xae *
                         Math.log(47.7*prop.wn),(a5-propa.aed-propa.ems*d5) /
                         (propa.emd-propa.ems)));
                  propa.aes=(propa.emd-propa.ems)*propa.dx+propa.aed;
                }
              else
                { propa.ems=propa.emd;
                  propa.aes=propa.aed;
                  propa.dx=10.e6;
                }
              wscat=true;
            }
          if(prop.dist>propa.dx) 
		  {
            prop.aref=propa.aes+propa.ems*prop.dist;
		  }
          else {
            prop.aref=propa.aed+propa.emd*prop.dist;
		  }
        }
      prop.aref=Math.max(prop.aref,0.0);
    }


    //I guess this is lrprop developed by Fred,
	//it is not used by other functions 
    void freds_lrprop (double d,
              prop_type prop, propa_type propa) // freds_lrprop
    { 
        
      Complex prop_zgnd = new Complex(prop.zgndreal, prop.zgndimag);
      double a0, a1, a2, a3, a4, a5, a6;
      double d0, d1, d2, d3, d4, d5, d6;
      double q;
      int j;

       if(prop.mdp!=0)
        {
          for(j=0;j<2;++j)
            propa.dls[j]=Math.sqrt(2.0*prop.he[j]/prop.gme);
          propa.dlsa=propa.dls[0]+propa.dls[1];
          propa.dla=prop.dl[0]+prop.dl[1];
          propa.tha=Math.max(prop.the[0]+prop.the[1],-propa.dla*prop.gme);
          wlos=false;
          wscat=false;
          if(prop.wn<0.838 || prop.wn>210.0)
            { prop.kwx=Math.max(prop.kwx,1);
            }
          for(j=0;j<2;++j)
            if(prop.hg[j]<1.0 || prop.hg[j]>1000.0)
              { prop.kwx=Math.max(prop.kwx,1);
              }
          for(j=0;j<2;++j)
            if( Math.abs(prop.the[j]) >200e-3 || prop.dl[j]<0.1*propa.dls[j] ||
               prop.dl[j]>3.0*propa.dls[j] )
              { prop.kwx=Math.max(prop.kwx,3);
              }
          if( prop.ens < 250.0   || prop.ens > 400.0  || 
              prop.gme < 75e-9 || prop.gme > 250e-9 || 
              prop_zgnd.real() <= Math.abs(prop_zgnd.imag()) || 
              prop.wn  < 0.419   || prop.wn  > 420.0 )
             { prop.kwx=4;
             }
          for(j=0;j<2;++j)
            if(prop.hg[j]<0.5 || prop.hg[j]>3000.0)
              { prop.kwx=4;
              }
          dmin=Math.abs(prop.he[0]-prop.he[1])/200e-3;
          q=adiff(0.0,prop,propa);
          xae=Math.pow(prop.wn * prop.gme * prop.gme,-THIRD);
          d3=Math.max(propa.dlsa,1.3787 * xae+propa.dla);
          d4=d3+2.7574 * xae;
          a3=adiff(d3,prop,propa);
          a4=adiff(d4,prop,propa);
          propa.emd=(a4-a3)/(d4-d3);
          propa.aed=a3-propa.emd * d3;
          if(prop.mdp==0)
            prop.dist=0;
          else if(prop.mdp>0)
            { prop.mdp=0;  prop.dist=0; }
          if((prop.dist>0.0) || (prop.mdp<0.0))
            {
              if(prop.dist>1000e3)
                { prop.kwx=Math.max(prop.kwx,1);
                }
              if(prop.dist<dmin)
                { prop.kwx=Math.max(prop.kwx,3);
                }
              if(prop.dist<1e3 || prop.dist>2000e3)
                { prop.kwx=4;
                }
            }
        }
      else
        { prop.dist=d;
          if(prop.dist>0.0)
            {
              if(prop.dist>1000e3)
                { prop.kwx=Math.max(prop.kwx,1);
                }
              if(prop.dist<dmin)
                { prop.kwx=Math.max(prop.kwx,3);
                }
              if(prop.dist<1e3 || prop.dist>2000e3)
                { prop.kwx=4;
                }
            }
        }

      if(prop.dist<propa.dlsa)
        {
          if(!wlos)
            {
              // Cooeficients for the line-of-sight range

              q=alos(0.0,prop,propa);
              d2=propa.dlsa;
              a2=propa.aed+d2*propa.emd;
              d0=1.908*prop.wn*prop.he[0]*prop.he[1];
              if(propa.aed>=0.0)
                { d0=Math.min(d0,0.5*propa.dla);
                  d1=d0+0.25*(propa.dla-d0);
                }
              else
                d1=Math.max(-propa.aed/propa.emd,0.25*propa.dla);
              a1=alos(d1,prop,propa);
            
              if(d0<d1)
                { a0=alos(d0,prop,propa);
                  q=Math.log(d2/d0);
                  propa.ak2=Math.max(0.0,((d2-d0)*(a1-a0)-(d1-d0)*(a2-a0)) /
                            ((d2-d0)*Math.log(d1-d0)-(d1-d0)*q));
                  if(propa.ak2<=0.0 && propa.aed<0.0)
                    { propa.ak2=0.0;
                      propa.ak1=(a2-a1)/(d2-d1);
                      if(propa.ak1<=0.0)
                        propa.ak2=propa.emd;
                    }
                  else
                    { propa.ak1=(a2-a0-propa.ak2*q)/(d2-d0);
                      if(propa.ak1<0.0)
                        { propa.ak1=0.0;
                          propa.ak2=FORTRAN_DIM(a2,a0)/q;
                          if(propa.ak2<=0.0)
                            propa.ak1=propa.emd;
                        }
                    }
                  propa.ael=a2-propa.ak1*d2-propa.ak2*Math.log(d2);
                  wlos=true;
                }
            }
          if(prop.dist>0.0)
            prop.aref=propa.ael+propa.ak1*prop.dist+propa.ak2*Math.log(prop.dist);
        }
      else
        { if(!wscat)
            { q=ascat(0.0,prop,propa);
              d5=propa.dla+200e3;
              d6=d5+200e3;
              a6=ascat(d6,prop,propa);
              a5=ascat(d5,prop,propa);
              if(a5>=1000.0)
                { propa.ems=propa.emd;
                  propa.aes=propa.aed;
                  propa.dx=10e6;
                }
              else
                { propa.ems=(a6-a5)/200e3;
                  propa.dx=Math.max(propa.dlsa,Math.max(propa.dla+0.3*xae*Math.log(47.7*prop.wn),
                           (a5-propa.aed-propa.ems*d5)/(propa.emd-propa.ems)));
                  propa.aes=(propa.emd-propa.ems)*propa.dx+propa.aed;
                }
              wscat=true;
            }
          if(prop.dist<=propa.dx)
            prop.aref=propa.aed+propa.emd*prop.dist;
          else
            prop.aref=propa.aes+propa.ems*prop.dist;
        }
      prop.aref=FORTRAN_DIM(prop.aref,0.0);
    }


    double curve (double c1, double c2, double x1,
                  double x2, double x3, double de)
    { return (c1+c2/(1.0+Math.pow((de-x2)/x3,2.0)))*Math.pow(de/x1,2.0) /
             (1.0+Math.pow(de/x1,2.0));
    }

    double avar (double zzt, double zzl, double zzc,
             prop_type prop, propv_type propv)
    { 
      double bv1 []={-9.67,-0.62,1.26,-9.21,-0.62,-0.39,3.15};
      double bv2 []={12.7,9.19,15.5,9.05,9.19,2.86,857.9};
      double xv1 []={144.9e3,228.9e3,262.6e3,84.1e3,228.9e3,141.7e3,2222.e3};
      double xv2 []={190.3e3,205.2e3,185.2e3,101.1e3,205.2e3,315.9e3,164.8e3};
      double xv3 []={133.8e3,143.6e3,99.8e3,98.6e3,143.6e3,167.4e3,116.3e3};
      double bsm1 []={2.13,2.66,6.11,1.98,2.68,6.86,8.51};
      double bsm2 []={159.5,7.67,6.65,13.11,7.16,10.38,169.8};
      double xsm1 []={762.2e3,100.4e3,138.2e3,139.1e3,93.7e3,187.8e3,609.8e3};
      double xsm2 []={123.6e3,172.5e3,242.2e3,132.7e3,186.8e3,169.6e3,119.9e3};
      double xsm3 []={94.5e3,136.4e3,178.6e3,193.5e3,133.5e3,108.9e3,106.6e3};
      double bsp1 []={2.11,6.87,10.08,3.68,4.75,8.58,8.43};
      double bsp2 []={102.3,15.53,9.60,159.3,8.12,13.97,8.19};
      double xsp1 []={636.9e3,138.7e3,165.3e3,464.4e3,93.2e3,216.0e3,136.2e3};
      double xsp2 []={134.8e3,143.7e3,225.7e3,93.1e3,135.9e3,152.0e3,188.5e3};
      double xsp3 []={95.6e3,98.6e3,129.7e3,94.2e3,113.4e3,122.7e3,122.9e3};
      double bsd1 []={1.224,0.801,1.380,1.000,1.224,1.518,1.518};
      double bzd1 []={1.282,2.161,1.282,20.,1.282,1.282,1.282};
      double bfm1 []={1.0,1.0,1.0,1.0,0.92,1.0,1.0};
      double bfm2 []={0.0,0.0,0.0,0.0,0.25,0.0,0.0};
      double bfm3 []={0.0,0.0,0.0,0.0,1.77,0.0,0.0};
      double bfp1 []={1.0,0.93,1.0,0.93,0.93,1.0,1.0};
      double bfp2 []={0.0,0.31,0.0,0.19,0.31,0.0,0.0};
      double bfp3 []={0.0,2.00,0.0,1.79,2.00,0.0,0.0};
     
      double rt=7.8, rl=24.0, avarv, q, vs, zt, zl, zc;
      double sgt, yr;
      int temp_klim = propv.klim-1;

      if(propv.lvar>0)
        { if(propv.lvar<=5)
            switch(propv.lvar)
             { default:
                 if(propv.klim<=0 || propv.klim>7)
                   { propv.klim = 5;
                     temp_klim = 4;
                     { prop.kwx=Math.max(prop.kwx,2);
                     }
                   }
                 cv1 = bv1[temp_klim];
                 cv2 = bv2[temp_klim];
                 yv1 = xv1[temp_klim];
                 yv2 = xv2[temp_klim];
                 yv3 = xv3[temp_klim];
                 csm1=bsm1[temp_klim];
                 csm2=bsm2[temp_klim];
                 ysm1=xsm1[temp_klim];
                 ysm2=xsm2[temp_klim];
                 ysm3=xsm3[temp_klim];
                 csp1=bsp1[temp_klim];
                 csp2=bsp2[temp_klim];
                 ysp1=xsp1[temp_klim];
                 ysp2=xsp2[temp_klim];
                 ysp3=xsp3[temp_klim];
                 csd1=bsd1[temp_klim];
                 zd  =bzd1[temp_klim];
                 cfm1=bfm1[temp_klim];
                 cfm2=bfm2[temp_klim];
                 cfm3=bfm3[temp_klim];
                 cfp1=bfp1[temp_klim];
                 cfp2=bfp2[temp_klim];
                 cfp3=bfp3[temp_klim];
               case 4:
                 kdv=propv.mdvar;
                 ws = kdv>=20;
                 if(ws)
                   kdv-=20;
                 w1 = kdv>=10;
                 if(w1)
                   kdv-=10;
                 if(kdv<0 || kdv>3)
                   { kdv=0;
                     prop.kwx=Math.max(prop.kwx,2);
                   }
               case 3:
                 q=Math.log(0.133*prop.wn);
                 gm=cfm1+cfm2/(Math.pow(cfm3*q,2.0)+1.0);
                 gp=cfp1+cfp2/(Math.pow(cfp3*q,2.0)+1.0);
               case 2:
                 dexa=Math.sqrt(18e6*prop.he[0])+Math.sqrt(18e6*prop.he[1]) +
                      Math.pow((575.7e12/prop.wn),THIRD);
               case 1:
                 if(prop.dist<dexa)
                   de=130e3*prop.dist/dexa;
                 else
                   de=130e3+prop.dist-dexa;
            }
            vmd=curve(cv1,cv2,yv1,yv2,yv3,de);
            sgtm=curve(csm1,csm2,ysm1,ysm2,ysm3,de) * gm;
            sgtp=curve(csp1,csp2,ysp1,ysp2,ysp3,de) * gp;
            sgtd=sgtp*csd1;
            tgtd=(sgtp-sgtd)*zd;
            if(w1)
              sgl=0.0;
            else
              { q=(1.0-0.8*Math.exp(-prop.dist/50e3))*prop.dh*prop.wn;
                sgl=10.0*q/(q+13.0);
              }
            if(ws)
              vs0=0.0;
            else
              vs0=Math.pow(5.0+3.0*Math.exp(-de/100e3),2.0);
            propv.lvar=0;
        }
      zt=zzt;
      zl=zzl;
      zc=zzc;
      switch(kdv)
        { case 0:
            zt=zc;
            zl=zc;
            break;
          case 1:
            zl=zc;
            break;
          case 2:
            zl=zt;
        }
      if(Math.abs(zt)>3.1 || Math.abs(zl)>3.1 || Math.abs(zc)>3.1)
          { prop.kwx=Math.max(prop.kwx,1);
          }
      if(zt<0.0)
        sgt=sgtm;
      else if(zt<=zd)
        sgt=sgtp;
      else
        sgt=sgtd+tgtd/zt;
      vs=vs0+Math.pow(sgt*zt,2.0)/(rt+zc*zc)+Math.pow(sgl*zl,2.0)/(rl+zc*zc);
      if(kdv==0)
        { yr=0.0;
          propv.sgc=Math.sqrt(sgt*sgt+sgl*sgl+vs);
        }
      else if(kdv==1)
        { yr=sgt*zt;
          propv.sgc=Math.sqrt(sgl*sgl+vs);
        }
      else if(kdv==2)
        { yr=Math.sqrt(sgt*sgt+sgl*sgl)*zt;
          propv.sgc=Math.sqrt(vs);
        }
      else
        { yr=sgt*zt+sgl*zl;
          propv.sgc=Math.sqrt(vs);
        }
      avarv=prop.aref-vmd-yr-propv.sgc*zc;
      if(avarv<0.0)
        avarv=avarv*(29.0-avarv)/(29.0-10.0*avarv);
      return avarv;

    }

    void hzns (double pfl[], prop_type prop)
    { boolean wq;
      int np;
      double xi, za, zb, qc, q, sb, sa;
      
      np=(int)pfl[0];
      xi=pfl[1];
      za=pfl[2]+prop.hg[0];
      zb=pfl[np+2]+prop.hg[1];
      qc=0.5*prop.gme;
      q=qc*prop.dist;
      prop.the[1]=(zb-za)/prop.dist;
      prop.the[0]=prop.the[1]-q;
      prop.the[1]=-prop.the[1]-q;
      prop.dl[0]=prop.dist;
      prop.dl[1]=prop.dist;
      if(np>=2)
        { sa=0.0;
          sb=prop.dist;
          wq=true;
          for(int i=1;i<np;++i)
            { sa+=xi;
              sb-=xi;
              q=pfl[i+2]-(qc*sa+prop.the[0])*sa-za;
              if(q>0.0)
                { prop.the[0]+=q/sa;
                  prop.dl[0]=sa;
                  wq=false;
                }
              if(!wq)
                { q=pfl[i+2]-(qc*sb+prop.the[1])*sb-zb;
                  if(q>0.0)
                    { prop.the[1]+=q/sb;
                      prop.dl[1]=sb;
                    }
                }
            }
        }
    }
      
    void z1sq1 (double z[], double x1, double x2,
                couple cp)
    { double xn, xa, xb, x, a, b;
      int n, ja, jb;
      xn=z[0];
      xa=(int)(FORTRAN_DIM(x1/z[1],0.0));
      xb=xn-(int)(FORTRAN_DIM(xn,x2/z[1]));
      if(xb<=xa)
        { xa=FORTRAN_DIM(xa,1.0);  // the 1.0 is the lowest array element
          xb=xn-FORTRAN_DIM(xn,xb+1.0); // should have xb not xb+1.0
        }
      ja=(int)xa;
      jb=(int)xb;
      n=jb-ja;
      xa=xb-xa;
      x=-0.5*xa;
      xb+=x;
      a=0.5*(z[ja+2]+z[jb+2]);
      b=0.5*(z[ja+2]-z[jb+2])*x;
      if(n>=2)
        for(int i=2;i<=n;++i)
          { ++ja;
            x+=1.0;
            a+=z[ja+2];
            b+=z[ja+2]*x;
          }
      a/=xa;
      b=b*12.0/((xa*xa+2.0)*xa);
      cp.x =a-b*xb;
      cp.y =a+b*(xn-xb);
    }

    double qtile ( int nn, double a[], int startIndex, int ir)
    { double q, r; 
      int m, n, i, j, j1, i0, k;
      boolean done=false;
      boolean goto10=true;

      m=0;
      i0 = 0;
      n=nn;
      k=Math.min(Math.max(0,ir),n);
      q = a[k+ startIndex];
      j1 = n;
      while(!done)
          {
          if(goto10)
        {  q=a[k+ startIndex];
          i0=m;
          j1=n;
        }
          i=i0;
          while(i<=n && a[i+ startIndex]>=q)
            i++;
          if(i>n)
            i=n;
          j=j1;
          while(j>=m && a[j+ startIndex]<=q)
            j--;
          if(j<m)
            j=m;
          if(i<j)
            { 	  r=a[i+startIndex]; a[i+startIndex]=a[j+ startIndex]; a[j+ startIndex]=r;
              i0=i+1;
              j1=j-1;
              goto10=false;
            }
          else if(i<k)
            {	  a[k + startIndex]=a[i+ startIndex];
              a[i+ startIndex]=q;
              m=i+1;
              goto10=true;
                }
          else if(j>k)
            { 	  a[k+ startIndex]=a[j+ startIndex];
              a[j+ startIndex]=q;
              n=j-1;
              goto10=true;
                }
          else
            done=true;
          }
      return q;
    }
    
    //myqtile is not used by other functions
    double myqtile ( int nn, double a[], int ir)
    { double q, r; 
      int m, n, i, j, j1, i0, k;
      boolean done=false;

      m=1;
      n=nn;
      k=Math.min(Math.max(1,ir),n);
      q=a[k-1];
      i0=m;
      j1=n;
      while(!done)
        { i=i0;
          while(i<=n && a[i-1]>=q)
            ++i;
          if(i>n)
            i=n;
          j=j1;
          while(j>=m && a[j-1]<=q)
            --j;
          if(j<m)
            j=m;
          if(i<j)
            { r=a[i-1]; a[i-1]=a[j-1]; a[j-1]=r;
              i0=i+1;
              j1=j-1;
            }
          else if(i<k)
            { a[k-1]=a[i-1];
              a[i-1]=q;
              m=i+1;
              q=a[k-1];
              i0=m;
              j1=n;
            }
          else 
            {  if(j>k)
                 { a[k-1]=a[j-1];
                   a[j-1]=q;
                   n=j-1;
                   q=a[k-1];
                   i0=m;
                   j1=n;
                 }
               else
                 done=true;
            }
        } // while(!done)
      return q;
    }

    double qerf(double z)
    { double b1=0.319381530, b2=-0.356563782, b3=1.781477937;
      double b4=-1.821255987, b5=1.330274429;
      double rp=4.317008, rrt2pi=0.398942280;
      double t, x, qerfv;
      x=z;
      t=Math.abs(x);
      if(t>=10.0)
        qerfv=0.0;
      else
        { t=rp/(t+rp);
          qerfv=Math.exp(-0.5*x*x)*rrt2pi*((((b5*t+b4)*t+b3)*t+b2)*t+b1)*t;
        }
      if(x<0.0) qerfv=1.0-qerfv;
      return qerfv;
    }

    double d1thx(double pfl[], double x1, double x2)
    { int np, ka, kb, n, k, j;
      double d1thxv, sn, xa, xb;
      double [] s;
      couple cp = new couple();

      np=(int)pfl[0];
      xa=x1/pfl[1];
      xb=x2/pfl[1];
      d1thxv=0.0;
      if(xb-xa<2.0)  // exit out
        return d1thxv;
      ka=(int)(0.1*(xb-xa+8.0));
      ka=Math.min(Math.max(4,ka),25);
      n=10*ka-5;
      kb=n-ka+1;
      sn=n-1;
      s = new double[n+2]; // pay more attention on it later!
      s[0]=sn;
      s[1]=1.0;
      xb=(xb-xa)/sn;
      k=(int)(xa+1.0);
      xa-=(double)k;
      for(j=0;j<n;j++)
        { while(xa>0.0 && k<np)
            { xa-=1.0;
              ++k;
            }
          s[j+2]=pfl[k+2]+(pfl[k+2]-pfl[k+1])*xa;
          xa=xa+xb;
        }
      z1sq1(s,0.0,sn,cp);
      xa = cp.x;
      xb = cp.y;
      xb=(xb-xa)/sn;
      for(j=0;j<n;j++)
        { s[j+2]-=xa;
          xa=xa+xb;
        }
      d1thxv=qtile(n-1,s,2,ka-1)-qtile(n-1,s,2,kb-1);
      d1thxv/=1.0-0.8*Math.exp(-(x2-x1)/50.0e3);
      return d1thxv;
    }


    double myd1thx(double pfl[], double x1, double x2)
    { int np, ka, kb, n, k, j;
      double d1thxv, sn, xa, xb;
      double [] s;
      couple cp = new couple();

      np=(int)pfl[0];
      xa=x1/pfl[1];
      xb=x2/pfl[1];
      d1thxv=0.0;
      if(xb-xa<2.0)  // exit out
        return d1thxv;
      ka=(int)(0.1*(xb-xa+8.0));
      ka=Math.min(Math.max(4,ka),25);
      n=10*ka-5;
      kb=n-ka+1;
      sn=n-1;
      
      s = new double[n+3]; // pay more attention on it.
      s[0]=sn;
      s[1]=1.0;
      xb=(xb-xa)/sn;
      k=(int)(xa+1.0);
      xa-=(double)k;
      for(j=0;j<n;++j)  
        { while(xa>0.0 && k<np)
            { xa-=1.0;
              ++k;
            }
          s[j+2]=pfl[k+2]+(pfl[k+2]-pfl[k+1])*xa;
          xa+=xb;
        }
      z1sq1(s,0.0,sn,cp);
      xa = cp.x;
      xb = cp.y;
      xb=(xb-xa)/sn;
      for(j=0;j<n;++j)   
        { s[j+2]-=xa;
          xa+=xb;
        }
      d1thxv=qtile(n,s,2,ka)-qtile(n,s,2,kb);
      d1thxv/=1.0-0.8*Math.exp(-(x2-x1)/50.0e3);
      return d1thxv;
    }

    void qlrpfl( double pfl[], int klimx, int mdvarx,
            prop_type prop, propa_type propa, propv_type propv )
    { int np, j;
      double xl[] = new double [2];
      double q, za, zb;
      couple cp = new couple();

      prop.dist=pfl[0]*pfl[1];
      np=(int)pfl[0];
      hzns(pfl,prop);
      for(j=0;j<2;++j)
        xl[j]=Math.min(15.0*prop.hg[j],0.1*prop.dl[j]);
      xl[1]=prop.dist-xl[1];
      prop.dh=d1thx(pfl,xl[0],xl[1]);
      if(prop.dl[0]+prop.dl[1]>=1.5*prop.dist)
        { z1sq1(pfl,xl[0],xl[1],cp);
          za = cp.x;
          zb = cp.y;
          prop.he[0]=prop.hg[0]+FORTRAN_DIM(pfl[2],za);
          prop.he[1]=prop.hg[1]+FORTRAN_DIM(pfl[np+2],zb);
          for(j=0;j<2;++j)
            prop.dl[j]=Math.sqrt(2.0*prop.he[j]/prop.gme) *
                        Math.exp(-0.07*Math.sqrt(prop.dh/Math.max(prop.he[j],5.0)));
          q=prop.dl[0]+prop.dl[1];

          if(q<=prop.dist)
            { q=Math.pow(prop.dist/q,2.0);
              for(j=0;j<2;++j)
                { prop.he[j]*=q;
                  prop.dl[j]=Math.sqrt(2.0*prop.he[j]/prop.gme) *
                        Math.exp(-0.07*Math.sqrt(prop.dh/Math.max(prop.he[j],5.0)));
                }
            }
          for(j=0;j<2;++j)
            { q=Math.sqrt(2.0*prop.he[j]/prop.gme);
              prop.the[j]=(0.65*prop.dh*(q/prop.dl[j]-1.0)-2.0 *
                           prop.he[j])/q;
            }
        }
      else
        { z1sq1(pfl,xl[0],0.9*prop.dl[0],cp);
          za = cp.x; q = cp.y;
          z1sq1(pfl,prop.dist-0.9*prop.dl[1],xl[1],cp);
          q = cp.x; zb = cp.y;
          prop.he[0]=prop.hg[0]+FORTRAN_DIM(pfl[2],za);
          prop.he[1]=prop.hg[1]+FORTRAN_DIM(pfl[np+2],zb);
        }
      prop.mdp=-1;
      propv.lvar=Math.max(propv.lvar,3);
      if(mdvarx>=0)
        { propv.mdvar=mdvarx;
          propv.lvar=Math.max(propv.lvar,4);
        }
      if(klimx>0)
        { propv.klim=klimx;
          propv.lvar=5;
        }
      lrprop(0.0,prop,propa);
    }

}
