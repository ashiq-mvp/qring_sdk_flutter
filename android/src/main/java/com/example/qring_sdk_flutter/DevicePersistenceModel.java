package com.example.qring_sdk_flutter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Device Persistence Model for storing and retrieving device information.
 * 
 * This class manages the persistence of device connection information using
 * Android SharedPreferences. It stores device MAC address, name, and connection
 * metadata to enable automatic reconnection after app restart or device reboot.
 * 
 * Requirements: 5.3, 12.1, 12.2, 12.3, 12.4
 */
public class DevicePersistenceModel {
    private static final String TAG = "DevicePersistenceModel";
    
    // SharedPreferences file name
    private static final String PREFS_NAME = "qring_device_persistence";
    
    // SharedPreferences keys
    private static final String PREF_DEVICE_MAC = "device_mac";
    private static final String PREF_DEVICE_NAME = "device_name";
    private static final String PREF_LAST_CONNECTED = "last_connected_time";
    private static final String PREF_AUTO_RECONNECT = "auto_reconnect";
    
    // Device information
    private String macAddress;
    private String deviceName;
    private long lastConnectedTime;
    private boolean autoReconnect;
    
    /**
     * Default constructor.
     */
    public DevicePersistenceModel() {
        this.macAddress = null;
        this.deviceName = null;
        this.lastConnectedTime = 0;
        this.autoReconnect = true;
    }
    
