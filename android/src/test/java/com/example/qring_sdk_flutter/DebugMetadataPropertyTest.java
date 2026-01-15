package com.example.qring_sdk_flutter;

import android.bluetooth.BluetoothDevice;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for debug metadata in BLE scan filtering.
 * 
 * Feature: sdk-driven-ble-scan-filtering
 * 
 * These tests validate Property 19: Debug Metadata Presence
 * 
 * Requirement 6.4: WHERE debugging is enabled, THE Native_Layer SHALL provide raw advertisement metadata
 */
public class DebugMetadataPropertyTest {
    
    private BleScanFilter scanFilter;
    private List<ScannedDevice> emittedDevices;
    
    @BeforeTry
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        scanFilter = new BleScanFilter();
        emittedDevices = new ArrayList<>();
        
        // Set up callback to capture emitted devices
        scanFilter.setCallback(device -> emittedDevices.add(device));
    }
    
    /**
     * Property 19: Debug Metadata Presence
     * 
     * For any device emitted by Native_Layer when debugging is enabled, it should 
     * include raw advertisement metadata.
     * 
     * Feature: sdk-driven-ble-scan-filtering, Property 19: Debug Metadata Presence
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property19_debugMetadataPresence(
            @ForAll("qringDeviceName") String deviceName,
            @ForAll("validMacAddress") String macAddress,
            @ForAll @IntRange(min = -100, max = 0) int rssi,
            @ForAll("rawAdvertisementData") byte[] scanRecord) {
        
        // Arrange: Enable debug mode
        scanFilter.setDebugEnabled(true);
        
        // Arrange: Create device
        BluetoothDevice device = mockDevice(deviceName, macAddress);
        
        // Act: Handle device with scan record
        scanFilter.handleDiscoveredDevice(device, rssi, scanRecord);
        
        // Assert: Device should be emitted
        assertEquals(1, emittedDevices.size(), 
            "Valid device should be emitted");
        
        // Assert: Emitted device should have debug metadata
        ScannedDevice emittedDevice = emittedDevices.get(0);
        assertTrue(emittedDevice.hasDebugMetadata(), 
            "Device emitted in debug mode should have debug metadata");
        
        // Assert: Raw advertisement data should be present
        assertNotNull(emittedDevice.getRawAdvertisementData(), 
            "Raw advertisement data should be present in debug mode");
        
        // Assert: Raw advertisement data should match input
        assertArrayEquals(scanRecord, emittedDevice.getRawAdvertisementData(), 
            "Raw advertisement data should match the scan record");
        
        // Assert: Map representation should include raw advertisement data
        Map<String, Object> deviceMap = emittedDevice.toMap();
        assertTrue(deviceMap.containsKey("rawAdvertisementData"), 
            "Device map should contain rawAdvertisementData field in debug mode");
    }
    
    /**
     * Property: No Debug Metadata When Debug Disabled
     * 
     * For any device emitted by Native_Layer when debugging is disabled, it should 
     * NOT include raw advertisement metadata.
     * 
     * Feature: sdk-driven-ble-scan-filtering
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property_noDebugMetadataWhenDebugDisabled(
            @ForAll("qringDeviceName") String deviceName,
            @ForAll("validMacAddress") String macAddress,
            @ForAll @IntRange(min = -100, max = 0) int rssi,
            @ForAll("rawAdvertisementData") byte[] scanRecord) {
        
        // Arrange: Ensure debug mode is disabled (default)
        scanFilter.setDebugEnabled(false);
        
        // Arrange: Create device
        BluetoothDevice device = mockDevice(deviceName, macAddress);
        
        // Act: Handle device with scan record
        scanFilter.handleDiscoveredDevice(device, rssi, scanRecord);
        
        // Assert: Device should be emitted
        assertEquals(1, emittedDevices.size(), 
            "Valid device should be emitted");
        
        // Assert: Emitted device should NOT have debug metadata
        ScannedDevice emittedDevice = emittedDevices.get(0);
        assertFalse(emittedDevice.hasDebugMetadata(), 
            "Device emitted without debug mode should not have debug metadata");
        
        // Assert: Raw advertisement data should be null
        assertNull(emittedDevice.getRawAdvertisementData(), 
            "Raw advertisement data should be null when debug is disabled");
        
        // Assert: Map representation should NOT include raw advertisement data
        Map<String, Object> deviceMap = emittedDevice.toMap();
        assertFalse(deviceMap.containsKey("rawAdvertisementData"), 
            "Device map should not contain rawAdvertisementData field when debug is disabled");
    }
    
    /**
     * Property: Debug Mode Toggle
     * 
     * For any scan filter, enabling and disabling debug mode should affect 
     * whether debug metadata is included in emitted devices.
     * 
     * Feature: sdk-driven-ble-scan-filtering
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property_debugModeToggle(
            @ForAll("qringDeviceName") String deviceName1,
            @ForAll("qringDeviceName") String deviceName2,
            @ForAll("validMacAddress") String macAddress1,
            @ForAll("validMacAddress") String macAddress2,
            @ForAll @IntRange(min = -100, max = 0) int rssi,
            @ForAll("rawAdvertisementData") byte[] scanRecord) {
        
        // Arrange: Create two different devices
        BluetoothDevice device1 = mockDevice(deviceName1, macAddress1);
        BluetoothDevice device2 = mockDevice(deviceName2, macAddress2);
        
        // Act: Handle first device with debug disabled
        scanFilter.setDebugEnabled(false);
        scanFilter.handleDiscoveredDevice(device1, rssi, scanRecord);
        
        // Assert: First device should not have debug metadata
        assertEquals(1, emittedDevices.size());
        assertFalse(emittedDevices.get(0).hasDebugMetadata(), 
            "First device should not have debug metadata");
        
        // Act: Enable debug and handle second device
        scanFilter.setDebugEnabled(true);
        scanFilter.handleDiscoveredDevice(device2, rssi, scanRecord);
        
        // Assert: Second device should have debug metadata
        assertEquals(2, emittedDevices.size());
        assertTrue(emittedDevices.get(1).hasDebugMetadata(), 
            "Second device should have debug metadata after enabling debug");
    }
    
    /**
     * Property: Debug Metadata With Null Scan Record
     * 
     * For any device with null scan record, debug metadata should handle it gracefully.
     * 
     * Feature: sdk-driven-ble-scan-filtering
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property_debugMetadataWithNullScanRecord(
            @ForAll("qringDeviceName") String deviceName,
            @ForAll("validMacAddress") String macAddress,
            @ForAll @IntRange(min = -100, max = 0) int rssi) {
        
        // Arrange: Enable debug mode
        scanFilter.setDebugEnabled(true);
        
        // Arrange: Create device
        BluetoothDevice device = mockDevice(deviceName, macAddress);
        
        // Act: Handle device with null scan record
        scanFilter.handleDiscoveredDevice(device, rssi, null);
        
        // Assert: Device should be emitted
        assertEquals(1, emittedDevices.size(), 
            "Valid device should be emitted even with null scan record");
        
        // Assert: Emitted device should not have debug metadata (null scan record)
        ScannedDevice emittedDevice = emittedDevices.get(0);
        assertFalse(emittedDevice.hasDebugMetadata(), 
            "Device with null scan record should not have debug metadata");
    }
    
    /**
     * Property: Debug Metadata With Empty Scan Record
     * 
     * For any device with empty scan record, debug metadata should handle it gracefully.
     * 
     * Feature: sdk-driven-ble-scan-filtering
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property_debugMetadataWithEmptyScanRecord(
            @ForAll("qringDeviceName") String deviceName,
            @ForAll("validMacAddress") String macAddress,
            @ForAll @IntRange(min = -100, max = 0) int rssi) {
        
        // Arrange: Enable debug mode
        scanFilter.setDebugEnabled(true);
        
        // Arrange: Create device
        BluetoothDevice device = mockDevice(deviceName, macAddress);
        
        // Act: Handle device with empty scan record
        byte[] emptyScanRecord = new byte[0];
        scanFilter.handleDiscoveredDevice(device, rssi, emptyScanRecord);
        
        // Assert: Device should be emitted
        assertEquals(1, emittedDevices.size(), 
            "Valid device should be emitted even with empty scan record");
        
        // Assert: Emitted device should have debug metadata (empty array is still metadata)
        ScannedDevice emittedDevice = emittedDevices.get(0);
        assertTrue(emittedDevice.hasDebugMetadata(), 
            "Device with empty scan record should have debug metadata");
        
        // Assert: Raw advertisement data should be empty array
        assertArrayEquals(emptyScanRecord, emittedDevice.getRawAdvertisementData(), 
            "Raw advertisement data should be empty array");
    }
    
    /**
     * Property: Debug Metadata Hex String Format
     * 
     * For any device with raw advertisement data, the map representation should 
     * contain a hex string representation of the data.
     * 
     * Feature: sdk-driven-ble-scan-filtering
     * Validates: Requirements 6.4
     */
    @Property(tries = 100)
    public void property_debugMetadataHexStringFormat(
            @ForAll("qringDeviceName") String deviceName,
            @ForAll("validMacAddress") String macAddress,
            @ForAll @IntRange(min = -100, max = 0) int rssi,
            @ForAll("rawAdvertisementData") byte[] scanRecord) {
        
        // Arrange: Enable debug mode
        scanFilter.setDebugEnabled(true);
        
        // Arrange: Create device
        BluetoothDevice device = mockDevice(deviceName, macAddress);
        
        // Act: Handle device with scan record
        scanFilter.handleDiscoveredDevice(device, rssi, scanRecord);
        
        // Assert: Device should be emitted
        assertEquals(1, emittedDevices.size());
        
        // Assert: Map should contain hex string representation
        Map<String, Object> deviceMap = emittedDevices.get(0).toMap();
        assertTrue(deviceMap.containsKey("rawAdvertisementData"));
        
        String hexString = (String) deviceMap.get("rawAdvertisementData");
        assertNotNull(hexString, "Hex string should not be null");
        
        // Assert: Hex string should have correct length (2 chars per byte)
        assertEquals(scanRecord.length * 2, hexString.length(), 
            "Hex string should have 2 characters per byte");
        
        // Assert: Hex string should only contain valid hex characters
        assertTrue(hexString.matches("[0-9A-F]*"), 
            "Hex string should only contain uppercase hex characters");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create a mock BluetoothDevice with specified name and MAC address.
     */
    private BluetoothDevice mockDevice(String name, String macAddress) {
        BluetoothDevice device = mock(BluetoothDevice.class);
        when(device.getName()).thenReturn(name);
        when(device.getAddress()).thenReturn(macAddress);
        return device;
    }
    
    // ========== Arbitraries (Generators) ==========
    
    /**
     * Generate valid MAC addresses in standard format.
     */
    @Provide
    Arbitrary<String> validMacAddress() {
        return Arbitraries.strings()
            .withChars("0123456789ABCDEF")
            .ofLength(2)
            .list().ofSize(6)
            .map(parts -> String.join(":", parts));
    }
    
    /**
     * Generate QRing device names (with valid prefixes).
     */
    @Provide
    Arbitrary<String> qringDeviceName() {
        return Arbitraries.of("O_", "Q_", "R")
            .flatMap(prefix -> Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(15)
                .map(suffix -> prefix + suffix));
    }
    
    /**
     * Generate raw advertisement data (byte arrays).
     * Typical BLE advertisement packets are 31 bytes or less.
     */
    @Provide
    Arbitrary<byte[]> rawAdvertisementData() {
        return Arbitraries.bytes()
            .array(byte[].class)
            .ofMinSize(1)
            .ofMaxSize(31);
    }
}
