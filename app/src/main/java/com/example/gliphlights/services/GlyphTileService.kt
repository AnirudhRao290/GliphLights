package com.example.gliphlights.services

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.gliphlights.MainActivity
import com.example.gliphlights.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GlyphTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var glyphRepository: com.example.gliphlights.repository.GlyphRepository

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // Launch activity to ensure SDK is initialized
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)

        // Toggle glyph after a short delay to allow SDK initialization
        scope.launch {
            kotlinx.coroutines.delay(500)
            glyphRepository.toggleAll()
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return

        scope.launch {
            glyphRepository.glyphState.collect { state ->
                tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = if (state.isActive) "Glyph On" else "Glyph Off"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    tile.stateDescription = if (state.isActive) "Active" else "Inactive"
                }

                tile.updateTile()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
