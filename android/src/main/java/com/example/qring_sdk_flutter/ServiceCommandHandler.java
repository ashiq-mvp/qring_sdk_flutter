package com.example.qring_sdk_flutter;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.req.FindDeviceReq;
import com.oudmon.ble.base.communication.rsp.BaseRspCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServiceCommandHandler - Processes commands from notification actions and Flutter.
 * 
 * This class handles:
 * - Find My Ring command execution
 * - Generic command handling for future extensibility
 * - Connection state validation before command execution
 * - Command result callbacks
 * 
 * Requirements: 4.2, 4.6, 6.4, 12.3
 */
public class ServiceCommandHandler {
    private static final String TAG = "ServiceCommandHandler";
    
    // Command timeout in milliseconds
    private static final int COMMAND_TIMEOUT_MS = 10000; // 10 seconds
    
    // Operation batching constants
    private static final int BATCH_DELAY_MS = 500; // 500ms delay for batching
    private static final int MAX_BATCH_SIZE = 5; // Maximum commands in a batch
    
    private final ServiceConnectionManager connectionManager;
    private final Handler batchHandler;
    private final List<PendingCommand> pendingCommands;
    private boolean isBatchScheduled = false;
    private long lastOperationTime = 0;
    private static final long IDLE_THRESHOLD_MS = 30000; // 30 seconds of idle
    
    /**
     * Represents a pending command waiting to be batched.
     */
    private static class PendingCommand {
        String command;
        Map<String, Object> params;
        CommandCallback callback;
        long timestamp;
        
