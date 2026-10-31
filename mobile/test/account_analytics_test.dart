import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/account_analytics_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

class FakeAccountApi extends AccountApi {
  final List<BankTransaction> fakeTransactions;

  FakeAccountApi(this.fakeTransactions);

  @override
  Future<TransactionPage> listTransactions(
    String accountId,
    TransactionQuery query,
  ) async {
    return TransactionPage(
      content: fakeTransactions,
      page: query.page,
      size: query.size,
      totalElements: fakeTransactions.length,
      totalPages: 1,
    );
  }
}

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

  final testAccount = Account(
    id: 'acc-analytics-1',
    accountNumber: '900000000001',
    accountType: AccountType.savings,
    currency: Currency.usd,
    balance: 5200.75,
    createdAt: DateTime(2026, 1, 1),
  );

  final now = DateTime.now();

  final testTransactions = [
    BankTransaction(
      id: 'tx-1',
      type: TransactionType.withdrawal,
      amount: 45.0,
      currency: Currency.usd,
      description: 'Coffee Shop groceries',
      balanceAfter: 5200.75,
      createdAt: now.subtract(const Duration(days: 2)),
    ),
    BankTransaction(
      id: 'tx-2',
      type: TransactionType.withdrawal,
      amount: 120.0,
      currency: Currency.usd,
      description: 'Utility Electricity bill',
      balanceAfter: 5245.75,
      createdAt: now.subtract(const Duration(days: 5)),
    ),
    BankTransaction(
      id: 'tx-3',
      type: TransactionType.deposit,
      amount: 1000.0,
      currency: Currency.usd,
      description: 'Salary payment deposit',
      balanceAfter: 5365.75,
      createdAt: now.subtract(const Duration(days: 10)),
    ),
  ];

  testWidgets(
    'AccountAnalyticsScreen renders analytics header, balance, inflow and outflow',
    (tester) async {
      final fakeApi = FakeAccountApi(testTransactions);

      await tester.pumpWidget(
        MaterialApp(
          home: AccountAnalyticsScreen(
            account: testAccount,
            accountApi: fakeApi,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Spending Analytics'), findsOneWidget);
      expect(find.byType(BackButton), findsOneWidget);
      expect(find.text('SAVINGS ACCOUNT'), findsOneWidget);
      expect(find.textContaining('900000000001'), findsOneWidget);
      expect(find.text('Total Spent'), findsOneWidget);
      expect(find.text('Total Received'), findsOneWidget);
      expect(find.text('Spending by Category'), findsOneWidget);
    },
  );

  testWidgets('AccountAnalyticsScreen toggles time range filters', (
    tester,
  ) async {
    final fakeApi = FakeAccountApi(testTransactions);

    await tester.pumpWidget(
      MaterialApp(
        home: AccountAnalyticsScreen(account: testAccount, accountApi: fakeApi),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('All Time'), findsOneWidget);
    expect(find.text('This Month'), findsOneWidget);
    expect(find.text('Last 30 Days'), findsOneWidget);

    // Tap This Month filter
    await tester.tap(find.text('This Month'));
    await tester.pumpAndSettle();

    expect(find.text('Total Spent'), findsOneWidget);
  });
}
