// @(#)MobilityModel.java   1/2004
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

import drcl.comp.*;
import drcl.inet.data.*;
import drcl.net.*;

/**
 * This class simulates the movement of the mobile nodes. The position of 
 * the node is periodically updated and reported to the <code>NodePositionTracker</code>
 * component. The coordinates can be either (longitude, lagitude, height) or (X, Y, Z).
 * There are two different modes. If a trajectory array is installed, the mobility
 * model assume the mobile node move from one point in the trajectory to the 
 * next point at a constant speed. Otherwise starting from the original 
 * position, the node randomly chooses a position in the simulated area as the destination,
 * and moves in a straight line to that destiona point at a constant speed which is 
 * uniformly distributed between 0 and Max_Speed. Whenever the node arrives at the destination,
 * it chooses the next position and repeats the procedure again.  
 *
 * @see WirelessPhy
 * @see NodePositionTracker
 * @author Ye Ge
 */     
public class MobilityModel extends drcl.net.Module implements drcl.comp.ActiveComponent
{

    /** 
     * This field is defined to keep the node Id instead of requesting such kind of
     * information from other components which may incur extra simulation overhead.
     */
    public long nid;
    
    /** 
     * Sets the nid. 
     *
     *@param nid_ the id of the mobile node.
     */ 
    public void setNid(long nid_) { nid = nid_; }
    
    /** Gets the nid. */
    public long getNid() { return nid; }
    
    /* configurate the ports */
    private static final String REPORT_PORT_ID  = ".report";   // connect to the mobile nodes' wirelessphy   
    private static final String CONFIG_PORT_ID  = ".config";
    private static final String QUERY_PORT_ID   = ".query";    // connect to the channel component
    
    /** the port which should be connected to the mobility tracker. */
    protected Port reportPort = addPort(REPORT_PORT_ID, false);
    
    /** the config port */
    protected Port configPort = addPort(CONFIG_PORT_ID, false); 
    
    /** the query port which should be connected to the WireleePhy component. */
    protected Port queryPort  = addPort(QUERY_PORT_ID, false);
    
    // parameters same as those in NodePositionTracker
    
    /** The largest x coordinate value of the simulated area */     
    public double maxX;
    /** The largest y coordinate value of the simulated area */     
    public double maxY;
    /** The largest z coordinate value of the simulated area */     
    public double maxZ;
    /** The smallest x coordinate value of the simulated area */     
    public double minX;
    /** The smallest y coordinate value of the simulated area */     
    public double minY;
    /** The smallest z coordinate value of the simulated area */     
    public double minZ;
    /** The grid size along X-axle */
    public double dX;
    /** The grid size along Y-axle */
    public double dY;
    /** The grid size along Z-axle */
    public double dZ;    

    /** The x coordinator of the node's current position */ 
    public double X;
    /** The y coordinator of the node's current position */ 
    public double Y;
    /** The z coordinator of the node's current position */ 
    public double Z;                            // current position
     
    
    /** The x coordinate of the node's previous position */ 
    public double X0;
    /** The y coordinate of the node's previous position */ 
    public double Y0;
    /** the z coordinate of the node's previous position */ 
    public double Z0;                   // position at the last update time
    /** The x coordinate of the destination position */ 
    public double destX;
    /** The y coordinate of the destination position */ 
    public double destY;
    /** The z coordinate of the destination position */ 
    public double destZ;          // destination position       
    
    /** The moving speed along the x-axle */ 
    public double sx;
    /** The moving speed along the y-axle */ 
    public double sy;
    /** The moving speed along the z-axle */ 
    public double sz;
    /** The moving speed */        
    public double speed;            // (sx, sy, sz) is the normalized direction of the movement 
    
    /** The maximal speed for generating a random speed heading to the next random direction */
    public double Max_Speed;                    // Max_Speed for generating a random speed heading to the next random direction
    
    ACATimer reportPositionTimer;       // the object to keep the reference to the ACATimer so that we can cancel it if necessary
    ACATimer changeDestinationTimer;  

