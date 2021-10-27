package com.mastercard.gateway.android.sampleapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mastercard.gateway.android.sdk.Gateway;

public class Utils {

    public static String getPreferenceStringValue(ConstantKeys key,Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key.getValue(), "");
    }

    public static void setPreferenceStringValue(Context context,ConstantKeys key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key.getValue(), value);
        editor.apply();
    }

    public static Gateway.Region getEnumNameForValue(String value){
        for(Gateway.Region eachValue : Gateway.Region.values()) {
            if (eachValue.toString().equalsIgnoreCase(value)) {
                return eachValue;
            }
        }
        return null;
    }
}
