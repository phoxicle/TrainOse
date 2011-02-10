package com.pheide.trainose;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;

public class RouteEdit extends Activity {
	
	private static final String TAG = "RouteEdit";
    
    private AutoCompleteTextView mSourceTextView;
    private AutoCompleteTextView mDestinationTextView;
    private CheckBox mTwoWay;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.route_edit);
        setTitle(R.string.new_route);

        // Create source/destination selectors
        String[] stations = getResources().getStringArray(R.array.stations_array);
        
        mSourceTextView = (AutoCompleteTextView) findViewById(R.id.source);
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, R.layout.autocomplete_item, stations);
        mSourceTextView.setAdapter(sourceAdapter);
        
        mDestinationTextView = (AutoCompleteTextView) findViewById(R.id.destination);
        ArrayAdapter<String> destinationAdapter = new ArrayAdapter<String>(this, R.layout.autocomplete_item, stations);
        mDestinationTextView.setAdapter(destinationAdapter);
        
        mTwoWay = (CheckBox) findViewById(R.id.twoway);
        
        // Create confirm button
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	createRoute();
            }
        });
    }
    
    private void createRoute() {
    	RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(this);
        routesDbAdapter.open();
        
        String source = mSourceTextView.getText().toString();
        String destination = mDestinationTextView.getText().toString();
        
        // Create route if it does not yet exist
        long routeId;
        try {
        	Cursor routeCursor = routesDbAdapter.fetchBySourceAndDestination(source,destination);
        	startManagingCursor(routeCursor);
        	routeId = routeCursor.getLong(routeCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
        	stopManagingCursor(routeCursor);
        } catch (Exception e) {
        	routeId = routesDbAdapter.create(source,destination);
        }

    	if (mTwoWay.isChecked()) {
    		try {
            	Cursor reversedRouteCursor = routesDbAdapter.fetchBySourceAndDestination(destination,source);
            	startManagingCursor(reversedRouteCursor);
            	routeId = reversedRouteCursor.getLong(reversedRouteCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
            	stopManagingCursor(reversedRouteCursor);
            } catch (Exception e) {
            	routesDbAdapter.create(destination,source);
            }
    	}
    	
    	routesDbAdapter.close();
    	
    	syncRoute(routeId); // uses callback
    }
    
    private void didCreateRoute(long routeId) {
    	Intent i = new Intent(this, Timetables.class);
        i.putExtra(RoutesDbAdapter.KEY_ROWID, routeId);
    	setResult(RESULT_OK,i);
        finish();
    }
    
    protected void syncRoute(long routeId) {
        
    	new AsyncTask<Long, Long, Long>() {
    		ProgressDialog mDialog;
    		 
    		protected void onPreExecute() {
    			mDialog = ProgressDialog.show(RouteEdit.this, "", 
    					RouteEdit.this.getString(R.string.sync_in_progress), true);
    		}
    		 
            protected Long doInBackground(Long... routeIds){
                try {
                	long routeId = routeIds[0];
                	TimetablesSynchronizer timetablesSynchronizer = new TimetablesSynchronizer(RouteEdit.this);
                	timetablesSynchronizer.syncTimetablesForRoute(routeId);
                	return routeId;
                } catch (Exception e) {
                    Log.e(TAG, "Exception syncing timetable", e);
                }
                return null;
            }
    	 
            @Override
            protected void onPostExecute(Long routeId) {
            	mDialog.dismiss();
            	didCreateRoute(routeId);
            }
     	        
    	}.execute(routeId);
    	
    }
    
}
