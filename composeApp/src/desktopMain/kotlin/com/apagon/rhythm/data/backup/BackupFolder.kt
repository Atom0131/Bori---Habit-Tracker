package com.apagon.rhythm.data.backup

import java.io.File

/**
 * Where this computer's automatic backups go: `Downloads/Bori backup`, the same folder name the
 * phone uses, so a person looking for one finds it in the same place on every device. Copies of a
 * phone's backups go in `From <phone>/` inside it.
 */
object BackupFolder {
    const val AUTO_PREFIX = "bori_autobackup_"
    const val KEEP = 14

    val dir: File
        get() = File(System.getProperty("bori.backupDir") ?: File(downloads(), "Bori backup").path)

    private fun downloads(): File {
        val home = File(System.getProperty("user.home"))
        // Linux: the desktop's own Downloads folder when it isn't ~/Downloads (another language).
        runCatching {
            val xdg = File(home, ".config/user-dirs.dirs").takeIf { it.isFile }?.readLines()
                ?.firstOrNull { it.startsWith("XDG_DOWNLOAD_DIR=") }
                ?.substringAfter('=')?.trim('"')?.replace("\$HOME", home.path)
            if (!xdg.isNullOrBlank() && File(xdg).isDirectory) return File(xdg)
        }
        return File(home, "Downloads")
    }

    private val AUTO_NAME = Regex("""bori_autobackup_\d{4}-\d{2}-\d{2}\.json""")

    fun isAutoBackupName(name: String) = AUTO_NAME.matches(name)

    /** Automatic backups in [folder], newest first (the date is in the name). */
    fun autoBackups(folder: File): List<File> =
        folder.listFiles()?.filter { it.isFile && isAutoBackupName(it.name) }?.sortedByDescending { it.name }.orEmpty()

    /** Keeps the newest [KEEP] automatic backups in [folder] and deletes the rest. */
    fun prune(folder: File) {
        autoBackups(folder).drop(KEEP).forEach { runCatching { it.delete() } }
    }

    /** `From Atom's Z Fold8`, with anything a file system might refuse replaced. */
    fun phoneFolder(phoneName: String?): File {
        val clean = (phoneName?.takeIf { it.isNotBlank() } ?: "phone").replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        return File(dir, "From $clean")
    }
}
