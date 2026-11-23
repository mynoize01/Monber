package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateManagerTest {

    @Test
    fun `resolveVersionCheckUrls handles github repo, blob json, raw json, and custom host json`() {
        val repoUrls = AppUpdateManager.resolveVersionCheckUrls("https://github.com/mynoize01/Monber.git")
        assertEquals(
            listOf(
                "https://api.github.com/repos/mynoize01/Monber/releases/latest",
                "https://api.github.com/repos/mynoize01/Monber/releases?per_page=10",
                "https://raw.githubusercontent.com/mynoize01/Monber/main/version.json",
                "https://raw.githubusercontent.com/mynoize01/Monber/master/version.json"
            ),
            repoUrls
        )

        val blobUrls = AppUpdateManager.resolveVersionCheckUrls(
            "https://github.com/mynoize01/Monber/blob/main/version.json"
        )
        assertEquals(
            listOf("https://raw.githubusercontent.com/mynoize01/Monber/main/version.json"),
            blobUrls
        )

        val customHostUrls = AppUpdateManager.resolveVersionCheckUrls(
            "https://updates.example.com/monber/version.json"
        )
        assertEquals(
            listOf("https://updates.example.com/monber/version.json"),
            customHostUrls
        )
    }

    @Test
    fun `parseVersionJson parses generic remote version json from custom host`() {
        val remoteJson = """
            {
              "versionCode": 6,
              "latestVersion": "1.5",
              "releaseNotes": "Perbaikan sinkronisasi.",
              "apkUrl": "https://updates.example.com/monber-v1.5.apk",
              "apkSizeBytes": 12582912
            }
        """.trimIndent()

        val parsed = AppUpdateManager.parseVersionJson(
            remoteJson,
            "https://updates.example.com/version.json"
        )

        assertEquals(6, parsed.versionCode)
        assertEquals("1.5", parsed.versionName)
        assertEquals("v1.5", parsed.tagName)
        assertEquals("Perbaikan sinkronisasi.", parsed.releaseNotes)
        assertEquals("https://updates.example.com/monber-v1.5.apk", parsed.apkDownloadUrl)
        assertEquals("monber-v1.5.apk", parsed.apkFileName)
        assertEquals(12582912L, parsed.apkSizeBytes)
    }

    @Test
    fun `parseVersionJson parses GitHub Releases API JSON`() {
        val githubJson = """
            {
              "tag_name": "v1.5",
              "name": "MonBer v1.5",
              "body": "Rilis baru",
              "html_url": "https://github.com/mynoize01/Monber/releases/tag/v1.5",
              "assets": [
                {
                  "name": "MonBer-v1.5.apk",
                  "browser_download_url": "https://github.com/mynoize01/Monber/releases/download/v1.5/MonBer-v1.5.apk",
                  "size": 10485760
                }
              ]
            }
        """.trimIndent()

        val parsed = AppUpdateManager.parseVersionJson(githubJson)

        assertEquals("v1.5", parsed.tagName)
        assertEquals("1.5", parsed.versionName)
        assertEquals("MonBer-v1.5.apk", parsed.apkFileName)
        assertEquals(10485760L, parsed.apkSizeBytes)
    }

    @Test
    fun `parseVersionJson selects non-draft release with apk asset from GitHub releases array`() {
        val releasesArrayJson = """
            [
              {
                "tag_name": "v1.4.7-draft",
                "draft": true,
                "name": "Draft Release",
                "assets": [
                  {
                    "name": "MonBer-v1.4.7-draft.apk",
                    "browser_download_url": "https://example.com/draft.apk",
                    "size": 999
                  }
                ]
              },
              {
                "tag_name": "v1.4.6",
                "draft": false,
                "name": "Tag Without APK",
                "assets": []
              },
              {
                "tag_name": "v1.4.5",
                "draft": false,
                "name": "MonBer v1.4.5",
                "html_url": "https://github.com/mynoize01/Monber/releases/tag/v1.4.5",
                "assets": [
                  {
                    "name": "MonBer-v1.4.5.apk",
                    "browser_download_url": "https://github.com/mynoize01/Monber/releases/download/v1.4.5/MonBer-v1.4.5.apk",
                    "size": 12345678
                  }
                ]
              }
            ]
        """.trimIndent()

        val parsed = AppUpdateManager.parseVersionJson(
            releasesArrayJson,
            "https://api.github.com/repos/mynoize01/Monber/releases?per_page=10"
        )

        assertEquals("v1.4.5", parsed.tagName)
        assertEquals("1.4.5", parsed.versionName)
        assertEquals("MonBer-v1.4.5.apk", parsed.apkFileName)
        assertEquals(
            "https://github.com/mynoize01/Monber/releases/download/v1.4.5/MonBer-v1.4.5.apk",
            parsed.apkDownloadUrl
        )
    }

    @Test
    fun `isNewerUpdate compares versionCode and versionName accurately`() {
        val newerByCode = GitHubReleaseInfo(
            tagName = "v1.4",
            versionName = "1.4",
            releaseName = "v1.4",
            releaseNotes = "",
            htmlUrl = "",
            apkDownloadUrl = null,
            apkFileName = null,
            apkSizeBytes = 0L,
            versionCode = 6
        )
        assertTrue(AppUpdateManager.isNewerUpdate(newerByCode, "1.4", 5L))

        val sameVersion = newerByCode.copy(versionCode = 5)
        assertFalse(AppUpdateManager.isNewerUpdate(sameVersion, "1.4", 5L))

        val newerByNameNoCode = newerByCode.copy(
            tagName = "v1.5",
            versionName = "1.5",
            versionCode = 0
        )
        assertTrue(AppUpdateManager.isNewerUpdate(newerByNameNoCode, "1.4", 5L))
        assertFalse(AppUpdateManager.isNewerUpdate(newerByNameNoCode.copy(tagName = "v1.3", versionName = "1.3"), "1.4", 5L))
    }
}
