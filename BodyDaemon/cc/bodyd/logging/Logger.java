/**
 * Logger is a quick and dirty logging class. It supports output to a text file,
 * logging on/off, enabled checking, and nothing else.
 * 
 * @author carlos
 *
 *	Much of this code is from an article by Thorton Rose on Developer.com
 *  see http://www.developer.com/java/other/article.php/10936_1404951_1
 */

package cc.bodyd.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Logger
{
	/** On/off flag. */
	protected boolean on = false;
	//log file object
	protected PrintStream logFile = null;

	/** Date format. */
	protected SimpleDateFormat dateFormat;

	protected Logger()
	{
	}
	
	/**
	* Return true if logging is on (logger is off by default).
	* @return true if logging is on
	*/
	public boolean isOn()
	{
		return on;
	}

	/**
	* Set logging on/off (logger is off by default).
	* @param isOn true sets logging on
	*/
	public void setOn(boolean isOn)
	{
		on = isOn;
	}
	
	/**
	 * Sets the date format
	 * @param sdf the date format
	 */
	public void setDateFormat(SimpleDateFormat sdf)
	{
		dateFormat = sdf;
	}
	
	/**
	 * get the date format
	 * @return the date format
	 */
	public SimpleDateFormat getDateFormat()
	{
		return dateFormat;
	}
	
	public static SimpleDateFormat getDefaultDateFormat()
	{
		return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss Z");	
	}

	/**
	* Log the given message.
	* @param msg the message to log
	*/
	public synchronized void log(String msg)
	{
		if (on) {
			try {
				if(logFile == null)
					logFile = new PrintStream(new FileOutputStream("logger.log", true));
				try {
					logFile.println(dateFormat.format(new Date()) + " " + msg);
				} finally {
					logFile.close();
				}
			} catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		notifyAll();
	}
	
}
