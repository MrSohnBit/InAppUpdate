package com.mrsohn.inappupdate

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil.setContentView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.updatePriority
import com.mrsohn.inappupdate.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName

//    private lateinit var appBarConfiguration: AppBarConfiguration
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
        }
    }

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Update flow result code: OK")
                showDialog("업데이트완료", "업데이트가 완료되었습니다.", {_,_ ->
                    restartApp()
                }, null)
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

    fun restartApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            val packageManager = packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            val componentName = intent!!.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            startActivity(mainIntent)
            exitProcess(0)
        }, 300)
    }

    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo


        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // This example applies an immediate update. To apply a flexible update
                // instead, pass in AppUpdateType.FLEXIBLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Request the update.

                var result = "업데이트 가능\n"
                "appUpdateInfo\n"
                result += "updateAvailability() : ${appUpdateInfo.updateAvailability()}\n"
                result += "isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}\n"
                result += "isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)}\n"
                result += "installStatus() : ${appUpdateInfo.installStatus()}\n"
                result += "updatePriority : ${appUpdateInfo.updatePriority}\n"
                result += "availableVersionCode() : ${appUpdateInfo.availableVersionCode()}\n"
                result += "clientVersionStalenessDays() : ${appUpdateInfo.clientVersionStalenessDays()}\n"

                setResultTxt(true, result)

                showDialog("업데이트",
                    "업데이트 하시겠습니까?",
                    { _, _ ->
                    startUpdate(appUpdateInfo)
                    },
                    { dialog, _ ->})

            } else {
                Toast.makeText(this@MainActivity, "업데이트 불 가능(addOnSuccessListener)", Toast.LENGTH_SHORT).show()
                var result = "업데이트 불 가능\n"
                "appUpdateInfo\n"
                result += "updateAvailability() : ${appUpdateInfo.updateAvailability()}\n"
                result += "isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)}\n"
                result += "isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) : ${appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)}\n"
                result += "installStatus() : ${appUpdateInfo.installStatus()}\n"
                result += "updatePriority : ${appUpdateInfo.updatePriority}\n"
                result += "availableVersionCode() : ${appUpdateInfo.availableVersionCode()}\n"
                result += "clientVersionStalenessDays() : ${appUpdateInfo.clientVersionStalenessDays()}\n"

                setResultTxt(true, result)
            }
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

            Toast.makeText(this@MainActivity, "업데이트 가능여부 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setResultTxt(isSucceed: Boolean, resultText: String) {
        val sb = StringBuilder()
        sb.append("Update flow result: ${if (isSucceed) "SUCCESS" else "FAILED"}")
        sb.append("\n")
        sb.append(resultText)
        binding.updateText.text = sb.toString()
    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            // Pass the intent that is returned by 'getAppUpdateInfo()'.
            appUpdateInfo,
            // an activity result launcher registered via registerForActivityResult
            activityResultLauncher,
            // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
            // flexible updates.
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())


        // Create a listener to track request state updates.
        val listener = InstallStateUpdatedListener { state ->
            // (Optional) Provide a download progress bar.
            if (state.installStatus() == InstallStatus.DOWNLOADING) {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                // Show update progress bar.
            }
            // Log state or install the update.
        }

        // Before starting an update, register a listener for updates.
        appUpdateManager.registerListener(listener)
        // Start an update.
        // When status updates are no longer needed, unregister the listener.
        appUpdateManager.unregisterListener(listener)
    }


//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        return when (item.itemId) {
//            R.id.action_settings -> true
//            else -> super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}