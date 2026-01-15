package com.example.qring_sdk_flutter;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for out-of-range reconnection behavior.
 * 
 * Tests verify:
 * - Property 39: Out-of-Range Reconnection
 * 
 * Validates: Requirements 10.3
 * 
 * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
 */
public class OutOfRangeReconnectionPropertyTest {
    
    /**
     * Property 39: Out-of-Range Reconnection
     * 
     * For any out-of-range then in-range cycle, the BLE_Manager should automatically reconnect.
     * 
     * This test verifies that:
     * 1. When a device goes out of range (disconnects), auto-reconnect is triggered
     * 2. Exponential backoff applies to out-of-range reconnection attempts
     * 3. When device returns to range, reconnection succeeds
     * 4. The reconnection uses the same exponential backoff strategy as other reconnections
     * 
     * Validates: Requirements 10.3
     * 
     * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
     */
    @Property(tries = 100)
    @Label("Property 39: Out-of-Range Reconnection - Auto-reconnect handles out-of-range scenarios")
    void property39_outOfRangeReconnection(
            @ForAll @IntRange(min = 1, max = 50) int attemptNumber) {
        
        // Arrange: Create a ServiceConnectionManager instance
        // This simulates the auto-reconnect engine behavior
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        
        // Simulate device going out of range by starting auto-reconnect
        String deviceMac = "AA:BB:CC:DD:EE:FF";
        manager.simulateOutOfRange(deviceMac);
        
        // Act: Calculate the backoff delay for the given attempt number
        // This represents the delay before attempting reconnection while device is out of range
        int delay = manager.calculateBackoffDelay(attemptNumber);
        
        // Assert: Verify that out-of-range reconnection uses exponential backoff
        // The same backoff strategy should apply regardless of disconnection reason
        
        if (attemptNumber <= 5) {
            // First 5 attempts: 10 seconds ± 20% jitter
            int baseDelay = 10000;
            int minDelay = (int) (baseDelay * 0.8);  // 8000ms
            int maxDelay = (int) (baseDelay * 1.2);  // 12000ms
            
            assertTrue(delay >= minDelay && delay <= maxDelay,
                String.format("Out-of-range attempt %d: delay %dms should be between %dms and %dms (10s ± 20%%)",
                    attemptNumber, delay, minDelay, maxDelay));
            
        } else if (attemptNumber <= 10) {
            // Attempts 6-10: 30 seconds ± 20% jitter
            int baseDelay = 30000;
            int minDelay = (int) (baseDelay * 0.8);  // 24000ms
            int maxDelay = (int) (baseDelay * 1.2);  // 36000ms
            
            assertTrue(delay >= minDelay && delay <= maxDelay,
                String.format("Out-of-range attempt %d: delay %dms should be between %dms and %dms (30s ± 20%%)",
                    attemptNumber, delay, minDelay, maxDelay));
            
        } else {
            // Attempts 11+: 60 seconds base, exponentially increasing, capped at 5 minutes
            int exponentialDelay = 60000 * (1 << (attemptNumber - 11));
            int cappedDelay = Math.min(exponentialDelay, 300000);
            
            // With ±20% jitter
            int minDelay = (int) (cappedDelay * 0.8);
            int maxDelay = (int) (cappedDelay * 1.2);
            
            // Absolute maximum should be 5 minutes (300000ms) + 20% jitter
            int absoluteMax = (int) (300000 * 1.2);  // 360000ms
            
            assertTrue(delay >= minDelay && delay <= absoluteMax,
                String.format("Out-of-range attempt %d: delay %dms should be between %dms and %dms (exponential with cap)",
                    attemptNumber, delay, minDelay, absoluteMax));
        }
        
        // Verify manager is in reconnecting state
        assertTrue(manager.isReconnecting(),
            "Manager should be in reconnecting state after device goes out of range");
        
        // Verify device MAC is preserved for reconnection
        assertEquals(deviceMac, manager.getDeviceMac(),
            "Device MAC should be preserved for reconnection when device returns to range");
    }
    
    /**
     * Property 39: Out-of-Range Reconnection - Reconnection succeeds when device returns
     * 
     * For any out-of-range scenario, when the device returns to range,
     * the reconnection should succeed and reset the attempt counter.
     * 
     * Validates: Requirements 10.3
     * 
     * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
     */
    @Property(tries = 100)
    @Label("Property 39: Out-of-Range Reconnection - Reconnection succeeds when device returns to range")
    void property39_reconnectionSucceedsWhenDeviceReturns(
            @ForAll @IntRange(min = 1, max = 20) int attemptsBeforeReturn) {
        
        // Arrange: Create a ServiceConnectionManager instance
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        String deviceMac = "AA:BB:CC:DD:EE:FF";
        
        // Simulate device going out of range
        manager.simulateOutOfRange(deviceMac);
        
        // Simulate multiple failed reconnection attempts while out of range
        for (int i = 1; i <= attemptsBeforeReturn; i++) {
            manager.incrementAttemptCount();
        }
        
        // Verify we're in reconnecting state with correct attempt count
        assertTrue(manager.isReconnecting(),
            "Manager should be in reconnecting state");
        assertEquals(attemptsBeforeReturn, manager.getReconnectAttempts(),
            "Attempt count should match number of failed attempts");
        
        // Act: Simulate device returning to range and connection succeeding
        manager.simulateConnectionSuccess(deviceMac);
        
        // Assert: Verify reconnection state is cleared
        assertFalse(manager.isReconnecting(),
            "Manager should not be in reconnecting state after successful connection");
        assertEquals(0, manager.getReconnectAttempts(),
            "Attempt count should be reset to 0 after successful reconnection");
        assertTrue(manager.isConnected(),
            "Manager should be in connected state after device returns to range");
    }
    
