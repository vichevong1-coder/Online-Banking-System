import 'dart:async';
import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/presentation/login_screen.dart';
import 'package:mobile/features/auth/presentation/onboarding_steps.dart';
import 'package:mobile/features/auth/presentation/otp_code_input.dart';

/// Step 3 of registration: the SMS code from US-008. The backend delivers it
/// through the `OtpSender` stub, so in local dev it is printed to the log.
class OtpVerificationScreen extends StatefulWidget {
  final String phone;

  const OtpVerificationScreen({super.key, required this.phone});

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  final GlobalKey<OtpCodeInputState> _codeKey = GlobalKey<OtpCodeInputState>();

  String _code = '';
  bool _isLoading = false;
  bool _isResending = false;
  String? _errorMessage;
  int _countdown = 60;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _startTimer() {
    _countdown = 60;
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_countdown > 0) {
        setState(() => _countdown--);
      } else {
        timer.cancel();
      }
    });
  }

  /// Shown next to "code sent to" — the raw +855… is hard to read back.
  String get _formattedPhone {
    final digits = widget.phone.replaceAll(RegExp(r'[^0-9]'), '');
    if (!widget.phone.startsWith('+855') || digits.length < 11) {
      return widget.phone;
    }
    final local = digits.substring(3);
    return '+855 ${local.substring(0, 2)} ${local.substring(2, 5)} ${local.substring(5)}';
  }

  Future<void> _verifyOtp() async {
    if (_code.length != 6) {
      setState(() => _errorMessage = 'Please enter the 6-digit code');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await AuthApi().verifyRegistrationOtp(phone: widget.phone, code: _code);

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Registration verified successfully! Please sign in.'),
          backgroundColor: AppTheme.primaryEmerald,
        ),
      );

      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (_) => const LoginScreen()),
        (route) => false,
      );
    } on ApiException catch (e) {
      _codeKey.currentState?.clear();
      setState(() {
        _code = '';
        _errorMessage = e.message;
      });
    } catch (_) {
      _codeKey.currentState?.clear();
      setState(() {
        _code = '';
        _errorMessage = 'Verification failed. Please try again.';
      });
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _resendOtp() async {
    if (_countdown > 0) return;

    setState(() {
      _isResending = true;
      _errorMessage = null;
    });

    try {
      await AuthApi().resendRegistrationOtp(phone: widget.phone);
      _codeKey.currentState?.clear();
      _startTimer();
      if (!mounted) return;
      setState(() => _code = '');
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('A new OTP code has been sent.')),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't resend code. Please try again.");
    } finally {
      if (mounted) setState(() => _isResending = false);
    }
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
        title: const Text('Verify', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const OnboardingSteps(currentStep: 3),
              const SizedBox(height: 32),
              const Text(
                'Enter your code',
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: Colors.white),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'Sent by SMS to $_formattedPhone',
                      style: const TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
                    ),
                  ),
                  TextButton(
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    onPressed: () => Navigator.pop(context),
                    child: const Text(
                      'Change',
                      style: TextStyle(color: AppTheme.emeraldLight, fontSize: 13, fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 28),

              OtpCodeInput(
                key: _codeKey,
                hasError: _errorMessage != null,
                onChanged: (code) => setState(() {
                  _code = code;
                  if (_errorMessage != null) _errorMessage = null;
                }),
                onCompleted: (_) => _verifyOtp(),
              ),

              if (_errorMessage != null) ...[
                const SizedBox(height: 14),
                Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 16),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: const TextStyle(color: AppTheme.primaryRedLight, fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ],

              const SizedBox(height: 24),
              Center(
                child: _isResending
                    ? const SizedBox(
                        height: 16,
                        width: 16,
                        child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.emeraldLight),
                      )
                    : Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Text("Didn't get it? ", style: TextStyle(color: Colors.white70, fontSize: 13)),
                          GestureDetector(
                            onTap: _countdown > 0 ? null : _resendOtp,
                            child: Text(
                              _countdown > 0 ? 'Resend in ${_countdown}s' : 'Resend code',
                              style: TextStyle(
                                color: _countdown > 0 ? Colors.white38 : AppTheme.emeraldLight,
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ],
                      ),
              ),
              const SizedBox(height: 36),
              PrimaryActionButton(
                title: 'VERIFY',
                isLoading: _isLoading,
                onPressed: _code.length == 6 ? _verifyOtp : null,
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
