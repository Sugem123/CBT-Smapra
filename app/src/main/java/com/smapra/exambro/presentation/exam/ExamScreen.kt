package com.smapra.exambro.presentation.exam

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.AudioManager
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smapra.exambro.security.SEBCompatibility
import com.smapra.exambro.security.SecurityViolation
import com.smapra.exambro.util.PlatformCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// PIN rahasia untuk teknisi/proktor keluar dari mode ujian
private const val PROCTOR_PIN = "131313"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExamScreen(
    url: String,
    onExitExam: () -> Unit,
    viewModel: ExamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allowedDomains by viewModel.allowedDomains.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Secret exit: tap 5x di pojok kanan atas → PIN dialog
    var secretTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Status bar: waktu, tanggal & baterai
    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
    }
    var currentDate by remember {
        mutableStateOf(SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")).format(Date()))
    }
    var batteryPercentage by remember { mutableStateOf(100) }
    var isBatteryCharging by remember { mutableStateOf(false) }

    // Dropdown Settings: Kecerahan & Volume
    var showSettingsDrawer by remember { mutableStateOf(false) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 7f)
    }
    var currentBrightness by remember {
        val attrBrightness = activity.window.attributes.screenBrightness
        mutableStateOf(if (attrBrightness in 0.05f..1.0f) attrBrightness else 0.7f)
    }
    var currentTextZoom by remember { mutableStateOf(100) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale("id", "ID")).format(now)
            delay(10_000L) // perbarui setiap 10 detik
        }
    }

    // Keep-alive Moodle session: jalankan touch script setiap 4 menit
    LaunchedEffect(Unit) {
        while (true) {
            delay(4 * 60 * 1000L) // 4 menit
            webView?.evaluateJavascript(
                "(function() { if (window.M && window.M.cfg) { fetch(window.M.cfg.wwwroot + '/lib/ajax/service.php', { method: 'HEAD' }).catch(function(){}); } })();",
                null,
            )
        }
    }

    DisposableEffect(context) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryPercentage = (level * 100 / scale.toFloat()).toInt()
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(batteryReceiver, filter)
        initialIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                batteryPercentage = (level * 100 / scale.toFloat()).toInt()
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isBatteryCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(url) {
        viewModel.initExam(url)
    }

    LaunchedEffect(Unit) {
        viewModel.startSecurityMonitoring(activity)
    }

    // Anti-sleep: keep screen on during exam
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    DisposableEffect(activity) {
        val mainActivity = activity as? com.smapra.exambro.presentation.MainActivity
        var focusLostJob: kotlinx.coroutines.Job? = null

        mainActivity?.setExamActive(true)
        mainActivity?.onFocusChangedListener = { hasFocus ->
            if (!hasFocus) {
                // Beri toleransi 800ms agar keyboard sistem/dialog permission tidak memicu false alarm
                focusLostJob?.cancel()
                focusLostJob = coroutineScope.launch {
                    delay(800L)
                    viewModel.onAppPaused()
                }
            } else {
                // Fokus kembali sebelum 800ms -> batalkan alarm
                focusLostJob?.cancel()
            }
        }

        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            focusLostJob?.cancel()
            mainActivity?.onFocusChangedListener = null
            mainActivity?.setExamActive(false)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExamEvent.ExitExam -> {
                    viewModel.stopSecurityMonitoring(activity)
                    webView?.destroy()
                    onExitExam()
                }
                is ExamEvent.ShowViolationAlert -> { /* handled by UI state */ }
            }
        }
    }

    // Security polling every 2 seconds
    LaunchedEffect(uiState.examState) {
        if (uiState.examState == ExamState.IN_PROGRESS || uiState.examState == ExamState.VIOLATION_DETECTED) {
            while (true) {
                delay(2_000L)
                // Safe multi-window/PiP check — returns false on API < 24/26
                val isMultiWindow = if (PlatformCompat.isApi24) activity.isInMultiWindowMode else false
                val isPip = if (PlatformCompat.isApi26) activity.isInPictureInPictureMode else false
                viewModel.performSecurityCheck(
                    activity = activity,
                    isInMultiWindowMode = isMultiWindow,
                    isInPipMode = isPip,
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSecurityMonitoring(activity)
            webView?.destroy()
        }
    }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            val currentUrl = wv.url ?: ""
            // Jika di halaman utama atau sedang di lembar kuis (attempt.php),
            // tampilkan exit dialog konfirmasi agar siswa tidak kehilangan progres ujian secara tidak sengaja
            val isRootOrExamPage = currentUrl.contains("/my/") ||
                currentUrl.contains("/course/view.php") ||
                currentUrl.contains("/mod/quiz/view.php") ||
                currentUrl.contains("/mod/quiz/attempt.php") ||
                currentUrl.contains("/mod/quiz/summary.php") ||
                currentUrl.contains("/login/") ||
                currentUrl.endsWith("/") && !currentUrl.contains("/mod/")
            if (isRootOrExamPage) {
                viewModel.requestExit()
            } else {
                wv.goBack()
            }
        } else {
            viewModel.requestExit()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView.setWebContentsDebuggingEnabled(true)
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    filterTouchesWhenObscured = true

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportMultipleWindows(false)
                        javaScriptCanOpenWindowsAutomatically = false
                        allowFileAccess = false
                        allowContentAccess = false
                        mediaPlaybackRequiresUserGesture = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = SEBCompatibility.SEB_USER_AGENT
                        cacheMode = WebSettings.LOAD_DEFAULT
                        setSupportZoom(true)
                        textZoom = 100 // Kunci skala teks agar tidak dipengaruhi font size sistem HP
                        // Rendering optimization for older WebView
                        setRenderPriority(WebSettings.RenderPriority.HIGH)
                        setEnableSmoothTransition(true)
                    }

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    if (PlatformCompat.isApi21) {
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                    }

                    webViewClient = createExamWebViewClient(
                        viewModel = viewModel,
                        allowedDomains = allowedDomains,
                        primaryUrl = url,
                    )

                    webChromeClient = createExamWebChromeClient(viewModel)

                    setOnLongClickListener { true }
                    isHapticFeedbackEnabled = false
                    isLongClickable = false
                    keepScreenOn = true

                    loadUrl(url)
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { wv ->
                wv.webViewClient = createExamWebViewClient(
                    viewModel = viewModel,
                    allowedDomains = allowedDomains,
                    primaryUrl = url,
                )
            },
        )

        // Loading indicator
        AnimatedVisibility(
            visible = uiState.examState == ExamState.LOADING,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(
                progress = { uiState.loadingProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            )
        }

        // Security violation overlay
        AnimatedVisibility(
            visible = uiState.examState == ExamState.VIOLATION_DETECTED,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SecurityViolationOverlay(
                violations = uiState.currentViolations,
                onDismiss = viewModel::dismissViolation,
            )
        }

        // Exit confirmation dialog
        if (uiState.showExitDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissExitDialog,
                title = { Text("Keluar Ujian?") },
                text = { Text("Apakah Anda yakin ingin keluar? Progres ujian mungkin tidak tersimpan.") },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmExit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("Keluar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissExitDialog) {
                        Text("Batal")
                    }
                },
            )
        }

        // ── TOP PANEL: PENGATURAN KECERAHAN & VOLUME (DROPDOWN) ──
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Tombol Toggle Kapsul Kecil
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showSettingsDrawer = !showSettingsDrawer },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                tonalElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Pengaturan",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = Color.White,
                    )
                    Icon(
                        imageVector = if (showSettingsDrawer) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Konten Panel Dropdown (Kecerahan & Volume)
            AnimatedVisibility(
                visible = showSettingsDrawer,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF00F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Slider Kecerahan
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "☀️ Kecerahan Layar",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${(currentBrightness * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = currentBrightness,
                            onValueChange = { newValue ->
                                currentBrightness = newValue
                                val lp = activity.window.attributes
                                lp.screenBrightness = newValue
                                activity.window.attributes = lp
                            },
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFBBF24),
                                activeTrackColor = Color(0xFFFBBF24),
                                inactiveTrackColor = Color(0x33FFFFFF),
                            ),
                        )

                        // Slider Volume
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🔊 Volume Media",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${((currentVolume / maxVolume.coerceAtLeast(1)) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF60A5FA),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = currentVolume,
                            onValueChange = { newVol ->
                                currentVolume = newVol
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                            },
                            valueRange = 0f..maxVolume.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF60A5FA),
                                activeTrackColor = Color(0xFF60A5FA),
                                inactiveTrackColor = Color(0x33FFFFFF),
                            ),
                        )

                        // Kontrol Ukuran Teks Soal (Text Zoom)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "🔤 Ukuran Huruf Soal",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (currentTextZoom > 80) {
                                                currentTextZoom -= 10
                                                webView?.settings?.textZoom = currentTextZoom
                                            }
                                        },
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = " A- ",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    )
                                }
                                Text(
                                    text = "$currentTextZoom%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF34D399),
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (currentTextZoom < 140) {
                                                currentTextZoom += 10
                                                webView?.settings?.textZoom = currentTextZoom
                                            }
                                        },
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = " A+ ",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── BOTTOM FLOATING BAR: STATUS (KIRI) & REFRESH (KANAN) ──
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            // KIRI: Kotak Status 2 Tingkat (Atas: Jam & Baterai, Bawah: Tanggal)
            // Sekaligus Secret Tap Target 5x untuk PIN Proktor
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 3_000L) {
                            secretTapCount = 0
                        }
                        lastTapTime = now
                        secretTapCount++
                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            pinInput = ""
                            pinError = false
                            showPinDialog = true
                        }
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xD90F172A), // Dark translucent
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // Tingkat 1: Waktu & Baterai
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = currentTime,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        val batteryColor = when {
                            isBatteryCharging -> Color(0xFF4CAF50)
                            batteryPercentage <= 20 -> Color(0xFFFF5252)
                            batteryPercentage <= 40 -> Color(0xFFFFB74D)
                            else -> Color.White
                        }
                        Text(
                            text = if (isBatteryCharging) "⚡$batteryPercentage%" else "$batteryPercentage%",
                            color = batteryColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }

                    // Tingkat 2: Tanggal
                    Text(
                        text = currentDate,
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    )
                }
            }

            // KANAN: Tombol Refresh Cantik
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { webView?.reload() },
                shape = RoundedCornerShape(14.dp),
                color = Color(0xD90F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFFFFF)),
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Muat Ulang Halaman",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }

        // PIN dialog for secret exit
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPinDialog = false
                    pinInput = ""
                    pinError = false
                },
                title = { Text("Akses Teknisi / Proktor") },
                text = {
                    Column {
                        Text("Masukkan PIN untuk keluar dari mode ujian.")
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                pinError = false
                            },
                            label = { Text("PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = pinError,
                            supportingText = if (pinError) {
                                { Text("PIN salah", color = MaterialTheme.colorScheme.error) }
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinInput == PROCTOR_PIN) {
                                showPinDialog = false
                                pinInput = ""
                                viewModel.confirmExit()
                            } else {
                                pinError = true
                            }
                        },
                    ) {
                        Text("Buka Kunci")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showPinDialog = false
                        pinInput = ""
                        pinError = false
                    }) {
                        Text("Batal")
                    }
                },
            )
        }
    }
}

