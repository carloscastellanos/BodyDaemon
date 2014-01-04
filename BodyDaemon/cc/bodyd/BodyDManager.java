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
 * Created on Aug 9, 2005
 *
 * This class does does the "heavy lifting" of establishing and managing
 * client sockets, the server socket and preparing the incoming serial data
 * for eventual transmission to the clients
 * 
 * Note: Even though this class must be made into an object
 * (because it needs to run as a thread), it is intended to
 * be a static member of whatver class uses it.  The reason
 * for this is that we probably don't want/need multiple
 * server objects handling the same serial data.  Hence, with
 * the exception of the thread-specific methods (start, stop, 
 * run), every method of this class is static as it is accessing
 * the same serial data (i.e. the same serial port).  Another
 * possibility is to make this class an inner class of
 * BodyDSerialDataHandler or a package private class
 */

/**
 * @author Carlos Castellanos
 */

package cc.bodyd;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import cc.averages.MovingAverage;
import cc.averages.WeightedAverage;
import cc.bodyd.logging.BioLogger;


public class BodyDManager implements Runnable
{
    private static ServerSocket bodydServer = null; // we only want one server... voltile?
    private Thread runner = null;
    private boolean running = false;
    
	// --- server properties --- //
    
    //  maximum number of overall sockets allowed
	private static int maxSockets = 1;
	//	 rate at which the data is sent to client (once socket connection is established)
	//private long dataTransferRate;
	//	 overall power level required to run
	private static final double powerLevelReq = 0.15;
	private static double currPowerLevel = 0.2;
	private static final double powerDrain = 0.0000001;
	private static final double loggingLevelReq = 0.75;
	private static final double nudgeValue = 0.0001;
	//	 timeout per socket (in milliseconds)
	private static int socketTimeout = 10000;
	
	// logging
	// make a new biologger with a capacity of 120
	// (120 secs of logging to memory)
	private static BioLogger logger = new BioLogger(120);
	private static boolean logging = false;
	
	//private BodyD bodyd;	// the bodyd server;
	//private boolean first; should these be volatile?
	private static Hashtable sensorData = new Hashtable();
	private static Hashtable serverData = new Hashtable();
	
	// keep track of how many threads of this class there are
	protected static Vector bodyDMThreads = new Vector();
	
    //  keep track of how many listeners of this class there are
    private static Vector bodyDMListeners = new Vector();
    
    // averages of sensors
    private static int avgGSR = 0; // gsr
    private static int avgECG = 0; // ecg/heart
    private static int avgResp = 0; // respiration
    private static double avg = 0;
    
	private final static int secs = 5;
	private final static int interval = 4; 	// data is sent every 1/4 of a second
	// number of readings to take
	// we're averaging over 5 secs
	private final static int numReadings = secs * interval;
	private final static double incr = 60 / secs;
	//private static CircularBuffer circBuff1;
	//private static CircularBuffer circBuff2;
	private static MovingAverage maECG;
	private static MovingAverage maECG2;
	private static MovingAverage maGSR;
	private static WeightedAverage waResp;
    
	public BodyDManager(int port) throws IOException
	{
		if(bodydServer == null || running == false) {
	        bodydServer = new ServerSocket(port);
	        System.out.println("\n====================================================================");
	        System.out.println("****** bodyd server ready to accept connections on port " 
	        		+ bodydServer.getLocalPort() + " ******");
	        System.out.println("====================================================================");
	    } else {
	        System.out.println("bodyd server already running on port " + bodydServer.getLocalPort());
	        throw new ConnectException("Only one bodyd server is allowed at a time!");
	    }
	}

	public BodyDManager() throws IOException
	{
		this(59000); // default port is 59000
	}
	
