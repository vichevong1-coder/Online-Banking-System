import 'package:flutter/material.dart';

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/presentation/otp_verification_screen.dart';

// US-007: KYC form & manual ID entry. Field list matches sprint-plan.md's US-007 note exactly
// (first/last name, Cambodian NID number, NID expiry date, date of birth, gender, phone) plus
// password, which the form itself requires but isn't part of the KYC data. No email field —
// customers are never identified or contacted by email anywhere in this app.
class RegistrationScreen extends StatefulWidget {
  const RegistrationScreen({super.key});

  @override
  State<RegistrationScreen> createState() => _RegistrationScreenState();
}

class _RegistrationScreenState extends State<RegistrationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _authApi = AuthApi();

  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _nidNumberController = TextEditingController();
  final _phoneController = TextEditingController();

  DateTime? _nidExpiryDate;
  DateTime? _dateOfBirth;
  Gender? _gender;

  final Map<String, String> _serverFieldErrors = {};
  bool _isSubmitting = false;
  String? _formError;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _passwordController.dispose();
    _nidNumberController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  String? _fieldValidator(String key, String? value, {int? minLength}) {
    final serverError = _serverFieldErrors[key];
    if (serverError != null) {
      return serverError;
    }
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }
    if (minLength != null && value.length < minLength) {
      return 'Must be at least $minLength characters';
    }
    return null;
  }

  Future<void> _pickDate({required bool isExpiry}) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: isExpiry ? now.add(const Duration(days: 365)) : DateTime(now.year - 25),
      firstDate: isExpiry ? now.add(const Duration(days: 1)) : DateTime(now.year - 120),
      lastDate: isExpiry ? DateTime(now.year + 20) : now.subtract(const Duration(days: 1)),
    );
    if (picked == null || !mounted) {
      return;
    }
    setState(() {
      if (isExpiry) {
        _nidExpiryDate = picked;
        _serverFieldErrors.remove('nidExpiryDate');
      } else {
        _dateOfBirth = picked;
        _serverFieldErrors.remove('dateOfBirth');
      }
    });
  }

  Future<void> _submit() async {
    setState(() => _formError = null);
    final isValid = _formKey.currentState!.validate();
    if (_nidExpiryDate == null || _dateOfBirth == null || _gender == null) {
      setState(() => _formError = 'Please complete every field.');
      return;
    }
    if (!isValid) {
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      await _authApi.register(
        RegisterRequest(
          firstName: _firstNameController.text.trim(),
          lastName: _lastNameController.text.trim(),
          password: _passwordController.text,
          nidNumber: _nidNumberController.text.trim(),
          nidExpiryDate: _nidExpiryDate!,
          dateOfBirth: _dateOfBirth!,
          gender: _gender!,
          phone: _phoneController.text.trim(),
        ),
      );
      if (!mounted) {
        return;
      }
      Navigator.of(
        context,
      ).pushReplacement(MaterialPageRoute(builder: (_) => OtpVerificationScreen(phone: _phoneController.text.trim())));
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        if (e.fieldErrors != null) {
          _serverFieldErrors
            ..clear()
            ..addAll(e.fieldErrors!);
          _formKey.currentState!.validate();
        } else {
          _formError = e.message;
        }
      });
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Create your account')),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              TextFormField(
                controller: _firstNameController,
                decoration: const InputDecoration(labelText: 'First name'),
                validator: (v) => _fieldValidator('firstName', v),
                onChanged: (_) => setState(() => _serverFieldErrors.remove('firstName')),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _lastNameController,
                decoration: const InputDecoration(labelText: 'Last name'),
                validator: (v) => _fieldValidator('lastName', v),
                onChanged: (_) => setState(() => _serverFieldErrors.remove('lastName')),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _passwordController,
                decoration: const InputDecoration(labelText: 'Password'),
                obscureText: true,
                validator: (v) => _fieldValidator('password', v, minLength: 8),
                onChanged: (_) => setState(() => _serverFieldErrors.remove('password')),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _nidNumberController,
                decoration: const InputDecoration(labelText: 'NID number'),
                validator: (v) => _fieldValidator('nidNumber', v),
                onChanged: (_) => setState(() => _serverFieldErrors.remove('nidNumber')),
              ),
              const SizedBox(height: 12),
              _DatePickerField(
                label: 'NID expiry date',
                value: _nidExpiryDate,
                errorText: _serverFieldErrors['nidExpiryDate'],
                onTap: () => _pickDate(isExpiry: true),
              ),
              const SizedBox(height: 12),
              _DatePickerField(
                label: 'Date of birth',
                value: _dateOfBirth,
                errorText: _serverFieldErrors['dateOfBirth'],
                onTap: () => _pickDate(isExpiry: false),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<Gender>(
                initialValue: _gender,
                decoration: InputDecoration(labelText: 'Gender', errorText: _serverFieldErrors['gender']),
                items: const [
                  DropdownMenuItem(value: Gender.female, child: Text('Female')),
                  DropdownMenuItem(value: Gender.male, child: Text('Male')),
                ],
                onChanged: (value) => setState(() {
                  _gender = value;
                  _serverFieldErrors.remove('gender');
                }),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _phoneController,
                decoration: const InputDecoration(labelText: 'Phone number', helperText: "We'll text a code to this"),
                keyboardType: TextInputType.phone,
                validator: (v) => _fieldValidator('phone', v),
                onChanged: (_) => setState(() => _serverFieldErrors.remove('phone')),
              ),
              if (_formError != null) ...[
                const SizedBox(height: 16),
                Text(_formError!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _isSubmitting ? null : _submit,
                child: _isSubmitting
                    ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text('Continue'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _DatePickerField extends StatelessWidget {
  const _DatePickerField({required this.label, required this.value, required this.onTap, this.errorText});

  final String label;
  final DateTime? value;
  final String? errorText;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: InputDecorator(
        decoration: InputDecoration(labelText: label, errorText: errorText),
        child: Text(value == null ? 'Select a date' : formatLocalDate(value!)),
      ),
    );
  }
}
