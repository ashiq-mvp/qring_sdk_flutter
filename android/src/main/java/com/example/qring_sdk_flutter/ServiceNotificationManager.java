package com.example.qring_sdk_flutter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * ServiceNotificationManager - Manages notification creation, updates, and actions
 * for the QRingBackgroundService.
 * 
 * This class handles:
 * - Notification channel creation for Android 8.0+
 * - Building notifications with dynamic content
 * - Updating notification status
 * - Creating PendingIntents for notification actions
 * 
 * Requirements: 3.1, 3.2, 3.8
 */
public class ServiceNotificationManager {
    private static final String TAG = "ServiceNotificationManager";
    
    private final Context context;
    private final NotificationManager notificationManager;
    
    /**
     * Constructor for ServiceNotificationManager.
     * 
     * @param context Application context
     */
    public ServiceNotificationManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) 
            context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    /**
     * Creates the notification channel for Android 8.0+ (API 26+).
     * The channel is configured with IMPORTANCE_LOW to be persistent but not intrusive.
     * 
     * Requirements: 3.1, 3.8
     */
    public void createNotificationChannel() {
        // Notification channels are only required for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NotificationConfig.CHANNEL_ID,
                NotificationConfig.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // Low importance for persistent, non-intrusive notifications
            );
            
            channel.setDescription(NotificationConfig.CHANNEL_DESCRIPTION);
            channel.setShowBadge(false);  // Don't show badge on app icon
            channel.enableLights(false);  // No LED light
            channel.enableVibration(false);  // No vibration for status updates
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Builds a notification with the specified content and actions.
     * 
     * @param title Notification title
     * @param message Notification message/description
     * @param status Current connection status (Connected, Disconnected, Reconnecting)
     * @param showFindMyRingAction Whether to show the "Find My Ring" action button
     * @return Configured Notification object
     * 
     * Requirements: 3.2, 3.3, 3.4, 3.5, 4.1
     */
    public Notification buildNotification(
            String title, 
            String message, 
            String status,
            boolean showFindMyRingAction) {
        
        // Create PendingIntent for opening the app when notification is tapped
        PendingIntent contentIntent = createOpenAppIntent();
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationConfig.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(getNotificationIcon())  // App icon
            .setContentIntent(contentIntent)
            .setOngoing(true)  // Cannot be dismissed by user
            .setPriority(NotificationCompat.PRIORITY_LOW)  // Low priority for non-intrusive display
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        
        // Add "Find My Ring" action button if device is connected
        if (showFindMyRingAction) {
            PendingIntent findMyRingIntent = createFindMyRingIntent();
            builder.addAction(
                android.R.drawable.ic_menu_mylocation,  // Location icon
                "Find My Ring",
                findMyRingIntent
            );
        }
        
        return builder.build();
    }
    
    /**
     * Updates the notification with new content.
     * 
     * @param notificationId The ID of the notification to update
     * @param notification The new notification object
     * 
     * Requirements: 3.2
     */
    public void updateNotification(int notificationId, Notification notification) {
        notificationManager.notify(notificationId, notification);
    }
    
    /**
     * Creates a PendingIntent for the "Find My Ring" action.
     * Uses FLAG_IMMUTABLE for Android 12+ compatibility.
     * 
     * @return PendingIntent for Find My Ring action
     * 
     * Requirements: 4.1, 8.2
     */
    public PendingIntent createFindMyRingIntent() {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(NotificationConfig.ACTION_FIND_MY_RING);
        
        // Use FLAG_IMMUTABLE for Android 12+ (API 31+) compatibility
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }
    
    /**
     * Creates a PendingIntent for opening the Flutter app.
     * 
     * @return PendingIntent for opening the app
     * 
     * Requirements: 5.1
     */
    public PendingIntent createOpenAppIntent() {
        // Get the launch intent for the app's main activity
        Intent intent = context.getPackageManager()
            .getLaunchIntentForPackage(context.getPackageName());
        
        if (intent == null) {
            // Fallback: create a basic intent
            intent = new Intent(context, context.getClass());
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Use FLAG_IMMUTABLE for Android 12+ (API 31+) compatibility
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
    
    /**
     * Builds a notification for connected state.
     * 
     * @param deviceName Optional device name to display
     * @return Notification configured for connected state
     * 
     * Requirements: 3.3, 3.4, 3.5, 7.2, 7.3
     */
    public Notification buildConnectedNotification(String deviceName) {
        String title = NotificationConfig.STATUS_CONNECTED_TITLE;
        String message = NotificationConfig.STATUS_CONNECTED_MESSAGE;
        
        if (deviceName != null && !deviceName.isEmpty()) {
            message = "Connected to " + deviceName;
        }
        
        return buildNotification(title, message, "Connected", true);
    }
    
    /**
     * Builds a notification for connected state with device name and battery level.
     * 
     * @param deviceName Optional device name to display
     * @param batteryLevel Battery level percentage (0-100), or -1 if unavailable
     * @return Notification configured for connected state with battery info
     * 
     * Requirements: 7.2, 7.3, 7.4
     */
    public Notification buildConnectedNotification(String deviceName, int batteryLevel) {
        String title = NotificationConfig.STATUS_CONNECTED_TITLE;
        String message = NotificationConfig.STATUS_CONNECTED_MESSAGE;
        
        // Build message with device name if available
        if (deviceName != null && !deviceName.isEmpty()) {
            message = "Connected to " + deviceName;
        }
        
        // Add battery level if available
        if (batteryLevel >= 0 && batteryLevel <= 100) {
            message += " • Battery: " + batteryLevel + "%";
        }
        
        return buildNotification(title, message, "Connected", true);
    }
    
    /**
     * Builds a notification for disconnected state.
     * 
     * @param isReconnecting Whether the service is attempting to reconnect
     * @return Notification configured for disconnected state
     * 
     * Requirements: 3.3, 3.4, 3.5
     */
    public Notification buildDisconnectedNotification(boolean isReconnecting) {
        String title = NotificationConfig.STATUS_DISCONNECTED_TITLE;
        String message = isReconnecting 
            ? NotificationConfig.STATUS_RECONNECTING_MESSAGE
            : "Your ring is disconnected";
        
        return buildNotification(title, message, "Disconnected", false);
    }
    
    /**
     * Builds a notification for reconnecting state with attempt count.
     * 
     * @param attemptNumber The current reconnection attempt number
     * @return Notification configured for reconnecting state
     * 
     * Requirements: 3.6, 3.7, 7.1
     */
    public Notification buildReconnectingNotification(int attemptNumber) {
        String title = NotificationConfig.STATUS_DISCONNECTED_TITLE;
        String message = "Reconnecting... (attempt " + attemptNumber + ")";
        
        return buildNotification(title, message, "Reconnecting", false);
    }
    
    /**
     * Builds a notification with feedback message (e.g., "Ring activated").
     * 
     * @param feedbackMessage The feedback message to display
     * @param isConnected Whether the device is currently connected
     * @return Notification with feedback message
     * 
     * Requirements: 4.4, 4.5
     */
    public Notification buildFeedbackNotification(String feedbackMessage, boolean isConnected) {
        String title = isConnected 
            ? NotificationConfig.STATUS_CONNECTED_TITLE 
            : NotificationConfig.STATUS_DISCONNECTED_TITLE;
        
        return buildNotification(title, feedbackMessage, "Feedback", isConnected);
    }
    
    /**
     * Builds a notification for error state.
     * 
     * @param errorTitle Error title
     * @param errorMessage Error message to display
     * @return Notification configured for error state
     * 
     * Requirements: 10.1
     */
    public Notification buildErrorNotification(String errorTitle, String errorMessage) {
        return buildNotification(errorTitle, errorMessage, "Error", false);
    }
    
    /**
     * Builds a notification for permission error with action to open app settings.
     * 
     * @param missingPermissions Comma-separated list of missing permission names
     * @return Notification configured for permission error
     * 
     * Requirements: 8.5
     */
    public Notification buildPermissionErrorNotification(String missingPermissions) {
        String title = "Permissions Required";
        String message = "Missing permissions: " + missingPermissions + ". Tap to grant permissions.";
        
        // Create PendingIntent to open app settings
        PendingIntent settingsIntent = createOpenSettingsIntent();
        
        // Build the notification with settings action
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationConfig.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(getNotificationIcon())
            .setContentIntent(settingsIntent)  // Tap notification to open settings
            .setOngoing(false)  // Can be dismissed
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // High priority for important error
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true);  // Dismiss when tapped
        
        // Add action button to open settings
        builder.addAction(
            android.R.drawable.ic_menu_preferences,  // Settings icon
            "Open Settings",
            settingsIntent
        );
        
        return builder.build();
    }
    
    /**
     * Creates a PendingIntent to open the app's settings page.
     * Used for permission error notifications.
     * 
     * @return PendingIntent for opening app settings
     * 
     * Requirements: 8.5
     */
    private PendingIntent createOpenSettingsIntent() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.fromParts("package", context.getPackageName(), null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // Use FLAG_IMMUTABLE for Android 12+ (API 31+) compatibility
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getActivity(context, 0, intent, flags);
    }
    
    /**
     * Gets the appropriate notification icon resource ID.
     * Uses the app's launcher icon.
     * 
     * @return Resource ID for the notification icon
     */
    private int getNotificationIcon() {
        // Try to get the app's launcher icon
        int iconId = context.getApplicationInfo().icon;
        
        // Fallback to a system icon if app icon is not available
        if (iconId == 0) {
            iconId = android.R.drawable.ic_dialog_info;
        }
        
        return iconId;
    }
}
