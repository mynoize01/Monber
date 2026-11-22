package com.example.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long
)

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data object Checking : AppUpdateState()
    data class UpToDate(val currentVersion: String) : AppUpdateState()
    data class UpdateAvailable(val release: GitHubReleaseInfo) : AppUpdateState()
    data class Downloading(
        val release: GitHubReleaseInfo,
        val progressPercent: Int,
        val downloadedMb: Float,
        val totalMb: Float
    ) : AppUpdateState()
    data class ReadyToInstall(
        val release: GitHubReleaseInfo,
        val apkFile: File
    ) : AppUpdateState()
    data class NeedsInstallPermission(
        val release: GitHubReleaseInfo,
        val apkFile: File
    ) : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

object AppUpdateManager {
    private const val CHANNEL_ID = "monber_app_updates"
    private const val NOTIFICATION_ID = 9001

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
    }

    /**
     * Converts inputs such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo/releases"
     * - "https://api.github.com/repos/owner/repo/releases/latest"
     * into a normalized GitHub Releases API endpoint.
     */
    fun normalizeRepoToApiUrl(rawInput: String): String? {
        val trimmed = rawInput.trim().trimEnd('/')
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("https://api.github.com/repos/")) {
            return if (trimmed.endsWith("/releases/latest")) trimmed else "$trimmed/releases/latest"
        }

        val githubRegex = Regex("""(?:https?://)?(?:www\.)?github\.com/([^/\s]+)/([^/\s]+)(?:/.*)?""", RegexOption.IGNORE_CASE)
        val match = githubRegex.find(trimmed)
        if (match != null) {
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")
            return "https://api.github.com/repos/$owner/$repo/releases/latest"
        }

        val slugRegex = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
        val slugMatch = slugRegex.find(trimmed)
        if (slugMatch != null) {
            val owner = slugMatch.groupValues[1]
            val repo = slugMatch.groupValues[2].removeSuffix(".git")
            return "https://api.github.com/repos/$owner/$repo/releases/latest"
        }

        return null
    }

    fun normalizeVersionNumber(raw: String): String {
        val match = Regex("""\d+(?:\.\d+)*""").find(raw.trim())
        return match?.value ?: raw.trim().removePrefix("v").removePrefix("V")
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = normalizeVersionNumber(remoteTag).split('.').map { it.toIntOrNull() ?: 0 }
        val localParts = normalizeVersionNumber(currentVersion).split('.').map { it.toIntOrNull() ?: 0 }
        if (remoteParts.isEmpty() || (remoteParts.size == 1 && remoteParts[0] == 0 && !remoteTag.contains("0"))) {
            return false
        }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    suspend fun fetchLatestRelease(repoInput: String): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = normalizeRepoToApiUrl(repoInput)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Format repositori GitHub tidak valid. Gunakan format owner/repo atau URL GitHub.")
                )

            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MonBer-Android-Updater")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val msg = when (response.code) {
                        404 -> "Rilis GitHub tidak ditemukan. Pastikan repositori publik dan sudah memiliki Release."
                        403 -> "Batas akses GitHub API tercapai. Coba beberapa saat lagi."
                        else -> "Gagal memeriksa pembaruan (HTTP ${response.code})."
                    }
                    return@withContext Result.failure(IllegalStateException(msg))
                }

                val json = JSONObject(bodyStr)
                val tagName = json.optString("tag_name", "").trim()
                val releaseName = json.optString("name", tagName).ifBlank { tagName }
                val releaseNotes = json.optString("body", "").trim()
                val htmlUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")

                var apkUrl: String? = null
                var apkName: String? = null
                var apkSize = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "").ifBlank { null }
                            apkName = name
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val info = GitHubReleaseInfo(
                    tagName = tagName,
                    versionName = normalizeVersionNumber(tagName),
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    htmlUrl = htmlUrl,
                    apkDownloadUrl = apkUrl,
                    apkFileName = apkName,
                    apkSizeBytes = apkSize
                )
                Result.success(info)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: GitHubReleaseInfo,
        onProgress: (percent: Int, downloadedMb: Float, totalMb: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = release.apkDownloadUrl
                ?: return@withContext Result.failure(
                    IllegalStateException("File APK tidak ditemukan pada rilis GitHub ${release.tagName}.")
                )

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val safeFileName = (release.apkFileName ?: "monber-${release.versionName}.apk")
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val targetFile = File(updatesDir, safeFileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "MonBer-Android-Updater")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Gagal mengunduh APK (HTTP ${response.code}).")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Respons unduhan kosong."))

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSizeBytes
                val totalMb = totalBytes / (1024f * 1024f)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloadedBytes = 0L
                        var lastReportedPercent = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent, downloadedBytes / (1024f * 1024f), totalMb)
                            }
                        }
                        output.flush()
                    }
                }
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        return try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showUpdateNotification(context: Context, release: GitHubReleaseInfo) {
        if (!hasNotificationPermission(context)) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pembaruan Aplikasi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat versi terbaru aplikasi tersedia di GitHub"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_update_dialog", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pembaruan MonBer Tersedia (${release.tagName})")
            .setContentText("Versi baru ${release.versionName} siap diunduh dan diinstal.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Versi terbaru ${release.tagName} tersedia di GitHub. Ketuk untuk mengunduh dan menginstal pembaruan."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
