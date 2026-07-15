## 1. Project Setup & Gradle Configuration

- [x] 1.1 Update root build.gradle.kts with required plugins (Hilt, Navigation, KSP)
- [x] 1.2 Update app build.gradle.kts with dependencies (Glyph SDK AAR, DataStore, Navigation Compose, Hilt, Glance, Coroutines, Material3)
- [x] 1.3 Configure AAR repository for Nothing Glyph SDK in build.gradle.kts
- [x] 1.4 Add NothingKey meta-data and ketchum permission to AndroidManifest.xml
- [x] 1.5 Add version catalog entries for new dependencies in libs.versions.toml
- [x] 1.6 Configure Kotlin Compose compiler options

## 2. Package Structure & Models

- [x] 2.1 Create package structure: ui/, viewmodel/, repository/, sdk/, widgets/, services/, models/, utils/
- [x] 2.2 Create GlyphZone enum (A, B, C) with channel indices for Phone (3a) Pro
- [x] 2.3 Create data models: GlyphState, GlyphChannel, DeviceInfo, AppSettings, AnimationParams
- [x] 2.4 Create sealed classes for UI state: GlyphUiState, ControlsUiState, SettingsUiState, ErrorState
- [x] 2.5 Create result wrapper class for SDK operation results

## 3. SDK Integration Layer

- [x] 3.1 Create GlyphRepository interface with toggle, animate, displayProgress, turnOff, getStatus methods
- [x] 3.2 Create GlyphManager wrapper class handling init/unInit lifecycle
- [x] 3.3 Implement register() with device detection (Common.is24111() → DEVICE_24111)
- [x] 3.4 Implement openSession()/closeSession() lifecycle management
- [x] 3.5 Implement toggle(GlyphFrame) with frame building from zone/channel selection
- [x] 3.6 Implement animate(GlyphFrame) with period/cycles/interval parameters
- [x] 3.7 Implement displayProgress(GlyphFrame, progress) for C zone
- [x] 3.8 Implement turnOff() for immediate glyph deactivation
- [x] 3.9 Create GlyphRepositoryImpl wrapping GlyphManager with session state machine
- [x] 3.10 Handle GlyphException at every SDK call point

## 4. Dependency Injection & Data Layer

- [x] 4.1 Set up Hilt application class with @HiltAndroidApp annotation
- [x] 4.2 Create Hilt modules for GlyphRepository and SettingsRepository bindings
- [x] 4.3 Implement DataStore Preferences for settings persistence
- [x] 4.4 Create SettingsRepository interface and implementation
- [x] 4.5 Define settings keys: animatePeriod, animateCycles, animateInterval, defaultZone, startupBehavior, theme

## 5. Dashboard UI

- [x] 5.1 Create DashboardScreen composable with Nothing OS-inspired monochrome design
- [x] 5.2 Implement glyph status display (active/inactive indicator)
- [x] 5.3 Implement device info display (model name via Common.is24111())
- [x] 5.4 Implement active zones display (which of A/B/C are active)
- [x] 5.5 Create action buttons: Toggle All, Animate All, Turn Off
- [x] 5.6 Implement SDK connection state indicator
- [x] 5.7 Add smooth animations and transitions between states
- [x] 5.8 Implement dark/light theme support

## 6. Glyph Controls UI

- [x] 6.1 Create ControlsScreen composable with zone sections (A, B, C)
- [x] 6.2 Implement Zone A section with 11 channel toggles (A1-A11)
- [x] 6.3 Implement Zone B section with 5 channel toggles (B1-B5)
- [x] 6.4 Implement Zone C section with 20 channel toggles (C1-C20)
- [x] 6.5 Implement zone-wide toggle buttons for each zone
- [x] 6.6 Implement zone-wide animate buttons with period/cycles/interval inputs
- [x] 6.7 Implement progress slider for Zone C displayProgress
- [x] 6.8 Implement multi-channel selection across zones
- [x] 6.9 Implement Turn Off All button
- [x] 6.10 Add visual feedback for active channels/zones

## 7. ViewModels

- [x] 7.1 Create DashboardViewModel with glyph state management and SDK lifecycle
- [x] 7.2 Create ControlsViewModel with zone/channel toggle and animate logic
- [x] 7.3 Create SettingsViewModel with preference management
- [x] 7.4 Implement coroutine-based state flows in all ViewModels
- [x] 7.5 Add error handling and loading states to ViewModels
- [x] 7.6 Connect ViewModels to Repositories via Hilt injection

## 8. Navigation & Theme

