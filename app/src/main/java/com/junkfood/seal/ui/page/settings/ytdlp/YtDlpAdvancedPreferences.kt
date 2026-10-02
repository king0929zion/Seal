package com.junkfood.seal.ui.page.settings.ytdlp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.booleanState
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.PreferenceInfo
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.ui.component.PreferenceSubtitle
import com.junkfood.seal.ui.component.PreferenceSwitch
import com.junkfood.seal.util.CUSTOM_COMMAND
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.removeValue
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.YTDLP_ADD_HEADERS
import com.junkfood.seal.util.YTDLP_DOWNLOADER_ARGS
import com.junkfood.seal.util.YTDLP_EXTRACTOR_ARGS_EXTRA
import com.junkfood.seal.util.YTDLP_FORCE_OVERWRITE
import com.junkfood.seal.util.YTDLP_GEO_BYPASS
import com.junkfood.seal.util.YTDLP_IMPERSONATE
import com.junkfood.seal.util.YTDLP_KEEP_VIDEO
import com.junkfood.seal.util.YTDLP_LIVE_FROM_START
import com.junkfood.seal.util.YTDLP_MATCH_FILTER
import com.junkfood.seal.util.YTDLP_MAX_FILESIZE
import com.junkfood.seal.util.YTDLP_MIN_FILESIZE
import com.junkfood.seal.util.YTDLP_NO_CHECK_CERTIFICATE
import com.junkfood.seal.util.YTDLP_PLAYLIST_END
import com.junkfood.seal.util.YTDLP_PLAYLIST_START
import com.junkfood.seal.util.YTDLP_PP_ARGS
import com.junkfood.seal.util.YTDLP_PREFER_FREE_FORMATS
import com.junkfood.seal.util.YTDLP_RECODE_VIDEO
import com.junkfood.seal.util.YTDLP_REFERER
import com.junkfood.seal.util.YTDLP_REMUX_VIDEO
import com.junkfood.seal.util.YTDLP_RETRIES
import com.junkfood.seal.util.YTDLP_SOCKET_TIMEOUT
import com.junkfood.seal.util.YTDLP_WAIT_FOR_VIDEO
import com.junkfood.seal.util.YTDLP_WINDOWS_FILENAMES
import com.junkfood.seal.util.YTDLP_WRITE_DESCRIPTION
import com.junkfood.seal.util.YTDLP_WRITE_INFO_JSON
import com.junkfood.seal.util.YtDlpAdvancedOptions

