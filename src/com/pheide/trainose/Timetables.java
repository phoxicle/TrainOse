package com.pheide.trainose;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
    
    static final int DIALOG_SORT_ID = 0;
    static final int DIALOG_DETAIL_ID = 1;
    
	TimetablesDbAdapter mTimetablesDbAdapter;
	long mRouteId;
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
        String source = routesCursor.getString(routesCursor.getColumnIndex(RoutesDbAdapter.KEY_SOURCE));
        String destination = routesCursor.getString(routesCursor.getColumnIndex(RoutesDbAdapter.KEY_DESTINATION));
        setTitle(source + " ⇨ " + destination);
        routesDbAdapter.close();
        
        mTimetablesDbAdapter = new TimetablesDbAdapter(this);
        mTimetablesDbAdapter.open();
        
        populateList();
        registerForContextMenu(getListView());
    }
    
    private void populateList() {
        this.populateListSorted(null);
        this.setLastSynced();
        
    }
    
    private void setLastSynced() {
        RoutesDbAdapter routesDbAdapter = new RoutesDbAdapter(this);
        routesDbAdapter.open();
        Cursor routesCursor = routesDbAdapter.fetch(mRouteId);
        long timestamp = routesCursor.getLong(routesCursor.getColumnIndex(RoutesDbAdapter.KEY_TIMESTAMP));
        Timestamp time = new Timestamp(timestamp);
        TextView lastSyncedTextView = (TextView) findViewById(R.id.last_synced);
        lastSyncedTextView.setText(time.toString());
        routesDbAdapter.close();
    }
    
    private void populateListSorted(String sorting) {
        Cursor timetablesCursor = mTimetablesDbAdapter.fetchByRouteSorted(mRouteId, sorting);
        startManagingCursor(timetablesCursor);

        String[] from = new String[]{TimetablesDbAdapter.KEY_DEPART, 
        		TimetablesDbAdapter.KEY_ARRIVE, TimetablesDbAdapter.KEY_DURATION,
        		TimetablesDbAdapter.KEY_TRAIN};
        int[] to = new int[]{R.id.depart, R.id.arrive, R.id.duration, R.id.train};

        SimpleCursorAdapter timetables = 
            new SimpleCursorAdapter(this, R.layout.timetables_row, timetablesCursor, from, to);
        setListAdapter(timetables);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, SYNC_ID, 0, R.string.optmenu_sync);
        menu.add(0, SORT_ID, 0, R.string.optmenu_sort);
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
        }

        return super.onMenuItemSelected(featureId, item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, COPY_ID, 0, R.string.ctxmenu_copy);
        menu.add(0, DETAILS_ID, 0, R.string.ctxmenu_details);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case COPY_ID:
            	// Alternatively, could copy text from visible views instead of DB
                Cursor timetableCursor = mTimetablesDbAdapter.fetch(info.id);
                startManagingCursor(timetableCursor);
                String depart = timetableCursor.getString(timetableCursor.getColumnIndex(TimetablesDbAdapter.KEY_DEPART));
                String arrive = timetableCursor.getString(timetableCursor.getColumnIndex(TimetablesDbAdapter.KEY_ARRIVE));
                String duration = timetableCursor.getString(timetableCursor.getColumnIndex(TimetablesDbAdapter.KEY_DURATION));
                String train = timetableCursor.getString(timetableCursor.getColumnIndex(TimetablesDbAdapter.KEY_TRAIN));
                
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            	clipboard.setText(depart + "- " + arrive + " (" + duration + ") " + train);
            	
            	stopManagingCursor(timetableCursor);
                return true;
            case DETAILS_ID:
            	mTimetableId = info.id;
            	showDialog(DIALOG_DETAIL_ID);
               
                return true;
        }
        return super.onContextItemSelected(item);
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case DIALOG_SORT_ID:
        	final CharSequence[] items = {this.getString(R.string.depart),
        			this.getString(R.string.arrive),this.getString(R.string.duration),
        			this.getString(R.string.train)};

        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setTitle(this.getString(R.string.sortBy));
        	builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
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
        	dialog = builder.create();
            break;
        case DIALOG_DETAIL_ID:
        	dialog = new Dialog(this);
        	dialog.setTitle("Details");
        	break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch(id) {
    		case DIALOG_DETAIL_ID:
    			Cursor timetableCursor = mTimetablesDbAdapter.fetch(mTimetableId);
                startManagingCursor(timetableCursor);
                String delay = timetableCursor.getString(timetableCursor.getColumnIndex(TimetablesDbAdapter.KEY_DELAY));
                stopManagingCursor(timetableCursor);
                
                TextView tv = new TextView(this);
                tv.setText("Delay: " + delay);
                dialog.setContentView(tv);
    			break;
    	}
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

	
    
}