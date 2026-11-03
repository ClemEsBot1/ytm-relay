package dev.relay.ytm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast

/** Forwards a search query to YT Music ReVanced. */
object Relay {
    const val TARGET = "app.revanced.android.apps.youtube.music"
    private const val TAG = "MusicRelay"

    /** Returns true if some intent to ReVanced was started. */
    fun forward(context: Context, rawQuery: String?): Boolean {
        val query = rawQuery?.trim().orEmpty()
        Log.i(TAG, "forward query='$query'")

        if (query.isEmpty()) {
            // "Play music on <app>" with no query: just open ReVanced.
            val launch = context.packageManager.getLaunchIntentForPackage(TARGET)
            return launch != null && start(context, launch)
        }

        // 1st choice: the same intent the MacroDroid macro sends.
        val search = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            .setPackage(TARGET)
            .putExtra("query", query) // SearchManager.QUERY
            .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
        if (start(context, search)) return true

        // Fallback: open the search page via deep link (opens results, may not auto-play).
        val view = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)))
            .setPackage(TARGET)
        if (start(context, view)) return true

        Toast.makeText(context, R.string.revanced_missing, Toast.LENGTH_LONG).show()
        return false
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "no handler for ${intent.action}", e)
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "blocked: ${intent.action}", e)
        false
    }
}
