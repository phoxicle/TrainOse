package com.pheide.trainose;

import android.database.Cursor;

/**
 * Helper functions when using cursors.
 * 
 * @author Christine Gerpheide
 */
public class CursorHelper {

	/**
	 * Convenience method to return the specified column 
	 * in a Cursor as a long.
	 * 
	 * @param Cursor the managed cursor
	 * @param columnName the column to retrieve
	 * 
	 * @return the column as a long
	 */
	public static long getLong(Cursor cursor, String columnName) {
		return cursor.getLong(cursor.getColumnIndex(columnName));
	}
	
	/**
	 * Convenience method to return the specified column 
	 * in a Cursor as a String.
	 * 
	 * @param Cursor the managed cursor
	 * @param columnName the column to retrieve
	 * 
	 * @return the column as a String
	 */
	public static String getString(Cursor cursor, String columnName) {
		return cursor.getString(cursor.getColumnIndex(columnName));
	}
	
	
}
