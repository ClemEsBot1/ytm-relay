package dev.relay.ytm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlin.concurrent.thread

/** Forwards a search query to YT Music ReVanced. */
object Relay {
    const val TARGET = "app.revanced.android.apps.youtube.music"
    private const val TAG = "MusicRelay"
    private const val PREFS = "relay"
    private const val KEY_METHOD = "method_v2"

    /** Delay between cold-starting ReVanced and re-sending the search. */
    private const val RETRY_DELAY_MS = 2500L

    enum class Method { AUTOPLAY, RETRY, INTENT, LINK }

    private val main = Handler(Looper.getMainLooper())

    fun method(context: Context): Method =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_METHOD, null)
            ?.let { runCatching { Method.valueOf(it) }.getOrNull() }
            ?: Method.AUTOPLAY

    fun setMethod(context: Context, method: Method) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_METHOD, method.name).apply()
    }

    /**
     * Sends the query to ReVanced. [done] runs on the main thread with true
     * if some intent was started. AUTOPLAY finishes asynchronously.
     */
    fun forward(context: Context, rawQuery: String?, done: (Boolean) -> Unit = {}) {
        val app = context.applicationContext
        val query = rawQuery?.trim().orEmpty()
        val method = method(app)
        Log.i(TAG, "forward query='$query' method=$method")

        if (query.isEmpty()) {
            // "Play music on <app>" with no query: just open ReVanced.
            val launch = app.packageManager.getLaunchIntentForPackage(TARGET)
            done(launch != null && start(app, launch))
            return
        }

        when (method) {
            Method.AUTOPLAY -> thread(name = "song-lookup") {
                val id = SongLookup.topVideoId(query)
                Log.i(TAG, "lookup '$query' -> $id")
                main.post {
                    val ok = (id != null && start(app, watchIntent(id))) || fallback(app, query)
                    done(ok)
                }
            }
            Method.INTENT -> done(start(app, searchIntent(query)) || fallback(app, query))
            Method.LINK -> done(fallback(app, query))
            Method.RETRY -> {
                // YT Music can drop the query on a cold start: send again later.
                val first = start(app, searchIntent(query))
                if (first) main.postDelayed({ start(app, searchIntent(query)) }, RETRY_DELAY_MS)
                done(first || fallback(app, query))
            }
        }
    }

    /** Search page deep link; shows results, does not auto-play. */
    private fun fallback(app: Context, query: String): Boolean {
        if (start(app, searchLinkIntent(query))) return true
        Toast.makeText(app, R.string.revanced_missing, Toast.LENGTH_LONG).show()
        return false
    }

    /** Plays the song, then continues with its radio mix. */
    private fun watchIntent(videoId: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId"))
            .setPackage(TARGET)

    /** Same intent the MacroDroid macro sends. */
    private fun searchIntent(query: String) =
        Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .setPackage(TARGET)
            .putExtra("query", query) // SearchManager.QUERY
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")

    private fun searchLinkIntent(query: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)))
            .setPackage(TARGET)

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Log.i(TAG, "started ${intent.action} ${intent.dataString ?: ""}")
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "no handler for ${intent.action}", e)
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "blocked: ${intent.action}", e)
        false
    }
}
