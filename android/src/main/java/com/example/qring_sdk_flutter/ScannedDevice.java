package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import java.util.HashMap;
import java.util.Map;

/**
 * Scanned Device Model for BLE scan results.
 * 
 * This class represents a discovered BLE device during scanning. It tracks
 * device information including MAC address, name, RSSI signal strength, and
 * last seen timestamp. The model supports RSSI updates with a threshold to
 * avoid excessive updates for minor signal fluctuations.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.5
 */
public class ScannedDevice {
    private static final String TAG = "ScannedDevice";
    
    // RSSI update threshold - only update if change is >= 5 dBm
    private static final int RSSI_UPDATE_THRESHOLD = 5;
    
    // Device information
    private final BluetoothDevice device;
    private final String macAddress;
    private final String name;
    private int rssi;
    private long lastSeenTimestamp;
    
    // Debug metadata (only populated when debug is enabled)
    // Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
    private final byte[] rawAdvertisementData;
    
    /**
     * Constructor accepting BluetoothDevice and RSSI.
     * 
     * Requirement 6.1: THE Native_Layer SHALL provide MAC_Address for each discovered device
     * Requirement 6.2: THE Native_Layer SHALL provide device name for each discovered device (nullable)
     * Requirement 6.3: THE Native_Layer SHALL provide RSSI signal strength for each discovered device
     * Requirement 6.5: THE Native_Layer SHALL provide a timestamp for when the device was last seen
     * 
     * @param device The Bluetooth device
     * @param rssi Signal strength in dBm
     */
    public ScannedDevice(BluetoothDevice device, int rssi) {
        this(device, rssi, null);
    }
    
    /**
     * Constructor accepting BluetoothDevice, RSSI, and optional raw advertisement data.
     * 
     * Requirement 6.1: THE Native_Layer SHALL provide MAC_Address for each discovered device
     * Requirement 6.2: THE Native_Layer SHALL provide device name for each discovered device (nullable)
     * Requirement 6.3: THE Native_Layer SHALL provide RSSI signal strength for each discovered device
     * Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
     * Requirement 6.5: THE Native_Layer SHALL provide a timestamp for when the device was last seen
     * 
     * @param device The Bluetooth device
     * @param rssi Signal strength in dBm
     * @param rawAdvertisementData Raw advertisement data (null if debug disabled)
     */
    public ScannedDevice(BluetoothDevice device, int rssi, byte[] rawAdvertisementData) {
        this.device = device;
        this.macAddress = device.getAddress();
        this.name = device.getName();
        this.rssi = rssi;
        this.lastSeenTimestamp = System.currentTimeMillis();
        this.rawAdvertisementData = rawAdvertisementData;
    }
    
    /**
     * Update RSSI value if it has changed significantly.
     * 
     * Only updates if the change is >= 5 dBm to avoid excessive updates
     * for minor signal fluctuations. Always updates the timestamp.
     * 
     * @param newRssi New RSSI value in dBm
     * @return true if RSSI changed by more than or equal to 5 dBm, false otherwise
     */
    public boolean updateRssi(int newRssi) {
        boolean significantChange = Math.abs(newRssi - this.rssi) >= RSSI_UPDATE_THRESHOLD;
        this.rssi = newRssi;
        this.lastSeenTimestamp = System.currentTimeMillis();
        return significantChange;
    }
    
    /**
     * Convert to map for Flutter bridge conversion.
     * 
     * Creates a map representation suitable for transmission to Flutter
     * via the platform channel. Handles null device names gracefully.
     * Includes raw advertisement data when available (debug mode).
     * 
     * Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
     * 
     * @return Map containing device information
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "Unknown Device");
        map.put("macAddress", macAddress);
        map.put("rssi", rssi);
        map.put("lastSeen", lastSeenTimestamp);
        
        // Include raw advertisement data if available (debug mode)
        if (rawAdvertisementData != null) {
            // Convert byte array to hex string for Flutter
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawAdvertisementData) {
                hexString.append(String.format("%02X", b));
            }
            map.put("rawAdvertisementData", hexString.toString());
        }
        
        return map;
    }
    
    // ========== Getters ==========
    
    /**
     * Get the Bluetooth device.
     * 
     * @return The Bluetooth device
     */
    public BluetoothDevice getDevice() {
        return device;
    }
    
    /**
     * Get the device MAC address.
     * 
     * @return Device MAC address
     */
    public String getMacAddress() {
        return macAddress;
    }
    
    /**
     * Get the device name.
     * 
     * @return Device name, or null if not available
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the RSSI signal strength.
     * 
     * @return RSSI value in dBm
     */
    public int getRssi() {
        return rssi;
    }
    
    /**
     * Get the last seen timestamp.
     * 
     * @return Timestamp in milliseconds since epoch
     */
    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }
    
    /**
     * Get the raw advertisement data (debug mode only).
     * 
     * @return Raw advertisement data, or null if not available
     */
    public byte[] getRawAdvertisementData() {
        return rawAdvertisementData;
    }
    
    /**
     * Check if debug metadata is available.
     * 
     * @return true if raw advertisement data is available, false otherwise
     */
    public boolean hasDebugMetadata() {
        return rawAdvertisementData != null;
    }
    
    // ========== Object Methods ==========
    
    /**
     * Equality based on MAC address.
     * 
     * Two devices are considered equal if they have the same MAC address,
     * regardless of other properties like name or RSSI.
     * 
     * @param obj Object to compare
     * @return true if MAC addresses match, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ScannedDevice)) return false;
        ScannedDevice other = (ScannedDevice) obj;
        return macAddress.equals(other.macAddress);
    }
    
    /**
     * Hash code based on MAC address.
     * 
     * @return Hash code of the MAC address
     */
    @Override
    public int hashCode() {
        return macAddress.hashCode();
    }
    
    @Override
    public String toString() {
        return "ScannedDevice{" +
                "macAddress='" + macAddress + '\'' +
                ", name='" + name + '\'' +
                ", rssi=" + rssi +
                ", lastSeenTimestamp=" + lastSeenTimestamp +
                '}';
    }
}
