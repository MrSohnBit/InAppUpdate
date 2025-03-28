package com.mrsohn.inappupdate

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class ApkInstallUtil {


    fun copyOrOpenApkFile(context: Context, assetFileName: String) {
        val apkFile = File(context.getExternalFilesDir(null), assetFileName)
        if (!apkFile.exists()) {
            copyAssetToExternalFilesDir(context, assetFileName)
        }

        copyAssetToDownloadFolder(context, assetFileName)

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



    fun copyAssetToDownloadFolder(context: Context, assetFileName: String) {
        val assetManager = context.assets
        val inputStream: InputStream = assetManager.open(assetFileName)

        val outputFile: File

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // ✅ Android 10(Q) 이상: MediaStore API 사용
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, assetFileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                inputStream.copyTo(outputStream!!)
                outputStream.close()
            }
        } else {
            // ✅ Android 9(P) 이하: 직접 파일 저장
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            outputFile = File(downloadsDir, assetFileName)

            val outputStream: OutputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            outputStream.close()
        }

        inputStream.close()
    }

}