package com.example.qring_sdk_flutter;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import android.bluetooth.le.ScanResult;
import com.oudmon.ble.base.bluetooth.BleAction;
import com.oudmon.ble.base.bluetooth.BleOperateManager;
import com.oudmon.ble.base.scan.BleScannerHelper;
import com.oudmon.ble.base.scan.ScanWrapperCallback;
import com.oudmon.ble.base.scan.ScanRecord;

/**
 * Flutter Plugin for QRing SDK with simplified BLE Connection Manager integration.
 * 
 * This plugin provides a clean API for Flutter to control BLE operations through
 * the centralized BleConnectionManager. The plugin focuses on observation and
 * simple commands, while the BleConnectionManager handles all BLE complexity.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 */
public class QringSdkFlutterPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private static final String TAG = "QringSdkFlutterPlugin";
    private static final String ACTION_SERVICE_STATE = "com.example.qring_sdk_flutter.SERVICE_STATE";
    private static final String EXTRA_EVENT_TYPE = "event_type";
    
    private MethodChannel channel;
    private EventChannel stateChannel;
    private EventChannel devicesChannel;
    private EventChannel measurementChannel;
    private EventChannel notificationChannel;
    private EventChannel exerciseChannel;
    private EventChannel firmwareProgressChannel;
    private EventChannel serviceStateChannel;
    
    // New event channels for BLE Connection Manager events
    private EventChannel bleConnectionStateChannel;
    private EventChannel bleBatteryChannel;
    private EventChannel bleErrorChannel;
    
    private Context context;
    
    // New: Use BleConnectionManager instead of BleManager
    private BleConnectionManager bleConnectionManager;
    
    // Event sinks for BLE Connection Manager events
    private EventChannel.EventSink bleConnectionStateSink;
    private EventChannel.EventSink bleBatterySink;
    private EventChannel.EventSink bleErrorSink;
    
    // Keep other managers for data operations (not connection management)
    private BleManager bleManager; // Still needed for scanning
    private DataSyncManager dataSyncManager;
    private MeasurementManager measurementManager;
    private SettingsManager settingsManager;
    private NotificationManager notificationManager;
    private ExerciseManager exerciseManager;
    private FirmwareManager firmwareManager;
    private PermissionManager permissionManager;
    private EventChannel.EventSink serviceStateSink;
    private android.content.BroadcastReceiver serviceStateReceiver;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        
        // Initialize BLE SDK - getInstance requires Application context
        BleOperateManager.getInstance((android.app.Application) context.getApplicationContext());
        BleOperateManager.getInstance().init();
        
        // Initialize the new BleConnectionManager
        bleConnectionManager = BleConnectionManager.getInstance();
        bleConnectionManager.initialize(context);
        
        // Create managers
        bleManager = new BleManager(context); // Still needed for scanning
        dataSyncManager = new DataSyncManager(context);
        measurementManager = new MeasurementManager(context);
        settingsManager = new SettingsManager(context);
        notificationManager = new NotificationManager(context);
        exerciseManager = new ExerciseManager(context);
        firmwareManager = new FirmwareManager(context);
        permissionManager = new PermissionManager(context);

        // Register the broadcast receiver for connection state changes
        IntentFilter intentFilter = BleAction.getIntentFilter();
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(bleManager.getConnectionReceiver(), intentFilter);

        channel = new MethodChannel(binding.getBinaryMessenger(), "qring_sdk_flutter");
        channel.setMethodCallHandler(this);

        stateChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/state");
        stateChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bleManager.setStateSink(events);
            }
            @Override
            public void onCancel(Object arguments) {
                bleManager.setStateSink(null);
            }
        });

        devicesChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/devices");
        devicesChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bleManager.setDevicesSink(events);
            }
            @Override
            public void onCancel(Object arguments) {
                bleManager.setDevicesSink(null);
            }
        });

        measurementChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/measurement");
        measurementChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) { 
                measurementManager.setMeasurementSink(events);
            }
            @Override
            public void onCancel(Object arguments) { 
                measurementManager.setMeasurementSink(null);
            }
        });

        notificationChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/notification");
        notificationChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink eventSink) {
                notificationManager.addNotificationSink(eventSink);
            }
            @Override
            public void onCancel(Object arguments) {
                // Note: We need to keep a reference to the eventSink to remove it
                // For now, we'll clear all sinks when cancelled
                // notificationManager.removeNotificationSink(eventSink);
            }
        });

        exerciseChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/exercise");
        exerciseChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                exerciseManager.setExerciseSink(events);
            }
            @Override
            public void onCancel(Object arguments) {
                exerciseManager.setExerciseSink(null);
            }
        });

        firmwareProgressChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/firmware_progress");
        firmwareProgressChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                firmwareManager.setProgressSink(events);
            }
            @Override
            public void onCancel(Object arguments) {
                firmwareManager.setProgressSink(null);
            }
        });

        serviceStateChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/service_state");
        serviceStateChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                serviceStateSink = events;
            }
            @Override
            public void onCancel(Object arguments) {
                serviceStateSink = null;
            }
        });
        
        // New event channels for BLE Connection Manager events
        bleConnectionStateChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/ble_connection_state");
        bleConnectionStateChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bleConnectionStateSink = events;
            }
            @Override
            public void onCancel(Object arguments) {
                bleConnectionStateSink = null;
            }
        });
        
        bleBatteryChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/ble_battery");
        bleBatteryChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bleBatterySink = events;
            }
            @Override
            public void onCancel(Object arguments) {
                bleBatterySink = null;
            }
        });
        
        bleErrorChannel = new EventChannel(binding.getBinaryMessenger(), "qring_sdk_flutter/ble_error");
        bleErrorChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                bleErrorSink = events;
            }
            @Override
            public void onCancel(Object arguments) {
                bleErrorSink = null;
            }
        });
        
        // Register BleConnectionManager state observer to emit events
        bleConnectionManager.registerObserver(new BleConnectionManager.StateObserver() {
            @Override
            public void onStateChanged(BleConnectionManager.BleState oldState, BleConnectionManager.BleState newState) {
                emitBleConnectionStateEvent(oldState, newState);
            }
        });
        
        // Register BroadcastReceiver for service state events
        serviceStateReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_SERVICE_STATE.equals(intent.getAction())) {
                    String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
                    String deviceMac = intent.getStringExtra(NotificationConfig.EXTRA_DEVICE_MAC);
                    boolean isRunning = intent.getBooleanExtra("isRunning", false);
                    boolean isConnected = intent.getBooleanExtra("isConnected", false);
                    
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("eventType", eventType);
                    eventData.put("isRunning", isRunning);
                    eventData.put("isConnected", isConnected);
                    if (deviceMac != null) {
                        eventData.put("deviceMac", deviceMac);
                    }
                    
                    if (serviceStateSink != null) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            serviceStateSink.success(eventData);
                        });
                    }
                }
            }
        };
        
        IntentFilter serviceStateFilter = new IntentFilter(ACTION_SERVICE_STATE);
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceStateReceiver, serviceStateFilter);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            // Scanning methods (still use old BleManager for now)
            case "scan":
                bleManager.startScan();
                result.success(null);
                break;
            case "stopScan":
                bleManager.stopScan();
                result.success(null);
                break;
                
            // New simplified connection methods using BleConnectionManager
            case "connectRing": {
                String mac = call.argument("mac");
                if (!ValidationUtils.isValidString(mac)) {
                    ExceptionHandler.sendValidationError(result, "MAC address", "non-empty string");
                    return;
                }
                if (!ValidationUtils.isValidMacAddress(mac)) {
                    ExceptionHandler.sendValidationError(result, "MAC address", "XX:XX:XX:XX:XX:XX format");
                    return;
                }
                SafeResult connectSafeResult = new SafeResult(result, "connectRing");
                try {
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    if (adapter == null) {
                        connectSafeResult.error(ErrorCodes.BLUETOOTH_UNAVAILABLE, 
                            "Bluetooth adapter not available", null);
                        return;
                    }
                    
                    BluetoothDevice device = adapter.getRemoteDevice(mac);
                    bleConnectionManager.connect(device, new BleConnectionManager.ConnectionCallback() {
                        @Override
                        public void onConnectionSuccess(BluetoothDevice device, BluetoothGatt gatt) {
                            Log.d(TAG, "Connection successful: " + device.getAddress());
                            connectSafeResult.success(null);
                        }
                        
                        @Override
                        public void onConnectionFailed(BluetoothDevice device, String error) {
                            Log.e(TAG, "Connection failed: " + error);
                            connectSafeResult.error(ErrorCodes.CONNECTION_FAILED, error, null);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during connectRing", e);
                    connectSafeResult.error(
                        ErrorCodes.SDK_ERROR,
                        ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage()),
                        null
                    );
                }
                break;
            }
            
            case "disconnectRing": {
                SafeResult disconnectSafeResult = new SafeResult(result, "disconnectRing");
                try {
                    bleConnectionManager.disconnect();
                    disconnectSafeResult.success(null);
                } catch (Exception e) {
                    Log.e(TAG, "Error during disconnectRing", e);
                    disconnectSafeResult.error(
                        ErrorCodes.SDK_ERROR,
                        ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage()),
                        null
                    );
                }
                break;
            }
            
            case "getConnectionState": {
                SafeResult getStateSafeResult = new SafeResult(result, "getConnectionState");
                try {
                    BleConnectionManager.BleState state = bleConnectionManager.getState();
                    String stateString = convertBleStateToString(state);
                    getStateSafeResult.success(stateString);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting connection state", e);
                    getStateSafeResult.error(
                        ErrorCodes.SDK_ERROR,
                        ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage()),
                        null
                    );
                }
                break;
            }
            
            case "findMyRing": {
                if (!bleConnectionManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult findRingSafeResult = new SafeResult(result, "findMyRing");
                bleConnectionManager.findRing(new BleConnectionManager.CommandCallback() {
                    @Override
                    public void onSuccess() {
                        findRingSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        findRingSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            
            // Legacy methods - keep for backward compatibility but use old BleManager
            case "connect": {
                String mac = call.argument("mac");
                if (!ValidationUtils.isValidString(mac)) {
                    ExceptionHandler.sendValidationError(result, "MAC address", "non-empty string");
                    return;
                }
                if (!ValidationUtils.isValidMacAddress(mac)) {
                    ExceptionHandler.sendValidationError(result, "MAC address", "XX:XX:XX:XX:XX:XX format");
                    return;
                }
                SafeResult connectSafeResult = new SafeResult(result, "connect");
                try {
                    bleManager.connect(mac);
                    connectSafeResult.success(null);
                } catch (Exception e) {
                    Log.e("QRingSDK", "Error during connect", e);
                    connectSafeResult.error(
                        ErrorCodes.SDK_ERROR,
                        ErrorCodes.getMessage(ErrorCodes.SDK_ERROR, e.getMessage()),
                        null
                    );
                }
                break;
            }
            case "disconnect":
                bleManager.disconnect();
                result.success(null);
                break;
            case "findRing":
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult findRingSafeResult = new SafeResult(result, "findRing");
                bleManager.findRing(new BleManager.FindRingCallback() {
                    @Override
                    public void onSuccess() {
                        findRingSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        findRingSafeResult.error(code, message, null);
                    }
                });
                break;
            case "battery":
                if (!bleManager.isConnected()) {
                    result.success(-1);
                    return;
                }
                SafeResult batterySafeResult = new SafeResult(result, "battery");
                bleManager.getBattery(new BleManager.BatteryCallback() {
                    @Override
                    public void onBatteryLevel(int level) {
                        batterySafeResult.success(level);
                    }

                    @Override
                    public void onError(String code, String message) {
                        batterySafeResult.error(code, message, null);
                    }
                });
                break;
            case "deviceInfo": {
                if (!bleManager.isConnected()) {
                    result.success(new HashMap<>());
                    return;
                }
                SafeResult safeResult = new SafeResult(result, "deviceInfo");
                bleManager.getDeviceInfo(new BleManager.DeviceInfoCallback() {
                    @Override
                    public void onDeviceInfo(Map<String, Object> info) {
                        safeResult.success(info);
                    }

                    @Override
                    public void onError(String code, String message) {
                        safeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "syncStepData": {
                if (!bleManager.isConnected()) {
                    ExceptionHandler.sendConnectionError(result);
                    return;
                }
                Integer dayOffset = call.argument("dayOffset");
                if (dayOffset == null) {
                    ExceptionHandler.sendValidationError(result, "dayOffset", "integer value");
                    return;
                }
                if (!ValidationUtils.isValidDayOffset(dayOffset)) {
                    ExceptionHandler.sendValidationError(result, "dayOffset", "0-6 (days)");
                    return;
                }
                SafeResult syncStepSafeResult = new SafeResult(result, "syncStepData");
                try {
                    dataSyncManager.syncStepData(dayOffset, new DataSyncManager.DataSyncCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            syncStepSafeResult.success(data);
                        }

                        @Override
                        public void onError(String code, String message) {
                            syncStepSafeResult.error(code, message, null);
                        }
                    });
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, syncStepSafeResult, "syncStepData");
                }
                break;
            }
            case "syncHeartRateData": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Integer dayOffset = call.argument("dayOffset");
                if (dayOffset == null) {
                    result.error("INVALID_ARGUMENT", "Day offset is required", null);
                    return;
                }
                SafeResult syncHeartRateSafeResult = new SafeResult(result, "syncHeartRateData");
                dataSyncManager.syncHeartRateData(dayOffset, new DataSyncManager.DataSyncCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        syncHeartRateSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        syncHeartRateSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "syncSleepData": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Integer dayOffset = call.argument("dayOffset");
                if (dayOffset == null) {
                    result.error("INVALID_ARGUMENT", "Day offset is required", null);
                    return;
                }
                SafeResult syncSleepSafeResult = new SafeResult(result, "syncSleepData");
                dataSyncManager.syncSleepData(dayOffset, new DataSyncManager.DataSyncCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        syncSleepSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        syncSleepSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "syncBloodOxygenData": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Integer dayOffset = call.argument("dayOffset");
                if (dayOffset == null) {
                    result.error("INVALID_ARGUMENT", "Day offset is required", null);
                    return;
                }
                SafeResult syncBloodOxygenSafeResult = new SafeResult(result, "syncBloodOxygenData");
                dataSyncManager.syncBloodOxygenData(dayOffset, new DataSyncManager.DataSyncCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        syncBloodOxygenSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        syncBloodOxygenSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "syncBloodPressureData": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Integer dayOffset = call.argument("dayOffset");
                if (dayOffset == null) {
                    result.error("INVALID_ARGUMENT", "Day offset is required", null);
                    return;
                }
                SafeResult syncBloodPressureSafeResult = new SafeResult(result, "syncBloodPressureData");
                dataSyncManager.syncBloodPressureData(dayOffset, new DataSyncManager.DataSyncCallback() {
                    @Override
                    public void onSuccess(Object data) {
                        syncBloodPressureSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        syncBloodPressureSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "startHeartRateMeasurement":
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult startHeartRateSafeResult = new SafeResult(result, "startHeartRateMeasurement");
                measurementManager.startHeartRateMeasurement();
                startHeartRateSafeResult.success(null);
                break;
            case "startBloodPressureMeasurement":
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult startBloodPressureSafeResult = new SafeResult(result, "startBloodPressureMeasurement");
                measurementManager.startBloodPressureMeasurement();
                startBloodPressureSafeResult.success(null);
                break;
            case "startBloodOxygenMeasurement":
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult startBloodOxygenSafeResult = new SafeResult(result, "startBloodOxygenMeasurement");
                measurementManager.startBloodOxygenMeasurement();
                startBloodOxygenSafeResult.success(null);
                break;
            case "startTemperatureMeasurement":
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult startTemperatureSafeResult = new SafeResult(result, "startTemperatureMeasurement");
                measurementManager.startTemperatureMeasurement();
                startTemperatureSafeResult.success(null);
                break;
            case "stopMeasurement":
                measurementManager.stopMeasurement();
                result.success(null);
                break;
            case "setContinuousHeartRate": {
                if (!bleManager.isConnected()) {
                    ExceptionHandler.sendConnectionError(result);
                    return;
                }
                Boolean enable = call.argument("enable");
                Integer intervalMinutes = call.argument("intervalMinutes");
                if (enable == null || intervalMinutes == null) {
                    ExceptionHandler.sendValidationError(result, "enable and intervalMinutes", "boolean and integer");
                    return;
                }
                if (!ValidationUtils.isValidHeartRateInterval(intervalMinutes)) {
                    ExceptionHandler.sendValidationError(result, "intervalMinutes", "10, 15, 20, 30, or 60 minutes");
                    return;
                }
                SafeResult setContinuousHeartRateSafeResult = new SafeResult(result, "setContinuousHeartRate");
                try {
                    settingsManager.setContinuousHeartRate(enable, intervalMinutes, new SettingsManager.SettingsCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> data) {
                            setContinuousHeartRateSafeResult.success(null);
                        }

                        @Override
                        public void onError(String code, String message) {
                            setContinuousHeartRateSafeResult.error(code, message, null);
                        }
                    });
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, setContinuousHeartRateSafeResult, "setContinuousHeartRate");
                }
                break;
            }
            case "getContinuousHeartRateSettings": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult getContinuousHeartRateSafeResult = new SafeResult(result, "getContinuousHeartRateSettings");
                settingsManager.getContinuousHeartRateSettings(new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        getContinuousHeartRateSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        getContinuousHeartRateSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "setContinuousBloodOxygen": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Boolean enable = call.argument("enable");
                Integer intervalMinutes = call.argument("intervalMinutes");
                if (enable == null || intervalMinutes == null) {
                    result.error("INVALID_ARGUMENT", "Enable and intervalMinutes are required", null);
                    return;
                }
                SafeResult setContinuousBloodOxygenSafeResult = new SafeResult(result, "setContinuousBloodOxygen");
                settingsManager.setContinuousBloodOxygen(enable, intervalMinutes, new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        setContinuousBloodOxygenSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        setContinuousBloodOxygenSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "getContinuousBloodOxygenSettings": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult getContinuousBloodOxygenSafeResult = new SafeResult(result, "getContinuousBloodOxygenSettings");
                settingsManager.getContinuousBloodOxygenSettings(new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        getContinuousBloodOxygenSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        getContinuousBloodOxygenSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "setContinuousBloodPressure": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Boolean enable = call.argument("enable");
                if (enable == null) {
                    result.error("INVALID_ARGUMENT", "Enable is required", null);
                    return;
                }
                SafeResult setContinuousBloodPressureSafeResult = new SafeResult(result, "setContinuousBloodPressure");
                settingsManager.setContinuousBloodPressure(enable, new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        setContinuousBloodPressureSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        setContinuousBloodPressureSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "getContinuousBloodPressureSettings": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult getContinuousBloodPressureSafeResult = new SafeResult(result, "getContinuousBloodPressureSettings");
                settingsManager.getContinuousBloodPressureSettings(new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        getContinuousBloodPressureSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        getContinuousBloodPressureSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "setDisplaySettings": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Boolean enabled = call.argument("enabled");
                Boolean leftHand = call.argument("leftHand");
                Integer brightness = call.argument("brightness");
                Integer maxBrightness = call.argument("maxBrightness");
                Boolean doNotDisturb = call.argument("doNotDisturb");
                Integer screenOnStartMinutes = call.argument("screenOnStartMinutes");
                Integer screenOnEndMinutes = call.argument("screenOnEndMinutes");
                
                if (enabled == null || leftHand == null || brightness == null || 
                    maxBrightness == null || doNotDisturb == null || 
                    screenOnStartMinutes == null || screenOnEndMinutes == null) {
                    result.error("INVALID_ARGUMENT", "All display settings parameters are required", null);
                    return;
                }
                
                SafeResult setDisplaySettingsSafeResult = new SafeResult(result, "setDisplaySettings");
                settingsManager.setDisplaySettings(
                    enabled, leftHand, brightness, maxBrightness, doNotDisturb,
                    screenOnStartMinutes, screenOnEndMinutes,
                    new SettingsManager.SettingsCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> data) {
                            setDisplaySettingsSafeResult.success(null);
                        }

                        @Override
                        public void onError(String code, String message) {
                            setDisplaySettingsSafeResult.error(code, message, null);
                        }
                    }
                );
                break;
            }
            case "getDisplaySettings": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult getDisplaySettingsSafeResult = new SafeResult(result, "getDisplaySettings");
                settingsManager.getDisplaySettings(new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        getDisplaySettingsSafeResult.success(data);
                    }

                    @Override
                    public void onError(String code, String message) {
                        getDisplaySettingsSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "setUserInfo": {
                if (!bleManager.isConnected()) {
                    ExceptionHandler.sendConnectionError(result);
                    return;
                }
                Integer age = call.argument("age");
                Integer heightCm = call.argument("heightCm");
                Integer weightKg = call.argument("weightKg");
                Boolean isMale = call.argument("isMale");
                
                if (age == null || heightCm == null || weightKg == null || isMale == null) {
                    ExceptionHandler.sendValidationError(result, "user info parameters", "age, heightCm, weightKg, isMale");
                    return;
                }
                
                if (!ValidationUtils.isValidAge(age)) {
                    ExceptionHandler.sendValidationError(result, "age", "1-150 years");
                    return;
                }
                if (!ValidationUtils.isValidHeight(heightCm)) {
                    ExceptionHandler.sendValidationError(result, "heightCm", "50-300 cm");
                    return;
                }
                if (!ValidationUtils.isValidWeight(weightKg)) {
                    ExceptionHandler.sendValidationError(result, "weightKg", "10-500 kg");
                    return;
                }
                
                SafeResult setUserInfoSafeResult = new SafeResult(result, "setUserInfo");
                try {
                    settingsManager.setUserInfo(age, heightCm, weightKg, isMale, new SettingsManager.SettingsCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> data) {
                            setUserInfoSafeResult.success(null);
                        }

                        @Override
                        public void onError(String code, String message) {
                            setUserInfoSafeResult.error(code, message, null);
                        }
                    });
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, setUserInfoSafeResult, "setUserInfo");
                }
                break;
            }
            case "setUserId": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                String userId = call.argument("userId");
                
                if (userId == null || userId.isEmpty()) {
                    result.error("INVALID_ARGUMENT", "User ID is required", null);
                    return;
                }
                
                SafeResult setUserIdSafeResult = new SafeResult(result, "setUserId");
                settingsManager.setUserId(userId, new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        setUserIdSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        setUserIdSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "factoryReset": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult factoryResetSafeResult = new SafeResult(result, "factoryReset");
                settingsManager.factoryReset(new SettingsManager.SettingsCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        factoryResetSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        factoryResetSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "startExercise": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                Integer exerciseType = call.argument("exerciseType");
                if (exerciseType == null) {
                    result.error("INVALID_ARGUMENT", "Exercise type is required", null);
                    return;
                }
                SafeResult startExerciseSafeResult = new SafeResult(result, "startExercise");
                exerciseManager.startExercise(exerciseType, new ExerciseManager.ExerciseCallback() {
                    @Override
                    public void onSuccess() {
                        startExerciseSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        startExerciseSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "pauseExercise": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult pauseExerciseSafeResult = new SafeResult(result, "pauseExercise");
                exerciseManager.pauseExercise(new ExerciseManager.ExerciseCallback() {
                    @Override
                    public void onSuccess() {
                        pauseExerciseSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        pauseExerciseSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "resumeExercise": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult resumeExerciseSafeResult = new SafeResult(result, "resumeExercise");
                exerciseManager.resumeExercise(new ExerciseManager.ExerciseCallback() {
                    @Override
                    public void onSuccess() {
                        resumeExerciseSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        resumeExerciseSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "stopExercise": {
                if (!bleManager.isConnected()) {
                    result.error("NOT_CONNECTED", "Device is not connected", null);
                    return;
                }
                SafeResult stopExerciseSafeResult = new SafeResult(result, "stopExercise");
                exerciseManager.stopExercise(new ExerciseManager.ExerciseSummaryCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> summary) {
                        stopExerciseSafeResult.success(summary);
                    }

                    @Override
                    public void onError(String code, String message) {
                        stopExerciseSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "validateFirmwareFile": {
                String filePath = call.argument("filePath");
                if (filePath == null || filePath.isEmpty()) {
                    result.error("INVALID_ARGUMENT", "File path is required", null);
                    return;
                }
                SafeResult validateFirmwareSafeResult = new SafeResult(result, "validateFirmwareFile");
                firmwareManager.validateFirmwareFile(filePath, new FirmwareManager.ValidationCallback() {
                    @Override
                    public void onSuccess(boolean isValid) {
                        validateFirmwareSafeResult.success(isValid);
                    }

                    @Override
                    public void onError(String code, String message) {
                        validateFirmwareSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "startFirmwareUpdate": {
                String filePath = call.argument("filePath");
                if (filePath == null || filePath.isEmpty()) {
                    result.error("INVALID_ARGUMENT", "File path is required", null);
                    return;
                }
                if (firmwareManager.isUpdating()) {
                    result.error("UPDATE_IN_PROGRESS", "Firmware update is already in progress", null);
                    return;
                }
                SafeResult startFirmwareUpdateSafeResult = new SafeResult(result, "startFirmwareUpdate");
                firmwareManager.startFirmwareUpdate(filePath, new FirmwareManager.FirmwareUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        startFirmwareUpdateSafeResult.success(null);
                    }

                    @Override
                    public void onError(String code, String message) {
                        startFirmwareUpdateSafeResult.error(code, message, null);
                    }
                });
                break;
            }
            case "checkPermissions": {
                Map<String, Boolean> permissions = permissionManager.checkPermissions();
                result.success(permissions);
                break;
            }
            case "requestPermissions": {
                // Note: This requires Activity context which we don't have in the plugin
                // The Flutter side should use a permission plugin like permission_handler
                // This method returns the list of missing permissions
                List<String> missingPermissions = permissionManager.getMissingPermissions();
                Map<String, Object> response = new HashMap<>();
                response.put("missingPermissions", missingPermissions);
                response.put("canRequest", false); // Cannot request from plugin context
                result.success(response);
                break;
            }
            case "startBackgroundService": {
                String deviceMac = call.argument("deviceMac");
                if (!ValidationUtils.isValidString(deviceMac)) {
                    ExceptionHandler.sendValidationError(result, "deviceMac", "non-empty string");
                    return;
                }
                if (!ValidationUtils.isValidMacAddress(deviceMac)) {
                    ExceptionHandler.sendValidationError(result, "deviceMac", "XX:XX:XX:XX:XX:XX format");
                    return;
                }
                SafeResult startServiceSafeResult = new SafeResult(result, "startBackgroundService");
                try {
                    startBackgroundService(deviceMac);
                    startServiceSafeResult.success(null);
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, startServiceSafeResult, "startBackgroundService");
                }
                break;
            }
            case "stopBackgroundService": {
                SafeResult stopServiceSafeResult = new SafeResult(result, "stopBackgroundService");
                try {
                    stopBackgroundService();
                    stopServiceSafeResult.success(null);
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, stopServiceSafeResult, "stopBackgroundService");
                }
                break;
            }
            case "isServiceRunning": {
                SafeResult isServiceRunningSafeResult = new SafeResult(result, "isServiceRunning");
                try {
                    boolean isRunning = isServiceRunning();
                    isServiceRunningSafeResult.success(isRunning);
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, isServiceRunningSafeResult, "isServiceRunning");
                }
                break;
            }
            case "sendRingCommand": {
                String command = call.argument("command");
                Map<String, Object> params = call.argument("params");
                if (!ValidationUtils.isValidString(command)) {
                    ExceptionHandler.sendValidationError(result, "command", "non-empty string");
                    return;
                }
                if (params == null) {
                    params = new HashMap<>();
                }
                SafeResult sendCommandSafeResult = new SafeResult(result, "sendRingCommand");
                try {
                    sendRingCommand(command, params, sendCommandSafeResult);
                } catch (Exception e) {
                    ExceptionHandler.handleException(e, sendCommandSafeResult, "sendRingCommand");
                }
                break;
            }
            default:
                result.notImplemented();
        }
    }

    /**
     * Start the background service with the specified device MAC address.
     * Requirements: 6.1
     */
    private void startBackgroundService(String deviceMac) {
        Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
        serviceIntent.putExtra(NotificationConfig.EXTRA_DEVICE_MAC, deviceMac);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        
        Log.d(TAG, "Background service start requested for device: " + deviceMac);
    }

    /**
     * Stop the background service.
     * Requirements: 6.2
     */
    private void stopBackgroundService() {
        Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
        context.stopService(serviceIntent);
        
        Log.d(TAG, "Background service stop requested");
    }

    /**
     * Check if the background service is currently running.
     * Requirements: 6.3
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (QRingBackgroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send a command to the background service.
     * Requirements: 6.4
     */
    private void sendRingCommand(String command, Map<String, Object> params, SafeResult result) {
        if (!isServiceRunning()) {
            result.error("SERVICE_NOT_RUNNING", "Background service is not running", null);
            return;
        }
        
        Intent serviceIntent = new Intent(context, QRingBackgroundService.class);
        serviceIntent.putExtra(NotificationConfig.EXTRA_COMMAND, command);
        
        // Convert params map to a format that can be passed via Intent
        // For now, we'll use a simple approach - the service will need to handle this
        HashMap<String, Object> paramsMap = new HashMap<>(params);
        serviceIntent.putExtra(NotificationConfig.EXTRA_COMMAND_PARAMS, paramsMap);
        
        context.startService(serviceIntent);
        
        // For now, return success immediately
        // In a more robust implementation, we'd wait for a response from the service
        result.success(null);
        
        Log.d(TAG, "Command sent to background service: " + command);
    }
    
    /**
     * Convert BleConnectionManager.BleState to a string for Flutter.
     * 
     * @param state The BLE state
     * @return String representation of the state
     */
    private String convertBleStateToString(BleConnectionManager.BleState state) {
        switch (state) {
            case IDLE:
                return "idle";
            case SCANNING:
                return "scanning";
            case CONNECTING:
                return "connecting";
            case PAIRING:
                return "pairing";
            case CONNECTED:
                return "connected";
            case DISCONNECTED:
                return "disconnected";
            case RECONNECTING:
                return "reconnecting";
            case ERROR:
                return "error";
            default:
                return "unknown";
        }
    }
    
    /**
     * Emit BLE connection state change events to Flutter.
     * 
     * Requirements: 8.5, 8.6, 8.7, 8.9
     * 
     * @param oldState The previous state
     * @param newState The new state
     */
    private void emitBleConnectionStateEvent(BleConnectionManager.BleState oldState, BleConnectionManager.BleState newState) {
        if (bleConnectionStateSink == null) {
            Log.d(TAG, "BLE connection state sink is null, skipping event emission");
            return;
        }
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("oldState", convertBleStateToString(oldState));
        eventData.put("newState", convertBleStateToString(newState));
        eventData.put("timestamp", System.currentTimeMillis());
        
        // Add device info if available
        BluetoothDevice currentDevice = bleConnectionManager.getCurrentDevice();
        if (currentDevice != null) {
            eventData.put("deviceMac", currentDevice.getAddress());
            String deviceName = currentDevice.getName();
            if (deviceName != null) {
                eventData.put("deviceName", deviceName);
            }
        }
        
        // Add error details if transitioning to ERROR state
        if (newState == BleConnectionManager.BleState.ERROR) {
            String errorCode = bleConnectionManager.getErrorCode();
            String errorMessage = bleConnectionManager.getErrorMessage();
            if (errorCode != null) {
                eventData.put("errorCode", errorCode);
            }
            if (errorMessage != null) {
                eventData.put("errorMessage", errorMessage);
            }
            
            // Requirement 8.9: Emit error event
            emitBleErrorEvent(errorCode, errorMessage);
        }
        
        // Emit specific events based on state transitions
        // Requirement 8.5: onBleConnected event
        if (newState == BleConnectionManager.BleState.CONNECTED) {
            Log.d(TAG, "Emitting onBleConnected event");
        }
        
        // Requirement 8.6: onBleDisconnected event
        if (newState == BleConnectionManager.BleState.DISCONNECTED) {
            Log.d(TAG, "Emitting onBleDisconnected event");
        }
        
        // Requirement 8.7: onBleReconnecting event
        if (newState == BleConnectionManager.BleState.RECONNECTING) {
            Log.d(TAG, "Emitting onBleReconnecting event");
        }
        
        // Emit the event on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                bleConnectionStateSink.success(eventData);
                Log.d(TAG, String.format("BLE connection state event emitted: %s -> %s", 
                    convertBleStateToString(oldState), convertBleStateToString(newState)));
            } catch (Exception e) {
                Log.e(TAG, "Error emitting BLE connection state event", e);
            }
        });
    }
    
    /**
     * Emit battery update event to Flutter.
     * 
     * Requirement 8.8: onBatteryUpdated event
     * 
     * @param batteryLevel The battery level (0-100)
     */
    private void emitBatteryUpdateEvent(int batteryLevel) {
        if (bleBatterySink == null) {
            Log.d(TAG, "BLE battery sink is null, skipping event emission");
            return;
        }
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("batteryLevel", batteryLevel);
        eventData.put("timestamp", System.currentTimeMillis());
        
        // Add device info if available
        BluetoothDevice currentDevice = bleConnectionManager.getCurrentDevice();
        if (currentDevice != null) {
            eventData.put("deviceMac", currentDevice.getAddress());
        }
        
        // Emit the event on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                bleBatterySink.success(eventData);
                Log.d(TAG, "Battery update event emitted: " + batteryLevel + "%");
            } catch (Exception e) {
                Log.e(TAG, "Error emitting battery update event", e);
            }
        });
    }
    
    /**
     * Emit BLE error event to Flutter.
     * 
     * Requirement 8.9: onBleError event with error details
     * 
     * @param errorCode The error code
     * @param errorMessage The error message
     */
    private void emitBleErrorEvent(String errorCode, String errorMessage) {
        if (bleErrorSink == null) {
            Log.d(TAG, "BLE error sink is null, skipping event emission");
            return;
        }
        
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("errorCode", errorCode != null ? errorCode : "UNKNOWN_ERROR");
        eventData.put("errorMessage", errorMessage != null ? errorMessage : "Unknown error occurred");
        eventData.put("timestamp", System.currentTimeMillis());
        
        // Add device info if available
        BluetoothDevice currentDevice = bleConnectionManager.getCurrentDevice();
        if (currentDevice != null) {
            eventData.put("deviceMac", currentDevice.getAddress());
        }
        
        // Emit the event on the main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                bleErrorSink.success(eventData);
                Log.d(TAG, String.format("BLE error event emitted: [%s] %s", errorCode, errorMessage));
            } catch (Exception e) {
                Log.e(TAG, "Error emitting BLE error event", e);
            }
        });
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        
        // Unregister the service state receiver
        if (serviceStateReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(serviceStateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering service state receiver", e);
            }
        }
        
        // Unregister the broadcast receiver
        if (bleManager != null) {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(bleManager.getConnectionReceiver());
            bleManager.dispose();
        }
        
        // Cleanup BleConnectionManager
        if (bleConnectionManager != null) {
            bleConnectionManager.cleanup();
        }
        
        if (measurementManager != null) {
            measurementManager.dispose();
        }
        if (settingsManager != null) {
            settingsManager.dispose();
        }
        if (notificationManager != null) {
            notificationManager.dispose();
        }
        if (exerciseManager != null) {
            exerciseManager.dispose();
        }
        if (firmwareManager != null) {
            firmwareManager.dispose();
        }
    }
}
