import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/account_detail_screen.dart';
import 'package:mobile/features/account/presentation/account_list_screen.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/account/presentation/open_account_screen.dart';
import 'package:mobile/features/auth/data/auth_models.dart';

// US-024: account dashboard / home screen — the post-2FA landing page (see two_factor_screen.dart),
// replacing the Sprint 1 SignedInScreen stub. Aggregates accounts, per-currency balance subtotals,
// and recent transactions across accounts. The AccountApi is built once here, from the tokens this
// screen receives at sign-in, and passed down to every child screen rather than re-threading the
// raw access token through each constructor.
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({required this.tokens, super.key});

  final AuthTokenResponse tokens;

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _RecentActivityItem {
  _RecentActivityItem(this.account, this.transaction);

  final Account account;
  final BankTransaction transaction;
}

class _DashboardScreenState extends State<DashboardScreen> {
  late final AccountApi _accountApi = AccountApi(accessToken: widget.tokens.accessToken);

  Future<List<Account>>? _accountsFuture;
  List<_RecentActivityItem> _recentActivity = [];
  bool _isLoadingActivity = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    final future = _accountApi.listAccounts();
    setState(() => _accountsFuture = future);
    future.then(_loadRecentActivity, onError: (_) {});
  }

  Future<void> _loadRecentActivity(List<Account> accounts) async {
    if (accounts.isEmpty) {
      setState(() => _recentActivity = []);
      return;
    }
    setState(() => _isLoadingActivity = true);
    try {
      final perAccount = await Future.wait(
        accounts.map(
          (account) async {
            final page = await _accountApi.listTransactions(account.id, TransactionQuery(size: 5));
            return page.content.map((tx) => _RecentActivityItem(account, tx));
          },
        ),
      );
      final merged = perAccount.expand((items) => items).toList()
        ..sort((a, b) => b.transaction.createdAt.compareTo(a.transaction.createdAt));
      if (!mounted) {
        return;
      }
      setState(() => _recentActivity = merged.take(5).toList());
    } catch (_) {
      // Recent activity is a dashboard convenience, not the source of truth — if it fails to
      // load, the account list above still renders fine, so this silently leaves it empty.
    } finally {
      if (mounted) {
        setState(() => _isLoadingActivity = false);
      }
    }
  }

  Future<void> _openAccount() async {
    final created = await Navigator.of(
      context,
    ).push<Account>(MaterialPageRoute(builder: (_) => OpenAccountScreen(accountApi: _accountApi)));
    if (created != null) {
      _load();
    }
  }

  Map<Currency, double> _subtotalsByCurrency(List<Account> accounts) {
    final totals = <Currency, double>{};
    for (final account in accounts) {
      totals[account.currency] = (totals[account.currency] ?? 0) + account.balance;
    }
    return totals;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Hi, ${widget.tokens.firstName}')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _openAccount,
        icon: const Icon(Icons.add),
        label: const Text('Open account'),
      ),
      body: SafeArea(
        child: FutureBuilder<List<Account>>(
          future: _accountsFuture,
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
            return RefreshIndicator(
              onRefresh: () async => _load(),
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                children: [
                  if (accounts.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Center(child: Text('No accounts yet. Open one to get started.')),
                    )
                  else ...[
                    _buildSubtotals(context, accounts),
                    const SizedBox(height: 24),
                    _buildAccountsSection(context, accounts),
                    const SizedBox(height: 24),
                    _buildRecentActivitySection(context),
                  ],
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _buildSubtotals(BuildContext context, List<Account> accounts) {
    final subtotals = _subtotalsByCurrency(accounts);
    return Row(
      children: subtotals.entries
          .map(
            (entry) => Expanded(
              child: Card(
                margin: const EdgeInsets.only(right: 8),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(entry.key.toJson(), style: Theme.of(context).textTheme.labelLarge),
                      const SizedBox(height: 4),
                      Text(formatMoney(entry.value, entry.key), style: Theme.of(context).textTheme.titleLarge),
                    ],
                  ),
                ),
              ),
            ),
          )
          .toList(),
    );
  }

  Widget _buildAccountsSection(BuildContext context, List<Account> accounts) {
    final shown = accounts.take(4).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text('Your accounts', style: Theme.of(context).textTheme.titleMedium),
            TextButton(
              onPressed: () => Navigator.of(
                context,
              ).push(MaterialPageRoute(builder: (_) => AccountListScreen(accountApi: _accountApi))).then((_) => _load()),
              child: const Text('See all'),
            ),
          ],
        ),
        ...shown.map(
          (account) => AccountTile(
            account: account,
            onTap: () => Navigator.of(
              context,
            ).push(MaterialPageRoute(builder: (_) => AccountDetailScreen(accountApi: _accountApi, account: account))),
          ),
        ),
      ],
    );
  }

  Widget _buildRecentActivitySection(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Recent activity', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 8),
        if (_isLoadingActivity)
          const Padding(padding: EdgeInsets.symmetric(vertical: 16), child: Center(child: CircularProgressIndicator()))
        else if (_recentActivity.isEmpty)
          const Padding(padding: EdgeInsets.symmetric(vertical: 8), child: Text('No recent transactions.'))
        else
          ..._recentActivity.map((item) {
            final tx = item.transaction;
            final sign = tx.type.isCredit ? '+' : '-';
            final color = tx.type.isCredit ? Colors.green.shade700 : Theme.of(context).colorScheme.error;
            return Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: ListTile(
                title: Text(tx.type.label),
                subtitle: Text('${item.account.accountType.label} · ${item.account.accountNumber}'),
                trailing: Text('$sign${formatMoney(tx.amount, tx.currency)}', style: TextStyle(color: color)),
              ),
            );
          }),
      ],
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
