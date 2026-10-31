import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';
import 'package:mobile/features/auth/presentation/phone_entry_screen.dart';
import 'package:mobile/features/auth/presentation/welcome_screen.dart';

void main() {
  testWidgets('WelcomeLandingScreen renders with CTA buttons', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeLandingScreen()));

    expect(find.text('Online Banking'), findsOneWidget);
    expect(find.text('Sign In to Your Account'), findsOneWidget);
    expect(find.text('Open Instant Account'), findsOneWidget);
  });

  testWidgets('WelcomeLandingScreen applies ambient brand glow decoration', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeLandingScreen()));

    final scaffoldFinder = find.byType(GradientScaffold);
    expect(scaffoldFinder, findsOneWidget);
    final scaffold = tester.widget<GradientScaffold>(scaffoldFinder);
    expect(scaffold.decoration.gradient, isA<RadialGradient>());
    final radial = scaffold.decoration.gradient as RadialGradient;
    expect(radial.colors.first, const Color(0x2600B27A));
    expect(AppTheme.baseBackground, const Color(0xFF121316));
    expect(AppTheme.surfaceElevated, const Color(0xFF1A1C21));
  });

  testWidgets('WelcomeLandingScreen navigates to LoginScreen', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeLandingScreen()));

    await tester.tap(find.text('Sign In to Your Account'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('Welcome Back'), findsOneWidget);
  });

  testWidgets('WelcomeLandingScreen navigates to PhoneEntryScreen', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeLandingScreen()));

    await tester.tap(find.text('Open Instant Account'));
    await tester.pumpAndSettle();

    expect(find.byType(PhoneEntryScreen), findsOneWidget);
    expect(find.text("What's your number?"), findsOneWidget);
  });
}
