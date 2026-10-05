# Rhythm (Desktop)

A Kotlin Multiplatform / Compose Desktop port of [Rhythm](https://play.google.com/store/apps),
an Android habit tracker, journal, and planner — habits, to-dos, calendar events, alarms, timers,
reminders, notes/notebooks, and journaling, all local-first with no backend.

This build runs natively on Linux (and should run on any JVM desktop target) and can optionally
sync with the Android app over a local network or [Tailscale](https://tailscale.com/), with no
account, server, or cloud dependency of any kind.

The desktop build is free and fully featured — there's no paywall or license check here. (The
Android app's own subscription model is unrelated to this repo.)

## Stack

- Kotlin Multiplatform + Compose Multiplatform (desktop target)
- [Room](https://developer.android.com/kotlin/multiplatform/room) (KMP) for local storage
- [Ktor](https://ktor.io/) for the optional peer-to-peer sync engine (WebSocket, local network only)
- [Koin](https://insert-koin.io/) for dependency injection
- [MaterialKolor](https://github.com/material-foundation/material-color-utilities) for dynamic,
  seed-color-based Material 3 theming

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full architecture map and
[`DESIGN.md`](DESIGN.md) for the design system.

## Building and running

Requires a JDK (17+ recommended). From the repo root:

```bash
./gradlew :composeApp:run
```

To build a distributable package for your platform:

```bash
./gradlew :composeApp:createDistributable
```

Data is stored under `~/.rhythm` by default.

## Syncing with the Android app

Sync is opt-in, peer-to-peer, and manually triggered — there's no background or cloud sync. From
Settings → Data → Sync with Phone, pair by entering the other device's local network or Tailscale
address and tapping Sync. Two desktop instances can also sync with each other for local testing by
launching each with its own data directory and port:

```bash
java -Drhythm.home=/path/to/device-a -Drhythm.syncPort=47890 -jar ...
java -Drhythm.home=/path/to/device-b -Drhythm.syncPort=47891 -jar ...
```

## License

Apache License 2.0 — see [`LICENSE`](LICENSE).
