## 1. Fix Toggle All Button (Dashboard)

- [x] 1.1 In `DashboardViewModel.toggleAll()`, check if `glyphState.isActive` — if active, call `glyphRepository.turnOff()`, otherwise call `glyphRepository.toggleAll()`
- [x] 1.2 In `GlyphRepositoryImpl.toggleAll()`, pass through to `glyphManagerWrapper.toggleChannels(allChannels)` (no change needed, logic fix is in ViewModel)

## 2. Fix Additive Channel Toggle (GlyphManagerWrapper)

- [x] 2.1 In `GlyphManagerWrapper.toggleChannels()`, determine which channels are being turned ON vs OFF by comparing with `activeChannels`
- [x] 2.2 If turning ON: build frame with `activeChannels + newChannels` and call `glyphManager.toggle(frame)`
- [x] 2.3 If turning OFF: build frame with `activeChannels - removedChannels` and call `glyphManager.toggle(frame)`
- [x] 2.4 If result is empty set (last channel turned off), call `glyphManager.turnOff()` instead
- [x] 2.5 Update `activeChannels` tracking after the SDK call

## 3. Fix Zone Toggle (ControlsViewModel)

- [x] 3.1 In `ControlsViewModel.toggleZone()`, implement additive logic: if zone is active, turn off only that zone's channels; if inactive, add zone's channels to active set
- [x] 3.2 Update `GlyphManagerWrapper` to expose a method that sets specific channels ON/OFF without affecting others

## 4. Add Progress for Zone A

- [x] 4.1 In `ControlsViewModel`, add `_progressZoneA` state (MutableStateFlow<Int>)
- [x] 4.2 Implement `updateProgressForZone(zone, progress)` that calculates channels to activate: `ceil(progress * zone.channelCount / 100)`
- [x] 4.3 Build a GlyphFrame with the calculated channels and call `toggle()` to set the zone's progress state

## 5. Add Progress for Zone B

- [x] 5.1 In `ControlsViewModel`, add `_progressZoneB` state (MutableStateFlow<Int>)
- [x] 5.2 Reuse `updateProgressForZone()` for Zone B with its 5 channels

## 6. Update Controls Screen UI

- [x] 6.1 In `ControlsScreen.kt`, add a progress slider for Zone A below the Zone A section
- [x] 6.2 In `ControlsScreen.kt`, add a progress slider for Zone B below the Zone B section
- [x] 6.3 Update `ProgressSection` to accept zone parameter and display appropriate label
- [x] 6.4 Add per-zone progress states to `ControlsUiState.Success`

## 7. Build & Verify

- [x] 7.1 Run `./gradlew assembleDebug` and verify successful build
- [ ] 7.2 Test Toggle All on Dashboard — lights SHALL turn off when already on
- [ ] 7.3 Test individual channel toggles — multiple channels SHALL stay on simultaneously
- [ ] 7.4 Test zone toggle — toggling Zone A SHALL not affect Zone B channels
- [ ] 7.5 Test progress sliders for all three zones
