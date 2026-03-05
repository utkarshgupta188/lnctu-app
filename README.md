# LNCT Attendance Fetcher

A tiny Compose Android app that fetches and shows your attendance using your own API endpoint.

Default endpoint used:

```
https://lnctu.vercel.app/attendance?username=&password=
```

It expects a JSON response shaped like:

```
{
  "success": true,
  "message": "Logged in and fetched",
  "data": {
    "total_classes": 309,
    "present": 238,
    "absent": 71,
    "percentage": 77.02,
    "overall_percentage": 77.02,
    "attended_classes": 238
  }
}
```

## Features
- Simple UI with fields for base URL, username, and password
- HTTPS by default; cleartext allowed for local/dev (can be tightened later)
- Parses and displays totals and percentages from the JSON response
- ViewModel + coroutines for background network calls

## How to run
- Open the project in Android Studio and Run on a device or emulator
- Or build a debug APK via Gradle and install to a connected device

## How to use
1. Launch the app
2. Leave the Base URL as-is or change it to your own (must accept `username` and `password` query params)
3. Enter username and password
4. Tap "Fetch Attendance"
5. The result section will show totals and percentages if the API returns `success: true`, otherwise it shows the API error message

## Customize
- UI: `app/src/main/java/com/meow/lnctattendance/MainActivity.kt`
- Networking/Parsing: `app/src/main/java/com/meow/lnctattendance/AttendanceViewModel.kt`
- Manifest & permissions: `app/src/main/AndroidManifest.xml`

If your API changes shape, adapt the parser in `parseAttendance` (same file as the ViewModel) and the UI fields in `AttendanceResult`.

## Notes
- INTERNET permission is included
- `usesCleartextTraffic` is enabled for development convenience. If your API is HTTPS-only, you can set it to `false`.
- No credentials are stored; they are only used to call the endpoint.

## Architecture
Android App → Open-source API → LNCTU portal

The API fetches attendance data using the user's session token.

API repository:
https://github.com/utkarshgupta188/lnctu

## Disclaimer
This project is not affiliated with LNCT University.
