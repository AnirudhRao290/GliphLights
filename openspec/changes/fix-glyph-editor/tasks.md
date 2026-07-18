## 1. Layout Sharing Fix

- [x] 1.1 Remove hardcoded 400x600 default layout from GlyphEditorViewModel
- [x] 1.2 Add onLayoutCreated callback to GlyphMapView
- [x] 1.3 Create layout in BoxWithConstraints and pass to ViewModel via callback
- [x] 1.4 Use ViewModel's shared layout in handleGestureEvent (remove local layout creation)

## 2. Gesture Handling Rewrite

- [x] 2.1 Remove transformable modifier from GlyphMapView
- [x] 2.2 Replace pointerInteropFilter with pointerInput using awaitPointerEventScope
- [x] 2.3 Implement two-finger zoom detection (distance change between pointers)
- [x] 2.4 Implement two-finger pan detection (centroid movement)
- [x] 2.5 Implement single-finger tap/drag for node painting
- [x] 2.6 Fix coordinate transformation: map touch coordinates through inverse of zoom/pan transform

## 3. GestureSampler Update

- [x] 3.1 Update GestureSampler to accept layout via constructor (already done, verify)
- [x] 3.2 Ensure processDown/processMove/processUp use the shared layout instance
- [x] 3.3 Verify hit-testing works at different zoom levels

## 4. Session Lifecycle Fix

- [x] 4.1 Add auto-start session in GlyphEditorScreen via LaunchedEffect
- [x] 4.2 Skip auto-start if session is already active (re-entering screen)
- [x] 4.3 Add session state check in GlyphSdkRenderer.render() — skip if inactive
- [x] 4.4 Add session state check in ViewModel.clearAll() — turnOff only if active
- [x] 4.5 Add session state check in ViewModel.sendToDevice() — render only if active

## 5. Performance Fix

- [x] 5.1 Verify layout is created only once (in BoxWithConstraints with remember)
- [x] 5.2 Ensure Animatable instances are remembered across recompositions
- [x] 5.3 Remove any unnecessary recompositions in GlyphMapView

## 6. Build Verification

- [x] 6.1 Run assembleDebug and verify no compilation errors
- [x] 6.2 Verify all editor screens compile correctly
