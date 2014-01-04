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

import java.util.Hashtable;
import org.xml.sax.Attributes;

public class ClientHistoryContext extends AbstractClientContext
{
	private Attributes atts = null;
	private ClientMessageFormatter formatter = null;
	
	protected ClientHistoryContext()
	{
		
	}
	
	public ClientHistoryContext(ClientMessageFormatter clientFormatter,
			String elementName, Attributes atts)
	{
		this.atts = atts;
		this.formatter = clientFormatter;
	}
	
	public void processElement()
	{
		Hashtable hist = new Hashtable(3);
		hist.put("sensor", atts.getValue("sensor"));
		hist.put("count", atts.getValue("count"));
		hist.put("interval", atts.getValue("interval"));
		formatter.setHistory(hist);
		hist = null;
	}

}
