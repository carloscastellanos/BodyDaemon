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

import org.xml.sax.Attributes;

public abstract class AbstractClientContext
{
	public static final String BODYDML = "bodydml";
	public static final String BODYDML_REQUESTTYPE_DEFAULT = "default";
	public static final String BODYDML_REQUESTTYPE_CUSTOM = "custom";
	public static final String ADD = "add";
	public static final String REMOVE = "remove";
	public static final String HISTORY = "history";
	private static boolean def;
	
	protected AbstractClientContext()
	{
	}
	
	protected AbstractClientContext(ClientMessageFormatter clientFormatter,
											String elementName, Attributes atts)
	{
	}
	
	// return the appropriate context for the given elemeent
	public static AbstractClientContext getContext(ClientMessageFormatter clientFormatter,
											String elementName, Attributes atts)
	{
		AbstractClientContext context = null;
		
		// check the bodydml elements
		if(elementName.equalsIgnoreCase(BODYDML)) {
			if(!(atts.getValue("requesttype").equalsIgnoreCase(BODYDML_REQUESTTYPE_CUSTOM))) {
				context = new ClientDefaultContext(clientFormatter, elementName, atts);
				// flag a default request
				def = true;
			} else {
				def = false;
			}
		// if it's not a default request
		} else if(!def) {
			if(elementName.equalsIgnoreCase(ADD)) {
				context = new ClientAddContext(clientFormatter, elementName, atts);
			} else if(elementName.equalsIgnoreCase(REMOVE)) {
				context = new ClientRemoveContext(clientFormatter, elementName, atts);
			} else if(elementName.equalsIgnoreCase(HISTORY)) {
				context = new ClientHistoryContext(clientFormatter, elementName, atts);
			}
		}
		
		return context;
	}
	
	public abstract void processElement();
	
	
}
