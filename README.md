# Music Relay

Relay app for YT Music ReVanced (`app.revanced.android.apps.youtube.music`).
Goal: "Hey Google, play Runaway on Music Relay" starts the song in ReVanced. No root.

## How it works
- `RelayActivity` handles `android.media.action.MEDIA_PLAY_FROM_SEARCH`.
  It forwards the query and closes without showing UI.
- `RelayMediaService` is a `MediaBrowserService` with a `MediaSession`.
  It supports `ACTION_PLAY_FROM_SEARCH`, so assistants see a media app.
- `Relay.forward()` sends ReVanced the same intent as the MacroDroid macro:
  - action `android.media.action.MEDIA_PLAY_FROM_SEARCH`
  - package `app.revanced.android.apps.youtube.music`
  - extra `query` set to the search text
  - extra `android.intent.extra.focus` set to `vnd.android.cursor.item/*`
- If ReVanced has no handler for that intent, the relay opens
  `https://music.youtube.com/search?q=...` in ReVanced instead. This shows results and may not auto-play.
- An empty query opens ReVanced.
- Opening the app directly shows a search box, a mic button and a Play button.

## Install
1. On your phone, open https://github.com/ClemEsBot1/ytm-relay/releases/latest and tap `music-relay.apk`.
2. Sideload it. Allow "Install unknown apps" for your browser or file manager.
3. Open Music Relay. Tap **Allow background launch**. Turn on "Display over other apps".
   The media-session path needs this. Android 10+ blocks background activity starts without it.

## Voice via Gemini
Gemini sends "play X on <app>" only to music services Google recognises. A sideloaded app is not one,
so Gemini opens YouTube instead. Workaround:
1. Say "Hey Google, open Music Relay".
2. The app opens and starts listening (setting: **Start listening when opened**, on by default).
3. Say the song, e.g. "Heartless Kanye West". It plays in ReVanced.

## Send method
The app screen has a **Send method** picker. It applies to every request, voice included.
- **Find top result and play it** (default): looks up the top YT Music result for the query,
  Song (audio-only) versions win over music videos: it tries the Songs filter, then the first song
  in all results, then the top result. It then opens `music.youtube.com/watch?v=<id>&list=RDAMVM<id>` in ReVanced. The song plays at once,
  followed by its radio mix. Uses YT Music's unofficial web search endpoint (no API key).
  If the lookup fails, the app opens the search page instead.
- **Open ReVanced, then send search**: sends the search intent, then sends it again 2.5 s later.
  This covers YT Music dropping the query on a cold start.
- **Send search intent only**: one intent, same as the MacroDroid macro.
- **Open search page link**: opens `music.youtube.com/search?q=...` in ReVanced. Shows results and may not auto-play.

## Rename
Change `app_name` in `app/src/main/res/values/strings.xml`. This is the name you say to Gemini.

## Test without voice
```bash
adb shell am start -a android.media.action.MEDIA_PLAY_FROM_SEARCH -n dev.relay.ytm/.RelayActivity -e query "Runaway Kanye West"
```

## Known limits
- Gemini may route "play X on ..." only to music services it recognises. It may ignore a sideloaded app.
- ReVanced may not handle `MEDIA_PLAY_FROM_SEARCH`. Test the MacroDroid macro first.
- Logs: `adb logcat -s MusicRelay`.

## Build locally
```bash
./gradlew assembleDebug
```
Needs JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0).
