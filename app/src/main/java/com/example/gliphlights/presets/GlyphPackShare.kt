package com.example.gliphlights.presets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.gliphlights.repository.PresetRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphPackShare @Inject constructor(
    @ApplicationContext private val context: Context,
    private val presetRepository: PresetRepository
) {
    suspend fun exportAndShareIntent(presetIds: List<String>): Intent {
        val json = presetRepository.exportPack(presetIds)
        val dir = File(context.cacheDir, "glyphpacks").also { it.mkdirs() }
        val file = File(dir, "preset_${System.currentTimeMillis()}.glyphpack")
        file.writeText(json)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Glyph Studio preset pack")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun importFromUri(uri: Uri): List<GlyphPreset> {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return emptyList()
        return presetRepository.importPack(json)
    }

    suspend fun importFromJson(json: String): List<GlyphPreset> = presetRepository.importPack(json)
}
