import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/account/presentation/home_tab_screen.dart';
import 'package:mobile/features/bill/presentation/bill_payments_tab_screen.dart';
import 'package:mobile/features/card/presentation/cards_tab_screen.dart';
import 'package:mobile/features/dashboard/presentation/main_dashboard_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scan_pay_screen.dart';
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

  testWidgets('MainDashboardScreen renders bottom navigation and tabs', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: MainDashboardScreen(),
      ),
    );

    expect(find.byType(BottomNavigationBar), findsOneWidget);
    expect(find.text('Accounts'), findsOneWidget);
    expect(find.text('Transfer'), findsOneWidget);
    expect(find.text('QR Pay'), findsOneWidget);
    expect(find.text('Bills'), findsOneWidget);
    expect(find.text('Cards'), findsOneWidget);
    expect(find.byType(HomeTabScreen), findsOneWidget);
  });

  testWidgets('MainDashboardScreen switches tabs on bottom bar tap', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: MainDashboardScreen(),
      ),
    );

    // Tap Transfer tab
    await tester.tap(find.text('Transfer'));
    await tester.pumpAndSettle();
    expect(find.byType(TransfersTabScreen), findsOneWidget);

    // Tap QR Pay tab
    await tester.tap(find.text('QR Pay'));
    await tester.pumpAndSettle();
    expect(find.byType(QRScanPayScreen), findsOneWidget);

    // Tap Bills tab
    await tester.tap(find.text('Bills'));
    await tester.pumpAndSettle();
    expect(find.byType(BillPaymentsTabScreen), findsOneWidget);

    // Tap Cards tab
    await tester.tap(find.text('Cards'));
    await tester.pumpAndSettle();
    expect(find.byType(CardsTabScreen), findsOneWidget);
  });
}
