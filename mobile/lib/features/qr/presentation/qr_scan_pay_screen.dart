import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/qr/data/qr_api.dart';
import 'package:mobile/features/qr/data/qr_models.dart';
import 'package:mobile/features/qr/data/qr_payload.dart';
import 'package:mobile/features/qr/presentation/qr_scanner_screen.dart';
import 'package:mobile/features/transfer/presentation/transfer_receipt_screen.dart';
import 'package:qr_flutter/qr_flutter.dart';

/// US-032 (receive) and US-033/US-034 (scan & pay). The camera lives in a
/// pushed [QrScannerScreen]; this screen owns the source account, the amount
/// rules and the confirmation the QR spec requires before any money moves.
class QRScanPayScreen extends StatefulWidget {
  const QRScanPayScreen({super.key});

  @override
  State<QRScanPayScreen> createState() => _QRScanPayScreenState();
}

class _QRScanPayScreenState extends State<QRScanPayScreen> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  List<Account> _accounts = [];
  Account? _receiveAccount;
  Account? _payFromAccount;
  String? _myPayload;
  bool _loading = true;

  final _payloadController = TextEditingController();
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  QrPayload? _scanned;
  bool _isPaying = false;
  String? _payErrorMessage;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _payloadController.addListener(_onPayloadChanged);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    _payloadController.dispose();
    _amountController.dispose();
    _descController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    try {
      final accounts = await AccountApi().listAccounts();
      if (accounts.isNotEmpty) {
        _accounts = accounts;
        _receiveAccount = accounts.first;
        _payFromAccount = accounts.first;
        await _loadMyPayload();
      }
      setState(() => _loading = false);
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _loadMyPayload() async {
    if (_receiveAccount == null) return;
    try {
      final res = await QrApi().getMyPayload(_receiveAccount!.id);
      if (!mounted) return;
      setState(() => _myPayload = res.payload);
    } catch (_) {
      if (!mounted) return;
      setState(() => _myPayload = null);
    }
  }

  void _onPayloadChanged() {
    final parsed = QrPayload.tryParse(_payloadController.text);
    if (parsed?.raw == _scanned?.raw) return;
    setState(() {
      _scanned = parsed;
      // A payload that fixes its own price owns the amount field: sending a
      // different one is a 400 QR_AMOUNT_MISMATCH.
      if (parsed?.hasFixedAmount ?? false) {
        _amountController.text = parsed!.amount!.toStringAsFixed(2);
      }
    });
  }

  Future<void> _openScanner() async {
    final result = await Navigator.push<QrPayload>(
      context,
      MaterialPageRoute(builder: (_) => const QrScannerScreen()),
    );
    if (result == null || !mounted) return;
    setState(() {
      _payErrorMessage = null;
      _payloadController.text = result.raw;
    });
  }

  void _showPaymentConfirmation() {
    final payload = _scanned;
    final amount = double.tryParse(_amountController.text.trim());

    if (payload == null) {
      setState(() => _payErrorMessage = 'Scan a payment code, or type one starting with OBS1:');
      return;
    }
    if (_payFromAccount == null) {
      setState(() => _payErrorMessage = 'Please select a source payment account');
      return;
    }
    if (amount == null || amount <= 0) {
      setState(() => _payErrorMessage = 'Please enter a valid payment amount');
      return;
    }
    if (payload.hasFixedAmount && (amount - payload.amount!).abs() > 0.0001) {
      setState(() => _payErrorMessage = 'This code asks for a fixed amount and cannot be changed');
      return;
    }

    setState(() => _payErrorMessage = null);

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
            const Text(
              'Check the destination against the code you scanned before paying.',
              style: TextStyle(fontSize: 11, color: Colors.white54),
            ),
            const SizedBox(height: 16),
            _summaryRow('Paying', payload.destinationLabel),
            _summaryRow('Type', payload.isPersonal ? 'Person' : 'Merchant'),
            _summaryRow('From Account',
                '${_payFromAccount!.accountNumber} (${_payFromAccount!.currency.toJson()})'),
            _summaryRow('Amount', formatMoney(amount, _payFromAccount!.currency)),
            if (payload.hasFixedAmount)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text('Amount fixed by the payment code',
                    style: TextStyle(fontSize: 11, color: AppTheme.emeraldLight)),
              ),
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
                    title: 'CONFIRM & PAY',
                    onPressed: () {
                      Navigator.pop(ctx);
                      _executeQrPay(payload, amount);
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

  Future<void> _executeQrPay(QrPayload payload, double amount) async {
    setState(() {
      _isPaying = true;
      _payErrorMessage = null;
    });

    try {
      final transfer = await QrApi().payQr(
        QrPayRequest(
          payload: payload.raw,
          fromAccountId: _payFromAccount!.id,
          amount: amount,
          currency: _payFromAccount!.currency.toJson(),
          description: _descController.text.trim().isNotEmpty ? _descController.text.trim() : null,
        ),
      );

      if (!mounted) return;
      _payloadController.clear();
      _amountController.clear();
      _descController.clear();
      setState(() => _scanned = null);

      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TransferReceiptScreen(
            transfer: transfer,
            payeeLabel: payload.destinationLabel,
          ),
        ),
      );
    } on ApiException catch (e) {
      // US-034's declining merchant is a 400 with a FAILED transfer behind it,
      // not a network problem — name it as a decline.
      setState(() => _payErrorMessage = e.code == 'MERCHANT_DECLINED'
          ? 'The merchant declined this payment. No money left your account.'
          : e.message);
    } catch (_) {
      setState(() => _payErrorMessage = "Couldn't complete QR payment. Please try again.");
    } finally {
      if (mounted) setState(() => _isPaying = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight));
    }

    return Column(
      children: [
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
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
              Tab(text: 'Receive (My QR)'),
              Tab(text: 'Scan & Pay'),
            ],
          ),
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [_buildReceiveTab(), _buildPayTab()],
          ),
        ),
      ],
    );
  }

  Widget _buildReceiveTab() {
    if (_receiveAccount == null) {
      return const Center(
        child: Text('Open an account to receive payments', style: TextStyle(color: Colors.white70)),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: Column(
        children: [
          const Text('My KHQR Code',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.white)),
          const SizedBox(height: 6),
          const Text('Show this QR code to receive funds',
              style: TextStyle(color: Colors.white70, fontSize: 12)),
          const SizedBox(height: 24),
          GlassCard(
            borderRadius: 24,
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: _myPayload == null
                      ? const SizedBox(
                          width: 200,
                          height: 200,
                          child: Center(child: CircularProgressIndicator(color: AppTheme.primaryEmerald)),
                        )
                      : QrImageView(
                          data: _myPayload!,
                          version: QrVersions.auto,
                          size: 200.0,
                          backgroundColor: Colors.white,
                        ),
                ),
                const SizedBox(height: 18),
                Text(
                  SessionManager.instance.fullName.toUpperCase(),
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                ),
                const SizedBox(height: 4),
                Text(
                  '${_receiveAccount!.accountNumber} (${_receiveAccount!.currency.toJson()})',
                  style: const TextStyle(color: AppTheme.emeraldLight, fontSize: 13, fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          if (_accounts.length > 1)
            GlassCard(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<Account>(
                  value: _receiveAccount,
                  isExpanded: true,
                  dropdownColor: const Color(0xFF0A2B24),
                  items: _accounts.map((a) {
                    return DropdownMenuItem(
                      value: a,
                      child: Text(
                        'Receive to: ${a.accountNumber} (${a.currency.toJson()})',
                        style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                      ),
                    );
                  }).toList(),
                  onChanged: (acc) {
                    if (acc == null) return;
                    setState(() {
                      _receiveAccount = acc;
                      _myPayload = null;
                    });
                    _loadMyPayload();
                  },
                ),
              ),
            ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildPayTab() {
    final payload = _scanned;
    final amountLocked = payload?.hasFixedAmount ?? false;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PrimaryActionButton(
            title: 'SCAN A QR CODE',
            icon: Icons.qr_code_scanner_rounded,
            onPressed: _openScanner,
          ),
          const SizedBox(height: 18),

          if (_payErrorMessage != null) ...[
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
                    child: Text(_payErrorMessage!, style: const TextStyle(color: Colors.white, fontSize: 13)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],

          // Resolved destination — everything the client can honestly say about
          // the code without a server round-trip.
          if (payload != null) ...[
            GlassCard(
              padding: const EdgeInsets.all(14),
              child: Row(
                children: [
                  CircleAvatar(
                    backgroundColor: const Color(0x3300FFB2),
                    child: Icon(
                      payload.isPersonal ? Icons.person : Icons.storefront,
                      color: AppTheme.emeraldLight,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          payload.destinationLabel,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: Colors.white),
                        ),
                        Text(
                          payload.hasFixedAmount
                              ? 'Requests ${formatMoney(payload.amount!, payload.currency ?? Currency.usd)}'
                              : 'You choose the amount',
                          style: const TextStyle(fontSize: 12, color: Colors.white60),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Clear',
                    icon: const Icon(Icons.close, color: Colors.white60, size: 18),
                    onPressed: () {
                      _payloadController.clear();
                      _amountController.clear();
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
          ],

          const Text('Pay From Account',
              style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: DropdownButtonHideUnderline(
              child: DropdownButton<Account>(
                value: _payFromAccount,
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
                onChanged: (acc) => setState(() => _payFromAccount = acc),
              ),
            ),
          ),
          const SizedBox(height: 18),

          const Text('Payment code',
              style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          GlassCard(
            child: TextField(
              controller: _payloadController,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                hintText: 'OBS1:P:900000000001',
                filled: false,
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 8),

          // Seeded demo merchants (US-034) — the third one always declines.
          Wrap(
            spacing: 8,
            children: [
              _merchantChip('Angkor Coffee', 'OBS1:M:MERCH-ANGKOR'),
              _merchantChip('Psar Thmei', 'OBS1:M:MERCH-PSAR'),
              _merchantChip('Riverside Books', 'OBS1:M:MERCH-DECLINE'),
            ],
          ),
          const SizedBox(height: 18),

          const Text('Payment Amount',
              style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          GlassCard(
            child: TextField(
              controller: _amountController,
              enabled: !amountLocked,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              style: TextStyle(
                color: amountLocked ? Colors.white70 : Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
              decoration: InputDecoration(
                hintText: '0.00',
                hintStyle: const TextStyle(color: Colors.white30, fontSize: 24),
                prefixText: _payFromAccount != null && _payFromAccount!.currency == Currency.usd ? '\$ ' : 'KHR ',
                prefixStyle: const TextStyle(
                    color: AppTheme.emeraldLight, fontSize: 24, fontWeight: FontWeight.bold),
                filled: false,
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                disabledBorder: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 18),

          const Text('Note (Optional)',
              style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          GlassCard(
            child: TextField(
              controller: _descController,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                hintText: 'e.g. Grocery purchase',
                filled: false,
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 28),

          PrimaryActionButton(
            title: 'REVIEW & PAY',
            isLoading: _isPaying,
            onPressed: _showPaymentConfirmation,
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _merchantChip(String label, String payload) {
    return ActionChip(
      backgroundColor: const Color(0x3300FFB2),
      label: Text(label, style: const TextStyle(fontSize: 11, color: AppTheme.emeraldLight)),
      onPressed: () => _payloadController.text = payload,
    );
  }
}
