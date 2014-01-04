/*
// 2005 by Carlos Castellanos
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
 *	Created on Aug 8, 2005
 *
 *	This class manages the serial port. It handles opening and closing
 *	the port, and handles the callback when new data arrives.  
 *	the serialEvent() method here is really just a stub, though, 
 *	and you should modify it to do what you want with the data.
 *
 *   Adapted from a SerialManager class by Tom Igoe (which was itself
 *	adapted form an example by Dan O'Sullivan)
 *  see http://www.tigoe.net/pcomp/code/archives//000320.shtml#000320
 */

/**
 * @author Carlos Castellanos
 */

package cc.bodyd;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.TooManyListenersException;
 
public class SerialManager implements SerialPortEventListener
{
    private SerialPort mySerialPort = null;    
    private InputStream in;	// input data from port
    private OutputStream out; // output data to port
    private boolean portOpen;	// status of the port
    private int readBufferSize;
    private int writeBufferSize;
    //  holds the list of SerialDataEventListener objects
    private static ArrayList listeners = new ArrayList();
    
	/*
     * 	Constructors.  Set status and speed of port and initlialize list of listeners
     */
    
    /**
     * Initializes a SerialManager with default read and write buffer sizes of 1
     */
    public SerialManager()
    {
        this(1, 1); // default read & write buffer size is 1
    }
    
    /**
     * Initializes a SerialManager with the specified read and write buffer sizes
     * @param readBufferSize	the read buffer size
     * @param writeBufferSize	the write buffer size
     */
    public SerialManager(int readBufferSize, int writeBufferSize)
    {
        this.readBufferSize = readBufferSize;
        this.writeBufferSize = writeBufferSize;
        portOpen = false;
    }
    
    /**
     * Opens a serial port with default data bits (8), stop bits(1)
     * and parity (none)
     * @param whichPort the serial port to open
     * @param whichSpeed the baud rate
     * @return true if a port was successfully opened, false otherwise
     */ 
    public boolean openPort(String whichPort, int whichSpeed)
    {
        try {
            //find the port
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(whichPort);
            //try to open the port
            mySerialPort = (SerialPort) portId.open("Serial Port" + whichPort, 2000);
            //configure the port
            try {
                mySerialPort.setSerialPortParams(whichSpeed,
                        SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } catch (UnsupportedCommOperationException e) {
                System.out.println("Comm parameter error: Probably an unsupported speed");
            }
            //establish streams for reading and writing to the port
            try {
                in = mySerialPort.getInputStream();
                out = mySerialPort.getOutputStream();
                System.out.println("Input and output streams established for serial I/O");
            } catch (IOException e) {
                System.out.println("couldn't establish streams for serial I/O");
            }
            // add a listener to trap serial events:
            try {
                mySerialPort.addEventListener(this);
                mySerialPort.notifyOnDataAvailable(true);
                System.out.println("SerialManager: listening for serial events at a " +
                		"baud rate of " + mySerialPort.getBaudRate() +  "...");
            } catch (TooManyListenersException e) {
                System.out.println("couldn't add serial I/O listener");
            }

            // port successfully opened; set portOpen to true
            portOpen = true;
            System.out.println("readBufferSize="+readBufferSize);
            return portOpen;
            
        } catch (Exception e) {
            // if we couldn't open the port, assume it's in use:
            System.out.println("Port in use or does not exist");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return portOpen;
        }
    }
    
    /**
     * Opens a serial port with the specified parameters
     * @param whichPort the serial port to open
     * @param whichSpeed the baud rate
     * @param dataBits the number of data bits
     * @param stopBits the number of stop bits
     * @param parity the parity
     * @return true if a port was successfully opened, false otherwise
     */
    public boolean openPort(String whichPort, int whichSpeed, int dataBits, int stopBits, int parity)
    {
        try {
            //find the port
            CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(whichPort);
            //try to open the port
            mySerialPort = (SerialPort) portId.open("Serial Port" + whichPort, 2000);
            //configure the port
            try {
                mySerialPort.setSerialPortParams(whichSpeed, dataBits, stopBits, parity);
            } catch (UnsupportedCommOperationException e) {
                System.out.println("Comm parameter error: Probably an unsupported speed");
            }
            //establish streams for reading and writing to the port
            try {
                in = mySerialPort.getInputStream();
                out = mySerialPort.getOutputStream();
                System.out.println("Input and output streams established for serial I/O");
            } catch (IOException e) {
                System.out.println("couldn't establish streams for serial I/O");
            }
            // add a listener to trap serial events:
            try {
                mySerialPort.addEventListener(this);
                mySerialPort.notifyOnDataAvailable(true);
                System.out.println("SerialManager: listening for serial events at a " +
                		"baud rate of " + mySerialPort.getBaudRate() +  "...");
            } catch (TooManyListenersException e) {
                System.out.println("couldn't add serial I/O listener");
            }

            // port successfully opened; set portOpen to true
            portOpen = true;
            return portOpen;
            
        } catch (Exception e) {
            // if we couldn't open the port, assume it's in use:
            System.out.println("Port in use or does not exist");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return portOpen;
        }
    }
    
	/**
	 * Closes the port and streams, and disposes
	 * @return true if the port was closed.
	 *  
	 */
	public boolean closePort()
	{
	    in = null;
	    out = null;
	    mySerialPort.close();
	    portOpen = false;
	    return !portOpen;
	}
   
	/**
	 * @return an Arraylist of the serial ports 
	 */
	public ArrayList getPorts()
	{
	    // Set up an ArrayList to store the ports:
        ArrayList portsAvailable = new ArrayList();
        Enumeration portList = CommPortIdentifier.getPortIdentifiers();
        
        // count the ports:
        int numberOfPorts = 0;
        
        // print out a header for the list of ports:
        System.out.println("No.\tSerial port\t\tOwner");
        
        // print out the list of ports:
        while (portList.hasMoreElements()) {
            
            // get all the ports available:
            CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();
            
            // if they're serial ports, add them to the Arraylist
            // and print them out:
            if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                portsAvailable.add(portId.getName());
                System.out.println(numberOfPorts + "\t" +portId.getName() + "\t\t"
                        + portId.getCurrentOwner());
                // increment number of ports:
                numberOfPorts++;
            }
        }
        // return the ArrayList:
        return portsAvailable;
    }
    
	
	/*
	 * ==================================================================
	 * These two send() methods need to be updated to reflect the changes
	 * that my be made at runtime to the writeBufferSize field
	 * ==================================================================
	 */

