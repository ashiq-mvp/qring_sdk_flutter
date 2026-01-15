package com.example.qring_sdk_flutter;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Error Handling and Reporting.
 * 
 * Feature: production-ble-manager
 * 
 * These tests validate the correctness properties for error handling defined in the design document.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
public class ErrorHandlingPropertyTest {
    
    private BleConnectionManager manager;
    
    @BeforeTry
    public void setUp() {
        // Get a fresh instance for each test
        manager = BleConnectionManager.getInstance();
        manager.clearObservers();
        manager.reset();
    }
    
    /**
     * Property 41: Error State on Operation Failure
     * 
     * For any BLE operation failure, the BLE_Manager should transition to ERROR state 
     * with error details.
     * 
     * Feature: production-ble-manager, Property 41: Error State on Operation Failure
     * Validates: Requirements 11.1
     */
    @Property(tries = 100)
    public void property41_errorStateOnOperationFailure(
            @ForAll("operationalStates") BleConnectionManager.BleState fromState,
            @ForAll("errorCodes") String errorCode,
            @ForAll("errorMessages") String errorMessage) {
        
        // Arrange: Set manager to an operational state
        manager.reset();
        forceState(manager, fromState);
        
        // Verify initial state
        assertEquals(fromState, manager.getState(), 
            "Manager should be in the specified state");
        assertFalse(manager.isError(), 
            "Manager should not be in error state initially");
        
        // Act: Transition to ERROR state with error details
        boolean transitioned = manager.transitionToError(errorCode, errorMessage);
        
        // Assert: Should successfully transition to ERROR state
        assertTrue(transitioned, 
            "Should be able to transition to ERROR state from " + fromState);
        assertEquals(BleConnectionManager.BleState.ERROR, manager.getState(),
            "State should be ERROR after operation failure");
        assertTrue(manager.isError(),
            "isError() should return true when in ERROR state");
        
        // Verify error details are stored
        assertEquals(errorCode, manager.getErrorCode(),
            "Error code should be stored correctly");
        assertEquals(errorMessage, manager.getErrorMessage(),
            "Error message should be stored correctly");
    }
    
    /**
     * Property 42: Specific Permission Error Reporting
     * 
     * For any permission denial, the BLE_Manager should report a specific permission error 
     * to Flutter_Bridge identifying which permission was denied.
     * 
     * Feature: production-ble-manager, Property 42: Specific Permission Error Reporting
     * Validates: Requirements 11.2
     */
    @Property(tries = 100)
    public void property42_specificPermissionErrorReporting(
            @ForAll("permissionErrorCodes") String permissionErrorCode) {
        
        // Arrange: Set manager to IDLE state
        manager.reset();
        assertEquals(BleConnectionManager.BleState.IDLE, manager.getState());
        
        // Act: Report a permission error
        String errorMessage = ErrorCodes.getMessage(permissionErrorCode);
        boolean transitioned = manager.transitionToError(permissionErrorCode, errorMessage);
        
        // Assert: Should transition to ERROR state with specific permission error
        assertTrue(transitioned, 
            "Should be able to transition to ERROR state for permission error");
        assertEquals(BleConnectionManager.BleState.ERROR, manager.getState(),
            "State should be ERROR after permission denial");
        
        // Verify the error code is a permission-related error
        String storedErrorCode = manager.getErrorCode();
        assertEquals(permissionErrorCode, storedErrorCode,
            "Error code should match the permission error code");
        
        // Verify the error code is one of the permission error codes
        boolean isPermissionError = 
            permissionErrorCode.equals(ErrorCodes.PERMISSION_DENIED) ||
            permissionErrorCode.equals(ErrorCodes.BLUETOOTH_PERMISSION_REQUIRED) ||
            permissionErrorCode.equals(ErrorCodes.LOCATION_PERMISSION_REQUIRED) ||
            permissionErrorCode.equals(ErrorCodes.BLUETOOTH_SCAN_PERMISSION_REQUIRED) ||
            permissionErrorCode.equals(ErrorCodes.BLUETOOTH_CONNECT_PERMISSION_REQUIRED);
        
        assertTrue(isPermissionError,
            "Error code should be a specific permission error code");
        
        // Verify error message is not null or empty
        String storedErrorMessage = manager.getErrorMessage();
        assertNotNull(storedErrorMessage, "Error message should not be null");
        assertFalse(storedErrorMessage.isEmpty(), "Error message should not be empty");
    }
    
