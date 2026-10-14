# Bori Habit Tracker (Desktop) <img src="docs/pr-flag-celeste.svg" height="22" alt="Puerto Rico flag, celeste" title="Bori, from Borikén">

*Formerly **Rhythm Habit Tracker**. Renamed to Bori in October 2026.*

A Kotlin Multiplatform / Compose Desktop port of [Bori](https://play.google.com/store/apps/details?id=com.apagon.rhythm), an
Android habit tracker, journal, and planner. It covers habits, to-dos, calendar events, alarms,
timers, reminders, notes and notebooks, and journaling, and it's local-first with no backend.

This build runs natively on Linux and Windows. It can optionally
sync with the Android app (and with your other computers) over [Tailscale](https://tailscale.com/),
a free private network between your own devices, with no Bori account, server, or cloud of any kind.

The desktop build is free and fully featured. There's no paywall or license check here.

**Sync between computers is free, with no limit.** Keep Bori in sync across as many Linux and
Windows computers as you like. **Only syncing with the phone needs a license:** Bori Pro on the
Android app (subscription or lifetime), bought in the phone app. Nothing is ever paid for on the
desktop side.

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

Grab the installer for your distro (or Windows, see below) from the [Releases](../../releases) page and install it with your
package manager's standard command (or just double-click it in your distro's software installer):

| Distro | Download | Install |
|---|---|---|
| Ubuntu, Debian, Mint, Pop!_OS | `bori_1.0.6-1_amd64.deb` | `sudo apt install ./bori_1.0.6-1_amd64.deb` |
| Fedora, openSUSE, RHEL | `bori-1.0.6-1.x86_64.rpm` | `sudo dnf install ./bori-1.0.6-1.x86_64.rpm` |
| Arch, CachyOS, Manjaro, Artix | `bori-1.0.6-1-x86_64.pkg.tar.zst` | `sudo pacman -U ./bori-1.0.6-1-x86_64.pkg.tar.zst` |
| Windows 10 / 11 | `bori-1.0.6.msi` | double-click it |

Each package bundles its own Java runtime, so you don't need Java installed separately. Once
installed, Bori is in your app menu, or run `bori` from a terminal. Data is stored under `~/.bori`.
An existing `~/.rhythm` from before the rename is moved there automatically on first launch, with a
`~/.rhythm` link left behind so older photo paths still work.

#### Updates

From 1.0.6 on, Bori keeps itself up to date. Once a week it checks this repo's Releases for a new
version, and if there is one, a **Bori 1.0.x is ready** card appears at the bottom of the sidebar
with *What's new*, **Update now** and **Later**. Nothing is downloaded until you click Update now.
Bori then downloads the installer for your system, checks it against the release's signed checksums
(it refuses anything that doesn't verify), installs it, deletes the download and reopens on the new
version. On Linux your system asks for your password once, as for any package install; on Windows
there's no prompt. You can also check any time from **Settings → About → Check now**.

Coming from 1.0.5 or older? Install 1.0.6 by hand once (below); after that it updates itself.

#### Windows

Download `bori-1.0.6.msi` from the [Releases](../../releases) page and double-click it. It installs
just for your user, so there's no admin prompt, and adds Bori to the Start Menu and your desktop.
The installer isn't code-signed yet, so Windows SmartScreen may warn about an unrecognised app:
click **More info**, then **Run anyway**. Data is stored in `%APPDATA%\Bori`. To sync with your
phone, install [Tailscale for Windows](https://tailscale.com/download/windows) and allow Bori
through Windows Firewall when it asks. Installing a newer `.msi` upgrades Bori in place; uninstall
it from Settings → Apps.

#### Arch / CachyOS / Artix (pacman)

Open a terminal and run these three commands, one at a time. They download the package into
`~/Downloads` and install it with `pacman -U`, pacman's standard command for installing a package
file (pacman will ask you to confirm; press Enter):

```bash
cd ~/Downloads
curl -LO https://github.com/Atom0131/Bori---Habit-Tracker/releases/download/v1.0.6/bori-1.0.6-1-x86_64.pkg.tar.zst
sudo pacman -U ./bori-1.0.6-1-x86_64.pkg.tar.zst
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

Sync is opt-in and peer-to-peer: the phone, or another computer, connects straight to this
computer over Tailscale. There's no cloud. Sync when you tap Sync, or turn on **Sync automatically**
(every 15 minutes up to once a day) on each device.

| Syncing between | Cost |
|---|---|
| Computer ↔ computer (Linux and Windows, as many as you want) | Free |
| Phone ↔ computer | Needs Bori Pro on the Android app (the desktop side stays free) |

The same steps are in the app under
**Settings → Tutorials → Set up computer sync**.

1. **Install Tailscale on every device** and sign in with the same account on all of them.
   - Android: [Tailscale on Google Play](https://play.google.com/store/apps/details?id=com.tailscale.ipn), then switch it on (a key icon appears in the status bar). On Samsung phones, set Tailscale's battery use to *Unrestricted* so it stays on.
   - Windows: [Tailscale for Windows](https://tailscale.com/download/windows), then sign in from its tray icon.
   - Arch / CachyOS / Manjaro:
     ```bash
     sudo pacman -S tailscale && sudo systemctl enable --now tailscaled
     sudo tailscale up
     ```
   - Other Linux distros:
     ```bash
     curl -fsSL https://tailscale.com/install.sh | sh
     sudo tailscale up
     ```
2. **Open Bori on the computer → Settings → Data Management → Sync with Phone & Computers.** Once Tailscale is up it shows
   *Ready for your phone* with a QR code (Bori notices Tailscale within about 15 seconds; no
   restart needed). On Windows, allow Bori when Windows Firewall asks.
3. **On the phone:** Bori → Settings → Data Management → Sync with Computers → **Scan QR Code**.

That page lists every phone and computer this one syncs with, each with an on/off switch, and
has the same **What to sync** switches as the phone. To sync two computers, use **Add another computer** on one
and enter the other's address. The sync server
only ever listens on the computer's Tailscale address (or `127.0.0.1` when Tailscale isn't
running), never on your local network.

Two desktop instances can also sync with each other for local testing by launching each with its
own data directory and port:

```bash
java -Dbori.home=/path/to/device-a -Dbori.syncPort=47890 -jar ...
java -Dbori.home=/path/to/device-b -Dbori.syncPort=47891 -jar ...
```

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