	public static void serialBioData(Hashtable data)
	{
		// --------------------------------------------------------- //
	    // the assumption here is that GSR is Sensor 1, ECG is
	    // Sensor 2, EMG is Sensor 3 and respiration is Sensor 4
	    // we also add "AVG", for the average of all the sensors
		// (may want to put this info in an external resource file
		// and load it dynamically at startup)
		// --------------------------------------------------------- //
		
		int gsr = ((Integer)data.get(new Integer(1))).intValue();
		int ecg = ((Integer)data.get(new Integer(2))).intValue();
		int resp = ((Integer)data.get(new Integer(4))).intValue();
		
		// emg is seperate
		sensorData.put("EMG", new Integer(((Integer)data.get(new Integer(3)) ).intValue()));

		/*
		// copy it into an ArrayList
		ArrayList sensorVals = new ArrayList(sensorData.size());
		Integer temp;
		Enumeration keys = sensorData.keys();
		while(keys.hasMoreElements()) {
			temp = (Integer) sensorData.get((Integer) keys.nextElement());
            	sensorVals.add(temp);
        }
        temp = null;
        */
		

        // GSR affects socket timeout
        // (a spike knocks down timeout, maybe down to 0)
        setSocketTimeout(gsr);
        // ECG (heart rate) affects max sockets
        // higher rate = more sockets available at one time
        setMaxSockets(ecg);
        // EMG hit will nudge up the overall power levels
        nudgeUpPowerLevels(((Integer)sensorData.get("EMG")).intValue());
        // once socket connection is established, Respiration affects
        // the rate/pace at which the server keeps sending the data
        setDataTransferRate(resp);
        // turns logging on if power levels are high enough
        // avg of all sensors affects power levels
        setPowerLevel();
        setLogging();

        //setPowerLevelRequirements();
        
	    //prepareData();
        
		//int avg = (int)((avgGSR + avgECG + avgResp) / 3);
		
		// === copy the sensor and server data into the static Hashtables of this class === //
		
		sensorData.put("GSR", new Integer(avgGSR));
		serverData.put("timeout", new Integer(socketTimeout));
		
		sensorData.put("ECG", new Integer(avgECG));
		serverData.put("maxsockets", new Integer(maxSockets));
		
		if(((Integer)(sensorData.get("EMG"))).intValue() > 0) {
			serverData.put("nudgepower", new Boolean(true));
		} else {
			serverData.put("nudgepower", new Boolean(false));
		}
		sensorData.put("Respiration", new Integer(avgResp));
		serverData.put("datarate", new Long(BodyD.getDataRate()));
		
		sensorData.put("avg", new Double(avg));
		serverData.put("powerlevel", new Double(currPowerLevel));
		
		if(logging)
			doLogging();
	}
	
	public synchronized void start()
	{
        runner = new Thread(this);
        runner.setPriority(Thread.MAX_PRIORITY);
        runner.start();
        running = true;
        
        maECG = new MovingAverage(numReadings, 0.5);
        maECG2 = new MovingAverage(numReadings * 2, 60);
        maGSR = new MovingAverage(numReadings, 200);
        waResp = new WeightedAverage(55, 55, 400);
	}
	
	public synchronized void stop()
	{
	    if(runner != null) {
	        if(runner != Thread.currentThread()) {
	            System.out.println("\n****** Shutting down the bodyd server... ******");
	            runner.interrupt();
	            System.out.println("BodyDManager shutting down...");
	            running = false;
	            try {
	            		bodydServer.close();
	            		bodydServer = null;
	            } catch(IOException ioe) {
	            		System.out.println("Error shutting down the bodyd server! " + ioe);
	            }
	        }
	        runner = null;
		    notifyBodyDManagerEventListeners("STOP");
		    bodyDMThreads.removeElement(this);
	    }
	    
	}
	
