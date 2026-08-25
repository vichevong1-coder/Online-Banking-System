import 'dart:ui';
import 'package:flutter/material.dart';

class AppTheme {
  AppTheme._();

  // Exact Figma Emerald to Forest Teal Gradient
  static const BoxDecoration backgroundGradient = BoxDecoration(
    gradient: LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [
        Color(0xFF00B27A), // Vibrant Emerald Teal (Top)
        Color(0xFF054338), // Medium Forest Teal
        Color(0xFF06221E), // Deep Dark Teal (Bottom)
      ],
      stops: [0.0, 0.45, 1.0],
    ),
  );

  // Modern Dark Surface Gradient for Dashboards
  static const BoxDecoration darkDashboardGradient = BoxDecoration(
    gradient: LinearGradient(
      begin: Alignment.topCenter,
      end: Alignment.bottomCenter,
      colors: [
        Color(0xFF0A2B24),
        Color(0xFF061A16),
        Color(0xFF04100E),
      ],
      stops: [0.0, 0.5, 1.0],
    ),
  );

  static const Color primaryEmerald = Color(0xFF00B27A);
  static const Color emeraldLight = Color(0xFF00FFB2);
  static const Color emeraldAccent = Color(0xFF00E676);
  static const Color primaryRed = Color(0xFFE53935);
  static const Color primaryRedLight = Color(0xFFFF5252);
  static const Color surfaceGlass = Color(0x18FFFFFF);
  static const Color surfaceGlassBorder = Color(0x2EFFFFFF);
  static const Color darkCardBackground = Color(0xBF08211C);
  static const Color textLight = Color(0xFFFFFFFF);
  static const Color textMuted = Color(0xB3FFFFFF);
  static const Color textFaint = Color(0x80FFFFFF);

  static ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: const Color(0xFF06221E),
    fontFamily: 'Roboto',
    colorScheme: const ColorScheme.dark(
      primary: primaryEmerald,
      secondary: emeraldLight,
      surface: Color(0xFF0A2621),
      error: primaryRedLight,
      onPrimary: Colors.white,
      onSurface: Colors.white,
    ),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.transparent,
      elevation: 0,
      centerTitle: false,
      titleTextStyle: TextStyle(
        color: Colors.white,
        fontSize: 18,
        fontWeight: FontWeight.bold,
      ),
      iconTheme: IconThemeData(color: Colors.white),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: const Color(0x1AFFFFFF),
      hintStyle: const TextStyle(color: Color(0x80FFFFFF), fontSize: 14),
      labelStyle: const TextStyle(color: Color(0xB3FFFFFF), fontSize: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Color(0x33FFFFFF)),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Color(0x33FFFFFF)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: emeraldLight, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: primaryRedLight),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        backgroundColor: primaryRed,
        foregroundColor: Colors.white,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
      ),
    ),
  );

  static ThemeData light = darkTheme; // Default to dark green luxury theme
}

// Background Wrapper Widget
class GradientScaffold extends StatelessWidget {
  final PreferredSizeWidget? appBar;
  final Widget body;
  final Widget? bottomNavigationBar;
  final Widget? floatingActionButton;
  final BoxDecoration decoration;

  const GradientScaffold({
    super.key,
    this.appBar,
    required this.body,
    this.bottomNavigationBar,
    this.floatingActionButton,
    this.decoration = AppTheme.backgroundGradient,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: decoration,
      child: Scaffold(
        backgroundColor: Colors.transparent,
        appBar: appBar,
        body: SafeArea(child: body),
        bottomNavigationBar: bottomNavigationBar,
        floatingActionButton: floatingActionButton,
      ),
    );
  }
}

// Custom Glass Container
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final double borderRadius;
  final Color backgroundColor;
  final Color borderColor;
  final VoidCallback? onTap;

  const GlassCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(20),
    this.borderRadius = 20,
    this.backgroundColor = AppTheme.surfaceGlass,
    this.borderColor = AppTheme.surfaceGlassBorder,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Widget card = ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
        child: Container(
          padding: padding,
          decoration: BoxDecoration(
            color: backgroundColor,
            borderRadius: BorderRadius.circular(borderRadius),
            border: Border.all(color: borderColor, width: 1),
          ),
          child: Material(
            color: Colors.transparent,
            child: child,
          ),
        ),
      ),
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(borderRadius),
          onTap: onTap,
          child: card,
        ),
      );
    }
    return card;
  }
}

// Red Gradient Action Button
class PrimaryActionButton extends StatelessWidget {
  final String title;
  final VoidCallback? onPressed;
  final bool isSecondary;
  final bool isLoading;
  final IconData? icon;
  final Color? customColor;

  const PrimaryActionButton({
    super.key,
    required this.title,
    required this.onPressed,
    this.isSecondary = false,
    this.isLoading = false,
    this.icon,
    this.customColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 52,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        gradient: isSecondary || customColor != null
            ? null
            : const LinearGradient(
                colors: [AppTheme.primaryRedLight, AppTheme.primaryRed],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
        color: customColor ?? (isSecondary ? const Color(0x33000000) : null),
        border: isSecondary ? Border.all(color: Colors.white30) : null,
        boxShadow: !isSecondary
            ? [
                BoxShadow(
                  color: (customColor ?? AppTheme.primaryRed).withValues(alpha: 0.3),
                  blurRadius: 10,
                  offset: const Offset(0, 4),
                ),
              ]
            : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: isLoading ? null : onPressed,
          child: Center(
            child: isLoading
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Colors.white,
                    ),
                  )
                : Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      if (icon != null) ...[
                        Icon(icon, color: Colors.white, size: 20),
                        const SizedBox(width: 8),
                      ],
                      Text(
                        title,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ],
                  ),
          ),
        ),
      ),
    );
  }
}
