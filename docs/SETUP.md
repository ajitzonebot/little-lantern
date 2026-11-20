# Setup

Two ways to lock the app:

| | App pinning | Device owner |
| --- | --- | --- |
| Phone | Any Android 9+ phone, accounts allowed | Spare phone, factory reset, no Google account |
| Child can leave | No, unless they know the phone PIN | No |
| After a restart | Not pinned until the app is opened again | Still locked |
| Setup | One Android setting | adb from a computer |

## 1. Install the app

On the phone, open https://github.com/ajitzonebot/little-lantern/releases/download/latest/little-lantern.apk and install it (allow "Install unknown apps" for the browser). The link always has the newest build. Newer builds install over older ones.

Or from a computer:

```bash
adb install -r little-lantern.apk
```

## 2a. App pinning (any phone)

1. In Settings, search **App pinning** (some phones: **Screen pinning**). Turn it on, and turn on **Ask for PIN before unpinning**.
2. Open Little Lantern. When it enters Child mode the phone asks to pin the app; accept.
3. Parent mode shows the lock state and has **Open App pinning settings**.

## 2b. Device owner (spare phone, full lock)

1. Factory reset. During setup, **skip adding a Google account**.
2. Enable Developer options (tap Build number 7 times) and turn on **USB debugging**.
3. Run:

```bash
adb shell dpm set-device-owner org.littlelantern/.lock.AdminReceiver
```

Expected output starts with `Success: Device owner set`. If it says the device already has accounts, remove them in Settings and try again.

## 3. First run

1. Enrol a fingerprint in Android Settings first if you want fingerprint unlock; otherwise the PIN is used.
2. Open Little Lantern. Choose a 6-digit parent PIN (entered twice).
3. The app goes to Child mode and locks.
4. Parent mode: press a volume key 5 times within 3 seconds, then fingerprint or PIN.
5. In Parent mode set the child's birth month (picks the games) and add videos.

## 4. Feedback to feedback.md

1. On GitHub: **Settings > Developer settings > Personal access tokens > Fine-grained tokens > Generate new token**.
2. Repository access: **Only select repositories**, pick this repository.
3. Permissions: **Contents: Read and write**. Nothing else.
4. On the phone: Parent mode, **Feedback** section, paste the token, set the branch if not the default, **Save GitHub settings**.
5. Tap **Feedback** (top right of any screen), write the note, **Save**. The app adds it to `feedback.md` under that screen's heading as one commit. If sending fails, notes stay on the phone; **Send unsent** in Parent mode retries.

The token is stored only in the app's private storage. Anyone with the token can change files in the repository, so use a token limited to this one repository and revoke it after testing.

## Uninstalling

App pinning: unpin (Parent mode, **Pause lock until Child mode**), then uninstall normally.

Device owner:

1. Parent mode, then **Remove device owner (before uninstall)**.
2. Uninstall normally, or:

```bash
adb uninstall org.littlelantern
```

## Known limits

- The volume-key gesture works only while Little Lantern is on screen.
- Check on your device that the fingerprint prompt appears while the lock is active; the PIN fallback always works.
- App pinning ends on restart; the app pins itself again when opened, but the phone asks to confirm each time.
