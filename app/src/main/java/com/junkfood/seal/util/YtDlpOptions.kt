package com.junkfood.seal.util

import android.util.Log
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getString
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.serialization.Serializable

/**
 * Engine-level coverage for `yt-dlp` options that have no dedicated GUI
 * toggle in Seal (yet). Every field defaults to "off"/empty so existing
 * behaviour is bit-for-bit preserved unless the user opts in
 * (via upcoming settings UI, backup import, or custom-command templates,
 * which already pass the full CLI through untouched).
 *
 * Mapping reference: `yt-dlp/yt_dlp/options.py` option groups
 * (Network / Geo-restriction / Video Selection / Download / Filesystem /
 * Video Format / Subtitle / Authentication / Post-Processing / SponsorBlock /
 * Extractor). Only options meaningful on Android are exposed — interactive
 * or TTY-bound flags (`--console-title`, `--progress`, config-file loading)
 * are deliberately excluded.
 *
 * Ordering contract: advanced options are appended AFTER Seal's built-in
 * flags, so for single-value options (yt-dlp `optparse` last-wins) the
 * advanced value takes precedence (e.g. custom `--retries` overrides the
 * info-fetch default of `-R 1`). Exception: `--extractor-args`, where upstream
 * *overwrites* same-extractor flags — the extra is merged with Seal's own
 * flags at the emission sites via [mergeExtractorArgs] instead of appended.
 */
