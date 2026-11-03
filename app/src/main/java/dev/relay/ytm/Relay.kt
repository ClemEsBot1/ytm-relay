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

/** Forwards a search query to YT Music ReVanced. */
object Relay {
    const val TARGET = "app.revanced.android.apps.youtube.music"
    private const val TAG = "MusicRelay"
    private const val PREFS = "relay"
    private const val KEY_METHOD = "method"

    /** Delay between cold-starting ReVanced and sending the search. */
    private const val RETRY_DELAY_MS = 2500L

    enum class Method { RETRY, INTENT, LINK }

    fun method(context: Context): Method =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_METHOD, null)
            ?.let { runCatching { Method.valueOf(it) }.getOrNull() }
            ?: Method.RETRY

    fun setMethod(context: Context, method: Method) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_METHOD, method.name).apply()
    }

    /** Returns true if some intent to ReVanced was started. */
    fun forward(context: Context, rawQuery: String?): Boolean {
        val app = context.applicationContext
        val query = rawQuery?.trim().orEmpty()
        val method = method(app)
        Log.i(TAG, "forward query='$query' method=$method")

        if (query.isEmpty()) {
            // "Play music on <app>" with no query: just open ReVanced.
            val launch = app.packageManager.getLaunchIntentForPackage(TARGET)
            return launch != null && start(app, launch)
        }

        val ok = when (method) {
            Method.INTENT -> start(app, searchIntent(query))
            Method.LINK -> start(app, linkIntent(query))
            Method.RETRY -> {
                // YT Music can drop the query on a cold start. Send it once now,
                // then again after the app has had time to initialise.
                val first = start(app, searchIntent(query))
                if (first) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        { start(app, searchIntent(query)) }, RETRY_DELAY_MS
                    )
                }
                first
            }
        }
        if (ok) return true

        // Last resort: search page deep link.
        if (method != Method.LINK && start(app, linkIntent(query))) return true

        Toast.makeText(app, R.string.revanced_missing, Toast.LENGTH_LONG).show()
        return false
    }

    /** Same intent the MacroDroid macro sends. */
    private fun searchIntent(query: String) =
        Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .setPackage(TARGET)
            .putExtra("query", query) // SearchManager.QUERY
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")

    /** Opens the search results page; may not auto-play. */
    private fun linkIntent(query: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)))
            .setPackage(TARGET)

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Log.i(TAG, "started ${intent.action}")
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "no handler for ${intent.action}", e)
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "blocked: ${intent.action}", e)
        false
    }
}
