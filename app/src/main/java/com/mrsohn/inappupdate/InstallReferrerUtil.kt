package com.mrsohn.inappupdate

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener

class InstallReferrerUtil() {
    private val TAG = javaClass.simpleName

    fun getInstallReferrerInfo(context: Context) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        val response = referrerClient.installReferrer
                        val referrerUrl = response.installReferrer
                        val referrerClickTime = response.referrerClickTimestampSeconds
                        val appInstallTime = response.installBeginTimestampSeconds

                        // 여기서 referrerUrl을 분석하여 앱 내부 특정 화면으로 이동 가능
                        println("Install Referrer: $referrerUrl")
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE  -> {
                        // Connection couldn't be established.

                    }
                }
            }
            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }


}