@Composable
private fun SecurityViolationOverlay(
    violations: List<SecurityViolation>,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Pelanggaran Keamanan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                violations.forEach { violation ->
                    Text(
                        text = "• ${violation.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Harap tutup aplikasi yang melanggar untuk melanjutkan ujian.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

private const val TAG = "ExamWebView"

private fun createExamWebViewClient(
    viewModel: ExamViewModel,
    allowedDomains: Set<String>,
    primaryUrl: String,
): WebViewClient {
    return object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d(TAG, "onPageStarted: $url")
            viewModel.onPageStarted()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "onPageFinished: $url")
            viewModel.onPageFinished()
            view?.let {
                viewModel.onCanGoBackChanged(it.canGoBack())
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val requestUrl = request?.url ?: return true
            val host = requestUrl.host ?: return true

            if (allowedDomains.isNotEmpty()) {
                val isAllowed = allowedDomains.any { domain ->
                    host == domain || host.endsWith(".$domain")
                }
                if (!isAllowed) {
                    Log.w(TAG, "Blocked navigation to $host (not in allowed domains)")
                    return true
                }
            } else {
                val primaryHost = android.net.Uri.parse(primaryUrl).host
                if (primaryHost != null) {
                    if (host != primaryHost && !host.endsWith(".$primaryHost")) {
                        Log.w(TAG, "Blocked navigation to $host (primary: $primaryHost)")
                        return true
                    }
                }
            }

            Log.d(TAG, "Allowing navigation to: $requestUrl")
            return false
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            Log.w(TAG, "SSL error: ${error?.primaryError} url=${error?.url}")
            // Allow self-signed certs common on school CBT servers
            handler?.proceed()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            Log.e(TAG, "WebView error: code=${error?.errorCode} desc=${error?.description} url=${request?.url}")
            super.onReceivedError(view, request, error)

            // Jika main frame gagal load karena jaringan putus, suntikkan tampilan ramah
            if (request?.isForMainFrame == true) {
                val errorHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body { font-family: -apple-system, sans-serif; text-align: center; padding: 40px 16px; background: #0f172a; color: white; margin: 0; }
                            .card { max-width: 400px; margin: 40px auto; background: #1e293b; padding: 32px 20px; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); }
                            h2 { color: #f87171; font-size: 20px; margin-bottom: 12px; }
                            p { color: #94a3b8; font-size: 14px; line-height: 1.6; }
                            .btn { margin-top: 20px; display: inline-block; background: #3b82f6; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <div class="card">
                            <div style="font-size: 48px">&#9888;</div>
                            <h2>Koneksi Terputus</h2>
                            <p>Tidak dapat terhubung ke server CBT.<br>Pastikan Anda terhubung ke Wi-Fi sekolah, lalu tekan tombol <b>Refresh</b> di bawah layar.</p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
            }
        }
    }
}

private fun createExamWebChromeClient(viewModel: ExamViewModel): WebChromeClient {
    return object : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            viewModel.onProgressChanged(newProgress)
        }

        // Blokir pemanggilan file chooser / upload dokumen untuk mencegah celah kecurangan
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
            fileChooserParams: FileChooserParams?,
        ): Boolean {
            filePathCallback?.onReceiveValue(null) // Batalkan langsung
            Log.w(TAG, "File chooser blocked by security policy")
            return true // handled, jangan biarkan sistem membuka file manager
        }

        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
            Log.d(TAG, "JS Console [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} at ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}")
            return true
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            return false
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            return false
        }
    }
}
