/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

/**
 * DbAdapter to handle the timetables table.
 * 
 * @author Christine Gerpheide
 */
public class LegsDbAdapter extends AbstractDbAdapter {

	public static final String KEY_ROWID = "_id";
	public static final String KEY_TIMETABLE = "timetable_id";
	public static final String KEY_DEPART = "depart";
	public static final String KEY_ARRIVE = "arrive";
	public static final String KEY_TRAIN = "train";
	public static final String KEY_TRAIN_NUM = "train_num";
	public static final String KEY_DELAY = "delay";
	public static final String KEY_SOURCE = "source";
	public static final String KEY_DESTINATION = "destination";

    private static final String DATABASE_TABLE = "legs";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public LegsDbAdapter(Context ctx) {
    	super(ctx);
    }
    
    /**
     * Create a new timetable.
     * 
     * @param timetableId
     * @param depart
     * @param arrive
     * @param train
     * @param trainNum
     * @param delay
     * @param source
     * @param destination
     * @return rowId or -1 if failed
     */
    public long create(long timetableId, String depart, String arrive, 
    		String train, String trainNum, String delay, String source,
    		String destination) {
    	ContentValues args = new ContentValues();
    	args.put(KEY_TIMETABLE,timetableId);
    	args.put(KEY_DEPART,depart);
    	args.put(KEY_ARRIVE,arrive);
    	args.put(KEY_TRAIN,train);
    	args.put(KEY_TRAIN_NUM,trainNum);
    	args.put(KEY_DELAY,delay);
    	args.put(KEY_SOURCE,source);
    	args.put(KEY_DESTINATION,destination);
    	
    	return mDb.insert(DATABASE_TABLE, null,args);
    }
    

    /**
     * Delete the timetable with the given rowId
     * 
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean delete(long rowId) {
    	return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    /**
     * Delete the legs with the given timetableId
     * 
     * @param timetableId the timetable to delete legs for
     * @return true if deleted, false otherwise
     */
    public boolean deleteByTimetable(long timetableId) {
    	return mDb.delete(DATABASE_TABLE, KEY_TIMETABLE + "=" + timetableId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all routes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAll() {
    	return mDb.query(DATABASE_TABLE, new String[] {"*"}, 
    			null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at the timetable that matches the given rowId
     * 
     * @param rowId id of route to retrieve
     * @return Cursor positioned to matching route, if found
     * @throws SQLException if route could not be found/retrieved
     */
    public Cursor fetch(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {"*"}, 
        			KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
    /**
     * Return a Cursor positioned at the legs that match a given timetables ID
     * 
     * @param rowId id of timetable to retrieve
     * @return Cursor positioned to matching route, if found
     * @throws SQLException if route could not be found/retrieved
     */
    public Cursor fetchByTimetable(long timetableId) throws SQLException {
    	Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {"*"}, 
        			KEY_TIMETABLE + "=" + timetableId, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }
    
}
