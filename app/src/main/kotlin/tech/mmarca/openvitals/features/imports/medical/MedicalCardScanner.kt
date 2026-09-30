package tech.mmarca.openvitals.features.imports.medical

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.medical.shc.CardScan
import tech.mmarca.openvitals.domain.medical.shc.QrCodes
import tech.mmarca.openvitals.domain.medical.shc.Raster
import tech.mmarca.openvitals.domain.medical.shc.luminanceOf
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The camera, looking for a SMART Health Card's QR code. Each frame is read on the phone and
 * dropped: nothing is saved or sent. It ends with [onScanned] as soon as a whole card is seen.
 * The caller asks for the camera permission before showing this.
 */
@Composable
internal fun MedicalCardScanner(onScanned: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanned by rememberUpdatedState(onScanned)
    var progress by remember { mutableStateOf<CardScan.Progress?>(null) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val scan = CardScan()
        val done = AtomicBoolean(false)
        val frames = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener(
            {
                val cameras = providerFuture.get().also { provider = it }
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(ResolutionStrategy(FrameSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                            .build(),
                    )
                    .build()
                analysis.setAnalyzer(frames) { frame ->
                    frame.use {
                        if (done.get()) return@use
                        val plane = frame.planes[0]
                        val raster = Raster(frame.width, frame.height, luminanceOf(plane.buffer, plane.rowStride, frame.width, frame.height))
                        for (text in QrCodes.read(raster)) {
                            val result = scan.add(text)
                            mainExecutor.execute {
                                progress = result
                                if (result is CardScan.Progress.Complete && done.compareAndSet(false, true)) scanned(result.text)
                            }
                        }
                    }
                }
                // A phone with no back camera, or one in use elsewhere, shows an empty preview; Cancel still works.
                runCatching {
                    cameras.unbindAll()
                    cameras.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            },
            mainExecutor,
        )
        onDispose {
            done.set(true)
            provider?.unbindAll()
            frames.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(text = scanText(progress), style = MaterialTheme.typography.bodyMedium)
                OpenVitalsOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun scanText(progress: CardScan.Progress?): String = when (progress) {
    is CardScan.Progress.Partial -> stringResource(R.string.medical_import_scan_partial, progress.have, progress.total)
    CardScan.Progress.NotACard -> stringResource(R.string.medical_import_scan_not_card)
    else -> stringResource(R.string.medical_import_scan_hint)
}

/** Enough pixels for the densest card held at arm's length. */
private val FrameSize = Size(1920, 1080)
