import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;
import 'package:mobile/features/bill/data/bill_api.dart';
import 'package:mobile/features/bill/data/bill_models.dart';

class RecurringBillScreen extends StatefulWidget {
  const RecurringBillScreen({super.key});

  @override
  State<RecurringBillScreen> createState() => _RecurringBillScreenState();
}

class _RecurringBillScreenState extends State<RecurringBillScreen> {
  List<RecurringBillPayment> _recurringList = [];
  List<BillProvider> _providers = [];
  List<Account> _accounts = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final recurring = await BillApi().listRecurring();
      final providers = await BillApi().listProviders();
      final accounts = await AccountApi().listAccounts();
      setState(() {
        _recurringList = recurring;
        _providers = providers;
        _accounts = accounts;
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load recurring payments.";
        _loading = false;
      });
    }
  }

  void _showAddRecurringDialog() {
    if (_providers.isEmpty || _accounts.isEmpty) return;

    BillProvider selectedProvider = _providers.first;
    Account selectedAccount = _accounts.first;
    final billAccCtrl = TextEditingController();
    final amountCtrl = TextEditingController();
    String frequency = 'MONTHLY';
    DateTime nextDate = DateTime.now().add(const Duration(days: 30));
    final messenger = ScaffoldMessenger.of(context);

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: const Color(0xFF0A2B24),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text('Schedule Recurring Bill', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<BillProvider>(
                  initialValue: selectedProvider,
                  dropdownColor: const Color(0xFF0A2B24),
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Biller / Provider'),
                  items: _providers.map((p) => DropdownMenuItem(value: p, child: Text(p.name))).toList(),
                  onChanged: (p) => setModalState(() => selectedProvider = p!),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<Account>(
                  initialValue: selectedAccount,
                  dropdownColor: const Color(0xFF0A2B24),
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'From Account'),
                  items: _accounts.map((a) => DropdownMenuItem(value: a, child: Text('${a.accountNumber} (${a.currency.toJson()})'))).toList(),
                  onChanged: (a) => setModalState(() => selectedAccount = a!),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: billAccCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Bill Account Number'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: amountCtrl,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  style: const TextStyle(color: Colors.white),
                  decoration: InputDecoration(labelText: 'Amount (${selectedAccount.currency.toJson()})'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: frequency,
                  dropdownColor: const Color(0xFF0A2B24),
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(labelText: 'Frequency'),
                  items: const [
                    DropdownMenuItem(value: 'DAILY', child: Text('Daily')),
                    DropdownMenuItem(value: 'WEEKLY', child: Text('Weekly')),
                    DropdownMenuItem(value: 'MONTHLY', child: Text('Monthly')),
                  ],
                  onChanged: (f) => setModalState(() => frequency = f!),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
              onPressed: () async {
                final billAcc = billAccCtrl.text.trim();
                final amount = double.tryParse(amountCtrl.text.trim());
                if (billAcc.isEmpty || amount == null || amount <= 0) return;
                Navigator.pop(ctx);
                try {
                  await BillApi().createRecurring(
                    CreateRecurringBillRequest(
                      fromAccountId: selectedAccount.id,
                      providerId: selectedProvider.id,
                      billAccountNumber: billAcc,
                      amount: amount,
                      currency: selectedAccount.currency.toJson(),
                      frequency: frequency,
                      nextPaymentDate: formatLocalDate(nextDate),
                    ),
                  );
                  _loadData();
                } catch (e) {
                  messenger.showSnackBar(
                    SnackBar(content: Text('Failed to schedule: $e'), backgroundColor: AppTheme.primaryRed),
                  );
                }
              },
              child: const Text('Schedule', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  void _cancelRecurring(RecurringBillPayment r) async {
    try {
      await BillApi().cancelRecurring(r.id);
      setState(() => _recurringList.removeWhere((item) => item.id == r.id));
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Recurring Bill Payments', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline, color: AppTheme.emeraldLight),
            onPressed: _showAddRecurringDialog,
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight))
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: AppTheme.primaryRedLight)),
                      const SizedBox(height: 12),
                      ElevatedButton(onPressed: _loadData, child: const Text('Retry')),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadData,
                  color: AppTheme.emeraldLight,
                  child: _recurringList.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(Icons.schedule, size: 64, color: Colors.white.withValues(alpha: 0.3)),
                              const SizedBox(height: 16),
                              const Text('No scheduled recurring payments', style: TextStyle(color: Colors.white70, fontSize: 16)),
                              const SizedBox(height: 12),
                              ElevatedButton.icon(
                                style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
                                icon: const Icon(Icons.add, color: Colors.white),
                                label: const Text('Schedule Bill Payment', style: TextStyle(color: Colors.white)),
                                onPressed: _showAddRecurringDialog,
                              ),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _recurringList.length,
                          itemBuilder: (ctx, idx) {
                            final item = _recurringList[idx];
                            final currency = Currency.values.firstWhere(
                              (c) => c.name.toUpperCase() == item.currency.toUpperCase(),
                              orElse: () => Currency.usd,
                            );

                            return Container(
                              margin: const EdgeInsets.only(bottom: 12),
                              child: GlassCard(
                                padding: const EdgeInsets.all(16),
                                child: Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor: const Color(0x3300FFB2),
                                      child: const Icon(Icons.autorenew, color: AppTheme.emeraldLight),
                                    ),
                                    const SizedBox(width: 14),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            item.providerName,
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: Colors.white),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Acc: ${item.billAccountNumber} • ${item.frequency}',
                                            style: const TextStyle(fontSize: 12, color: Colors.white60),
                                          ),
                                          const SizedBox(height: 4),
                                          Text(
                                            formatMoney(item.amount, currency),
                                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: AppTheme.emeraldLight),
                                          ),
                                        ],
                                      ),
                                    ),
                                    IconButton(
                                      icon: const Icon(Icons.cancel_outlined, color: AppTheme.primaryRedLight, size: 22),
                                      onPressed: () => _cancelRecurring(item),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
    );
  }
}