	public void run()
	{
		try {
			bodyDMThreads.addElement(this);
			while(running && !Thread.interrupted() && !(bodydServer == null)) {
			// start a bodyd thread if the socket is available
				if(BodyD.bodyDObjs.size() < maxSockets) {
					Socket client = bodydServer.accept(); // accept a connection
					System.out.println(">>>>>> Accepted connection from " + client.getInetAddress() + " <<<<<<");
					try {
						client.setTcpNoDelay(true); // good idea to do this, avoid delays in sending data
						client.setSoTimeout(socketTimeout); // socket timeout
					} catch(SocketException se) {
						System.out.println("*** Socket error: " +
								"unable to set the socket timeout and/or tcp no delay! ***");
					}
					// create a bodyd thread and give it the data to send to the client
					BodyD bodyd = new BodyD(client);
					// start bodyd
					bodyd.start();
					// power drain
					currPowerLevel = currPowerLevel - powerDrain;
				} else {
					//System.out.println("*** bodyd server cannot accept anymore connections at this time ***");
				}
			}
		} catch(IOException ioe) {
			System.out.println("*** Error establishing a connection to bodyd " + ioe + " ***");
		}
		/*
        finally
        {
        		if(bodyDMThreads.contains(this)) {
        			bodyDMThreads.removeElement(this);
            		System.out.println("removing a BodyDManager object from the list");
        		}
        }
        */
		//stop();
	}
	
	// uses the average sensor levels to set the power level
	// all values are mapped from their respective ranges to a
	// double value berween 0 and 1.
	private static int counter = 0;
	private static int readings = 0;
	private static void setPowerLevel()
	{
		counter++;
		readings++;
		System.out.println("counter="+counter + " readings="+readings);
		
		double range = 1.0;
		double min = 0.0;
		
		// GSR range
		int gsrRange = 1400;
		int minGSR = 300;
		int currGSR = 300;
		if(avgGSR < minGSR)
			currGSR = minGSR;
		else
			currGSR = avgGSR;	
		//double gsrLevel = (currGSR - minGSR) / gsrRange;
		double finalGSR = (((currGSR - minGSR) * range) / gsrRange) + min;
		
		// ECG range
		int ecgRange = 145;
		int minECG = 55;
		int currECG = 55;
		if(avgECG < minECG)
			currECG = minECG;
		else
			currECG = avgECG;
		//double ecgLevel = (currECG - 60) / ecgRange;
		double finalECG = (((currECG - minECG) * range) / ecgRange) + min;
		
		// respiration range
		int respRange = 450;
		int minResp = 300;
		int currResp = 300;
		if(avgResp < minResp)
			currResp = minResp;
		else
			currResp = avgResp;
		//double respLevel = (currResp - 400) / respRange;
		double finalResp = (((currResp - minResp) * range) / respRange) + min;
		
		avg = (finalGSR + finalECG + finalResp) / 3;
		currPowerLevel = ((finalGSR + finalECG + finalResp) / 3) - powerDrain;

		if(currPowerLevel < 0.0)
			currPowerLevel = 0.0;
		
	    if(currPowerLevel < powerLevelReq && counter >= 200) {
	    		// close all bodyd threads
	    		System.out.println("\n*** Power levels too low (" + currPowerLevel + 
	    					"), BodyDManager closing down... ***");
	    		counter = 0;
	        if(closeAllConnections()) {
	        		//BodyDManager bdm;
	        		Enumeration e = bodyDMThreads.elements();
	        		try {
	        			while(e.hasMoreElements()) {
	            			((BodyDManager) e.nextElement()).stop();
	        			}
	        			//bodyDMThreads.removeAllElements();
	        		} catch(Exception ex) { }
	        } else {
	        		System.out.println("*** Error closing the BodyD threads/connections. ***");
	        }
		}
	    if(readings >= numReadings)
	    		readings = 0;
		
	}
	
	public static double getPowerLevel()
	{
	    return currPowerLevel;
	}
	
