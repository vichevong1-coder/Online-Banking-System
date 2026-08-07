import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;

// US-017/018: list + filter by date / type / amount. Page size fixed at 20 (TransactionQuery's
// default) with a manual "Load more" rather than a paging widget — one screen doesn't need one.
class TransactionHistoryScreen extends StatefulWidget {
  const TransactionHistoryScreen({required this.accountApi, required this.account, super.key});

  final AccountApi accountApi;
  final Account account;

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
      final result = await widget.accountApi.listTransactions(widget.account.id, _queryFor(0));
      if (!mounted) {
        return;
      }
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _totalPages = result.totalPages;
      });
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
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
      final result = await widget.accountApi.listTransactions(widget.account.id, _queryFor(_page + 1));
      if (!mounted) {
        return;
      }
      setState(() {
        _items.addAll(result.content);
        _page = result.page;
        _totalPages = result.totalPages;
      });
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
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
    if (picked == null) {
      return;
    }
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
    return Scaffold(
      appBar: AppBar(title: const Text('Transaction history')),
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButtonFormField<TransactionType?>(
                          initialValue: _type,
                          decoration: const InputDecoration(labelText: 'Type', isDense: true),
                          items: [
                            const DropdownMenuItem(value: null, child: Text('All')),
                            ...TransactionType.values.map(
                              (type) => DropdownMenuItem(value: type, child: Text(type.label)),
                            ),
                          ],
                          onChanged: (value) => setState(() => _type = value),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => _pickDate(isFrom: true),
                          child: Text(_fromDate == null ? 'From date' : formatLocalDate(_fromDate!)),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () => _pickDate(isFrom: false),
                          child: Text(_toDate == null ? 'To date' : formatLocalDate(_toDate!)),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _minAmountController,
                          decoration: const InputDecoration(labelText: 'Min amount', isDense: true),
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: _maxAmountController,
                          decoration: const InputDecoration(labelText: 'Max amount', isDense: true),
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(onPressed: _isLoading ? null : _applyFilters, child: const Text('Apply filters')),
                  ),
                ],
              ),
            ),
            const Divider(height: 24),
            Expanded(child: _buildList()),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_errorText != null) {
      return Center(child: Text(_errorText!, style: TextStyle(color: Theme.of(context).colorScheme.error)));
    }
    if (_items.isEmpty) {
      return const Center(child: Text('No transactions match these filters.'));
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
                  ? const CircularProgressIndicator()
                  : OutlinedButton(onPressed: _loadMore, child: const Text('Load more')),
            ),
          );
        }
        final tx = _items[index];
        final sign = tx.type.isCredit ? '+' : '-';
        final color = tx.type.isCredit ? Colors.green.shade700 : Theme.of(context).colorScheme.error;
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            title: Text(tx.type.label),
            subtitle: Text(tx.description?.isNotEmpty == true ? tx.description! : formatLocalDate(tx.createdAt)),
            trailing: Text('$sign${formatMoney(tx.amount, tx.currency)}', style: TextStyle(color: color)),
          ),
        );
      },
    );
  }
}
