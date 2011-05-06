package com.pheide.trainose;

import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

/**
 * Class with helper functions used for display purposes.
 * 
 * @author Christine Gerpheide
 */
public class ViewHelper {
	
	 // TODO view
	/**
	 * Linkify any links appearing in the text and place it in the text view.
	 */
    public static void linkifyTextView(Context ctx, TextView textView, int stringResId) {
    	SpannableString s = new SpannableString(ctx.getText(stringResId));
    	Linkify.addLinks(s, Linkify.WEB_URLS);
    	textView.setText(s);
    	textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

}
