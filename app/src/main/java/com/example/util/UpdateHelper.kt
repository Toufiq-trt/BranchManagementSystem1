package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class AppUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String
)

object UpdateHelper {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val updateAdapter = moshi.adapter(AppUpdate::class.java)

    /**
     * Checks for updates by downloading version.json from the specified repository.
     */
    suspend fun checkForUpdates(owner: String, repo: String, branch: String): AppUpdate? {
        return withContext(Dispatchers.IO) {
            val url = "https://raw.githubusercontent.com/$owner/$repo/$branch/version.json"
            val request = Request.Builder()
                .url(url)
                .header("Cache-Control", "no-cache")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bodyString = response.body?.string() ?: return@withContext null
                    return@withContext updateAdapter.fromJson(bodyString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    /**
     * Downloads the APK file showing real-time progress.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File {
        return withContext(Dispatchers.IO) {
            val destinationFile = File(context.cacheDir, "smartbanking-update.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error code: ${response.code} (${response.message})")
                }
                val body = response.body ?: throw IOException("Empty response body from server")
                val contentLength = body.contentLength()
                
                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            val progress = if (contentLength > 0) totalBytesRead.toFloat() / contentLength else 0.0f
                            onProgress(progress, totalBytesRead, contentLength)
                        }
                    }
                }
                return@withContext destinationFile
            }
        }
    }

    /**
     * Launches Android Package Installer to perform an in-place update.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        try {
            // Verify and request Package Installer Permission on Oreo+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return false
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
