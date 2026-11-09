package com.smapra.exambro.presentation.exam

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smapra.exambro.data.ExamPreferences
import com.smapra.exambro.security.FloatingAppDetector
import com.smapra.exambro.security.KioskModeManager
import com.smapra.exambro.security.SEBCompatibility
import com.smapra.exambro.security.ScreenCaptureGuard
import com.smapra.exambro.security.SecurityViolation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

enum class ExamState {
    IDLE,
    LOADING,
    IN_PROGRESS,
    LOCKED,
    VIOLATION_DETECTED,
}

data class ExamUiState(
    val examState: ExamState = ExamState.IDLE,
    val url: String = "",
    val loadingProgress: Int = 0,
    val currentViolations: List<SecurityViolation> = emptyList(),
    val showExitDialog: Boolean = false,
    val canGoBack: Boolean = false,
)

sealed class ExamEvent {
    data object ExitExam : ExamEvent()
    data class ShowViolationAlert(val violation: SecurityViolation) : ExamEvent()
}

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val floatingAppDetector: FloatingAppDetector,
    private val screenCaptureGuard: ScreenCaptureGuard,
    private val kioskModeManager: KioskModeManager,
    private val examPreferences: ExamPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExamEvent>()
    val events = _events.asSharedFlow()

    val allowedDomains: StateFlow<Set<String>> = examPreferences.allowedDomains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun initExam(url: String) {
        _uiState.update {
            it.copy(
                url = url,
                examState = ExamState.LOADING,
            )
        }
    }

    fun onPageStarted() {
        _uiState.update { it.copy(examState = ExamState.LOADING, loadingProgress = 0) }
    }

    fun onPageFinished() {
        _uiState.update { it.copy(examState = ExamState.IN_PROGRESS) }
    }

    fun onProgressChanged(progress: Int) {
        _uiState.update { it.copy(loadingProgress = progress) }
    }

    fun onCanGoBackChanged(canGoBack: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack) }
    }

    fun startSecurityMonitoring(activity: Activity) {
        screenCaptureGuard.blockScreenshots(activity)
        kioskModeManager.startKioskMode(activity)
    }

    fun stopSecurityMonitoring(activity: Activity) {
        screenCaptureGuard.removeProtection(activity)
        kioskModeManager.stopKioskMode(activity)
    }

    fun performSecurityCheck(
        activity: Activity,
        isInMultiWindowMode: Boolean,
        isInPipMode: Boolean,
    ) {
        val violations = mutableListOf<SecurityViolation>()

        violations.addAll(
            floatingAppDetector.detectViolations(isInMultiWindowMode, isInPipMode)
        )

        if (screenCaptureGuard.isScreenBeingCaptured(activity)) {
            violations.add(SecurityViolation.ScreenCaptureDetected)
        }

        if (violations.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    examState = ExamState.VIOLATION_DETECTED,
                    currentViolations = violations,
                )
            }
            viewModelScope.launch {
                val firstViolation = violations.first()
                _events.emit(ExamEvent.ShowViolationAlert(firstViolation))
                // Kirim laporan pelanggaran ke server Moodle secara otomatis
                reportViolationToServer("floating_overlay", firstViolation.message)
            }
        } else if (_uiState.value.examState == ExamState.VIOLATION_DETECTED) {
            _uiState.update {
                it.copy(
                    examState = ExamState.IN_PROGRESS,
                    currentViolations = emptyList(),
                )
            }
        }
    }

    fun onAppPaused() {
        if (_uiState.value.examState == ExamState.IN_PROGRESS) {
            _uiState.update {
                it.copy(
                    examState = ExamState.VIOLATION_DETECTED,
                    currentViolations = it.currentViolations + SecurityViolation.AppSwitchDetected,
                )
            }
            viewModelScope.launch {
                _events.emit(ExamEvent.ShowViolationAlert(SecurityViolation.AppSwitchDetected))
                // Laporkan counter siswa keluar aplikasi / pindah ke aplikasi lain
                reportViolationToServer("app_switch", "Siswa keluar / beralih aplikasi selama ujian")
            }
        }
    }

    private suspend fun reportViolationToServer(type: String, details: String) = withContext(Dispatchers.IO) {
        try {
            val examUrl = _uiState.value.url
            if (examUrl.isBlank()) return@withContext
            val uri = android.net.Uri.parse(examUrl)
            val scheme = uri.scheme ?: "http"
            val host = uri.host ?: return@withContext
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val endpoint = "$scheme://$host$port/local/bulkquiz/api_cbt_violation.php"

            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", SEBCompatibility.SEB_USER_AGENT)

            // Teruskan cookie sesi Moodle saat ini jika ada
            val cookie = android.webkit.CookieManager.getInstance().getCookie(examUrl)
            if (!cookie.isNullOrBlank()) {
                conn.setRequestProperty("Cookie", cookie)
            }

            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val jsonPayload = org.json.JSONObject().apply {
                put("violation_type", type)
                put("details", details)
            }.toString()

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val respCode = conn.responseCode
            android.util.Log.d("ExamViewModel", "Reported violation to server ($respCode)")
        } catch (e: Exception) {
            android.util.Log.w("ExamViewModel", "Failed to report violation: ${e.message}")
        }
    }

    fun dismissViolation() {
        _uiState.update {
            it.copy(
                examState = if (it.currentViolations.size <= 1) ExamState.IN_PROGRESS else ExamState.VIOLATION_DETECTED,
                currentViolations = it.currentViolations.drop(1),
            )
        }
    }

    fun requestExit() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    fun confirmExit() {
        _uiState.update { it.copy(showExitDialog = false, examState = ExamState.IDLE) }
        viewModelScope.launch {
            examPreferences.clearExamData()
            _events.emit(ExamEvent.ExitExam)
        }
    }
}
