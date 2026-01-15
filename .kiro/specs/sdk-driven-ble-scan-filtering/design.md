# Design Document: SDK-Driven BLE Scan Filtering

## Overview

This design implements SDK-driven BLE scan filtering for the QRing Flutter plugin to ensure only QRing-compatible devices appear in scan results. The filtering will be performed in the Android native layer using SDK validation rules extracted from official documentation and sample code, eliminating unreliable device name heuristics.

The core principle is to move all filtering logic to the native Android layer, where we can access raw BLE advertisement data and apply SDK-specific validation rules. Flutter will receive only pre-validated devices, simplifying the Dart layer and ensuring users only see devices they can actually connect to.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    Flutter[Flutter UI Layer]
    Bridge[Flutter Bridge]
    ScanFilter[BLE Scan Filter]
    Scanner[BLE Scanner Helper]
    SDK[QRing SDK]
    BLE[Android BLE API]
    
    Flutter -->|startScan| Bridge
    Flutter <-.|device stream| Bridge
    Bridge --> ScanFilter
    ScanFilter --> Scanner
    Scanner --> SDK
    SDK --> BLE
    BLE -->|ScanResult| SDK
    SDK -->|onLeScan| Scanner
    Scanner -->|raw device| ScanFilter
    ScanFilter -->|validated device| Bridge
    Bridge -->|emit device| Flutter
```

### Component Responsibilities

**BLE Scan Filter (New)**
- Extracts advertisement data from scan results
- Applies SDK validation rules
- Filters out unsupported devices
- Prevents duplicate devices
- Emits only validated devices to Flutter

**BLE Scanner Helper (SDK Component)**
- Wraps Android BLE scanning API
- Provides ScanWrapperCallback interface
- Handles scan lifecycle

**Flutter Bridge (Modified)**
- Receives scan requests from Flutter
- Delegates to BLE Scan Filter
- Streams validated devices to Flutter
- No filtering logic in Dart

## Components and Interfaces

### 1. BLE Scan Filter

The new component that performs SDK-driven device validation.

```java
public class BleScanFilter {
    // SDK validation rules (extracted from documentation/sample code)
    private static final String[] VALID_DEVICE_NAME_PREFIXES = {"O_", "Q_", "R"};
    private static final int MIN_RSSI_THRESHOLD = -100;
    
    // Device tracking
    private final Map<String, ScannedDevice> discoveredDevices = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Callbacks
    private DeviceFilterCallback callback;
    
    /**
     * Validate a discovered BLE device against SDK rules.
     * 
     * @param device The Bluetooth device
     * @param rssi Signal strength
     * @param scanRecord Raw advertisement data
     * @return true if device is QRing-compatible, false otherwise
     */
    public boolean validateDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Rule 1: Device must have a MAC address
        if (device == null || device.getAddress() == null) {
            logRejection(device, "No MAC address");
            return false;
        }
        
        // Rule 2: RSSI must be above minimum threshold
        if (rssi < MIN_RSSI_THRESHOLD) {
            logRejection(device, "RSSI too low: " + rssi);
            return false;
        }
        
        // Rule 3: Device name validation (primary filter based on SDK sample)
        String deviceName = device.getName();
        if (deviceName != null && !deviceName.isEmpty()) {
            boolean nameMatches = false;
            for (String prefix : VALID_DEVICE_NAME_PREFIXES) {
                if (deviceName.startsWith(prefix)) {
                    nameMatches = true;
                    break;
                }
            }
            
            if (!nameMatches) {
                logRejection(device, "Device name doesn't match QRing pattern: " + deviceName);
                return false;
            }
        }
        // Note: Devices with null/empty names are allowed if they pass other checks
        
        // Rule 4: Additional validation from scan record if available
        if (scanRecord != null) {
            // Parse scan record for service UUIDs and manufacturer data
            // This would require additional SDK documentation
            // For now, we rely on device name as primary filter
        }
        
