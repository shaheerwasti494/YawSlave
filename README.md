📐 Yaw Master & Slave — Bluetooth LE Yaw Sync
A dual-app Android project where two devices communicate over Bluetooth Low Energy (BLE) to share yaw orientation data in real time.
The Master app scans and connects to the Slave app, retrieves yaw data, and calculates the relative orientation between the two devices.

🚀 Features
🔵 BLE communication between two Android devices
🧭 Real-time yaw display using custom DialView widgets
📊 Relative yaw calculation (±180°)
⏱ Latency measurement for BLE packets
📱 Edge-to-edge UI for fullscreen look
🎯 Works across Android 12 to Android 15
📂 Project Structure
yaw-common/ → Shared code & custom views (DialView, AngleUtils, YawPacket, YawProvider) yaw-master/ → Master app (scans, connects, receives data) yaw-slave/ → Slave app (advertises, sends yaw data)

📋 Requirements
Two Android devices with Bluetooth Low Energy support
Android 12+ (permissions differ for Android ≤ 11)
Android Studio (latest stable version)
🔧 Setup & Installation
1️⃣ Clone the repository
git clone https://github.com/shaheerwasti494/yawmasterapp.git

2️⃣ Open in Android Studio

Open the root folder in Android Studio

Wait for Gradle sync to finish

3️⃣ Build the apps

Select the yaw-master module → Run → Install on Device 1 (Master)

Select the yaw-slave module → Run → Install on Device 2 (Slave)


📜 Permissions
Master app (AndroidManifest.xml)
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />



Slave app (AndroidManifest.xml)
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />


⚠ Notes:

On Android 12+, you must request BLUETOOTH_SCAN and BLUETOOTH_CONNECT at runtime.

On Android 11 and below, BLE scanning requires ACCESS_FINE_LOCATION permission.

▶️ How to Use
On the Slave Device:

Install and open Slave app

Grant Bluetooth and Location permissions when prompted

The app will start advertising yaw data

On the Master Device:

Install and open Master app

Grant Bluetooth and Location permissions

The app will scan for the Slave device and connect

Once connected:

Master Dial shows Master device yaw

Slave Dial shows Slave device yaw

Relative Dial shows the difference between the two

Latency (ms) is displayed

🔄 BLE Communication Flow
┌──────────┐        Advertises Yaw Data        ┌──────────┐
│  Slave   │  <──────────────────────────────  │  Master  │
│  Device  │        Scans & Connects           │  Device  │
└──────────┘  ──────────────────────────────▶  └──────────┘
                     Sends Packets

🎨 UI Layout
Master app

Two small dials (Master & Slave yaw)

One large dial (Relative yaw)

Text status bar with yaw values & latency

Slave app

Single dial (Slave yaw)

Connection status indicator

⚠ Troubleshooting

No connection?

Ensure both devices have Bluetooth enabled

Check that permissions are granted on both devices

BLE range is typically 10–30m indoors

Slave not detected?

Restart Bluetooth on both devices

Ensure the Slave app is in the foreground
