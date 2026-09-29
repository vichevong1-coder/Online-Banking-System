import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/presentation/reset_password_screen.dart';
import 'package:mobile/core/widgets/bank_input_field.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final TextEditingController _identifierController = TextEditingController();
  bool _isLoading = false;
  String? _errorMessage;
  String? _identifierError;

  Future<void> _handleForgot() async {
    final identifier = _identifierController.text.trim();
    
    setState(() {
      _identifierError = identifier.isEmpty ? 'Please enter your phone number or email' : null;
      _errorMessage = null;
    });

    if (identifier.isEmpty) return;

    setState(() {
      _isLoading = true;
    });

    try {
      await AuthApi().forgotPassword(identifier);

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Password reset verification code sent.'),
          backgroundColor: AppTheme.primaryEmerald,
        ),
      );

      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => ResetPasswordScreen(identifier: identifier),
        ),
      );
    } on ApiException catch (e) {
      setState(() => _errorMessage = e.message);
    } catch (_) {
      setState(() => _errorMessage = "Couldn't send reset code. Please try again.");
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
          icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 20),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text('Forgot Password', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 10),
            const Text(
              'Reset Account Password',
              style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: Colors.white),
            ),
            const SizedBox(height: 6),
            const Text(
              'Enter your registered phone number or email address. We will send you a 6-digit verification code.',
              style: TextStyle(color: Colors.white70, fontSize: 13, height: 1.4),
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

            BankInputField(
              label: 'Phone or Email',
              hint: '+85512345678 or user@email.com',
              controller: _identifierController,
              prefixIcon: Icons.account_circle_outlined,
              errorText: _identifierError,
              onChanged: (_) {
                if (_identifierError != null) setState(() => _identifierError = null);
              },
              onSubmitted: (_) => _handleForgot(),
            ),
            const Spacer(),
            PrimaryActionButton(
              title: 'SEND RESET CODE',
              isLoading: _isLoading,
              onPressed: _handleForgot,
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }
}
