package com.example.gliphlights.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gliphlights.pathbuilder.model.InterpolationMode
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.physics.model.PhysicsMode
import com.example.gliphlights.physics.model.PhysicsParams
import com.example.gliphlights.presets.GlyphPreset
import com.example.gliphlights.presets.PresetType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.presetDataStore: DataStore<Preferences> by preferencesDataStore(name = "glyph_presets")

private val KEY_PRESETS = stringPreferencesKey("presets_json")

interface PresetRepository {
    val presets: Flow<List<GlyphPreset>>
    suspend fun save(preset: GlyphPreset): GlyphPreset
    suspend fun saveEditorFrame(name: String, channels: Set<Int>): GlyphPreset
    suspend fun savePath(
        name: String,
        nodes: List<PathNode>,
        settings: PathSettings
    ): GlyphPreset
    suspend fun savePhysics(
        name: String,
        mode: PhysicsMode,
        params: PhysicsParams
    ): GlyphPreset
    suspend fun saveVisualizer(
        name: String,
        mode: String,
        sensitivity: Float
    ): GlyphPreset
    suspend fun update(preset: GlyphPreset): GlyphPreset
    suspend fun fork(id: String, newName: String? = null): GlyphPreset?
    suspend fun delete(id: String)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun getById(id: String): GlyphPreset?
    suspend fun pinned(): List<GlyphPreset>
    suspend fun exportPack(ids: List<String>): String
    suspend fun importPack(json: String): List<GlyphPreset>
}

@Singleton
class PresetRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PresetRepository {

    override val presets: Flow<List<GlyphPreset>> =
        context.presetDataStore.data.map { prefs ->
            parseList(prefs[KEY_PRESETS].orEmpty()).sortedWith(
                compareByDescending<GlyphPreset> { it.pinned }.thenByDescending { it.updatedAtMs }
            )
        }

    override suspend fun save(preset: GlyphPreset): GlyphPreset {
        val toStore = if (preset.id.isBlank()) {
            preset.copy(id = UUID.randomUUID().toString())
        } else {
            preset
        }
        upsert(toStore)
        return toStore
    }