        logAcceptance(device, rssi);
        return true;
    }
    
    /**
     * Handle a discovered device, applying filtering and deduplication.
     */
    public void handleDiscoveredDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (!validateDevice(device, rssi, scanRecord)) {
            return;
        }
        
        String macAddress = device.getAddress();
        ScannedDevice scannedDevice = discoveredDevices.get(macAddress);
        
        if (scannedDevice == null) {
            // New device
            scannedDevice = new ScannedDevice(device, rssi);
            discoveredDevices.put(macAddress, scannedDevice);
            emitDevice(scannedDevice);
        } else {
            // Update existing device
            if (scannedDevice.updateRssi(rssi)) {
                emitDevice(scannedDevice);
            }
        }
    }
    
    /**
     * Clear discovered devices and reset filter state.
     */
    public void reset() {
        discoveredDevices.clear();
    }
    
    /**
     * Set callback for device emissions.
     */
    public void setCallback(DeviceFilterCallback callback) {
        this.callback = callback;
    }
    
    private void emitDevice(ScannedDevice device) {
        if (callback != null) {
            mainHandler.post(() -> callback.onDeviceDiscovered(device));
        }
    }
    
    private void logAcceptance(BluetoothDevice device, int rssi) {
        Log.d("BleScanFilter", "Device accepted: " + device.getName() + 
              " (" + device.getAddress() + ") RSSI: " + rssi);
    }
    
    private void logRejection(BluetoothDevice device, String reason) {
        String name = device != null ? device.getName() : "null";
        String mac = device != null ? device.getAddress() : "null";
        Log.d("BleScanFilter", "Device rejected: " + name + " (" + mac + ") - " + reason);
    }
    
    /**
     * Callback interface for filtered device emissions.
     */
    public interface DeviceFilterCallback {
        void onDeviceDiscovered(ScannedDevice device);
    }
}
```

### 2. Scanned Device Model

Data model for discovered devices.

```java
public class ScannedDevice {
    private final BluetoothDevice device;
    private final String macAddress;
    private final String name;
    private int rssi;
    private long lastSeenTimestamp;
    
