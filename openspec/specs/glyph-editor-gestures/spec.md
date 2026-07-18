## ADDED Requirements

### Requirement: Tap to toggle node
The system SHALL toggle a Glyph node's state when the user taps on it. If the node is OFF, it turns ON. If the node is ON, it turns OFF.

#### Scenario: Tap on inactive node
- **WHEN** user taps on a node that is currently OFF
- **THEN** the node state changes to ON

#### Scenario: Tap on active node
- **WHEN** user taps on a node that is currently ON
- **THEN** the node state changes to OFF

### Requirement: Drag painting across nodes
The system SHALL allow the user to drag across multiple nodes to paint them. Nodes encountered during drag SHALL be toggled ON. If drag enters a node that is already ON, it SHALL be toggled OFF.

#### Scenario: Drag paints inactive nodes ON
- **WHEN** user drags from node A1 through A5 and all are OFF
- **THEN** nodes A1–A5 are all toggled ON

#### Scenario: Drag toggles active nodes OFF
- **WHEN** user drags over node A3 which is already ON
- **THEN** node A3 is toggled OFF

#### Scenario: Multiple nodes active simultaneously
- **WHEN** user paints across several nodes
- **THEN** all painted nodes remain active simultaneously until individually toggled off

### Requirement: Gesture deduplication
The system SHALL not re-toggle a node if the user's pointer is still over the same node during a drag gesture. The system SHALL track the last-hit node per pointer and only toggle a node when the pointer enters it for the first time during a drag.

#### Scenario: No duplicate toggle on same node
- **WHEN** user drags slowly over node A5 and the pointer remains over A5 for multiple touch events
- **THEN** node A5 is toggled only once (on first entry)

#### Scenario: Re-entry toggles again
- **WHEN** user drags away from A5 and then back over A5 during the same gesture
- **THEN** node A5 is toggled again (OFF if it was ON, ON if it was OFF)

### Requirement: Haptic feedback on node entry
The system SHALL provide haptic feedback when the user's pointer first enters a new node during a drag gesture.

#### Scenario: Haptic on first entry
- **WHEN** user drags from node A1 to A2
- **THEN** haptic feedback is triggered once when A2 is first entered

#### Scenario: No haptic on re-entry within same drag
- **WHEN** user's pointer moves slightly but remains within the same node's hit area
- **THEN** no additional haptic feedback is triggered

### Requirement: Pinch-to-zoom
The system SHALL support pinch-to-zoom gestures on the Glyph map, allowing the user to zoom in and out of the node layout.

#### Scenario: Pinch out to zoom in
- **WHEN** user performs a pinch-out gesture on the Glyph map
- **THEN** the map scales up, showing nodes larger

#### Scenario: Pinch in to zoom out
- **WHEN** user performs a pinch-in gesture on the Glyph map
- **THEN** the map scales down, showing nodes smaller

### Requirement: Pan gesture
The system SHALL support pan (two-finger drag or single-finger drag when not over a node) to move the Glyph map within the viewport.

#### Scenario: Pan moves the map
- **WHEN** user drags with two fingers on the Glyph map
- **THEN** the map translates in the drag direction

#### Scenario: Pan does not toggle nodes
- **WHEN** user pans the map
- **THEN** no nodes are toggled during the pan gesture

### Requirement: 60 FPS rendering
The Glyph map and node state changes SHALL render at 60 FPS or better using Compose Canvas.

#### Scenario: Smooth animation
- **WHEN** the user interacts with the Glyph map
- **THEN** visual updates render at the device's native refresh rate (60 FPS minimum)
