package com.example.qring_sdk_flutter;

/**
 * NotificationConfig - Constants for notification channel and actions.
 * 
 * Defines all constants used for the persistent notification displayed by
 * the QRingBackgroundService, including channel configuration, notification IDs,
 * action identifiers, and intent extras.
 * 
 * Requirements: 3.1, 4.1
 */
public class NotificationConfig {
    
    // Notification Channel Configuration
    /**
     * Unique identifier for the notification channel.
     * Used for Android 8.0+ notification channel management.
     */
    public static final String CHANNEL_ID = "qring_service_channel";
    
    /**
     * User-visible name for the notification channel.
     * Displayed in system notification settings.
     */
    public static final String CHANNEL_NAME = "Smart Ring Service";
    
    /**
     * Description of the notification channel.
     * Helps users understand the purpose of notifications from this channel.
     */
    public static final String CHANNEL_DESCRIPTION = "Notifications for QRing background service status";
    
    // Notification ID
    /**
     * Unique identifier for the foreground service notification.
     * Used to update the notification without creating duplicates.
     */
    public static final int NOTIFICATION_ID = 1001;
    
    // Action Constants
    /**
     * Action identifier for the "Find My Ring" notification button.
     * Triggers the ring to emit a sound or vibration for locating purposes.
     */
    public static final String ACTION_FIND_MY_RING = "com.example.qring_sdk_flutter.FIND_MY_RING";
    
    /**
     * Action identifier for opening the Flutter app.
     * Used when the user taps the notification body.
     */
    public static final String ACTION_OPEN_APP = "com.example.qring_sdk_flutter.OPEN_APP";
    
    /**
     * Action identifier for stopping the background service.
     * Can be used for a future "Stop Service" notification action.
     */
    public static final String ACTION_STOP_SERVICE = "com.example.qring_sdk_flutter.STOP_SERVICE";
    
    // Intent Extra Keys
    /**
     * Intent extra key for device MAC address.
     * Used to pass the device MAC address when starting the service.
     */
    public static final String EXTRA_DEVICE_MAC = "device_mac";
    
    /**
     * Intent extra key for command type.
     * Used to pass command identifiers for service operations.
     */
    public static final String EXTRA_COMMAND = "command";
    
    /**
     * Intent extra key for command parameters.
     * Used to pass additional parameters for commands as a JSON string.
     */
    public static final String EXTRA_COMMAND_PARAMS = "command_params";
    
    // Notification Status Messages
    /**
     * Notification title when device is connected.
     */
    public static final String STATUS_CONNECTED_TITLE = "Smart Ring Connected";
    
    /**
     * Notification title when device is disconnected.
     */
    public static final String STATUS_DISCONNECTED_TITLE = "Smart Ring Disconnected";
    
    /**
     * Notification message when device is connected.
     */
    public static final String STATUS_CONNECTED_MESSAGE = "Your ring is connected and active";
    
    /**
     * Notification message when device is disconnected and reconnecting.
     */
    public static final String STATUS_RECONNECTING_MESSAGE = "Reconnecting to your ring...";
    
    /**
     * Notification message when Find My Ring is activated.
     */
    public static final String STATUS_RING_ACTIVATED = "Ring activated";
    
    /**
     * Notification message when Find My Ring fails.
     */
    public static final String STATUS_RING_NOT_CONNECTED = "Ring not connected";
    
    // Private constructor to prevent instantiation
    private NotificationConfig() {
        throw new AssertionError("NotificationConfig is a utility class and should not be instantiated");
    }
}
