import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/transfer/data/transfer_api.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';
import 'package:mobile/features/transfer/presentation/transfer_receipt_screen.dart';

/// US-026, same-bank half: send money to another customer of this bank by
/// account number. Settles instantly — both legs are ours — which is what
/// separates it from the interbank screen next door.
class P2pTransferScreen extends StatefulWidget {
  const P2pTransferScreen({super.key});

  @override
  State<P2pTransferScreen> createState() => _P2pTransferScreenState();
}

class _P2pTransferScreenState extends State<P2pTransferScreen> {
  // Mirrors CreateP2pTransferRequest's @Pattern("^[0-9]{6,20}$").
  static const _minAccountNumberLength = 6;
  static const _maxAccountNumberLength = 20;

  List<Account> _accounts = [];
  Account? _fromAccount;
  final _accountNumberController = TextEditingController();
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  bool _isLoading = false;
  bool _fetchingAccounts = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _accountNumberController.addListener(() => setState(() {}));
    _fetchAccounts();
  }

  @override
  void dispose() {
    _accountNumberController.dispose();
    _amountController.dispose();
    _descController.dispose();
    super.dispose();
  }

  Future<void> _fetchAccounts() async {
    try {
      final accounts = await AccountApi().listAccounts();
      setState(() {
        _accounts = accounts;
        if (accounts.isNotEmpty) _fromAccount = accounts.first;
        _fetchingAccounts = false;
      });
    } catch (_) {
      setState(() => _fetchingAccounts = false);
    }
  }

  void _showConfirmation() {
    final amount = double.tryParse(_amountController.text.trim());
    final accNum = _accountNumberController.text.trim();

    if (_fromAccount == null) {
      setState(() => _errorMessage = 'Please select a source account');
      return;
    }
    if (accNum.length < _minAccountNumberLength || accNum.length > _maxAccountNumberLength) {
      setState(() => _errorMessage =
          "Enter the recipient's account number ($_minAccountNumberLength–$_maxAccountNumberLength digits)");
      return;
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
            const Text('Confirm payment',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
            const SizedBox(height: 6),
            // The bank does not hand back the holder's name, so the number the
            // payer typed is the only thing there is to check. Say so plainly
            // rather than implying a name was verified.
            const Text(
              'Check the account number with the person you are paying — this cannot be reversed.',
              style: TextStyle(fontSize: 11, color: Colors.white54),
            ),
            const SizedBox(height: 16),
            _summaryRow('To account', accNum),
            _summaryRow('From Account', '${_fromAccount!.accountNumber} (${_fromAccount!.currency.toJson()})'),
            _summaryRow('Amount', formatMoney(amount, _fromAccount!.currency)),
            if (_descController.text.isNotEmpty) _summaryRow('Note', _descController.text),
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
                    title: 'SEND MONEY',
                    onPressed: () {
                      Navigator.pop(ctx);
                      _executeTransfer(amount, accNum);
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
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _executeTransfer(double amount, String accNum) async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final transfer = await TransferApi().p2pTransfer(
        P2pTransferRequest(
          fromAccountId: _fromAccount!.id,
          toAccountNumber: accNum,
          amount: amount,
          description: _descController.text.trim().isNotEmpty ? _descController.text.trim() : null,
        ),
      );

      if (!mounted) return;
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => TransferReceiptScreen(transfer: transfer, payeeLabel: 'Account $accNum'),
        ),
      );
    } on ApiException catch (e) {
      // An unknown number and someone else's source account both come back as
      // ACCOUNT_NOT_FOUND — deliberately indistinguishable on the server, so
      // the wording here cannot claim to know which it was.
      setState(() => _errorMessage = e.code == 'ACCOUNT_NOT_FOUND'
          ? "That account number doesn't match an account at this bank."
          : e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't complete the transfer. Please try again.");
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
        title: const Text('Send to Someone Else', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ),
      body: _fetchingAccounts
          ? const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight))
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
                        border: Border.all(color: AppTheme.primaryRedLight.withValues(alpha: 0.5)),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 20),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(_errorMessage!,
                                style: const TextStyle(color: Colors.white, fontSize: 13)),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],

                  const Text('From Account',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<Account>(
                        value: _fromAccount,
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
                        onChanged: (acc) => setState(() => _fromAccount = acc),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  const Text("Recipient's Account Number",
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _accountNumberController,
                            keyboardType: TextInputType.number,
                            maxLength: _maxAccountNumberLength,
                            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                            style: const TextStyle(
                                color: Colors.white, fontWeight: FontWeight.w600, letterSpacing: 1.1),
                            decoration: const InputDecoration(
                              hintText: 'e.g. 900000000002',
                              counterText: '',
                              filled: false,
                              border: InputBorder.none,
                              enabledBorder: InputBorder.none,
                              focusedBorder: InputBorder.none,
                            ),
                          ),
                        ),
                        if (_accountNumberController.text.trim().length >= _minAccountNumberLength)
                          const Icon(Icons.check_circle, color: AppTheme.emeraldLight, size: 18),
                      ],
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Only accounts at this bank. To pay another bank, use an interbank transfer.',
                    style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.45)),
                  ),
                  const SizedBox(height: 20),

                  const Text('Amount',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _amountController,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold),
                      decoration: InputDecoration(
                        hintText: '0.00',
                        hintStyle: const TextStyle(color: Colors.white30, fontSize: 24),
                        prefixText:
                            _fromAccount != null && _fromAccount!.currency == Currency.usd ? '\$ ' : 'KHR ',
                        prefixStyle: const TextStyle(
                            color: AppTheme.emeraldLight, fontSize: 24, fontWeight: FontWeight.bold),
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  const Text('Note (Optional)',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _descController,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        hintText: 'e.g. Split dinner',
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
                  const SizedBox(height: 12),
                ],
              ),
            ),
    );
  }
}
