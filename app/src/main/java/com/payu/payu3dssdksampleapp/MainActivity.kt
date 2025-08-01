package com.payu.payu3dssdksampleapp

import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText
import com.payu.paymentparamhelper.PaymentParams
import com.payu.paymentparamhelper.PayuConstants.POST_SALT
import com.payu.threeDS2.PayU3DS2
import com.payu.threeDS2.config.ACSContentConfig
import com.payu.threeDS2.config.PayU3DS2Config
import com.payu.threedsbase.constants.APIConstants
import com.payu.threedsbase.data.PayU3DS2DeviceWarning
import com.payu.threedsbase.interfaces.listeners.PayU3DS2Callback
import com.payu.threedsbase.interfaces.listeners.PayUHashGeneratedListener
import com.payu.threedsui.interfaces.listeners.PayU3DS2PaymentCallback
import com.payu.threedsui.uiCustomisation.enums.FontName
import kotlinx.android.synthetic.main.activity_main.et_card_token
import kotlinx.android.synthetic.main.activity_main.et_key
import kotlinx.android.synthetic.main.activity_main.et_salt
import kotlinx.android.synthetic.main.activity_main.et_txn_amount
import kotlinx.android.synthetic.main.activity_main.et_txn_id
import kotlinx.android.synthetic.main.activity_main.layout_bs_customisation
import kotlinx.android.synthetic.main.activity_main.layout_button_customisation
import kotlinx.android.synthetic.main.activity_main.layout_custom_ui
import kotlinx.android.synthetic.main.activity_main.layout_label_customisation
import kotlinx.android.synthetic.main.activity_main.layout_textBox_customisation
import kotlinx.android.synthetic.main.activity_main.layout_toolbar_customisation
import kotlinx.android.synthetic.main.activity_main.scAuthOnly
import kotlinx.android.synthetic.main.activity_main.scAutoRead
import kotlinx.android.synthetic.main.activity_main.scAutoSubmit
import kotlinx.android.synthetic.main.activity_main.scHashTimeout
import kotlinx.android.synthetic.main.activity_main.scSaveCard
import kotlinx.android.synthetic.main.activity_main.switch_acs_on_off
import kotlinx.android.synthetic.main.activity_main.switch_bs_on_off
import kotlinx.android.synthetic.main.activity_main.switch_btn_on_off
import kotlinx.android.synthetic.main.activity_main.switch_label_on_off
import kotlinx.android.synthetic.main.activity_main.switch_textBox_on_off
import kotlinx.android.synthetic.main.activity_main.switch_toolbar_on_off
import kotlinx.android.synthetic.main.layout_custom_ui.scCustomUI
import kotlinx.android.synthetic.main.layout_custom_ui.scShowTimer
import kotlinx.android.synthetic.main.layout_custom_ui.tv_amount
import kotlinx.android.synthetic.main.layout_custom_ui.tv_merchant_name
import kotlinx.android.synthetic.main.layout_custom_ui.tv_otp_content
import kotlinx.android.synthetic.main.layout_custom_ui.tv_resend_btn_title
import kotlinx.android.synthetic.main.layout_custom_ui.tv_resend_max_otp_content
import kotlinx.android.synthetic.main.layout_custom_ui.tv_resend_otp_content
import kotlinx.android.synthetic.main.layout_custom_ui.tv_submit_btn_title
import kotlinx.android.synthetic.main.layout_label_customisation.acs_label_font_name_value
import kotlinx.android.synthetic.main.layout_label_customisation.acs_label_heading_name_value
import kotlinx.android.synthetic.main.layout_textbox_customisation.acs_font_name_value
import kotlinx.android.synthetic.main.layout_toolbar_customisation.acs_toolbar_font_name_value
import java.io.IOException
import java.util.Date


class MainActivity : AppCompatActivity(), PayU3DS2PaymentCallback, PayU3DS2Callback {
    private val HASH_NAME = "hashName"
    private val HASH_STRING = "hashString"
    private val CP_LOOKUP_API_HASH = "lookup_api_hash";

    private var scFallback: SwitchCompat? = null
    private var scEnv: SwitchCompat? = null

