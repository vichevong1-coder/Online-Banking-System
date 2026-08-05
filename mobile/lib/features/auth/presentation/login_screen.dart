import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/presentation/otp_verification_screen.dart';
import 'package:mobile/features/auth/presentation/registration_screen.dart';
import 'package:mobile/features/auth/presentation/two_factor_screen.dart';

const Map<String, String> _loginErrorMessages = {
  'INVALID_CREDENTIALS': 'Incorrect phone number or password.',
  'ACCOUNT_SUSPENDED': 'This account is suspended.',
  'ACCOUNT_LOCKED': 'This account is locked.',
};

// US-009: customer login (mobile). Customers are keyed by phone (US-007 collects no email);
// admin/staff use email instead (US-010, web-admin only). 2FA is mandatory for every account —
// this always leads to the challenge screen, never straight into the app.
class LoginScreen extends StatefulWidget {
  const LoginScreen({this.initialPhone, super.key});

  final String? initialPhone;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _authApi = AuthApi();
  late final TextEditingController _phoneController = TextEditingController(text: widget.initialPhone);
  final _passwordController = TextEditingController();

  bool _isSubmitting = false;
  String? _errorText;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final phone = _phoneController.text.trim();
    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });
    try {
      final response = await _authApi.login(phone: phone, password: _passwordController.text);
      if (!mounted) {
        return;
      }
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (_) => TwoFactorScreen(challengeToken: response.challengeToken)));
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      if (e.code == 'PHONE_NOT_VERIFIED') {
        Navigator.of(context).push(MaterialPageRoute(builder: (_) => OtpVerificationScreen(phone: phone)));
        return;
      }
      setState(() => _errorText = _loginErrorMessages[e.code] ?? e.message);
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Online Banking', style: Theme.of(context).textTheme.headlineSmall, textAlign: TextAlign.center),
              const SizedBox(height: 32),
              TextField(
                controller: _phoneController,
                keyboardType: TextInputType.phone,
                decoration: const InputDecoration(labelText: 'Phone number'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _passwordController,
                obscureText: true,
                decoration: InputDecoration(labelText: 'Password', errorText: _errorText),
              ),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _isSubmitting ? null : _submit,
                child: _isSubmitting
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Sign in'),
              ),
              const SizedBox(height: 12),
              TextButton(
                onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const RegistrationScreen())),
                child: const Text("Don't have an account? Register"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
