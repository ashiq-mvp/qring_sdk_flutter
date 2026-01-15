package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.bluetooth.QCBluetoothCallbackCloneReceiver;
import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.LargeDataHandler;

import java.util.Random;

/**
 * ServiceConnectionManager - Manages BLE connection lifecycle for the background service.
 * 
 * This class handles:
 * - Connection and disconnection to QRing devices
 * - Automatic reconnection with exponential backoff
 * - Bluetooth state monitoring
 * - Connection state callbacks
 * 
 * Requirements: 2.1, 2.2, 7.1, 7.4, 7.5
 */
public class ServiceConnectionManager {
    private static final String TAG = "ServiceConnectionManager";
    
    // Reconnection timing constants
    private static final int INITIAL_RETRY_INTERVAL_MS = 10000; // 10 seconds
    private static final int RETRY_INTERVAL_AFTER_5_FAILURES_MS = 30000; // 30 seconds
    private static final int RETRY_INTERVAL_AFTER_10_FAILURES_MS = 60000; // 60 seconds
    private static final int MAX_RETRY_INTERVAL_MS = 300000; // 5 minutes
    
    private final Context context;
    private final Handler reconnectHandler;
    private final Random random;
    private final PowerManager powerManager;
    
    private String deviceMac;
    private boolean isConnected = false;
    private boolean isReconnecting = false;
    private int reconnectAttempts = 0;
    private int sdkInitializationAttempts = 0;
    private static final int MAX_SDK_INIT_ATTEMPTS = 3;
    private ConnectionCallback connectionCallback;
    private BluetoothStateReceiver bluetoothStateReceiver;
    private boolean isBluetoothEnabled = true;
    private boolean isDeviceIdle = false;
    
    // Connection state receiver for QRing SDK
    private ServiceBluetoothConnectionReceiver connectionReceiver;
    
    /**
     * Callback interface for connection state changes.
     */
    public interface ConnectionCallback {
        void onConnected(String deviceMac);
        void onDisconnected();
        void onConnectionFailed(String error);
        void onReconnecting(int attemptNumber);
    }
    
    /**
     * Constructor.
     * 
     * @param context Application context
     */
    public ServiceConnectionManager(Context context) {
        this.context = context;
        this.reconnectHandler = new Handler(Looper.getMainLooper());
        this.random = new Random();
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        
        // Initialize connection receiver
        this.connectionReceiver = new ServiceBluetoothConnectionReceiver();
        
        // Register Bluetooth state receiver
        registerBluetoothStateReceiver();
        
        // Register Doze mode receiver for monitoring
        registerDozeStateReceiver();
        
        // Check initial Doze mode state
        checkDozeState();
    }
    
    /**
     * Broadcast receiver for handling connection state changes from QRing SDK.
     */
    private class ServiceBluetoothConnectionReceiver extends QCBluetoothCallbackCloneReceiver {
        @Override
        public void connectStatue(BluetoothDevice device, boolean connected) {
            try {
                if (device != null && connected) {
                    Log.d(TAG, "Device connected: " + device.getAddress());
                    handleConnectionSuccess(device.getAddress());
                } else {
                    Log.d(TAG, "Device disconnected");
                    handleDisconnection();
                }
            } catch (Exception e) {
                // Requirement 10.1: Catch exceptions in callback
                Log.e(TAG, "Exception in connectStatue callback", e);
            }
        }

        @Override
        public void onServiceDiscovered() {
            // Initialize LargeDataHandler after service discovery
            Log.d(TAG, "Service discovered, initializing LargeDataHandler");
            try {
                LargeDataHandler.getInstance().initEnable();
            } catch (Exception e) {
                // Requirement 10.1: Catch exceptions during initialization
                Log.e(TAG, "Error initializing LargeDataHandler", e);
            }
        }

        @Override
        public void onCharacteristicChange(String address, String uuid, byte[] data) {
            // Handle characteristic changes if needed
            try {
                // Future implementation
            } catch (Exception e) {
                Log.e(TAG, "Exception in onCharacteristicChange", e);
            }
        }

        @Override
        public void onCharacteristicRead(String uuid, byte[] data) {
            // Handle characteristic reads if needed
            try {
                // Future implementation
            } catch (Exception e) {
                Log.e(TAG, "Exception in onCharacteristicRead", e);
            }
        }
    }
    
