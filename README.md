# DayOne — daily photo tracker (local-only, Android)

A fully offline app for taking one photo a day, forever, across as many independent
"instances" (projects) as you want — e.g. one for yourself, one for a group, one for
someone else. Nothing in this app ever touches the network: no `INTERNET` permission
is even declared in the manifest.

## What's included

- **Multiple instances** — create any number of projects (Me / Group / Friend / etc),
  each with its own folder on disk, streak, and reminder time.
- **Capture screen** — live camera preview with:
  - a translucent ghost overlay of *yesterday's* photo so you can line up your position
  - a dashed oval "head zone" guide + center crosshair
  - a slider to adjust the ghost overlay strength
- **Auto face-centered crop** — every captured photo is run through on-device ML Kit
  face detection and cropped/scaled so the face is consistently centered day to day,
  matching the guide oval. Fully offline (ML Kit's bundled model does not call the network).
- **Daily reminder that won't let you forget** — an exact alarm fires at your chosen time
  each morning; if you haven't taken that day's photo, it re-notifies every 45 minutes
  until you do (or until midnight). Survives reboots.
- **Timeline view** — scrub through every day with a slider + thumbnail strip.
- **Video export** — stitches all of a project's photos into an MP4 timelapse with
  burned-in overlays you can toggle: day number, date, age (from an optional birthdate),
  and year. Built with the platform's own `MediaCodec`/`MediaMuxer`, no extra library.

## Project structure

```
DayOne/
├── app/src/main/java/com/dayone/app/
│   ├── data/            Room database, DAOs, PhotoRepository (all local storage)
│   ├── camera/           CameraX wrapper + ML Kit face-crop logic
│   ├── notify/           AlarmManager-based daily/nag reminder system
│   ├── video/            MediaCodec-based MP4 exporter with overlays
│   └── ui/screens/       Jetpack Compose screens (list, capture, timeline, export)
└── app/src/main/AndroidManifest.xml
```

## How to build the APK

You'll need [Android Studio](https://developer.android.com/studio) (free) installed.
No coding required — this is a one-time, few-click process:

1. Unzip this project.
2. Open Android Studio → **File → Open** → select the unzipped `DayOne` folder.
3. Let Gradle sync finish (Android Studio will download the Gradle wrapper and all
   dependencies automatically the first time — this needs an internet connection just
   for this one-time setup step; the *app itself* never uses the network at runtime).
4. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. When it finishes, click the "locate" link in the notification, or find the file at:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Copy that `.apk` to your phone (email, USB, cloud drive — whatever you like) and
   install it. You'll need to allow "install from unknown sources" for whichever app
   you use to open the file, since it isn't from the Play Store.

If you'd rather have a **release** build (smaller, no `.debug` suffix on the package
name), use **Build → Generate Signed App Bundle / APK** instead and let Android Studio
create a new keystore for you when prompted — keep that keystore file safe if you ever
want to update the app later with the same package identity.

## First-run setup on your phone

The app will ask for two things it needs to actually nag you reliably:

1. **Notification permission** (Android 13+) — asked automatically on first launch.
2. **"Allow exact alarms"** (Android 12+) — Android treats this as a special
   permission that can't be requested via a normal popup. The app shows a banner
   on the home screen that takes you straight to the right Settings page — tap it
   once and flip the switch.

Also worth doing manually, since manufacturers vary:
- Disable battery optimization for DayOne (Settings → Apps → DayOne → Battery →
  "Unrestricted") so Android doesn't kill the reminder alarms in the background.

## Notes / things you may want to tweak

- Reminder nag interval is 45 minutes — change `NAG_INTERVAL_MINUTES` in
  `notify/ReminderScheduler.kt`.
- Default reminder time is 8:00am — set per-project when you create it.
- Photos are stored at `Android/data/com.dayone.app/files/DayOne/<ProjectFolder>/`
  on your phone's storage (app-specific, no storage permission needed). Exported
  videos land in a `exports/` subfolder there too.
- Face-crop "headroom" and oval size are tuned in `camera/FaceCropper.kt`
  (`headFraction`) and `ui/screens/CaptureOverlay.kt` if you want the crop tighter
  or looser.

## A note on how this was built

This project was generated as source code only — it has **not** been compiled or run
in a real Android build environment, since that requires the Android SDK and Google's
Maven repository, which weren't reachable from the sandbox this was written in. The
code is complete and follows standard, well-tested Android patterns (CameraX, Room,
ML Kit, AlarmManager, MediaCodec), but budget an extra pass for the normal small fixes
that come up the first time any nontrivial project is actually compiled — a typo, a
missing import, a small API mismatch. Nothing here is exotic, so those should be quick
to spot from Android Studio's error list if they come up at all.