@Serializable
data class YtDlpAdvancedOptions(
    // region Network / anti-bot (options.py: Network Options)
    /** `--impersonate CLIENT` (e.g. `chrome`, `firefox`, `safari`). */
    val impersonateClient: String = "",
    /** `--no-check-certificate`. */
    val noCheckCertificate: Boolean = false,
    /** `--geo-bypass`. */
    val geoBypass: Boolean = false,
    /** `--source-address ADDR` (bind outgoing traffic). */
    val sourceAddress: String = "",
    /** `--http-chunk-size N`, e.g. `10M`. */
    val httpChunkSize: String = "",
    /** `--referer URL`. */
    val referer: String = "",
    /** Extra `--add-headers "Key:Value"`, `;`- or newline-separated. */
    val addHeaders: String = "",
    // endregion

    // region Resilience (options.py: Download Options)
    /** `--retries N` / `infinite`. Blank = Seal default. */
    val retries: String = "",
    /** `--fragment-retries N` / `infinite`. */
    val fragmentRetries: String = "",
    /** `--retry-sleep ...`, e.g. `fragment:30`. */
    val retrySleep: String = "",
    /** `--socket-timeout N` seconds. */
    val socketTimeout: String = "",
    // endregion

    // region Selection / filtering (options.py: Video Selection)
    /** `--playlist-start N`. */
    val playlistStart: String = "",
    /** `--playlist-end N`. */
    val playlistEnd: String = "",
    /** `--match-filter FILTER`. */
    val matchFilter: String = "",
    /** `--min-filesize SIZE`, e.g. `50M`. */
    val minFilesize: String = "",
    /** `--max-filesize SIZE`. */
    val maxFilesize: String = "",
    /** `--live-from-start`. */
    val liveFromStart: Boolean = false,
    /** `--wait-for-video MIN[-MAX]` seconds. */
    val waitForVideo: String = "",
    // endregion

    // region Filesystem (options.py: Filesystem Options)
    /** `--windows-filenames` (skipped when `--restrict-filenames` is on). */
    val windowsFilenames: Boolean = false,
    /** `--force-overwrites`. Default false = yt-dlp default untouched. */
    val forceOverwrite: Boolean = false,
    /** `--write-info-json` sidecar. */
    val writeInfoJson: Boolean = false,
    /** `--write-description` sidecar. */
    val writeDescription: Boolean = false,
    // endregion

    // region Format / post-processing
    /** `--prefer-free-formats`. */
    val preferFreeFormats: Boolean = false,
    /**
     * Format check: `""` untouched, `"no"` -> `--no-check-formats`,
     * `"select"` -> `--check-formats`.
     */
    val checkFormats: String = "",
    /** `--keep-video` (only applied to `-x`/audio downloads). */
    val keepVideo: Boolean = false,
    /** `--recode-video EXT`, e.g. `mp4`. */
    val recodeVideo: String = "",
    /** `--remux-video EXT`, e.g. `mkv`. Applied after Seal's own remux flag. */
    val remuxVideo: String = "",
    /** `--postprocessor-args "NAME:ARGS"`, e.g. `ffmpeg:-crf 23`. */
    val postprocessorArgs: String = "",
    /** `--external-downloader-args "aria2c:..."` (appended to the library's). */
    val downloaderArgs: String = "",
    /** Extra `--extractor-args "KEY:ARGS"` (Seal may set its own first). */
    val extractorArgsExtra: String = "",
    /** `--sponsorblock-mark CATS` (complements `--sponsorblock-remove`). */
    val sponsorblockMark: String = "",
    // endregion
) {
    fun isEmpty(): Boolean =
        impersonateClient.isBlank() &&
            !noCheckCertificate &&
            !geoBypass &&
            sourceAddress.isBlank() &&
            httpChunkSize.isBlank() &&
            referer.isBlank() &&
            addHeaders.isBlank() &&
            retries.isBlank() &&
            fragmentRetries.isBlank() &&
            retrySleep.isBlank() &&
            socketTimeout.isBlank() &&
            playlistStart.isBlank() &&
            playlistEnd.isBlank() &&
            matchFilter.isBlank() &&
            minFilesize.isBlank() &&
            maxFilesize.isBlank() &&
            !liveFromStart &&
            waitForVideo.isBlank() &&
            !windowsFilenames &&
            !forceOverwrite &&
            !writeInfoJson &&
            !writeDescription &&
            !preferFreeFormats &&
            checkFormats.isBlank() &&
            !keepVideo &&
            recodeVideo.isBlank() &&
            remuxVideo.isBlank() &&
            postprocessorArgs.isBlank() &&
            downloaderArgs.isBlank() &&
            extractorArgsExtra.isBlank() &&
            sponsorblockMark.isBlank()

    companion object {
        val EMPTY = YtDlpAdvancedOptions()

        /**
         * MMKV keys backing [fromPreferences], in a fixed order. The settings
         * page reuses this for Reset so the key set cannot drift between the
         * engine and the UI.
         */
        val preferenceKeys: List<String> =
            listOf(
                YTDLP_IMPERSONATE,
                YTDLP_NO_CHECK_CERTIFICATE,
                YTDLP_GEO_BYPASS,
                YTDLP_SOURCE_ADDRESS,
                YTDLP_HTTP_CHUNK_SIZE,
                YTDLP_REFERER,
                YTDLP_ADD_HEADERS,
                YTDLP_RETRIES,
                YTDLP_FRAGMENT_RETRIES,
                YTDLP_RETRY_SLEEP,
                YTDLP_SOCKET_TIMEOUT,
                YTDLP_PLAYLIST_START,
                YTDLP_PLAYLIST_END,
                YTDLP_MATCH_FILTER,
                YTDLP_MIN_FILESIZE,
                YTDLP_MAX_FILESIZE,
                YTDLP_LIVE_FROM_START,
                YTDLP_WAIT_FOR_VIDEO,
                YTDLP_WINDOWS_FILENAMES,
                YTDLP_FORCE_OVERWRITE,
                YTDLP_WRITE_INFO_JSON,
                YTDLP_WRITE_DESCRIPTION,
                YTDLP_PREFER_FREE_FORMATS,
                YTDLP_CHECK_FORMATS,
                YTDLP_KEEP_VIDEO,
                YTDLP_RECODE_VIDEO,
                YTDLP_REMUX_VIDEO,
                YTDLP_PP_ARGS,
                YTDLP_DOWNLOADER_ARGS,
                YTDLP_EXTRACTOR_ARGS_EXTRA,
                YTDLP_SPONSORBLOCK_MARK,
            )

        fun fromPreferences(): YtDlpAdvancedOptions =
            YtDlpAdvancedOptions(
                impersonateClient = YTDLP_IMPERSONATE.getString(),
                noCheckCertificate = YTDLP_NO_CHECK_CERTIFICATE.getBoolean(),
                geoBypass = YTDLP_GEO_BYPASS.getBoolean(),
                sourceAddress = YTDLP_SOURCE_ADDRESS.getString(),
                httpChunkSize = YTDLP_HTTP_CHUNK_SIZE.getString(),
                referer = YTDLP_REFERER.getString(),
                addHeaders = YTDLP_ADD_HEADERS.getString(),
                retries = YTDLP_RETRIES.getString(),
                fragmentRetries = YTDLP_FRAGMENT_RETRIES.getString(),
                retrySleep = YTDLP_RETRY_SLEEP.getString(),
                socketTimeout = YTDLP_SOCKET_TIMEOUT.getString(),
                playlistStart = YTDLP_PLAYLIST_START.getString(),
                playlistEnd = YTDLP_PLAYLIST_END.getString(),
                matchFilter = YTDLP_MATCH_FILTER.getString(),
                minFilesize = YTDLP_MIN_FILESIZE.getString(),
                maxFilesize = YTDLP_MAX_FILESIZE.getString(),
                liveFromStart = YTDLP_LIVE_FROM_START.getBoolean(),
                waitForVideo = YTDLP_WAIT_FOR_VIDEO.getString(),
                windowsFilenames = YTDLP_WINDOWS_FILENAMES.getBoolean(),
                forceOverwrite = YTDLP_FORCE_OVERWRITE.getBoolean(),
                writeInfoJson = YTDLP_WRITE_INFO_JSON.getBoolean(),
                writeDescription = YTDLP_WRITE_DESCRIPTION.getBoolean(),
                preferFreeFormats = YTDLP_PREFER_FREE_FORMATS.getBoolean(),
                checkFormats = YTDLP_CHECK_FORMATS.getString(),
                keepVideo = YTDLP_KEEP_VIDEO.getBoolean(),
                recodeVideo = YTDLP_RECODE_VIDEO.getString(),
                remuxVideo = YTDLP_REMUX_VIDEO.getString(),
                postprocessorArgs = YTDLP_PP_ARGS.getString(),
                downloaderArgs = YTDLP_DOWNLOADER_ARGS.getString(),
                extractorArgsExtra = YTDLP_EXTRACTOR_ARGS_EXTRA.getString(),
                sponsorblockMark = YTDLP_SPONSORBLOCK_MARK.getString(),
            )
    }
}

