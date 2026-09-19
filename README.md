# DayOne — daily photo tracker (local-only, Android)

A fully offline app for taking one photo a day, forever, across as many independent
"instances" (projects) as you want — e.g. one for yourself, one for a group, one for
someone else. Nothing in this app ever touches the network: no `INTERNET` permission
is even declared in the manifest.

## Install it

Every release ships a ready-to-install APK on the
[Releases page](../../releases) — download `DayOne-<version>.apk` on the phone and open it
(you'll need to allow "install from unknown sources" for whichever app opens the file).

Releases are built by GitHub Actions and signed with the key in `keystore/`, which is
deliberately checked in so that **every** build — from CI, from your laptop, from anywhere —
is signed identically. Android only lets an app update in place when the new APK carries
the same signature, so a stable key is what keeps your photos, streaks and settings across
updates. It is a self-signed sideload key for a private, offline app, not a Play Store key.

## What's included

- **Multiple instances** — any number of projects (Me / Group / Friend / …), each with its
  own folder on disk, streak, colour and reminder schedule. Archive the ones you pause.
- **Capture screen** built around getting the same shot every day:
  - the square that actually gets saved is outlined on screen, everything outside it dimmed
  - an overlay of a previous photo in one of five styles — ghost, **outline** (edge-detected,
    the easiest to line up against), split-screen, stripes, or off
  - the reference can be yesterday's photo, the very first one, or any day you pin
  - drag/pinch to nudge and resize the overlay, mirror it, adjust opacity
  - grid overlays, a head-guide oval matching the auto-crop, tap-to-focus, pinch zoom,
    torch, self-timer, and an optional review-before-saving step
- **Auto face-centred crop** — on-device ML Kit face detection crops and scales each photo
  so the face lands in the same place day to day. Crop tightness is adjustable per project,
  and can be switched off entirely.
- **Reminders that fit your week** — pick the time, pick the days (every day, Mon–Fri,
  weekends, or e.g. Mon/Wed/Sat), add a second reminder, choose how often it repeats while
  the photo is outstanding and what time it gives up. Notification buttons: take the photo,
  snooze an hour, or skip today. Survives reboots, clock changes and app updates.
- **Streaks that understand your schedule** — days you aren't scheduled for, and days you
  deliberately skip, don't count towards the streak and don't break it. Current streak,
  best streak, completion rate and a month calendar of shot / missed / rest days.
- **Timeline** — swipe through every day, add a note, star favourites, share a single
  photo, pin one as the capture reference, or delete it.
- **Video export** — stitches a project's photos into an MP4 timelapse with burned-in
  overlays (day number, date, age, year, notes, project name, progress bar), optional
  crossfades, and a choice of resolution / frame rate / pace / quality. The finished file
  is copied to `Movies/DayOne`, so it shows up in your gallery and Files app.
- **Backup, restore and import** — export everything to a `.zip` in `Documents/DayOne`
  (outside the app, so it survives an uninstall), restore it later, or bulk-import existing
  photos whose dates come from their filenames or EXIF data.
- **Photos in your gallery** — optionally copy every photo to `Pictures/DayOne/<project>`
  as you take it, and a one-tap **Copy existing photos to your gallery** for everything
  already in the app (per project, or all of them at once).
- **Face light** — the screen around the crop frame turns into a soft white panel at full
  brightness, so a dim room still gets a usable photo.
- **Theming** — system/light/dark, pure-black dark mode, Material You dynamic colour, or a
  fixed accent colour.
- **First-run onboarding** with terms and privacy acceptance, and permission priming that
  explains why each one is needed. Terms, privacy policy and open-source licences are all
  readable inside the app, since it has no way to open a web page.
- **Launcher shortcuts** — long-press the icon to jump straight into whichever project
  still needs a photo today.

## Project structure

```
DayOne/
├── app/src/main/java/com/dayone/app/
│   ├── data/             Room database, repositories, streak maths, backup/restore, MediaStore
│   ├── camera/           CameraX wrapper, ML Kit face-crop, edge-detection for the overlay
│   ├── notify/           AlarmManager reminder scheduling, notifications and their actions
│   ├── video/            MediaCodec + OpenGL ES MP4 exporter
│   └── ui/               Compose screens, shared components and theming
├── app/src/test/         JVM unit tests for the streak and schedule rules
└── .github/workflows/    CI build + tagged release that publishes the APK
```

## Building it yourself

Open the folder in [Android Studio](https://developer.android.com/studio) and let Gradle
sync, then **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. From the command line:

```bash
./gradlew :app:assembleRelease      # signed APK at app/build/outputs/apk/release/
./gradlew :app:bundleRelease        # Play App Bundle at app/build/outputs/bundle/release/
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # lint
```

Requirements: JDK 17, Android SDK 35. The Gradle wrapper fetches everything else.

### Cutting a release

```bash
git tag v2.1 && git push origin v2.1
```

The `Release APK` workflow builds, tests and publishes `DayOne-v2.1.apk` to a GitHub
Release. Bump `versionCode` / `versionName` in `app/build.gradle.kts` first — Android
refuses to install an update whose `versionCode` isn't higher than the installed one.

## Publishing to Google Play

`docs/play-store.md` has the listing copy, the data-safety answers, permission
justifications and a pre-launch checklist. `PRIVACY.md` and `TERMS.md` mirror the text
shown inside the app and are what the Play listing should link to. Read the **Signing**
section there before the first upload: the key in `keystore/` is checked in so sideload
updates always install in place, which is fine for an upload key under Play App Signing
but must not stay in the repo if it is also the app signing key.

## First-run setup on your phone

1. **Notification permission** (Android 13+) — asked on first launch.
2. **"Allow exact alarms"** (Android 12+) — Android treats this as a special permission
   that can't be requested with a normal popup. The home screen shows a banner that takes
   you straight to the right Settings page; tap it once and flip the switch.
3. **Battery optimisation** — set DayOne to "Unrestricted" (Settings → Apps → DayOne →
   Battery, or the shortcut in the app's settings) so Android doesn't delay the alarms.

## Where your data lives

- Photos: `Android/data/com.dayone.app/files/DayOne/<Project>/`, one JPEG per day named
  `<epochDay>_<yyyy-MM-dd>.jpg`.
- Gallery copies (optional): `Pictures/DayOne/<Project>/`, same filenames.
- Metadata: a Room database inside the app's private storage.
- Exported videos: `Movies/DayOne` (shared storage) plus a copy under the app's `exports/`.
- Backups: `Documents/DayOne/dayone-backup-<timestamp>.zip`.

Photos in app storage are deleted if you uninstall the app, so use **Settings → Export a
backup** before uninstalling, switching phones, or installing a build signed with a
different key. Auto Backup rules are also set up, so a Google-backed device transfer
carries the projects and photos across.

## Upgrading from 1.x

The database migration is a real, non-destructive migration — projects, photos, notes and
streaks all carry over, provided the update installs over the top of the old app. That
requires the same package name **and** the same signing key; if Android refuses the
install with a signature error, export a backup first (or copy the photo folder off the
phone over USB), uninstall, install, then use **Restore** or **Import photos**.
