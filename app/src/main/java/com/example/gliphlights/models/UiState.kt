package com.example.gliphlights.models

sealed class GlyphUiState {
    data object Loading : GlyphUiState()
    data class Success(
        val glyphState: GlyphState,
        val deviceInfo: DeviceInfo
    ) : GlyphUiState()
    data class Error(val message: String) : GlyphUiState()
}

sealed class ControlsUiState {
    data object Loading : ControlsUiState()
    data class Success(
        val glyphState: GlyphState,
        val zones: List<GlyphZone>,
        val selectedChannels: Set<Int> = emptySet()
    ) : ControlsUiState()
    data class Error(val message: String) : ControlsUiState()
}

sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Success(val settings: AppSettings) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

sealed class ErrorState {
    data object None : ErrorState()
    data class SdkUnavailable(val message: String) : ErrorState()
    data class UnsupportedDevice(val message: String) : ErrorState()
    data class PermissionDenied(val message: String) : ErrorState()
    data class RuntimeError(val message: String, val throwable: Throwable? = null) : ErrorState()
}
