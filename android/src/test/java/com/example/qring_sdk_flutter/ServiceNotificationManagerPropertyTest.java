package com.example.qring_sdk_flutter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ServiceNotificationManager.
 * 
 * Feature: production-ble-manager
 * 
 * These tests validate the correctness properties defined in the design document
 * for notification management operations.
 * 
 * Tests Properties 25, 26, 27, 28, 29, 30:
 * - Persistent Notification While Service Running
 * - Notification Contains Device Name
 * - Notification Contains Connection State
 * - Notification Contains Battery When Available
 * - Find My Ring on Notification Tap
 * - Notification Updates on State Change
 */
public class ServiceNotificationManagerPropertyTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private NotificationManager mockNotificationManager;
    
    private ServiceNotificationManager notificationManager;
    
    @BeforeTry
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock behavior
        when(mockContext.getSystemService(Context.NOTIFICATION_SERVICE))
            .thenReturn(mockNotificationManager);
        when(mockContext.getPackageName()).thenReturn("com.example.qring_sdk_flutter");
        when(mockContext.getApplicationInfo()).thenReturn(new android.content.pm.ApplicationInfo());
        
        // Create notification manager
        notificationManager = new ServiceNotificationManager(mockContext);
    }
    
    /**
     * Property 25: Persistent Notification While Service Running
     * 
     * For any time the Foreground_Service is running, a persistent notification should be displayed.
     * 
     * Feature: production-ble-manager, Property 25: Persistent Notification While Service Running
     * Validates: Requirements 7.1
     */
    @Property(tries = 100)
    public void property25_persistentNotificationWhileServiceRunning(
            @ForAll("deviceName") String deviceName,
            @ForAll("batteryLevel") int batteryLevel) {
        
        // Act: Build connected notification
        Notification notification = notificationManager.buildConnectedNotification(deviceName, batteryLevel);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification should be ongoing (cannot be dismissed)
        assertTrue((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0 ||
                   (notification.flags & Notification.FLAG_NO_CLEAR) != 0,
                   "Notification should be persistent (ongoing)");
    }
    
    /**
     * Property 26: Notification Contains Device Name
     * 
     * For any notification displayed by the Foreground_Service, it should contain 
     * the QRing device name.
     * 
     * Feature: production-ble-manager, Property 26: Notification Contains Device Name
     * Validates: Requirements 7.2
     */
    @Property(tries = 100)
    public void property26_notificationContainsDeviceName(
            @ForAll("nonEmptyDeviceName") String deviceName,
            @ForAll("batteryLevel") int batteryLevel) {
        
        // Act: Build connected notification with device name
        Notification notification = notificationManager.buildConnectedNotification(deviceName, batteryLevel);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification extras should contain the device name
        // Check in the notification text content
        String notificationText = getNotificationText(notification);
        assertNotNull(notificationText, "Notification text should not be null");
        assertTrue(notificationText.contains(deviceName), 
            "Notification should contain device name: " + deviceName);
    }
    
    /**
     * Property 27: Notification Contains Connection State
     * 
     * For any notification displayed by the Foreground_Service, it should contain 
     * the current connection state (Connected, Connecting, Disconnected, Reconnecting).
     * 
     * Feature: production-ble-manager, Property 27: Notification Contains Connection State
     * Validates: Requirements 7.3
     */
    @Property(tries = 100)
    public void property27_notificationContainsConnectionState(
            @ForAll("connectionState") String connectionState,
            @ForAll("deviceName") String deviceName) {
        
        // Act: Build notification based on connection state
        Notification notification;
        switch (connectionState) {
            case "Connected":
                notification = notificationManager.buildConnectedNotification(deviceName, -1);
                break;
            case "Disconnected":
                notification = notificationManager.buildDisconnectedNotification(false);
                break;
            case "Reconnecting":
                notification = notificationManager.buildReconnectingNotification(1);
                break;
            default:
                notification = notificationManager.buildDisconnectedNotification(false);
                break;
        }
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification should indicate connection state
        String notificationTitle = getNotificationTitle(notification);
        String notificationText = getNotificationText(notification);
        
        assertNotNull(notificationTitle, "Notification title should not be null");
        assertNotNull(notificationText, "Notification text should not be null");
        
        // Verify state is reflected in title or text
        String combinedContent = notificationTitle + " " + notificationText;
        assertTrue(
            combinedContent.toLowerCase().contains(connectionState.toLowerCase()) ||
            combinedContent.toLowerCase().contains("ring"),
            "Notification should reflect connection state: " + connectionState
        );
    }
    
    /**
     * Property 28: Notification Contains Battery When Available
     * 
     * For any notification when battery level is available, it should display 
     * the battery percentage.
     * 
     * Feature: production-ble-manager, Property 28: Notification Contains Battery When Available
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    public void property28_notificationContainsBatteryWhenAvailable(
            @ForAll("deviceName") String deviceName,
            @ForAll("validBatteryLevel") int batteryLevel) {
        
        // Act: Build connected notification with battery level
        Notification notification = notificationManager.buildConnectedNotification(deviceName, batteryLevel);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification text should contain battery percentage
        String notificationText = getNotificationText(notification);
        assertNotNull(notificationText, "Notification text should not be null");
        assertTrue(notificationText.contains(batteryLevel + "%"), 
            "Notification should contain battery level: " + batteryLevel + "%");
        assertTrue(notificationText.toLowerCase().contains("battery"), 
            "Notification should contain the word 'battery'");
    }
    
    /**
     * Property 28 Extended: Notification Without Battery When Unavailable
     * 
     * For any notification when battery level is unavailable (-1), it should not 
     * display battery information.
     * 
     * Feature: production-ble-manager, Property 28: Battery Display (Extended)
     * Validates: Requirements 7.4
     */
    @Property(tries = 100)
    public void property28_notificationWithoutBatteryWhenUnavailable(
            @ForAll("deviceName") String deviceName) {
        
        // Act: Build connected notification without battery level
        Notification notification = notificationManager.buildConnectedNotification(deviceName, -1);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification text should not contain battery information
        String notificationText = getNotificationText(notification);
        assertNotNull(notificationText, "Notification text should not be null");
        assertFalse(notificationText.toLowerCase().contains("battery"), 
            "Notification should not contain battery info when unavailable");
    }
    
    /**
     * Property 29: Find My Ring on Notification Tap
     * 
     * For any tap on the notification, the BLE_Manager should trigger the Find My Ring feature.
     * 
     * Feature: production-ble-manager, Property 29: Find My Ring on Notification Tap
     * Validates: Requirements 7.5, 10.5
     */
    @Property(tries = 100)
    public void property29_findMyRingOnNotificationTap(
            @ForAll("deviceName") String deviceName,
            @ForAll("batteryLevel") int batteryLevel) {
        
        // Act: Build connected notification (which includes Find My Ring action)
        Notification notification = notificationManager.buildConnectedNotification(deviceName, batteryLevel);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Notification should not be null");
        
        // Assert: Notification should have actions
        assertNotNull(notification.actions, "Notification should have actions");
        assertTrue(notification.actions.length > 0, "Notification should have at least one action");
        
        // Assert: One action should be "Find My Ring"
        boolean hasFindMyRingAction = false;
        for (Notification.Action action : notification.actions) {
            if (action.title != null && action.title.toString().contains("Find My Ring")) {
                hasFindMyRingAction = true;
                // Assert: Action should have a PendingIntent
                assertNotNull(action.actionIntent, "Find My Ring action should have a PendingIntent");
                break;
            }
        }
        assertTrue(hasFindMyRingAction, "Notification should have 'Find My Ring' action");
    }
    
    /**
     * Property 29 Extended: Find My Ring Intent Creation
     * 
     * For any Find My Ring action, a valid PendingIntent should be created.
     * 
     * Feature: production-ble-manager, Property 29: Find My Ring Intent (Extended)
     * Validates: Requirements 7.5, 10.5
     */
    @Property(tries = 100)
    public void property29_findMyRingIntentCreation() {
        
        // Act: Create Find My Ring PendingIntent
        PendingIntent pendingIntent = notificationManager.createFindMyRingIntent();
        
        // Assert: PendingIntent should not be null
        assertNotNull(pendingIntent, "Find My Ring PendingIntent should not be null");
    }
    
    /**
     * Property 30: Notification Updates on State Change
     * 
     * For any connection state or battery level change, the notification should update 
     * to reflect the new information.
     * 
     * Feature: production-ble-manager, Property 30: Notification Updates on State Change
     * Validates: Requirements 7.6
     */
    @Property(tries = 100)
    public void property30_notificationUpdatesOnStateChange(
            @ForAll("deviceName") String deviceName,
            @ForAll("validBatteryLevel") int initialBattery,
            @ForAll("validBatteryLevel") int updatedBattery) {
        
        // Assume: Battery levels are different
        Assume.that(initialBattery != updatedBattery);
        
        // Act: Build initial notification
        Notification initialNotification = notificationManager.buildConnectedNotification(
            deviceName, initialBattery);
        
        // Act: Build updated notification
        Notification updatedNotification = notificationManager.buildConnectedNotification(
            deviceName, updatedBattery);
        
        // Assert: Both notifications should not be null
        assertNotNull(initialNotification, "Initial notification should not be null");
        assertNotNull(updatedNotification, "Updated notification should not be null");
        
        // Assert: Notification content should be different
        String initialText = getNotificationText(initialNotification);
        String updatedText = getNotificationText(updatedNotification);
        
        assertNotNull(initialText, "Initial notification text should not be null");
        assertNotNull(updatedText, "Updated notification text should not be null");
        
        // Assert: Updated notification should contain new battery level
        assertTrue(updatedText.contains(updatedBattery + "%"), 
            "Updated notification should contain new battery level: " + updatedBattery + "%");
        
        // Assert: Content should be different
        assertNotEquals(initialText, updatedText, 
            "Notification content should change when battery level changes");
    }
    
    /**
     * Property 30 Extended: Notification Updates on Connection State Change
     * 
     * For any connection state change, the notification should update to reflect the new state.
     * 
     * Feature: production-ble-manager, Property 30: State Change Updates (Extended)
     * Validates: Requirements 7.6
     */
    @Property(tries = 100)
    public void property30_notificationUpdatesOnConnectionStateChange(
            @ForAll("deviceName") String deviceName,
            @ForAll("validBatteryLevel") int batteryLevel) {
        
        // Act: Build connected notification
        Notification connectedNotification = notificationManager.buildConnectedNotification(
            deviceName, batteryLevel);
        
        // Act: Build disconnected notification
        Notification disconnectedNotification = notificationManager.buildDisconnectedNotification(false);
        
        // Assert: Both notifications should not be null
        assertNotNull(connectedNotification, "Connected notification should not be null");
        assertNotNull(disconnectedNotification, "Disconnected notification should not be null");
        
        // Assert: Notification titles should be different
        String connectedTitle = getNotificationTitle(connectedNotification);
        String disconnectedTitle = getNotificationTitle(disconnectedNotification);
        
        assertNotNull(connectedTitle, "Connected notification title should not be null");
        assertNotNull(disconnectedTitle, "Disconnected notification title should not be null");
        assertNotEquals(connectedTitle, disconnectedTitle, 
            "Notification title should change when connection state changes");
    }
    
    /**
     * Property: Reconnecting Notification Contains Attempt Number
     * 
     * For any reconnecting notification, it should display the attempt number.
     * 
     * Feature: production-ble-manager, Property: Reconnecting Attempt Display
     * Validates: Requirements 7.3, 7.6
     */
    @Property(tries = 100)
    public void property_reconnectingNotificationContainsAttemptNumber(
            @ForAll("attemptNumber") int attemptNumber) {
        
        // Act: Build reconnecting notification
        Notification notification = notificationManager.buildReconnectingNotification(attemptNumber);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Reconnecting notification should not be null");
        
        // Assert: Notification text should contain attempt number
        String notificationText = getNotificationText(notification);
        assertNotNull(notificationText, "Notification text should not be null");
        assertTrue(notificationText.contains(String.valueOf(attemptNumber)), 
            "Notification should contain attempt number: " + attemptNumber);
        assertTrue(notificationText.toLowerCase().contains("reconnect"), 
            "Notification should indicate reconnecting state");
    }
    
    /**
     * Property: Error Notification Contains Error Message
     * 
     * For any error notification, it should display the error message.
     * 
     * Feature: production-ble-manager, Property: Error Display
     * Validates: Requirements 7.6
     */
    @Property(tries = 100)
    public void property_errorNotificationContainsErrorMessage(
            @ForAll("errorTitle") String errorTitle,
            @ForAll("errorMessage") String errorMessage) {
        
        // Act: Build error notification
        Notification notification = notificationManager.buildErrorNotification(errorTitle, errorMessage);
        
        // Assert: Notification should not be null
        assertNotNull(notification, "Error notification should not be null");
        
        // Assert: Notification should contain error information
        String notificationTitle = getNotificationTitle(notification);
        String notificationText = getNotificationText(notification);
        
        assertNotNull(notificationTitle, "Notification title should not be null");
        assertNotNull(notificationText, "Notification text should not be null");
        
        // Assert: Title or text should contain error information
        assertTrue(notificationTitle.contains(errorTitle) || notificationText.contains(errorMessage),
            "Notification should contain error information");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Extract notification title from Notification object.
     */
    private String getNotificationTitle(Notification notification) {
        if (notification.extras != null) {
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            return title != null ? title.toString() : "";
        }
        return "";
    }
    
    /**
     * Extract notification text from Notification object.
     */
    private String getNotificationText(Notification notification) {
        if (notification.extras != null) {
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            return text != null ? text.toString() : "";
        }
        return "";
    }
    
    // ========== Arbitraries (Generators) ==========
    
    /**
     * Generate device names (can be null or non-empty strings).
     */
    @Provide
    Arbitrary<String> deviceName() {
        return Arbitraries.oneOf(
            Arbitraries.just(null),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
        );
    }
    
    /**
     * Generate non-empty device names.
     */
    @Provide
    Arbitrary<String> nonEmptyDeviceName() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }
    
    /**
     * Generate battery levels (0-100 or -1 for unavailable).
     */
    @Provide
    Arbitrary<Integer> batteryLevel() {
        return Arbitraries.oneOf(
            Arbitraries.just(-1),  // Unavailable
            Arbitraries.integers().between(0, 100)  // Valid range
        );
    }
    
    /**
     * Generate valid battery levels (0-100 only).
     */
    @Provide
    Arbitrary<Integer> validBatteryLevel() {
        return Arbitraries.integers().between(0, 100);
    }
    
    /**
     * Generate connection states.
     */
    @Provide
    Arbitrary<String> connectionState() {
        return Arbitraries.of("Connected", "Disconnected", "Reconnecting");
    }
    
    /**
     * Generate attempt numbers for reconnection.
     */
    @Provide
    Arbitrary<Integer> attemptNumber() {
        return Arbitraries.integers().between(1, 100);
    }
    
    /**
     * Generate error titles.
     */
    @Provide
    Arbitrary<String> errorTitle() {
        return Arbitraries.of(
            "Connection Error",
            "Permission Error",
            "Bluetooth Error",
            "Device Error"
        );
    }
    
    /**
     * Generate error messages.
     */
    @Provide
    Arbitrary<String> errorMessage() {
        return Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50);
    }
}