    /**
     * Get the connection receiver for registration with LocalBroadcastManager.
     * This should be registered in the service's onCreate method.
     */
    public QCBluetoothCallbackCloneReceiver getConnectionReceiver() {
        return connectionReceiver;
    }
    
    /**
     * Connect to a QRing device by MAC address.
     * 
     * Requirements: 2.1, 2.2, 10.1
     * 
     * @param deviceMac MAC address of the device to connect to
     * @param callback Callback for connection state changes
     */
    public void connect(String deviceMac, ConnectionCallback callback) {
        if (deviceMac == null || deviceMac.isEmpty()) {
            Log.e(TAG, "Invalid device MAC address");
            if (callback != null) {
                callback.onConnectionFailed("Invalid MAC address");
            }
            return;
        }
        
        this.deviceMac = deviceMac;
        this.connectionCallback = callback;
        
        // Stop any ongoing reconnection attempts
        stopAutoReconnect();
        
        Log.d(TAG, "Connecting to device: " + deviceMac);
        
        try {
            // Initialize QRing SDK if needed
            BleOperateManager bleManager = BleOperateManager.getInstance();
            
            // Connect directly to the device
            bleManager.connectDirectly(deviceMac);
            
        } catch (SecurityException e) {
            // Requirement 10.1: Catch SecurityException for permission errors
            Log.e(TAG, "SecurityException during connection - missing Bluetooth permissions", e);
            isConnected = false;
            if (connectionCallback != null) {
                connectionCallback.onConnectionFailed("Permission denied: " + e.getMessage());
            }
        } catch (IllegalStateException e) {
            // Bluetooth adapter might be off or unavailable
            // This could be a critical SDK error
            Log.e(TAG, "IllegalStateException during connection - Bluetooth may be disabled or SDK not initialized", e);
            isConnected = false;
            
            // Check if this is a critical SDK error
            if (e.getMessage() != null && (
                e.getMessage().contains("not initialized") || 
                e.getMessage().contains("SDK") ||
                e.getMessage().contains("BleOperateManager"))) {
                // Requirement 10.2: Attempt SDK reinitialization on critical errors
                handleCriticalSDKError(e.getMessage());
            } else {
                if (connectionCallback != null) {
                    connectionCallback.onConnectionFailed("Bluetooth unavailable: " + e.getMessage());
                }
            }
        } catch (NullPointerException e) {
            // SDK might not be initialized
            Log.e(TAG, "NullPointerException during connection - SDK may not be initialized", e);
            isConnected = false;
            
            // Requirement 10.2: Attempt SDK reinitialization on critical errors
            handleCriticalSDKError("SDK not initialized");
        } catch (Exception e) {
            // Requirement 10.1: Catch generic exceptions
            Log.e(TAG, "Exception during connection", e);
            isConnected = false;
            if (connectionCallback != null) {
                connectionCallback.onConnectionFailed("Connection exception: " + e.getMessage());
            }
        }
    }
    
    /**
     * Disconnect from the currently connected device.
     * 
     * Requirements: 2.2, 10.1
     */
    public void disconnect() {
        if (deviceMac == null) {
            Log.d(TAG, "No device connected, ignoring disconnect request");
            return;
        }
        
        Log.d(TAG, "Disconnecting from device: " + deviceMac);
        
        // Stop reconnection attempts
        stopAutoReconnect();
        
        try {
            // Disconnect using QRing SDK
            BleOperateManager.getInstance().unBindDevice();
        } catch (SecurityException e) {
            // Requirement 10.1: Catch SecurityException
            Log.e(TAG, "SecurityException during disconnection - missing permissions", e);
        } catch (Exception e) {
            // Requirement 10.1: Catch generic exceptions
            Log.e(TAG, "Exception during disconnection", e);
        }
        
        // Clear state
        isConnected = false;
        deviceMac = null;
        
        if (connectionCallback != null) {
            connectionCallback.onDisconnected();
        }
    }
    
    /**
     * Check if a device is currently connected.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Get the currently connected device MAC address.
     * 
     * @return MAC address or null if not connected
     */
    public String getConnectedDeviceMac() {
        return deviceMac;
    }
    
