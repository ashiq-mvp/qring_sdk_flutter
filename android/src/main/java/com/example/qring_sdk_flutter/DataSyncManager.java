package com.example.qring_sdk_flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.ILargeDataSleepResponse;
import com.oudmon.ble.base.communication.ILargeDataLaunchSleepResponse;
import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.req.ReadDetailSportDataReq;
import com.oudmon.ble.base.communication.req.ReadHeartRateReq;
import com.oudmon.ble.base.communication.rsp.ReadDetailSportDataRsp;
import com.oudmon.ble.base.communication.rsp.ReadHeartRateRsp;
import com.oudmon.ble.base.communication.rsp.SleepNewProtoResp;
import com.oudmon.ble.base.communication.LargeDataHandler;
import com.oudmon.ble.base.communication.bigData.IBloodOxygenCallback;
import com.oudmon.ble.base.communication.bigData.BloodOxygenEntity;
import com.oudmon.ble.base.bean.SleepDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages health data synchronization operations.
 * Wraps CommandHandle and LargeDataHandler from QC SDK.
 */
public class DataSyncManager {
    private static final String TAG = "DataSyncManager";
    private static final int MAX_DAY_OFFSET = 6; // Support up to 7 days of historical data
    
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DataSyncManager(Context context) {
        this.context = context;
    }

