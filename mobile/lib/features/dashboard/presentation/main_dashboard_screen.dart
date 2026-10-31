import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/account/presentation/home_tab_screen.dart';

class MainDashboardScreen extends StatelessWidget {
  final int initialTabIndex;
  final List<Account>? initialAccounts;

  const MainDashboardScreen({
    super.key,
    this.initialTabIndex = 0,
    this.initialAccounts,
  });

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      body: SafeArea(child: HomeTabScreen(initialAccounts: initialAccounts)),
    );
  }
}
