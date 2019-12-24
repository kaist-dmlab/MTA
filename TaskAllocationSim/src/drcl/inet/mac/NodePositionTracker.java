// @(#)NodePositionTracker.java   7/2003
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

import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*; 
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;

/**
 * This component divides the X-Y plane of the simulated space area into multiple
 * subareas. Each mobile node's <code>MobiltiyModel</code> periodically reports
 * its position to <code>NodePositionTracker</code>. 
 * When a <code>NodePositionTracker</code> receives a neighboring node inquery from
 * the <node>Channel</code> component, the <code>NodePositionTracker</code> 
 * first finds out which subarea that sender node is located and 
 * returns the nid's of all mobile nodes which are currently located in all neighbouring
 * subareas.  
 * 
 * @author Ye Ge
 * @see Channel
 * @see MobilityModel 
 *
 */
public class NodePositionTracker extends drcl.net.Module {
   
    /* configurate the ports */
    private static final String NODE_PORT_ID    = ".node";          // connect to the mobile nodes' wirelessphy   
    private static final String CONFIG_PORT_ID  = ".config";
    private static final String CHANNEL_PORT_ID = ".channel";    // connect to the channel component
    
    private Port nodePort    = addPort(NODE_PORT_ID, false);
    private Port configPort  = addPort(CONFIG_PORT_ID, false); 
    private Port channelPort = addServerPort(CHANNEL_PORT_ID);

    {
        removeDefaultUpPort();
        removeDefaultDownPort();
        removeTimerPort();
    }    
    
    /** The grid size along X-axle */
    public double dX;
    
    /** The grid size along Y-axle */
    public double dY;
    
    /** The largest x coordinate value of the simulated area */
    public double maxX;
    /** The largest y coordinate value of the simulated area */
    public double maxY;
    /** The smallest x coordinate value of the simulated area */
    public double minX;
    /** The smallest y coordinate value of the simulated area */
    public double minY;    
    
    /** Number of subareas along the X-axle. */
    public int    m;
    /** Number of subareas along the X-axle. */
    public int    n;
    
    /** 
     * Each element of this two dimensional array is 
     * a vector to hold the ids of all mobile nodes 
     * currently are located in that subarea.
     */
    public Vector g[][];
    
    /**
     * Construction function.
     *
     * @param maxX_ the largest x coordinate value of the simulated area
     * @param minX_ the smallest x coordinate value of the simulated area
     * @param maxY_ the largest y coordinate value of the simulated area
     * @param minY_ the smallest y coordinate value of the simulated area
     * @param dX_   the grid size along X-axle
     * @param dY_   the grid size along Y-axle
     */
    public NodePositionTracker(double maxX_, double minX_, double maxY_, double minY_, double dX_, double dY_) {
        super();
        maxX = maxX_; minX = minX_; dX = dX_;
        maxY = maxY_; minY = minY_; dY = dY_;
        m = ((int)((maxX - minX)/dX)) + 1;
        n = ((int)((maxY - minY)/dY)) + 1;
        g = new Vector[m][n];
		//System.out.println("size of g "+m+" "+n);
        for (int i = 0; i < m; i ++ ) 
            for (int j = 0; j < n; j ++ ) 
                g[i][j] = new Vector();
    }

    public NodePositionTracker( ) {
        super();
    }
    
    /**
     * Sets the grid parameters to divide the simulated area into subareas.
     *
     * @param maxX_ the largest x coordinate value of the simulated area
     * @param minX_ the smallest x coordinate value of the simulated area
     * @param maxY_ the largest y coordinate value of the simulated area
     * @param minY_ the smallest y coordinate value of the simulated area
     * @param dX_   the grid size along X-axle
     * @param dY_   the grid size along Y-axle
     */
    public void setGrid(double maxX_, double minX_, double maxY_, double minY_, double dX_, double dY_) {
        //System.out.println("setGrid maxX_ = " + maxX_ + " minX_ = " + minX_ + " maxY_ = " + maxY_ + " minY_ = " + minY_ + "dX = " + dX_ + " dY_ = " + dY_ );        
        maxX = maxX_; minX = minX_; dX = dX_;
        maxY = maxY_; minY = minY_; dY = dY_;
        m = ((int)((maxX - minX)/dX)) + 1;
        n = ((int)((maxY - minY)/dY)) + 1;
        g = new Vector[m][n];
        for (int i = 0; i < m; i ++ ) 
            for (int j = 0; j < n; j ++ ) 
                g[i][j] = new Vector();
    }    

    public String getName() { return "NodePositionTracker"; }
    
