package tech.mmarca.openvitals.testing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.Locale
import kotlin.math.abs
import org.junit.Assert.fail
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

const val OpenVitalsVisualRootTag = "openvitals_visual_root"

private const val GoldenAssetDir = "goldens"
private const val GoldenOutputDir = "/sdcard/Download/openvitals-goldens"
private const val RecordGoldensArgument = "openvitals.recordGoldens"

@Composable
fun OpenVitalsVisualTestSurface(
    width: Dp = 393.dp,
    height: Dp = 852.dp,
    content: @Composable () -> Unit,
) {
    OpenVitalsTheme {
        CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1f)) {
            Box(
                modifier = Modifier
                    .requiredSize(width, height)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .testTag(OpenVitalsVisualRootTag),
            ) {
                content()
            }
        }
    }
}

fun ComposeTestRule.assertVisualRootMatchesGolden(
    goldenName: String,
    perChannelTolerance: Int = 2,
    allowedDifferentPixelRatio: Double = 0.005,
) {
    waitForIdle()
    val actual = onNodeWithTag(OpenVitalsVisualRootTag, useUnmergedTree = true)
        .captureToImage()
        .asAndroidBitmap()
        .copy(Bitmap.Config.ARGB_8888, false)

    assertBitmapMatchesGolden(
        goldenName = goldenName,
        actual = actual,
        perChannelTolerance = perChannelTolerance,
        allowedDifferentPixelRatio = allowedDifferentPixelRatio,
    )
}

private fun assertBitmapMatchesGolden(
    goldenName: String,
    actual: Bitmap,
    perChannelTolerance: Int,
    allowedDifferentPixelRatio: Double,
) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val recordGoldens = InstrumentationRegistry.getArguments()
        .getString(RecordGoldensArgument)
        ?.toBooleanStrictOrNull()
        ?: false

    val outputPath = goldenOutputPath(goldenName)
    if (recordGoldens) {
        writeBitmapToDeviceDownloads(actual, outputPath)
        return
    }

    val expected = try {
        instrumentation.context.assets
            .open("$GoldenAssetDir/$goldenName.png")
            .use(BitmapFactory::decodeStream)
    } catch (_: FileNotFoundException) {
        writeBitmapToDeviceDownloads(actual, outputPath)
        fail(
            "Missing golden '$goldenName'. Wrote current rendering to " +
                "$outputPath. Re-run with " +
                "-Pandroid.testInstrumentationRunnerArguments.$RecordGoldensArgument=true " +
                "to record baselines.",
        )
        return
    }
    if (expected == null) {
        fail("Golden '$goldenName' could not be decoded")
        return
    }

    if (expected.width != actual.width || expected.height != actual.height) {
        writeBitmapToDeviceDownloads(actual, outputPath)
        fail(
            "Golden '$goldenName' size changed. Expected ${expected.width}x${expected.height}, " +
                "actual ${actual.width}x${actual.height}. Wrote actual to $outputPath.",
        )
    }

    val comparison = compareBitmaps(
        expected = expected,
        actual = actual,
        perChannelTolerance = perChannelTolerance,
    )
    if (comparison.differentPixelRatio > allowedDifferentPixelRatio) {
        writeBitmapToDeviceDownloads(actual, outputPath)
        fail(
            "Golden '$goldenName' differed by " +
                String.format(Locale.US, "%.3f", comparison.differentPixelRatio * 100.0) +
                "% (${comparison.differentPixels}/${comparison.totalPixels} pixels). " +
                "Max channel delta ${comparison.maxChannelDelta}. Wrote actual to $outputPath.",
        )
    }
}

private data class BitmapComparison(
    val differentPixels: Int,
    val totalPixels: Int,
    val maxChannelDelta: Int,
) {
    val differentPixelRatio: Double = differentPixels.toDouble() / totalPixels.toDouble()
}

private fun compareBitmaps(
    expected: Bitmap,
    actual: Bitmap,
    perChannelTolerance: Int,
): BitmapComparison {
    val width = expected.width
    val height = expected.height
    val expectedPixels = IntArray(width * height)
    val actualPixels = IntArray(width * height)
    expected.getPixels(expectedPixels, 0, width, 0, 0, width, height)
    actual.getPixels(actualPixels, 0, width, 0, 0, width, height)

    var differentPixels = 0
    var maxChannelDelta = 0
    expectedPixels.indices.forEach { index ->
        val expectedPixel = expectedPixels[index]
        val actualPixel = actualPixels[index]
        val delta = maxOf(
            abs(Color.alpha(expectedPixel) - Color.alpha(actualPixel)),
            abs(Color.red(expectedPixel) - Color.red(actualPixel)),
            abs(Color.green(expectedPixel) - Color.green(actualPixel)),
            abs(Color.blue(expectedPixel) - Color.blue(actualPixel)),
        )
        maxChannelDelta = maxOf(maxChannelDelta, delta)
        if (delta > perChannelTolerance) {
            differentPixels += 1
        }
    }

    return BitmapComparison(
        differentPixels = differentPixels,
        totalPixels = expectedPixels.size,
        maxChannelDelta = maxChannelDelta,
    )
}

private fun goldenOutputPath(goldenName: String): String = "$GoldenOutputDir/$goldenName.png"

private fun writeBitmapToDeviceDownloads(bitmap: Bitmap, outputPath: String) {
    val pngBytes = ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        output.toByteArray()
    }

    val mkdirResult = runShellCommand("mkdir -p $GoldenOutputDir")
    if (mkdirResult.stderr.isNotBlank()) {
        fail("Could not create golden output directory $GoldenOutputDir: ${mkdirResult.stderr}")
    }

    val writeResult = writeShellCommand(
        command = "tee $outputPath",
        stdin = pngBytes,
        collectStdout = false,
    )
    if (writeResult.stderr.isNotBlank()) {
        fail("Could not write golden output $outputPath: ${writeResult.stderr}")
    }

    val verifyResult = runShellCommand("ls -la $outputPath")
    if (verifyResult.stderr.isNotBlank() || outputPath !in verifyResult.stdout) {
        fail(
            "Golden output $outputPath was not created. " +
                "stdout=${verifyResult.stdout} stderr=${verifyResult.stderr}",
        )
    }
}

private data class ShellResult(
    val stdout: String,
    val stderr: String,
)

private fun runShellCommand(command: String): ShellResult = writeShellCommand(
    command = command,
    stdin = ByteArray(0),
    collectStdout = true,
)

private fun writeShellCommand(
    command: String,
    stdin: ByteArray,
    collectStdout: Boolean,
): ShellResult {
    val descriptor = InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommandRwe(command)

    ParcelFileDescriptor.AutoCloseOutputStream(descriptor[1]).use { shellInput ->
        shellInput.write(stdin)
        shellInput.flush()
    }
    val stdout = ParcelFileDescriptor.AutoCloseInputStream(descriptor[0]).use { shellOutput ->
        val output = shellOutput.readBytes()
        if (collectStdout) output.decodeToString() else ""
    }
    val stderr = ParcelFileDescriptor.AutoCloseInputStream(descriptor[2]).use { shellError ->
        shellError.readBytes().decodeToString()
    }

    return ShellResult(stdout = stdout.trim(), stderr = stderr.trim())
}
