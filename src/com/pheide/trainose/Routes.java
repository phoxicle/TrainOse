/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView;

/**
 * Class which displays the list of downloaded routes.
 * 
 * @author Christine Gerpheide
 */
public class Routes extends ListActivity {
	
	private static final String TAG = "Routes";
	
	private static final int ACTIVITY_CREATE = 0;
	private static final int ACTIVITY_TIMETABLES = 1;
	
	private static final int NEW_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int SYNC_ID = Menu.FIRST + 2;
    private static final int ABOUT_ID = Menu.FIRST + 3;
    
	RoutesDbAdapter mRoutesDbAdapter;
	
	/**
	 * Initialize this view
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.routes_list);
        
        mRoutesDbAdapter = new RoutesDbAdapter(this);
        mRoutesDbAdapter.open();
        
        this.populateList();
        this.registerForContextMenu(this.getListView());
    }
    
    /* Lists */
    
    /**
     * Populate the view with a list of all stored routes.
     */
    private void populateList() {
        Cursor routesCursor = mRoutesDbAdapter.fetchAll();
        this.startManagingCursor(routesCursor);

        String[] from = new String[]{RoutesDbAdapter.KEY_SOURCE, RoutesDbAdapter.KEY_DESTINATION};
        int[] to = new int[]{R.id.source, R.id.destination};
        SimpleCursorAdapter notes = new SimpleCursorAdapter(this, R.layout.routes_row, routesCursor, from, to);
        this.setListAdapter(notes);
    }
    
    /**
     * Handler for when a route is clicked.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        this.showTimetables(id);
    }
    
    /* Context menu */
    
    /**
     * Build the context menu for a route.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.ctxmenu_delete_route);
    }

    /**
     * Handler for when a route's context menu item is clicked.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
                mRoutesDbAdapter.delete(info.id);
                this.populateList();
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    /* Options menu */
    
    /**
     * Build the options menu for routes.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, NEW_ID, 0, R.string.optmenu_new_route);
        menu.add(0, SYNC_ID, 0, R.string.optmenu_sync_all);
        menu.add(0, ABOUT_ID, 0, R.string.optmenu_about);
        return true;
    }

    /**
     * Handler for when an option menu item is pressed.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case NEW_ID:
                this.showNewRoute();
                return true;
            case SYNC_ID:
                this.syncAllRoutes();
                return true;
            case ABOUT_ID:
                this.showAbout();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
    
    /**
     * Start a new activity to show the timetables for a route.
     * 
     * @param long the routeId to show timetables for
     */
    private void showTimetables(long routeId) {
    	Intent i = new Intent(this, Timetables.class);
        i.putExtra(RoutesDbAdapter.KEY_ROWID, routeId);
        this.startActivityForResult(i, ACTIVITY_TIMETABLES);
    }
    
    /**
     * Start a new activity to create a new route.
     */
    private void showNewRoute() {
    	Intent i = new Intent(this, RouteEdit.class);
        this.startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    /**
     * Show a dialog with info about this application.
     */
    private void showAbout() { //TODO put these in some kind of "view"
    	Dialog dialog = new Dialog(this);

    	dialog.setContentView(R.layout.about_dialog);
    	dialog.setTitle(R.string.app_desc_title);

    	TextView versionTextView = (TextView) dialog.findViewById(R.id.version);
        try {
        	PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);
        	versionTextView.setText(packageInfo.versionName + " (" 
        			+ this.getString(R.string.build_number) + " " + packageInfo.versionCode + ")");

        	TextView appDescTextView = (TextView) dialog.findViewById(R.id.app_desc);
        	ViewHelper.linkifyTextView(this, appDescTextView, R.string.app_desc);
        	
        	TextView moreInfoTextView = (TextView) dialog.findViewById(R.id.more_info);
        	ViewHelper.linkifyTextView(this,moreInfoTextView, R.string.app_moreinfo);
        } catch (NameNotFoundException e) {
            // TODO Log error
        }
    	
    	dialog.show();
    }
    
    /**
     * Initiate a background task that will cycle through each route
     * and synchronize it.
     */
    private void syncAllRoutes() {
        
    	new AsyncTask<Void, Void, Void>() {
    		ProgressDialog mDialog;
    		 
    		protected void onPreExecute() {
    			mDialog = ProgressDialog.show(Routes.this, "", 
    					Routes.this.getString(R.string.sync_in_progress), true);
    		}
    		 
            protected Void doInBackground(Void... params){
                try {
                	Cursor routesCursor = mRoutesDbAdapter.fetchAll();
                    Routes.this.startManagingCursor(routesCursor);
                    int routeIdIdx = routesCursor.getColumnIndex(RoutesDbAdapter.KEY_ROWID);
                    for (routesCursor.moveToFirst(); routesCursor.isAfterLast() == false;
                    		routesCursor.moveToNext()) {
                    	long routeId = routesCursor.getLong(routeIdIdx);
                     	TimetablesSynchronizer timetablesSynchronizer = new TimetablesSynchronizer(Routes.this);
                    	timetablesSynchronizer.syncTimetablesForRoute(routeId);
                    }
                    routesCursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Exception syncing routes", e);
                }
                return null;
            }
    	 
            @Override
            protected void onPostExecute(Void result) {
            	mDialog.dismiss();
            	Routes.this.populateList();
            }
     	        
    	}.execute();
    	
    }
    
    /* Activity methods */
    
    /**
     * Handle when a view returns to this view.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode) {
        	case ACTIVITY_CREATE:
        		if (resultCode == RESULT_OK) {
	        		Bundle extras = intent.getExtras();
	                long routeId = extras != null ? extras.getLong(RoutesDbAdapter.KEY_ROWID) : null;
	                showTimetables(routeId);
        		}
        }
    }
    
    
    
}