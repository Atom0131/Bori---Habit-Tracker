# Rhythm (Desktop)

A Kotlin Multiplatform / Compose Desktop port of [Rhythm](https://play.google.com/store/apps), an
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

Grab the latest `.deb` (Debian, Ubuntu, Mint, and friends) or `.rpm` (Fedora, openSUSE, RHEL, and
friends) from the [Releases](../../releases) page and install it the normal way:

```bash
sudo apt install ./rhythm_<version>_amd64.deb      # Debian/Ubuntu-family
sudo dnf install ./rhythm-<version>.x86_64.rpm      # Fedora/RHEL-family
```

(Or just double-click it in your distro's GUI package installer.) Each package bundles its own
Java runtime, so you don't need Java installed separately. Data is stored under `~/.rhythm`.

#### Arch / Artix (pacman)

Not on the AUR yet, so there's no `yay -S rhythm`. A `PKGBUILD` is included in this repo so you can
build and install a real pacman package yourself:

```bash
sudo pacman -S jdk17-openjdk dpkg rpm-tools   # build-time deps (jpackage + packaging tools)
git clone https://github.com/Atom0131/rhythm-desktop.git
cd rhythm-desktop
makepkg -si                                   # builds rhythm-<version>-1-x86_64.pkg.tar.zst and installs it
```

(`dpkg` and `rpm-tools` aren't actually needed to *run* the app. They're only there to build the
`.deb`/`.rpm` targets elsewhere in this same Gradle config, which `makepkg` shells out to, so it's
harmless to have them installed either way.) Once this `PKGBUILD` gets published to the AUR,
`yay -S rhythm` will work directly. Until then, building it yourself with `makepkg -si` is the
pacman-native path.

### Build from source

Requires a JDK with `jpackage` (17+). A stripped JRE like Android Studio's bundled JBR won't work
for packaging, though it's fine for `:composeApp:run`. Building the `.deb`/`.rpm` also needs
`dpkg-deb`/`rpmbuild` on your `PATH` (on Arch/Artix: `sudo pacman -S jdk17-openjdk dpkg rpm-tools`).

```bash
git clone https://github.com/Atom0131/rhythm-desktop.git
cd rhythm-desktop
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

Sync is opt-in, peer-to-peer, and manually triggered. There's no background or cloud sync. From
Settings → Data → Sync with Phone, pair by entering the other device's local network or Tailscale
address and tapping Sync. Two desktop instances can also sync with each other for local testing by
launching each with its own data directory and port:

```bash
java -Drhythm.home=/path/to/device-a -Drhythm.syncPort=47890 -jar ...
java -Drhythm.home=/path/to/device-b -Drhythm.syncPort=47891 -jar ...
```

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
