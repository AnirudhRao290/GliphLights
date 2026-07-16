package com.example.gliphlights.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioCaptureManager"
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_SIZE_SAMPLES = 4096
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _audioBuffer = MutableSharedFlow<ShortArray>(
        replay = 1,
        extraBufferCapacity = 1
    )
    val audioBuffer: SharedFlow<ShortArray> = _audioBuffer.asSharedFlow()

    val isCapturing: Boolean
        get() = captureJob?.isActive == true

    fun hasPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun start(): Boolean {
        if (captureJob?.isActive == true) {
            Log.w(TAG, "Already capturing")
            return true
        }

        if (!hasPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return false
        }

        return try {
            val bufferSize = maxOf(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
                BUFFER_SIZE_SAMPLES * 2
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            captureJob = scope.launch { captureLoop() }

            Log.d(TAG, "Audio capture started (bufferSize=$bufferSize)")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting AudioRecord", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio capture", e)
            false
        }
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "AudioRecord was not recording", e)
        }

        audioRecord?.release()
        audioRecord = null

        Log.d(TAG, "Audio capture stopped")
    }

    private suspend fun captureLoop() {
        val record = audioRecord ?: return
        val readBuffer = ShortArray(BUFFER_SIZE_SAMPLES)

        while (currentCoroutineContext().isActive) {
            val samplesRead = record.read(readBuffer, 0, BUFFER_SIZE_SAMPLES)
            if (samplesRead > 0) {
                val buffer = if (samplesRead == BUFFER_SIZE_SAMPLES) {
                    readBuffer.copyOf()
                } else {
                    readBuffer.copyOf(samplesRead)
                }
                _audioBuffer.tryEmit(buffer)
            } else if (samplesRead < 0) {
                Log.e(TAG, "AudioRecord read error: $samplesRead")
                break
            }
            yield()
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
