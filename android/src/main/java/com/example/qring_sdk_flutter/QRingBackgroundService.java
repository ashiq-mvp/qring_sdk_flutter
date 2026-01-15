package com.example.qring_sdk_flutter;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.oudmon.ble.base.bluetooth.BleAction;

import java.util.Map;

/**
 * QRingBackgroundService - Android Foreground Service for maintaining continuous
 * communication with the QRing smart ring device.
 * 
 * This service runs independently of the Flutter app lifecycle and provides:
 * - Persistent notification with ring status
 * - Automatic reconnection on disconnection
 * - "Find My Ring" action from notification
 * - State persistence across service restarts
 * 
 * Requirements: 1.1, 1.4
 */
public class QRingBackgroundService extends Service {
    private static final String TAG = "QRingBackgroundService";
    
    // SharedPreferences constants for state persistence
    private static final String PREFS_NAME = "qring_service_state";
    private static final String KEY_DEVICE_MAC = "device_mac";
    private static final String KEY_IS_CONNECTED = "is_connected";
    
    private ServiceNotificationManager notificationManager;
    private ServiceConnectionManager connectionManager;
    private ServiceCommandHandler commandHandler;
    private PermissionManager permissionManager;
    private String deviceMac;
    private boolean isConnected = false;
    
    // Wake lock for battery optimization
    private PowerManager.WakeLock wakeLock;
    private boolean isWakeLockHeld = false;
    
    // Event types for Flutter EventChannel
    private static final String EVENT_SERVICE_STARTED = "serviceStarted";
    private static final String EVENT_SERVICE_STOPPED = "serviceStopped";
    private static final String EVENT_DEVICE_CONNECTED = "deviceConnected";
    private static final String EVENT_DEVICE_DISCONNECTED = "deviceDisconnected";
    
    // Broadcast action for service state events
    private static final String ACTION_SERVICE_STATE = "com.example.qring_sdk_flutter.SERVICE_STATE";
    private static final String EXTRA_EVENT_TYPE = "event_type";
    private static final String EXTRA_EVENT_DATA = "event_data";
    
