import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/qr/presentation/qr_scan_pay_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scanner_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await SessionManager.instance.init();
    await SessionManager.instance.saveSession(
      accessToken: 'test_token',
      refreshToken: 'test_refresh',
      userId: 'test_user',
      firstName: 'VICHOVONG',
      lastName: 'NHAR',
    );
  });

  group('CBA QR Screen (qr_scan_pay_screen.dart)', () {
    testWidgets(
      'renders CBA QR title, subtitle, recipient name, amount 0, and KHQR card elements',
      (tester) async {
        tester.view.physicalSize = const Size(800, 1200);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        await tester.pumpWidget(
          const MaterialApp(home: QRScanPayScreen(initialTabIndex: 0)),
        );
        await tester.pumpAndSettle();

        // Top header and subtitle
        expect(
          find.textContaining('Use this QR code to receive money from others'),
          findsOneWidget,
        );
        expect(find.byType(BackButton), findsOneWidget);
        expect(find.byIcon(Icons.close), findsOneWidget);

        // Camera scanner toggle action in AppBar
        expect(
          find.byKey(const ValueKey('toggle_scanner_button')),
          findsOneWidget,
        );

        // Card elements: Recipient Name and initial Amount 0
        expect(find.text('VICHOVONG NHAR'), findsOneWidget);
        expect(find.text('0'), findsOneWidget);
        expect(find.text('KHQR'), findsOneWidget);

        // + ENTER AMOUNT pill button
        expect(find.text('ENTER AMOUNT'), findsOneWidget);

        // Destination account selectors
        expect(find.text('Receive KHR to:'), findsOneWidget);
        expect(find.text('Receive USD to:'), findsOneWidget);

        // Bottom action buttons
        expect(find.text('Download'), findsOneWidget);
        expect(find.text('Share via'), findsOneWidget);
      },
    );

    testWidgets('tapping Download button triggers card capture and file save', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(home: QRScanPayScreen(initialTabIndex: 0)),
      );
      await tester.pumpAndSettle();

      final downloadButton = find.text('Download');
      expect(downloadButton, findsOneWidget);

      await tester.runAsync(() async {
        await tester.tap(downloadButton);
        await Future.delayed(const Duration(milliseconds: 100));
      });
      await tester.pumpAndSettle();

      expect(find.textContaining('KHQR Card'), findsOneWidget);
    });

    testWidgets(
      'tapping + ENTER AMOUNT opens amount input dialog and updates amount',
      (tester) async {
        tester.view.physicalSize = const Size(800, 1200);
        tester.view.devicePixelRatio = 1.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);

        await tester.pumpWidget(
          const MaterialApp(home: QRScanPayScreen(initialTabIndex: 0)),
        );
        await tester.pumpAndSettle();

        final enterAmt = find.text('ENTER AMOUNT');
        await tester.tap(enterAmt);
        await tester.pumpAndSettle();

        expect(find.text('Enter Receive Amount'), findsOneWidget);
        expect(find.text('USD (\$)'), findsOneWidget);
        expect(find.text('KHR (៛)'), findsOneWidget);

        // Enter amount 25.00
        final textField = find.byType(TextField);
        await tester.enterText(textField, '25.00');
        await tester.tap(find.text('Set Amount'));
        await tester.pumpAndSettle();

        expect(find.text('\$ 25.00'), findsOneWidget);
        expect(find.text('EDIT AMOUNT'), findsOneWidget);
      },
    );
  });

  group('QrScannerScreen', () {
    testWidgets('renders camera scanner with toggle to view KHQR', (
      tester,
    ) async {
      await tester.pumpWidget(const MaterialApp(home: QrScannerScreen()));
      await tester.pump();

      expect(find.text('Scan to pay'), findsOneWidget);
      expect(find.byKey(const ValueKey('toggle_khqr_button')), findsOneWidget);
      expect(
        find.byKey(const ValueKey('scanner_view_khqr_button')),
        findsOneWidget,
      );
      expect(find.text('Type code'), findsOneWidget);
    });
  });
}
