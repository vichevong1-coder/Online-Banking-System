import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/bill/data/bill_api.dart';
import 'package:mobile/features/bill/data/bill_models.dart';
import 'package:mobile/features/bill/presentation/pay_bill_screen.dart';
import 'package:mobile/features/bill/presentation/recurring_bill_screen.dart';
import 'package:mobile/features/transfer/presentation/transfer_receipt_screen.dart';
import 'package:mobile/core/widgets/app_state_view.dart';

class BillPaymentsTabScreen extends StatefulWidget {
  const BillPaymentsTabScreen({super.key});

  @override
  State<BillPaymentsTabScreen> createState() => _BillPaymentsTabScreenState();
}

class _BillPaymentsTabScreenState extends State<BillPaymentsTabScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<BillProvider> _providers = [];
  List<BillPayment> _history = [];
  String _selectedCategory = 'ALL';
  bool _loading = true;
  String? _error;

  final List<String> _categories = [
    'ALL',
    'ELECTRICITY',
    'WATER',
    'INTERNET',
    'MOBILE_TOPUP',
    'OTHER',
  ];

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final providers = await BillApi().listProviders();
      final history = await BillApi().getPaymentHistory();
      setState(() {
        _providers = providers;
        _history = history;
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load bill providers.";
        _loading = false;
      });
    }
  }

  IconData _iconForCategory(String category) {
    switch (category) {
      case 'ELECTRICITY':
        return Icons.bolt;
      case 'WATER':
        return Icons.water_drop;
      case 'INTERNET':
        return Icons.wifi;
      case 'MOBILE_TOPUP':
        return Icons.phone_android;
      default:
        return Icons.receipt_long;
    }
  }

  List<BillProvider> get _filteredProviders {
    if (_selectedCategory == 'ALL') return _providers;
    return _providers
        .where((p) => p.category.toUpperCase() == _selectedCategory)
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const GradientScaffold(
        body: Center(
          child: CircularProgressIndicator(color: AppTheme.emeraldLight),
        ),
      );
    }

    if (_error != null) {
      return GradientScaffold(
        appBar: AppBar(
          backgroundColor: Colors.transparent,
          elevation: 0,
          leading: const BackButton(color: Colors.white),
          title: const Text(
            'Bill Payments',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
          ),
        ),
        body: AppErrorView(message: _error!, onRetry: _loadData),
      );
    }

    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: const BackButton(color: Colors.white),
        title: const Text(
          'Bill Payments',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Hero Header: CBA Payments
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  'CBA Payments',
                  style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w800,
                    color: Colors.white,
                  ),
                ),
                SizedBox(height: 6),
                Text(
                  'Top up and pay bills anything, with no fees',
                  style: TextStyle(color: Colors.white70, fontSize: 13),
                ),
              ],
            ),
          ),

          // Tab Header (Providers vs History)
          Container(
            margin: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            decoration: BoxDecoration(
              color: AppTheme.surfaceGlass,
              borderRadius: BorderRadius.circular(14),
            ),
            child: TabBar(
              controller: _tabController,
              indicatorSize: TabBarIndicatorSize.tab,
              indicator: BoxDecoration(
                borderRadius: BorderRadius.circular(12),
                color: AppTheme.primaryEmerald,
              ),
              labelColor: Colors.white,
              unselectedLabelColor: Colors.white60,
              tabs: const [
                Tab(text: 'Pay Bills'),
                Tab(text: 'Payment History'),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // TAB 1: Pay Bills
                RefreshIndicator(
                  onRefresh: _loadData,
                  color: AppTheme.emeraldLight,
                  child: ListView(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 20,
                      vertical: 12,
                    ),
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text(
                            'Utility Billers',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                          TextButton.icon(
                            icon: const Icon(
                              Icons.schedule,
                              size: 16,
                              color: AppTheme.emeraldLight,
                            ),
                            label: const Text(
                              'Recurring',
                              style: TextStyle(
                                color: AppTheme.emeraldLight,
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => const RecurringBillScreen(),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),

                      // Category Chips
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: _categories.map((cat) {
                            final selected = _selectedCategory == cat;
                            return Padding(
                              padding: const EdgeInsets.only(right: 8.0),
                              child: ChoiceChip(
                                label: Text(
                                  cat == 'ALL'
                                      ? 'All Billers'
                                      : cat.replaceAll('_', ' '),
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: selected
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                    color: selected
                                        ? Colors.white
                                        : Colors.white70,
                                  ),
                                ),
                                selected: selected,
                                selectedColor: AppTheme.primaryEmerald,
                                backgroundColor: AppTheme.surfaceGlass,
                                onSelected: (_) =>
                                    setState(() => _selectedCategory = cat),
                              ),
                            );
                          }).toList(),
                        ),
                      ),
                      const SizedBox(height: 16),

                      if (_filteredProviders.isEmpty)
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 32),
                          child: Center(
                            child: Text(
                              'No providers available in this category',
                              style: TextStyle(
                                color: Colors.white60,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        )
                      else
                        ..._filteredProviders.map(
                          (p) => Container(
                            margin: const EdgeInsets.only(bottom: 12),
                            child: GlassCard(
                              padding: const EdgeInsets.all(14),
                              borderRadius: 14,
                              onTap: () => Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (_) => PayBillScreen(provider: p),
                                ),
                              ).then((_) => _loadData()),
                              child: Row(
                                children: [
                                  CircleAvatar(
                                    backgroundColor: const Color(0x3300FFB2),
                                    child: Icon(
                                      _iconForCategory(p.category),
                                      color: AppTheme.emeraldLight,
                                      size: 22,
                                    ),
                                  ),
                                  const SizedBox(width: 14),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          p.name,
                                          style: const TextStyle(
                                            fontWeight: FontWeight.bold,
                                            fontSize: 15,
                                            color: Colors.white,
                                          ),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          p.category.replaceAll('_', ' '),
                                          style: const TextStyle(
                                            color: Colors.white60,
                                            fontSize: 12,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const Icon(
                                    Icons.arrow_forward_ios,
                                    size: 14,
                                    color: Colors.white60,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      const SizedBox(height: 20),
                    ],
                  ),
                ),

                // TAB 2: Payment History
                RefreshIndicator(
                  onRefresh: _loadData,
                  color: AppTheme.emeraldLight,
                  child: _history.isEmpty
                      ? AppEmptyView(
                          icon: Icons.receipt_long_outlined,
                          title: 'No bill payment history yet',
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(20),
                          itemCount: _history.length,
                          itemBuilder: (ctx, idx) {
                            final item = _history[idx];
                            final currency = Currency.values.firstWhere(
                              (c) =>
                                  c.name.toUpperCase() ==
                                  item.currency.toUpperCase(),
                              orElse: () => Currency.usd,
                            );

                            return Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              child: GlassCard(
                                padding: const EdgeInsets.all(14),
                                borderRadius: 14,
                                onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => TransferReceiptScreen(
                                      transfer: item.asTransfer(),
                                      payeeLabel:
                                          '${item.providerName} (${item.billAccountNumber})',
                                    ),
                                  ),
                                ),
                                child: Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor: const Color(0x33FF3B30),
                                      child: const Icon(
                                        Icons.arrow_upward,
                                        color: Colors.redAccent,
                                        size: 20,
                                      ),
                                    ),
                                    const SizedBox(width: 14),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            item.providerName,
                                            style: const TextStyle(
                                              fontWeight: FontWeight.bold,
                                              fontSize: 14,
                                              color: Colors.white,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Acc: ${item.billAccountNumber} • ${item.createdAt.isNotEmpty ? item.createdAt.substring(0, 10) : ""}',
                                            style: const TextStyle(
                                              fontSize: 11,
                                              color: Colors.white60,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Text(
                                      '- ${formatMoney(item.amount, currency)}',
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 14,
                                        color: Colors.white,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