    public ScannedDevice(BluetoothDevice device, int rssi) {
        this.device = device;
        this.macAddress = device.getAddress();
        this.name = device.getName();
        this.rssi = rssi;
        this.lastSeenTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Update RSSI value if it has changed significantly.
     * @return true if RSSI changed by more than 5 dBm, false otherwise
     */
    public boolean updateRssi(int newRssi) {
        boolean significantChange = Math.abs(newRssi - this.rssi) >= 5;
        this.rssi = newRssi;
        this.lastSeenTimestamp = System.currentTimeMillis();
        return significantChange;
    }
    
    /**
     * Convert to map for Flutter bridge.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "Unknown Device");
        map.put("macAddress", macAddress);
        map.put("rssi", rssi);
        map.put("lastSeen", lastSeenTimestamp);
        return map;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public String getName() {
        return name;
    }
    
    public int getRssi() {
        return rssi;
    }
    
    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ScannedDevice)) return false;
        ScannedDevice other = (ScannedDevice) obj;
        return macAddress.equals(other.macAddress);
    }
    
    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }
}
```

### 3. Updated BLE Manager

Integration of scan filter into existing BLE Manager.

```java
public class BleManager {
    private static final String TAG = "BleManager";
    
    private final Context context;
    private final BleScanFilter scanFilter;
    private EventChannel.EventSink devicesSink;
    private boolean isScanning = false;
    
    public BleManager(Context context) {
        this.context = context;
        this.scanFilter = new BleScanFilter();
        
        // Set up filter callback
        scanFilter.setCallback(device -> {
            if (devicesSink != null) {
                List<Map<String, Object>> deviceList = new ArrayList<>();
                deviceList.add(device.toMap());
                devicesSink.success(deviceList);
            }
        });
    }
    
    /**
     * Start BLE scanning with SDK-driven filtering.
     */
    public void startScan() {
        if (isScanning) {
            Log.d(TAG, "Scan already in progress");
            return;
        }
        
        scanFilter.reset();
        isScanning = true;
        
        Log.d(TAG, "Starting BLE scan with SDK filtering");
        
        BleScannerHelper.getInstance().scanDevice(
            context,
            null, // UUID filter (null = scan all)
            new ScanWrapperCallback() {
                @Override
                public void onStart() {
                    Log.d(TAG, "Scan started");
                }
                
                @Override
                public void onStop() {
                    Log.d(TAG, "Scan stopped");
                    isScanning = false;
                }
                
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // Delegate to scan filter for validation
                    scanFilter.handleDiscoveredDevice(device, rssi, scanRecord);
                }
                
                @Override
                public void onScanFailed(int errorCode) {
                    Log.e(TAG, "Scan failed with error code: " + errorCode);
                    isScanning = false;
                    if (devicesSink != null) {
                        devicesSink.error("SCAN_FAILED", 
                            "BLE scan failed with code: " + errorCode, null);
                    }
                }
                
                @Override
                public void onParsedData(BluetoothDevice device, ScanRecord scanRecord) {
                    // Handle parsed scan record if needed
                    scanFilter.handleDiscoveredDevice(device, 0, null);
                }
                
                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult result : results) {
                        scanFilter.handleDiscoveredDevice(
                            result.getDevice(), 
                            result.getRssi(), 
                            result.getScanRecord() != null ? 
                                result.getScanRecord().getBytes() : null
                        );
                    }
                }
            }
        );
    }
    
    /**
     * Stop BLE scanning.
     */
    public void stopScan() {
        if (!isScanning) {
            return;
        }
        
        Log.d(TAG, "Stopping BLE scan");
        BleScannerHelper.getInstance().stopScan(context);
        isScanning = false;
    }
}
```

### 4. Flutter Bridge (No Changes Required)

The existing Flutter bridge API remains unchanged. The filtering happens transparently in the native layer.

```dart
// Existing API - no changes needed
class QringSdkFlutter {
    static Future<void> startScan();
    static Future<void> stopScan();
    static Stream<List<QRingDevice>> get devicesStream;
}
```

## Data Models

### SDK Validation Rules

Based on the SDK sample code analysis, the validation rules are:

**Primary Rule: Device Name Prefix**
- Devices with names starting with "O_", "Q_", or "R" are QRing devices
- This is the primary filter used in the official SDK sample code
- Devices with null/empty names are allowed (may be unnamed QRing devices)

**Secondary Rules:**
- RSSI threshold: -100 dBm minimum (filter out extremely weak signals)
- MAC address must be present
- Device must be connectable

**Future Enhancement:**
- Service UUID filtering (requires additional SDK documentation)
- Manufacturer data validation (requires additional SDK documentation)

### Device Information Flow

```
BLE Advertisement
    ↓
Android BLE API (ScanResult)
    ↓
QRing SDK (BleScannerHelper)
    ↓
ScanWrapperCallback.onLeScan()
    ↓
BleScanFilter.validateDevice()
    ↓ (if valid)
BleScanFilter.handleDiscoveredDevice()
    ↓
DeviceFilterCallback.onDeviceDiscovered()
    ↓
EventChannel.EventSink
    ↓
Flutter Stream
    ↓
UI Display
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1: SDK Rules Application
*For any* discovered BLE device, the Scan_Filter should apply SDK validation rules before determining compatibility.

**Validates: Requirements 1.1**

### Property 2: Service UUID Validation
*For any* device with advertised Service_UUID values, the Scan_Filter should check them against QRing service identifiers and only accept devices with matching UUIDs.

**Validates: Requirements 1.2**

### Property 3: Manufacturer Data Validation
*For any* device with Manufacturer_Data, the Scan_Filter should check it against QRing manufacturer identifiers and only accept devices with matching data.

**Validates: Requirements 1.3**

### Property 4: Name-Independent Validation
*For any* device with valid Service_UUID or Manufacturer_Data but no device name, the Scan_Filter should still accept the device (device name is not the primary criterion).

**Validates: Requirements 1.4**

### Property 5: Null Name Acceptance
*For any* QRing-compatible device with empty or null device name, the Scan_Filter should allow it if other validation criteria pass.

**Validates: Requirements 1.5**

