import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';
import 'package:mobile/features/auth/presentation/otp_code_input.dart';
import 'package:mobile/features/auth/presentation/two_factor_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await SessionManager.instance.init();
  });

  group('Cambodian Phone Formatting and Normalization', () {
    test(
      'formatCambodianPhone formats standard 9-digit mobile numbers with prefix and gaps',
      () {
        expect(formatCambodianPhone('012000001'), '+855 12 000 001');
        expect(formatCambodianPhone('012 000 001'), '+855 12 000 001');
        expect(formatCambodianPhone('+85512000001'), '+855 12 000 001');
        expect(formatCambodianPhone('+855 12 000 001'), '+855 12 000 001');
      },
    );

    test(
      'formatCambodianPhone formats 8-digit mobile numbers with prefix and gaps',
      () {
        expect(formatCambodianPhone('01234567'), '+855 12 345 67');
        expect(formatCambodianPhone('1234567'), '+855 12 345 67');
        expect(formatCambodianPhone('+855 12 345 67'), '+855 12 345 67');
      },
    );

    test('formatCambodianPhone formats 8-digit landline numbers', () {
      expect(formatCambodianPhone('023123456'), '+855 23 123 456');
    });

    test('formatCambodianPhone handles empty and edge inputs', () {
      expect(formatCambodianPhone(''), '');
      expect(formatCambodianPhone('   '), '');
      expect(formatCambodianPhone('0'), '+855 ');
      expect(formatCambodianPhone('855'), '+855 ');
    });

    test(
      'normalizePhoneToE164 normalizes various inputs to E.164 +855XXXXXXXX',
      () {
        expect(normalizePhoneToE164('012 000 001'), '+85512000001');
        expect(normalizePhoneToE164('+855 12 000 001'), '+85512000001');
        expect(normalizePhoneToE164('+85512000001'), '+85512000001');
        expect(normalizePhoneToE164('012000001'), '+85512000001');
        expect(normalizePhoneToE164('12000001'), '+85512000001');
        expect(normalizePhoneToE164('023 123 456'), '+85523123456');
      },
    );

    test(
      'CambodianPhoneFormatter drops leading 0 typed after +855 and maintains gaps',
      () {
        final formatter = CambodianPhoneFormatter();
        // User types '0' when input is '+855 '
        final result1 = formatter.formatEditUpdate(
          const TextEditingValue(text: '+855 '),
          const TextEditingValue(text: '+855 0'),
        );
        expect(result1.text, '+855 ');

        // User types '12000001'
        final result2 = formatter.formatEditUpdate(
          const TextEditingValue(text: '+855 '),
          const TextEditingValue(text: '+855 12000001'),
        );
        expect(result2.text, '+855 12 000 001');
      },
    );
  });

  group('LoginScreen UI & Phone Input', () {
    testWidgets('LoginScreen initial phone controller contains +855 with gap', (
      tester,
    ) async {
      await tester.pumpWidget(const MaterialApp(home: LoginScreen()));

      final phoneField = find.byWidgetPredicate(
        (w) => w is TextField && w.controller?.text == '+855 ',
      );
      expect(phoneField, findsOneWidget);
    });

    testWidgets(
      'Entering a Cambodian number with leading 0 formats automatically',
      (tester) async {
        await tester.pumpWidget(const MaterialApp(home: LoginScreen()));

        final phoneFinder = find.byType(TextField).first;
        await tester.enterText(phoneFinder, '012 000 001');
        await tester.pump();

        final textField = tester.widget<TextField>(phoneFinder);
        expect(textField.controller?.text, '+855 12 000 001');
      },
    );
  });

  group('OtpCodeInput Widget', () {
    testWidgets('Renders 6 individual digit input boxes', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(body: OtpCodeInput(length: 6, onChanged: (_) {})),
        ),
      );

      // There should be 6 digit boxes (TextFields)
      expect(find.byType(TextField), findsNWidgets(6));
    });

    testWidgets('Entering 6 digits triggers onCompleted with full code', (
      tester,
    ) async {
      String submittedCode = '';

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: OtpCodeInput(
              length: 6,
              onChanged: (_) {},
              onCompleted: (code) => submittedCode = code,
            ),
          ),
        ),
      );

      final textFields = find.byType(TextField);
      for (int i = 0; i < 6; i++) {
        await tester.enterText(textFields.at(i), '${i + 1}');
        await tester.pump();
      }

      expect(submittedCode, '123456');
    });

    testWidgets(
      'Pasting a 6-digit OTP code into any focused box populates all 6 boxes',
      (tester) async {
        String submittedCode = '';

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: OtpCodeInput(
                length: 6,
                onChanged: (_) {},
                onCompleted: (code) => submittedCode = code,
              ),
            ),
          ),
        );

        // Focus on box index 2 and paste '654321'
        final textFields = find.byType(TextField);
        await tester.enterText(textFields.at(2), '654321');
        await tester.pump();

        expect(submittedCode, '654321');
      },
    );

    testWidgets(
      'Entering a digit into an already-filled box replaces the digit and advances focus',
      (tester) async {
        String latestCode = '';

        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: OtpCodeInput(
                length: 6,
                onChanged: (code) => latestCode = code,
              ),
            ),
          ),
        );

        final textFields = find.byType(TextField);
        // Type '5' into box 0
        await tester.enterText(textFields.at(0), '5');
        await tester.pump();
        expect(latestCode, '5');

        // Now enter '8' into box 0 (simulating user tapping box 0 and typing '8' to replace)
        await tester.enterText(textFields.at(0), '58');
        await tester.pump();
        expect(latestCode, '8');
      },
    );
  });

  group('TwoFactorScreen', () {
    testWidgets(
      'Renders 2FA page with OtpCodeInput, countdown timer and action buttons',
      (tester) async {
        await tester.pumpWidget(
          const MaterialApp(
            home: TwoFactorScreen(
              phone: '+855 12 000 001',
              challengeToken: 'test.challenge.token',
            ),
          ),
        );
        await tester.pump();

        expect(find.text('Two-Factor Authentication'), findsOneWidget);
        expect(find.text('Security Verification'), findsOneWidget);
        expect(find.textContaining('+855 12 000 001'), findsOneWidget);
        expect(find.byType(OtpCodeInput), findsOneWidget);
        expect(find.byType(TextField), findsNWidgets(6));
        expect(find.textContaining('Code expires in'), findsOneWidget);
        expect(find.text('VERIFY & SIGN IN'), findsOneWidget);
        expect(find.text('Resend 2FA Code'), findsOneWidget);
      },
    );
  });
}
