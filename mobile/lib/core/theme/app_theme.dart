import 'package:flutter/material.dart';

class AppTheme {
  AppTheme._();

  // Base background and elevated surface (Warm Matte Graphite - Linear/Stripe style)
  static const Color baseBackground = Color(0xFF121316);
  static const Color surfaceElevated = Color(0xFF1A1C21);
  static const Color surfaceBorder = Color(0x1FFFFFFF);

  // Clean flat dark surface across all screens
  static const BoxDecoration backgroundGradient = BoxDecoration(
    color: baseBackground,
  );

  // Modern Dark Surface for Dashboards
  static const BoxDecoration darkDashboardGradient = BoxDecoration(
    color: baseBackground,
  );

  static const Color primaryEmerald = Color(0xFF00B27A);
  static const Color emeraldLight = Color(0xFF00FFB2);
  static const Color emeraldAccent = Color(0xFF00E676);
  static const Color primaryRed = Color(0xFFE53935);
  static const Color primaryRedLight = Color(0xFFFF5252);
  static const Color surfaceGlass = Color(0xFF1A1C21);
  static const Color surfaceGlassBorder = Color(0x1FFFFFFF);
  static const Color darkCardBackground = Color(0xFF1A1C21);
  static const Color modalBackground = Color(0xFF1A1C21);
  static const Color textLight = Color(0xFFFFFFFF);
  static const Color textMuted = Color(0xB3FFFFFF);
  static const Color textFaint = Color(0x80FFFFFF);

  static ThemeData darkTheme = ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: baseBackground,
    fontFamily: 'Roboto',
    colorScheme: const ColorScheme.dark(
      primary: primaryEmerald,
      secondary: emeraldLight,
      surface: surfaceElevated,
      error: primaryRedLight,
      onPrimary: Colors.white,
      onSurface: Colors.white,
    ),
    canvasColor: surfaceElevated,
    bottomSheetTheme: const BottomSheetThemeData(
      backgroundColor: surfaceElevated,
      modalBackgroundColor: surfaceElevated,
    ),
    popupMenuTheme: PopupMenuThemeData(
      color: surfaceElevated,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: surfaceGlassBorder),
      ),
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
      fillColor: const Color(0x14FFFFFF),
      hintStyle: const TextStyle(color: Color(0x80FFFFFF), fontSize: 14),
      labelStyle: const TextStyle(color: Color(0xB3FFFFFF), fontSize: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: surfaceGlassBorder),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: surfaceGlassBorder),
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
        // Disabled buttons must look disabled, not like a live red button.
        disabledBackgroundColor: const Color(0x33E53935),
        disabledForegroundColor: Colors.white38,
        elevation: 0,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        textStyle: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: emeraldLight,
        textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: const Color(0xFF2A2E36),
      contentTextStyle: const TextStyle(
        color: Colors.white,
        fontSize: 14,
        fontWeight: FontWeight.w600,
      ),
      actionTextColor: emeraldLight,
      insetPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: surfaceElevated,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: const TextStyle(
        color: Colors.white,
        fontSize: 18,
        fontWeight: FontWeight.bold,
      ),
      contentTextStyle: const TextStyle(
        color: textMuted,
        fontSize: 14,
        height: 1.4,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: const BorderSide(color: surfaceGlassBorder),
      ),
    ),
    progressIndicatorTheme: const ProgressIndicatorThemeData(
      color: emeraldLight,
    ),
    dividerTheme: const DividerThemeData(
      color: surfaceGlassBorder,
      space: 1,
      thickness: 1,
    ),
  );

  static ThemeData light = darkTheme; // Default to dark luxury theme
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

// Elevated Dark Card Container (formerly GlassCard)
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
    Widget content = Material(
      color: Colors.transparent,
      child: Padding(padding: padding, child: child),
    );

    if (onTap != null) {
      content = Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(borderRadius),
          onTap: onTap,
          child: Padding(padding: padding, child: child),
        ),
      );
    }

    return Container(
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(borderRadius),
        border: Border.all(color: borderColor, width: 1),
        boxShadow: const [
          BoxShadow(
            color: Color(0x26000000),
            blurRadius: 10,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(borderRadius),
        child: content,
      ),
    );
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
    // A button with no handler should read as unavailable, not as a live CTA.
    final isDisabled = onPressed == null && !isLoading;
    return Opacity(
      opacity: isDisabled ? 0.45 : 1,
      child: Container(
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
                    color: (customColor ?? AppTheme.primaryRed).withValues(
                      alpha: 0.3,
                    ),
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
      ),
    );
  }
}
