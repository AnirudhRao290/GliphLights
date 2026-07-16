package com.example.gliphlights.sdk

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.GlyphZone
import com.example.gliphlights.models.SdkResult
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlyphManagerWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GlyphManagerWrapper"
    }

    enum class SessionState {
        DISCONNECTED,
        INITIALIZING,
        CONNECTED,
        SESSION_ACTIVE,
        SESSION_CLOSED
    }

    private val _sessionState = MutableStateFlow(SessionState.DISCONNECTED)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _glyphState = MutableStateFlow(GlyphState.INACTIVE)
    val glyphState: StateFlow<GlyphState> = _glyphState.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfo.UNKNOWN)
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private var glyphManager: GlyphManager? = null
    private var serviceConnected = CompletableDeferred<Unit>()
    private val activeChannels = mutableSetOf<Int>()

    suspend fun init(): SdkResult<Unit> {
        return try {
            _sessionState.value = SessionState.INITIALIZING
            serviceConnected = CompletableDeferred()

            val device = detectDevice()
            _deviceInfo.value = device

            if (!device.isSupported) {
                _sessionState.value = SessionState.DISCONNECTED
                return SdkResult.Error(
                    Exception("Unsupported device"),
                    "This device is not supported by the Glyph SDK"
                )
            }

            withContext(Dispatchers.IO) {
                glyphManager = GlyphManager.getInstance(context)
                val callback = object : GlyphManager.Callback {
                    override fun onServiceConnected(componentName: ComponentName) {
                        Log.d(TAG, "Glyph service connected")
                        serviceConnected.complete(Unit)
                    }

                    override fun onServiceDisconnected(componentName: ComponentName) {
                        Log.e(TAG, "Glyph service disconnected")
                        serviceConnected.completeExceptionally(
                            Exception("Glyph service disconnected")
                        )
                    }
                }
                glyphManager?.init(callback)
                serviceConnected.await()
            }

            _sessionState.value = SessionState.CONNECTED
            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Glyph SDK", e)
            _sessionState.value = SessionState.DISCONNECTED
            SdkResult.Error(e, "Failed to initialize Glyph SDK: ${e.message}")
        }
    }

    suspend fun register(): SdkResult<Unit> {
        return try {
            if (_sessionState.value != SessionState.CONNECTED) {
                return SdkResult.Error(
                    Exception("Not connected"),
                    "SDK must be connected before registration"
                )
            }

            withContext(Dispatchers.IO) {
                val result = glyphManager?.register(Glyph.DEVICE_24111) ?: false
                if (!result) {
                    throw Exception("Registration failed - not authorized")
                }
            }

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register", e)
            SdkResult.Error(e, "Failed to register: ${e.message}")
        }
    }

    suspend fun openSession(): SdkResult<Unit> {
        return try {
            if (_sessionState.value != SessionState.CONNECTED &&
                _sessionState.value != SessionState.SESSION_CLOSED
            ) {
                return SdkResult.Error(
                    Exception("Invalid state"),
                    "Cannot open session in current state"
                )
            }

            withContext(Dispatchers.IO) {
                glyphManager?.openSession()
            }

            _sessionState.value = SessionState.SESSION_ACTIVE
            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open session", e)
            SdkResult.Error(e, "Failed to open session: ${e.message}")
        }
    }

    suspend fun closeSession(): SdkResult<Unit> {
        return try {
            if (_sessionState.value != SessionState.SESSION_ACTIVE) {
                return SdkResult.Success(Unit)
            }

            withContext(Dispatchers.IO) {
                glyphManager?.closeSession()
            }

            _sessionState.value = SessionState.SESSION_CLOSED
            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close session", e)
            SdkResult.Error(e, "Failed to close session: ${e.message}")
        }
    }

    suspend fun toggleChannels(channels: List<Int>): SdkResult<Unit> {
        return try {
            ensureSessionActive()

            val channelsToToggle = channels.toSet()
            val turningOff = channelsToToggle.any { it in activeChannels }
            val turningOn = channelsToToggle.any { it !in activeChannels }

            val newActiveChannels = activeChannels.toMutableSet()
            channels.forEach { channel ->
                if (channel in newActiveChannels) {
                    newActiveChannels.remove(channel)
                } else {
                    newActiveChannels.add(channel)
                }
            }

            if (newActiveChannels.isEmpty()) {
                withContext(Dispatchers.IO) {
                    glyphManager?.turnOff()
                }
            } else {
                withContext(Dispatchers.IO) {
                    val builder = glyphManager?.getGlyphFrameBuilder()
                        ?: throw Exception("GlyphManager not initialized")
                    newActiveChannels.forEach { builder.buildChannel(it) }
                    val frame = builder.build()
                    glyphManager?.toggle(frame)
                }
            }

            activeChannels.clear()
            activeChannels.addAll(newActiveChannels)
            updateGlyphState()

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle channels", e)
            SdkResult.Error(e, "Failed to toggle channels: ${e.message}")
        }
    }

    suspend fun animateChannels(
        channels: List<Int>,
        params: AnimationParams
    ): SdkResult<Unit> {
        return try {
            ensureSessionActive()

            withContext(Dispatchers.IO) {
                val builder = glyphManager?.getGlyphFrameBuilder()
                    ?: throw Exception("GlyphManager not initialized")
                channels.forEach { builder.buildChannel(it) }
                val frame = builder
                    .buildPeriod(params.period)
                    .buildCycles(params.cycles)
                    .buildInterval(params.interval)
                    .build()
                glyphManager?.animate(frame)
            }

            activeChannels.addAll(channels)
            updateGlyphState()

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to animate", e)
            SdkResult.Error(e, "Failed to animate: ${e.message}")
        }
    }

    suspend fun displayProgress(
        progress: Int,
        reverse: Boolean
    ): SdkResult<Unit> {
        return try {
            ensureSessionActive()

            withContext(Dispatchers.IO) {
                val builder = glyphManager?.getGlyphFrameBuilder()
                    ?: throw Exception("GlyphManager not initialized")
                val frame = builder.buildChannelC().build()
                glyphManager?.displayProgress(frame, progress, reverse)
            }

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display progress", e)
            SdkResult.Error(e, "Failed to display progress: ${e.message}")
        }
    }

    suspend fun turnOff(): SdkResult<Unit> {
        return try {
            ensureSessionActive()

            withContext(Dispatchers.IO) {
                glyphManager?.turnOff()
            }

            activeChannels.clear()
            updateGlyphState()

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off", e)
            SdkResult.Error(e, "Failed to turn off: ${e.message}")
        }
    }

    suspend fun toggleWithBrightness(channels: List<Int>, brightness: Float): SdkResult<Unit> {
        return try {
            ensureSessionActive()

            if (brightness <= 0.0f || channels.isEmpty()) {
                withContext(Dispatchers.IO) {
                    glyphManager?.turnOff()
                }
                activeChannels.clear()
                updateGlyphState()
                return SdkResult.Success(Unit)
            }

            val channelCount = channels.size
            val activeCount = (channelCount * brightness.coerceIn(0.0f, 1.0f)).toInt().coerceAtLeast(1)
            val activeChannelsList = channels.take(activeCount)

            withContext(Dispatchers.IO) {
                val builder = glyphManager?.getGlyphFrameBuilder()
                    ?: throw Exception("GlyphManager not initialized")
                activeChannelsList.forEach { builder.buildChannel(it) }
                val frame = builder.build()
                glyphManager?.toggle(frame)
            }

            activeChannels.clear()
            activeChannels.addAll(activeChannelsList)
            updateGlyphState()

            SdkResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle with brightness", e)
            SdkResult.Error(e, "Failed to toggle with brightness: ${e.message}")
        }
    }

    suspend fun cleanup() {
        try {
            closeSession()
            withContext(Dispatchers.IO) {
                glyphManager?.unInit()
            }
            glyphManager = null
            _sessionState.value = SessionState.DISCONNECTED
            activeChannels.clear()
            updateGlyphState()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    private fun detectDevice(): DeviceInfo {
        return if (Common.is24111()) {
            DeviceInfo(
                model = "Nothing Phone (3a) Pro",
                isSupported = true,
                availableZones = listOf(GlyphZone.A, GlyphZone.B, GlyphZone.C)
            )
        } else {
            DeviceInfo.UNKNOWN
        }
    }

    private fun ensureSessionActive() {
        if (_sessionState.value != SessionState.SESSION_ACTIVE) {
            throw IllegalStateException("Session is not active")
        }
    }

    private fun updateGlyphState() {
        val activeZones = mutableSetOf<GlyphZone>()
        activeChannels.forEach { channel ->
            when {
                channel in GlyphZone.A.channels -> activeZones.add(GlyphZone.A)
                channel in GlyphZone.B.channels -> activeZones.add(GlyphZone.B)
                channel in GlyphZone.C.channels -> activeZones.add(GlyphZone.C)
            }
        }
        _glyphState.value = GlyphState(
            isActive = activeChannels.isNotEmpty(),
            activeChannels = activeChannels.toSet(),
            activeZones = activeZones
        )
    }
}
