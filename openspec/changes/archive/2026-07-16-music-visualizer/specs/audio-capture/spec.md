## ADDED Requirements

### Requirement: Audio capture with AudioRecord
The system SHALL capture microphone audio using Android's `AudioRecord` API with the following configuration: 44100 Hz sample rate, mono channel, PCM 16-bit encoding.

#### Scenario: Successful audio capture initialization
- **WHEN** the visualizer starts and RECORD_AUDIO permission is granted
- **THEN** an `AudioRecord` instance is created with 44100Hz, mono, PCM_16BIT and begins recording

#### Scenario: Audio capture on background thread
- **WHEN** audio capture is active
- **THEN** audio data is read on a background coroutine (Dispatchers.IO), never blocking the UI thread

### Requirement: Circular buffer for audio data
The system SHALL use a pre-allocated circular buffer (4096 samples) to store audio data from the microphone.

#### Scenario: Buffer wraps around without allocation
- **WHEN** more than 4096 samples are captured
- **THEN** new samples overwrite the oldest samples without allocating new memory

#### Scenario: Buffer provides recent audio window
- **WHEN** the processing loop reads from the buffer
- **THEN** it receives the most recent 4096 samples of audio data

### Requirement: Continuous recording
The system SHALL continuously record audio while the visualizer is active, reading audio data in a loop without gaps.

#### Scenario: Continuous data flow
- **WHEN** the visualizer is running
- **THEN** audio data is captured and processed continuously without interruption

#### Scenario: Recording stops when visualizer stops
- **WHEN** the user stops the visualizer or navigates away
- **THEN** the AudioRecord is released and audio capture ceases

### Requirement: Permission handling
The system SHALL request RECORD_AUDIO permission at runtime before starting audio capture, and handle denial gracefully.

#### Scenario: Permission granted
- **WHEN** user grants RECORD_AUDIO permission
- **THEN** audio capture begins immediately

#### Scenario: Permission denied
- **WHEN** user denies RECORD_AUDIO permission
- **THEN** the visualizer displays a message explaining why the permission is needed and does not start capture

#### Scenario: Permission previously denied
- **WHEN** user opens the visualizer with permission previously denied
- **THEN** the visualizer shows a "Permission required" state with a button to open app settings
