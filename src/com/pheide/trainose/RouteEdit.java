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
    private CheckBox mTwoWayCheckBox;
    
    long mRouteId;
    Boolean mIsTwoWay;
    long mReversedRouteId;
    
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
        
        mTwoWayCheckBox = (CheckBox) findViewById(R.id.twoway);
        
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
        try {
        	Cursor routeCursor = routesDbAdapter.fetchBySourceAndDestination(source,destination);
        	startManagingCursor(routeCursor);
        	mRouteId = routeCursor.getLong(routeCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
        	stopManagingCursor(routeCursor);
        } catch (Exception e) {
        	mRouteId = routesDbAdapter.create(source,destination);
        }

    	if (mTwoWayCheckBox.isChecked()) {
    		mIsTwoWay = true;
    		try {
            	Cursor reversedRouteCursor = routesDbAdapter.fetchBySourceAndDestination(destination,source);
            	startManagingCursor(reversedRouteCursor);
            	mReversedRouteId = reversedRouteCursor.getLong(reversedRouteCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID));
            	stopManagingCursor(reversedRouteCursor);
            } catch (Exception e) {
            	mReversedRouteId = routesDbAdapter.create(destination,source);
            }
    	}
    	
    	routesDbAdapter.close();
    	
    	syncRoute(); // uses callback
    }
    
    private void didCreateRoute() {
    	Intent i = new Intent(this, Timetables.class);
        i.putExtra(RoutesDbAdapter.KEY_ROWID, mRouteId);
    	setResult(RESULT_OK,i);
        finish();
    }
    
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
            	didCreateRoute();
            }
     	        
    	}.execute();
    	
    }
    
}
