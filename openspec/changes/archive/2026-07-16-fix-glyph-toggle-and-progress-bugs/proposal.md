## Why

The Glyph SDK integration is working but has critical usability bugs:
1. **Toggle All only turns lights ON** — calling Toggle All a second time does not turn them OFF because the SDK's `toggle()` method sets channels ON rather than toggling their state
2. **Individual channel buttons override each other** — clicking A1 then A2 turns OFF A1 because `toggle()` replaces all active channels with only the new frame's channels
3. **Progress only works for Zone C** — the SDK's `displayProgress()` is limited to the C/D camera ring, but users expect progress feedback on all zones

These bugs make the app frustrating to use — users cannot selectively control channels or toggle groups off.

## What Changes

- **Toggle logic fix**: When channels are already active, call `turnOff()` instead of `toggle()` to actually turn them off
- **Additive channel toggle**: When toggling individual channels, build frames that include ALL currently active channels (additive) rather than replacing them
- **Zone progress simulation**: Simulate progress on Zone A and Zone B by progressively toggling channels based on the progress percentage (e.g., 50% = first half of channels ON)

## Capabilities

### New Capabilities
- `additive-channel-toggle`: Individual channel toggles preserve other active channels instead of overriding them

### Modified Capabilities
- `glyph-sdk-integration`: Toggle behavior changes from replace-all to additive; Toggle All checks active state before calling SDK

## Impact

- `GlyphManagerWrapper.kt` — toggle logic rewrite, new additive frame building
- `ControlsScreen.kt` — progress sliders for Zone A and Zone B
- `ControlsViewModel.kt` — progress handlers for all zones
- `DashboardViewModel.kt` — toggleAll checks active state
