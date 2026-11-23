package com.example.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
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
    val apkSizeBytes: Long,
    val versionCode: Int = 0
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

    fun getCurrentVersionCode(context: Context): Long {
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
            PackageInfoCompat.getLongVersionCode(pInfo)
        } catch (_: Exception) {
            BuildConfig.VERSION_CODE.toLong()
        }
    }

    /**
     * Converts inputs such as:
     * - "owner/repo"
     * - "https://github.com/owner/repo"
     * - "https://github.com/owner/repo.git"
     * - "https://github.com/owner/repo/blob/main/version.json" -> raw.githubusercontent.com
     * - "https://raw.githubusercontent.com/owner/repo/main/version.json"
     * - "https://example.com/version.json" (any remote JSON host)
     * into a primary normalized endpoint URL.
     */
    fun normalizeRepoToApiUrl(rawInput: String): String? {
        return resolveVersionCheckUrls(rawInput).firstOrNull()
    }

    /**
     * Resolves an input repository or remote JSON URL into ordered endpoint candidates.
     * Supports direct remote JSON URLs on GitHub/Gist/any host, plus GitHub Releases API
     * with automatic fallback to `version.json` on `main` / `master`.
     */
    fun resolveVersionCheckUrls(rawInput: String): List<String> {
        val trimmed = rawInput.trim().trimEnd('/')
        if (trimmed.isBlank()) return emptyList()

        // 1. GitHub blob/raw file link -> raw.githubusercontent.com
        val githubFileRegex = Regex(
            """https?://(?:www\.)?github\.com/([^/\s]+)/([^/\s]+)/(?:blob|raw)/([^/\s]+)/(.+)""",
            RegexOption.IGNORE_CASE
        )
        githubFileRegex.find(trimmed)?.let { m ->
            val owner = m.groupValues[1]
            val repo = m.groupValues[2].removeSuffix(".git")
            val branch = m.groupValues[3]
            val path = m.groupValues[4]
            return listOf("https://raw.githubusercontent.com/$owner/$repo/$branch/$path")
        }

        // 2. GitHub Releases API URL directly
        if (trimmed.startsWith("https://api.github.com/repos/", ignoreCase = true)) {
            return when {
                trimmed.endsWith("/releases/latest", ignoreCase = true) -> {
                    val baseReleases = trimmed.removeSuffix("/latest").removeSuffix("/LATEST")
                    listOf(trimmed, "$baseReleases?per_page=10")
                }
                trimmed.endsWith("/releases", ignoreCase = true) -> {
                    listOf("$trimmed/latest", "$trimmed?per_page=10")
                }
                else -> listOf("$trimmed/releases/latest", "$trimmed/releases?per_page=10")
            }
        }

        // 3. Direct remote JSON URL on GitHub raw, Gist, or any other host
        val isHttpUrl = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        val isGitHubRepoHost = Regex("""^https?://(?:www\.)?github\.com/[^/\s]+/[^/\s]+(?:/releases(?:/latest)?)?(?:\.git)?$""", RegexOption.IGNORE_CASE)
            .matches(trimmed)

        if (isHttpUrl && !isGitHubRepoHost) {
            return listOf(trimmed)
        }

        // 4. GitHub repository URL -> Releases API + raw version.json fallbacks
        val githubRegex = Regex(
            """(?:https?://)?(?:www\.)?github\.com/([^/\s]+)/([^/\s]+)(?:/.*)?""",
            RegexOption.IGNORE_CASE
        )
        githubRegex.find(trimmed)?.let { match ->
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")
            return listOf(
                "https://api.github.com/repos/$owner/$repo/releases/latest",
                "https://api.github.com/repos/$owner/$repo/releases?per_page=10",
                "https://raw.githubusercontent.com/$owner/$repo/main/version.json",
                "https://raw.githubusercontent.com/$owner/$repo/master/version.json"
            )
        }

        // 5. Shorthand "owner/repo"
        val slugRegex = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
        slugRegex.find(trimmed)?.let { slugMatch ->
            val owner = slugMatch.groupValues[1]
            val repo = slugMatch.groupValues[2].removeSuffix(".git")
            return listOf(
                "https://api.github.com/repos/$owner/$repo/releases/latest",
                "https://api.github.com/repos/$owner/$repo/releases?per_page=10",
                "https://raw.githubusercontent.com/$owner/$repo/main/version.json",
                "https://raw.githubusercontent.com/$owner/$repo/master/version.json"
            )
        }

        return emptyList()
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

    fun isNewerUpdate(
        release: GitHubReleaseInfo,
        currentVersionName: String,
        currentVersionCode: Long
    ): Boolean {
        if (release.versionCode > 0 && currentVersionCode > 0L) {
            if (release.versionCode.toLong() > currentVersionCode) return true
            if (release.versionCode.toLong() < currentVersionCode) return false
        }
        val remoteVersionCandidate = release.versionName.ifBlank { release.tagName }
        return isNewerVersion(remoteVersionCandidate, currentVersionName)
    }

    /**
     * Parses either a GitHub Releases API JSON response (single object or release list array)
     * or a generic remote `version.json` file.
     */
    fun parseVersionJson(bodyStr: String, sourceUrl: String = ""): GitHubReleaseInfo {
        val trimmedBody = bodyStr.trim()
        if (trimmedBody.startsWith("[")) {
            val releasesArray = JSONArray(trimmedBody)
            var firstNonDraft: GitHubReleaseInfo? = null
            for (i in 0 until releasesArray.length()) {
                val obj = releasesArray.optJSONObject(i) ?: continue
                if (obj.optBoolean("draft", false)) continue
                val parsed = parseGitHubReleaseObject(obj)
                if (firstNonDraft == null) {
                    firstNonDraft = parsed
                }
                if (!parsed.apkDownloadUrl.isNullOrBlank()) {
                    return parsed
                }
            }
            if (firstNonDraft != null) return firstNonDraft
            throw IllegalStateException("Tidak ada rilis yang tersedia dari $sourceUrl.")
        }

        val json = JSONObject(trimmedBody)
        val isGitHubReleaseJson = json.has("tag_name") && json.has("assets")

        if (isGitHubReleaseJson) {
            return parseGitHubReleaseObject(json)
        }

        // Generic remote version.json manifest (GitHub raw, Gist, or any other host)
        val rawVersionName = firstNonBlank(
            json.optString("versionName", ""),
            json.optString("version_name", ""),
            json.optString("latestVersion", ""),
            json.optString("latest_version", ""),
            json.optString("version", "")
        )
        val rawTagName = firstNonBlank(
            json.optString("tagName", ""),
            json.optString("tag_name", "")
        ).ifBlank {
            if (rawVersionName.isNotBlank()) "v${normalizeVersionNumber(rawVersionName)}" else ""
        }
        val normalizedVersion = normalizeVersionNumber(rawVersionName.ifBlank { rawTagName })
        val remoteVersionCode = when {
            json.has("versionCode") -> json.optInt("versionCode", 0)
            json.has("version_code") -> json.optInt("version_code", 0)
            json.has("latestVersionCode") -> json.optInt("latestVersionCode", 0)
            json.has("latest_version_code") -> json.optInt("latest_version_code", 0)
            else -> 0
        }

        if (normalizedVersion.isBlank() && remoteVersionCode <= 0) {
            throw IllegalStateException("Format JSON versi tidak valid dari $sourceUrl.")
        }

        val apkUrl = firstNonBlank(
            json.optString("apkUrl", ""),
            json.optString("apk_url", ""),
            json.optString("apkDownloadUrl", ""),
            json.optString("downloadUrl", ""),
            json.optString("download_url", ""),
            json.optString("url", "")
        ).ifBlank { null }

        val htmlUrl = firstNonBlank(
            json.optString("htmlUrl", ""),
            json.optString("html_url", ""),
            json.optString("pageUrl", ""),
            json.optString("page_url", ""),
            apkUrl ?: ""
        )

        val releaseName = firstNonBlank(
            json.optString("releaseName", ""),
            json.optString("release_name", ""),
            json.optString("title", ""),
            json.optString("name", ""),
            rawTagName
        )

        val releaseNotes = firstNonBlank(
            json.optString("releaseNotes", ""),
            json.optString("release_notes", ""),
            json.optString("changelog", ""),
            json.optString("notes", ""),
            json.optString("body", ""),
            json.optString("description", "")
        )

        val apkFileName = firstNonBlank(
            json.optString("apkFileName", ""),
            json.optString("apk_file_name", "")
        ).ifBlank {
            apkUrl?.substringAfterLast('/')?.substringBefore('?')?.takeIf { it.endsWith(".apk", ignoreCase = true) }
        }

        val apkSize = when {
            json.has("apkSizeBytes") -> json.optLong("apkSizeBytes", 0L)
            json.has("apk_size_bytes") -> json.optLong("apk_size_bytes", 0L)
            json.has("size") -> json.optLong("size", 0L)
            else -> 0L
        }

        return GitHubReleaseInfo(
            tagName = rawTagName.ifBlank { "v$normalizedVersion" },
            versionName = normalizedVersion,
            releaseName = releaseName,
            releaseNotes = releaseNotes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkFileName = apkFileName,
            apkSizeBytes = apkSize,
            versionCode = remoteVersionCode
        )
    }

    private fun parseGitHubReleaseObject(json: JSONObject): GitHubReleaseInfo {
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

        return GitHubReleaseInfo(
            tagName = tagName,
            versionName = normalizeVersionNumber(tagName),
            releaseName = releaseName,
            releaseNotes = releaseNotes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkFileName = apkName,
            apkSizeBytes = apkSize,
            versionCode = json.optInt("versionCode", json.optInt("version_code", 0))
        )
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }

    suspend fun fetchLatestRelease(repoInput: String): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val candidateUrls = resolveVersionCheckUrls(repoInput)
            if (candidateUrls.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Format URL repositori atau file JSON versi tidak valid.")
                )
            }

            var lastErrorMessage: String? = null
            var fallbackRelease: GitHubReleaseInfo? = null
            for (endpointUrl in candidateUrls) {
                try {
                    val isGitHubApi = endpointUrl.contains("api.github.com", ignoreCase = true)
                    val request = Request.Builder()
                        .url(endpointUrl)
                        .header(
                            "Accept",
                            if (isGitHubApi) "application/vnd.github+json" else "application/json"
                        )
                        .header("User-Agent", "MonBer-Android-Updater")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val bodyStr = response.body?.string().orEmpty()
                        if (response.isSuccessful && bodyStr.isNotBlank()) {
                            val parsed = parseVersionJson(bodyStr, endpointUrl)
                            if (!parsed.apkDownloadUrl.isNullOrBlank()) {
                                return@withContext Result.success(parsed)
                            }
                            if (fallbackRelease == null) {
                                fallbackRelease = parsed
                            }
                        } else {
                            lastErrorMessage = when (response.code) {
                                404 -> "Rilis atau file version.json tidak ditemukan (404)."
                                403 -> "Batas akses server tercapai (HTTP 403). Coba beberapa saat lagi."
                                else -> "Gagal memeriksa pembaruan (HTTP ${response.code})."
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMessage = e.message ?: "Gagal menghubungi server pembaruan."
                }
            }

            fallbackRelease?.let { return@withContext Result.success(it) }

            Result.failure(
                IllegalStateException(
                    lastErrorMessage ?: "Gagal memuat informasi versi terbaru dari server."
                )
            )
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

                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.startsWith("text/html", ignoreCase = true)) {
                    return@withContext Result.failure(
                        IllegalStateException("URL unduhan mengembalikan halaman HTML, bukan file APK.")
                    )
                }

                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Respons unduhan kosong."))

                val totalBytes = if (body.contentLength() > 0) body.contentLength() else release.apkSizeBytes
                val totalMb = totalBytes / (1024f * 1024f)

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
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

            if (!isValidApkFile(targetFile)) {
                targetFile.delete()
                return@withContext Result.failure(
                    IllegalStateException("File yang diunduh bukan paket APK yang valid.")
                )
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isValidApkFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(2)
                input.read(header) == 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            }
        } catch (_: Exception) {
            false
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

    suspend fun installApkWithPackageInstaller(
        context: Context,
        apkFile: File
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var session: PackageInstaller.Session? = null
        try {
            if (!apkFile.exists() || apkFile.length() <= 0L) {
                return@withContext Result.failure(
                    IllegalStateException("File APK pembaruan tidak ditemukan atau kosong.")
                )
            }

            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(context.packageName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }

            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)

            session.openWrite("monber_update.apk", 0, apkFile.length()).use { out ->
                FileInputStream(apkFile).use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                    }
                }
                session.fsync(out)
            }

            val callbackIntent = Intent(context, AppUpdateInstallReceiver::class.java).apply {
                action = "${context.packageName}.ACTION_APP_UPDATE_INSTALL_STATUS"
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val statusPendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                callbackIntent,
                pendingIntentFlags
            )

            session.commit(statusPendingIntent.intentSender)
            session.close()
            session = null
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                session?.abandon()
            } catch (_: Exception) {
            }
            try {
                session?.close()
            } catch (_: Exception) {
            }
            Result.failure(e)
        }
    }

    suspend fun installApk(context: Context, apkFile: File): Boolean {
        val packageInstallerResult = installApkWithPackageInstaller(context, apkFile)
        if (packageInstallerResult.isSuccess) {
            return true
        }
        return launchApkInstaller(context, apkFile)
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
