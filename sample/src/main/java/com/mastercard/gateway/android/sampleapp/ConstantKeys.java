package com.mastercard.gateway.android.sampleapp;

/**
 * Created by askar on 6/25/16.
 */

public enum ConstantKeys {
    Amount( "Amount" ),
    Currency( "Currency" ),
    Token( "Token" ),
    secureId3D( "secureId3D" ),

    Operation( "Operation" );

    private final String value;

    ConstantKeys(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }


}