    /** The last time (X,Y,Z) is updated */
    private double positionUpdateTime;           // the last time (X,Y,Z) is updated
    /** The interval between automatic position updates */    
    private double positionReportPeriod;         // automatic position update period time
                                         // position update may also be triggered by the query
    /** The time period between two destination points */
    double changeDestinationPeriod;      // the time between two destination points

    /** The index of the current starting position in the trajectory array. */
    int        nCurrentTraj;            // index of the current starting position  
    
    /**
     * Number of points on the trajectory.
     */
    int        nTrajectory;
    
    /**
     * An two dimensional array represents the trajectory of the mobile node.
     */
    double[][] trajectory;              // arrys of ( time, longitude,  latitude, altitude ) 

    
    /**
     * The random number generator. The default seed is set to 11111.
     */
    static java.util.Random rand = new java.util.Random(11111); 

    /** Installs a trajectory array. */
    public void installTrajectory(double[][] trajectory_)  {      // the trajectory point array derived from SDF file 
        trajectory   = trajectory_;             // set the trajectory ( arry of states in SDF file ) 
        nTrajectory  = trajectory.length;
        nCurrentTraj = -1;         
	}
    
    /** 
     * Constructor.
     *
     *@param nid_ the id of the mobile node.
     */
    public MobilityModel(long nid_) {
        super();
        nid = nid_;
        X  = 0.0; Y  = 0.0; Z  = 0.0;
        X0 = 0.0; Y0 = 0.0; Z0 = 0.0;
        sx = 0.0; sy = 0.0; sz = 0.0;
        speed = 0.0;
        queryPort.setType(drcl.comp.Port.PortType_SERVER);
        positionReportPeriod = 1.0;

        removeDefaultDownPort(); 
        removeDefaultUpPort();  
        
    }

    /**
     * Constructor.
     */
    public MobilityModel( ) {
        super();
        nid = -1;
        X  = 0.0; Y  = 0.0; Z  = 0.0;
        X0 = 0.0; Y0 = 0.0; Z0 = 0.0;
        sx = 0.0; sy = 0.0; sz = 0.0;
        speed = 0.0;
        queryPort.setType(drcl.comp.Port.PortType_SERVER);
        positionReportPeriod = 1.0;

        removeDefaultDownPort(); 
        removeDefaultUpPort();  

    }
    
    /**
     * Set topology parameters
     *
     * @param maxX_ largest x coordinate value of the simulated area
     * @param minX_ smallest x coordinate value of the simulated area
     * @param maxY_ largest y coordinate value of the simulated area
     * @param minY_ smallest y coordinate value of the simulated area
     * @param dX_   grid size along X-axle
     * @param dY_   grid size along Y-axle
     * @param dZ_   grid size along Z-axle
     */
    public void setTopologyParameters(double maxX_, double maxY_, double minX_, double minY_, double dX_, double dY_, double dZ_)  {
        maxX = maxX_; maxY = maxY_; maxZ = 10.0; 
        minX = minX_; minY = minY_; minZ = 0.0;
        dX = dX_;     dY = dY_;     dZ = dZ_;
    }    
    
    /**
     * Set topology parameters
     *
     * @param maxX_ largest x coordinate value of the simulated area
     * @param minX_ smallest x coordinate value of the simulated area
     * @param maxY_ largest y coordinate value of the simulated area
     * @param minY_ smallest y coordinate value of the simulated area
     * @param maxZ_ largest y coordinate value of the simulated area
     * @param minZ_ smallest y coordinate value of the simulated area
     * @param dX_   grid size along X-axle
     * @param dY_   grid size along Y-axle
     * @param dZ_   grid size along Z-axle
     */
    public void setTopologyParameters(double maxX_, double maxY_, double maxZ_, double minX_, double minY_, double minZ_, double dX_, double dY_, double dZ_)  {
        maxX = maxX_; maxY = maxY_; maxZ = maxZ_; 
        minX = minX_; minY = minY_; minZ = minZ_;
        dX   = dX_;   dY   = dY_;   dZ   = dZ_;
    }    

