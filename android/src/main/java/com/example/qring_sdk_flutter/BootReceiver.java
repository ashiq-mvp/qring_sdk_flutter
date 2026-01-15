package com.example.qring_sdk_flutter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

/**
 * BootReceiver - Restarts the QRing background service after device boot.
 * 
 * This receiver listens for BOOT_COMPLETED broadcasts and automatically
 * restarts the foreground service if a QRing device was previously connected.
 * 
 * Requirements: 6.3
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
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
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            handleBootCompleted(context);
        }
    }
    
    /**
     * Handle device boot completion.
     * Restarts the service if a device was previously connected.
     * 
     * Requirements: 6.3
     * 
     * @param context Application context
     */
    private void handleBootCompleted(Context context) {
        Log.d(TAG, "Device boot completed - checking for saved device");
        
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
                
                Log.d(TAG, "Service restart initiated after boot");
            } else {
                Log.d(TAG, "No saved device found - service not restarted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception handling boot completed", e);
        }
    }
}
