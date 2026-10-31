import 'dart:async';
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
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
import 'package:printing/printing.dart';
import 'package:qr_flutter/qr_flutter.dart';

class DashedLinePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white24
      ..strokeWidth = 1.0;
    const dashWidth = 5.0;
    const dashSpace = 4.0;
    double startX = 0;
    while (startX < size.width) {
      canvas.drawLine(Offset(startX, 0), Offset(startX + dashWidth, 0), paint);
      startX += dashWidth + dashSpace;
    }
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}

class KhqrBannerClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final path = Path();
    const chamferWidth = 28.0;
    const chamferDrop = 14.0;
    final baseHeight = size.height - chamferDrop;

    path.lineTo(0, baseHeight);
    path.lineTo(size.width - chamferWidth, baseHeight);
    path.lineTo(size.width, size.height);
    path.lineTo(size.width, 0);
    path.close();
    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => false;
}

class QRScanPayScreen extends StatefulWidget {
  final int initialTabIndex;
  final QrPayload? initialScannedPayload;

  const QRScanPayScreen({
    super.key,
    this.initialTabIndex = 0,
    this.initialScannedPayload,
  });

  @override
  State<QRScanPayScreen> createState() => QRScanPayScreenState();
}

class QRScanPayScreenState extends State<QRScanPayScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final GlobalKey _khqrCardKey = GlobalKey();

  List<Account> _accounts = [];
  Account? _receiveAccount;
  Account? _khrAccount;
  Account? _usdAccount;
  Account? _payFromAccount;
  String? _myPayload;
  bool _loading = true;

  double? _customAmount;
  Currency _customCurrency = Currency.usd;

  final _payloadController = TextEditingController();
  final _amountController = TextEditingController();
  final _descController = TextEditingController();
  QrPayload? _scanned;
  bool _isPaying = false;
  String? _payErrorMessage;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex.clamp(0, 1),
    );
    _tabController.addListener(() {
      if (mounted) setState(() {});
    });
    _payloadController.addListener(_onPayloadChanged);
    if (widget.initialScannedPayload != null) {
      _payloadController.text = widget.initialScannedPayload!.raw;
    }
    _loadData();
  }

  @override
  void didUpdateWidget(QRScanPayScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.initialTabIndex != widget.initialTabIndex) {
      switchToTab(widget.initialTabIndex);
    }
  }

  void switchToTab(int index) {
    if (index >= 0 && index < _tabController.length) {
      _tabController.animateTo(index);
    }
  }

  Future<void> openScanner() => _openScanner();

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
        for (final a in accounts) {
          if (a.currency == Currency.khr && _khrAccount == null) {
            _khrAccount = a;
          } else if (a.currency == Currency.usd && _usdAccount == null) {
            _usdAccount = a;
          }
        }
        _receiveAccount = _usdAccount ?? accounts.first;
        _payFromAccount = accounts.first;
        await _loadMyPayload();
      }
      if (mounted) setState(() => _loading = false);
    } catch (_) {
      if (mounted) setState(() => _loading = false);
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

  String get _currentQrPayload {
    if (_receiveAccount == null) return 'OBS1:P:900000000001';
    if (_customAmount != null && _customAmount! > 0) {
      return 'OBS1:P:${_receiveAccount!.accountNumber}:${_customCurrency.toJson()}:${_customAmount!.toStringAsFixed(2)}';
    }
    return _myPayload ?? 'OBS1:P:${_receiveAccount!.accountNumber}';
  }

  void _onPayloadChanged() {
    final parsed = QrPayload.tryParse(_payloadController.text);
    if (parsed?.raw == _scanned?.raw) return;
    setState(() {
      _scanned = parsed;
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

  Future<Uint8List?> _captureCardPng() async {
    try {
      final boundary =
          _khqrCardKey.currentContext?.findRenderObject()
              as RenderRepaintBoundary?;
      if (boundary == null) return null;
      final image = await boundary.toImage(pixelRatio: 3.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      return byteData?.buffer.asUint8List();
    } catch (e) {
      debugPrint('Capture error: $e');
      return null;
    }
  }

  Future<void> _downloadKhqrCard() async {
    final bytes = await _captureCardPng();
    if (!mounted) return;
    if (bytes != null) {
      try {
        final dir = Directory.systemTemp;
        final timestamp = DateTime.now().millisecondsSinceEpoch;
        final file = File('${dir.path}/cba_khqr_$timestamp.png');
        await file.writeAsBytes(bytes);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('KHQR Card saved: ${file.path}'),
            backgroundColor: AppTheme.primaryEmerald,
          ),
        );
      } catch (_) {
        // The write failed, so nothing was saved: say so rather than
        // reporting success.
        if (!mounted) return;
        _showDownloadFailed();
      }
    } else {
      if (!mounted) return;
      _showDownloadFailed();
    }
  }

  void _showDownloadFailed() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text("KHQR Card couldn't be saved. Please try again."),
        backgroundColor: AppTheme.primaryRed,
      ),
    );
  }

  Future<void> _shareKhqrCard() async {
    final bytes = await _captureCardPng();
    if (bytes != null) {
      try {
        await Printing.sharePdf(bytes: bytes, filename: 'cba_khqr.png');
      } catch (_) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('KHQR Card image ready to share!'),
            backgroundColor: AppTheme.primaryEmerald,
          ),
        );
      }
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('KHQR Card image copied to share.'),
          backgroundColor: AppTheme.primaryEmerald,
        ),
      );
    }
  }

  void _showEnterAmountDialog() {
    final ctrl = TextEditingController(
      text: _customAmount != null ? _customAmount!.toStringAsFixed(2) : '',
    );
    Currency selectedCurr = _customCurrency;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text(
            'Enter Receive Amount',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Select Currency',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  ChoiceChip(
                    label: const Text('USD (\$)'),
                    selected: selectedCurr == Currency.usd,
                    selectedColor: AppTheme.primaryEmerald,
                    onSelected: (val) =>
                        setModalState(() => selectedCurr = Currency.usd),
                  ),
                  const SizedBox(width: 8),
                  ChoiceChip(
                    label: const Text('KHR (៛)'),
                    selected: selectedCurr == Currency.khr,
                    selectedColor: AppTheme.primaryEmerald,
                    onSelected: (val) =>
                        setModalState(() => selectedCurr = Currency.khr),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              TextField(
                controller: ctrl,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                autofocus: true,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
                decoration: InputDecoration(
                  labelText: 'Amount',
                  prefixText: selectedCurr == Currency.usd ? '\$ ' : '៛ ',
                  hintText: '0.00',
                ),
              ),
            ],
          ),
          actions: [
            if (_customAmount != null)
              TextButton(
                onPressed: () {
                  setState(() {
                    _customAmount = null;
                  });
                  Navigator.pop(ctx);
                },
                child: const Text(
                  'Clear',
                  style: TextStyle(color: AppTheme.primaryRedLight),
                ),
              ),
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text(
                'Cancel',
                style: TextStyle(color: Colors.white60),
              ),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryEmerald,
              ),
              onPressed: () {
                final amt = double.tryParse(ctrl.text.trim());
                if (amt != null && amt > 0) {
                  setState(() {
                    _customAmount = amt;
                    _customCurrency = selectedCurr;
                    if (selectedCurr == Currency.khr && _khrAccount != null) {
                      _receiveAccount = _khrAccount;
                    } else if (selectedCurr == Currency.usd &&
                        _usdAccount != null) {
                      _receiveAccount = _usdAccount;
                    }
                  });
                }
                Navigator.pop(ctx);
              },
              child: const Text(
                'Set Amount',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _selectCurrencyAccount(Currency curr) {
    final matching = _accounts.where((a) => a.currency == curr).toList();
    if (matching.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('No ${curr.toJson()} account available.'),
          backgroundColor: AppTheme.primaryRed,
        ),
      );
      return;
    }

    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surfaceElevated,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Select ${curr.toJson()} Destination Account',
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 12),
            ...matching.map((acc) {
              return ListTile(
                title: Text(
                  '${acc.accountType.name.toUpperCase()} - ${acc.accountNumber}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                subtitle: Text(
                  formatMoney(acc.balance, acc.currency),
                  style: const TextStyle(color: AppTheme.emeraldLight),
                ),
                trailing: const Icon(Icons.check, color: AppTheme.emeraldLight),
                onTap: () {
                  setState(() {
                    if (curr == Currency.khr) {
                      _khrAccount = acc;
                    } else {
                      _usdAccount = acc;
                    }
                    _receiveAccount = acc;
                    _myPayload = null;
                  });
                  _loadMyPayload();
                  Navigator.pop(ctx);
                },
              );
            }),
          ],
        ),
      ),
    );
  }

  void _showPaymentConfirmation() {
    final payload = _scanned;
    final amount = double.tryParse(_amountController.text.trim());

    if (payload == null) {
      setState(
        () => _payErrorMessage =
            'Scan a payment code, or type one starting with OBS1:',
      );
      return;
    }
    if (_payFromAccount == null) {
      setState(
        () => _payErrorMessage = 'Please select a source payment account',
      );
      return;
    }
    if (amount == null || amount <= 0) {
      setState(() => _payErrorMessage = 'Please enter a valid payment amount');
      return;
    }
    if (payload.hasFixedAmount && (amount - payload.amount!).abs() > 0.0001) {
      setState(
        () => _payErrorMessage =
            'This code asks for a fixed amount and cannot be changed',
      );
      return;
    }

    setState(() => _payErrorMessage = null);

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
              'Confirm payment',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Check the destination against the code you scanned before paying.',
              style: TextStyle(fontSize: 11, color: Colors.white54),
            ),
            const SizedBox(height: 16),
            _summaryRow('Paying', payload.destinationLabel),
            _summaryRow('Type', payload.isPersonal ? 'Person' : 'Merchant'),
            _summaryRow(
              'From Account',
              '${_payFromAccount!.accountNumber} (${_payFromAccount!.currency.toJson()})',
            ),
            _summaryRow(
              'Amount',
              formatMoney(amount, _payFromAccount!.currency),
            ),
            if (payload.hasFixedAmount)
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text(
                  'Amount fixed by the payment code',
                  style: TextStyle(fontSize: 11, color: AppTheme.emeraldLight),
                ),
              ),
            if (_descController.text.isNotEmpty)
              _summaryRow('Note', _descController.text),
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
          Text(
            label,
            style: const TextStyle(color: Colors.white60, fontSize: 13),
          ),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
                fontSize: 13,
              ),
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
          description: _descController.text.trim().isNotEmpty
              ? _descController.text.trim()
              : null,
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
      setState(
        () => _payErrorMessage = e.code == 'MERCHANT_DECLINED'
            ? 'The merchant declined this payment. No money left your account.'
            : e.message,
      );
    } catch (_) {
      setState(
        () => _payErrorMessage =
            "Couldn't complete QR payment. Please try again.",
      );
    } finally {
      if (mounted) setState(() => _isPaying = false);
    }
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

    final isScanTab = _tabController.index == 1;

    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: const BackButton(color: Colors.white),
        title: isScanTab
            ? const Text(
                'Scan & Pay',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
              )
            : RichText(
                text: const TextSpan(
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  children: [
                    TextSpan(
                      text: 'CBA',
                      style: TextStyle(color: Colors.white),
                    ),
                    TextSpan(
                      text: "'",
                      style: TextStyle(
                        color: Color(0xFFE5252A),
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    TextSpan(
                      text: ' QR',
                      style: TextStyle(color: Colors.white),
                    ),
                  ],
                ),
              ),
        actions: [
          IconButton(
            key: const ValueKey('toggle_scanner_button'),
            tooltip: 'Open Camera Scanner',
            icon: const Icon(
              Icons.qr_code_scanner_rounded,
              color: AppTheme.emeraldLight,
            ),
            onPressed: () => Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => const QrScannerScreen()),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, color: Colors.white, size: 22),
            onPressed: () => Navigator.maybePop(context),
          ),
        ],
      ),
      body: Column(
        children: [
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
                Tab(text: 'Receive (My QR)'),
                Tab(text: 'Scan & Pay'),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [_buildKhqrReceiveView(), _buildPayTab()],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildKhqrReceiveView() {
    final fullName = SessionManager.instance.fullName.isNotEmpty
        ? SessionManager.instance.fullName.toUpperCase()
        : 'CUSTOMER NAME';

    final amountText = _customAmount != null && _customAmount! > 0
        ? (_customCurrency == Currency.usd
              ? '\$ ${_customAmount!.toStringAsFixed(2)}'
              : '${_customAmount!.toStringAsFixed(0)} KHR')
        : '0';

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
      child: Column(
        children: [
          // Subtitle
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: const [
              Text(
                'Use this QR code to receive money from others',
                style: TextStyle(color: Colors.white70, fontSize: 12),
              ),
              SizedBox(width: 4),
              Icon(Icons.info_outline, color: Colors.white54, size: 14),
            ],
          ),
          const SizedBox(height: 16),

          // The KHQR Card
          RepaintBoundary(
            key: _khqrCardKey,
            child: Container(
              width: double.infinity,
              constraints: const BoxConstraints(maxWidth: 340),
              decoration: BoxDecoration(
                color: const Color(0xFF131B24),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
                boxShadow: const [
                  BoxShadow(
                    color: Colors.black54,
                    blurRadius: 18,
                    offset: Offset(0, 8),
                  ),
                ],
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(22),
                child: Column(
                  children: [
                    // Red top banner with white KHQR emblem
                    ClipPath(
                      clipper: KhqrBannerClipper(),
                      child: Container(
                        height: 52,
                        color: const Color(0xFFE5252A),
                        alignment: const Alignment(0, -0.15),
                        child: const Text(
                          'KHQR',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w900,
                            fontSize: 18,
                            letterSpacing: 3,
                          ),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 20,
                        vertical: 14,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Recipient Name in bold uppercase
                          Text(
                            fullName,
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                              letterSpacing: 0.5,
                            ),
                          ),
                          const SizedBox(height: 6),

                          // Amount display (defaults to 0 or custom set amount)
                          Text(
                            amountText,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 26,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 12),

                          // Dashed divider line
                          CustomPaint(
                            size: const Size(double.infinity, 1),
                            painter: DashedLinePainter(),
                          ),
                          const SizedBox(height: 16),

                          // Centered crisp QR code on white rounded background with center red logo
                          Center(
                            child: Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(16),
                              ),
                              child: Stack(
                                alignment: Alignment.center,
                                children: [
                                  QrImageView(
                                    data: _currentQrPayload,
                                    version: QrVersions.auto,
                                    size: 210.0,
                                    backgroundColor: Colors.white,
                                    padding: EdgeInsets.zero,
                                  ),
                                  Container(
                                    width: 38,
                                    height: 38,
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFE5252A),
                                      shape: BoxShape.circle,
                                      border: Border.all(
                                        color: Colors.white,
                                        width: 2.5,
                                      ),
                                      boxShadow: const [
                                        BoxShadow(
                                          color: Colors.black26,
                                          blurRadius: 4,
                                        ),
                                      ],
                                    ),
                                    child: const Center(
                                      child: Icon(
                                        Icons.account_balance,
                                        color: Colors.white,
                                        size: 18,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 18),

          // + ENTER AMOUNT pill button
          InkWell(
            onTap: _showEnterAmountDialog,
            borderRadius: BorderRadius.circular(24),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 10),
              decoration: BoxDecoration(
                color: Colors.black.withValues(alpha: 0.3),
                borderRadius: BorderRadius.circular(24),
                border: Border.all(color: AppTheme.emeraldLight, width: 1.2),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.add_circle_outline,
                    color: AppTheme.emeraldLight,
                    size: 18,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    _customAmount != null ? 'EDIT AMOUNT' : 'ENTER AMOUNT',
                    style: const TextStyle(
                      color: AppTheme.emeraldLight,
                      fontWeight: FontWeight.bold,
                      fontSize: 13,
                      letterSpacing: 0.8,
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 22),

          // Destination account selectors: Receive KHR to and Receive USD to
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: const [
                        Icon(
                          Icons.info_outline,
                          size: 16,
                          color: Colors.white70,
                        ),
                        SizedBox(width: 8),
                        Text(
                          'Receive KHR to:',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                    InkWell(
                      onTap: () => _selectCurrencyAccount(Currency.khr),
                      child: Row(
                        children: [
                          Text(
                            _khrAccount != null
                                ? _khrAccount!.accountNumber
                                : '600 004 162',
                            style: const TextStyle(
                              color: AppTheme.emeraldLight,
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                            ),
                          ),
                          const SizedBox(width: 4),
                          const Icon(
                            Icons.keyboard_arrow_down,
                            color: AppTheme.emeraldLight,
                            size: 18,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: const [
                        Icon(
                          Icons.attach_money,
                          size: 16,
                          color: Colors.white70,
                        ),
                        SizedBox(width: 8),
                        Text(
                          'Receive USD to:',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                    InkWell(
                      onTap: () => _selectCurrencyAccount(Currency.usd),
                      child: Row(
                        children: [
                          Text(
                            _usdAccount != null
                                ? _usdAccount!.accountNumber
                                : '078 800 449',
                            style: const TextStyle(
                              color: AppTheme.emeraldLight,
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                            ),
                          ),
                          const SizedBox(width: 4),
                          const Icon(
                            Icons.keyboard_arrow_down,
                            color: AppTheme.emeraldLight,
                            size: 18,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),

          // Bottom actions: Download and Share via
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              _bottomActionButton(
                icon: Icons.file_download_outlined,
                label: 'Download',
                onTap: _downloadKhqrCard,
              ),
              const SizedBox(width: 48),
              _bottomActionButton(
                icon: Icons.ios_share_rounded,
                label: 'Share via',
                onTap: _shareKhqrCard,
              ),
            ],
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _bottomActionButton({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.white24),
            ),
            child: Icon(icon, color: Colors.white, size: 24),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Colors.white70,
              fontWeight: FontWeight.w600,
            ),
          ),
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
            title: 'OPEN CAMERA SCANNER',
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
                border: Border.all(
                  color: AppTheme.primaryRedLight.withValues(alpha: 0.5),
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
                      _payErrorMessage!,
                      style: const TextStyle(color: Colors.white, fontSize: 13),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],

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
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 14,
                            color: Colors.white,
                          ),
                        ),
                        Text(
                          payload.hasFixedAmount
                              ? 'Requests ${formatMoney(payload.amount!, payload.currency ?? Currency.usd)}'
                              : 'You choose the amount',
                          style: const TextStyle(
                            fontSize: 12,
                            color: Colors.white60,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Clear',
                    icon: const Icon(
                      Icons.close,
                      color: Colors.white60,
                      size: 18,
                    ),
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

          const Text(
            'Pay From Account',
            style: TextStyle(
              color: Colors.white70,
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 8),
          GlassCard(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: DropdownButtonHideUnderline(
              child: DropdownButton<Account>(
                value: _payFromAccount,
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
                onChanged: (acc) => setState(() => _payFromAccount = acc),
              ),
            ),
          ),
          const SizedBox(height: 18),

          const Text(
            'Payment code',
            style: TextStyle(
              color: Colors.white70,
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
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

          Wrap(
            spacing: 8,
            children: [
              _merchantChip('Angkor Coffee', 'OBS1:M:MERCH-ANGKOR'),
              _merchantChip('Psar Thmei', 'OBS1:M:MERCH-PSAR'),
              _merchantChip('Riverside Books', 'OBS1:M:MERCH-DECLINE'),
            ],
          ),
          const SizedBox(height: 18),

          const Text(
            'Payment Amount',
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
              enabled: !amountLocked,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              style: TextStyle(
                color: amountLocked ? Colors.white70 : Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
              decoration: InputDecoration(
                hintText: '0.00',
                hintStyle: const TextStyle(color: Colors.white30, fontSize: 24),
                prefixText:
                    _payFromAccount != null &&
                        _payFromAccount!.currency == Currency.usd
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
                disabledBorder: InputBorder.none,
              ),
            ),
          ),
          const SizedBox(height: 18),

          const Text(
            'Note (Optional)',
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
      label: Text(
        label,
        style: const TextStyle(fontSize: 11, color: AppTheme.emeraldLight),
      ),
      onPressed: () => _payloadController.text = payload,
    );
  }
}
