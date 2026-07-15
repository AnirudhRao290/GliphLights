package com.example.gliphlights.models

data class GlyphState(
    val isActive: Boolean = false,
    val activeChannels: Set<Int> = emptySet(),
    val activeZones: Set<GlyphZone> = emptySet()
) {
    companion object {
        val INACTIVE = GlyphState(isActive = false)
    }
}

data class GlyphChannel(
    val index: Int,
    val zone: GlyphZone,
    val isActive: Boolean = false
) {
    val displayName: String
        get() = zone.getChannelName(index)
}

data class DeviceInfo(
    val model: String,
    val isSupported: Boolean,
    val availableZones: List<GlyphZone> = emptyList()
) {
    companion object {
        val UNKNOWN = DeviceInfo(
            model = "Unknown Device",
            isSupported = false
        )
    }
}

data class AnimationParams(
    val period: Int = 2000,
    val cycles: Int = 2,
    val interval: Int = 200
)

data class AppSettings(
    val animatePeriod: Int = 2000,
    val animateCycles: Int = 2,
    val animateInterval: Int = 200,
    val defaultZone: GlyphZone? = null,
    val startupBehavior: StartupBehavior = StartupBehavior.DO_NOTHING,
    val theme: ThemePreference = ThemePreference.SYSTEM
)

enum class StartupBehavior(val displayName: String) {
    DO_NOTHING("Do nothing"),
    SHOW_LAST_STATE("Show last state")
}

enum class ThemePreference(val displayName: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}
