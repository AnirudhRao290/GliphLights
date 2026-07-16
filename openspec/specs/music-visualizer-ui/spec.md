### Requirement: Dedicated Music Visualizer screen
The app SHALL provide a dedicated Music Visualizer screen accessible from the main navigation.

#### Scenario: User navigates to visualizer
- **WHEN** user taps the Visualizer option in navigation
- **THEN** the Music Visualizer screen is displayed

### Requirement: Start and Stop controls
The app SHALL provide Start and Stop buttons to control the visualizer.

#### Scenario: User starts visualizer
- **WHEN** user taps the Start button
- **THEN** audio capture begins, signal processing starts, and Glyph commands are dispatched

#### Scenario: User stops visualizer
- **WHEN** user taps the Stop button
- **THEN** audio capture stops, all Glyph channels are turned off, and the UI shows idle state

### Requirement: Sensitivity slider
The app SHALL provide a sensitivity slider that adjusts the amplitude scaling factor.

#### Scenario: User increases sensitivity
- **WHEN** user moves sensitivity slider to maximum
- **THEN** audio amplitudes are scaled up, making the visualizer more responsive to quiet sounds

#### Scenario: User decreases sensitivity
- **WHEN** user moves sensitivity slider to minimum
- **THEN** audio amplitudes are scaled down, requiring louder sounds to produce Glyph activity

### Requirement: Noise gate slider
The app SHALL provide a noise gate slider that sets the minimum amplitude threshold.

#### Scenario: User adjusts noise gate
- **WHEN** user moves noise gate slider to 0.1
- **THEN** amplitudes below 0.1 are treated as silence

### Requirement: Visualization mode selector
The app SHALL provide a selector to choose between visualization modes (Pulse, Wave, Beat, Glow).

#### Scenario: User selects mode
- **WHEN** user taps a mode button
- **THEN** the visualizer switches to the selected mode and applies its rendering logic

#### Scenario: Current mode highlighted
- **WHEN** a mode is active
- **THEN** the mode button is visually highlighted in the UI

### Requirement: Real-time status indicators
The app SHALL display real-time status information including: current audio level, FPS, Glyph update rate, and latency.

#### Scenario: Audio level displayed
- **WHEN** the visualizer is running
- **THEN** a waveform meter or volume bar shows the current audio amplitude

#### Scenario: Performance metrics displayed
- **WHEN** the visualizer is running
- **THEN** FPS, update rate (commands/second), and processing latency are shown

### Requirement: Microphone permission status
The app SHALL display the current microphone permission status and prompt for permission if needed.

#### Scenario: Permission not yet granted
- **WHEN** user opens visualizer without RECORD_AUDIO permission
- **THEN** a permission request dialog is shown

#### Scenario: Permission status indicator
- **WHEN** the visualizer screen is displayed
- **THEN** a microphone icon or status text shows whether permission is granted

### Requirement: Debug overlay
The app SHALL provide an optional debug overlay showing raw microphone amplitude, filtered amplitude, RMS value, peak value, frame processing time, and Glyph update frequency.

#### Scenario: Debug mode enabled
- **WHEN** user enables debug mode in settings or via a hidden gesture
- **THEN** an overlay appears with real-time diagnostic information

#### Scenario: Debug mode disabled by default
- **WHEN** user opens the visualizer
- **THEN** the debug overlay is not shown unless previously enabled
