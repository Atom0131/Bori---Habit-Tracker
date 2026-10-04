package com.apagon.rhythm.data.backup

import com.apagon.rhythm.core.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Desktop port of Android's `data/backup/BackupCrypto.kt` — same algorithm, same envelope shape,
 * byte/field-identical JSON so a backup taken on one platform decrypts on the other.
 *
 * ## The format
 *
 * ```json
 * { "version": 1,
 *   "encryption": { "alg": …, "kdf": …, "iterations": …, "salt": …, "nonce": … },
 *   "ciphertext": "<base64 of AES-GCM output, tag appended>" }
 * ```
 *
 * AES-256-GCM with a PBKDF2-HMAC-SHA256 key at 600k iterations, a fresh 16-byte salt and 12-byte
 * nonce per export, and the KDF parameters bound as AAD via the same
 * `"$alg|$kdf|$iterations|$saltB64|$nonceB64"` construction Android uses — so an attacker can't
 * edit `iterations` down for a cheap offline attack without the tag check failing first.
 *
 * **Deliberate simplification vs Android**: Android carries two PBKDF2 implementations (the
 * platform `SecretKeyFactory` plus a hand-rolled RFC 8018 fallback) solely because
 * `PBKDF2WithHmacSHA256` is missing below API 26. The JVM desktop target has no such gap, so this
 * calls `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` directly — no fallback, no runtime
 * cross-check. See this file's own scratch verification (reported alongside this port) for the
 * proof that the platform factory alone is safe here, including for a non-ASCII and an empty
 * password.
 *
 * Also uses `java.util.Base64.getEncoder()/getDecoder()` instead of `android.util.Base64` — a
 * drop-in equivalent for `NO_WRAP` mode (neither inserts line breaks) — and desktop's own
 * `core.json.JSONObject`/`JSONArray` (kotlinx.serialization-backed, see `core/json/OrgJsonCompat.kt`)
 * rather than `org.json`, matching what `DesktopBackupManager.kt` already uses.
 *
 * There is deliberately no port of Android's `BackupKeyStore` — desktop always prompts for the
 * password on export/import, no remembered-password feature in this pass.
 */
object DesktopBackupCrypto {

    const val ITERATIONS = 600_000

    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    private const val ALG = "AES-256-GCM"
    private const val KDF = "PBKDF2-HMAC-SHA256"

    const val KEY_ENCRYPTION = "encryption"
    private const val KEY_CIPHERTEXT = "ciphertext"

    /** True when [root] is an encrypted envelope rather than a plain backup. */
    fun isEncrypted(root: JSONObject): Boolean = root.has(KEY_ENCRYPTION)

    fun encrypt(plaintext: String, password: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }

        val header = JSONObject().apply {
            put("alg", ALG)
            put("kdf", KDF)
            put("iterations", ITERATIONS)
            put("salt", salt.b64())
            put("nonce", nonce.b64())
        }

        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad(ALG, KDF, ITERATIONS, salt, nonce))
        }
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return JSONObject().apply {
            put("version", 1)
            put(KEY_ENCRYPTION, header)
            put(KEY_CIPHERTEXT, ciphertext.b64())
        }.toString(2)
    }

    /** Convenience overload matching Android's shape of taking the raw envelope JSON string. */
    fun decrypt(envelopeJson: String, password: CharArray): String =
        decrypt(JSONObject(envelopeJson), password)

    /**
     * @throws javax.crypto.AEADBadTagException when the password is wrong or the file was edited.
     *   Callers must turn that into "Wrong password" rather than surfacing it raw, and must not
     *   have opened a database transaction yet — a failure here means nothing should be written.
     */
    fun decrypt(root: JSONObject, password: CharArray): String {
        val header = root.getJSONObject(KEY_ENCRYPTION)
        val alg = header.optString("alg", ALG)
        val kdf = header.optString("kdf", KDF)
        val iterations = header.getInt("iterations")
        val salt = header.getString("salt").unB64()
        val nonce = header.getString("nonce").unB64()

        require(alg == ALG && kdf == KDF) { "Unsupported backup encryption: $alg / $kdf" }

        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(aad(alg, kdf, iterations, salt, nonce))
        }
        return cipher.doFinal(root.getString(KEY_CIPHERTEXT).unB64()).toString(Charsets.UTF_8)
    }

    /** Canonical, order-independent binding of every KDF parameter into the tag. */
    private fun aad(alg: String, kdf: String, iterations: Int, salt: ByteArray, nonce: ByteArray) =
        "$alg|$kdf|$iterations|${salt.b64()}|${nonce.b64()}".toByteArray(Charsets.UTF_8)

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int) =
        SecretKeySpec(pbkdf2(password, salt, iterations, KEY_BITS / 8), "AES")

    /**
     * PBKDF2-HMAC-SHA256 via the platform `SecretKeyFactory` — no RFC 8018 fallback. Unlike
     * Android (minSdk 24, where `PBKDF2WithHmacSHA256` doesn't exist below API 26), the JVM this
     * runs on always provides it, so there is exactly one implementation and nothing to
     * cross-check at runtime.
     */
    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, lengthBytes: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password, salt, iterations, lengthBytes * 8))
            .encoded

    private fun ByteArray.b64(): String = Base64.getEncoder().encodeToString(this)
    private fun String.unB64(): ByteArray = Base64.getDecoder().decode(this)
}
