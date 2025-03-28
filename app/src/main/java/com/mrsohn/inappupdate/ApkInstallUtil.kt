package com.mrsohn.inappupdate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ApkInstallUtil {


    fun copyOrOpenApkFile(context: Context, assetFileName: String) {
        val apkFile = File(context.getExternalFilesDir(null), assetFileName)
        if (!apkFile.exists()) {
            copyAssetToExternalFilesDir(context, assetFileName)
        }

        installApk(context, apkFile)
    }

    // 🔹 assets에서 외부 저장소의 앱 전용 폴더로 복사 (Android 14 대응)
    fun copyAssetToExternalFilesDir(context: Context, assetFileName: String) {
        val assetManager = context.assets
        val inputStream: InputStream = assetManager.open(assetFileName)

        val apkFile = File(context.getExternalFilesDir(null), assetFileName)
        val outputStream = FileOutputStream(apkFile)

        inputStream.copyTo(outputStream)
        outputStream.close()
        inputStream.close()
    }

    // 🔹 APK 설치 실행 (Android 14 지원)
    fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            if (!canInstall) {
                // ✅ "출처를 알 수 없는 앱 설치" 권한 요청
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}