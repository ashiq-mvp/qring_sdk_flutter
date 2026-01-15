package com.example.qring_sdk_flutter;

import android.util.Log;
import io.flutter.plugin.common.MethodChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized exception handling for the QRing SDK Flutter plugin.
 * Wraps native SDK exceptions and converts them to Flutter-compatible errors.
 * 
 * Requirement 11.6: THE BLE_Manager SHALL log all errors for debugging purposes
 */
public class ExceptionHandler {
    private static final String TAG = "QRingSDK";
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    /**
     * Handle an exception and send an error result to Flutter.
     * Logs the exception and converts it to a PlatformException.
     */
    public static void handleException(Exception e, MethodChannel.Result result, String operation) {
        Log.e(TAG, "Error during " + operation, e);
        
        String errorCode = ErrorCodes.SDK_ERROR;
        String errorMessage = ErrorCodes.getMessage(ErrorCodes.SDK_ERROR);
        
        // Try to extract more specific error information
        if (e.getMessage() != null) {
            errorMessage = ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage());
        }
        
        // Check for specific exception types
        if (e instanceof NullPointerException) {
            errorCode = ErrorCodes.INTERNAL_ERROR;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.INTERNAL_ERROR, "Null pointer exception");
        } else if (e instanceof IllegalArgumentException) {
            errorCode = ErrorCodes.INVALID_ARGUMENT;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.INVALID_ARGUMENT, e.getMessage());
        } else if (e instanceof IllegalStateException) {
            errorCode = ErrorCodes.OPERATION_FAILED;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.OPERATION_FAILED, e.getMessage());
        } else if (e instanceof SecurityException) {
            errorCode = ErrorCodes.PERMISSION_DENIED;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.PERMISSION_DENIED, e.getMessage());
        }
        
        result.error(errorCode, errorMessage, getStackTraceString(e));
    }
    
    /**
     * Handle an exception and send an error result to Flutter using SafeResult.
     * Logs the exception and converts it to a PlatformException.
     */
    public static void handleException(Exception e, SafeResult result, String operation) {
        Log.e(TAG, "Error during " + operation, e);
        
        String errorCode = ErrorCodes.SDK_ERROR;
        String errorMessage = ErrorCodes.getMessage(ErrorCodes.SDK_ERROR);
        
        // Try to extract more specific error information
        if (e.getMessage() != null) {
            errorMessage = ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage());
        }
        
        // Check for specific exception types
        if (e instanceof NullPointerException) {
            errorCode = ErrorCodes.INTERNAL_ERROR;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.INTERNAL_ERROR, "Null pointer exception");
        } else if (e instanceof IllegalArgumentException) {
            errorCode = ErrorCodes.INVALID_ARGUMENT;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.INVALID_ARGUMENT, e.getMessage());
        } else if (e instanceof SecurityException) {
            errorCode = ErrorCodes.PERMISSION_DENIED;
            errorMessage = ErrorCodes.getMessage(ErrorCodes.PERMISSION_DENIED, e.getMessage());
        }
        
        result.error(errorCode, errorMessage, getStackTraceString(e));
    }
    
    /**
     * Handle a timeout exception.
     */
    public static void handleTimeout(MethodChannel.Result result, String operation) {
        Log.w(TAG, "Timeout during " + operation);
        result.error(
            ErrorCodes.OPERATION_TIMEOUT,
            ErrorCodes.getMessage(ErrorCodes.OPERATION_TIMEOUT, operation),
            null
        );
    }
    
    /**
     * Send a connection error to Flutter.
     */
    public static void sendConnectionError(MethodChannel.Result result) {
        result.error(
            ErrorCodes.NOT_CONNECTED,
            ErrorCodes.getMessage(ErrorCodes.NOT_CONNECTED),
            null
        );
    }
    
    /**
     * Send a validation error to Flutter.
     */
    public static void sendValidationError(MethodChannel.Result result, String paramName, String expectedFormat) {
        result.error(
            ErrorCodes.INVALID_ARGUMENT,
            ValidationUtils.getValidationErrorMessage(paramName, expectedFormat),
            null
        );
    }
    
    /**
     * Send a custom error to Flutter.
     */
    public static void sendError(MethodChannel.Result result, String code, String message) {
        result.error(code, message, null);
    }
    
    /**
     * Log an error without sending it to Flutter (for internal errors).
     */
    public static void logError(String operation, String message) {
        Log.e(TAG, operation + ": " + message);
    }
    
    /**
     * Log an error with full details including timestamp, code, message, and stack trace.
     * 
     * Requirement 11.6: THE BLE_Manager SHALL log all errors for debugging purposes
     * 
     * @param code Error code
     * @param message Error message
     * @param operation Operation that failed
     * @param throwable Optional throwable for stack trace
     */
    public static void logErrorWithDetails(String code, String message, String operation, Throwable throwable) {
        String timestamp = DATE_FORMAT.format(new Date());
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n========== ERROR LOG ==========\n");
        logMessage.append("Timestamp: ").append(timestamp).append("\n");
        logMessage.append("Error Code: ").append(code).append("\n");
        logMessage.append("Operation: ").append(operation).append("\n");
        logMessage.append("Message: ").append(message).append("\n");
        
        if (throwable != null) {
            logMessage.append("Exception: ").append(throwable.getClass().getSimpleName()).append("\n");
            logMessage.append("Stack Trace:\n");
            logMessage.append(getStackTraceString(throwable));
        }
        
        logMessage.append("===============================\n");
        
        Log.e(TAG, logMessage.toString());
    }
    
    /**
     * Log an error with full details (without throwable).
     * 
     * @param code Error code
     * @param message Error message
     * @param operation Operation that failed
     */
    public static void logErrorWithDetails(String code, String message, String operation) {
        logErrorWithDetails(code, message, operation, null);
    }
    
    /**
     * Log a warning.
     */
    public static void logWarning(String operation, String message) {
        Log.w(TAG, operation + ": " + message);
    }
    
    /**
     * Convert exception stack trace to string for debugging.
     */
    private static String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        // Include cause if present
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            sb.append("Caused by: ");
            sb.append(getStackTraceString(cause));
        }
        
        return sb.toString();
    }
    
    /**
     * Wrap a runnable with exception handling.
     */
    public static void safeExecute(Runnable runnable, MethodChannel.Result result, String operation) {
        try {
            runnable.run();
        } catch (Exception e) {
            handleException(e, result, operation);
        }
    }
}
