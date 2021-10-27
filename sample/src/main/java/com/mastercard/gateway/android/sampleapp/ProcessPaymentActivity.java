package com.mastercard.gateway.android.sampleapp;

import android.app.Activity;
import android.content.Intent;
import androidx.databinding.DataBindingUtil;
import android.graphics.Paint;
import android.os.Bundle;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.mastercard.gateway.android.sampleapp.databinding.ActivityProcessPaymentBinding;
import com.mastercard.gateway.android.sdk.Gateway;
import com.mastercard.gateway.android.sdk.Gateway3DSecureCallback;
import com.mastercard.gateway.android.sdk.GatewayCallback;
import com.mastercard.gateway.android.sdk.GatewayMap;

import java.util.UUID;

public class ProcessPaymentActivity extends AppCompatActivity {

    static final int REQUEST_CARD_INFO = 100;


    ActivityProcessPaymentBinding binding;
    Gateway gateway;
    String sessionId, apiVersion, threeDSecureId, orderId, transactionId,amount,currency,token;
    int operation;
    boolean isGooglePay = false;
    ApiController apiController ;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_process_payment);

        Config config = new Config();
        config.merchantId = getString(R.string.merchant_id);
        config.merchant_url = getString(R.string.merchant_url);
        config.merchant_gateway_username = getString(R.string.merchant_gateway_username);
        config.merchant_gateway_password = getString(R.string.merchant_gateway_password);
        apiController = new ApiController(config);
        apiController.setMerchantServerUrl(getString(R.string.merchant_url));

        // init gateway
        gateway = new Gateway();
        gateway.setMerchantId(getString(R.string.merchant_id));
        try {
            Gateway.Region region = Config.region;
            gateway.setRegion(region);
        } catch (Exception e) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), "Invalid Gateway region value provided", e);
        }
        amount = getIntent().getExtras().getString(ConstantKeys.Amount.getValue());
        currency = getIntent().getExtras().getString(ConstantKeys.Currency.getValue());
        token = getIntent().getExtras().getString(ConstantKeys.Token.getValue());
        threeDSecureId = getIntent().getExtras().getString(ConstantKeys.secureId3D.getValue());
        operation = getIntent().getExtras().getInt(ConstantKeys.Operation.getValue(),0);
        // random order/txn IDs for example purposes
        orderId = UUID.randomUUID().toString();
        orderId = orderId.substring(0, orderId.indexOf('-'));
        transactionId = UUID.randomUUID().toString();
        transactionId = transactionId.substring(0, transactionId.indexOf('-'));

        if(threeDSecureId ==null || threeDSecureId.isEmpty()) {
            threeDSecureId = new RandomString().nextString();
        }
        // bind buttons
        binding.startButton.setText("Pay "+amount + " "+currency);
        binding.startButton.setOnClickListener(v -> {
            if(operation == 2){
                processPayment();
            }else{
                createSession();
            }
    });
        binding.confirmButton.setOnClickListener(v -> {
            // 3DS is not applicable to Google Pay transactions
            if (isGooglePay) {
                processPayment();
            } else {
                check3dsEnrollment();
            }
        });
        binding.doneButton.setOnClickListener(v -> finish());

        initUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle the 3DSecure lifecycle
        if (Gateway.handle3DSecureResult(requestCode, resultCode, data, new ThreeDSecureCallback())) {
            return;
        }

        if (requestCode == REQUEST_CARD_INFO) {
            binding.collectCardInfoProgress.setVisibility(View.GONE);

            if (resultCode == Activity.RESULT_OK) {
                binding.collectCardInfoSuccess.setVisibility(View.VISIBLE);

                String googlePayToken = data.getStringExtra(CollectCardInfoActivity.EXTRA_PAYMENT_TOKEN);

                String cardDescription = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_DESCRIPTION);
                binding.confirmCardDescription.setText(cardDescription);

                if (googlePayToken != null) {
                    isGooglePay = true;

                    binding.check3dsLabel.setPaintFlags(binding.check3dsLabel.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                    String paymentToken = data.getStringExtra(CollectCardInfoActivity.EXTRA_PAYMENT_TOKEN);

                    updateSession(paymentToken);
                } else {
                    isGooglePay = false;

                    String cardName = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_NAME);
                    String cardNumber = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_NUMBER);
                    String cardExpiryMonth = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_EXPIRY_MONTH);
                    String cardExpiryYear = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_EXPIRY_YEAR);
                    String cardCvv = data.getStringExtra(CollectCardInfoActivity.EXTRA_CARD_CVV);

                    updateSession(cardName, cardNumber, cardExpiryMonth, cardExpiryYear, cardCvv);
                }

            } else {
                binding.collectCardInfoError.setVisibility(View.VISIBLE);

                showResult(R.drawable.failed, R.string.pay_error_card_info_not_collected);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    void initUI() {
        binding.createSessionProgress.setVisibility(View.GONE);
        binding.createSessionSuccess.setVisibility(View.GONE);
        binding.createSessionError.setVisibility(View.GONE);

        binding.collectCardInfoProgress.setVisibility(View.GONE);
        binding.collectCardInfoSuccess.setVisibility(View.GONE);
        binding.collectCardInfoError.setVisibility(View.GONE);

        binding.updateSessionProgress.setVisibility(View.GONE);
        binding.updateSessionSuccess.setVisibility(View.GONE);
        binding.updateSessionError.setVisibility(View.GONE);

        binding.check3dsProgress.setVisibility(View.GONE);
        binding.check3dsSuccess.setVisibility(View.GONE);
        binding.check3dsError.setVisibility(View.GONE);

        binding.processPaymentProgress.setVisibility(View.GONE);
        binding.processPaymentSuccess.setVisibility(View.GONE);
        binding.processPaymentError.setVisibility(View.GONE);

        binding.startButton.setEnabled(true);
        binding.confirmButton.setEnabled(true);

        binding.startButton.setVisibility(View.VISIBLE);
        binding.groupConfirm.setVisibility(View.GONE);
        binding.groupResult.setVisibility(View.GONE);
    }

    void createSession() {
        binding.startButton.setEnabled(false);
        binding.createSessionProgress.setVisibility(View.VISIBLE);

        apiController.createSession(new CreateSessionCallback());
    }

    void collectCardInfo() {
        binding.collectCardInfoProgress.setVisibility(View.VISIBLE);

        Intent i = new Intent(this, CollectCardInfoActivity.class);
        i.putExtra(CollectCardInfoActivity.EXTRA_GOOGLE_PAY_TXN_AMOUNT, amount);
        i.putExtra(CollectCardInfoActivity.EXTRA_GOOGLE_PAY_TXN_CURRENCY, currency);

        startActivityForResult(i, REQUEST_CARD_INFO);
    }

    void updateSession(String paymentToken) {
        binding.updateSessionProgress.setVisibility(View.VISIBLE);

        GatewayMap request = new GatewayMap()
                .set("sourceOfFunds.provided.card.devicePayment.paymentToken", paymentToken);

        gateway.updateSession(sessionId, apiVersion, request, new UpdateSessionCallback());
    }

    void updateSession(String name, String number, String expiryMonth, String expiryYear, String cvv) {
        binding.updateSessionProgress.setVisibility(View.VISIBLE);

        // build the gateway request
        GatewayMap request = new GatewayMap()
                .set("sourceOfFunds.provided.card.nameOnCard", name)
                .set("sourceOfFunds.provided.card.number", number)
                .set("sourceOfFunds.provided.card.securityCode", cvv)
                .set("sourceOfFunds.provided.card.expiry.month", expiryMonth)
                .set("sourceOfFunds.provided.card.expiry.year", expiryYear);

        gateway.updateSession(sessionId, apiVersion, request, new UpdateSessionCallback());
    }

    void check3dsEnrollment() {
        binding.check3dsProgress.setVisibility(View.VISIBLE);
        binding.confirmButton.setEnabled(false);



        apiController.check3DSecureEnrollment(operation,token,sessionId, amount, currency, threeDSecureId, new Check3DSecureEnrollmentCallback());
    }

    void processPayment() {
        binding.processPaymentProgress.setVisibility(View.VISIBLE);

        apiController.completeSession(operation,token,sessionId, orderId, transactionId, amount, currency, threeDSecureId, isGooglePay, new CompleteSessionCallback());
        if(operation == 1) {
            apiController.completeToken(sessionId, new CompleteTokenCallback());
        }
    }

    void showResult(@DrawableRes int iconId, @StringRes int messageId) {
        binding.resultIcon.setImageResource(iconId);
        binding.resultText.setText(messageId);

        binding.groupConfirm.setVisibility(View.GONE);
        binding.groupResult.setVisibility(View.VISIBLE);
    }


    class CreateSessionCallback implements ApiController.CreateSessionCallback {
        @Override
        public void onSuccess(String sessionId, String apiVersion) {
            Log.i("CreateSessionTask", "Session established");
            binding.createSessionProgress.setVisibility(View.GONE);
            binding.createSessionSuccess.setVisibility(View.VISIBLE);

            ProcessPaymentActivity.this.sessionId = sessionId;
            ProcessPaymentActivity.this.apiVersion = apiVersion;

            collectCardInfo();
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            binding.createSessionProgress.setVisibility(View.GONE);
            binding.createSessionError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_unable_to_create_session);
        }
    }

    class UpdateSessionCallback implements GatewayCallback {
        @Override
        public void onSuccess(GatewayMap response) {
            Log.i(ProcessPaymentActivity.class.getSimpleName(), "Successfully updated session");
            binding.updateSessionProgress.setVisibility(View.GONE);
            binding.updateSessionSuccess.setVisibility(View.VISIBLE);

            binding.startButton.setVisibility(View.GONE);
            binding.groupConfirm.setVisibility(View.VISIBLE);
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            binding.updateSessionProgress.setVisibility(View.GONE);
            binding.updateSessionError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_unable_to_update_session);
        }
    }

    class Check3DSecureEnrollmentCallback implements ApiController.Check3DSecureEnrollmentCallback {
        @Override
        public void onSuccess(GatewayMap response) {
            int apiVersionInt = Integer.valueOf(apiVersion);

            String html = null;
            if (response.containsKey("gatewayResponse.3DSecure.authenticationRedirect.simple.htmlBodyContent")) {
                html = (String) response.get("gatewayResponse.3DSecure.authenticationRedirect.simple.htmlBodyContent");
            }

            // for API versions <= 46, you must use the summary status field to determine next steps for 3DS
            if (apiVersionInt <= 46) {
                String summaryStatus = (String) response.get("gatewayResponse.3DSecure.summaryStatus");

                if ("CARD_ENROLLED".equalsIgnoreCase(summaryStatus)) {
                    Gateway.start3DSecureActivity(ProcessPaymentActivity.this, html);
                    return;
                }

                binding.check3dsProgress.setVisibility(View.GONE);
                binding.check3dsSuccess.setVisibility(View.VISIBLE);
                ProcessPaymentActivity.this.threeDSecureId = null;

                // for these 2 cases, you still provide the 3DSecureId with the pay operation
                if ("CARD_NOT_ENROLLED".equalsIgnoreCase(summaryStatus) || "AUTHENTICATION_NOT_AVAILABLE".equalsIgnoreCase(summaryStatus)) {
                    ProcessPaymentActivity.this.threeDSecureId = threeDSecureId;
                }

                processPayment();
            }

            // for API versions >= 47, you must look to the gateway recommendation and the presence of 3DS info in the payload
            else {
                String gatewayRecommendation = (String) response.get("gatewayResponse.response.gatewayRecommendation");

                // if DO_NOT_PROCEED returned in recommendation, should stop transaction
                if ("DO_NOT_PROCEED".equalsIgnoreCase(gatewayRecommendation)) {
                    binding.check3dsProgress.setVisibility(View.GONE);
                    binding.check3dsError.setVisibility(View.VISIBLE);

                    showResult(R.drawable.failed, R.string.pay_error_3ds_authentication_failed);
                    return;
                }

                // if PROCEED in recommendation, and we have HTML for 3ds, perform 3DS
                if (html != null) {
                    Gateway.start3DSecureActivity(ProcessPaymentActivity.this, html);
                    return;
                }

                ProcessPaymentActivity.this.threeDSecureId = threeDSecureId;

                processPayment();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            binding.check3dsProgress.setVisibility(View.GONE);
            binding.check3dsError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_3ds_authentication_failed);
        }
    }

    class ThreeDSecureCallback implements Gateway3DSecureCallback {
        @Override
        public void on3DSecureCancel() {
            showError();
        }

        @Override
        public void on3DSecureComplete(GatewayMap result) {
            int apiVersionInt = Integer.valueOf(apiVersion);

            if (apiVersionInt <= 46) {
                if ("AUTHENTICATION_FAILED".equalsIgnoreCase((String) result.get("3DSecure.summaryStatus"))) {
                    showError();
                    return;
                }
            } else { // version >= 47
                if ("DO_NOT_PROCEED".equalsIgnoreCase((String) result.get("response.gatewayRecommendation"))) {
                    showError();
                    return;
                }
            }

            binding.check3dsProgress.setVisibility(View.GONE);
            binding.check3dsSuccess.setVisibility(View.VISIBLE);

            ProcessPaymentActivity.this.threeDSecureId = threeDSecureId;

            processPayment();
        }

        void showError() {
            binding.check3dsProgress.setVisibility(View.GONE);
            binding.check3dsError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_3ds_authentication_failed);
        }
    }

    class CompleteSessionCallback implements ApiController.CompleteSessionCallback {
        @Override
        public void onSuccess(GatewayResponse result) {
            binding.processPaymentProgress.setVisibility(View.GONE);
            binding.processPaymentSuccess.setVisibility(View.VISIBLE);
            showResult(R.drawable.success, R.string.pay_you_payment_was_successful);
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            binding.processPaymentProgress.setVisibility(View.GONE);
            binding.processPaymentError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_processing_your_payment);
        }
    }
    class CompleteTokenCallback implements ApiController.CompleteTokenCallback {
        @Override
        public void onSuccess(GatewayResponse result) {
            binding.processPaymentProgress.setVisibility(View.GONE);
            binding.processCreateTokenSuccess.setVisibility(View.VISIBLE);
            Utils.setPreferenceStringValue(ProcessPaymentActivity.this,ConstantKeys.Token,result.token);

            Utils.setPreferenceStringValue(ProcessPaymentActivity.this,ConstantKeys.secureId3D,threeDSecureId);
            showResult(R.drawable.success, R.string.pay_you_payment_was_successful);
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e(ProcessPaymentActivity.class.getSimpleName(), throwable.getMessage(), throwable);

            binding.processPaymentProgress.setVisibility(View.GONE);
            binding.processCreateTokenError.setVisibility(View.VISIBLE);

            showResult(R.drawable.failed, R.string.pay_error_processing_your_token);
        }
    }
}
