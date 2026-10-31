import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/core/widgets/app_state_view.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/account_analytics_screen.dart';
import 'package:mobile/features/account/presentation/money_format.dart';
import 'package:mobile/features/account/presentation/open_account_screen.dart';
import 'package:mobile/features/bill/presentation/bill_payments_tab_screen.dart';
import 'package:mobile/features/card/presentation/cards_tab_screen.dart';
import 'package:mobile/features/notification/data/notification_api.dart';
import 'package:mobile/features/notification/presentation/notifications_screen.dart';
import 'package:mobile/features/profile/presentation/profile_screen.dart';
import 'package:mobile/features/qr/data/qr_payload.dart';
import 'package:mobile/features/qr/presentation/qr_scan_pay_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scanner_screen.dart';
import 'package:mobile/features/transfer/data/transfer_api.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';
import 'package:mobile/features/transfer/presentation/beneficiary_management_screen.dart';
import 'package:mobile/features/transfer/presentation/transfers_tab_screen.dart';

class HomeTabScreen extends StatefulWidget {
  final void Function(int index, [int? subTabIndex])? onNavigateTab;
  final List<Account>? initialAccounts;

  const HomeTabScreen({super.key, this.onNavigateTab, this.initialAccounts});

  @override
  State<HomeTabScreen> createState() => _HomeTabScreenState();
}

