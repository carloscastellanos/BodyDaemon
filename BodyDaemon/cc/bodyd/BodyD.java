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
 * 
 * This class does the actual sending and receiving
 * of data from clients on open sockets
 */

/**
 * @author Carlos Castellanos
 */

package cc.bodyd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import cc.bodyd.xml.ClientMessageHandler;
import cc.bodyd.xml.XmlDataEventListener;

public class BodyD implements Runnable, XmlDataEventListener
{
    protected InputStream in;
    protected OutputStream out;
    //protected DataInputStream in;
    //protected DataOutputStream out;
    protected Thread listener = null; // we're running this class as a thread
    protected Socket connection = null;
    private boolean running = false;
    
    private static long dataRate = 1000; // default dataRate
    
    //private static Hashtable currSensorData = null;
    private Hashtable currentXmlData = new Hashtable();
    private String xmlStr = null;
	private ClientMessageHandler messageHandler;
	// xml elements and attributes
    public static final String XML_DECL = "<?xml version=\"1.0\"?>";
    public static final String ROOT_ELEMENT_NAME = "bodydml";
    public static final String LIVEDATA_ELEMENT_NAME = "livedata";
    public static final String SENSOR_ELEMENT_NAME = "sensor";
    public static final String SERVERPROP_ELEMENT_NAME = "serverprop";
    public static final String HISTORY_ELEMENT_NAME = "history";
    public static final String DATA_ELEMENT_NAME = "data";
    public static final String STATUS_ATTRIBUTE_NAME = "status";
    public static final String TYPE_ATTRIBUTE_NAME = "type";
    public static final String VALUE_ATTRIBUTE_NAME = "value";
    public static final String TIME_ATTRIBUTE_NAME = "time";
    private static final String FAIL_MESSAGE = XML_DECL+"<"+ROOT_ELEMENT_NAME+ " "+STATUS_ATTRIBUTE_NAME
	+"=\"fail\"></"+ROOT_ELEMENT_NAME+">";
    
    // store a list of all the current BodyD objects/client sockets
    protected static Vector bodyDObjs = new Vector();
    
    private boolean live = true;
 
    // constructor
    public BodyD(Socket connection)
    {
        this.connection = connection;
    }

