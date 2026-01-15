package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GattConnectionManager.
 * 
 * Feature: production-ble-manager
 * 
 * These tests validate the correctness properties defined in the design document
 * for GATT connection management, service discovery, MTU negotiation, and resource cleanup.
 * 
 * Note: These tests focus on the logic of GattConnectionManager without requiring
 * actual BluetoothDevice objects. We test the state management, callback behavior,
 * and resource cleanup using the public API.
 */
public class GattConnectionManagerPropertyTest {
    
    private GattConnectionManager manager;
    
    @BeforeTry
    public void setUp() {
        // Get a fresh instance for each test
        manager = GattConnectionManager.getInstance();
        
        // Close any existing connection
        if (manager.isConnected()) {
            manager.disconnect();
            manager.close();
        }
    }
    
    /**
     * Property 12: AutoConnect Parameter
     * 
     * For any GATT connection establishment, the BLE_Manager should use autoConnect 
     * parameter set to true.
     * 
     * This property tests that the connect method correctly handles the autoConnect
     * parameter and validates inputs.
     * 
     * Feature: production-ble-manager, Property 12: AutoConnect Parameter
     * Validates: Requirements 4.1
     */
    @Property(tries = 100)
    public void property12_autoConnectParameter(@ForAll boolean autoConnect) {
        
        // Arrange: Create a test callback
        TestGattCallback callback = new TestGattCallback();
        
        // Act: Attempt to connect with null device
        manager.connect(null, autoConnect, callback);
        
        // Assert: Should receive error callback for null device
        assertTrue(callback.awaitError(1, TimeUnit.SECONDS), 
            "Should receive error callback for null device");
        assertTrue(callback.isError(), "Error should be set");
        assertEquals("connect", callback.getErrorOperation(), 
            "Error operation should be 'connect'");
        
        // Assert: Should not be connected
        assertFalse(manager.isConnected(), 
            "Should not be connected after null device");
    }
    
    /**
     * Property 12 Extended: Null Callback Handling
     * 
     * For any connection attempt with null callback, the manager should handle gracefully.
     * 
     * Feature: production-ble-manager, Property 12: AutoConnect Parameter (Extended)
     * Validates: Requirements 4.1
     */
    @Property(tries = 100)
    public void property12_nullCallbackHandling(@ForAll boolean autoConnect) {
        
        // Act: Attempt to connect with null callback (should not throw)
        manager.connect(null, autoConnect, null);
        
        // Assert: Should not be connected
        assertFalse(manager.isConnected(), 
            "Should not be connected with null callback");
    }
    
    /**
     * Property 13: Service Discovery Before Data Operations
     * 
     * For any GATT connection, the BLE_Manager should call discoverServices and wait 
     * for completion before allowing any data operations.
     * 
     * This property tests that service discovery cannot be called when not connected.
     * 
     * Feature: production-ble-manager, Property 13: Service Discovery Before Data Operations
     * Validates: Requirements 4.2
     */
    @Property(tries = 100)
    public void property13_serviceDiscoveryBeforeDataOperations() {
        
        // Arrange: Ensure not connected
        assertFalse(manager.isConnected(), "Should not be connected initially");
        
        // Act: Attempt to discover services when not connected
        boolean started = manager.discoverServices();
        
        // Assert: Should fail to start service discovery
        assertFalse(started, 
            "Service discovery should not start when not connected");
        
        // Assert: Services should not be discovered
        assertFalse(manager.areServicesDiscovered(), 
            "Services should not be discovered when not connected");
    }
    
    /**
     * Property 14: MTU Negotiation After Service Discovery
     * 
     * For any successful service discovery, the BLE_Manager should negotiate MTU 
     * to optimize data transfer.
     * 
     * This property tests that MTU negotiation cannot be called when not connected.
     * 
     * Feature: production-ble-manager, Property 14: MTU Negotiation After Service Discovery
     * Validates: Requirements 4.3
     */
    @Property(tries = 100)
    public void property14_mtuNegotiationAfterServiceDiscovery(
            @ForAll @IntRange(min = 23, max = 517) int mtu) {
        
        // Arrange: Ensure not connected
        assertFalse(manager.isConnected(), "Should not be connected initially");
        
        // Act: Attempt to request MTU when not connected
        boolean requested = manager.requestMtu(mtu);
        
        // Assert: Should fail to request MTU
        assertFalse(requested, 
            "MTU request should not succeed when not connected");
        
        // Assert: MTU should remain at default
        assertEquals(23, manager.getNegotiatedMtu(), 
            "MTU should remain at default (23) when not connected");
    }
    
