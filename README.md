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
1. Get `app-debug.apk` from the **Build debug APK** workflow run (Actions tab, Artifacts).
2. Sideload it. Allow "Install unknown apps" for your browser or file manager.
3. Open Music Relay. Tap **Allow background launch**. Turn on "Display over other apps".
   The media-session path needs this. Android 10+ blocks background activity starts without it.

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
