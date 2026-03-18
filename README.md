# LipApp

Android IRC client for [Lipservice](../lipservice), built with Kotlin and Jetpack Compose.

This is the Android counterpart of [lip2](../lip2), a GTK4 IRC client. Both
communicate with a Lipservice bouncer via its REST + SSE API rather than
speaking the IRC protocol directly.

## Features

- Multi-network IRC via Lipservice bouncer
- Channel and private message support
- Real-time message delivery via Server-Sent Events
- IRC text formatting (bold, italic, underline, colors) — rendering and input
- Nick mention highlighting
- Clickable URL detection
- Message search (local text + server-side)
- Unread tracking with visual indicators
- Session persistence (current view and read pointers saved server-side)
- Dark mode toggle
- Material 3 / Material You design

## Architecture

- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Retrofit** + **OkHttp** for REST API
- **SSE** via manual OkHttp streaming (no extra library)
- **Hilt** for dependency injection
- **DataStore** for local preferences
- **MVVM** with `ViewModel` + `StateFlow`
- **kotlinx.serialization** for JSON

## Building

Open the `lipapp` directory in Android Studio, or build from the command line:

```
./gradlew assembleDebug
```

## Configuration

On the login screen, enter:

- **Server URL** — the Lipservice API endpoint (e.g. `http://192.168.1.5:8080/api/`)
- **Username** / **Password** — Lipservice credentials

The URL and username are saved locally for next launch. The default URL
points to `10.0.2.2:8080` (Android emulator alias for host localhost).
