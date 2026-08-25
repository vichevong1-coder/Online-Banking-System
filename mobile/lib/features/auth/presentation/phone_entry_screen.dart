import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/presentation/confirm_details_screen.dart';
import 'package:mobile/features/auth/presentation/onboarding_steps.dart';

/// Step 1 of registration: the phone number every later step hangs off — it is
/// the OTP destination (US-008) and the customer's sign-in identifier (US-009).
class PhoneEntryScreen extends StatefulWidget {
  const PhoneEntryScreen({super.key});

  @override
  State<PhoneEntryScreen> createState() => _PhoneEntryScreenState();
}

class _PhoneEntryScreenState extends State<PhoneEntryScreen> {
  static const _countryCode = '+855';

  final TextEditingController _phoneController = TextEditingController();
  final FocusNode _phoneFocus = FocusNode();
  String? _error;

  @override
  void initState() {
    super.initState();
    _phoneController.addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    _phoneController.dispose();
    _phoneFocus.dispose();
    super.dispose();
  }

  /// Cambodian mobile numbers are 8–9 digits after the country code, with or
  /// without the national trunk '0'.
  String get _localDigits {
    final digits = _phoneController.text.replaceAll(RegExp(r'[^0-9]'), '');
    return digits.startsWith('0') ? digits.substring(1) : digits;
  }

  bool get _isComplete => _localDigits.length >= 8 && _localDigits.length <= 9;

  void _onNext() {
    if (!_isComplete) {
      setState(() => _error = 'Enter an 8 or 9 digit Cambodian mobile number');
      return;
    }
    setState(() => _error = null);

    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ConfirmDetailsScreen(phone: '$_countryCode$_localDigits'),
      ),
    );
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
        title: const Text(
          'Open Account',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const OnboardingSteps(currentStep: 1),
              const SizedBox(height: 32),
              const Text(
                "What's your number?",
                style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: Colors.white),
              ),
              const SizedBox(height: 10),
              const Text(
                "We'll text you a 6-digit code to verify it. This number becomes your login.",
                style: TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
              ),
              const SizedBox(height: 32),

              GlassCard(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                child: Row(
                  children: [
                    const Text('🇰🇭', style: TextStyle(fontSize: 22)),
                    const SizedBox(width: 8),
                    const Text(
                      _countryCode,
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                    Container(
                      width: 1,
                      height: 26,
                      margin: const EdgeInsets.symmetric(horizontal: 14),
                      color: Colors.white24,
                    ),
                    Expanded(
                      child: TextField(
                        controller: _phoneController,
                        focusNode: _phoneFocus,
                        autofocus: true,
                        keyboardType: TextInputType.phone,
                        textInputAction: TextInputAction.done,
                        maxLength: 10,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                        style: const TextStyle(
                          fontSize: 20,
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1.2,
                        ),
                        decoration: const InputDecoration(
                          hintText: '12 345 678',
                          counterText: '',
                          filled: false,
                          border: InputBorder.none,
                          enabledBorder: InputBorder.none,
                          focusedBorder: InputBorder.none,
                        ),
                        onSubmitted: (_) => _onNext(),
                      ),
                    ),
                    if (_isComplete)
                      const Icon(Icons.check_circle, color: AppTheme.emeraldLight, size: 20),
                  ],
                ),
              ),

              if (_error != null) ...[
                const SizedBox(height: 10),
                Row(
                  children: [
                    const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 16),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        _error!,
                        style: const TextStyle(color: AppTheme.primaryRedLight, fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ],

              const SizedBox(height: 20),
              Row(
                children: [
                  const Icon(Icons.lock_outline, size: 14, color: Colors.white38),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      'Your number is only used for verification and account alerts.',
                      style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.45)),
                    ),
                  ),
                ],
              ),

              const Spacer(),
              PrimaryActionButton(
                title: 'CONTINUE',
                onPressed: _isComplete ? _onNext : null,
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