private const val DIALOG_IDENTITY = "identity"
private const val DIALOG_RESILIENCE = "resilience"
private const val DIALOG_SELECTION = "selection"
private const val DIALOG_POST = "post"
private const val DIALOG_RESET = "reset"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YtDlpAdvancedPreferences(onNavigateBack: () -> Unit) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState(),
            canScroll = { true },
        )

    var dialog by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }

    // Revision-keyed so Reset (which clears MMKV directly) refreshes the UI.
    var noCheckCertificate by remember(revision) {
        mutableStateOf(YTDLP_NO_CHECK_CERTIFICATE.getBoolean())
    }
    var geoBypass by remember(revision) { mutableStateOf(YTDLP_GEO_BYPASS.getBoolean()) }
    var liveFromStart by remember(revision) { mutableStateOf(YTDLP_LIVE_FROM_START.getBoolean()) }
    var windowsFilenames by remember(revision) {
        mutableStateOf(YTDLP_WINDOWS_FILENAMES.getBoolean())
    }
    var forceOverwrite by remember(revision) {
        mutableStateOf(YTDLP_FORCE_OVERWRITE.getBoolean())
    }
    var writeInfoJson by remember(revision) {
        mutableStateOf(YTDLP_WRITE_INFO_JSON.getBoolean())
    }
    var writeDescription by remember(revision) {
        mutableStateOf(YTDLP_WRITE_DESCRIPTION.getBoolean())
    }
    var preferFreeFormats by remember(revision) {
        mutableStateOf(YTDLP_PREFER_FREE_FORMATS.getBoolean())
    }
    var keepVideo by remember(revision) { mutableStateOf(YTDLP_KEEP_VIDEO.getBoolean()) }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(id = R.string.ytdlp_advanced)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        content = {
            val isCustomCommandEnabled by CUSTOM_COMMAND.booleanState

            val identitySummary = remember(revision) {
                YTDLP_IMPERSONATE.getString().ifEmpty { null }
                    ?: YTDLP_REFERER.getString().ifEmpty { null }
                    ?: YTDLP_ADD_HEADERS.getString().lineSequence().firstOrNull()?.ifEmpty { null }
            }
            val resilienceSummary = remember(revision) {
                YTDLP_RETRIES.getString().ifEmpty { null }
                    ?: YTDLP_SOCKET_TIMEOUT.getString().ifEmpty { null }?.let { "$it s" }
            }
            val selectionSummary = remember(revision) {
                val start = YTDLP_PLAYLIST_START.getString()
                val end = YTDLP_PLAYLIST_END.getString()
                when {
                    start.isNotEmpty() || end.isNotEmpty() ->
                        "${start.ifEmpty { "1" }}–${end.ifEmpty { "∞" }}"
                    YTDLP_MATCH_FILTER.getString().isNotEmpty() -> YTDLP_MATCH_FILTER.getString()
                    YTDLP_MIN_FILESIZE.getString().isNotEmpty() ||
                        YTDLP_MAX_FILESIZE.getString().isNotEmpty() ->
                        "${YTDLP_MIN_FILESIZE.getString()}…${YTDLP_MAX_FILESIZE.getString()}"
                    YTDLP_WAIT_FOR_VIDEO.getString().isNotEmpty() -> YTDLP_WAIT_FOR_VIDEO.getString()
                    else -> null
                }
            }
            val postSummary = remember(revision) {
                YTDLP_REMUX_VIDEO.getString().ifEmpty { null }?.let { "remux:$it" }
                    ?: YTDLP_RECODE_VIDEO.getString().ifEmpty { null }?.let { "recode:$it" }
                    ?: YTDLP_PP_ARGS.getString().ifEmpty { null }
                    ?: YTDLP_DOWNLOADER_ARGS.getString().ifEmpty { null }
                    ?: YTDLP_EXTRACTOR_ARGS_EXTRA.getString().ifEmpty { null }
            }

            LazyColumn(contentPadding = it) {
                if (isCustomCommandEnabled)
                    item {
                        PreferenceInfo(
                            text = stringResource(id = R.string.custom_command_enabled_hint)
                        )
                    }
                item { PreferenceInfo(text = stringResource(id = R.string.ytdlp_advanced_hint)) }

                item { PreferenceSubtitle(text = stringResource(R.string.ytdlp_group_network)) }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.ytdlp_identity),
                        description =
                            identitySummary ?: stringResource(R.string.ytdlp_identity_desc),
                        icon = Icons.Outlined.VpnKey,
                        enabled = !isCustomCommandEnabled,
                    ) {
                        dialog = DIALOG_IDENTITY
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_no_check_certificate),
                        description = stringResource(R.string.ytdlp_no_check_certificate_desc),
                        icon = Icons.Outlined.ErrorOutline,
                        enabled = !isCustomCommandEnabled,
                        isChecked = noCheckCertificate,
                    ) {
                        noCheckCertificate = !noCheckCertificate
                        YTDLP_NO_CHECK_CERTIFICATE.updateBoolean(noCheckCertificate)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_geo_bypass),
                        description = stringResource(R.string.ytdlp_geo_bypass_desc),
                        icon = Icons.Outlined.SyncAlt,
                        enabled = !isCustomCommandEnabled,
                        isChecked = geoBypass,
                    ) {
                        geoBypass = !geoBypass
                        YTDLP_GEO_BYPASS.updateBoolean(geoBypass)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.ytdlp_resilience),
                        description =
                            resilienceSummary ?: stringResource(R.string.ytdlp_resilience_desc),
                        icon = Icons.Outlined.RestartAlt,
                        enabled = !isCustomCommandEnabled,
                    ) {
                        dialog = DIALOG_RESILIENCE
                    }
                }

                item { PreferenceSubtitle(text = stringResource(R.string.ytdlp_group_selection)) }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.ytdlp_selection),
                        description =
                            selectionSummary ?: stringResource(R.string.ytdlp_selection_desc),
                        icon = Icons.Outlined.Search,
                        enabled = !isCustomCommandEnabled,
                    ) {
                        dialog = DIALOG_SELECTION
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_live_from_start),
                        description = stringResource(R.string.ytdlp_live_from_start_desc),
                        icon = Icons.Outlined.Subscriptions,
                        enabled = !isCustomCommandEnabled,
                        isChecked = liveFromStart,
                    ) {
                        liveFromStart = !liveFromStart
                        YTDLP_LIVE_FROM_START.updateBoolean(liveFromStart)
                    }
                }

                item { PreferenceSubtitle(text = stringResource(R.string.ytdlp_group_files)) }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_windows_filenames),
                        description = stringResource(R.string.ytdlp_windows_filenames_desc),
                        icon = Icons.Outlined.SnippetFolder,
                        enabled = !isCustomCommandEnabled,
                        isChecked = windowsFilenames,
                    ) {
                        windowsFilenames = !windowsFilenames
                        YTDLP_WINDOWS_FILENAMES.updateBoolean(windowsFilenames)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_force_overwrite),
                        description = stringResource(R.string.ytdlp_force_overwrite_desc),
                        icon = Icons.Outlined.Sync,
                        enabled = !isCustomCommandEnabled,
                        isChecked = forceOverwrite,
                    ) {
                        forceOverwrite = !forceOverwrite
                        YTDLP_FORCE_OVERWRITE.updateBoolean(forceOverwrite)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_write_info_json),
                        description = stringResource(R.string.ytdlp_write_info_json_desc),
                        icon = Icons.Outlined.Code,
                        enabled = !isCustomCommandEnabled,
                        isChecked = writeInfoJson,
                    ) {
                        writeInfoJson = !writeInfoJson
                        YTDLP_WRITE_INFO_JSON.updateBoolean(writeInfoJson)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_write_description),
                        description = stringResource(R.string.ytdlp_write_description_desc),
                        icon = Icons.Outlined.Edit,
                        enabled = !isCustomCommandEnabled,
                        isChecked = writeDescription,
                    ) {
                        writeDescription = !writeDescription
                        YTDLP_WRITE_DESCRIPTION.updateBoolean(writeDescription)
                    }
                }

                item { PreferenceSubtitle(text = stringResource(R.string.ytdlp_group_post)) }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_prefer_free_formats),
                        description = stringResource(R.string.ytdlp_prefer_free_formats_desc),
                        icon = Icons.Outlined.HighQuality,
                        enabled = !isCustomCommandEnabled,
                        isChecked = preferFreeFormats,
                    ) {
                        preferFreeFormats = !preferFreeFormats
                        YTDLP_PREFER_FREE_FORMATS.updateBoolean(preferFreeFormats)
                    }
                }
                item {
                    PreferenceSwitch(
                        title = stringResource(R.string.ytdlp_keep_video),
                        description = stringResource(R.string.ytdlp_keep_video_desc),
                        icon = Icons.Outlined.VideoFile,
                        enabled = !isCustomCommandEnabled,
                        isChecked = keepVideo,
                    ) {
                        keepVideo = !keepVideo
                        YTDLP_KEEP_VIDEO.updateBoolean(keepVideo)
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.ytdlp_postprocessing),
                        description =
                            postSummary ?: stringResource(R.string.ytdlp_postprocessing_desc),
                        icon = Icons.Outlined.Settings,
                        enabled = !isCustomCommandEnabled,
                    ) {
                        dialog = DIALOG_POST
                    }
                }
                item {
                    PreferenceItem(
                        title = stringResource(R.string.ytdlp_reset),
                        description = stringResource(R.string.ytdlp_reset_desc),
                        icon = Icons.Outlined.Restore,
                    ) {
                        dialog = DIALOG_RESET
                    }
                }
            }
        },
    )

    when (dialog) {
        DIALOG_IDENTITY ->
            IdentityDialog {
                dialog = null
                revision++
            }
        DIALOG_RESILIENCE ->
            ResilienceDialog {
                dialog = null
                revision++
            }
        DIALOG_SELECTION ->
            SelectionDialog {
                dialog = null
                revision++
            }
        DIALOG_POST ->
            PostprocessingDialog {
                dialog = null
                revision++
            }
        DIALOG_RESET ->
            ResetAdvancedDialog(
                onDismissRequest = { dialog = null },
                onReset = {
                    YtDlpAdvancedOptions.preferenceKeys.forEach { it.removeValue() }
                    revision++
                },
            )
    }
}