	//private static int[] gsrValues = new int[numReadings]; // holds the level values
	private static int prevAVG = -1;
	private static int change = 0;
	//private static int currGSRNum = 0;
	// sets the socket timeout of the server
	// based on the running average of gsr reading
	private static void setSocketTimeout(int level)
	{
		/*
		// put the reading into the array 
		gsrValues[currGSRNum] = level;
		currGSRNum++;
		//System.out.println("curGSRNum=" + currGSRNum + " gsrValues.length=" + gsrValues.length);
		
		if(gsrValues.length >= numReadings) {
			avgGSR = (int)(Math.round(averageArray(gsrValues, secs)));
			System.out.println("avgGSR = " + avgGSR + " Hz");
			// find the amountof change in GSR
			if(prevAVG == -1) {
				prevAVG = avgGSR;
			} else {
				change = avgGSR - prevAVG;
				prevAVG = avgGSR;
			}
			//gsrValues = null;
			if(currGSRNum >= numReadings)
				currGSRNum = 0;
			//gsrValues = new int[numReadings];
			 */
		// get the average
		double leveld = (double)level;
		double avgSoFar;
		avgSoFar = maGSR.update(leveld);
		avgGSR = (int) (Math.round( (avgSoFar * interval) / 2));
		
		if(prevAVG == -1) {
			prevAVG = avgGSR;
		} else {
			change = avgGSR - prevAVG;
			prevAVG = avgGSR;
		}
		
		// large spikes in GSR (greater than 100)
		// result in immediate change in socket timeout to 1 ms;
		if(Math.abs(change) <= 100) {
			// timeout range
			int toRange = 50000;
			int minTO = 10000;
			int gsrRange = 1400;
			int minGSR = 300;	
			//double gsrLevel = (currGSR - minGSR) / gsrRange;
			double finalTO = (((avgGSR - minGSR) * toRange) / gsrRange) + minTO;

			socketTimeout = (int)finalTO;
		} else {
			socketTimeout = 1;
		}
	    
		if (socketTimeout < 1)
			socketTimeout = 1;
		
		System.out.println("avgGSR = " + avgGSR + " Hz");
		System.out.println("--socketTimeout = " + socketTimeout);
		//}
	}
	
	public static int getSocketTimeout()
	{
	    return socketTimeout;
	}
	

	//private static int[] ecgValues = new int[numReadings]; // holds the level values
	//private static int currECGNum = 0;

	//private static double[] ecgAvg = new double[12];
	private static int currECGAvgNum = 0;
	//private static double finalAvg = 1.0;
	// sets the maximum sockets the server can support
	// based on the running average of the heart rate reading
	private static void setMaxSockets(int level)
	{
		/*
		// put the reading into the array 
		ecgValues[currECGNum++] = level;
		if(ecgValues.length >= numReadings) {
			ecgAvg[currECGAvgNum++] = averageArray(ecgValues, secs);
			if(ecgAvg.length >= 12) {
				finalAvg = averageArray(ecgAvg, 12);
				if(currECGAvgNum >= 12)
					currECGAvgNum = 0;
			}
		*/
		/*
		ecgValues[currECGNum++] = level;
		if(ecgValues.length >= numReadings) {

			//ecgValues = null;
			if(currECGNum >= numReadings)
				currECGNum = 0;
			//ecgValues = new int[numReadings];
			
			avgECG = (int) (Math.round(averageArray(ecgValues, secs) * 60.0));
			System.out.println("avgECG = " + avgECG + " bpm");
			*/
		// get the average
		/*
		double leveld = (double)level;
		double avgSoFar = raECG.update(leveld);
		avgECG = (int) (Math.round( (avgSoFar * incr * numReadings) / 2));
		*/
		// get the average
		double leveld = (double)level;
		double avgSoFar = maECG.update(leveld);
		double avg = (avgSoFar * incr * numReadings) / 2;
		if(currECGAvgNum > numReadings * 2) { // 10 secs
			avgECG = (int)maECG2.update(avg);
		} else {
			avgECG = (int)Math.round(avg);
			maECG2.update(avg);  // update anyway
			currECGAvgNum++;
		}

		/*
		circBuff1.add(new Integer(level));
		circBuff2.add(new Integer(level));
		
		if(circBuff2.isFull()) {
			int[] sixtySecs = new int[circBuff2.CAPACITY];
			Object[] obj2 = circBuff2.toArray();
			for(int i = 0; i < obj2.length; i++) {
				Integer intObj2 = (Integer)obj2[i];
				sixtySecs[i] = intObj2.intValue();
			}
			avgECG = (int) (Math.round(averageArray(sixtySecs, 60)));
		} else {
			if (circBuff1.isFull()) {
				int[] fiveSecs = new int[circBuff1.CAPACITY];
				Object[] obj = circBuff1.toArray();
				for(int j = 0; j < obj.length; j++) {
					Integer intObj = (Integer)obj[j];
					fiveSecs[j] = intObj.intValue();
					System.out.println("fiveSecs="+fiveSecs[j]);
				}
				avgECG = (int) (Math.round(averageArray(fiveSecs, secs) * 60.0));
			}
		}
		*/
		
		// ECG range
		double min = 1.0;
		double range = 19.0; // more than 20 sockets should require thread pooling
		int ecgRange = 145;
		int minECG = 55;
		//double ecgLevel = (currECG - 60) / ecgRange;
		double finalECG = (((avgECG - minECG) * range) / ecgRange) + min;
			
		if(finalECG < 1.0)
			finalECG = 1.0;
			
		maxSockets = (int)Math.round(finalECG);
			
		System.out.println("avgECG = " + avgECG + " bpm");
		System.out.println("--maxSockets = " + maxSockets);
	}
	
