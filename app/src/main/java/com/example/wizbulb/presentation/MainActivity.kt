package com.example.wizbulb.presentation

import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val AXES            = 6
private const val CONFIDENCE_MIN  = 0.65f

// Stage 1: motion detection
private const val MONITOR_INTERVAL_MS = 50L
private const val MOTION_WINDOW       = 10
private const val MOTION_THRESHOLD    = 0.8f

// Stage 2: capture + inference
private const val GESTURE_WAIT_MS = 1000L
private const val COOLDOWN_MS     = 1500L

// Success accent colours
private val COLOR_DOUBLE_CLAP = Color(0xFFE3F2FD)
private val COLOR_BRIGHTEN    = Color(0xFFFFB300)
private val COLOR_DIM         = Color(0xFF7986CB)
private val COLOR_CYCLE       = Color(0xFF00E5FF)

class MainActivity : ComponentActivity() {

    external fun getFeatureCount(): Int
    external fun runInference(data: FloatArray): String?

    companion object {
        init { System.loadLibrary("edgeimpulsewearos") }
    }

    private lateinit var sensorCollector: SensorDataCollector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val featureCount = getFeatureCount()
        val rawSamples   = featureCount / AXES
        sensorCollector  = SensorDataCollector(this, rawSamples)

        // Restore saved bulb settings
        val (ip, port) = BulbSettings.load(this)
        WizController.configure(ip, port)

        setContent {
            BulbControlApp(sensorCollector, featureCount, ::runInference)
        }
    }
}

@Composable
fun BulbControlApp(
    sensorCollector: SensorDataCollector,
    featureCount: Int,
    runInference: (FloatArray) -> String?
) {
    val minSamples = featureCount / AXES / 2

    var uiState      by remember { mutableStateOf<GestureUiState>(GestureUiState.Stopped) }
    var isRunning    by remember { mutableStateOf(false) }
    var inferenceJob by remember { mutableStateOf<Job?>(null) }
    val scope        = rememberCoroutineScope()
    val context      = LocalContext.current
    val activity     = context as? ComponentActivity
    val powerManager = context.getSystemService(PowerManager::class.java)
    val wakeLock     = remember {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WizBulb:GestureWakeLock")
    }

    DisposableEffect(isRunning) {
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("WakelockTimeout")
            if (!wakeLock.isHeld) wakeLock.acquire()
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    fun onToggle() {
        if (!isRunning) {
            isRunning = true
            sensorCollector.start()
            uiState = GestureUiState.Filling

            inferenceJob = scope.launch {
                while (isActive && sensorCollector.sampleCount < minSamples) delay(50)
                uiState = GestureUiState.Listening

                while (isActive) {
                    val variance = sensorCollector.recentAccVariance(MOTION_WINDOW)

                    if (variance < MOTION_THRESHOLD) {
                        delay(MONITOR_INTERVAL_MS)
                        continue
                    }

                    uiState = GestureUiState.Motion

                    delay(GESTURE_WAIT_MS)

                    val input      = sensorCollector.buildInputArray(featureCount)
                    val result     = runInference(input)
                    val label      = result?.substringBefore(":") ?: "idle"
                    val confidence = result?.substringAfter(":")?.toFloatOrNull() ?: 0f

                    if (confidence > CONFIDENCE_MIN && label != "idle") {
                        when (label) {
                            "double_clap" -> { WizController.toggle();    uiState = GestureUiState.Success(label, COLOR_DOUBLE_CLAP) }
                            "brighten"    -> { WizController.bright();    uiState = GestureUiState.Success(label, COLOR_BRIGHTEN) }
                            "dim"         -> { WizController.dim();       uiState = GestureUiState.Success(label, COLOR_DIM) }
                            "color_cycle" -> { WizController.nextColor(); uiState = GestureUiState.Success(label, COLOR_CYCLE) }
                        }
                        delay(COOLDOWN_MS)
                    } else {
                        uiState = GestureUiState.LowConfidence(label, (confidence * 100).toInt())
                        delay(500)
                    }

                    uiState = GestureUiState.Listening
                }
            }
        } else {
            inferenceJob?.cancel()
            inferenceJob = null
            sensorCollector.stop()
            isRunning = false
            uiState   = GestureUiState.Stopped
        }
    }

    val pagerState = rememberPagerState(pageCount = { 3 })

    val pageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset:  Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int  get() = pagerState.currentPage
            override val pageCount:    Int  get() = 3
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> GestureScreen(uiState, isRunning, onToggle = ::onToggle)
                1 -> ManualScreen()
                2 -> SettingsScreen(context)
            }
        }

        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
            modifier           = Modifier.align(Alignment.BottomCenter)
        )
    }
}
