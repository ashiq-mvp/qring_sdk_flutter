package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Manages BLE device pairing and bonding operations.
 * 
 * This class handles the reliable pairing workflow including:
 * - Bond state checking
 * - Bonding initiation for unbonded devices
 * - Bonding retry logic (max 1 retry)
 * - Timeout handling (30 seconds)
 * - Bond state change monitoring via BroadcastReceiver
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
public class PairingManager {
    private static final String TAG = "PairingManager";
    
    // Bonding timeout in milliseconds (30 seconds)
    private static final long BONDING_TIMEOUT_MS = 30000;
    
    // Maximum number of bonding retries
    private static final int MAX_BONDING_RETRIES = 1;
    
    // Singleton instance
    private static PairingManager instance;
    
    // Context
    private Context context;
    
    // Current pairing state
    private BluetoothDevice currentDevice;
    private PairingCallback currentCallback;
    private int retryCount;
    private boolean isPairing;
    
    // Handler for timeout management
    private final Handler handler;
    private Runnable timeoutRunnable;
    
    // Broadcast receiver for bond state changes
    private BondStateReceiver bondStateReceiver;
    
    /**
     * Callback interface for pairing operations.
     */
    public interface PairingCallback {
        /**
         * Called when pairing succeeds.
         * 
         * @param device The paired device
         */
        void onPairingSuccess(BluetoothDevice device);
        
        /**
         * Called when pairing fails.
         * 
         * @param error Error message describing the failure
         */
        void onPairingFailed(String error);
        
        /**
         * Called when pairing is being retried.
         * 
         * @param attemptNumber The current attempt number (1-based)
         */
        void onPairingRetry(int attemptNumber);
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private PairingManager() {
        this.handler = new Handler(Looper.getMainLooper());
        this.retryCount = 0;
        this.isPairing = false;
    }
    
    /**
     * Get the singleton instance of PairingManager.
     * 
     * @return The singleton instance
     */
    public static synchronized PairingManager getInstance() {
        if (instance == null) {
            instance = new PairingManager();
        }
        return instance;
    }
    
    /**
     * Initialize the PairingManager with application context.
     * 
     * @param context Application context
     */
    public void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        Log.d(TAG, "PairingManager initialized");
    }
    
    /**
     * Check the bond state of a device.
     * 
     * Requirement 3.1: WHEN initiating connection to a QRing, THE BLE_Manager SHALL 
     * check the Bond_State before proceeding
     * 
     * @param device The device to check
     * @return The bond state (BOND_NONE, BOND_BONDING, or BOND_BONDED)
     */
    public int checkBondState(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot check bond state: device is null");
            return BluetoothDevice.BOND_NONE;
        }
        
        int bondState = device.getBondState();
        Log.d(TAG, String.format("Bond state for device %s: %s", 
            device.getAddress(), bondStateToString(bondState)));
        