	public static int getMaxSockets()
	{
	    return maxSockets;
	}
	
	//private static double currentEstimate = 0;		// result of the weighted averaged reading
	//private static double lastEstimate = 0; 			// previous result
	//private static int weight;
	
	public static void setDataTransferRate(int level)
	{
		// weighted average of the respiration is calculated
		// avgECG is the weight
		// weighted averge is done for the purposes
		// of setting the data transfer rate
		// the raw sensor value is used for the avgResp variable
		
		//weight = (int) (((double)avgECG / 150) - 0.3);
		
/*
		weight = avgECG;
		
		avgResp = level;
		
		// filter the sensor's result:
		currentEstimate = weightedAverage(level, weight, lastEstimate);
		// save the current result for future use:
		lastEstimate = currentEstimate;
		
		//avgResp = currentEstimate;
		double weightedResp = currentEstimate;
*/
		avgResp = level;
		
		double weightedResp = waResp.update(level, avgECG);
		
		// respiration range
		double rRange = 4900.0;
		double rMin = 100.0;
		int respRange = 450;
		int minResp = 300;
		//double respLevel = (currResp - 400) / respRange;
		double finalResp = (((weightedResp - minResp) * rRange) / respRange) + rMin;
		
		BodyD.setDataRate(Math.round(finalResp));
		
		System.out.println("avgResp = " + avgResp);
		System.out.println("--dataRate = " + BodyD.getDataRate() + "  weightedResp = " + weightedResp);
	}
	
	public static long getDataTransferRate()
	{
	    return BodyD.getDataRate();
	}
	
	/*
	public void setPowerLevelRequirements()
	{
	    //powerLevelReq = d;
	}
	
	public double getPowerLevelRequirements()
	{
	    return powerLevelReq;
	}
	*/
	
	public static int getNumSockets()
	{
	    return BodyD.bodyDObjs.size();
	}
	
	public static Vector getSockets()
	{
		return BodyD.bodyDObjs;
	}
	
	private static void nudgeUpPowerLevels(int value)
	{
		// need a buffer? //
		
		if(value > 0)
			currPowerLevel = currPowerLevel + nudgeValue;

		System.out.println("EMG = " + value);
		System.out.println("--currPowerLevel = " + currPowerLevel);
	}
	
	public boolean isRunning()
	{
	    return running;
	}

	/*
	private static boolean closeConnection(int index)
	{
		boolean closed = false;
		BodyD bd = (BodyD) bodyDObjs.elementAt(index);
        try {
            bd.stop();
            //bd.connection.close();
            bodyDObjs.removeElementAt(index);
            closed = true;
        } catch(Exception e) {
        		System.out.println("=== error closing a BodyD connection === " + e);
        		e.printStackTrace();
        }
        
        return closed;
	}
	*/
	
