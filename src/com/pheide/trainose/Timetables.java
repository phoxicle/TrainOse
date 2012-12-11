/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ExpandableListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * Class which displays timetables for a specific route.
 * 
 * @author Christine Gerpheide
 */
public class Timetables extends ExpandableListActivity {
	
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
	
	
	private int mGroupIdColumnIndex; 
    private ExpandableListAdapter mAdapter;
    private int mCurrentLegCount = 0;
	
	/**
	 * Initialize this view.
	 */
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
        this.startManagingCursor(routesCursor);
        mSourceTitle = CursorHelper.getString(routesCursor, RoutesDbAdapter.KEY_SOURCE);
        mDestinationTitle = CursorHelper.getString(routesCursor, RoutesDbAdapter.KEY_DESTINATION);
        this.setTitle(mSourceTitle + " ⇨ " + mDestinationTitle);
        routesDbAdapter.close();
        
        mTimetablesDbAdapter = new TimetablesDbAdapter(this);
        mTimetablesDbAdapter.open();
        
        this.populateList();
        this.registerForContextMenu(this.getExpandableListView());
    }
    
    /* List methods */
    
    /**
     * Populate the view with the timetables for this route with
     * a default sorting.
     */
    private void populateList() {
        this.populateListSorted(null);
        this.setLastSynced();
    }
    
    /**
     * Retrieve the last synced timestamp for this route and show it
     * in the view.
     */
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
    
    /**
     * Populate the view with the timetables for this route with the 
     * specified sorting.
     * 
     * @param String the sorting to use for the timetables
     */
    private void populateListSorted(String sorting) {
        Cursor timetablesCursor = mTimetablesDbAdapter.fetchByRouteSorted(mRouteId, sorting);
        this.startManagingCursor(timetablesCursor);
        
        mGroupIdColumnIndex = timetablesCursor.getColumnIndexOrThrow(TimetablesDbAdapter.KEY_ROWID);
        mAdapter = new MyExpandableListAdapter(timetablesCursor,this);
        this.setListAdapter(mAdapter);
    }
    
    
    
    /* Options menu */
    
    /**
     * Create the options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SYNC_ID, 0, R.string.optmenu_sync);
        menu.add(0, SORT_ID, 0, R.string.optmenu_sort);
        menu.add(0, DELETE_ID, 0, R.string.optmenu_delete);
        menu.add(0, COPY_ALL_ID, 0, R.string.optmenu_copy_all);
        return true;
    }

    /**
     * Handle when an option menu item is selected.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case SYNC_ID:
            	this.syncRoute();
                return true;
            case SORT_ID:
            	this.showDialog(DIALOG_SORT_ID);
                return true;
            case DELETE_ID:
            	this.showDialog(DIALOG_DELETE_ID);
                return true;
            case COPY_ALL_ID:
            	this.copyAllTimetablesToClipboard();
                return true;
        }

        return super.onMenuItemSelected(featureId, item);
    }
    
    /**
     * Copy all timetables for this route to the clipboard.
     */
    protected void copyAllTimetablesToClipboard() {
    	String text = this.getText(R.string.schedule) + " " + mSourceTitle + " " 
    			+ this.getText(R.string.to) + " " + mDestinationTitle;
    	
    	Cursor timetablesCursor = mTimetablesDbAdapter.fetchByRoute(mRouteId);
    	this.startManagingCursor(timetablesCursor);
    	for (timetablesCursor.moveToFirst(); timetablesCursor.isAfterLast() == false;
    		timetablesCursor.moveToNext()) {
    			text += "\n" + this.getStringForTimetableFromCursor(timetablesCursor);
    	}
    	this.stopManagingCursor(timetablesCursor);
    	
    	this.copyTextToClipboard(text);
    }
    
    /* Context menu */
    
    /**
     * Create the context menu for each timetable.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, COPY_ID, 0, R.string.ctxmenu_copy);
        menu.add(0, SEATS_ID, 0, R.string.ctxmenu_seat_availability);
    }

    /**
     * Handler for pressing a context menu item for a timetable.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
    	
    	mTimetableId = info.id;
    	Log.w("TEST","" + mTimetableId);
    	
        switch(item.getItemId()) {
            case COPY_ID:
            	this.copyTimetableToClipboard(mTimetableId);
                return true;
            case DETAILS_ID:
            	this.showDialog(DIALOG_DETAIL_ID);
                return true;
            case SEATS_ID:
            	this.showDialog(DIALOG_SEATS_ID);
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    /**
     * Copy the selected timetable to the clipboard.
     * 
     * @param long the ID of the timetable
     */
    protected void copyTimetableToClipboard(long timetableId) {
    	Cursor timetableCursor = mTimetablesDbAdapter.fetch(timetableId);
    	this.startManagingCursor(timetableCursor);
        this.copyTextToClipboard(getStringForTimetableFromCursor(timetableCursor));
    	this.stopManagingCursor(timetableCursor);
    }
    
    /**
     * Open a web browser showing the seat availability for this route.
     */
    protected void openSeatAvailability() {

    	
    	LegsDbAdapter legsDbAdapter = new LegsDbAdapter(this);
   		legsDbAdapter.open();
   		Cursor legsCursor = legsDbAdapter.fetchByTimetable(mTimetableId);
   		this.startManagingCursor(legsCursor);
   		
   		String url = "http://www.pheide.com/Services/TrainOse/seatAvailability_new.php?";
   		
   		for (legsCursor.moveToFirst(); legsCursor.isAfterLast() == false;
   				legsCursor.moveToNext()) {
   			String source = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_SOURCE);
   	        String destination = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_DESTINATION);
   			
   			String depart = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_DEPART);
   	        String arrive = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_ARRIVE);
   	        String train = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_TRAIN_NUM);
   	        
   	        try {
	   	        url += "from[]=" + URLEncoder.encode(source,"UTF-8") 
	   	        	+ "&to[]=" + URLEncoder.encode(destination,"UTF-8")
	   	        	+ "&depart[]=" + URLEncoder.encode(depart,"UTF-8")
	   	        	+ "&arrive[]=" + URLEncoder.encode(arrive,"UTF-8")
	   	        	+ "&trainNum[]=" + URLEncoder.encode(train,"UTF-8")
	   	        	+ "&";
   	        } catch (Exception e) {
   	    		//TODO log encoding exception
   	        }
		}
   		
		legsCursor.close();
		
		Uri seatsAvailabilityUri = Uri.parse(url);
		Intent intent = new Intent(Intent.ACTION_VIEW, seatsAvailabilityUri);
   	 	this.startActivity(intent);
    }
    
    /* Dialogs */
    
    /**
     * Prepares any dynamic data needed within a dialog before it is displayed.
     * 
     * @param int the Dialog ID
     * @param Dialog the dialog
     */
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
    		case DIALOG_DETAIL_ID:
    			/*
    			Cursor timetableCursor = mTimetablesDbAdapter.fetch(mTimetableId);
                this.startManagingCursor(timetableCursor);
                String delay = CursorHelper.getString(timetableCursor,TimetablesDbAdapter.KEY_DELAY);
                this.stopManagingCursor(timetableCursor);
                
                TextView tv = new TextView(this);
                tv.setText("Delay: " + delay);
                dialog.setContentView(tv);
                */
    			break;
    	}
    }
    
    /**
     * Builder for all dialogs.
     * 
     * @param int the ID of the dialog to build
     * @return the built dialog
     */
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
        	                Timetables.this.openSeatAvailability();
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
    
    /**
     * Called by synchronize button when there are no routes.
     */
    public void syncRoute(View v) {
    	this.syncRoute();
    }
    
    /**
     * Initialize a background process to synchronize this route.
     */
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
            	Timetables.this.populateList();
            }
     	        
    	}.execute(mRouteId);
    }
    
    /* Helpers */
    
    /**
     * Helper to return a row in a Cursor as a hash map.
     */
