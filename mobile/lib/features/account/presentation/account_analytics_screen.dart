import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/auth/data/auth_models.dart'
    show formatLocalDate;
import 'package:mobile/core/widgets/app_state_view.dart';

class AccountAnalyticsScreen extends StatefulWidget {
  final Account account;
  final AccountApi? accountApi;

  const AccountAnalyticsScreen({
    super.key,
    required this.account,
    this.accountApi,
  });

  @override
  State<AccountAnalyticsScreen> createState() => _AccountAnalyticsScreenState();
}

class _AccountAnalyticsScreenState extends State<AccountAnalyticsScreen> {
  AccountApi get _api => widget.accountApi ?? AccountApi();

  bool _loading = true;
  String? _error;
  List<BankTransaction> _transactions = [];
  String _timeRange = 'All Time'; // 'This Month', 'Last 30 Days', 'All Time'

  @override
  void initState() {
    super.initState();
    _loadAnalytics();
  }

  Future<void> _loadAnalytics() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final page = await _api.listTransactions(
        widget.account.id,
        TransactionQuery(page: 0, size: 50),
      );
      setState(() {
        _transactions = page.content;
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load spending analytics.";
        _loading = false;
      });
    }
  }

  List<BankTransaction> get _filteredTransactions {
    final now = DateTime.now();
    if (_timeRange == 'This Month') {
      return _transactions
          .where(
            (t) =>
                t.createdAt.year == now.year && t.createdAt.month == now.month,
          )
          .toList();
    } else if (_timeRange == 'Last 30 Days') {
      final thirtyDaysAgo = now.subtract(const Duration(days: 30));
      return _transactions
          .where((t) => t.createdAt.isAfter(thirtyDaysAgo))
          .toList();
    }
    return _transactions;
  }

  double get _totalSpent {
    double sum = 0.0;
    for (final t in _filteredTransactions) {
      if (!t.type.isCredit) {
        sum += t.amount;
      }
    }
    return sum;
  }

  double get _totalIncome {
    double sum = 0.0;
    for (final t in _filteredTransactions) {
      if (t.type.isCredit) {
        sum += t.amount;
      }
    }
    return sum;
  }

  Map<String, double> get _categorySpending {
    final map = <String, double>{};
    for (final t in _filteredTransactions) {
      if (!t.type.isCredit) {
        final category = _categorizeTransaction(t);
        map[category] = (map[category] ?? 0.0) + t.amount;
      }
    }
    return map;
  }

  String _categorizeTransaction(BankTransaction t) {
    final desc = (t.description ?? '').toLowerCase();
    final type = t.type.name.toLowerCase();
    if (type.contains('bill') ||
        desc.contains('bill') ||
        desc.contains('utility')) {
      return 'Bills & Utilities';
    } else if (type.contains('card') ||
        desc.contains('pos') ||
        desc.contains('card')) {
      return 'Card Purchases';
    } else if (type.contains('transfer') ||
        desc.contains('transfer') ||
        desc.contains('p2p')) {
      return 'Transfers & Remittances';
    } else if (desc.contains('qr') || type.contains('qr')) {
      return 'QR Payments';
    } else {
      return 'General Spending';
    }
  }

  IconData _iconForCategory(String category) {
    switch (category) {
      case 'Bills & Utilities':
        return Icons.receipt_long_rounded;
      case 'Card Purchases':
        return Icons.credit_card_rounded;
      case 'Transfers & Remittances':
        return Icons.swap_horiz_rounded;
      case 'QR Payments':
        return Icons.qr_code_scanner_rounded;
      default:
        return Icons.shopping_bag_outlined;
    }
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: const BackButton(color: Colors.white),
        title: const Text(
          'Spending Analytics',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
      ),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(color: AppTheme.emeraldLight),
            )
          : _error != null
          ? AppErrorView(message: _error!, onRetry: _loadAnalytics)
          : RefreshIndicator(
              onRefresh: _loadAnalytics,
              color: AppTheme.emeraldLight,
              child: ListView(
                padding: const EdgeInsets.symmetric(
                  horizontal: 20,
                  vertical: 12,
                ),
                children: [
                  // Account Info Card
                  GlassCard(
                    padding: const EdgeInsets.all(18),
                    borderRadius: 20,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${widget.account.accountType.name.toUpperCase()} ACCOUNT',
                              style: const TextStyle(
                                color: Colors.white60,
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.account.accountNumber,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                letterSpacing: 0.8,
                              ),
                            ),
                          ],
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 5,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0x3300FFB2),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: AppTheme.emeraldLight.withValues(
                                alpha: 0.5,
                              ),
                            ),
                          ),
                          child: Text(
                            widget.account.currency.toJson(),
                            style: const TextStyle(
                              color: AppTheme.emeraldLight,
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 18),

                  // Time range chips
                  Row(
                    children: ['All Time', 'This Month', 'Last 30 Days'].map((
                      range,
                    ) {
                      final isSelected = _timeRange == range;
                      return Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ChoiceChip(
                          label: Text(
                            range,
                            style: TextStyle(
                              color: isSelected ? Colors.white : Colors.white70,
                              fontWeight: isSelected
                                  ? FontWeight.bold
                                  : FontWeight.normal,
                              fontSize: 12,
                            ),
                          ),
                          selected: isSelected,
                          selectedColor: AppTheme.primaryEmerald,
                          backgroundColor: AppTheme.surfaceGlass,
                          onSelected: (_) => setState(() => _timeRange = range),
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 18),

                  // Outflow vs Inflow Cards
                  Row(
                    children: [
                      Expanded(
                        child: GlassCard(
                          padding: const EdgeInsets.all(16),
                          borderRadius: 16,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: const [
                                  Icon(
                                    Icons.arrow_upward_rounded,
                                    color: AppTheme.primaryRedLight,
                                    size: 16,
                                  ),
                                  SizedBox(width: 4),
                                  Text(
                                    'Total Spent',
                                    style: TextStyle(
                                      color: Colors.white60,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              Text(
                                formatMoney(
                                  _totalSpent,
                                  widget.account.currency,
                                ),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: GlassCard(
                          padding: const EdgeInsets.all(16),
                          borderRadius: 16,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: const [
                                  Icon(
                                    Icons.arrow_downward_rounded,
                                    color: AppTheme.emeraldLight,
                                    size: 16,
                                  ),
                                  SizedBox(width: 4),
                                  Text(
                                    'Total Received',
                                    style: TextStyle(
                                      color: Colors.white60,
                                      fontSize: 12,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 8),
                              Text(
                                formatMoney(
                                  _totalIncome,
                                  widget.account.currency,
                                ),
                                style: const TextStyle(
                                  color: Color(0xFF69F0AE),
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),

                  // Spending by Category Breakdown
                  const Text(
                    'Spending by Category',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 17,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),

                  if (_categorySpending.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(24),
                      child: Center(
                        child: Text(
                          'No spending records found in this period',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.6),
                            fontSize: 13,
                          ),
                        ),
                      ),
                    )
                  else ...[
                    ..._categorySpending.entries.map((entry) {
                      final pct = _totalSpent > 0
                          ? (entry.value / _totalSpent)
                          : 0.0;
                      return Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        child: GlassCard(
                          padding: const EdgeInsets.all(16),
                          borderRadius: 16,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  CircleAvatar(
                                    radius: 18,
                                    backgroundColor: const Color(0x3300FFB2),
                                    child: Icon(
                                      _iconForCategory(entry.key),
                                      color: AppTheme.emeraldLight,
                                      size: 18,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Text(
                                      entry.key,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.bold,
                                        fontSize: 14,
                                      ),
                                    ),
                                  ),
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.end,
                                    children: [
                                      Text(
                                        formatMoney(
                                          entry.value,
                                          widget.account.currency,
                                        ),
                                        style: const TextStyle(
                                          color: Colors.white,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14,
                                        ),
                                      ),
                                      Text(
                                        '${(pct * 100).toStringAsFixed(1)}%',
                                        style: const TextStyle(
                                          color: Colors.white60,
                                          fontSize: 11,
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ),
                              const SizedBox(height: 10),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(4),
                                child: LinearProgressIndicator(
                                  value: pct,
                                  minHeight: 6,
                                  backgroundColor: Colors.white12,
                                  color: AppTheme.emeraldLight,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                  ],
                  const SizedBox(height: 20),

                  // Transactions in this breakdown
                  const Text(
                    'Period Transactions',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 17,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (_filteredTransactions.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 20),
                      child: Center(
                        child: Text(
                          'No transactions to display',
                          style: TextStyle(color: Colors.white60, fontSize: 13),
                        ),
                      ),
                    )
                  else
                    ..._filteredTransactions.map((tx) {
                      final isDebit = !tx.type.isCredit;
                      return Container(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: GlassCard(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 10,
                          ),
                          borderRadius: 12,
                          child: Row(
                            children: [
                              Icon(
                                isDebit
                                    ? Icons.arrow_upward_rounded
                                    : Icons.arrow_downward_rounded,
                                color: isDebit
                                    ? Colors.redAccent
                                    : Colors.greenAccent,
                                size: 18,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      tx.description != null &&
                                              tx.description!.isNotEmpty
                                          ? tx.description!
                                          : tx.type.name.toUpperCase(),
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.w600,
                                        fontSize: 13,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                    Text(
                                      formatLocalDate(tx.createdAt),
                                      style: const TextStyle(
                                        color: Colors.white54,
                                        fontSize: 11,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Text(
                                '${isDebit ? "- " : "+ "}${formatMoney(tx.amount, tx.currency)}',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 13,
                                  color: isDebit
                                      ? Colors.white
                                      : const Color(0xFF69F0AE),
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                  const SizedBox(height: 24),
                ],
              ),
            ),
    );
  }
}
