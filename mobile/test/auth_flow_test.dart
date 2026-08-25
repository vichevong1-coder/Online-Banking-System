import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';
import 'package:mobile/features/auth/presentation/phone_entry_screen.dart';
import 'package:mobile/features/auth/presentation/welcome_screen.dart';

void main() {
  testWidgets('WelcomeLandingScreen renders with CTA buttons', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: WelcomeLandingScreen(),
      ),
    );

    expect(find.text('Online Banking'), findsOneWidget);
    expect(find.text('Sign In to Your Account'), findsOneWidget);
    expect(find.text('Open Instant Account'), findsOneWidget);
  });

  testWidgets('WelcomeLandingScreen navigates to LoginScreen', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: WelcomeLandingScreen(),
      ),
    );

    await tester.tap(find.text('Sign In to Your Account'));
    await tester.pumpAndSettle();

    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.text('Welcome Back'), findsOneWidget);
  });

  testWidgets('WelcomeLandingScreen navigates to PhoneEntryScreen', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: WelcomeLandingScreen(),
      ),
    );

    await tester.tap(find.text('Open Instant Account'));
    await tester.pumpAndSettle();

    expect(find.byType(PhoneEntryScreen), findsOneWidget);
    expect(find.text("What's your number?"), findsOneWidget);
  });
}
