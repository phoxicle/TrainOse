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

public class RoutesDbAdapter extends AbstractDbAdapter {

	public static final String KEY_SOURCE = "source";
	public static final String KEY_DESTINATION = "destination";
	public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_ROWID = "_id";

    private static final String DATABASE_TABLE = "routes";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public RoutesDbAdapter(Context ctx) {
    	super(ctx);
    }
    
    /**
     * Create a new route.
     * 
     * @param source
     * @param destination
     * @return rowId or -1 if failed
     */
    public long create(String source, String destination) {
    	ContentValues args = new ContentValues();
    	args.put(KEY_SOURCE,source);
    	args.put(KEY_DESTINATION,destination);
    	
    	return mDb.insert(DATABASE_TABLE, null,args);
    }
    

    /**
     * Delete the route with the given rowId
     * 
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean delete(long rowId) {
    	return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all routes in the database
     * 
     * @return Cursor over all notes
     */
    public Cursor fetchAll() {
    	return mDb.query(DATABASE_TABLE, new String[] {"*"},
    			null, null, null, null, KEY_SOURCE + ',' + KEY_DESTINATION);
    }

    /**
     * Return a Cursor positioned at the route that matches the given rowId
     * 
     * @param rowId id of route to retrieve
     * @return Cursor positioned to matching route, if found
     * @throws SQLException if route could not be found/retrieved
     */
    public Cursor fetch(long rowId) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {"*"},
            		KEY_ROWID + "=" + rowId, null,
                    null, null, null, "1");
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }
    
    /**
     * Return a Cursor positioned at the route that matches the given rowId
     * 
     * @param String source
     * @param String destination
     * @return Cursor positioned to matching route, if found
     * @throws SQLException if route could not be found/retrieved
     */
    public Cursor fetchBySourceAndDestination(String source, String destination) throws SQLException {
        Cursor mCursor =
            mDb.query(true, DATABASE_TABLE, new String[] {"*"}, 
            		KEY_SOURCE + "='" + source + "' AND " + KEY_DESTINATION + "='" + destination + "'", null,
                    null, null, null, "1");
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    /**
     * Update the route using the details provided.
     * 
     * @param rowId
     * @param source
     * @param destination
     * @param timestamp
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean update(long rowId, String source, String destination, long timestamp) {
        ContentValues args = new ContentValues();
        args.put(KEY_SOURCE,source);
        args.put(KEY_DESTINATION,destination);

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
    public boolean touchTimestamp(long rowId) {
    	ContentValues args = new ContentValues();
        args.put(KEY_TIMESTAMP,System.currentTimeMillis());

        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
