package com.example.gliphlights.pathbuilder

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.gliphlights.pathbuilder.model.InterpolationMode
import com.example.gliphlights.pathbuilder.model.PathNode
import com.example.gliphlights.pathbuilder.model.PathSettings
import com.example.gliphlights.pathbuilder.model.SavedSequence
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sequenceDataStore: DataStore<Preferences> by preferencesDataStore(name = "glyph_path_sequences")

private val KEY_SEQUENCES = stringPreferencesKey("saved_sequences_json")

interface SequenceRepository {
    val savedSequences: Flow<List<SavedSequence>>
    suspend fun save(name: String, nodes: List<PathNode>, settings: PathSettings): SavedSequence
    suspend fun delete(id: String)
    suspend fun getById(id: String): SavedSequence?
}

@Singleton
class SequenceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SequenceRepository {

    override val savedSequences: Flow<List<SavedSequence>> =
        context.sequenceDataStore.data.map { prefs ->
            parseList(prefs[KEY_SEQUENCES].orEmpty())
        }

    override suspend fun save(
        name: String,
        nodes: List<PathNode>,
        settings: PathSettings
    ): SavedSequence {
        val sequence = SavedSequence(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Sequence ${System.currentTimeMillis() % 10000}" },
            nodes = nodes,
            settings = settings,
            createdAtMs = System.currentTimeMillis(),
            isPreset = false
        )
        context.sequenceDataStore.edit { prefs ->
            val current = parseList(prefs[KEY_SEQUENCES].orEmpty()).toMutableList()
            current.add(0, sequence)
            prefs[KEY_SEQUENCES] = serializeList(current)
        }
        return sequence
    }

    override suspend fun delete(id: String) {
        context.sequenceDataStore.edit { prefs ->
            val current = parseList(prefs[KEY_SEQUENCES].orEmpty()).filterNot { it.id == id }
            prefs[KEY_SEQUENCES] = serializeList(current)
        }
    }

    override suspend fun getById(id: String): SavedSequence? {
        val list = context.sequenceDataStore.data.map { parseList(it[KEY_SEQUENCES].orEmpty()) }.first()
        return list.find { it.id == id }
    }

    private fun serializeList(list: List<SavedSequence>): String {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        return arr.toString()
    }

    private fun parseList(raw: String): List<SavedSequence> {
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

    private fun toJson(seq: SavedSequence): JSONObject = JSONObject().apply {
        put("id", seq.id)
        put("name", seq.name)
        put("createdAtMs", seq.createdAtMs)
        put("isPreset", seq.isPreset)
        put("settings", settingsToJson(seq.settings))
        put("nodes", JSONArray().also { arr ->
            seq.nodes.forEach { n ->
                arr.put(JSONObject().apply {
                    put("nodeId", n.nodeId)
                    put("sdkIndex", n.sdkIndex)
                    put("regionName", n.regionName)
                })
            }
        })
    }

    private fun fromJson(obj: JSONObject): SavedSequence {
        val nodesArr = obj.getJSONArray("nodes")
        val nodes = buildList {
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
        return SavedSequence(
            id = obj.getString("id"),
            name = obj.getString("name"),
            nodes = nodes,
            settings = settingsFromJson(obj.optJSONObject("settings")),
            createdAtMs = obj.optLong("createdAtMs", 0L),
            isPreset = obj.optBoolean("isPreset", false)
        )
    }

    private fun settingsToJson(s: PathSettings): JSONObject = JSONObject().apply {
        put("animationSpeed", s.animationSpeed.toDouble())
        put("nodeDurationMs", s.nodeDurationMs)
        put("fadeDurationMs", s.fadeDurationMs)
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

    private fun settingsFromJson(obj: JSONObject?): PathSettings {
        if (obj == null) return PathSettings()
        return PathSettings(
            animationSpeed = obj.optDouble("animationSpeed", 1.0).toFloat(),
            nodeDurationMs = obj.optLong("nodeDurationMs", 120L),
            fadeDurationMs = obj.optLong("fadeDurationMs", 80L),
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
}
