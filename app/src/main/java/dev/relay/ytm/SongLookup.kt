package dev.relay.ytm

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Finds the top YT Music result for a query via the web client's search
 * endpoint (unofficial; no API key). Returns a video ID or null.
 */
object SongLookup {
    private const val TAG = "MusicRelay"
    private const val ENDPOINT = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241023.01.00"
    private val VIDEO_ID = Regex("\"videoId\":\"([\\w-]{11})\"")

    /** Blocking. Call off the main thread. */
    fun topVideoId(query: String): String? = try {
        val body = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", "en")))
            .put("query", query)
            .toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) {
                Log.w(TAG, "lookup HTTP ${conn.responseCode}")
                null
            } else {
                // First videoId in the response is the top result.
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                VIDEO_ID.find(text)?.groupValues?.get(1)
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "lookup failed", e)
        null
    }
}
