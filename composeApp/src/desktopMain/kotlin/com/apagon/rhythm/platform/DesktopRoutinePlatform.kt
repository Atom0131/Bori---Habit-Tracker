package com.apagon.rhythm.platform

import com.apagon.rhythm.data.routine.RoutinePlatform
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The computer's side of a running routine (see `RoutineRunner`).
 *
 * - **The run** lives in `routine_run.json` in the data folder, so closing Bori or restarting the
 *   computer mid-routine picks it up again. Like the phone's own store it's never backed up or
 *   synced: a run in progress is live state, not data.
 * - **The chime** is two soft bell notes made on the spot with Java Sound, so it needs no sound file
 *   and plays the same on Linux and Windows.
 * - **Spoken steps** use the system voice: `spd-say` (speech-dispatcher) or `espeak-ng` on Linux,
 *   Windows' built-in System.Speech through PowerShell. Without one, steps simply aren't read.
 * - **Notifications** go through [DesktopOsNotifier]: `notify-send` on Linux, a tray toast on Windows.
 */
class DesktopRoutinePlatform(private val notifier: DesktopOsNotifier) : RoutinePlatform {
    private val file = File(AppHome.dir, "routine_run.json")
    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    override fun loadRun(): String? = runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()

    override fun saveRun(json: String?) {
        if (json == null) file.delete() else file.writeText(json)
    }

    override fun chime() {
        Thread {
            runCatching {
                val rate = 44_100f
                val pcm = bell(rate, 880.0) + bell(rate, 1318.5)
                val format = AudioFormat(rate, 16, 1, true, false)
                AudioSystem.getSourceDataLine(format).use { line ->
                    line.open(format)
                    line.start()
                    line.write(pcm, 0, pcm.size)
                    line.drain()
                }
            }
        }.apply { isDaemon = true }.start()
    }

    /** One bell note: a sine with a soft attack and an exponential fade, 16-bit little-endian. */
    private fun bell(rate: Float, freq: Double): ByteArray {
        val seconds = 0.45
        val n = (rate * seconds).toInt()
        val out = ByteArray(n * 2)
        for (i in 0 until n) {
            val t = i / rate.toDouble()
            val envelope = (t / 0.01).coerceAtMost(1.0) * exp(-t * 7.0)
            val sample = (sin(2 * PI * freq * t) * 0.8 + sin(4 * PI * freq * t) * 0.2) * envelope * 0.35
            val v = (sample * Short.MAX_VALUE).toInt().toShort()
            out[2 * i] = (v.toInt() and 0xff).toByte()
            out[2 * i + 1] = ((v.toInt() shr 8) and 0xff).toByte()
        }
        return out
    }

    override fun speak(text: String) {
        val cmd = when {
            isWindows -> listOf(
                "powershell.exe", "-NoProfile", "-WindowStyle", "Hidden", "-Command",
                "Add-Type -AssemblyName System.Speech; " +
                    "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('${text.replace("'", "''")}')"
            )
            onPath("spd-say") -> listOf("spd-say", "--", text)
            onPath("espeak-ng") -> listOf("espeak-ng", "--", text)
            onPath("espeak") -> listOf("espeak", "--", text)
            else -> return
        }
        runCatching {
            ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start()
        }
    }

    override fun missingVoiceHint(): String? = when {
        isWindows || onPath("spd-say") || onPath("espeak-ng") || onPath("espeak") -> null
        else -> "No system voice found. Install speech-dispatcher (with espeak-ng) to hear steps read aloud."
    }

    override fun notify(title: String, text: String) {
        notifier.notify(FiredAlert(kind = FiredAlertKind.REMINDER, sourceId = 0, title = title, subtitle = text))
    }

    private fun onPath(cmd: String) = System.getenv("PATH").orEmpty().split(File.pathSeparator)
        .any { File(it, cmd).canExecute() }
}
