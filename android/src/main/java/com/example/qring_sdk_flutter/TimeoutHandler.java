package com.example.qring_sdk_flutter;

import android.os.Handler;
import android.os.Looper;
import io.flutter.plugin.common.MethodChannel;

/**
 * Handles timeout operations for BLE operations.
 * Ensures operations don't hang indefinitely.
 */
public class TimeoutHandler {
    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final int CONNECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final int DATA_SYNC_TIMEOUT_MS = 60000; // 60 seconds
    private static final int MEASUREMENT_TIMEOUT_MS = 120000; // 120 seconds
    private static final int COMMAND_TIMEOUT_MS = 10000; // 10 seconds
    
    private final Handler handler;
    private Runnable timeoutRunnable;
    
    public TimeoutHandler() {
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Start a timeout for a connection operation.
     */
    public void startConnectionTimeout(MethodChannel.Result result, Runnable onTimeout) {
        startTimeout(CONNECTION_TIMEOUT_MS, result, "connection", onTimeout);
    }
    
    /**
     * Start a timeout for a data sync operation.
     */
    public void startDataSyncTimeout(MethodChannel.Result result, Runnable onTimeout) {
        startTimeout(DATA_SYNC_TIMEOUT_MS, result, "data synchronization", onTimeout);
    }
    
    /**
     * Start a timeout for a measurement operation.
     */
    public void startMeasurementTimeout(MethodChannel.Result result, Runnable onTimeout) {
        startTimeout(MEASUREMENT_TIMEOUT_MS, result, "measurement", onTimeout);
    }
    
    /**
     * Start a timeout for a command operation.
     */
    public void startCommandTimeout(MethodChannel.Result result, Runnable onTimeout) {
        startTimeout(COMMAND_TIMEOUT_MS, result, "command", onTimeout);
    }
    
    /**
     * Start a custom timeout.
     */
    public void startTimeout(int timeoutMs, MethodChannel.Result result, String operation, Runnable onTimeout) {
        cancel(); // Cancel any existing timeout
        
        timeoutRunnable = () -> {
            ExceptionHandler.handleTimeout(result, operation);
            if (onTimeout != null) {
                onTimeout.run();
            }
        };
        
        handler.postDelayed(timeoutRunnable, timeoutMs);
    }
    
    /**
     * Cancel the current timeout.
     */
    public void cancel() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }
    
    /**
     * Check if a timeout is currently active.
     */
    public boolean isActive() {
        return timeoutRunnable != null;
    }
}
