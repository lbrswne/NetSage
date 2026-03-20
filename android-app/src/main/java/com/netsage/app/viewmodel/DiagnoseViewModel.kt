package com.netsage.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netsage.app.model.CauseItem
import com.netsage.app.repo.DiagnoseRepository
import com.netsage.app.util.ErrorMapper
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
                    val msg = ErrorMapper.mapMessage(e.message)
                    _uiState.value = DiagnoseUiState(error = msg)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun consumeCauses() {
        _uiState.value = _uiState.value.copy(causes = emptyList())
    }
}