	private static boolean closeAllConnections()
	{
        boolean closed = false;
        BodyD bd;
        Enumeration en = BodyD.bodyDObjs.elements();
        try {
        		System.out.println("*** closing all BodyD connections... ***");
        		while(en.hasMoreElements()) {
        			bd = (BodyD) en.nextElement();
        			bd.stop();
        			//bd.listener.interrupt();
        			//bd.connection.close();
            }
        		closed = true;
        		System.out.println("*** all BodyD connections closed ***");
        } catch(Exception e) {
        		System.out.println("=== error closing BodyD connections === " + e);
        		e.printStackTrace();
        }
        
        return closed;
	}
	
    /**
     * adds a listener to trap events from this BodyDManager
     */
	public static void addBodyDManagerEventListener(BodyDManagerEventListener bdmListener)
	{
        if(bdmListener != null && bodyDMListeners.indexOf(bdmListener) == -1) {
            bodyDMListeners.add(bdmListener);
            System.out.println("[+ BodyDManagerEventListener] " + bdmListener);
        }
	}

    /**  
     * removes a listener from this BodyDManager
     */
	public static void removeBodyDManagerEventListener(BodyDManagerEventListener bdmListener)
	{
        if(bodyDMListeners.contains(bdmListener)) {
            bodyDMListeners.remove(bodyDMListeners.indexOf(bdmListener));
            System.out.println("[- BodyDManagerEventListener] " + bdmListener);
        }
	}
    
	/**
	 * let everyone know an event was received
	 */
	private static void notifyBodyDManagerEventListeners(String msg)
	{
	    if(bodyDMListeners == null) {
	        return;
        } else {
            ListIterator iter = bodyDMListeners.listIterator();
            while(iter.hasNext()) {
                	((BodyDManagerEventListener) iter.next()).bodydManagerEvent(msg);
            	}
        } 
	}
    
	// returns the curent sensor data
	public static Hashtable getCurrentSensorData()
	{
		return sensorData;
	}
	
	// returns the curent server data
	public static Hashtable getCurrentServerData()
	{
		return serverData;
	}
	
	// ========  logging  ========= //
	private static final long ONE_SECOND = 1000;
	private static long delayTime = ONE_SECOND; // default
	private static long prevTime = System.currentTimeMillis();
	
	private static void doLogging(long del)
	{
		// logging
		delayTime = del;
		
		if(canLog()) {
			// first log to memory
			Hashtable sensors = new Hashtable(sensorData);
			Hashtable server = new Hashtable(serverData);
			Hashtable h = new Hashtable(2);
			h.put("sensors", sensors);
			h.put("server", server);
			logger.logToMemory(new Hashtable(h));
			
			/*
			// now log to file
			StringBuffer logString = new StringBuffer(512);
			
			// sensor data
			Enumeration sensorKeys = sensorData.keys();
			while(sensorKeys.hasMoreElements()) {
				String k = (String)sensorKeys.nextElement();
				String sensorVal = null;
				String prop = null;
				String serverpropVal = null;
				if(k.equalsIgnoreCase("GSR")) {
					prop = "timeout";
					sensorVal = ((Integer)(sensorData.get(k))).toString();
					serverpropVal = ((Integer)(serverData.get(prop))).toString();
				} else if(k.equalsIgnoreCase("ECG")) {
					prop = "maxsockets";
					sensorVal = ((Integer)(sensorData.get(k))).toString();
					serverpropVal = ((Integer)(serverData.get(prop))).toString();
				} else if(k.equalsIgnoreCase("EMG")) {
					prop = "nudgepower";
					sensorVal = ((Integer)(sensorData.get(k))).toString();
					serverpropVal = ((Double)(serverData.get(prop))).toString();
				} else if(k.equalsIgnoreCase("Respiration")) {
					prop = "datarate";
					sensorVal = ((Integer)(sensorData.get(k))).toString();
					serverpropVal = ((Integer)(serverData.get(prop))).toString();
				} else if(k.equalsIgnoreCase("avg")) {
					prop = "powerlevel";
					sensorVal = ((Integer)(sensorData.get(k))).toString();
					serverpropVal = ((Double)(serverData.get(prop))).toString();
				}
				
				logString.append(k + "=" + sensorVal + " " + prop + "=" + serverpropVal);
			} // end while
			
			// --- log it! --- //
			logger.log(logString.toString());
			*/
		}
		
	}

