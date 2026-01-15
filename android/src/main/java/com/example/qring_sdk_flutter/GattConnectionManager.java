package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Manages GATT connection lifecycle with proper resource management.
 * 
 * This class handles:
 * - GATT connection with autoConnect=true
 * - Service discovery workflow
 * - MTU negotiation (512 bytes)
 * - Proper disconnect and close sequence
 * - GATT resource cleanup
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 9.5
 */
public class GattConnectionManager {
    private static final String TAG = "GattConnectionManager";
    
    // MTU size to request (512 bytes for optimal throughput)
    private static final int REQUESTED_MTU = 512;
    
    // Connection timeout in milliseconds
    private static final long CONNECTION_TIMEOUT_MS = 30000;
    
    // Service discovery timeout in milliseconds
    private static final long SERVICE_DISCOVERY_TIMEOUT_MS = 10000;
    
    // Singleton instance
    private static GattConnectionManager instance;
    
    // Context
    private Context context;
    
    // GATT connection
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice currentDevice;
    
    // Connection state
    private boolean isConnected;
    private boolean servicesDiscovered;
    private int negotiatedMtu;
    
    // Callback
    private GattCallback currentCallback;
    
    // Handler for timeout management
    private final Handler handler;
    private Runnable connectionTimeoutRunnable;
    private Runnable serviceDiscoveryTimeoutRunnable;
    
    /**
     * Callback interface for GATT operations.
     */
    public interface GattCallback {
        /**
         * Called when GATT connection is established.
         * 
         * @param device The connected device
         */
        void onConnected(BluetoothDevice device);
        
        /**
         * Called when GATT services are discovered.
         * 
         * @param device The device
         * @param gatt The GATT instance
         */
        void onServicesDiscovered(BluetoothDevice device, BluetoothGatt gatt);
        
        /**
         * Called when MTU is negotiated.
         * 
         * @param device The device
         * @param mtu The negotiated MTU size
         */
        void onMtuNegotiated(BluetoothDevice device, int mtu);
        
        /**
         * Called when GATT connection is lost.
         * 
         * @param device The device
         * @param wasExpected true if disconnect was expected (manual), false if unexpected
         */
        void onDisconnected(BluetoothDevice device, boolean wasExpected);
        
        /**
         * Called when a GATT error occurs.
         * 
         * @param device The device
         * @param operation The operation that failed
         * @param status The GATT status code
         * @param error Error message
         */
        void onError(BluetoothDevice device, String operation, int status, String error);
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private GattConnectionManager() {
        this.handler = new Handler(Looper.getMainLooper());
        this.isConnected = false;
        this.servicesDiscovered = false;
        this.negotiatedMtu = 23; // Default MTU
    }
    
    /**
     * Get the singleton instance of GattConnectionManager.
     * 
     * @return The singleton instance
     */
    public static synchronized GattConnectionManager getInstance() {
        if (instance == null) {
            instance = new GattConnectionManager();
        }
        return instance;
    }
    
    /**
     * Initialize the GattConnectionManager with application context.
     * 
     * @param context Application context
     */
    public void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        Log.d(TAG, "GattConnectionManager initialized");
    }
    
    /**
     * Connect to a BLE device using GATT.
     * 
     * Requirement 4.1: WHEN establishing GATT connection, THE BLE_Manager SHALL use 
     * autoConnect parameter set to true
     * 
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     * 
     * @param device The device to connect to
     * @param autoConnect Whether to use autoConnect (should always be true)
     * @param callback Callback to receive connection events
     */
    public void connect(BluetoothDevice device, boolean autoConnect, GattCallback callback) {
        if (device == null) {
            Log.e(TAG, "Cannot connect: device is null");
            if (callback != null) {
                callback.onError(null, "connect", -1, "Device is null");
            }
            return;
        }
        
        if (callback == null) {
            Log.e(TAG, "Cannot connect: callback is null");
            return;
        }
        
        if (context == null) {
            Log.e(TAG, "Cannot connect: context is null (not initialized)");
            callback.onError(device, "connect", -1, "GattConnectionManager not initialized");
            return;
        }
        
        if (bluetoothGatt != null) {
            Log.w(TAG, "GATT connection already exists, closing old connection first");
            close();
        }
        
        // Store state
        this.currentDevice = device;
        this.currentCallback = callback;
        this.isConnected = false;
        this.servicesDiscovered = false;
        this.negotiatedMtu = 23;
        
        // Requirement 4.1: Use autoConnect=true for automatic OS-level reconnection
        Log.d(TAG, String.format("Connecting to device %s with autoConnect=%b", 
            device.getAddress(), autoConnect));
        
        try {
            // Connect to GATT server
            bluetoothGatt = device.connectGatt(context, autoConnect, gattCallback);
            
            if (bluetoothGatt == null) {
                Log.e(TAG, "Failed to create GATT connection");
                callback.onError(device, "connect", -1, "Failed to create GATT connection");
                resetState();
                return;
            }
            
            // Start connection timeout
            startConnectionTimeout();
            
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during connection
            Log.e(TAG, "SecurityException while connecting to GATT (permission revoked)", e);
            callback.onError(device, "connect", -1, "Permission revoked: " + e.getMessage());
            resetState();
        } catch (Exception e) {
            Log.e(TAG, "Exception while connecting to GATT", e);
            callback.onError(device, "connect", -1, "Exception: " + e.getMessage());
            resetState();
        }
    }
    
