import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/account/presentation/open_account_screen.dart';
import 'package:mobile/features/account/presentation/statement_viewer_screen.dart';
import 'package:mobile/features/account/presentation/transaction_history_screen.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;
import 'package:mobile/features/notification/data/notification_api.dart';
import 'package:mobile/features/notification/presentation/notifications_screen.dart';
import 'package:mobile/features/profile/presentation/profile_screen.dart';

class HomeTabScreen extends StatefulWidget {
  final Function(int)? onNavigateTab;

  const HomeTabScreen({super.key, this.onNavigateTab});

  @override
  State<HomeTabScreen> createState() => _HomeTabScreenState();
}

class _HomeTabScreenState extends State<HomeTabScreen> with WidgetsBindingObserver {
  // US-022's push channel is a notifications table plus in-app polling, so the
  // badge is only as fresh as the last poll. Thirty seconds keeps a balance
  // alert visible during a demo without hammering the endpoint.
  static const Duration _notificationPollInterval = Duration(seconds: 30);

  List<Account> _accounts = [];
  Account? _selectedAccount;
  Balance? _currentBalance;
  List<BankTransaction> _recentTransactions = [];
  bool _loading = true;
  bool _hideBalance = false;
  int _unreadNotifications = 0;
  String? _error;
  Timer? _notificationTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadData();
    _notificationTimer = Timer.periodic(_notificationPollInterval, (_) => _refreshUnreadCount());
  }

  @override
  void dispose() {
    _notificationTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Coming back from the background is the one moment the badge is most
    // likely to be stale, and polling was paused the whole time.
    if (state == AppLifecycleState.resumed) {
      _refreshUnreadCount();
    }
  }

  Future<void> _refreshUnreadCount() async {
    try {
      final count = await NotificationApi().getUnreadCount();
      if (!mounted || count == _unreadNotifications) return;
      setState(() => _unreadNotifications = count);
    } catch (_) {
      // A failed poll is not worth surfacing; the next one will try again.
    }
  }

  Future<void> _loadData() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final accountApi = AccountApi();
      final accounts = await accountApi.listAccounts();

      if (accounts.isNotEmpty) {
        _accounts = accounts;
        _selectedAccount = accounts.first;
        final balance = await accountApi.getBalance(_selectedAccount!.id);
        _currentBalance = balance;

        final txPage = await accountApi.listTransactions(
          _selectedAccount!.id,
          TransactionQuery(page: 0, size: 5),
        );
        _recentTransactions = txPage.content;
      }

      try {
        _unreadNotifications = await NotificationApi().getUnreadCount();
      } catch (_) {}

      setState(() => _loading = false);
    } catch (_) {
      setState(() {
        _error = "Couldn't load accounts.";
        _loading = false;
      });
    }
  }

  Future<void> _selectAccount(Account acc) async {
    setState(() {
      _selectedAccount = acc;
      _currentBalance = null;
      _recentTransactions = [];
    });

    try {
      final accountApi = AccountApi();
      final balance = await accountApi.getBalance(acc.id);
      final txPage = await accountApi.listTransactions(
        acc.id,
        TransactionQuery(page: 0, size: 5),
      );
      setState(() {
        _currentBalance = balance;
        _recentTransactions = txPage.content;
      });
    } catch (_) {}
  }

  void _emailStatement() async {
    if (_selectedAccount == null) return;
    try {
      await AccountApi().emailStatement(_selectedAccount!.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Statement PDF has been sent to your email address.'),
          backgroundColor: AppTheme.primaryEmerald,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Failed to email statement: $e'),
          backgroundColor: AppTheme.primaryRed,
        ),
      );
    }
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

    return RefreshIndicator(
      onRefresh: _loadData,
      color: AppTheme.emeraldLight,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        children: [
          // Top Header (Avatar, Greeting, Tier, Notifications)
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              GestureDetector(
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const ProfileScreen()),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: Colors.white.withValues(alpha: 0.15),
                        border: Border.all(color: AppTheme.emeraldLight.withValues(alpha: 0.5)),
                      ),
                      child: const Icon(Icons.person, color: Colors.white, size: 24),
                    ),
                    const SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Hello, ${SessionManager.instance.firstName ?? "Customer"}',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white),
                        ),
                        const SizedBox(height: 2),
                        const Text('CBA Gold Tier', style: TextStyle(color: AppTheme.emeraldLight, fontSize: 12, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ],
                ),
              ),
              Stack(
                children: [
                  IconButton(
                    icon: const Icon(Icons.notifications_none_rounded, color: Colors.white, size: 26),
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const NotificationsScreen()),
                    ).then((_) => _loadData()),
                  ),
                  if (_unreadNotifications > 0)
                    Positioned(
                      right: 8,
                      top: 8,
                      child: Container(
                        padding: const EdgeInsets.all(4),
                        decoration: const BoxDecoration(
                          color: AppTheme.primaryRed,
                          shape: BoxShape.circle,
                        ),
                        child: Text(
                          _unreadNotifications > 9 ? '9+' : '$_unreadNotifications',
                          style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white),
                        ),
                      ),
                    ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Accounts Card
          if (_selectedAccount != null) ...[
            GlassCard(
              borderRadius: 24,
              padding: const EdgeInsets.all(22),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Text(
                            '${_selectedAccount!.accountType.name.toUpperCase()} (${_selectedAccount!.currency.toJson()})',
                            style: const TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600),
                          ),
                          if (_accounts.length > 1) ...[
                            const SizedBox(width: 6),
                            PopupMenuButton<Account>(
                              icon: const Icon(Icons.keyboard_arrow_down, color: AppTheme.emeraldLight, size: 20),
                              color: const Color(0xFF0A2B24),
                              onSelected: _selectAccount,
                              itemBuilder: (ctx) => _accounts.map(
                                (acc) => PopupMenuItem(
                                  value: acc,
                                  child: Text(
                                    '${acc.accountType.name} (${acc.currency.toJson()}) - ${acc.accountNumber}',
                                    style: const TextStyle(color: Colors.white, fontSize: 13),
                                  ),
                                ),
                              ).toList(),
                            ),
                          ],
                        ],
                      ),
                      IconButton(
                        icon: Icon(
                          _hideBalance ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                          size: 20,
                          color: Colors.white70,
                        ),
                        onPressed: () => setState(() => _hideBalance = !_hideBalance),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _hideBalance
                        ? '••••••••'
                        : (_currentBalance != null
                            ? formatMoney(_currentBalance!.balance, _selectedAccount!.currency)
                            : formatMoney(_selectedAccount!.balance, _selectedAccount!.currency)),
                    style: const TextStyle(
                      fontSize: 32,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                      color: Colors.white,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Text(
                        'Account: ${_selectedAccount!.accountNumber}',
                        style: const TextStyle(fontSize: 13, color: Colors.white70, letterSpacing: 0.5),
                      ),
                      IconButton(
                        icon: const Icon(Icons.copy, size: 14, color: AppTheme.emeraldLight),
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: _selectedAccount!.accountNumber));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Account number copied to clipboard')),
                          );
                        },
                      ),
                    ],
                  ),
                  const Divider(color: Colors.white24, height: 24),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      OutlinedButton.icon(
                        style: OutlinedButton.styleFrom(
                          foregroundColor: Colors.white,
                          side: const BorderSide(color: Colors.white30),
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                        icon: const Icon(Icons.email_outlined, size: 14, color: AppTheme.emeraldLight),
                        label: const Text('Email PDF', style: TextStyle(fontSize: 11)),
                        onPressed: _emailStatement,
                      ),
                      ElevatedButton.icon(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.primaryRed,
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                        ),
                        icon: const Icon(Icons.picture_as_pdf, size: 14, color: Colors.white),
                        label: const Text('View PDF', style: TextStyle(fontSize: 11, color: Colors.white, fontWeight: FontWeight.bold)),
                        onPressed: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => StatementViewerScreen(
                              accountId: _selectedAccount!.id,
                              accountNumber: _selectedAccount!.accountNumber,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ] else ...[
            GlassCard(
              child: Column(
                children: [
                  const Text('No Active Accounts Found', style: TextStyle(color: Colors.white70)),
                  const SizedBox(height: 12),
                  PrimaryActionButton(
                    title: 'OPEN AN ACCOUNT',
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (_) => const OpenAccountScreen()),
                    ).then((_) => _loadData()),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 24),

          // Quick Action Grid
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _quickAction(
                icon: Icons.send_rounded,
                label: 'Transfer',
                onTap: () => widget.onNavigateTab?.call(1),
              ),
              _quickAction(
                icon: Icons.qr_code_2_rounded,
                label: 'Receive QR',
                // The QR tab opens on "Receive (My QR)" — one screen owns the
                // personal code rather than two copies of it.
                onTap: () => widget.onNavigateTab?.call(2),
              ),
              _quickAction(
                icon: Icons.receipt_long_rounded,
                label: 'Pay Bills',
                onTap: () => widget.onNavigateTab?.call(3),
              ),
              _quickAction(
                icon: Icons.add_circle_outline,
                label: 'New Account',
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(builder: (_) => const OpenAccountScreen()),
                ).then((_) => _loadData()),
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Recent Transactions Section
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Recent Transactions', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
              if (_selectedAccount != null)
                GestureDetector(
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => TransactionHistoryScreen(account: _selectedAccount!),
                    ),
                  ),
                  child: const Text('See All', style: TextStyle(color: AppTheme.emeraldLight, fontWeight: FontWeight.bold, fontSize: 13)),
                ),
            ],
          ),
          const SizedBox(height: 12),

          if (_recentTransactions.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text('No transactions recorded yet', style: TextStyle(color: Colors.white60, fontSize: 13)),
              ),
            )
          else
            ..._recentTransactions.map((tx) => _transactionTile(tx)),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _quickAction({required IconData icon, required String label, required VoidCallback onTap}) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Column(
        children: [
          Container(
            width: 60,
            height: 60,
            decoration: BoxDecoration(
              color: AppTheme.surfaceGlass,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.surfaceGlassBorder),
            ),
            child: Icon(icon, color: Colors.white, size: 24),
          ),
          const SizedBox(height: 8),
          Text(label, style: const TextStyle(fontSize: 12, color: Colors.white70, fontWeight: FontWeight.w500)),
        ],
      ),
    );
  }

  Widget _transactionTile(BankTransaction tx) {
    final isDebit = !tx.type.isCredit;

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      child: GlassCard(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        borderRadius: 14,
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: isDebit ? const Color(0x33FF3B30) : const Color(0x3300C853),
              child: Icon(
                isDebit ? Icons.arrow_upward : Icons.arrow_downward,
                color: isDebit ? Colors.redAccent : Colors.greenAccent,
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
              '${isDebit ? "- " : "+ "}${formatMoney(tx.amount, tx.currency)}',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 15,
                color: isDebit ? Colors.white : const Color(0xFF69F0AE),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
