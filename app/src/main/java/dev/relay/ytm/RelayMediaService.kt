package dev.relay.ytm

import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService

/**
 * Exposes a media session so assistants treat this app as a media app.
 * Every play request is relayed to ReVanced. Launching from here counts as a
 * background start: grant "Display over other apps" so Android allows it.
 */
class RelayMediaService : MediaBrowserService() {

    private lateinit var session: MediaSession

    override fun onCreate() {
        super.onCreate()
        session = MediaSession(this, "MusicRelay").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                    Relay.forward(this@RelayMediaService, query)
                }
                override fun onPlay() {
                    Relay.forward(this@RelayMediaService, null)
                }
            })
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PLAY_FROM_SEARCH or
                            PlaybackState.ACTION_PREPARE_FROM_SEARCH
                    )
                    .setState(PlaybackState.STATE_STOPPED, 0, 1f)
                    .build()
            )
            isActive = true
        }
        sessionToken = session.sessionToken
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?) =
        BrowserRoot(ROOT, null)

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private companion object { const val ROOT = "root" }
}
