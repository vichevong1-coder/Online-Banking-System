import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/home_tab_screen.dart';
import 'package:mobile/features/bill/presentation/bill_payments_tab_screen.dart';
import 'package:mobile/features/card/presentation/cards_tab_screen.dart';
import 'package:mobile/features/dashboard/presentation/main_dashboard_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scan_pay_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scanner_screen.dart';
import 'package:mobile/features/transfer/presentation/transfers_tab_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await SessionManager.instance.init();
    await SessionManager.instance.saveSession(
      accessToken: 'test_token',
      refreshToken: 'test_refresh',
      userId: 'test_user',
      firstName: 'Chan',
      lastName: 'Bopha',
    );
  });

  final sampleAccounts = [
    Account(
      id: 'acc-1',
      accountNumber: '900000000001',
      accountType: AccountType.savings,
      currency: Currency.usd,
      balance: 1500.50,
      createdAt: DateTime(2026, 1, 1),
    ),
    Account(
      id: 'acc-2',
      accountNumber: '900000000002',
      accountType: AccountType.checking,
      currency: Currency.khr,
      balance: 4000000,
      createdAt: DateTime(2026, 1, 2),
    ),
  ];

  testWidgets(
    'MainDashboardScreen renders hub root without BottomNavigationBar',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(home: MainDashboardScreen(initialAccounts: sampleAccounts)),
      );

      // BottomNavigationBar is completely removed in favor of hub-and-spoke navigation
      expect(find.byType(BottomNavigationBar), findsNothing);
      expect(find.byType(HomeTabScreen), findsOneWidget);
    },
  );

  testWidgets(
    'MainDashboardScreen supports hub-and-spoke navigation with back buttons',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(home: MainDashboardScreen(initialAccounts: sampleAccounts)),
      );
      await tester.pump();

      // 1. Navigate to Cards
      await tester.tap(find.byKey(const ValueKey('shortcut_cards')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(CardsTabScreen), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.byType(CardsTabScreen), findsNothing);

      // 2. Navigate to Payments (Bills)
      await tester.tap(find.byKey(const ValueKey('shortcut_payments')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(BillPaymentsTabScreen), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.byType(BillPaymentsTabScreen), findsNothing);

      // 3. Navigate to Transfer
      await tester.tap(find.byKey(const ValueKey('shortcut_transfer')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(TransfersTabScreen), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.byType(TransfersTabScreen), findsNothing);

      // 4. Navigate to QR screen via top right QR button
      await tester.tap(find.byKey(const ValueKey('top_right_qr_button')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(QRScanPayScreen), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.byType(QRScanPayScreen), findsNothing);

      // 5. Navigate directly to camera scanner via Scan shortcut
      await tester.tap(find.byKey(const ValueKey('shortcut_scan')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(QrScannerScreen), findsOneWidget);
      expect(find.byKey(const ValueKey('toggle_khqr_button')), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.byType(QrScannerScreen), findsNothing);
    },
  );
}