    	public void xmlDataEvent(Hashtable data)
    	{
    		boolean failed = false;
    		
    		// create a string buffer to add the xml in as we go
    		StringBuffer sb = new StringBuffer(1024);
    		
    		Enumeration allKeys = data.keys();
    		while(allKeys.hasMoreElements()) {
    			String key = (String)allKeys.nextElement();
    			// if the value is a String (and thus an add, remove or default request)
    			if(data.get(key) instanceof String) {
    				live = true;
    				String val = (String)data.get(key);
    				// determine the value of the current key
    				if(val.equalsIgnoreCase("add")) {
    					Hashtable currSensorData = BodyDManager.getCurrentSensorData();
    					if(key.equalsIgnoreCase("GSR") || key.equalsIgnoreCase("ECG") 
    						|| key.equalsIgnoreCase("EMG") || key.equalsIgnoreCase("Respiration")) {
    						currentXmlData.put(key, (String)currSensorData.get(key));
    					}
    				} else if(val.equalsIgnoreCase("remove")) {
    					currentXmlData.remove(key);
    				} else {
    					// default = all sensor data
    					Hashtable currSensorData = BodyDManager.getCurrentSensorData();
    					currentXmlData.putAll(currSensorData);
    				}
    				// === generate the xml === //
				sb.append("<" + LIVEDATA_ELEMENT_NAME + ">");
				// grab the data from the currentXmlData hashtable and generate an xml string
				Enumeration liveKeys = currentXmlData.keys();
				// grab the current server data
				Hashtable currServerData = BodyDManager.getCurrentServerData();
				while(liveKeys.hasMoreElements()) {
					String k = (String)liveKeys.nextElement();
					String prop = null;
    					String sensorVal = null;
    					String serverpropVal = null;
					if(k.equalsIgnoreCase("GSR")) {
						prop = "timeout";
    						sensorVal = ((Integer)(currentXmlData.get(k))).toString();
    						serverpropVal = ((Integer)(currServerData.get(prop))).toString();
					} else if(k.equalsIgnoreCase("ECG")) {
						prop = "maxsockets";
    						sensorVal = ((Integer)(currentXmlData.get(k))).toString();
    						serverpropVal = ((Integer)(currServerData.get(prop))).toString();
					} else if(k.equalsIgnoreCase("EMG")) {
						prop = "nudgepower";
    						sensorVal = ((Integer)(currentXmlData.get(k))).toString();
    						serverpropVal = ((Boolean)(currServerData.get(prop))).toString();
					} else if(k.equalsIgnoreCase("Respiration")) {
						prop = "datarate";
    						sensorVal = ((Integer)(currentXmlData.get(k))).toString();
    						serverpropVal = ((Long)(currServerData.get(prop))).toString();
					} else if(k.equalsIgnoreCase("avg")) {
						prop = "powerlevel";
    						sensorVal = ((Double)(currentXmlData.get(k))).toString();
    						serverpropVal = ((Double)(currServerData.get(prop))).toString();
					}
						
					// sensor
					sb.append("<" + SENSOR_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" + k + "\" ");
					sb.append(VALUE_ATTRIBUTE_NAME + "=\"" + sensorVal + "\">");
					// serverprop
					sb.append("<" + SERVERPROP_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" + prop + "\">");
					sb.append(serverpropVal);
					sb.append("</" + SERVERPROP_ELEMENT_NAME + ">");
					sb.append("</" + SENSOR_ELEMENT_NAME + ">");
	    				
				} // end inner while
				sb.append("</" + LIVEDATA_ELEMENT_NAME + ">");
				
             // or if it's a Hashtable (and thus a history request)
    			} else {
    				// make sure logging is on
    				if(BodyDManager.isLogging()) {
    					live = false;
    					// generate the xml
    					sb.append("<" + HISTORY_ELEMENT_NAME + ">");
    					
    					Hashtable h = (Hashtable)data.get(key);
    					String sensor = (String)h.get("sensor");
    					String count = (String)h.get("count");
    					int intCount = Integer.parseInt(count);
    					String interval = (String)h.get("interval");
    					int intInterval = 1;
    					// determine whether it's minutes or seconds
    					// if it's minutes, convert to seconds
    					if(interval.indexOf("m") == -1 && interval.indexOf("M") == -1) {
    						if(interval.indexOf("s") != -1)
    							intInterval = Integer.parseInt(interval.substring(0, interval.indexOf("s")));
    						else if(interval.indexOf("S") != -1)
    							intInterval = Integer.parseInt(interval.substring(0, interval.indexOf("S")));
    					} else {
    						if(interval.indexOf("m") != -1)
    							intInterval = Integer.parseInt(interval.substring(0, interval.indexOf("m")));
    						else if(interval.indexOf("M") != -1)
    							intInterval = Integer.parseInt(interval.substring(0, interval.indexOf("M")));
    						intInterval = intInterval = 60 * intInterval;
    					}
    					
    					// get the log entries from memory
    					LinkedList logEntries = BodyDManager.getLogsFromMemory();
    					int logCount = logEntries.size();
    					// make sure we don't try and return more than we have
    					if ((intCount * intInterval) > logCount)
    						intCount = logCount;
    					for(int i = 0; i < (intCount * intInterval); i += intInterval) {
    						Hashtable entry;
    						if(i == 0)
    							entry = (Hashtable)logEntries.get(i);
    						else
    							entry = (Hashtable)logEntries.get(i-1);
    						
    						// get all the sensor and server entries
    						Hashtable sensors = (Hashtable)entry.get("sensors");
    						Hashtable server = (Hashtable)entry.get("server");
    						String time = (String)entry.get("time");
    						sb.append("<" + DATA_ELEMENT_NAME + " " + TIME_ATTRIBUTE_NAME + "=\"" + time + "\">");
    						
    						String prop = null;
    	    					String sensorVal = null;
    	    					String serverpropVal = null;
    	    					boolean all = false;
    	    					if(sensor.equalsIgnoreCase("GSR")) {
    	    						prop = "timeout";
    	    						sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    						serverpropVal = ((Integer)(server.get(prop))).toString();
    						} else if(sensor.equalsIgnoreCase("ECG")) {
    							prop = "maxsockets";
    	    						sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    						serverpropVal = ((Integer)(server.get(prop))).toString();
    						} else if(sensor.equalsIgnoreCase("EMG")) {
    							prop = "nudgepower";
    	    						sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    						serverpropVal = ((Boolean)(server.get(prop))).toString();
    						} else if(sensor.equalsIgnoreCase("Respiration")) {
    							prop = "datarate";
    	    						sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    						serverpropVal = ((Long)(server.get(prop))).toString();
    						} else if(sensor.equalsIgnoreCase("avg")) {
    							prop = "powerlevel";
    	    						sensorVal = ((Double)(sensors.get(sensor))).toString();
    	    						serverpropVal = ((Double)(server.get(prop))).toString();
    						} else if(sensor.equalsIgnoreCase("all")) {
    							all = true;
    						}
    	    					
    	    					if(!all) {
    	    						// sensor
    	    						sb.append("<" + SENSOR_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" 
    								+ sensor + "\" ");
    	    						sb.append(VALUE_ATTRIBUTE_NAME + "=\"" + sensorVal + "\">");
    	    						// serverprop
    	    						sb.append("<" + SERVERPROP_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" 
    								+ prop + "\">");
    	    						sb.append(serverpropVal);
    	    						sb.append("</" + SERVERPROP_ELEMENT_NAME + ">");
    	    						sb.append("</" + SENSOR_ELEMENT_NAME + ">");
    	    					} else {
    	    						// we want them all!
    	    						Enumeration sensorKeys = sensors.keys();
    	    						while(sensorKeys.hasMoreElements()) {
    	    							String sk = (String)sensorKeys.nextElement();
    	    	    						if(sk.equalsIgnoreCase("GSR")) {
    	    	    							prop = "timeout";
    	    	    							sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    	    							serverpropVal = ((Integer)(server.get(prop))).toString();
    	    	    						} else if(sensor.equalsIgnoreCase("ECG")) {
    	    	    							prop = "maxsockets";
    	    	    							sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    	    							serverpropVal = ((Integer)(server.get(prop))).toString();
    	    	    						} else if(sensor.equalsIgnoreCase("EMG")) {
    	    	    							prop = "nudgepower";
    	    	    							sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    	    							serverpropVal = ((Boolean)(server.get(prop))).toString();
    	    	    						} else if(sensor.equalsIgnoreCase("Respiration")) {
    	    	    							prop = "datarate";
    	    	    							sensorVal = ((Integer)(sensors.get(sensor))).toString();
    	    	    							serverpropVal = ((Long)(server.get(prop))).toString();
    	    	    						} else if(sensor.equalsIgnoreCase("avg")) {
    	    	    							prop = "powerlevel";
    	    	    							sensorVal = ((Double)(sensors.get(sensor))).toString();
    	    	    							serverpropVal = ((Double)(server.get(prop))).toString();
    	    	    						}
    	    	    						
    	    	    						// sensor
    	    	    						sb.append("<" + SENSOR_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" 
    	    								+ sensor + "\" ");
    	    	    						sb.append(VALUE_ATTRIBUTE_NAME + "=\"" + sensorVal + "\">");
    	    	    						// serverprop
    	    	    						sb.append("<" + SERVERPROP_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" 
    	    								+ prop + "\">");
    	    	    						sb.append(serverpropVal);
    	    	    						sb.append("</" + SERVERPROP_ELEMENT_NAME + ">");
    	    	    						sb.append("</" + SENSOR_ELEMENT_NAME + ">");
    	    						} // end while
    	    					}
    	    					sb.append("</" + DATA_ELEMENT_NAME + ">");
    					} // end for
    					sb.append("</" + HISTORY_ELEMENT_NAME + ">");
    				} else {
    					// if logging is not on, send a fail message
    					failed = true;
    	        			sendMessage(FAIL_MESSAGE);
    				}
    			}
    		} // end outer while
    		
    		// this is basically just to check whether a fail message was sent
    		if(!failed) {
        		// every message will have these two lines
    			// insert them to the front of the buffer
    			sb.insert(0, "<" + ROOT_ELEMENT_NAME + " " + STATUS_ATTRIBUTE_NAME + "=\"success\">");
    			sb.insert(0, XML_DECL);
    			// close the root tag and send the message
        		sb.append("</" + ROOT_ELEMENT_NAME + ">");
        		xmlStr = sb.toString();
        		sendMessage(xmlStr);
    		}
    	}
    	
