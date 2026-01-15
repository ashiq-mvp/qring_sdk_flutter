package com.example.qring_sdk_flutter;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Utility class for converting QC SDK data types to Flutter-compatible maps.
 */
public class DataConverter {
    private static final String TAG = "DataConverter";

    /**
     * Convert ReadDetailSportDataRsp to StepData map.
     */
    public static Map<String, Object> convertStepData(
        int year, int month, int day,
        int totalSteps, int runningSteps, int calorie,
        int walkDistance, int sportDuration, int sleepDuration
    ) {
        Map<String, Object> map = new HashMap<>();
        
        // Format date as ISO 8601 string
        String dateStr = String.format("%04d-%02d-%02d", year, month, day);
        map.put("date", dateStr);
        
        map.put("totalSteps", totalSteps);
        map.put("runningSteps", runningSteps);
        map.put("calories", calorie);
        map.put("distanceMeters", walkDistance);
        map.put("sportDurationSeconds", sportDuration);
        map.put("sleepDurationSeconds", sleepDuration);
        
        Log.d(TAG, "Converted step data for " + dateStr + ": " + totalSteps + " steps");
        return map;
    }

    /**
     * Convert heart rate array to list of HeartRateData maps.
     * 
     * @param hrArray Array of heart rate values (one per 5-minute interval)
     * @param baseTime Unix timestamp for the start of the day
     * @return List of heart rate data maps
     */
    public static List<Map<String, Object>> convertHeartRateData(byte[] hrArray, int baseTime) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        if (hrArray == null || hrArray.length == 0) {
            Log.d(TAG, "No heart rate data to convert");
            return list;
        }
        
        // Each entry represents a 5-minute interval
        final int INTERVAL_SECONDS = 5 * 60;
        
        for (int i = 0; i < hrArray.length; i++) {
            int hrValue = hrArray[i] & 0xFF; // Convert to unsigned byte
            
            // Skip invalid values (0 typically means no measurement)
            if (hrValue > 0) {
                Map<String, Object> hr = new HashMap<>();
                long timestamp = (long) baseTime + (i * INTERVAL_SECONDS);
                hr.put("timestamp", timestamp * 1000); // Convert to milliseconds
                hr.put("heartRate", hrValue);
                list.add(hr);
            }
        }
        
        Log.d(TAG, "Converted " + list.size() + " heart rate measurements");
        return list;
    }

    /**
     * Convert sleep data to SleepData map.
     * 
     * @param sleepDetails List of sleep stage durations (in minutes)
     * @param sleepStages List of sleep stage types
     * @param startTime Sleep start time (Unix timestamp)
     * @param endTime Sleep end time (Unix timestamp)
     * @param hasLunchBreak Whether there's a lunch break
     * @param lunchStartTime Lunch break start time (Unix timestamp, can be 0)
     * @param lunchEndTime Lunch break end time (Unix timestamp, can be 0)
     * @return SleepData map
     */
    public static Map<String, Object> convertSleepData(
        List<Integer> sleepDetails,
        List<Integer> sleepStages,
        long startTime,
        long endTime,
        boolean hasLunchBreak,
        long lunchStartTime,
        long lunchEndTime
    ) {
        Map<String, Object> map = new HashMap<>();
        
        map.put("startTime", startTime * 1000); // Convert to milliseconds
        map.put("endTime", endTime * 1000);
        
        // Convert sleep details
        List<Map<String, Object>> details = new ArrayList<>();
        if (sleepDetails != null && sleepStages != null) {
            int minSize = Math.min(sleepDetails.size(), sleepStages.size());
            for (int i = 0; i < minSize; i++) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("durationMinutes", sleepDetails.get(i));
                detail.put("stage", convertSleepStage(sleepStages.get(i)));
                details.add(detail);
            }
        }
        map.put("details", details);
        
        map.put("hasLunchBreak", hasLunchBreak);
        if (hasLunchBreak && lunchStartTime > 0 && lunchEndTime > 0) {
            map.put("lunchStartTime", lunchStartTime * 1000);
            map.put("lunchEndTime", lunchEndTime * 1000);
        }
        
        Log.d(TAG, "Converted sleep data with " + details.size() + " stages");
        return map;
    }

    /**
     * Convert sleep stage code to string.
     */
    private static String convertSleepStage(int stage) {
        switch (stage) {
            case 0: return "notSleeping";
            case 1: return "removed";
            case 2: return "lightSleep";
            case 3: return "deepSleep";
            case 4: return "rem";
            case 5: return "awake";
            default: return "notSleeping";
        }
    }

    /**
     * Convert blood oxygen array to list of BloodOxygenData maps.
     * 
     * @param spo2Array Array of SpO2 values
     * @param baseTime Unix timestamp for the start of the day
     * @return List of blood oxygen data maps
     */
    public static List<Map<String, Object>> convertBloodOxygenData(byte[] spo2Array, int baseTime) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        if (spo2Array == null || spo2Array.length == 0) {
            Log.d(TAG, "No blood oxygen data to convert");
            return list;
        }
        
        // Each entry represents a measurement interval
        final int INTERVAL_SECONDS = 5 * 60; // 5 minutes
        
        for (int i = 0; i < spo2Array.length; i++) {
            int spo2Value = spo2Array[i] & 0xFF; // Convert to unsigned byte
            
            // Skip invalid values (0 typically means no measurement)
            if (spo2Value > 0) {
                Map<String, Object> spo2 = new HashMap<>();
                long timestamp = (long) baseTime + (i * INTERVAL_SECONDS);
                spo2.put("timestamp", timestamp * 1000); // Convert to milliseconds
                spo2.put("spO2", spo2Value);
                list.add(spo2);
            }
        }
        
        Log.d(TAG, "Converted " + list.size() + " blood oxygen measurements");
        return list;
    }

    /**
     * Convert blood pressure data to list of BloodPressureData maps.
     * 
     * @param systolicArray Array of systolic values
     * @param diastolicArray Array of diastolic values
     * @param baseTime Unix timestamp for the start of the day
     * @return List of blood pressure data maps
     */
    public static List<Map<String, Object>> convertBloodPressureData(
        byte[] systolicArray,
        byte[] diastolicArray,
        int baseTime
    ) {
        List<Map<String, Object>> list = new ArrayList<>();
        
        if (systolicArray == null || diastolicArray == null) {
            Log.d(TAG, "No blood pressure data to convert");
            return list;
        }
        
        int minLength = Math.min(systolicArray.length, diastolicArray.length);
        final int INTERVAL_SECONDS = 5 * 60; // 5 minutes
        
        for (int i = 0; i < minLength; i++) {
            int systolic = systolicArray[i] & 0xFF;
            int diastolic = diastolicArray[i] & 0xFF;
            
            // Skip invalid values
            if (systolic > 0 && diastolic > 0) {
                Map<String, Object> bp = new HashMap<>();
                long timestamp = (long) baseTime + (i * INTERVAL_SECONDS);
                bp.put("timestamp", timestamp * 1000); // Convert to milliseconds
                bp.put("systolic", systolic);
                bp.put("diastolic", diastolic);
                list.add(bp);
            }
        }
        
        Log.d(TAG, "Converted " + list.size() + " blood pressure measurements");
        return list;
    }

    /**
     * Calculate Unix timestamp for a given day offset.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @return Unix timestamp at midnight of the specified day
     */
    public static int calculateDayStartTime(int dayOffset) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -dayOffset);
        
        return (int) (calendar.getTimeInMillis() / 1000);
    }
}
