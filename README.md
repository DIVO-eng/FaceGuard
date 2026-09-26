# FaceGuard

A privacy-first Android app that snaps a front-camera photo when a lock-screen
unlock attempt fails, so the phone owner can review who tried to access their
device.

## How it actually works (read this first)

Android does **not** let any third-party app secretly watch the system lock
screen, read a PIN/pattern/password, or hook into biometric hardware. FaceGuard
uses the one legitimate mechanism that exists for this:

- **`DeviceAdminReceiver.onPasswordFailed()`** — fires on any failed PIN,
  pattern, or password attempt. It never reveals what was typed — just "an
  attempt failed, right now." This is the same public API used by MDM and
  "Find My Device"-style apps, not an exploit or accessibility-service abuse.
- **`onPasswordSucceeded()`** covers successful PIN/pattern/password unlocks
  only. **Android does not forward successful biometric (fingerprint/face)
  unlocks to any third-party app**, period — there's no workaround for that
  without root, and this project deliberately doesn't go there. The Settings
  screen states this plainly instead of pretending otherwise.

Because of this, activating FaceGuard requires the owner to grant **Device
Admin** rights in Android Settings, in addition to Camera and Notification
permissions — all requested explicitly during onboarding, never silently.

## Project structure

```
app/src/main/java/com/faceguard/app/
├── admin/FaceGuardDeviceAdminReceiver.kt   # the event source
├── camera/                                 # silent front-camera capture + on-device face match (ML Kit)
├── data/                                   # Room (SQLCipher-encrypted) + encrypted photo storage
├── service/MonitorService.kt               # foreground service tying it together
├── ui/                                     # Compose: consent, dashboard, settings, gallery
└── util/                                   # Keystore helpers, encrypted prefs, WorkManager scheduling
```

## To build it yourself

1. Open this folder in Android Studio (Koala+ recommended).
2. Let it sync Gradle — it will pull CameraX, ML Kit Face Detection,
   SQLCipher, Jetpack Security, and Biometric from Maven automatically.
3. Run on a device or emulator with API 26+.
4. A couple of integration points are deliberately left as stubs for you to
   fill in with your own credentials/keys:
   - `CloudBackupClient.uploadEncrypted()` in `RetentionScheduler.kt` — wire
     in Google Drive or Dropbox's SDK here.
   - Face enrollment UI — `FaceEnrollmentStore` exists, but there's no
     "enroll my face" screen yet; add one that runs `FaceMatcher` once on a
     selfie and calls `saveEnrolledLandmarks()`.
   - App icon / `mipmap` launcher icons aren't included — Android Studio's
     Image Asset tool can generate these in a couple of clicks.

## Honest limitation of the face-matching

The on-device "known vs. unknown face" check uses ML Kit's bundled face
detector plus a simple landmark-distance heuristic — not a dedicated
face-recognition model. It's the same rough tier of accuracy other consumer
intruder-detection apps ship with, not bank-grade biometric matching. This is
disclosed in-app rather than oversold.

## Publishing to Google Play — what to expect

This exact app category exists on Play today (e.g. "Intruder Selfie"), so
it's approvable, but a few things Google will specifically check:

1. **Device Admin justification.** Play's console requires a form explaining
   why your app needs Device Admin, plus in-app disclosure — the consent
   screen here is written to satisfy that.
2. **Camera + background use disclosure.** Since the camera fires from a
   background service, your Play Data Safety form must declare camera data
   collection, on-device processing, and (if you enable it) cloud transfer.
3. **Prominent disclosure for sensitive permissions.** The consent screen
   must appear *before* requesting Camera/Device Admin — already the flow
   here (`ConsentScreen` → permission prompts).
4. **Privacy policy URL** is mandatory for any app requesting Camera +
   Device Admin. You'll need to host one (a simple static page describing
   the same points as the consent screen works).
5. **Foreground service policy (Android 14+).** The manifest already declares
   `foregroundServiceType="specialUse"` with a `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`
   description, which Play now requires you to justify in the console too.
6. Expect Play's automated + human review to take longer than average for
   apps requesting Device Admin — budget a few extra days before a hard
   launch date.

## What's NOT included (by design)

- No accessibility-service tricks, root access, or hidden background
  surveillance of any kind.
- No sending of photos to any third-party facial-recognition cloud service.
- No silent uploads — backup is off until the owner turns it on and picks a
  provider.
