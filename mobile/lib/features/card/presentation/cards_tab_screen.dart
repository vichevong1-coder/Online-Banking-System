import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/card/data/card_api.dart';
import 'package:mobile/features/card/data/card_models.dart';

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
          content: Text(freeze ? 'Card has been frozen.' : 'Card is now active.'),
          backgroundColor: freeze ? AppTheme.primaryRed : AppTheme.primaryEmerald,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update card: $e'), backgroundColor: AppTheme.primaryRed),
      );
    }
  }

  void _showChangePinDialog() {
    if (_selectedCard == null) return;
    final pinCtrl = TextEditingController();
    final confirmCtrl = TextEditingController();
    final messenger = ScaffoldMessenger.of(context);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0A2B24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Change Card PIN', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: pinCtrl,
              keyboardType: TextInputType.number,
              obscureText: true,
              maxLength: 4,
              style: const TextStyle(color: Colors.white, letterSpacing: 4),
              decoration: const InputDecoration(labelText: 'New 4-Digit PIN', counterText: ''),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: confirmCtrl,
              keyboardType: TextInputType.number,
              obscureText: true,
              maxLength: 4,
              style: const TextStyle(color: Colors.white, letterSpacing: 4),
              decoration: const InputDecoration(labelText: 'Confirm 4-Digit PIN', counterText: ''),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
            onPressed: () async {
              final pin = pinCtrl.text.trim();
              if (pin.length != 4 || pin != confirmCtrl.text.trim()) {
                messenger.showSnackBar(
                  const SnackBar(content: Text('PIN must be 4 digits and match confirmation')),
                );
                return;
              }
              Navigator.pop(ctx);
              try {
                final updated = await CardApi().updateCard(_selectedCard!.id, UpdateCardRequest(pin: pin));
                if (!mounted) return;
                setState(() {
                  _selectedCard = updated;
                  _cards = _cards.map((c) => c.id == updated.id ? updated : c).toList();
                });
                messenger.showSnackBar(
                  const SnackBar(
                    content: Text('Card PIN updated successfully!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              } catch (e) {
                messenger.showSnackBar(
                  SnackBar(content: Text('Failed to update PIN: $e'), backgroundColor: AppTheme.primaryRed),
                );
              }
            },
            child: const Text('Update PIN', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _showModifyLimitsDialog() {
    if (_selectedCard == null) return;
    final dailyCtrl = TextEditingController(text: _selectedCard!.dailyLimit.toStringAsFixed(2));
    final txCtrl = TextEditingController(text: _selectedCard!.perTransactionLimit.toStringAsFixed(2));
    final messenger = ScaffoldMessenger.of(context);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF0A2B24),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: const Text('Modify Spending Limits', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: dailyCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'Daily Spending Limit (\$)'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: txCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(labelText: 'Per-Transaction Limit (\$)'),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
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
                  _cards = _cards.map((c) => c.id == updated.id ? updated : c).toList();
                });
                messenger.showSnackBar(
                  const SnackBar(
                    content: Text('Spending limits updated successfully!'),
                    backgroundColor: AppTheme.primaryEmerald,
                  ),
                );
              } catch (e) {
                messenger.showSnackBar(
                  SnackBar(content: Text('Failed to update limits: $e'), backgroundColor: AppTheme.primaryRed),
                );
              }
            },
            child: const Text('Save Limits', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
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
          backgroundColor: const Color(0xFF0A2B24),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          title: const Text('Request Debit Card', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<Account>(
                initialValue: selectedAcc,
                dropdownColor: const Color(0xFF0A2B24),
                style: const TextStyle(color: Colors.white),
                decoration: const InputDecoration(labelText: 'Linked Account'),
                items: _accounts.map((a) => DropdownMenuItem(value: a, child: Text('${a.accountNumber} (${a.currency.toJson()})'))).toList(),
                onChanged: (a) => setModalState(() => selectedAcc = a!),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: pinCtrl,
                keyboardType: TextInputType.number,
                obscureText: true,
                maxLength: 4,
                style: const TextStyle(color: Colors.white, letterSpacing: 4),
                decoration: const InputDecoration(labelText: 'Set 4-Digit PIN', counterText: ''),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel', style: TextStyle(color: Colors.white60)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
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
                    SnackBar(content: Text('Failed to request card: $e'), backgroundColor: AppTheme.primaryRed),
                  );
                }
              },
              child: const Text('Request', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: AppTheme.emeraldLight));
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_error!, style: const TextStyle(color: AppTheme.primaryRedLight)),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _loadData, child: const Text('Retry')),
          ],
        ),
      );
    }

    final isFrozen = _selectedCard?.isBlocked ?? false;

    return RefreshIndicator(
      onRefresh: _loadData,
      color: AppTheme.emeraldLight,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Debit Cards', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white)),
              IconButton(
                icon: const Icon(Icons.add_card, color: AppTheme.emeraldLight),
                onPressed: _showRequestCardDialog,
              ),
            ],
          ),
          const SizedBox(height: 16),

          if (_selectedCard != null) ...[
            // Virtual Card Graphic
            Container(
              height: 200,
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                gradient: LinearGradient(
                  colors: isFrozen
                      ? [const Color(0xFF2C3E50), const Color(0xFF1A252F)]
                      : [const Color(0xFF00B27A), const Color(0xFF042D26)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: const [
                  BoxShadow(color: Colors.black45, blurRadius: 16, offset: Offset(0, 6)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'CBA Platinum Debit',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14, color: Colors.white),
                      ),
                      Icon(isFrozen ? Icons.lock : Icons.contactless, color: Colors.white70),
                    ],
                  ),
                  Text(
                    _selectedCard!.cardNumberMasked,
                    style: const TextStyle(fontSize: 20, letterSpacing: 3, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        _selectedCard!.cardHolderName.toUpperCase(),
                        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: Colors.white),
                      ),
                      Text(
                        'EXP ${_selectedCard!.expiryDate}',
                        style: const TextStyle(fontSize: 12, color: Colors.white70),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Card Switcher (if multiple cards)
            if (_cards.length > 1) ...[
              GlassCard(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<CardModel>(
                    value: _selectedCard,
                    isExpanded: true,
                    dropdownColor: const Color(0xFF0A2B24),
                    items: _cards.map((c) {
                      return DropdownMenuItem(
                        value: c,
                        child: Text(
                          '${c.cardNumberMasked} (${c.status})',
                          style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                        ),
                      );
                    }).toList(),
                    onChanged: (c) => setState(() => _selectedCard = c),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],

            // Card Controls
            GlassCard(
              child: Column(
                children: [
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Freeze / Block Card', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    subtitle: const Text('Temporarily lock payments and ATM withdrawals', style: TextStyle(fontSize: 11, color: Colors.white60)),
                    value: isFrozen,
                    activeThumbColor: AppTheme.primaryRed,
                    onChanged: (val) => _toggleFreeze(val),
                  ),
                  const Divider(color: Colors.white12),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.pin_outlined, color: AppTheme.emeraldLight),
                    title: const Text('Change Card PIN', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.white60),
                    onTap: _showChangePinDialog,
                  ),
                  const Divider(color: Colors.white12),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: const Icon(Icons.tune, color: AppTheme.emeraldLight),
                    title: const Text('Modify Spending Limits', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    subtitle: Text(
                      'Daily: \$${_selectedCard!.dailyLimit.toStringAsFixed(0)} • Per-Tx: \$${_selectedCard!.perTransactionLimit.toStringAsFixed(0)}',
                      style: const TextStyle(fontSize: 11, color: Colors.white60),
                    ),
                    trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: Colors.white60),
                    onTap: _showModifyLimitsDialog,
                  ),
                ],
              ),
            ),
          ] else ...[
            GlassCard(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  Icon(Icons.credit_card_off_outlined, size: 64, color: Colors.white.withValues(alpha: 0.3)),
                  const SizedBox(height: 16),
                  const Text('No Debit Cards Found', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white)),
                  const SizedBox(height: 6),
                  const Text('Request your instant virtual or physical debit card now', style: TextStyle(color: Colors.white70, fontSize: 12), textAlign: TextAlign.center),
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
    );
  }
}
