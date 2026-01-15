import 'package:flutter/material.dart';

/// Helper class for responsive design
class ResponsiveHelper {
  /// Check if the screen is small (phone)
  static bool isSmallScreen(BuildContext context) {
    return MediaQuery.of(context).size.width < 600;
  }

  /// Check if the screen is medium (tablet)
  static bool isMediumScreen(BuildContext context) {
    final width = MediaQuery.of(context).size.width;
    return width >= 600 && width < 1200;
  }

  /// Check if the screen is large (desktop)
  static bool isLargeScreen(BuildContext context) {
    return MediaQuery.of(context).size.width >= 1200;
  }

  /// Get responsive padding
  static EdgeInsets getResponsivePadding(BuildContext context) {
    if (isSmallScreen(context)) {
      return const EdgeInsets.all(16.0);
    } else if (isMediumScreen(context)) {
      return const EdgeInsets.all(24.0);
    } else {
      return const EdgeInsets.all(32.0);
    }
  }

  /// Get responsive grid cross axis count
  static int getGridCrossAxisCount(BuildContext context) {
    if (isSmallScreen(context)) {
      return 2;
    } else if (isMediumScreen(context)) {
      return 3;
    } else {
      return 4;
    }
  }

  /// Get responsive font size multiplier
  static double getFontSizeMultiplier(BuildContext context) {
    if (isSmallScreen(context)) {
      return 1.0;
    } else if (isMediumScreen(context)) {
      return 1.1;
    } else {
      return 1.2;
    }
  }
}
