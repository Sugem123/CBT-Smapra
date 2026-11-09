package com.smapra.exambro.presentation.setup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smapra.exambro.data.ExamPreferences
import com.smapra.exambro.security.SEBCompatibility
import com.smapra.exambro.updater.AppUpdater
import com.smapra.exambro.updater.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updateInfo: UpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f,
    val isDownloadingUpdate: Boolean = false,
)

sealed class SetupEvent {
    data class NavigateToExam(val url: String) : SetupEvent()
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val examPreferences: ExamPreferences,
    private val sebCompatibility: SEBCompatibility,
    private val appUpdater: AppUpdater,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SetupEvent>()
    val events = _events.asSharedFlow()

    init {
        // Bersihkan riwayat URL lama agar siswa tidak dapat melihat/mengakses alamat server
        viewModelScope.launch {
            examPreferences.clearSavedUrls()
        }
    }

    /**
     * Dipanggil saat QR Code berhasil dipindai oleh kamera.
     * Langsung memvalidasi dan login/navigasi ke ruang ujian tanpa menampilkan URL ke siswa.
     */
    fun onScanQr(scannedText: String) {
        val raw = scannedText.trim()
        if (raw.isBlank()) {
            _uiState.update { it.copy(errorMessage = "QR Code kosong atau tidak terbaca") }
            return
        }

        // Handle skema SEB jika ada
        val resolvedUrl = if (raw.startsWith("seb://", ignoreCase = true) || raw.startsWith("sebs://", ignoreCase = true)) {
            sebCompatibility.parseSebUrl(raw) ?: raw
        } else {
            raw
        }

        val normalizedUrl = normalizeUrl(resolvedUrl)

        if (!isValidUrl(normalizedUrl)) {
            _uiState.update {
                it.copy(
                    errorMessage = "QR Code tidak valid. Pastikan memindai QR Code resmi dari pengawas ujian.",
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val domain = sebCompatibility.extractDomainFromUrl(normalizedUrl)
            if (domain != null) {
                examPreferences.saveAllowedDomains(setOf(domain))
            }
            // Langsung navigasi ke ruang ujian
            _events.emit(SetupEvent.NavigateToExam(normalizedUrl))
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun checkForUpdate(
        currentVersionCode: Int,
        currentVersionName: String = "",
        showDialogOnFound: Boolean = true,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val info = appUpdater.checkForUpdate(currentVersionCode, currentVersionName)
            _uiState.update {
                it.copy(
                    updateInfo = info,
                    showUpdateDialog = if (info != null) showDialogOnFound else false,
                    isCheckingUpdate = false,
                )
            }
        }
    }

    fun openUpdateDialog() {
        if (_uiState.value.updateInfo != null) {
            _uiState.update { it.copy(showUpdateDialog = true) }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun downloadAndInstall(context: android.content.Context) {
        val downloadUrl = _uiState.value.updateInfo?.apkDownloadUrl ?: return
        _uiState.update { it.copy(isDownloadingUpdate = true, updateDownloadProgress = 0f) }

        viewModelScope.launch {
            val apkFile = appUpdater.downloadApk(context, downloadUrl) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }
            _uiState.update { it.copy(isDownloadingUpdate = false) }
            if (apkFile != null) {
                appUpdater.installApk(context, apkFile)
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://$url"
        }
        return url
    }

    private fun isValidUrl(url: String): Boolean {
        return (url.startsWith("http://") || url.startsWith("https://")) &&
            Patterns.WEB_URL.matcher(url).matches()
    }
}
