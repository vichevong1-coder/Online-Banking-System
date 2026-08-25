import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';

class OpenAccountScreen extends StatefulWidget {
  const OpenAccountScreen({this.accountApi, super.key});

  final AccountApi? accountApi;

  @override
  State<OpenAccountScreen> createState() => _OpenAccountScreenState();
}

class _OpenAccountScreenState extends State<OpenAccountScreen> {
  AccountType _accountType = AccountType.savings;
  Currency _currency = Currency.usd;
  bool _isSubmitting = false;
  String? _errorText;

  Future<void> _submit() async {
    setState(() {
      _isSubmitting = true;
      _errorText = null;
    });
    try {
      final api = widget.accountApi ?? AccountApi();
      final account = await api.openAccount(
        OpenAccountRequest(accountType: _accountType, currency: _currency),
      );
      if (!mounted) return;
      Navigator.of(context).pop(account);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _errorText = e.message);
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
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
        title: const Text('Open New Account', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const Text(
              'Select Account Details',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
            ),
            const SizedBox(height: 6),
            const Text(
              'Instant auto-approval for Savings and Checking accounts in USD or KHR.',
              style: TextStyle(color: Colors.white70, fontSize: 13),
            ),
            const SizedBox(height: 24),

            if (_errorText != null) ...[
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppTheme.primaryRed.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: AppTheme.primaryRedLight.withValues(alpha: 0.5)),
                ),
                child: Text(_errorText!, style: const TextStyle(color: Colors.white, fontSize: 13)),
              ),
              const SizedBox(height: 20),
            ],

            GlassCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Account Type', style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<AccountType>(
                    initialValue: _accountType,
                    dropdownColor: const Color(0xFF0A2B24),
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                    items: AccountType.values
                        .map((type) => DropdownMenuItem(value: type, child: Text(type.label)))
                        .toList(),
                    onChanged: _isSubmitting ? null : (value) => setState(() => _accountType = value!),
                  ),
                  const SizedBox(height: 20),
                  const Text('Currency', style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<Currency>(
                    initialValue: _currency,
                    dropdownColor: const Color(0xFF0A2B24),
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600),
                    items: Currency.values
                        .map((currency) => DropdownMenuItem(value: currency, child: Text(currency.toJson())))
                        .toList(),
                    onChanged: _isSubmitting ? null : (value) => setState(() => _currency = value!),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 32),
            PrimaryActionButton(
              title: 'OPEN ACCOUNT NOW',
              isLoading: _isSubmitting,
              onPressed: _submit,
            ),
          ],
        ),
      ),
    );
  }
}
