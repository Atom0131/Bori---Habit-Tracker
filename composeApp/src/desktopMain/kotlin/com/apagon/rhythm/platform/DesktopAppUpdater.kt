package com.apagon.rhythm.platform

import com.apagon.rhythm.core.app.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.util.Base64
import java.util.Properties

/**
 * Keeps an installed Bori up to date from GitHub Releases, asking first.
 *
 * - **Once a week** (checked hourly, first one a minute after launch) it reads the latest release.
 *   "Check now" in Settings → About is the only other time it asks GitHub.
 * - A newer version shows the "Update now / Later" banner. Nothing downloads until Update now.
 * - **Every installer is verified** before it runs: `SHA256SUMS.sig` must be an Ed25519 signature
 *   of `SHA256SUMS` by the key in [RELEASE_PUBLIC_KEY] (its private half lives only on the
 *   release machine, `~/.config/bori-release/`, see `scripts/sign-release.sh`), and the installer
 *   must match its line in `SHA256SUMS`. A release without a valid signature is never installed.
 * - **Windows:** a hidden PowerShell script waits for Bori to quit, runs the per-user .msi with
 *   `/passive` (a progress bar, no admin prompt), deletes it and starts Bori again.
 * - **Linux:** the package (.deb / .rpm / pacman, whichever package manager owns the running
 *   launcher) is installed with `pkexec`, the system's password prompt, then Bori restarts itself
 *   and deletes the download.
 * - A copy not installed from a package (`./gradlew run`) never updates itself.
 */
class DesktopAppUpdater : AppUpdates {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    override val state: StateFlow<UpdateState> = _state

    private val stateFile = File(AppHome.dir, "update_check.properties")
    private val downloadDir = File(AppHome.dir, "updates")
    private val logFile = File(AppHome.dir, "update.log")
    private val restartLog = File(AppHome.dir, "update-restart.log")
    private val props = Properties().apply { runCatching { stateFile.inputStream().use { load(it) } } }

    private val _lastCheckedAt = MutableStateFlow(props.getProperty(KEY_LAST_CHECK)?.toLongOrNull() ?: 0L)
    override val lastCheckedAt: StateFlow<Long> = _lastCheckedAt

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build()

    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    /** The installed launcher (`/opt/bori/bin/bori`, `...\Bori.exe`); null when not run from a package. */
    private val launcher: String? = System.getProperty("jpackage.app-path")?.takeIf { File(it).exists() }

    private val installKind: InstallKind? by lazy { detectInstallKind() }

    private var latest: Release? = null

    /** Quits Bori cleanly (the same path as the tray's Quit). Set by main(). */
    var quitApp: () -> Unit = { kotlin.system.exitProcess(0) }

    fun start() {
        finishPreviousUpdate()
        if (launcher == null) { _state.value = UpdateState.NotInstalled; return }
        scope.launch {
            delay(60_000L)
            while (true) {
                val due = System.currentTimeMillis() - _lastCheckedAt.value >= WEEK_MS
                if (due && _state.value.isQuiet()) check(manual = false)
                delay(60 * 60_000L)
            }
        }
    }

    override fun checkNow() {
        if (launcher == null) { _state.value = UpdateState.NotInstalled; return }
        if (!_state.value.isQuiet()) return
        scope.launch { check(manual = true) }
    }

    override fun later() {
        val s = _state.value
        if (s is UpdateState.Available) _state.value = s.copy(dismissed = true)
        if (s is UpdateState.Failed) _state.value = latest?.takeIf { isNewer(it.version, AppInfo.VERSION) }
            ?.let { UpdateState.Available(it.version, it.notes, dismissed = true) } ?: UpdateState.Idle
    }

    override fun install() {
        val release = latest ?: return
        if (_state.value !is UpdateState.Available) return
        scope.launch {
            runCatching { downloadAndInstall(release) }.onFailure {
                log("install failed: $it")
                cleanDownloads()
                _state.value = UpdateState.Failed(it.message ?: "The update couldn't be installed.", inBanner = true)
            }
        }
    }

    private fun UpdateState.isQuiet() =
        this is UpdateState.Idle || this is UpdateState.UpToDate || this is UpdateState.Failed ||
            (this is UpdateState.Available && dismissed)

