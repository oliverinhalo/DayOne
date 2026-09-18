# DayOne 2.1

Fixes the shutter, makes the overlay and video pace adjustable, adds a screen-based face
light, and puts the app in shape for a Play Store listing.

## Fixed

- **The shutter button did nothing.** `CameraController.bind(...)` was called with a
  trailing lambda, which bound to its `onError` parameter instead of `onReady`, so
  `cameraReady` never flipped to true and the button stayed disabled for ever. The call
  now names its arguments, and the button is an ordinary `clickable` with a proper
  "Take photo" content description.

## Updating keeps everything

The database schema is byte-identical to 2.0 (same Room identity hash), every new setting
is a preference with a default, and photo paths, package name and signing key are
unchanged. Updating over 2.0 keeps all projects, photos, notes, streaks and settings. A
1.x install still gets the non-destructive 1 → 2 migration.

## Video pace

- Time per photo now has quick presets (50ms / 80 / 100 / 150 / 200 / 330 / 500 / 1s) plus
  a fine-tune slider from 30ms to 1s in 10ms steps, labelled in photos-per-second.
- The Mbps quality picker is gone. Bitrate is derived from the resolution and frame rate,
  which is the only thing that was really being chosen.

## Capture

- **Face light** — the area around the crop frame becomes a soft white panel and the
  screen goes to full brightness, lighting your face in a dim room. Toggle in the top bar,
  brightness adjustable.
- **Guide oval size** is now adjustable on its own, separately from the crop tightness.
- **Overlay size** has a slider as well as the existing pinch gesture.

## Ready to publish

- First-run onboarding: what the app does, terms and privacy acceptance, then camera and
  notification permission priming with reasons. Re-runs if the terms change.
- Terms of use, privacy policy and open-source licences are readable inside the app -
  it has no internet permission, so a hosted page would be unreachable - and mirrored in
  `PRIVACY.md` and `TERMS.md` for the Play listing.
- `USE_EXACT_ALARM` dropped from the manifest: Play restricts it to alarm-clock apps and
  declaring it is grounds for rejection. Reminders use the user-granted
  `SCHEDULE_EXACT_ALARM` and fall back to inexact alarms.
- Releases now also build an `.aab` App Bundle for Play alongside the sideload `.apk`.
- `docs/play-store.md` carries the listing copy, the data-safety answers ("no data
  collected", "no data shared"), permission justifications and a pre-launch checklist.
- Birthday picker starts 18 years ago instead of today.
- Long-press the launcher icon for shortcuts straight into the projects that still need a
  photo today.

---

# DayOne 2.0

A big pass over every part of the app: settings you can actually change, an overlay that
makes lining up the shot easy, a video export that works, and schedules that fit real life.

## Reminders on the days you choose

- **Pick your days.** Every project has its own set of weekdays - every day, Mon-Fri,
  weekends, or anything like Mon/Wed/Sat. Days that aren't scheduled never send a
  reminder.
- **Streaks understand rest days.** A day outside the schedule doesn't count towards the
  streak *and doesn't break it*, so a Mon/Wed/Sat project can run a 200-day streak
  without shooting on Tuesdays.
- **Change the reminder time whenever you like** from the project's settings screen, with
  a proper clock picker instead of plus/minus buttons.
- **A second reminder** later in the day, if mornings don't always work.
- **Repeat interval and cut-off time are yours to set** - repeat every 15 minutes to 3
  hours, and stop at whatever time you choose instead of pestering you until midnight.
- **Notification buttons**: take the photo, snooze an hour, or skip today. A skipped day
  is deliberate, so it keeps the streak intact.
- Reminders re-arm themselves after a reboot, a clock change, a timezone change, and
  after the app updates.

## A far better capture overlay

- **The crop frame is drawn on screen.** The square that actually gets saved is outlined,
  and everything outside it is dimmed, so what you line up is what you get.
- **Five overlay styles**: ghost, outline (edge-detected - by far the easiest to line up
  against), split screen, stripes, or off.
- **Choose the reference photo**: yesterday's, the very first one, or any day you pin from
  the timeline.
- **Move and resize the overlay** with a drag and a pinch, and mirror it if it came from
  the other camera. Your adjustment is remembered.
- Opacity slider on the main screen, plus grids (thirds / fine / centre cross), a head
  guide oval that matches the auto-crop, tap-to-focus, pinch zoom, torch, and a
  self-timer (3/5/10s).
- **Review before saving** - check the shot and retake it before it counts for the day.
  A retake you discard no longer overwrites the photo already saved.

## Video export that actually produces a file

- The exporter was rewritten. It previously drew frames with `Surface.lockCanvas()` on a
  MediaCodec input surface, which is not allowed and cannot produce a valid video; it now
  renders through OpenGL ES on the encoder's surface with proper frame timestamps.
- **Saved where you can get to it**: the finished MP4 is copied to `Movies/DayOne`, so it
  appears in your gallery and Files app, and survives uninstalling the app. Play and
  Share buttons are right there.
- Choose resolution (720/1080/1440), frame rate, time per photo, and quality; optional
  crossfade between days, reverse order, and favourites-only.
- Overlays: day number, date, age, year, per-day notes, project name, and a progress bar.
- Runs in the background with progress and a cancel button - leaving the screen no longer
  kills the export.

## Settings, rebuilt

- A real settings screen: theme (system/light/dark), pure-black dark mode, Material You
  colours, accent colour, capture behaviour, overlay defaults, reminder defaults for new
  projects, and video defaults.
- A per-project settings screen: name, colour, birthday, reminder time and days, repeat
  behaviour, auto-crop and crop tightness, storage used, delete.
- Shortcuts to the system notification and battery-optimisation screens, since those are
  what actually keep reminders on time.

## Not losing your photos

- **Backup to a .zip** in `Documents/DayOne` - every project, photo, note and setting.
  It lives outside the app, so it survives uninstalling.
- **Restore** merges a backup back in: existing days are kept, missing ones filled.
- **Import photos** into a project from your gallery; each photo's date comes from its
  filename or EXIF data, so old photos land on the right days.
- Optional **copy every photo to `Pictures/DayOne`** as you take it.
- The database migration from 1.x is a real migration, so updating keeps every project,
  photo and streak.

## Everywhere else

- Project list shows each project's latest photo, streak, and today's status, with a
  one-tap capture button and a daily progress summary.
- Project home has streak/best/total/completion stats and a month calendar showing shot,
  missed and rest days.
- Timeline is a swipeable pager with notes per day, favourites, share, pin-as-reference
  and delete.
- Archive projects you've paused instead of deleting them.
- Rebuilt on Kotlin 2.0, AGP 8.7, compileSdk 35, Material 3 with dynamic colour.
- Unit tests cover the streak and schedule rules.

Still completely offline: no `INTERNET` permission, no accounts, no tracking.

## One more thing

The app promised to be network-incapable, but 1.x quietly shipped with
`android.permission.INTERNET` merged in from a dependency's manifest (Coil declares it
for remote image loading; DayOne only ever loads files from disk). 2.0 removes it from
the merged manifest, so the app now genuinely cannot reach the network at the OS level,
whatever any library tries.
