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
    val url: String = "",
    val urlError: String? = null,
    val isLoading: Boolean = false,
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

    val savedUrls: StateFlow<Set<String>> = examPreferences.savedUrls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch {
            examPreferences.lastUrl.collect { lastUrl ->
                if (lastUrl.isNotBlank() && _uiState.value.url.isBlank()) {
                    _uiState.update { it.copy(url = lastUrl) }
                }
            }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url, urlError = null) }
    }

    fun onStartExam() {
        val url = _uiState.value.url.trim()

        if (url.isBlank()) {
            _uiState.update { it.copy(urlError = "URL tidak boleh kosong") }
            return
        }

        val normalizedUrl = normalizeUrl(url)

        if (!isValidUrl(normalizedUrl)) {
            _uiState.update { it.copy(urlError = "Masukkan URL yang valid") }
            return
        }

        _uiState.update { it.copy(isLoading = true, urlError = null, url = normalizedUrl) }

        viewModelScope.launch {
            examPreferences.saveUrl(normalizedUrl)
            val domain = sebCompatibility.extractDomainFromUrl(normalizedUrl)
            if (domain != null) {
                examPreferences.saveAllowedDomains(setOf(domain))
            }
            _events.emit(SetupEvent.NavigateToExam(normalizedUrl))
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSebUrl(sebUrl: String) {
        val httpUrl = sebCompatibility.parseSebUrl(sebUrl)
        if (httpUrl != null) {
            _uiState.update { it.copy(url = httpUrl) }
            onStartExam()
        }
    }

    fun onSelectSavedUrl(url: String) {
        _uiState.update { it.copy(url = url, urlError = null) }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            examPreferences.clearSavedUrls()
        }
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
