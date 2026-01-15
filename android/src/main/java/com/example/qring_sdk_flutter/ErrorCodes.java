package com.example.qring_sdk_flutter;

/**
 * Centralized error codes and messages for the QRing SDK Flutter plugin.
 * All error codes follow a consistent naming convention and include descriptive messages.
 */
public class ErrorCodes {
    // Connection Errors
    public static final String NOT_CONNECTED = "NOT_CONNECTED";
    public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
    public static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
    public static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";
    public static final String DISCONNECTION_FAILED = "DISCONNECTION_FAILED";
    
    // Parameter Validation Errors
    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String MISSING_ARGUMENT = "MISSING_ARGUMENT";
    public static final String INVALID_RANGE = "INVALID_RANGE";
    public static final String INVALID_FORMAT = "INVALID_FORMAT";
    
    // BLE Operation Errors
    public static final String BLE_NOT_AVAILABLE = "BLE_NOT_AVAILABLE";
    public static final String BLE_NOT_ENABLED = "BLE_NOT_ENABLED";
    public static final String BLUETOOTH_OFF = "BLUETOOTH_OFF";
    public static final String SCAN_FAILED = "SCAN_FAILED";
    public static final String OPERATION_TIMEOUT = "OPERATION_TIMEOUT";
    public static final String OPERATION_FAILED = "OPERATION_FAILED";
    public static final String PAIRING_FAILED = "PAIRING_FAILED";
    public static final String GATT_ERROR = "GATT_ERROR";
    public static final String RECONNECTION_FAILED = "RECONNECTION_FAILED";
    public static final String RECONNECTION_SETUP_FAILED = "RECONNECTION_SETUP_FAILED";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String COMMAND_FAILED = "COMMAND_FAILED";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String EXCEPTION = "EXCEPTION";
    public static final String BLUETOOTH_UNAVAILABLE = "BLUETOOTH_UNAVAILABLE";
    
    // Permission Errors
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String PERMISSION_REVOKED = "PERMISSION_REVOKED";
    public static final String BLUETOOTH_PERMISSION_REQUIRED = "BLUETOOTH_PERMISSION_REQUIRED";
    public static final String LOCATION_PERMISSION_REQUIRED = "LOCATION_PERMISSION_REQUIRED";
    public static final String BLUETOOTH_SCAN_PERMISSION_REQUIRED = "BLUETOOTH_SCAN_PERMISSION_REQUIRED";
    public static final String BLUETOOTH_CONNECT_PERMISSION_REQUIRED = "BLUETOOTH_CONNECT_PERMISSION_REQUIRED";
    
    // Data Sync Errors
    public static final String SYNC_FAILED = "SYNC_FAILED";
    public static final String DATA_PARSE_ERROR = "DATA_PARSE_ERROR";
    public static final String NO_DATA_AVAILABLE = "NO_DATA_AVAILABLE";
    
    // Measurement Errors
    public static final String MEASUREMENT_FAILED = "MEASUREMENT_FAILED";
    public static final String MEASUREMENT_IN_PROGRESS = "MEASUREMENT_IN_PROGRESS";
    public static final String MEASUREMENT_TIMEOUT = "MEASUREMENT_TIMEOUT";
    
    // Firmware Update Errors
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String FILE_NOT_READABLE = "FILE_NOT_READABLE";
    public static final String INVALID_FILE = "INVALID_FILE";
    public static final String UPDATE_IN_PROGRESS = "UPDATE_IN_PROGRESS";
    public static final String UPDATE_FAILED = "UPDATE_FAILED";
    
    // SDK Errors
    public static final String SDK_ERROR = "SDK_ERROR";
    public static final String SDK_NOT_INITIALIZED = "SDK_NOT_INITIALIZED";
    public static final String UNSUPPORTED_FEATURE = "UNSUPPORTED_FEATURE";
    
