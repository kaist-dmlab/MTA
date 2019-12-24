// @(#)SensorMobilityModel.java   12/2003
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

import drcl.comp.*;
import drcl.inet.data.*;
import drcl.net.*;
import drcl.inet.mac.MobilityModel;
import drcl.inet.mac.PositionReportContract;

/** This class implements the sensor mobility model which handles location, speed and mobility pattern of a sensor/target node.
*
* @author Ahmed Sobeih
* @version 1.0, 12/19/2003
*/
public class SensorMobilityModel extends drcl.inet.mac.MobilityModel
{
    public static final String REPORT_SENSOR_PORT_ID  = ".report_sensor";          
    /** Connects to the sensor nodes' node position tracker */
    protected Port report_SensorPort = addPort(REPORT_SENSOR_PORT_ID, false);

    public SensorMobilityModel(long nid_) {
        super(nid_);
    }

    public SensorMobilityModel( ) {
        super();
    }

    /** Sets the dimensions of the terrain of the sensor network */    
    public void setTopologyParameters(double maxX_, double maxY_, double minX_, double minY_)  {
        maxX = maxX_; maxY = maxY_; maxZ = 10.0; 
        minX = minX_; minY = minY_; minZ = 0.0;
	dX = 1.0 ; dY = 1.0 ; dZ = 1.0 ;
    }    
    
    /** Sets the dimensions of the terrain of the sensor network */ 
    public void setTopologyParameters(double maxX_, double maxY_, double maxZ_, double minX_, double minY_, double minZ_)  {
        maxX = maxX_; maxY = maxY_; maxZ = maxZ_; 
        minX = minX_; minY = minY_; minZ = minZ_;
	dX = 1.0 ; dY = 1.0 ; dZ = 1.0 ;
    }    

    /** Sends position report to the mobility tracker components */
    public synchronized void reportPosition(boolean forcedReport) {
        if (forcedReport == true || (X-X0) >= dX || (X0-X) >= dX || (Y-Y0) >= dY || (Y0-Y) >= dY || (Z-Z0) >= dZ || (Z0-Z) >= dZ ) 
	{   // if not forced report, report only if the change of position has exceeded the boundary
		if ( reportPort.anyPeer() )
		{
			reportPort.doSending(new PositionReportContract.Message(nid, X, Y, Z, X0, Y0, Z0));
		}
            X0 = X; Y0 = Y; Z0 = Z;
        }    

        if (forcedReport == true || (X-X0) >= 1.0 || (X0-X) >= 1.0 || (Y-Y0) >= 1.0 || (Y0-Y) >= 1.0 || (Z-Z0) >= 1.0 || (Z0-Z) >= 1.0 ) 
	{   // if not forced report, report only if the change of position has exceeded the boundary
		if ( report_SensorPort.anyPeer() )
		{
			report_SensorPort.doSending(new SensorPositionReportContract.Message(nid, X, Y, Z, X0, Y0, Z0));
		}
		X0 = X; Y0 = Y; Z0 = Z;
        }
    }    
     
    /** Processes location query and replies with current location  */   
    protected synchronized void processQuery(Object data_) 
    {
        updatePosition();

	/* queryPort is a server port, so it can be connected to multiple ports with no problem */
	/* If query comes from WirelessPhy */
        if (data_ instanceof PositionReportContract.Message) 
	{
		if ( queryPort.anyPeer() )
			queryPort.doSending(new PositionReportContract.Message(X, Y, Z));
        }

	/* If query comes from SensorPhy */
        if (data_ instanceof SensorPositionReportContract.Message) 
	{
		if ( queryPort.anyPeer() )
			queryPort.doSending(new SensorPositionReportContract.Message(X, Y, Z));
        }       
    }    
}
