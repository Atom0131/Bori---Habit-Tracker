# Bori Habit Tracker (Desktop) <img src="docs/pr-flag-celeste.svg" height="22" alt="Puerto Rico flag, celeste" title="Bori, from Borikén">

*Formerly **Rhythm Habit Tracker**. Renamed to Bori in October 2026.*

A Kotlin Multiplatform / Compose Desktop port of [Bori](https://play.google.com/store/apps/details?id=com.apagon.rhythm), an
Android habit tracker, journal, and planner. It covers habits (with a built-in routine manager),
to-dos, calendar events, alarms, timers, reminders, notes and notebooks, and journaling, and it's
local-first with no backend.

**New in 1.1.9:**

- **Works with local AI.** Hook up an assistant that runs on your own computer, such as Hermes with a
  local model via Ollama or LM Studio, or Claude Desktop, Claude Code and other MCP apps, and run your day
  by chat. See [Connect an AI assistant](#connect-an-ai-assistant-optional).
- **Notes as a Markdown vault.** Keep a notebook in sync, both ways, with an Obsidian vault or any folder
  of `.md` files. See [Markdown / Obsidian vault](#markdown--obsidian-vault-optional).
- **Repeats** for events, to-dos and reminders: daily, weekly on chosen days, monthly, yearly, every N,
  until a date or for N times. Edit or skip one date, or the whole series. Syncs with the phone (Android
  1.95.0 and later).
- **Faster, steadier start on Windows.** About 4 seconds every time, where some launches (the first after
  an update, or after Windows shut down with Bori running) used to take half a minute. A splash screen
  now shows the moment you open Bori, on Windows and Linux.
- **To-do alerts on the computer, and an early reminder.** A to-do with a time now rings on the computer
  too (Settings → Notifications & alerts → To-dos), and can also ring 5 minutes to a day before it's due.
  An AI assistant can set that early reminder, and if it asks for something Bori can't do, Bori now says
  so instead of quietly skipping it.
- **No lost alerts.** When a reminder and a timer go off together, both now show, one after the other
  ("Reminder · 1 more"). The alert window also fits its text properly on Windows.
- **Better on Windows:** *Run in background* now hides Bori to the notification-area icon, routine steps
  are read aloud straight away instead of a few seconds late, and the local AI connection's key file is
  readable only by your Windows account.

### Routines inside your habits

Any habit can be broken into steps, and giving a step a time turns the habit into a **timed
routine**. Press ▶ on the habit and Bori walks you through it: a countdown ring for each step, a
chime when one ends and the next begins, pause, +1 min, skip and done, and a summary at the end
("8 of 9 steps · took 12 min · planned 15 min"). Finished steps tick themselves off, so the habit
completes when the routine does.

- **Templates** to start from: Morning routine, Workout, Focus session, Wind down, Meditation.
  Every step and time stays editable, and steps can be reordered or removed.
- **Keeps running in the background.** A card in the sidebar shows the current step and time left
  on every page, and a system notification marks each new step. Closing Bori or restarting the
  computer mid-routine picks up where it left off.
- **Recent runs** for each routine: when, how long against the plan, and how many steps were done.
- **Steps read aloud**, optionally ("Stretch, 5 minutes"). Windows has a voice built in; on Linux
  install `speech-dispatcher` (with `espeak-ng`).
- The same routines run on the phone, and the steps and their times sync between your devices.

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
| Ubuntu, Debian, Mint, Pop!_OS | `bori_1.1.9-1_amd64.deb` | `sudo apt install ./bori_1.1.9-1_amd64.deb` |
| Fedora, openSUSE, RHEL | `bori-1.1.9-1.x86_64.rpm` | `sudo dnf install ./bori-1.1.9-1.x86_64.rpm` |
| Arch, CachyOS, Manjaro, Artix | `bori-1.1.9-1-x86_64.pkg.tar.zst` | `sudo pacman -U ./bori-1.1.9-1-x86_64.pkg.tar.zst` |
| Windows 10 / 11 | `bori-1.1.9.msi` | double-click it |

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
there's no prompt. You can also check any time from **Settings → About → App updates → Check now**.

Coming from 1.0.5 or older? Install the latest version by hand once (below); after that it updates itself.

#### Windows

Download `bori-1.1.9.msi` from the [Releases](../../releases) page and double-click it. It installs
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
curl -LO https://github.com/Atom0131/Bori---Habit-Tracker/releases/download/v1.1.9/bori-1.1.9-1-x86_64.pkg.tar.zst
sudo pacman -U ./bori-1.1.9-1-x86_64.pkg.tar.zst
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

### Battery use on the phone

Sync over Tailscale barely touches the phone's battery. Measured on a Galaxy Z Fold8 with
Android's own battery stats (October 2026), over almost 10 hours off the charger with
**Sync automatically** set to every 15 minutes:

| | Battery used | Share of the phone's total |
|---|---|---|
| Bori, everything (including using the app) | 4.9 mAh | about 0.2% |
| Tailscale | 8.3 mAh | about 0.4% |
| **Both together** | **about 13 mAh** | **well under 1%** |
| The screen alone, for comparison | 339 mAh | about 16% |

The 30 automatic syncs in that time took about 5 seconds each and kept the phone awake for
36 seconds in total, using about 160 KB of data. While Bori is open on the phone it keeps a light
connection to your computers so a computer's **Sync** button can reach it. That connection closes
as soon as you leave the app, so it costs nothing in the background.

Battery tips:
- Leave Tailscale's battery use on *Unrestricted* (see step 1). Samsung's battery manager can
  otherwise turn it off, and sync stops working until you open Tailscale again.
- Bori itself doesn't need *Unrestricted*. Android may skip or cut short a background sync now and
  then to save power, and the next run catches up.
- Choose a longer interval under **Sync automatically** (every hour, or once a day) if you'd rather
  it ran less often. Tapping **Sync** always works straight away.

Two desktop instances can also sync with each other for local testing by launching each with its
own data directory and port:

```bash
java -Dbori.home=/path/to/device-a -Dbori.syncPort=47890 -jar ...
java -Dbori.home=/path/to/device-b -Dbori.syncPort=47892 -jar ...
```

## Markdown / Obsidian vault (optional)

Bori can keep your notes as plain Markdown files, so they open in [Obsidian](https://obsidian.md) or any
editor and can be read by tools on your computer.

1. Open **Settings → Obsidian & AI → Markdown vault** and turn on **Sync a Markdown folder**.
2. Choose your Obsidian vault, or any folder of `.md` files.

- **Folder = notebook.** The folder becomes a notebook in Bori, each subfolder a notebook inside it, and
  each file a note named after the file. Checklists, tables and tags (frontmatter) carry over, and lines you
  didn't touch stay exactly as they were.
- **Both ways, automatically.** Edit in Obsidian and Bori updates within seconds; edit in Bori (or on your
  phone, then sync) and the file updates. If both sides changed, the newer one wins and the other version
  is kept in `vault_conflicts` in your Bori data folder.
- **Deletes are safe.** A note deleted in Bori goes to the vault's `.trash/` folder; a file deleted outside
  Bori sends the note to Recently Deleted for 30 days. If the folder looks empty (for example a drive that
  isn't mounted), Bori stops and changes nothing.
- **Locked notebooks stay private.** A locked notebook is never written to the vault, and locking one
  removes its files.
- The first sync only imports; it doesn't rewrite any of your files.

## Connect an AI assistant (optional)

Bori can work with an AI assistant on the same computer, such as [Hermes](https://github.com/NousResearch/hermes-agent),
Claude Desktop, Claude Code, Cursor, or anything else that supports [MCP](https://modelcontextprotocol.io).
**It works fully offline with a local model:** run Hermes (or another MCP client) on top of
[Ollama](https://ollama.com) or LM Studio and nothing leaves your computer.
The assistant can read your agenda and add or change events, to-dos, reminders, habit check-ins, notes,
timers and alarms ("remind me every day at 8 to take meds", "what's on this week?"). Changes show up in
Bori straight away and sync to your phone like anything else.

1. In Bori, open **Settings → Obsidian & AI** and turn on **Allow local AI agents**.
2. Tap **Connect an AI app**, pick your assistant, and press **Copy**.
3. Paste it where the dialog says (for Hermes, `~/.hermes/config.yaml`; for Claude Code, run the command in a
   terminal), then restart the assistant.

That's all. Nothing else needs installing: the assistant runs Bori itself with `--mcp`, for example
`/usr/bin/bori --mcp`. If you use Hermes with Telegram or Discord, Bori works from those chats too.

- **Private to this computer.** Bori only accepts these requests from programs on the same machine
  (`127.0.0.1`), and only with a secret token kept in your Bori data folder. **New token** in the same
  settings page cuts off anything that had the old one.
- **Asks before deleting.** The assistant is told to get your explicit yes first, and Bori refuses a
  delete that isn't confirmed. Deleted items go to Recently Deleted for 30 days.
- **Bori has to be open** (it can run in the background) for the assistant to reach it.

## License

Apache License 2.0. See [`LICENSE`](LICENSE).
