# Setup

## 1. Prepare the device

1. Use a spare Android phone or tablet, Android 9 or later.
2. Factory reset it. During setup, **skip adding a Google account**. Device owner can only be set when the device has no accounts.
3. Enable Developer options (tap Build number 7 times) and turn on **USB debugging**.

## 2. Install the app

Download `little-lantern-debug-apk` from the latest **Android CI** run on GitHub, unzip it, then:

```bash
adb install app-debug.apk
```

## 3. Make Little Lantern the device owner

```bash
adb shell dpm set-device-owner org.littlelantern/.lock.AdminReceiver
```

Expected output starts with `Success: Device owner set`. If it says the device already has accounts, remove them in Settings and try again.

## 4. First run

1. Open Little Lantern. Choose a 6-digit parent PIN (entered twice).
2. The app locks itself into Child mode.
3. Enrol a fingerprint in Android Settings **before** step 1 if you want fingerprint unlock; otherwise the PIN is used.

## 5. Add games

1. Install the open-source games you want (for example from F-Droid) while in Parent mode with the lock paused.
2. In Parent mode, switch on each game under **Games allowed in Child mode**.

## 6. Add videos

1. Connect the device to Wi-Fi.
2. Parent mode, then **Videos**. Paste a YouTube video or playlist link and tap **Add and check**.
   - Or: Parent mode, **Pause lock**, open the YouTube app, **Share > Little Lantern**, then go back to Child mode.
3. Wait for each video to download and be checked. Read the result, then **Approve** or **Reject**.

## 7. Feedback to GitHub

1. On GitHub: **Settings > Developer settings > Personal access tokens > Fine-grained tokens > Generate new token**.
2. Repository access: **Only select repositories**, pick this repository.
3. Permissions: **Issues: Read and write**. Nothing else.
4. Copy the token. On the phone: Parent mode, **Feedback**, paste it, **Save GitHub settings**.
5. Write feedback with the **Feedback** button on any screen. Later, Parent mode, **Feedback**, **Send to GitHub**.

The token is stored only in the app's private storage on the phone. Without a token, use **Copy unsent as text**.

## Uninstalling

1. Parent mode, then **Remove device owner (before uninstall)**.
2. Uninstall normally, or:

```bash
adb uninstall org.littlelantern
```

## Known limits (Phase 1)

- The volume-key gesture works only while Little Lantern is on screen. If the child is inside a game, finish or back out of the game first.
- Check on your device that the fingerprint prompt appears while the lock is active; the PIN fallback always works.
