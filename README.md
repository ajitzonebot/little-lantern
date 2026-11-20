# Little Lantern

A calm, locked-down player for children aged 3 to 5 on Android. Parent-approved videos (always streamed, never downloaded) and 30 simple in-app games matched to the child's age, with no ads, no autoplay, no feed, no scores, and a 60-minute day that calms down and ends on its own.

> Status: **0.2.0**: streaming player, add-video stream check, 60-minute session, 30 games. Not yet tested on a device. See the [spec](docs/SPEC.md).

## Why

Toddlers get hooked on phones mainly through design and habits, not just content:

- **Autoplay** extends viewing and reduces children's ability to stop on their own (Hiniker et al., CHI 2018).
- **Manipulative design** (lures, rewards, characters urging play) appears in about 80% of apps used by 3 to 5 year olds (Radesky et al., JAMA Network Open 2022).
- **Fantastical, fast-paced content** lowers self-control right after watching (Lillard & Peterson 2011; Lillard et al. 2015).
- **Abrupt, parent-enforced endings** cause tantrums; natural stopping points help (Hiniker et al., CHI 2016).

Little Lantern is built around one rule: **make stopping easy and predictable.** Full evidence table in [docs/SPEC.md](docs/SPEC.md).

## Two modes

| Mode | Who | What it shows | How to enter |
| --- | --- | --- | --- |
| Child mode | Child | Approved videos and games | Default on boot and after every session |
| Parent mode | Parent | Settings, allowed games, maintenance | Press volume up or down **5 times within 3 s**, then **fingerprint** (6-digit PIN fallback) |

Parent mode returns to Child mode after 2 minutes without a touch.

## Features

- Runs as **device owner** in **Lock Task Mode**: no home, recents, notifications or other apps.
- **Parent gate**: volume x5 + BiometricPrompt; salted PBKDF2 PIN fallback.
- **Videos**: parent pastes a YouTube link; the app streams the lowest quality once to check pace, flashes, colour and loudness, and checks SponsorBlock, YouTube flags and keywords for dark patterns. Approved videos always stream.
- **60 minutes a day**: from minute 15, colour, brightness, volume and choices ease down; at 60 the lantern sleeps until tomorrow.
- **30 in-app games**, 10 per age, spoken prompts, no scores.
- Calm, colour-blind-safe palette.

## Setup

You need a **spare Android device (Android 9+) with no Google account added**. Device owner cannot be set on a phone that already has accounts. Steps: [docs/SETUP.md](docs/SETUP.md).

The debug APK is built by GitHub Actions on every push: open the latest **Android CI** run and download `little-lantern-debug-apk`.

## Building locally

Requires JDK 17 and the Android SDK (or Android Studio).

```bash
./gradlew test assembleDebug
```

## License

GPL-3.0. The app uses [NewPipeExtractor](https://github.com/teamnewpipe/newpipeextractor) (GPL-3.0) to fetch ad-free video streams, so the whole app is GPL-3.0.

Downloading YouTube videos may conflict with YouTube's Terms of Service. This project is intended for personal, private family use.
