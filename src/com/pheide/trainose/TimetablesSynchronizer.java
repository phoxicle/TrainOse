/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.database.Cursor;
import android.widget.Toast;

/**
 * Class to handle synchronizing the timetables for a route.
 * 
 * Typically run in the background.
 * 
 * @author Christine Gerpheide
 */
public class TimetablesSynchronizer {

	Activity mActivity; // Needed for context and cursor management
	List<HashMap<String,String>> mTimetablesList = null;
    
	/**
	 * Construct the synchronizer.
	 * 
	 * @param activity to use for context and cursor management.
	 */
    public TimetablesSynchronizer(Activity activity) {
    	mActivity = activity;
    }
    
    /**
     * Download and store all timetables for a given route.
     * 
     * @param long the route ID for which to download timetables.
     * @return
     */
    public Boolean syncTimetablesForRoute(long routeId) {
	    try {
    		// Set up XML parsers etc
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            
            // Get details (to/from) for this route
            RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(mActivity);
            routesDbAdapter.open();
            Cursor routesCursor = routesDbAdapter.fetch(routeId);
            mActivity.startManagingCursor(routesCursor);
            String source = CursorHelper.getString(routesCursor,RoutesDbAdapter.KEY_SOURCE);
            String destination = CursorHelper.getString(routesCursor,RoutesDbAdapter.KEY_DESTINATION);
            mActivity.stopManagingCursor(routesCursor);
            routesDbAdapter.close();

            // Retrieve and parse XML from server
            URL sourceUrl = new URL("http://www.pheide.com/Services/TrainOse/ose.php?"
            		+ "from=" + URLEncoder.encode(source,"UTF-8") 
            		+ "&to=" + URLEncoder.encode(destination,"UTF-8"));
            TimetablesXmlHandler timetablesXmlHandler = new TimetablesXmlHandler();
            xr.setContentHandler(timetablesXmlHandler);
            xr.parse(new InputSource(sourceUrl.openStream()));

            // Clear current timetables and add the new ones
            if (mTimetablesList.size() > 1) {
	            TimetablesDbAdapter timetablesDbAdapter = new TimetablesDbAdapter(mActivity);
	            timetablesDbAdapter.open();
	            timetablesDbAdapter.deleteByRoute(routeId);
	            for (HashMap<String,String> currentMap : mTimetablesList) {
	            	timetablesDbAdapter.create(routeId, currentMap.get("depart"), 
	            			currentMap.get("arrive"), currentMap.get("duration"), 
	            			currentMap.get("train"), currentMap.get("trainNum"), 
	            			currentMap.get("delay"));
	            }
	            timetablesDbAdapter.close();
            }
            
            // Update date last synced for this route
            routesDbAdapter.open();
            routesDbAdapter.touchTimestamp(routeId);
            routesDbAdapter.close();
            
	    } catch (IOException e) {
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, "Network error", Toast.LENGTH_SHORT).show();
        	    }
        	});
	    } catch (SAXException e) {
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, "Server error", Toast.LENGTH_SHORT).show();
        	    }
        	});
	    }  catch (Exception e) {
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, "An error occured", Toast.LENGTH_SHORT).show();
        	    }
        	});
        }
	    
	    return true;
    }
    
    /**
     * Class to handle parsing the timetables XML returned from the server.
     * 
     * @author Christine Gerpheide
     */
    protected class TimetablesXmlHandler extends DefaultHandler {
    	
    	Boolean currentElement = false;
    	String currentValue = null;
        HashMap<String,String> currentMap = null;
        Boolean inRoute = false;
 
	    @Override
	    public void startElement(String uri, String localName, String qName,
	            Attributes attributes) throws SAXException {
	 
	        this.currentElement = true;
	        this.currentValue = null;
	 
	        if (localName.equals("xml")) {
	            TimetablesSynchronizer.this.mTimetablesList = 
	            		new ArrayList<HashMap<String,String>>();
	        } else if (localName.equals("route")) {
	        	this.currentMap = new HashMap<String,String>();
	        	this.inRoute = true;
	        }
	 
	    }
	 
	    @Override
	    public void endElement(String uri, String localName, String qName)
	            throws SAXException {
	 
	        this.currentElement = false;
	        if (this.inRoute) {
	        	this.currentMap.put(localName, this.currentValue != null ? 
	        			this.currentValue : new String());
	        }
	        
	        if (localName.equalsIgnoreCase("route")) {
	        	this.inRoute = false;
	        	TimetablesSynchronizer.this.mTimetablesList.add(this.currentMap);
	        }
	 
	    }
	 
	    @Override
	    public void characters(char[] ch, int start, int length)
	            throws SAXException {
	 
	        if (this.currentElement) {
	            this.currentValue = new String(ch, start, length);
	            this.currentElement = false;
	         }
	 
	    }
    }
 
}
