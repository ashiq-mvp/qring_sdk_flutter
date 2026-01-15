package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for BluetoothReceiver.
 * 
 * Tests verify:
 * - Property 24: Bluetooth ON Service Restart
 * 
 * Validates: Requirements 6.4
 * 
 * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
 */
public class BluetoothReceiverPropertyTest {
    
    /**
     * Property 24: Bluetooth ON Service Restart
     * 
     * For any Bluetooth state change to ON when a QRing was previously connected,
     * the Foreground_Service should restart automatically.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Service restarts when Bluetooth turns ON")
    void bluetoothReceiverRestartsServiceWhenBluetoothTurnsOn(
            @ForAll @AlphaChars @StringLength(min = 17, max = 17) String macAddress) {
        
        // Arrange: Format MAC address properly (XX:XX:XX:XX:XX:XX)
        String formattedMac = formatMacAddress(macAddress);
        
        // Create mock context and SharedPreferences
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        
        // Save device MAC to simulate previous connection
        prefs.edit().putString("device_mac", formattedMac).apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Create Bluetooth STATE_CHANGED intent with STATE_ON
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        
        // Act: Trigger Bluetooth receiver
        receiver.onReceive(context, intent);
        
        // Assert: Verify service was started
        assertTrue(context.wasServiceStarted(), 
                  "Service should be started when Bluetooth turns ON and device was previously connected");
        
        // Verify the correct service was started
        assertEquals(QRingBackgroundService.class.getName(), 
                    context.getStartedServiceClassName(),
                    "QRingBackgroundService should be started");
        
        // Verify device MAC was passed to service
        assertEquals(formattedMac, 
                    context.getStartedServiceExtra(NotificationConfig.EXTRA_DEVICE_MAC),
                    "Device MAC should be passed to service");
    }
    
    /**
     * Property 24: Bluetooth ON Service Restart - No restart without saved device
     * 
     * For any Bluetooth state change to ON when no QRing was previously connected,
     * the service should NOT restart.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Service does not restart when no device was connected")
    void bluetoothReceiverDoesNotRestartServiceWhenNoDeviceWasConnected() {
        
        // Arrange: Create mock context with no saved device
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        
        // Ensure no device MAC is saved
        prefs.edit().clear().apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Create Bluetooth STATE_CHANGED intent with STATE_ON
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        
        // Act: Trigger Bluetooth receiver
        receiver.onReceive(context, intent);
        
        // Assert: Verify service was NOT started
        assertFalse(context.wasServiceStarted(), 
                   "Service should NOT be started when Bluetooth turns ON and no device was previously connected");
    }
    
    /**
     * Property 24: Bluetooth ON Service Restart - No restart when Bluetooth turns OFF
     * 
     * For any Bluetooth state change to OFF, the service should NOT restart.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Service does not restart when Bluetooth turns OFF")
    void bluetoothReceiverDoesNotRestartServiceWhenBluetoothTurnsOff(
            @ForAll @AlphaChars @StringLength(min = 17, max = 17) String macAddress) {
        
        // Arrange: Format MAC address properly
        String formattedMac = formatMacAddress(macAddress);
        
        // Create mock context with saved device
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        prefs.edit().putString("device_mac", formattedMac).apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Create Bluetooth STATE_CHANGED intent with STATE_OFF
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_ON);
        
        // Act: Trigger Bluetooth receiver
        receiver.onReceive(context, intent);
        
        // Assert: Verify service was NOT started
        assertFalse(context.wasServiceStarted(), 
                   "Service should NOT be started when Bluetooth turns OFF");
    }
    
    /**
     * Property 24: Bluetooth ON Service Restart - Handles null intent gracefully
     * 
     * For any null intent, the receiver should handle it gracefully without crashing.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Handles null intent gracefully")
    void bluetoothReceiverHandlesNullIntentGracefully(
            @ForAll @AlphaChars @StringLength(min = 17, max = 17) String macAddress) {
        
        // Arrange: Format MAC address properly
        String formattedMac = formatMacAddress(macAddress);
        
        // Create mock context with saved device
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        prefs.edit().putString("device_mac", formattedMac).apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Act & Assert: Should not crash with null intent
        assertDoesNotThrow(() -> receiver.onReceive(context, null),
                          "Receiver should handle null intent gracefully");
        
        // Verify service was NOT started
        assertFalse(context.wasServiceStarted(), 
                   "Service should NOT be started with null intent");
    }
    
    /**
     * Property 24: Bluetooth ON Service Restart - Handles empty MAC gracefully
     * 
     * For any empty MAC address, the receiver should not restart the service.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Handles empty MAC gracefully")
    void bluetoothReceiverHandlesEmptyMacGracefully() {
        
        // Arrange: Create mock context with empty MAC
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        prefs.edit().putString("device_mac", "").apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Create Bluetooth STATE_CHANGED intent with STATE_ON
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        
        // Act: Trigger Bluetooth receiver
        receiver.onReceive(context, intent);
        
        // Assert: Verify service was NOT started
        assertFalse(context.wasServiceStarted(), 
                   "Service should NOT be started with empty MAC address");
    }
    
    /**
     * Property 24: Bluetooth ON Service Restart - Handles intermediate states
     * 
     * For any intermediate Bluetooth states (TURNING_ON, TURNING_OFF),
     * the service should NOT restart.
     * 
     * Validates: Requirements 6.4
     * 
     * Feature: production-ble-manager, Property 24: Bluetooth ON Service Restart
     */
    @Property(tries = 100)
    @Label("Property 24: Bluetooth ON Service Restart - Handles intermediate states")
    void bluetoothReceiverHandlesIntermediateStates(
            @ForAll @AlphaChars @StringLength(min = 17, max = 17) String macAddress,
            @ForAll("intermediateBluetoothStates") int intermediateState) {
        
        // Arrange: Format MAC address properly
        String formattedMac = formatMacAddress(macAddress);
        
        // Create mock context with saved device
        MockContext context = new MockContext();
        SharedPreferences prefs = context.getSharedPreferences("qring_service_state", Context.MODE_PRIVATE);
        prefs.edit().putString("device_mac", formattedMac).apply();
        
        // Create BluetoothReceiver
        BluetoothReceiver receiver = new BluetoothReceiver();
        
        // Create Bluetooth STATE_CHANGED intent with intermediate state
        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, intermediateState);
        intent.putExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, BluetoothAdapter.STATE_OFF);
        
        // Act: Trigger Bluetooth receiver
        receiver.onReceive(context, intent);
        
        // Assert: Verify service was NOT started for intermediate states
        assertFalse(context.wasServiceStarted(), 
                   "Service should NOT be started for intermediate Bluetooth states");
    }
    
    /**
     * Arbitrary provider for intermediate Bluetooth states.
     */
    @Provide
    Arbitrary<Integer> intermediateBluetoothStates() {
        return Arbitraries.of(
            BluetoothAdapter.STATE_TURNING_ON,
            BluetoothAdapter.STATE_TURNING_OFF
        );
    }
    
    /**
     * Helper method to format MAC address with colons.
     * Converts "AABBCCDDEEFF" to "AA:BB:CC:DD:EE:FF"
     */
    private String formatMacAddress(String macAddress) {
        if (macAddress == null || macAddress.length() != 17) {
            // Generate a valid MAC if input is invalid
            macAddress = "AABBCCDDEEFF0000".substring(0, 17);
        }
        
        // Remove any existing colons
        String cleaned = macAddress.replaceAll(":", "");
        
        // Ensure we have at least 12 characters
        if (cleaned.length() < 12) {
            cleaned = (cleaned + "000000000000").substring(0, 12);
        }
        
        // Format as XX:XX:XX:XX:XX:XX
        return String.format("%s:%s:%s:%s:%s:%s",
                           cleaned.substring(0, 2),
                           cleaned.substring(2, 4),
                           cleaned.substring(4, 6),
                           cleaned.substring(6, 8),
                           cleaned.substring(8, 10),
                           cleaned.substring(10, 12));
    }
    
    /**
     * Mock Context for testing.
     * Tracks service start calls and provides SharedPreferences.
     */
    private static class MockContext extends android.content.ContextWrapper {
        private boolean serviceStarted = false;
        private String startedServiceClassName = null;
        private Intent startedServiceIntent = null;
        private SharedPreferences sharedPreferences;
        
        public MockContext() {
            super(null);
            this.sharedPreferences = new MockSharedPreferences();
        }
        
        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return sharedPreferences;
        }
        
        @Override
        public android.content.ComponentName startService(Intent service) {
            serviceStarted = true;
            startedServiceIntent = service;
            if (service.getComponent() != null) {
                startedServiceClassName = service.getComponent().getClassName();
            }
            return new android.content.ComponentName(this, QRingBackgroundService.class);
        }
        
        @Override
        public android.content.ComponentName startForegroundService(Intent service) {
            return startService(service);
        }
        
        public boolean wasServiceStarted() {
            return serviceStarted;
        }
        
        public String getStartedServiceClassName() {
            return startedServiceClassName;
        }
        
        public String getStartedServiceExtra(String key) {
            if (startedServiceIntent != null) {
                return startedServiceIntent.getStringExtra(key);
            }
            return null;
        }
    }
    
    /**
     * Mock SharedPreferences for testing.
     */
    private static class MockSharedPreferences implements SharedPreferences {
        private java.util.Map<String, Object> data = new java.util.HashMap<>();
        
        @Override
        public java.util.Map<String, ?> getAll() {
            return data;
        }
        
        @Override
        public String getString(String key, String defValue) {
            Object value = data.get(key);
            return value != null ? (String) value : defValue;
        }
        
        @Override
        public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) {
            return defValues;
        }
        
        @Override
        public int getInt(String key, int defValue) {
            Object value = data.get(key);
            return value != null ? (Integer) value : defValue;
        }
        
        @Override
        public long getLong(String key, long defValue) {
            Object value = data.get(key);
            return value != null ? (Long) value : defValue;
        }
        
        @Override
        public float getFloat(String key, float defValue) {
            Object value = data.get(key);
            return value != null ? (Float) value : defValue;
        }
        
        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object value = data.get(key);
            return value != null ? (Boolean) value : defValue;
        }
        
        @Override
        public boolean contains(String key) {
            return data.containsKey(key);
        }
        
        @Override
        public Editor edit() {
            return new MockEditor();
        }
        
        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
        
        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
        
        private class MockEditor implements Editor {
            private java.util.Map<String, Object> changes = new java.util.HashMap<>();
            
            @Override
            public Editor putString(String key, String value) {
                changes.put(key, value);
                return this;
            }
            
            @Override
            public Editor putStringSet(String key, java.util.Set<String> values) {
                changes.put(key, values);
                return this;
            }
            
            @Override
            public Editor putInt(String key, int value) {
                changes.put(key, value);
                return this;
            }
            
            @Override
            public Editor putLong(String key, long value) {
                changes.put(key, value);
                return this;
            }
            
            @Override
            public Editor putFloat(String key, float value) {
                changes.put(key, value);
                return this;
            }
            
            @Override
            public Editor putBoolean(String key, boolean value) {
                changes.put(key, value);
                return this;
            }
            
            @Override
            public Editor remove(String key) {
                changes.remove(key);
                data.remove(key);
                return this;
            }
            
            @Override
            public Editor clear() {
                changes.clear();
                data.clear();
                return this;
            }
            
            @Override
            public boolean commit() {
                data.putAll(changes);
                return true;
            }
            
            @Override
            public void apply() {
                data.putAll(changes);
            }
        }
    }
}
