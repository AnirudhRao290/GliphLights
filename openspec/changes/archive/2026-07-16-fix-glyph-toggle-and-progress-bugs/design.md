## Context

The Glyph SDK's `toggle(GlyphFrame)` method does NOT toggle channels on/off — it sets the frame's channels ON and turns everything else OFF. This is confirmed by the SDK docs: "When toggling frame2, frame1 will be toggled off." The current implementation treats `toggle()` as a true toggle, causing:
- Toggle All only turns ON (never OFF)
- Individual channel clicks override all other active channels

Additionally, `displayProgress()` is SDK-limited to the C/D camera ring zone only.

## Goals / Non-Goals

**Goals:**
- Toggle All button turns lights ON when off, OFF when on
- Individual channel clicks are additive (A1+A2+A3 can all be on simultaneously)
- Progress simulation for Zone A and Zone B in addition to Zone C

**Non-Goals:**
- Changing the SDK's `toggle()` behavior (we don't control the SDK)
- True per-channel brightness control (not available in SDK)
- Progress accuracy beyond visual approximation on A/B zones

## Decisions

### 1. Toggle All: check activeChannels before calling SDK

**Decision**: If `activeChannels` contains all channels, call `glyphManager.turnOff()`. Otherwise, build a frame with all channels and call `glyphManager.toggle(frame)`.

**Rationale**: The SDK's `toggle()` always turns channels ON. To turn OFF, we must use `turnOff()`. Checking `activeChannels` tells us the current state.

**Alternative considered**: Call `toggle()` twice — wasteful and unreliable.

### 2. Additive channel toggle: build frame with all active channels

**Decision**: When toggling a single channel, build the frame with ALL currently active channels (plus the new one if turning on, minus if turning off).

**Rationale**: Since `toggle()` replaces all active channels with the frame's channels, we must include all channels we want to stay ON in the frame.

**Example**: Active = {A1, A2}. User clicks A3. Frame = {A1, A2, A3}. All three turn ON.
**Example**: Active = {A1, A2, A3}. User clicks A2. Frame = {A1, A3}. A2 turns OFF.

### 3. Zone progress: simulate by toggling channel subsets

**Decision**: For Zone A and Zone B, divide channels into segments and toggle progressively based on progress percentage.

**Rationale**: The SDK's `displayProgress()` only works for Zone C. For A/B, we simulate by turning ON the first N channels where N = progress% * channelCount / 100.

**Zone A** (11 channels): 0% = all off, 50% = first 5-6 on, 100% = all 11 on
**Zone B** (5 channels): 0% = all off, 50% = first 2-3 on, 100% = all 5 on

### 4. Track per-zone progress state

**Decision**: Store progress values per zone in the ViewModel (`Map<GlyphZone, Int>`) to support independent sliders.

**Rationale**: Each zone needs its own progress state for the UI sliders.

## Risks / Trade-offs

- **[Race condition on rapid clicks]** → Multiple rapid toggles could build frames with stale `activeChannels`. Mitigation: Use synchronized access or snapshot `activeChannels` at frame-build time.
- **[Progress simulation not smooth]** → Zone A/B progress steps in channel-sized increments (e.g., ~9% per channel). Mitigation: Acceptable for visual feedback; the SDK's native C-zone progress is smooth.
- **[turnOff() clears all zones]** → Turning off one zone via progress 0% would turn off all zones. Mitigation: Only call turnOff when ALL zones are at 0%, otherwise use additive toggle to set only the target zone's channels.