    /**
     * Sets the initial position of the node.
     * @param Max_Speed_ - maximum moving speed
	 * @param X_ - initial X coordinate
	 * @param Y_ - initial Y coordinate
	 * @param Z_ - initial Z coordinate
     */
    public void setPosition(double Max_Speed_, double X_, double Y_, double Z_) {
        X = X_;  Y = Y_;  Z = Z_;
        X0 = X_; Y0 = Y_; Z0 = Z_;
        Max_Speed = Max_Speed_;        
    }  
    
    
    //public void putPosition() {
    //  debug("X=" + X + " Y=" + Y + "Z = " + Z);        
    //}
    
    protected void _start ()  {
        //randomPosition();

        //System.out.println("MobilityModel _start()");        
        if ( trajectory == null ) {     
            reportPosition(true);         // the position of the node must have been set in tcl file
            setRandomDestination();
            positionUpdateTime = getTime();
        }    
        else {
            setTrajectoryDestination();
            reportPosition(true);
            positionUpdateTime = getTime();
        }    
	}
    
	protected void _stop()  {
        if (reportPositionTimer    != null)  cancelTimeout(reportPositionTimer);
        if (changeDestinationTimer != null)  cancelTimeout(changeDestinationTimer);	
    }

	protected void _resume()  {   
        // Why comment out this?
        //if (reportPositionTimer == null)  updatePosition( );
	}    
    
    /**
     * Sets this mobile node to a random generated position.
     */ 
    public synchronized void setRandomPosition() {

        X = minX + rand.nextDouble() * ( maxX - minX );
        Y = minY + rand.nextDouble() * ( maxY - minY );
        Z = minZ + rand.nextDouble() * ( maxZ - minZ );        
                
        reportPosition(true);    // force it to report
        positionUpdateTime = getTime();       
    }
        
    /**
     * Sets a random generated destination to move to 
     */
    public synchronized void setRandomDestination( ) {
        double dist;
        
        updatePosition();    // calculate the current position
        
        // speed = Math.random() * Max_Speed;
        speed = rand.nextDouble() * Max_Speed;
        
        destX = minX + rand.nextDouble() * (maxX - minX);
        destY = minY + rand.nextDouble() * (maxY - minY);
        destZ = minZ + rand.nextDouble() * (maxZ - minZ);
        
        sx = destX - X;
        sy = destY - Y;
        sz = destZ - Z;
        
        dist = Math.sqrt((sx * sx) + (sy * sy) + (sz * sz));
        
        if ( destX != X || destY != Y || destZ != Z ) {
            sx = sx/dist*speed;
            sy = sy/dist*speed;
            sz = sz/dist*speed;
        }
        
        if ( speed > 1.0e-20 ) 
            changeDestinationPeriod = dist / speed;
        else
            changeDestinationPeriod = dist / 1.0e-20;
        
        // why cancel?
        //if ( changeDestinationTimer != null )  cancelTimeout(changeDestinationTimer); 
        changeDestinationTimer = setTimeout("ChangeDestination", changeDestinationPeriod);
        
    }

    /**
     * Sets the next destination according to the installed trajectory.
     */
    public synchronized void setTrajectoryDestination( ) {
        double dist;
        double now = getTime();

        if ( nCurrentTraj == -1 ) {   // first time calling, initiate
            nCurrentTraj = 0;
            X  = trajectory[0][1];  Y  = trajectory[0][2]; Z  = trajectory[0][3];
            X0 = trajectory[0][1];  Y0 = trajectory[0][2]; Z0 = trajectory[0][3];
        }    
        else {
            X = trajectory[nCurrentTraj][1];  Y = trajectory[nCurrentTraj][2]; Z = trajectory[nCurrentTraj][3];  // set the current position to the destination
        }    
        
        positionUpdateTime = now;

        if ( nCurrentTraj < (nTrajectory-1) ) {
            sx = (trajectory[nCurrentTraj+1][1] - trajectory[nCurrentTraj][1]);     // this is not direction yet
            sy = (trajectory[nCurrentTraj+1][2] - trajectory[nCurrentTraj][2]); 
            sz = (trajectory[nCurrentTraj+1][3] - trajectory[nCurrentTraj][3]);             
            dist = Math.sqrt(sx*sx + sy*sy + sz*sz);
            changeDestinationPeriod = (double) ( trajectory[nCurrentTraj+1][0] - trajectory[nCurrentTraj][0] );  // time to the next update time
            speed = dist / changeDestinationPeriod;                    
            sx = sx / changeDestinationPeriod;          //  velocity in x direction
            sy = sy / changeDestinationPeriod;
            sz = sz / changeDestinationPeriod;
            
            destX = trajectory[nCurrentTraj+1][1];
            destY = trajectory[nCurrentTraj+1][2];
            destZ = trajectory[nCurrentTraj+1][3];

            nCurrentTraj = nCurrentTraj + 1;
        
            //if ( changeDestinationTimer != null )  cancelTimeout(changeDestinationTimer); 
            changeDestinationTimer = setTimeout("ChangeDestination", changeDestinationPeriod);
        }
        else {
            dist = 0.0; speed = 0.0; 
            changeDestinationPeriod = 1000000000.0;      
            sx = 0.0; sy = 0.0; sz = 0.0;
            // no more trajectory update timeout event scheduled
        }    
    }    
    
