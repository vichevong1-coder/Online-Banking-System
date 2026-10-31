import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';

void main() {
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
}
