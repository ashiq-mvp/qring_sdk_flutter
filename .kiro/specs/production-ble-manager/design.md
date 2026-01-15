# Design Document: Production-Grade BLE Connection Manager

## Overview

This design transforms the QRing Flutter plugin into a production-grade BLE connection manager that behaves like professional fitness band applications (Fitbit, Oura, etc.). The system will provide stable pairing, automatic reconnection, background reliability, and proper permission handling for Android devices.

The design builds upon the existing architecture while introducing a centralized state machine, enhanced error handling, and robust background service capabilities. The core principle is to make BLE connection management invisible to the Flutter layer - the native Android code handles all complexity while Flutter simply observes state changes.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    Flutter[Flutter App Layer]
    Bridge[Flutter Bridge / Plugin]
    BleConnectionManager[BLE Connection Manager<br/>State Machine]
    ForegroundService[Foreground Service]
    PermissionMgr[Permission Manager]
    NotificationMgr[Notification Manager]
    AutoReconnect[Auto-Reconnect Engine]
    GATT[Android GATT API]
    
    Flutter -->|Method Calls| Bridge
    Flutter <-.|Event Streams| Bridge
    Bridge --> BleConnectionManager
    Bridge --> ForegroundService
    BleConnectionManager --> PermissionMgr
    BleConnectionManager --> GATT
    BleConnectionManager --> AutoReconnect
    ForegroundService --> BleConnectionManager
    ForegroundService --> NotificationMgr
    AutoReconnect --> BleConnectionManager
    NotificationMgr --> ForegroundService
```

### Component Responsibilities

**BLE Connection Manager (New)**
- Single source of truth for all BLE state
- Implements state machine with explicit states
- Coordinates all BLE operations
- Manages pairing/bonding workflow
- Delegates to Auto-Reconnect Engine when needed

**Auto-Reconnect Engine (Enhanced)**
- Exponential backoff strategy
- Bluetooth state monitoring
- Doze mode awareness
- Persistent device MAC storage

**Foreground Service (Enhanced)**
- START_STICKY lifecycle
- Boot and Bluetooth-on restart
- Persistent notification with actions
- Wake lock management

**Permission Manager (Enhanced)**
- Android 12+ permission handling
- Runtime permission checking
- Permission error reporting

**Flutter Bridge (Modified)**
- Simplified API surface
- State observation only
- No direct BLE control from Flutter

## Components and Interfaces

### 1. BLE Connection Manager

The central component that replaces the current distributed BLE logic.

```java
public class BleConnectionManager {
    // State enum
    public enum BleState {
        IDLE,           // No operation in progress
        SCANNING,       // Actively scanning for devices
        CONNECTING,     // Connection attempt in progress
        PAIRING,        // Bonding/pairing in progress
        CONNECTED,      // Successfully connected and services discovered
        DISCONNECTED,   // Disconnected (not attempting reconnect)
        RECONNECTING,   // Auto-reconnect in progress
        ERROR           // Error state with details
    }
    
    // Core methods
    public void initialize(Context context);
    public void startScan(ScanCallback callback);
    public void stopScan();
    public void connect(String macAddress, ConnectionCallback callback);
    public void disconnect();
    public BleState getState();
    public void findRing(CommandCallback callback);
    public void getBattery(BatteryCallback callback);
    
    // State management
    private void transitionTo(BleState newState);
    private boolean canTransition(BleState from, BleState to);
    private void notifyStateChange(BleState state);
    
    // Pairing workflow
    private void checkBondState(BluetoothDevice device);
    private void initiateBonding(BluetoothDevice device);
    private void waitForBonding(BluetoothDevice device);
    
    // GATT management
    private void connectGatt(BluetoothDevice device);
    private void discoverServices();
    private void negotiateMtu();
    private void closeGatt();
}
```

**State Transitions:**
```
IDLE -> SCANNING -> IDLE
IDLE -> CONNECTING -> PAIRING -> CONNECTED
CONNECTED -> DISCONNECTED
CONNECTED -> RECONNECTING -> CONNECTED
CONNECTED -> ERROR
```

**State Validation:**
- Scan only allowed from IDLE
- Connect only allowed from IDLE or DISCONNECTED
- Disconnect allowed from any connected state
- Commands (findRing, getBattery) only allowed from CONNECTED

### 2. Permission Manager (Enhanced)

Handles all permission checking with Android 12+ support.

```java
public class PermissionManager {
    // Permission checking
    public boolean checkAllRequiredPermissions();
    public boolean checkBluetoothScanPermission();    // Android 12+
    public boolean checkBluetoothConnectPermission(); // Android 12+
    public boolean checkLocationPermission();         // Android < 12
    public boolean checkNotificationPermission();     // Android 13+
    public boolean checkForegroundServicePermission();
    
