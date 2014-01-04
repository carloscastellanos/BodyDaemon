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
 * Created on Aug 8, 2005
 */

/**
 * @author Carlos Castellanos
 */

package cc.bodyd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class BodyDaemon
{
    // the object to handle serial data and start the server
    static BodyDSerialDataHandler bodyDSerial;
    
    public static void main(String[] args)
    {
        System.out.println("Welcome to BodyDaemon 0.1 alpha...\n");
        //System.out.println("Press \"q\" " + "or the escape key at any time to exit the program");
        
        int port;				// the port for the bodyd server to listen on
        int baud = 9600;			// our baud rate
        int serialPortNum = 0;	// the serial port we'll open
        ArrayList portList;		// the list of available serial ports;
        int numSensors = 0;		// number of sensors to listen for
        BufferedReader inStream;
        
		if((args.length != 1))
			throw new IllegalArgumentException("Usage: BodyDaemon <# of sensors> or <# of sensors>:<port>");
		else if(args[0].indexOf(":") < 0) {
			numSensors = Integer.parseInt(args[0]);
			port = 59000;
		} else {
			int idx = args[0].indexOf (":");
			numSensors = Integer.parseInt(args[0].substring(0, idx));
			port = Integer.parseInt (args[0].substring(idx + 1));
        }
        
        bodyDSerial = new BodyDSerialDataHandler(port, numSensors);
        portList = bodyDSerial.getSerialPorts();
        
        System.out.println("\nPick the number of a serial port to open.");
        
        try {
            // open input from keyboard
            inStream = new BufferedReader(new InputStreamReader(System.in));
            String inputText;
            
            // read as long as we have data at stdin
            while((inputText = inStream.readLine()) != null) {
                // if port is not open, assume user is typing a number
                // and open the corresponding port:
                if (!bodyDSerial.isSerialOpen()) {
                    serialPortNum = getNumber(inputText);
                    // if serialPortNum is in the right range, open it:
                    if (serialPortNum >= 0) {
                        if (serialPortNum < portList.size()) {
                            String whichPort = (String)portList.get(serialPortNum);
                            if (bodyDSerial.openSerial(whichPort, baud)) {
                                break;
                            }
                        } else {
                            // You didn't ge a valid port:
                            System.out.println(serialPortNum + " is not a valid serial port number. Please choose again");
                        }
                    }
                } /*else {
                    // port is open:
                    // if user types +++, close the port and repeat the port selection dialog:
                    if (inputText.equals("+++")) {
                        bodyDSerial.closeSerial();
                        System.out.println("Serial port closed.");
                        portList = bodyDSerial.getSerialPorts();                  
                        System.out.println("\nPick the number of a serial port to open.");
                    }            
                }*/
            }
            // if stdin closes, close port and quit:
            //inStream.close();
            //bodyDSerial.closeSerial();
            //System.out.println("Serial port closed; thank you, have a nice day.");
            System.out.println(" ");
        } catch(IOException e) {
            System.out.println(e);
        }
    }
    
    /**
     * @return an int from a string that's a valid number.
     **/   
    private static int getNumber(String inString) {
        int value = -1;
        try {
            value = Integer.parseInt(inString);
        } catch (NumberFormatException ne) {
            System.out.println("not a valid number");
        }
        return value;
    }
    
    /*
    public void keyPressed(KeyEvent e) {
        // Retrieve the key pressed.
        int keyCode = e.getKeyCode();
        if(keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_ESCAPE)
            exitProgram();
    }
    
    public void keyReleased(KeyEvent e) {
        
    }
    
    public void keyTyped(KeyEvent e) {
        
    }
    
    private void exitProgram()
    {
        System.out.println("shutting down BodyDaemon...");
        if(bodyDSerial != null) {
            if(bodyDSerial.close()) {
                bodyDSerial = null;
            }
        }
        System.exit(0);
    }
    */
}
