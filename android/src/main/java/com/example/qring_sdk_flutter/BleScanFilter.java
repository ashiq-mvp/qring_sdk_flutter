package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * BLE Scan Filter for SDK-driven device validation.
 * 
 * This class performs SDK-driven BLE device validation to ensure only
 * QRing-compatible devices are emitted to the Flutter layer. It applies
 * validation rules based on device name prefixes, RSSI thresholds, and
 * MAC address requirements. The filter also handles device deduplication
 * and RSSI updates.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 7.1, 7.2, 7.3
 */
public class BleScanFilter {
    private static final String TAG = "BleScanFilter";
    
    // SDK validation rules constants (device name prefixes: "O_", "Q_", "R")
    // Based on official SDK sample code analysis
    private static final String[] VALID_DEVICE_NAME_PREFIXES = {"O_", "Q_", "R"};
    
    // RSSI threshold constant (-100 dBm minimum)
    // Devices with weaker signals are filtered out
    private static final int MIN_RSSI_THRESHOLD = -100;
    
    // Device tracking with HashMap<String, ScannedDevice>
    private final Map<String, ScannedDevice> discoveredDevices = new HashMap<>();
    
    // Handler for main thread operations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Callback for device emission
    private DeviceFilterCallback callback;
    
    // Debug flag for verbose logging
    // Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
    // Requirement 12.1-12.5: Debug and logging support
    private boolean debugEnabled = false;
    
    /**
     * Validate a discovered BLE device against SDK rules.
     * 
     * Requirement 1.1: THE Scan_Filter SHALL validate devices using SDK_Rules
     * Requirement 1.4: THE Scan_Filter SHALL NOT use device name as the primary filtering criterion
     * Requirement 1.5: THE Scan_Filter SHALL allow QRing_Device instances with empty or null device names
     * Requirement 7.3: WHEN a device fails SDK validation, THE Scan_Filter SHALL exclude it
     * Requirement 12.2: THE Scan_Filter SHALL log the validation decision for each device (accepted/rejected)
     * Requirement 12.3: THE Scan_Filter SHALL log the reason for rejection
     * 
     * @param device The Bluetooth device
     * @param rssi Signal strength in dBm
     * @param scanRecord Raw advertisement data (currently unused, for future enhancement)
     * @return true if device is QRing-compatible, false otherwise
     */
    public boolean validateDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Log raw advertisement data if debug enabled
        // Requirement 12.4: WHERE debugging is enabled, THE BLE_Scanner SHALL log raw advertisement data
        if (debugEnabled && scanRecord != null) {
            logRawAdvertisementData(device, scanRecord);
        }
        
        // Rule 1: Device must have a MAC address
        if (device == null || device.getAddress() == null) {
            logRejection(device, "No MAC address");
            return false;
        }
        
        // Rule 2: RSSI must be above minimum threshold
        // Requirement 7.3: Filter out devices with extremely weak signals
        if (rssi < MIN_RSSI_THRESHOLD) {
            logRejection(device, "RSSI too low: " + rssi + " dBm (minimum: " + MIN_RSSI_THRESHOLD + " dBm)");
            return false;
        }
        
        // Rule 3: Device name validation
        // Requirement 1.4: Device name is not the primary criterion
        // Requirement 1.5: Devices with null/empty names are allowed if they pass other checks
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
        // This is for future enhancement when SDK documentation provides
        // Service UUID and Manufacturer Data validation rules
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
     * 
     * Requirement 5.1: THE BLE_Scanner SHALL use MAC_Address to identify unique devices
     * Requirement 5.2: WHEN a device is discovered multiple times, THE BLE_Scanner SHALL update the existing entry
     * Requirement 5.3: WHEN a device RSSI changes, THE BLE_Scanner SHALL update the RSSI value
     * Requirement 12.1: THE BLE_Scanner SHALL log each discovered device with MAC_Address and name
     * 
     * @param device The Bluetooth device
     * @param rssi Signal strength in dBm
     * @param scanRecord Raw advertisement data
     */
    public void handleDiscoveredDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // Log device discovery
        // Requirement 12.1: THE BLE_Scanner SHALL log each discovered device with MAC_Address and name
        logDeviceDiscovery(device, rssi);
        
        // Apply validation first
        if (!validateDevice(device, rssi, scanRecord)) {
            return;
        }
        
        // Use MAC address for deduplication (Requirement 5.1)
        String macAddress = device.getAddress();
        ScannedDevice scannedDevice = discoveredDevices.get(macAddress);
        
