/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Timetables extends ListActivity {
	
	private static final String TAG = "Timetables";
	
    private static final int SYNC_ID = Menu.FIRST;
    private static final int SORT_ID = Menu.FIRST + 1;
    private static final int COPY_ID = Menu.FIRST + 2;
    private static final int DETAILS_ID = Menu.FIRST + 3;
    private static final int SEATS_ID = Menu.FIRST + 4;
    private static final int DELETE_ID = Menu.FIRST + 5;
    private static final int COPY_ALL_ID = Menu.FIRST + 6;
    
    static final int DIALOG_SORT_ID = 0;
    static final int DIALOG_DETAIL_ID = 1;
    static final int DIALOG_SEATS_ID = 2;
    static final int DIALOG_DELETE_ID = 3;
    
	TimetablesDbAdapter mTimetablesDbAdapter;
	long mRouteId;
	String mSourceTitle;
	String mDestinationTitle;
	long mTimetableId;
	ProgressDialog mDialog;
	public static List<HashMap<String,String>> timetablesList = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timetables_list);
        
        Bundle extras = getIntent().getExtras();
        mRouteId = extras != null ? extras.getLong(RoutesDbAdapter.KEY_ROWID) : null;

        // Set title
        RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(this);
        routesDbAdapter.open();
        Cursor routesCursor = routesDbAdapter.fetch(mRouteId);
        startManagingCursor(routesCursor);
        mSourceTitle = CursorHelper.getString(routesCursor, RoutesDbAdapter.KEY_SOURCE);
        mDestinationTitle = CursorHelper.getString(routesCursor, RoutesDbAdapter.KEY_DESTINATION);
        setTitle(mSourceTitle + " ⇨ " + mDestinationTitle);
        routesDbAdapter.close();
        
        mTimetablesDbAdapter = new TimetablesDbAdapter(this);
        mTimetablesDbAdapter.open();
        
        populateList();
        registerForContextMenu(getListView());
    }
    
    /* List methods */
    
    private void populateList() {
        this.populateListSorted(null);
        this.setLastSynced();
    }
    
    private void setLastSynced() {
        RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(this);
        routesDbAdapter.open();
        Cursor routesCursor = routesDbAdapter.fetch(mRouteId);
        long timestamp = CursorHelper.getLong(routesCursor, RoutesDbAdapter.KEY_TIMESTAMP);
        if (timestamp > 0) {
        	Timestamp time = new Timestamp(timestamp);
        	TextView lastSyncedTextView = (TextView) findViewById(R.id.last_synced);
        	lastSyncedTextView.setText(time.toLocaleString());
        }
        routesDbAdapter.close();
    }
    
    private void populateListSorted(String sorting) {
        Cursor timetablesCursor = mTimetablesDbAdapter.fetchByRouteSorted(mRouteId, sorting);
        startManagingCursor(timetablesCursor);
        TimetablesCursorAdapter timetables = new TimetablesCursorAdapter(this,timetablesCursor);
        setListAdapter(timetables);
    }
    
    /* Options menu */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SYNC_ID, 0, R.string.optmenu_sync);
        menu.add(0, SORT_ID, 0, R.string.optmenu_sort);
        menu.add(0, DELETE_ID, 0, R.string.optmenu_delete);
        menu.add(0, COPY_ALL_ID, 0, R.string.optmenu_copy_all);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case SYNC_ID:
            	syncRoute();
                return true;
            case SORT_ID:
            	showDialog(DIALOG_SORT_ID);
                return true;
            case DELETE_ID:
            	showDialog(DIALOG_DELETE_ID);
                return true;
            case COPY_ALL_ID:
            	copyAllTimetablesToClipboard();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
    
    protected void copyAllTimetablesToClipboard() {
    	String text = this.getText(R.string.schedule) + " " + mSourceTitle + " " 
    			+ this.getText(R.string.to) + " " + mDestinationTitle;
    	
    	Cursor timetablesCursor = mTimetablesDbAdapter.fetchByRoute(mRouteId);
    	startManagingCursor(timetablesCursor);
    	for (timetablesCursor.moveToFirst(); timetablesCursor.isAfterLast() == false;
    		timetablesCursor.moveToNext()) {
    			text += "\n" + getStringForTimetableFromCursor(timetablesCursor);
    	}
    	stopManagingCursor(timetablesCursor);
    	
    	copyTextToClipboard(text);
    }
    
    /* Context menu */
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, COPY_ID, 0, R.string.ctxmenu_copy);
        //menu.add(0, DETAILS_ID, 0, R.string.ctxmenu_details);
        menu.add(0, SEATS_ID, 0, R.string.ctxmenu_seat_availability);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	mTimetableId = info.id;
        switch(item.getItemId()) {
            case COPY_ID:
            	copyTimetableToClipboard(mTimetableId);
                return true;
            case DETAILS_ID:
            	showDialog(DIALOG_DETAIL_ID);
                return true;
            case SEATS_ID:
            	showDialog(DIALOG_SEATS_ID);
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    protected void copyTimetableToClipboard(long timetableId) {
    	Cursor timetableCursor = mTimetablesDbAdapter.fetch(timetableId);
    	startManagingCursor(timetableCursor);
        copyTextToClipboard(getStringForTimetableFromCursor(timetableCursor));
    	stopManagingCursor(timetableCursor);
    }
    
    protected void openSeatAvailability() {
    	//http://tickets.trainose.gr/dromologia/touch_seats.html?c=krathsh_wt&op=trip_available_seats&trip=56|ŒëŒòŒóŒù|ŒòŒïŒ£Œ£|20110210|19.29|20110210|23.55|11:&lang=gr
    	HashMap<String,String> timetableMap = this.fetchAsHashMap(mTimetableId);
    	try {
	    	 Uri seatsAvailabilityUri = Uri.parse("http://www.pheide.com/Services/TrainOse/seatAvailability.php?"
	         		+ "from=" + URLEncoder.encode(mSourceTitle,"UTF-8") 
	         		+ "&to=" + URLEncoder.encode(mDestinationTitle,"UTF-8")
	         		+ "&depart=" + timetableMap.get(TimetablesDbAdapter.KEY_DEPART)
	         		+ "&arrive=" + timetableMap.get(TimetablesDbAdapter.KEY_ARRIVE)
	         		+ "&trainNum=" + timetableMap.get(TimetablesDbAdapter.KEY_TRAIN_NUM));
	    	 Intent intent = new Intent(Intent.ACTION_VIEW, seatsAvailabilityUri);
	    	 startActivity(intent);
    	} catch (Exception e) {
    		//TODO log encoding exception
    	}
    }
    
    /* Dialogs */
    
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
    		case DIALOG_DETAIL_ID:
    			Cursor timetableCursor = mTimetablesDbAdapter.fetch(mTimetableId);
                startManagingCursor(timetableCursor);
                String delay = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_DELAY);
                stopManagingCursor(timetableCursor);
                
                TextView tv = new TextView(this);
                tv.setText("Delay: " + delay);
                dialog.setContentView(tv);
    			break;
    	}
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        
        switch(id) {
        case DIALOG_SORT_ID:
        	final CharSequence[] items = {this.getString(R.string.depart),
        			this.getString(R.string.arrive),this.getString(R.string.duration),
        			this.getString(R.string.train)};
        	alertBuilder.setTitle(this.getString(R.string.sortBy));
        	alertBuilder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int item) {
        	    	String sorting;
        	    	switch(item) {
        	    	case 0:
        	    		sorting = TimetablesDbAdapter.KEY_DEPART;
        	    		break;
        	    	case 1:
        	    		sorting = TimetablesDbAdapter.KEY_ARRIVE;
        	    		break;
        	    	case 2:
        	    		sorting = TimetablesDbAdapter.KEY_DURATION;
        	    		break;
        	    	case 3:
        	    		sorting = TimetablesDbAdapter.KEY_TRAIN;
        	    		break;
        	    	default:
        	    		sorting = null;
        	    	}
        	    	Timetables.this.populateListSorted(sorting);
        	    	dialog.dismiss();
        	    }
        	});
        	dialog = alertBuilder.create();
            break;
        case DIALOG_DETAIL_ID:
        	dialog = new Dialog(this);
        	dialog.setTitle("Details");
        	break;
        case DIALOG_SEATS_ID:
        	alertBuilder.setMessage(R.string.seats_open_new_window)
        	       .setCancelable(false)
        	       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                openSeatAvailability();
        	           }
        	       })
        	       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	dialog = alertBuilder.create();
        	break;
        case DIALOG_DELETE_ID:
        	alertBuilder.setMessage(R.string.confirm_delete)
        	       .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(Timetables.this);
        	               routesDbAdapter.open();
        	               routesDbAdapter.delete(mRouteId);
        	               routesDbAdapter.close();
        	               Timetables.this.finish();
        	           }
        	       })
        	       .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	dialog = alertBuilder.create();
        	break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    protected void syncRoute() {
    
    	new AsyncTask<Long, Void, Void>() {
    		ProgressDialog mDialog;
    		 
    		protected void onPreExecute() {
    			mDialog = ProgressDialog.show(Timetables.this, "", 
    					Timetables.this.getString(R.string.sync_in_progress), true);
    		}
    		 
            protected Void doInBackground(Long... routeIds){
               	TimetablesSynchronizer timetablesSynchronizer = new TimetablesSynchronizer(Timetables.this);
               	timetablesSynchronizer.syncTimetablesForRoute(routeIds[0]);
                return null;
            }
    	 
            @Override
            protected void onPostExecute(Void result) {
            	mDialog.dismiss();
            	populateList();
            }
     	        
    	}.execute(mRouteId);
    	
    }
    
    /* Helpers */
    
    /**
     * Helper to return a row in a Cursor as a hash map.
     */
    public HashMap<String,String> fetchAsHashMap(long timetableId) {
    	Cursor timetableCursor = mTimetablesDbAdapter.fetch(timetableId);
        startManagingCursor(timetableCursor);
        
    	HashMap<String,String> timetableMap = new HashMap<String,String>();
    	String[] mapKeys = { TimetablesDbAdapter.KEY_DEPART , TimetablesDbAdapter.KEY_ARRIVE,
    			TimetablesDbAdapter.KEY_DURATION, TimetablesDbAdapter.KEY_TRAIN,
    			TimetablesDbAdapter.KEY_TRAIN_NUM, TimetablesDbAdapter.KEY_DELAY
    	};
    	for (int i = 0; i < mapKeys.length; i++) {
    		String key = mapKeys[i];
    		timetableMap.put(key, CursorHelper.getString(timetableCursor,key));
    	}
    	
    	stopManagingCursor(timetableCursor);
    	return timetableMap;
    }
    
    /**
     * Given a managed cursor, return a single timetable row as a readable string.
     * 
     * @param Cursor A managed timetableCursor
     * @return the readable timetable string
     */
    protected String getStringForTimetableFromCursor(Cursor timetableCursor) {
    	String depart = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_DEPART);
        String arrive = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_ARRIVE);
        String duration = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_DURATION);
        String train = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_TRAIN)
        		+ " " + CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_TRAIN_NUM);
        
        return depart + " - " + arrive + " (" + duration + ") " + train;
   }
   
    /**
     * Copy the given text to the user's clipboard and show a toast.
     * 
     * @param String the text to copy
     */
   protected void copyTextToClipboard(String text) {
	   ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
	   clipboard.setText(text);
	   Toast.makeText(this, R.string.copy_succesful, Toast.LENGTH_SHORT).show();
   }
    
}