- [x] 8.1 Set up Navigation Compose with route definitions (dashboard, controls, settings)
- [x] 8.2 Create NavHost with Dashboard, Controls, and Settings destinations
- [x] 8.3 Add bottom navigation bar for screen switching
- [x] 8.4 Implement custom Nothing-inspired Material 3 theme (monochrome, rounded cards)
- [x] 8.5 Define custom color palette (black/white/gray), typography, and shapes
- [x] 8.6 Apply theme throughout all screens

## 9. Home Screen Widget

- [x] 9.1 Create Glance widget composable with Toggle and Turn Off buttons
- [x] 9.2 Implement widget state management (active/inactive indicator)
- [x] 9.3 Create widget configuration XML (AppWidgetProviderInfo)
- [x] 9.4 Register widget receiver in AndroidManifest
- [x] 9.5 Implement immediate widget refresh after state changes
- [x] 9.6 Handle widget resizing and layout adaptation

## 10. Quick Settings Tile

- [x] 10.1 Create TileService subclass for Quick Settings
- [x] 10.2 Implement dynamic icon based on glyph active/inactive state
- [x] 10.3 Implement toggle logic on tile tap (toggle all channels)
- [x] 10.4 Register tile service in AndroidManifest with META-DATA
- [x] 10.5 Create tile icon assets (active/inactive vector drawables)
- [x] 10.6 Handle tile state updates from external changes

## 11. Settings Screen

- [x] 11.1 Create SettingsScreen composable with preference list
- [x] 11.2 Implement default animate period setting (slider, ms)
- [x] 11.3 Implement default cycles setting (number input)
- [x] 11.4 Implement default interval setting (slider, ms)
- [x] 11.5 Implement default zone preset setting (dropdown: All/A/B/C)
- [x] 11.6 Implement startup behavior setting (Do nothing / Show last state)
- [x] 11.7 Implement theme preference setting (Dark/Light/System)
- [x] 11.8 Persist all settings via DataStore and verify across restarts

## 12. Error Handling & Edge Cases

- [x] 12.1 Implement SDK unavailable error screen with device check message
- [x] 12.2 Implement unsupported device warning with available features list
- [x] 12.3 Implement permission request flow with rationale for ketchum permission
- [x] 12.4 Add try-catch around all GlyphManager calls catching GlyphException
- [x] 12.5 Create user-friendly error message strings for all error cases
- [x] 12.6 Implement retry mechanism for failed SDK operations (re-init, re-register)
- [x] 12.7 Handle app lifecycle: closeSession on background, re-open on foreground

## 13. AndroidManifest & Resources

- [x] 13.1 Add com.nothing.ketchum.permission.ENABLE permission
- [x] 13.2 Add NothingKey meta-data with test key for debug
- [x] 13.3 Register MainActivity, widget receiver, and tile service
- [x] 13.4 Add widget layout XML and AppWidgetProviderInfo XML
- [x] 13.5 Add Quick Settings tile icon resources (active/inactive vectors)
- [x] 13.6 Add string resources for all user-facing text
- [x] 13.7 Add drawable resources for zone indicators and status icons
- [x] 13.8 Configure app theme in styles.xml and themes.xml

## 14. Testing

- [x] 14.1 Write unit tests for GlyphRepositoryImpl with mocked GlyphManager
- [x] 14.2 Write unit tests for DashboardViewModel state management
- [x] 14.3 Write unit tests for ControlsViewModel zone/channel logic
- [x] 14.4 Write unit tests for SettingsViewModel preference handling
- [x] 14.5 Write unit tests for SettingsRepository DataStore operations
- [x] 14.6 Write UI tests for DashboardScreen rendering and button interactions
- [x] 14.7 Write UI tests for ControlsScreen zone toggles
- [x] 14.8 Write widget tests for Glance widget rendering and actions
- [x] 14.9 Verify all tests pass

## 15. Build Verification

- [ ] 15.1 Run full project build and fix any compilation errors
- [ ] 15.2 Run lint checks and fix warnings
- [ ] 15.3 Verify app installs and launches on Nothing Phone (3a) Pro
- [ ] 15.4 Test glyph toggle on all 3 zones (A, B, C)
- [ ] 15.5 Test animate with custom parameters
- [ ] 15.6 Test displayProgress on C zone
- [ ] 15.7 Test turnOff functionality
- [ ] 15.8 Test widget toggle and turn off on home screen
- [ ] 15.9 Test Quick Settings tile toggle
- [ ] 15.10 Test settings persistence across app restarts
- [ ] 15.11 Test SDK unavailable state on non-Nothing device
