package com.example.qring_sdk_flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

import com.oudmon.ble.base.communication.DfuHandle;
// Note: Use DfuHandle.IOpResult as the callback interface

/**
 * Manages firmware update operations for the QC Ring device.
 * Wraps the DfuHandle from the QC SDK to provide firmware validation and update functionality.
 */
public class FirmwareManager {
    private static final String TAG = "FirmwareManager";
    private final Context context;
    private final Handler mainHandler;
    private EventChannel.EventSink progressSink;
    private boolean isUpdating = false;

    public FirmwareManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Set the event sink for streaming firmware update progress.
     */
    public void setProgressSink(EventChannel.EventSink sink) {
        this.progressSink = sink;
    }

    /**
     * Validate a firmware file before starting the update process.
     * 
     * @param filePath Path to the firmware file
     * @param callback Callback for validation result
     */
    public void validateFirmwareFile(String filePath, ValidationCallback callback) {
        try {
            File file = new File(filePath);
            
            // Check if file exists
            if (!file.exists()) {
                callback.onError("FILE_NOT_FOUND", "Firmware file does not exist");
                return;
            }

            // Check if file is readable
            if (!file.canRead()) {
                callback.onError("FILE_NOT_READABLE", "Cannot read firmware file");
                return;
            }

            // Check file size (should be reasonable, not empty and not too large)
            long fileSize = file.length();
            if (fileSize == 0) {
                callback.onError("INVALID_FILE", "Firmware file is empty");
                return;
            }
            if (fileSize > 10 * 1024 * 1024) { // 10MB max
                callback.onError("INVALID_FILE", "Firmware file is too large");
                return;
            }

            // Use DfuHandle to check the file
            boolean isValid = DfuHandle.getInstance().checkFile(filePath);
            
            if (isValid) {
                callback.onSuccess(true);
            } else {
                callback.onError("INVALID_FILE", "Firmware file validation failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error validating firmware file", e);
            callback.onError("VALIDATION_ERROR", "Failed to validate firmware file: " + e.getMessage());
        }
    }

    /**
     * Start the firmware update process.
     * 
     * @param filePath Path to the validated firmware file
     * @param callback Callback for update initiation result
     */
    public void startFirmwareUpdate(String filePath, FirmwareUpdateCallback callback) {
        if (isUpdating) {
            callback.onError("UPDATE_IN_PROGRESS", "Firmware update is already in progress");
            return;
        }

        try {
            File file = new File(filePath);
            
            if (!file.exists()) {
                callback.onError("FILE_NOT_FOUND", "Firmware file does not exist");
                return;
            }

            isUpdating = true;
            
            // Start the DFU update process
            DfuHandle.getInstance().start(new DfuHandle.IOpResult() {
                @Override
                public void onProgress(int progress) {
                    // Ensure progress is in valid range [0, 100]
                    int validProgress = Math.max(0, Math.min(100, progress));
                    
                    Log.d(TAG, "Firmware update progress: " + validProgress + "%");
                    
                    // Send progress update to Flutter
                    if (progressSink != null) {
                        mainHandler.post(() -> {
                            Map<String, Object> progressData = new HashMap<>();
                            progressData.put("progress", validProgress);
                            progressData.put("status", "updating");
                            progressSink.success(progressData);
                        });
                    }
                }

                @Override
                public void onActionResult(int errorCode, int result) {
                    if (errorCode == DfuHandle.RSP_OK) {
                         Log.d(TAG, "Firmware update completed successfully");
                        isUpdating = false;
                        
                        // Send completion event to Flutter
                        if (progressSink != null) {
                            mainHandler.post(() -> {
                                Map<String, Object> completionData = new HashMap<>();
                                completionData.put("progress", 100);
                                completionData.put("status", "completed");
                                progressSink.success(completionData);
                            });
                        }
                        
                        callback.onSuccess();
                    } else {
                        Log.e(TAG, "Firmware update failed: " + errorCode);
                        isUpdating = false;
                        
                        // Send error event to Flutter
                        if (progressSink != null) {
                            mainHandler.post(() -> {
                                Map<String, Object> errorData = new HashMap<>();
                                errorData.put("status", "failed");
                                errorData.put("errorCode", errorCode);
                                errorData.put("errorMessage", "Firmware update failed with code " + errorCode);
                                progressSink.success(errorData);
                            });
                        }
                        
                        callback.onError("UPDATE_FAILED", "Firmware update failed: " + errorCode);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting firmware update", e);
            isUpdating = false;
            callback.onError("UPDATE_ERROR", "Failed to start firmware update: " + e.getMessage());
        }
    }

    /**
     * Check if a firmware update is currently in progress.
     */
    public boolean isUpdating() {
        return isUpdating;
    }

    /**
     * Clean up resources.
     */
    public void dispose() {
        progressSink = null;
        isUpdating = false;
    }

    /**
     * Callback interface for firmware file validation.
     */
    public interface ValidationCallback {
        void onSuccess(boolean isValid);
        void onError(String code, String message);
    }

    /**
     * Callback interface for firmware update operations.
     */
    public interface FirmwareUpdateCallback {
        void onSuccess();
        void onError(String code, String message);
    }
}