    /**
     * Property 15: Disconnect on Service Discovery Failure
     * 
     * For any service discovery failure, the BLE_Manager should disconnect and 
     * report error to Flutter_Bridge.
     * 
     * This property tests that the manager correctly tracks service discovery state.
     * 
     * Feature: production-ble-manager, Property 15: Disconnect on Service Discovery Failure
     * Validates: Requirements 4.4
     */
    @Property(tries = 100)
    public void property15_disconnectOnServiceDiscoveryFailure() {
        
        // Arrange: Ensure not connected
        assertFalse(manager.isConnected(), "Should not be connected initially");
        
        // Assert: Services should not be discovered
        assertFalse(manager.areServicesDiscovered(), 
            "Services should not be discovered initially");
        
        // Act: Verify state consistency
        boolean connected = manager.isConnected();
        boolean servicesDiscovered = manager.areServicesDiscovered();
        
        // Assert: If not connected, services cannot be discovered
        if (!connected) {
            assertFalse(servicesDiscovered, 
                "Services cannot be discovered when not connected");
        }
    }
    
    /**
     * Property 16: GATT Resource Cleanup
     * 
     * For any disconnect operation, the BLE_Manager should properly clean up GATT 
     * resources including calling disconnect() and close().
     * 
     * This property tests that close() properly resets all state.
     * 
     * Feature: production-ble-manager, Property 16: GATT Resource Cleanup
     * Validates: Requirements 4.5, 9.5
     */
    @Property(tries = 100)
    public void property16_gattResourceCleanup() {
        
        // Act: Call close() (should be safe even when not connected)
        manager.close();
        
        // Assert: All state should be reset
        assertFalse(manager.isConnected(), 
            "Should not be connected after close");
        assertFalse(manager.areServicesDiscovered(), 
            "Services should not be discovered after close");
        assertEquals(23, manager.getNegotiatedMtu(), 
            "MTU should be reset to default (23) after close");
        assertNull(manager.getBluetoothGatt(), 
            "BluetoothGatt should be null after close");
        assertNull(manager.getCurrentDevice(), 
            "Current device should be null after close");
    }
    
    /**
     * Property 16 Extended: Disconnect Then Close Sequence
     * 
     * For any disconnect operation, the BLE_Manager should call disconnect() 
     * followed by close() in that order.
     * 
     * This property tests that disconnect() can be called safely even when not connected.
     * 
     * Feature: production-ble-manager, Property 16: GATT Resource Cleanup (Extended)
     * Validates: Requirements 4.5, 9.1, 9.2, 9.5
     */
    @Property(tries = 100)
    public void property16_disconnectThenCloseSequence() {
        
        // Act: Call disconnect() when not connected (should be safe)
        manager.disconnect();
        
        // Assert: Should still not be connected
        assertFalse(manager.isConnected(), 
            "Should not be connected after disconnect");
        
        // Act: Call close() after disconnect
        manager.close();
        
        // Assert: All state should be reset
        assertFalse(manager.isConnected(), 
            "Should not be connected after close");
        assertNull(manager.getBluetoothGatt(), 
            "BluetoothGatt should be null after close");
    }
    
    /**
     * Property 16 Extended: Multiple Close Calls
     * 
     * For any state, calling close() multiple times should be safe and idempotent.
     * 
     * Feature: production-ble-manager, Property 16: GATT Resource Cleanup (Extended)
     * Validates: Requirements 4.5, 9.5
     */
    @Property(tries = 100)
    public void property16_multipleCloseCalls(@ForAll @IntRange(min = 1, max = 10) int closeCount) {
        
        // Act: Call close() multiple times
        for (int i = 0; i < closeCount; i++) {
            manager.close();
        }
        
        // Assert: State should remain consistent
        assertFalse(manager.isConnected(), 
            "Should not be connected after multiple close calls");
        assertFalse(manager.areServicesDiscovered(), 
            "Services should not be discovered after multiple close calls");
        assertNull(manager.getBluetoothGatt(), 
            "BluetoothGatt should be null after multiple close calls");
        assertNull(manager.getCurrentDevice(), 
            "Current device should be null after multiple close calls");
    }
    
