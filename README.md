# Little Lantern

A calm, locked-down player for toddlers on Android. Parent-approved slow videos and simple open-source games, with no ads, no autoplay, no feed, and sessions that end on their own.

> Status: **Phase 1 (lockdown shell)**. Video playback arrives in Phase 2. See the [roadmap](docs/SPEC.md#roadmap).

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

## Phase 1 features

- Runs as **device owner** in **Lock Task Mode**: no home, recents, notifications or other apps.
- Registers as the **home screen**, so a reboot lands back in Little Lantern.
- **Parent gate**: volume x5 + BiometricPrompt; salted PBKDF2 PIN fallback; 5 wrong PINs returns to Child mode.
- **Allowlisted games**: the parent picks which installed apps the child may open; only those can run inside the lock.
- Muted colours and desaturated icons by default.

## Setup

You need a **spare Android device (Android 9+) with no Google account added**. Device owner cannot be set on a phone that already has accounts. Steps: [docs/SETUP.md](docs/SETUP.md).

The debug APK is built by GitHub Actions on every push: open the latest **Android CI** run and download `little-lantern-debug-apk`.

## Suggested open-source games

Install from F-Droid, test each one yourself, then allow it in Parent mode.

| App | Type | License |
| --- | --- | --- |
| [12345 - Learn Counting](https://f-droid.org/en/packages/eu.quelltext.counting/) | Counting pictures | AGPL |
| [BabbyPaint](https://f-droid.org/en/packages/dev.alexjyong.babbypaint/) | Simple painting | GPL-3.0 |
| [KidOrb](https://f-droid.org/en/packages/com.wian.kidorb/) | Calm colour painting | GPL-3.0 |
| [Tux Paint](https://f-droid.org/packages/org.tuxpaint/) | Drawing, ages 3 to 12 | GPL-2.0+ |
| [GCompris](https://apps.kde.org/gcompris/) | 100+ activities, ages 2 to 10 | AGPL-3.0 |

Check before allowing: no ads, no links to a browser or store, no rewards or timers, no settings the child can reach.

## Building locally

Requires JDK 17 and the Android SDK (or Android Studio).

```bash
./gradlew test assembleDebug
```

## License

GPL-3.0. Phase 2 uses [NewPipeExtractor](https://github.com/teamnewpipe/newpipeextractor) (GPL-3.0) to fetch ad-free video streams, so the whole app is GPL-3.0.

Downloading YouTube videos may conflict with YouTube's Terms of Service. This project is intended for personal, private family use.
