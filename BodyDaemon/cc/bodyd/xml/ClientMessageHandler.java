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
 *  This is where the client request is given back to
 *  BodyD (in the form of a Hashtable)
 */

/**
 * @author carlos
 *
 */

package cc.bodyd.xml;

import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Vector;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ClientMessageHandler extends DefaultHandler
{
	private static Vector xmlListeners = new Vector();
	private ClientMessageFormatter formatter = null;
	
	public ClientMessageHandler()
	{
		formatter = new ClientMessageFormatter();
	}
	
	public void startElement(String namepaceURI, String localName,
					String qName, Attributes atts) throws SAXException
	{
		System.out.println("startElement " + localName);
			
		AbstractClientContext context = AbstractClientContext.getContext(formatter, localName, atts);
		context.processElement();
	}
	
	public void endElement(String namepaceURI, String localName,
			String qName) throws SAXException
	{
		System.out.println("endElement " + localName);
	}
	
	public void startDocument() throws SAXException
	{
		System.out.println("startDocument");	
	}
	
	public void endDocument() throws SAXException
	{
		System.out.println("endDocument");
		notifyXmlDataEventListeners(formatter.getFormattedData());
		//formatter.clear();
	}
	
	public void fatalError(SAXParseException e) throws SAXException
	{
		System.out.println("### Fatal xml parsing error! ### " + e);
	}
	
	public void warning(SAXParseException e) throws SAXException
	{
		System.out.println("### xml parse warning ### " + e);
	}
	
    public synchronized void addXmlDataEventListener(XmlDataEventListener xmldel)
    {
    		if(xmldel != null && xmlListeners.indexOf(xmldel) == -1) {
    			xmlListeners.add(xmldel);
    			System.out.println("[+ XmlDataEventListener] " + xmldel);
        }
    	
    }

    public synchronized void removeXmlDataEventListener(XmlDataEventListener xmldel)
    {
    		if(xmlListeners.contains(xmldel)) {
            xmlListeners.remove(xmlListeners.indexOf(xmldel));
            System.out.println("[- XmlDataEventListener] " + xmldel);
        }
    }
    
    private synchronized void notifyXmlDataEventListeners(Hashtable data)
    {
	    if(xmlListeners == null) {
	        return;
        } else {
            ListIterator iter = xmlListeners.listIterator();
            while(iter.hasNext()) {
                	((XmlDataEventListener) iter.next()).xmlDataEvent(new Hashtable(data));
            	}
        } 
    }
}
