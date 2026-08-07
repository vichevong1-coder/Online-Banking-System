import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/account/presentation/statement_viewer_screen.dart';
import 'package:mobile/features/account/presentation/transaction_history_screen.dart';

// US-015/016: dedicated /balance fetch (rather than trusting the list's snapshot) is what makes
// this "real-time" — pull-to-refresh re-hits the endpoint every time.
class AccountDetailScreen extends StatefulWidget {
  const AccountDetailScreen({required this.accountApi, required this.account, super.key});

  final AccountApi accountApi;
  final Account account;

  @override
  State<AccountDetailScreen> createState() => _AccountDetailScreenState();
}

class _AccountDetailScreenState extends State<AccountDetailScreen> {
  Future<Balance>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() => _future = widget.accountApi.getBalance(widget.account.id));
  }

  @override
  Widget build(BuildContext context) {
    final account = widget.account;
    return Scaffold(
      appBar: AppBar(title: Text('${account.accountType.label} · ${account.currency.toJson()}')),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: () async => _load(),
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Text(account.accountNumber, style: Theme.of(context).textTheme.bodyMedium),
              const SizedBox(height: 16),
              FutureBuilder<Balance>(
                future: _future,
                builder: (context, snapshot) {
                  if (snapshot.connectionState != ConnectionState.done) {
                    return const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Center(child: CircularProgressIndicator()),
                    );
                  }
                  final error = snapshot.error;
                  if (error != null) {
                    return Text(
                      error is ApiException ? error.message : 'Could not load balance.',
                      style: TextStyle(color: Theme.of(context).colorScheme.error),
                    );
                  }
                  final balance = snapshot.data!;
                  return Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Current balance', style: Theme.of(context).textTheme.labelLarge),
                          const SizedBox(height: 8),
                          Text(
                            formatMoney(balance.balance, balance.currency),
                            style: Theme.of(context).textTheme.headlineMedium,
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
              const SizedBox(height: 24),
              ListTile(
                leading: const Icon(Icons.receipt_long),
                title: const Text('Transaction history'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => TransactionHistoryScreen(accountApi: widget.accountApi, account: account),
                  ),
                ),
              ),
              ListTile(
                leading: const Icon(Icons.picture_as_pdf),
                title: const Text('Statement'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => StatementViewerScreen(accountApi: widget.accountApi, account: account),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
