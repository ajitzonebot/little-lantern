# Little Lantern: product spec

Little Lantern is a locked-down Android app for children aged about 2 to 5. It shows only parent-approved, slow, real-world videos and calm games, then ends sessions predictably so stopping is easy.

Scope: personal use on one dedicated family device. Findings below come from abstracts, university releases and news coverage; not every full paper was read.

## Research basis

| Factor | Key finding | Evidence | App response |
| --- | --- | --- | --- |
| Autoplay | Extended viewing, reduced self-regulation, more parent intervention; a one-video lockout alone did not cut viewing (Hiniker 2018, 24 families) | Strong, experimental | No autoplay, no queue |
| Manipulative design | 80% of 160 apps used by 3 to 5 year olds had lures, time pressure or ad pressure (Radesky 2022) | Strong, descriptive | No rewards, streaks, characters urging play, ads |
| Fantastical content | 10 to 20 min lowered executive function in 4 year olds (Lillard 2015); review of 15 studies: consistent negative effect on inhibitory control (Namazi 2024) | Moderate, short-term | Prefer real-world content |
| Fast pacing | Lowered executive function right after 9 min (Lillard 2011); 2024 review: effects on attention contradictory | Mixed | Score and cap cuts per minute |
| Devices used to calm | Linked to higher emotional reactivity and lower executive function later, 422 children (Radesky 2022) | Moderate, observational | Calm-down mode that hands back to parent |
| Abrupt endings | 2-minute warnings worsened tantrums; natural stopping points and device-initiated endings helped (Hiniker 2016) | Suggestive | End at video end, app ends session, fixed closing routine |
| Visual clutter | Decorated classroom: 38.6% vs 28.4% off-task (Fisher 2014) | Moderate, not screens | Desaturate, simple scenes |
| Bright colour alone | No study shows harm | None | Low priority |

## Screen time targets

| Age | WHO 2019 | AAP 2016 | App default |
| --- | --- | --- | --- |
| Under 1 | No screen time | Avoid except video chat (under 18 to 24 months) | Not for use |
| 2 to 5 | 1 hour or less (ages 2 to 4) | 1 hour a day of high-quality content, co-viewed | 60 min/day max |

In February 2026 the AAP replaced fixed limits with a "5 Cs" framework (Child, Content, Calm, Crowding out, Communication).

## Design principles

- One thing at a time: one video or one game, then stop.
- Never work against the child's decision to stop: no autoplay, lures, rewards, streaks.
- The app ends the session at a natural stopping point, with no countdown warning.
- Real-world and slow over fantastical and fast.
- Calm visuals and sound by default. Same closing routine every time.

## Progressive boredom engine (Phase 4)

| Lever | Session start | Session end |
| --- | --- | --- |
| Saturation | 60% | 20% |
| Volume | Parent cap | 50% of cap |
| Pause between videos | 30 s | 3 min |
| Choices shown | 6 | 2 |

Across weeks: daily budget falls by a parent-set step to a floor; library rotates weekly; replays capped per day. These are design hypotheses, not tested interventions.

## Adding videos (Phase 2 and 3)

1. Parent pastes a YouTube link in Parent mode.
2. App resolves the stream with NewPipeExtractor (GPL-3.0, no ads, no recommendations) and downloads it to app storage.
3. App scores it on the device: cuts per minute, colour saturation, loudness spikes, length; parent ticks "fantastical".
4. Parent approves or rejects.

## Roadmap

1. **Lockdown shell** (this release): device owner, Lock Task Mode, home launcher, volume x5 + fingerprint + PIN gate, Child and Parent mode, allowlisted games.
   Gate: child cannot exit in 10 minutes of trying; reboot returns to the app.
2. **Calm player**: offline allowlist, thumbnail picker, no autoplay, pause screen, saturation filter, natural-end session, closing routine.
3. **Curation**: add by link with NewPipeExtractor, on-device scoring, score card, budgets, dashboard, mood log.
4. **Games and boredom engine**: in-app calm games (counting, shape sort, letter trace, match pairs, picture book) and boredom levers.

## Validation plan

Two-week baseline, then four weeks with the app. Track daily minutes, hand-over reaction (calm, upset, tantrum), phone requests, escape attempts, voluntary stops.
