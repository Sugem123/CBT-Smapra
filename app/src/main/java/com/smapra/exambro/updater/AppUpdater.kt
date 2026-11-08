package com.smapra.exambro.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val publishedAt: String,
)

@Singleton
class AppUpdater @Inject constructor() {

    companion object {
        private const val TAG = "AppUpdater"
        private const val GITHUB_OWNER = "Sugem123"
        private const val GITHUB_REPO = "CBT-Smapra"
        private const val RELEASES_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
    }

    suspend fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String = "",
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$RELEASES_URL?per_page=1")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            if (conn.responseCode != 200) {
                Log.w(TAG, "GitHub API returned ${conn.responseCode}")
                return@withContext null
            }

            val response = conn.inputStream.bufferedReader().readText()
            val releases = JSONArray(response)

            if (releases.length() == 0) return@withContext null

            val latest = releases.getJSONObject(0)
            val tagName = latest.getString("tag_name") // e.g. "v1.1.0"
            val body = latest.optString("body", "")
            val publishedAt = latest.optString("published_at", "")

            val cleanRemoteVersion = tagName.trimStart('v', 'V')
            val cleanCurrentVersion = currentVersionName.trimStart('v', 'V')

            val hasUpdate = if (cleanCurrentVersion.isNotBlank()) {
                compareSemVer(cleanRemoteVersion, cleanCurrentVersion) > 0
            } else {
                val remoteVersionCode = parseVersionCode(cleanRemoteVersion)
                remoteVersionCode > currentVersionCode
            }

            if (!hasUpdate) {
                Log.d(TAG, "Up to date: local=$currentVersionName remote=$cleanRemoteVersion")
                return@withContext null
            }

            // Find APK asset matched with platform architecture/SDK
            val assets = latest.getJSONArray("assets")
            val isModernDevice = android.os.Build.VERSION.SDK_INT >= 26
            val preferredTag = if (isModernDevice) "Android8+" else "Android5+"

            var apkUrl: String? = null
            var fallbackApkUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    val downloadUrl = asset.getString("browser_download_url")
                    if (fallbackApkUrl == null) fallbackApkUrl = downloadUrl

                    if (name.contains(preferredTag, ignoreCase = true)) {
                        apkUrl = downloadUrl
                        break
                    }
                }
            }

            val finalApkUrl = apkUrl ?: fallbackApkUrl

            if (finalApkUrl == null) {
                Log.w(TAG, "No APK asset found in release $tagName")
                return@withContext null
            }

            UpdateInfo(
                versionName = cleanRemoteVersion,
                versionCode = parseVersionCode(cleanRemoteVersion),
                releaseNotes = body,
                apkDownloadUrl = finalApkUrl,
                publishedAt = publishedAt,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed", e)
            null
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true

            val totalSize = conn.contentLength.toLong()
            val apkFile = File(context.cacheDir, "update.apk")

            conn.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (totalSize > 0) {
                            onProgress(downloaded.toFloat() / totalSize.toFloat())
                        }
                    }
                }
            }

            Log.d(TAG, "APK downloaded: ${apkFile.length()} bytes")
            apkFile
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    private fun compareSemVer(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    private fun parseVersionCode(versionName: String): Int {
        // "1.2.3" → 10203, "1.0.0" → 10000
        val parts = versionName.split(".").map { it.toIntOrNull() ?: 0 }
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10000 + minor * 100 + patch
    }
}
