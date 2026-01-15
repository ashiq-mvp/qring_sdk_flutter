package com.example.qring_sdk_flutter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * NotificationActionReceiver - BroadcastReceiver for handling notification action button taps.
 * 
 * This receiver processes actions triggered from the persistent notification, such as:
 * - Find My Ring: Triggers the ring to emit a sound/vibration
 * - Stop Service: Stops the background service (future enhancement)
 * 
 * The receiver forwards actions to the QRingBackgroundService for execution.
 * 
 * Requirements: 4.2, 12.2
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";
    
    /**
     * Called when a notification action is triggered.
     * Processes the action and forwards it to the QRingBackgroundService.
     * 
     * @param context The Context in which the receiver is running
     * @param intent The Intent being received
     * 
     * Requirements: 4.2, 12.2
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG, "Received null intent or action");
            return;
        }
        
        String action = intent.getAction();
        Log.d(TAG, "Received notification action: " + action);
        
        // Handle different notification actions
        switch (action) {
            case NotificationConfig.ACTION_FIND_MY_RING:
                handleFindMyRing(context);
                break;
                
            case NotificationConfig.ACTION_STOP_SERVICE:
                handleStopService(context);
                break;
                
            default:
                Log.w(TAG, "Unknown action received: " + action);
                break;
        }
    }
    
    /**
     * Handles the "Find My Ring" action by forwarding it to the service.
     * 
     * @param context Application context
     * 
     * Requirements: 4.2
     */
    private void handleFindMyRing(Context context) {
        Log.d(TAG, "Handling Find My Ring action");
        
        // Create intent to forward action to the service
        Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
        serviceIntent.setAction(NotificationConfig.ACTION_FIND_MY_RING);
        
        // Start the service with the action
        // The service will process the Find My Ring command
        try {
            context.startService(serviceIntent);
            Log.d(TAG, "Find My Ring action forwarded to service");
        } catch (Exception e) {
            Log.e(TAG, "Failed to forward Find My Ring action to service", e);
        }
    }
    
    /**
     * Handles the "Stop Service" action by stopping the background service.
     * 
     * @param context Application context
     */
    private void handleStopService(Context context) {
        Log.d(TAG, "Handling Stop Service action");
        
        // Create intent to stop the service
        Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
        
        try {
            context.stopService(serviceIntent);
            Log.d(TAG, "Service stop requested");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service", e);
        }
    }
}
