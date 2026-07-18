package com.example.gliphlights.utils

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.gliphlights.repository.GlyphRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glyphRepository: GlyphRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Do not auto-open sessions — Editor / Dashboard manage their own lifecycle.
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Turn lights off when backgrounded, but keep the session so Editor can resume.
        // Preserve last-state so SHOW_LAST_STATE can restore lit channels on cold start.
        scope.launch {
            if (glyphRepository.isSessionActive.value) {
                glyphRepository.turnOffPreservingLastState()
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