    /**
     * Sends a byte out the serial port: 
     */   
	public void send(int outputByte)
	{
	    try {
            out.write(outputByte);
	    } catch (IOException e) {
            System.out.println("error: couldn't send byte");
        }
    }
	
    /**
     * Sends a String out the serial port: 
     */   
    public void send(String stringToSend)
    {
        try {
            out.write(stringToSend.getBytes());
        } catch (IOException e) {
            System.out.println("error: couldn't send string");
        }       
    }
    
    /**
     * @return a boolean indicating whether or not the port is open.
     */
    protected boolean isPortOpen()
    {
        return portOpen;
    }
    
    /**
     * 
     * @return the size of the write buffer
     */
    public int getWriteBufferSize()
    {
        return writeBufferSize;
    }
    
    /**
     * Sets how many bytes to write at once to the serial port.
     * The data gets stored into a buffer of length <CODE>size</CODE>
     * @param size the buffer size
     */
    public void setWriteBufferSize(int size)
    {
        writeBufferSize = size;
    }
    
    /**
     * 
     * @return the size of the read buffer
     */
    public int getReadBufferSize()
    {
        return readBufferSize;
    }
    
    /**
     * Sets how many bytes to read at once from the serial port.
     * The data gets stored into a buffer of length <CODE>size</CODE>
     * @param size the buffer size
     */
    public void setReadBufferSize(int size)
    {
        readBufferSize = size;
    }
    
    // SerialEventTracker interface implementations
    /**
     * adds a listener to trap serial data events from this SerialManagaer
     */
    public void addSerialDataEventListener(SerialDataEventListener sdel)
    {
        if(sdel != null && listeners.indexOf(sdel) == -1) {
            listeners.add(sdel);
            System.out.println("[+ SerialDataEventListener] " + sdel);
        }
    }

    /**  
     * removes a listener from this SerialManagaer
     */
    public void removeSerialDataEventListener(SerialDataEventListener sdel)
    {
        if(listeners.contains(sdel)) {
            listeners.remove(listeners.indexOf(sdel));
            System.out.println("[- SerialDataEventListener] " + sdel);
        }
    }
    
	/**
	 *  Reads the latest byte off the input buffer when it comes in 
     */
	public void serialEvent(SerialPortEvent event)
	{
	    //int numBytes = 0;
	    int[] currBytes;
	    if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
	        //System.out.println("serial event...");
	    		try { 
	            //byte[] readBuffer = new byte[readBufferSize];
	        		if(in.available() >= readBufferSize) {
	         	/*
	                //int numBytes = in.read(readBuffer);
	            		in.read(readBuffer);
	                // copy the bytes into an int array
	                currBytes = new int[readBuffer.length];
	                for(int i = 0; i < readBuffer.length; i++)
	                    currBytes[i] = (int)readBuffer[i];

				*/
		            // capture bytes from the serial port into a buffer
	                currBytes = new int[readBufferSize];
	                for(int i = 0; i < readBufferSize; i++)
	                	   currBytes[i] = in.read();

	                System.out.println("\ncurrBytes=" + currBytes.length);
	                
	                // send out the data
	                notifySerialDataEventListeners(currBytes);
	            }
	            //readBuffer = null;
	        } catch (IOException e) {
	            System.out.println(e);
	        }          
	    }
	    currBytes = null;
	}
	
	/**
	 * let everyone know a serial data event was received
	 */
	private void notifySerialDataEventListeners(int[] data)
	{
	    if(listeners == null) {
	        return;
        } else {
            ListIterator iter = listeners.listIterator();
            while(iter.hasNext()) {
                	((SerialDataEventListener) iter.next()).serialDataEvent(data);
            	}
        }
    	}    


}
