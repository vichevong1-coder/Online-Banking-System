import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/bill/data/bill_api.dart';
import 'package:mobile/features/bill/data/bill_models.dart';
import 'package:mobile/features/transfer/presentation/transfer_receipt_screen.dart';

class PayBillScreen extends StatefulWidget {
  final BillProvider provider;

  const PayBillScreen({super.key, required this.provider});

  @override
  State<PayBillScreen> createState() => _PayBillScreenState();
}

class _PayBillScreenState extends State<PayBillScreen> {
  List<Account> _accounts = [];
  Account? _selectedAccount;
  final _billAccountController = TextEditingController();
  final _amountController = TextEditingController();
  bool _isLoading = false;
  bool _fetchingAccounts = true;
  String? _errorMessage;

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
          _selectedAccount = accounts.first;
        }
        _fetchingAccounts = false;
      });
    } catch (_) {
      setState(() => _fetchingAccounts = false);
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

  void _showConfirmation() {
    final billAcc = _billAccountController.text.trim();
    final amountText = _amountController.text.trim();
    final amount = double.tryParse(amountText);

    if (_selectedAccount == null) {
      setState(() => _errorMessage = 'Please select a source account');
      return;
    }
    if (billAcc.isEmpty) {
      setState(() => _errorMessage = 'Please enter your customer/bill account number');
      return;
    }

    if (widget.provider.accountNumberPattern != null && widget.provider.accountNumberPattern!.isNotEmpty) {
      final regex = RegExp(widget.provider.accountNumberPattern!);
      if (!regex.hasMatch(billAcc)) {
        setState(() => _errorMessage = 'Account number must match pattern: ${widget.provider.accountNumberPattern}');
        return;
      }
    }

    if (amount == null || amount <= 0) {
      setState(() => _errorMessage = 'Please enter a valid amount');
      return;
    }

    setState(() => _errorMessage = null);

    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF0A2B24),
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Confirm Bill Payment', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
            const SizedBox(height: 16),
            _summaryRow('Biller', widget.provider.name),
            _summaryRow('Bill Account Number', billAcc),
            _summaryRow('Pay From Account', '${_selectedAccount!.accountNumber} (${_selectedAccount!.currency.toJson()})'),
            _summaryRow('Payment Amount', formatMoney(amount, _selectedAccount!.currency)),
            const SizedBox(height: 24),
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
                    title: 'CONFIRM PAYMENT',
                    onPressed: () {
                      Navigator.pop(ctx);
                      _executePayment(billAcc, amount);
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

  Widget _summaryRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(color: Colors.white60, fontSize: 13)),
          Text(value, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }

  Future<void> _executePayment(String billAcc, double amount) async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final req = PayBillRequest(
        fromAccountId: _selectedAccount!.id,
        providerId: widget.provider.id,
        billAccountNumber: billAcc,
        amount: amount,
        currency: _selectedAccount!.currency.toJson(),
      );

      final payment = await BillApi().payBill(req);

      if (!mounted) return;
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => TransferReceiptScreen(
            transfer: payment.asTransfer(),
            payeeLabel: '${widget.provider.name} ($billAcc)',
          ),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't process bill payment. Please try again.");
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
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(widget.provider.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ),
      body: _fetchingAccounts
          ? const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Provider Header Card
                  GlassCard(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 24,
                          backgroundColor: const Color(0x3300FFB2),
                          child: Icon(_iconForCategory(widget.provider.category), color: AppTheme.emeraldLight, size: 24),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                widget.provider.name,
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Category: ${widget.provider.category}',
                                style: const TextStyle(color: Colors.white60, fontSize: 12),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  if (_errorMessage != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryRed.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppTheme.primaryRedLight.withValues(alpha: 0.5)),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 20),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              _errorMessage!,
                              style: const TextStyle(color: Colors.white, fontSize: 13),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],

                  // Pay From Account
                  const Text('Pay From Account', style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<Account>(
                        value: _selectedAccount,
                        isExpanded: true,
                        dropdownColor: const Color(0xFF0A2B24),
                        items: _accounts.map((a) {
                          return DropdownMenuItem(
                            value: a,
                            child: Text(
                              '${a.accountNumber} (${a.currency.toJson()}) - ${formatMoney(a.balance, a.currency)}',
                              style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                            ),
                          );
                        }).toList(),
                        onChanged: (acc) => setState(() => _selectedAccount = acc),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Bill Account Number
                  const Text('Customer / Bill Account Number', style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _billAccountController,
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                      decoration: InputDecoration(
                        hintText: widget.provider.accountNumberPattern != null
                            ? 'Pattern: ${widget.provider.accountNumberPattern}'
                            : 'e.g. 10293847',
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Amount
                  const Text('Payment Amount', style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _amountController,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                      decoration: InputDecoration(
                        hintText: '0.00',
                        hintStyle: const TextStyle(color: Colors.white30, fontSize: 24),
                        prefixText: _selectedAccount != null && _selectedAccount!.currency == Currency.usd ? '\$ ' : 'KHR ',
                        prefixStyle: const TextStyle(color: AppTheme.emeraldLight, fontSize: 24, fontWeight: FontWeight.bold),
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  PrimaryActionButton(
                    title: 'REVIEW PAYMENT',
                    isLoading: _isLoading,
                    onPressed: _showConfirmation,
                  ),
                ],
              ),
            ),
    );
  }
}
