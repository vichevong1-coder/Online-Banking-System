import 'package:flutter/material.dart';

import 'core/theme/app_theme.dart';
import 'features/auth/presentation/login_screen.dart';

class ObsApp extends StatelessWidget {
  const ObsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Online Banking System',
      theme: AppTheme.light,
      home: const LoginScreen(),
    );
  }
}
