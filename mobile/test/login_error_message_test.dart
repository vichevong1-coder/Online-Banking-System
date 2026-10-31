import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/presentation/login/login_screen.dart';
import 'package:mobile/features/auth/presentation/login/two_factor_screen.dart';

import 'support/kyc_fakes.dart';

void main() {
  group('loginErrorMessage pure function', () {
    test('a pending account is told its application is under review', () {
      final message = loginErrorMessage(
        ApiException(403, 'ACCOUNT_PENDING_APPROVAL', 'Account is awaiting approval'),
      );
      expect(message, contains('still being reviewed'));
    });

    test('a rejected account is pointed to support', () {
      final message = loginErrorMessage(
        ApiException(403, 'ACCOUNT_REJECTED', 'Account registration was rejected'),
      );
      expect(message, contains('contact support'));
    });

    test('other errors keep the server message', () {
      final message = loginErrorMessage(
        ApiException(401, 'BAD_CREDENTIALS', 'Invalid phone or password'),
      );
      expect(message, 'Invalid phone or password');
    });
  });

  group('LoginScreen pending and rejected account status display', () {
    Future<void> enterCredentialsAndSignIn(WidgetTester tester) async {
      final textFields = find.byType(TextField);
      await tester.enterText(textFields.first, '012 345 678');
      await tester.pump();
      await tester.enterText(textFields.last, 'Password123!');
      await tester.pump();
      await tester.tap(find.text('SIGN IN'));
      await tester.pumpAndSettle();
    }

    testWidgets('shows review explanation when account is pending approval', (
      tester,
    ) async {
      final fakeApi = FakeAuthApi(
        loginError: ApiException(
          403,
          'ACCOUNT_PENDING_APPROVAL',
          'Account is awaiting approval',
        ),
      );

      await tester.pumpWidget(
        MaterialApp(home: LoginScreen(authApi: fakeApi)),
      );
      await enterCredentialsAndSignIn(tester);

      expect(fakeApi.loginCalls, 1);
      expect(
        find.text(
          "Your application is still being reviewed. We'll let you know once your account is approved.",
        ),
        findsOneWidget,
      );
    });

    testWidgets('shows support advice when account is rejected', (
      tester,
    ) async {
      final fakeApi = FakeAuthApi(
        loginError: ApiException(
          403,
          'ACCOUNT_REJECTED',
          'Account registration was rejected',
        ),
      );

      await tester.pumpWidget(
        MaterialApp(home: LoginScreen(authApi: fakeApi)),
      );
      await enterCredentialsAndSignIn(tester);

      expect(fakeApi.loginCalls, 1);
      expect(
        find.text(
          "We couldn't approve your application. Please contact support or visit a branch for help.",
        ),
        findsOneWidget,
      );
    });

    testWidgets('navigates to TwoFactorScreen on successful login', (
      tester,
    ) async {
      final fakeApi = FakeAuthApi();

      await tester.pumpWidget(
        MaterialApp(home: LoginScreen(authApi: fakeApi)),
      );
      await enterCredentialsAndSignIn(tester);

      expect(fakeApi.loginCalls, 1);
      expect(find.byType(TwoFactorScreen), findsOneWidget);
    });
  });
}
