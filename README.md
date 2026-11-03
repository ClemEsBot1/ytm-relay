# Music Relay

Voice-to-song launcher for YT Music ReVanced (`app.revanced.android.apps.youtube.music`). No root.

## Use
1. Say "Hey Google, open Music Relay" (or open it any other way).
2. The app starts listening. Say the song, e.g. "Heartless Kanye West".
3. The song plays in ReVanced, followed by its radio mix.

You can also type in the search box and press Play.
**Start listening when opened** turns the automatic voice prompt on or off.

## How it works
- The app searches YT Music's web endpoint (unofficial, no API key) for the query.
- It prefers the song (audio-only) version: Songs filter first, then the first song in all results,
  then the top non-podcast result.
- It opens `music.youtube.com/watch?v=<id>&list=RDAMVM<id>` in ReVanced, which plays at once.
- If the lookup fails, it opens the ReVanced search page for the query instead.
- Other apps can send `android.media.action.MEDIA_PLAY_FROM_SEARCH` with a `query` extra; same result.

Gemini does not route "play X on Music Relay" to a sideloaded app; it opens YouTube instead.
That is why the app is opened by name and then listens.

## Install
On your phone, open https://github.com/ClemEsBot1/ytm-relay/releases/latest and tap `music-relay.apk`.
Every push to `main` rebuilds that release.

## Rename
Change `app_name` in `app/src/main/res/values/strings.xml`. This is the name you say to Gemini.

## Debug
```bash
adb logcat -s MusicRelay
```

## Build locally
```bash
./gradlew assembleDebug
```
Needs JDK 17+ and the Android SDK (platform 35, build-tools 35.0.0).
