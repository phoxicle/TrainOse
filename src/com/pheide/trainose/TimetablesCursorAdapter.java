/**
 * Copyright (c) 2011 Christine Gerpheide <christine.ger@pheide.com>
 * 
 * This code is distributed under the MIT License. Please see LICENSE.txt
 * for more details.
 */

package com.pheide.trainose;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

/**
 * Special cursor adapter to show a custom view of timetables
 * in a grid-like form instead of a simple list.
 * 
 * @author Christine Gerpheide
 */
public class TimetablesCursorAdapter extends CursorAdapter {
	
	LayoutInflater mInflater;
	
	int mIdxDepart;
	int mIdxArrive;
	int mIdxDuration;
	int mIdxTrain;
	int mIdxTrainNum;

	/**
	 * Construct the adapter.
	 * 
	 * @param context
	 * @param cursor
	 */
	public TimetablesCursorAdapter(Context context, Cursor cursor) {
		super(context, cursor);
		
		mInflater = LayoutInflater.from(context); 
		
		// more efficient to do this before-hand
		mIdxDepart = cursor.getColumnIndex(TimetablesDbAdapter.KEY_DEPART);
		mIdxArrive = cursor.getColumnIndex(TimetablesDbAdapter.KEY_ARRIVE);
		mIdxDuration = cursor.getColumnIndex(TimetablesDbAdapter.KEY_DURATION);
		mIdxTrain = cursor.getColumnIndex(TimetablesDbAdapter.KEY_TRAIN);
		mIdxTrainNum = cursor.getColumnIndex(TimetablesDbAdapter.KEY_TRAIN_NUM);
	}

	/**
	 * Bind each column in the timetable to a view.
	 */
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView tv = (TextView) view.findViewById(R.id.depart); 
		tv.setText(cursor.getString(mIdxDepart));
		
		TextView tv1 = (TextView) view.findViewById(R.id.arrive); 
		tv1.setText(cursor.getString(mIdxArrive)); 
		
		TextView tv2 = (TextView) view.findViewById(R.id.duration); 
		tv2.setText(cursor.getString(mIdxDuration)); 
		
		TextView tv3 = (TextView) view.findViewById(R.id.train); 
		tv3.setText(cursor.getString(mIdxTrain) + " " + cursor.getString(mIdxTrainNum)); 
		
	}

	/**
	 * Specify where to get the view layout for each timetable.
	 */
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.timetables_row, parent, false); 
		return view; 
	}

}