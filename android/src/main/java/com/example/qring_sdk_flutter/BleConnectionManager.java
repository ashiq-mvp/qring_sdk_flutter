package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralized BLE Connection Manager with State Machine.
 * 
 * This class serves as the single source of truth for all BLE operations including
 * scan, connect, bond, reconnect, disconnect, and GATT lifecycle management.
 * 
 * Implements a state machine with explicit state validation and observer pattern
 * for state change notifications.
 * 
 * Requirements: 1.2, 1.3, 1.4, 1.5, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 9.1, 9.2
 */
public class BleConnectionManager {
    private static final String TAG = "BleConnectionManager";
    
    // Connection timeout in milliseconds
    private static final long CONNECTION_TIMEOUT_MS = 35000; // 35 seconds (pairing + GATT)
    
    /**
     * BLE State Enum - represents all possible states of the BLE connection manager.
     * 
     * Requirement 1.2: THE BLE_Manager SHALL maintain state using exactly one of these values
     */
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
    
    /**
     * Observer interface for state change notifications.
     * 
     * Requirement 1.4: WHEN state changes occur, THE BLE_Manager SHALL notify all registered observers
     */
    public interface StateObserver {
        /**
         * Called when the BLE state changes.
         * 
         * @param oldState The previous state
         * @param newState The new state
         */
        void onStateChanged(BleState oldState, BleState newState);
    }
    
    /**
     * Callback interface for connection operations.
     */
    public interface ConnectionCallback {
        /**
         * Called when connection succeeds (fully connected with services discovered).
         * 
         * @param device The connected device
         * @param gatt The GATT instance
         */
        void onConnectionSuccess(BluetoothDevice device, BluetoothGatt gatt);
        
        /**
         * Called when connection fails.
         * 
         * @param device The device
         * @param error Error message
         */
        void onConnectionFailed(BluetoothDevice device, String error);
    }
    
    // Singleton instance
    private static BleConnectionManager instance;
    
    // Current state
    private BleState currentState;
    
    // State observers (thread-safe list)
    private final List<StateObserver> observers;
    
    // Context
    private Context context;
    
    // Component managers
    private PairingManager pairingManager;
    private GattConnectionManager gattConnectionManager;
    private ServiceConnectionManager autoReconnectEngine;
    
    // Current connection state
    private BluetoothDevice currentDevice;
    private ConnectionCallback currentConnectionCallback;
    private boolean isManualDisconnect;
    
    // Auto-reconnect state
    private boolean autoReconnectEnabled;
    
    // Device persistence
    private DevicePersistenceModel devicePersistence;
    
    // Handler for timeout management
    private final Handler handler;
    private Runnable connectionTimeoutRunnable;
    
    // Bluetooth state monitoring
    private BluetoothStateReceiver bluetoothStateReceiver;
    private boolean isBluetoothEnabled;
    
    // Error details (when in ERROR state)
    private String errorCode;
    private String errorMessage;
    
    /**
     * Private constructor for singleton pattern.
     */
    private BleConnectionManager() {
        this.currentState = BleState.IDLE;
        this.observers = new CopyOnWriteArrayList<>();
        this.handler = new Handler(Looper.getMainLooper());
        this.isManualDisconnect = false;
        this.autoReconnectEnabled = true; // Enabled by default
        this.isBluetoothEnabled = true; // Assume enabled initially
    }
    
    /**
     * Get the singleton instance of BleConnectionManager.
     * 
     * @return The singleton instance
     */
    public static synchronized BleConnectionManager getInstance() {
        if (instance == null) {
            instance = new BleConnectionManager();
        }
        return instance;
    }
    
    /**
     * Initialize the BLE Connection Manager with application context.
     * 
     * @param context Application context
     */
    public void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        
        // Initialize component managers
        this.pairingManager = PairingManager.getInstance();
        this.pairingManager.initialize(this.context);
        
        this.gattConnectionManager = GattConnectionManager.getInstance();
        this.gattConnectionManager.initialize(this.context);
        
        this.autoReconnectEngine = new ServiceConnectionManager(this.context);
        
        // Register Bluetooth state receiver
        registerBluetoothStateReceiver();
        
