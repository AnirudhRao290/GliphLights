## MODIFIED Requirements

### Requirement: Tap to toggle node
The system SHALL toggle a Glyph node's state when the user taps on it. If the node is OFF, it turns ON. If the node is ON, it turns OFF. The tap position SHALL be correctly mapped to node positions accounting for any active zoom or pan transform.

#### Scenario: Tap on inactive node
- **WHEN** user taps on a node that is currently OFF
- **THEN** the node state changes to ON

#### Scenario: Tap on active node
- **WHEN** user taps on a node that is currently ON
- **THEN** the node state changes to OFF

#### Scenario: Tap at zoomed level
- **WHEN** user zooms in 2x and taps on a node
- **THEN** the correct node is toggled despite the zoom transform

### Requirement: Drag painting across nodes
The system SHALL allow the user to drag across multiple nodes to paint them. Nodes encountered during drag SHALL be toggled ON. If drag enters a node that is already ON, it SHALL be toggled OFF. Drag gestures SHALL NOT cause the map to pan or zoom.

#### Scenario: Drag paints inactive nodes ON
- **WHEN** user drags from node A1 through A5 and all are OFF
- **THEN** nodes A1–A5 are all toggled ON

#### Scenario: Drag toggles active nodes OFF
- **WHEN** user drags over node A3 which is already ON
- **THEN** node A3 is toggled OFF

#### Scenario: Multiple nodes active simultaneously
- **WHEN** user paints across several nodes
- **THEN** all painted nodes remain active simultaneously until individually toggled off

#### Scenario: Single-finger drag does not pan
- **WHEN** user drags with one finger across the Glyph map
- **THEN** nodes are painted but the map does not translate

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
The system SHALL support pinch-to-zoom gestures on the Glyph map, allowing the user to zoom in and out of the node layout. Zoom SHALL use Compose's unified pointer input system without conflicting with tap/drag gestures.

#### Scenario: Pinch out to zoom in
- **WHEN** user performs a two-finger pinch-out gesture on the Glyph map
- **THEN** the map scales up, showing nodes larger

#### Scenario: Pinch in to zoom out
- **WHEN** user performs a two-finger pinch-in gesture on the Glyph map
- **THEN** the map scales down, showing nodes smaller

#### Scenario: Zoom does not trigger node toggle
- **WHEN** user pinch-zooms on the map
- **THEN** no nodes are toggled during the zoom gesture

### Requirement: Pan gesture
The system SHALL support pan via two-finger drag to move the Glyph map within the viewport. Single-finger drag SHALL NOT pan the map (it paints nodes instead).

#### Scenario: Two-finger pan moves the map
- **WHEN** user drags with two fingers on the Glyph map
- **THEN** the map translates in the drag direction

#### Scenario: Single-finger drag does not pan
- **WHEN** user drags with one finger on the Glyph map
- **THEN** the map does not translate (nodes are painted instead)

#### Scenario: Pan does not toggle nodes
- **WHEN** user pans the map with two fingers
- **THEN** no nodes are toggled during the pan gesture

### Requirement: 60 FPS rendering
The Glyph map and node state changes SHALL render at 60 FPS or better using Compose Canvas.

#### Scenario: Smooth animation
- **WHEN** the user interacts with the Glyph map
- **THEN** visual updates render at the device's native refresh rate (60 FPS minimum)
