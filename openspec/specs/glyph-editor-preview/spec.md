## ADDED Requirements

### Requirement: Software preview renderer
The system SHALL render the Glyph node map and node states using Compose Canvas as a software preview, independent of the physical Glyph hardware.

#### Scenario: Preview shows all nodes
- **WHEN** the Glyph editor screen is displayed
- **THEN** all 36 nodes are rendered on the Canvas at their correct arc positions

#### Scenario: Preview shows node states
- **WHEN** a node is toggled ON
- **THEN** the preview renderer immediately shows the node in its active state

### Requirement: Node glow animation on toggle
The system SHALL animate a glow effect on nodes when they are toggled. Toggle ON SHALL animate glow intensity from 0 to 1 over 150ms. Toggle OFF SHALL animate glow intensity from 1 to 0 over 100ms.

#### Scenario: Glow on activation
- **WHEN** node A5 is toggled ON
- **THEN** a glow animation plays from transparent to opaque over 150ms

#### Scenario: Glow on deactivation
- **WHEN** node A5 is toggled OFF
- **THEN** the glow fades from opaque to transparent over 100ms

#### Scenario: Multiple nodes glow independently
- **WHEN** nodes A3 and A7 are both ON
- **THEN** each node has its own independent glow animation state

### Requirement: Node visual states
The system SHALL render nodes in distinct visual states: inactive (dim/off), active (glowing), and hit-target (highlighted during touch).

#### Scenario: Inactive node appearance
- **WHEN** a node is OFF
- **THEN** it is rendered as a dim circle with the region's color

#### Scenario: Active node appearance
- **WHEN** a node is ON
- **THEN** it is rendered as a bright, glowing circle with a glow effect

#### Scenario: Hit target feedback
- **WHEN** user's pointer enters a node's hit area during a gesture
- **THEN** the node shows a brief highlight indicating it is the current touch target

### Requirement: Region color coding
The system SHALL render nodes in each region with a distinct color to visually differentiate Region A, Region B, and Region C.

#### Scenario: Region A color
- **WHEN** Region A nodes are rendered
- **THEN** they use a consistent color distinct from Regions B and C

#### Scenario: Region B color
- **WHEN** Region B nodes are rendered
- **THEN** they use a consistent color distinct from Regions A and C

#### Scenario: Region C color
- **WHEN** Region C nodes are rendered
- **THEN** they use a consistent color distinct from Regions A and B
