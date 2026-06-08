# VigoTrack

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android)
![Min SDK](https://img.shields.io/badge/minSDK-33-586CD7)
![Target SDK](https://img.shields.io/badge/targetSDK-36-586CD7)
![Kotlin](https://img.shields.io/badge/kotlin-2.3.21-7F52FF?logo=kotlin)
![License](https://img.shields.io/badge/license-TBD-lightgrey)

> Rehabilitation data collection via Polar BLE heart rate sensors.

VigoTrack is an Android application built with Kotlin and Jetpack Compose that connects to Polar BLE (Bluetooth Low Energy) wearable sensors to stream, visualize, and log biometric data during physical therapy and rehabilitation sessions. It supports multiple patients, multiple sensors, and structured therapy stages with CSV export.

---

## Screenshots

<!-- TODO: Add screenshots -->
<!-- ![Stages List](url) -->
<!-- ![Live Session](url) -->
<!-- ![Bilan Grid](url) -->
<!-- ![Settings](url) -->

---

## Features

- **Stage Management** — Organize rehabilitation into named periods with start/end dates
- **Activity Sessions** — Create data-collection sessions with types: MARCHE, APA, HIIT, RENFORCEMENT, PISCINE, TDM6, 10m walk test, REPOS
- **Bilan Grid** — Matrix view of patients vs. assessment types for quick recording
- **Multi-Sensor BLE** — Connect to multiple Polar Pacer Pro (and compatible) devices simultaneously
- **Real-Time Streaming** — Live HR, PPI, Accelerometer (X/Y/Z), and ECG data with Canvas-based mini-graphs
- **Patient Management** — Add/delete patients, pre-link them to sensors with default feature selections
- **Data Export** — Log all streamed data to structured files using Android SAF with a customizable file naming template
- **Foreground Service** — Keeps BLE connections alive and data streaming in the background
- **Dark/Light Theme** — Material 3 with dynamic color support (Android 12+) and brand palette

---

## User Guide

### Requirements

- Android 13+ (API 33)
- A compatible Polar BLE device (e.g., Polar Pacer Pro)

### Installation

Build the APK with Android Studio and sideload, or distribute via your preferred method.

```bash
git clone https://github.com/your-username/vigotrack.git
```

Open in Android Studio, sync Gradle, and run on a connected device.

### Quick Start

1. **Add a Stage** — Tap `+` on the main screen to create a rehabilitation stage
2. **Create an Activity** — Tap a stage, then tap a date to create a new activity session
3. **Connect a Sensor** — Open settings (gear icon) → scan for Polar devices → connect
4. **Link a Patient** — In settings, assign a patient to the connected sensor
5. **Start Streaming** — In the activity session screen, tap a patient card to begin recording
6. **Export Data** — Choose an export folder in settings; CSV files are written automatically

### Stages

Stages represent phases of rehabilitation. Each stage has a name, start date, and end date. They are listed on the home screen ordered by start date (newest first). Tap a stage to view its detail screen showing activities grouped by day.

### Activity Sessions

Each activity belongs to one of two categories:

| Category | Types |
|---|---|
| **Activité** | MARCHE, APA, HIIT, RENFORCEMENT, PISCINE |
| **Bilan** | TDM6 (6-minute walk test), 10m (10-meter walk), REPOS |

Activities can be **Scheduled**, **In Progress**, or **Completed**. You can change the activity type mid-session, which automatically splits the session.

### Bilan Grid

Navigate to the Bilan screen to see a matrix of patients vs. assessment types. Tap any cell to start or record that assessment. Completed assessments show a checkmark.

### Connecting Polar Sensors

Open the settings dialog from any screen via the gear icon. The **Devices** tab lists available Polar sensors. Scans discover nearby BLE devices. Once connected, the connection state transitions through:

`NOT_CONNECTED` → `CONNECTING` → `CONNECTED` → `FEATURES_READY`

Use the **Links** tab to pre-configure patient-sensor-feature associations. Sensors persist across app restarts with auto-reconnection.

### Live Data Visualization

During an active session, each linked patient displays a color-coded card showing:
- **HR** — Beats per minute with mini line chart
- **PPI** — Pulse-to-pulse interval in ms
- **ACC** — Accelerometer magnitude with mini chart (X/Y/Z axes)
- **ECG** — Voltage in µV with mini chart

### Data Export

Streamed data is logged to CSV files using the Android Storage Access Framework. The default file naming template is:

```
{stage}/{patient}/{category}/{activity}_{datetime}/{sensor}_{tag}
```

Available placeholders:

| Placeholder | Description |
|---|---|
| `{stage}` | Stage name |
| `{patient}` | Patient name |
| `{category}` | Activity category |
| `{activity}` | Activity type |
| `{sensor}` | Sensor display name |
| `{device}` | Device address |
| `{tag}` | Data type (HR, PPI, ACC, ECG) |
| `{date}` | Current date (yyyyMMdd) |
| `{time}` | Current time (HHmmss) |
| `{datetime}` | Date + time |
| `{timestamp}` | Unix epoch ms |

Each data type produces a separate CSV file with appropriate headers.

---

## Developer Guide

### Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.3.21 |
| **UI** | Jetpack Compose + Material 3 (BOM 2025.02.00) |
| **Navigation** | Navigation Compose 2.9.8 |
| **Architecture** | MVVM (ViewModel + StateFlow + Repository) |
| **Database** | Room 2.8.4 (KSP) |
| **BLE SDK** | Polar BLE SDK 7.1.0 (JitPack) |
| **Reactive** | RxJava 3, RxAndroid 3, coroutines-rx3 bridge |
| **DI** | Manual (via Application class) |
| **Build** | Gradle KTS, AGP 8.9.1, JDK 17 |

### Setup

```bash
git clone https://github.com/your-username/vigotrack.git
```

Open the project in **Android Studio Ladybug** (or newer). The Gradle sync should download all dependencies automatically. If you encounter SDK issues, ensure `local.properties` points to your Android SDK:

```
sdk.dir=/path/to/Android/Sdk
```

Run on a device or emulator (API 33+). BLE features require a physical device with a Polar sensor.

### Project Structure

```
app/src/main/java/com/maathisv/vigotrack/
├── VigoTrackApplication.kt        # Application class (manual DI)
├── MainActivity.kt                 # Entry point, permissions, NavHost
├── models/                         # Domain models (Patient, Stage, Sensor, etc.)
├── data/
│   ├── VigoTrackDatabase.kt        # Room database (v5)
│   ├── dao/                        # Room DAOs (5 tables)
│   ├── entities/                   # Room entities (6 tables)
│   └── mappers/                    # Entity ↔ Domain mappers
├── repository/
│   ├── SensorRepository.kt         # Polar BLE API wrapper
│   └── ActivityRepository.kt       # Activity CRUD
├── services/
│   └── PolarService.kt             # Foreground LifecycleService
├── ui/
│   ├── navigation/                 # NavGraph + route definitions
│   ├── screens/                    # Screen composables + ViewModel
│   ├── components/                 # Reusable composables
│   └── theme/                      # Color, Theme, Typography
└── util/
    └── DataLogger.kt               # CSV file logger (SAF)
```

### Architecture

The app follows an MVVM pattern with manual dependency injection:

```
UI (Compose) → HomeViewModel → Repository → Room Database / Polar SDK
```

`HomeViewModel` is a shared `AndroidViewModel` injected via the Navigation Graph. It exposes state via `StateFlow`/`SharedFlow`. Repositories are initialized lazily in `VigoTrackApplication` and passed through.

The `SensorRepository` wraps the `PolarBleApi` and manages BLE scanning, connection lifecycle, auto-reconnection, and data streaming. It exposes `SharedFlow`s for each data type (HR, PPI, ACC, ECG).

### Database Schema

| Table | Key Columns | Description |
|---|---|---|
| `stages` | id, name, startDate, endDate | Rehabilitation phases |
| `activities` | id (String UUID), activityType, scheduledDate, startTime, endTime, isRunning, stageId (FK) | Data-collection sessions |
| `activity_links` | linkId, parentActivityId (FK), patientId (FK), patientName, sensorId (FK), features | Links activities to patients/sensors |
| `patients` | id, name, isCalibrated, createdAt | Patient records |
| `sensors` | deviceId (PK), address, name, displayName, lastSeen | Paired Polar devices |
| `sensor_patient_links` | id, patientId (FK), sensorId (FK), features | Pre-configured patient-sensor associations |

### Key Dependencies

All versions are managed via `gradle/libs.versions.toml`. Major dependencies:

- `com.github.polarofficial:polar-ble-sdk:7.1.0` — Polar BLE communication
- `androidx.room:room-*:2.8.4` — Local persistence
- `androidx.compose:compose-bom:2025.02.00` — Compose UI toolkit
- `io.reactivex.rxjava3:rxjava:3.1.12` — Reactive streams
- `androidx.navigation:navigation-compose:2.9.8` — Screen navigation

### Testing

Currently the project includes default template tests (no custom tests) :
- `app/src/test/` — Unit tests (JUnit 4)
- `app/src/androidTest/` — Instrumented tests (Compose UI Test)

---

## Acknowledgments

- **Fondation Ellen Poidatz** — Visual identity / brand color palette
- **Polar** — BLE SDK and wearable hardware

---

## License

<!-- TODO: Choose a license -->
TBD