    	// continuosly send the data contained in the currentXmlData Hashtable
    	private void sendContinuousXml()
    	{
    		//boolean failed = false;
    		// create a string buffer to add the xml in as we go
    		StringBuffer sb = new StringBuffer(1024);
    		// live data (not sending continuous history data)
    		if(live) {
    			// === generate the xml === //
    			sb.append("<" + LIVEDATA_ELEMENT_NAME + ">");
    			Enumeration liveKeys = currentXmlData.keys();
    			Hashtable currSensorData = BodyDManager.getCurrentSensorData();
    			Hashtable currServerData = BodyDManager.getCurrentServerData();
    			while(liveKeys.hasMoreElements()) {
    				String k = (String)liveKeys.nextElement();
    				String prop = null;
    				String sensorVal = null;
    				String serverpropVal = null;
    				if(k.equalsIgnoreCase("GSR")) {
    					prop = "timeout";
    					sensorVal = ((Integer)(currSensorData.get(k))).toString();
    					serverpropVal = ((Integer)(currServerData.get(prop))).toString();
    				} else if(k.equalsIgnoreCase("ECG")) {
    					prop = "maxsockets";
    					sensorVal = ((Integer)(currSensorData.get(k))).toString();
    					serverpropVal = ((Integer)(currServerData.get(prop))).toString();
    				} else if(k.equalsIgnoreCase("EMG")) {
    					prop = "nudgepower";
    					sensorVal = ((Integer)(currSensorData.get(k))).toString();
    					serverpropVal = ((Boolean)(currServerData.get(prop))).toString();
    				} else if(k.equalsIgnoreCase("Respiration")) {
    					prop = "datarate";
    					sensorVal = ((Integer)(currSensorData.get(k))).toString();
    					serverpropVal = ((Long)(currServerData.get(prop))).toString();
    				} else if(k.equalsIgnoreCase("avg")) {
    					prop = "powerlevel";
    					sensorVal = ((Double)(currSensorData.get(k))).toString();
    					serverpropVal = ((Double)(currServerData.get(prop))).toString();
    				}
    				
				// sensor
				sb.append("<" + SENSOR_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" + k + "\" ");
				sb.append(VALUE_ATTRIBUTE_NAME + "=\"" + sensorVal + "\">");
				// serverprop
				sb.append("<" + SERVERPROP_ELEMENT_NAME + " " + TYPE_ATTRIBUTE_NAME + "=\"" + prop + "\">");
				sb.append(serverpropVal);
				sb.append("</" + SERVERPROP_ELEMENT_NAME + ">");
				sb.append("</" + SENSOR_ELEMENT_NAME + ">");
	    		
    			} // end while
    			sb.append("</" + LIVEDATA_ELEMENT_NAME + ">");
    			
    		// or history
    		} /*else {
    			if(BodyDManager.isLogging()) {
    				// generate the xml
    				sb.append("<" + HISTORY_ELEMENT_NAME + ">");
    				//more stuff... (get logging data from logging object/data structure)
					
    			} else {
    				// if logging is not on, send a fail message
    				failed = true;
    				sendMessage(FAIL_MESSAGE);
    			}
    		}
    		
    		// this is basically just to check whether a fail message was sent
    		if(!failed) {
        		// every message will have these two lines
    			// insert them to the front of the buffer
    			sb.insert(0, "<" + ROOT_ELEMENT_NAME + STATUS_ATTRIBUTE_NAME + "=\"success\">");
    			sb.insert(0, XML_DECL);
    			// close the root tag and send the message
        		sb.append("</" + ROOT_ELEMENT_NAME + ">");
        		xmlStr = sb.toString();
        		sendMessage(xmlStr);
    		}*/
    		// every message will have these two lines
		// insert them to the front of the buffer
		sb.insert(0, "<" + ROOT_ELEMENT_NAME + " " + STATUS_ATTRIBUTE_NAME + "=\"success\">");
		sb.insert(0, XML_DECL);
		// close the root tag and send the message
    		sb.append("</" + ROOT_ELEMENT_NAME + ">");
    		xmlStr = sb.toString();
    		sendMessage(xmlStr);
    	}
    	
