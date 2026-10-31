import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/registration_draft.dart';
import 'package:mobile/features/auth/presentation/login/login_screen.dart';
import 'package:mobile/features/auth/presentation/registration/otp_verification_screen.dart';
import 'package:mobile/features/auth/presentation/registration/pending_approval_screen.dart';

import 'support/kyc_fakes.dart';

RegistrationDraft _draftWithPhotos() {
  final image = CapturedImage(path: '/fake.png', bytes: tinyPng);
  return RegistrationDraft(phone: '+85512000001')
    ..details = sampleRegisterRequest()
    ..nidFront = image
    ..nidBack = image
    ..selfie = image;
}

Future<void> _pump(
  WidgetTester tester, {
  required RegistrationDraft draft,
  required FakeAuthApi api,
}) async {
  tester.view.physicalSize = const Size(800, 2600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      home: OtpVerificationScreen(draft: draft, authApi: api),
    ),
  );
}

/// Pasting six digits into the first box fills every box and submits.
Future<void> _enterCode(WidgetTester tester) async {
  await tester.enterText(find.byType(TextField).first, '123456');
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 500));
}

void main() {
  testWidgets('verifies the code, uploads all three photos, then waits', (
    tester,
  ) async {
    final api = FakeAuthApi();
    await _pump(tester, draft: _draftWithPhotos(), api: api);

    await _enterCode(tester);

    expect(api.verifyCalls, 1);
    expect(api.uploads.map((u) => u.type), [
      KycDocumentType.nidFront,
      KycDocumentType.nidBack,
      KycDocumentType.selfie,
    ]);
    expect(api.uploads.every((u) => u.token == 'upload-token'), isTrue);
    expect(find.byType(PendingApprovalScreen), findsOneWidget);
    expect(find.byType(OtpVerificationScreen), findsNothing);
  });

  testWidgets('a failed upload offers a retry that reuses the same token', (
    tester,
  ) async {
    final api = FakeAuthApi(uploadFailures: 1);
    await _pump(tester, draft: _draftWithPhotos(), api: api);

    await _enterCode(tester);

    expect(find.text("Couldn't upload your photos. Please retry."), findsOneWidget);
    expect(find.text('RETRY UPLOAD'), findsOneWidget);
    expect(find.byType(PendingApprovalScreen), findsNothing);

    await tester.tap(find.text('RETRY UPLOAD'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    // The single-use code was not submitted a second time.
    expect(api.verifyCalls, 1);
    expect(api.uploads.length, 3);
    expect(find.byType(PendingApprovalScreen), findsOneWidget);
  });

  testWidgets('with nothing to approve, the customer goes to sign in', (
    tester,
  ) async {
    final api = FakeAuthApi(uploadToken: null);
    await _pump(tester, draft: _draftWithPhotos(), api: api);

    await _enterCode(tester);

    expect(api.uploads, isEmpty);
    expect(find.byType(LoginScreen), findsOneWidget);
  });

  testWidgets('VERIFY stays disabled until six digits are entered', (
    tester,
  ) async {
    final api = FakeAuthApi();
    await _pump(tester, draft: _draftWithPhotos(), api: api);

    final button = find.ancestor(
      of: find.text('VERIFY'),
      matching: find.byType(InkWell),
    );
    expect(tester.widget<InkWell>(button.first).onTap, isNull);

    // Dispose the screen so its resend countdown timer is cancelled.
    await tester.pumpWidget(const SizedBox());
  });
}