        Log.d(TAG, "BleConnectionManager initialized");
    }
    
    /**
     * Check if required permissions are granted before performing BLE operations.
     * 
     * Requirement 10.4: Add permission check before each BLE operation
     * 
     * @return true if all required permissions are granted, false otherwise
     */
    private boolean checkPermissionsBeforeOperation() {
        if (context == null) {
            Log.e(TAG, "Cannot check permissions: context is null");
            return false;
        }
        
        try {
            PermissionManager permissionManager = new PermissionManager(context);
            
            // Check all required Bluetooth permissions
            if (!permissionManager.checkBluetoothPermissions()) {
                List<String> missingPermissions = permissionManager.getMissingPermissions();
                Log.e(TAG, "Missing Bluetooth permissions: " + missingPermissions);
                
                // Requirement 11.2: Report specific permission error
                String errorMessage = "Missing permissions: " + String.join(", ", missingPermissions);
                transitionToError(ErrorCodes.PERMISSION_DENIED, errorMessage);
                
                return false;
            }
            
            return true;
            
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during connection
            Log.e(TAG, "SecurityException while checking permissions", e);
            transitionToError(ErrorCodes.PERMISSION_REVOKED, 
                "Permission was revoked: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception while checking permissions", e);
            return false;
        }
    }
    
    /**
     * Handle SecurityException that occurs during BLE operations.
     * 
     * Requirement 10.4: Implement graceful handling when permissions revoked during connection
     * Requirement 11.2: Report specific permission error
     * 
     * @param operation The operation that failed
     * @param e The SecurityException
     */
    private void handleSecurityException(String operation, SecurityException e) {
        Log.e(TAG, String.format("SecurityException during %s: %s", operation, e.getMessage()), e);
        
        // Determine which permission was revoked based on the exception message
        String errorMessage = String.format("Permission revoked during %s: %s", 
            operation, e.getMessage());
        
        // Requirement 11.2: Report specific permission error
        transitionToError(ErrorCodes.PERMISSION_REVOKED, errorMessage, e);
        
        // If we were connected, disconnect gracefully
        if (isConnected() || currentState == BleState.CONNECTING || 
            currentState == BleState.PAIRING) {
            
            Log.d(TAG, "Disconnecting due to permission revocation");
            
            try {
                // Try to disconnect gracefully, but don't fail if it throws another SecurityException
                gattConnectionManager.disconnect();
                gattConnectionManager.close();
            } catch (SecurityException se) {
                Log.w(TAG, "SecurityException during cleanup after permission revocation", se);
            } catch (Exception ex) {
                Log.w(TAG, "Exception during cleanup after permission revocation", ex);
            }
            
            // Reset connection state
            resetConnectionState();
        }
    }
    
    /**
     * Get the current BLE state.
     * 
     * @return The current state
     */
    public BleState getState() {
        return currentState;
    }
    
    /**
     * Register a state observer to receive state change notifications.
     * 
     * @param observer The observer to register
     */
    public void registerObserver(StateObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            Log.d(TAG, "Observer registered. Total observers: " + observers.size());
        }
    }
    
    /**
     * Unregister a state observer.
     * 
     * @param observer The observer to unregister
     */
    public void unregisterObserver(StateObserver observer) {
        if (observer != null) {
            observers.remove(observer);
            Log.d(TAG, "Observer unregistered. Total observers: " + observers.size());
        }
    }
    
    /**
     * Clear all registered observers.
     */
    public void clearObservers() {
        observers.clear();
        Log.d(TAG, "All observers cleared");
    }
    
    /**
     * Validate if a state transition is allowed.
     * 
     * Requirement 1.3: WHEN any BLE operation is requested, THE BLE_Manager SHALL validate 
     * the current state before proceeding
     * 
     * Requirement 1.5: THE BLE_Manager SHALL prevent concurrent conflicting operations 
     * through state validation
     * 
     * @param from The current state
     * @param to The target state
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransition(BleState from, BleState to) {
        if (from == null || to == null) {
            return false;
        }
        
        // Same state transition is always allowed (no-op)
        if (from == to) {
            return true;
        }
        
        // Define valid state transitions
        switch (from) {
            case IDLE:
                // From IDLE, can start scanning or connecting
                return to == BleState.SCANNING || to == BleState.CONNECTING;
                
            case SCANNING:
                // From SCANNING, can return to IDLE or start connecting
                return to == BleState.IDLE || to == BleState.CONNECTING;
                
            case CONNECTING:
                // From CONNECTING, can move to PAIRING, CONNECTED, DISCONNECTED, or ERROR
                return to == BleState.PAIRING || to == BleState.CONNECTED || 
                       to == BleState.DISCONNECTED || to == BleState.ERROR;
                
            case PAIRING:
                // From PAIRING, can move to CONNECTED, DISCONNECTED, or ERROR
                return to == BleState.CONNECTED || to == BleState.DISCONNECTED || 
                       to == BleState.ERROR;
                
            case CONNECTED:
                // From CONNECTED, can disconnect, reconnect, or error
                return to == BleState.DISCONNECTED || to == BleState.RECONNECTING || 
                       to == BleState.ERROR;
                
            case DISCONNECTED:
                // From DISCONNECTED, can return to IDLE, start connecting, or reconnecting
                return to == BleState.IDLE || to == BleState.CONNECTING || 
                       to == BleState.RECONNECTING;
                
            case RECONNECTING:
                // From RECONNECTING, can connect, disconnect, or error
                return to == BleState.CONNECTING || to == BleState.CONNECTED || 
                       to == BleState.DISCONNECTED || to == BleState.ERROR;
                
            case ERROR:
                // From ERROR, can return to IDLE or DISCONNECTED to allow retry
                return to == BleState.IDLE || to == BleState.DISCONNECTED;
                
            default:
                return false;
        }
    }
    
    /**
     * Transition to a new state with validation and notification.
     * 
     * Requirement 1.3: WHEN any BLE operation is requested, THE BLE_Manager SHALL validate 
     * the current state before proceeding
     * 
     * Requirement 1.4: WHEN state changes occur, THE BLE_Manager SHALL notify all registered 
     * observers of the new state
     * 
     * @param newState The target state
     * @return true if transition was successful, false if invalid
     */
    public boolean transitionTo(BleState newState) {
        if (newState == null) {
            Log.e(TAG, "Cannot transition to null state");
            return false;
        }
        
        BleState oldState = currentState;
        
        // Validate transition
        if (!canTransition(oldState, newState)) {
            Log.e(TAG, String.format("Invalid state transition: %s -> %s", oldState, newState));
            return false;
        }
        
        // Perform transition
        currentState = newState;
        
        // Log the transition
        Log.i(TAG, String.format("State transition: %s -> %s", oldState, newState));
        
        // Clear error details when leaving ERROR state
        if (oldState == BleState.ERROR && newState != BleState.ERROR) {
            errorCode = null;
            errorMessage = null;
        }
        
        // Notify observers
        notifyStateChange(oldState, newState);
        
        return true;
    }
    
    /**
     * Transition to ERROR state with error details.
     * 
     * Requirement 11.1: WHEN any BLE operation fails, THE BLE_Manager SHALL transition to ERROR state with error details
     * Requirement 11.6: THE BLE_Manager SHALL log all errors for debugging purposes
     * 
     * @param code Error code
     * @param message Error message
     * @return true if transition was successful, false if invalid
     */
    public boolean transitionToError(String code, String message) {
        return transitionToError(code, message, null);
    }
    
    /**
     * Transition to ERROR state with error details and optional throwable.
     * 
     * Requirement 11.1: WHEN any BLE operation fails, THE BLE_Manager SHALL transition to ERROR state with error details
     * Requirement 11.6: THE BLE_Manager SHALL log all errors for debugging purposes
     * 
     * @param code Error code
     * @param message Error message
     * @param throwable Optional throwable for stack trace
     * @return true if transition was successful, false if invalid
     */
    public boolean transitionToError(String code, String message, Throwable throwable) {
        this.errorCode = code;
        this.errorMessage = message;
        
        // Requirement 11.6: Log error with full details
        ExceptionHandler.logErrorWithDetails(code, message, "BLE Operation", throwable);
        
        return transitionTo(BleState.ERROR);
    }
    
    /**
     * Get the error code when in ERROR state.
     * 
     * @return The error code, or null if not in ERROR state
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the error message when in ERROR state.
     * 
     * @return The error message, or null if not in ERROR state
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Acknowledge the error and allow retry operations.
     * 
     * This method transitions from ERROR state to IDLE or DISCONNECTED state,
     * allowing the application to retry operations after an error.
     * 
     * Requirement 11.5: WHEN in ERROR state, THE BLE_Manager SHALL allow retry operations 
     * after error is acknowledged
     * 
     * @return true if error was acknowledged and state transitioned, false if not in ERROR state
     */
    public boolean acknowledgeError() {
        if (currentState != BleState.ERROR) {
            Log.w(TAG, "Cannot acknowledge error: not in ERROR state");
            return false;
        }
        
        Log.d(TAG, "Error acknowledged, transitioning to IDLE state to allow retry");
        
        // Clear error details
        String previousErrorCode = errorCode;
        String previousErrorMessage = errorMessage;
        
        // Transition to IDLE to allow retry
        boolean transitioned = transitionTo(BleState.IDLE);
        
        if (transitioned) {
            Log.i(TAG, String.format("Error acknowledged and cleared: [%s] %s", 
                previousErrorCode, previousErrorMessage));
        }
        
        return transitioned;
    }
    
    /**
     * Notify all registered observers of a state change.
     * 
     * Requirement 1.4: WHEN state changes occur, THE BLE_Manager SHALL notify all registered 
     * observers of the new state
     * 
     * @param oldState The previous state
     * @param newState The new state
     */
    private void notifyStateChange(BleState oldState, BleState newState) {
        Log.d(TAG, String.format("Notifying %d observers of state change: %s -> %s", 
                observers.size(), oldState, newState));
        
        // Notify all observers (CopyOnWriteArrayList is thread-safe for iteration)
        for (StateObserver observer : observers) {
            try {
                observer.onStateChanged(oldState, newState);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying observer", e);
            }
        }
    }
    
    /**
     * Validate that the current state allows scanning operations.
     * 
     * @return true if scanning is allowed, false otherwise
     */
    public boolean canStartScan() {
        return currentState == BleState.IDLE;
    }
    
    /**
     * Validate that the current state allows connection operations.
     * 
     * @return true if connection is allowed, false otherwise
     */
    public boolean canConnect() {
        return currentState == BleState.IDLE || currentState == BleState.DISCONNECTED;
    }
    
    /**
     * Validate that the current state allows disconnection operations.
     * 
     * @return true if disconnection is allowed, false otherwise
     */
    public boolean canDisconnect() {
        return currentState == BleState.CONNECTING || currentState == BleState.PAIRING ||
               currentState == BleState.CONNECTED || currentState == BleState.RECONNECTING;
    }
    
    /**
     * Validate that the current state allows command operations (findRing, getBattery, etc.).
     * 
     * @return true if commands are allowed, false otherwise
     */
    public boolean canExecuteCommands() {
        return currentState == BleState.CONNECTED;
    }
    
    /**
     * Check if currently in a connected state.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return currentState == BleState.CONNECTED;
    }
    
    /**
     * Check if currently scanning.
     * 
     * @return true if scanning, false otherwise
     */
    public boolean isScanning() {
        return currentState == BleState.SCANNING;
    }
    
    /**
     * Check if currently in error state.
     * 
     * @return true if in error state, false otherwise
     */
    public boolean isError() {
        return currentState == BleState.ERROR;
    }
    
    /**
     * Check if auto-reconnect is enabled.
     * 
     * Requirement 5.4: Auto-reconnect can be disabled
     * 
     * @return true if auto-reconnect is enabled, false otherwise
     */
    public boolean isAutoReconnectEnabled() {
        return autoReconnectEnabled;
    }
    
    /**
     * Enable auto-reconnect functionality.
     * 
     * When enabled, the manager will automatically attempt to reconnect
     * after unexpected disconnections.
     * 
     * Requirement 5.1: Auto-reconnect on unexpected disconnect
     */
    public void enableAutoReconnect() {
        this.autoReconnectEnabled = true;
        Log.d(TAG, "Auto-reconnect enabled");
    }
    
    /**
     * Disable auto-reconnect functionality.
     * 
     * When disabled, the manager will not attempt to reconnect after disconnections.
     * This is typically called during manual disconnect operations.
     * 
     * Requirement 5.4, 9.3: Disable auto-reconnect on manual disconnect
     */
    public void disableAutoReconnect() {
        this.autoReconnectEnabled = false;
        
        // Stop any ongoing reconnection attempts
        if (autoReconnectEngine != null && autoReconnectEngine.isReconnecting()) {
            autoReconnectEngine.stopAutoReconnect();
        }
        
        Log.d(TAG, "Auto-reconnect disabled");
    }
    
    /**
     * Reset the state machine to IDLE state.
     * This should be used carefully as it bypasses normal state transitions.
     */
    public void reset() {
        BleState oldState = currentState;
        currentState = BleState.IDLE;
        errorCode = null;
        errorMessage = null;
        autoReconnectEnabled = true; // Reset to default
        
        Log.w(TAG, String.format("State machine reset: %s -> IDLE", oldState));
        
        notifyStateChange(oldState, BleState.IDLE);
    }
    
    /**
     * Connect to a BLE device with full workflow.
     * 
     * This implements the complete connection workflow:
     * 1. Check permissions
     * 2. Check bond state
     * 3. Pair if needed
     * 4. Connect GATT with autoConnect=true
     * 5. Discover services
     * 6. Negotiate MTU
     * 
     * Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 10.4
     * 
     * @param device The device to connect to
     * @param callback Callback to receive connection results
     */
    public void connect(BluetoothDevice device, ConnectionCallback callback) {
        if (device == null) {
            Log.e(TAG, "Cannot connect: device is null");
            if (callback != null) {
                callback.onConnectionFailed(null, "Device is null");
            }
            return;
        }
        
        if (callback == null) {
            Log.e(TAG, "Cannot connect: callback is null");
            return;
        }
        
        if (!canConnect()) {
            String error = String.format("Cannot connect from state: %s", currentState);
            Log.e(TAG, error);
            callback.onConnectionFailed(device, error);
            return;
        }
        
        // Requirement 10.4: Add permission check before each BLE operation
        if (!checkPermissionsBeforeOperation()) {
            String error = "Missing required permissions for connection";
            Log.e(TAG, error);
            callback.onConnectionFailed(device, error);
            return;
        }
        
        // Store connection state
        this.currentDevice = device;
        this.currentConnectionCallback = callback;
        this.isManualDisconnect = false;
        
        // Transition to CONNECTING state
        if (!transitionTo(BleState.CONNECTING)) {
            callback.onConnectionFailed(device, "Failed to transition to CONNECTING state");
            return;
        }
        
        // Start connection timeout
        startConnectionTimeout();
        
        try {
            // Requirement 3.1: Check bond state before proceeding
            Log.d(TAG, "Starting connection workflow for device: " + device.getAddress());
            
            int bondState = pairingManager.checkBondState(device);
            
            if (bondState == BluetoothDevice.BOND_BONDED) {
                // Already bonded, proceed directly to GATT connection
                Log.d(TAG, "Device already bonded, proceeding to GATT connection");
                connectGatt(device);
            } else {
                // Requirement 3.2: Trigger pairing if not bonded
                Log.d(TAG, "Device not bonded, starting pairing workflow");
                transitionTo(BleState.PAIRING);
                startPairing(device);
            }
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during connection
            Log.e(TAG, "SecurityException during connection", e);
            handleSecurityException("connect", e);
            callback.onConnectionFailed(device, "Permission revoked during connection");
            cancelConnectionTimeout();
            resetConnectionState();
        } catch (Exception e) {
            Log.e(TAG, "Exception during connection", e);
            transitionToError(ErrorCodes.CONNECTION_FAILED, "Connection failed: " + e.getMessage(), e);
            callback.onConnectionFailed(device, "Connection failed: " + e.getMessage());
            cancelConnectionTimeout();
            resetConnectionState();
        }
    }
    
    /**
     * Start the pairing workflow.
     * 
     * Requirements: 3.2, 3.3, 3.4
     * 
     * @param device The device to pair with
     */
    private void startPairing(BluetoothDevice device) {
        pairingManager.startPairing(device, new PairingManager.PairingCallback() {
            @Override
            public void onPairingSuccess(BluetoothDevice device) {
                Log.d(TAG, "Pairing successful, proceeding to GATT connection");
                
                // Requirement 3.6: Proceed with GATT connection after bonding
                connectGatt(device);
            }
            
            @Override
            public void onPairingFailed(String error) {
                Log.e(TAG, "Pairing failed: " + error);
                
                // Cancel connection timeout
                cancelConnectionTimeout();
                
                // Requirement 11.3: Report pairing error with reason
                transitionToError(ErrorCodes.PAIRING_FAILED, "Pairing failed: " + error);
                
                // Notify callback
                if (currentConnectionCallback != null) {
                    currentConnectionCallback.onConnectionFailed(device, "Pairing failed: " + error);
                }
                
                // Reset connection state
                resetConnectionState();
            }
            
            @Override
            public void onPairingRetry(int attemptNumber) {
                Log.d(TAG, "Pairing retry attempt: " + attemptNumber);
            }
        });
    }
    
    /**
     * Connect to GATT server.
     * 
     * Requirements: 4.1, 4.2, 4.3, 10.4
     * 
     * @param device The device to connect to
     */
    private void connectGatt(BluetoothDevice device) {
        try {
            // Requirement 4.1: Use autoConnect=true
            gattConnectionManager.connect(device, true, new GattConnectionManager.GattCallback() {
                @Override
                public void onConnected(BluetoothDevice device) {
                    Log.d(TAG, "GATT connected, waiting for service discovery");
                    // Service discovery is automatically triggered by GattConnectionManager
                }
                
                @Override
                public void onServicesDiscovered(BluetoothDevice device, BluetoothGatt gatt) {
                    Log.d(TAG, "Services discovered, waiting for MTU negotiation");
                    // MTU negotiation is automatically triggered by GattConnectionManager
                }
                
                @Override
                public void onMtuNegotiated(BluetoothDevice device, int mtu) {
                    Log.d(TAG, String.format("MTU negotiated: %d bytes, connection complete", mtu));
                    
                    // Cancel connection timeout
                    cancelConnectionTimeout();
                    
                    // Transition to CONNECTED state
                    if (transitionTo(BleState.CONNECTED)) {
                        // Requirement 5.3, 12.1, 12.2: Persist device information on successful connection
                        saveDeviceInfo(device);
                        
                        // Notify callback of successful connection
                        if (currentConnectionCallback != null) {
                            BluetoothGatt gatt = gattConnectionManager.getBluetoothGatt();
                            currentConnectionCallback.onConnectionSuccess(device, gatt);
                        }
                    }
                }
                
                @Override
                public void onDisconnected(BluetoothDevice device, boolean wasExpected) {
                    Log.d(TAG, String.format("GATT disconnected (expected: %b)", wasExpected));
                    
                    // Cancel connection timeout
                    cancelConnectionTimeout();
                    
                    if (isManualDisconnect) {
                        // Requirement 9.4: Manual disconnect - transition to DISCONNECTED
                        transitionTo(BleState.DISCONNECTED);
                        
                        // Reset connection state
                        resetConnectionState();
                    } else {
                        // Unexpected disconnect
                        if (autoReconnectEnabled && device != null) {
                            // Requirement 5.1: Enter RECONNECTING state on unexpected disconnect
                            Log.d(TAG, "Unexpected disconnect with auto-reconnect enabled, entering RECONNECTING state");
                            
                            if (transitionTo(BleState.RECONNECTING)) {
                                // Start auto-reconnect with exponential backoff
                                startAutoReconnect(device);
                            } else {
                                // Failed to transition to RECONNECTING, go to DISCONNECTED
                                transitionTo(BleState.DISCONNECTED);
                            }
                        } else {
                            // Auto-reconnect disabled or no device - just disconnect
                            transitionTo(BleState.DISCONNECTED);
                        }
                        
                        // Notify callback if we were in the middle of connecting
                        if (currentConnectionCallback != null) {
                            currentConnectionCallback.onConnectionFailed(device, "Disconnected unexpectedly");
                        }
                        
                        // Reset connection state
                        resetConnectionState();
                    }
                }
                
                @Override
                public void onError(BluetoothDevice device, String operation, int status, String error) {
                    Log.e(TAG, String.format("GATT error during %s: %s (status: %d)", 
                        operation, error, status));
                    
                    // Cancel connection timeout
                    cancelConnectionTimeout();
                    
                    // Requirement 11.4: Report GATT error with operation details
                    String errorMessage = String.format("GATT %s failed: %s (status: %d)", 
                        operation, error, status);
                    transitionToError(ErrorCodes.GATT_ERROR, errorMessage);
                    
                    // Notify callback
                    if (currentConnectionCallback != null) {
                        currentConnectionCallback.onConnectionFailed(device, errorMessage);
                    }
                    
                    // Requirement 4.4: Disconnect on service discovery failure
                    if ("discoverServices".equals(operation)) {
                        gattConnectionManager.disconnect();
                    }
                    
                    // Reset connection state
                    resetConnectionState();
                }
            });
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during connection
            Log.e(TAG, "SecurityException during GATT connection", e);
            handleSecurityException("connectGatt", e);
            
            if (currentConnectionCallback != null) {
                currentConnectionCallback.onConnectionFailed(device, "Permission revoked during GATT connection");
            }
            
            cancelConnectionTimeout();
            resetConnectionState();
        } catch (Exception e) {
            Log.e(TAG, "Exception during GATT connection", e);
            transitionToError(ErrorCodes.CONNECTION_FAILED, "GATT connection failed: " + e.getMessage(), e);
            
            if (currentConnectionCallback != null) {
                currentConnectionCallback.onConnectionFailed(device, "GATT connection failed: " + e.getMessage());
            }
            
            cancelConnectionTimeout();
            resetConnectionState();
        }
    }
    
    /**
     * Disconnect from the current device.
     * 
     * This implements the complete disconnect workflow:
     * 1. Disable auto-reconnect
     * 2. Disconnect GATT
     * 3. Close GATT
     * 4. Clear state
     * 
     * Requirements: 5.4, 9.1, 9.2, 9.3, 10.4, 12.4
     */
    public void disconnect() {
        if (!canDisconnect()) {
            Log.w(TAG, String.format("Cannot disconnect from state: %s", currentState));
            return;
        }
        
        Log.d(TAG, "Starting disconnect workflow");
        
        // Mark as manual disconnect
        this.isManualDisconnect = true;
        
        // Requirement 5.4, 9.3: Disable auto-reconnect on manual disconnect
        disableAutoReconnect();
        
        // Requirement 12.4: Clear persisted device information on manual disconnect
        clearDeviceInfo();
        
        // Cancel any pending operations
        cancelConnectionTimeout();
        
        try {
            // Cancel pairing if in progress
            if (pairingManager.isPairing()) {
                pairingManager.cancelPairing();
            }
            
            // Requirement 9.1: Disconnect GATT
            gattConnectionManager.disconnect();
            
            // Note: GATT close will be called in the onDisconnected callback
            // to ensure proper sequence: disconnect() -> wait for callback -> close()
            
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during disconnection
            Log.w(TAG, "SecurityException during disconnect (permission revoked)", e);
            
            // Even if we get a SecurityException, we should still clean up
            try {
                gattConnectionManager.close();
            } catch (Exception ex) {
                Log.w(TAG, "Exception during cleanup after SecurityException", ex);
            }
            
            // Transition to DISCONNECTED state
            transitionTo(BleState.DISCONNECTED);
            resetConnectionState();
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during disconnect", e);
            
            // Try to clean up anyway
            try {
                gattConnectionManager.close();
            } catch (Exception ex) {
                Log.w(TAG, "Exception during cleanup after exception", ex);
            }
            
            // Transition to DISCONNECTED state
            transitionTo(BleState.DISCONNECTED);
            resetConnectionState();
        }
    }
    
    /**
     * Complete the disconnect workflow by closing GATT and clearing state.
     * 
     * This should be called after disconnect() completes.
     * 
     * Requirement 9.2: Close GATT after disconnect
     */
    private void completeDisconnect() {
        Log.d(TAG, "Completing disconnect workflow");
        
        // Requirement 9.2: Close GATT
        gattConnectionManager.close();
        
        // Reset connection state
        resetConnectionState();
        
        // Transition to DISCONNECTED state
        transitionTo(BleState.DISCONNECTED);
    }
    
    /**
     * Start the connection timeout timer.
     */
    private void startConnectionTimeout() {
        // Cancel any existing timeout
        cancelConnectionTimeout();
        
        // Create new timeout runnable
        connectionTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Connection timeout after " + CONNECTION_TIMEOUT_MS + "ms");
                
                // Transition to ERROR state
                transitionToError(ErrorCodes.CONNECTION_TIMEOUT, 
                    "Connection timeout after " + CONNECTION_TIMEOUT_MS + "ms");
                
                // Notify callback
                if (currentConnectionCallback != null && currentDevice != null) {
                    currentConnectionCallback.onConnectionFailed(currentDevice, "Connection timeout");
                }
                
                // Cancel pairing if in progress
                if (pairingManager.isPairing()) {
                    pairingManager.cancelPairing();
                }
                
                // Disconnect and close GATT
                gattConnectionManager.disconnect();
                gattConnectionManager.close();
                
                // Reset connection state
                resetConnectionState();
            }
        };
        
        // Schedule timeout
        handler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT_MS);
        Log.d(TAG, "Connection timeout scheduled for " + CONNECTION_TIMEOUT_MS + "ms");
    }
    
    /**
     * Cancel the connection timeout timer.
     */
    private void cancelConnectionTimeout() {
        if (connectionTimeoutRunnable != null) {
            handler.removeCallbacks(connectionTimeoutRunnable);
            connectionTimeoutRunnable = null;
            Log.d(TAG, "Connection timeout cancelled");
        }
    }
    
    /**
     * Reset the connection state.
     */
    private void resetConnectionState() {
        this.currentDevice = null;
        this.currentConnectionCallback = null;
        this.isManualDisconnect = false;
    }
    
    /**
     * Save device information to persistence.
     * 
     * Requirements: 5.3, 12.1, 12.2
     * 
     * @param device The device to save
     */
    private void saveDeviceInfo(BluetoothDevice device) {
        if (device == null || context == null) {
            Log.w(TAG, "Cannot save device info: device or context is null");
            return;
        }
        
        try {
            String macAddress = device.getAddress();
            String deviceName = device.getName();
            
            devicePersistence = new DevicePersistenceModel(macAddress, deviceName);
            boolean success = devicePersistence.save(context);
            
            if (success) {
                Log.d(TAG, String.format("Device info persisted: MAC=%s, Name=%s", 
                    macAddress, deviceName));
            } else {
                Log.e(TAG, "Failed to persist device info");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving device info", e);
        }
    }
    
    /**
     * Clear device information from persistence.
     * 
     * Requirement 12.4
     */
    private void clearDeviceInfo() {
        if (context == null) {
            Log.w(TAG, "Cannot clear device info: context is null");
            return;
        }
        
        try {
            boolean success = DevicePersistenceModel.clear(context);
            
            if (success) {
                Log.d(TAG, "Device info cleared from persistence");
                devicePersistence = null;
            } else {
                Log.e(TAG, "Failed to clear device info");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing device info", e);
        }
    }
    
    /**
     * Load device information from persistence.
     * 
     * Requirement 12.3
     * 
     * @return DevicePersistenceModel with loaded data, or null if no data exists
     */
    public DevicePersistenceModel loadDeviceInfo() {
        if (context == null) {
            Log.w(TAG, "Cannot load device info: context is null");
            return null;
        }
        
        try {
            devicePersistence = DevicePersistenceModel.load(context);
            
            if (devicePersistence != null) {
                Log.d(TAG, String.format("Device info loaded: MAC=%s, Name=%s", 
                    devicePersistence.getMacAddress(), devicePersistence.getDeviceName()));
            } else {
                Log.d(TAG, "No saved device info found");
            }
            
            return devicePersistence;
        } catch (Exception e) {
            Log.e(TAG, "Error loading device info", e);
            return null;
        }
    }
    
    /**
     * Get the currently persisted device information.
     * 
     * @return DevicePersistenceModel, or null if no device is persisted
     */
    public DevicePersistenceModel getPersistedDeviceInfo() {
        return devicePersistence;
    }
    
    /**
     * Get a string representation of the current state for debugging.
     * 
     * @return String representation of the state
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BleConnectionManager{");
        sb.append("state=").append(currentState);
        if (currentState == BleState.ERROR) {
            sb.append(", errorCode='").append(errorCode).append('\'');
            sb.append(", errorMessage='").append(errorMessage).append('\'');
        }
        sb.append(", observers=").append(observers.size());
        sb.append(", autoReconnectEnabled=").append(autoReconnectEnabled);
        sb.append('}');
        return sb.toString();
    }
    
    /**
     * Start auto-reconnect process with exponential backoff.
     * 
     * Requirements: 5.1, 5.2, 5.5
     * 
     * @param device The device to reconnect to
     */
    private void startAutoReconnect(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot start auto-reconnect: device is null");
            return;
        }
        
        if (!autoReconnectEnabled) {
            Log.d(TAG, "Auto-reconnect is disabled, skipping");
            return;
        }
        
        if (autoReconnectEngine == null) {
            Log.e(TAG, "Cannot start auto-reconnect: engine not initialized");
            return;
        }
        
        String macAddress = device.getAddress();
        Log.d(TAG, "Starting auto-reconnect for device: " + macAddress);
        
        // Set up reconnection callback
        autoReconnectEngine.connect(macAddress, new ServiceConnectionManager.ConnectionCallback() {
            @Override
            public void onConnected(String deviceMac) {
                Log.d(TAG, "Auto-reconnect successful");
                
                // Requirement 5.5: Restore full GATT setup on reconnection
                handleReconnectionSuccess(device);
            }
            
            @Override
            public void onDisconnected() {
                Log.d(TAG, "Auto-reconnect disconnected");
                
                // If we're still in RECONNECTING state, this is unexpected
                if (currentState == BleState.RECONNECTING) {
                    // Continue reconnection attempts (handled by ServiceConnectionManager)
                    Log.d(TAG, "Continuing reconnection attempts");
                }
            }
            
            @Override
            public void onConnectionFailed(String error) {
                Log.e(TAG, "Auto-reconnect failed: " + error);
                
                // Transition to ERROR state
                transitionToError(ErrorCodes.RECONNECTION_FAILED, 
                    "Auto-reconnect failed: " + error);
            }
            
            @Override
            public void onReconnecting(int attemptNumber) {
                Log.d(TAG, "Auto-reconnect attempt #" + attemptNumber);
                
                // Ensure we're in RECONNECTING state
                if (currentState != BleState.RECONNECTING) {
                    transitionTo(BleState.RECONNECTING);
                }
            }
        });
    }
    
    /**
     * Handle successful reconnection by restoring full GATT setup.
     * 
     * Requirement 5.5: WHEN reconnection succeeds, THE BLE_Manager SHALL restore full 
     * GATT connection including service discovery and MTU negotiation
     * 
     * @param device The reconnected device
     */
    private void handleReconnectionSuccess(BluetoothDevice device) {
        Log.d(TAG, "Handling reconnection success, restoring full GATT setup");
        
        // Transition from RECONNECTING to CONNECTING
        if (!transitionTo(BleState.CONNECTING)) {
            Log.e(TAG, "Failed to transition to CONNECTING state during reconnection");
            return;
        }
        
        // Perform full connection workflow (pairing check, GATT connect, service discovery, MTU)
        connect(device, new ConnectionCallback() {
            @Override
            public void onConnectionSuccess(BluetoothDevice device, BluetoothGatt gatt) {
                Log.d(TAG, "Reconnection complete with full GATT setup");
                
                // Transition to CONNECTED state
                transitionTo(BleState.CONNECTED);
                
                // Re-enable auto-reconnect for future disconnections
                enableAutoReconnect();
            }
            
            @Override
            public void onConnectionFailed(BluetoothDevice device, String error) {
                Log.e(TAG, "Failed to restore GATT setup during reconnection: " + error);
                
                // Transition to ERROR state
                transitionToError(ErrorCodes.RECONNECTION_SETUP_FAILED, 
                    "Failed to restore GATT setup: " + error);
            }
        });
    }
    
    /**
     * Check if Bluetooth is currently enabled.
     * 
     * Requirement 10.2: Monitor Bluetooth state
     * 
     * @return true if Bluetooth is enabled, false otherwise
     */
    public boolean isBluetoothEnabled() {
        return isBluetoothEnabled;
    }
    
    /**
     * Register broadcast receiver for Bluetooth state changes.
     * 
     * Requirement 10.2: THE BLE_Manager SHALL monitor Bluetooth state and pause/resume 
     * reconnection accordingly
     */
    private void registerBluetoothStateReceiver() {
        if (context == null) {
            Log.w(TAG, "Cannot register Bluetooth state receiver: context is null");
            return;
        }
        
        try {
            bluetoothStateReceiver = new BluetoothStateReceiver();
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(bluetoothStateReceiver, filter);
            
            // Check initial Bluetooth state
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                isBluetoothEnabled = adapter.isEnabled();
                Log.d(TAG, "Initial Bluetooth state: " + (isBluetoothEnabled ? "ENABLED" : "DISABLED"));
            }
            
            Log.d(TAG, "Bluetooth state receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering Bluetooth state receiver", e);
        }
    }
    
    /**
     * Unregister broadcast receiver for Bluetooth state changes.
     */
    private void unregisterBluetoothStateReceiver() {
        if (bluetoothStateReceiver != null && context != null) {
            try {
                context.unregisterReceiver(bluetoothStateReceiver);
                Log.d(TAG, "Bluetooth state receiver unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering Bluetooth state receiver", e);
            }
            bluetoothStateReceiver = null;
        }
    }
    
    /**
     * Broadcast receiver for monitoring Bluetooth state changes.
     * 
     * Requirement 10.2: Pause reconnection on Bluetooth OFF, resume on Bluetooth ON
     */
    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth disabled");
                        handleBluetoothDisabled();
                        break;
                        
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth enabled");
                        handleBluetoothEnabled();
                        break;
                        
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth turning off");
                        break;
                        
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth turning on");
                        break;
                }
            }
        }
    }
    
    /**
     * Handle Bluetooth being disabled.
     * 
     * Requirement 10.2: Pause reconnection on Bluetooth OFF
     */
    private void handleBluetoothDisabled() {
        boolean wasEnabled = isBluetoothEnabled;
        isBluetoothEnabled = false;
        
        if (wasEnabled) {
            Log.d(TAG, "Bluetooth disabled, pausing reconnection attempts");
            
            // If we're in RECONNECTING state, pause reconnection
            if (currentState == BleState.RECONNECTING && autoReconnectEngine != null) {
                // The ServiceConnectionManager will automatically pause reconnection
                // when it detects Bluetooth is disabled
                Log.d(TAG, "Reconnection paused due to Bluetooth disabled");
            }
            
            // If we're connected, we'll get a disconnect callback
            // which will handle the state transition
        }
    }
    
    /**
     * Handle Bluetooth being enabled.
     * 
     * Requirement 10.2: Resume reconnection on Bluetooth ON
     */
    private void handleBluetoothEnabled() {
        boolean wasDisabled = !isBluetoothEnabled;
        isBluetoothEnabled = true;
        
        if (wasDisabled) {
            Log.d(TAG, "Bluetooth re-enabled, resuming reconnection if needed");
            
            // If we're in RECONNECTING state, resume reconnection immediately
            if (currentState == BleState.RECONNECTING && autoReconnectEngine != null) {
                Log.d(TAG, "Bluetooth re-enabled, resuming reconnection immediately");
                
                // The ServiceConnectionManager will automatically resume reconnection
                // when it detects Bluetooth is enabled
                
                // Load persisted device info if available
                DevicePersistenceModel persistedDevice = loadDeviceInfo();
                if (persistedDevice != null && persistedDevice.getMacAddress() != null) {
                    String macAddress = persistedDevice.getMacAddress();
                    Log.d(TAG, "Attempting to reconnect to persisted device: " + macAddress);
                    
                    // The auto-reconnect engine will handle the reconnection
                    // through its own Bluetooth state monitoring
                }
            }
        }
    }
    
    /**
     * Callback interface for command operations (findRing, getBattery, etc.).
     */
    public interface CommandCallback {
        /**
         * Called when the command succeeds.
         */
        void onSuccess();
        
        /**
         * Called when the command fails.
         * 
         * @param code Error code
         * @param message Error message
         */
        void onError(String code, String message);
    }
    
    /**
     * Trigger the Find My Ring feature to make the ring vibrate.
     * 
     * This method delegates to the SDK's CommandHandle to execute the find device command.
     * The ring must be in CONNECTED state for this command to work.
     * 
     * Requirement 8.4: THE Flutter_Bridge SHALL provide findMyRing method to trigger ring vibration
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     * 
     * @param callback Callback to handle success or error
     */
    public void findRing(final CommandCallback callback) {
        if (callback == null) {
            Log.e(TAG, "Cannot execute findRing: callback is null");
            return;
        }
        
        if (!canExecuteCommands()) {
            String error = String.format("Cannot execute findRing from state: %s", currentState);
            Log.e(TAG, error);
            callback.onError(ErrorCodes.INVALID_STATE, error);
            return;
        }
        
        // Requirement 10.4: Add permission check before each BLE operation
        if (!checkPermissionsBeforeOperation()) {
            String error = "Missing required permissions for findRing command";
            Log.e(TAG, error);
            callback.onError(ErrorCodes.PERMISSION_DENIED, error);
            return;
        }
        
        Log.d(TAG, "Executing find ring command");
        
        // TODO: Fix SDK integration - CommandHandle and related classes not found in current SDK version
        // The SDK classes com.oudmon.ble.base.bluetooth.CommandHandle and com.oudmon.ble.base.cmd
        // are not available in the current SDK version. This needs to be updated when the correct
        // SDK version is available.
        Log.w(TAG, "Find ring functionality temporarily disabled - SDK integration pending");
        callback.onError(ErrorCodes.UNSUPPORTED_FEATURE, 
            "Find ring functionality temporarily unavailable");
        
        /* Commented out until SDK classes are available:
        try {
            // Import required SDK classes
            com.oudmon.ble.base.bluetooth.CommandHandle commandHandle = 
                com.oudmon.ble.base.bluetooth.CommandHandle.getInstance();
            com.oudmon.ble.base.bluetooth.ICommandResponse<com.oudmon.ble.base.cmd.BaseRspCmd> responseCallback = 
                new com.oudmon.ble.base.bluetooth.ICommandResponse<com.oudmon.ble.base.cmd.BaseRspCmd>() {
                    @Override
                    public void onDataResponse(com.oudmon.ble.base.cmd.BaseRspCmd rsp) {
                        if (rsp.getStatus() == com.oudmon.ble.base.cmd.BaseRspCmd.RESULT_OK) {
                            Log.d(TAG, "Find ring command successful");
                            handler.post(() -> callback.onSuccess());
                        } else {
                            Log.e(TAG, "Find ring command failed with status: " + rsp.getStatus());
                            handler.post(() -> callback.onError(ErrorCodes.COMMAND_FAILED, 
                                "Find ring command failed with status: " + rsp.getStatus()));
                        }
                    }
                    
                    @Override
                    public void onTimeout() {
                        Log.e(TAG, "Find ring command timeout");
                        handler.post(() -> callback.onError(ErrorCodes.TIMEOUT, 
                            "Find ring command timed out"));
                    }
                    
                    @Override
                    public void onFailed(int errCode) {
                        Log.e(TAG, "Find ring command failed with error code: " + errCode);
                        handler.post(() -> callback.onError(ErrorCodes.COMMAND_FAILED, 
                            "Find ring command failed with code: " + errCode));
                    }
                };
            
            commandHandle.executeReqCmd(
                new com.oudmon.ble.base.cmd.FindDeviceReq(),
                responseCallback
            );
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during operation
            Log.e(TAG, "SecurityException executing find ring command", e);
            handleSecurityException("findRing", e);
            callback.onError(ErrorCodes.PERMISSION_REVOKED, 
                "Permission revoked during find ring command: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception executing find ring command", e);
            callback.onError(ErrorCodes.EXCEPTION, 
                "Exception executing find ring command: " + e.getMessage());
        }
        */
    }
    
    /**
     * Get the currently connected device.
     * 
     * @return The currently connected BluetoothDevice, or null if not connected
     */
    public BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }
    
    /**
     * Clean up resources and unregister receivers.
     * Should be called when the BLE manager is no longer needed.
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up BleConnectionManager");
        
        // Cancel any pending operations
        cancelConnectionTimeout();
        
        // Stop auto-reconnect
        if (autoReconnectEngine != null) {
            autoReconnectEngine.stopAutoReconnect();
        }
        
        // Disconnect if connected
        if (isConnected()) {
            disconnect();
        }
        
        // Unregister Bluetooth state receiver
        unregisterBluetoothStateReceiver();
        
        // Clear observers
        clearObservers();
        
        // Clean up component managers
        if (pairingManager != null) {
            // TODO: PairingManager.cleanup() method not available in current implementation
            // pairingManager.cleanup();
            pairingManager = null;
        }
        
        if (gattConnectionManager != null) {
            gattConnectionManager.close();
        }
        
        if (autoReconnectEngine != null) {
            autoReconnectEngine.cleanup();
        }
        
        Log.d(TAG, "BleConnectionManager cleanup complete");
    }
}
