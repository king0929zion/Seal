package com.junkfood.seal.ui.page.settings.ytdlp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.LinkButton
import com.junkfood.seal.ui.component.OutlinedButtonChip
import com.junkfood.seal.ui.page.settings.general.ytdlpReference
import com.junkfood.seal.util.PreferenceUtil.getString
import com.junkfood.seal.util.PreferenceUtil.updateString
import com.junkfood.seal.util.YTDLP_ADD_HEADERS
import com.junkfood.seal.util.YTDLP_CHECK_FORMATS
import com.junkfood.seal.util.YTDLP_DOWNLOADER_ARGS
import com.junkfood.seal.util.YTDLP_EXTRACTOR_ARGS_EXTRA
import com.junkfood.seal.util.YTDLP_FRAGMENT_RETRIES
import com.junkfood.seal.util.YTDLP_HTTP_CHUNK_SIZE
import com.junkfood.seal.util.YTDLP_IMPERSONATE
import com.junkfood.seal.util.YTDLP_MATCH_FILTER
import com.junkfood.seal.util.YTDLP_MAX_FILESIZE
import com.junkfood.seal.util.YTDLP_MIN_FILESIZE
import com.junkfood.seal.util.YTDLP_PLAYLIST_END
import com.junkfood.seal.util.YTDLP_PLAYLIST_START
import com.junkfood.seal.util.YTDLP_PP_ARGS
import com.junkfood.seal.util.YTDLP_RECODE_VIDEO
import com.junkfood.seal.util.YTDLP_REFERER
import com.junkfood.seal.util.YTDLP_REMUX_VIDEO
import com.junkfood.seal.util.YTDLP_RETRIES
import com.junkfood.seal.util.YTDLP_RETRY_SLEEP
import com.junkfood.seal.util.YTDLP_SOCKET_TIMEOUT
import com.junkfood.seal.util.YTDLP_SOURCE_ADDRESS
import com.junkfood.seal.util.YTDLP_SPONSORBLOCK_MARK
import com.junkfood.seal.util.YTDLP_WAIT_FOR_VIDEO
import com.junkfood.seal.util.isFilesizeSpec
import com.junkfood.seal.util.isValidDigitsOrEmpty
import com.junkfood.seal.util.isValidPlaylistRange
import com.junkfood.seal.util.isValidRetries
import com.junkfood.seal.util.isWaitForVideoSpec

