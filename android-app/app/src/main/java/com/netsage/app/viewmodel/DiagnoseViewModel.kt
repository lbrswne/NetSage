package com.netsage.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsage.app.model.CauseItem
import com.netsage.app.repo.DiagnoseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DiagnoseUiState(
    val loading: Boolean = false,
    val causes: List<CauseItem> = emptyList(),
    val error: String? = null
)

class DiagnoseViewModel(private val repo: DiagnoseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DiagnoseUiState())
    val uiState: StateFlow<DiagnoseUiState> = _uiState

    fun diagnose(logText: String) {
        if (logText.isBlank()) return
        _uiState.value = DiagnoseUiState(loading = true)

        viewModelScope.launch {
            runCatching { repo.diagnose(logText) }
                .onSuccess { resp ->
                    _uiState.value = DiagnoseUiState(causes = resp.top_causes)
                }
                .onFailure { e ->
                    val msg = when {
                        (e.message ?: "").contains("timeout", ignoreCase = true) -> "请求超时，请稍后重试"
                        (e.message ?: "").contains("failed to connect", ignoreCase = true) -> "网络不可达，请检查后端是否启动"
                        else -> "服务异常，请稍后重试"
                    }
                    _uiState.value = DiagnoseUiState(error = msg)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
