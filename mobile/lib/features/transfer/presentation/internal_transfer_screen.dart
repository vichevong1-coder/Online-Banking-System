import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/core/widgets/app_state_view.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/transfer/data/transfer_api.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';
import 'package:mobile/features/transfer/presentation/transfer_receipt_screen.dart';

class InternalTransferScreen extends StatefulWidget {
  const InternalTransferScreen({super.key});

  @override
  State<InternalTransferScreen> createState() => _InternalTransferScreenState();
}

class _InternalTransferScreenState extends State<InternalTransferScreen> {
  List<Account> _accounts = [];
  Account? _fromAccount;
  Account? _toAccount;
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  bool _isLoading = false;
  bool _fetchingAccounts = true;
  String? _errorMessage;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _fetchAccounts();
  }

  Future<void> _fetchAccounts() async {
    try {
      final accounts = await AccountApi().listAccounts();
      setState(() {
        _accounts = accounts;
        if (accounts.isNotEmpty) {
          _fromAccount = accounts.first;
          if (accounts.length > 1) {
            _toAccount = accounts[1];
          }
        }
        _fetchingAccounts = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _fetchingAccounts = false;
        _loadError = "Couldn't load your accounts. Check your connection.";
      });
    }
  }

  void _retryFetchAccounts() {
    setState(() {
      _fetchingAccounts = true;
      _loadError = null;
    });
    _fetchAccounts();
  }

  void _showConfirmation() {
    final amountText = _amountController.text.trim();
    final amount = double.tryParse(amountText);

    if (_fromAccount == null || _toAccount == null) {
      setState(
        () => _errorMessage =
            'Please select both source and destination accounts',
      );
      return;
    }
    if (_fromAccount!.id == _toAccount!.id) {
      setState(
        () =>
            _errorMessage = 'Source and destination accounts must be different',
      );
      return;
    }
    if (amount == null || amount <= 0) {
      setState(() => _errorMessage = 'Please enter a valid transfer amount');
      return;
    }
    if (_fromAccount!.currency != _toAccount!.currency) {
      setState(
        () => _errorMessage =
            'Both accounts must use the same currency (${_fromAccount!.currency.toJson()})',
      );
      return;
    }

    if (amount > _fromAccount!.balance) {
      final available = formatMoney(
        _fromAccount!.balance,
        _fromAccount!.currency,
      );
      setState(
        () => _errorMessage =
            'That is more than the available balance ($available)',
      );
      return;
    }

    setState(() => _errorMessage = null);

    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Confirm Internal Transfer',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 20),
            // The amount is the one thing the customer must get right, so it
            // leads the sheet instead of sitting as one row among many.
            Center(
              child: Text(
                formatMoney(amount, _fromAccount!.currency),
                style: const TextStyle(
                  fontSize: 32,
                  fontWeight: FontWeight.bold,
                  color: AppTheme.emeraldLight,
                ),
              ),
            ),
            const SizedBox(height: 20),
            _summaryRow(
              'From',
              '${_fromAccount!.accountNumber} (${_fromAccount!.currency.toJson()})',
            ),
            _summaryRow(
              'To',
              '${_toAccount!.accountNumber} (${_toAccount!.currency.toJson()})',
            ),
            if (_descController.text.isNotEmpty)
              _summaryRow('Description', _descController.text),
            const SizedBox(height: 16),
            const Text(
              'Check the accounts and amount before confirming.',
              style: TextStyle(color: Colors.white54, fontSize: 12, height: 1.4),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: PrimaryActionButton(
                    title: 'CANCEL',
                    isSecondary: true,
                    onPressed: () => Navigator.pop(ctx),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: PrimaryActionButton(
                    title: 'TRANSFER NOW',
                    onPressed: () {
                      Navigator.pop(ctx);
                      _executeTransfer(amount);
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
          ],
        ),
      ),
    );
  }

  // Presets are in the source account's currency. KHR has no cents, so its
  // steps are larger.
  List<double> get _quickAmounts => _fromAccount?.currency == Currency.khr
      ? const [50000, 100000, 200000, 500000]
      : const [10, 50, 100, 500];

  Widget _quickAmountRow() {
    final currency = _fromAccount?.currency ?? Currency.usd;
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final value in _quickAmounts)
          ActionChip(
            label: Text(formatMoney(value, currency)),
            backgroundColor: Colors.white.withValues(alpha: 0.08),
            side: BorderSide(color: Colors.white.withValues(alpha: 0.15)),
            labelStyle: const TextStyle(
              color: Colors.white,
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
            onPressed: () => setState(
              () => _amountController.text = value.toStringAsFixed(
                currency == Currency.usd ? 2 : 0,
              ),
            ),
          ),
      ],
    );
  }

  Widget _summaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(color: Colors.white60, fontSize: 13),
          ),
          Text(
            value,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.bold,
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _executeTransfer(double amount) async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final request = InternalTransferRequest(
        fromAccountId: _fromAccount!.id,
        toAccountId: _toAccount!.id,
        amount: amount,
        description: _descController.text.trim().isNotEmpty
            ? _descController.text.trim()
            : null,
      );

      final transfer = await TransferApi().internalTransfer(request);

      if (!mounted) return;
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => TransferReceiptScreen(transfer: transfer),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(
        () => _errorMessage = "Couldn't complete transfer. Please try again.",
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.white,
            size: 20,
          ),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Transfer Between Own Accounts',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
        ),
      ),
      body: _fetchingAccounts
          ? const Center(
              child: CircularProgressIndicator(color: AppTheme.emeraldLight),
            )
          : _loadError != null
          ? AppErrorView(message: _loadError!, onRetry: _retryFetchAccounts)
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_errorMessage != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryRed.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: AppTheme.primaryRedLight.withValues(
                            alpha: 0.5,
                          ),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.error_outline,
                            color: AppTheme.primaryRedLight,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],

                  // From Account
                  const Text(
                    'From Account',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  GlassCard(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<Account>(
                        value: _fromAccount,
                        isExpanded: true,
                        dropdownColor: AppTheme.surfaceElevated,
                        items: _accounts.map((a) {
                          return DropdownMenuItem(
                            value: a,
                            child: Text(
                              '${a.accountNumber} (${a.currency.toJson()}) - ${formatMoney(a.balance, a.currency)}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          );
                        }).toList(),
                        onChanged: (acc) => setState(() => _fromAccount = acc),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // To Account
                  const Text(
                    'To Account',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  GlassCard(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<Account>(
                        value: _toAccount,
                        isExpanded: true,
                        dropdownColor: AppTheme.surfaceElevated,
                        items: _accounts.map((a) {
                          return DropdownMenuItem(
                            value: a,
                            child: Text(
                              '${a.accountNumber} (${a.currency.toJson()}) - ${formatMoney(a.balance, a.currency)}',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          );
                        }).toList(),
                        onChanged: (acc) => setState(() => _toAccount = acc),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Amount
                  const Text(
                    'Amount',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _amountController,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                      decoration: InputDecoration(
                        hintText: '0.00',
                        hintStyle: const TextStyle(
                          color: Colors.white30,
                          fontSize: 24,
                        ),
                        prefixText:
                            _fromAccount != null &&
                                _fromAccount!.currency == Currency.usd
                            ? '\$ '
                            : 'KHR ',
                        prefixStyle: const TextStyle(
                          color: AppTheme.emeraldLight,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  if (_fromAccount != null) ...[
                    const SizedBox(height: 10),
                    Text(
                      'Available: ${formatMoney(_fromAccount!.balance, _fromAccount!.currency)}',
                      style: const TextStyle(
                        color: Colors.white60,
                        fontSize: 13,
                      ),
                    ),
                    const SizedBox(height: 12),
                    _quickAmountRow(),
                  ],
                  const SizedBox(height: 20),

                  // Note / Description
                  const Text(
                    'Description (Optional)',
                    style: TextStyle(
                      color: Colors.white70,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _descController,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        hintText: 'e.g. Savings allocation',
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  PrimaryActionButton(
                    title: 'REVIEW TRANSFER',
                    isLoading: _isLoading,
                    onPressed: _showConfirmation,
                  ),
                ],
              ),
            ),
    );
  }
}