	private static void doLogging()
	{
		doLogging(ONE_SECOND);
	}
	/*
	private static void doExceptionLogging()
	{
		
	}
	*/
	
	private static boolean canLog()
	{
		boolean canLog = false;
		long time = System.currentTimeMillis();
		long diff = time - prevTime;
		if(diff >= delayTime) {
			canLog = true;
        		prevTime = time;
		} else {
			canLog = false;
		}
		return canLog;
	}
	
	protected static LinkedList getLogsFromMemory()
	{
		return logger.getLogsFromMemory();
	}
	
	public static void setLoggingDelay(long delay)
	{
		delayTime = delay;
	}
	
	public static long getLoggingDelay()
	{
		return delayTime;
	}
	
	private static void setLogging()
	{
		if(!logger.isOn()) {
			logging = false;
			if(currPowerLevel >= loggingLevelReq) {
				// trun on logging if power levels are high enough
				logger.setOn(true);
	    			logging = true;
	    			logger.log("Starting the BodyDaemon logger...");
	    			System.out.println("Starting the BodyDaemon logger...");
			}
	    } else if(currPowerLevel < loggingLevelReq) {
	    		// turn off logging
	    		logger.log("Halting the BodyDaemon logger...");
	    		System.out.println("Halting the BodyDaemon logger...");
	    		logger.setOn(false);
			logging = false;
	    }
	}
	
	public static boolean isLogging()
	{
	    return logging;
	}
	// ===== end logging methods ===== //
	
	/*
	// average the values in an array: 
	private static double averageArray(int[] intArray, int divisor)
	{
	  int total = 0;
	  double average = 0.0;
	  for (int i = 0; i < intArray.length; i++) { 
	    total = total + intArray[i];
	  }
	  average = total/(double)divisor;
	  return average;
	}
	*/
	
	/*
	private static double averageArray(double[] dblArray, int divisor)
	{
	  double total = 0.0;
	  double average = 0.0;
	  for (int i = 0; i < dblArray.length; i++) { 
	    total = total + dblArray[i];
	  }
	  average = total/divisor;
	  return average;
	}
	*/
	
	/*
	// filter the current result using a weighted average filter:
	private static double weightedAverage(int rawValue, int weight, double lastValue)
	{
		double value = 0;
		//double x = 0.0;
		// convert the weight number (avgECG) to a value between 0 and 1:
		//x = (double) (weight/1023.0);
		// ECG range
		int ecgRange = 145;
		int minECG = 55;
		int currECG = 55;
		if(weight < minECG)
			currECG = minECG;
		else
			currECG = weight;
		//double ecgLevel = (currECG - 60) / ecgRange;
		double finalECG = (((currECG - minECG) * 1.0) / ecgRange) + 0.0;
		// run the filter:
		value = finalECG * (double)rawValue + (1.0-finalECG)*(double)lastValue;
		// return the result:
		return value;
	}
	*/
	
	/*
	static void resetValues()
	{
		currPowerLevel = 0.5;
		maxSockets = 1;
		socketTimeout = 10000;
	    avgGSR = 0;
	    avgECG = 0;
	    avgResp = 0;
	    bodyDMThreads.clear();
	    bodyDMListeners.clear();
	    sensorData.clear();
	    serverData.clear();
	    logging = false;
	    
		gsrValues = new int[numReadings];
		prevAVG = -1;
		change = 0;
		currGSRNum = 0;
		
		ecgValues = new int[numReadings];
		currECGNum = 0;
		
		currentEstimate = 0;
		lastEstimate = 0;
	}
	*/
}
