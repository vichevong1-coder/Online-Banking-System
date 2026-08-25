import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/presentation/home_tab_screen.dart';
import 'package:mobile/features/bill/presentation/bill_payments_tab_screen.dart';
import 'package:mobile/features/card/presentation/cards_tab_screen.dart';
import 'package:mobile/features/qr/presentation/qr_scan_pay_screen.dart';
import 'package:mobile/features/transfer/presentation/transfers_tab_screen.dart';

class MainDashboardScreen extends StatefulWidget {
  final int initialTabIndex;

  const MainDashboardScreen({super.key, this.initialTabIndex = 0});

  @override
  State<MainDashboardScreen> createState() => _MainDashboardScreenState();
}

class _MainDashboardScreenState extends State<MainDashboardScreen> {
  late int _currentIndex;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialTabIndex;
  }

  void _onNavigateTab(int index) {
    setState(() => _currentIndex = index);
  }

  @override
  Widget build(BuildContext context) {
    final List<Widget> pages = [
      HomeTabScreen(onNavigateTab: _onNavigateTab),
      const TransfersTabScreen(),
      const QRScanPayScreen(),
      const BillPaymentsTabScreen(),
      const CardsTabScreen(),
    ];

    return GradientScaffold(
      body: pages[_currentIndex],
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: Color(0xE6041C18),
          border: Border(top: BorderSide(color: Colors.white12, width: 0.8)),
        ),
        child: BottomNavigationBar(
          currentIndex: _currentIndex,
          backgroundColor: Colors.transparent,
          type: BottomNavigationBarType.fixed,
          selectedItemColor: AppTheme.emeraldLight,
          unselectedItemColor: Colors.white54,
          elevation: 0,
          selectedLabelStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
          unselectedLabelStyle: const TextStyle(fontSize: 11),
          onTap: (i) => setState(() => _currentIndex = i),
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.account_balance_rounded),
              label: 'Accounts',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.swap_horiz_rounded),
              label: 'Transfer',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.qr_code_scanner_rounded),
              label: 'QR Pay',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.receipt_long_rounded),
              label: 'Bills',
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.credit_card_rounded),
              label: 'Cards',
            ),
          ],
        ),
      ),
    );
  }
}
