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
        // App came to foreground - re-open session
        scope.launch {
            glyphRepository.openSession()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App went to background - close session
        scope.launch {
            glyphRepository.closeSession()
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
