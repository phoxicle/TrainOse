package com.pheide.trainose;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
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

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class TimetablesSynchronizer {

	Activity mActivity; // Needed for context and cursor management
	List<HashMap<String,String>> mTimetablesList = null;
    
    public TimetablesSynchronizer(Activity activity) {
    	mActivity = activity;
    }
    
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
            String source = routesCursor.getString(routesCursor.getColumnIndex(RoutesDbAdapter.KEY_SOURCE));
            String destination = routesCursor.getString(routesCursor.getColumnIndex(RoutesDbAdapter.KEY_DESTINATION));
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
	            for (int i = 0; i < mTimetablesList.size(); i++) {
	            	HashMap<String,String> currentMap = mTimetablesList.get(i);
	            	timetablesDbAdapter.create(routeId, currentMap.get("depart"), 
	            			currentMap.get("arrive"), currentMap.get("duration"), currentMap.get("train"),
	            			currentMap.get("delay"));
	            }
	            timetablesDbAdapter.close();
            } else {
            	throw new SAXException(); // no routes in XML
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
    
    protected class TimetablesXmlHandler extends DefaultHandler {
    	
    	Boolean currentElement = false;
    	String currentValue = null;
        HashMap<String,String> currentMap = null;
        Boolean inRoute = false;
 
	    @Override
	    public void startElement(String uri, String localName, String qName,
	            Attributes attributes) throws SAXException {
	 
	        currentElement = true;
	        currentValue = null;
	 
	        if (localName.equals("xml")) {
	            mTimetablesList = new ArrayList<HashMap<String,String>>();
	        } else if (localName.equals("route")) {
	        	currentMap = new HashMap<String,String>();
	        	inRoute = true;
	        }
	 
	    }
	 
	    @Override
	    public void endElement(String uri, String localName, String qName)
	            throws SAXException {
	 
	        currentElement = false;
	        if (inRoute) {
	        	currentMap.put(localName, currentValue != null ? 
	        			currentValue : new String());
	        }
	        
	        if (localName.equalsIgnoreCase("route")) {
	        	inRoute = false;
	        	mTimetablesList.add(currentMap);
	        }
	 
	    }
	 
	    @Override
	    public void characters(char[] ch, int start, int length)
	            throws SAXException {
	 
	        if (currentElement) {
	            currentValue = new String(ch, start, length);
	            currentElement = false;
	         }
	 
	    }
    }
 
}
