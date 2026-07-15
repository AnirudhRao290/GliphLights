## Why

Nothing Phone (3a) Pro users lack a comprehensive, official-feeling app to control their Glyph Interface. The official Nothing Glyph SDK (com.nothing.ketchum) provides zone-level control over the 36 addressable LED channels across 3 zones (A, B, C) but has no official companion app for custom control. A native Android app using this SDK provides safe, reliable, and beautiful control over the phone's unique lighting system.

## What Changes

- Transform the blank GliphLights project into a Glyph Control app
- Add Jetpack Compose UI with Material 3, Nothing OS-inspired design
- Integrate the official Nothing Glyph SDK (com.nothing.ketchum) for lighting control
- Support all 3 glyph zones: Zone A (A1-A11, vertical bar), Zone B (B1-B5, horizontal bar), Zone C (C1-C20, camera ring)
- Implement toggle, animate, displayProgress, and turnOff SDK capabilities
- Implement Home Screen Widget for quick glyph toggling
- Add Quick Settings Tile for system-level access
- Create Settings screen with DataStore persistence
- Follow MVVM + Clean Architecture patterns
- Handle SDK unavailability, unsupported devices, and errors gracefully

## SDK Limitations (Important)

The Nothing Glyph SDK has significant limitations that shape this app's scope:

- **No brightness control** - LEDs are either ON or OFF per channel
- **No music visualization** - Not available in the SDK
- **No notification effects** - Not exposed by the SDK
- **No charging animation** - Not exposed by the SDK
- **No timer effect** - Not exposed by the SDK
- **Toggle only** - No explicit ON/OFF, only toggle state per frame
- **Foreground only** - SDK only works in foreground apps
- **Session required** - Must call openSession() before use, closeSession() when done
- **API key required** - Need Nothing API key (or "test" key for debug)
- **Android 14+ only** - SDK requires Nothing device running Android 14+

## Capabilities

### New Capabilities

- `glyph-dashboard`: Home screen with glyph status, device info, and action buttons (Toggle, Animate, Turn Off)
- `glyph-controls`: Zone-level control for all 36 channels across zones A/B/C with toggle and animate
- `glyph-widget`: Home Screen Widget with Toggle and live status
- `glyph-quick-settings`: Android Quick Settings Tile with dynamic icon and toggle
- `glyph-settings`: User preferences for animate duration, zone presets, and theme via DataStore
- `glyph-sdk-integration`: Repository and SDK abstraction layer wrapping com.nothing.ketchum
- `glyph-error-handling`: Graceful handling of SDK unavailability, unsupported devices, missing permissions, session errors

### Modified Capabilities

<!-- None - this is a fresh project with no existing specs -->

## Impact

- **App structure**: Complete rewrite of MainActivity, new package structure (ui/, viewmodel/, repository/, sdk/, widgets/, services/, models/, utils/)
- **Dependencies**: Add Nothing Glyph SDK (AAR), DataStore, Navigation Compose, Hilt (DI), Coroutines, Glance (widget)
- **Gradle**: Update build.gradle.kts with new dependencies, AAR repository configuration
- **AndroidManifest**: Add NothingKey meta-data, ketchum permission, widget receiver, quick settings tile service
- **Resources**: New layouts for widget, drawable assets for zone indicators, strings, themes
- **Testing**: Unit tests for ViewModels and repositories, UI tests for key screens
