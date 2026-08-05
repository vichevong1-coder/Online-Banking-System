import 'dart:async';

import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/jwt.dart';
import 'package:mobile/features/auth/presentation/signed_in_screen.dart';

const Map<String, String> _twoFactorErrorMessages = {
  'INVALID_OR_EXPIRED_OTP': 'That code is incorrect or has expired.',
  'INVALID_OR_EXPIRED_CHALLENGE': 'Your session expired. Please sign in again.',
};

// A resend has no backend rate limit — this is purely a client-side guard against
// double-taps / impatient re-sends, same as web-admin's TwoFactorPage.
const _resendCooldownSeconds = 30;

// US-011: 2FA challenge (customer). Mirrors web-admin's TwoFactorPage (US-012, admin) exactly —
// same countdown-from-the-token-expiry + resend pattern, same mandatory-for-every-account 2FA.
class TwoFactorScreen extends StatefulWidget {
  const TwoFactorScreen({required this.challengeToken, super.key});

  final String challengeToken;

  @override
  State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}

class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final _authApi = AuthApi();
  final _codeController = TextEditingController();

  late String _challengeToken = widget.challengeToken;
  Timer? _countdownTimer;
  Timer? _cooldownTimer;
  Duration _remaining = Duration.zero;
  int _resendCooldown = 0;

  bool _isSubmitting = false;
  bool _isResending = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _startCountdown();
  }

  @override
  void dispose() {
    _codeController.dispose();
    _countdownTimer?.cancel();
    _cooldownTimer?.cancel();
    super.dispose();
  }

  void _startCountdown() {
    _countdownTimer?.cancel();
    final expiresAt = decodeJwtExpiry(_challengeToken);
    if (expiresAt == null) {
      return;
    }
    void tick() {
      final remaining = expiresAt.difference(DateTime.now());
      setState(() => _remaining = remaining.isNegative ? Duration.zero : remaining);
    }

    tick();
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (_) => tick());
  }

  bool get _isExpired => _remaining == Duration.zero;

  Future<void> _verify() async {
    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });
    try {
      final tokens = await _authApi.verifyTwoFactor(challengeToken: _challengeToken, code: _codeController.text.trim());
      if (!mounted) {
        return;
      }
      Navigator.of(
        context,
      ).pushAndRemoveUntil(MaterialPageRoute(builder: (_) => SignedInScreen(tokens: tokens)), (_) => false);
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      if (e.code == 'INVALID_OR_EXPIRED_CHALLENGE') {
        Navigator.of(context).popUntil((route) => route.isFirst);
        return;
      }
      setState(() => _errorText = _twoFactorErrorMessages[e.code] ?? e.message);
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  Future<void> _resend() async {
    setState(() => _isResending = true);
    try {
      final response = await _authApi.resendTwoFactor(challengeToken: _challengeToken);
      if (!mounted) {
        return;
      }
      setState(() {
        _challengeToken = response.challengeToken;
        _resendCooldown = _resendCooldownSeconds;
      });
      _startCountdown();
      _cooldownTimer?.cancel();
      _cooldownTimer = Timer.periodic(const Duration(seconds: 1), (_) {
        setState(() => _resendCooldown = _resendCooldown > 0 ? _resendCooldown - 1 : 0);
        if (_resendCooldown == 0) {
          _cooldownTimer?.cancel();
        }
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('A new code is on its way.')));
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      if (e.code == 'INVALID_OR_EXPIRED_CHALLENGE') {
        Navigator.of(context).popUntil((route) => route.isFirst);
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) {
        setState(() => _isResending = false);
      }
    }
  }

  String _formatCountdown(Duration duration) {
    final minutes = duration.inMinutes;
    final seconds = duration.inSeconds % 60;
    return '$minutes:${seconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Two-factor verification')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('We sent a 6-digit code by SMS to the phone number on file.'),
              const SizedBox(height: 16),
              TextField(
                controller: _codeController,
                keyboardType: TextInputType.number,
                maxLength: 6,
                enabled: !_isExpired,
                decoration: InputDecoration(labelText: 'Verification code', errorText: _errorText),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    _isExpired ? 'Code expired.' : 'Expires in ${_formatCountdown(_remaining)}',
                    style: TextStyle(
                      color: _isExpired ? Theme.of(context).colorScheme.error : Theme.of(context).colorScheme.outline,
                    ),
                  ),
                  TextButton(
                    onPressed: _isResending || _resendCooldown > 0 ? null : _resend,
                    child: Text(
                      _isResending
                          ? 'Sending...'
                          : _resendCooldown > 0
                          ? 'Resend code (${_resendCooldown}s)'
                          : 'Resend code',
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _isSubmitting || _isExpired ? null : _verify,
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
