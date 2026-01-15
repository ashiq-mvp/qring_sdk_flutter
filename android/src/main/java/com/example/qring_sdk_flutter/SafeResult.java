package com.example.qring_sdk_flutter;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.plugin.common.MethodChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread-safe wrapper around Flutter's MethodChannel.Result that prevents
 * "Reply already submitted" crashes caused by duplicate BLE SDK callbacks.
 * 
 * This class ensures that only the first reply (success, error, or notImplemented)
 * is sent to Flutter, and all subsequent attempts are silently ignored with logging.
 */
public class SafeResult {
    private static final String TAG = "SafeResult";
    
    private final MethodChannel.Result result;
    private final AtomicBoolean replied;
    private final String methodName;
    
    /**
     * Creates a new SafeResult wrapper.
     * 
     * @param result The Flutter method channel result to wrap
     * @param methodName The name of the method being called (for debugging context)
     * @throws IllegalArgumentException if result or methodName is null
     */
    public SafeResult(@NonNull MethodChannel.Result result, @NonNull String methodName) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("Method name cannot be null");
        }
        
        this.result = result;
        this.replied = new AtomicBoolean(false);
        this.methodName = methodName;
    }
    
    /**
     * Sends a successful result to Flutter.
     * Only the first call will be transmitted; subsequent calls are ignored.
     * 
     * @param data The result data (may be null)
     */
    public void success(@Nullable Object data) {
        if (replied.compareAndSet(false, true)) {
            result.success(data);
        } else {
            logDuplicateAttempt("success");
        }
    }
    
    /**
     * Sends an error result to Flutter.
     * Only the first call will be transmitted; subsequent calls are ignored.
     * 
     * @param code The error code
     * @param message The error message
     * @param details Additional error details (may be null)
     */
    public void error(@NonNull String code, @Nullable String message, @Nullable Object details) {
        if (replied.compareAndSet(false, true)) {
            result.error(code, message, details);
        } else {
            logDuplicateAttempt("error");
        }
    }
    
    /**
     * Sends a "not implemented" result to Flutter.
     * Only the first call will be transmitted; subsequent calls are ignored.
     */
    public void notImplemented() {
        if (replied.compareAndSet(false, true)) {
            result.notImplemented();
        } else {
            logDuplicateAttempt("notImplemented");
        }
    }
    
    /**
     * Checks if a reply has already been sent.
     * 
     * @return true if a reply has been sent, false otherwise
     */
    public boolean hasReplied() {
        return replied.get();
    }
    
    /**
     * Logs a duplicate reply attempt with context information.
     * 
     * @param replyType The type of reply that was attempted (success/error/notImplemented)
     */
    private void logDuplicateAttempt(String replyType) {
        Log.w(TAG, String.format(
            "Duplicate reply attempt for method '%s' (type: %s). " +
            "This is likely caused by duplicate BLE SDK callbacks. " +
            "The duplicate has been ignored to prevent crash.",
            methodName,
            replyType
        ));
        
        // Log stack trace for debugging
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Duplicate reply stack trace:", new Exception("Stack trace"));
        }
    }
}