private const val TAG = "YtDlpOptions"

private fun androidWarn(tag: String, message: String) {
    Log.w(tag, message)
}

/**
 * Warning sink used by the options layer. Defaults to `Log.w`; unit tests
 * replace it because `android.util.Log` is a stub on the JVM.
 */
internal var warnLogger: (tag: String, message: String) -> Unit = ::androidWarn

internal fun resetWarnLogger() {
    warnLogger = ::androidWarn
}

private val FILESIZE_RE = Regex("""^\d+(\.\d+)?[KMGTPE]?$""", RegexOption.IGNORE_CASE)
private val WAIT_FOR_VIDEO_RE = Regex("""^\d+(-\d+)?$""")

/** `true` for values yt-dlp accepts as `SIZE` (`50M`, `1.5G`, `1024`). */
internal fun isFilesizeSpec(value: String): Boolean = FILESIZE_RE.matches(value.trim())

/** `true` for `--wait-for-video` values (`60`, `30-120`). */
internal fun isWaitForVideoSpec(value: String): Boolean = WAIT_FOR_VIDEO_RE.matches(value.trim())

/**
 * `true` for a usable `--playlist-start`/`--playlist-end` pair. Either side
 * may be empty (open-ended); when both are set, start must not exceed end.
 */
internal fun isValidPlaylistRange(start: String, end: String): Boolean {
    if (!isValidDigitsOrEmpty(start) || !isValidDigitsOrEmpty(end)) return false
    if (start.isEmpty() || end.isEmpty()) return true
    val startNum = start.toLongOrNull() ?: return false
    val endNum = end.toLongOrNull() ?: return false
    return startNum <= endNum
}

internal fun isValidDigitsOrEmpty(value: String): Boolean =
    value.isEmpty() || value.all { it.isDigit() }

