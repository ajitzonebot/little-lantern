# Setup

## Quick start: your own phone (App pinning)

1. Install the app from https://github.com/ajitzonebot/little-lantern/releases/download/latest/little-lantern.apk
2. Android Settings, Security (or Security and privacy, More settings), **App pinning**: turn it on, and turn on **Ask for PIN before unpinning**. The menu name differs by phone.
3. Open Little Lantern, choose a parent PIN. In Child mode the app pins itself; tap **Pin** or **Got it** if Android asks.
4. Parent mode shows a green "App pinning is on" card when it works.

Limit: restarting the phone ends pinning. The app pins again when it is opened.

## Full lock: spare phone


## 1. Prepare the device

1. Use a spare Android phone or tablet, Android 9 or later.
2. Factory reset it. During setup, **skip adding a Google account**. Device owner can only be set when the device has no accounts.
3. Enable Developer options (tap Build number 7 times) and turn on **USB debugging**.

## 2. Install the app

Download https://github.com/ajitzonebot/little-lantern/releases/download/latest/little-lantern.apk on the device and open it (allow "Install unknown apps" for your browser), or from a computer:

```bash
adb install little-lantern.apk
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

## 6. Add videos

1. Connect the device to Wi-Fi. Videos stream, so Wi-Fi is also needed while your child watches.
2. Parent mode, then **Videos**. Paste a YouTube video or playlist link and tap **Add and check**.
   - Or: Parent mode, **Pause lock**, open the YouTube app, **Share > Little Lantern**, then go back to Child mode.
3. Wait for each video to be checked (short samples are streamed; nothing is downloaded). Read the result, then **Approve** or **Reject**.

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
