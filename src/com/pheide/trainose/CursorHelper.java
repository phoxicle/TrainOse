package com.pheide.trainose;

import java.lang.reflect.Array;

import android.database.Cursor;

public class CursorHelper {

	public static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
	}
	
	public static String getString(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndex(columnName));
	}
	
	
}