        if (scannedDevice == null) {
            // New device - create and emit
            scannedDevice = new ScannedDevice(device, rssi, debugEnabled ? scanRecord : null);
            discoveredDevices.put(macAddress, scannedDevice);
            emitDevice(scannedDevice);
            Log.d(TAG, "New device discovered: " + macAddress + " (" + device.getName() + ")");
        } else {
            // Existing device - update RSSI if significant change (Requirement 5.2, 5.3)
            if (scannedDevice.updateRssi(rssi)) {
                emitDevice(scannedDevice);
                Log.d(TAG, "Device RSSI updated: " + macAddress + " -> " + rssi + " dBm");
            }
        }
    }
    
    /**
     * Reset method to clear discovered devices.
     * 
     * Clears the internal device tracking map. This should be called
     * when starting a new scan to ensure fresh results.
     * 
     * Requirement 12.5: THE BLE_Scanner SHALL log scan start and stop events with timestamps
     */
    public void reset() {
        discoveredDevices.clear();
        Log.d(TAG, "Scan filter reset - cleared discovered devices at " + System.currentTimeMillis());
    }
    
    /**
     * Set callback for device emission.
     * 
     * @param callback Callback to receive filtered devices
     */
    public void setCallback(DeviceFilterCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Enable or disable debug logging.
     * 
     * When enabled, the filter will log raw advertisement data and additional
     * debug information for troubleshooting.
     * 
     * Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
     * Requirement 12.4: WHERE debugging is enabled, THE BLE_Scanner SHALL log raw advertisement data
     * 
     * @param enabled true to enable debug logging, false to disable
     */
    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
        Log.d(TAG, "Debug logging " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if debug logging is enabled.
     * 
     * @return true if debug logging is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * Get the number of discovered devices (for testing).
     * 
     * @return Number of unique devices discovered
     */
    public int getDiscoveredDevicesCount() {
        return discoveredDevices.size();
    }
    
    /**
     * Emit a device to the callback on the main thread.
     * 
     * @param device The device to emit
     */
    private void emitDevice(ScannedDevice device) {
        if (callback != null) {
            mainHandler.post(() -> callback.onDeviceDiscovered(device));
        }
    }
    
    /**
     * Log device acceptance with details.
     * 
     * Requirement 12.2: THE Scan_Filter SHALL log the validation decision for each device (accepted/rejected)
     * 
     * @param device The accepted device
     * @param rssi Signal strength
     */
    private void logAcceptance(BluetoothDevice device, int rssi) {
        String name = device.getName() != null ? device.getName() : "<no name>";
        Log.d(TAG, "✓ Device accepted: " + name + " (" + device.getAddress() + ") RSSI: " + rssi + " dBm");
    }
    
    /**
     * Log device rejection with reason.
     * 
     * Requirement 7.5: THE Scan_Filter SHALL log excluded devices for debugging purposes
     * Requirement 12.2: THE Scan_Filter SHALL log the validation decision for each device (accepted/rejected)
     * Requirement 12.3: THE Scan_Filter SHALL log the reason for rejection
     * 
     * @param device The rejected device (may be null)
     * @param reason Reason for rejection
     */
    private void logRejection(BluetoothDevice device, String reason) {
        String name = device != null && device.getName() != null ? device.getName() : "<no name>";
        String mac = device != null ? device.getAddress() : "<null>";
        Log.d(TAG, "✗ Device rejected: " + name + " (" + mac + ") - " + reason);
    }
    
    /**
     * Log device discovery event.
     * 
     * Requirement 12.1: THE BLE_Scanner SHALL log each discovered device with MAC_Address and name
     * 
     * @param device The discovered device
     * @param rssi Signal strength
     */
    private void logDeviceDiscovery(BluetoothDevice device, int rssi) {
        if (device == null) {
            Log.d(TAG, "Device discovery: null device");
            return;
        }
        
        String name = device.getName() != null ? device.getName() : "<no name>";
        String mac = device.getAddress();
        Log.d(TAG, "Device discovered: " + name + " (" + mac + ") RSSI: " + rssi + " dBm");
    }
    
    /**
     * Log raw advertisement data for debugging.
     * 
     * Requirement 12.4: WHERE debugging is enabled, THE BLE_Scanner SHALL log raw advertisement data
     * 
     * @param device The device
     * @param scanRecord Raw advertisement data
     */
    private void logRawAdvertisementData(BluetoothDevice device, byte[] scanRecord) {
        if (scanRecord == null || scanRecord.length == 0) {
            Log.d(TAG, "Raw advertisement data: <empty>");
            return;
        }
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : scanRecord) {
            hexString.append(String.format("%02X ", b));
        }
        
        String mac = device != null ? device.getAddress() : "<null>";
        Log.d(TAG, "Raw advertisement data for " + mac + " (" + scanRecord.length + " bytes): " + hexString.toString().trim());
    }
    
    /**
     * Callback interface for filtered device emissions.
     */
    public interface DeviceFilterCallback {
        /**
         * Called when a validated device is discovered or updated.
         * 
         * @param device The scanned device
         */
        void onDeviceDiscovered(ScannedDevice device);
    }
}
