import 'package:flutter/material.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/presentation/login/welcome_screen.dart';
import 'package:mobile/features/dashboard/presentation/main_dashboard_screen.dart';

class ObsApp extends StatelessWidget {
  const ObsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: SessionManager.instance,
      builder: (context, _) {
        final session = SessionManager.instance;

        // Three states, not two: while the stored refresh token is being
        // exchanged there is no access token yet, and showing the welcome
        // screen would flash sign-in at a customer who is already signed in.
        final Widget home = !session.isInitialized
            ? const _SplashScreen()
            : (session.isAuthenticated
                  ? const MainDashboardScreen()
                  : const WelcomeLandingScreen());

        return MaterialApp(
          key: ValueKey(session.isAuthenticated),
          title: 'CBA',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.darkTheme,
          home: home,
        );
      },
    );
  }
}

class _SplashScreen extends StatelessWidget {
  const _SplashScreen();

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(18),
                boxShadow: [
                  BoxShadow(
                    color: AppTheme.primaryEmerald.withValues(alpha: 0.3),
                    blurRadius: 20,
                    spreadRadius: 2,
                  ),
                ],
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(18),
                child: Image.asset(
                  'assets/images/logo.png',
                  fit: BoxFit.cover,
                ),
              ),
            ),
            const SizedBox(height: 24),
            const SizedBox(
              width: 22,
              height: 22,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: AppTheme.emeraldLight,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
