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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smapra.exambro.security.SEBCompatibility
import com.smapra.exambro.security.SecurityViolation
import com.smapra.exambro.util.PlatformCompat
import kotlinx.coroutines.delay

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

    LaunchedEffect(url) {
        viewModel.initExam(url)
    }

    LaunchedEffect(Unit) {
        viewModel.startSecurityMonitoring(activity)
    }

    // Anti-sleep: keep screen on during exam
    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
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
            // Jika sudah di halaman utama (login/dashboard/course/view quiz),
            // jangan goBack lagi — tampilkan exit dialog
            val isRootPage = currentUrl.contains("/my/") ||
                currentUrl.contains("/course/view.php") ||
                currentUrl.contains("/mod/quiz/view.php") ||
                currentUrl.contains("/login/") ||
                currentUrl.endsWith("/") && !currentUrl.contains("/mod/")
            if (isRootPage) {
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

        // Secret exit tap zone — invisible 48dp box di pojok kanan atas
        // Tap 5x cepat (dalam 3 detik) untuk buka PIN dialog
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
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
        )

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
        }
    }
}

private fun createExamWebChromeClient(viewModel: ExamViewModel): WebChromeClient {
    return object : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            viewModel.onProgressChanged(newProgress)
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