	public void duplicate(Object source_)
	{ 
		super.duplicate(source_);
		NodePositionTracker that_ = (NodePositionTracker)source_;
		maxX = that_.maxX; minX = that_.minX; dX = that_.dX;
        maxY = that_.maxY; minY = that_.minY; dY = that_.dY;

        m = ((int)((maxX - minX)/dX)) + 1;
        n = ((int)((maxY - minY)/dY)) + 1;
        g = new Vector[m][n];
        for (int i = 0; i < m; i ++ ) 
            for (int j = 0; j < n; j ++ ) 
                g[i][j] = new Vector();
	}    

  	protected void processOther(Object data_, Port inPort_)
	{
		String portid_ = inPort_.getID();
    
        if (portid_.equals(NODE_PORT_ID)) {
    	   /*
            if (!(data_ instanceof PositionReportContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            */
            processReport(data_, inPort_);
        }    
        else if (portid_.equals(CHANNEL_PORT_ID)) {
           /*
            if (!(data_ instanceof NeighborQueryContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            */
            processQuery(data_, inPort_);
        }
        else 
            super.processOther(data_, inPort_);
	}  

    /**
     * Processes the position update reports sent from the <code>MobilityModel</code>
     * of the mobile nodes
     */
    protected void processReport(Object data_, Port inPort_) {
        //debug("received position report" + data_.toString());        
        
        if ( !(data_ instanceof PositionReportContract.Message) ) {
            error(data_, "processReport()", inPort_, "unknown object");
            return;
        }    
        PositionReportContract.Message msg = (PositionReportContract.Message) data_;
        
        long id;  
        double X, Y, Z, X0, Y0, Z0;

        id = msg.getNid();
        X  = msg.getX();
        Y  = msg.getY();
        Z  = msg.getZ();
        X0  = msg.getX0();
        Y0  = msg.getY0();
        Z0  = msg.getZ0();
        
        int i, j, i0, j0;
        
        if ( X == X0 && Y == Y0 )  {  // first time or not moved yet 
            //System.out.println("minX "+minX+ " X "+X+" dX "+dX);
            //System.out.println("minY "+minY+ " Y "+Y+" dY "+dY);
            i = (int)((X-minX)/dX);
            j = (int)((Y-minY)/dY);
            //System.out.println("i "+i+" j "+j);
            if ( g[i][j].contains(new Long(id)) == false )
                g[i][j].insertElementAt(new Long(id), 0);
        }
        else {
            i = (int)((X-minX)/dX);
            j = (int)((Y-minY)/dY);
            i0 = (int)((X0-minX)/dX);
            j0 = (int)((Y0-minY)/dY);

            g[i0][j0].remove(new Long(id));
            g[i][j].insertElementAt(new Long(id), 0);
        }    
    }
    
    /**
     * Processes the neighbouring node inquery.
     */
    protected void processQuery(Object data_, Port inPort_) {
        if ( !(data_ instanceof NeighborQueryContract.Message) )  {
            error(data_, "processQuery()", inPort_, "unknown object");
            return;
        }
        
        NeighborQueryContract.Message msg = (NeighborQueryContract.Message) data_;
        
        long id;  
        double X, Y, Z;
        long[] nodeList;
        int  nGrids;

        X  = msg.getX();
        Y  = msg.getY();
        Z  = msg.getZ();
        nGrids = msg.getnGrids();
        
        int i, j, il, ir, jl, jr;
        
        i = (int)((X-minX)/dX);
        j = (int)((Y-minY)/dY);
        il = Math.max(0, i-nGrids);
        ir = Math.min(m-1, i+nGrids);
        jl = Math.max(0, j-nGrids);
        jr = Math.min(n-1, j+nGrids);

        int nn = 0;
        int ki, kj, kk, kv;
        for ( ki = il; ki <= ir; ki ++ )
            for ( kj = jl; kj <= jr; kj ++ )
                nn = nn + g[ki][kj].size();
        nodeList = new long[nn];
        kk = 0;
        for ( ki = il; ki <= ir; ki ++ ) {
            for ( kj = jl; kj <= jr; kj ++ ) {
                for ( kv = 0; kv < g[ki][kj].size(); kv ++ ) {
                    nodeList[kk] = ((Long)(g[ki][kj].elementAt(kv))).longValue();
                    kk = kk + 1;
                }    
            }
        }    
        // debug("query X = " + X + " Y = " + Y + " il = " + il + " i = " + i + " ir = " + ir + " jl = " + jl + " j = " + j + " jr = " + jr + " nodeList size = " + nn);
        channelPort.doSending(new NeighborQueryContract.Message(nodeList));
    }    
}