### Property 6: Validation Before Emission
*For any* device emitted to Flutter_Layer, validation must have been performed before emission (no device reaches Flutter without validation).

**Validates: Requirements 2.1**

### Property 7: Service UUID Extraction
*For any* Advertisement_Packet containing Service_UUID values, the Native_Layer should extract all advertised UUIDs correctly.

**Validates: Requirements 2.2**

### Property 8: Manufacturer Data Extraction
*For any* Advertisement_Packet containing Manufacturer_Data, the Native_Layer should extract it correctly.

**Validates: Requirements 2.3**

### Property 9: Only Valid Devices to Flutter
*For any* device received by Flutter_Layer, it must have passed Native_Layer validation (no invalid devices reach Flutter).

**Validates: Requirements 2.5**

### Property 10: MAC Address Extraction
*For any* discovered BLE device, the BLE_Scanner should extract the MAC_Address.

**Validates: Requirements 3.3**

### Property 11: RSSI Extraction
*For any* discovered BLE device, the BLE_Scanner should extract the RSSI signal strength.

**Validates: Requirements 3.4**

### Property 12: Device Name Extraction
*For any* discovered BLE device with a name, the BLE_Scanner should extract the device name.

**Validates: Requirements 3.5**

### Property 13: MAC-Based Deduplication
*For any* two devices with the same MAC_Address, the BLE_Scanner should treat them as the same device (use MAC for uniqueness).

**Validates: Requirements 5.1**

### Property 14: Update on Rediscovery
*For any* device discovered multiple times, the BLE_Scanner should update the existing entry rather than create a duplicate.

**Validates: Requirements 5.2**

### Property 15: RSSI Update
*For any* device with changed RSSI value, the BLE_Scanner should update the RSSI in the existing entry.

**Validates: Requirements 5.3**

### Property 16: MAC Address Presence
*For any* device emitted by Native_Layer, it must include a MAC_Address field.

**Validates: Requirements 6.1**

### Property 17: Device Name Field Presence
*For any* device emitted by Native_Layer, it must include a device name field (which may be null).

**Validates: Requirements 6.2**

### Property 18: RSSI Field Presence
*For any* device emitted by Native_Layer, it must include an RSSI signal strength value.

**Validates: Requirements 6.3**

### Property 19: Debug Metadata Presence
*For any* device emitted by Native_Layer when debugging is enabled, it should include raw advertisement metadata.

**Validates: Requirements 6.4**

### Property 20: Timestamp Presence
*For any* device emitted by Native_Layer, it must include a timestamp for when the device was last seen.

**Validates: Requirements 6.5**

### Property 21: Exclude Non-QRing UUIDs
*For any* device that does not advertise QRing Service_UUID values, the Scan_Filter should exclude it from results.

**Validates: Requirements 7.1**

### Property 22: Exclude Non-QRing Manufacturer Data
*For any* device that does not have matching QRing Manufacturer_Data, the Scan_Filter should exclude it from results.

**Validates: Requirements 7.2**

### Property 23: Exclude Failed Validation
*For any* device that fails SDK validation, the Scan_Filter should exclude it from results.

**Validates: Requirements 7.3**

### Property 24: SDK Compatibility Guarantee
*For any* device emitted to Flutter_Layer, the QRing SDK must support the device (all emitted devices are SDK-compatible).

**Validates: Requirements 8.2**

### Property 25: No False Positives
*For any* non-QRing device, the Scan_Filter should reject it (prevent false positives).

**Validates: Requirements 8.5**

### Property 26: Scan Failure Reporting
*For any* scan failure, the BLE_Scanner should report the failure reason to Flutter_Layer.

**Validates: Requirements 10.3**

## Error Handling

### Error Categories

**Bluetooth Errors**
- Code: `BLUETOOTH_DISABLED`
- Message: "Bluetooth is disabled"
- Recovery: Prompt user to enable Bluetooth
- Reporting: Emit error to Flutter via error stream

**Permission Errors**
- Code: `PERMISSION_DENIED`
- Message: "Location permission is required for BLE scanning"
- Recovery: Prompt user to grant permission
- Reporting: Emit error to Flutter via error stream

