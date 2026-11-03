package dev.relay.ytm

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

class RelayActivity : Activity() {

    private lateinit var queryBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (handleSearchIntent(intent)) return

        setContentView(R.layout.activity_relay)
        queryBox = findViewById(R.id.query)

        findViewById<Button>(R.id.play).setOnClickListener { send(queryBox.text.toString()) }
        queryBox.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) { send(queryBox.text.toString()); true } else false
        }
        findViewById<ImageButton>(R.id.mic).setOnClickListener { listen() }
        val autoListen = prefs().getBoolean(KEY_AUTO_LISTEN, true)
        findViewById<CheckBox>(R.id.auto_listen).apply {
            isChecked = autoListen
            setOnCheckedChangeListener { _, on -> prefs().edit().putBoolean(KEY_AUTO_LISTEN, on).apply() }
        }
        // "Hey Google, open Music Relay" -> go straight to voice input.
        if (savedInstanceState == null && autoListen) listen()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSearchIntent(intent)
    }

    /** MEDIA_PLAY_FROM_SEARCH from another app: play and close without showing UI. */
    private fun handleSearchIntent(intent: Intent?): Boolean {
        if (intent?.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return false
        // Stay open until ReVanced launches: Android blocks launches from the background.
        Relay.play(this, intent.getStringExtra(SearchManager.QUERY)) { finish() }
        return true
    }

    private fun send(query: String) {
        if (query.isBlank()) return
        findViewById<Button>(R.id.play).apply { isEnabled = false; setText(R.string.finding) }
        Relay.play(this, query) { ok ->
            if (ok) finish()
            else findViewById<Button>(R.id.play).apply { isEnabled = true; setText(R.string.play) }
        }
    }

    private fun listen() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.hint))
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(i, REQ_SPEECH)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No speech recognizer", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Framework Activity API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_SPEECH || resultCode != RESULT_OK) return
        val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return
        queryBox.setText(text)
        send(text)
    }

    private fun prefs() = getSharedPreferences("relay", MODE_PRIVATE)

    private companion object {
        const val REQ_SPEECH = 1
        const val KEY_AUTO_LISTEN = "auto_listen"
    }
}