        PendingCommand(String command, Map<String, Object> params, CommandCallback callback) {
            this.command = command;
            this.params = params;
            this.callback = callback;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Callback interface for command execution results.
     */
    public interface CommandCallback {
        /**
         * Called when a command completes successfully.
         * 
         * @param result Map containing command result data
         */
        void onSuccess(Map<String, Object> result);
        
        /**
         * Called when a command fails.
         * 
         * @param errorCode Error code identifying the failure type
         * @param errorMessage Human-readable error message
         */
        void onError(String errorCode, String errorMessage);
    }
    
    /**
     * Constructor.
     * 
     * @param connectionManager Connection manager to check device connection state
     */
    public ServiceCommandHandler(ServiceConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.batchHandler = new Handler(Looper.getMainLooper());
        this.pendingCommands = new ArrayList<>();
    }
    
    /**
     * Handles the Find My Ring command.
     * Validates that the device is connected before execution.
     * Executes FindDeviceReq through QRing SDK.
     * 
     * Requirements: 4.2, 4.6, 10.1, 12.3
     * 
     * @param callback Callback to handle success or error
     */
    public void handleFindMyRing(final CommandCallback callback) {
        Log.d(TAG, "Handling Find My Ring command");
        
        // Update last operation time
        lastOperationTime = System.currentTimeMillis();
        
        // Validate device is connected before execution
        // Requirement 4.6: Validate connection state before execution
        if (!connectionManager.isConnected()) {
            Log.w(TAG, "Cannot execute Find My Ring: device not connected");
            if (callback != null) {
                callback.onError("NOT_CONNECTED", "Ring not connected");
            }
            return;
        }
        
        // Execute immediately (Find My Ring is time-sensitive, don't batch)
        executeImmediateFindMyRing(callback);
    }
    
    /**
     * Execute Find My Ring command immediately without batching.
     * This is a time-sensitive operation that should not be delayed.
     * 
     * @param callback Callback to handle success or error
     */
    private void executeImmediateFindMyRing(final CommandCallback callback) {
        try {
            // Execute FindDeviceReq through QRing SDK
            // Requirement 4.2: Execute Find_My_Ring command through QRing_SDK
            Log.d(TAG, "Sending FindDeviceReq to QRing SDK");
            
            CommandHandle.getInstance().executeReqCmd(
                new FindDeviceReq(),
                new ICommandResponse<BaseRspCmd>() {
                    public void onDataResponse(BaseRspCmd rsp) {
                        try {
                            Log.d(TAG, "Find My Ring command succeeded");
                            
                            // Return success through callback
                            if (callback != null) {
                                Map<String, Object> result = new HashMap<>();
                                result.put("command", "findMyRing");
                                result.put("success", true);
                                result.put("timestamp", System.currentTimeMillis());
                                callback.onSuccess(result);
                            }
                        } catch (Exception e) {
                            // Requirement 10.1: Catch exceptions in callback
                            Log.e(TAG, "Exception in onDataResponse callback", e);
                            if (callback != null) {
                                callback.onError("CALLBACK_EXCEPTION", "Exception: " + e.getMessage());
                            }
                        }
                    }
                    
                    public void onTimeout() {
                        try {
                            Log.e(TAG, "Find My Ring command timed out");
                            
                            // Return error through callback
                            if (callback != null) {
                                callback.onError("TIMEOUT", "Command timed out");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception in onTimeout callback", e);
                        }
                    }
                    
                    public void onFailed(int errorCode) {
                        try {
                            Log.e(TAG, "Find My Ring command failed with error code: " + errorCode);
                            
                            // Return error through callback
                            if (callback != null) {
                                callback.onError("COMMAND_FAILED", "Command failed with error code: " + errorCode);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception in onFailed callback", e);
                        }
                    }
                }
            );
            
        } catch (SecurityException e) {
            // Requirement 10.1: Catch SecurityException
            Log.e(TAG, "SecurityException executing Find My Ring command - missing permissions", e);
            
            // Return error through callback
            if (callback != null) {
                callback.onError("PERMISSION_DENIED", "Permission denied: " + e.getMessage());
            }
        } catch (IllegalStateException e) {
            // SDK might not be initialized
            Log.e(TAG, "IllegalStateException executing Find My Ring command - SDK may not be initialized", e);
            
            // Return error through callback
            if (callback != null) {
                callback.onError("SDK_NOT_INITIALIZED", "SDK not initialized: " + e.getMessage());
            }
        } catch (Exception e) {
            // Requirement 10.1: Catch generic exceptions
            Log.e(TAG, "Exception executing Find My Ring command", e);
            
            // Return error through callback
            if (callback != null) {
                callback.onError("EXCEPTION", "Exception: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles generic commands for future extensibility.
     * Validates connection state before execution.
     * 
     * Requirements: 6.4, 12.3
     * 
     * @param command Command identifier (e.g., "findMyRing", "getBattery")
     * @param params Command parameters as a map
     * @param callback Callback to handle success or error
     */
    public void handleCommand(String command, Map<String, Object> params, final CommandCallback callback) {
        Log.d(TAG, "Handling generic command: " + command);
        
        // Validate device is connected before execution
        // Requirement 12.3: Validate connection state before execution
        if (!connectionManager.isConnected()) {
            Log.w(TAG, "Cannot execute command '" + command + "': device not connected");
            if (callback != null) {
                callback.onError("NOT_CONNECTED", "Ring not connected");
            }
            return;
        }
        
        // Route to specific command handlers
        switch (command) {
            case "findMyRing":
                handleFindMyRing(callback);
                break;
                
            case "getBattery":
                handleGetBattery(callback);
                break;
                
            default:
                Log.w(TAG, "Unknown command: " + command);
                if (callback != null) {
                    callback.onError("UNKNOWN_COMMAND", "Unknown command: " + command);
                }
                break;
        }
    }
    
    /**
     * Handles the Get Battery command (example for future extensibility).
     * This is a placeholder for demonstrating the generic command pattern.
     * 
     * @param callback Callback to handle success or error
     */
    private void handleGetBattery(final CommandCallback callback) {
        Log.d(TAG, "Handling Get Battery command");
        
        // This is a placeholder - actual implementation would use QRing SDK
        // to query battery level
        if (callback != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("command", "getBattery");
            result.put("batteryLevel", -1); // Placeholder value
            result.put("timestamp", System.currentTimeMillis());
            callback.onSuccess(result);
        }
    }
    
    /**
     * Add a command to the batch queue.
     * Commands are batched to reduce BLE operation frequency during idle.
     * 
     * Requirements: 9.4
     * 
     * @param command Command identifier
     * @param params Command parameters
     * @param callback Callback for command result
     */
    private void addToBatch(String command, Map<String, Object> params, CommandCallback callback) {
        synchronized (pendingCommands) {
            pendingCommands.add(new PendingCommand(command, params, callback));
            Log.d(TAG, "Command added to batch: " + command + " (batch size: " + pendingCommands.size() + ")");
            
            // If batch is full, execute immediately
            if (pendingCommands.size() >= MAX_BATCH_SIZE) {
                Log.d(TAG, "Batch full, executing immediately");
                executeBatch();
            } else if (!isBatchScheduled) {
                // Schedule batch execution
                scheduleBatchExecution();
            }
        }
    }
    
    /**
     * Schedule batch execution after a delay.
     * This allows multiple commands to accumulate before execution.
     * 
     * Requirements: 9.4
     */
    private void scheduleBatchExecution() {
        isBatchScheduled = true;
        batchHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                executeBatch();
            }
        }, BATCH_DELAY_MS);
        Log.d(TAG, "Batch execution scheduled in " + BATCH_DELAY_MS + "ms");
    }
    
    /**
     * Execute all pending commands in the batch.
     * Commands are executed sequentially to avoid overwhelming the BLE connection.
     * 
     * Requirements: 9.4
     */
    private void executeBatch() {
        synchronized (pendingCommands) {
            if (pendingCommands.isEmpty()) {
                isBatchScheduled = false;
                return;
            }
            
            Log.d(TAG, "Executing batch of " + pendingCommands.size() + " commands");
            
            // Create a copy of pending commands and clear the list
            List<PendingCommand> commandsToExecute = new ArrayList<>(pendingCommands);
            pendingCommands.clear();
            isBatchScheduled = false;
            
            // Execute each command sequentially
            for (PendingCommand cmd : commandsToExecute) {
                executeCommandInternal(cmd.command, cmd.params, cmd.callback);
            }
        }
    }
    
    /**
     * Execute a command internally (used by batch execution).
     * 
     * @param command Command identifier
     * @param params Command parameters
     * @param callback Callback for command result
     */
    private void executeCommandInternal(String command, Map<String, Object> params, CommandCallback callback) {
        // Update last operation time
        lastOperationTime = System.currentTimeMillis();
        
        // Route to specific command handlers
        switch (command) {
            case "getBattery":
                handleGetBattery(callback);
                break;
                
            default:
                Log.w(TAG, "Unknown batched command: " + command);
                if (callback != null) {
                    callback.onError("UNKNOWN_COMMAND", "Unknown command: " + command);
                }
                break;
        }
    }
    
    /**
     * Check if the service is currently idle.
     * Idle is defined as no operations for IDLE_THRESHOLD_MS.
     * 
     * Requirements: 9.4
     * 
     * @return true if idle, false otherwise
     */
    public boolean isIdle() {
        long timeSinceLastOperation = System.currentTimeMillis() - lastOperationTime;
        return timeSinceLastOperation > IDLE_THRESHOLD_MS;
    }
    
    /**
     * Get the time since the last operation in milliseconds.
     * 
     * @return Time since last operation in milliseconds
     */
    public long getTimeSinceLastOperation() {
        return System.currentTimeMillis() - lastOperationTime;
    }
    
    /**
     * Clear all pending batched commands.
     * Called during cleanup or when connection is lost.
     */
    public void clearBatch() {
        synchronized (pendingCommands) {
            if (!pendingCommands.isEmpty()) {
                Log.d(TAG, "Clearing " + pendingCommands.size() + " pending commands");
                
                // Notify all callbacks of cancellation
                for (PendingCommand cmd : pendingCommands) {
                    if (cmd.callback != null) {
                        cmd.callback.onError("CANCELLED", "Command cancelled due to connection loss");
                    }
                }
                
                pendingCommands.clear();
            }
            
            // Cancel scheduled batch execution
            if (isBatchScheduled) {
                batchHandler.removeCallbacksAndMessages(null);
                isBatchScheduled = false;
            }
        }
    }
}
