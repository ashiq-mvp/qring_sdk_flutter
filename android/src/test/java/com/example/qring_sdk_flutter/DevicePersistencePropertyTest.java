package com.example.qring_sdk_flutter;

import android.content.Context;
import android.content.SharedPreferences;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for DevicePersistenceModel.
 * 
 * Feature: production-ble-manager
 * 
 * These tests validate the correctness properties defined in the design document
 * for device persistence operations.
 * 
 * Tests Properties 19, 46, 47, 48:
 * - Device MAC Persistence on Connection
 * - Device Name Persistence on Connection
 * - Device Info Loading on App Restart
 * - Clear Persisted Info on Manual Disconnect
 */
public class DevicePersistencePropertyTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPrefs;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    @BeforeTry
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup mock behavior
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(true);
    }
    
    /**
     * Property 19: Device MAC Persistence on Connection
     * 
     * For any successful connection, the BLE_Manager should persist the device MAC address 
     * to enable reconnection after app restart.
     * 
     * Feature: production-ble-manager, Property 19: Device MAC Persistence on Connection
     * Validates: Requirements 5.3, 12.1
     */
    @Property(tries = 100)
    public void property19_deviceMacPersistenceOnConnection(
            @ForAll("validMacAddress") String macAddress,
            @ForAll("deviceName") String deviceName) {
        
        // Arrange: Create device persistence model
        DevicePersistenceModel model = new DevicePersistenceModel(macAddress, deviceName);
        
        // Act: Save device info
        boolean success = model.save(mockContext);
        
        // Assert: Save should succeed
        assertTrue(success, "Device info save should succeed");
        
        // Assert: MAC address should be saved
        verify(mockEditor).putString(eq("device_mac"), eq(macAddress));
        verify(mockEditor).commit();
        
        // Assert: Model should be valid
        assertTrue(model.isValid(), "Model should be valid after save");
        assertEquals(macAddress, model.getMacAddress(), "MAC address should match");
    }
    
    /**
     * Property 46: Device Name Persistence on Connection
     * 
     * For any successful connection, the BLE_Manager should persist the device name.
     * 
     * Feature: production-ble-manager, Property 46: Device Name Persistence on Connection
     * Validates: Requirements 12.2
     */
    @Property(tries = 100)
    public void property46_deviceNamePersistenceOnConnection(
            @ForAll("validMacAddress") String macAddress,
            @ForAll("deviceName") String deviceName) {
        
        // Arrange: Create device persistence model
        DevicePersistenceModel model = new DevicePersistenceModel(macAddress, deviceName);
        
        // Act: Save device info
        boolean success = model.save(mockContext);
        
        // Assert: Save should succeed
        assertTrue(success, "Device info save should succeed");
        
        // Assert: Device name should be saved
        verify(mockEditor).putString(eq("device_name"), eq(deviceName));
        verify(mockEditor).commit();
        
        // Assert: Device name should match
        assertEquals(deviceName, model.getDeviceName(), "Device name should match");
    }
    
    /**
     * Property 47: Device Info Loading on App Restart
     * 
     * For any app restart with saved device information, the BLE_Manager should load 
     * the last connected device information.
     * 
     * Feature: production-ble-manager, Property 47: Device Info Loading on App Restart
     * Validates: Requirements 12.3
     */
    @Property(tries = 100)
    public void property47_deviceInfoLoadingOnAppRestart(
            @ForAll("validMacAddress") String macAddress,
            @ForAll("deviceName") String deviceName,
            @ForAll long lastConnectedTime,
            @ForAll boolean autoReconnect) {
        
        // Arrange: Setup mock to return saved data
        when(mockPrefs.getString(eq("device_mac"), isNull())).thenReturn(macAddress);
        when(mockPrefs.getString(eq("device_name"), isNull())).thenReturn(deviceName);
        when(mockPrefs.getLong(eq("last_connected_time"), eq(0L))).thenReturn(lastConnectedTime);
        when(mockPrefs.getBoolean(eq("auto_reconnect"), eq(true))).thenReturn(autoReconnect);
        
        // Act: Load device info
        DevicePersistenceModel loaded = DevicePersistenceModel.load(mockContext);
        
        // Assert: Loaded model should not be null
        assertNotNull(loaded, "Loaded model should not be null");
        
        // Assert: All fields should match
        assertEquals(macAddress, loaded.getMacAddress(), "MAC address should match");
        assertEquals(deviceName, loaded.getDeviceName(), "Device name should match");
        assertEquals(lastConnectedTime, loaded.getLastConnectedTime(), "Last connected time should match");
        assertEquals(autoReconnect, loaded.isAutoReconnect(), "Auto-reconnect flag should match");
        
        // Assert: Model should be valid
        assertTrue(loaded.isValid(), "Loaded model should be valid");
    }
    
    /**
     * Property 47 Extended: No Device Info Returns Null
     * 
     * For any app restart without saved device information, load should return null.
     * 
     * Feature: production-ble-manager, Property 47: Device Info Loading (Extended)
     * Validates: Requirements 12.3
     */
    @Property(tries = 100)
    public void property47_noDeviceInfoReturnsNull() {
        
        // Arrange: Setup mock to return no saved data
        when(mockPrefs.getString(eq("device_mac"), isNull())).thenReturn(null);
        
        // Act: Load device info
        DevicePersistenceModel loaded = DevicePersistenceModel.load(mockContext);
        
        // Assert: Loaded model should be null
        assertNull(loaded, "Loaded model should be null when no data exists");
    }
    
    /**
     * Property 48: Clear Persisted Info on Manual Disconnect
     * 
     * For any manual disconnect, the BLE_Manager should clear the persisted device information.
     * 
     * Feature: production-ble-manager, Property 48: Clear Persisted Info on Manual Disconnect
     * Validates: Requirements 12.4
     */
    @Property(tries = 100)
    public void property48_clearPersistedInfoOnManualDisconnect() {
        
        // Act: Clear device info
        boolean success = DevicePersistenceModel.clear(mockContext);
        
        // Assert: Clear should succeed
        assertTrue(success, "Device info clear should succeed");
        
        // Assert: All keys should be removed
        verify(mockEditor).remove(eq("device_mac"));
        verify(mockEditor).remove(eq("device_name"));
        verify(mockEditor).remove(eq("last_connected_time"));
        verify(mockEditor).remove(eq("auto_reconnect"));
        verify(mockEditor).commit();
    }
    
    /**
     * Property 48 Extended: Clear Then Load Returns Null
     * 
     * For any clear operation followed by load, load should return null.
     * 
     * Feature: production-ble-manager, Property 48: Clear Persisted Info (Extended)
     * Validates: Requirements 12.4
     */
    @Property(tries = 100)
    public void property48_clearThenLoadReturnsNull() {
        
        // Arrange: Clear device info
        DevicePersistenceModel.clear(mockContext);
        
        // Arrange: Setup mock to return no data after clear
        when(mockPrefs.getString(eq("device_mac"), isNull())).thenReturn(null);
        
        // Act: Load device info
        DevicePersistenceModel loaded = DevicePersistenceModel.load(mockContext);
        
        // Assert: Loaded model should be null
        assertNull(loaded, "Loaded model should be null after clear");
    }
    
    /**
     * Property: Save Then Load Round Trip
     * 
     * For any device information, saving then loading should produce equivalent data.
     * 
     * Feature: production-ble-manager, Property: Round Trip Consistency
     * Validates: Requirements 5.3, 12.1, 12.2, 12.3
     */
    @Property(tries = 100)
    public void property_saveThenLoadRoundTrip(
            @ForAll("validMacAddress") String macAddress,
            @ForAll("deviceName") String deviceName,
            @ForAll long lastConnectedTime,
            @ForAll boolean autoReconnect) {
        
        // Arrange: Create and save device persistence model
        DevicePersistenceModel original = new DevicePersistenceModel(
            macAddress, deviceName, lastConnectedTime, autoReconnect
        );
        original.save(mockContext);
        
        // Arrange: Setup mock to return saved data
        when(mockPrefs.getString(eq("device_mac"), isNull())).thenReturn(macAddress);
        when(mockPrefs.getString(eq("device_name"), isNull())).thenReturn(deviceName);
        when(mockPrefs.getLong(eq("last_connected_time"), eq(0L))).thenReturn(lastConnectedTime);
        when(mockPrefs.getBoolean(eq("auto_reconnect"), eq(true))).thenReturn(autoReconnect);
        
        // Act: Load device info
        DevicePersistenceModel loaded = DevicePersistenceModel.load(mockContext);
        
        // Assert: Loaded model should match original
        assertNotNull(loaded, "Loaded model should not be null");
        assertEquals(original.getMacAddress(), loaded.getMacAddress(), "MAC address should match");
        assertEquals(original.getDeviceName(), loaded.getDeviceName(), "Device name should match");
        assertEquals(original.getLastConnectedTime(), loaded.getLastConnectedTime(), 
            "Last connected time should match");
        assertEquals(original.isAutoReconnect(), loaded.isAutoReconnect(), 
            "Auto-reconnect flag should match");
    }
    
    /**
     * Property: Invalid Model Cannot Be Saved
     * 
     * For any device model without MAC address, save should fail.
     * 
     * Feature: production-ble-manager, Property: Validation
     * Validates: Requirements 12.1
     */
    @Property(tries = 100)
    public void property_invalidModelCannotBeSaved(
            @ForAll("deviceName") String deviceName) {
        
        // Arrange: Create model without MAC address
        DevicePersistenceModel model = new DevicePersistenceModel();
        model.setDeviceName(deviceName);
        
        // Assert: Model should not be valid
        assertFalse(model.isValid(), "Model without MAC address should not be valid");
        
        // Act: Try to save
        boolean success = model.save(mockContext);
        
        // Assert: Save should fail
        assertFalse(success, "Save should fail for invalid model");
        
        // Assert: No data should be saved
        verify(mockEditor, never()).commit();
    }
    
    /**
     * Property: Null Context Handling
     * 
     * For any operation with null context, the operation should fail gracefully.
     * 
     * Feature: production-ble-manager, Property: Error Handling
     * Validates: Requirements 5.3, 12.1, 12.2, 12.3, 12.4
     */
    @Property(tries = 100)
    public void property_nullContextHandling(
            @ForAll("validMacAddress") String macAddress,
            @ForAll("deviceName") String deviceName) {
        
        // Arrange: Create model
        DevicePersistenceModel model = new DevicePersistenceModel(macAddress, deviceName);
        
        // Act & Assert: Save with null context should fail
        boolean saveSuccess = model.save(null);
        assertFalse(saveSuccess, "Save with null context should fail");
        
        // Act & Assert: Load with null context should return null
        DevicePersistenceModel loaded = DevicePersistenceModel.load(null);
        assertNull(loaded, "Load with null context should return null");
        
        // Act & Assert: Clear with null context should fail
        boolean clearSuccess = DevicePersistenceModel.clear(null);
        assertFalse(clearSuccess, "Clear with null context should fail");
        
        // Act & Assert: Exists with null context should return false
        boolean exists = DevicePersistenceModel.exists(null);
        assertFalse(exists, "Exists with null context should return false");
    }
    
    // ========== Arbitraries (Generators) ==========
    
    /**
     * Generate valid MAC addresses in standard format.
     */
    @Provide
    Arbitrary<String> validMacAddress() {
        return Arbitraries.strings()
            .withCharRange('0', '9')
            .ofLength(2)
            .list().ofSize(6)
            .map(parts -> String.join(":", parts));
    }
    
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
}