    /**
     * Handle successful connection.
     */
    private void handleConnectionSuccess(String connectedMac) {
        isConnected = true;
        isReconnecting = false;
        reconnectAttempts = 0;
        
        // Stop any pending reconnection attempts
        reconnectHandler.removeCallbacksAndMessages(null);
        
        if (connectionCallback != null) {
            connectionCallback.onConnected(connectedMac);
        }
    }
    
    /**
     * Handle disconnection event.
     */
    private void handleDisconnection() {
        boolean wasConnected = isConnected;
        isConnected = false;
        
        if (connectionCallback != null) {
            connectionCallback.onDisconnected();
        }
        
        // Start automatic reconnection if we were previously connected
        // and we have a device MAC to reconnect to
        if (wasConnected && deviceMac != null && !isReconnecting) {
            Log.d(TAG, "Device disconnected, starting automatic reconnection");
            startAutoReconnect(deviceMac);
        }
    }
    
    /**
     * Start automatic reconnection attempts.
     * 
     * Requirements: 7.1, 7.3
     * 
     * @param deviceMac MAC address of the device to reconnect to
     */
    public void startAutoReconnect(String deviceMac) {
        if (deviceMac == null || deviceMac.isEmpty()) {
            Log.e(TAG, "Cannot start auto-reconnect: invalid device MAC");
            return;
        }
        
        this.deviceMac = deviceMac;
        this.isReconnecting = true;
        this.reconnectAttempts = 0;
        
        Log.d(TAG, "Starting auto-reconnect for device: " + deviceMac);
        
        // Schedule first reconnection attempt
        scheduleReconnect();
    }
    
    /**
     * Stop automatic reconnection attempts.
     */
    public void stopAutoReconnect() {
        if (!isReconnecting) {
            return;
        }
        
        Log.d(TAG, "Stopping auto-reconnect");
        isReconnecting = false;
        reconnectAttempts = 0;
        reconnectHandler.removeCallbacksAndMessages(null);
    }
    
