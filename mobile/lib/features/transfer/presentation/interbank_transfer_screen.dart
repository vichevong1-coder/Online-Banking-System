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

/// US-026. The destination is a bank code plus an account number — the backend
/// holds no row for the receiving account, so there is no name to look up and
/// none is sent. A name belongs to a beneficiary (US-029), which this screen can
/// create on the way through.
class InterbankTransferScreen extends StatefulWidget {
  final Beneficiary? prefilledBeneficiary;

  const InterbankTransferScreen({super.key, this.prefilledBeneficiary});

  @override
  State<InterbankTransferScreen> createState() => _InterbankTransferScreenState();
}

class _InterbankTransferScreenState extends State<InterbankTransferScreen> {
  // Mirrors CreateExternalTransferRequest's @Pattern("^[A-Za-z0-9]{6,34}$").
  static const _minAccountNumberLength = 6;
  static const _maxAccountNumberLength = 34;

  List<Account> _accounts = [];
  Account? _fromAccount;
  String _selectedBank = DestinationBank.all.first.code;
  final _accountNumberController = TextEditingController();
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  final _beneficiaryNameController = TextEditingController();
  bool _saveAsBeneficiary = false;
  bool _isLoading = false;
  bool _fetchingAccounts = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    final b = widget.prefilledBeneficiary;
    if (b != null) {
      _selectedBank = DestinationBank.all.any((bank) => bank.code == b.bankCode)
          ? b.bankCode
          : DestinationBank.all.first.code;
      _accountNumberController.text = b.accountNumber;
    }
    _fetchAccounts();
  }

  @override
  void dispose() {
    _accountNumberController.dispose();
    _amountController.dispose();
    _descController.dispose();
    _beneficiaryNameController.dispose();
    super.dispose();
  }

  Future<void> _fetchAccounts() async {
    try {
      final accounts = await AccountApi().listAccounts();
      setState(() {
        _accounts = accounts;
        if (accounts.isNotEmpty) {
          _fromAccount = accounts.first;
        }
        _fetchingAccounts = false;
      });
    } catch (_) {
      setState(() => _fetchingAccounts = false);
    }
  }

  /// The payee label on this screen: the saved beneficiary's name when we came
  /// from one, otherwise just the bank. Never a name we did not verify.
  String get _destinationLabel =>
      widget.prefilledBeneficiary?.displayName ?? DestinationBank.nameFor(_selectedBank);

  void _showConfirmation() {
    final amount = double.tryParse(_amountController.text.trim());
    final accNum = _accountNumberController.text.trim();

    if (_fromAccount == null) {
      setState(() => _errorMessage = 'Please select a source account');
      return;
    }
    if (accNum.length < _minAccountNumberLength || accNum.length > _maxAccountNumberLength) {
      setState(() => _errorMessage =
          'Account number must be $_minAccountNumberLength–$_maxAccountNumberLength letters or digits');
      return;
    }
    if (amount == null || amount <= 0) {
      setState(() => _errorMessage = 'Please enter a valid transfer amount');
      return;
    }
    if (_saveAsBeneficiary && _beneficiaryNameController.text.trim().isEmpty) {
      setState(() => _errorMessage = 'Give this favorite a name, or turn off saving');
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
            const Text('Confirm Interbank Transfer',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
            const SizedBox(height: 6),
            const Text(
              'Settlement at the receiving bank is simulated — the transfer lands as pending.',
              style: TextStyle(fontSize: 11, color: Colors.white54),
            ),
            const SizedBox(height: 16),
            _summaryRow('From Account', '${_fromAccount!.accountNumber} (${_fromAccount!.currency.toJson()})'),
            _summaryRow('To', _destinationLabel),
            _summaryRow('Destination Bank', '${DestinationBank.nameFor(_selectedBank)} ($_selectedBank)'),
            _summaryRow('Account Number', accNum),
            _summaryRow('Transfer Amount', formatMoney(amount, _fromAccount!.currency)),
            if (_descController.text.isNotEmpty) _summaryRow('Description', _descController.text),
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
      final transfer = await TransferApi().interbankTransfer(
        InterbankTransferRequest(
          fromAccountId: _fromAccount!.id,
          beneficiaryBankCode: _selectedBank,
          beneficiaryAccountNumber: accNum,
          currency: _fromAccount!.currency,
          amount: amount,
          description: _descController.text.trim().isNotEmpty ? _descController.text.trim() : null,
        ),
      );

      // Saving the payee is a convenience, not part of the transfer: a failure
      // here must not make a settled transfer look failed.
      if (_saveAsBeneficiary) {
        try {
          await TransferApi().createBeneficiary(
            CreateBeneficiaryRequest(
              displayName: _beneficiaryNameController.text.trim(),
              bankCode: _selectedBank,
              accountNumber: accNum,
            ),
          );
        } catch (_) {}
      }

      if (!mounted) return;
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => TransferReceiptScreen(transfer: transfer, payeeLabel: _destinationLabel),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't complete interbank transfer. Please try again.");
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
        title: const Text('Interbank Transfer', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ),
      body: _fetchingAccounts
          ? const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight))
          : SingleChildScrollView(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (widget.prefilledBeneficiary != null) ...[
                    GlassCard(
                      padding: const EdgeInsets.all(14),
                      child: Row(
                        children: [
                          const CircleAvatar(
                            backgroundColor: Color(0x3300FFB2),
                            child: Icon(Icons.person, color: AppTheme.emeraldLight, size: 20),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  widget.prefilledBeneficiary!.displayName,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold, fontSize: 14, color: Colors.white),
                                ),
                                Text(
                                  '${DestinationBank.nameFor(widget.prefilledBeneficiary!.bankCode)} • ${widget.prefilledBeneficiary!.accountNumber}',
                                  style: const TextStyle(fontSize: 12, color: Colors.white60),
                                ),
                              ],
                            ),
                          ),
                          const Text('Favorite', style: TextStyle(fontSize: 11, color: AppTheme.emeraldLight)),
                        ],
                      ),
                    ),
                    const SizedBox(height: 20),
                  ],

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

                  // From Account
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

                  // Destination Bank
                  const Text('Destination Bank',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<String>(
                        value: _selectedBank,
                        isExpanded: true,
                        dropdownColor: const Color(0xFF0A2B24),
                        items: DestinationBank.all.map((b) {
                          return DropdownMenuItem(
                            value: b.code,
                            child: Text(
                              '${b.name} (${b.code})',
                              style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                            ),
                          );
                        }).toList(),
                        onChanged: (bank) =>
                            setState(() => _selectedBank = bank ?? DestinationBank.all.first.code),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Account Number
                  const Text('Recipient Account Number',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _accountNumberController,
                      keyboardType: TextInputType.text,
                      maxLength: _maxAccountNumberLength,
                      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9]'))],
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                      decoration: const InputDecoration(
                        hintText: 'e.g. 000123456789',
                        counterText: '',
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Amount
                  const Text('Transfer Amount',
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

                  // Note / Description
                  const Text('Note / Remark (Optional)',
                      style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  GlassCard(
                    child: TextField(
                      controller: _descController,
                      style: const TextStyle(color: Colors.white),
                      decoration: const InputDecoration(
                        hintText: 'e.g. Dinner share',
                        filled: false,
                        border: InputBorder.none,
                        enabledBorder: InputBorder.none,
                        focusedBorder: InputBorder.none,
                      ),
                    ),
                  ),

                  // US-029 from inside the transfer flow — the only place the
                  // app has a name for a destination it cannot look up.
                  if (widget.prefilledBeneficiary == null) ...[
                    const SizedBox(height: 12),
                    GlassCard(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: Column(
                        children: [
                          SwitchListTile(
                            contentPadding: EdgeInsets.zero,
                            dense: true,
                            title: const Text('Save as favorite',
                                style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                            value: _saveAsBeneficiary,
                            activeThumbColor: AppTheme.emeraldLight,
                            onChanged: (v) => setState(() => _saveAsBeneficiary = v),
                          ),
                          if (_saveAsBeneficiary)
                            TextField(
                              controller: _beneficiaryNameController,
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                              decoration: const InputDecoration(
                                hintText: 'What do you call this payee?',
                                filled: false,
                                border: InputBorder.none,
                                enabledBorder: InputBorder.none,
                                focusedBorder: InputBorder.none,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 28),

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
