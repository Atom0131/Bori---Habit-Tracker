# Bori (Desktop) <img src="docs/pr-flag-celeste.svg" height="22" alt="Puerto Rico flag, celeste" title="Bori, from Borikén">

*Formerly Rhythm. Renamed to Bori in October 2026.*

A Kotlin Multiplatform / Compose Desktop port of [Bori](https://play.google.com/store/apps/details?id=com.apagon.rhythm), an
Android habit tracker, journal, and planner. It covers habits, to-dos, calendar events, alarms,
timers, reminders, notes and notebooks, and journaling, and it's local-first with no backend.

This build runs natively on Linux (and should run on any JVM desktop target). It can optionally
sync with the Android app over a local network or [Tailscale](https://tailscale.com/), with no
account, server, or cloud dependency of any kind.

The desktop build is free and fully featured. There's no paywall or license check here. (The
Android app's own subscription model doesn't apply to this repo.)

## Stack

- Kotlin Multiplatform + Compose Multiplatform (desktop target)
- [Room](https://developer.android.com/kotlin/multiplatform/room) (KMP) for local storage
- [Ktor](https://ktor.io/) for the optional peer-to-peer sync engine (WebSocket, local network only)
- [Koin](https://insert-koin.io/) for dependency injection
- [MaterialKolor](https://github.com/material-foundation/material-color-utilities) for dynamic,
  seed-color-based Material 3 theming

See [`DESIGN.md`](DESIGN.md) for the design system.

## Installing

### Download (recommended for most users)

Grab the installer for your distro from the [Releases](../../releases) page and install it with your
package manager's standard command (or just double-click it in your distro's software installer):

| Distro | Download | Install |
|---|---|---|
| Ubuntu, Debian, Mint, Pop!_OS | `bori_1.0.0-1_amd64.deb` | `sudo apt install ./bori_1.0.0-1_amd64.deb` |
| Fedora, openSUSE, RHEL | `bori-1.0.0-1.x86_64.rpm` | `sudo dnf install ./bori-1.0.0-1.x86_64.rpm` |
| Arch, CachyOS, Manjaro, Artix | `bori-1.0.0-1-x86_64.pkg.tar.zst` | `sudo pacman -U ./bori-1.0.0-1-x86_64.pkg.tar.zst` |

Each package bundles its own Java runtime, so you don't need Java installed separately. Once
installed, Bori is in your app menu, or run `bori` from a terminal. Data is stored under `~/.bori`.
An existing `~/.rhythm` from before the rename is moved there automatically on first launch, with a
`~/.rhythm` link left behind so older photo paths still work.

#### Arch / CachyOS / Artix (pacman)

Download the package from the [Releases](../../releases) page, then install it with `pacman -U`,
pacman's standard command for installing a package file:

```bash
curl -LO https://github.com/Atom0131/Bori---Habit-Tracker/releases/download/v1.0.0/bori-1.0.0-1-x86_64.pkg.tar.zst
sudo pacman -U ./bori-1.0.0-1-x86_64.pkg.tar.zst
```

(Install from the downloaded file rather than passing the URL straight to `pacman -U`: for a URL,
pacman's default config requires a `.sig` signature file, and these packages aren't signed.)

pacman tracks it like any other package: `pacman -Q bori` shows it, installing a newer release the
same way upgrades it, and `sudo pacman -R bori` removes it cleanly. It also replaces an installed
`rhythm` package. Bori isn't on the AUR yet, so there's no `yay -S bori` for now.

Or build the pacman package yourself with the `PKGBUILD` included in this repo:

```bash
sudo pacman -S --needed jdk17-openjdk         # build-time dep (provides jpackage)
git clone https://github.com/Atom0131/Bori---Habit-Tracker.git
cd Bori---Habit-Tracker
makepkg -si                                   # builds bori-<version>-1-x86_64.pkg.tar.zst and installs it
```

Once this `PKGBUILD` gets published to the AUR, `yay -S bori` will work directly.

### Build from source

Requires a JDK with `jpackage` (17+). A stripped JRE like Android Studio's bundled JBR won't work
for packaging, though it's fine for `:composeApp:run`. Building the `.deb`/`.rpm` also needs
`dpkg-deb`/`rpmbuild` on your `PATH` (on Arch/Artix: `sudo pacman -S jdk17-openjdk dpkg rpm-tools`).

```bash
git clone https://github.com/Atom0131/Bori---Habit-Tracker.git
cd Bori---Habit-Tracker
./gradlew :composeApp:run                 # run directly, no packaging
./gradlew :composeApp:packageDeb          # build an installable .deb yourself
./gradlew :composeApp:packageRpm          # build an installable .rpm yourself
./gradlew :composeApp:createDistributable # or just a runnable app-image folder, no installer
makepkg -si                               # or a real pacman package (see Arch/Artix above)
```

Installer output lands in `composeApp/build/compose/binaries/main/{deb,rpm,app}/`.

### Contributing

Fork it, branch, open a PR.

## Syncing with the Android app

Sync is opt-in, peer-to-peer, and manually triggered. Syncing with the phone needs Bori Pro on the Android app; the desktop side is free. There's no background or cloud sync. From
Settings → Data → Sync with Phone, pair by entering the other device's local network or Tailscale
address and tapping Sync. Two desktop instances can also sync with each other for local testing by
launching each with its own data directory and port:

```bash
java -Dbori.home=/path/to/device-a -Dbori.syncPort=47890 -jar ...
java -Dbori.home=/path/to/device-b -Dbori.syncPort=47891 -jar ...
```

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
