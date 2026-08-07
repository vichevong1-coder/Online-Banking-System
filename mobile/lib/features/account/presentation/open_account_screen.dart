import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_api.dart';
import 'package:mobile/features/account/data/account_models.dart';

// US-014: auto-approved on submit — POST /accounts/requests returns the created ACTIVE account
// immediately, there's no pending/review state to show.
class OpenAccountScreen extends StatefulWidget {
  const OpenAccountScreen({required this.accountApi, super.key});

  final AccountApi accountApi;

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
      final account = await widget.accountApi.openAccount(
        OpenAccountRequest(accountType: _accountType, currency: _currency),
      );
      if (!mounted) {
        return;
      }
      Navigator.of(context).pop(account);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Open a new account')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            DropdownButtonFormField<AccountType>(
              initialValue: _accountType,
              decoration: const InputDecoration(labelText: 'Account type'),
              items: AccountType.values
                  .map((type) => DropdownMenuItem(value: type, child: Text(type.label)))
                  .toList(),
              onChanged: _isSubmitting ? null : (value) => setState(() => _accountType = value!),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<Currency>(
              initialValue: _currency,
              decoration: const InputDecoration(labelText: 'Currency', helperText: 'One currency per account'),
              items: Currency.values
                  .map((currency) => DropdownMenuItem(value: currency, child: Text(currency.toJson())))
                  .toList(),
              onChanged: _isSubmitting ? null : (value) => setState(() => _currency = value!),
            ),
            if (_errorText != null) ...[
              const SizedBox(height: 16),
              Text(_errorText!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
            ],
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _isSubmitting ? null : _submit,
              child: _isSubmitting
                  ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Text('Open account'),
            ),
          ],
        ),
      ),
    );
  }
}