    //  open an input and output stream and start a new Thread
    public synchronized void start()
    {
    		if(listener == null) {
    			try {
    				// i/o streams
    				in = connection.getInputStream();
    				out = connection.getOutputStream();
    				// for xml handling/parsing
    				messageHandler = new ClientMessageHandler();
    				messageHandler.addXmlDataEventListener(this);
    				listener = new Thread(this);
    				listener.start();
    				running = true;
    			}
    			catch(IOException ioe) {
    				System.out.println("Error establishing socket input and/or output streams!");
    				ioe.printStackTrace();
    				running = false;
    			}
    		}
    }
    
    // interrupt the listener Thread and close the i/o streams
    public synchronized void stop()
    {
        if(listener != null) {
            try {
                if(listener != Thread.currentThread())
                    listener.interrupt();
                // remove xml handling
                messageHandler.removeXmlDataEventListener(this);
                messageHandler = null;
                // close the socket, input & output streams
                connection.close();
                out.close();
                in.close();
                connection = null;
                out = null;
                in = null;
                listener = null;
                running = false;
                bodyDObjs.removeElement(this);
                System.out.println("*** BodyD thread " + this + " stopped. ***");
            }
            catch(IOException ignored) {
            }
            //handlers.removeElement(this);
        }
    }
    
