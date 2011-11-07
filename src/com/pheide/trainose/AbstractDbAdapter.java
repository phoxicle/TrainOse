/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Abstract class to take care of shared DbAdapter code.
 * 
 * Note that all create statements for all tables must be here.
 * 
 * @author Christine Gerpheide
 */
public abstract class AbstractDbAdapter {

    protected static final String TAG = "TrainOseDbAdapter";
    protected DatabaseHelper mDbHelper;
    protected SQLiteDatabase mDb;

    protected static final String TABLE_CREATE_ROUTES =
        "create table routes (_id integer primary key autoincrement, "
        + "source text not null, destination text not null, timestamp integer);";
    protected static final String TABLE_CREATE_TIMETABLES =    
        "create table timetables (_id integer primary key autoincrement, "
    	+ "route_id integer, depart text not null, arrive text not null, "
    	+ "duration text not null, train text not null, train_num text not null, " 
    	+ "num_legs integer not null, delay text not null" 
    	+ ");";
    protected static final String TABLE_CREATE_LEGS =    
        "create table legs (_id integer primary key autoincrement, "
    	+ "source text not null, destination text not null, "
    	+ "timetable_id integer, depart text not null, arrive text not null, "
    	+ "train text not null, train_num text not null, "
    	+ "delay text not null" 
    	+ ");";

    protected static final String DATABASE_NAME = "data";
    protected static final int DATABASE_VERSION = 2;

    protected final Context mCtx;

    protected static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE_ROUTES);
            db.execSQL(TABLE_CREATE_TIMETABLES);
            db.execSQL(TABLE_CREATE_LEGS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS routes");
            onCreate(db);
        }
    }

    public AbstractDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public AbstractDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

}
