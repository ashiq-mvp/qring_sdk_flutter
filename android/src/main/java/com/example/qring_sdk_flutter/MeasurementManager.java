package com.example.qring_sdk_flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.communication.ICommandResponse;
import com.oudmon.ble.base.communication.rsp.BpDataRsp;
import com.oudmon.ble.base.communication.rsp.StartHeartRateRsp;
import com.oudmon.ble.base.communication.rsp.BaseRspCmd;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

/**
 * Manages manual health measurements.
 * Wraps BleOperateManager manual measurement methods from QC SDK.
 * 
 * Note: Manual measurements use StartHeartRateRsp for starting, but real-time data
 * comes through DeviceNotifyListener. The callback here just confirms the measurement started.
 */
public class MeasurementManager {
    private static final String TAG = "MeasurementManager";
    
    private final Context context;
    private EventChannel.EventSink measurementSink;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String activeMeasurementType = null;

    public MeasurementManager(Context context) {
        this.context = context;
    }

    public void setMeasurementSink(EventChannel.EventSink sink) {
        this.measurementSink = sink;
    }

    /**
     * Start manual heart rate measurement.
     * The callback confirms the measurement started.
     * Real-time results come through DeviceNotifyListener.
     */
    public void startHeartRateMeasurement() {
        Log.d(TAG, "Starting heart rate measurement");
        activeMeasurementType = "heartRate";
        
        BleOperateManager.getInstance().manualModeHeart(
            new ICommandResponse<StartHeartRateRsp>() {
                public void onDataResponse(StartHeartRateRsp rsp) {
                    Log.d(TAG, "Heart rate measurement started");
                    
                    Map<String, Object> measurement = new HashMap<>();
                    measurement.put("type", "heartRate");
                    measurement.put("timestamp", System.currentTimeMillis());
                    measurement.put("status", "started");
                    measurement.put("success", true);
                    
                    emitMeasurement(measurement);
                }

                public void onTimeout() {
                    Log.e(TAG, "Heart rate measurement start timeout");
                    emitError("heartRate", "TIMEOUT", "Heart rate measurement start timed out");
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Heart rate measurement start failed with error code: " + errCode);
                    emitError("heartRate", "MEASUREMENT_FAILED", "Heart rate measurement start failed with code: " + errCode);
                }
            },
            false // false = start measurement, true = stop measurement
        );
    }

    /**
     * Start manual blood pressure measurement.
     * The callback confirms the measurement started.
     * Real-time results come through DeviceNotifyListener.
     */
    public void startBloodPressureMeasurement() {
        Log.d(TAG, "Starting blood pressure measurement");
        activeMeasurementType = "bloodPressure";
        
        BleOperateManager.getInstance().manualModeBP(
            new ICommandResponse<StartHeartRateRsp>() {
                public void onDataResponse(StartHeartRateRsp rsp) {
                    Log.d(TAG, "Blood pressure measurement started");
                    
                    Map<String, Object> measurement = new HashMap<>();
                    measurement.put("type", "bloodPressure");
                    measurement.put("timestamp", System.currentTimeMillis());
                    measurement.put("status", "started");
                    measurement.put("success", true);
                    
                    emitMeasurement(measurement);
                }

                public void onTimeout() {
                    Log.e(TAG, "Blood pressure measurement start timeout");
                    emitError("bloodPressure", "TIMEOUT", "Blood pressure measurement start timed out");
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Blood pressure measurement start failed with error code: " + errCode);
                    emitError("bloodPressure", "MEASUREMENT_FAILED", "Blood pressure measurement start failed with code: " + errCode);
                }
            },
            false // false = start measurement, true = stop measurement
        );
    }