    // this is where the Thread enters its execution cycle
    // and where we receive and handle messages
    public void run()
    {
    		try {
    			bodyDObjs.addElement(this);
    			System.out.println("adding a BodyD object to the list");
    			while(running && !Thread.interrupted() && !connection.isClosed()) {
    	    			StringBuffer sb = new StringBuffer();
    	    			byte[] buf = new byte[1024];
    	    			// receive client messages
    	    			try {
    	    				int avail = in.available();
    	    				while(avail > 0) {
    	    					System.out.println("BodyD: data in buffer...");
    	    					int amt = avail;
    	    					if(amt > buf.length)
    	    						amt = buf.length;
    	    					amt = in.read(buf, 0, amt);
    	    			
    	    					int marker = 0;
    	    					for(int i = 0; i < amt; i++) {
    	    						// scan for the zero-byte EOM (end of message) delimeter
    	    						if(buf[i] == (byte)0) {
    	    							String tmp = new String(buf, marker, i - marker);
    	    							sb.append(tmp);
    	    							// handle the xml request 
    	    							// this will evenetually trigger the xmlDataEvent
    	    							// callback method and send back xml to the client
    	    							try {
    	    								handleRequest(messageHandler, sb.toString());
    								} catch(SAXException se) {
    									System.out.println("*** Parsing error *** " + se);
    									System.out.println("sending fail message...");
    									sendMessage(FAIL_MESSAGE);
    								}
    	    							sb.setLength(0);
    	    							marker = i + 1;
    	    						}
    	    					}
    	    					if(marker < amt) {
    	    						// save everything so far, still waiting for the final EOM
    	    						sb.append(new String(buf, marker, amt - marker));
    	    					}
    	    					avail = in.available();
    	    				} // *** end inner while loop ***
    	    			} catch(IOException ioe) {
    	    				System.out.println("Error reading from the socket! " + ioe.getClass());
    	    				//sendMessage(FAIL_MESSAGE);
    	    				//stop();
    	    			}

    				// constantly send the bio/server data
    	    			// note: this happens after the handleRequest() method has been called
    				if(!(currentXmlData.isEmpty()))
    					sendContinuousXml();
    				
    			} // *** end outer while loop ***
        }
        catch(Exception e) {
        		sendMessage(FAIL_MESSAGE);
        		System.out.println("Client error and/or error while parsing xml: " + e.getClass());
        		if(listener == Thread.currentThread())
        			e.printStackTrace();
        		//stop();
        }
        /*
        finally
        {
        		if(BodyDManager.bodyDObjs.contains(this)) {
        			BodyDManager.bodyDObjs.removeElement(this);
            		System.out.println("removing a BodyD object from the list");
        		}
        }
        */
        //stop();
    }
    
