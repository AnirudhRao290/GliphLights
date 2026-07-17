package com.example.gliphlights.physics

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.physicsSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "glyph_physics_settings"
)

data class SavedPhysicsSettings(
    val mode: PhysicsMode,
    val params: PhysicsParams
)

interface PhysicsSettingsRepository {
    val settings: Flow<SavedPhysicsSettings?>
    suspend fun save(mode: PhysicsMode, params: PhysicsParams)
    suspend fun clear()
}

@Singleton
class PhysicsSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : PhysicsSettingsRepository {

    private object Keys {
        val MODE = stringPreferencesKey("mode")
        val GRAVITY_STRENGTH = floatPreferencesKey("gravity_strength")
        val FLOW_SPEED = floatPreferencesKey("flow_speed")
        val DAMPING = floatPreferencesKey("damping")
        val TRAIL_LENGTH = intPreferencesKey("trail_length")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val FLUID_AMOUNT = floatPreferencesKey("fluid_amount")
        val VISCOSITY = floatPreferencesKey("viscosity")
        val SURFACE_TENSION = floatPreferencesKey("surface_tension")
        val GRAVITY_MULTIPLIER = floatPreferencesKey("gravity_multiplier")
        val ENERGY_LOSS = floatPreferencesKey("energy_loss")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val SIMULATION_SPEED = floatPreferencesKey("simulation_speed")
        val PARTICLE_COUNT = intPreferencesKey("particle_count")
    }

    override val settings: Flow<SavedPhysicsSettings?> =
        context.physicsSettingsDataStore.data.map { prefs ->
            val modeName = prefs[Keys.MODE] ?: return@map null
            val mode = runCatching { PhysicsMode.valueOf(modeName) }.getOrNull()
                ?: return@map null
            SavedPhysicsSettings(
                mode = mode,
                params = PhysicsParams(
                    gravityStrength = prefs[Keys.GRAVITY_STRENGTH] ?: PhysicsParams().gravityStrength,
                    flowSpeed = prefs[Keys.FLOW_SPEED] ?: PhysicsParams().flowSpeed,
                    damping = prefs[Keys.DAMPING] ?: PhysicsParams().damping,
                    trailLength = prefs[Keys.TRAIL_LENGTH] ?: PhysicsParams().trailLength,
                    brightness = prefs[Keys.BRIGHTNESS] ?: PhysicsParams().brightness,
                    fluidAmount = prefs[Keys.FLUID_AMOUNT] ?: PhysicsParams().fluidAmount,
                    viscosity = prefs[Keys.VISCOSITY] ?: PhysicsParams().viscosity,
                    surfaceTension = prefs[Keys.SURFACE_TENSION] ?: PhysicsParams().surfaceTension,
                    gravityMultiplier = prefs[Keys.GRAVITY_MULTIPLIER] ?: PhysicsParams().gravityMultiplier,
                    energyLoss = prefs[Keys.ENERGY_LOSS] ?: PhysicsParams().energyLoss,
                    glowIntensity = prefs[Keys.GLOW_INTENSITY] ?: PhysicsParams().glowIntensity,
                    simulationSpeed = prefs[Keys.SIMULATION_SPEED] ?: PhysicsParams().simulationSpeed,
                    particleCount = prefs[Keys.PARTICLE_COUNT] ?: PhysicsParams().particleCount
                )
            )
        }

    override suspend fun save(mode: PhysicsMode, params: PhysicsParams) {
        context.physicsSettingsDataStore.edit { prefs ->
            prefs[Keys.MODE] = mode.name
            prefs[Keys.GRAVITY_STRENGTH] = params.gravityStrength
            prefs[Keys.FLOW_SPEED] = params.flowSpeed
            prefs[Keys.DAMPING] = params.damping
            prefs[Keys.TRAIL_LENGTH] = params.trailLength
            prefs[Keys.BRIGHTNESS] = params.brightness
            prefs[Keys.FLUID_AMOUNT] = params.fluidAmount
            prefs[Keys.VISCOSITY] = params.viscosity
            prefs[Keys.SURFACE_TENSION] = params.surfaceTension
            prefs[Keys.GRAVITY_MULTIPLIER] = params.gravityMultiplier
            prefs[Keys.ENERGY_LOSS] = params.energyLoss
            prefs[Keys.GLOW_INTENSITY] = params.glowIntensity
            prefs[Keys.SIMULATION_SPEED] = params.simulationSpeed
            prefs[Keys.PARTICLE_COUNT] = params.particleCount
        }
    }

    override suspend fun clear() {
        context.physicsSettingsDataStore.edit { it.clear() }
    }
}