/** `true` for `--retries`/`--fragment-retries` values (`10`, `inf`, `infinite`). */
internal fun isValidRetries(value: String): Boolean =
    value.isEmpty() ||
        value.equals("inf", ignoreCase = true) ||
        value.equals("infinite", ignoreCase = true) ||
        value.all { it.isDigit() }

/**
 * Normalizes a retries value for the CLI, or `null` when invalid.
 * Upstream `parse_retries` only accepts lowercase `inf`/`infinite`, so any
 * casing is canonicalized instead of failing the whole request.
 */
internal fun normalizeRetries(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.equals("inf", ignoreCase = true) ||
        trimmed.equals("infinite", ignoreCase = true)
    ) {
        return "infinite"
    }
    return trimmed.takeIf { it.all { c -> c.isDigit() } }
}

/**
 * Merges `--extractor-args` specs (`KEY:ARGS`) by extractor key.
 *
 * Required because upstream *overwrites* (not merges) repeated flags for the
 * same extractor (`_dict_from_options_callback` with `append=False`): naively
 * appending the advanced extra after Seal's own `youtube:skip=translated_subs`
 * would silently drop the skip. Same-key args are joined with `;` (upstream
 * arg separator); different keys stay as separate flags.
 *
 * Returns the merged specs (empty when nothing usable). Malformed specs are
 * skipped with a warning instead of failing the whole request.
 */
internal fun mergeExtractorArgs(vararg specs: String): List<String> {
    val grouped = linkedMapOf<String, MutableList<String>>()
    for (raw in specs) {
        val spec = raw.trim()
        if (spec.isEmpty()) continue
        val match = EXTRACTOR_ARG_SPEC_RE.matchEntire(spec)
        if (match == null || match.groupValues[2].trim().isEmpty()) {
            warnLogger(TAG, "ignoring malformed --extractor-args: $spec")
            continue
        }
        // Mirror upstream key normalization (options.py _extractor_arg_parser).
        val key = match.groupValues[1].lowercase().replace('-', '_')
        grouped.getOrPut(key) { mutableListOf() }.add(match.groupValues[2].trim())
    }
    return grouped.entries.map { (key, args) -> "$key:${args.joinToString(";")}" }
}

private val EXTRACTOR_ARG_SPEC_RE = Regex("""^([\w-]+)\s*:(.*)$""")

private fun YoutubeDLRequest.opt(flag: String, value: String?): YoutubeDLRequest {
    if (!value.isNullOrBlank()) addOption(flag, value.trim())
    return this
}

/**
 * Subset that is safe for metadata-only requests (`--dump-json`): no output,
 * post-processing or download-behaviour flags, only connection identity and
 * resilience overrides.
 */
fun YoutubeDLRequest.applyAdvancedForInfoFetch(
    advanced: YtDlpAdvancedOptions
): YoutubeDLRequest {
    if (advanced.isEmpty()) return this
    return apply {
        if (advanced.impersonateClient.isNotBlank()) {
            // Needs curl-cffi in the interpreter env; without it yt-dlp
            // reports the missing dependency and the fetch fails loudly.
            addOption("--impersonate", advanced.impersonateClient.trim())
        }
        if (advanced.noCheckCertificate) addOption("--no-check-certificate")
        if (advanced.geoBypass) addOption("--geo-bypass")
        if (advanced.sourceAddress.isNotBlank()) {
            addOption("--source-address", advanced.sourceAddress.trim())
        }
        if (advanced.referer.isNotBlank()) addOption("--referer", advanced.referer.trim())
        applyExtraHeaders(advanced.addHeaders)
        normalizeRetries(advanced.retries)?.let { addOption("-R", it) }
        if (advanced.socketTimeout.isNotBlank()) {
            addOption("--socket-timeout", advanced.socketTimeout.trim())
        }
        // NOTE: --extractor-args is intentionally NOT added here. Upstream
        // overwrites (not merges) same-extractor flags, so the extra is merged
        // with Seal's own flags at the emission sites via mergeExtractorArgs().
    }
}

