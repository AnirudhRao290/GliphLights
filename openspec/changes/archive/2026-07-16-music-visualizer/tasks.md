## 1. SDK Integration

- [x] 1.1 Add `toggleWithBrightness(channels, brightness)` method to `GlyphRepository` interface
- [x] 1.2 Implement `toggleWithBrightness()` in `GlyphRepositoryImpl` using `buildBrightness()`

## 2. Audio Capture

- [x] 2.1 Create `AudioCaptureManager` class with `AudioRecord` initialization (44.1kHz, mono, PCM 16-bit)
- [x] 2.2 Implement circular buffer (4096 samples) for audio data storage
- [x] 2.3 Implement continuous audio capture loop on Dispatchers.IO coroutine
- [x] 2.4 Add start/stop methods with proper resource lifecycle management
- [x] 2.5 Expose audio buffer as `SharedFlow<ShortArray>` for downstream processing

## 3. Signal Processing

- [x] 3.1 Create `SignalProcessor` class with RMS amplitude calculation
- [x] 3.2 Add peak amplitude tracking per buffer
- [x] 3.3 Implement EMA smoothing with configurable alpha (default 0.3)
- [x] 3.4 Implement configurable noise gate threshold
- [x] 3.5 Add sensitivity scaling factor
- [x] 3.6 Expose processed amplitude as `StateFlow<Float>` (0.0–1.0)

## 4. Visualizer Engine

- [x] 4.1 Create `VisualizationMode` interface with `render(amplitude, timestamp): GlyphCommand`
- [x] 4.2 Implement `PulseMode` — maps amplitude to global brightness
- [x] 4.3 Implement `WaveMode` — activates zones A→B→C sequentially based on amplitude
- [x] 4.4 Implement `BeatMode` — detects peaks, flashes Glyph on beat
- [x] 4.5 Implement `GlowMode` — smooth brightness following with extra EMA (alpha=0.15)
- [x] 4.6 Create `GlyphVisualizer` engine class with rate limiting (33ms min interval)
- [x] 4.7 Wire engine to `AudioCaptureManager` and `SignalProcessor`
- [x] 4.8 Add start/stop lifecycle management with cleanup

## 5. ViewModel

- [x] 5.1 Create `MusicVisualizerViewModel` with Hilt injection
- [x] 5.2 Add UI state data class (isRunning, mode, sensitivity, noiseGate, audioLevel, fps, latency, permission status)
- [x] 5.3 Implement start/stop actions
- [x] 5.4 Implement mode selection
- [x] 5.5 Implement sensitivity and noise gate adjustment
- [x] 5.6 Collect audio level and performance metrics for UI display
- [x] 5.7 Handle permission state and request flow

## 6. UI Screen

- [x] 6.1 Create `MusicVisualizerScreen` composable with Material 3 styling
- [x] 6.2 Add Start/Stop buttons
- [x] 6.3 Add visualization mode selector (Pulse, Wave, Beat, Glow)
- [x] 6.4 Add sensitivity slider
- [x] 6.5 Add noise gate slider
- [x] 6.6 Add real-time waveform/volume bar meter
- [x] 6.7 Add status indicators (FPS, update rate, latency, permission status)
- [x] 6.8 Add permission request flow with explanation dialog
- [x] 6.9 Add debug overlay (raw amplitude, filtered, RMS, peak, processing time)

## 7. Navigation & Integration

- [x] 7.1 Add visualizer route to navigation graph
- [x] 7.2 Add Visualizer entry point to main dashboard or navigation
- [x] 7.3 Add RECORD_AUDIO permission to AndroidManifest.xml

## 8. Spec Sync

- [x] 8.1 Sync delta specs to main specs