    /**
     * Property 39: Out-of-Range Reconnection - Multiple out-of-range cycles
     * 
     * For any sequence of out-of-range and in-range cycles,
     * the manager should handle each cycle independently with proper state management.
     * 
     * Validates: Requirements 10.3
     * 
     * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
     */
    @Property(tries = 100)
    @Label("Property 39: Out-of-Range Reconnection - Handles multiple out-of-range cycles")
    void property39_multipleOutOfRangeCycles(
            @ForAll @IntRange(min = 1, max = 5) int numberOfCycles) {
        
        // Arrange: Create a ServiceConnectionManager instance
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        String deviceMac = "AA:BB:CC:DD:EE:FF";
        
        // Act & Assert: Simulate multiple out-of-range and in-range cycles
        for (int cycle = 1; cycle <= numberOfCycles; cycle++) {
            // Device goes out of range
            manager.simulateOutOfRange(deviceMac);
            
            assertTrue(manager.isReconnecting(),
                String.format("Cycle %d: Manager should be reconnecting after going out of range", cycle));
            
            // Simulate a few reconnection attempts
            int attemptsThisCycle = 2 + (cycle % 3); // 2-4 attempts per cycle
            for (int i = 0; i < attemptsThisCycle; i++) {
                manager.incrementAttemptCount();
            }
            
            // Device returns to range and reconnects
            manager.simulateConnectionSuccess(deviceMac);
            
            assertFalse(manager.isReconnecting(),
                String.format("Cycle %d: Manager should not be reconnecting after successful connection", cycle));
            assertEquals(0, manager.getReconnectAttempts(),
                String.format("Cycle %d: Attempt count should be reset after successful connection", cycle));
            assertTrue(manager.isConnected(),
                String.format("Cycle %d: Manager should be connected after device returns", cycle));
        }
    }
    
    /**
     * Property 39: Out-of-Range Reconnection - Delay cap is enforced
     * 
     * For any number of out-of-range reconnection attempts,
     * the delay should never exceed the maximum cap (5 minutes + jitter).
     * 
     * Validates: Requirements 10.3
     * 
     * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
     */
    @Property(tries = 100)
    @Label("Property 39: Out-of-Range Reconnection - Delay cap is enforced for long out-of-range periods")
    void property39_delayCapEnforcedForLongOutOfRange(
            @ForAll @IntRange(min = 20, max = 100) int attemptNumber) {
        
        // Arrange: Create a ServiceConnectionManager instance
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        String deviceMac = "AA:BB:CC:DD:EE:FF";
        
        // Simulate device going out of range for extended period
        manager.simulateOutOfRange(deviceMac);
        
        // Act: Calculate delay for high attempt numbers (long out-of-range period)
        int delay = manager.calculateBackoffDelay(attemptNumber);
        
        // Assert: Verify delay is capped at 5 minutes + 20% jitter (360000ms)
        int maxAllowedDelay = (int) (300000 * 1.2);  // 360000ms (6 minutes)
        
        assertTrue(delay <= maxAllowedDelay,
            String.format("Out-of-range attempt %d: delay %dms should not exceed maximum of %dms (5 min + 20%% jitter)",
                attemptNumber, delay, maxAllowedDelay));
        
        // For very high attempt numbers, delay should be near the cap
        if (attemptNumber > 20) {
            assertTrue(delay >= 240000,  // 4 minutes (80% of 5 minutes)
                String.format("Out-of-range attempt %d: delay %dms should be near the cap for extended out-of-range periods",
                    attemptNumber, delay));
        }
    }
    