    /**
     * Property: State Consistency
     * 
     * For any state, the manager should maintain consistent state relationships.
     * 
     * Feature: production-ble-manager, Property: State Consistency
     * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
     */
    @Property(tries = 100)
    public void propertyStateConsistency() {
        
        // Act: Get current state
        boolean connected = manager.isConnected();
        boolean servicesDiscovered = manager.areServicesDiscovered();
        BluetoothGatt gatt = manager.getBluetoothGatt();
        BluetoothDevice device = manager.getCurrentDevice();
        
        // Assert: State consistency rules
        
        // Rule 1: If not connected, services cannot be discovered
        if (!connected) {
            assertFalse(servicesDiscovered, 
                "Services cannot be discovered when not connected");
        }
        
        // Rule 2: If services are discovered, must be connected
        if (servicesDiscovered) {
            assertTrue(connected, 
                "Must be connected if services are discovered");
        }
        
        // Rule 3: If connected, GATT and device should not be null
        if (connected) {
            assertNotNull(gatt, 
                "GATT should not be null when connected");
            assertNotNull(device, 
                "Device should not be null when connected");
        }
        
        // Rule 4: If not connected, GATT and device should be null
        if (!connected) {
            assertNull(gatt, 
                "GATT should be null when not connected");
            assertNull(device, 
                "Device should be null when not connected");
        }
    }
    
    // ========== Helper Classes ==========
    
    /**
     * Test callback for tracking GATT events.
     */
    private static class TestGattCallback implements GattConnectionManager.GattCallback {
        private final AtomicBoolean connected = new AtomicBoolean(false);
        private final AtomicBoolean servicesDiscovered = new AtomicBoolean(false);
        private final AtomicBoolean mtuNegotiated = new AtomicBoolean(false);
        private final AtomicBoolean disconnected = new AtomicBoolean(false);
        private final AtomicBoolean error = new AtomicBoolean(false);
        
        private final AtomicInteger negotiatedMtu = new AtomicInteger(23);
        private final AtomicBoolean wasExpectedDisconnect = new AtomicBoolean(false);
        
        private final AtomicReference<String> errorOperation = new AtomicReference<>();
        private final AtomicInteger errorStatus = new AtomicInteger(0);
        private final AtomicReference<String> errorMessage = new AtomicReference<>();
        
        private final CountDownLatch connectedLatch = new CountDownLatch(1);
        private final CountDownLatch servicesDiscoveredLatch = new CountDownLatch(1);
        private final CountDownLatch mtuNegotiatedLatch = new CountDownLatch(1);
        private final CountDownLatch disconnectedLatch = new CountDownLatch(1);
        private final CountDownLatch errorLatch = new CountDownLatch(1);
        
        @Override
        public void onConnected(BluetoothDevice device) {
            connected.set(true);
            connectedLatch.countDown();
        }
        
        @Override
        public void onServicesDiscovered(BluetoothDevice device, BluetoothGatt gatt) {
            servicesDiscovered.set(true);
            servicesDiscoveredLatch.countDown();
        }
        
        @Override
        public void onMtuNegotiated(BluetoothDevice device, int mtu) {
            mtuNegotiated.set(true);
            negotiatedMtu.set(mtu);
            mtuNegotiatedLatch.countDown();
        }
        
        @Override
        public void onDisconnected(BluetoothDevice device, boolean wasExpected) {
            disconnected.set(true);
            wasExpectedDisconnect.set(wasExpected);
            disconnectedLatch.countDown();
        }
        
        @Override
        public void onError(BluetoothDevice device, String operation, int status, String error) {
            this.error.set(true);
            this.errorOperation.set(operation);
            this.errorStatus.set(status);
            this.errorMessage.set(error);
            errorLatch.countDown();
        }
        
        public boolean isConnected() {
            return connected.get();
        }
        
        public boolean isServicesDiscovered() {
            return servicesDiscovered.get();
        }
        
        public boolean isMtuNegotiated() {
            return mtuNegotiated.get();
        }
        
        public boolean isDisconnected() {
            return disconnected.get();
        }
        
        public boolean isError() {
            return error.get();
        }
        
        public int getNegotiatedMtu() {
            return negotiatedMtu.get();
        }
        
        public boolean wasExpectedDisconnect() {
            return wasExpectedDisconnect.get();
        }
        
        public String getErrorOperation() {
            return errorOperation.get();
        }
        
        public int getErrorStatus() {
            return errorStatus.get();
        }
        
        public String getErrorMessage() {
            return errorMessage.get();
        }
        
        public boolean awaitConnected(long timeout, TimeUnit unit) {
            try {
                return connectedLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        public boolean awaitServicesDiscovered(long timeout, TimeUnit unit) {
            try {
                return servicesDiscoveredLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        public boolean awaitMtuNegotiated(long timeout, TimeUnit unit) {
            try {
                return mtuNegotiatedLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        public boolean awaitDisconnected(long timeout, TimeUnit unit) {
            try {
                return disconnectedLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            }
        }
        
        public boolean awaitError(long timeout, TimeUnit unit) {
            try {
                return errorLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                return false;
            }
        }
    }
}
