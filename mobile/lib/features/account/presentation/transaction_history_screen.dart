import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;

class TransactionHistoryScreen extends StatefulWidget {
  final Account account;
  final AccountApi? accountApi;

  const TransactionHistoryScreen({
    super.key,
    required this.account,
    this.accountApi,
  });

  @override
  State<TransactionHistoryScreen> createState() => _TransactionHistoryScreenState();
}

class _TransactionHistoryScreenState extends State<TransactionHistoryScreen> {
  TransactionType? _type;
  DateTime? _fromDate;
  DateTime? _toDate;
  final _minAmountController = TextEditingController();
  final _maxAmountController = TextEditingController();

  final List<BankTransaction> _items = [];
  int _page = 0;
  int? _totalPages;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  String? _errorText;

  AccountApi get _api => widget.accountApi ?? AccountApi();

  @override
  void initState() {
    super.initState();
    _applyFilters();
  }

  @override
  void dispose() {
    _minAmountController.dispose();
    _maxAmountController.dispose();
    super.dispose();
  }

  TransactionQuery _queryFor(int page) => TransactionQuery(
    type: _type,
    fromDate: _fromDate,
    toDate: _toDate,
    minAmount: double.tryParse(_minAmountController.text.trim()),
    maxAmount: double.tryParse(_maxAmountController.text.trim()),
    page: page,
  );

  Future<void> _applyFilters() async {
    setState(() {
      _isLoading = true;
      _errorText = null;
      _items.clear();
      _page = 0;
      _totalPages = null;
    });
    try {
      final result = await _api.listTransactions(widget.account.id, _queryFor(0));
      if (!mounted) return;
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _totalPages = result.totalPages;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _errorText = e.message);
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _loadMore() async {
    setState(() => _isLoadingMore = true);
    try {
      final result = await _api.listTransactions(widget.account.id, _queryFor(_page + 1));
      if (!mounted) return;
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _totalPages = result.totalPages;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) {
        setState(() => _isLoadingMore = false);
      }
    }
  }

  Future<void> _pickDate({required bool isFrom}) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: (isFrom ? _fromDate : _toDate) ?? now,
      firstDate: DateTime(now.year - 10),
      lastDate: now,
    );
    if (picked == null) return;

    setState(() {
      if (isFrom) {
        _fromDate = picked;
      } else {
        _toDate = picked;
      }
    });
  }

  bool get _hasMore => _totalPages != null && _page + 1 < _totalPages!;

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
        title: Text(
          'Transactions - ${widget.account.accountNumber}',
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: GlassCard(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    DropdownButtonFormField<TransactionType?>(
                      initialValue: _type,
                      dropdownColor: const Color(0xFF0A2B24),
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(labelText: 'Transaction Type', isDense: true),
                      items: [
                        const DropdownMenuItem(value: null, child: Text('All Types')),
                        ...TransactionType.values.map(
                          (type) => DropdownMenuItem(value: type, child: Text(type.label)),
                        ),
                      ],
                      onChanged: (value) => setState(() => _type = value),
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton(
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.white,
                              side: const BorderSide(color: Colors.white24),
                            ),
                            onPressed: () => _pickDate(isFrom: true),
                            child: Text(_fromDate == null ? 'From date' : formatLocalDate(_fromDate!)),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton(
                            style: OutlinedButton.styleFrom(
                              foregroundColor: Colors.white,
                              side: const BorderSide(color: Colors.white24),
                            ),
                            onPressed: () => _pickDate(isFrom: false),
                            child: Text(_toDate == null ? 'To date' : formatLocalDate(_toDate!)),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _minAmountController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Min amount', isDense: true),
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _maxAmountController,
                            style: const TextStyle(color: Colors.white),
                            decoration: const InputDecoration(labelText: 'Max amount', isDense: true),
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    PrimaryActionButton(
                      title: 'APPLY FILTERS',
                      onPressed: _isLoading ? null : _applyFilters,
                    ),
                  ],
                ),
              ),
            ),
            Expanded(child: _buildList()),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight));
    }
    if (_errorText != null) {
      return Center(child: Text(_errorText!, style: const TextStyle(color: AppTheme.primaryRedLight)));
    }
    if (_items.isEmpty) {
      return const Center(child: Text('No transactions match these filters.', style: TextStyle(color: Colors.white60)));
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      itemCount: _items.length + 1,
      itemBuilder: (context, index) {
        if (index == _items.length) {
          if (!_hasMore) {
            return const SizedBox.shrink();
          }
          return Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Center(
              child: _isLoadingMore
                  ? const CircularProgressIndicator(color: AppTheme.emeraldLight)
                  : OutlinedButton(
                      style: OutlinedButton.styleFrom(foregroundColor: Colors.white, side: const BorderSide(color: Colors.white24)),
                      onPressed: _loadMore,
                      child: const Text('Load more'),
                    ),
            ),
          );
        }
        final tx = _items[index];
        final isCredit = tx.type == TransactionType.transferIn || tx.type == TransactionType.deposit;
        final sign = isCredit ? '+' : '-';
        final color = isCredit ? const Color(0xFF69F0AE) : Colors.white;

        return Container(
          margin: const EdgeInsets.only(bottom: 8),
          child: GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            borderRadius: 14,
            child: Row(
              children: [
                CircleAvatar(
                  backgroundColor: isCredit ? const Color(0x3300C853) : const Color(0x33FF3B30),
                  child: Icon(
                    isCredit ? Icons.arrow_downward : Icons.arrow_upward,
                    color: isCredit ? Colors.greenAccent : Colors.redAccent,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        tx.description != null && tx.description!.isNotEmpty
                            ? tx.description!
                            : tx.type.name.toUpperCase(),
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: Colors.white),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        formatLocalDate(tx.createdAt),
                        style: const TextStyle(fontSize: 11, color: Colors.white60),
                      ),
                    ],
                  ),
                ),
                Text(
                  '$sign ${formatMoney(tx.amount, tx.currency)}',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15, color: color),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