    /**
     * Constructor with device information.
     * 
     * @param macAddress Device MAC address
     * @param deviceName Device name
     */
    public DevicePersistenceModel(String macAddress, String deviceName) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.lastConnectedTime = System.currentTimeMillis();
        this.autoReconnect = true;
    }
    
    /**
     * Constructor with full device information.
     * 
     * @param macAddress Device MAC address
     * @param deviceName Device name
     * @param lastConnectedTime Last connection timestamp
     * @param autoReconnect Auto-reconnect flag
     */
    public DevicePersistenceModel(String macAddress, String deviceName, 
                                 long lastConnectedTime, boolean autoReconnect) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.lastConnectedTime = lastConnectedTime;
        this.autoReconnect = autoReconnect;
    }
    
    // ========== Getters and Setters ==========
    
    /**
     * Get the device MAC address.
     * 
     * @return Device MAC address, or null if not set
     */
    public String getMacAddress() {
        return macAddress;
    }
    
    /**
     * Set the device MAC address.
     * 
     * @param macAddress Device MAC address
     */
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    /**
     * Get the device name.
     * 
     * @return Device name, or null if not set
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    /**
     * Set the device name.
     * 
     * @param deviceName Device name
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    /**
     * Get the last connected timestamp.
     * 
     * @return Last connected timestamp in milliseconds
     */
    public long getLastConnectedTime() {
        return lastConnectedTime;
    }
    
    /**
     * Set the last connected timestamp.
     * 
     * @param lastConnectedTime Last connected timestamp in milliseconds
     */
    public void setLastConnectedTime(long lastConnectedTime) {
        this.lastConnectedTime = lastConnectedTime;
    }
    
    /**
     * Check if auto-reconnect is enabled.
     * 
     * @return true if auto-reconnect is enabled, false otherwise
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }
    
    /**
     * Set the auto-reconnect flag.
     * 
     * @param autoReconnect Auto-reconnect flag
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
    
    /**
     * Check if device information is valid (has MAC address).
     * 
     * @return true if MAC address is set, false otherwise
     */
    public boolean isValid() {
        return macAddress != null && !macAddress.isEmpty();
    }
    
    // ========== Persistence Methods ==========
    
    /**
     * Save device information to SharedPreferences.
     * 
     * Requirement 12.1: WHEN a QRing successfully connects, THE BLE_Manager SHALL 
     * persist the device MAC address
     * 
     * Requirement 12.2: WHEN a QRing successfully connects, THE BLE_Manager SHALL 
     * persist the device name
     * 
     * @param context Application context
     * @return true if save was successful, false otherwise
     */
    public boolean save(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot save: context is null");
            return false;
        }
        
        if (!isValid()) {
            Log.e(TAG, "Cannot save: device information is invalid (no MAC address)");
            return false;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Save device information
            editor.putString(PREF_DEVICE_MAC, macAddress);
            editor.putString(PREF_DEVICE_NAME, deviceName);
            editor.putLong(PREF_LAST_CONNECTED, lastConnectedTime);
            editor.putBoolean(PREF_AUTO_RECONNECT, autoReconnect);
            
            // Commit changes
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, String.format("Device info saved: MAC=%s, Name=%s", 
                    macAddress, deviceName));
            } else {
                Log.e(TAG, "Failed to save device info");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving device info", e);
            return false;
        }
    }
    
    /**
     * Load device information from SharedPreferences.
     * 
     * Requirement 12.3: WHEN the app restarts, THE BLE_Manager SHALL load the 
     * last connected device information
     * 
     * @param context Application context
     * @return DevicePersistenceModel with loaded data, or null if no data exists
     */
    public static DevicePersistenceModel load(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot load: context is null");
            return null;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            
            // Load device information
            String macAddress = prefs.getString(PREF_DEVICE_MAC, null);
            
            // If no MAC address is saved, return null
            if (macAddress == null || macAddress.isEmpty()) {
                Log.d(TAG, "No saved device info found");
                return null;
            }
            
            String deviceName = prefs.getString(PREF_DEVICE_NAME, null);
            long lastConnectedTime = prefs.getLong(PREF_LAST_CONNECTED, 0);
            boolean autoReconnect = prefs.getBoolean(PREF_AUTO_RECONNECT, true);
            
            DevicePersistenceModel model = new DevicePersistenceModel(
                macAddress, deviceName, lastConnectedTime, autoReconnect
            );
            
            Log.d(TAG, String.format("Device info loaded: MAC=%s, Name=%s", 
                macAddress, deviceName));
            
            return model;
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading device info", e);
            return null;
        }
    }
    
    /**
     * Clear device information from SharedPreferences.
     * 
     * Requirement 12.4: WHEN manual disconnect is performed, THE BLE_Manager SHALL 
     * clear the persisted device information
     * 
     * @param context Application context
     * @return true if clear was successful, false otherwise
     */
    public static boolean clear(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot clear: context is null");
            return false;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Remove all device information
            editor.remove(PREF_DEVICE_MAC);
            editor.remove(PREF_DEVICE_NAME);
            editor.remove(PREF_LAST_CONNECTED);
            editor.remove(PREF_AUTO_RECONNECT);
            
            // Commit changes
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "Device info cleared");
            } else {
                Log.e(TAG, "Failed to clear device info");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing device info", e);
            return false;
        }
    }
    
    /**
     * Check if device information exists in SharedPreferences.
     * 
     * @param context Application context
     * @return true if device information exists, false otherwise
     */
    public static boolean exists(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String macAddress = prefs.getString(PREF_DEVICE_MAC, null);
            return macAddress != null && !macAddress.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if device info exists", e);
            return false;
        }
    }
    
    // ========== Object Methods ==========
    
    @Override
    public String toString() {
        return "DevicePersistenceModel{" +
                "macAddress='" + macAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", lastConnectedTime=" + lastConnectedTime +
                ", autoReconnect=" + autoReconnect +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DevicePersistenceModel that = (DevicePersistenceModel) o;
        
        if (macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null)
            return false;
        return deviceName != null ? deviceName.equals(that.deviceName) : that.deviceName == null;
    }
    
    @Override
    public int hashCode() {
        int result = macAddress != null ? macAddress.hashCode() : 0;
        result = 31 * result + (deviceName != null ? deviceName.hashCode() : 0);
        return result;
    }
}