    /**
     * Property 43: Pairing Error with Reason
     * 
     * For any pairing failure, the BLE_Manager should report a pairing error with failure reason.
     * 
     * Feature: production-ble-manager, Property 43: Pairing Error with Reason
     * Validates: Requirements 11.3
     */
    @Property(tries = 100)
    public void property43_pairingErrorWithReason(
            @ForAll("pairingFailureReasons") String failureReason) {
        
        // Arrange: Set manager to PAIRING state
        manager.reset();
        forceState(manager, BleConnectionManager.BleState.PAIRING);
        
        // Verify initial state
        assertEquals(BleConnectionManager.BleState.PAIRING, manager.getState(),
            "Manager should be in PAIRING state");
        
        // Act: Report a pairing error with reason
        String errorMessage = "Pairing failed: " + failureReason;
        boolean transitioned = manager.transitionToError(ErrorCodes.PAIRING_FAILED, errorMessage);
        
        // Assert: Should transition to ERROR state with pairing error
        assertTrue(transitioned, 
            "Should be able to transition to ERROR state for pairing failure");
        assertEquals(BleConnectionManager.BleState.ERROR, manager.getState(),
            "State should be ERROR after pairing failure");
        
        // Verify error code is PAIRING_FAILED
        assertEquals(ErrorCodes.PAIRING_FAILED, manager.getErrorCode(),
            "Error code should be PAIRING_FAILED");
        
        // Verify error message contains the failure reason
        String storedErrorMessage = manager.getErrorMessage();
        assertNotNull(storedErrorMessage, "Error message should not be null");
        assertTrue(storedErrorMessage.contains(failureReason),
            "Error message should contain the failure reason: " + failureReason);
        assertTrue(storedErrorMessage.contains("Pairing failed"),
            "Error message should indicate pairing failure");
    }
    
    /**
     * Property 44: GATT Error with Operation Details
     * 
     * For any GATT operation failure, the BLE_Manager should report a GATT error with 
     * operation details.
     * 
     * Feature: production-ble-manager, Property 44: GATT Error with Operation Details
     * Validates: Requirements 11.4
     */
    @Property(tries = 100)
    public void property44_gattErrorWithOperationDetails(
            @ForAll("gattOperations") String operation,
            @ForAll("gattStatusCodes") int statusCode) {
        
        // Arrange: Set manager to CONNECTING state (where GATT operations occur)
        manager.reset();
        forceState(manager, BleConnectionManager.BleState.CONNECTING);
        
        // Verify initial state
        assertEquals(BleConnectionManager.BleState.CONNECTING, manager.getState(),
            "Manager should be in CONNECTING state");
        
        // Act: Report a GATT error with operation details
        String errorMessage = String.format("GATT %s failed: error (status: %d)", 
            operation, statusCode);
        boolean transitioned = manager.transitionToError(ErrorCodes.GATT_ERROR, errorMessage);
        
        // Assert: Should transition to ERROR state with GATT error
        assertTrue(transitioned, 
            "Should be able to transition to ERROR state for GATT failure");
        assertEquals(BleConnectionManager.BleState.ERROR, manager.getState(),
            "State should be ERROR after GATT failure");
        
        // Verify error code is GATT_ERROR
        assertEquals(ErrorCodes.GATT_ERROR, manager.getErrorCode(),
            "Error code should be GATT_ERROR");
        
        // Verify error message contains operation details
        String storedErrorMessage = manager.getErrorMessage();
        assertNotNull(storedErrorMessage, "Error message should not be null");
        assertTrue(storedErrorMessage.contains(operation),
            "Error message should contain the operation name: " + operation);
        assertTrue(storedErrorMessage.contains("status: " + statusCode),
            "Error message should contain the status code: " + statusCode);
        assertTrue(storedErrorMessage.contains("GATT"),
            "Error message should indicate GATT error");
    }
    
