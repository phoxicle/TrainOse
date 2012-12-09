/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import com.pheide.trainose.FlatCharArrayAdapter;

import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

/**
 * Class which displays the form to create or edit routes.
 * 
 * @author Christine Gerpheide
 */
public class RouteEdit extends Activity {
	
	private static final String TAG = "RouteEdit";
    
    private AutoCompleteTextView mSourceTextView;
    private AutoCompleteTextView mDestinationTextView;
    private CheckBox mTwoWayCheckBox;
    
    long mRouteId;
    Boolean mIsTwoWay;
    long mReversedRouteId;
    
    /**
     * Initialize this view
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.route_edit);
        this.setTitle(R.string.new_route);

        // Create source/destination selectors
        String[] stations = getResources().getStringArray(R.array.stations_array);
        
        mSourceTextView = (AutoCompleteTextView) findViewById(R.id.source);
        FlatCharArrayAdapter sourceAdapter = new FlatCharArrayAdapter(this, R.layout.autocomplete_item, stations);
        mSourceTextView.setAdapter(sourceAdapter);
        
        mDestinationTextView = (AutoCompleteTextView) findViewById(R.id.destination);
        FlatCharArrayAdapter destinationAdapter = new FlatCharArrayAdapter(this, R.layout.autocomplete_item, stations);
        mDestinationTextView.setAdapter(destinationAdapter);
        
        mTwoWayCheckBox = (CheckBox) findViewById(R.id.twoway);
        
        // Create confirm button
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if (stationsAreValid()) {
            		RouteEdit.this.createRoute();
            	} else {
            		Toast.makeText(RouteEdit.this, R.string.invalid_stations, Toast.LENGTH_SHORT).show();
            	}
            }
        });
    }
    
    /**
     * Check whether the stations entered are valid stations.
     * 
     * @return boolean
     */
    private Boolean stationsAreValid() { // Android really ought to handle this itself..
    	// Make sure stations entered are capitalized
    	String sourceText = mSourceTextView.getText().toString();
    	String destinationText = mDestinationTextView.getText().toString();
    	
    	if (sourceText.length() == 0 || destinationText.length() == 0) {
    		return false;
    	}
    	
    	// Capitalize first letter, lowercase the rest
    	mSourceTextView.setText(sourceText.substring(0, 1).toUpperCase() 
    			+ sourceText.substring(1).toLowerCase());
    	mDestinationTextView.setText(destinationText.substring(0, 1).toUpperCase() 
    			+ destinationText.substring(1).toLowerCase());
    	
    	String[] stations = getResources().getStringArray(R.array.stations_array);
        Arrays.sort(stations);
        if (Arrays.binarySearch(stations, mSourceTextView.getText().toString()) > 0
        	&& Arrays.binarySearch(stations, mDestinationTextView.getText().toString()) > 0) {
            return true;
        } else {
        	return false;
        }
    }
    
    /**
     * Create a route with the stations entered by the user.
     */
    private void createRoute() {
    	RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(this);
        routesDbAdapter.open();
        
        String source = mSourceTextView.getText().toString();
        String destination = mDestinationTextView.getText().toString();
        
        // Create route if it does not yet exist
        try {
        	Cursor routeCursor = routesDbAdapter.fetchBySourceAndDestination(source,destination);
        	this.startManagingCursor(routeCursor);
        	mRouteId = routeCursor.getLong(routeCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
        	this.stopManagingCursor(routeCursor);
        } catch (Exception e) {
        	mRouteId = routesDbAdapter.create(source,destination);
        }

    	if (mTwoWayCheckBox.isChecked()) {
    		mIsTwoWay = true;
    		try {
            	Cursor reversedRouteCursor = routesDbAdapter.fetchBySourceAndDestination(destination,source);
            	this.startManagingCursor(reversedRouteCursor);
            	mReversedRouteId = reversedRouteCursor.getLong(reversedRouteCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
            	this.stopManagingCursor(reversedRouteCursor);
            } catch (Exception e) {
            	mReversedRouteId = routesDbAdapter.create(destination,source);
            }
    	}
    	
    	routesDbAdapter.close();
    	
    	this.syncRoute(); // uses callback
    }
    
    /**
     * Callback after creating a route. Close this view and
     * show the timetables for the route.
     */
    private void didCreateRoute() {
    	Intent i = new Intent(this, Timetables.class);
        i.putExtra(RoutesDbAdapter.KEY_ROWID, mRouteId);
    	this.setResult(RESULT_OK,i);
        this.finish();
    }
    
    /**
     * Initialize a background task to synchronize this route.
     */
    protected void syncRoute() {
        
    	new AsyncTask<Void, Void, Void>() {
    		ProgressDialog mDialog;
    		
    		@Override
    		protected void onPreExecute() {
    			mDialog = ProgressDialog.show(RouteEdit.this, "", 
    					RouteEdit.this.getString(R.string.sync_in_progress), true);
    		}
    		
    		@Override
            protected Void doInBackground(Void... params){
                try {
                	TimetablesSynchronizer timetablesSynchronizer = new TimetablesSynchronizer(RouteEdit.this);
                	timetablesSynchronizer.syncTimetablesForRoute(mRouteId);
                	
                	if (mIsTwoWay) {
                		timetablesSynchronizer.syncTimetablesForRoute(mReversedRouteId);
                	}
                } catch (Exception e) {
                    Log.e(TAG, "Exception syncing timetables", e);
                }
				return null;
            }
    	 
    		@Override
            protected void onPostExecute(Void result) {
            	mDialog.dismiss();
            	RouteEdit.this.didCreateRoute();
            }
     	        
    	}.execute();
    	
    }
    
}
