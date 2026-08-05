import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';

// US-008: phone OTP verification after registration. No timer here — unlike the 2FA challenge
// (US-011/US-012), there's no token whose expiry can be read client-side, and hardcoding the
// server's 5-minute OtpService TTL would drift the moment that value changes. Resend is what
// actually matters: without it, a customer whose code expires has no way forward.
class OtpVerificationScreen extends StatefulWidget {
  const OtpVerificationScreen({required this.phone, super.key});

  final String phone;

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  final _authApi = AuthApi();
  final _codeController = TextEditingController();

  bool _isSubmitting = false;
  bool _isResending = false;
  String? _errorText;

  @override
  void dispose() {
    _codeController.dispose();
    super.dispose();
  }

  Future<void> _verify() async {
    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });
    try {
      await _authApi.verifyRegistrationOtp(phone: widget.phone, code: _codeController.text.trim());
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Phone verified. Please sign in.')));
      Navigator.of(
        context,
      ).pushAndRemoveUntil(MaterialPageRoute(builder: (_) => LoginScreen(initialPhone: widget.phone)), (_) => false);
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() => _errorText = e.message);
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  Future<void> _resend() async {
    setState(() => _isResending = true);
    try {
      await _authApi.resendRegistrationOtp(phone: widget.phone);
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('A new code is on its way.')));
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) {
        setState(() => _isResending = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Verify your phone')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('We sent a 6-digit code by SMS to ${widget.phone}.'),
              const SizedBox(height: 16),
              TextField(
                controller: _codeController,
                keyboardType: TextInputType.number,
                maxLength: 6,
                decoration: InputDecoration(labelText: 'Verification code', errorText: _errorText),
              ),
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: _isResending ? null : _resend,
                  child: Text(_isResending ? 'Sending...' : 'Resend code'),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _isSubmitting ? null : _verify,
                child: _isSubmitting
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Verify'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
