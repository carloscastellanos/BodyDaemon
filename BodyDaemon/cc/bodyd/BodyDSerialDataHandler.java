/*
// 
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the
// Free Software Foundation; either version 2 of the License, or (at your
// option) any later version.
// 
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
 */

/*
 * Created on Aug 8, 2005
 */

/**
 * @author Carlos Castellanos
 */

package cc.bodyd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class BodyDSerialDataHandler implements SerialDataEventListener, BodyDManagerEventListener
{
    private static BodyDManager bodyDM;		// manages connections to the bodyd server
    private int bodydPort;					// portthe bodyd server will listen on
    private boolean bodydOn;					// indicates whether the bodyd server is on or off
    private boolean serialOpen;				// indicates whether the serial port is open
    // Object used to start/stop the serial stream
    private static SerialManagerWrapper serial = new SerialManagerWrapper();
    //private SerialBioEventListener bioClient;	// Object that will receive bio-events from this object
    private int numSensors;					// the number of sensors to listen for
    //private int[] buffer = null;
    
    // this where we'll store the parsed data to send out
    Hashtable bioData;
    
    //  keep track of how many biolisteners there are
    //private static ArrayList bioListeners = new ArrayList();	
    
    public BodyDSerialDataHandler(int port, int sensorNum)
    {
        bodydPort = port;
        bodydOn = false;
        serialOpen = false;
        numSensors = sensorNum;
        //serial = new SerialManagerWrapper(this);
        bioData = new Hashtable();
        
    }
    
    public BodyDSerialDataHandler()
    {
        this(-1, 1); //default is 1 sensor
    }
    
    /**
     * Reads the latest byte from the SerialManager (or any other lower-level class
     * forwarding captured serial data), puts it in a Hashtable then sends it to
     * the BodyDManager.  Also starts the BodyDManager if the sensor levels are 
     * high enough
     */
    public void serialDataEvent(int[] data)
    {
    		//System.out.println("\ndata.length=" + data.length);
/*
    		if(data.length >= serial.getReadBufferSize()) {
    			int sense1 = data[0] + (data[1] * 256);
    			System.out.println("1" + " = " + sense1);
    			int sense2 = data[2] + (data[3] * 256);
    			System.out.println("2" + " = " + sense2);
    			int sense3 = data[4] + (data[5] * 256);
    			System.out.println("3" + " = " + sense3);
    			int sense4 = data[6] + (data[7] * 256);
    			System.out.println("4" + " = " + sense4);
    			
    			bioData.put(new Integer(1), new Integer(sense1));
    			bioData.put(new Integer(2), new Integer(sense2));
    			bioData.put(new Integer(3), new Integer(sense3));
    			bioData.put(new Integer(4), new Integer(sense4));
    		}
*/ 		

    		for(int i = 0; i < data.length-1; i+=2) {
            // make it 10-bit
        		int num1 = data[i];
        		int num2 = data[i+1];
        		int total = num1 + (num2 * 256);
        		// make the hashtable keys Integers
        		Integer key = new Integer((int)(i/2) + 1);
        		//String key = Integer.toString((int)(i/2) + 1);
        		bioData.put(key, new Integer(total));
        		System.out.println(key.toString() + " = " + total);
        }
       
        if(!bodydOn)
        		analyzeSerialData(new Hashtable(bioData));
        else
             // send callback to BodyDManager
        		BodyDManager.serialBioData(new Hashtable(bioData));
        
        bioData.clear();
        
		// send a reply to the serial port
        serial.send(1);
    }
    
    public void bodydManagerEvent(String event)
    {
    		if(event.equalsIgnoreCase("STOP")) {
    			if(stopBodyDameonServer())
    				System.out.println("BodyDManager has shut down.");
    		}
    }
    
    public boolean openSerial(String sPort, int baudRate)
    {
        if(!serialOpen) {
            try {
                // set the read buffer size
                serial.setReadBufferSize(numSensors * 2);
                // open the serial port
                serialOpen = serial.openStream(sPort, baudRate, this);
            } catch(Exception ex) {
            }
        }
        return serialOpen;
    }
    
    public boolean closeSerial()
    {
        try {
            if(serial.closeStream()) {
                serialOpen = false;
                stopBodyDameonServer();
            }
        } catch (Exception ex) {
            System.out.println("Error while trying to close serial port!");
            System.out.println(ex);
        }
        return !serialOpen;
    }
    
    public boolean isSerialOpen()
    {
        return serial.isPortOpen();
    }
    
    public boolean close()
    {
        if(closeSerial()) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * @return a list of the available serial ports
     */
    public ArrayList getSerialPorts()
    {
        return serial.getPorts();
    }
    
    /*
     * Analyze the sensor data and start the BodyDManager if
     * the readings are above the threshold
     */
	private static final double thresh = 0.25; // threshold
	private static final int numReadings = 1000;
	private static int[] sensor1Values;
	private static int[] sensor2Values;
	//private static int[] sensor3Values = new int[numReadings];
	//private static int[] sensor4Values = new int[numReadings];
	private static int avg1 = 0;
	private static int avg2 = 0;
	//private static int avg3 = 0;
	//private static int avg4 = 0;
	private static int currNum = 0;
    private void analyzeSerialData(Hashtable data)
    {
    		if(sensor1Values == null || sensor2Values == null) {
    			sensor1Values = new int[numReadings];
    			sensor2Values = new int[numReadings];
    		}
    		System.out.println("analyzing serial data...");
    		ArrayList sensorVals = new ArrayList(data.size());
    		Integer temp;
    		Enumeration keys = data.keys();
    		while(keys.hasMoreElements()) {
    			temp = (Integer) data.get((Integer) keys.nextElement());
            	sensorVals.add(temp);
        }
        temp = null;
        
        /*
        ListIterator sensorIter = sensorVals.listIterator();
        int total = 0;
        while(sensorIter.hasNext()) {
        		
        		total += ((Integer) sensorIter.next()).intValue();
        }
        */
        
        sensor1Values[currNum] = ((Integer)sensorVals.get(0)).intValue();
        sensor2Values[currNum] = ((Integer)sensorVals.get(1)).intValue();
        //sensor3Values[currNum] = ((Integer)sensorVals.get(2)).intValue();
        //sensor4Values[currNum] = ((Integer)sensorVals.get(3)).intValue();
        //divisor is 10 because were measureing 10 secs (1000 readings, period is 0.01)
        // from microcontroller see BodyDaemon.bas for more
        currNum++;
		if(sensor1Values.length >= numReadings)
			avg1 = (int)averageArray(sensor1Values, 10);
		if(sensor2Values.length >= numReadings)
			avg2 = (int)(averageArray(sensor2Values, 10) * 60); // heart rate/bpm
		//if(sensor3Values.length >= numReadings)
			//avg3 = averageArray(sensor3Values, 5);
		//if(sensor4Values.length >= numReadings)
			//avg4 = averageArray(sensor4Values, 5);

		if(currNum >= numReadings)
			currNum = 0;
        
		double range = 1.0;
		double min = 0.0;
		
		// GSR range
		int gsrRange = 1400;
		int minGSR = 400;
		int currGSR = 400;
		if(avg1 < minGSR)
			currGSR = minGSR;
		else
			currGSR = avg1;	
		//double gsrLevel = (currGSR - minGSR) / gsrRange;
		double finalGSR = (((currGSR - minGSR) * range) / gsrRange) + min;
		
		// ECG range
		int ecgRange = 145;
		int minECG = 55;
		int currECG = 55;
		if(avg2 < minECG)
			currECG = minECG;
		else
			currECG = avg2;
		//double ecgLevel = (currECG - 60) / ecgRange;
		double finalECG = (((currECG - minECG) * range) / ecgRange) + min;
		
		// respiration range
		int resp = ((Integer)sensorVals.get(3)).intValue();
		int respRange = 400;
		int minResp = 300;
		int currResp = 300;
		if(resp < minResp)
			currResp = minResp;
		else
			currResp = resp;
		//double respLevel = (currResp - 400) / respRange;
		double finalResp = (((currResp - minResp) * range) / respRange) + min;
		
		double powerLevel = ((finalGSR + finalECG + finalResp) / 3);
        

        System.out.println("powerLevel="+powerLevel);

        // start bodyd if sensor values reach the threshold
        if(powerLevel > thresh) {
        		if(startBodyDaemonServer()) {
        			BodyDManager.serialBioData(data);
        			powerLevel = 0.0;
        		}
        }
        data = null;
    }
    
    /* 
     * returns true if BodyDManager was started succesfully
     */
    private boolean startBodyDaemonServer()
    {
    		try {
    			if(!bodydOn) {
    				//BodyDManager.resetValues();
    				if(bodydPort == -1) {
    					bodyDM = new BodyDManager();
    				} else {
    					bodyDM = new BodyDManager(bodydPort);
    				}
    				System.out.println("Launching BodyDManager...");
    				bodyDM.start();
    				bodydOn = true;
    				//bioClient = bodyDM;
    				//addSerialBioEventListener(bodyDM);
    				BodyDManager.addBodyDManagerEventListener(this);
    			}
    		} catch(IOException ioe) {
    			System.out.println("Server could not start because of a " + ioe);
    			//System.out.println(ioe.getMessage());
    		}
    		return bodydOn;
    }
    
    /* 
     * returns true if BodyDManager was stopped succesfully
     */
    private boolean stopBodyDameonServer()
    {
        if(bodydOn) {
        		bodyDM.stop();
        		bodydOn = false;
        		//System.out.println("BodyDManager stopped...");
        }
        //removeSerialBioEventListener(bodyDM);
        BodyDManager.removeBodyDManagerEventListener(this);
        //bioClient = null;
        bodyDM = null;
        sensor1Values = null;
        sensor2Values = null;
        avg1 = avg2 = 0;
        return !bodydOn;
    }
    
	// average the values in an array: 
	private static double averageArray(int[] intArray, int divisor)
	{
	  int total = 0;
	  double average =0.0;
	  for (int i = 0; i < intArray.length; i++) { 
	    total = total + intArray[i];
	  }
	  average = total/divisor;
	  return average;
	}
	
/*    *//**
     * adds a listener to trap serial bio-data events from this BodyDSerialDataHandler
     *//*
    public void addSerialBioEventListener(SerialBioEventListener sbel)
    {
        if(sbel != null && bioListeners.indexOf(sbel) == -1) {
            bioListeners.add(sbel);
            System.out.println("[+ SerialBioEventListener] " + sbel);
        }
    }

    *//**  
     * removes a listener from this BodyDSerialDataHandler
     *//*
    public void removeSerialBioEventListener(SerialBioEventListener sbel)
    {
        if(bioListeners.contains(sbel)) {
            bioListeners.remove(bioListeners.indexOf(sbel));
            System.out.println("[- SerialBioEventListener] " + sbel);
        }
    }

	*//**
	 * let everyone know a serial bio-data event was received
	 *//*
    private void notifyBioEventListeners(Hashtable bioData)
    {
	    if(bioListeners == null) {
	        return;
        } else {
            ListIterator iter = bioListeners.listIterator();
            while(iter.hasNext()) {
                	((SerialBioEventListener) iter.next()).serialBioEvent(new Hashtable(bioData));
            	}
        }        
    }*/
}
