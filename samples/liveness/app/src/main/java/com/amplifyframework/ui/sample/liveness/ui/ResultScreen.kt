package com.amplifyframework.ui.sample.liveness.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amplifyframework.ui.liveness.model.FaceLivenessDetectionException
import com.amplifyframework.ui.sample.liveness.BuildConfig
import com.amplifyframework.ui.sample.liveness.MainViewModel
import com.amplifyframework.ui.sample.liveness.R
import com.amplifyframework.ui.sample.liveness.ResultData
import com.amplifyframework.ui.sample.liveness.ui.theme.MyApplicationTheme
import com.amplifyframework.ui.sample.liveness.ui.theme.onSuccessContainer
import com.amplifyframework.ui.sample.liveness.ui.theme.successContainer
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
fun ResultScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val fetchingResult = viewModel.fetchingResult.collectAsState().value
    val resultData = viewModel.resultData.collectAsState().value ?: return

    if (fetchingResult) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
        }
    } else {
        ResultsView(resultData, onTryAgain = onBack)
    }
}

@Composable
private fun ResultsView(resultData: ResultData, onTryAgain: () -> Unit) {
    ResultsView(
        sessionId = resultData.sessionId,
        tryAgainButtonOnClick = onTryAgain,
        error = resultData.error,
        isLive = resultData.isLive,
        confidenceScore = if (BuildConfig.SHOW_DEBUG_UI) resultData.confidenceScore else { null },
        referenceImage = if (BuildConfig.SHOW_DEBUG_UI) resultData.referenceImage else { null }
    )
}

@Composable
private fun ResultsView(sessionId: String,
                        tryAgainButtonOnClick: () -> Unit,
                        error: FaceLivenessDetectionException? = null,
                        isLive: Boolean = false,
                        confidenceScore: Float? = 0f,
                        referenceImage: Bitmap? = null) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.background)
        .padding(16.dp)
    ) {

        Column(modifier = Modifier
            .verticalScroll(rememberScrollState())
            .weight(1f)
            .fillMaxWidth()
        ) {
            Text(
                text = if (error != null) {
                    stringResource(id = R.string.liveness_check)
                } else {
                    stringResource(id = R.string.liveness_result)
                },
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 16.dp)
            ) {
                Column(modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.session_id_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = sessionId,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(sessionId))
                        // Show a toast for Android 12 and lower
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                            Toast
                                .makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_content_copy_24),
                        contentDescription = stringResource(id = R.string.copy_session_id),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (error != null) {
                val displayError = getDisplayError(error)

                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_error_24),
                        contentDescription = stringResource(id = R.string.error),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.concatenated_error, displayError.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = displayError.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row {
                        Text(
                            text = stringResource(R.string.result_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(Modifier.width(4.dp))

                        Text(
                            text = if (isLive) {
                                stringResource(id = R.string.check_successful)
                            } else {
                                stringResource(id = R.string.check_unsuccessful)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    confidenceScore?.let {score ->
                        Row {
                            Text(
                                text = stringResource(id = R.string.confidence_score_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = formattedConfidenceScore(score),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isLive) {
                                    MaterialTheme.colorScheme.onSuccessContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier
                                    .background(
                                        color = if (isLive) {
                                            MaterialTheme.colorScheme.successContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        },
                                        shape = MaterialTheme.shapes.extraLarge
                                    )
                                    .padding(start = 16.dp, end = 16.dp)
                            )
                        }
                    }
                }
                referenceImage?.let { image ->
                    Spacer(modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp))
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.reference_image_content_description),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        alignment = Alignment.Center,
                        contentScale = ContentScale.FillHeight
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = tryAgainButtonOnClick
        ) {
            Text(text = stringResource(id = R.string.try_again))
        }
    }
}

private data class DisplayError(val title: String, val message: String)

@Composable
private fun getDisplayError(error: FaceLivenessDetectionException): DisplayError {
    return when (error) {
        is FaceLivenessDetectionException.CameraPermissionDeniedException -> {
            DisplayError(
                title = stringResource(id = R.string.error_camera_permission_denied_title),
                message = stringResource(id = R.string.error_camera_permission_denied_message)
            )
        }
        is FaceLivenessDetectionException.SessionTimedOutException -> {
            DisplayError(
                title = stringResource(id = R.string.error_timed_out_title),
                message = if (error.message.contains("did not match oval", true)) {
                    stringResource(id = R.string.error_timed_out_face_fit_message)
                } else {
                    stringResource(id = R.string.error_timed_out_session_message)
                }
            )
        }
        else -> {
            if (error.message.contains("failed during countdown", ignoreCase = true)) {
                DisplayError(
                    title = stringResource(id = R.string.error_failure_during_countdown_title),
                    message = stringResource(id = R.string.error_failure_during_countdown_message)
                )
            } else {
                DisplayError(
                    title = stringResource(id = R.string.error_server_issue_title),
                    message = stringResource(id = R.string.error_server_issue_message)
                )
            }
        }
    }
}

private fun formattedConfidenceScore(confidenceScore: Float): String {
    var truncatedConfidenceScore = floor(confidenceScore * 10000) / 10000
    truncatedConfidenceScore = min(truncatedConfidenceScore, 99.9999f)
    truncatedConfidenceScore = max(truncatedConfidenceScore, 0.0001f)

    return NumberFormat.getInstance().apply {
        maximumFractionDigits = 4
        minimumFractionDigits = 4
    }.format(truncatedConfidenceScore)
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResultsViewSuccessPreview() {
    MyApplicationTheme{
        ResultsView(
            sessionId = UUID.randomUUID().toString(),
            tryAgainButtonOnClick = { },
            isLive = true,
            confidenceScore = 100f
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResultsViewFailedConfidencePreview() {
    MyApplicationTheme{
        ResultsView(
            sessionId = UUID.randomUUID().toString(),
            tryAgainButtonOnClick = { },
            isLive = false,
            confidenceScore = 0f
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ResultsViewErrorPreview() {
    MyApplicationTheme{
        ResultsView(
            sessionId = UUID.randomUUID().toString(),
            isLive = false,
            error = FaceLivenessDetectionException.SessionTimedOutException(),
            tryAgainButtonOnClick = { }
        )
    }
}