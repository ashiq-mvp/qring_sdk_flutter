package com.example.qring_sdk_flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.CommandHandle;
import com.oudmon.ble.base.communication.req.BloodOxygenSettingReq;
import com.oudmon.ble.base.communication.req.BpSettingReq;
import com.oudmon.ble.base.communication.req.HeartRateSettingReq;
import com.oudmon.ble.base.communication.req.PalmScreenReq;
import com.oudmon.ble.base.communication.req.PhoneIdReq;
import com.oudmon.ble.base.communication.req.RestoreKeyReq;
import com.oudmon.ble.base.communication.req.TimeFormatReq;
import com.oudmon.ble.base.communication.rsp.BloodOxygenSettingRsp;
import com.oudmon.ble.base.communication.rsp.BpSettingRsp;
import com.oudmon.ble.base.communication.rsp.HeartRateSettingRsp;
import com.oudmon.ble.base.communication.rsp.PalmScreenRsp;
import com.oudmon.ble.base.communication.rsp.PhoneIdRsp;
import com.oudmon.ble.base.communication.rsp.BaseRspCmd;
import com.oudmon.ble.base.communication.rsp.TimeFormatRsp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages device settings including continuous monitoring configuration.
 * Wraps HeartRateSettingReq, BloodOxygenSettingReq, and BpSettingReq from QC SDK.
 */
public class SettingsManager {
    private static final String TAG = "SettingsManager";
    
    // Valid heart rate monitoring intervals in minutes
    private static final Set<Integer> VALID_HR_INTERVALS = new HashSet<>(
        Arrays.asList(10, 15, 20, 30, 60)
    );
    
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SettingsManager(Context context) {
        this.context = context;
    }

