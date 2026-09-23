package com.apagon.rhythm.platform

/**
 * Copies a platform photo reference (content:// URI on Android, PHPicker file
 * URL on iOS) into app-private storage so it stays readable after the picker
 * permission lapses. Returns the stored file's absolute path, or null on failure.
 */
interface PhotoStorage {
    suspend fun importPhoto(sourceUri: String, subdirectory: String): String?
}