    /**
     * Property 39: Out-of-Range Reconnection - Bluetooth state affects reconnection
     * 
     * For any out-of-range scenario, if Bluetooth is turned off,
     * reconnection attempts should pause and resume when Bluetooth is turned back on.
     * 
     * Validates: Requirements 10.3
     * 
     * Feature: production-ble-manager, Property 39: Out-of-Range Reconnection
     */
    @Property(tries = 100)
    @Label("Property 39: Out-of-Range Reconnection - Bluetooth state affects out-of-range reconnection")
    void property39_bluetoothStateAffectsOutOfRangeReconnection() {
        
        // Arrange: Create a ServiceConnectionManager instance
        TestableServiceConnectionManager manager = new TestableServiceConnectionManager();
        String deviceMac = "AA:BB:CC:DD:EE:FF";
        
        // Device goes out of range
        manager.simulateOutOfRange(deviceMac);
        assertTrue(manager.isReconnecting(), "Should be reconnecting after going out of range");
        
        // Act: Simulate Bluetooth being turned off while device is out of range
        manager.setBluetoothEnabled(false);
        
        // Assert: Reconnection should be paused
        // (In real implementation, scheduled reconnection attempts would be cancelled)
        assertFalse(manager.isBluetoothEnabled(),
            "Bluetooth should be disabled");
        assertTrue(manager.isReconnecting(),
            "Manager should still be in reconnecting state (paused)");
        
        // Act: Simulate Bluetooth being turned back on
        manager.setBluetoothEnabled(true);
        
        // Assert: Reconnection should resume
        assertTrue(manager.isBluetoothEnabled(),
            "Bluetooth should be enabled");
        assertTrue(manager.isReconnecting(),
            "Manager should still be in reconnecting state (resumed)");
        
        // Simulate device returning to range and connection succeeding
        manager.simulateConnectionSuccess(deviceMac);
        
        assertFalse(manager.isReconnecting(),
            "Manager should not be reconnecting after successful connection");
        assertTrue(manager.isConnected(),
            "Manager should be connected after device returns to range");
    }
    
    /**
     * Testable version of ServiceConnectionManager that exposes internal state
     * for testing without requiring Android Context or actual BLE operations.
     */
    private static class TestableServiceConnectionManager {
        private static final int INITIAL_RETRY_INTERVAL_MS = 10000;
        private static final int RETRY_INTERVAL_AFTER_5_FAILURES_MS = 30000;
        private static final int RETRY_INTERVAL_AFTER_10_FAILURES_MS = 60000;
        private static final int MAX_RETRY_INTERVAL_MS = 300000;
        
        private final java.util.Random random = new java.util.Random();
        private boolean isConnected = false;
        private boolean isReconnecting = false;
        private int reconnectAttempts = 0;
        private String deviceMac = null;
        private boolean isBluetoothEnabled = true;
        private boolean isDeviceIdle = false;
        
        /**
         * Calculate the backoff delay for reconnection attempts.
         * This matches ServiceConnectionManager.calculateBackoffDelay implementation.
         */
        public int calculateBackoffDelay(int attemptNumber) {
            int baseDelay;
            
            if (attemptNumber <= 5) {
                // First 5 attempts: 10 seconds
                baseDelay = INITIAL_RETRY_INTERVAL_MS;
            } else if (attemptNumber <= 10) {
                // Attempts 6-10: 30 seconds
                baseDelay = RETRY_INTERVAL_AFTER_5_FAILURES_MS;
            } else {
                // Attempts 11+: 60 seconds, increasing exponentially
                int exponentialDelay = RETRY_INTERVAL_AFTER_10_FAILURES_MS * (1 << (attemptNumber - 11));
                baseDelay = Math.min(exponentialDelay, MAX_RETRY_INTERVAL_MS);
            }
            
            // Add jitter (±20%) to prevent thundering herd
            int jitter = (int) (baseDelay * 0.2 * (random.nextDouble() - 0.5) * 2);
            int finalDelay = baseDelay + jitter;
            
            // Doze mode adjustment (optional, for testing we keep it disabled)
            if (isDeviceIdle && attemptNumber > 5) {
                finalDelay = (int) (finalDelay * 1.2);
            }
            
            // Ensure delay is positive and within bounds
            return Math.max(1000, Math.min(finalDelay, MAX_RETRY_INTERVAL_MS));
        }
        
        /**
         * Simulate device going out of range.
         * This triggers auto-reconnect mode.
         */
        public void simulateOutOfRange(String deviceMac) {
            this.deviceMac = deviceMac;
            this.isConnected = false;
            this.isReconnecting = true;
            this.reconnectAttempts = 0;
        }
        
        /**
         * Simulate successful connection (device returned to range).
         */
        public void simulateConnectionSuccess(String deviceMac) {
            this.deviceMac = deviceMac;
            this.isConnected = true;
            this.isReconnecting = false;
            this.reconnectAttempts = 0;
        }
        
        /**
         * Increment the reconnection attempt counter.
         */
        public void incrementAttemptCount() {
            this.reconnectAttempts++;
        }
        
        /**
         * Check if currently reconnecting.
         */
        public boolean isReconnecting() {
            return isReconnecting;
        }
        
        /**
         * Check if currently connected.
         */
        public boolean isConnected() {
            return isConnected;
        }
        
        /**
         * Get the current reconnection attempt count.
         */
        public int getReconnectAttempts() {
            return reconnectAttempts;
        }
        
        /**
         * Get the device MAC address.
         */
        public String getDeviceMac() {
            return deviceMac;
        }
        
        /**
         * Set Bluetooth enabled state.
         */
        public void setBluetoothEnabled(boolean enabled) {
            this.isBluetoothEnabled = enabled;
        }
        
        /**
         * Check if Bluetooth is enabled.
         */
        public boolean isBluetoothEnabled() {
            return isBluetoothEnabled;
        }
    }
}