/**
 * Full advanced set for real downloads. Must be applied LAST so explicit
 * values win over Seal presets (single-value options are last-wins).
 *
 * @param isAudio whether this request extracts audio (`-x`); gates
 *   `--keep-video`.
 * @param restrictFilenamesActive whether Seal already passed
 *   `--restrict-filenames`; gates `--windows-filenames`.
 */
fun YoutubeDLRequest.applyAdvancedForDownload(
    advanced: YtDlpAdvancedOptions,
    isAudio: Boolean,
    restrictFilenamesActive: Boolean,
): YoutubeDLRequest {
    if (advanced.isEmpty()) return this
    return apply {
        applyAdvancedForInfoFetch(advanced)

        if (advanced.httpChunkSize.isNotBlank()) {
            if (isFilesizeSpec(advanced.httpChunkSize)) {
                addOption("--http-chunk-size", advanced.httpChunkSize.trim())
            } else {
                warnLogger(TAG, "ignoring invalid --http-chunk-size: ${advanced.httpChunkSize}")
            }
        }
        normalizeRetries(advanced.fragmentRetries)?.let { addOption("--fragment-retries", it) }
        opt("--retry-sleep", advanced.retrySleep)

        opt("--playlist-start", advanced.playlistStart)
        opt("--playlist-end", advanced.playlistEnd)
        opt("--match-filter", advanced.matchFilter)
        if (advanced.minFilesize.isNotBlank()) {
            if (isFilesizeSpec(advanced.minFilesize)) {
                addOption("--min-filesize", advanced.minFilesize.trim())
            } else {
                warnLogger(TAG, "ignoring invalid --min-filesize: ${advanced.minFilesize}")
            }
        }
        if (advanced.maxFilesize.isNotBlank()) {
            if (isFilesizeSpec(advanced.maxFilesize)) {
                addOption("--max-filesize", advanced.maxFilesize.trim())
            } else {
                warnLogger(TAG, "ignoring invalid --max-filesize: ${advanced.maxFilesize}")
            }
        }
        if (advanced.liveFromStart) addOption("--live-from-start")
        if (advanced.waitForVideo.isNotBlank()) {
            if (isWaitForVideoSpec(advanced.waitForVideo)) {
                addOption("--wait-for-video", advanced.waitForVideo.trim())
            } else {
                warnLogger(TAG, "ignoring invalid --wait-for-video: ${advanced.waitForVideo}")
            }
        }

        if (advanced.windowsFilenames && !restrictFilenamesActive) {
            addOption("--windows-filenames")
        }
        if (advanced.forceOverwrite) addOption("--force-overwrites")
        if (advanced.writeInfoJson) addOption("--write-info-json")
        if (advanced.writeDescription) addOption("--write-description")

        if (advanced.preferFreeFormats) addOption("--prefer-free-formats")
        when (advanced.checkFormats.trim().lowercase()) {
            "no" -> addOption("--no-check-formats")
            "select", "yes", "check" -> addOption("--check-formats")
        }
        if (advanced.keepVideo && isAudio) addOption("--keep-video")
        opt("--recode-video", advanced.recodeVideo)
        opt("--remux-video", advanced.remuxVideo)
        opt("--postprocessor-args", advanced.postprocessorArgs)
        if (advanced.downloaderArgs.isNotBlank()) {
            addOption("--external-downloader-args", advanced.downloaderArgs.trim())
        }
        opt("--sponsorblock-mark", advanced.sponsorblockMark)
    }
}

private fun YoutubeDLRequest.applyExtraHeaders(raw: String) {
    if (raw.isBlank()) return
    // Split on newlines first; a ';' inside a value (e.g. `Cookie: a=b; c=d`)
    // must not start a new header — segments without ':' continue the previous one.
    raw.lineSequence()
        .flatMap { line ->
            val merged = mutableListOf<String>()
            for (part in line.split(';')) {
                val text = part.trim()
                if (text.isEmpty()) continue
                if (text.contains(':') || merged.isEmpty()) merged.add(text)
                else merged[merged.lastIndex] = merged.last() + ";" + part.trimEnd()
            }
            merged
        }
        .map { it.trim() }
        .filter { it.contains(':') }
        .forEach { addOption("--add-headers", it) }
}