    /** A failed weekly check stays quiet and is retried within the hour; only "Check now" reports it. */
    private fun check(manual: Boolean) {
        val before = _state.value
        _state.value = UpdateState.Checking
        val release = runCatching { fetchLatest() }.getOrElse {
            _state.value = if (manual) UpdateState.Failed("Couldn't reach GitHub to check for updates.") else before
            return
        }
        _lastCheckedAt.value = System.currentTimeMillis()
        saveProp(KEY_LAST_CHECK, _lastCheckedAt.value.toString())
        latest = release
        log("checked (${if (manual) "manual" else "weekly"}): latest is ${release.version}")
        _state.value = if (isNewer(release.version, AppInfo.VERSION)) {
            UpdateState.Available(release.version, release.notes)
        } else {
            UpdateState.UpToDate
        }
    }

    private fun fetchLatest(): Release {
        // -Dbori.updateFeed=<url> points at a stand-in for GitHub's JSON (testing the updater locally).
        val feed = System.getProperty("bori.updateFeed") ?: "https://api.github.com/repos/$REPO/releases/latest"
        val body = get(feed, HttpResponse.BodyHandlers.ofString())
        val json = Json.parseToJsonElement(body).jsonObject
        val tag = json["tag_name"]!!.jsonPrimitive.content
        val assets = json["assets"]!!.jsonArray.associate {
            val o = it.jsonObject
            o["name"]!!.jsonPrimitive.content to o["browser_download_url"]!!.jsonPrimitive.content
        }
        return Release(
            version = tag.removePrefix("v"),
            notes = json["body"]?.jsonPrimitive?.content.orEmpty(),
            assets = assets
        )
    }

    private suspend fun downloadAndInstall(release: Release) {
        val kind = installKind ?: error("Bori can't tell how it was installed, so it can't update itself. Download the new version from GitHub instead.")
        val (assetName, assetUrl) = release.assets.entries.firstOrNull { kind.matches(it.key) }?.toPair()
            ?: error("This release has no ${kind.label} installer yet. Try again in a few minutes.")
        val sumsUrl = release.assets["SHA256SUMS"] ?: error("This release has no SHA256SUMS, so it can't be verified.")
        val sigUrl = release.assets["SHA256SUMS.sig"] ?: error("This release isn't signed, so Bori won't install it.")

        _state.value = UpdateState.Downloading(release.version, 0f)
        val sums = get(sumsUrl, HttpResponse.BodyHandlers.ofByteArray())
        val sig = get(sigUrl, HttpResponse.BodyHandlers.ofByteArray())
        if (!verifySignature(sums, sig)) error("The update's signature didn't check out, so Bori didn't install it.")
        val expected = String(sums).lineSequence()
            .map { it.trim().split(Regex("\\s+\\*?"), limit = 2) }
            .firstOrNull { it.size == 2 && it[1] == assetName }?.get(0)?.lowercase()
            ?: error("$assetName isn't listed in SHA256SUMS.")

        cleanDownloads()
        downloadDir.mkdirs()
        val file = File(downloadDir, assetName)
        download(assetUrl, file) { _state.value = UpdateState.Downloading(release.version, it) }
        if (sha256(file) != expected) error("The download was damaged (checksum mismatch). Try again.")

        log("downloaded and verified $assetName")
        _state.value = UpdateState.Installing(release.version)
        saveProp(KEY_PENDING, release.version)
        if (isWindows) installWindows(file) else installLinux(kind, file)
    }

    /** Hands off to a script that outlives Bori: it waits for this process, runs the .msi, cleans up, relaunches. */
    private fun installWindows(msi: File) {
        fun q(s: String) = "'" + s.replace("'", "''") + "'"
        val script = File(downloadDir, "install-update.ps1")
        script.writeText(
            """
            |${'$'}ErrorActionPreference = 'SilentlyContinue'
            |Wait-Process -Id ${ProcessHandle.current().pid()} -Timeout 60
            |Start-Process msiexec.exe -ArgumentList @('/i', '"${msi.absolutePath.replace("'", "''")}"', '/passive', '/norestart') -Wait
            |Remove-Item -LiteralPath ${q(msi.absolutePath)} -Force
            |Start-Process -FilePath ${q(launcher!!)}
            |Remove-Item -LiteralPath ${'$'}MyInvocation.MyCommand.Path -Force
            """.trimMargin()
        )
        log("handing over to install-update.ps1")
        ProcessBuilder(
            "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden",
            "-File", script.absolutePath
        ).withoutLauncherEnv().start()
        quit()
    }

