import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/registration_draft.dart';
import 'package:mobile/features/auth/presentation/otp_verification_screen.dart';
import 'package:mobile/features/auth/presentation/selfie_screen.dart';

import 'support/kyc_fakes.dart';

RegistrationDraft _draft() =>
    RegistrationDraft(phone: '+85512000001')..details = sampleRegisterRequest();

Future<void> _pump(
  WidgetTester tester, {
  required RegistrationDraft draft,
  required ImageCapturer capturer,
  required FakeAuthApi api,
}) async {
  tester.view.physicalSize = const Size(800, 2600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      home: SelfieScreen(draft: draft, capturer: capturer, authApi: api),
    ),
  );
}

bool _continueEnabled(WidgetTester tester) {
  // PrimaryActionButton is an InkWell; it has no onTap when disabled.
  final button = find.ancestor(
    of: find.text('CONTINUE'),
    matching: find.byType(InkWell),
  );
  return tester.widget<InkWell>(button.first).onTap != null;
}

void main() {
  testWidgets('CONTINUE stays disabled until a selfie is taken', (
    tester,
  ) async {
    final draft = _draft();
    await _pump(
      tester,
      draft: draft,
      capturer: FakeCapturer(),
      api: FakeAuthApi(),
    );

    expect(_continueEnabled(tester), isFalse);

    await tester.tap(find.text('Take selfie'));
    await tester.pump();

    expect(draft.selfie, isNotNull);
    expect(find.text('Retake'), findsOneWidget);
    expect(_continueEnabled(tester), isTrue);
  });

  testWidgets('a camera failure points to the gallery and keeps it disabled', (
    tester,
  ) async {
    final draft = _draft();
    await _pump(
      tester,
      draft: draft,
      capturer: FakeCapturer(fail: true),
      api: FakeAuthApi(),
    );

    await tester.tap(find.text('Take selfie'));
    await tester.pump();

    expect(find.textContaining('choose a photo from your gallery'), findsOneWidget);
    expect(draft.selfie, isNull);
    expect(_continueEnabled(tester), isFalse);
  });

  testWidgets('CONTINUE registers once and opens the OTP screen', (
    tester,
  ) async {
    final draft = _draft();
    final api = FakeAuthApi();
    await _pump(tester, draft: draft, capturer: FakeCapturer(), api: api);

    await tester.tap(find.text('Take selfie'));
    await tester.pump();
    await tester.tap(find.text('CONTINUE'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(api.registerCalls, 1);
    expect(find.byType(OtpVerificationScreen), findsOneWidget);
    // The selfie screen was replaced, so back cannot re-submit registration.
    expect(find.byType(SelfieScreen), findsNothing);

    // Dispose the OTP screen so its resend countdown timer is cancelled.
    await tester.pumpWidget(const SizedBox());
  });

  testWidgets('rejected fields are reported and the customer stays put', (
    tester,
  ) async {
    final draft = _draft();
    final api = FakeAuthApi(
      registerError: ApiException(
        400,
        'VALIDATION_ERROR',
        'Validation failed',
        fieldErrors: {'nidNumber': 'already registered'},
      ),
    );
    await _pump(tester, draft: draft, capturer: FakeCapturer(), api: api);

    await tester.tap(find.text('Take selfie'));
    await tester.pump();
    await tester.tap(find.text('CONTINUE'));
    await tester.pump();

    expect(
      find.textContaining('Some details were rejected (already registered)'),
      findsOneWidget,
    );
    expect(find.byType(SelfieScreen), findsOneWidget);
    expect(find.byType(OtpVerificationScreen), findsNothing);
  });

  testWidgets('a plain API error shows its message', (tester) async {
    final draft = _draft();
    final api = FakeAuthApi(
      registerError: ApiException(409, 'PHONE_TAKEN', 'Phone already in use'),
    );
    await _pump(tester, draft: draft, capturer: FakeCapturer(), api: api);

    await tester.tap(find.text('Take selfie'));
    await tester.pump();
    await tester.tap(find.text('CONTINUE'));
    await tester.pump();

    expect(find.text('Phone already in use'), findsOneWidget);
  });
}
