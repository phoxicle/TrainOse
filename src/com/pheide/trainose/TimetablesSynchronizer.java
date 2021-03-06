/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            // Get details (to/from) for this route
            RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(mActivity);
            routesDbAdapter.open();
            Cursor routesCursor = routesDbAdapter.fetch(routeId);
            mActivity.startManagingCursor(routesCursor);
            String source = CursorHelper.getString(routesCursor,RoutesDbAdapter.KEY_SOURCE);
            String destination = CursorHelper.getString(routesCursor,RoutesDbAdapter.KEY_DESTINATION);
            mActivity.stopManagingCursor(routesCursor);
            routesDbAdapter.close();

            // Retrieve JSON from server
            URL sourceUrl = new URL("http://www.pheide.com/Services/TrainOse/ose_json.php?"
            		+ "from=" + URLEncoder.encode(source,"UTF-8") 
            		+ "&to=" + URLEncoder.encode(destination,"UTF-8"));
            BufferedReader in = new BufferedReader(new InputStreamReader(
    				sourceUrl.openStream()));
	    	String inputLine;
	    	String jsonStr = "";
	    	while ((inputLine = in.readLine()) != null)
	    		jsonStr = jsonStr + inputLine;
	    	in.close();
	    	JSONObject jsonObj = new JSONObject(jsonStr);
	    	
	    	// Clear current timetables and add the new ones
	    	JSONArray routes = (JSONArray) jsonObj.get("routes");
	    	if (routes.length() > 0) {
	    		 TimetablesDbAdapter timetablesDbAdapter = new TimetablesDbAdapter(mActivity);
	    		 timetablesDbAdapter.open();
	             timetablesDbAdapter.deleteByRoute(routeId);
	             
	             LegsDbAdapter legsDbAdapter = new LegsDbAdapter(mActivity);
	             legsDbAdapter.open();
	             for (int i = 0; i < routes.length(); i++) {
	            	 JSONObject route = routes.getJSONObject(i);
	            	 JSONArray legs = (JSONArray) route.get("legs");
	            	 
	            	 // Create the route, with some values from the legs for convenience
	            	 int numLegs = legs.length();
	            	 JSONObject firstLeg = legs.getJSONObject(0); // one should always exist
	            	 long timetableId = timetablesDbAdapter.create(routeId, 
	            			 firstLeg.getString("depart"), 
	            			 legs.getJSONObject(numLegs - 1).getString("arrive"), // last leg
	            			 route.getString("duration"), 
	            			 firstLeg.getString("train"), 
	            			 firstLeg.getString("trainNum"), numLegs,
	            			 firstLeg.getString("delay"));
	            	 
	            	 // Add legs for this timetable
	            	 legsDbAdapter.deleteByTimetable(timetableId);
	            	 for (int j = 0; j < legs.length(); j++) {
	            		 JSONObject leg = legs.getJSONObject(j);
	            		 legsDbAdapter.create(timetableId, leg.getString("depart"),
	            				 leg.getString("arrive"), leg.getString("train"), 
	            				 leg.getString("trainNum"),leg.getString("delay"),
	            				 leg.getString("source"),leg.getString("destination"));
	            	 }
	             }
	             legsDbAdapter.close();
	             timetablesDbAdapter.close();
	    	}
	    	
            // Update date last synced for this route
            routesDbAdapter.open();
            routesDbAdapter.touchTimestamp(routeId);
            routesDbAdapter.close();
            
	    } catch (IOException e) { // Couldn't reach host
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, mActivity.getString(R.string.exception_network), Toast.LENGTH_LONG).show();
        	    }
        	});
	    } catch (JSONException e) { // JSON empty or not formatted
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, mActivity.getString(R.string.exception_server), Toast.LENGTH_LONG).show();
        	    }
        	});
	    }  catch (Exception e) { // Something else. Hmm...
	    	mActivity.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(mActivity, mActivity.getString(R.string.exception), Toast.LENGTH_LONG).show();
        	    }
        	});
        }
	    
	    return true;
    }
    
}