    // Generic Errors
    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    
    /**
     * Get a human-readable error message for a given error code.
     */
    public static String getMessage(String code) {
        switch (code) {
            // Connection Errors
            case NOT_CONNECTED:
                return "Device is not connected. Please connect to a device first.";
            case CONNECTION_FAILED:
                return "Failed to establish connection with the device.";
            case CONNECTION_TIMEOUT:
                return "Connection attempt timed out. Please try again.";
            case ALREADY_CONNECTED:
                return "Device is already connected.";
            case DISCONNECTION_FAILED:
                return "Failed to disconnect from the device.";
            
            // Parameter Validation Errors
            case INVALID_ARGUMENT:
                return "Invalid argument provided.";
            case MISSING_ARGUMENT:
                return "Required argument is missing.";
            case INVALID_RANGE:
                return "Value is outside the valid range.";
            case INVALID_FORMAT:
                return "Invalid format for the provided value.";
            
            // BLE Operation Errors
            case BLE_NOT_AVAILABLE:
                return "Bluetooth Low Energy is not available on this device.";
            case BLE_NOT_ENABLED:
                return "Bluetooth is not enabled. Please enable Bluetooth.";
            case BLUETOOTH_OFF:
                return "Bluetooth is turned off. Please enable Bluetooth to continue.";
            case SCAN_FAILED:
                return "Failed to start device scanning.";
            case OPERATION_TIMEOUT:
                return "Operation timed out. Please try again.";
            case OPERATION_FAILED:
                return "Operation failed. Please try again.";
            case PAIRING_FAILED:
                return "Failed to pair with the device. Please try again.";
            case GATT_ERROR:
                return "Bluetooth GATT operation failed.";
            case RECONNECTION_FAILED:
                return "Failed to reconnect to the device.";
            case RECONNECTION_SETUP_FAILED:
                return "Failed to restore connection after reconnection.";
            case INVALID_STATE:
                return "Operation not allowed in current state.";
            case COMMAND_FAILED:
                return "Command execution failed.";
            case TIMEOUT:
                return "Operation timed out.";
            case EXCEPTION:
                return "An exception occurred during operation.";
            case BLUETOOTH_UNAVAILABLE:
                return "Bluetooth adapter is not available.";
            
            // Permission Errors
            case PERMISSION_DENIED:
                return "Required permission was denied.";
            case PERMISSION_REVOKED:
                return "Permission was revoked during operation. Please grant the permission again.";
            case BLUETOOTH_PERMISSION_REQUIRED:
                return "Bluetooth permission is required for this operation.";
            case LOCATION_PERMISSION_REQUIRED:
                return "Location permission is required for Bluetooth scanning.";
            case BLUETOOTH_SCAN_PERMISSION_REQUIRED:
                return "Bluetooth scan permission is required (Android 12+).";
            case BLUETOOTH_CONNECT_PERMISSION_REQUIRED:
                return "Bluetooth connect permission is required (Android 12+).";
            
            // Data Sync Errors
            case SYNC_FAILED:
                return "Failed to synchronize data from the device.";
            case DATA_PARSE_ERROR:
                return "Failed to parse data received from the device.";
            case NO_DATA_AVAILABLE:
                return "No data available for the requested period.";
            
            // Measurement Errors
            case MEASUREMENT_FAILED:
                return "Measurement failed. Please try again.";
            case MEASUREMENT_IN_PROGRESS:
                return "A measurement is already in progress.";
            case MEASUREMENT_TIMEOUT:
                return "Measurement timed out. Please try again.";
            
            // Firmware Update Errors
            case FILE_NOT_FOUND:
                return "Firmware file not found at the specified path.";
            case FILE_NOT_READABLE:
                return "Cannot read the firmware file.";
            case INVALID_FILE:
                return "Invalid firmware file format.";
            case UPDATE_IN_PROGRESS:
                return "A firmware update is already in progress.";
            case UPDATE_FAILED:
                return "Firmware update failed.";
            
            // SDK Errors
            case SDK_ERROR:
                return "An error occurred in the native SDK.";
            case SDK_NOT_INITIALIZED:
                return "SDK is not initialized.";
            case UNSUPPORTED_FEATURE:
                return "This feature is not supported by the device.";
            
            // Generic Errors
            case UNKNOWN_ERROR:
                return "An unknown error occurred.";
            case INTERNAL_ERROR:
                return "An internal error occurred.";
            
            default:
                return "An error occurred: " + code;
        }
    }
    
    /**
     * Get a detailed error message with additional context.
     */
    public static String getMessage(String code, String details) {
        String baseMessage = getMessage(code);
        if (details != null && !details.isEmpty()) {
            return baseMessage + " Details: " + details;
        }
        return baseMessage;
    }
}
