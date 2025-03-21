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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.mrsohn.inappupdate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName

    private lateinit var binding: ActivityMainBinding
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode
                            }
            val versionName = packageInfo.versionName

            binding.versionCode = "$versionCode"
            binding.versionName = versionName

            Log.d("MainActivity", "Version Code : $versionCode")
            Log.d("MainActivity", "Version Name : $versionName")
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.updateBtn.setOnClickListener {
            binding.updateText.text = ""
            checkForUpdates()

            binding.updateBtnLayout.visibility = View.GONE
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
        Log.d(TAG, "inAppUpdate activityResultLauncher Update flow result code: OK")

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Update flow result code: OK")
//                restartApp()
            }
            Activity.RESULT_CANCELED -> {
                Log.d(TAG, "Update flow result code: CANCELED")
                showDialog("알림", "업데이트가 취소되었습니다..", { dialog, _ ->
                    dialog.dismiss()
                }, null)
            }
            else -> {
                Log.e(TAG, "Update flow failed: result code = ${result.resultCode}")
                showDialog("알림", "업데이트가 실패 했습니다..", { dialog, _ ->
                    dialog.dismiss()
                }, null)
            }
        }
    }


    fun restartApp() {
        Handler(Looper.getMainLooper()).postDelayed({
//            val packageManager = packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.mrsohn.inappupdate")
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
//            exitProcess(0)
        }, 300)
    }

    private fun checkForUpdates(isForceUpdate: Boolean = false) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            var result =""
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                binding.updateBtnLayout.visibility = View.VISIBLE

                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    setResultTxt(true, result)
                    binding.flexUpdateBtn.setOnClickListener {
                        showUpdateAlert(appUpdateInfo, AppUpdateType.FLEXIBLE)
                    }
                }

            } else {
                binding.updateBtnLayout.visibility = View.VISIBLE
            }

            binding.flexUpdateBtn.visibility = if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) View.VISIBLE else View.GONE
            binding.forceUpdateBtn.setOnClickListener {
                showUpdateAlert(appUpdateInfo, AppUpdateType.IMMEDIATE)
            }

            binding.flexUpdateBtn.setOnClickListener {
                showUpdateAlert(appUpdateInfo, AppUpdateType.FLEXIBLE)
            }

            setAppUpdateInfoLog(appUpdateInfo)

            if (isForceUpdate)
                startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
        }

        appUpdateInfoTask.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get app update info", exception)
            // Exception 객체를 통해 실패 원인을 확인
            val errorMessage = exception.message
            val cause = exception.cause
            Log.e(TAG, "Error message: $errorMessage")
            Log.e(TAG, "Cause: $cause")

            var result = "Error message: $errorMessage"
            result += "\n"
            result += "Cause: $cause"
            setResultTxt(false, result)
        }
    }

    fun setAppUpdateInfoLog(appUpdateInfo: AppUpdateInfo) {
        var result = ""
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode : Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    packageInfo.longVersionCode
                                } else {
                                    packageInfo.versionCode.toLong()
                                }

        if (versionCode >= appUpdateInfo.availableVersionCode()) {
            result = "최신버전 입니다.\n\n"
        }

        result += "구글플레이 등록 버전코드 : ${appUpdateInfo.availableVersionCode()}\n\n"
        result += "강제업데이트 가능 : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}\n"
        result += "유연 업데이트 가능 : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)}\n"
        result += "updateAvailability() : ${appUpdateInfo.updateAvailability()}\n"

        setResultTxt(true, result)
    }

    private fun showUpdateAlert(appUpdateInfo: AppUpdateInfo, appUpdateType: Int) {
        showDialog("${if (appUpdateType == AppUpdateType.IMMEDIATE) "강제" else "선택"} 업데이트",
            "업데이트 하시겠습니까?",
            { _, _ ->
                startUpdate(appUpdateInfo, appUpdateType)

                binding.updateBtnLayout.visibility = View.GONE
            },
            { dialog, _ ->})
    }

    private fun setResultTxt(isSucceed: Boolean, resultText: String) {
        val sb = StringBuilder()
//        sb.append("Update flow result: ${if (isSucceed) "SUCCESS" else "FAILED"}")
        if (!isSucceed)
            sb.append("업데이트 정보 조회 실패\n\n")

        sb.append(resultText)
        binding.updateText.text = sb.toString()
    }


    private fun startUpdate(appUpdateInfo: AppUpdateInfo, appUpdateType: Int) {
        appUpdateManager.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // an activity result launcher registered via registerForActivityResult
            activityResultLauncher,
            // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
            // flexible updates.
            AppUpdateOptions.newBuilder(appUpdateType)
//            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                .build())

        setAppUpdateListener(appUpdateInfo, appUpdateType)
    }

    fun setAppUpdateListener(appUpdateInfo: AppUpdateInfo, appUpdateType: Int) {
        // Create a listener to track request state updates.
        val inAppUpdateListener = InstallStateUpdatedListener { state ->
            // (Optional) Provide a download progress bar.

            if (state.installStatus() == InstallStatus.DOWNLOADED) {
//                popupSnackbarForCompleteUpdate()
                Log.i(TAG, "InappUpdate Update: InstallStatus.DOWNLOADED, FLEXIBLE=${appUpdateType == AppUpdateType.FLEXIBLE}")
                if (appUpdateType == AppUpdateType.FLEXIBLE) {
                    showDialog("다운로드 완료",
                        "다운로드가 완료되었습니다. 설치 하시겠습니까?",
                        { _, _ ->
//                            startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                            checkForUpdates(true)
                        },
                        { _, _ ->

                        })

                }
                checkForUpdates(false)
            } else if (state.installStatus() == InstallStatus.PENDING) {
                Log.i(TAG, "InappUpdate Update: InstallStatus.PENDING")
            } else if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                Log.i(TAG, "InAppUpdate InstallStatus.DOWNLOADING: ${bytesDownloaded} / ${totalBytesToDownload}")
                var progress = 0
                if (bytesDownloaded != 0L)
                    progress = ((bytesDownloaded/ totalBytesToDownload) * 100).toInt()

                binding.progressBar.setProgress(progress, true)
            } else if (state.installStatus() == InstallStatus.INSTALLING) {
                Log.i(TAG, "InappUpdate Update: InstallStatus.INSTALLING")
            } else if (state.installStatus() == InstallStatus.INSTALLED) {
                Log.i(TAG, "InappUpdate Update: InstallStatus.INSTALLED")
                // 앱을 재시작해줍니다.
                restartApp()
            } else if (state.installStatus() == InstallStatus.FAILED) {
                Log.i(TAG, "InappUpdate Update: InstallStatus.FAILED")
            } else if (state.installStatus() == InstallStatus.CANCELED) {
                Log.i(TAG, "InappUpdate Update: InstallStatus.CANCELED")
            } else {
                Log.i(TAG, "InappUpdate Update: Else(${state.installStatus()})")
            }
        }

        appUpdateManager.registerListener(inAppUpdateListener)
    }


    fun showDialog(title: String, message: String, positiveClickListener: DialogInterface.OnClickListener?, negativeClickListener: DialogInterface.OnClickListener?) {
        AlertDialog.Builder(this@MainActivity).apply {
            setTitle(title)
            setMessage(message)
            positiveClickListener?.let {
                setPositiveButton("확인", positiveClickListener)
            }

            negativeClickListener?.let {
                setNegativeButton("취소") { dialog, _ ->
                    it
                    dialog.dismiss()
                }
                show()
            }
        }

    }
}