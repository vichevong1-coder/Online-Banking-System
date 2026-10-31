import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/presentation/forgot_password_screen.dart';
import 'package:mobile/features/auth/presentation/phone_entry_screen.dart';
import 'package:mobile/features/auth/presentation/two_factor_screen.dart';
import 'package:mobile/core/widgets/bank_input_field.dart';

/// Strips non-digits, strips leading Cambodian trunk '0' or country code '855',
/// formats with spaces: +855 XX XXX XXX (or +855 XX XXX XXXX).
String formatCambodianPhone(String input) {
  if (input.trim().isEmpty) return '';
  String digits = input.replaceAll(RegExp(r'[^0-9]'), '');
  if (digits.startsWith('855')) {
    digits = digits.substring(3);
  }
  if (digits.startsWith('0')) {
    digits = digits.substring(1);
  }
  if (digits.isEmpty) {
    return '+855 ';
  }
  final buffer = StringBuffer('+855 ');
  if (digits.length <= 2) {
    buffer.write(digits);
  } else if (digits.length <= 5) {
    buffer.write('${digits.substring(0, 2)} ${digits.substring(2)}');
  } else if (digits.length <= 8) {
    buffer.write(
      '${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5)}',
    );
  } else {
    buffer.write(
      '${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5, digits.length.clamp(5, 9))}',
    );
  }
  return buffer.toString();
}

/// Normalizes Cambodian phone number to clean E.164 format: +855XXXXXXXX
String normalizePhoneToE164(String input) {
  String digits = input.replaceAll(RegExp(r'[^0-9]'), '');
  if (digits.startsWith('855')) {
    digits = digits.substring(3);
  }
  if (digits.startsWith('0')) {
    digits = digits.substring(1);
  }
  return '+855$digits';
}

/// Customer-facing text for a failed sign-in. Accounts still under KYC review
/// or turned down get a plain explanation instead of the raw server message.
String loginErrorMessage(ApiException e) {
  switch (e.code) {
    case 'ACCOUNT_PENDING_APPROVAL':
      return 'Your application is still being reviewed. '
          "We'll let you know once your account is approved.";
    case 'ACCOUNT_REJECTED':
      return "We couldn't approve your application. "
          'Please contact support or visit a branch for help.';
    default:
      return e.message;
  }
}

class CambodianPhoneFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final text = newValue.text;
    if (text.isEmpty) {
      return newValue;
    }
    // Allow deleting down to empty
    if (oldValue.text == '+855 ' && text.length < oldValue.text.length) {
      return const TextEditingValue(text: '');
    }
    final formatted = formatCambodianPhone(text);
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phoneController = TextEditingController(text: '+855 ');
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;
  String? _phoneError;
  String? _passwordError;

  @override
  void dispose() {
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    final rawPhone = _phoneController.text.trim();
    final password = _passwordController.text.trim();

    String digitsOnly = rawPhone.replaceAll(RegExp(r'[^0-9]'), '');
    if (digitsOnly.startsWith('855')) {
      digitsOnly = digitsOnly.substring(3);
    }
    if (digitsOnly.startsWith('0')) {
      digitsOnly = digitsOnly.substring(1);
    }
    final localDigits = digitsOnly;

    final isEmptyPhone =
        rawPhone.isEmpty || rawPhone == '+855' || rawPhone == '+855 ';

    setState(() {
      _phoneError = isEmptyPhone
          ? 'Please enter your phone number'
          : (localDigits.length < 8
                ? 'Please enter a valid phone number'
                : null);
      _passwordError = password.isEmpty ? 'Please enter your password' : null;
      _errorMessage = null;
    });

    if (isEmptyPhone || localDigits.length < 8 || password.isEmpty) return;

    final normalizedPhone = normalizePhoneToE164(rawPhone);

    setState(() {
      _isLoading = true;
    });

    try {
      final response = await AuthApi().login(
        phone: normalizedPhone,
        password: password,
      );

      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => TwoFactorScreen(
            phone: normalizedPhone,
            challengeToken: response.challengeToken,
          ),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = loginErrorMessage(e));
    } catch (_) {
      setState(
        () => _errorMessage = "Couldn't sign in. Please check your connection.",
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return GradientScaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(
            Icons.arrow_back_ios_new,
            color: Colors.white,
            size: 20,
          ),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Sign In',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 10),
            const Text(
              'Welcome Back',
              style: TextStyle(
                fontSize: 28,
                fontWeight: FontWeight.w800,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Sign in with your registered phone number',
              style: TextStyle(color: Colors.white70, fontSize: 14),
            ),
            const SizedBox(height: 28),

            if (_errorMessage != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.primaryRed.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: AppTheme.primaryRedLight.withValues(alpha: 0.5),
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.error_outline,
                      color: AppTheme.primaryRedLight,
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
            ],

            BankInputField(
              label: 'Phone Number',
              hint: '+855 12 000 001',
              controller: _phoneController,
              keyboardType: TextInputType.phone,
              textInputAction: TextInputAction.next,
              autofillHints: const [AutofillHints.telephoneNumber],
              prefixIcon: Icons.phone_outlined,
              inputFormatters: [CambodianPhoneFormatter()],
              errorText: _phoneError,
              onChanged: (_) {
                if (_phoneError != null) setState(() => _phoneError = null);
              },
            ),
            const SizedBox(height: 20),
            BankInputField(
              label: 'Password',
              hint: '••••••••',
              controller: _passwordController,
              obscureText: _obscurePassword,
              prefixIcon: Icons.lock_outline,
              errorText: _passwordError,
              textInputAction: TextInputAction.done,
              autofillHints: const [AutofillHints.password],
              trailingLabelWidget: InkWell(
                borderRadius: BorderRadius.circular(8),
                // Keeps the tap target at least 44px tall without changing the layout.
                onTap: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (_) => const ForgotPasswordScreen(),
                  ),
                ),
                child: const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12, horizontal: 4),
                  child: Text(
                    'Forgot Password?',
                    style: TextStyle(
                      fontSize: 12,
                      color: AppTheme.emeraldLight,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscurePassword
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                  color: Colors.white70,
                  size: 20,
                ),
                onPressed: () =>
                    setState(() => _obscurePassword = !_obscurePassword),
              ),
              onChanged: (_) {
                if (_passwordError != null) {
                  setState(() => _passwordError = null);
                }
              },
              onSubmitted: (_) => _handleLogin(),
            ),
            const SizedBox(height: 32),
            PrimaryActionButton(
              title: 'SIGN IN',
              isLoading: _isLoading,
              onPressed: _handleLogin,
            ),
            const SizedBox(height: 24),
            Center(
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text(
                    "Don't have an account? ",
                    style: TextStyle(color: Colors.white70, fontSize: 13),
                  ),
                  GestureDetector(
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const PhoneEntryScreen(),
                      ),
                    ),
                    child: const Text(
                      'Open Account',
                      style: TextStyle(
                        color: AppTheme.emeraldLight,
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
