package com.junkfood.seal.util

import android.content.Context
import android.util.Log
import com.junkfood.seal.App
import com.junkfood.seal.BuildConfig
import com.junkfood.seal.util.PreferenceUtil.encodeString
import com.junkfood.seal.util.PreferenceUtil.getString
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.security.MessageDigest

/**
 * Installs the `yt-dlp` single-file executable built from the vendored source
 * tree (`yt-dlp/`, see `tools/yt-dlp-packager/`) over the copy shipped inside
 * the `youtubedl-android` AAR.
 *
 * Why an overwrite instead of a fork: `youtubedl-android` executes
 * `python libpython.so <noBackupFilesDir/youtubedl-android/yt-dlp/yt-dlp> [args]`
 * ([YoutubeDL.executeImpl]) and only seeds that file from `R.raw.ytdlp` when it
 * is missing ([YoutubeDL.init_ytdlp]). Replacing the file post-`init` therefore
 * makes the local checkout the single source of truth for extractors and
 * core logic without changing the execution bridge at all.
 *
 * Must be called on a background thread, strictly after [YoutubeDL.init].
 * Seed semantics (same as the library's own `R.raw.ytdlp` seed): the bundle
 * is copied when its version differs from the installed marker, then the
 * regular OTA updater ([UpdateUtil.updateYtDlp]) takes precedence until the
 * next bundle version bump. No-op when this APK carries no bundle.
 */
object YtDlpBundle {

    private const val TAG = "YtDlpBundle"

    private const val ASSET_BIN = "yt-dlp/yt-dlp"
    private const val ASSET_VERSION = "yt-dlp/yt-dlp.version"
    private const val ASSET_SHA256 = "yt-dlp/yt-dlp.sha256"

    /** MMKV marker for the bundled version already copied into files dir. */
    const val BUNDLED_VERSION_INSTALLED = "yt_dlp_bundled_installed"

    sealed interface InstallResult {
        /** No local bundle in this APK (task skipped) or version unknown. */
        data object Unavailable : InstallResult

        /** Installed file already matches the bundled version. */
        data object UpToDate : InstallResult

        /** Bundle copied over the library copy. */
        data class Installed(val version: String) : InstallResult

        /** Bundle present but unusable (e.g. checksum mismatch); old file kept. */
        data class Failed(val reason: String) : InstallResult
    }

    fun targetFile(context: Context = App.context): File =
        File(
            File(context.noBackupFilesDir, YoutubeDL.baseName),
            "${YoutubeDL.ytdlpDirName}/${YoutubeDL.ytdlpBin}",
        )

    fun installIfNeeded(context: Context = App.context): InstallResult {
        val bundledVersion = BuildConfig.YT_DLP_BUNDLED_VERSION.trim()
        if (bundledVersion.isEmpty() || bundledVersion == "unknown") {
            return InstallResult.Unavailable
        }

        val target = targetFile(context)
        val installedMarker = BUNDLED_VERSION_INSTALLED.getString()
        if (installedMarker == bundledVersion && target.exists() && target.length() > 0) {
            return InstallResult.UpToDate
        }

        val assetVersion =
            runCatching {
                context.assets.open(ASSET_VERSION).bufferedReader().use { it.readText().trim() }
            }.getOrNull() ?: return InstallResult.Unavailable

        val expectedSha =
            runCatching {
                context.assets.open(ASSET_SHA256).bufferedReader().use {
                    it.readText().trim().split(Regex("\\s+")).firstOrNull()
                }
            }.getOrNull()
        if (expectedSha.isNullOrEmpty()) {
            Log.w(TAG, "bundled yt-dlp present but sha256 sidecar is missing")
            return InstallResult.Failed("missing sha256 sidecar")
        }

        // Single pass: stream asset -> temp file while hashing, so the ~3MB
        // bundle never sits fully in memory twice.
        return runCatching {
            target.parentFile?.mkdirs()
            val tmp = File(target.parentFile, "${target.name}.seal-bundle.tmp")
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            context.assets.open(ASSET_BIN).use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(128 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        digest.update(buf, 0, n)
                        output.write(buf, 0, n)
                        size += n
                    }
                }
            }
            if (size == 0L) {
                tmp.delete()
                return InstallResult.Failed("empty bundle asset")
            }
            val actualSha = digest.digest().toHexFast()
            if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                tmp.delete()
                Log.w(TAG, "bundled yt-dlp sha256 mismatch: expected $expectedSha got $actualSha")
                return InstallResult.Failed("sha256 mismatch")
            }
            if (!tmp.renameTo(target)) {
                // renameTo can fail across edge cases; fall back to a copy.
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            target.setReadable(true, false)
            BUNDLED_VERSION_INSTALLED.encodeString(assetVersion.ifEmpty { bundledVersion })
            YT_DLP_VERSION.encodeString(assetVersion.ifEmpty { bundledVersion })
            Log.i(TAG, "installed bundled yt-dlp $assetVersion ($size bytes)")
            InstallResult.Installed(assetVersion.ifEmpty { bundledVersion })
        }.getOrElse { th ->
            Log.w(TAG, "failed to install bundled yt-dlp", th)
            InstallResult.Failed(th.message ?: th::class.simpleName.orEmpty())
        }
    }

    internal fun ByteArray.toHexFast(): String {
        val hex = CharArray(size * 2)
        forEachIndexed { i, b ->
            val v = b.toInt() and 0xFF
            hex[i * 2] = HEX_CHARS[v ushr 4]
            hex[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return hex.concatToString()
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()
}
