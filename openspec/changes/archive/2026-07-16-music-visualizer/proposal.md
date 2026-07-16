## Why

The Glyph Control app currently provides manual control over the Nothing Phone (3a) Pro's Glyph Interface. Users want an immersive, reactive experience where the Glyph LEDs respond to ambient sound in real-time. A music visualizer transforms the phone into a dynamic light display that reacts to music, conversations, and environmental audio — a signature Nothing feature that no third-party app has fully delivered.

## What Changes

- Add a real-time Music Visualizer feature that captures microphone audio and drives the Glyph Interface
- Add audio capture using Android's `AudioRecord` API (44.1kHz, mono, PCM 16-bit)
- Add signal processing: RMS volume, peak amplitude, EMA smoothing, noise gate
- Add 4 visualization modes: Pulse, Wave, Beat, Glow — mapped to Glyph zones and brightness
- Add a dedicated Music Visualizer screen with controls (start/stop, sensitivity, noise gate, mode selector)
- Add microphone permission handling with graceful denial
- Add performance monitoring (FPS, latency, update rate)
- Add optional debug overlay for developer diagnostics

**SDK Limitations (documented, not modified):**
- The Glyph SDK uses discrete `toggle()` and `animate()` commands — no continuous frame buffer
- Maximum update rate is limited by SDK command throughput (~30 FPS target)
- Brightness is per-frame via `buildBrightness()`, not per-individual-LED
- No color control — only on/off/brightness levels
- These limitations are worked around by mapping audio amplitude to brightness and zone activation patterns

## Capabilities

### New Capabilities
- `audio-capture`: Microphone audio capture using AudioRecord with circular buffer, background coroutine processing, and permission handling
- `audio-signal-processing`: RMS/peak/amplitude calculation, EMA smoothing, noise gate, normalization
- `glyph-visualizer`: Core visualizer engine mapping audio signals to Glyph commands (toggle with brightness)
- `music-visualizer-ui`: Dedicated screen with controls, status indicators, waveform meter, and debug overlay

### Modified Capabilities
- `glyph-sdk-integration`: Add `toggleWithBrightness(channels, brightness)` convenience method to GlyphRepository for the visualizer to use

## Impact

- **New packages:** `audio/`, `ui/visualizer/`
- **New files:** AudioCaptureManager, SignalProcessor, GlyphVisualizer, GlyphController, MusicVisualizerScreen, MusicVisualizerViewModel
- **Modified:** GlyphRepository (add toggleWithBrightness), GlyphRepositoryImpl, Navigation (add visualizer route)
- **Dependencies:** None new — uses Android's built-in AudioRecord and RECORD_AUDIO permission
- **Permissions:** `android.permission.RECORD_AUDIO` (runtime permission required)