    /**
     * Property 45: Retry Allowed After Error Acknowledgment
     * 
     * For any ERROR state, the BLE_Manager should allow retry operations after error 
     * is acknowledged.
     * 
     * Feature: production-ble-manager, Property 45: Retry Allowed After Error Acknowledgment
     * Validates: Requirements 11.5
     */
    @Property(tries = 100)
    public void property45_retryAllowedAfterErrorAcknowledgment(
            @ForAll("errorCodes") String errorCode,
            @ForAll("errorMessages") String errorMessage) {
        
        // Arrange: Set manager to ERROR state
        manager.reset();
        forceState(manager, BleConnectionManager.BleState.CONNECTING);
        boolean errorTransitioned = manager.transitionToError(errorCode, errorMessage);
        
        // Verify we're in ERROR state
        assertTrue(errorTransitioned, "Should transition to ERROR state");
        assertEquals(BleConnectionManager.BleState.ERROR, manager.getState(),
            "Manager should be in ERROR state");
        assertTrue(manager.isError(), "isError() should return true");
        
        // Verify error details are present
        assertEquals(errorCode, manager.getErrorCode());
        assertEquals(errorMessage, manager.getErrorMessage());
        
        // Act: Acknowledge the error
        boolean acknowledged = manager.acknowledgeError();
        
        // Assert: Error should be acknowledged and state should allow retry
        assertTrue(acknowledged, 
            "Error should be acknowledged successfully");
        assertFalse(manager.isError(),
            "Should not be in ERROR state after acknowledgment");
        
        // Verify state transitioned to IDLE (allowing retry)
        assertEquals(BleConnectionManager.BleState.IDLE, manager.getState(),
            "State should be IDLE after error acknowledgment to allow retry");
        
        // Verify error details are cleared
        assertNull(manager.getErrorCode(),
            "Error code should be cleared after acknowledgment");
        assertNull(manager.getErrorMessage(),
            "Error message should be cleared after acknowledgment");
        
        // Verify that operations can now be attempted (state allows transitions)
        boolean canConnect = manager.canConnect();
        assertTrue(canConnect,
            "Should be able to connect after error acknowledgment");
        
        // Verify we can transition to CONNECTING state (retry operation)
        boolean canRetry = manager.canTransition(
            BleConnectionManager.BleState.IDLE, 
            BleConnectionManager.BleState.CONNECTING);
        assertTrue(canRetry,
            "Should be able to transition to CONNECTING state to retry operation");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Force the manager into a specific state for testing purposes.
     * This bypasses normal state validation.
     */
    private void forceState(BleConnectionManager manager, BleConnectionManager.BleState state) {
        // Build a path from IDLE to the target state
        java.util.List<BleConnectionManager.BleState> path = findPathToState(state);
        for (BleConnectionManager.BleState step : path) {
            manager.transitionTo(step);
        }
    }
    
    /**
     * Find a valid path from IDLE to the target state.
     */
    private java.util.List<BleConnectionManager.BleState> findPathToState(BleConnectionManager.BleState target) {
        java.util.List<BleConnectionManager.BleState> path = new java.util.ArrayList<>();
        
        switch (target) {
            case IDLE:
                // Already at IDLE
                break;
            case SCANNING:
                path.add(BleConnectionManager.BleState.SCANNING);
                break;
            case CONNECTING:
                path.add(BleConnectionManager.BleState.CONNECTING);
                break;
            case PAIRING:
                path.add(BleConnectionManager.BleState.CONNECTING);
                path.add(BleConnectionManager.BleState.PAIRING);
                break;
            case CONNECTED:
                path.add(BleConnectionManager.BleState.CONNECTING);
                path.add(BleConnectionManager.BleState.CONNECTED);
                break;
            case DISCONNECTED:
                path.add(BleConnectionManager.BleState.CONNECTING);
                path.add(BleConnectionManager.BleState.DISCONNECTED);
                break;
            case RECONNECTING:
                path.add(BleConnectionManager.BleState.CONNECTING);
                path.add(BleConnectionManager.BleState.CONNECTED);
                path.add(BleConnectionManager.BleState.RECONNECTING);
                break;
            case ERROR:
                path.add(BleConnectionManager.BleState.CONNECTING);
                path.add(BleConnectionManager.BleState.ERROR);
                break;
        }
        
        return path;
    }
    
    // ========== Arbitraries (Generators) ==========
    
    @Provide
    Arbitrary<BleConnectionManager.BleState> operationalStates() {
        // States from which operations can fail and transition to ERROR
        return Arbitraries.of(
            BleConnectionManager.BleState.IDLE,
            BleConnectionManager.BleState.SCANNING,
            BleConnectionManager.BleState.CONNECTING,
            BleConnectionManager.BleState.PAIRING,
            BleConnectionManager.BleState.CONNECTED,
            BleConnectionManager.BleState.RECONNECTING
        );
    }
    
    @Provide
    Arbitrary<String> errorCodes() {
        return Arbitraries.of(
            ErrorCodes.CONNECTION_FAILED,
            ErrorCodes.CONNECTION_TIMEOUT,
            ErrorCodes.PAIRING_FAILED,
            ErrorCodes.GATT_ERROR,
            ErrorCodes.RECONNECTION_FAILED,
            ErrorCodes.RECONNECTION_SETUP_FAILED,
            ErrorCodes.PERMISSION_DENIED,
            ErrorCodes.BLUETOOTH_OFF,
            ErrorCodes.INVALID_STATE,
            ErrorCodes.COMMAND_FAILED,
            ErrorCodes.OPERATION_FAILED
        );
    }
    
    @Provide
    Arbitrary<String> errorMessages() {
        return Arbitraries.of(
            "Connection failed due to timeout",
            "Device not found",
            "Pairing rejected by user",
            "GATT service discovery failed",
            "Bluetooth adapter not available",
            "Permission denied by user",
            "Invalid state for operation",
            "Command execution failed",
            "Unexpected error occurred"
        );
    }
    
    @Provide
    Arbitrary<String> permissionErrorCodes() {
        return Arbitraries.of(
            ErrorCodes.PERMISSION_DENIED,
            ErrorCodes.BLUETOOTH_PERMISSION_REQUIRED,
            ErrorCodes.LOCATION_PERMISSION_REQUIRED,
            ErrorCodes.BLUETOOTH_SCAN_PERMISSION_REQUIRED,
            ErrorCodes.BLUETOOTH_CONNECT_PERMISSION_REQUIRED
        );
    }
    
    @Provide
    Arbitrary<String> pairingFailureReasons() {
        return Arbitraries.of(
            "User rejected pairing request",
            "Pairing timeout",
            "Device bond state changed to BOND_NONE",
            "Bonding failed after retry",
            "Device not responding",
            "Authentication failed",
            "Pairing cancelled"
        );
    }
    
    @Provide
    Arbitrary<String> gattOperations() {
        return Arbitraries.of(
            "connect",
            "discoverServices",
            "requestMtu",
            "readCharacteristic",
            "writeCharacteristic",
            "enableNotification",
            "disconnect"
        );
    }
    
    @Provide
    Arbitrary<Integer> gattStatusCodes() {
        // Common GATT status codes
        return Arbitraries.of(
            0,   // GATT_SUCCESS
            1,   // GATT_INVALID_HANDLE
            2,   // GATT_READ_NOT_PERMIT
            3,   // GATT_WRITE_NOT_PERMIT
            8,   // GATT_INSUFFICIENT_AUTHENTICATION
            13,  // GATT_INVALID_ATTRIBUTE_LENGTH
            15,  // GATT_INSUFFICIENT_ENCRYPTION
            133, // GATT_ERROR (generic)
            257  // GATT_FAILURE
        );
    }
}