    private fun installLinux(kind: InstallKind, pkg: File) {
        if (!onPath("pkexec")) {
            error("Bori needs pkexec (polkit) to install updates. The new version is on GitHub: ${releasesPage()}")
        }
        // The restart helper starts *before* the install, while Bori's own files are untouched. It
        // waits for this process to exit and relaunches only if the install succeeded (the go file).
        val go = File(AppHome.dir, "update-restart.go").apply { delete() }
        ProcessBuilder(
            "setsid", "-f", "sh", "-c",
            "while kill -0 \"$1\" 2>/dev/null; do sleep 0.5; done; " +
                "if [ -f \"$3\" ]; then rm -f \"$3\"; echo \"$(date) relaunching $2\"; exec \"$2\"; fi; " +
                "echo \"$(date) no go file, not relaunching\"",
            "bori-restart", ProcessHandle.current().pid().toString(), launcher!!, go.absolutePath
        )
            .redirectInput(File("/dev/null"))
            .redirectOutput(ProcessBuilder.Redirect.appendTo(restartLog))
            .redirectErrorStream(true)
            .withoutLauncherEnv()
            .start()
        log("restart helper started; running pkexec ${kind.installCommand(pkg.absolutePath).joinToString(" ")}")

        val proc = ProcessBuilder(listOf("pkexec") + kind.installCommand(pkg.absolutePath))
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        val code = proc.waitFor()
        pkg.delete()
        log("pkexec exited $code: ${output.lineSequence().lastOrNull { it.isNotBlank() }.orEmpty()}")
        when (code) {
            0 -> Unit
            126, 127 -> {
                saveProp(KEY_PENDING, null)
                _state.value = latest?.let { UpdateState.Available(it.version, it.notes) } ?: UpdateState.Idle
                return
            }
            else -> {
                saveProp(KEY_PENDING, null)
                error("The package manager stopped the update: ${output.lineSequence().lastOrNull { it.isNotBlank() }.orEmpty()}")
            }
        }
        go.createNewFile()
        log("installed; quitting so the helper can relaunch")
        quit()
    }

    /**
     * The jpackage launcher sets `_JPACKAGE_LAUNCHER` on itself. A Bori started with it still set
     * thinks it's a nested launch, skips its own config and runs `java` with no main class (prints
     * java's usage and exits). That's what stopped the relaunch in testing, so the helpers that
     * relaunch Bori must not inherit it.
     */
    private fun ProcessBuilder.withoutLauncherEnv() = apply {
        environment().keys.removeIf { it.startsWith("_JPACKAGE") }
    }

    private fun quit() {
        quitApp()
        // exitApplication() normally ends the JVM; this is the backstop so the restart never waits forever.
        Thread { Thread.sleep(5_000L); Runtime.getRuntime().halt(0) }.apply { isDaemon = true }.start()
    }

    /** Runs at launch: deletes leftovers and reports an update that didn't take. */
    private fun finishPreviousUpdate() {
        cleanDownloads()
        File(AppHome.dir, "update-restart.go").delete()
        val pending = props.getProperty(KEY_PENDING) ?: return
        saveProp(KEY_PENDING, null)
        log("started after updating to $pending")
        if (isNewer(pending, AppInfo.VERSION)) {
            _state.value = UpdateState.Failed("The update to $pending didn't install. You're still on ${AppInfo.VERSION}.", inBanner = true)
        }
    }