    // UAT Testing
    private val key = ""
    private var salt = ""
    private var fontName = arrayOf(
        "Nothing", FontName.ROBOTO_MEDIUM.name, FontName.ROBOTO_REGULAR.name
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scFallback = findViewById(R.id.scFallback)
        scEnv = findViewById(R.id.scEnv)
        initCustomisationView()
        et_key.setText(key)
        et_salt.setText(salt)
        val fontNameAdapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this, android.R.layout.simple_spinner_item, fontName
        )

        fontNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        acs_font_name_value.adapter = fontNameAdapter
        acs_toolbar_font_name_value.adapter = fontNameAdapter
        acs_label_font_name_value.adapter = fontNameAdapter
        acs_label_heading_name_value.adapter = fontNameAdapter


        findViewById<AppCompatButton>(R.id.btn_makePayment).setOnClickListener {
            val config = PayU3DS2Config()
            val paymentParams = createPaymentParams()
            config.fallback3DS1 = scFallback!!.isChecked
            config.isProduction = scEnv!!.isChecked
            config.autoRead = scAutoRead.isChecked
            config.autoSubmit = scAutoSubmit.isChecked
            config.authenticateOnly = scAuthOnly.isChecked
            config.enableCustomizedOtpUIFlow = scCustomUI.isChecked
            config.enableTxnTimeoutTimer = scShowTimer.isChecked
            config.merchantName = tv_merchant_name.text.toString()
            config.amount = tv_amount.text.toString()
            config.enableMFAViaBiometric = true
            val acsContentConfig = ACSContentConfig()
            acsContentConfig.otpContent = tv_otp_content.text.toString()
            acsContentConfig.resendButtonTitle = tv_resend_btn_title.text.toString()
            acsContentConfig.submitButtonTitle = tv_submit_btn_title.text.toString()
            acsContentConfig.resendInfoContent = tv_resend_otp_content.text.toString()
            acsContentConfig.maxResendInfoContent = tv_resend_max_otp_content.text.toString()
            config.acsContentConfig = acsContentConfig
            initialisePayment(config, paymentParams)
        }

    }

    private fun initCustomisationView() {
        switch_textBox_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_textBox_customisation.visibility = View.VISIBLE
            } else {
                layout_textBox_customisation.visibility = View.GONE
            }
        }

        switch_toolbar_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_toolbar_customisation.visibility = View.VISIBLE
            } else {
                layout_toolbar_customisation.visibility = View.GONE
            }
        }
        switch_btn_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_button_customisation.visibility = View.VISIBLE
            } else {
                layout_button_customisation.visibility = View.GONE
            }
        }

        switch_label_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_label_customisation.visibility = View.VISIBLE
            } else {
                layout_label_customisation.visibility = View.GONE
            }
        }
        switch_bs_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_bs_customisation.visibility = View.VISIBLE
            } else {
                layout_bs_customisation.visibility = View.GONE
            }
        }

        switch_acs_on_off.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                layout_custom_ui.visibility = View.VISIBLE
            } else {
                layout_custom_ui.visibility = View.GONE
            }
        }
    }

    private fun initialisePayment(
        config: PayU3DS2Config, paymentParams: PaymentParams
    ) {
        PayU3DS2.initiatePayment(this, config, paymentParams, this)
    }

    private fun createPaymentParams(): PaymentParams {
        val paymentParams = PaymentParams()
        paymentParams.cardNumber =
            findViewById<TextInputEditText>(R.id.et_card_number).text.toString()
        paymentParams.key = et_key.text.toString()
        paymentParams.cvv = findViewById<TextInputEditText>(R.id.et_card_cvv).text.toString()
        paymentParams.expiryMonth =
            findViewById<TextInputEditText>(R.id.et_card_expiry_month).text.toString()
        paymentParams.expiryYear =
            findViewById<TextInputEditText>(R.id.et_card_expiry_year).text.toString()
        paymentParams.firstName = ""
        paymentParams.txnId = Date().time.toString()
        et_txn_id.setText(paymentParams.txnId)
        paymentParams.amount = et_txn_amount.text.toString()
        paymentParams.productInfo = "Product Info"
        paymentParams.email = ""
        paymentParams.udf1 = "test"
        paymentParams.udf2 = "testing"
        paymentParams.udf3 = "test1"
        paymentParams.udf4 = "test2"
        paymentParams.udf5 = "test3"
        paymentParams.surl = ""
        paymentParams.storeCard = if (scSaveCard!!.isChecked) 1 else 0
        paymentParams.userCredentials = "test:test"
        paymentParams.cardToken = et_card_token.text.toString()
        paymentParams.furl = ""
        return paymentParams
    }

    private var paymentResponse: Any? = null

    private fun processResponseAuthorizeAPI(response: Any) {
        val res = getThreeDSPaymentResponse(response)
        res?.let {
            showResponseDialog(it)
        }
    }

    private fun getThreeDSPaymentResponse(response: Any): Any? {
        if(response is HashMap<*, *>) {
            val mfaStatus = response[APIConstants.MFA_REGISTRATION_STATUS]
            if (mfaStatus == APIConstants.MFA_REGISTRATION_STATUS_IN_PROGRESS) {
                paymentResponse = response[APIConstants.PAYMENT_RESPONSE]
                return null
            } else if (mfaStatus == APIConstants.MFA_REGISTRATION_STATUS_SUCCESS) {
                return paymentResponse
            } else {
                return response[APIConstants.PAYMENT_RESPONSE]
            }
        }
        return response
    }

    private fun showResponseDialog(response: Any) {
        runOnUiThread {
            val builder =
                AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert)
            builder.setCancelable(true)
            builder.setMessage(response.toString())
            builder.setPositiveButton(
                "Ok"
            ) { _, _ -> // Do something
                builder.create().dismiss()
            }

            val dialog = builder.create()
            dialog.show()

            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.black))
        }

    }

    override fun onPaymentSuccess(successResponse: Any) {
        processResponseAuthorizeAPI(successResponse)
    }

    override fun onPaymentFailure(failureResponse: Any) {
        processResponseAuthorizeAPI(failureResponse)
    }


    override fun onPaymentCancel(isTxnInitiated: Boolean) {
        Toast.makeText(
            this@MainActivity, "Transaction cancelled by user", Toast.LENGTH_LONG
        ).show()
    }

    override fun onError(errorCode: Int, errorMessage: String) {
        Toast.makeText(this@MainActivity, "onError"+errorMessage, Toast.LENGTH_LONG).show()
    }

    override fun generateHash(
        map: HashMap<String, String>, hashGenerationListener: PayUHashGeneratedListener
    ) {
        Thread {
            if (scHashTimeout.isChecked) Thread.sleep(12000)
            if (map.containsKey(HASH_STRING) && map.containsKey(HASH_NAME)) {
                val hashData = map[HASH_STRING]
                val hashName = map[HASH_NAME]
                val postSalt = map[POST_SALT]
                val hash: String?
                var newsalt = et_salt.text.toString()

                if (!postSalt.isNullOrEmpty()) {
                    newsalt += postSalt
                }
                if (hashName.equals("pricingHash")) {
                    newsalt = "<your salt>"
                }
                if (hashName.equals(
                        CP_LOOKUP_API_HASH, ignoreCase = true
                    )
                ) {
                    hash = HashGenerationUtils.generateHashFromSDK(
                        hashData!!, newsalt, "<merchant secret key>"
                    )
                } else {
                    hash = HashGenerationUtils.generateHashFromSDK(
                        hashData!!, newsalt
                    )
                }

                if (!TextUtils.isEmpty(hash)) {
                    val hashMap: HashMap<String, String> = HashMap()
                    hashMap[hashName!!] = hash!!
                    runOnUiThread({
                        hashGenerationListener.onHashGenerated(hashMap)

                    })
                }
            }


        }.start()


    }

    override fun mfaRegistrationStatus(status: Boolean) {
        if (paymentResponse != null && paymentResponse.toString()
                .isNotEmpty()) {
            showResponseDialog(paymentResponse as Any)
        }
        val statusMsg = if (status) "Success" else "Failed"

        Toast.makeText(
            this, "Registration Status: $statusMsg", Toast.LENGTH_LONG
        ).show()
    }

    override fun onSuccess(response: Any) {
        Toast.makeText(
            this, (response as List<PayU3DS2DeviceWarning>).get(0).message, Toast.LENGTH_LONG
        ).show()
    }
}