package com.example.gliphlights.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gliphlights.models.AppSettings
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SettingsUiState
import com.example.gliphlights.models.StartupBehavior
import com.example.gliphlights.models.ThemePreference
import com.example.gliphlights.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings
                .catch { e ->
                    _uiState.value = SettingsUiState.Error(e.message ?: "Failed to load settings")
                }
                .collect { settings ->
                    _uiState.value = SettingsUiState.Success(settings)
                }
        }
    }

    fun updateAnimatePeriod(period: Int) {
        viewModelScope.launch {
            settingsRepository.updateAnimatePeriod(period)
        }
    }

    fun updateAnimateCycles(cycles: Int) {
        viewModelScope.launch {
            settingsRepository.updateAnimateCycles(cycles)
        }
    }

    fun updateAnimateInterval(interval: Int) {
        viewModelScope.launch {
            settingsRepository.updateAnimateInterval(interval)
        }
    }

    fun updateDefaultZone(zone: GlyphZone?) {
        viewModelScope.launch {
            settingsRepository.updateDefaultZone(zone)
        }
    }

    fun updateStartupBehavior(behavior: StartupBehavior) {
        viewModelScope.launch {
            settingsRepository.updateStartupBehavior(behavior)
        }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.updateTheme(theme)
        }
    }
}
