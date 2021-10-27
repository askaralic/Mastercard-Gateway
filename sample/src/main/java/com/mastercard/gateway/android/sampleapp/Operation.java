package com.mastercard.gateway.android.sampleapp;

/**
 * Created by askar on 6/25/16.
 */
public enum Operation {
    Ignore(0),
    Insert(1),
    Update(2),
    UpdateAll(3);

    private final int value;

    Operation(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public enum PaymentProcess {
        PaymentByCard( 1 ),
        PaymentByToken( 2 );
        private final int value;

        PaymentProcess(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

}