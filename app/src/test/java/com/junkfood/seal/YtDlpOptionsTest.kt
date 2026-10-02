package com.junkfood.seal

import com.junkfood.seal.util.YtDlpAdvancedOptions
import com.junkfood.seal.util.YtDlpBundle
import com.junkfood.seal.util.applyAdvancedForDownload
import com.junkfood.seal.util.applyAdvancedForInfoFetch
import com.junkfood.seal.util.isFilesizeSpec
import com.junkfood.seal.util.isValidDigitsOrEmpty
import com.junkfood.seal.util.isValidPlaylistRange
import com.junkfood.seal.util.isValidRetries
import com.junkfood.seal.util.isWaitForVideoSpec
import com.junkfood.seal.util.mergeExtractorArgs
import com.junkfood.seal.util.normalizeRetries
import com.yausername.youtubedl_android.YoutubeDLRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Host-side tests for the vendored yt-dlp options layer ([YtDlpAdvancedOptions]).
 *
 * Only valid inputs are exercised here: invalid values are dropped with
 * `android.util.Log` warnings on-device, and `Log` is not mocked on the JVM.
 * Validation rules themselves are covered via [isFilesizeSpec] /
 * [isWaitForVideoSpec], which are pure functions.
 */
class YtDlpOptionsTest {

    private val url = "https://example.com/video"

    private fun List<String>.argAfter(flag: String): String? {
        val i = indexOf(flag)
        return if (i >= 0 && i + 1 < size) this[i + 1] else null
    }

