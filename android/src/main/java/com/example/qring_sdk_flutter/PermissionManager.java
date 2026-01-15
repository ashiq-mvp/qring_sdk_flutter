package com.example.qring_sdk_flutter;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * PermissionManager - Handles permission checking for the QRingBackgroundService.
 * 
 * This class provides methods to check required permissions for:
 * - Bluetooth operations (Android 12+ requires BLUETOOTH_CONNECT and BLUETOOTH_SCAN)
 * - Notifications (Android 13+ requires POST_NOTIFICATIONS)
 * 
 * Requirements: 8.2, 8.3, 8.4
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    private final Context context;
    
    /**
     * Constructor for PermissionManager.
     * 
     * @param context Application context
     */
    public PermissionManager(Context context) {
        this.context = context;
    }
    
    /**
     * Checks if all required Bluetooth permissions are granted.
     * For Android 12+ (API 31+), checks BLUETOOTH_CONNECT and BLUETOOTH_SCAN.
     * For older versions, checks legacy BLUETOOTH and BLUETOOTH_ADMIN permissions.
     * 
     * @return true if all required Bluetooth permissions are granted, false otherwise
     * 
     * Requirements: 8.2, 8.3
     */
    public boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) requires new Bluetooth permissions
            boolean hasConnect = checkPermission(Manifest.permission.BLUETOOTH_CONNECT);
            boolean hasScan = checkPermission(Manifest.permission.BLUETOOTH_SCAN);
            
            Log.d(TAG, "Bluetooth permissions (Android 12+): CONNECT=" + hasConnect + ", SCAN=" + hasScan);
            
            return hasConnect && hasScan;
        } else {
            // Older Android versions use legacy Bluetooth permissions
            // These are typically granted at install time
            boolean hasBluetooth = checkPermission(Manifest.permission.BLUETOOTH);
            boolean hasBluetoothAdmin = checkPermission(Manifest.permission.BLUETOOTH_ADMIN);
            
            Log.d(TAG, "Bluetooth permissions (legacy): BLUETOOTH=" + hasBluetooth + ", BLUETOOTH_ADMIN=" + hasBluetoothAdmin);
            
            return hasBluetooth && hasBluetoothAdmin;
        }
    }
    
    /**
     * Checks if the BLUETOOTH_CONNECT permission is granted (Android 12+).
     * This permission is required to connect to paired Bluetooth devices.
     * 
     * @return true if BLUETOOTH_CONNECT permission is granted, false otherwise
     * 
     * Requirements: 8.2
     */
    public boolean checkBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasPermission = checkPermission(Manifest.permission.BLUETOOTH_CONNECT);
            Log.d(TAG, "BLUETOOTH_CONNECT permission: " + hasPermission);
            return hasPermission;
        }
        
        // Permission not required on older Android versions
        return true;
    }
    
    /**
     * Checks if the BLUETOOTH_SCAN permission is granted (Android 12+).
     * This permission is required to discover and scan for Bluetooth devices.
     * 
     * @return true if BLUETOOTH_SCAN permission is granted, false otherwise
     * 
     * Requirements: 8.3
     */
    public boolean checkBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasPermission = checkPermission(Manifest.permission.BLUETOOTH_SCAN);
            Log.d(TAG, "BLUETOOTH_SCAN permission: " + hasPermission);
            return hasPermission;
        }
        
        // Permission not required on older Android versions
        return true;
    }
    
    /**
     * Checks if the POST_NOTIFICATIONS permission is granted (Android 13+).
     * This permission is required to display notifications on Android 13 and above.
     * 
     * @return true if POST_NOTIFICATIONS permission is granted or not required, false otherwise
     * 
     * Requirements: 8.4
     */
    public boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = checkPermission(Manifest.permission.POST_NOTIFICATIONS);
            Log.d(TAG, "POST_NOTIFICATIONS permission: " + hasPermission);
            return hasPermission;
        }
        
        // Permission not required on older Android versions
        Log.d(TAG, "POST_NOTIFICATIONS permission not required on Android < 13");
        return true;
    }
    
    /**
     * Checks if all required permissions for the service are granted.
     * This includes Bluetooth permissions and notification permissions.
     * 
     * @return true if all required permissions are granted, false otherwise
     * 
     * Requirements: 8.2, 8.3, 8.4
     */
    public boolean checkAllRequiredPermissions() {
        boolean bluetoothPermissions = checkBluetoothPermissions();
        boolean notificationPermission = checkNotificationPermission();
        
        boolean allGranted = bluetoothPermissions && notificationPermission;
        
        Log.d(TAG, "All required permissions granted: " + allGranted);
        
        return allGranted;
    }
    
    /**
     * Checks if the ACCESS_FINE_LOCATION permission is granted (Android < 12).
     * This permission is required for BLE scanning on Android versions below 12.
     * 
     * @return true if ACCESS_FINE_LOCATION permission is granted, false otherwise
     * 
     * Requirements: 2.3
     */
    public boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            boolean hasPermission = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.d(TAG, "ACCESS_FINE_LOCATION permission: " + hasPermission);
            return hasPermission;
        }
        
        // Permission not required on Android 12+
        Log.d(TAG, "ACCESS_FINE_LOCATION permission not required on Android 12+");
        return true;
    }
    
    /**
     * Gets a list of missing permissions.
     * Useful for displaying error messages to the user.
     * 
     * @return List of missing permission names, or empty list if all granted
     * 
     * Requirements: 2.7
     */
    public java.util.List<String> getMissingPermissions() {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                missing.add("BLUETOOTH_CONNECT");
            }
            if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                missing.add("BLUETOOTH_SCAN");
            }
        } else {
            // For Android < 12, check location permission
            if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                missing.add("ACCESS_FINE_LOCATION");
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                missing.add("POST_NOTIFICATIONS");
            }
        }
        
        return missing;
    }
    
    /**
     * Generates a user-friendly error message for a missing permission.
     * 
     * @param permission The permission name (e.g., "BLUETOOTH_CONNECT")
     * @return A user-friendly error message explaining why the permission is needed
     * 
     * Requirements: 2.6
     */
    public String getPermissionErrorMessage(String permission) {
        switch (permission) {
            case "BLUETOOTH_CONNECT":
                return "Bluetooth Connect permission is required to connect to your QRing device. Please grant this permission in Settings.";
            case "BLUETOOTH_SCAN":
                return "Bluetooth Scan permission is required to discover your QRing device. Please grant this permission in Settings.";
            case "ACCESS_FINE_LOCATION":
                return "Location permission is required for Bluetooth scanning on this Android version. Please grant this permission in Settings.";
            case "POST_NOTIFICATIONS":
                return "Notification permission is required to show connection status. Please grant this permission in Settings.";
            case "BLUETOOTH":
                return "Bluetooth permission is required to connect to your QRing device. Please grant this permission in Settings.";
            case "BLUETOOTH_ADMIN":
                return "Bluetooth Admin permission is required to manage Bluetooth connections. Please grant this permission in Settings.";
            default:
                return "Permission " + permission + " is required for the app to function properly. Please grant this permission in Settings.";
        }
    }
    
    /**
     * Gets the Android SDK version for testing purposes.
     * 
     * @return The current Android SDK version
     */
    public int getAndroidVersion() {
        return Build.VERSION.SDK_INT;
    }
    
    /**
     * Checks all permissions and returns a map of permission name to granted status.
     * Useful for Flutter integration to check individual permission states.
     * 
     * @return Map of permission names to their granted status
     */
    public java.util.Map<String, Boolean> checkPermissions() {
        java.util.HashMap<String, Boolean> permissions = new java.util.HashMap<>();
        
        // Check Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.put("BLUETOOTH_CONNECT", checkPermission(Manifest.permission.BLUETOOTH_CONNECT));
            permissions.put("BLUETOOTH_SCAN", checkPermission(Manifest.permission.BLUETOOTH_SCAN));
        } else {
            permissions.put("BLUETOOTH", checkPermission(Manifest.permission.BLUETOOTH));
            permissions.put("BLUETOOTH_ADMIN", checkPermission(Manifest.permission.BLUETOOTH_ADMIN));
        }
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.put("POST_NOTIFICATIONS", checkPermission(Manifest.permission.POST_NOTIFICATIONS));
        }
        
        // Check location permission (required for BLE scanning on some Android versions)
        permissions.put("ACCESS_FINE_LOCATION", checkPermission(Manifest.permission.ACCESS_FINE_LOCATION));
        
        return permissions;
    }
    
    /**
     * Helper method to check if a specific permission is granted.
     * 
     * @param permission The permission to check
     * @return true if the permission is granted, false otherwise
     */
    private boolean checkPermission(String permission) {
        try {
            int result = ContextCompat.checkSelfPermission(context, permission);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error checking permission: " + permission, e);
            return false;
        }
    }
}
