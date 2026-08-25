import 'dart:async';
import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/session/session_manager.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/jwt.dart';

class TwoFactorScreen extends StatefulWidget {
  final String phone;
  final String challengeToken;

  const TwoFactorScreen({
    super.key,
    required this.phone,
    required this.challengeToken,
  });

  @override
  State<TwoFactorScreen> createState() => _TwoFactorScreenState();
}

class _TwoFactorScreenState extends State<TwoFactorScreen> {
  final TextEditingController _codeController = TextEditingController();
  late String _currentChallengeToken;
  bool _isLoading = false;
  bool _isResending = false;
  String? _errorMessage;
  int _secondsRemaining = 300;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _currentChallengeToken = widget.challengeToken;
    _initCountdown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _codeController.dispose();
    super.dispose();
  }

  void _initCountdown() {
    final exp = decodeJwtExpiry(_currentChallengeToken);
    if (exp != null) {
      final diff = (exp.millisecondsSinceEpoch - DateTime.now().millisecondsSinceEpoch) ~/ 1000;
      _secondsRemaining = diff > 0 ? diff : 0;
    } else {
      _secondsRemaining = 300;
    }

    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_secondsRemaining > 0) {
        setState(() => _secondsRemaining--);
      } else {
        _timer?.cancel();
      }
    });
  }

  Future<void> _verify2Fa() async {
    final code = _codeController.text.trim();
    if (code.length != 6) {
      setState(() => _errorMessage = 'Please enter a 6-digit code');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final tokens = await AuthApi().verifyTwoFactor(
        challengeToken: _currentChallengeToken,
        code: code,
      );

      await SessionManager.instance.saveSession(
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: tokens.userId,
        firstName: tokens.firstName,
        lastName: tokens.lastName,
        phone: widget.phone,
      );
      // No manual navigation needed — the ListenableBuilder in app.dart
      // automatically rebuilds to MainDashboardScreen when isAuthenticated
      // becomes true after saveSession() calls notifyListeners().
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't verify 2FA. Please try again.");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _resendCode() async {
    setState(() {
      _isResending = true;
      _errorMessage = null;
    });

    try {
      final response = await AuthApi().resendTwoFactor(challengeToken: _currentChallengeToken);
      _currentChallengeToken = response.challengeToken;
      _initCountdown();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('A fresh 2FA code has been sent.')),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't resend 2FA code.");
    } finally {
      if (mounted) setState(() => _isResending = false);
    }
  }

  String _formatTime(int seconds) {
    final m = (seconds ~/ 60).toString().padLeft(2, '0');
    final s = (seconds % 60).toString().padLeft(2, '0');
    return '$m:$s';
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Two-Factor Authentication', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const SizedBox(height: 10),
            const Text(
              'Security Verification',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 8),
            Text(
              'Please enter the 6-digit authentication code sent to\n${widget.phone}',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 13, color: Colors.white70, height: 1.4),
            ),
            const SizedBox(height: 32),

            if (_errorMessage != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.primaryRed.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.primaryRedLight.withValues(alpha: 0.5)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: const TextStyle(color: Colors.white, fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
            ],

            GlassCard(
              child: Column(
                children: [
                  TextField(
                    controller: _codeController,
                    keyboardType: TextInputType.number,
                    textAlign: TextAlign.center,
                    maxLength: 6,
                    style: const TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 10,
                      color: Colors.white,
                    ),
                    decoration: const InputDecoration(
                      hintText: '••••••',
                      counterText: '',
                      hintStyle: TextStyle(letterSpacing: 10, color: Colors.white38),
                    ),
                    onSubmitted: (_) => _verify2Fa(),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  Icons.timer_outlined,
                  size: 16,
                  color: _secondsRemaining > 0 ? Colors.white70 : AppTheme.primaryRedLight,
                ),
                const SizedBox(width: 6),
                Text(
                  _secondsRemaining > 0
                      ? 'Code expires in ${_formatTime(_secondsRemaining)}'
                      : 'Code expired. Request a new one.',
                  style: TextStyle(
                    fontSize: 13,
                    color: _secondsRemaining > 0 ? Colors.white70 : AppTheme.primaryRedLight,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            GestureDetector(
              onTap: _isResending ? null : _resendCode,
              child: Text(
                _isResending ? 'Resending…' : 'Resend 2FA Code',
                style: const TextStyle(
                  color: AppTheme.emeraldLight,
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                ),
              ),
            ),
            const SizedBox(height: 36),
            PrimaryActionButton(
              title: 'VERIFY & SIGN IN',
              isLoading: _isLoading,
              onPressed: _verify2Fa,
            ),
          ],
        ),
      ),
    );
  }
}