    // Permission reporting
    public List<String> getMissingPermissions();
    public Map<String, Boolean> getPermissionStatus();
    
    // Error handling
    public String getPermissionErrorMessage(String permission);
}
```

**Permission Matrix:**

| Android Version | Required Permissions |
|----------------|---------------------|
| < 12 (API < 31) | BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION |
| 12+ (API 31+) | BLUETOOTH_SCAN, BLUETOOTH_CONNECT |
| 13+ (API 33+) | POST_NOTIFICATIONS (additional) |
| All | FOREGROUND_SERVICE |

### 3. Pairing and Bonding Manager

Handles the reliable pairing workflow.

```java
public class PairingManager {
    // Pairing workflow
    public void startPairing(BluetoothDevice device, PairingCallback callback);
    public void checkBondState(BluetoothDevice device);
    public void retryPairing(BluetoothDevice device);
    
    // Bond state monitoring
    private BroadcastReceiver bondStateReceiver;
    private void registerBondStateReceiver();
    private void handleBondStateChange(int state);
    
    // Callbacks
    public interface PairingCallback {
        void onPairingSuccess(BluetoothDevice device);
        void onPairingFailed(String error);
        void onPairingRetry(int attemptNumber);
    }
}
```

**Pairing Workflow:**
1. Check current bond state
2. If BOND_NONE, call `createBond()`
3. Wait for BOND_BONDED broadcast (timeout: 30s)
4. If BOND_BONDING fails, retry once
5. If retry fails, report error
6. Once BOND_BONDED, proceed to GATT connection

### 4. GATT Connection Manager

Manages stable GATT connections with proper lifecycle.

```java
public class GattConnectionManager {
    // GATT operations
    public void connect(BluetoothDevice device, boolean autoConnect);
    public void disconnect();
    public void close();
    public void discoverServices();
    public void requestMtu(int mtu);
    
    // Lifecycle management
    private BluetoothGatt bluetoothGatt;
    private boolean isConnected;
    private boolean servicesDiscovered;
    
    // GATT callback
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // Handle connection state changes
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // Handle service discovery
        }
        
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            // Handle MTU negotiation
        }
    };
}
```

**GATT Strategy:**
- Use `autoConnect = true` for automatic OS-level reconnection
- Always call `discoverServices()` after connection
- Negotiate MTU to 512 bytes for optimal throughput
- Proper cleanup: `disconnect()` then `close()`

### 5. Auto-Reconnect Engine (Enhanced)

Implements exponential backoff with intelligent retry logic.

```java
public class AutoReconnectEngine {
    // Reconnection management
    public void startReconnection(String macAddress);
    public void stopReconnection();
    public boolean isReconnecting();
    public int getAttemptCount();
    
    // Backoff calculation
    private int calculateBackoffDelay(int attemptNumber);
    private void scheduleReconnect(int delayMs);
    
    // State monitoring
    private void registerBluetoothStateReceiver();
    private void handleBluetoothStateChange(int state);
    
