# Requirements Document

## Introduction

This specification defines the requirements for implementing SDK-driven BLE scan filtering for the QRing Flutter plugin. The system shall filter BLE devices during scanning to show only QRing-compatible devices based on SDK validation rules (advertised services, manufacturer data, SDK logic) rather than unreliable device name heuristics. This ensures users only see devices they can actually connect to, providing a professional smart ring app experience.

## Glossary

- **BLE_Scanner**: The Bluetooth Low Energy scanning component that discovers nearby devices
- **Scan_Filter**: The filtering logic that determines which discovered devices are QRing-compatible
- **Service_UUID**: Universally Unique Identifier for BLE services advertised by devices
- **Manufacturer_Data**: Device-specific data included in BLE advertisements
- **Advertisement_Packet**: The data broadcast by BLE devices during discovery
- **QRing_Device**: A smart ring device that is compatible with the QRing SDK
- **Flutter_Layer**: The Dart/Flutter application code that displays scan results
- **Native_Layer**: The Android Java code that performs BLE scanning and filtering
- **SDK_Rules**: The official QRing SDK validation logic for device compatibility
- **MAC_Address**: Media Access Control address uniquely identifying a BLE device

## Requirements

### Requirement 1: SDK-Based Device Validation

**User Story:** As a user, I want to see only QRing-compatible devices in the scan list, so that I don't waste time trying to connect to unsupported devices.

#### Acceptance Criteria

1. THE Scan_Filter SHALL validate devices using SDK_Rules extracted from official documentation
2. THE Scan_Filter SHALL check advertised Service_UUID values against QRing service identifiers
3. THE Scan_Filter SHALL check Manufacturer_Data against QRing manufacturer identifiers
4. THE Scan_Filter SHALL NOT use device name as the primary filtering criterion
5. THE Scan_Filter SHALL allow QRing_Device instances with empty or null device names

### Requirement 2: Native Layer Filtering

**User Story:** As a developer, I want BLE filtering to happen in the native Android layer, so that Flutter only receives validated devices and the architecture is clean.

#### Acceptance Criteria

1. THE Native_Layer SHALL perform all device validation before emitting to Flutter_Layer
2. THE Native_Layer SHALL extract Service_UUID values from Advertisement_Packet
3. THE Native_Layer SHALL extract Manufacturer_Data from Advertisement_Packet
4. THE Native_Layer SHALL apply SDK_Rules to determine device compatibility
5. THE Flutter_Layer SHALL receive only devices that passed Native_Layer validation
6. THE Flutter_Layer SHALL treat every received device as QRing-compatible

### Requirement 3: Advertisement Data Extraction

**User Story:** As a developer, I want to extract all relevant BLE advertisement data, so that I can apply comprehensive filtering rules.

#### Acceptance Criteria

1. WHEN a BLE device is discovered, THE BLE_Scanner SHALL extract all advertised Service_UUID values
2. WHEN a BLE device is discovered, THE BLE_Scanner SHALL extract Manufacturer_Data if present
3. WHEN a BLE device is discovered, THE BLE_Scanner SHALL extract the MAC_Address
4. WHEN a BLE device is discovered, THE BLE_Scanner SHALL extract the RSSI signal strength
5. WHEN a BLE device is discovered, THE BLE_Scanner SHALL extract the device name if present

### Requirement 4: SDK Documentation Compliance

**User Story:** As a developer, I want filtering rules to match the official SDK documentation, so that the implementation is correct and maintainable.

#### Acceptance Criteria

1. THE Scan_Filter SHALL use Service_UUID values specified in the SDK documentation
2. THE Scan_Filter SHALL use Manufacturer_Data patterns specified in the SDK documentation
3. THE Scan_Filter SHALL use any SDK-provided validation methods if available
4. THE Scan_Filter SHALL NOT invent filtering rules not present in SDK documentation
5. WHEN SDK documentation is updated, THE Scan_Filter SHALL be updatable to match new rules

### Requirement 5: Duplicate Device Handling

**User Story:** As a user, I want each device to appear only once in the scan list, so that the list is clean and easy to navigate.

#### Acceptance Criteria

1. THE BLE_Scanner SHALL use MAC_Address to identify unique devices
2. WHEN a device is discovered multiple times, THE BLE_Scanner SHALL update the existing entry
3. WHEN a device RSSI changes, THE BLE_Scanner SHALL update the RSSI value
4. THE BLE_Scanner SHALL NOT create duplicate entries for the same MAC_Address
5. THE BLE_Scanner SHALL maintain a map of MAC_Address to device information

