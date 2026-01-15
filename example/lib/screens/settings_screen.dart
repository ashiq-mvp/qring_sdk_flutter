import 'package:flutter/material.dart';
import 'package:qring_sdk_flutter/qring_sdk_flutter.dart' as qring;

import '../widgets/error_card.dart';
import '../widgets/success_snackbar.dart';

/// Screen for device and monitoring settings
///
/// Provides configuration for:
/// - Continuous monitoring (HR, SpO2, BP)
/// - Display settings (brightness, orientation, DND)
/// - User profile (age, height, weight, gender)
/// - Factory reset
class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  bool _isLoading = true;
  String? _errorMessage;

  // Continuous monitoring settings
  bool _hrEnabled = false;
  int _hrInterval = 30;
  bool _spo2Enabled = false;
  int _spo2Interval = 30;
  bool _bpEnabled = false;

  // Display settings
  bool _displayEnabled = true;
  bool _leftHand = false;
  double _brightness = 3;
  int _maxBrightness = 5;
  bool _doNotDisturb = false;
  TimeOfDay _screenOnStart = const TimeOfDay(hour: 0, minute: 0);
  TimeOfDay _screenOnEnd = const TimeOfDay(hour: 23, minute: 59);

  // User profile
  final _ageController = TextEditingController();
  final _heightController = TextEditingController();
  final _weightController = TextEditingController();
  final _userIdController = TextEditingController();
  bool _isMale = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  @override
  void dispose() {
    _ageController.dispose();
    _heightController.dispose();
    _weightController.dispose();
    _userIdController.dispose();
    super.dispose();
  }

  /// Load all settings from the device on screen init
  Future<void> _loadSettings() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      // Load continuous monitoring settings
      final hrSettings =
          await qring.QringSettings.getContinuousHeartRateSettings();
      final spo2Settings =
          await qring.QringSettings.getContinuousBloodOxygenSettings();
      final bpSettings =
          await qring.QringSettings.getContinuousBloodPressureSettings();

      // Load display settings
      final displaySettings = await qring.QringSettings.getDisplaySettings();

      if (mounted) {
        setState(() {
          // Continuous monitoring
          _hrEnabled = hrSettings.enabled;
          _hrInterval = hrSettings.intervalMinutes;
          _spo2Enabled = spo2Settings.enabled;
          _spo2Interval = spo2Settings.intervalMinutes;
          _bpEnabled = bpSettings.enabled;

          // Display settings
          _displayEnabled = displaySettings.enabled;
          _leftHand = displaySettings.leftHand;
          _brightness = displaySettings.brightness.toDouble();
          _maxBrightness = displaySettings.maxBrightness;
          _doNotDisturb = displaySettings.doNotDisturb;
          _screenOnStart = TimeOfDay(
            hour: displaySettings.screenOnStartMinutes ~/ 60,
            minute: displaySettings.screenOnStartMinutes % 60,
          );
          _screenOnEnd = TimeOfDay(
            hour: displaySettings.screenOnEndMinutes ~/ 60,
            minute: displaySettings.screenOnEndMinutes % 60,
          );

          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = 'Failed to load settings: $e';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _saveContinuousHeartRate() async {
    try {
      await qring.QringSettings.setContinuousHeartRate(
        enable: _hrEnabled,
        intervalMinutes: _hrInterval,
      );
      if (mounted) {
        showSuccessSnackbar(context, 'Heart rate settings saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _saveContinuousBloodOxygen() async {
    try {
      await qring.QringSettings.setContinuousBloodOxygen(
        enable: _spo2Enabled,
        intervalMinutes: _spo2Interval,
      );
      if (mounted) {
        showSuccessSnackbar(context, 'Blood oxygen settings saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _saveContinuousBloodPressure() async {
    try {
      await qring.QringSettings.setContinuousBloodPressure(enable: _bpEnabled);
      if (mounted) {
        showSuccessSnackbar(context, 'Blood pressure settings saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _saveDisplaySettings() async {
    try {
      final settings = qring.DisplaySettings(
        enabled: _displayEnabled,
        leftHand: _leftHand,
        brightness: _brightness.toInt(),
        maxBrightness: _maxBrightness,
        doNotDisturb: _doNotDisturb,
        screenOnStartMinutes: _screenOnStart.hour * 60 + _screenOnStart.minute,
        screenOnEndMinutes: _screenOnEnd.hour * 60 + _screenOnEnd.minute,
      );
      await qring.QringSettings.setDisplaySettings(settings);
      if (mounted) {
        showSuccessSnackbar(context, 'Display settings saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _saveUserInfo() async {
    try {
      final age = int.tryParse(_ageController.text);
      final height = int.tryParse(_heightController.text);
      final weight = int.tryParse(_weightController.text);

      if (age == null || height == null || weight == null) {
        throw Exception(
          'Please enter valid numbers for age, height, and weight',
        );
      }

      final userInfo = qring.UserInfo(
        age: age,
        heightCm: height,
        weightKg: weight,
        isMale: _isMale,
      );
      await qring.QringSettings.setUserInfo(userInfo);
      if (mounted) {
        showSuccessSnackbar(context, 'User info saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _saveUserId() async {
    try {
      if (_userIdController.text.isEmpty) {
        throw Exception('User ID cannot be empty');
      }
      await qring.QringSettings.setUserId(_userIdController.text);
      if (mounted) {
        showSuccessSnackbar(context, 'User ID saved');
      }
    } catch (e) {
      if (mounted) {
        showErrorSnackbar(context, 'Failed to save: $e');
      }
    }
  }

  Future<void> _performFactoryReset() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Factory Reset'),
        content: const Text(
          'This will reset all device settings to factory defaults. '
          'This action cannot be undone. Continue?',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Reset'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        await qring.QringSdkFlutter.factoryReset();
        if (mounted) {
          showSuccessSnackbar(context, 'Factory reset completed');
          // Reload settings after reset
          _loadSettings();
        }
      } catch (e) {
        if (mounted) {
          showErrorSnackbar(context, 'Failed to reset: $e');
        }
      }
    }
  }

  Future<void> _selectTime(BuildContext context, bool isStart) async {
    final initialTime = isStart ? _screenOnStart : _screenOnEnd;
    final time = await showTimePicker(
      context: context,
      initialTime: initialTime,
    );

    if (time != null && mounted) {
      setState(() {
        if (isStart) {
          _screenOnStart = time;
        } else {
          _screenOnEnd = time;
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: ErrorCard(message: _errorMessage!, onRetry: _loadSettings),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildContinuousMonitoringSection(),
        const SizedBox(height: 24),
        _buildDisplaySettingsSection(),
        const SizedBox(height: 24),
        _buildUserProfileSection(),
      ],
    );
  }

  Widget _buildContinuousMonitoringSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Continuous Monitoring',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16),
            // Heart Rate
            SwitchListTile(
              title: const Text('Continuous Heart Rate'),
              subtitle: Text(
                _hrEnabled ? 'Every $_hrInterval minutes' : 'Disabled',
              ),
              value: _hrEnabled,
              onChanged: (value) {
                setState(() => _hrEnabled = value);
                _saveContinuousHeartRate();
              },
            ),
            if (_hrEnabled) ...[
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Interval (minutes)'),
                    SegmentedButton<int>(
                      segments: const [
                        ButtonSegment(value: 10, label: Text('10')),
                        ButtonSegment(value: 15, label: Text('15')),
                        ButtonSegment(value: 20, label: Text('20')),
                        ButtonSegment(value: 30, label: Text('30')),
                        ButtonSegment(value: 60, label: Text('60')),
                      ],
                      selected: {_hrInterval},
                      onSelectionChanged: (Set<int> selected) {
                        setState(() => _hrInterval = selected.first);
                        _saveContinuousHeartRate();
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
            ],
            const Divider(),
            // Blood Oxygen
            SwitchListTile(
              title: const Text('Continuous Blood Oxygen'),
              subtitle: Text(
                _spo2Enabled ? 'Every $_spo2Interval minutes' : 'Disabled',
              ),
              value: _spo2Enabled,
              onChanged: (value) {
                setState(() => _spo2Enabled = value);
                _saveContinuousBloodOxygen();
              },
            ),
            if (_spo2Enabled) ...[
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Interval: $_spo2Interval minutes'),
                    Slider(
                      value: _spo2Interval.toDouble(),
                      min: 10,
                      max: 60,
                      divisions: 10,
                      label: '$_spo2Interval min',
                      onChanged: (value) {
                        setState(() => _spo2Interval = value.toInt());
                      },
                      onChangeEnd: (value) {
                        _saveContinuousBloodOxygen();
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
            ],
            const Divider(),
            // Blood Pressure
            SwitchListTile(
              title: const Text('Continuous Blood Pressure'),
              subtitle: Text(_bpEnabled ? 'Enabled' : 'Disabled'),
              value: _bpEnabled,
              onChanged: (value) {
                setState(() => _bpEnabled = value);
                _saveContinuousBloodPressure();
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDisplaySettingsSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Display Settings',
              style: Theme.of(context).textTheme.titleLarge,
            ),
            const SizedBox(height: 16),
            SwitchListTile(
              title: const Text('Display Enabled'),
              value: _displayEnabled,
              onChanged: (value) {
                setState(() => _displayEnabled = value);
                _saveDisplaySettings();
              },
            ),
            const Divider(),
            SwitchListTile(
              title: const Text('Left Hand'),
              subtitle: Text(
                _leftHand ? 'Worn on left hand' : 'Worn on right hand',
              ),
              value: _leftHand,
              onChanged: (value) {
                setState(() => _leftHand = value);
                _saveDisplaySettings();
              },
            ),
            const Divider(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Brightness: ${_brightness.toInt()}'),
                  Slider(
                    value: _brightness,
                    min: 1,
                    max: _maxBrightness.toDouble(),
                    divisions: _maxBrightness - 1,
                    label: _brightness.toInt().toString(),
                    onChanged: (value) {
                      setState(() => _brightness = value);
                    },
                    onChangeEnd: (value) {
                      _saveDisplaySettings();
                    },
                  ),
                ],
              ),
            ),
            const Divider(),
            SwitchListTile(
              title: const Text('Do Not Disturb'),
              value: _doNotDisturb,
              onChanged: (value) {
                setState(() => _doNotDisturb = value);
                _saveDisplaySettings();
              },
            ),
            const Divider(),
            ListTile(
              title: const Text('Screen On Start Time'),
              subtitle: Text(_screenOnStart.format(context)),
              trailing: const Icon(Icons.access_time),
              onTap: () => _selectTime(context, true),
            ),
            ListTile(
              title: const Text('Screen On End Time'),
              subtitle: Text(_screenOnEnd.format(context)),
              trailing: const Icon(Icons.access_time),
              onTap: () => _selectTime(context, false),
            ),
            const SizedBox(height: 8),
            Center(
              child: ElevatedButton(
                onPressed: _saveDisplaySettings,
                child: const Text('Save Display Settings'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildUserProfileSection() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('User Profile', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            TextField(
              controller: _ageController,
              decoration: const InputDecoration(
                labelText: 'Age',
                border: OutlineInputBorder(),
                suffixText: 'years',
              ),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _heightController,
              decoration: const InputDecoration(
                labelText: 'Height',
                border: OutlineInputBorder(),
                suffixText: 'cm',
              ),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _weightController,
              decoration: const InputDecoration(
                labelText: 'Weight',
                border: OutlineInputBorder(),
                suffixText: 'kg',
              ),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            SegmentedButton<bool>(
              segments: const [
                ButtonSegment(
                  value: true,
                  label: Text('Male'),
                  icon: Icon(Icons.male),
                ),
                ButtonSegment(
                  value: false,
                  label: Text('Female'),
                  icon: Icon(Icons.female),
                ),
              ],
              selected: {_isMale},
              onSelectionChanged: (Set<bool> selected) {
                setState(() => _isMale = selected.first);
              },
            ),
            const SizedBox(height: 16),
            Center(
              child: ElevatedButton(
                onPressed: _saveUserInfo,
                child: const Text('Save User Info'),
              ),
            ),
            const Divider(height: 32),
            TextField(
              controller: _userIdController,
              decoration: const InputDecoration(
                labelText: 'User ID',
                border: OutlineInputBorder(),
                hintText: 'Enter unique user identifier',
              ),
            ),
            const SizedBox(height: 16),
            Center(
              child: ElevatedButton(
                onPressed: _saveUserId,
                child: const Text('Save User ID'),
              ),
            ),
            const Divider(height: 32),
            Center(
              child: ElevatedButton(
                onPressed: _performFactoryReset,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
                child: const Text('Factory Reset'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
