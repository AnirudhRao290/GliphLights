## ADDED Requirements

### Requirement: Settings screen accessible from dashboard
The app SHALL provide navigation to a Settings screen from the main dashboard.

#### Scenario: User navigates to settings
- **WHEN** user taps settings icon/button on dashboard
- **THEN** app navigates to the Settings screen

### Requirement: Default animate duration setting
The app SHALL allow users to configure the default animation duration (period) in milliseconds.

#### Scenario: User sets default period
- **WHEN** user adjusts the default period slider in settings
- **THEN** app persists the value and uses it as default for animate() calls

#### Scenario: Period persisted across sessions
- **WHEN** user closes and reopens the app
- **THEN** previously set period value is restored

### Requirement: Default cycles setting
The app SHALL allow users to configure the default number of animation cycles.

#### Scenario: User sets default cycles
- **WHEN** user adjusts the default cycles value in settings
- **THEN** app persists the value and uses it as default for animate() calls

### Requirement: Default interval setting
The app SHALL allow users to configure the default interval between animation cycles.

#### Scenario: User sets default interval
- **WHEN** user adjusts the default interval value in settings
- **THEN** app persists the value and uses it as default for animate() calls

### Requirement: Default zone preset setting
The app SHALL allow users to configure which zone is used by default for quick actions.

#### Scenario: User selects default zone
- **WHEN** user selects a default zone (All, A, B, or C) in settings
- **THEN** app persists the selection and uses it for dashboard quick actions

### Requirement: Startup behavior setting
The app SHALL allow users to configure what happens when the app starts.

#### Scenario: User sets startup behavior
- **WHEN** user selects startup behavior option
- **THEN** app persists the choice (e.g., "Do nothing", "Show last state")

### Requirement: Theme preference setting
The app SHALL allow users to configure the app theme.

#### Scenario: User selects dark theme
- **WHEN** user selects "Dark" theme option
- **THEN** app applies dark theme immediately

#### Scenario: User selects light theme
- **WHEN** user selects "Light" theme option
- **THEN** app applies light theme immediately

#### Scenario: User selects system theme
- **WHEN** user selects "System" theme option
- **THEN** app follows system-wide theme setting

### Requirement: Settings persisted via DataStore
All settings SHALL be stored using Android DataStore Preferences.

#### Scenario: Settings survive app restart
- **WHEN** user modifies any setting and restarts the app
- **THEN** all settings retain their previously configured values

#### Scenario: Settings survive device reboot
- **WHEN** device is rebooted
- **THEN** all app settings retain their previously configured values
