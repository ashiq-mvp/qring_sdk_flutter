package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * BluetoothReceiver - Restarts the QRing background service when Bluetooth is turned on.
 * 
 * This receiver listens for Bluetooth state changes and automatically
 * restarts the foreground service when Bluetooth is enabled, if a QRing
 * device was previously connected.
 * 
 * Requirements: 6.4
 */
public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothReceiver";
    
    // SharedPreferences constants (must match QRingBackgroundService)
    private static final String PREFS_NAME = "qring_service_state";
    private static final String KEY_DEVICE_MAC = "device_mac";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null intent or action");
            return;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);
        
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            handleBluetoothStateChange(context, intent);
        }
    }
    
    /**
     * Handle Bluetooth state change.
     * Restarts the service when Bluetooth is turned on if a device was previously connected.
     * 
     * Requirements: 6.4
     * 
     * @param context Application context
     * @param intent The broadcast intent containing state information
     */
    private void handleBluetoothStateChange(Context context, Intent intent) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
        int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.ERROR);
        
        Log.d(TAG, "Bluetooth state changed: " + previousState + " -> " + state);
        
        // Only act when Bluetooth is turned ON
        if (state == BluetoothAdapter.STATE_ON) {
            handleBluetoothOn(context);
        } else if (state == BluetoothAdapter.STATE_OFF) {
            Log.d(TAG, "Bluetooth turned off - service will handle reconnection pause");
        }
    }
    
    /**
     * Handle Bluetooth being turned on.
     * Restarts the service if a device was previously connected.
     * 
     * Requirements: 6.4
     * 
     * @param context Application context
     */
    private void handleBluetoothOn(Context context) {
        Log.d(TAG, "Bluetooth turned on - checking for saved device");
        
        try {
            // Check if a device was previously connected
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String savedMac = prefs.getString(KEY_DEVICE_MAC, null);
            
            if (savedMac != null && !savedMac.isEmpty()) {
                Log.d(TAG, "Found saved device MAC: " + savedMac + " - restarting service");
                
                // Create intent to start the service
                Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
                serviceIntent.putExtra(NotificationConfig.EXTRA_DEVICE_MAC, savedMac);
                
                // Start foreground service
                // Android 8+ requires startForegroundService
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                Log.d(TAG, "Service restart initiated after Bluetooth ON");
            } else {
                Log.d(TAG, "No saved device found - service not restarted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception handling Bluetooth ON", e);
        }
    }
}
