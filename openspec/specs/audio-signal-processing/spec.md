### Requirement: RMS amplitude calculation
The system SHALL calculate the Root Mean Square (RMS) amplitude from each audio buffer to represent the signal's power.

#### Scenario: RMS calculated per buffer
- **WHEN** a new 4096-sample buffer is available
- **THEN** the RMS amplitude is computed and returned as a value between 0.0 and 1.0

#### Scenario: Silence produces near-zero RMS
- **WHEN** the microphone captures silence
- **THEN** the RMS amplitude is below 0.01

### Requirement: Peak amplitude tracking
The system SHALL track the peak (maximum absolute) amplitude within each audio buffer.

#### Scenario: Peak captured per buffer
- **WHEN** a new buffer is processed
- **THEN** the peak amplitude is recorded as a value between 0.0 and 1.0

### Requirement: Exponential Moving Average smoothing
The system SHALL apply Exponential Moving Average (EMA) smoothing to the amplitude signal with a configurable alpha parameter (default 0.3) to prevent flickering.

#### Scenario: Smooth transitions on volume change
- **WHEN** audio amplitude changes abruptly from 0.1 to 0.9
- **THEN** the smoothed output transitions gradually over multiple buffers, avoiding instant jumps

#### Scenario: EMA responds to sustained sound
- **WHEN** a sustained loud sound is present for 500ms
- **THEN** the smoothed amplitude converges to the actual amplitude within ~300ms

### Requirement: Configurable noise gate
The system SHALL implement a noise gate that suppresses amplitude values below a configurable threshold.

#### Scenario: Below threshold treated as silence
- **WHEN** raw amplitude is below the noise gate threshold (default 0.05)
- **THEN** the output amplitude is set to 0.0

#### Scenario: Above threshold passes through
- **WHEN** raw amplitude is above the noise gate threshold
- **THEN** the amplitude passes through with smoothing applied

### Requirement: Normalized output
The system SHALL normalize the processed amplitude to a 0.0–1.0 range suitable for mapping to Glyph brightness.

#### Scenario: Output always in valid range
- **WHEN** any audio input is processed
- **THEN** the output amplitude is between 0.0 and 1.0 inclusive