    @Test
    fun emptyAdvancedAddsNothing() {
        val fetch = YoutubeDLRequest(url).applyAdvancedForInfoFetch(YtDlpAdvancedOptions()).buildCommand()
        assertEquals(listOf(url), fetch)

        val download =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(),
                    isAudio = false,
                    restrictFilenamesActive = false,
                )
                .buildCommand()
        assertEquals(listOf(url), download)
    }

    @Test
    fun infoFetchAppliesNetworkSubsetOnly() {
        val cmd =
            YoutubeDLRequest(url)
                .applyAdvancedForInfoFetch(
                    YtDlpAdvancedOptions(
                        impersonateClient = "chrome",
                        noCheckCertificate = true,
                        geoBypass = true,
                        sourceAddress = "0.0.0.0",
                        referer = "https://example.com/",
                        retries = "5",
                        socketTimeout = "10",
                        extractorArgsExtra = "youtube:player_client=android",
                    )
                )
                .buildCommand()

        assertEquals("chrome", cmd.argAfter("--impersonate"))
        assertTrue(cmd.contains("--no-check-certificate"))
        assertTrue(cmd.contains("--geo-bypass"))
        assertEquals("0.0.0.0", cmd.argAfter("--source-address"))
        assertEquals("https://example.com/", cmd.argAfter("--referer"))
        assertEquals("5", cmd.argAfter("-R"))
        assertEquals("10", cmd.argAfter("--socket-timeout"))
        // --extractor-args is merged at the emission sites (mergeExtractorArgs),
        // never appended here: upstream overwrites same-extractor flags.
        assertFalse(cmd.contains("--extractor-args"))
        // Download-only flags must not leak into metadata requests.
        assertFalse(cmd.contains("--write-info-json"))
        assertFalse(cmd.contains("--match-filter"))
        assertFalse(cmd.contains("--keep-video"))
        assertFalse(cmd.contains("--postprocessor-args"))
    }

    @Test
    fun downloadAppliesFullSetLast() {
        val cmd =
            YoutubeDLRequest(url)
                .apply { addOption("-R", "1") }
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(
                        retries = "infinite",
                        fragmentRetries = "3",
                        playlistStart = "2",
                        matchFilter = "duration < 600",
                        minFilesize = "50M",
                        liveFromStart = true,
                        forceOverwrite = true,
                        writeInfoJson = true,
                        preferFreeFormats = true,
                        recodeVideo = "mp4",
                        postprocessorArgs = "ffmpeg:-crf 23",
                        downloaderArgs = "aria2c:--max-connection-per-server=4",
                        sponsorblockMark = "all",
                    ),
                    isAudio = false,
                    restrictFilenamesActive = false,
                )
                .buildCommand()

        // Advanced values are appended after presets (yt-dlp uses last-wins
        // for single-value options), so both occurrences exist and ours is last.
        assertEquals(listOf("1", "infinite"), cmd.windowed(2).filter { it[0] == "-R" }.map { it[1] })
        assertEquals("3", cmd.argAfter("--fragment-retries"))
        assertEquals("2", cmd.argAfter("--playlist-start"))
        assertEquals("duration < 600", cmd.argAfter("--match-filter"))
        assertEquals("50M", cmd.argAfter("--min-filesize"))
        assertTrue(cmd.contains("--live-from-start"))
        assertTrue(cmd.contains("--force-overwrites"))
        assertTrue(cmd.contains("--write-info-json"))
        assertTrue(cmd.contains("--prefer-free-formats"))
        assertEquals("mp4", cmd.argAfter("--recode-video"))
        assertEquals("ffmpeg:-crf 23", cmd.argAfter("--postprocessor-args"))
        assertEquals(
            "aria2c:--max-connection-per-server=4",
            cmd.argAfter("--external-downloader-args"),
        )
        assertEquals("all", cmd.argAfter("--sponsorblock-mark"))
    }

    @Test
    fun keepVideoGatedOnAudio() {
        val audio =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(keepVideo = true),
                    isAudio = true,
                    restrictFilenamesActive = false,
                )
                .buildCommand()
        assertTrue(audio.contains("--keep-video"))

        val video =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(keepVideo = true),
                    isAudio = false,
                    restrictFilenamesActive = false,
                )
                .buildCommand()
        assertFalse(video.contains("--keep-video"))
    }

    @Test
    fun windowsFilenamesGatedOnRestrict() {
        val withoutRestrict =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(windowsFilenames = true),
                    isAudio = false,
                    restrictFilenamesActive = false,
                )
                .buildCommand()
        assertTrue(withoutRestrict.contains("--windows-filenames"))

        val withRestrict =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(windowsFilenames = true),
                    isAudio = false,
                    restrictFilenamesActive = true,
                )
                .buildCommand()
        assertFalse(withRestrict.contains("--windows-filenames"))
    }

    @Test
    fun checkFormatsTriState() {
        fun flagsFor(value: String) =
            YoutubeDLRequest(url)
                .applyAdvancedForDownload(
                    YtDlpAdvancedOptions(checkFormats = value),
                    isAudio = false,
                    restrictFilenamesActive = false,
                )
                .buildCommand()

        assertFalse(flagsFor("").contains("--no-check-formats"))
        assertTrue(flagsFor("no").contains("--no-check-formats"))
        assertTrue(flagsFor("select").contains("--check-formats"))
    }

    @Test
    fun extraHeadersSplitAndInvalidSkipped() {
        val cmd =
            YoutubeDLRequest(url)
                .applyAdvancedForInfoFetch(
                    YtDlpAdvancedOptions(addHeaders = "X-A: 1\nY-B:2\nnot-a-header")
                )
                .buildCommand()

        val headers = cmd.windowed(2).filter { it[0] == "--add-headers" }.map { it[1] }
        assertEquals(listOf("X-A: 1", "Y-B:2"), headers)
    }

    @Test
    fun headerValueContainingSemicolonIsKeptWhole() {
        // e.g. Cookie values must not be split at ';' into a broken header.
        val cmd =
            YoutubeDLRequest(url)
                .applyAdvancedForInfoFetch(
                    YtDlpAdvancedOptions(addHeaders = "Cookie: a=b; c=d\nX-A: 1")
                )
                .buildCommand()

        val headers = cmd.windowed(2).filter { it[0] == "--add-headers" }.map { it[1] }
        assertEquals(listOf("Cookie: a=b; c=d", "X-A: 1"), headers)
    }

    @Test
    fun retriesNormalizedToLowercaseInfinite() {
        val cmd =
            YoutubeDLRequest(url)
                .applyAdvancedForInfoFetch(YtDlpAdvancedOptions(retries = "Infinite"))
                .buildCommand()
        // Upstream parse_retries only accepts lowercase inf/infinite.
        assertEquals("infinite", cmd.argAfter("-R"))
    }

    @Test
    fun extractorArgsMergedByKey() {
        // Same-extractor specs join with ';' (upstream overwrites repeated flags).
        assertEquals(
            listOf("youtube:skip=translated_subs;player_client=android"),
            mergeExtractorArgs("youtube:skip=translated_subs", "youtube:player_client=android"),
        )
        // Different extractors stay as separate flags, first-seen order kept.
        assertEquals(
            listOf("generic:foo=bar", "youtube:player_client=android"),
            mergeExtractorArgs("generic:foo=bar", "", "youtube:player_client=android"),
        )
        // Malformed specs are skipped, blanks ignored.
        assertEquals(
            listOf("youtube:player_client=android"),
            mergeExtractorArgs("no-colon-here", "  ", "youtube:player_client=android"),
        )
        assertEquals(emptyList<String>(), mergeExtractorArgs("", "   "))
    }

    @Test
    fun playlistRangeValidator() {
        assertTrue(isValidPlaylistRange("", ""))
        assertTrue(isValidPlaylistRange("2", ""))
        assertTrue(isValidPlaylistRange("", "10"))
        assertTrue(isValidPlaylistRange("2", "10"))
        assertTrue(isValidPlaylistRange("5", "5"))
        assertFalse(isValidPlaylistRange("10", "2"))
        assertFalse(isValidPlaylistRange("abc", "10"))
        assertFalse(isValidPlaylistRange("2", "xyz"))
    }

    @Test
    fun digitsValidator() {
        assertTrue(isValidDigitsOrEmpty(""))
        assertTrue(isValidDigitsOrEmpty("60"))
        assertFalse(isValidDigitsOrEmpty("6a"))
    }

    @Test
    fun retriesValidator() {
        assertTrue(isValidRetries(""))
        assertTrue(isValidRetries("10"))
        assertTrue(isValidRetries("inf"))
        assertTrue(isValidRetries("Infinite"))
        assertFalse(isValidRetries("10x"))
        assertFalse(isValidRetries("-1"))
        assertFalse(isValidRetries("1.5"))
    }

    @Test
    fun normalizeRetriesValues() {
        assertEquals(null, normalizeRetries(""))
        assertEquals(null, normalizeRetries("  "))
        assertEquals(null, normalizeRetries("lots"))
        assertEquals("infinite", normalizeRetries("infinite"))
        assertEquals("infinite", normalizeRetries("Infinite"))
        assertEquals("infinite", normalizeRetries("INF"))
        assertEquals("3", normalizeRetries("3"))
        assertEquals("3", normalizeRetries("  3  "))
    }

    @Test
    fun bundleHexMatchesKnownVector() {
        // SHA-256("abc"), guards the lookup-table hex encoder.
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val actual =
            YtDlpBundle.run {
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest("abc".toByteArray())
                    .toHexFast()
            }
        assertEquals(expected, actual)
    }

    @Test
    fun filesizeValidator() {
        assertTrue(isFilesizeSpec("1024"))
        assertTrue(isFilesizeSpec("50M"))
        assertTrue(isFilesizeSpec("1.5G"))
        assertTrue(isFilesizeSpec("10k"))
        assertFalse(isFilesizeSpec(""))
        assertFalse(isFilesizeSpec("10MB"))
        assertFalse(isFilesizeSpec("abc"))
        assertFalse(isFilesizeSpec("10 M"))
    }

    @Test
    fun waitForVideoValidator() {
        assertTrue(isWaitForVideoSpec("60"))
        assertTrue(isWaitForVideoSpec("30-120"))
        assertFalse(isWaitForVideoSpec(""))
        assertFalse(isWaitForVideoSpec("60s"))
        assertFalse(isWaitForVideoSpec("-5"))
    }
}
