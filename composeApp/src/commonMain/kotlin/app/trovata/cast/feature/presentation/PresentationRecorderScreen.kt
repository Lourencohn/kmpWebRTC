package app.trovata.cast.feature.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.trovata.cast.platform.LiveCatalogWebView
import app.trovata.cast.platform.ShareController
import app.trovata.cast.ui.theme.TrovataTokens
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

data class PresentationRecorderRoute(
    val catalogoUuid: String,
    val catalogoNome: String,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val share = koinInject<ShareController>()
        val clipboard = LocalClipboardManager.current
        val screenModel = koinScreenModel<PresentationRecorderScreenModel> {
            parametersOf(catalogoUuid, catalogoNome)
        }
        val state by screenModel.state.collectAsState()

        RecorderBody(
            state = state,
            screenModel = screenModel,
            onClose = { navigator.pop() },
            onShare = { state.shareMessage?.let { share.share(it, "Apresentação $catalogoNome") } },
            onCopy = { state.shareUrl?.let { clipboard.setText(AnnotatedString(it)) } },
        )
    }
}

@Composable
private fun RecorderBody(
    state: PresentationRecorderUiState,
    screenModel: PresentationRecorderScreenModel,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    val colors = TrovataTokens.colors
    var showGuide by rememberSaveable { mutableStateOf(true) }
    Box(modifier = Modifier.fillMaxSize().background(colors.ink)) {
        Column(modifier = Modifier.fillMaxSize()) {
            RecorderTopBar(state = state, onClose = onClose)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White),
            ) {
                LiveCatalogWebView(
                    url = state.pageUrl,
                    bridge = screenModel.webBridge,
                    modifier = Modifier.fillMaxSize(),
                )
                RecordingFrame(phase = state.phase)
                RecordedActionToast(
                    action = state.lastAction,
                    capturing = state.phase == RecorderPhase.Recording,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                )
                when (state.phase) {
                    RecorderPhase.Preparing -> if (showGuide) {
                        HowItWorksCard(
                            onDismiss = { showGuide = false },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                        )
                    } else {
                        HowItWorksChip(
                            onClick = { showGuide = true },
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        )
                    }
                    RecorderPhase.Countdown -> CountdownOverlay(
                        seconds = state.countdown,
                        onCancel = screenModel::cancelCountdown,
                    )
                    RecorderPhase.Paused -> PausedBanner(
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    )
                    else -> Unit
                }
            }
            RecorderActionBar(
                state = state,
                onStart = screenModel::requestStart,
                onCancelCountdown = screenModel::cancelCountdown,
                onPause = screenModel::pause,
                onResume = screenModel::resume,
                onFinish = screenModel::finish,
                onToggleDrawing = screenModel::toggleDrawing,
                onClearDrawing = screenModel::clearDrawing,
            )
        }

        state.error?.takeIf { state.phase != RecorderPhase.Review }?.let { message ->
            RecorderErrorBanner(
                message = message,
                onDismiss = screenModel::dismissError,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 132.dp, start = 16.dp, end = 16.dp),
            )
        }

        when (state.phase) {
            RecorderPhase.Finishing, RecorderPhase.Review, RecorderPhase.Uploading -> ReviewSheet(
                state = state,
                onPublish = screenModel::publish,
                onDiscard = { screenModel.startOver() },
            )
            RecorderPhase.Shared -> SharedSheet(
                state = state,
                onShare = onShare,
                onCopy = onCopy,
                onRecordAnother = { screenModel.startOver() },
                onClose = onClose,
            )
            else -> Unit
        }
    }
}
