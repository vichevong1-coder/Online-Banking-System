import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/transaction_history_screen.dart';
import 'package:mobile/features/card/data/card_api.dart';
import 'package:mobile/features/card/data/card_models.dart';
import 'package:mobile/features/transfer/presentation/transfers_tab_screen.dart';
import 'package:mobile/core/widgets/app_state_view.dart';

class CardsTabScreen extends StatefulWidget {
  const CardsTabScreen({super.key});

  @override
  State<CardsTabScreen> createState() => _CardsTabScreenState();
}

class _CardsTabScreenState extends State<CardsTabScreen> {
  List<CardModel> _cards = [];
  List<Account> _accounts = [];
  CardModel? _selectedCard;
  bool _loading = true;
  String? _error;
  bool _showCvv = false;

  final Map<String, String> _cardNicknames = {};
  final Map<String, int> _cardThemes = {}; // index of theme

  static const List<List<Color>> _themes = [
    [Color(0xFF00B27A), Color(0xFF042D26)], // Emerald
    [Color(0xFF1E3A8A), Color(0xFF0F172A)], // Royal Blue
    [Color(0xFF701A75), Color(0xFF2E1065)], // Midnight Violet
    [Color(0xFF991B1B), Color(0xFF450A0A)], // Ruby Dark
    [Color(0xFF334155), Color(0xFF0F172A)], // Slate Platinum
  ];

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
      final cards = await CardApi().listCards();
      final accounts = await AccountApi().listAccounts();
      setState(() {
        _cards = cards;
        _accounts = accounts;
        if (cards.isNotEmpty) {
          _selectedCard = cards.first;
        }
        _loading = false;
      });
    } catch (_) {
      setState(() {
        _error = "Couldn't load cards.";
        _loading = false;
      });
    }
  }

  // Server messages are written for customers, so show them as-is. Anything
  // else (network, parsing) gets a plain fallback instead of an exception dump.
  String _friendlyError(Object e, String fallback) =>
      e is ApiException ? e.message : fallback;

  Future<void> _toggleFreeze(bool freeze) async {
    if (_selectedCard == null) return;
    try {
      final updated = freeze
          ? await CardApi().blockCard(_selectedCard!.id)
          : await CardApi().unblockCard(_selectedCard!.id);

      setState(() {
        _selectedCard = updated;
        _cards = _cards.map((c) => c.id == updated.id ? updated : c).toList();
      });

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            freeze ? 'Card has been frozen.' : 'Card is now active.',
          ),
          backgroundColor: freeze
              ? AppTheme.primaryRed
              : AppTheme.primaryEmerald,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            _friendlyError(e, "Couldn't update the card. Please try again."),
          ),
          backgroundColor: AppTheme.primaryRed,
        ),
      );
    }
  }

  String _getCvvForCard(CardModel card) {
    final hash = (card.id.hashCode.abs() % 900 + 100);
    return hash.toString();
  }

  LinearGradient _getCardGradient(bool isFrozen) {
    if (isFrozen) {
      return const LinearGradient(
        colors: [Color(0xFF2C3E50), Color(0xFF1A252F)],
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
      );
    }
    final themeIdx = _cardThemes[_selectedCard?.id] ?? 0;
    final colors = _themes[themeIdx % _themes.length];
    return LinearGradient(
      colors: colors,
      begin: Alignment.topLeft,
      end: Alignment.bottomRight,
    );
  }

  void _openLinkedAccountHistory() {
    if (_selectedCard == null) return;
    Account? linked;
    for (final acc in _accounts) {
      if (acc.id == _selectedCard!.accountId ||
          acc.accountNumber == _selectedCard!.accountNumber) {
        linked = acc;
        break;
      }
    }
    linked ??= Account(
      id: _selectedCard!.accountId,
      accountNumber: _selectedCard!.accountNumber.isNotEmpty
          ? _selectedCard!.accountNumber
          : '900000000001',
      accountType: AccountType.checking,
      currency: Currency.usd,
      balance: 0.0,
      createdAt: DateTime.now(),
    );

    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => TransactionHistoryScreen(account: linked!),
      ),
    );
  }

  void _showPersonalizeDialog() {
    if (_selectedCard == null) return;
    final nameCtrl = TextEditingController(
      text: _cardNicknames[_selectedCard!.id] ?? 'CBA Platinum Debit',
    );
    int selectedTheme = _cardThemes[_selectedCard!.id] ?? 0;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text(
            'Personalize Card',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Card Nickname',
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: nameCtrl,
                  style: const TextStyle(color: Colors.white),
                  decoration: const InputDecoration(
                    hintText: 'e.g. My Daily Spending Card',
                  ),
                ),
                const SizedBox(height: 18),
                const Text(
                  'Card Theme',
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: List.generate(_themes.length, (idx) {
                    final isCurrent = selectedTheme == idx;
                    return GestureDetector(
                      onTap: () => setModalState(() => selectedTheme = idx),
                      child: Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(10),
                          gradient: LinearGradient(
                            colors: _themes[idx],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          border: Border.all(
                            color: isCurrent ? Colors.white : Colors.white24,
                            width: isCurrent ? 2.5 : 1,
                          ),
                        ),
                        child: isCurrent
                            ? const Icon(
                                Icons.check,
                                color: Colors.white,
                                size: 20,
                              )
                            : null,
                      ),
                    );
                  }),
                ),
              ],
            ),
          ),
          actions: [
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
                final nick = nameCtrl.text.trim();
                setState(() {
                  if (nick.isNotEmpty) {
                    _cardNicknames[_selectedCard!.id] = nick;
                  }
                  _cardThemes[_selectedCard!.id] = selectedTheme;
                });
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Card personalization saved!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              },
              child: const Text(
                'Save',
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

  void _showChangeLinkedAccountDialog() {
    if (_selectedCard == null || _accounts.isEmpty) return;
    Account selected = _accounts.firstWhere(
      (a) => a.id == _selectedCard!.accountId,
      orElse: () => _accounts.first,
    );

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text(
            'Change Linked Account',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Select the primary account to link this card for ATM and POS transactions:',
                style: TextStyle(color: Colors.white70, fontSize: 13),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<Account>(
                initialValue: selected,
                dropdownColor: AppTheme.surfaceElevated,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(labelText: 'Primary Account'),
                items: _accounts.map((a) {
                  return DropdownMenuItem(
                    value: a,
                    child: Text(
                      '${a.accountType.name.toUpperCase()} - ${a.accountNumber} (${a.currency.toJson()})',
                    ),
                  );
                }).toList(),
                onChanged: (a) => setModalState(() => selected = a!),
              ),
            ],
          ),
          actions: [
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
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(
                      'Linked account updated to ${selected.accountNumber}',
                    ),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              },
              child: const Text(
                'Confirm',
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

  void _showRenameCardDialog() {
    if (_selectedCard == null) return;
    final ctrl = TextEditingController(
      text: _cardNicknames[_selectedCard!.id] ?? 'CBA Platinum Debit',
    );

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surfaceElevated,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Rename Card',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
        content: TextField(
          controller: ctrl,
          style: const TextStyle(color: Colors.white),
          decoration: const InputDecoration(labelText: 'Card Nickname'),
        ),
        actions: [
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
              final val = ctrl.text.trim();
              if (val.isNotEmpty) {
                setState(() => _cardNicknames[_selectedCard!.id] = val);
              }
              Navigator.pop(ctx);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Card renamed successfully!'),
                  backgroundColor: AppTheme.primaryEmerald,
                ),
              );
            },
            child: const Text(
              'Rename',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showCloseCardDialog() {
    if (_selectedCard == null) return;

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surfaceElevated,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Close Card',
          style: TextStyle(
            color: AppTheme.primaryRedLight,
            fontWeight: FontWeight.bold,
          ),
        ),
        content: Text(
          'Are you sure you want to permanently close card ${_selectedCard!.cardNumberMasked}? This action will permanently deactivate the card.',
          style: const TextStyle(color: Colors.white70, fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text(
              'Keep Card',
              style: TextStyle(color: Colors.white60),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.primaryRed,
            ),
            onPressed: () async {
              Navigator.pop(ctx);
              try {
                await CardApi().blockCard(_selectedCard!.id);
                setState(() {
                  _cards.removeWhere((c) => c.id == _selectedCard!.id);
                  _selectedCard = _cards.isNotEmpty ? _cards.first : null;
                });
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Card has been permanently closed.'),
                    backgroundColor: AppTheme.primaryRed,
                  ),
                );
              } catch (_) {
                if (!mounted) return;
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Failed to close card.'),
                    backgroundColor: AppTheme.primaryRed,
                  ),
                );
              }
            },
            child: const Text(
              'Close Card',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showModifyLimitsDialog() {
    if (_selectedCard == null) return;
    final dailyCtrl = TextEditingController(
      text: _selectedCard!.dailyLimit.toStringAsFixed(2),
    );
    final txCtrl = TextEditingController(
      text: _selectedCard!.perTransactionLimit.toStringAsFixed(2),
    );
    final messenger = ScaffoldMessenger.of(context);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surfaceElevated,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text(
          'Modify Spending Limits',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: dailyCtrl,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Daily Spending Limit (\$)',
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: txCtrl,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Per-Transaction Limit (\$)',
              ),
            ),
          ],
        ),
        actions: [
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
            onPressed: () async {
              final daily = double.tryParse(dailyCtrl.text.trim());
              final tx = double.tryParse(txCtrl.text.trim());
              if (daily == null || tx == null) return;
              Navigator.pop(ctx);
              try {
                final updated = await CardApi().updateCard(
                  _selectedCard!.id,
                  UpdateCardRequest(dailyLimit: daily, perTransactionLimit: tx),
                );
                if (!mounted) return;
                setState(() {
                  _selectedCard = updated;
                  _cards = _cards
                      .map((c) => c.id == updated.id ? updated : c)
                      .toList();
                });
                messenger.showSnackBar(
                  const SnackBar(
                    content: Text('Spending limits updated successfully!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              } catch (e) {
                messenger.showSnackBar(
                  SnackBar(
                    content: Text(
                      _friendlyError(e, "Couldn't update limits. Please try again."),
                    ),
                    backgroundColor: AppTheme.primaryRed,
                  ),
                );
              }
            },
            child: const Text(
              'Save Limits',
              style: TextStyle(
                color: Colors.white,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showRequestCardDialog() {
    if (_accounts.isEmpty) return;
    Account selectedAcc = _accounts.first;
    final pinCtrl = TextEditingController(text: '1234');
    final messenger = ScaffoldMessenger.of(context);

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.surfaceElevated,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          title: const Text(
            'Request Debit Card',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<Account>(
                initialValue: selectedAcc,
                dropdownColor: AppTheme.surfaceElevated,
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(labelText: 'Linked Account'),
                items: _accounts
                    .map(
                      (a) => DropdownMenuItem(
                        value: a,
                        child: Text(
                          '${a.accountNumber} (${a.currency.toJson()})',
                        ),
                      ),
                    )
                    .toList(),
                onChanged: (a) => setModalState(() => selectedAcc = a!),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: pinCtrl,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 4,
                style: const TextStyle(color: Colors.white, letterSpacing: 4),
                decoration: const InputDecoration(
                  labelText: 'Set 4-Digit PIN',
                  counterText: '',
                ),
              ),
            ],
          ),
          actions: [
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
              onPressed: () async {
                final pin = pinCtrl.text.trim();
                if (pin.length != 4) return;
                Navigator.pop(ctx);
                try {
                  await CardApi().requestCard(
                    CreateCardRequest(accountId: selectedAcc.id, pin: pin),
                  );
                  _loadData();
                } catch (e) {
                  messenger.showSnackBar(
                    SnackBar(
                      content: Text(
                        _friendlyError(e, "Couldn't request the card. Please try again."),
                      ),
                      backgroundColor: AppTheme.primaryRed,
                    ),
                  );
                }
              },
              child: const Text(
                'Request',
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

  Widget _quickActionButton({
    required IconData icon,
    required String label,
    required VoidCallback onTap,
    Color? iconColor,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: AppTheme.surfaceGlass,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.surfaceGlassBorder),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.15),
                  blurRadius: 6,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: Icon(
              icon,
              color: iconColor ?? AppTheme.emeraldLight,
              size: 24,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Colors.white,
              fontWeight: FontWeight.w600,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
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
            'Cards',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
          ),
        ),
        body: AppErrorView(message: _error!, onRetry: _loadData),
      );
    }

    final isFrozen = _selectedCard?.isBlocked ?? false;

    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: const BackButton(color: Colors.white),
        title: const Text(
          'Cards',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_card, color: AppTheme.emeraldLight),
            onPressed: _showRequestCardDialog,
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadData,
        color: AppTheme.emeraldLight,
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
          children: [
            if (_selectedCard != null) ...[
              // Card graphic aspect ratio: balanced 4:6 / ~1.58 credit card ratio
              AspectRatio(
                aspectRatio: 1.58,
                child: Container(
                  padding: const EdgeInsets.all(22),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(20),
                    gradient: _getCardGradient(isFrozen),
                    boxShadow: const [
                      BoxShadow(
                        color: Colors.black45,
                        blurRadius: 16,
                        offset: Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            _cardNicknames[_selectedCard!.id] ??
                                'CBA Platinum Debit',
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                              color: Colors.white,
                            ),
                          ),
                          Icon(
                            isFrozen ? Icons.lock : Icons.contactless,
                            color: Colors.white70,
                          ),
                        ],
                      ),
                      Text(
                        _selectedCard!.cardNumberMasked,
                        style: const TextStyle(
                          fontSize: 20,
                          letterSpacing: 3,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _selectedCard!.cardHolderName.toUpperCase(),
                                style: const TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.white,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'EXP ${_selectedCard!.expiryDate}',
                                style: const TextStyle(
                                  fontSize: 11,
                                  color: Colors.white70,
                                ),
                              ),
                            ],
                          ),
                          // Mask CVV with *** by default, revealing the 3-digit CVV only on click/tap
                          GestureDetector(
                            onTap: () => setState(() => _showCvv = !_showCvv),
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 10,
                                vertical: 4,
                              ),
                              decoration: BoxDecoration(
                                color: Colors.black.withValues(alpha: 0.3),
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: Colors.white24),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Text(
                                    'CVV ',
                                    style: TextStyle(
                                      fontSize: 10,
                                      color: Colors.white60,
                                    ),
                                  ),
                                  Text(
                                    _showCvv
                                        ? _getCvvForCard(_selectedCard!)
                                        : '***',
                                    style: const TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.white,
                                      letterSpacing: 1.5,
                                    ),
                                  ),
                                  const SizedBox(width: 4),
                                  Icon(
                                    _showCvv
                                        ? Icons.visibility_off
                                        : Icons.visibility,
                                    size: 12,
                                    color: Colors.white60,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 20),

              // Card Switcher (if multiple cards)
              if (_cards.length > 1) ...[
                GlassCard(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<CardModel>(
                      value: _selectedCard,
                      isExpanded: true,
                      dropdownColor: AppTheme.surfaceElevated,
                      items: _cards.map((c) {
                        return DropdownMenuItem(
                          value: c,
                          child: Text(
                            '${_cardNicknames[c.id] ?? c.cardNumberMasked} (${c.status})',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        );
                      }).toList(),
                      onChanged: (c) => setState(() => _selectedCard = c),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
              ],

              // 4 quick action buttons directly below the card: Transfer, See History, Freeze, Personalize
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _quickActionButton(
                    icon: Icons.swap_horiz_rounded,
                    label: 'Transfer',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const TransfersTabScreen(),
                      ),
                    ),
                  ),
                  _quickActionButton(
                    icon: Icons.history_rounded,
                    label: 'See History',
                    onTap: _openLinkedAccountHistory,
                  ),
                  _quickActionButton(
                    icon: isFrozen
                        ? Icons.lock_open_rounded
                        : Icons.lock_outline_rounded,
                    label: isFrozen ? 'Unfreeze' : 'Freeze',
                    iconColor: isFrozen
                        ? AppTheme.primaryRedLight
                        : AppTheme.emeraldLight,
                    onTap: () => _toggleFreeze(!isFrozen),
                  ),
                  _quickActionButton(
                    icon: Icons.palette_outlined,
                    label: 'Personalize',
                    onTap: _showPersonalizeDialog,
                  ),
                ],
              ),
              const SizedBox(height: 24),

              // Settings list: Change Transaction Limit, Change Linked Account, Rename Card, Close Card
              GlassCard(
                child: Column(
                  children: [
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(
                        Icons.tune,
                        color: AppTheme.emeraldLight,
                      ),
                      title: const Text(
                        'Change Transaction Limit',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      subtitle: Text(
                        'Daily: \$${_selectedCard!.dailyLimit.toStringAsFixed(0)} • Per-Tx: \$${_selectedCard!.perTransactionLimit.toStringAsFixed(0)}',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.white60,
                        ),
                      ),
                      trailing: const Icon(
                        Icons.arrow_forward_ios,
                        size: 14,
                        color: Colors.white60,
                      ),
                      onTap: _showModifyLimitsDialog,
                    ),
                    const Divider(color: Colors.white12),
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(
                        Icons.link_rounded,
                        color: AppTheme.emeraldLight,
                      ),
                      title: const Text(
                        'Change Linked Account',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      subtitle: Text(
                        'Linked: ${_selectedCard!.accountNumber.isNotEmpty ? _selectedCard!.accountNumber : "Default Account"}',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.white60,
                        ),
                      ),
                      trailing: const Icon(
                        Icons.arrow_forward_ios,
                        size: 14,
                        color: Colors.white60,
                      ),
                      onTap: _showChangeLinkedAccountDialog,
                    ),
                    const Divider(color: Colors.white12),
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(
                        Icons.edit_outlined,
                        color: AppTheme.emeraldLight,
                      ),
                      title: const Text(
                        'Rename Card',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      subtitle: Text(
                        _cardNicknames[_selectedCard!.id] ??
                            'CBA Platinum Debit',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.white60,
                        ),
                      ),
                      trailing: const Icon(
                        Icons.arrow_forward_ios,
                        size: 14,
                        color: Colors.white60,
                      ),
                      onTap: _showRenameCardDialog,
                    ),
                    const Divider(color: Colors.white12),
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(
                        Icons.cancel_outlined,
                        color: AppTheme.primaryRedLight,
                      ),
                      title: const Text(
                        'Close Card',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                          color: AppTheme.primaryRedLight,
                        ),
                      ),
                      subtitle: const Text(
                        'Permanently deactivate this card',
                        style: TextStyle(fontSize: 11, color: Colors.white60),
                      ),
                      trailing: const Icon(
                        Icons.arrow_forward_ios,
                        size: 14,
                        color: Colors.white60,
                      ),
                      onTap: _showCloseCardDialog,
                    ),
                  ],
                ),
              ),
            ] else ...[
              GlassCard(
                padding: const EdgeInsets.all(24),
                child: Column(
                  children: [
                    Icon(
                      Icons.credit_card_off_outlined,
                      size: 64,
                      color: Colors.white.withValues(alpha: 0.3),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'No Debit Cards Found',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Request your instant virtual or physical debit card now',
                      style: TextStyle(color: Colors.white70, fontSize: 12),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 20),
                    PrimaryActionButton(
                      title: 'REQUEST DEBIT CARD',
                      onPressed: _showRequestCardDialog,
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}
