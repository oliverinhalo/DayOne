# Publishing DayOne on Google Play

Everything Play asks for, with the answer already worked out. Nothing here is code — it
is the paperwork side of a release.

## Build the upload artifact

Play takes an App Bundle, not an APK:

```bash
./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab
```

The `Release APK` workflow attaches both the `.apk` (for sideloading) and the `.aab` (for
Play) to every GitHub release.

### Signing

The bundle is signed with `keystore/dayone.keystore`, checked into this repo so that every
build is byte-for-byte replaceable and updates always install in place.

**Before the first Play upload, decide on the key.** Two options:

1. **Play App Signing (recommended).** Upload the bundle and let Google hold the app
   signing key; the checked-in key becomes the *upload* key only. Losing or rotating it is
   then recoverable.
2. **Keep this key as the app signing key.** Then it must be treated as a secret: move it
   out of the repo into a GitHub Actions secret (`base64 -w0 keystore/dayone.keystore`),
   restore it in the workflow, and rewrite `signingConfigs` to read the passwords from
   environment variables. A key published in a repo can be used by anyone to sign an APK
   that claims to be this app.

Sideloading the GitHub release APK is unaffected either way.

## Store listing

**App name:** DayOne — daily photo

**Short description (80 max):**
> One photo a day. Line up with yesterday, keep the streak, watch the year go by.

**Full description:**
> DayOne takes one photo a day and turns a year of them into a thirty-second timelapse.
>
> A ghost or edge-outline of a previous photo sits over the viewfinder so today lines up
> with yesterday, and on-device face detection keeps your face in the same place in every
> frame. Reminders arrive at the time you choose, on the days you choose — Mon–Fri,
> weekends, or any mix — and days you aren't scheduled for never break your streak.
>
> • Unlimited projects: yourself, a group, a room, a garden
> • Overlay styles: ghost, outline, split-screen, stripes
> • Grid guides, self-timer, face light, tap-to-focus, review-before-save
> • Streaks, best-run, completion rate and a month calendar
> • Notes and favourites per day
> • Timelapse export at 720p/1080p/1440p, saved to your gallery
> • Backup and restore everything to a single file
>
> Completely offline. DayOne does not request internet permission, so it cannot send your
> photos anywhere. No account, no analytics, no ads, no tracking.

**Category:** Photography · **Content rating:** Everyone · **Ads:** none ·
**In-app purchases:** none

**Assets needed before upload** (not in this repo):
- Icon 512×512 PNG
- Feature graphic 1024×500 PNG
- 2–8 phone screenshots (capture screen with overlay, project home with calendar,
  timeline, video export, settings)

## Data safety form

Answer **"No data collected"** and **"No data shared"**. Supporting detail if asked:

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | No |
| Is all user data encrypted in transit? | N/A — the app has no network access |
| Do you provide a way to request data deletion? | Data never leaves the device; deleting a project or uninstalling removes it |
| Photos & videos | Stored on-device only, never transmitted |
| Device or other identifiers | Not collected |

Privacy policy URL: link to `PRIVACY.md` in this repository (raw or GitHub Pages). The
same text ships inside the app under Settings → Privacy policy.

## Permissions declarations

| Permission | Why | Play notes |
| --- | --- | --- |
| `CAMERA` | Taking the daily photo | Core function; requested in context |
| `POST_NOTIFICATIONS` | Daily reminder | Requested during onboarding |
| `SCHEDULE_EXACT_ALARM` | Reminder at the exact minute chosen | User-granted; app falls back to inexact alarms when denied |
| `RECEIVE_BOOT_COMPLETED` | Re-arm reminders after a reboot | Standard |
| `VIBRATE` | Notification vibration | Standard |
| `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28) | Save exports on Android 9 and older | Legacy only; Android 10+ uses MediaStore |

`USE_EXACT_ALARM` is deliberately **not** declared — Play restricts it to alarm-clock and
calendar apps, and declaring it here would risk rejection. No `INTERNET` permission is
declared, and it is explicitly removed from the merged manifest.

## Pre-launch checklist

- [ ] `versionCode` bumped above the last upload
- [ ] `./gradlew :app:testDebugUnitTest :app:lintDebug` clean
- [ ] `./gradlew :app:bundleRelease` succeeds
- [ ] Terms and privacy text in `LegalText.kt` match `TERMS.md` / `PRIVACY.md`
- [ ] Onboarding shows, terms acceptance sticks across a restart
- [ ] Tested on a real device: capture, reminder firing, video export, backup + restore
- [ ] Screenshots and graphics prepared
- [ ] Data safety form filled in as above
- [ ] Decide on Play App Signing vs. self-managed key (see Signing)
