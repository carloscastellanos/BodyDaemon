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

package cc.bodyd.xml;

import org.xml.sax.Attributes;

public class ClientAddContext extends AbstractClientContext
{
	private Attributes atts = null;
	private ClientMessageFormatter formatter = null;
	
	protected ClientAddContext()
	{
		
	}
	
	public ClientAddContext(ClientMessageFormatter clientFormatter,
			String elementName, Attributes atts)
	{
		this.atts = atts;
		this.formatter = clientFormatter;
	}
	
	public void processElement()
	{
		formatter.setAddSensor(atts.getValue("sensor"));
	}

}
