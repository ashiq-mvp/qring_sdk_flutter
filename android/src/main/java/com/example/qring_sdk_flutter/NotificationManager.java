package com.example.qring_sdk_flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.bluetooth.ListenerKey;
import com.oudmon.ble.base.communication.responseImpl.DeviceNotifyListener;
import com.oudmon.ble.base.communication.rsp.DeviceNotifyRsp;
import com.oudmon.ble.base.communication.rsp.BaseRspCmd;
import com.oudmon.ble.base.communication.utils.BLEDataFormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

/**
 * Manages real-time notifications from the QC Ring device.
 * Uses DeviceNotifyListener with unified onDataResponse method.
 * All notification data comes through DeviceNotifyRsp with different dataType values.
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    
    // Data type constants from SDK
    private static final int DATA_TYPE_HEART_RATE = 1;
    private static final int DATA_TYPE_BLOOD_PRESSURE = 2;
    private static final int DATA_TYPE_BLOOD_OXYGEN = 3;
    private static final int DATA_TYPE_STEP_CHANGE = 4;
    private static final int DATA_TYPE_TEMPERATURE = 5;
    private static final int DATA_TYPE_EXERCISE_RECORD = 7;
    private static final int DATA_TYPE_CUSTOM_BUTTON = 0x2d;
    private static final int DATA_TYPE_HEART_RATE_REMINDER = 0x3A;
    private static final int DATA_TYPE_TEMPERATURE_MEASURE = 0x3b;
    
    private final Context context;
    private final List<EventChannel.EventSink> notificationSinks = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DeviceNotifyListener deviceNotifyListener;
    private boolean isListening = false;

    public NotificationManager(Context context) {
        this.context = context;
        setupNotificationListener();
    }

    /**
     * Add a notification sink to receive notifications.
     * Supports multiple concurrent listeners.
     */
    public void addNotificationSink(EventChannel.EventSink sink) {
        synchronized (notificationSinks) {
            if (!notificationSinks.contains(sink)) {
                notificationSinks.add(sink);
                Log.d(TAG, "Added notification sink. Total sinks: " + notificationSinks.size());
            }
        }
        
        // Start listening if not already listening
        if (!isListening) {
            startListening();
        }
    }

    /**
     * Remove a notification sink.
     */
    public void removeNotificationSink(EventChannel.EventSink sink) {
        synchronized (notificationSinks) {
            notificationSinks.remove(sink);
            Log.d(TAG, "Removed notification sink. Total sinks: " + notificationSinks.size());
        }
        
        // Stop listening if no more sinks
        if (notificationSinks.isEmpty() && isListening) {
            stopListening();
        }
    }

    /**
     * Setup the device notification listener.
     * Uses unified onDataResponse method that receives DeviceNotifyRsp with different dataType values.
     */
    private void setupNotificationListener() {
        deviceNotifyListener = new DeviceNotifyListener() {
            @Override
            public void onDataResponse(DeviceNotifyRsp resultEntity) {
                if (resultEntity == null) {
                    Log.w(TAG, "Received null DeviceNotifyRsp");
                    return;
                }
                
                if (resultEntity.getStatus() != BaseRspCmd.RESULT_OK) {
                    Log.w(TAG, "Notification response status not OK: " + resultEntity.getStatus());
                    return;
                }
                
                // Parse notification based on dataType
                switch (resultEntity.getDataType()) {
                    case DATA_TYPE_HEART_RATE:
                        handleHeartRateNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_BLOOD_PRESSURE:
                        handleBloodPressureNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_BLOOD_OXYGEN:
                        handleBloodOxygenNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_STEP_CHANGE:
                        handleStepChangeNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_TEMPERATURE:
                        handleTemperatureNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_HEART_RATE_REMINDER:
                        handleHeartRateReminderNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_TEMPERATURE_MEASURE:
                        handleTemperatureMeasureNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_EXERCISE_RECORD:
                        handleExerciseRecordNotification(resultEntity);
                        break;
                        
                    case DATA_TYPE_CUSTOM_BUTTON:
                        handleCustomButtonNotification(resultEntity);
                        break;
                        
                    default:
                        Log.d(TAG, "Unknown notification dataType: " + resultEntity.getDataType());
                        break;
                }
            }
        };
    }
    
    /**
     * Handle heart rate notification.
     */
    private void handleHeartRateNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 1) {
            Log.w(TAG, "Invalid heart rate data");
            return;
        }
        
        int heartRate = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[0]});
        Log.d(TAG, "Heart rate notification: " + heartRate);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "heartRate");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("heartRate", heartRate);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle blood pressure notification.
     */
    private void handleBloodPressureNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 2) {
            Log.w(TAG, "Invalid blood pressure data");
            return;
        }
        
        int systolic = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[0]});
        int diastolic = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[1]});
        Log.d(TAG, "Blood pressure notification: " + systolic + "/" + diastolic);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "bloodPressure");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("systolic", systolic);
        notification.put("diastolic", diastolic);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle blood oxygen notification.
     */
    private void handleBloodOxygenNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 1) {
            Log.w(TAG, "Invalid blood oxygen data");
            return;
        }
        
        int spO2 = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[0]});
        Log.d(TAG, "Blood oxygen notification: " + spO2);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "bloodOxygen");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("spO2", spO2);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle step change notification.
     */
    private void handleStepChangeNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 4) {
            Log.w(TAG, "Invalid step count data");
            return;
        }
        
        int steps = BLEDataFormatUtils.bytes2Int(new byte[]{
            rsp.getLoadData()[0], rsp.getLoadData()[1], rsp.getLoadData()[2], rsp.getLoadData()[3]
        });
        Log.d(TAG, "Step count notification: " + steps);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "stepCount");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("steps", steps);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle temperature notification (daily temperature change).
     */
    private void handleTemperatureNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 2) {
            Log.w(TAG, "Invalid temperature data");
            return;
        }
        
        int tempValue = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[0], rsp.getLoadData()[1]});
        double tempCelsius = tempValue / 100.0;
        Log.d(TAG, "Temperature notification: " + tempCelsius + "°C");
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "temperature");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("temperature", tempCelsius);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle heart rate reminder notification (too low/high).
     */
    private void handleHeartRateReminderNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 3) {
            Log.w(TAG, "Invalid heart rate reminder data");
            return;
        }
        
        int type = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[1]});
        int value = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[2]});
        
        String reminderType = type == 1 ? "tooLow" : type == 2 ? "tooHigh" : "unknown";
        Log.d(TAG, "Heart rate reminder: " + reminderType + " value: " + value);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "heartRateReminder");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("reminderType", reminderType);
        notification.put("heartRate", value);
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle temperature measurement notification.
     */
    private void handleTemperatureMeasureNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 3) {
            Log.w(TAG, "Invalid temperature measurement data");
            return;
        }
        
        int tempValue = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[1], rsp.getLoadData()[2]});
        if (tempValue > 0) {
            double tempCelsius = tempValue / 100.0;
            Log.d(TAG, "Temperature measurement: " + tempCelsius + "°C");
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "temperature");
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("temperature", tempCelsius);
            notification.put("success", true);
            
            emitNotification(notification);
        }
    }
    
    /**
     * Handle exercise record notification.
     */
    private void handleExerciseRecordNotification(DeviceNotifyRsp rsp) {
        Log.d(TAG, "New exercise record generated");
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "exerciseRecord");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("success", true);
        
        emitNotification(notification);
    }
    
    /**
     * Handle custom button notification.
     */
    private void handleCustomButtonNotification(DeviceNotifyRsp rsp) {
        if (rsp.getLoadData() == null || rsp.getLoadData().length < 2) {
            Log.w(TAG, "Invalid custom button data");
            return;
        }
        
        int event = BLEDataFormatUtils.bytes2Int(new byte[]{rsp.getLoadData()[1]});
        // event: 0=null, 1=decline, 2=slide up, 3=single, 4=long press
        String eventType = event == 1 ? "decline" : event == 2 ? "slideUp" : 
                          event == 3 ? "single" : event == 4 ? "longPress" : "unknown";
        Log.d(TAG, "Custom button event: " + eventType);
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "customButton");
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("eventType", eventType);
        notification.put("success", true);
        
        emitNotification(notification);
    }

    /**
     * Start listening for device notifications.
     * Uses addOutDeviceListener with ListenerKey instead of setDeviceNotifyListener.
     */
    private void startListening() {
        if (isListening) {
            Log.d(TAG, "Already listening for notifications");
            return;
        }

        Log.d(TAG, "Starting notification listener");
        // Register listener for all notification types
        // Use ListenerKey.Heart for heart rate notifications
        // Use ListenerKey.All for all notification types
        BleOperateManager.getInstance().addOutDeviceListener(ListenerKey.All, deviceNotifyListener);
        isListening = true;
    }

    /**
     * Stop listening for device notifications.
     */
    private void stopListening() {
        if (!isListening) {
            Log.d(TAG, "Not currently listening for notifications");
            return;
        }

        Log.d(TAG, "Stopping notification listener");
        BleOperateManager.getInstance().removeNotifyListener(ListenerKey.All);
        isListening = false;
    }

    /**
     * Emit notification to all registered sinks.
     * Supports multiple concurrent listeners.
     */
    private void emitNotification(Map<String, Object> notification) {
        synchronized (notificationSinks) {
            if (notificationSinks.isEmpty()) {
                Log.w(TAG, "No notification sinks registered, cannot emit notification");
                return;
            }

            // Create a copy of the list to avoid concurrent modification
            List<EventChannel.EventSink> sinksCopy = new ArrayList<>(notificationSinks);
            
            mainHandler.post(() -> {
                for (EventChannel.EventSink sink : sinksCopy) {
                    try {
                        sink.success(notification);
                    } catch (Exception e) {
                        Log.e(TAG, "Error emitting notification to sink: " + e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Clean up resources.
     */
    public void dispose() {
        stopListening();
        synchronized (notificationSinks) {
            notificationSinks.clear();
        }
    }
}