    protected synchronized void sendMessage(String message)
    {
    		if(message == null) return;
    		
    		// ----- send data to the client ----- //
    		if(!Thread.interrupted() && connection.isConnected()) {
    			try {
    				this.out.write(message.getBytes());
    				this.out.write((byte)0); // EOM
    				this.out.flush();
    				System.out.println("+++ BodyD: sending xml... +++ " + message);
    				// send the data at the rate determined by the dataRate field
    				Thread.sleep(dataRate);
    			} catch(Exception e) {
    				System.out.println("Error while writing to the socket! " + e);
    				this.stop();
    			}
    		}
    		notifyAll();
    }
    
    protected void handleRequest(ClientMessageHandler cmh, String str) throws IOException, SAXException
    {
		// ----- read in data the client sends ----- //
    		System.out.println("+++ BodyD: receiving xml... +++");
    		System.out.println(str);
    		// get an xml parser
    		XMLReader reader = new SAXParser();
    		reader.setContentHandler(cmh);
    		
    		// begin reading from the input stream
    		StringReader sr = new StringReader(str);
    		InputSource is = new InputSource(sr);
    		System.out.println("+++ BodyD: parsing xml... +++");
    		reader.parse(is);
    		
    		/*
    		// --- Begin Validation --- //
    	    // create a SchemaFactory capable of understanding WXS schemas
    	    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    	    // load a WXS schema, represented by a Schema instance
    	    Source schemaFile = new StreamSource(new File("bodyd_schema_client.xsd"));
    	    Schema schema = factory.newSchema(schemaFile);

    	    // create a Validator instance, which can be used to validate an instance document
    	    Validator validator = schema.newValidator();

    	    // validate the SAX source
    	    try {
    	    		System.out.println("+++ BodyD: validating xml... +++");
    	    		validator.validate(new SAXSource(is));
    	    		
    	         // parse it if it's valid
    	    		System.out.println("+++ BodyD: parsing xml... +++");
        		reader.parse(is);
    	    } catch (SAXException e) {
    	    		sendMessage(FAIL_MESSAGE);
    	    		System.out.println("+++ xml document is invalid! +++\n" + e.getMessage());
    	    		
    	    }
    	    // --- End Validation --- //
    	    */
    		
    }
    
    /*
    private void parseMessage(String msg)
    {
		if(msg.equalsIgnoreCase("CLOSE")) {
			try {
				handlers.removeElement(this);
				stop();
				connection.close(); // close the socket connection
			} catch (IOException ioe) {
				System.out.println("Error while attempting to close the socket!");
			}
		}
    }
    */
    
    /*
    protected static int numConnections()
    {
        return handlers.size();
    }
    
    protected static Vector getConnections()
    {
        return handlers;
    }
    */
    
    /*
    protected boolean close(int index)
    {
        boolean closed = false;
        BodyD bd = (BodyD) handlers.elementAt(index);
        try {
            bd.stop();
            //bd.connection.close();
            handlers.removeElementAt(index);
            closed = true;
        } catch(Exception e) { }
        
        return closed;
    }
    
    protected static boolean closeAll()
    {
        boolean closed = false;
        BodyD bd;
        ListIterator iter = handlers.listIterator();
        try {
        		System.out.println("*** closing all BodyD connections... ***");
        		while(iter.hasNext()) {
        			bd = (BodyD) iter.next();
        			//bd.listener.interrupt();
        			//bd.connection.close();
        			bd.stop();
            }
            //handlers.removeAllElements();
            closed = true;
            System.out.println("*** all BodyD connections closed ***");
        } catch(Exception e) {
        		System.out.println("=== error closing BodyD connections === " + e);
        		e.printStackTrace();
        }
        
        return closed;
    }
    */
    protected String getXML()
    {
    		return xmlStr;
    }
    
    protected static void setDataRate(long millisecs)
    {
    		if(millisecs < 1000)
    			millisecs = 1000;
    		dataRate = millisecs;
    }
    
    protected static long getDataRate()
    {
    		return dataRate;
    }

}
