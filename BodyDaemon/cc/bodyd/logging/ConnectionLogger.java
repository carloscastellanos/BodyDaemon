/**
 * This class connection data (i.e - from clients who connect to the server)
 */
package cc.bodyd.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author carlos
 *
 */
public class ConnectionLogger extends Logger
{
	public ConnectionLogger()
	{
		this(Logger.getDefaultDateFormat());
	}
	
	public ConnectionLogger(SimpleDateFormat sdf)
	{
		this.dateFormat = sdf;
	}
	
	/**
	* Log the given message.
	* @param msg the message to log
	*/
	public synchronized void log(String msg)
	{
		if (on) {
			/*
			get a formatted date and alter the string to
			replace spaces and colons (so that it makes
			a valid filename)
			*/
			Date date = new Date();
			String dateParsed = dateFormat.format(date);
			String newDate = dateParsed.replace(' ', '_');
			String finalDate = newDate.replace(':', '.');
			
			try {
				if(logFile == null)
					logFile = new PrintStream(new FileOutputStream("connect_" +
						finalDate + ".log", true));
				try {
					logFile.println("[" + dateFormat.format(date) + "]" + " " + msg);
				} finally {
					logFile.close();
				}
			} catch(IOException ex) {
				System.out.println("Error writing log file!");
				ex.printStackTrace();
			}
		}
		notifyAll();
	}
	
	/**
	* Log an exception.  Also useful for general errors
	* @param ex the Exception
	* @param obj the Object where the exception was thrown
	*/
	public synchronized void logException(Exception ex, Object obj)
	{
		Class exClass = ex.getClass();
		Class objClass = obj.getClass();
		
		String e = exClass.getName();
		String o = objClass.getName();
		String str = e + " " + o;
		log(str);
		notifyAll();
	}
}
