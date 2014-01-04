/*
// Copyright (C) 2005 by Carlos Castellanos
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

/**
 * 
 */

/**
 * @author carlos
 *
 */

package cc.bodyd.xml;

import java.util.Hashtable;

public class ClientMessageFormatter
{
	private Hashtable formattedData = null;
	
	public ClientMessageFormatter()
	{
		formattedData = new Hashtable();
	}
	
	public void setAddSensor(String sensorToAdd)
	{
		formattedData.put(sensorToAdd, "add");
	}
	
	public void setRemoveSensor(String sensorToRemove)
	{
		formattedData.put(sensorToRemove, "remove");
	}
	
	public void setHistory(Hashtable history)
	{
		formattedData.put((String) history.get("sensor") + "History", new Hashtable(history));
	}
	
	public void setDefault()
	{
		formattedData.put("default", "");
	}
	
	public void clear()
	{
		formattedData.clear();
	}
	
	public Hashtable getFormattedData()
	{	
		return formattedData;
	}
}