//    TODO needed?
    public HashMap<String,String> fetchAsHashMap(long timetableId) {
    	Cursor timetableCursor = mTimetablesDbAdapter.fetch(timetableId);
        this.startManagingCursor(timetableCursor);
        
    	HashMap<String,String> timetableMap = new HashMap<String,String>();
    	String[] mapKeys = { TimetablesDbAdapter.KEY_DEPART , TimetablesDbAdapter.KEY_ARRIVE,
    			TimetablesDbAdapter.KEY_DURATION, TimetablesDbAdapter.KEY_TRAIN,
    			TimetablesDbAdapter.KEY_TRAIN_NUM, TimetablesDbAdapter.KEY_DELAY
    	};
    	for (int i = 0; i < mapKeys.length; i++) {
    		String key = mapKeys[i];
    		timetableMap.put(key, CursorHelper.getString(timetableCursor,key));
    	}
    	
    	this.stopManagingCursor(timetableCursor);
    	return timetableMap;
    }
    
    /**
     * Given a managed cursor, return a single timetable row as a readable string.
     * 
     * @param Cursor A managed timetableCursor
     * @return the readable timetable string
     */
    protected String getStringForTimetableFromCursor(Cursor timetableCursor) {
    	// TODO implement as toString() functions
    	// Copy information for each leg
    	
    	LegsDbAdapter legsDbAdapter = new LegsDbAdapter(this);
   		legsDbAdapter.open();
   		Cursor legsCursor = legsDbAdapter.fetchByTimetable(
   				CursorHelper.getLong(timetableCursor, TimetablesDbAdapter.KEY_ROWID));
   		this.startManagingCursor(legsCursor);
   		
   		String text = new String();
   		
   		for (legsCursor.moveToFirst(); legsCursor.isAfterLast() == false;
   				legsCursor.moveToNext()) {
   			String source = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_SOURCE);
   	        String destination = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_DESTINATION);
   			
   			String depart = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_DEPART);
   	        String arrive = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_ARRIVE);
   	        String train = CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_TRAIN)
   	        		+ " " + CursorHelper.getString(legsCursor,LegsDbAdapter.KEY_TRAIN_NUM);
   	        
   	        text += source + " -> " + destination + "\n";
   	        text += depart + " - " + arrive + " " + train + "\n";
		}
		legsCursor.close();
		
        return text;
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
   
   
   public class MyExpandableListAdapter extends CursorTreeAdapter {
	   LayoutInflater mInflater;
		
       public MyExpandableListAdapter(Cursor cursor, Context context) {
    	   super(cursor, context);
    	   mInflater = LayoutInflater.from(context); 
       }

       @Override
       protected Cursor getChildrenCursor(Cursor groupCursor) {
    	    LegsDbAdapter legsDbAdapter = new LegsDbAdapter(Timetables.this);
      		legsDbAdapter.open();
      		return legsDbAdapter.fetchByTimetable(
      				CursorHelper.getLong(groupCursor, TimetablesDbAdapter.KEY_ROWID));
       }

	@Override
	protected void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {
		
		TextView tv0 = (TextView) view.findViewById(R.id.count); 
		tv0.setText(Integer.toString(mCurrentLegCount++) + '.');
		
		TextView tv = (TextView) view.findViewById(R.id.depart); 
		tv.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_DEPART));
		
		TextView tv1 = (TextView) view.findViewById(R.id.arrive); 
		tv1.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_ARRIVE));
		
		TextView tv3 = (TextView) view.findViewById(R.id.train); 
		tv3.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_TRAIN));
		
		TextView tv7 = (TextView) view.findViewById(R.id.train_num); 
		tv7.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_TRAIN_NUM));
		
		TextView tv4 = (TextView) view.findViewById(R.id.source); 
		tv4.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_SOURCE));
		
		TextView tv5 = (TextView) view.findViewById(R.id.destination); 
		tv5.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_DESTINATION));
		
		TextView tv6 = (TextView) view.findViewById(R.id.delay);
		// For now, don't display the delay, since it would invalidate the day caching...
		//tv6.setText(CursorHelper.getString(cursor, LegsDbAdapter.KEY_DELAY));
		tv6.setText(" ");
		
	}

	@Override
	protected void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {
		mCurrentLegCount = 1; // reset leg counter
		
		TextView tv = (TextView) view.findViewById(R.id.depart); 
		tv.setText(CursorHelper.getString(cursor, TimetablesDbAdapter.KEY_DEPART));
		
		TextView tv1 = (TextView) view.findViewById(R.id.arrive); 
		tv1.setText(CursorHelper.getString(cursor, TimetablesDbAdapter.KEY_ARRIVE)); 
		
		TextView tv2 = (TextView) view.findViewById(R.id.duration); 
		tv2.setText(CursorHelper.getString(cursor, TimetablesDbAdapter.KEY_DURATION)); 
		
		TextView tv3 = (TextView) view.findViewById(R.id.train); 
		if (CursorHelper.getInt(cursor, TimetablesDbAdapter.KEY_NUM_LEGS) > 1) {
			tv3.setText("*");
		} else {
			tv3.setText(CursorHelper.getString(cursor, TimetablesDbAdapter.KEY_TRAIN) 
					+ " " + CursorHelper.getString(cursor, TimetablesDbAdapter.KEY_TRAIN_NUM));
		}
		
	}

	@Override
	protected View newChildView(Context context, Cursor cursor, boolean isLastChild,
			ViewGroup parent) {
		View view = mInflater.inflate(R.layout.legs_row, parent, false); 
		return view;
	}

	@Override
	protected View newGroupView(Context context, Cursor cursor, boolean isLastChild,
			ViewGroup parent) {
		View view = mInflater.inflate(R.layout.timetables_row, parent, false); 
		return view;
	}

   }
    
}