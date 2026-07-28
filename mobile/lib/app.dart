import 'package:flutter/material.dart';

import 'core/theme/app_theme.dart';

class ObsApp extends StatelessWidget {
  const ObsApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Online Banking System',
      theme: AppTheme.light,
      home: const _PlaceholderHome(),
    );
  }
}

class _PlaceholderHome extends StatelessWidget {
  const _PlaceholderHome();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Online Banking System')),
      body: const Center(child: Text('Mobile app setup complete')),
    );
  }
}