@Composable
private fun YtDlpField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { text -> { Text(text) } },
        supportingText = supportingText?.let { text -> { Text(text) } },
        isError = isError,
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YtDlpDialogShell(
    onDismissRequest: () -> Unit,
    icon: @Composable () -> Unit,
    title: String,
    canConfirm: Boolean,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        title = { Text(title) },
        text = { Column { content() } },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton(enabled = canConfirm) {
                onConfirm()
                onDismissRequest()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityDialog(onDismissRequest: () -> Unit) {
    var impersonate by remember { mutableStateOf(YTDLP_IMPERSONATE.getString()) }
    var referer by remember { mutableStateOf(YTDLP_REFERER.getString()) }
    var sourceAddress by remember { mutableStateOf(YTDLP_SOURCE_ADDRESS.getString()) }
    var addHeaders by remember { mutableStateOf(YTDLP_ADD_HEADERS.getString()) }

    YtDlpDialogShell(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.VpnKey, null) },
        title = stringResource(R.string.ytdlp_identity),
        canConfirm = true,
        onConfirm = {
            YTDLP_IMPERSONATE.updateString(impersonate.trim())
            YTDLP_REFERER.updateString(referer.trim())
            YTDLP_SOURCE_ADDRESS.updateString(sourceAddress.trim())
            YTDLP_ADD_HEADERS.updateString(addHeaders.trim())
        },
    ) {
        Text(
            stringResource(R.string.ytdlp_impersonate_desc),
            style = MaterialTheme.typography.bodyMedium,
        )
        YtDlpField(
            value = impersonate,
            onValueChange = { impersonate = it },
            label = stringResource(R.string.ytdlp_impersonate),
            placeholder = "chrome",
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("", "chrome", "firefox", "safari").forEach {
                item {
                    OutlinedButtonChip(label = it.ifEmpty { stringResource(R.string.none) }) {
                        impersonate = it
                    }
                }
            }
        }
        YtDlpField(
            value = referer,
            onValueChange = { referer = it },
            label = stringResource(R.string.ytdlp_referer),
            placeholder = "https://",
        )
        YtDlpField(
            value = sourceAddress,
            onValueChange = { sourceAddress = it },
            label = stringResource(R.string.ytdlp_source_address),
        )
        YtDlpField(
            value = addHeaders,
            onValueChange = { addHeaders = it },
            label = stringResource(R.string.ytdlp_add_headers),
            supportingText = stringResource(R.string.ytdlp_add_headers_desc),
            minLines = 3,
        )
        LinkButton(link = ytdlpReference)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResilienceDialog(onDismissRequest: () -> Unit) {
    var retries by remember { mutableStateOf(YTDLP_RETRIES.getString()) }
    var fragmentRetries by remember { mutableStateOf(YTDLP_FRAGMENT_RETRIES.getString()) }
    var retrySleep by remember { mutableStateOf(YTDLP_RETRY_SLEEP.getString()) }
    var socketTimeout by remember { mutableStateOf(YTDLP_SOCKET_TIMEOUT.getString()) }
    var httpChunkSize by remember { mutableStateOf(YTDLP_HTTP_CHUNK_SIZE.getString()) }

    val retriesOk = isValidRetries(retries)
    val fragmentOk = isValidRetries(fragmentRetries)
    val timeoutOk = isValidDigitsOrEmpty(socketTimeout)
    val chunkOk = httpChunkSize.isEmpty() || isFilesizeSpec(httpChunkSize)

    YtDlpDialogShell(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.RestartAlt, null) },
        title = stringResource(R.string.ytdlp_resilience),
        canConfirm = retriesOk && fragmentOk && timeoutOk && chunkOk,
        onConfirm = {
            YTDLP_RETRIES.updateString(retries.trim())
            YTDLP_FRAGMENT_RETRIES.updateString(fragmentRetries.trim())
            YTDLP_RETRY_SLEEP.updateString(retrySleep.trim())
            YTDLP_SOCKET_TIMEOUT.updateString(socketTimeout.trim())
            YTDLP_HTTP_CHUNK_SIZE.updateString(httpChunkSize.trim())
        },
    ) {
        YtDlpField(
            value = retries,
            onValueChange = { retries = it },
            label = stringResource(R.string.ytdlp_retries),
            placeholder = stringResource(R.string.ytdlp_retries_desc),
            isError = !retriesOk,
            keyboardType = KeyboardType.Text,
        )
        YtDlpField(
            value = fragmentRetries,
            onValueChange = { fragmentRetries = it },
            label = stringResource(R.string.ytdlp_fragment_retries),
            placeholder = stringResource(R.string.ytdlp_retries_desc),
            isError = !fragmentOk,
        )
        YtDlpField(
            value = retrySleep,
            onValueChange = { retrySleep = it },
            label = stringResource(R.string.ytdlp_retry_sleep),
            placeholder = stringResource(R.string.ytdlp_retry_sleep_desc),
        )
        YtDlpField(
            value = socketTimeout,
            onValueChange = { socketTimeout = it },
            label = stringResource(R.string.ytdlp_socket_timeout),
            isError = !timeoutOk,
            keyboardType = KeyboardType.Number,
        )
        YtDlpField(
            value = httpChunkSize,
            onValueChange = { httpChunkSize = it },
            label = stringResource(R.string.ytdlp_http_chunk_size),
            supportingText = stringResource(R.string.ytdlp_http_chunk_size_desc),
            isError = !chunkOk,
        )
        if (!retriesOk || !fragmentOk || !timeoutOk || !chunkOk) {
            Text(
                stringResource(R.string.invalid_input),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDialog(onDismissRequest: () -> Unit) {
    var playlistStart by remember { mutableStateOf(YTDLP_PLAYLIST_START.getString()) }
    var playlistEnd by remember { mutableStateOf(YTDLP_PLAYLIST_END.getString()) }
    var matchFilter by remember { mutableStateOf(YTDLP_MATCH_FILTER.getString()) }
    var minFilesize by remember { mutableStateOf(YTDLP_MIN_FILESIZE.getString()) }
    var maxFilesize by remember { mutableStateOf(YTDLP_MAX_FILESIZE.getString()) }
    var waitForVideo by remember { mutableStateOf(YTDLP_WAIT_FOR_VIDEO.getString()) }

    val startOk = isValidDigitsOrEmpty(playlistStart)
    val endOk = isValidDigitsOrEmpty(playlistEnd)
    val rangeOk = isValidPlaylistRange(playlistStart, playlistEnd)
    val minOk = minFilesize.isEmpty() || isFilesizeSpec(minFilesize)
    val maxOk = maxFilesize.isEmpty() || isFilesizeSpec(maxFilesize)
    val waitOk = waitForVideo.isEmpty() || isWaitForVideoSpec(waitForVideo)

    YtDlpDialogShell(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Search, null) },
        title = stringResource(R.string.ytdlp_selection),
        canConfirm = startOk && endOk && rangeOk && minOk && maxOk && waitOk,
        onConfirm = {
            YTDLP_PLAYLIST_START.updateString(playlistStart.trim())
            YTDLP_PLAYLIST_END.updateString(playlistEnd.trim())
            YTDLP_MATCH_FILTER.updateString(matchFilter.trim())
            YTDLP_MIN_FILESIZE.updateString(minFilesize.trim())
            YTDLP_MAX_FILESIZE.updateString(maxFilesize.trim())
            YTDLP_WAIT_FOR_VIDEO.updateString(waitForVideo.trim())
        },
    ) {
        YtDlpField(
            value = playlistStart,
            onValueChange = { playlistStart = it },
            label = stringResource(R.string.ytdlp_playlist_start),
            isError = !startOk || !rangeOk,
            keyboardType = KeyboardType.Number,
        )
        YtDlpField(
            value = playlistEnd,
            onValueChange = { playlistEnd = it },
            label = stringResource(R.string.ytdlp_playlist_end),
            isError = !endOk || !rangeOk,
            keyboardType = KeyboardType.Number,
        )
        YtDlpField(
            value = matchFilter,
            onValueChange = { matchFilter = it },
            label = stringResource(R.string.ytdlp_match_filter),
            placeholder = stringResource(R.string.ytdlp_match_filter_desc),
        )
        YtDlpField(
            value = minFilesize,
            onValueChange = { minFilesize = it },
            label = stringResource(R.string.ytdlp_min_filesize),
            placeholder = stringResource(R.string.ytdlp_filesize_desc),
            isError = !minOk,
        )
        YtDlpField(
            value = maxFilesize,
            onValueChange = { maxFilesize = it },
            label = stringResource(R.string.ytdlp_max_filesize),
            placeholder = stringResource(R.string.ytdlp_filesize_desc),
            isError = !maxOk,
        )
        YtDlpField(
            value = waitForVideo,
            onValueChange = { waitForVideo = it },
            label = stringResource(R.string.ytdlp_wait_for_video),
            supportingText = stringResource(R.string.ytdlp_wait_for_video_desc),
            isError = !waitOk,
        )
        if (!startOk || !endOk || !rangeOk || !minOk || !maxOk || !waitOk) {
            Text(
                stringResource(R.string.invalid_input),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostprocessingDialog(onDismissRequest: () -> Unit) {
    var checkFormats by remember { mutableStateOf(YTDLP_CHECK_FORMATS.getString()) }
    var recodeVideo by remember { mutableStateOf(YTDLP_RECODE_VIDEO.getString()) }
    var remuxVideo by remember { mutableStateOf(YTDLP_REMUX_VIDEO.getString()) }
    var ppArgs by remember { mutableStateOf(YTDLP_PP_ARGS.getString()) }
    var downloaderArgs by remember { mutableStateOf(YTDLP_DOWNLOADER_ARGS.getString()) }
    var extractorArgs by remember { mutableStateOf(YTDLP_EXTRACTOR_ARGS_EXTRA.getString()) }
    var sponsorblockMark by remember { mutableStateOf(YTDLP_SPONSORBLOCK_MARK.getString()) }

    YtDlpDialogShell(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Settings, null) },
        title = stringResource(R.string.ytdlp_postprocessing),
        canConfirm = true,
        onConfirm = {
            YTDLP_CHECK_FORMATS.updateString(checkFormats)
            YTDLP_RECODE_VIDEO.updateString(recodeVideo.trim())
            YTDLP_REMUX_VIDEO.updateString(remuxVideo.trim())
            YTDLP_PP_ARGS.updateString(ppArgs.trim())
            YTDLP_DOWNLOADER_ARGS.updateString(downloaderArgs.trim())
            YTDLP_EXTRACTOR_ARGS_EXTRA.updateString(extractorArgs.trim())
            YTDLP_SPONSORBLOCK_MARK.updateString(sponsorblockMark.trim())
        },
    ) {
        Text(
            stringResource(R.string.ytdlp_check_formats_desc),
            style = MaterialTheme.typography.bodyMedium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "" to stringResource(R.string.ytdlp_check_formats_default),
                "select" to "check",
                "no" to "no-check",
            ).forEach { (value, label) ->
                item {
                    OutlinedButtonChip(label = label) { checkFormats = value }
                }
            }
        }
        YtDlpField(
            value = recodeVideo,
            onValueChange = { recodeVideo = it },
            label = stringResource(R.string.ytdlp_recode_video),
            placeholder = "mp4",
        )
        YtDlpField(
            value = remuxVideo,
            onValueChange = { remuxVideo = it },
            label = stringResource(R.string.ytdlp_remux_video),
            supportingText = stringResource(R.string.ytdlp_remux_video_desc),
            placeholder = "mkv",
        )
        YtDlpField(
            value = ppArgs,
            onValueChange = { ppArgs = it },
            label = stringResource(R.string.ytdlp_pp_args),
            placeholder = stringResource(R.string.ytdlp_pp_args_desc),
        )
        YtDlpField(
            value = downloaderArgs,
            onValueChange = { downloaderArgs = it },
            label = stringResource(R.string.ytdlp_downloader_args),
            placeholder = stringResource(R.string.ytdlp_downloader_args_desc),
        )
        YtDlpField(
            value = extractorArgs,
            onValueChange = { extractorArgs = it },
            label = stringResource(R.string.ytdlp_extractor_args),
            placeholder = stringResource(R.string.ytdlp_extractor_args_desc),
        )
        YtDlpField(
            value = sponsorblockMark,
            onValueChange = { sponsorblockMark = it },
            label = stringResource(R.string.ytdlp_sponsorblock_mark),
            placeholder = stringResource(R.string.ytdlp_sponsorblock_mark_desc),
        )
        LinkButton(link = ytdlpReference)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetAdvancedDialog(onDismissRequest: () -> Unit, onReset: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.Restore, null) },
        title = { Text(stringResource(R.string.ytdlp_reset)) },
        text = { Text(stringResource(R.string.ytdlp_reset_confirm)) },
        dismissButton = { DismissButton { onDismissRequest() } },
        confirmButton = {
            ConfirmButton {
                onReset()
                onDismissRequest()
            }
        },
    )
}
