package com.example

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.util.LinkedList

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun restoreDebugKeystore() {
    var projectRoot: File? = null
    var curr: File? = File(".").absoluteFile
    while (curr != null) {
      if (File(curr, "settings.gradle.kts").exists() || File(curr, "debug.keystore.base64").exists()) {
        projectRoot = curr
        break
      }
      curr = curr.parentFile
    }

    if (projectRoot == null) {
      projectRoot = File("/")
    }

    val b64File = File(projectRoot, "debug.keystore.base64")
    if (!b64File.exists()) {
      println("debug.keystore.base64 does not exist at ${b64File.absolutePath}")
      return
    }
    val destFile = File(projectRoot, "debug.keystore")
    if (destFile.exists() && destFile.length() > 0L) {
      assertTrue(destFile.exists() && destFile.length() > 0)
      return
    }
    val b64Content = b64File.readText().replace(Regex("\\s+"), "")
    val decodedBytes = java.util.Base64.getMimeDecoder().decode(b64Content)
    destFile.writeBytes(decodedBytes)
    println("Successfully decoded ${b64File.absolutePath} and wrote to ${destFile.absolutePath} with size: ${destFile.length()} bytes")
    assertTrue(destFile.exists() && destFile.length() > 0)
  }

  @Test
  fun processAppIconTransparency() {
    val file = File("/app/src/main/res/drawable/bri_app_logo_1780991036669.png")
    if (!file.exists()) {
       println("File not found at /app/src/main/res/drawable/bri_app_logo_1780991036669.png")
       return
    }

    val img = ImageIO.read(file)
    val out = BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_ARGB)
    for (x in 0 until img.width) {
      for (y in 0 until img.height) {
         out.setRGB(x, y, img.getRGB(x, y))
      }
    }

    fun isWhite(rgb: Int): Boolean {
      val a = (rgb shr 24) and 0xff
      val r = (rgb shr 16) and 0xff
      val g = (rgb shr 8) and 0xff
      val b = rgb and 0xff
      return a > 0 && r > 240 && g > 240 && b > 240
    }

    val q = LinkedList<Pair<Int, Int>>()
    val visited = Array(img.width) { BooleanArray(img.height) }

    // Add 4 corners as initial seeds for BFS flood fill
    val corners = listOf(
      Pair(0, 0),
      Pair(img.width - 1, 0),
      Pair(0, img.height - 1),
      Pair(img.width - 1, img.height - 1)
    )
    for (p in corners) {
      if (isWhite(out.getRGB(p.first, p.second))) {
        q.add(p)
        visited[p.first][p.second] = true
      }
    }

    while (!q.isEmpty()) {
      val curr = q.poll() ?: continue
      val cx = curr.first
      val cy = curr.second
      out.setRGB(cx, cy, 0x00000000) // set fully transparent

      val dx = intArrayOf(0, 0, 1, -1)
      val dy = intArrayOf(1, -1, 0, 0)
      for (i in 0..3) {
        val nx = cx + dx[i]
        val ny = cy + dy[i]
        if (nx in 0 until img.width && ny in 0 until img.height) {
          if (!visited[nx][ny] && isWhite(out.getRGB(nx, ny))) {
            visited[nx][ny] = true
            q.add(Pair(nx, ny))
          }
        }
      }
    }

    ImageIO.write(out, "png", file)
    println("Successfully processed transparency for bri_app_logo_1780991036669.png!")
  }

  @Test
  fun testFetchHeaders() {
    val url = "https://docs.google.com/spreadsheets/d/1hEMC8jlu3M6GLpBTZQJVvgyRpBTcNK-6/edit?usp=drivesdk&ouid=112982864281396524406&rtpof=true&sd=true"
    val trimmed = url.trim()
    val token = "/d/"
    val startIdx = trimmed.indexOf(token) + token.length
    val nextSlash = trimmed.indexOf("/", startIdx)
    val sheetId = trimmed.substring(startIdx, nextSlash)
    val targetUrl = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"
    println("--- DIAGNOSTIC START ---")
    println("Fetching from: $targetUrl")
    
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val request = okhttp3.Request.Builder().url(targetUrl).build()
    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val lines = body.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                println("Total lines fetched: ${lines.size}")
                if (lines.isNotEmpty()) {
                    println("Line 0 (Header): ${lines[0]}")
                    for (i in 1..kotlin.math.min(5, lines.size - 1)) {
                        println("Line $i: ${lines[i]}")
                    }
                }
            } else {
                println("Response failed: ${response.code}")
            }
        }
    } catch (e: Exception) {
        println("Error fetching: ${e.message}")
        e.printStackTrace()
    }
    println("--- DIAGNOSTIC END ---")
  }
}