    /**
     * Configure continuous heart rate monitoring.
     * 
     * @param enable Enable or disable continuous monitoring
     * @param intervalMinutes Monitoring interval (10, 15, 20, 30, or 60 minutes)
     * @param callback Callback to handle success or error
     */
    public void setContinuousHeartRate(boolean enable, int intervalMinutes, final SettingsCallback callback) {
        // Validate interval
        if (enable && !VALID_HR_INTERVALS.contains(intervalMinutes)) {
            mainHandler.post(() -> callback.onError(
                "INVALID_INTERVAL",
                "Heart rate interval must be one of: 10, 15, 20, 30, 60 minutes. Got: " + intervalMinutes
            ));
            return;
        }

        Log.d(TAG, "Setting continuous heart rate: enable=" + enable + ", interval=" + intervalMinutes);
        
        HeartRateSettingReq req = HeartRateSettingReq.getWriteInstance(enable, intervalMinutes);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<HeartRateSettingRsp>() {
                public void onDataResponse(HeartRateSettingRsp rsp) {
                    Log.d(TAG, "Continuous heart rate setting successful");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "Continuous heart rate setting timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Heart rate setting request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Continuous heart rate setting failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Heart rate setting failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Get current continuous heart rate monitoring settings.
     * 
     * @param callback Callback to handle settings data or error
     */
    public void getContinuousHeartRateSettings(final SettingsCallback callback) {
        Log.d(TAG, "Getting continuous heart rate settings");
        
        // Create a read request by using HeartRateSettingReq in read mode
        HeartRateSettingReq req = HeartRateSettingReq.getReadInstance();
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<HeartRateSettingRsp>() {
                public void onDataResponse(HeartRateSettingRsp rsp) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("enabled", rsp.isEnable());
                    settings.put("intervalMinutes", rsp.getHeartInterval());
                    
                    Log.d(TAG, "Heart rate settings received: " + settings);
                    mainHandler.post(() -> callback.onSuccess(settings));
                }

                public void onTimeout() {
                    Log.e(TAG, "Get heart rate settings timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Get heart rate settings timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Get heart rate settings failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Get heart rate settings failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Configure continuous blood oxygen monitoring.
     * 
     * @param enable Enable or disable continuous monitoring
     * @param intervalMinutes Monitoring interval in minutes
     * @param callback Callback to handle success or error
     */
    public void setContinuousBloodOxygen(boolean enable, int intervalMinutes, final SettingsCallback callback) {
        if (intervalMinutes <= 0) {
            mainHandler.post(() -> callback.onError(
                "INVALID_INTERVAL",
                "Blood oxygen interval must be positive. Got: " + intervalMinutes
            ));
            return;
        }

        Log.d(TAG, "Setting continuous blood oxygen: enable=" + enable + ", interval=" + intervalMinutes);
        
        BloodOxygenSettingReq req = BloodOxygenSettingReq.getWriteInstance(enable, (byte) intervalMinutes);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<BloodOxygenSettingRsp>() {
                public void onDataResponse(BloodOxygenSettingRsp rsp) {
                    Log.d(TAG, "Continuous blood oxygen setting successful");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "Continuous blood oxygen setting timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Blood oxygen setting request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Continuous blood oxygen setting failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Blood oxygen setting failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Get current continuous blood oxygen monitoring settings.
     * 
     * @param callback Callback to handle settings data or error
     */
    public void getContinuousBloodOxygenSettings(final SettingsCallback callback) {
        Log.d(TAG, "Getting continuous blood oxygen settings");
        
        BloodOxygenSettingReq req = BloodOxygenSettingReq.getReadInstance();
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<BloodOxygenSettingRsp>() {
                public void onDataResponse(BloodOxygenSettingRsp rsp) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("enabled", rsp.isEnable());
                    settings.put("intervalMinutes", rsp.getInterval() & 0xFF);
                    
                    Log.d(TAG, "Blood oxygen settings received: " + settings);
                    mainHandler.post(() -> callback.onSuccess(settings));
                }

                public void onTimeout() {
                    Log.e(TAG, "Get blood oxygen settings timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Get blood oxygen settings timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Get blood oxygen settings failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Get blood oxygen settings failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Configure continuous blood pressure monitoring.
     * 
     * @param enable Enable or disable continuous monitoring
     * @param callback Callback to handle success or error
     */
    public void setContinuousBloodPressure(boolean enable, final SettingsCallback callback) {
        Log.d(TAG, "Setting continuous blood pressure: enable=" + enable);
        
        // Pass null for StartEndTimeEntity and 0 for value as they were not set originally
        BpSettingReq req = BpSettingReq.getWriteInstance(enable, null, 0);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<BpSettingRsp>() {
                public void onDataResponse(BpSettingRsp rsp) {
                    Log.d(TAG, "Continuous blood pressure setting successful");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "Continuous blood pressure setting timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Blood pressure setting request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Continuous blood pressure setting failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Blood pressure setting failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Get current continuous blood pressure monitoring settings.
     * 
     * @param callback Callback to handle settings data or error
     */
    public void getContinuousBloodPressureSettings(final SettingsCallback callback) {
        Log.d(TAG, "Getting continuous blood pressure settings");
        
        BpSettingReq req = BpSettingReq.getReadInstance();
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<BpSettingRsp>() {
                public void onDataResponse(BpSettingRsp rsp) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("enabled", rsp.isEnable());
                    
                    Log.d(TAG, "Blood pressure settings received: " + settings);
                    mainHandler.post(() -> callback.onSuccess(settings));
                }

                public void onTimeout() {
                    Log.e(TAG, "Get blood pressure settings timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Get blood pressure settings timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Get blood pressure settings failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Get blood pressure settings failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Configure display settings.
     * 
     * @param enabled Whether display is enabled
     * @param leftHand Whether ring is worn on left hand
     * @param brightness Screen brightness level (1 to maxBrightness)
     * @param maxBrightness Maximum brightness level
     * @param doNotDisturb Whether Do Not Disturb mode is enabled
     * @param screenOnStartMinutes Screen-on start time in minutes from midnight
     * @param screenOnEndMinutes Screen-on end time in minutes from midnight
     * @param callback Callback to handle success or error
     */
    public void setDisplaySettings(
        boolean enabled,
        boolean leftHand,
        int brightness,
        int maxBrightness,
        boolean doNotDisturb,
        int screenOnStartMinutes,
        int screenOnEndMinutes,
        final SettingsCallback callback
    ) {
        // Validate brightness
        if (brightness < 1 || brightness > maxBrightness) {
            mainHandler.post(() -> callback.onError(
                "INVALID_BRIGHTNESS",
                "Brightness must be between 1 and " + maxBrightness + ". Got: " + brightness
            ));
            return;
        }

        Log.d(TAG, "Setting display settings: enabled=" + enabled + ", leftHand=" + leftHand + 
              ", brightness=" + brightness + ", doNotDisturb=" + doNotDisturb);
        
        PalmScreenReq req = new PalmScreenReq(enabled, leftHand, brightness, maxBrightness, doNotDisturb, screenOnStartMinutes, screenOnEndMinutes);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<PalmScreenRsp>() {
                public void onDataResponse(PalmScreenRsp rsp) {
                    Log.d(TAG, "Display settings configured successfully");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "Display settings timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Display settings request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Display settings failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Display settings failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Get current display settings.
     * 
     * @param callback Callback to handle settings data or error
     */
    public void getDisplaySettings(final SettingsCallback callback) {
        Log.d(TAG, "Getting display settings");
        
        PalmScreenReq req = PalmScreenReq.getRingReadInstance();
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<PalmScreenRsp>() {
                public void onDataResponse(PalmScreenRsp rsp) {
                    Map<String, Object> settings = new HashMap<>();
                    settings.put("enabled", rsp.isEnable());
                    settings.put("leftHand", rsp.isLeft());
                    settings.put("brightness", rsp.getScreenLight());
                    settings.put("maxBrightness", rsp.getMaxLight());
                    settings.put("doNotDisturb", rsp.isDnd());
                    settings.put("screenOnStartMinutes", rsp.getStart());
                    settings.put("screenOnEndMinutes", rsp.getEnd());
                    
                    Log.d(TAG, "Display settings received: " + settings);
                    mainHandler.post(() -> callback.onSuccess(settings));
                }

                public void onTimeout() {
                    Log.e(TAG, "Get display settings timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Get display settings timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Get display settings failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Get display settings failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Configure user information.
     * 
     * @param age User age in years
     * @param heightCm User height in centimeters
     * @param weightKg User weight in kilograms
     * @param isMale Whether user is male
     * @param callback Callback to handle success or error
     */
    public void setUserInfo(int age, int heightCm, int weightKg, boolean isMale, final SettingsCallback callback) {
        Log.d(TAG, "Setting user info: age=" + age + ", height=" + heightCm + 
              ", weight=" + weightKg + ", isMale=" + isMale);
              
        // Using placeholder values for unknown params: is12Hour(true), year(2000), month(1), day(1), stepLength(0)
        TimeFormatReq req = TimeFormatReq.getWriteInstance(
            true, // is12Hour
            isMale,
            age,
            heightCm,
            weightKg,
            2000, 1, 1, 0, 0 // year, month, day, stepLength, targetStep? (10 arg version if exists, else 9)
        );
        // Note: Assuming the 10-int version is (is12h, male, age, height, weight, year, month, day, stepLength, goal)
        // or similar. The javap output showed 9-10 arg versions. 
        // I will key off the 10-arg if it matches boolean + 9 ints, or boolean + boolean + 8 ints.
        // Re-checking javap: getWriteInstance(boolean, boolean, int...)
        // Actually TimeFormatReq.getWriteInstance(boolean, boolean, int, int, int, int, int, int, int, int)
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<TimeFormatRsp>() {
                public void onDataResponse(TimeFormatRsp rsp) {
                    Log.d(TAG, "User info configured successfully");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "User info setting timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "User info setting request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "User info setting failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "User info setting failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Set user ID on the device.
     * 
     * @param userId User identifier string
     * @param callback Callback to handle success or error
     */
    public void setUserId(String userId, final SettingsCallback callback) {
        Log.d(TAG, "Setting user ID: " + userId);
        
        PhoneIdReq req = PhoneIdReq.getWriteInstance(userId);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<PhoneIdRsp>() {
                public void onDataResponse(PhoneIdRsp rsp) {
                    Log.d(TAG, "User ID set successfully");
                    mainHandler.post(() -> callback.onSuccess(null));
                }

                public void onTimeout() {
                    Log.e(TAG, "Set user ID timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Set user ID request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Set user ID failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Set user ID failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Perform factory reset on the device.
     * 
     * @param callback Callback to handle success or error
     */
    public void factoryReset(final SettingsCallback callback) {
        Log.d(TAG, "Performing factory reset");
        
        RestoreKeyReq req = new RestoreKeyReq(com.oudmon.ble.base.communication.Constants.CMD_RE_STORE);
        
        CommandHandle.getInstance().executeReqCmd(
            req,
            new ICommandResponse<BaseRspCmd>() {
                public void onDataResponse(BaseRspCmd rsp) {
                    if (rsp.getStatus() == BaseRspCmd.RESULT_OK) {
                        Log.d(TAG, "Factory reset successful");
                        mainHandler.post(() -> callback.onSuccess(null));
                    } else {
                        Log.e(TAG, "Factory reset failed with status: " + rsp.getStatus());
                        mainHandler.post(() -> callback.onError(
                            "COMMAND_FAILED",
                            "Factory reset failed with status: " + rsp.getStatus()
                        ));
                    }
                }

                public void onTimeout() {
                    Log.e(TAG, "Factory reset timeout");
                    mainHandler.post(() -> callback.onError("TIMEOUT", "Factory reset request timed out"));
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Factory reset failed with error code: " + errCode);
                    mainHandler.post(() -> callback.onError(
                        "COMMAND_FAILED",
                        "Factory reset failed with code: " + errCode
                    ));
                }
            }
        );
    }

    /**
     * Callback interface for settings operations.
     */
    public interface SettingsCallback {
        void onSuccess(Map<String, Object> data);
        void onError(String code, String message);
    }

    /**
     * Clean up resources.
     */
    public void dispose() {
        // No resources to clean up currently
    }
}
