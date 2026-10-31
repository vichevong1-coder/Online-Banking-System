import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/registration_draft.dart';
import 'package:mobile/features/auth/presentation/confirm_details_screen.dart';
import 'package:mobile/features/auth/presentation/nid_scan_screen.dart';

// A valid 1x1 PNG so Image.memory can decode it.
final Uint8List _png = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
);

class _FakeCapturer implements ImageCapturer {
  _FakeCapturer({this.fail = false});

  final bool fail;
  int calls = 0;

  @override
  Future<CapturedImage?> capture({
    required bool selfie,
    bool fromGallery = false,
  }) async {
    calls++;
    if (fail) throw Exception('camera denied');
    return CapturedImage(path: '/fake/$calls.png', bytes: _png);
  }
}

class _FakeReader implements NidTextReader {
  _FakeReader({this.text = '', this.fail = false});

  final String text;
  final bool fail;

  @override
  Future<String> read(String imagePath) async {
    if (fail) throw Exception('ocr failed');
    return text;
  }
}

Future<void> _pump(
  WidgetTester tester, {
  required RegistrationDraft draft,
  required ImageCapturer capturer,
  required NidTextReader reader,
}) async {
  tester.view.physicalSize = const Size(800, 2600);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    MaterialApp(
      home: NidScanScreen(draft: draft, capturer: capturer, reader: reader),
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
  testWidgets('CONTINUE stays disabled until both sides are captured', (
    tester,
  ) async {
    final draft = RegistrationDraft(phone: '+85512000001');
    await _pump(
      tester,
      draft: draft,
      capturer: _FakeCapturer(),
      reader: _FakeReader(),
    );

    expect(_continueEnabled(tester), isFalse);

    await tester.tap(find.text('Take photo').first);
    await tester.pump();
    expect(draft.nidFront, isNotNull);
    expect(_continueEnabled(tester), isFalse);

    await tester.tap(find.text('Take photo'));
    await tester.pump();
    expect(draft.nidBack, isNotNull);
    expect(_continueEnabled(tester), isTrue);
  });

  testWidgets('scanned text prefills the draft and opens the details form', (
    tester,
  ) async {
    final draft = RegistrationDraft(phone: '+85512000001');
    await _pump(
      tester,
      draft: draft,
      capturer: _FakeCapturer(),
      reader: _FakeReader(text: 'CHAN SOKHA\nID 123456789'),
    );

    await tester.tap(find.text('Take photo').first);
    await tester.pump();
    await tester.tap(find.text('Take photo'));
    await tester.pump();
    await tester.tap(find.text('CONTINUE'));
    await tester.pumpAndSettle();

    expect(draft.scanned?.nidNumber, '123456789');
    expect(draft.scanned?.lastName, 'Chan');
    expect(find.byType(ConfirmDetailsScreen), findsOneWidget);
  });

  testWidgets('an OCR failure still lets the customer continue', (
    tester,
  ) async {
    final draft = RegistrationDraft(phone: '+85512000001');
    await _pump(
      tester,
      draft: draft,
      capturer: _FakeCapturer(),
      reader: _FakeReader(fail: true),
    );

    await tester.tap(find.text('Take photo').first);
    await tester.pump();
    await tester.tap(find.text('Take photo'));
    await tester.pump();
    await tester.tap(find.text('CONTINUE'));
    await tester.pumpAndSettle();

    expect(draft.scanned, isNull);
    expect(find.byType(ConfirmDetailsScreen), findsOneWidget);
  });

  testWidgets('a camera failure shows a message that points to the gallery', (
    tester,
  ) async {
    final draft = RegistrationDraft(phone: '+85512000001');
    await _pump(
      tester,
      draft: draft,
      capturer: _FakeCapturer(fail: true),
      reader: _FakeReader(),
    );

    await tester.tap(find.text('Take photo').first);
    await tester.pump();

    expect(find.textContaining('choose a photo from your gallery'), findsOneWidget);
    expect(draft.nidFront, isNull);
  });
}
