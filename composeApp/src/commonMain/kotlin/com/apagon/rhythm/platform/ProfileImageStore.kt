package com.apagon.rhythm.platform

/**
 * Carries the profile picture itself across a sync, never its path (2026-10-09). The path points
 * into this device's own storage, so it is stripped from the settings blob; the picture travels as
 * `PreferencesDto.profileImage` instead: null = no information, "" = no picture, otherwise base64
 * JPEG bounded to 512px.
 */
interface ProfileImageStore {
    suspend fun export(): String?
    /** Applies a peer's picture without stamping the settings timestamp. */
    suspend fun import(encoded: String)
}
