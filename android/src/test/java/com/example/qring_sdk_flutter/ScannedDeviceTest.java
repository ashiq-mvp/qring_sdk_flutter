package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScannedDevice model.
 * 
 * Tests verify:
 * - RSSI update threshold (5 dBm)
 * - Timestamp updates
 * - Map conversion for Flutter bridge
 * - Equality based on MAC address
 * 
 * Validates: Requirements 6.1, 6.2, 6.3, 6.5
 */
public class ScannedDeviceTest {
    
    @Mock
    private BluetoothDevice mockDevice;
    
    private static final String TEST_MAC_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String TEST_DEVICE_NAME = "Q_Ring_Test";
    private static final int TEST_RSSI = -70;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockDevice.getAddress()).thenReturn(TEST_MAC_ADDRESS);
        when(mockDevice.getName()).thenReturn(TEST_DEVICE_NAME);
    }
    
    /**
     * Test RSSI update threshold - significant change (>= 5 dBm).
     * 
     * Validates: Requirements 6.3
     */
    @Test
    public void testRssiUpdateThreshold_SignificantChange() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        
        // Test change of exactly 5 dBm (should be significant)
        boolean result1 = device.updateRssi(TEST_RSSI + 5);
        assertTrue("RSSI change of 5 dBm should be significant", result1);
        assertEquals("RSSI should be updated", TEST_RSSI + 5, device.getRssi());
        
        // Test change of more than 5 dBm (should be significant)
        boolean result2 = device.updateRssi(TEST_RSSI + 10);
        assertTrue("RSSI change of 10 dBm should be significant", result2);
        assertEquals("RSSI should be updated", TEST_RSSI + 10, device.getRssi());
        
        // Test negative change of 5 dBm (should be significant)
        boolean result3 = device.updateRssi(TEST_RSSI);
        assertTrue("RSSI change of -10 dBm should be significant", result3);
        assertEquals("RSSI should be updated", TEST_RSSI, device.getRssi());
    }
    
    /**
     * Test RSSI update threshold - insignificant change (< 5 dBm).
     * 
     * Validates: Requirements 6.3
     */
    @Test
    public void testRssiUpdateThreshold_InsignificantChange() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        
        // Test change of less than 5 dBm (should not be significant)
        boolean result1 = device.updateRssi(TEST_RSSI + 3);
        assertFalse("RSSI change of 3 dBm should not be significant", result1);
        assertEquals("RSSI should still be updated", TEST_RSSI + 3, device.getRssi());
        
        // Test change of 4 dBm (should not be significant)
        boolean result2 = device.updateRssi(TEST_RSSI + 7);
        assertFalse("RSSI change of 4 dBm should not be significant", result2);
        assertEquals("RSSI should still be updated", TEST_RSSI + 7, device.getRssi());
        
        // Test no change (should not be significant)
        boolean result3 = device.updateRssi(TEST_RSSI + 7);
        assertFalse("RSSI change of 0 dBm should not be significant", result3);
        assertEquals("RSSI should remain the same", TEST_RSSI + 7, device.getRssi());
    }
    
    /**
     * Test that timestamp is updated on RSSI update.
     * 
     * Validates: Requirements 6.5
     */
    @Test
    public void testTimestampUpdates() throws InterruptedException {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        long initialTimestamp = device.getLastSeenTimestamp();
        
        // Wait a bit to ensure timestamp difference
        Thread.sleep(10);
        
        // Update RSSI (regardless of significance)
        device.updateRssi(TEST_RSSI + 2);
        long updatedTimestamp = device.getLastSeenTimestamp();
        
        assertTrue("Timestamp should be updated after RSSI update",
                   updatedTimestamp > initialTimestamp);
    }
    
    /**
     * Test timestamp is set on construction.
     * 
     * Validates: Requirements 6.5
     */
    @Test
    public void testTimestampSetOnConstruction() {
        long beforeCreation = System.currentTimeMillis();
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        long afterCreation = System.currentTimeMillis();
        
        long timestamp = device.getLastSeenTimestamp();
        
        assertTrue("Timestamp should be set during construction",
                   timestamp >= beforeCreation && timestamp <= afterCreation);
    }
    
    /**
     * Test map conversion with valid device name.
     * 
     * Validates: Requirements 6.1, 6.2, 6.3, 6.5
     */
    @Test
    public void testMapConversion_WithDeviceName() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        Map<String, Object> map = device.toMap();
        
        assertNotNull("Map should not be null", map);
        assertEquals("Map should contain device name", TEST_DEVICE_NAME, map.get("name"));
        assertEquals("Map should contain MAC address", TEST_MAC_ADDRESS, map.get("macAddress"));
        assertEquals("Map should contain RSSI", TEST_RSSI, map.get("rssi"));
        assertNotNull("Map should contain timestamp", map.get("lastSeen"));
        assertTrue("Timestamp should be a long", map.get("lastSeen") instanceof Long);
    }
    
    /**
     * Test map conversion with null device name.
     * 
     * Validates: Requirements 6.1, 6.2, 6.3, 6.5
     */
    @Test
    public void testMapConversion_WithNullDeviceName() {
        when(mockDevice.getName()).thenReturn(null);
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        Map<String, Object> map = device.toMap();
        
        assertNotNull("Map should not be null", map);
        assertEquals("Map should contain 'Unknown Device' for null name", 
                     "Unknown Device", map.get("name"));
        assertEquals("Map should contain MAC address", TEST_MAC_ADDRESS, map.get("macAddress"));
        assertEquals("Map should contain RSSI", TEST_RSSI, map.get("rssi"));
        assertNotNull("Map should contain timestamp", map.get("lastSeen"));
    }
    
    /**
     * Test equality based on MAC address - same MAC.
     * 
     * Validates: Requirements 6.1
     */
    @Test
    public void testEquality_SameMacAddress() {
        BluetoothDevice mockDevice2 = mock(BluetoothDevice.class);
        when(mockDevice2.getAddress()).thenReturn(TEST_MAC_ADDRESS);
        when(mockDevice2.getName()).thenReturn("Different_Name");
        
        ScannedDevice device1 = new ScannedDevice(mockDevice, TEST_RSSI);
        ScannedDevice device2 = new ScannedDevice(mockDevice2, TEST_RSSI + 10);
        
        assertEquals("Devices with same MAC should be equal", device1, device2);
        assertEquals("Hash codes should match for same MAC", 
                     device1.hashCode(), device2.hashCode());
    }
    
    /**
     * Test equality based on MAC address - different MAC.
     * 
     * Validates: Requirements 6.1
     */
    @Test
    public void testEquality_DifferentMacAddress() {
        BluetoothDevice mockDevice2 = mock(BluetoothDevice.class);
        when(mockDevice2.getAddress()).thenReturn("11:22:33:44:55:66");
        when(mockDevice2.getName()).thenReturn(TEST_DEVICE_NAME);
        
        ScannedDevice device1 = new ScannedDevice(mockDevice, TEST_RSSI);
        ScannedDevice device2 = new ScannedDevice(mockDevice2, TEST_RSSI);
        
        assertNotEquals("Devices with different MAC should not be equal", device1, device2);
    }
    
    /**
     * Test equality with same object.
     */
    @Test
    public void testEquality_SameObject() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        
        assertEquals("Device should equal itself", device, device);
    }
    
    /**
     * Test equality with null.
     */
    @Test
    public void testEquality_WithNull() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        
        assertNotEquals("Device should not equal null", device, null);
    }
    
    /**
     * Test equality with different class.
     */
    @Test
    public void testEquality_WithDifferentClass() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        String notADevice = "Not a device";
        
        assertNotEquals("Device should not equal different class", device, notADevice);
    }
    
    /**
     * Test getters return correct values.
     * 
     * Validates: Requirements 6.1, 6.2, 6.3, 6.5
     */
    @Test
    public void testGetters() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        
        assertEquals("getMacAddress should return correct value", 
                     TEST_MAC_ADDRESS, device.getMacAddress());
        assertEquals("getName should return correct value", 
                     TEST_DEVICE_NAME, device.getName());
        assertEquals("getRssi should return correct value", 
                     TEST_RSSI, device.getRssi());
        assertNotNull("getDevice should return device", device.getDevice());
        assertEquals("getDevice should return the same device", 
                     mockDevice, device.getDevice());
        assertTrue("getLastSeenTimestamp should return positive value", 
                   device.getLastSeenTimestamp() > 0);
    }
    
    /**
     * Test toString method.
     */
    @Test
    public void testToString() {
        ScannedDevice device = new ScannedDevice(mockDevice, TEST_RSSI);
        String result = device.toString();
        
        assertNotNull("toString should not return null", result);
        assertTrue("toString should contain MAC address", 
                   result.contains(TEST_MAC_ADDRESS));
        assertTrue("toString should contain device name", 
                   result.contains(TEST_DEVICE_NAME));
        assertTrue("toString should contain RSSI", 
                   result.contains(String.valueOf(TEST_RSSI)));
    }
}