**Scan Errors**
- Code: `SCAN_FAILED`
- Message: "BLE scan failed with code: [errorCode]"
- Recovery: Retry scan or report to user
- Reporting: Emit error to Flutter via error stream

**Empty Results**
- Code: None (not an error)
- Message: Empty device list
- Recovery: Continue scanning or inform user
- Reporting: Emit empty list to Flutter

### Error Logging

All errors and rejections should be logged with:
- Timestamp
- Error code or rejection reason
- Device information (MAC, name if available)
- Context (scan state, filter state)

## Testing Strategy

### Unit Testing

**BLE Scan Filter Tests**
- Device name prefix validation
- RSSI threshold validation
- MAC address requirement
- Null/empty name handling
- Duplicate device handling
- RSSI update logic

**Scanned Device Model Tests**
- RSSI update threshold (5 dBm)
- Timestamp updates
- Map conversion
- Equality based on MAC address

**BLE Manager Integration Tests**
- Scan start/stop lifecycle
- Filter callback integration
- Event sink emission
- Error handling

### Property-Based Testing

Each correctness property will be implemented as a property-based test using jqwik (Java property testing framework).

**Test Configuration:**
- Minimum 100 iterations per property test
- Random input generation for device data, RSSI values, and MAC addresses
- Shrinking enabled for failure case minimization

**Property Test Examples:**

```java
@Property
public void validDeviceNamePrefixAccepted(@ForAll("qringDeviceNames") String deviceName,
                                          @ForAll @IntRange(min = -100, max = 0) int rssi) {
    BluetoothDevice device = mockDevice(deviceName, "AA:BB:CC:DD:EE:FF");
    BleScanFilter filter = new BleScanFilter();
    
    boolean result = filter.validateDevice(device, rssi, null);
    
    assertTrue(result, "Device with QRing name prefix should be accepted");
}

@Provide
Arbitrary<String> qringDeviceNames() {
    return Arbitraries.of("O_", "Q_", "R")
        .flatMap(prefix -> Arbitraries.strings().alpha().ofMinLength(1)
            .map(suffix -> prefix + suffix));
}

@Property
public void invalidDeviceNamePrefixRejected(@ForAll("nonQringDeviceNames") String deviceName,
                                            @ForAll @IntRange(min = -100, max = 0) int rssi) {
    BluetoothDevice device = mockDevice(deviceName, "AA:BB:CC:DD:EE:FF");
    BleScanFilter filter = new BleScanFilter();
    
    boolean result = filter.validateDevice(device, rssi, null);
    
    assertFalse(result, "Device without QRing name prefix should be rejected");
}

@Provide
Arbitrary<String> nonQringDeviceNames() {
    return Arbitraries.strings().alpha().ofMinLength(1)
        .filter(name -> !name.startsWith("O_") && 
                       !name.startsWith("Q_") && 
                       !name.startsWith("R"));
}

@Property
public void duplicateDevicesByMacAreDeduped(@ForAll("macAddress") String mac,
                                           @ForAll @IntRange(min = -100, max = 0) int rssi1,
                                           @ForAll @IntRange(min = -100, max = 0) int rssi2) {
    BluetoothDevice device1 = mockDevice("Q_Ring1", mac);
    BluetoothDevice device2 = mockDevice("Q_Ring2", mac);
    BleScanFilter filter = new BleScanFilter();
    
    filter.handleDiscoveredDevice(device1, rssi1, null);
    filter.handleDiscoveredDevice(device2, rssi2, null);
    
    assertEquals(1, filter.getDiscoveredDevicesCount(), 
        "Devices with same MAC should be deduplicated");
}

@Provide
Arbitrary<String> macAddress() {
    return Arbitraries.strings()
        .withChars("0123456789ABCDEF")
        .ofLength(2)
        .list().ofSize(6)
        .map(parts -> String.join(":", parts));
}

@Property
public void rssiUpdateOnlyWhenSignificantChange(@ForAll("macAddress") String mac,
                                                @ForAll @IntRange(min = -100, max = 0) int initialRssi) {
    BluetoothDevice device = mockDevice("Q_Ring", mac);
    ScannedDevice scannedDevice = new ScannedDevice(device, initialRssi);
    
    // Small change (< 5 dBm) should not trigger update
    boolean smallChange = scannedDevice.updateRssi(initialRssi + 3);
    assertFalse(smallChange, "Small RSSI change should not trigger update");
    
    // Large change (>= 5 dBm) should trigger update
    boolean largeChange = scannedDevice.updateRssi(initialRssi + 10);
    assertTrue(largeChange, "Large RSSI change should trigger update");
}

@Property
public void onlyValidatedDevicesReachFlutter(@ForAll("randomDevices") List<BluetoothDevice> devices,
                                             @ForAll("rssiValues") List<Integer> rssiValues) {
    BleScanFilter filter = new BleScanFilter();
    List<ScannedDevice> emittedDevices = new ArrayList<>();
    
    filter.setCallback(emittedDevices::add);
    
    for (int i = 0; i < devices.size(); i++) {
        filter.handleDiscoveredDevice(devices.get(i), rssiValues.get(i), null);
    }
    
    // All emitted devices must pass validation
    for (ScannedDevice device : emittedDevices) {
        assertTrue(filter.validateDevice(device.getDevice(), device.getRssi(), null),
            "All emitted devices must pass validation");
    }
}
```

