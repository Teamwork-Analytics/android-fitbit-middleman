# Android Fitbit Middleman

A Android application that received data from Fitbit companion and forward to main server hosted on pc.

## Overview

This application acts as a middle-man connecting Fitbit and PC Main Server. It is built for major purposes below:

1. To received data from Fitbit companion on the _SAME_ Android device (i.e. Google Pixel phone).
2. To forward the data received to pc main server.

3. The reason of having this application is due to the restriction that Fitbit companion can only do POST request
   to the same device (phone in our case), or a non-self-signed https url. Hence, this Android app helps to achieve the communication
   between Fitbit and PC server.

## How to install

To install the application on the devices (phones), connect the phone to the computer that has this project opened in
Android Studio. On the top right of Android Studio, select the devices connected, and click `run app` button (green triangle).
After running the app, Android studio should install a dev version of the app in the phone. The app is called `System Fitbit Connector` on
the device.

## Configuration

> [!Tip]
> Do a search of `!!ChangeMe` in `MainActivity` you can find all the variable that might need to be changed.

> [!TIP]
> The app is designed to update the destination IP address automatically, [`fitbit-receiver`](https://github.com/Teamwork-Analytics/teamwork-visualiser-dashboard/tree/main/fitbit-receiver) is the server that updates the IP. In the occasion that it is not forwarding to the right IP, try closing the app (from running in the background) and restart the app.

1. The app will update/resolve the IP address using a serverless API hosted on vercel. If the IP address shown is not correct, click on the refresh button.
2. Each device's colour/role can be change on the text field in the app. Please make sure it is correct, the `fitbit-receiver` will record incoming data according to the role name.

## How to run

After installation, find the app on the phone to open it. The application should start working once opened.

## Local backup

The app has a local backup feature implemented. It will save the heart rate data in the app directory. The data will only be kept for 5 days (duration can be changed in the code). Follow the instruction below to access the backup data.

The app is writing backups to:

```
/sdcard/Android/data/com.example.systemfitbitconnector/files/backups/
```

(each day: `incoming-YYYYMMDD.jsonl`)

---

### For macOS

#### 1) Install ADB

Option A (easiest):

```bash
brew install --cask android-platform-tools
```

Option B (manual): download Google’s “Platform-Tools” zip, unzip it, and use the `platform-tools` folder. (No driver needed on macOS.)

#### 2) Enable USB debugging on the phone

- On the phone: **Settings → About phone → Build number** → tap 7 times to enable Developer options.
- Back to **Settings → Developer options → USB debugging** → turn ON.
- Connect the phone via USB, choose **File Transfer** mode if prompted.
- When the “Allow USB debugging?” prompt appears, tap **Allow**.

#### 3) Verify ADB sees the device

```bash
adb devices
```

You should see your device listed as `device` (not `unauthorized`).

#### 4) List your backup files (optional check)

```bash
adb shell ls -l /sdcard/Android/data/com.example.systemfitbitconnector/files/backups
```

#### 5) Copy backups to your Mac

- Copy the whole folder:

```bash
mkdir -p ~/Desktop/fitbit-backups
adb pull /sdcard/Android/data/com.example.systemfitbitconnector/files/backups ~/Desktop/fitbit-backups
```

- Or copy a single day:

```bash
adb pull /sdcard/Android/data/com.example.systemfitbitconnector/files/backups/incoming-20250929.jsonl ~/Desktop/
```

> Tip: `/sdcard/` and `/storage/emulated/0/` are equivalent. If one path fails, try the other:
>
> ```
> adb pull /storage/emulated/0/Android/data/com.example.systemfitbitconnector/files/backups ~/Desktop/fitbit-backups
> ```

---

### For Windows

#### 1) Install ADB

Option A (manual, official): download the **Platform-Tools** zip from Google, unzip to e.g. `C:\platform-tools`.
Option B (package manager): in an **Administrator** PowerShell:

```powershell
choco install adb -y
```

(If you used the manual zip, run commands from `C:\platform-tools` or add that folder to your PATH.)

#### 2) USB driver & debugging

- Install your phone’s USB driver if Windows doesn’t recognize it (most devices work out of the box; some vendors need drivers).
- On the phone: enable **Developer options** and **USB debugging** (same steps as macOS).
- Connect via USB, choose **File Transfer** if asked, and **Allow** the debugging prompt.

#### 3) Verify ADB sees the device

Open **PowerShell** (or Command Prompt) and run:

```powershell
adb devices
```

Your device should show as `device`.

#### 4) List your backup files (optional)

```powershell
adb shell ls -l /sdcard/Android/data/com.example.systemfitbitconnector/files/backups
```

#### 5) Copy backups to your PC

- Copy the whole folder:

```powershell
mkdir $HOME\Desktop\fitbit-backups
adb pull /sdcard/Android/data/com.example.systemfitbitconnector/files/backups $HOME\Desktop\fitbit-backups
```

- Or copy a single file:

```powershell
adb pull /sdcard/Android/data/com.example.systemfitbitconnector/files/backups/incoming-20250929.jsonl $HOME\Desktop\
```

> If you get a path error, try the alternate prefix:
>
> ```
> adb pull /storage/emulated/0/Android/data/com.example.systemfitbitconnector/files/backups %USERPROFILE%\Desktop\fitbit-backups
> ```

---

### Common issues

- **Device shows “unauthorized”** in `adb devices`: unplug/replug USB, unlock phone, accept the **Allow USB debugging** fingerprint dialog, then run `adb devices` again.
- **Permission denied / file not found**: confirm the exact path using:

  ```bash
  adb shell 'ls -l /sdcard/Android/data/com.example.systemfitbitconnector/files/backups'
  ```

- **Nothing listed yet**: make sure the app has actually received/backup’d some payloads (your code writes on each forward).
- **MTP/Finder can’t see `Android/data`**: that’s expected on modern Android; use **ADB** as shown above.

## Support

The app is last maintained by Jay, as of October 2025, feel free to contact him if needed :))
