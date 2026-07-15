## ADDED Requirements

### Requirement: Widget displays glyph status
The Home Screen Widget SHALL display whether glyph is currently active or inactive.

#### Scenario: Widget shows active state
- **WHEN** glyph channels are currently ON
- **THEN** widget displays active/ON indicator

#### Scenario: Widget shows inactive state
- **WHEN** no glyph channels are active
- **THEN** widget displays inactive/OFF indicator

### Requirement: Widget provides toggle button
The widget SHALL include a button to toggle all glyph channels.

#### Scenario: User taps widget toggle
- **WHEN** user taps the toggle button on the widget
- **THEN** widget triggers GlyphManager.toggle() with all channels and updates display

### Requirement: Widget provides turn off button
The widget SHALL include a button to turn off all glyphs.

#### Scenario: User taps widget turn off
- **WHEN** user taps the turn off button on the widget
- **THEN** widget calls GlyphManager.turnOff() and updates display

### Requirement: Widget updates immediately after action
The widget SHALL update its display immediately after any glyph state change.

#### Scenario: State change reflected
- **WHEN** any glyph state change occurs (from widget, app, or Quick Settings)
- **THEN** widget display updates within 1 second

### Requirement: Widget is resizable
The widget SHALL support standard Android widget resizing.

#### Scenario: User resizes widget
- **WHEN** user long-presses and resizes the widget
- **THEN** widget adjusts layout to fit new dimensions while maintaining toggle/turn off buttons