    /**
     * Synchronize step data for a specific day.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @param callback Callback to handle step data or error
     */
    public void syncStepData(int dayOffset, final DataSyncCallback callback) {
        if (dayOffset < 0 || dayOffset > MAX_DAY_OFFSET) {
            mainHandler.post(() -> callback.onError(
                "INVALID_PARAMETER",
                "Day offset must be between 0 and " + MAX_DAY_OFFSET
            ));
            return;
        }

        Log.d(TAG, "Syncing step data for day offset: " + dayOffset);
        
        // ReadDetailSportDataReq parameters:
        // - dayOffset: number of days before today
        // - startIndex: starting index for data (0 for beginning)
        // - count: number of records to read (95 = full day in 15-min intervals)
        CommandHandle.getInstance().executeReqCmd(
            new ReadDetailSportDataReq(dayOffset, 0, 95),
            new ICommandResponse<ReadDetailSportDataRsp>() {
                @Override
                public void onDataResponse(ReadDetailSportDataRsp rsp) {
                    List<com.oudmon.ble.base.communication.entity.BleStepDetails> details = rsp.getBleStepDetailses();
                    if (details != null && !details.isEmpty()) {
                        com.oudmon.ble.base.communication.entity.BleStepDetails first = details.get(0);
                        Log.d(TAG, "Step data received for " + first.getYear() + "-" + first.getMonth() + "-" + first.getDay());
                        
                        // Aggregate data
                        int totalSteps = 0;
                        int runningSteps = 0;
                        int calorie = 0;
                        int walkDistance = 0;
                        int sportDuration = 0; // Not direct field in details, assume 0 or calc?
                        // BleStepDetails has: walkSteps, runSteps, calorie, distance.
                        
                        for (com.oudmon.ble.base.communication.entity.BleStepDetails item : details) {
                             totalSteps += item.getWalkSteps() + item.getRunSteps(); // Assuming total = walk + run
                             runningSteps += item.getRunSteps();
                             calorie += item.getCalorie();
                             walkDistance += item.getDistance();
                             // sportDuration? details has timeIndex?
                        }

                        Map<String, Object> stepData = DataConverter.convertStepData(
                            first.getYear(),
                            first.getMonth(),
                            first.getDay(),
                            totalSteps,
                            runningSteps,
                            calorie,
                            walkDistance,
                            0, // sportDuration placeholder
                            0  // sleepDuration placeholder
                        );
                        
                        mainHandler.post(() -> callback.onSuccess(stepData));
                    } else {
                         Log.w(TAG, "No step data details received");
                         mainHandler.post(() -> callback.onSuccess(new HashMap<>()));
                    }
                }

                public void onTimeout() {
                    Log.e(TAG, "Step data sync timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Step data sync timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Step data sync failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "SYNC_FAILED",
                        "Step data sync failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Synchronize heart rate data for a specific day.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @param callback Callback to handle heart rate data or error
     */
    public void syncHeartRateData(int dayOffset, final DataSyncCallback callback) {
        if (dayOffset < 0 || dayOffset > MAX_DAY_OFFSET) {
            mainHandler.post(() -> callback.onError(
                "INVALID_PARAMETER",
                "Day offset must be between 0 and " + MAX_DAY_OFFSET
            ));
            return;
        }

        Log.d(TAG, "Syncing heart rate data for day offset: " + dayOffset);
        
        // Calculate Unix timestamp for the start of the specified day
        int nowTime = DataConverter.calculateDayStartTime(dayOffset);
        
        CommandHandle.getInstance().executeReqCmd(
            new ReadHeartRateReq(nowTime),
            new ICommandResponse<ReadHeartRateRsp>() {
                public void onDataResponse(ReadHeartRateRsp rsp) {
                    byte[] hrArray = rsp.getmHeartRateArray();
                    Log.d(TAG, "Heart rate data received, array length: " + 
                        (hrArray != null ? hrArray.length : 0));
                    
                    List<Map<String, Object>> hrData = DataConverter.convertHeartRateData(
                        hrArray,
                        rsp.getmUtcTime()
                    );
                    
                    mainHandler.post(() -> callback.onSuccess(hrData));
                }

                public void onTimeout() {
                    Log.e(TAG, "Heart rate data sync timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Heart rate data sync timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Heart rate data sync failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "SYNC_FAILED",
                        "Heart rate data sync failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Synchronize sleep data for a specific day using LargeDataHandler.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @param callback Callback to handle sleep data or error
     */
    public void syncSleepData(int dayOffset, final DataSyncCallback callback) {
        if (dayOffset < 0 || dayOffset > MAX_DAY_OFFSET) {
            mainHandler.post(() -> callback.onError(
                "INVALID_PARAMETER",
                "Day offset must be between 0 and " + MAX_DAY_OFFSET
            ));
            return;
        }

        Log.d(TAG, "Syncing sleep data for day offset: " + dayOffset);
        
        LargeDataHandler.getInstance().syncSleepList(
            dayOffset,
            new ILargeDataSleepResponse() {
                public void sleepData(SleepDisplay sleepDisplay) {
                    if (sleepDisplay != null) {
                        Log.d(TAG, "Sleep data received: " + sleepDisplay.toString());
                        Map<String, Object> sleepData = new HashMap<>();
                        sleepData.put("startTime", sleepDisplay.getSleepTime());
                        sleepData.put("endTime", sleepDisplay.getWakeTime());
                        sleepData.put("totalDuration", sleepDisplay.getTotalSleepDuration());
                        sleepData.put("deepSleep", sleepDisplay.getDeepSleepDuration());
                        sleepData.put("lightSleep", sleepDisplay.getShallowSleepDuration());
                        sleepData.put("remSleep", sleepDisplay.getRapidDuration());
                        sleepData.put("awake", sleepDisplay.getAwakeDuration());
                        mainHandler.post(() -> callback.onSuccess(sleepData));
                    } else {
                        Log.w(TAG, "Empty sleep data received");
                        mainHandler.post(() -> callback.onSuccess(createEmptySleepData(0)));
                    }
                }
            },
            new ILargeDataLaunchSleepResponse() {
                public void sleepData(SleepNewProtoResp sleepNewProtoResp) {
                    // Launch sleep data callback (optional)
                    Log.d(TAG, "Launch sleep data received");
                }
            }
        );
    }

    /**
     * Synchronize blood oxygen data for a specific day using LargeDataHandler.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @param callback Callback to handle blood oxygen data or error
     */
    public void syncBloodOxygenData(int dayOffset, final DataSyncCallback callback) {
        if (dayOffset < 0 || dayOffset > MAX_DAY_OFFSET) {
            mainHandler.post(() -> callback.onError(
                "INVALID_PARAMETER",
                "Day offset must be between 0 and " + MAX_DAY_OFFSET
            ));
            return;
        }

        Log.d(TAG, "Syncing blood oxygen data for day offset: " + dayOffset);
        
        LargeDataHandler.getInstance().syncBloodOxygenWithCallback(new IBloodOxygenCallback() {
            public void readBloodOxygen(List<BloodOxygenEntity> entities) {
                if (entities != null && !entities.isEmpty()) {
                    Log.d(TAG, "Blood oxygen data received, count: " + entities.size());
                    List<Map<String, Object>> spo2Data = new ArrayList<>();
                    for (BloodOxygenEntity entity : entities) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("timestamp", entity.getUnix_time());
                        int val = 0;
                        if (entity.getMaxArray() != null && !entity.getMaxArray().isEmpty()) {
                            val = entity.getMaxArray().get(0);
                        }
                        item.put("value", val);
                        spo2Data.add(item);
                    }
                    mainHandler.post(() -> callback.onSuccess(spo2Data));
                } else {
                    Log.w(TAG, "Empty blood oxygen data received");
                    mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                }
            }
        });
    }

    /**
     * Synchronize blood pressure data for a specific day.
     * Note: Blood pressure data sync may not be available in this SDK version.
     * This is a placeholder implementation.
     * 
     * @param dayOffset Number of days before today (0 = today, 1 = yesterday, etc.)
     * @param callback Callback to handle blood pressure data or error
     */
    public void syncBloodPressureData(int dayOffset, final DataSyncCallback callback) {
        if (dayOffset < 0 || dayOffset > MAX_DAY_OFFSET) {
            mainHandler.post(() -> callback.onError(
                "INVALID_PARAMETER",
                "Day offset must be between 0 and " + MAX_DAY_OFFSET
            ));
            return;
        }

        Log.d(TAG, "Blood pressure data sync not implemented in SDK");
        
        // Blood pressure historical data sync is not available in the SDK
        // Only real-time measurements are supported
        mainHandler.post(() -> callback.onError(
            "NOT_SUPPORTED",
            "Blood pressure historical data sync is not supported by the SDK"
        ));
    }

    /**
     * Create an empty sleep data map for when no data is available.
     */
    private Map<String, Object> createEmptySleepData(long baseTime) {
        Map<String, Object> emptyData = new HashMap<>();
        emptyData.put("startTime", baseTime);
        emptyData.put("endTime", baseTime);
        emptyData.put("totalDuration", 0);
        emptyData.put("deepSleep", 0);
        emptyData.put("lightSleep", 0);
        emptyData.put("remSleep", 0);
        emptyData.put("awake", 0);
        return emptyData;
    }

    /**
     * Callback interface for data synchronization operations.
     */
    public interface DataSyncCallback {
        void onSuccess(Object data);
        void onError(String code, String message);
    }
}