    private fun detectInstallKind(): InstallKind? {
        val path = launcher ?: return null
        if (isWindows) return InstallKind.MSI
        fun owns(vararg cmd: String) = onPath(cmd[0]) && runCatching {
            ProcessBuilder(*cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start().waitFor() == 0
        }.getOrDefault(false)
        return when {
            owns("pacman", "-Qqo", path) -> InstallKind.PACMAN
            owns("dpkg-query", "-S", path) -> InstallKind.DEB
            owns("rpm", "-qf", path) -> InstallKind.RPM
            else -> null
        }
    }

    private fun verifySignature(data: ByteArray, sig: ByteArray): Boolean = runCatching {
        val key = KeyFactory.getInstance("Ed25519")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(RELEASE_PUBLIC_KEY)))
        Signature.getInstance("Ed25519").run {
            initVerify(key)
            update(data)
            verify(sig)
        }
    }.getOrDefault(false)

    private fun <T> get(url: String, handler: HttpResponse.BodyHandler<T>): T {
        val response = http.send(request(url), handler)
        if (response.statusCode() !in 200..299) error("GitHub answered ${response.statusCode()}.")
        return response.body()
    }

    private fun download(url: String, dest: File, onProgress: (Float) -> Unit) {
        val response = http.send(request(url), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) error("The download failed (${response.statusCode()}).")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1L)
        response.body().use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                var lastReported = 0f
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    done += n
                    if (total > 0) {
                        val p = done.toFloat() / total
                        if (p - lastReported >= 0.01f) { lastReported = p; onProgress(p) }
                    }
                }
            }
        }
    }

    private fun request(url: String) = HttpRequest.newBuilder(URI(url))
        .header("User-Agent", "Bori/${AppInfo.VERSION}")
        .header("Accept", if (url.contains("api.github.com")) "application/vnd.github+json" else "*/*")
        .timeout(Duration.ofMinutes(10))
        .build()

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** update.log in the data folder: one line per step, so a failed update leaves a trail. */
    @Synchronized
    private fun log(msg: String) = runCatching {
        if (logFile.length() > 200_000) logFile.delete()
        logFile.appendText("${java.time.LocalDateTime.now().withNano(0)}  ${AppInfo.VERSION}  $msg\n")
    }

    private fun cleanDownloads() {
        downloadDir.listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    private fun saveProp(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
        runCatching { stateFile.outputStream().use { props.store(it, "Bori update checks") } }
    }

    private fun onPath(cmd: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .any { File(it, cmd).canExecute() }

    private fun releasesPage() = "https://github.com/$REPO/releases/latest"

    private data class Release(val version: String, val notes: String, val assets: Map<String, String>)

    private enum class InstallKind(val label: String) {
        MSI("Windows"),
        DEB(".deb"),
        RPM(".rpm"),
        PACMAN("Arch (pacman)");

        fun matches(name: String) = when (this) {
            MSI -> name.endsWith(".msi")
            DEB -> name.endsWith("_amd64.deb")
            RPM -> name.endsWith(".x86_64.rpm")
            PACMAN -> name.endsWith("-x86_64.pkg.tar.zst")
        }

        fun installCommand(path: String): List<String> = when (this) {
            PACMAN -> listOf("pacman", "-U", "--noconfirm", path)
            DEB -> if (File("/usr/bin/apt-get").exists()) listOf("apt-get", "install", "-y", path) else listOf("dpkg", "-i", path)
            RPM -> when {
                File("/usr/bin/dnf").exists() -> listOf("dnf", "install", "-y", path)
                File("/usr/bin/zypper").exists() -> listOf("zypper", "--non-interactive", "install", "--allow-unsigned-rpm", path)
                else -> listOf("rpm", "-U", path)
            }
            MSI -> error("not used")
        }
    }

    companion object {
        private const val REPO = "Atom0131/Bori---Habit-Tracker"
        private const val WEEK_MS = 7L * 24 * 60 * 60_000L
        private const val KEY_LAST_CHECK = "lastCheckedAt"
        private const val KEY_PENDING = "pendingVersion"

        /** Ed25519 public key (X.509 DER, base64). Never change it: installed copies only trust this key. */
        private const val RELEASE_PUBLIC_KEY = "MCowBQYDK2VwAyEArvuRqm7JHqCZHataTLEGEfMNeNWVT/atxaDATWUgHOU="

        /** "1.0.10" > "1.0.9"; missing parts count as 0. */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split('.', '-').map { it.toIntOrNull() ?: 0 }
            val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