    // Persistence
    private void saveLastDevice(String macAddress);
    private String loadLastDevice();
    private void clearLastDevice();
}
```

**Backoff Strategy:**
- Attempts 1-5: 10 seconds
- Attempts 6-10: 30 seconds
- Attempts 11+: 60 seconds, doubling each time (max 5 minutes)
- Add ±20% jitter to prevent thundering herd
- Pause during Bluetooth OFF
- Resume immediately when Bluetooth ON

**Persistence:**
- Store last connected MAC in SharedPreferences
- Load on service start for automatic reconnection
- Clear on manual disconnect

### 6. Foreground Service (Enhanced)

Runs as a foreground service with START_STICKY.

```java
public class QRingForegroundService extends Service {
    // Lifecycle
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle actions
        // Start foreground with notification
        return START_STICKY;
    }
    
    @Override
    public void onCreate() {
        // Initialize components
        // Register receivers
        // Restore state
    }
    
    @Override
    public void onDestroy() {
        // Cleanup
        // Save state
        // Release resources
    }
    
    // Boot receiver
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                // Restart service if device was connected
            }
        }
    }
    
    // Bluetooth receiver
    public static class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_ON) {
                    // Restart service if device was connected
                }
            }
        }
    }
}
```

**Service Configuration:**
- START_STICKY: System restarts service after kill
- Foreground notification: Required for Android 8+
- Boot receiver: Restart on device boot
- Bluetooth receiver: Restart when Bluetooth enabled
- Wake lock: Partial wake lock during active operations only

### 7. Notification Manager (Enhanced)

Manages persistent notification with dynamic content.

```java
public class ServiceNotificationManager {
    // Notification building
    public Notification buildConnectedNotification(String deviceName, int battery);
    public Notification buildDisconnectedNotification();
    public Notification buildReconnectingNotification(int attemptNumber);
    public Notification buildErrorNotification(String error);
    
    // Notification actions
    private PendingIntent createFindMyRingAction();
    private PendingIntent createOpenAppAction();
    
    // Notification updates
    public void updateNotification(Notification notification);
    public void updateBattery(int battery);
    public void updateConnectionState(String state);
}
```

**Notification Content:**
- Title: "QRing Connected" / "QRing Disconnected" / "QRing Reconnecting..."
- Text: Device name, battery level, connection state
- Actions: "Find My Ring" (always), "Open App" (tap notification)
- Icon: Dynamic based on connection state
- Priority: LOW (not intrusive)

### 8. Flutter Bridge (Simplified)

Simplified API that focuses on observation rather than control.

```dart
class QringSdkFlutter {
    // Connection methods
    static Future<void> connectRing(String macAddress);
    static Future<void> disconnectRing();
    static Future<ConnectionState> getConnectionState();
    
    // Device operations
    static Future<void> findMyRing();
    static Future<int> getBattery();
    static Future<DeviceInfo> getDeviceInfo();
    
    // Event streams
    static Stream<ConnectionState> get connectionStateStream;
    static Stream<BleError> get errorStream;
    static Stream<int> get batteryStream;
    
    // Permission checking
    static Future<Map<String, bool>> checkPermissions();
}
```

**Event Types:**
```dart
enum ConnectionState {
    disconnected,
    connecting,
    pairing,
    connected,
    reconnecting,
    error
}

class BleError {
    final String code;
    final String message;
    final ErrorType type;
}

enum ErrorType {
    permissionDenied,
    bluetoothOff,
    deviceNotFound,
    pairingFailed,
    connectionFailed,
    commandFailed
}
```

## Data Models

### Connection State Model

```java
public class ConnectionStateModel {
    private BleState state;
    private String deviceMac;
    private String deviceName;
    private int batteryLevel;
    private long lastConnectedTime;
    private int reconnectAttempts;
    private String errorMessage;
    
    // Getters and setters
    public Map<String, Object> toMap();
    public static ConnectionStateModel fromMap(Map<String, Object> map);
}
```

### Device Persistence Model

```java
public class DevicePersistenceModel {
    private String macAddress;
    private String deviceName;
    private long lastConnectedTime;
    private boolean autoReconnect;
    
    // SharedPreferences keys
    private static final String PREF_DEVICE_MAC = "device_mac";
    private static final String PREF_DEVICE_NAME = "device_name";
    private static final String PREF_LAST_CONNECTED = "last_connected_time";
    private static final String PREF_AUTO_RECONNECT = "auto_reconnect";
    
