# Design Document: UI Loading States Fix

## Overview

This design addresses three critical UX issues in the QRing SDK Flutter example app:
1. Connect button provides no visual feedback when tapped
2. Find My Ring feature shows infinite loading
3. Device Info display shows infinite loading

The solution involves proper state management for async operations and leveraging the SDK's connection state stream to provide accurate feedback.

## Architecture

The fix will be implemented entirely in the UI layer (Flutter widgets) without requiring changes to the SDK itself. We'll use Flutter's built-in state management (setState) to track loading states for individual operations.

### Key Components

1. **DeviceScanningScreen**: Manages device discovery and connection initiation
2. **QuickActionsScreen**: Manages Find My Ring and Device Info features
3. **Connection State Stream**: SDK-provided stream for connection state updates

## Components and Interfaces

### DeviceScanningScreen State Management

**New State Variables:**
```dart
String? _connectingDeviceMac; // Track which device is currently connecting
```

**Modified Methods:**
- `_connectToDevice(String macAddress)`: Add loading state management
- `_buildDeviceList()`: Update Connect button to show loading state

### QuickActionsScreen State Management

**Existing State Variables (to be used correctly):**
- `_isFindingRing`: Already exists, needs timeout logic
- `_isLoadingDeviceInfo`: Already exists, needs proper error handling

**Modified Methods:**
- `_findRing()`: Add automatic timeout after command completes
- `_refreshDeviceInfo()`: Ensure loading state is cleared on all code paths

## Data Models

No new data models required. We'll use existing models:
- `qring.ConnectionState` (enum): disconnected, connecting, connected, disconnecting
- `qring.QringDeviceInfo`: Device information model

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Connect Button State Consistency
*For any* device in the device list, when a connection is initiated, the Connect button for that device should show a loading state and be disabled until the connection completes (success or failure).

**Validates: Requirements 1.1, 1.2, 1.3, 1.4**

### Property 2: Single Active Connection
*For any* set of discovered devices, at most one device should be in the "connecting" state at any given time, and all other Connect buttons should be disabled during this time.

**Validates: Requirements 4.1, 4.2**

### Property 3: Find Ring Loading Timeout
*For any* Find My Ring operation, the loading indicator should be visible for at least 500ms and at most 2 seconds, regardless of when the SDK call completes.

**Validates: Requirements 2.1, 2.2**

### Property 4: Device Info Loading Termination
*For any* device info fetch operation, the loading state should terminate within a reasonable timeout (10 seconds) even if the SDK call hangs, and an error should be displayed.

**Validates: Requirements 3.1, 3.2, 3.3**

### Property 5: Loading State Cleanup
*For any* async operation (connect, find ring, fetch device info), if the widget is disposed before the operation completes, the loading state should not cause setState to be called on an unmounted widget.

**Validates: Requirements 1.3, 2.2, 3.2**

## Error Handling

### Connection Errors
- **Timeout**: If connection doesn't complete within 30 seconds, show error and reset state
- **SDK Error**: Display error message from SDK exception
- **Widget Disposed**: Check `mounted` before calling setState

### Find Ring Errors
- **SDK Error**: Display error message, clear loading state
- **Timeout**: Automatically clear loading after 2 seconds regardless of SDK response

### Device Info Errors
- **SDK Error**: Display error message, clear loading state
- **Timeout**: Show "Request timed out" after 10 seconds
- **Empty Response**: Handle null/empty device info gracefully

## Testing Strategy

### Unit Tests
- Test that Connect button state changes correctly when connection is initiated
- Test that only one device can be in connecting state at a time
- Test that Find Ring loading clears after timeout
- Test that Device Info loading clears on error
- Test that mounted checks prevent setState on disposed widgets

### Property-Based Tests
- Property tests will use the `test` package with custom generators
- Each test will run a minimum of 100 iterations
- Tests will simulate various timing scenarios and state transitions

### Integration Tests
- Test full connection flow from scan to connect to connected state
- Test Find Ring feature with actual device
- Test Device Info fetch with actual device
- Test error scenarios (no device, connection failure, etc.)

## Implementation Notes

### Connect Button Loading
The Connect button will track the connecting device's MAC address. When a connection is initiated:
1. Store the MAC address in `_connectingDeviceMac`
2. Listen to connection state stream
3. When state changes to `connected` or `disconnected`, clear `_connectingDeviceMac`
4. Button shows loading spinner and "Connecting..." text while MAC matches

### Find Ring Timeout
The Find Ring feature will use a combination of SDK response and timeout:
1. Set `_isFindingRing = true`
2. Call SDK's `findRing()`
3. Use `Future.delayed` to ensure minimum 500ms loading time
4. Automatically clear loading after 2 seconds maximum
5. Show success message for 3 seconds

### Device Info Retry Logic
Device Info will implement proper error handling:
1. Set `_isLoadingDeviceInfo = true`
2. Wrap SDK call in try-catch
3. Always clear loading state in finally block
4. Add timeout using `Future.timeout(Duration(seconds: 10))`

### Mounted Checks
All async operations will check `if (mounted)` before calling setState to prevent errors when the widget is disposed during an async operation.
