import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';

/// Shared error state: what went wrong, plus a way to try again.
class AppErrorView extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;

  const AppErrorView({super.key, required this.message, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: AppTheme.primaryRed.withValues(alpha: 0.15),
              ),
              child: const Icon(
                Icons.cloud_off_rounded,
                color: AppTheme.primaryRedLight,
                size: 30,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white70,
                fontSize: 15,
                height: 1.4,
              ),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              OutlinedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh_rounded, size: 18),
                label: const Text('Try again'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppTheme.emeraldLight,
                  side: const BorderSide(color: AppTheme.emeraldLight),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 12,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Shared empty state for lists and tables that have no rows yet.
class AppEmptyView extends StatelessWidget {
  final IconData icon;
  final String title;
  final String? subtitle;

  const AppEmptyView({
    super.key,
    required this.icon,
    required this.title,
    this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: Colors.white24, size: 48),
            const SizedBox(height: 14),
            Text(
              title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 16,
                fontWeight: FontWeight.w600,
              ),
            ),
            if (subtitle != null) ...[
              const SizedBox(height: 6),
              Text(
                subtitle!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Colors.white54,
                  fontSize: 13,
                  height: 1.4,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// A static grey block used while content loads. Deliberately not animated,
/// so it never keeps the frame scheduler busy.
class AppSkeletonBlock extends StatelessWidget {
  final double height;
  final double? width;
  final double radius;

  const AppSkeletonBlock({
    super.key,
    required this.height,
    this.width,
    this.radius = 16,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height,
      width: width,
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
      ),
    );
  }
}
