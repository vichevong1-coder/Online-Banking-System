import 'package:flutter/material.dart';

import 'package:mobile/features/auth/data/auth_models.dart';

// No dashboard exists yet (Sprint 2) — this just proves the auth pipeline reaches a signed-in
// state, mirroring web-admin's OverviewPage stub.
class SignedInScreen extends StatelessWidget {
  const SignedInScreen({required this.tokens, super.key});

  final AuthTokenResponse tokens;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Online Banking')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.check_circle, size: 48, color: Colors.green),
              const SizedBox(height: 16),
              Text('Signed in as ${tokens.firstName} ${tokens.lastName}', textAlign: TextAlign.center),
              const SizedBox(height: 8),
              Text(
                'Account features land in Sprint 2.',
                style: Theme.of(context).textTheme.bodySmall,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