    override suspend fun saveEditorFrame(name: String, channels: Set<Int>): GlyphPreset {
        val preset = GlyphPreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Frame ${System.currentTimeMillis() % 10000}" },
            type = PresetType.EDITOR,
            channels = channels
        )
        upsert(preset)
        return preset
    }

    override suspend fun savePath(
        name: String,
        nodes: List<PathNode>,
        settings: PathSettings
    ): GlyphPreset {
        val preset = GlyphPreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Path ${System.currentTimeMillis() % 10000}" },
            type = PresetType.PATH,
            pathNodes = nodes,
            pathSettings = settings
        )
        upsert(preset)
        return preset
    }

    override suspend fun savePhysics(
        name: String,
        mode: PhysicsMode,
        params: PhysicsParams
    ): GlyphPreset {
        val preset = GlyphPreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Physics ${mode.displayName}" },
            type = PresetType.PHYSICS,
            physicsMode = mode,
            physicsParams = params
        )
        upsert(preset)
        return preset
    }

    override suspend fun saveVisualizer(
        name: String,
        mode: String,
        sensitivity: Float
    ): GlyphPreset {
        val preset = GlyphPreset(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Viz $mode" },
            type = PresetType.VISUALIZER,
            visualizerMode = mode,
            visualizerSensitivity = sensitivity
        )
        upsert(preset)
        return preset
    }

    override suspend fun update(preset: GlyphPreset): GlyphPreset {
        val updated = preset.copy(updatedAtMs = System.currentTimeMillis())
        upsert(updated)
        return updated
    }

    override suspend fun fork(id: String, newName: String?): GlyphPreset? {
        val original = getById(id) ?: return null
        val forked = original.copy(
            id = UUID.randomUUID().toString(),
            name = newName ?: "${original.name} (remix)",
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            pinned = false,
            forkedFromId = original.id
        )
        upsert(forked)
        return forked
    }

    override suspend fun delete(id: String) {
        context.presetDataStore.edit { prefs ->
            val current = parseList(prefs[KEY_PRESETS].orEmpty()).filterNot { it.id == id }
            prefs[KEY_PRESETS] = serializeList(current)
        }
    }

    override suspend fun setPinned(id: String, pinned: Boolean) {
        val existing = getById(id) ?: return
        update(existing.copy(pinned = pinned))
    }

    override suspend fun getById(id: String): GlyphPreset? {
        return context.presetDataStore.data.map { parseList(it[KEY_PRESETS].orEmpty()) }.first()
            .find { it.id == id }
    }

    override suspend fun pinned(): List<GlyphPreset> =
        presets.first().filter { it.pinned }

    override suspend fun exportPack(ids: List<String>): String {
        val all = presets.first().filter { it.id in ids }
        val root = JSONObject()
        root.put("format", "glyphpack")
        root.put("version", 1)
        root.put("presets", JSONArray().also { arr ->
            all.forEach { arr.put(toJson(it)) }
        })
        return root.toString(2)
    }

    override suspend fun importPack(json: String): List<GlyphPreset> {
        val root = JSONObject(json)
        val arr = root.optJSONArray("presets") ?: JSONArray(json)
        val imported = buildList {
            for (i in 0 until arr.length()) {
                val parsed = fromJson(arr.getJSONObject(i))
                add(
                    parsed.copy(
                        id = UUID.randomUUID().toString(),
                        createdAtMs = System.currentTimeMillis(),
                        updatedAtMs = System.currentTimeMillis(),
                        pinned = false
                    )
                )
            }
        }
        imported.forEach { upsert(it) }
        return imported
    }

    private suspend fun upsert(preset: GlyphPreset) {
        context.presetDataStore.edit { prefs ->
            val current = parseList(prefs[KEY_PRESETS].orEmpty()).toMutableList()
            val idx = current.indexOfFirst { it.id == preset.id }
            if (idx >= 0) current[idx] = preset else current.add(0, preset)
            prefs[KEY_PRESETS] = serializeList(current)
        }
    }

    private fun serializeList(list: List<GlyphPreset>): String {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        return arr.toString()
    }

    private fun parseList(raw: String): List<GlyphPreset> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(fromJson(arr.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun toJson(p: GlyphPreset): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("type", p.type.name)
        put("createdAtMs", p.createdAtMs)
        put("updatedAtMs", p.updatedAtMs)
        put("pinned", p.pinned)
        put("forkedFromId", p.forkedFromId)
        put("channels", JSONArray(p.channels.toList()))
        put("pathSettings", pathSettingsToJson(p.pathSettings))
        put("pathNodes", JSONArray().also { arr ->
            p.pathNodes.forEach { n ->
                arr.put(JSONObject().apply {
                    put("nodeId", n.nodeId)
                    put("sdkIndex", n.sdkIndex)
                    put("regionName", n.regionName)
                })
            }
        })
        put("physicsMode", p.physicsMode?.name)
        put("physicsParams", physicsParamsToJson(p.physicsParams))
        put("visualizerMode", p.visualizerMode)
        put("visualizerSensitivity", p.visualizerSensitivity.toDouble())
    }

    private fun fromJson(obj: JSONObject): GlyphPreset {
        val channelsArr = obj.optJSONArray("channels")
        val channels = buildSet {
            if (channelsArr != null) {
                for (i in 0 until channelsArr.length()) add(channelsArr.getInt(i))
            }
        }
        val nodesArr = obj.optJSONArray("pathNodes")
        val nodes = buildList {
            if (nodesArr != null) {
                for (i in 0 until nodesArr.length()) {
                    val n = nodesArr.getJSONObject(i)
                    add(
                        PathNode(
                            nodeId = n.getString("nodeId"),
                            sdkIndex = n.getInt("sdkIndex"),
                            regionName = n.getString("regionName")
                        )
                    )
                }
            }
        }
        val type = try {
            PresetType.valueOf(obj.optString("type", PresetType.EDITOR.name))
        } catch (_: Exception) {
            PresetType.EDITOR
        }
        val physicsMode = obj.optString("physicsMode", "").takeIf { it.isNotBlank() }?.let {
            try {
                PhysicsMode.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }
        return GlyphPreset(
            id = obj.getString("id"),
            name = obj.getString("name"),
            type = type,
            createdAtMs = obj.optLong("createdAtMs", 0L),
            updatedAtMs = obj.optLong("updatedAtMs", obj.optLong("createdAtMs", 0L)),
            pinned = obj.optBoolean("pinned", false),
            forkedFromId = obj.optString("forkedFromId").takeIf { it.isNotBlank() },
            channels = channels,
            pathNodes = nodes,
            pathSettings = pathSettingsFromJson(obj.optJSONObject("pathSettings")),
            physicsMode = physicsMode,
            physicsParams = physicsParamsFromJson(obj.optJSONObject("physicsParams")),
            visualizerMode = obj.optString("visualizerMode").takeIf { it.isNotBlank() },
            visualizerSensitivity = obj.optDouble("visualizerSensitivity", 1.0).toFloat()
        )
    }

    private fun pathSettingsToJson(s: PathSettings): JSONObject = JSONObject().apply {
        put("animationSpeed", s.animationSpeed.toDouble())
        put("nodeDurationMs", s.nodeDurationMs)
        put("fadeDurationMs", s.fadeDurationMs)
        put("attackMs", s.attackMs)
        put("releaseMs", s.releaseMs)
        put("sustainRatio", s.sustainRatio.toDouble())
        put("brightness", s.brightness.toDouble())
        put("trailLength", s.trailLength)
        put("trailFade", s.trailFade.toDouble())
        put("repeatCount", s.repeatCount)
        put("infiniteLoop", s.infiniteLoop)
        put("reversePlayback", s.reversePlayback)
        put("pingPong", s.pingPong)
        put("interpolation", s.interpolation.name)
        put("smoothingStrength", s.smoothingStrength.toDouble())
        put("samplingDensityPx", s.samplingDensityPx.toDouble())
        put("minimumNodeDistance", s.minimumNodeDistance)
    }

    private fun pathSettingsFromJson(obj: JSONObject?): PathSettings {
        if (obj == null) return PathSettings()
        return PathSettings(
            animationSpeed = obj.optDouble("animationSpeed", 1.0).toFloat(),
            nodeDurationMs = obj.optLong("nodeDurationMs", 120L),
            fadeDurationMs = obj.optLong("fadeDurationMs", 80L),
            attackMs = obj.optLong("attackMs", -1L),
            releaseMs = obj.optLong("releaseMs", -1L),
            sustainRatio = obj.optDouble("sustainRatio", 0.55).toFloat(),
            brightness = obj.optDouble("brightness", 1.0).toFloat(),
            trailLength = obj.optInt("trailLength", 3),
            trailFade = obj.optDouble("trailFade", 0.55).toFloat(),
            repeatCount = obj.optInt("repeatCount", 1),
            infiniteLoop = obj.optBoolean("infiniteLoop", false),
            reversePlayback = obj.optBoolean("reversePlayback", false),
            pingPong = obj.optBoolean("pingPong", false),
            interpolation = try {
                InterpolationMode.valueOf(obj.optString("interpolation", InterpolationMode.LINEAR.name))
            } catch (_: Exception) {
                InterpolationMode.LINEAR
            },
            smoothingStrength = obj.optDouble("smoothingStrength", 0.35).toFloat(),
            samplingDensityPx = obj.optDouble("samplingDensityPx", 4.0).toFloat(),
            minimumNodeDistance = obj.optInt("minimumNodeDistance", 1)
        )
    }

    private fun physicsParamsToJson(p: PhysicsParams): JSONObject = JSONObject().apply {
        put("gravityStrength", p.gravityStrength.toDouble())
        put("flowSpeed", p.flowSpeed.toDouble())
        put("damping", p.damping.toDouble())
        put("trailLength", p.trailLength)
        put("brightness", p.brightness.toDouble())
        put("fluidAmount", p.fluidAmount.toDouble())
        put("viscosity", p.viscosity.toDouble())
        put("surfaceTension", p.surfaceTension.toDouble())
        put("gravityMultiplier", p.gravityMultiplier.toDouble())
        put("energyLoss", p.energyLoss.toDouble())
        put("glowIntensity", p.glowIntensity.toDouble())
        put("simulationSpeed", p.simulationSpeed.toDouble())
        put("particleCount", p.particleCount)
    }

    private fun physicsParamsFromJson(obj: JSONObject?): PhysicsParams {
        if (obj == null) return PhysicsParams()
        return PhysicsParams(
            gravityStrength = obj.optDouble("gravityStrength", 1.0).toFloat(),
            flowSpeed = obj.optDouble("flowSpeed", 1.0).toFloat(),
            damping = obj.optDouble("damping", 0.85).toFloat(),
            trailLength = obj.optInt("trailLength", 3),
            brightness = obj.optDouble("brightness", 1.0).toFloat(),
            fluidAmount = obj.optDouble("fluidAmount", 0.45).toFloat(),
            viscosity = obj.optDouble("viscosity", 0.55).toFloat(),
            surfaceTension = obj.optDouble("surfaceTension", 0.4).toFloat(),
            gravityMultiplier = obj.optDouble("gravityMultiplier", 1.0).toFloat(),
            energyLoss = obj.optDouble("energyLoss", 0.92).toFloat(),
            glowIntensity = obj.optDouble("glowIntensity", 1.0).toFloat(),
            simulationSpeed = obj.optDouble("simulationSpeed", 1.0).toFloat(),
            particleCount = obj.optInt("particleCount", 48)
        )
    }
}