        return bondState;
    }
    
    /**
     * Check if a device is bonded.
     * 
     * @param device The device to check
     * @return true if the device is bonded, false otherwise
     */
    public boolean isBonded(BluetoothDevice device) {
        return checkBondState(device) == BluetoothDevice.BOND_BONDED;
    }
    
    /**
     * Start the pairing process for a device.
     * 
     * This method handles the complete pairing workflow:
     * 1. Check current bond state
     * 2. If not bonded, trigger createBond()
     * 3. Wait for BOND_BONDED state
     * 4. Retry once on failure
     * 5. Report success or failure via callback
     * 
     * Requirements: 3.2, 3.3, 3.4, 3.5, 3.6
     * 
     * @param device The device to pair with
     * @param callback Callback to receive pairing results
     */
    public void startPairing(BluetoothDevice device, PairingCallback callback) {
        if (device == null) {
            Log.e(TAG, "Cannot start pairing: device is null");
            if (callback != null) {
                callback.onPairingFailed("Device is null");
            }
            return;
        }
        
        if (callback == null) {
            Log.e(TAG, "Cannot start pairing: callback is null");
            return;
        }
        
        if (isPairing) {
            Log.w(TAG, "Pairing already in progress, ignoring new request");
            callback.onPairingFailed("Pairing already in progress");
            return;
        }
        
        // Check current bond state
        int bondState = checkBondState(device);
        
        // Requirement 3.2: IF the Bond_State is not BOND_BONDED, THEN THE BLE_Manager 
        // SHALL trigger createBond
        if (bondState == BluetoothDevice.BOND_BONDED) {
            Log.d(TAG, "Device already bonded, no pairing needed");
            callback.onPairingSuccess(device);
            return;
        }
        
        // Start pairing process
        this.currentDevice = device;
        this.currentCallback = callback;
        this.retryCount = 0;
        this.isPairing = true;
        
        // Register bond state receiver
        registerBondStateReceiver();
        
        // Initiate bonding
        initiateBonding(device);
    }
    
    /**
     * Initiate bonding with a device.
     * 
     * Requirement 3.2: IF the Bond_State is not BOND_BONDED, THEN THE BLE_Manager 
     * SHALL trigger createBond
     * 
     * @param device The device to bond with
     */
    private void initiateBonding(BluetoothDevice device) {
        Log.d(TAG, String.format("Initiating bonding with device %s (attempt %d/%d)", 
            device.getAddress(), retryCount + 1, MAX_BONDING_RETRIES + 1));
        
        try {
            // Trigger createBond()
            boolean bondingStarted = device.createBond();
            
            if (!bondingStarted) {
                Log.e(TAG, "Failed to start bonding process");
                handleBondingFailure("Failed to start bonding");
                return;
            }
            
            // Requirement 3.5: Add timeout handling for bonding (30 seconds)
            startBondingTimeout();
            
        } catch (Exception e) {
            Log.e(TAG, "Exception while initiating bonding", e);
            handleBondingFailure("Exception during bonding: " + e.getMessage());
        }
    }
    
    /**
     * Start the bonding timeout timer.
     * 
     * Requirement 3.5: Add timeout handling for bonding (30 seconds)
     */
    private void startBondingTimeout() {
        // Cancel any existing timeout
        cancelBondingTimeout();
        
        // Create new timeout runnable
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Bonding timeout after " + BONDING_TIMEOUT_MS + "ms");
                handleBondingFailure("Bonding timeout");
            }
        };
        
        // Schedule timeout
        handler.postDelayed(timeoutRunnable, BONDING_TIMEOUT_MS);
        Log.d(TAG, "Bonding timeout scheduled for " + BONDING_TIMEOUT_MS + "ms");
    }
    
    /**
     * Cancel the bonding timeout timer.
     */
    private void cancelBondingTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
            Log.d(TAG, "Bonding timeout cancelled");
        }
    }
    
    /**
     * Register the bond state broadcast receiver.
     */
    private void registerBondStateReceiver() {
        if (context == null) {
            Log.e(TAG, "Cannot register bond state receiver: context is null");
            return;
        }
        
        // Unregister existing receiver if any
        unregisterBondStateReceiver();
        
        // Create and register new receiver
        bondStateReceiver = new BondStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(bondStateReceiver, filter);
        
        Log.d(TAG, "Bond state receiver registered");
    }
    
    /**
     * Unregister the bond state broadcast receiver.
     */
    private void unregisterBondStateReceiver() {
        if (bondStateReceiver != null && context != null) {
            try {
                context.unregisterReceiver(bondStateReceiver);
                Log.d(TAG, "Bond state receiver unregistered");
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, ignore
                Log.d(TAG, "Bond state receiver was not registered");
            }
            bondStateReceiver = null;
        }
    }
    
    /**
     * Handle bonding success.
     * 
     * Requirement 3.6: WHEN Bond_State becomes BOND_BONDED, THE BLE_Manager SHALL 
     * proceed with GATT connection
     */
    private void handleBondingSuccess() {
        Log.d(TAG, "Bonding successful");
        
        // Cancel timeout
        cancelBondingTimeout();
        
        // Unregister receiver
        unregisterBondStateReceiver();
        
        // Notify callback
        if (currentCallback != null) {
            currentCallback.onPairingSuccess(currentDevice);
        }
        
        // Reset state
        resetPairingState();
    }
    
    /**
     * Handle bonding failure.
     * 
     * Requirement 3.4: IF bonding fails, THEN THE BLE_Manager SHALL retry the bonding 
     * process once
     * 
     * Requirement 3.5: IF bonding fails after retry, THEN THE BLE_Manager SHALL report 
     * pairing error to Flutter_Bridge
     * 
     * @param error Error message
     */
    private void handleBondingFailure(String error) {
        Log.e(TAG, "Bonding failed: " + error);
        
        // Cancel timeout
        cancelBondingTimeout();
        
        // Check if we should retry
        if (retryCount < MAX_BONDING_RETRIES) {
            retryCount++;
            Log.d(TAG, String.format("Retrying bonding (attempt %d/%d)", 
                retryCount + 1, MAX_BONDING_RETRIES + 1));
            
            // Notify callback of retry
            if (currentCallback != null) {
                currentCallback.onPairingRetry(retryCount + 1);
            }
            
            // Retry bonding after a short delay
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (currentDevice != null) {
                        initiateBonding(currentDevice);
                    }
                }
            }, 1000); // 1 second delay before retry
            
        } else {
            // Max retries reached, report failure
            Log.e(TAG, "Max bonding retries reached, giving up");
            
            // Unregister receiver
            unregisterBondStateReceiver();
            
            // Notify callback
            if (currentCallback != null) {
                currentCallback.onPairingFailed(error);
            }
            
            // Reset state
            resetPairingState();
        }
    }
    
    /**
     * Reset the pairing state.
     */
    private void resetPairingState() {
        this.currentDevice = null;
        this.currentCallback = null;
        this.retryCount = 0;
        this.isPairing = false;
    }
    
    /**
     * Check if pairing is currently in progress.
     * 
     * @return true if pairing is in progress, false otherwise
     */
    public boolean isPairing() {
        return isPairing;
    }
    
    /**
     * Cancel the current pairing operation.
     */
    public void cancelPairing() {
        if (!isPairing) {
            Log.d(TAG, "No pairing in progress, nothing to cancel");
            return;
        }
        
        Log.d(TAG, "Cancelling pairing operation");
        
        // Cancel timeout
        cancelBondingTimeout();
        
        // Unregister receiver
        unregisterBondStateReceiver();
        
        // Notify callback
        if (currentCallback != null) {
            currentCallback.onPairingFailed("Pairing cancelled");
        }
        
        // Reset state
        resetPairingState();
    }
    
    /**
     * Convert bond state integer to string for logging.
     * 
     * @param bondState The bond state integer
     * @return String representation of the bond state
     */
    private String bondStateToString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN (" + bondState + ")";
        }
    }
    
    /**
     * BroadcastReceiver for monitoring bond state changes.
     * 
     * Requirement 3.3: WHEN createBond is triggered, THE BLE_Manager SHALL wait for 
     * BOND_BONDED state before establishing GATT connection
     */
    private class BondStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            
            // Get the device from the intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null || currentDevice == null || !device.getAddress().equals(currentDevice.getAddress())) {
                // Not the device we're pairing with
                return;
            }
            
            // Get the bond state
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
            int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
            
            Log.d(TAG, String.format("Bond state changed: %s -> %s for device %s", 
                bondStateToString(previousBondState), 
                bondStateToString(bondState), 
                device.getAddress()));
            
            // Handle bond state changes
            switch (bondState) {
                case BluetoothDevice.BOND_BONDED:
                    // Bonding successful
                    handleBondingSuccess();
                    break;
                    
                case BluetoothDevice.BOND_NONE:
                    // Bonding failed (transitioned from BONDING to NONE)
                    if (previousBondState == BluetoothDevice.BOND_BONDING) {
                        handleBondingFailure("Bonding failed");
                    }
                    break;
                    
                case BluetoothDevice.BOND_BONDING:
                    // Bonding in progress, just log
                    Log.d(TAG, "Bonding in progress...");
                    break;
            }
        }
    }
}