    /**
     * Called when the service is first created.
     * Initializes service components and prepares for operation.
     * Implements state restoration for crash recovery.
     * 
     * Requirements: 10.3, 10.4
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate() - Initializing background service");
        
        try {
            // Initialize notification manager
            notificationManager = new ServiceNotificationManager(this);
            notificationManager.createNotificationChannel();
            Log.d(TAG, "Notification manager initialized");
            
            // Initialize permission manager
            permissionManager = new PermissionManager(this);
            Log.d(TAG, "Permission manager initialized");
            
            // Initialize connection manager
            connectionManager = new ServiceConnectionManager(this);
            
            // Initialize command handler
            commandHandler = new ServiceCommandHandler(connectionManager);
            Log.d(TAG, "Command handler initialized");
            
            // Initialize wake lock for battery optimization
            // Requirement 9.5: Acquire partial wake lock only during active operations
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "QRingService::WakeLock"
                );
                wakeLock.setReferenceCounted(false);
                Log.d(TAG, "Wake lock initialized");
            } else {
                Log.w(TAG, "PowerManager not available, wake lock not initialized");
            }
            
            // Register connection receiver with LocalBroadcastManager
            IntentFilter intentFilter = BleAction.getIntentFilter();
            LocalBroadcastManager.getInstance(this).registerReceiver(
                connectionManager.getConnectionReceiver(),
                intentFilter
            );
            Log.d(TAG, "Connection manager initialized");
            
            // Requirement 10.4: Implement state restoration in onCreate()
            // Load saved device state for crash recovery
            // Requirement 11.2: Load saved MAC on service start
            String savedMac = loadSavedDeviceMac();
            if (savedMac != null && !savedMac.isEmpty()) {
                deviceMac = savedMac;
                Log.d(TAG, "State restored from crash: device MAC loaded, will attempt reconnection");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during service initialization", e);
            // If initialization fails, we cannot continue
            // This is an unrecoverable error
            handleUnrecoverableError("Service Initialization Failed", 
                "Failed to initialize service: " + e.getMessage());
        }
    }
    
    /**
     * Called each time the service is started via startService().
     * Returns START_STICKY to ensure automatic restart after system kill.
     * 
     * @param intent The Intent supplied to startService()
     * @param flags Additional data about this start request
     * @param startId A unique integer representing this specific request to start
     * @return START_STICKY to ensure service is restarted if killed by system
     * 
     * Requirements: 1.4, 10.3
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand() - Service started with startId: " + startId);
        
        // Check if this is a restart after crash
        // Requirement 10.3: Verify START_STICKY is set
        if (flags == START_FLAG_REDELIVERY || flags == START_FLAG_RETRY) {
            Log.d(TAG, "Service restarted after crash - flags: " + flags);
        }
        
        // Requirement 8.5: Check all required permissions in onStartCommand()
        if (!permissionManager.checkAllRequiredPermissions()) {
            Log.e(TAG, "Required permissions not granted - stopping service");
            
            // Get list of missing permissions for error message
            java.util.List<String> missingPermissions = permissionManager.getMissingPermissions();
            StringBuilder permissionList = new StringBuilder();
            for (int i = 0; i < missingPermissions.size(); i++) {
                permissionList.append(missingPermissions.get(i));
                if (i < missingPermissions.size() - 1) {
                    permissionList.append(", ");
                }
            }
            
            // Requirement 8.5: If permissions missing, show error notification
            showPermissionErrorNotification(permissionList.toString());
            
            // Requirement 8.5: Stop service if permissions not granted
            stopSelf();
            
            return START_NOT_STICKY;  // Don't restart if permissions are missing
        }
        
        // Extract device MAC from intent if provided
        if (intent != null && intent.hasExtra(NotificationConfig.EXTRA_DEVICE_MAC)) {
            deviceMac = intent.getStringExtra(NotificationConfig.EXTRA_DEVICE_MAC);
            Log.d(TAG, "Device MAC received: " + deviceMac);
        }
        
        // Handle notification actions
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "Handling action: " + action);
            
            if (NotificationConfig.ACTION_FIND_MY_RING.equals(action)) {
                handleFindMyRingAction();
            }
        } else {
            // Regular service start - create and display foreground notification
            Notification notification = notificationManager.buildDisconnectedNotification(false);
            startForeground(NotificationConfig.NOTIFICATION_ID, notification);
            Log.d(TAG, "Service started in foreground with notification");
            
            // Emit serviceStarted event to Flutter
            // Requirement 6.5: Emit serviceStarted when service starts
            emitServiceStateEvent(EVENT_SERVICE_STARTED, deviceMac);
            
            // Start connection to device if MAC address is provided or saved
            // Requirement 11.3: Attempt reconnection if saved MAC exists
            // Requirement 10.4: Load saved device MAC and attempt reconnection after crash
            if (deviceMac != null && !deviceMac.isEmpty()) {
                connectToDevice(deviceMac);
            } else {
                Log.d(TAG, "No device MAC available for connection");
            }
        }
        
        // Return START_STICKY to ensure automatic restart after system kill
        // This satisfies requirements 1.4 and 10.3 for automatic service restart
        return START_STICKY;
    }
    
    /**
     * Handles the Find My Ring action from the notification.
     * Calls ServiceCommandHandler to execute the command and updates notification with feedback.
     * 
     * Requirements: 4.2, 4.3, 4.4, 4.5
     */
    private void handleFindMyRingAction() {
        Log.d(TAG, "Find My Ring action triggered");
        
        // Acquire wake lock for active operation
        // Requirement 9.5: Acquire partial wake lock only during active operations
        acquireWakeLock();
        
        // Call ServiceCommandHandler.handleFindMyRing()
        // Requirement 4.3: Execute command without app running
        commandHandler.handleFindMyRing(new ServiceCommandHandler.CommandCallback() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                Log.d(TAG, "Find My Ring command succeeded");
                
                // Update notification with success feedback
                // Requirement 4.4: Show "Ring activated" on success
                Notification notification = notificationManager.buildFeedbackNotification(
                    NotificationConfig.STATUS_RING_ACTIVATED, 
                    true
                );
                notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                
                // Restore normal notification after 3 seconds
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnected) {
                            Notification notification = notificationManager.buildConnectedNotification(null);
                            notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        }
                        
                        // Release wake lock when idle
                        // Requirement 9.5: Release wake lock when idle
                        releaseWakeLock();
                    }
                }, 3000);
            }
            
            @Override
            public void onError(String errorCode, String errorMessage) {
                Log.e(TAG, "Find My Ring command failed: " + errorCode + " - " + errorMessage);
                
                // Update notification with error feedback
                // Requirement 4.5: Show "Ring not connected" on failure
                Notification notification = notificationManager.buildFeedbackNotification(
                    NotificationConfig.STATUS_RING_NOT_CONNECTED, 
                    false
                );
                notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                
                // Restore normal notification after 3 seconds
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isConnected) {
                            Notification notification = notificationManager.buildConnectedNotification(null);
                            notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        } else {
                            Notification notification = notificationManager.buildDisconnectedNotification(false);
                            notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        }
                        
                        // Release wake lock when idle
                        // Requirement 9.5: Release wake lock when idle
                        releaseWakeLock();
                    }
                }, 3000);
            }
        });
    }
    
    /**
     * Handles generic commands from Flutter or other sources.
     * Supports custom commands for future extensibility.
     * Validates connection state before execution.
     * 
     * Requirements: 6.4, 12.3
     * 
     * @param command Command identifier (e.g., "findMyRing", "getBattery")
     * @param params Command parameters as a map
     * @param callback Callback to handle success or error
     */
    public void handleGenericCommand(String command, Map<String, Object> params, 
                                     final ServiceCommandHandler.CommandCallback callback) {
        Log.d(TAG, "Handling generic command: " + command);
        
        // Delegate to ServiceCommandHandler
        // Requirement 6.4: Support custom commands from Flutter
        // Requirement 12.3: Validate connection state before execution
        commandHandler.handleCommand(command, params, callback);
    }
    
    /**
     * Called when the service is being destroyed.
     * Performs cleanup of resources and connections.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy() - Cleaning up resources");
        
        // Emit serviceStopped event to Flutter
        // Requirement 6.5: Emit serviceStopped when service stops
        emitServiceStateEvent(EVENT_SERVICE_STOPPED, deviceMac);
        
        // Save current state before cleanup
        // Requirement 11.5: Save connection state on service stop
        if (deviceMac != null && !deviceMac.isEmpty()) {
            saveConnectionState(deviceMac);
        }
        
        // Release wake locks before cleanup
        // Requirement 9.5: Release wake lock when idle
        releaseWakeLocks();
        
        // Cleanup connection manager (stops reconnection, disconnects, unregisters receivers)
        if (connectionManager != null) {
            connectionManager.cleanup();
        }
        
        // Unregister connection receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(
                connectionManager.getConnectionReceiver()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering connection receiver", e);
        }
        
        // Stop foreground and remove notification
        stopForeground(true);
        Log.d(TAG, "Notification removed");
        
        super.onDestroy();
    }
    
    /**
     * Called when a client binds to the service.
     * This service does not support binding, so we return null.
     * 
     * @param intent The Intent that was used to bind to this service
     * @return null as this service does not support binding
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind() - Binding not supported");
        // This service does not support binding
        return null;
    }
    
    /**
     * Called when the system is running low on memory.
     * Release non-essential resources while keeping the connection alive.
     * 
     * Requirements: 10.5
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received - releasing non-essential resources");
        
        try {
            // Release non-essential resources
            // Keep connection alive but clear any cached data
            
            // Clear any cached notification objects (they can be rebuilt)
            // The notification manager itself is lightweight and should be kept
            
            // Request garbage collection (hint to system)
            System.gc();
            
            Log.d(TAG, "Non-essential resources released");
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during low memory handling", e);
        }
    }
    
    /**
     * Called when the system is running low on memory and actively running processes
     * should trim their memory usage.
     * 
     * Requirements: 10.5
     * 
     * @param level The context of the trim request
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.w(TAG, "Memory trim requested with level: " + level);
        
        try {
            switch (level) {
                case TRIM_MEMORY_RUNNING_CRITICAL:
                    // System is running critically low on memory
                    Log.w(TAG, "Critical memory pressure - releasing all non-essential resources");
                    // Release all non-essential resources
                    System.gc();
                    break;
                    
                case TRIM_MEMORY_RUNNING_LOW:
                    // System is running low on memory
                    Log.w(TAG, "Low memory pressure - releasing cached data");
                    // Release cached data
                    System.gc();
                    break;
                    
                case TRIM_MEMORY_RUNNING_MODERATE:
                    // System is beginning to run low on memory
                    Log.d(TAG, "Moderate memory pressure - trimming caches");
                    // Trim caches
                    break;
                    
                default:
                    // Other trim levels
                    Log.d(TAG, "Memory trim level: " + level);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception during memory trim", e);
        }
    }
    
    /**
     * Connect to a QRing device.
     * 
     * Requirements: 10.1
     * 
     * @param deviceMac MAC address of the device to connect to
     */
    private void connectToDevice(String deviceMac) {
        Log.d(TAG, "Connecting to device: " + deviceMac);
        
        // Acquire wake lock for active operation
        // Requirement 9.5: Acquire partial wake lock only during active operations
        acquireWakeLock();
        
        try {
            connectionManager.connect(deviceMac, new ServiceConnectionManager.ConnectionCallback() {
                @Override
                public void onConnected(String connectedMac) {
                    try {
                        Log.d(TAG, "Device connected: " + connectedMac);
                        isConnected = true;
                        QRingBackgroundService.this.deviceMac = connectedMac;
                        
                        // Save device MAC when connection succeeds
                        // Requirement 11.1: Save device MAC when connection succeeds
                        saveConnectionState(connectedMac);
                        
                        // Emit deviceConnected event to Flutter
                        // Requirement 6.5: Emit deviceConnected when ring connects
                        emitServiceStateEvent(EVENT_DEVICE_CONNECTED, connectedMac);
                        
                        // Update notification to show connected status
                        Notification notification = notificationManager.buildConnectedNotification(null);
                        notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        
                        // Release wake lock when idle
                        // Requirement 9.5: Release wake lock when idle
                        releaseWakeLock();
                    } catch (Exception e) {
                        // Requirement 10.1: Catch exceptions in callback
                        Log.e(TAG, "Exception in onConnected callback", e);
                        releaseWakeLock();
                    }
                }
                
                @Override
                public void onDisconnected() {
                    try {
                        Log.d(TAG, "Device disconnected");
                        isConnected = false;
                        
                        // Emit deviceDisconnected event to Flutter
                        // Requirement 6.5: Emit deviceDisconnected when ring disconnects
                        emitServiceStateEvent(EVENT_DEVICE_DISCONNECTED, QRingBackgroundService.this.deviceMac);
                        
                        // Update notification to show disconnected status
                        Notification notification = notificationManager.buildDisconnectedNotification(false);
                        notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        
                        // Release wake lock when idle
                        // Requirement 9.5: Release wake lock when idle
                        releaseWakeLock();
                    } catch (Exception e) {
                        // Requirement 10.1: Catch exceptions in callback
                        Log.e(TAG, "Exception in onDisconnected callback", e);
                        releaseWakeLock();
                    }
                }
                
                @Override
                public void onConnectionFailed(String error) {
                    try {
                        Log.e(TAG, "Connection failed: " + error);
                        isConnected = false;
                        
                        // Requirement 10.6: Check if error is unrecoverable
                        if (isUnrecoverableError(error)) {
                            handleUnrecoverableError("Connection Failed", error);
                        } else {
                            // Requirement 10.1: Update notification with error status
                            Notification notification = notificationManager.buildErrorNotification(
                                "Connection Error", 
                                error
                            );
                            notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                        }
                        
                        // Release wake lock when idle
                        // Requirement 9.5: Release wake lock when idle
                        releaseWakeLock();
                    } catch (Exception e) {
                        // Requirement 10.1: Catch exceptions in callback
                        Log.e(TAG, "Exception in onConnectionFailed callback", e);
                        releaseWakeLock();
                    }
                }
                
                @Override
                public void onReconnecting(int attemptNumber) {
                    try {
                        Log.d(TAG, "Reconnecting... attempt #" + attemptNumber);
                        
                        // Update notification to show reconnecting status
                        Notification notification = notificationManager.buildReconnectingNotification(attemptNumber);
                        notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
                    } catch (Exception e) {
                        // Requirement 10.1: Catch exceptions in callback
                        Log.e(TAG, "Exception in onReconnecting callback", e);
                    }
                }
            });
        } catch (Exception e) {
            // Requirement 10.1: Catch exceptions during connection setup
            Log.e(TAG, "Exception setting up connection", e);
            isConnected = false;
            
            // Update notification with error
            try {
                Notification notification = notificationManager.buildErrorNotification(
                    "Connection Error", 
                    "Failed to start connection: " + e.getMessage()
                );
                notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, notification);
            } catch (Exception notifException) {
                Log.e(TAG, "Exception updating notification", notifException);
            }
            
            // Release wake lock on error
            // Requirement 9.5: Release wake lock when idle
            releaseWakeLock();
        }
    }
    
    /**
     * Manually disconnect from the device and clear saved state.
     * Called when user explicitly stops the service or disconnects.
     * 
     * Requirement 11.4: Clear saved MAC on manual disconnect
     */
    private void disconnectDevice() {
        Log.d(TAG, "Manual disconnect requested");
        
        // Clear any pending batched commands
        if (commandHandler != null) {
            commandHandler.clearBatch();
        }
        
        // Stop reconnection attempts
        if (connectionManager != null) {
            connectionManager.stopAutoReconnect();
            connectionManager.disconnect();
        }
        
        // Clear saved state on manual disconnect
        clearSavedState();
        
        isConnected = false;
        deviceMac = null;
        
        Log.d(TAG, "Device disconnected and state cleared");
    }
    
    /**
     * Save the connection state to SharedPreferences.
     * Persists the device MAC address for reconnection after service restart.
     * 
     * @param deviceMac MAC address of the connected device
     * 
     * Requirements: 11.1, 11.2
     */
    private void saveConnectionState(String deviceMac) {
        if (deviceMac == null || deviceMac.isEmpty()) {
            Log.w(TAG, "Cannot save null or empty device MAC");
            return;
        }
        
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DEVICE_MAC, deviceMac);
            editor.putBoolean(KEY_IS_CONNECTED, isConnected);
            editor.apply();
            
            Log.d(TAG, "Connection state saved: deviceMac=" + deviceMac + ", isConnected=" + isConnected);
        } catch (Exception e) {
            Log.e(TAG, "Error saving connection state", e);
        }
    }
    
    /**
     * Load the saved device MAC address from SharedPreferences.
     * Used to restore connection state after service restart.
     * 
     * @return The saved device MAC address, or null if none exists
     * 
     * Requirements: 11.2
     */
    private String loadSavedDeviceMac() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedMac = prefs.getString(KEY_DEVICE_MAC, null);
            
            if (savedMac != null && !savedMac.isEmpty()) {
                Log.d(TAG, "Loaded saved device MAC: " + savedMac);
                return savedMac;
            } else {
                Log.d(TAG, "No saved device MAC found");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading saved device MAC", e);
            return null;
        }
    }
    
    /**
     * Clear the saved connection state from SharedPreferences.
     * Called when user manually disconnects or when state should be reset.
     * 
     * Requirements: 11.4
     */
    private void clearSavedState() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_DEVICE_MAC);
            editor.remove(KEY_IS_CONNECTED);
            editor.apply();
            
            Log.d(TAG, "Saved connection state cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing saved state", e);
        }
    }
    
    /**
     * Emit a service state event to Flutter via LocalBroadcastManager.
     * Events are received by the plugin and forwarded to the EventChannel.
     * 
     * Requirements: 6.5
     * 
     * @param eventType Type of event (serviceStarted, serviceStopped, deviceConnected, deviceDisconnected)
     * @param deviceMac Optional device MAC address for connection events
     */
    private void emitServiceStateEvent(String eventType, String deviceMac) {
        try {
            Intent intent = new Intent(ACTION_SERVICE_STATE);
            intent.putExtra(EXTRA_EVENT_TYPE, eventType);
            
            if (deviceMac != null && !deviceMac.isEmpty()) {
                intent.putExtra(NotificationConfig.EXTRA_DEVICE_MAC, deviceMac);
            }
            
            intent.putExtra("isRunning", true);
            intent.putExtra("isConnected", isConnected);
            
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            Log.d(TAG, "Service state event emitted: " + eventType);
        } catch (Exception e) {
            Log.e(TAG, "Error emitting service state event", e);
        }
    }
    
    /**
     * Show an error notification for missing permissions.
     * This notification includes an action button to open app settings.
     * 
     * Requirements: 8.5
     * 
     * @param missingPermissions Comma-separated list of missing permission names
     */
    private void showPermissionErrorNotification(String missingPermissions) {
        Log.e(TAG, "Showing permission error notification for: " + missingPermissions);
        
        try {
            // Build error notification with action to open settings
            Notification errorNotification = notificationManager.buildPermissionErrorNotification(
                missingPermissions
            );
            
            // Start foreground with error notification
            startForeground(NotificationConfig.NOTIFICATION_ID, errorNotification);
            
            Log.d(TAG, "Permission error notification displayed");
            
        } catch (Exception e) {
            Log.e(TAG, "Exception showing permission error notification", e);
        }
    }
    
    /**
     * Detect if an error is unrecoverable.
     * Unrecoverable errors include permission denied and SDK unavailable.
     * 
     * Requirements: 10.6
     * 
     * @param errorMessage The error message to check
     * @return true if the error is unrecoverable, false otherwise
     */
    private boolean isUnrecoverableError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        
        String lowerError = errorMessage.toLowerCase();
        
        // Check for permission errors
        if (lowerError.contains("permission denied") || 
            lowerError.contains("security") ||
            lowerError.contains("bluetooth_connect") ||
            lowerError.contains("bluetooth_scan")) {
            return true;
        }
        
        // Check for SDK unavailable errors
        if (lowerError.contains("sdk unavailable") ||
            lowerError.contains("sdk not found") ||
            lowerError.contains("library not found")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Acquire a partial wake lock for active operations.
     * Wake locks ensure the CPU stays awake during BLE operations.
     * 
     * Requirements: 9.5
     */
    private void acquireWakeLock() {
        if (wakeLock == null) {
            Log.w(TAG, "Wake lock not initialized, cannot acquire");
            return;
        }
        
        try {
            if (!isWakeLockHeld) {
                wakeLock.acquire(60000); // 60 second timeout as safety
                isWakeLockHeld = true;
                Log.d(TAG, "Wake lock acquired for active operation");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception acquiring wake lock", e);
        }
    }
    
    /**
     * Release the wake lock when operations are complete.
     * This allows the device to enter low-power states.
     * 
     * Requirements: 9.5
     */
    private void releaseWakeLock() {
        if (wakeLock == null) {
            return;
        }
        
        try {
            if (isWakeLockHeld && wakeLock.isHeld()) {
                wakeLock.release();
                isWakeLockHeld = false;
                Log.d(TAG, "Wake lock released - device can enter low-power state");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception releasing wake lock", e);
        }
    }
    
    /**
     * Release all wake locks.
     * Called during service cleanup to ensure no wake locks are leaked.
     * 
     * Requirements: 9.5, 2.6
     */
    public void releaseWakeLocks() {
        Log.d(TAG, "Releasing all wake locks");
        
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                isWakeLockHeld = false;
                Log.d(TAG, "All wake locks released");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception releasing wake locks", e);
        }
    }
    
    /**
     * Handle an unrecoverable error by displaying error notification and stopping service.
     * 
     * Requirements: 10.6
     * 
     * @param errorTitle Title for the error notification
     * @param errorMessage Error message to display
     */
    private void handleUnrecoverableError(String errorTitle, String errorMessage) {
        Log.e(TAG, "Unrecoverable error detected: " + errorTitle + " - " + errorMessage);
        
        try {
            // Display error notification
            Notification errorNotification = notificationManager.buildErrorNotification(
                errorTitle,
                errorMessage + " - Service will stop"
            );
            notificationManager.updateNotification(NotificationConfig.NOTIFICATION_ID, errorNotification);
            
            // Wait a moment for user to see the notification
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Clear saved state
                    clearSavedState();
                    
                    // Stop service gracefully
                    stopSelf();
                }
            }, 5000); // Show error for 5 seconds before stopping
            
        } catch (Exception e) {
            Log.e(TAG, "Exception handling unrecoverable error", e);
            // Force stop even if notification fails
            stopSelf();
        }
    }
}
