## Context

GliphLights is a fresh Android project (empty "Hello Android" template) targeting Nothing Phone (3a) Pro. The project uses Kotlin, Jetpack Compose, and Material 3. The goal is to build a Glyph Control application that interfaces with the official Nothing Glyph SDK (`com.nothing.ketchum`) to control the phone's unique LED lighting system.

### Nothing Glyph SDK Reality

The SDK (`com.nothing.ketchum`) provides:

**Core APIs (GlyphManager):**
- `init(callback)` / `unInit()` - Service binding lifecycle
- `register()` / `register(targetDevice)` - App authorization
- `openSession()` / `closeSession()` - Session management (mandatory)
- `toggle(GlyphFrame)` - Toggle channels ON/OFF
- `animate(GlyphFrame)` - Breathing animation with period/cycles/interval
- `displayProgress(GlyphFrame, progress)` - Progress bar on C zone
- `displayProgressAndToggle(GlyphFrame, progress, reverse)` - Simultaneous toggle + progress
- `turnOff()` - Turn off all active glyphs

**GlyphFrame.Builder:**
- `buildChannel(int)` / `buildChannelA()` / `buildChannelB()` / `buildChannelC()` - Zone selection
- `buildPeriod(int)` - Animation duration in ms
- `buildCycles(int)` - Number of animation cycles
- `buildInterval(int)` - Interval between cycles

**Phone (3a) Pro Zones (is24111 / DEVICE_24111):**
- Zone A: A1-A11 (indices 20-30) - Vertical bar, left side
- Zone B: B1-B5 (indices 31-35) - Horizontal bar, top
- Zone C: C1-C20 (indices 0-19) - Camera ring (large circle)

**What the SDK does NOT provide:**
- No brightness control (ON/OFF only per channel)
- No music visualization
- No notification effects
- No charging animation
- No timer effects
- No explicit ON/OFF (only toggle)
- No individual LED control within zones

**Requirements:**
- Foreground app only
- API key required (or "test" for debug)
- Android 14+ on Nothing device
- Debug mode via adb (auto-disabled after 48h)

## Goals / Non-Goals

**Goals:**
- Provide a beautiful, Nothing OS-inspired interface for glyph control
- Implement all officially supported Glyph SDK capabilities (toggle, animate, progress, turnOff)
- Expose all 36 channels across 3 zones with individual and group control
- Support Home Screen Widget and Quick Settings Tile
- Persist user settings via DataStore
- Handle SDK unavailability, session errors, and unsupported devices gracefully
- Follow MVVM + Clean Architecture with SOLID principles
- Production-ready code with proper error handling and testing

**Non-Goals:**
- Root access or reverse engineering of the Glyph system
- Support for devices other than Nothing Phone (3a) Pro (though graceful fallback for other Nothing devices)
- Brightness control (SDK does not support it)
- Music visualization (SDK does not support it)
- Notification/charging/timer effects (SDK does not expose these)
- Cloud sync or account system
- In-app purchases or monetization

## Decisions

### 1. Architecture: MVVM + Clean Architecture

**Decision**: Use MVVM with Clean Architecture layers (ui → viewmodel → repository → sdk).

**Rationale**: Separation of concerns, testability, and maintainability. ViewModels handle UI state, Repositories abstract data sources, SDK layer wraps Nothing Glyph SDK.

**Alternatives considered**:
- MVI: More boilerplate for this use case, MVVM sufficient
- No architecture: Unmaintainable at this scale

### 2. Dependency Injection: Hilt

**Decision**: Use Hilt for dependency injection.

**Rationale**: Official Android DI solution, good Compose integration, reduces boilerplate for scoping and lifecycle management.

**Alternatives considered**:
- Manual DI: Too much boilerplate for multiple ViewModels and repositories
- Koin: Less type-safe, less official Android support

### 3. Navigation: Navigation Compose

**Decision**: Use Jetpack Navigation Compose for screen navigation.

**Rationale**: Official solution, deep linking support, type-safe arguments, good animation support.

### 4. Settings Persistence: DataStore

**Decision**: Use DataStore Preferences for settings storage.

**Rationale**: Modern replacement for SharedPreferences, coroutine-based, type-safe, handles conflicts gracefully.

### 5. Glyph SDK Abstraction

**Decision**: Create a `GlyphRepository` interface with `GlyphRepositoryImpl` that wraps the Nothing Glyph SDK.

**Rationale**: Enables testing with mocks, isolates SDK dependencies, allows graceful fallback when SDK unavailable. The repository manages the full SDK lifecycle: init → register → openSession → operations → closeSession → unInit.

### 6. SDK Session Management

**Decision**: Implement a state machine for SDK lifecycle: DISCONNECTED → INITIALIZING → CONNECTED → SESSION_ACTIVE → SESSION_CLOSED.

**Rationale**: The SDK requires strict session management (openSession before use, closeSession when done). A state machine ensures correct ordering and handles reconnection on lifecycle events.

### 7. Widget Implementation: Glance

**Decision**: Use Jetpack Glance for the Home Screen Widget.

**Rationale**: Compose-based widget API, consistent with app's UI framework, modern and maintained.

**Alternatives considered**:
- RemoteViews: More verbose, less Compose integration
- Custom widget: Too much boilerplate

### 8. Quick Settings Tile: TileService

**Decision**: Use Android TileService API.

**Rationale**: Official API for Quick Settings tiles, direct system integration, dynamic icon/state support.

### 9. Theming: Material 3 + Custom Nothing Theme

**Decision**: Extend Material 3 with custom Nothing-inspired theme (monochrome, rounded cards, specific typography).

**Rationale**: Leverages Material 3 foundation while achieving Nothing OS aesthetic.

### 10. Zone Control UI

**Decision**: Display all 3 zones (A, B, C) with individual channel toggles and zone-wide actions.

**Rationale**: The SDK supports zone-level shortcuts (buildChannelA/B/C) and individual channels. The UI should expose both for power users and quick actions.

## Risks / Trade-offs

- **[SDK Availability]** → Nothing Glyph SDK only works on Nothing devices with Android 14+. Mitigation: Graceful fallback with clear messaging, SDK check at startup.
- **[Toggle Only]** → SDK only supports toggle, not explicit ON/OFF. Mitigation: Track state in app, infer intent from current state.
- **[Session Lifecycle]** → SDK requires strict openSession/closeSession ordering. Mitigation: State machine in repository, auto-manage on lifecycle events.
- **[Foreground Only]** → SDK only works in foreground. Mitigation: Widget and Quick Settings tile must launch activity or use service binding carefully.
- **[API Key]** → SDK requires Nothing API key (or test key). Mitigation: Use "test" key for debug builds, document API key setup for release.
- **[Widget Updates]** → Widget may not update immediately on all launchers. Mitigation: Use AppWidgetManager.updateAppWidget() proactively.
- **[Battery Impact]** → Glyph animations consume battery. Mitigation: Auto-off timer setting, efficient animation handling.
- **[Device Fragmentation]** → Phone (3a) Pro zones differ from other Nothing models. Mitigation: Use is24111() check, graceful degradation for other models.