    /**
     * Discover GATT services.
     * 
     * Requirement 4.2: WHEN GATT connection is established, THE BLE_Manager SHALL call 
     * discoverServices before any data operations
     * 
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     * 
     * @return true if service discovery was started, false otherwise
     */
    public boolean discoverServices() {
        if (bluetoothGatt == null) {
            Log.e(TAG, "Cannot discover services: GATT is null");
            return false;
        }
        
        if (!isConnected) {
            Log.e(TAG, "Cannot discover services: not connected");
            return false;
        }
        
        Log.d(TAG, "Starting service discovery");
        
        try {
            boolean started = bluetoothGatt.discoverServices();
            
            if (started) {
                // Start service discovery timeout
                startServiceDiscoveryTimeout();
            } else {
                Log.e(TAG, "Failed to start service discovery");
            }
            
            return started;
            
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during operation
            Log.e(TAG, "SecurityException while discovering services (permission revoked)", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception while discovering services", e);
            return false;
        }
    }
    
    /**
     * Request MTU negotiation.
     * 
     * Requirement 4.3: WHEN services are discovered, THE BLE_Manager SHALL negotiate MTU 
     * to optimize data transfer
     * 
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     * 
     * @param mtu The MTU size to request (default: 512 bytes)
     * @return true if MTU request was sent, false otherwise
     */
    public boolean requestMtu(int mtu) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "Cannot request MTU: GATT is null");
            return false;
        }
        
        if (!isConnected) {
            Log.e(TAG, "Cannot request MTU: not connected");
            return false;
        }
        
        Log.d(TAG, String.format("Requesting MTU: %d bytes", mtu));
        
        try {
            return bluetoothGatt.requestMtu(mtu);
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during operation
            Log.e(TAG, "SecurityException while requesting MTU (permission revoked)", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception while requesting MTU", e);
            return false;
        }
    }
    
    /**
     * Disconnect from the GATT server.
     * 
     * Requirement 9.1: WHEN disconnectRing is called, THE BLE_Manager SHALL call 
     * bluetoothGatt.disconnect()
     * 
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     */
    public void disconnect() {
        if (bluetoothGatt == null) {
            Log.d(TAG, "Cannot disconnect: GATT is null");
            return;
        }
        
        Log.d(TAG, "Disconnecting from GATT server");
        
        try {
            bluetoothGatt.disconnect();
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during disconnection
            Log.w(TAG, "SecurityException while disconnecting (permission revoked)", e);
            // Continue with cleanup even if disconnect fails
        } catch (Exception e) {
            Log.e(TAG, "Exception while disconnecting", e);
        }
    }
    
    /**
     * Close the GATT connection and release resources.
     * 
     * Requirement 9.2: WHEN bluetoothGatt.disconnect() completes, THE BLE_Manager SHALL 
     * call bluetoothGatt.close()
     * 
     * Requirement 4.5: THE BLE_Manager SHALL maintain GATT connection lifecycle including 
     * proper cleanup on disconnect
     * 
     * Requirement 9.5: THE BLE_Manager SHALL release all GATT resources after manual disconnect
     * 
     * Requirement 10.4: Add SecurityException catching in all BLE operations
     */
    public void close() {
        if (bluetoothGatt == null) {
            Log.d(TAG, "Cannot close: GATT is null");
            return;
        }
        
        Log.d(TAG, "Closing GATT connection and releasing resources");
        
        try {
            bluetoothGatt.close();
        } catch (SecurityException e) {
            // Requirement 10.4: Graceful handling when permissions revoked during close
            Log.w(TAG, "SecurityException while closing GATT (permission revoked)", e);
            // Continue with state reset even if close fails
        } catch (Exception e) {
            Log.e(TAG, "Exception while closing GATT", e);
        }
        
        // Reset state
        resetState();
    }
    
    /**
     * Get the current GATT instance.
     * 
     * @return The BluetoothGatt instance, or null if not connected
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }
    
    /**
     * Check if currently connected.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }
    
    /**
     * Check if services have been discovered.
     * 
     * @return true if services discovered, false otherwise
     */
    public boolean areServicesDiscovered() {
        return servicesDiscovered;
    }
    
    /**
     * Get the negotiated MTU size.
     * 
     * @return The negotiated MTU size in bytes
     */
    public int getNegotiatedMtu() {
        return negotiatedMtu;
    }
    
    /**
     * Get the currently connected device.
     * 
     * @return The connected device, or null if not connected
     */
    public BluetoothDevice getCurrentDevice() {
        return currentDevice;
    }
    
    /**
     * Reset the internal state.
     */
    private void resetState() {
        bluetoothGatt = null;
        currentDevice = null;
        currentCallback = null;
        isConnected = false;
        servicesDiscovered = false;
        negotiatedMtu = 23;
        
        // Cancel any pending timeouts
        cancelConnectionTimeout();
        cancelServiceDiscoveryTimeout();
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
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onError(currentDevice, "connect", -1, "Connection timeout");
                }
                // Close the connection
                close();
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
     * Start the service discovery timeout timer.
     */
    private void startServiceDiscoveryTimeout() {
        // Cancel any existing timeout
        cancelServiceDiscoveryTimeout();
        
        // Create new timeout runnable
        serviceDiscoveryTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Service discovery timeout after " + SERVICE_DISCOVERY_TIMEOUT_MS + "ms");
                if (currentCallback != null && currentDevice != null) {
                    // Requirement 4.4: IF discoverServices fails, THEN THE BLE_Manager SHALL 
                    // disconnect and report error
                    currentCallback.onError(currentDevice, "discoverServices", -1, 
                        "Service discovery timeout");
                }
                // Disconnect and close
                disconnect();
            }
        };
        
        // Schedule timeout
        handler.postDelayed(serviceDiscoveryTimeoutRunnable, SERVICE_DISCOVERY_TIMEOUT_MS);
        Log.d(TAG, "Service discovery timeout scheduled for " + SERVICE_DISCOVERY_TIMEOUT_MS + "ms");
    }
    
    /**
     * Cancel the service discovery timeout timer.
     */
    private void cancelServiceDiscoveryTimeout() {
        if (serviceDiscoveryTimeoutRunnable != null) {
            handler.removeCallbacks(serviceDiscoveryTimeoutRunnable);
            serviceDiscoveryTimeoutRunnable = null;
            Log.d(TAG, "Service discovery timeout cancelled");
        }
    }
    
    /**
     * GATT callback implementation.
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, String.format("onConnectionStateChange: status=%d, newState=%d", 
                status, newState));
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Connection established
                Log.i(TAG, "Connected to GATT server");
                
                // Cancel connection timeout
                cancelConnectionTimeout();
                
                // Update state
                isConnected = true;
                
                // Notify callback
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onConnected(currentDevice);
                }
                
                // Requirement 4.2: Automatically start service discovery after connection
                discoverServices();
                
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Connection lost
                Log.i(TAG, "Disconnected from GATT server");
                
                // Cancel timeouts
                cancelConnectionTimeout();
                cancelServiceDiscoveryTimeout();
                
                // Determine if disconnect was expected
                boolean wasExpected = (status == BluetoothGatt.GATT_SUCCESS);
                
                // Store device before reset
                BluetoothDevice device = currentDevice;
                GattCallback callback = currentCallback;
                
                // Update state
                isConnected = false;
                servicesDiscovered = false;
                
                // Notify callback
                if (callback != null && device != null) {
                    callback.onDisconnected(device, wasExpected);
                }
                
            } else {
                Log.w(TAG, "Unknown connection state: " + newState);
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, String.format("onServicesDiscovered: status=%d", status));
            
            // Cancel service discovery timeout
            cancelServiceDiscoveryTimeout();
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Services discovered successfully
                Log.i(TAG, "Services discovered successfully");
                
                // Update state
                servicesDiscovered = true;
                
                // Notify callback
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onServicesDiscovered(currentDevice, gatt);
                }
                
                // Requirement 4.3: Negotiate MTU after service discovery
                requestMtu(REQUESTED_MTU);
                
            } else {
                // Service discovery failed
                Log.e(TAG, "Service discovery failed with status: " + status);
                
                // Requirement 4.4: Disconnect on service discovery failure
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onError(currentDevice, "discoverServices", status, 
                        "Service discovery failed");
                }
                
                // Disconnect and close
                disconnect();
            }
        }
        
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, String.format("onMtuChanged: mtu=%d, status=%d", mtu, status));
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // MTU negotiated successfully
                Log.i(TAG, String.format("MTU negotiated: %d bytes", mtu));
                
                // Update state
                negotiatedMtu = mtu;
                
                // Notify callback
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onMtuNegotiated(currentDevice, mtu);
                }
                
            } else {
                // MTU negotiation failed (not critical, use default)
                Log.w(TAG, String.format("MTU negotiation failed with status: %d, using default MTU", status));
                
                // Still notify callback with default MTU
                if (currentCallback != null && currentDevice != null) {
                    currentCallback.onMtuNegotiated(currentDevice, negotiatedMtu);
                }
            }
        }
    };
}
