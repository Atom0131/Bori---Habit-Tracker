package com.apagon.rhythm.data.sync

/**
 * The files that travel beside a sync's rows: pictures (both ways) and the phone's backup copy
 * (phone → computer). Rows say where a picture is on the device that added it; this moves the
 * picture itself. The desktop implementation is DesktopSyncAttachments (desktopMain), which owns
 * the photo folders under AppHome.
 *
 * [peer] is whatever key the caller already uses for that device's watermark: the deviceId for a
 * device that synced in, host:port for a computer this one syncs out to.
 */
interface SyncAttachments {
    /** Pictures [peer] hasn't been sent yet, up to one sync's worth (the rest go next time). */
    suspend fun outgoingPictures(peer: String): List<PictureDto>

    /**
     * Saves the pictures [peer] sent (never overwriting one already here) and remembers [peer] has
     * them. [peerDeviceId] is who answered: when a different device now answers for [peer], the
     * record of what it was sent is forgotten, so everything goes again.
     */
    suspend fun receivePictures(peer: String, peerDeviceId: String?, pictures: List<PictureDto>)

    /** Called once a round has gone through, with what [outgoingPictures] gave for it. */
    suspend fun markSent(peer: String, pictures: List<PictureDto>)

    /** Keeps a phone's backup file in `<backup folder>/From <phone>/`. */
    suspend fun receivePhoneBackup(fromName: String?, backup: BackupFileDto)
}