    /**
     * Schedule the next reconnection attempt.
     */
    private void scheduleReconnect() {
        if (!isReconnecting) {
            return;
        }
        
        // Don't reconnect if Bluetooth is disabled
        if (!isBluetoothEnabled) {
            Log.d(TAG, "Bluetooth disabled, pausing reconnection attempts");
            return;
        }
        
        reconnectAttempts++;
        
        // Calculate delay with exponential backoff
        int delay = calculateBackoffDelay(reconnectAttempts);
        
        Log.d(TAG, "Scheduling reconnection attempt #" + reconnectAttempts + " in " + delay + "ms");
        
        if (connectionCallback != null) {
            connectionCallback.onReconnecting(reconnectAttempts);
        }
        
        reconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                attemptReconnect();
            }
        }, delay);
    }
    
    /**
     * Attempt to reconnect to the device.
     */
    private void attemptReconnect() {
        if (!isReconnecting || deviceMac == null) {
            return;
        }
        
        // Don't reconnect if Bluetooth is disabled
        if (!isBluetoothEnabled) {
            Log.d(TAG, "Bluetooth disabled, skipping reconnection attempt");
            return;
        }
        
        Log.d(TAG, "Attempting reconnection #" + reconnectAttempts + " to device: " + deviceMac);
        
        try {
            BleOperateManager.getInstance().connectDirectly(deviceMac);
            
            // Schedule next attempt in case this one fails
            // The connection receiver will stop reconnection if successful
            reconnectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isReconnecting && !isConnected) {
                        scheduleReconnect();
                    }
                }
            }, 5000); // Wait 5 seconds to see if connection succeeds
            
        } catch (SecurityException e) {
            // Requirement 10.1: Catch SecurityException
            Log.e(TAG, "SecurityException during reconnection attempt - missing permissions", e);
            // Schedule next attempt
            scheduleReconnect();
        } catch (IllegalStateException e) {
            // Bluetooth adapter might be off
            Log.e(TAG, "IllegalStateException during reconnection attempt - Bluetooth may be disabled", e);
            // Schedule next attempt
            scheduleReconnect();
        } catch (Exception e) {
            // Requirement 10.1: Catch generic exceptions
            Log.e(TAG, "Exception during reconnection attempt", e);
            // Schedule next attempt
            scheduleReconnect();
        }
    }
    
    /**
     * Calculate the backoff delay for reconnection attempts.
     * Uses exponential backoff with jitter.
     * 
     * Requirements: 7.2, 9.2
     * 
     * @param attemptNumber The current attempt number (1-based)
     * @return Delay in milliseconds
     */
    public int calculateBackoffDelay(int attemptNumber) {
        int baseDelay;
        
        if (attemptNumber <= 5) {
            // First 5 attempts: 10 seconds
            baseDelay = INITIAL_RETRY_INTERVAL_MS;
        } else if (attemptNumber <= 10) {
            // Attempts 6-10: 30 seconds
            baseDelay = RETRY_INTERVAL_AFTER_5_FAILURES_MS;
        } else {
            // Attempts 11+: 60 seconds, increasing exponentially
            int exponentialDelay = RETRY_INTERVAL_AFTER_10_FAILURES_MS * (1 << (attemptNumber - 11));
            baseDelay = Math.min(exponentialDelay, MAX_RETRY_INTERVAL_MS);
        }
        
        // Requirement 9.2: Add jitter (±20%) to prevent thundering herd
        // This prevents synchronized reconnections from multiple devices
        int jitter = (int) (baseDelay * 0.2 * (random.nextDouble() - 0.5) * 2);
        int finalDelay = baseDelay + jitter;
        
        // Requirement 9.3: During Doze mode, foreground service continues
        // but we can optionally increase delays to be more battery-friendly
        // Note: Foreground services are exempt from Doze restrictions
        if (isDeviceIdle && attemptNumber > 5) {
            // Slightly increase delay during Doze mode for better battery life
            finalDelay = (int) (finalDelay * 1.2);
            Log.d(TAG, "Device in Doze mode, increasing reconnection delay by 20%");
        }
        
        // Ensure delay is positive and within bounds
        return Math.max(1000, Math.min(finalDelay, MAX_RETRY_INTERVAL_MS));
    }
    
    /**
     * Register broadcast receiver for Bluetooth state changes.
     * 
     * Requirements: 7.4, 7.5
     */
    private void registerBluetoothStateReceiver() {
        bluetoothStateReceiver = new BluetoothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(bluetoothStateReceiver, filter);
        
        // Check initial Bluetooth state
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            isBluetoothEnabled = adapter.isEnabled();
        }
    }
    
    /**
     * Unregister broadcast receiver for Bluetooth state changes.
     */
    private void unregisterBluetoothStateReceiver() {
        if (bluetoothStateReceiver != null) {
            try {
                context.unregisterReceiver(bluetoothStateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering Bluetooth state receiver", e);
            }
            bluetoothStateReceiver = null;
        }
    }
    
    /**
     * Broadcast receiver for monitoring Bluetooth state changes.
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
                        isBluetoothEnabled = false;
                        // Pause reconnection attempts
                        if (isReconnecting) {
                            reconnectHandler.removeCallbacksAndMessages(null);
                            Log.d(TAG, "Reconnection paused due to Bluetooth disabled");
                        }
                        break;
                        
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth enabled");
                        boolean wasDisabled = !isBluetoothEnabled;
                        isBluetoothEnabled = true;
                        
                        // Resume reconnection immediately if it was paused
                        if (wasDisabled && isReconnecting && deviceMac != null) {
                            Log.d(TAG, "Bluetooth re-enabled, resuming reconnection immediately");
                            reconnectHandler.removeCallbacksAndMessages(null);
                            reconnectHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    attemptReconnect();
                                }
                            });
                        }
                        break;
                }
            }
        }
    }
    
    /**
     * Get the current reconnection attempt count.
     * 
     * @return Number of reconnection attempts
     */
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
    
    /**
     * Check if currently attempting to reconnect.
     * 
     * @return true if reconnecting, false otherwise
     */
    public boolean isReconnecting() {
        return isReconnecting;
    }
    
    /**
     * Register broadcast receiver for Doze mode state changes.
     * Monitors when device enters/exits idle (Doze) mode.
     * 
     * Requirements: 9.3
     */
    private void registerDozeStateReceiver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                IntentFilter filter = new IntentFilter();
                filter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
                context.registerReceiver(new DozeStateReceiver(), filter);
                Log.d(TAG, "Doze state receiver registered");
            } catch (Exception e) {
                Log.e(TAG, "Error registering Doze state receiver", e);
            }
        }
    }
    
    /**
     * Check the current Doze mode state.
     * 
     * Requirements: 9.3
     */
    private void checkDozeState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager != null) {
            try {
                isDeviceIdle = powerManager.isDeviceIdleMode();
                Log.d(TAG, "Initial Doze state: " + (isDeviceIdle ? "IDLE" : "ACTIVE"));
            } catch (Exception e) {
                Log.e(TAG, "Error checking Doze state", e);
                isDeviceIdle = false;
            }
        }
    }
    
    /**
     * Broadcast receiver for monitoring Doze mode state changes.
     * 
     * Requirements: 9.3
     */
    private class DozeStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String action = intent.getAction();
                
                if (PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED.equals(action)) {
                    if (powerManager != null) {
                        boolean wasIdle = isDeviceIdle;
                        isDeviceIdle = powerManager.isDeviceIdleMode();
                        
                        if (wasIdle != isDeviceIdle) {
                            Log.d(TAG, "Doze mode changed: " + (isDeviceIdle ? "ENTERED" : "EXITED"));
                            
                            // Requirement 9.3: Foreground service continues during Doze
                            // We don't pause reconnection, but we adjust delays in calculateBackoffDelay
                            if (isDeviceIdle) {
                                Log.d(TAG, "Device entered Doze mode - foreground service continues with adjusted delays");
                            } else {
                                Log.d(TAG, "Device exited Doze mode - normal reconnection delays resumed");
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Clean up resources.
     * Should be called when the service is destroyed.
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up ServiceConnectionManager");
        
        stopAutoReconnect();
        unregisterBluetoothStateReceiver();
        
        if (isConnected) {
            disconnect();
        }
        
        connectionCallback = null;
    }
    
    /**
     * Reinitialize the QRing SDK after a critical error.
     * Attempts to reinitialize up to MAX_SDK_INIT_ATTEMPTS times.
     * 
     * Requirements: 10.2
     * 
     * @return true if reinitialization succeeded, false otherwise
     */
    private boolean reinitializeSDK() {
        if (sdkInitializationAttempts >= MAX_SDK_INIT_ATTEMPTS) {
            Log.e(TAG, "Maximum SDK reinitialization attempts reached");
            return false;
        }
        
        sdkInitializationAttempts++;
        Log.d(TAG, "Attempting SDK reinitialization (attempt " + sdkInitializationAttempts + ")");
        
        try {
            // Disconnect any existing connections
            try {
                BleOperateManager.getInstance().unBindDevice();
            } catch (Exception e) {
                Log.w(TAG, "Error unbinding device during reinitialization", e);
            }
            
            // Wait a moment before reinitializing
            Thread.sleep(1000);
            
            // Reinitialize BleOperateManager
            BleOperateManager bleManager = BleOperateManager.getInstance();
            
            // Reinitialize CommandHandle
            CommandHandle.getInstance();
            
            // Reinitialize LargeDataHandler
            LargeDataHandler.getInstance().initEnable();
            
            Log.d(TAG, "SDK reinitialization successful");
            return true;
            
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted during SDK reinitialization", e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception during SDK reinitialization", e);
            return false;
        }
    }
    
    /**
     * Handle a critical SDK error by attempting reinitialization.
     * If reinitialization succeeds, retry the connection.
     * 
     * Requirements: 10.2
     * 
     * @param errorMessage The error message from the critical error
     */
    private void handleCriticalSDKError(String errorMessage) {
        Log.e(TAG, "Critical SDK error detected: " + errorMessage);
        
        // Attempt to reinitialize SDK
        boolean reinitSuccess = reinitializeSDK();
        
        if (reinitSuccess) {
            Log.d(TAG, "SDK reinitialized successfully, retrying connection");
            
            // Reset SDK initialization attempts on success
            sdkInitializationAttempts = 0;
            
            // Retry connection if we have a device MAC
            if (deviceMac != null && !deviceMac.isEmpty()) {
                // Schedule reconnection after a short delay
                reconnectHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isConnected && deviceMac != null) {
                            attemptReconnect();
                        }
                    }
                }, 2000);
            }
        } else {
            Log.e(TAG, "SDK reinitialization failed");
            
            // Notify callback of failure
            if (connectionCallback != null) {
                connectionCallback.onConnectionFailed("SDK reinitialization failed");
            }
        }
    }
}