    // Persistence methods
    public void save(Context context);
    public static DevicePersistenceModel load(Context context);
    public static void clear(Context context);
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Valid State Enum
*For any* BLE_Manager instance at any point in time, its state should be exactly one of the valid enum values: IDLE, SCANNING, CONNECTING, CONNECTED, DISCONNECTED, RECONNECTING, PAIRING, or ERROR.

**Validates: Requirements 1.2**

### Property 2: State Validation Before Operations
*For any* BLE operation request (scan, connect, disconnect, command), the BLE_Manager should validate the current state before proceeding, rejecting operations that are invalid for the current state.

**Validates: Requirements 1.3, 1.5**

### Property 3: Observer Notification on State Change
*For any* state change in the BLE_Manager, all registered observers should receive a notification with the new state.

**Validates: Requirements 1.4**

### Property 4: Permission Check Before Scan (Android 12+)
*For any* scan operation on Android 12+, the BLE_Manager should verify BLUETOOTH_SCAN permission before proceeding, returning a permission error if not granted.

**Validates: Requirements 2.1**

### Property 5: Permission Check Before Connect (Android 12+)
*For any* connection operation on Android 12+, the BLE_Manager should verify BLUETOOTH_CONNECT permission before proceeding, returning a permission error if not granted.

**Validates: Requirements 2.2**

### Property 6: Location Permission Check (Android < 12)
*For any* BLE operation on Android versions below 12, the BLE_Manager should verify ACCESS_FINE_LOCATION permission before proceeding.

**Validates: Requirements 2.3**

### Property 7: Permission Error Reporting
*For any* BLE operation attempted without required permissions, the BLE_Manager should return a permission error to Flutter_Bridge identifying the missing permission.

**Validates: Requirements 2.6**

### Property 8: Bond State Check Before Connection
*For any* connection attempt to a QRing device, the BLE_Manager should check the Bond_State before proceeding with GATT connection.

**Validates: Requirements 3.1**

### Property 9: Bonding Trigger for Unbonded Devices
*For any* connection attempt where Bond_State is not BOND_BONDED, the BLE_Manager should trigger createBond before establishing GATT connection.

**Validates: Requirements 3.2**

### Property 10: GATT Connection After Bonding
*For any* bonding operation, the BLE_Manager should wait for BOND_BONDED state before establishing GATT connection.

**Validates: Requirements 3.3, 3.6**

### Property 11: Bonding Retry on Failure
*For any* bonding failure, the BLE_Manager should retry the bonding process exactly once before reporting error.

**Validates: Requirements 3.4**

### Property 12: AutoConnect Parameter
*For any* GATT connection establishment, the BLE_Manager should use autoConnect parameter set to true.

**Validates: Requirements 4.1**

### Property 13: Service Discovery Before Data Operations
*For any* GATT connection, the BLE_Manager should call discoverServices and wait for completion before allowing any data operations.

**Validates: Requirements 4.2**

### Property 14: MTU Negotiation After Service Discovery
*For any* successful service discovery, the BLE_Manager should negotiate MTU to optimize data transfer.

**Validates: Requirements 4.3**

### Property 15: Disconnect on Service Discovery Failure
*For any* service discovery failure, the BLE_Manager should disconnect and report error to Flutter_Bridge.

**Validates: Requirements 4.4**

### Property 16: GATT Resource Cleanup
*For any* disconnect operation, the BLE_Manager should properly clean up GATT resources including calling disconnect() and close().

**Validates: Requirements 4.5, 9.5**

### Property 17: Reconnecting State on Unexpected Disconnection
*For any* unexpected disconnection (not manual disconnect), the BLE_Manager should enter RECONNECTING state.

**Validates: Requirements 5.1**

### Property 18: Exponential Backoff Strategy
*For any* reconnection attempt number N, the calculated backoff delay should follow the exponential backoff strategy: 10s for attempts 1-5, 30s for 6-10, exponentially increasing for 11+ with ±20% jitter, capped at 5 minutes.

**Validates: Requirements 5.2**

### Property 19: Device MAC Persistence on Connection
*For any* successful connection, the BLE_Manager should persist the device MAC address to enable reconnection after app restart.

**Validates: Requirements 5.3, 12.1**

### Property 20: Auto-Reconnect Disabled on Manual Disconnect
*For any* manual disconnect operation, the BLE_Manager should disable Auto_Reconnect for that session.

**Validates: Requirements 5.4, 9.3**

### Property 21: Full GATT Setup on Reconnection
*For any* successful reconnection, the BLE_Manager should restore full GATT connection including service discovery and MTU negotiation.

**Validates: Requirements 5.5**

### Property 22: Reconnection Attempt Limit
*For any* reconnection sequence, the BLE_Manager should limit the maximum delay between attempts to prevent infinite loops (max 5 minutes between attempts).

**Validates: Requirements 5.6**

### Property 23: Boot Receiver Service Restart
*For any* device boot event when a QRing was previously connected, the Foreground_Service should restart automatically.

**Validates: Requirements 6.3**

### Property 24: Bluetooth ON Service Restart
*For any* Bluetooth state change to ON when a QRing was previously connected, the Foreground_Service should restart automatically.

**Validates: Requirements 6.4**

### Property 25: Persistent Notification While Service Running
*For any* time the Foreground_Service is running, a persistent notification should be displayed.

**Validates: Requirements 7.1**

### Property 26: Notification Contains Device Name
*For any* notification displayed by the Foreground_Service, it should contain the QRing device name.

**Validates: Requirements 7.2**

### Property 27: Notification Contains Connection State
*For any* notification displayed by the Foreground_Service, it should contain the current connection state (Connected, Connecting, Disconnected, Reconnecting).

**Validates: Requirements 7.3**

### Property 28: Notification Contains Battery When Available
*For any* notification when battery level is available, it should display the battery percentage.

**Validates: Requirements 7.4**

### Property 29: Find My Ring on Notification Tap
*For any* tap on the notification, the BLE_Manager should trigger the Find My Ring feature.

**Validates: Requirements 7.5, 10.5**

### Property 30: Notification Updates on State Change
*For any* connection state or battery level change, the notification should update to reflect the new information.

**Validates: Requirements 7.6**

### Property 31: Connected Event Emission
*For any* successful connection establishment, the Flutter_Bridge should emit an onBleConnected event.

**Validates: Requirements 8.5**

### Property 32: Disconnected Event Emission
*For any* disconnection, the Flutter_Bridge should emit an onBleDisconnected event.

**Validates: Requirements 8.6**

### Property 33: Reconnecting Event Emission
*For any* reconnection attempt, the Flutter_Bridge should emit an onBleReconnecting event.

**Validates: Requirements 8.7**

### Property 34: Battery Updated Event Emission
*For any* battery level change, the Flutter_Bridge should emit an onBatteryUpdated event.

**Validates: Requirements 8.8**

### Property 35: Error Event Emission
*For any* error occurrence, the Flutter_Bridge should emit an onBleError event with error details.

**Validates: Requirements 8.9**

### Property 36: Disconnect Then Close Sequence
*For any* disconnect operation, the BLE_Manager should call bluetoothGatt.disconnect() followed by bluetoothGatt.close() in that order.

**Validates: Requirements 9.1, 9.2**

### Property 37: Disconnected State After Manual Disconnect
*For any* manual disconnect completion, the BLE_Manager should transition to DISCONNECTED state.

**Validates: Requirements 9.4**

### Property 38: Bluetooth Toggle Reconnection
*For any* Bluetooth toggle cycle (OFF then ON), the BLE_Manager should automatically reconnect to the QRing.

**Validates: Requirements 10.2**

### Property 39: Out-of-Range Reconnection
*For any* out-of-range then in-range cycle, the BLE_Manager should automatically reconnect.

**Validates: Requirements 10.3**

### Property 40: Graceful Permission Revocation Handling
*For any* permission revocation while connected, the BLE_Manager should handle gracefully and report error.

**Validates: Requirements 10.4**

### Property 41: Error State on Operation Failure
*For any* BLE operation failure, the BLE_Manager should transition to ERROR state with error details.

**Validates: Requirements 11.1**

### Property 42: Specific Permission Error Reporting
*For any* permission denial, the BLE_Manager should report a specific permission error to Flutter_Bridge identifying which permission was denied.

**Validates: Requirements 11.2**

### Property 43: Pairing Error with Reason
*For any* pairing failure, the BLE_Manager should report a pairing error with failure reason.

**Validates: Requirements 11.3**

### Property 44: GATT Error with Operation Details
*For any* GATT operation failure, the BLE_Manager should report a GATT error with operation details.

**Validates: Requirements 11.4**

### Property 45: Retry Allowed After Error Acknowledgment
*For any* ERROR state, the BLE_Manager should allow retry operations after error is acknowledged.

**Validates: Requirements 11.5**

### Property 46: Device Name Persistence on Connection
*For any* successful connection, the BLE_Manager should persist the device name.

**Validates: Requirements 12.2**

### Property 47: Device Info Loading on App Restart
*For any* app restart with saved device information, the BLE_Manager should load the last connected device information.

**Validates: Requirements 12.3**

### Property 48: Clear Persisted Info on Manual Disconnect
*For any* manual disconnect, the BLE_Manager should clear the persisted device information.

**Validates: Requirements 12.4**

## Error Handling

### Error Categories

**Permission Errors**
- Code: `PERMISSION_DENIED`
- Message: "Missing permission: [PERMISSION_NAME]"
- Recovery: Prompt user to grant permission
- Reporting: Emit to Flutter via errorStream

**Bluetooth Errors**
- Code: `BLUETOOTH_OFF`
- Message: "Bluetooth is disabled"
- Recovery: Prompt user to enable Bluetooth
- Reporting: Emit to Flutter via errorStream

**Pairing Errors**
- Code: `PAIRING_FAILED`
- Message: "Failed to pair with device"
- Recovery: Retry once, then report to user
- Reporting: Emit to Flutter via errorStream

**Connection Errors**
- Code: `CONNECTION_FAILED`
- Message: "Failed to connect to device"
- Recovery: Enter auto-reconnect mode
- Reporting: Emit to Flutter via errorStream

**GATT Errors**
- Code: `GATT_ERROR`
- Message: "GATT operation failed: [details]"
- Recovery: Disconnect and reconnect
- Reporting: Emit to Flutter via errorStream

**Command Errors**
- Code: `COMMAND_FAILED`
- Message: "Command execution failed: [command]"
- Recovery: Retry command or report to user
- Reporting: Return error in method result

### Error Recovery Strategies

**Transient Errors** (network issues, temporary disconnections)
- Strategy: Auto-reconnect with exponential backoff
- Max attempts: Unlimited (with increasing delays)
- User notification: Show reconnecting status

**Permanent Errors** (permission denied, Bluetooth unavailable)
- Strategy: Stop operations, report to user
- Max attempts: 0 (no retry)
- User notification: Show error with action button

**Recoverable Errors** (pairing failed, GATT error)
- Strategy: Retry with limited attempts
- Max attempts: 1-3 depending on error type
- User notification: Show error after max attempts

### Error Logging

All errors should be logged with:
- Timestamp
- Error code
- Error message
- Stack trace (if exception)
- Current BLE state
- Device MAC (if applicable)

## Testing Strategy

### Unit Testing

**BLE Connection Manager Tests**
- State transition validation
- Permission checking before operations
- Pairing workflow execution
- GATT lifecycle management
- Error handling and recovery

**Auto-Reconnect Engine Tests**
- Backoff delay calculation
- Bluetooth state monitoring
- Device persistence
- Reconnection scheduling

**Permission Manager Tests**
- Android version-specific permission checking
- Missing permission detection
- Permission status reporting

**Notification Manager Tests**
- Notification content generation
- Action intent creation
- Notification updates

### Property-Based Testing

Each correctness property will be implemented as a property-based test using a suitable Android testing framework (e.g., junit-quickcheck or custom generators).

**Test Configuration:**
- Minimum 100 iterations per property test
- Random input generation for device MACs, states, and timing
- Shrinking enabled for failure case minimization

**Property Test Examples:**

```java
@Property
public void stateTransitionValidity(@ForAll BleState currentState, 
                                   @ForAll BleState requestedState) {
    BleConnectionManager manager = new BleConnectionManager();
    manager.setState(currentState);
    
    boolean shouldAllow = isValidTransition(currentState, requestedState);
    boolean actuallyAllowed = manager.canTransitionTo(requestedState);
    
    assertEquals(shouldAllow, actuallyAllowed);
}

@Property
public void permissionEnforcement(@ForAll BleOperation operation,
                                  @ForAll Set<String> grantedPermissions) {
    PermissionManager permManager = new PermissionManager();
    permManager.setGrantedPermissions(grantedPermissions);
    
    Set<String> required = operation.getRequiredPermissions();
    boolean hasAll = grantedPermissions.containsAll(required);
    
    boolean allowed = permManager.checkPermissionsFor(operation);
    
    assertEquals(hasAll, allowed);
}

@Property
public void exponentialBackoffTiming(@ForAll @IntRange(min = 1, max = 100) int attemptNumber) {
    AutoReconnectEngine engine = new AutoReconnectEngine();
    
    int delay = engine.calculateBackoffDelay(attemptNumber);
    
    // Verify delay follows exponential backoff rules
    if (attemptNumber <= 5) {
        assertTrue(delay >= 8000 && delay <= 12000); // 10s ± 20%
    } else if (attemptNumber <= 10) {
        assertTrue(delay >= 24000 && delay <= 36000); // 30s ± 20%
    } else {
        int baseDelay = 60000 * (1 << (attemptNumber - 11));
        int maxDelay = Math.min(baseDelay, 300000);
        assertTrue(delay >= maxDelay * 0.8 && delay <= maxDelay * 1.2);
    }
}
```

### Integration Testing

**End-to-End Scenarios:**
1. App killed → ring stays connected
2. Bluetooth toggle → automatic reconnection
3. Out-of-range → reconnection when back in range
4. Permission revoked → graceful error handling
5. Notification action → find my ring works
6. Device reboot → service restarts and reconnects

**Test Environment:**
- Physical Android device (API 31+)
- QRing device for actual BLE testing
- Automated test scripts for scenario execution

### Manual Testing Checklist

- [ ] Connect to ring, kill app, verify connection maintained
- [ ] Turn Bluetooth off/on, verify automatic reconnection
- [ ] Walk out of range, return, verify reconnection
- [ ] Revoke Bluetooth permission, verify error handling
- [ ] Tap "Find My Ring" in notification, verify ring vibrates
- [ ] Reboot device, verify service restarts
- [ ] Manual disconnect, verify auto-reconnect disabled
- [ ] Low battery notification accuracy
- [ ] Multiple reconnection attempts with correct delays
- [ ] Service survives low memory conditions

## Implementation Notes

### Android Manifest Changes

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:minSdkVersion="31" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" android:minSdkVersion="33" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Service -->
<service
    android:name=".QRingForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="connectedDevice" />

<!-- Receivers -->
<receiver
    android:name=".QRingForegroundService$BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

<receiver
    android:name=".QRingForegroundService$BluetoothReceiver"
    android:enabled="true"
    android:exported="false">
    <intent-filter>
        <action android:name="android.bluetooth.adapter.action.STATE_CHANGED" />
    </intent-filter>
</receiver>
```

### Migration Strategy

**Phase 1: Create New Components**
- Implement BleConnectionManager with state machine
- Implement enhanced PermissionManager
- Implement PairingManager
- Implement GattConnectionManager

**Phase 2: Enhance Existing Components**
- Enhance AutoReconnectEngine with exponential backoff
- Enhance QRingForegroundService with START_STICKY
- Enhance ServiceNotificationManager with dynamic content

**Phase 3: Integrate and Test**
- Wire new components together
- Update Flutter bridge
- Run integration tests
- Fix issues

**Phase 4: Deprecate Old Code**
- Remove old BleManager
- Remove old connection logic
- Clean up unused code

### Performance Considerations

**Battery Optimization:**
- Use partial wake locks only during active operations
- Release wake locks immediately after operations complete
- Increase reconnection delays during Doze mode
- Use autoConnect=true for OS-level reconnection

**Memory Optimization:**
- Limit stored device list size
- Clear old scan results
- Release GATT resources promptly
- Handle low memory warnings

**Network Optimization:**
- Negotiate MTU to 512 bytes
- Batch commands when possible
- Use connection intervals appropriately
- Minimize unnecessary GATT operations

## Security Considerations

**Permission Security:**
- Request minimum required permissions
- Explain permission usage to users
- Handle permission denial gracefully
- Never store sensitive data without encryption

**Bluetooth Security:**
- Require bonding for all connections
- Validate device MAC addresses
- Implement connection timeout
- Handle malicious devices gracefully

**Data Security:**
- Encrypt persisted device MAC
- Clear sensitive data on logout
- Validate all command responses
- Implement command timeout

## Future Enhancements

**iOS Support:**
- Implement CoreBluetooth equivalent
- Handle iOS background limitations
- Implement iOS-specific permission handling

**Multi-Device Support:**
- Support multiple simultaneous connections
- Implement device priority system
- Handle device switching

**Advanced Features:**
- Implement connection quality monitoring
- Add connection statistics
- Implement predictive reconnection
- Add mesh network support (if applicable)

**Analytics:**
- Track connection success rate
- Monitor reconnection patterns
- Measure battery impact
- Identify common failure modes
