## ADDED Requirements

### Requirement: Glyph node layout model
The system SHALL define a node layout model for the Nothing Phone (3a) Pro Glyph Interface consisting of 36 addressable nodes across 3 arc regions. Each node SHALL have a unique id, region identifier, SDK channel index, screen position, and state.

#### Scenario: Node count matches hardware
- **WHEN** the node layout is initialized
- **THEN** exactly 36 nodes are created: 11 in Region A, 5 in Region B, 20 in Region C

#### Scenario: SDK channel mapping
- **WHEN** a node is referenced by its region and local index
- **THEN** it maps to the correct SDK array index (A: 20–30, B: 31–35, C: 0–19)

### Requirement: Arc-based node positioning
The system SHALL position nodes along parametric circular arcs matching the physical Glyph layout. Region A nodes SHALL form a vertical arc on the right side. Region B nodes SHALL form a diagonal arc on the bottom-left. Region C nodes SHALL form a large arc around the camera module on the top-left.

#### Scenario: Region A vertical arc
- **WHEN** Region A nodes are rendered
- **THEN** nodes A1–A11 are positioned along a vertical arc from top to bottom on the right side of the Glyph map

#### Scenario: Region B diagonal arc
- **WHEN** Region B nodes are rendered
- **THEN** nodes B1–B5 are positioned along a diagonal arc from bottom-right to top-left

#### Scenario: Region C camera ring arc
- **WHEN** Region C nodes are rendered
- **THEN** nodes C1–C20 are positioned along a large arc around the camera lens area, from bottom-left to top-right

### Requirement: Node adjacency graph
The system SHALL model each region as a linear chain graph where each node knows its adjacent neighbors. Region A: A1↔A2↔...↔A11. Region B: B1↔B2↔...↔B5. Region C: C1↔C2↔...↔C20.

#### Scenario: Adjacent node query
- **WHEN** node A5 is queried for neighbors
- **THEN** the system returns A4 and A6 as adjacent nodes

#### Scenario: Endpoint nodes have single neighbor
- **WHEN** node A1 is queried for neighbors
- **THEN** the system returns only A2 as an adjacent node

### Requirement: Responsive node layout
The system SHALL compute node positions relative to the viewport size, maintaining correct arc proportions regardless of screen size or density.

#### Scenario: Different screen sizes
- **WHEN** the Glyph map is rendered on screens of different sizes
- **THEN** node positions scale proportionally and remain visually correct