    /**
     * Start manual blood oxygen (SpO2) measurement.
     * The callback confirms the measurement started.
     * Real-time results come through DeviceNotifyListener.
     */
    public void startBloodOxygenMeasurement() {
        Log.d(TAG, "Starting blood oxygen measurement");
        activeMeasurementType = "bloodOxygen";
        
        BleOperateManager.getInstance().manualModeSpO2(
            new ICommandResponse<StartHeartRateRsp>() {
                public void onDataResponse(StartHeartRateRsp rsp) {
                    Log.d(TAG, "Blood oxygen measurement started");
                    
                    Map<String, Object> measurement = new HashMap<>();
                    measurement.put("type", "bloodOxygen");
                    measurement.put("timestamp", System.currentTimeMillis());
                    measurement.put("status", "started");
                    measurement.put("success", true);
                    
                    emitMeasurement(measurement);
                }

                public void onTimeout() {
                    Log.e(TAG, "Blood oxygen measurement start timeout");
                    emitError("bloodOxygen", "TIMEOUT", "Blood oxygen measurement start timed out");
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Blood oxygen measurement start failed with error code: " + errCode);
                    emitError("bloodOxygen", "MEASUREMENT_FAILED", "Blood oxygen measurement start failed with code: " + errCode);
                }
            },
            false // false = start measurement, true = stop measurement
        );
    }

    /**
     * Start manual temperature measurement.
     * The callback confirms the measurement started.
     * Real-time results come through DeviceNotifyListener.
     */
    public void startTemperatureMeasurement() {
        Log.d(TAG, "Starting temperature measurement");
        activeMeasurementType = "temperature";
        
        BleOperateManager.getInstance().manualTemperature(
            new ICommandResponse<StartHeartRateRsp>() {
                public void onDataResponse(StartHeartRateRsp rsp) {
                    Log.d(TAG, "Temperature measurement started");
                    
                    Map<String, Object> measurement = new HashMap<>();
                    measurement.put("type", "temperature");
                    measurement.put("timestamp", System.currentTimeMillis());
                    measurement.put("status", "started");
                    measurement.put("success", true);
                    
                    emitMeasurement(measurement);
                }

                public void onTimeout() {
                    Log.e(TAG, "Temperature measurement start timeout");
                    emitError("temperature", "TIMEOUT", "Temperature measurement start timed out");
                }

                public void onFailed(int errCode) {
                    Log.e(TAG, "Temperature measurement start failed with error code: " + errCode);
                    emitError("temperature", "MEASUREMENT_FAILED", "Temperature measurement start failed with code: " + errCode);
                }
            },
            false // false = start measurement, true = stop measurement
        );
    }

    /**
     * Stop any active manual measurement.
     */
    public void stopMeasurement() {
        if (activeMeasurementType == null) {
            Log.d(TAG, "No active measurement to stop");
            return;
        }

        Log.d(TAG, "Stopping measurement: " + activeMeasurementType);
        
        // Call the appropriate stop method based on active measurement type
        switch (activeMeasurementType) {
            case "heartRate":
                BleOperateManager.getInstance().manualModeHeart(null, true);
                break;
            case "bloodPressure":
                BleOperateManager.getInstance().manualModeBP(null, true);
                break;
            case "bloodOxygen":
                BleOperateManager.getInstance().manualModeSpO2(null, true);
                break;
            case "temperature":
                BleOperateManager.getInstance().manualTemperature(null, true);
                break;
            default:
                Log.w(TAG, "Unknown measurement type: " + activeMeasurementType);
                break;
        }
        
        activeMeasurementType = null;
    }

    /**
     * Emit measurement result to Flutter via event channel.
     */
    private void emitMeasurement(Map<String, Object> measurement) {
        if (measurementSink != null) {
            mainHandler.post(() -> measurementSink.success(measurement));
        } else {
            Log.w(TAG, "Measurement sink is null, cannot emit measurement");
        }
    }

    /**
     * Emit error to Flutter via event channel.
     */
    private void emitError(String measurementType, String errorCode, String errorMessage) {
        Map<String, Object> measurement = new HashMap<>();
        measurement.put("type", measurementType);
        measurement.put("timestamp", System.currentTimeMillis());
        measurement.put("success", false);
        measurement.put("errorMessage", errorMessage);
        
        emitMeasurement(measurement);
    }

    /**
     * Clean up resources.
     */
    public void dispose() {
        stopMeasurement();
        measurementSink = null;
    }
}