### Requirement 6: Device Information Model

**User Story:** As a developer, I want a clear data model for scanned devices, so that Flutter can display relevant information to users.

#### Acceptance Criteria

1. THE Native_Layer SHALL provide MAC_Address for each discovered device
2. THE Native_Layer SHALL provide device name for each discovered device (nullable)
3. THE Native_Layer SHALL provide RSSI signal strength for each discovered device
4. WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
5. THE Native_Layer SHALL provide a timestamp for when the device was last seen

### Requirement 7: Unsupported Device Exclusion

**User Story:** As a user, I want to see only devices I can connect to, so that I don't encounter connection failures with unsupported devices.

#### Acceptance Criteria

1. WHEN a device does not advertise QRing Service_UUID values, THE Scan_Filter SHALL exclude it
2. WHEN a device does not have matching Manufacturer_Data, THE Scan_Filter SHALL exclude it
3. WHEN a device fails SDK validation, THE Scan_Filter SHALL exclude it
4. THE Scan_Filter SHALL NOT emit unsupported devices to Flutter_Layer
5. THE Scan_Filter SHALL log excluded devices for debugging purposes

### Requirement 8: Connection Success Guarantee

**User Story:** As a user, I want every device in the scan list to be connectable, so that I have confidence in the app's reliability.

#### Acceptance Criteria

1. FOR ALL devices emitted to Flutter_Layer, connection attempts SHALL succeed if the device is in range
2. FOR ALL devices emitted to Flutter_Layer, the QRing SDK SHALL support the device
3. THE Scan_Filter SHALL ensure only SDK-compatible devices are emitted
4. WHEN a device appears in the scan list, THE user SHALL be able to initiate pairing
5. THE Scan_Filter SHALL prevent false positives (non-QRing devices appearing as QRing devices)

### Requirement 9: Scan Performance

**User Story:** As a user, I want device scanning to be fast and responsive, so that I can quickly find my ring.

#### Acceptance Criteria

1. THE BLE_Scanner SHALL emit discovered devices immediately after validation
2. THE BLE_Scanner SHALL perform filtering synchronously during scan callback
3. THE BLE_Scanner SHALL NOT batch device results unnecessarily
4. THE Scan_Filter SHALL complete validation within 10 milliseconds per device
5. THE BLE_Scanner SHALL update Flutter_Layer within 100 milliseconds of device discovery

### Requirement 10: Error Handling

**User Story:** As a user, I want clear error messages when scanning fails, so that I understand what went wrong.

#### Acceptance Criteria

1. WHEN Bluetooth is disabled, THE BLE_Scanner SHALL report a Bluetooth error
2. WHEN location permission is missing, THE BLE_Scanner SHALL report a permission error
3. WHEN scan fails to start, THE BLE_Scanner SHALL report the failure reason
4. WHEN no devices are found, THE BLE_Scanner SHALL report an empty result (not an error)
5. THE BLE_Scanner SHALL log all errors with timestamps and error codes

### Requirement 11: Flutter API Simplification

**User Story:** As a Flutter developer, I want a simple scanning API, so that I can easily integrate device discovery into the UI.

#### Acceptance Criteria

1. THE Flutter_Layer SHALL provide a startScan method that returns a stream of devices
2. THE Flutter_Layer SHALL provide a stopScan method to terminate scanning
3. THE Flutter_Layer SHALL emit only validated QRing_Device instances
4. THE Flutter_Layer SHALL NOT require additional filtering logic in Dart
5. THE Flutter_Layer SHALL provide device updates (RSSI changes) through the same stream

### Requirement 12: Debug and Logging Support

**User Story:** As a developer, I want comprehensive logging during scanning, so that I can troubleshoot filtering issues.

#### Acceptance Criteria

1. THE BLE_Scanner SHALL log each discovered device with MAC_Address and name
2. THE Scan_Filter SHALL log the validation decision for each device (accepted/rejected)
3. THE Scan_Filter SHALL log the reason for rejection (missing UUID, wrong manufacturer, etc.)
4. WHERE debugging is enabled, THE BLE_Scanner SHALL log raw advertisement data
5. THE BLE_Scanner SHALL log scan start and stop events with timestamps
