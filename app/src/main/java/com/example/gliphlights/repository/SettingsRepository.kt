package com.example.gliphlights.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gliphlights.models.AppSettings
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.StartupBehavior
import com.example.gliphlights.models.ThemePreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glyph_settings")

object SettingsKeys {
    val ANIMATE_PERIOD = intPreferencesKey("animate_period")
    val ANIMATE_CYCLES = intPreferencesKey("animate_cycles")
    val ANIMATE_INTERVAL = intPreferencesKey("animate_interval")
    val DEFAULT_ZONE = stringPreferencesKey("default_zone")
    val STARTUP_BEHAVIOR = stringPreferencesKey("startup_behavior")
    val THEME = stringPreferencesKey("theme")
    val LAST_STUDIO_ROUTE = stringPreferencesKey("last_studio_route")
    val LAST_ACTIVE_CHANNELS = stringPreferencesKey("last_active_channels")
    val TOUR_COMPLETED = booleanPreferencesKey("tour_completed")
    val AMBIENT_RITUAL = stringPreferencesKey("ambient_ritual")
    val AMBIENT_BRIGHTNESS = floatPreferencesKey("ambient_brightness")
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    val lastStudioRoute: Flow<String>
    val lastActiveChannels: Flow<Set<Int>>
    val tourCompleted: Flow<Boolean>
    val ambientRitual: Flow<String>
    val ambientBrightness: Flow<Float>
    suspend fun updateAnimatePeriod(period: Int)
    suspend fun updateAnimateCycles(cycles: Int)
    suspend fun updateAnimateInterval(interval: Int)
    suspend fun updateDefaultZone(zone: GlyphZone?)
    suspend fun updateStartupBehavior(behavior: StartupBehavior)
    suspend fun updateTheme(theme: ThemePreference)
    suspend fun updateLastStudioRoute(route: String)
    suspend fun updateLastActiveChannels(channels: Set<Int>)
    suspend fun setTourCompleted(completed: Boolean)
    suspend fun resetTips()
    suspend fun updateAmbientRitual(ritual: String)
    suspend fun updateAmbientBrightness(brightness: Float)
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            animatePeriod = preferences[SettingsKeys.ANIMATE_PERIOD] ?: 2000,
            animateCycles = preferences[SettingsKeys.ANIMATE_CYCLES] ?: 2,
            animateInterval = preferences[SettingsKeys.ANIMATE_INTERVAL] ?: 200,
            defaultZone = preferences[SettingsKeys.DEFAULT_ZONE]?.let { zoneName ->
                try {
                    GlyphZone.valueOf(zoneName)
                } catch (e: Exception) {
                    null
                }
            },
            startupBehavior = preferences[SettingsKeys.STARTUP_BEHAVIOR]?.let { behaviorName ->
                try {
                    StartupBehavior.valueOf(behaviorName)
                } catch (e: Exception) {
                    StartupBehavior.DO_NOTHING
                }
            } ?: StartupBehavior.DO_NOTHING,
            theme = preferences[SettingsKeys.THEME]?.let { themeName ->
                try {
                    ThemePreference.valueOf(themeName)
                } catch (e: Exception) {
                    ThemePreference.SYSTEM
                }
            } ?: ThemePreference.SYSTEM
        )
    }

    override val lastStudioRoute: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.LAST_STUDIO_ROUTE] ?: "editor"
    }

    override val lastActiveChannels: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.LAST_ACTIVE_CHANNELS]
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    override suspend fun updateAnimatePeriod(period: Int) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.ANIMATE_PERIOD] = period
        }
    }

    override suspend fun updateAnimateCycles(cycles: Int) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.ANIMATE_CYCLES] = cycles
        }
    }

    override suspend fun updateAnimateInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.ANIMATE_INTERVAL] = interval
        }
    }

    override suspend fun updateDefaultZone(zone: GlyphZone?) {
        context.dataStore.edit { preferences ->
            if (zone != null) {
                preferences[SettingsKeys.DEFAULT_ZONE] = zone.name
            } else {
                preferences.remove(SettingsKeys.DEFAULT_ZONE)
            }
        }
    }

    override suspend fun updateStartupBehavior(behavior: StartupBehavior) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.STARTUP_BEHAVIOR] = behavior.name
        }
    }

    override suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.THEME] = theme.name
        }
    }

    override suspend fun updateLastStudioRoute(route: String) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.LAST_STUDIO_ROUTE] = route
        }
    }

    override suspend fun updateLastActiveChannels(channels: Set<Int>) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.LAST_ACTIVE_CHANNELS] =
                channels.sorted().joinToString(",")
        }
    }

    override val tourCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.TOUR_COMPLETED] ?: false
    }

    override val ambientRitual: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.AMBIENT_RITUAL] ?: "FOCUS"
    }

    override val ambientBrightness: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.AMBIENT_BRIGHTNESS] ?: 0.45f
    }

    override suspend fun setTourCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.TOUR_COMPLETED] = completed
        }
    }

    override suspend fun resetTips() {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.TOUR_COMPLETED] = false
        }
    }

    override suspend fun updateAmbientRitual(ritual: String) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.AMBIENT_RITUAL] = ritual
        }
    }

    override suspend fun updateAmbientBrightness(brightness: Float) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.AMBIENT_BRIGHTNESS] = brightness.coerceIn(0.1f, 1f)
        }
    }
}
