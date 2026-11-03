package dev.relay.ytm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlin.concurrent.thread

/** Finds a song and plays it in YT Music ReVanced. */
object Relay {
    const val TARGET = "app.revanced.android.apps.youtube.music"
    private const val TAG = "MusicRelay"

    private val main = Handler(Looper.getMainLooper())

    /**
     * Looks up the song and opens it in ReVanced. Call from a foreground
     * activity. [done] runs on the main thread with true if ReVanced opened.
     */
    fun play(context: Context, rawQuery: String?, done: (Boolean) -> Unit = {}) {
        val app = context.applicationContext
        val query = rawQuery?.trim().orEmpty()
        Log.i(TAG, "play query='$query'")

        if (query.isEmpty()) {
            val launch = app.packageManager.getLaunchIntentForPackage(TARGET)
            done(launch != null && start(app, launch))
            return
        }

        thread(name = "song-lookup") {
            val id = SongLookup.topVideoId(query)
            Log.i(TAG, "lookup '$query' -> $id")
            main.post {
                // Lookup failed: show the search page so the user can pick.
                val ok = (id != null && start(app, watchIntent(id))) || start(app, searchIntent(query))
                if (!ok) Toast.makeText(app, R.string.revanced_missing, Toast.LENGTH_LONG).show()
                done(ok)
            }
        }
    }

    /** Plays the song, then continues with its radio mix. */
    private fun watchIntent(videoId: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/watch?v=$videoId&list=RDAMVM$videoId"))
            .setPackage(TARGET)

    private fun searchIntent(query: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(query)))
            .setPackage(TARGET)

    private fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Log.i(TAG, "started ${intent.dataString ?: intent.action}")
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "no handler for ${intent.dataString}", e)
        false
    }
}
