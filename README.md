# Little Lantern

A calm, locked-down player for toddlers on Android. Parent-approved slow videos and simple open-source games, with no ads, no autoplay, no feed, and sessions that end on their own.

> Status: **v0.3: streaming only, App pinning lock, in-app games**. See the [roadmap](docs/SPEC.md#roadmap).

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

## Videos: links and playlists only

1. In Parent mode, open **Videos** and paste a YouTube video or playlist link (or use **Share > Little Lantern** in the YouTube app).
2. The app reads the link with [NewPipeExtractor](https://github.com/teamnewpipe/newpipeextractor): direct streams, no ads, no recommendations. **Nothing is downloaded**: videos stream when played (Wi-Fi needed). Playlists add up to 50 videos.
3. **Kid check**, measured on the phone from 6 samples of 20 seconds streamed from across the whole video (kept in memory, then dropped):

| Check | What is measured | Why (see docs/SPEC.md) |
| --- | --- | --- |
| Pace | Scene cuts per minute | Fast pacing lowered self-control right after viewing (Lillard 2011); evidence mixed |
| Brightness jumps | Large brightness changes per minute | Flashes are strong attention grabbers |
| Colour intensity | Average colour saturation | Weak evidence; busy visuals distract (Fisher 2014) |
| Sudden loud sounds | Jumps of 10 dB or more above the typical level, per minute | Part of the overstimulation pattern |
| Length | Minutes | Short videos give natural stopping points |
| Fantastical content | Title and description words (surprise eggs, compilation, superhero, magic...) plus a parent tick box | Fantastical events lowered self-control (Lillard 2015) |

Result: **Calm**, **Check before approving** or **Not recommended**. Thresholds are this project's defaults, not research numbers. The parent approves or rejects every video; nothing reaches Child mode without approval.

## Calm playback

- One video at a time. No controls, no seeking, no autoplay, no queue.
- Colour reduced to 60% and volume capped at 60%. Streams at up to 480p.
- After each video, a still pause screen; "One more" or "All done" appear after 30 seconds.
- Sessions end only when a video finishes, never mid-video, then the same closing screen every time and a rest period.
- Defaults: 60 min a day, 15 min sessions, 60 min rest. The parent can change them.
- Three choices at a time, the same set all week, rotating weekly.

## Feedback while testing

Every screen has a small **Feedback** button (top right). Notes are saved on the phone with the screen name. In Parent mode, **Feedback > Send to GitHub** turns each note into an issue labelled `feedback` plus `bug`, `idea` or `other`. Setup: [docs/SETUP.md](docs/SETUP.md#7-feedback-to-github). Switch the button off in Parent mode when testing is done.

## Phase 1 features

- Runs as **device owner** in **Lock Task Mode**: no home, recents, notifications or other apps.
- Registers as the **home screen**, so a reboot lands back in Little Lantern.
- **Parent gate**: volume x5 + BiometricPrompt; salted PBKDF2 PIN fallback; 5 wrong PINs returns to Child mode.
- Muted colours and desaturated icons by default.

## Setup

You need a **spare Android device (Android 9+) with no Google account added**. Device owner cannot be set on a phone that already has accounts. Steps: [docs/SETUP.md](docs/SETUP.md).

**Download the app:** https://github.com/ajitzonebot/little-lantern/releases/download/latest/little-lantern.apk (rebuilt on every push).

## In-app games

Count, Shapes, Pairs and Paint are built into the app; no other app ever opens. No timers, scores, stars, rewards or wrong-answer sounds. Each game ends on its own after a few rounds, shows a question for the grown-up, and its time counts toward the daily limit.

## Child lock

- **App pinning (any phone):** turn on Android's App pinning with "Ask for PIN before unpinning". Little Lantern pins itself in Child mode; home, back and swipe gestures are blocked and leaving needs the phone PIN. A restart ends pinning; the app pins again when opened. Parent mode shows red until a lock is active.
- **Full lock (spare phone):** device owner via adb, see [docs/SETUP.md](docs/SETUP.md).

## Building locally

Requires JDK 17 and the Android SDK (or Android Studio).

```bash
./gradlew test assembleDebug
```

## License

GPL-3.0. Phase 2 uses [NewPipeExtractor](https://github.com/teamnewpipe/newpipeextractor) (GPL-3.0) to fetch ad-free video streams, so the whole app is GPL-3.0.

Downloading YouTube videos may conflict with YouTube's Terms of Service. This project is intended for personal, private family use.
