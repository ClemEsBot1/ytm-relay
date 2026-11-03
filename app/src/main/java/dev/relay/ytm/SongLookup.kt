package dev.relay.ytm

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Finds a YT Music track for a query via the web client's search endpoint
 * (unofficial; no API key). Prefers audio-only song versions over music videos.
 */
object SongLookup {
    private const val TAG = "MusicRelay"
    private const val ENDPOINT = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private const val CLIENT_VERSION = "1.20241023.01.00"

    /** Search filter "Songs" (same value the web client sends). */
    private const val SONGS_FILTER = "EgWKAQIIAWoMEA4QChADEAQQCRAF"

    /** Audio track ("song") as opposed to OMV/UGC music videos. */
    private const val SONG_TYPE = "MUSIC_VIDEO_TYPE_ATV"

    private const val PODCAST_TYPE = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"

    private data class Hit(val videoId: String, val type: String?)

    /** Blocking. Call off the main thread. */
    fun topVideoId(query: String): String? {
        // 1. Songs-only search.
        search(query, SONGS_FILTER)
            ?.firstOrNull { it.type == null || it.type == SONG_TYPE }
            ?.let { return it.videoId.also { _ -> Log.i(TAG, "lookup: songs filter") } }

        // 2. Unfiltered search: first song version, else the top result.
        val hits = search(query, null) ?: return null
        hits.firstOrNull { it.type == SONG_TYPE }
            ?.let { return it.videoId.also { _ -> Log.i(TAG, "lookup: first song in all results") } }
        Log.i(TAG, "lookup: no song version, using top non-podcast result")
        return hits.firstOrNull { it.type != PODCAST_TYPE }?.videoId
    }

    private fun search(query: String, params: String?): List<Hit>? = try {
        val body = JSONObject()
            .put("context", JSONObject().put("client", JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", "en")))
            .put("query", query)
            .apply { if (params != null) put("params", params) }
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
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                mutableListOf<Hit>().also { collect(json, it) }
            }
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        Log.w(TAG, "lookup failed", e)
        null
    }

    /** Walks the response in order and records each playable result. */
    private fun collect(node: Any?, out: MutableList<Hit>) {
        when (node) {
            is JSONObject -> {
                val item = node.optJSONObject("musicResponsiveListItemRenderer")
                    ?: node.optJSONObject("musicCardShelfRenderer")
                if (item != null) {
                    firstWatchEndpoint(item)?.let { we ->
                        val type = we.optJSONObject("watchEndpointMusicSupportedConfigs")
                            ?.optJSONObject("watchEndpointMusicConfig")
                            ?.optString("musicVideoType")?.takeIf { it.isNotEmpty() }
                        val id = we.optString("videoId")
                        if (id.length == 11 && out.none { it.videoId == id }) out += Hit(id, type)
                    }
                    // Card shelves contain further items; keep walking.
                    if (node.has("musicResponsiveListItemRenderer")) return
                }
                node.keys().forEach { collect(node.opt(it), out) }
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), out)
        }
    }

    private fun firstWatchEndpoint(node: Any?): JSONObject? {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("watchEndpoint")?.let { if (it.has("videoId")) return it }
                for (k in node.keys()) firstWatchEndpoint(node.opt(k))?.let { return it }
            }
            is JSONArray -> for (i in 0 until node.length()) firstWatchEndpoint(node.opt(i))?.let { return it }
        }
        return null
    }
}
