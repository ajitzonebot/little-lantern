# Little Lantern: product spec

Little Lantern is a locked-down Android app for children aged about 3 to 5. It plays only videos the parent added, has 30 simple in-app games matched to the child's age, and stops after 60 minutes a day.

Scope: personal use on one dedicated family device. Findings below come from abstracts, university releases and news coverage; not every full paper was read.

## What the child gets

| Area | Behaviour |
| --- | --- |
| Videos | Only parent-approved videos. Always streamed, never downloaded. No autoplay, no next video, no suggestions. A still "all done" card at the end, then home. |
| Games | 10 in-app games for the child's age (3, 4 or 5), plus younger ones the parent leaves on. No scores, stars, levels or timers. Every prompt is spoken. |
| Daily limit | 60 minutes of Child mode a day, the maximum. A video that would run past the hour cannot start. At 60 minutes the same "lantern is sleeping" screen shows every day, with no countdown. |
| Calming | From minute 15 to 60: colour drains to grey (Android 12+), screen brightness falls to 65%, volume to 50% of the parent's cap, video choices shown drop from all to 4 to 2. |

## Research basis

| Factor | Key finding | Evidence | App response |
| --- | --- | --- | --- |
| Autoplay | Extended viewing, reduced self-regulation (Hiniker 2018) | Strong, experimental | No autoplay, no queue |
| Manipulative design | 80% of 160 apps used by 3 to 5 year olds had lures, time pressure or ad pressure (Radesky 2022) | Strong, descriptive | No rewards, streaks, characters urging play, ads; online dark-pattern check for videos |
| Fast pacing | Lowered executive function right after 9 min of a show cutting every 11 s vs 34 s (Lillard 2011) | Mixed | Stream check measures cuts per minute |
| Abrupt endings | 2-minute warnings worsened reactions; natural stopping points helped (Hiniker 2016) | Suggestive | End at video end; no countdown |
| Saturation and arousal | Saturation raised arousal; brightness raised pleasure (Valdez & Mehrabian 1994, adults) | Moderate | Light, low-saturation screens; colour only on small pieces |
| Colour blindness | Up to 5.6% of boys aged 3 to 6 (MEPEDS 2014) | Strong | Okabe-Ito-based piece colours, outlines, shape as a second cue, colour-blind mode |
| Grayscale | Grayscale phones cut adult screen time by about 20 to 50 min/day | Moderate, adults only | Progressive desaturation (a hypothesis for children) |

## Adding a video (Parent mode)

1. Paste a YouTube link.
2. NewPipeExtractor (GPL-3.0) resolves the streams. No ads, no recommendations.
3. The check streams the lowest-quality version once and decodes it in memory; nothing is written to storage:
   - cuts per minute (colour-histogram change between frames), labelled against Lillard's 1.8 (slow) and 5.5 (fast);
   - flashes and saturated-red flashes, more than 3 per second flagged (broadcast/WCAG idea, whole-frame approximation);
   - average colour saturation (rough);
   - sudden loudness jumps (15 dB over the previous 3 s).
4. Online dark-pattern checks:
   - SponsorBlock "like and subscribe", sponsor and self-promo segments (hash-prefix lookup); found segments are skipped during playback;
   - YouTube Data API paid product placement and made-for-kids flags (needs the parent's API key);
   - a title and description keyword scan.
5. The parent ticks "mostly fantasy" or "urges to keep watching" if needed, then approves or rejects. Unknown results are never shown as a pass.

Thresholds are design choices anchored on the studies above, not validated cut-offs.

## Games (10 per age)

| Age 3 | Age 4 | Age 5 |
| --- | --- | --- |
| Shape fit | Number path 1 to 10 | Number path 1 to 20 |
| Picture puzzle, 2 to 4 pieces | What comes next? AB | Patterns ABC and AABB |
| Colour sort, 2 colours | Count 1 to 5 | Switch sort: colour, then shape |
| How many? 1 to 3 dots | Sort by shape, then size | Count to 10, match numerals |
| Big or small | Story order, 3 pictures | Drum the syllables |
| Find pairs, 4 cards | Rhyme match | First sound |
| Feelings: happy or sad | Find the colour | Trace letters |
| Finger paint | Build a face | Picture puzzle, 9 pieces |
| Listen and tap | Feelings: what helps? | Sink or float |
| Move like animals (screen rests) | Hop and balance (screen rests) | Move and freeze (screen rests) |

Basis per game is shown in Parent mode: Study (published study), CDC (CDC milestone for that age) or Design (our judgment). Strongest: the linear number board game (Siegler and Ramani, randomized trials). Age placement follows CDC milestones and the card-sort task (most 3-year-olds cannot switch rules; most 4 and 5 year olds can).

## Palette

Background #F4F1EA, surface #FBF9F4, ink #2F2F2B (11.9:1), soft ink #5F5A50 (6.1:1), sage #5B6B5D. Game pieces: blue #2585BA, orange #DA7423, sky #6EBDE9, pink #D28BB1 (Okabe-Ito, softened 15%), each with a #5F5A50 outline. End screen: dim warm grey #57524A, not black.

## Roadmap

1. Lockdown shell: done.
2. Streaming player, stream check, 60-minute session with calming, 30 games: done (0.2.0).
3. App pinning on any phone, in-app feedback to feedback.md, fixed APK link and signing key: this release (0.3.0).
4. Next: test on a device, tune thresholds from real videos, fix whatever feedback.md collects.

## Validation plan

Two-week baseline, then four weeks with the app. Track daily minutes, hand-over reaction (calm, upset, tantrum), phone requests, escape attempts, voluntary stops.