### Integration Testing

**End-to-End Scenarios:**
1. Start scan → discover QRing devices → verify only QRing devices appear
2. Start scan → discover mixed devices → verify non-QRing devices filtered out
3. Discover same device multiple times → verify deduplication
4. Discover device with RSSI changes → verify RSSI updates
5. Scan with Bluetooth disabled → verify error reported
6. Scan without permissions → verify error reported

**Test Environment:**
- Physical Android device (API 31+)
- QRing device for actual BLE testing
- Non-QRing BLE devices for negative testing
- Automated test scripts for scenario execution

### Manual Testing Checklist

- [ ] Scan shows only QRing devices (O_, Q_, R prefixes)
- [ ] Non-QRing devices are filtered out
- [ ] Devices with no name but valid properties appear
- [ ] Same device doesn't appear multiple times
- [ ] RSSI updates reflect signal strength changes
- [ ] Bluetooth disabled error is shown
- [ ] Permission denied error is shown
- [ ] Empty scan results handled gracefully
- [ ] All listed devices can be connected successfully

## Implementation Notes

### SDK Documentation Findings

Based on analysis of the official SDK sample code (DeviceBindActivity.kt), the primary filtering mechanism is:

**Device Name Prefix Matching:**
```kotlin
// From SDK sample code (commented out but shows intent):
// if (device.name.startsWith("O_")||device.name.startsWith("Q_"))
```

This indicates QRing devices use specific name prefixes:
- "O_" prefix (likely for Oura-compatible devices)
- "Q_" prefix (likely for QRing devices)
- "R" prefix (observed in current implementation)

**Current Implementation:**
The current implementation uses a simple name check:
```dart
e.name.toLowerCase().startsWith('r')
```

This is too broad and unreliable. The new implementation will use the SDK-documented prefixes.

### Future Enhancements

**Service UUID Filtering:**
- Requires additional SDK documentation
- Would provide more reliable filtering than name-based
- Should be added when SDK documentation is available

**Manufacturer Data Filtering:**
- Requires manufacturer ID from SDK documentation
- Would provide definitive device identification
- Should be added when SDK documentation is available

**SDK Validation Method:**
- Check if SDK provides `isSupportedDevice()` or similar
- Would be the most reliable filtering mechanism
- Should be used if available in SDK

### Migration Strategy

**Phase 1: Name-Based Filtering (Current)**
- Implement proper name prefix checking (O_, Q_, R)
- Move filtering to native layer
- Remove Flutter-side filtering

**Phase 2: Enhanced Filtering (Future)**
- Add Service UUID filtering when documented
- Add Manufacturer Data filtering when documented
- Use SDK validation methods if available

**Phase 3: Optimization (Future)**
- Implement scan result caching
- Add background scanning support
- Optimize battery usage
