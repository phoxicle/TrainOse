package com.pheide.trainose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

/**
 * Special array adapter to be used with auto complete views
 * that work with special characters (such as accented characters).
 * 
 * Using this adapter allows one to type in unaccented characters and
 * still have the autocomplete return accented strings.
 *
 */
public class FlatCharArrayAdapter extends ArrayAdapter<String> implements Filterable{

	HashMap<String,String> mStringMap;
	
	public FlatCharArrayAdapter(Context context, int textViewResourceId,
			String[] strings) {
		
		super(context, textViewResourceId);
		
		mStringMap = new HashMap<String,String>();
		
		for(int i=0; i<strings.length; i++){

			// Store a "flattened" version of each city name without special chars
			// Note that this requires that no two unflattened strings 
			// map to the same flattened string.
			
			mStringMap.put(FlatCharArrayAdapter.flatten(strings[i]), strings[i]);
		}
	}
	
	private static String flatten(String original){
		
		String flat = original.toLowerCase();
		
		// Characters and their flattened versions
		
		return flat
			.replace('ά', 'α')
			.replace('έ', 'ε')
			.replace('ί', 'ι')
			.replace('ό', 'ο')
			.replace('ώ', 'ω')
			.replace('ύ', 'υ')
			;
	}
	
	@Override
    public Filter getFilter() {
		
		return new Filter() {

			@Override
		    protected FilterResults performFiltering(CharSequence constraint) {
		    	FilterResults results = new FilterResults();
		    	
		    	if (constraint != null) {
		    		
		    		// First flatten the search constraint
		    		
		    		String needle = FlatCharArrayAdapter.flatten(constraint.toString());
		    		
		    		ArrayList<String> matches = new ArrayList<String>();
		    		
		    		// Add any matches to a list
		    		
		    		for (Map.Entry<String, String> entry : mStringMap.entrySet()) {
		    		
		    			if (entry.getKey().startsWith(needle)) {

		    				matches.add(entry.getValue());
		                }
		    		}
		    		
		    		results.values = matches;
		            results.count = matches.size();
		        }
		    	
		        return results;
		    }
	
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				
				if(results != null && results.count > 0) {
	
					clear();
					for (String s : (ArrayList<String>) results.values)
						add(s);
					
					notifyDataSetChanged();
				}
				else {
					notifyDataSetInvalidated();
				}
			}
		};
	}
}

