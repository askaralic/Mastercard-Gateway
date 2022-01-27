package com.mastercard.gateway.android.sampleapp;


import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import com.mastercard.gateway.android.sampleapp.databinding.ActivityMainBinding;
import com.mastercard.gateway.android.sdk.Gateway;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    TextChangeListener textChangeListener = new TextChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.merchantId.setText(getString(R.string.merchant_id));
        binding.merchantId.addTextChangedListener(textChangeListener);
        Config.region = Gateway.Region.ASIA_PACIFIC;
        binding.region.setText(Config.region.name());
        binding.region.addTextChangedListener(textChangeListener);
        binding.region.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.region.clearFocus();
                showRegionPicker();
            }
        });

        binding.merchantUrl.setText(getString(R.string.merchant_url));
        binding.merchantUrl.addTextChangedListener(textChangeListener);

        binding.processPaymentButton.setOnClickListener(v -> goTo(Operation.PaymentProcess.PaymentByCard.getValue()));
        binding.processLastTokenButton.setOnClickListener(view -> goTo(Operation.PaymentProcess.PaymentByToken.getValue()));
        enableButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindViewWithValues();
    }

    private void bindViewWithValues() {
        binding.edtLastTransactionToken.setText(Utils.getPreferenceStringValue(ConstantKeys.Token,this));
    }

    void goTo(int operation) {
        Intent intent = new Intent(this, ProcessPaymentActivity.class);
        intent.putExtra(ConstantKeys.Amount.getValue(), "2.00");
        intent.putExtra(ConstantKeys.Currency.getValue(), "AED");
        intent.putExtra(ConstantKeys.Token.getValue(),  Objects.requireNonNull(binding.edtLastTransactionToken.getText()).toString());
        intent.putExtra(ConstantKeys.secureId3D.getValue(),  Utils.getPreferenceStringValue(ConstantKeys.secureId3D,this));
        intent.putExtra(ConstantKeys.Operation.getValue(), operation);
        startActivity(intent);
    }


    void enableButtons() {
        boolean enabled = !TextUtils.isEmpty(binding.merchantId.getText())
                && !TextUtils.isEmpty(binding.region.getText())
                && !TextUtils.isEmpty(binding.merchantUrl.getText());

        binding.processPaymentButton.setEnabled(enabled);
    }

    void showRegionPicker() {
        Gateway.Region[] regions = Gateway.Region.values();
        final String[] items = new String[regions.length + 1];
        items[0] = getString(R.string.none);
        for (int i = 0; i < regions.length; i++) {
            items[i + 1] = regions[i].name();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.main_select_region)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        binding.region.setText("");
                    } else {
                        binding.region.setText(items[which]);
                    }
                    dialog.cancel();
                })
                .show();
    }

    class TextChangeListener implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            enableButtons();
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }
}
