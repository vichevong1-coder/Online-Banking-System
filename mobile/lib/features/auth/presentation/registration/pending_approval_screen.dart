import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/presentation/login/login_screen.dart';

/// Shown once the ID and selfie are uploaded. The account cannot sign in until
/// a bank officer approves it, so this is the end of the signup flow.
class PendingApprovalScreen extends StatelessWidget {
  const PendingApprovalScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(
                Icons.hourglass_top_rounded,
                size: 56,
                color: AppTheme.emeraldLight,
              ),
              const SizedBox(height: 24),
              const Text(
                'Application received',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'A bank officer is checking your ID and selfie. Once your '
                'account is approved you can sign in with your phone number '
                'and password. Until then, signing in will tell you it is '
                'still pending.',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.white70,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 36),
              PrimaryActionButton(
                title: 'BACK TO SIGN IN',
                onPressed: () => Navigator.pushAndRemoveUntil(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                  (route) => false,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
