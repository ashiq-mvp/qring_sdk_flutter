package com.example.qring_sdk_flutter;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for parameter validation.
 * Provides common validation methods to ensure data integrity.
 */
public class ValidationUtils {
    
    // Valid heart rate monitoring intervals in minutes
    private static final List<Integer> VALID_HR_INTERVALS = Arrays.asList(10, 15, 20, 30, 60);
    
    /**
     * Validate that a string is not null or empty.
     */
    public static boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Validate that a MAC address has a valid format.
     */
    public static boolean isValidMacAddress(String mac) {
        if (mac == null) return false;
        // MAC address format: XX:XX:XX:XX:XX:XX
        return mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }
    
    /**
     * Validate that a value is within a specified range (inclusive).
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * Validate that a day offset is within the valid range (0-6 days).
     */
    public static boolean isValidDayOffset(int dayOffset) {
        return isInRange(dayOffset, 0, 6);
    }
    
    /**
     * Validate that a battery level is within the valid range (0-100).
     */
    public static boolean isValidBatteryLevel(int level) {
        return isInRange(level, 0, 100);
    }
    
    /**
     * Validate that a brightness value is within the valid range.
     */
    public static boolean isValidBrightness(int brightness, int maxBrightness) {
        return isInRange(brightness, 1, maxBrightness);
    }
    
    /**
     * Validate that a heart rate interval is one of the allowed values.
     */
    public static boolean isValidHeartRateInterval(int intervalMinutes) {
        return VALID_HR_INTERVALS.contains(intervalMinutes);
    }
    
    /**
     * Validate that an age is reasonable.
     */
    public static boolean isValidAge(int age) {
        return isInRange(age, 1, 150);
    }
    
    /**
     * Validate that a height is reasonable (in cm).
     */
    public static boolean isValidHeight(int heightCm) {
        return isInRange(heightCm, 50, 300);
    }
    
    /**
     * Validate that a weight is reasonable (in kg).
     */
    public static boolean isValidWeight(int weightKg) {
        return isInRange(weightKg, 10, 500);
    }
    
    /**
     * Validate that a file path is not null or empty.
     */
    public static boolean isValidFilePath(String filePath) {
        return isValidString(filePath);
    }
    
    /**
     * Validate that an exercise type is within the valid range.
     * QC SDK supports exercise types 0-20+
     */
    public static boolean isValidExerciseType(int exerciseType) {
        return isInRange(exerciseType, 0, 30);
    }
    
    /**
     * Validate that screen-on time is within valid range (0-1439 minutes in a day).
     */
    public static boolean isValidScreenOnTime(int minutes) {
        return isInRange(minutes, 0, 1439);
    }
    
    /**
     * Get a descriptive error message for invalid parameters.
     */
    public static String getValidationErrorMessage(String paramName, String expectedFormat) {
        return String.format("Invalid %s. Expected: %s", paramName, expectedFormat);
    }
}
