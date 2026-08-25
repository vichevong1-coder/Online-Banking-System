import 'package:flutter/material.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/dashboard/presentation/main_dashboard_screen.dart';

class DashboardScreen extends StatelessWidget {
  final AuthTokenResponse? tokens;

  const DashboardScreen({this.tokens, super.key});

  @override
  Widget build(BuildContext context) {
    if (tokens != null) {
      SessionManager.instance.saveSession(
        accessToken: tokens!.accessToken,
        refreshToken: tokens!.refreshToken,
        userId: tokens!.userId,
        firstName: tokens!.firstName,
        lastName: tokens!.lastName,
      );
    }

    return const MainDashboardScreen();
  }
}