    /**
     * Calculates the current position upon timeout or query
     */
    public synchronized void updatePosition() {       
        double now = getTime();
        double interval = now - positionUpdateTime;   // the time from now to the next timeout 
        
        if ( interval == 0.0 )  return;
        
        X = X + sx * interval;
        Y = Y + sy * interval;
        Z = Z + sz * interval;
        
        // adjust the position if it exceed the destination point 
        if ((sx > 0 && X > destX) || (sx < 0 && X < destX))  X = destX;   
        if ((sy > 0 && Y > destY) || (sy < 0 && Y < destY))  Y = destY;
        if ((sz > 0 && Z > destZ) || (sz < 0 && Z < destZ))  Z = destZ;
        
        
        positionUpdateTime = now;

        //debug("updatePosition X=" + X + " Y=" + Y);        
    }
    
    /**
     * Sends position report to the <code>NodePositionTracker</code> component.
     * If falseReport is set to true, the report is generated immediately.
     */
    public synchronized void reportPosition(boolean forcedReport) {
        if (forcedReport == true || (X-X0) >= dX || (X0-X) >= dX || (Y-Y0) >= dY || (Y0-Y) >= dY || (Z-Z0) >= dZ || (Z0-Z) >= dZ ) {   // if not forced report, report only if the change of position has exceeded the boundary
            reportPort.doSending(new PositionReportContract.Message(nid, X, Y, Z, X0, Y0, Z0));
            X0 = X; Y0 = Y; Z0 = Z;
        }    
    }    
        
    /**
     * Handles timeout events.
     */
    protected synchronized void timeout(Object data_) {
        if ( data_.equals("ChangeDestination") ) {
            if ( trajectory == null ) {
                setRandomDestination();
                reportPosition(false);
                // timeout is scheduled again in setRandomDestination
            }
            else {
                setTrajectoryDestination();
                reportPosition(false);
                // timeout is scheduled again in setTrajectoryDestination
            }    
        }
        else if ( data_.equals("ReportPosition") ) {   
            if ( trajectory == null ) {
                updatePosition();
                reportPosition(false);
                reportPositionTimer = setTimeout("ReportPosition", positionReportPeriod);
            }
            else {
                updatePosition();
                reportPosition(false);
                reportPositionTimer = setTimeout("ReportPosition", positionReportPeriod);
            }    
        }    
    }
    
    protected synchronized void processOther(Object data_, Port inPort_)  {
		String portid_ = inPort_.getID();
    
        if (portid_.equals(QUERY_PORT_ID)) 
            processQuery(data_);
        else 
            super.processOther(data_, inPort_);
	}  

    /**
     * Processes the position query from <code>WirelessPhy</code> component.
     */
    protected synchronized void processQuery(Object data_) {
        updatePosition();
        queryPort.doSending(new PositionReportContract.Message(X, Y, Z));
        
    }    

    /** 
     * Sets the seed of the random number generator.
     */
    public void setSeed(long seed) {
        rand.setSeed(seed);
    }    
}

