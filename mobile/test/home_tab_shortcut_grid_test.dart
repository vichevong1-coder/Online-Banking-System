import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/home_tab_screen.dart';
import 'package:mobile/features/card/presentation/cards_tab_screen.dart';
import 'package:mobile/features/transfer/presentation/beneficiary_management_screen.dart';
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

  testWidgets('HomeTabScreen renders top-right QR button and 6 shortcuts', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: HomeTabScreen(initialAccounts: sampleAccounts)),
      ),
    );
    await tester.pump();

    // Verify top-right QR button
    expect(find.byKey(const ValueKey('top_right_qr_button')), findsOneWidget);

    // Verify top-right QR button is at the far right of header (to the right of Notifications)
    final qrOffset = tester.getTopRight(
      find.byKey(const ValueKey('top_right_qr_button')),
    );
    final notifOffset = tester.getTopRight(find.byTooltip('Notifications'));
    expect(qrOffset.dx, greaterThan(notifOffset.dx));

    // Verify all 6 shortcut items
    expect(find.byKey(const ValueKey('shortcut_accounts')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_cards')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_payments')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_scan')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_favorites')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_transfer')), findsOneWidget);

    // Verify labels
    expect(find.text('Accounts'), findsWidgets);
    expect(find.text('Cards'), findsWidgets);
    expect(find.text('Payments'), findsWidgets);
    expect(find.text('Scan'), findsWidgets);
    expect(find.text('Favorites'), findsWidgets);
    expect(find.text('Transfer'), findsWidgets);
  });

  testWidgets('Top-right QR button triggers onNavigateTab(2, 0)', (
    tester,
  ) async {
    int? navigatedTab;
    int? navigatedSubTab;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeTabScreen(
            initialAccounts: sampleAccounts,
            onNavigateTab: (tab, [subTab]) {
              navigatedTab = tab;
              navigatedSubTab = subTab;
            },
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('top_right_qr_button')));
    await tester.pump();

    expect(navigatedTab, 2);
    expect(navigatedSubTab, 0);
  });

  testWidgets(
    'Accounts shortcut opens accounts bottom sheet and allows selecting account',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(body: HomeTabScreen(initialAccounts: sampleAccounts)),
        ),
      );
      await tester.pump();

      // Tap Accounts shortcut
      await tester.tap(find.byKey(const ValueKey('shortcut_accounts')));
      await tester.pumpAndSettle();

      // Verify modal bottom sheet is shown
      expect(find.text('Your Accounts'), findsOneWidget);
      expect(find.text('900000000001'), findsWidgets);
      expect(find.text('900000000002'), findsOneWidget);
      expect(find.text('OPEN ANOTHER ACCOUNT'), findsOneWidget);

      // Tap second account
      await tester.tap(find.text('900000000002'));
      await tester.pumpAndSettle();

      // Sheet should be dismissed
      expect(find.text('Your Accounts'), findsNothing);
    },
  );

  testWidgets('Cards shortcut navigates to tab 4', (tester) async {
    int? navigatedTab;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeTabScreen(
            initialAccounts: sampleAccounts,
            onNavigateTab: (tab, [subTab]) => navigatedTab = tab,
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('shortcut_cards')));
    await tester.pump();

    expect(navigatedTab, 4);
  });

  testWidgets('Payments shortcut navigates to tab 3', (tester) async {
    int? navigatedTab;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeTabScreen(
            initialAccounts: sampleAccounts,
            onNavigateTab: (tab, [subTab]) => navigatedTab = tab,
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('shortcut_payments')));
    await tester.pump();

    expect(navigatedTab, 3);
  });

  testWidgets('Scan shortcut navigates to tab 2, subTab 1 (Scan & Pay)', (
    tester,
  ) async {
    int? navigatedTab;
    int? navigatedSubTab;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeTabScreen(
            initialAccounts: sampleAccounts,
            onNavigateTab: (tab, [subTab]) {
              navigatedTab = tab;
              navigatedSubTab = subTab;
            },
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('shortcut_scan')));
    await tester.pump();

    expect(navigatedTab, 2);
    expect(navigatedSubTab, 1);
  });

  testWidgets('Favorites shortcut navigates to BeneficiaryManagementScreen', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: HomeTabScreen(initialAccounts: sampleAccounts)),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('shortcut_favorites')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));

    expect(find.byType(BeneficiaryManagementScreen), findsOneWidget);
  });

  testWidgets('Transfer shortcut navigates to tab 1', (tester) async {
    int? navigatedTab;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: HomeTabScreen(
            initialAccounts: sampleAccounts,
            onNavigateTab: (tab, [subTab]) => navigatedTab = tab,
          ),
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const ValueKey('shortcut_transfer')));
    await tester.pump();

    expect(navigatedTab, 1);
  });

  testWidgets('Empty accounts list displays empty state in bottom sheet', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: HomeTabScreen(initialAccounts: [])),
      ),
    );
    await tester.pump();

    // Verify empty state on card
    expect(find.text('No Active Accounts Found'), findsOneWidget);

    // Tap Accounts shortcut
    await tester.tap(find.byKey(const ValueKey('shortcut_accounts')));
    await tester.pumpAndSettle();

    // Verify empty state inside bottom sheet
    expect(find.text('No accounts found'), findsOneWidget);
    expect(find.text('OPEN AN ACCOUNT'), findsWidgets);
  });

  testWidgets(
    'Standalone HomeTabScreen pushes screens when onNavigateTab is null',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(body: HomeTabScreen(initialAccounts: sampleAccounts)),
        ),
      );
      await tester.pump();

      // Test top-right QR fallback navigation
      await tester.tap(find.byKey(const ValueKey('top_right_qr_button')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.text('Receive (My QR)'), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      // Pop back using the real BackButton
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      expect(find.text('Receive (My QR)'), findsNothing);

      // Test Cards shortcut fallback navigation
      await tester.tap(find.byKey(const ValueKey('shortcut_cards')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.byType(CardsTabScreen), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      // Pop back
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();

      // Test Payments shortcut fallback navigation
      await tester.tap(find.byKey(const ValueKey('shortcut_payments')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.text('Bill Payments'), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      // Pop back
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();

      // Test Scan shortcut fallback navigation
      await tester.tap(find.byKey(const ValueKey('shortcut_scan')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.text('Scan to pay'), findsWidgets);
      expect(find.byKey(const ValueKey('toggle_khqr_button')), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);

      // Pop back
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();

      // Test Transfer shortcut fallback navigation
      await tester.tap(find.byKey(const ValueKey('shortcut_transfer')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 300));
      expect(find.text('Transfer'), findsWidgets);
      expect(find.byType(BackButton), findsOneWidget);

      // Pop back
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
    },
  );

  testWidgets('Shortcut grid handles narrow screen without overflow', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(300, 600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: HomeTabScreen(initialAccounts: sampleAccounts)),
      ),
    );
    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(find.byKey(const ValueKey('shortcut_accounts')), findsOneWidget);
    expect(find.byKey(const ValueKey('shortcut_transfer')), findsOneWidget);
  });
}
