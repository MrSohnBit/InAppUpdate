package com.mrsohn.inappupdate

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.google.android.gms.common.wrappers.Wrappers.packageManager
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.mrsohn.inappupdate.databinding.ActivityMainBinding

class InAppUpdateUtil(val activity: Activity,
                      val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>) {
    private val TAG = javaClass.simpleName

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(activity)
    }


    fun startUpdate(appUpdateInfo: AppUpdateInfo, appUpdateType: Int) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(appUpdateType)
                .build())

    }

    private fun checkForUpdates(isForceUpdate: Boolean = false) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateManager.registerListener(inAppUpdateListener)

//        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
//            var result =""
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
//                // Request the update.
//                binding.updateBtnLayout.visibility = View.VISIBLE
//
//                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
//                    setResultTxt(true, result)
//                    binding.flexUpdateBtn.setOnClickListener {
//                        showUpdateAlert(appUpdateInfo, AppUpdateType.FLEXIBLE)
//                    }
//                }
//
//            } else {
//                binding.updateBtnLayout.visibility = View.VISIBLE
//            }
//
//            binding.flexUpdateBtn.visibility = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) View.VISIBLE else View.GONE
//            binding.forceUpdateBtn.setOnClickListener {
//                showUpdateAlert(appUpdateInfo, AppUpdateType.IMMEDIATE)
//            }
//
//            binding.flexUpdateBtn.setOnClickListener {
//                showUpdateAlert(appUpdateInfo, AppUpdateType.FLEXIBLE)
//            }
//
//            setAppUpdateInfoLog(appUpdateInfo)
//
//            if (isForceUpdate)
//                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
//        }
//
//        appUpdateInfoTask.addOnFailureListener { exception ->
//            Log.e(TAG, "Failed to get app update info", exception)
//            // Exception 객체를 통해 실패 원인을 확인
//            val errorMessage = exception.message
//            val cause = exception.cause
//            Log.e(TAG, "Error message: $errorMessage")
//            Log.e(TAG, "Cause: $cause")
//
//            var result = "Error message: $errorMessage"
//            result += "\n"
//            result += "Cause: $cause"
//            setResultTxt(false, result)
//        }
    }


    val inAppUpdateListener = InstallStateUpdatedListener { state ->
        // (Optional) Provide a download progress bar.

        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // 업데이트가 다운로드되면 앱을 재시작합니다.
            appUpdateManager.completeUpdate()
        } else if (state.installStatus() == InstallStatus.PENDING) {
            Log.i(TAG, "InappUpdate Update: InstallStatus.PENDING")
        } else if (state.installStatus() == InstallStatus.DOWNLOADING) {
            val bytesDownloaded = state.bytesDownloaded()
            val totalBytesToDownload = state.totalBytesToDownload()
            Log.i(TAG, "InAppUpdate InstallStatus.DOWNLOADING: ${bytesDownloaded} / ${totalBytesToDownload}")
            var progress = 0
            if (bytesDownloaded != 0L)
                progress = ((bytesDownloaded/ totalBytesToDownload) * 100).toInt()

//            binding.progressBar.setProgress(progress, true)
        } else if (state.installStatus() == InstallStatus.INSTALLING) {
            Log.i(TAG, "InappUpdate Update: InstallStatus.INSTALLING")
        } else if (state.installStatus() == InstallStatus.INSTALLED) {
            Log.i(TAG, "InappUpdate Update: InstallStatus.INSTALLED")
            // 앱을 재시작해줍니다.
        } else if (state.installStatus() == InstallStatus.FAILED) {
            Log.i(TAG, "InappUpdate Update: InstallStatus.FAILED")
        } else if (state.installStatus() == InstallStatus.CANCELED) {
            Log.i(TAG, "InappUpdate Update: InstallStatus.CANCELED")
        } else {
            Log.i(TAG, "InappUpdate Update: Else(${state.installStatus()})")
        }
    }


}