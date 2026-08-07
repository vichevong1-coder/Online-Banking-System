import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/account_detail_screen.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/account/presentation/open_account_screen.dart';

// US-013: view linked accounts. US-014's "open new account" is reached from here too, since
// that's the natural entry point once a customer is looking at their account list.
class AccountListScreen extends StatefulWidget {
  const AccountListScreen({required this.accountApi, super.key});

  final AccountApi accountApi;

  @override
  State<AccountListScreen> createState() => _AccountListScreenState();
}

class _AccountListScreenState extends State<AccountListScreen> {
  Future<List<Account>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() => _future = widget.accountApi.listAccounts());
  }

  Future<void> _openAccount() async {
    final created = await Navigator.of(
      context,
    ).push<Account>(MaterialPageRoute(builder: (_) => OpenAccountScreen(accountApi: widget.accountApi)));
    if (created != null) {
      _load();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Your accounts')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openAccount,
        icon: const Icon(Icons.add),
        label: const Text('Open account'),
      ),
      body: SafeArea(
        child: FutureBuilder<List<Account>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            final error = snapshot.error;
            if (error != null) {
              return _ErrorState(
                message: error is ApiException ? error.message : 'Something went wrong.',
                onRetry: _load,
              );
            }
            final accounts = snapshot.data ?? const [];
            if (accounts.isEmpty) {
              return const Center(child: Text('No accounts yet. Open one to get started.'));
            }
            return RefreshIndicator(
              onRefresh: () async => _load(),
              child: ListView.builder(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                itemCount: accounts.length,
                itemBuilder: (context, index) => AccountTile(
                  account: accounts[index],
                  onTap: () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => AccountDetailScreen(accountApi: widget.accountApi, account: accounts[index]),
                    ),
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class AccountTile extends StatelessWidget {
  const AccountTile({required this.account, required this.onTap, super.key});

  final Account account;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        onTap: onTap,
        title: Text('${account.accountType.label} · ${account.currency.toJson()}'),
        subtitle: Text(account.accountNumber),
        trailing: Text(
          formatMoney(account.balance, account.currency),
          style: Theme.of(context).textTheme.titleMedium,
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
          ],
        ),
      ),
    );
  }
}
