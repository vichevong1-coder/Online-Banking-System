import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/presentation/login/login_screen.dart';
import 'package:mobile/features/auth/presentation/registration/pending_approval_screen.dart';

void main() {
  group('PendingApprovalScreen', () {
    testWidgets('renders hourglass icon, title, and bank officer review explanation', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(home: PendingApprovalScreen()),
      );

      // Icon & theme styling
      expect(find.byType(GradientScaffold), findsOneWidget);
      expect(find.byIcon(Icons.hourglass_top_rounded), findsOneWidget);

      // Header & explanation
      expect(find.text('Application received'), findsOneWidget);
      expect(
        find.textContaining('A bank officer is checking your ID and selfie'),
        findsOneWidget,
      );
      expect(
        find.textContaining('signing in will tell you it is still pending'),
        findsOneWidget,
      );

      // Action button
      expect(find.text('BACK TO SIGN IN'), findsOneWidget);
    });

    testWidgets('tapping BACK TO SIGN IN navigates to LoginScreen and clears history', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          initialRoute: '/pending',
          routes: {
            '/pending': (_) => const PendingApprovalScreen(),
            '/login': (_) => const LoginScreen(),
          },
        ),
      );

      expect(find.byType(PendingApprovalScreen), findsOneWidget);

      await tester.tap(find.text('BACK TO SIGN IN'));
      await tester.pumpAndSettle();

      expect(find.byType(PendingApprovalScreen), findsNothing);
      expect(find.byType(LoginScreen), findsOneWidget);
    });
  });
}