class _HomeTabScreenState extends State<HomeTabScreen>
    with WidgetsBindingObserver {
  // US-022's push channel is a notifications table plus in-app polling, so the
  // badge is only as fresh as the last poll. Thirty seconds keeps a balance
  // alert visible during a demo without hammering the endpoint.
  static const Duration _notificationPollInterval = Duration(seconds: 30);

  List<Account> _accounts = [];
  List<Beneficiary> _beneficiaries = [];
  Account? _selectedAccount;
  Balance? _currentBalance;
  bool _loading = true;
  bool _hideBalance = false;
  int _unreadNotifications = 0;
  String? _error;
  Timer? _notificationTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.initialAccounts != null) {
      _accounts = widget.initialAccounts!;
      if (_accounts.isNotEmpty) {
        _selectedAccount = _accounts.first;
      }
      _loading = false;
    } else {
      _loadData();
    }
    _notificationTimer = Timer.periodic(
      _notificationPollInterval,
      (_) => _refreshUnreadCount(),
    );
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
      }

      try {
        _unreadNotifications = await NotificationApi().getUnreadCount();
      } catch (_) {}

      try {
        _beneficiaries = await TransferApi().listBeneficiaries();
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
    });

    try {
      final accountApi = AccountApi();
      final balance = await accountApi.getBalance(acc.id);
      if (!mounted) return;
      setState(() {
        _currentBalance = balance;
      });
    } catch (_) {}
  }

  Future<void> _handleScanShortcut() async {
    if (widget.onNavigateTab != null) {
      widget.onNavigateTab!(2, 1);
    } else {
      final result = await Navigator.push<QrPayload>(
        context,
        MaterialPageRoute(builder: (_) => const QrScannerScreen()),
      );
      if (!mounted || result == null) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => QRScanPayScreen(
            initialTabIndex: 1,
            initialScannedPayload: result,
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      // Skeleton that mirrors the real layout, so the screen fills in
      // in place instead of a blank page with a spinner.
      return ListView(
        physics: const NeverScrollableScrollPhysics(),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        children: const [
          AppSkeletonBlock(height: 52, radius: 14),
          SizedBox(height: 12),
          AppSkeletonBlock(height: 56, radius: 16),
          SizedBox(height: 12),
          AppSkeletonBlock(height: 180, radius: 24),
          SizedBox(height: 16),
          AppSkeletonBlock(height: 84, radius: 20),
          SizedBox(height: 12),
          AppSkeletonBlock(height: 84, radius: 20),
        ],
      );
    }

    if (_error != null) {
      return AppErrorView(message: _error!, onRetry: _loadData);
    }

    return RefreshIndicator(
      onRefresh: _loadData,
      color: AppTheme.emeraldLight,
      child: ListView(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        children: [
          // Row 1: Top Action Row (Bank brand/logo on left; Notification Bell and QR code at far top-right)
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // Bank brand/logo on the left
              Expanded(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 38,
                      height: 38,
                      decoration: BoxDecoration(
                        gradient: const LinearGradient(
                          colors: [AppTheme.primaryEmerald, Color(0xFF00796B)],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                        borderRadius: BorderRadius.circular(10),
                        boxShadow: [
                          BoxShadow(
                            color: AppTheme.primaryEmerald.withValues(
                              alpha: 0.3,
                            ),
                            blurRadius: 8,
                            offset: const Offset(0, 2),
                          ),
                        ],
                      ),
                      child: const Center(
                        child: Icon(
                          Icons.account_balance,
                          color: Colors.white,
                          size: 22,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisSize: MainAxisSize.min,
                        children: const [
                          Text(
                            'CBA BANK',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontWeight: FontWeight.w900,
                              fontSize: 15,
                              letterSpacing: 1.1,
                              color: Colors.white,
                            ),
                          ),
                          Text(
                            'Digital Banking',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 0.5,
                              color: AppTheme.emeraldLight,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              // Notification Bell and QR code at far top-right in their own row
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Stack(
                    children: [
                      Container(
                        width: 40,
                        height: 40,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: Colors.white.withValues(alpha: 0.12),
                          border: Border.all(color: Colors.white24),
                        ),
                        child: IconButton(
                          padding: EdgeInsets.zero,
                          tooltip: 'Notifications',
                          icon: const Icon(
                            Icons.notifications_none_rounded,
                            color: Colors.white,
                            size: 22,
                          ),
                          onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const NotificationsScreen(),
                            ),
                          ).then((_) => _loadData()),
                        ),
                      ),
                      if (_unreadNotifications > 0)
                        Positioned(
                          right: 0,
                          top: 0,
                          child: Container(
                            padding: const EdgeInsets.all(4),
                            decoration: const BoxDecoration(
                              color: AppTheme.primaryRed,
                              shape: BoxShape.circle,
                            ),
                            child: Text(
                              _unreadNotifications > 9
                                  ? '9+'
                                  : '$_unreadNotifications',
                              style: const TextStyle(
                                fontSize: 9,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(width: 8),
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.white.withValues(alpha: 0.12),
                      border: Border.all(
                        color: AppTheme.emeraldLight.withValues(alpha: 0.4),
                      ),
                    ),
                    child: IconButton(
                      key: const ValueKey('top_right_qr_button'),
                      padding: EdgeInsets.zero,
                      tooltip: 'My QR Code',
                      icon: const Icon(
                        Icons.qr_code_2_rounded,
                        color: AppTheme.emeraldLight,
                        size: 22,
                      ),
                      onPressed: () {
                        if (widget.onNavigateTab != null) {
                          widget.onNavigateTab!(2, 0);
                        } else {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  const QRScanPayScreen(initialTabIndex: 0),
                            ),
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Row 2: Customer Profile greeting moved down into its own row
          GestureDetector(
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const ProfileScreen()),
            ),
            // Plain row, not a card: the greeting is secondary to the balance
            // below it, and a card here competed with the account card.
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: Colors.white.withValues(alpha: 0.15),
                      border: Border.all(
                        color: AppTheme.emeraldLight.withValues(alpha: 0.5),
                      ),
                    ),
                    child: const Icon(
                      Icons.person,
                      color: Colors.white,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Hello, ${SessionManager.instance.firstName ?? "Customer"}',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                            color: Colors.white,
                          ),
                          overflow: TextOverflow.ellipsis,
                          maxLines: 1,
                        ),
                        const SizedBox(height: 2),
                        FittedBox(
                          fit: BoxFit.scaleDown,
                          alignment: Alignment.centerLeft,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0x3300FFB2),
                              borderRadius: BorderRadius.circular(6),
                              border: Border.all(
                                color: AppTheme.emeraldLight.withValues(
                                  alpha: 0.5,
                                ),
                                width: 0.8,
                              ),
                            ),
                            child: const Text(
                              'CBA Gold Tier',
                              style: TextStyle(
                                color: AppTheme.emeraldLight,
                                fontSize: 11,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.arrow_forward_ios,
                    color: Colors.white38,
                    size: 14,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Row 3: Account Card (with Analytics button instead of Email/View PDF)
          if (_selectedAccount != null) ...[
            GlassCard(
              borderRadius: 24,
              padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            Flexible(
                              child: Text(
                                '${_selectedAccount!.accountType.name.toUpperCase()} (${_selectedAccount!.currency.toJson()})',
                                style: const TextStyle(
                                  color: Colors.white70,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            if (_accounts.length > 1) ...[
                              const SizedBox(width: 6),
                              PopupMenuButton<Account>(
                                icon: const Icon(
                                  Icons.keyboard_arrow_down,
                                  color: AppTheme.emeraldLight,
                                  size: 20,
                                ),
                                color: AppTheme.surfaceElevated,
                                onSelected: _selectAccount,
                                itemBuilder: (ctx) => _accounts
                                    .map(
                                      (acc) => PopupMenuItem(
                                        value: acc,
                                        child: Text(
                                          '${acc.accountType.name} (${acc.currency.toJson()}) - ${acc.accountNumber}',
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontSize: 13,
                                          ),
                                        ),
                                      ),
                                    )
                                    .toList(),
                              ),
                            ],
                          ],
                        ),
                      ),
                      IconButton(
                        icon: Icon(
                          _hideBalance
                              ? Icons.visibility_off_outlined
                              : Icons.visibility_outlined,
                          size: 20,
                          color: Colors.white70,
                        ),
                        onPressed: () =>
                            setState(() => _hideBalance = !_hideBalance),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _hideBalance
                        ? '••••••••'
                        : (_currentBalance != null
                              ? formatMoney(
                                  _currentBalance!.balance,
                                  _selectedAccount!.currency,
                                )
                              : formatMoney(
                                  _selectedAccount!.balance,
                                  _selectedAccount!.currency,
                                )),
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
                      Expanded(
                        child: Text(
                          'Account: ${_selectedAccount!.accountNumber}',
                          style: const TextStyle(
                            fontSize: 13,
                            color: Colors.white70,
                            letterSpacing: 0.5,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      IconButton(
                        icon: const Icon(
                          Icons.copy,
                          size: 14,
                          color: AppTheme.emeraldLight,
                        ),
                        onPressed: () {
                          Clipboard.setData(
                            ClipboardData(
                              text: _selectedAccount!.accountNumber,
                            ),
                          );
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text(
                                'Account number copied to clipboard',
                              ),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                  const Divider(color: Colors.white24, height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0x2600FFB2),
                        foregroundColor: AppTheme.emeraldLight,
                        elevation: 0,
                        side: const BorderSide(
                          color: AppTheme.emeraldLight,
                          width: 1.2,
                        ),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 10,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      icon: const Icon(
                        Icons.insights_rounded,
                        size: 18,
                        color: AppTheme.emeraldLight,
                      ),
                      label: const Text(
                        'Analytics',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => AccountAnalyticsScreen(
                            account: _selectedAccount!,
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ] else ...[
            GlassCard(
              child: Column(
                children: [
                  const Text(
                    'No Active Accounts Found',
                    style: TextStyle(color: Colors.white70),
                  ),
                  const SizedBox(height: 12),
                  PrimaryActionButton(
                    title: 'OPEN AN ACCOUNT',
                    onPressed: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const OpenAccountScreen(),
                      ),
                    ).then((_) => _loadData()),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 14),

          // Row 4: Shortcut Grid (accounts, cards, payments, scan, favorites, transfer)
          Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_accounts'),
                      icon: Icons.account_balance_wallet_rounded,
                      label: 'Accounts',
                      onTap: _showAccountsBottomSheet,
                    ),
                  ),
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_cards'),
                      icon: Icons.credit_card_rounded,
                      label: 'Cards',
                      onTap: () {
                        if (widget.onNavigateTab != null) {
                          widget.onNavigateTab!(4);
                        } else {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const CardsTabScreen(),
                            ),
                          );
                        }
                      },
                    ),
                  ),
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_payments'),
                      icon: Icons.receipt_long_rounded,
                      label: 'Payments',
                      onTap: () {
                        if (widget.onNavigateTab != null) {
                          widget.onNavigateTab!(3);
                        } else {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const BillPaymentsTabScreen(),
                            ),
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_scan'),
                      icon: Icons.qr_code_scanner_rounded,
                      label: 'Scan',
                      onTap: _handleScanShortcut,
                    ),
                  ),
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_favorites'),
                      icon: Icons.star_rounded,
                      label: 'Favorites',
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const BeneficiaryManagementScreen(),
                        ),
                      ).then((_) => _loadData()),
                    ),
                  ),
                  Expanded(
                    child: _shortcutItem(
                      key: const ValueKey('shortcut_transfer'),
                      icon: Icons.swap_horiz_rounded,
                      label: 'Transfer',
                      onTap: () {
                        if (widget.onNavigateTab != null) {
                          widget.onNavigateTab!(1);
                        } else {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const TransfersTabScreen(),
                            ),
                          );
                        }
                      },
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Row 5: News and promotions horizontal card carousel
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'News and promotions',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                height: 148,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: [
                    _promoCard(
                      title: 'Fixed Deposit 7.75% p.a.',
                      subtitle: 'Grow your savings with guaranteed returns.',
                      tag: 'HIGH RETURN',
                      gradientColors: const [
                        Color(0xFF004D40),
                        Color(0xFF00796B),
                      ],
                      icon: Icons.savings_rounded,
                    ),
                    _promoCard(
                      title: '5% Weekend Cashback',
                      subtitle:
                          'Earn cashback on dining & supermarket shopping.',
                      tag: 'PROMO',
                      gradientColors: const [
                        Color(0xFF1E3A8A),
                        Color(0xFF2563EB),
                      ],
                      icon: Icons.credit_card_rounded,
                    ),
                    _promoCard(
                      title: 'Scan KHQR & Win Weekly',
                      subtitle: 'Pay at merchants for a chance to win \$500.',
                      tag: 'REWARDS',
                      gradientColors: const [
                        Color(0xFF831843),
                        Color(0xFFDB2777),
                      ],
                      icon: Icons.qr_code_scanner_rounded,
                    ),
                    _promoCard(
                      title: 'Free International Wire',
                      subtitle:
                          'Zero remittance fee to overseas bank accounts.',
                      tag: 'NEW',
                      gradientColors: const [
                        Color(0xFF312E81),
                        Color(0xFF4F46E5),
                      ],
                      icon: Icons.public_rounded,
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Row 6: Favorites Grid (beneficiaries with quick-send action and add new)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Favorites',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.white,
                    ),
                  ),
                  GestureDetector(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const BeneficiaryManagementScreen(),
                      ),
                    ).then((_) => _loadData()),
                    child: const Text(
                      'Manage',
                      style: TextStyle(
                        color: AppTheme.emeraldLight,
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: 1 + _beneficiaries.length,
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 4,
                  crossAxisSpacing: 10,
                  mainAxisSpacing: 12,
                  childAspectRatio: 0.82,
                ),
                itemBuilder: (ctx, index) {
                  if (index == 0) {
                    return InkWell(
                      onTap: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const BeneficiaryManagementScreen(),
                        ),
                      ).then((_) => _loadData()),
                      borderRadius: BorderRadius.circular(16),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            width: 52,
                            height: 52,
                            decoration: BoxDecoration(
                              color: AppTheme.surfaceGlass,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(
                                color: AppTheme.emeraldLight.withValues(
                                  alpha: 0.5,
                                ),
                              ),
                            ),
                            child: const Icon(
                              Icons.add,
                              color: AppTheme.emeraldLight,
                              size: 24,
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            'Add New',
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 11,
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
                  final b = _beneficiaries[index - 1];
                  return InkWell(
                    onTap: () {
                      if (widget.onNavigateTab != null) {
                        widget.onNavigateTab!(1);
                      } else {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => const TransfersTabScreen(),
                          ),
                        );
                      }
                    },
                    borderRadius: BorderRadius.circular(16),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Stack(
                          children: [
                            Container(
                              width: 52,
                              height: 52,
                              decoration: BoxDecoration(
                                color: AppTheme.surfaceGlass,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(
                                  color: AppTheme.surfaceGlassBorder,
                                ),
                              ),
                              child: Center(
                                child: Text(
                                  b.displayName.isNotEmpty
                                      ? b.displayName[0].toUpperCase()
                                      : 'B',
                                  style: const TextStyle(
                                    color: AppTheme.emeraldLight,
                                    fontWeight: FontWeight.bold,
                                    fontSize: 18,
                                  ),
                                ),
                              ),
                            ),
                            if (b.favorite)
                              const Positioned(
                                right: 0,
                                top: 0,
                                child: CircleAvatar(
                                  radius: 7,
                                  backgroundColor: AppTheme.surfaceElevated,
                                  child: Icon(
                                    Icons.star,
                                    color: Color(0xFFFFC107),
                                    size: 10,
                                  ),
                                ),
                              ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          b.displayName,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  );
                },
              ),
            ],
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _promoCard({
    required String title,
    required String subtitle,
    required String tag,
    required List<Color> gradientColors,
    required IconData icon,
  }) {
    return Container(
      width: 240,
      margin: const EdgeInsets.only(right: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: LinearGradient(
          colors: gradientColors,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.2),
            blurRadius: 8,
            offset: const Offset(0, 4),
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
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  tag,
                  style: const TextStyle(
                    fontSize: 9,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                    letterSpacing: 0.8,
                  ),
                ),
              ),
              Icon(icon, color: Colors.white.withValues(alpha: 0.8), size: 22),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: TextStyle(
                  fontSize: 11,
                  color: Colors.white.withValues(alpha: 0.85),
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _shortcutItem({
    Key? key,
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    return InkWell(
      key: key,
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: AppTheme.surfaceGlass,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppTheme.surfaceGlassBorder),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.15),
                  blurRadius: 8,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Icon(icon, color: AppTheme.emeraldLight, size: 24),
          ),
          const SizedBox(height: 6),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: FittedBox(
              fit: BoxFit.scaleDown,
              child: Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.2,
                ),
                maxLines: 1,
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showAccountsBottomSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppTheme.surfaceElevated,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Container(
                    width: 40,
                    height: 4,
                    margin: const EdgeInsets.only(bottom: 16),
                    decoration: BoxDecoration(
                      color: Colors.white24,
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Row(
                      children: [
                        Icon(
                          Icons.account_balance_wallet_rounded,
                          color: AppTheme.emeraldLight,
                          size: 22,
                        ),
                        SizedBox(width: 10),
                        Text(
                          'Your Accounts',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      icon: const Icon(
                        Icons.close,
                        color: Colors.white70,
                        size: 20,
                      ),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                if (_accounts.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Column(
                        children: [
                          const Text(
                            'No accounts found',
                            style: TextStyle(color: Colors.white60),
                          ),
                          const SizedBox(height: 12),
                          PrimaryActionButton(
                            title: 'OPEN AN ACCOUNT',
                            onPressed: () {
                              Navigator.pop(ctx);
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (_) => const OpenAccountScreen(),
                                ),
                              ).then((_) => _loadData());
                            },
                          ),
                        ],
                      ),
                    ),
                  )
                else ...[
                  ConstrainedBox(
                    constraints: BoxConstraints(
                      maxHeight: MediaQuery.of(context).size.height * 0.45,
                    ),
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: _accounts.length,
                      separatorBuilder: (context, index) =>
                          const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final acc = _accounts[index];
                        final isSelected = acc.id == _selectedAccount?.id;
                        return InkWell(
                          onTap: () {
                            Navigator.pop(ctx);
                            _selectAccount(acc);
                          },
                          borderRadius: BorderRadius.circular(16),
                          child: Container(
                            padding: const EdgeInsets.all(14),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? const Color(0x3300FFB2)
                                  : AppTheme.surfaceGlass,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(
                                color: isSelected
                                    ? AppTheme.emeraldLight
                                    : AppTheme.surfaceGlassBorder,
                                width: isSelected ? 1.5 : 1,
                              ),
                            ),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: isSelected
                                        ? AppTheme.primaryEmerald.withValues(
                                            alpha: 0.3,
                                          )
                                        : Colors.white.withValues(alpha: 0.08),
                                    shape: BoxShape.circle,
                                  ),
                                  child: Icon(
                                    Icons.account_balance_rounded,
                                    color: isSelected
                                        ? AppTheme.emeraldLight
                                        : Colors.white70,
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 14),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        '${acc.accountType.name.toUpperCase()} (${acc.currency.toJson()})',
                                        style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14,
                                          color: isSelected
                                              ? Colors.white
                                              : Colors.white70,
                                        ),
                                      ),
                                      const SizedBox(height: 2),
                                      Text(
                                        acc.accountNumber,
                                        style: const TextStyle(
                                          fontSize: 12,
                                          color: Colors.white60,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: [
                                    Text(
                                      formatMoney(
                                        (isSelected && _currentBalance != null)
                                            ? _currentBalance!.balance
                                            : acc.balance,
                                        acc.currency,
                                      ),
                                      style: const TextStyle(
                                        fontWeight: FontWeight.bold,
                                        fontSize: 14,
                                        color: Colors.white,
                                      ),
                                    ),
                                    if (isSelected)
                                      const Padding(
                                        padding: EdgeInsets.only(top: 2),
                                        child: Text(
                                          'Active',
                                          style: TextStyle(
                                            color: AppTheme.emeraldLight,
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 16),
                  PrimaryActionButton(
                    title: 'OPEN ANOTHER ACCOUNT',
                    icon: Icons.add,
                    isSecondary: true,
                    onPressed: () {
                      Navigator.pop(ctx);
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => const OpenAccountScreen(),
                        ),
                      ).then((_) => _loadData());
                    },
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }
}
