import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/presentation/onboarding_steps.dart';
import 'package:mobile/features/auth/presentation/otp_verification_screen.dart';

/// Step 2 of registration — US-007's KYC form. Fields match the sprint-plan
/// note exactly (first/last name, Cambodian NID, NID expiry, date of birth,
/// gender) plus the password, with the phone carried in from step 1. No email:
/// customers are never identified or contacted by email anywhere in this app.
///
/// Nothing here is pre-filled. The customer types their own identity data — a
/// KYC form that arrives with someone else's name in it is worse than useless.
class ConfirmDetailsScreen extends StatefulWidget {
  final String phone;

  const ConfirmDetailsScreen({super.key, required this.phone});

  @override
  State<ConfirmDetailsScreen> createState() => _ConfirmDetailsScreenState();
}

class _ConfirmDetailsScreenState extends State<ConfirmDetailsScreen> {
  // Cambodian NID numbers are exactly 9 digits — mirrors RegisterRequest's
  // @Pattern so a malformed number is caught here, not as a server 400.
  static const _nidNumberLength = 9;
  static const _minPasswordLength = 8;

  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _nidController = TextEditingController();
  final _passwordController = TextEditingController();

  DateTime? _nidExpiryDate;
  DateTime? _dateOfBirth;
  Gender? _gender;

  final Map<String, String> _serverFieldErrors = {};
  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _nidController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  String? _validate(
    String key,
    String? value, {
    int? minLength,
    int? exactLength,
    String? label,
  }) {
    final serverError = _serverFieldErrors[key];
    if (serverError != null) {
      return serverError;
    }
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }
    if (minLength != null && value.length < minLength) {
      return '${label ?? 'This'} must be at least $minLength characters';
    }
    if (exactLength != null && value.trim().length != exactLength) {
      return 'Must be exactly $exactLength digits';
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
    if (picked == null || !mounted) return;
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

  Future<void> _submitRegistration() async {
    setState(() => _errorMessage = null);
    final isValid = _formKey.currentState!.validate();

    // The pickers and the dropdown sit outside the Form's validation, so they
    // are checked by hand rather than silently submitting as null.
    if (_nidExpiryDate == null || _dateOfBirth == null || _gender == null) {
      setState(() => _errorMessage = 'Please choose your NID expiry date, date of birth and gender.');
      return;
    }
    if (!isValid) return;

    setState(() => _isLoading = true);

    try {
      final request = RegisterRequest(
        firstName: _firstNameController.text.trim(),
        lastName: _lastNameController.text.trim(),
        password: _passwordController.text,
        nidNumber: _nidController.text.trim(),
        nidExpiryDate: _nidExpiryDate!,
        dateOfBirth: _dateOfBirth!,
        gender: _gender!,
        phone: widget.phone,
      );

      await AuthApi().register(request);

      if (!mounted) return;
      // pushReplacement, not push: going "back" from the OTP screen would land
      // on a filled form whose submit would re-POST /auth/register.
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => OtpVerificationScreen(phone: widget.phone),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        if (e.fieldErrors != null && e.fieldErrors!.isNotEmpty) {
          _serverFieldErrors
            ..clear()
            ..addAll(e.fieldErrors!);
          _formKey.currentState!.validate();
        } else {
          _errorMessage = e.message;
        }
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _errorMessage = "Couldn't complete registration. Please try again.");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Widget _buildField({
    required IconData icon,
    required String label,
    required String hint,
    required TextEditingController controller,
    required String? Function(String?) validator,
    required VoidCallback onChanged,
    bool isPassword = false,
    TextInputType? keyboardType,
    int? maxLength,
    List<TextInputFormatter>? inputFormatters,
    TextCapitalization textCapitalization = TextCapitalization.none,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 15, color: Colors.white70),
            const SizedBox(width: 6),
            Text(label, style: const TextStyle(fontSize: 12, color: Colors.white70)),
          ],
        ),
        const SizedBox(height: 4),
        TextFormField(
          controller: controller,
          obscureText: isPassword && _obscurePassword,
          keyboardType: keyboardType,
          maxLength: maxLength,
          inputFormatters: inputFormatters,
          textCapitalization: textCapitalization,
          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: Colors.white),
          decoration: InputDecoration(
            isDense: true,
            hintText: hint,
            hintStyle: const TextStyle(fontSize: 13, color: Colors.white24, fontWeight: FontWeight.normal),
            counterText: '',
            contentPadding: const EdgeInsets.symmetric(vertical: 6),
            border: const UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
            enabledBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Colors.white24)),
            focusedBorder: const UnderlineInputBorder(
              borderSide: BorderSide(color: AppTheme.emeraldLight, width: 1.5),
            ),
            filled: false,
            suffixIcon: isPassword
                ? IconButton(
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                    icon: Icon(
                      _obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                      size: 18,
                      color: Colors.white54,
                    ),
                    onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                  )
                : null,
          ),
          validator: validator,
          onChanged: (_) => onChanged(),
        ),
        const SizedBox(height: 12),
      ],
    );
  }

  Widget _buildPickerField({
    required IconData icon,
    required String label,
    required DateTime? value,
    required String? errorText,
    required VoidCallback onTap,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 15, color: Colors.white70),
            const SizedBox(width: 6),
            Text(label, style: const TextStyle(fontSize: 12, color: Colors.white70)),
          ],
        ),
        const SizedBox(height: 8),
        InkWell(
          onTap: onTap,
          child: Text(
            value == null ? 'Select date' : formatLocalDate(value),
            style: TextStyle(
              fontSize: 14,
              fontWeight: value == null ? FontWeight.normal : FontWeight.w600,
              color: value == null ? Colors.white24 : Colors.white,
            ),
          ),
        ),
        const Divider(color: Colors.white24, height: 16),
        if (errorText != null)
          Text(errorText, style: const TextStyle(color: AppTheme.primaryRedLight, fontSize: 11)),
        const SizedBox(height: 8),
      ],
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
          'Your details',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const OnboardingSteps(currentStep: 2),
                const SizedBox(height: 28),
                const Text(
                  'Tell us who you are',
                  style: TextStyle(fontSize: 26, fontWeight: FontWeight.w800, color: Colors.white),
                ),
                const SizedBox(height: 10),
                Text(
                  'These details come straight off your Cambodian national ID card, and set the password for ${widget.phone}.',
                  style: const TextStyle(fontSize: 13, color: Colors.white70, height: 1.5),
                ),
                const SizedBox(height: 24),

                if (_errorMessage != null) ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryRed.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppTheme.primaryRedLight.withValues(alpha: 0.5)),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.error_outline, color: AppTheme.primaryRedLight, size: 18),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            _errorMessage!,
                            style: const TextStyle(color: Colors.white, fontSize: 13),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                ],

                GlassCard(
                  child: Column(
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: _buildField(
                              icon: Icons.person_outline,
                              label: 'First Name',
                              hint: 'e.g. Sokha',
                              controller: _firstNameController,
                              textCapitalization: TextCapitalization.words,
                              validator: (v) => _validate('firstName', v),
                              onChanged: () => setState(() => _serverFieldErrors.remove('firstName')),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: _buildField(
                              icon: Icons.person_outline,
                              label: 'Last Name',
                              hint: 'e.g. Chan',
                              controller: _lastNameController,
                              textCapitalization: TextCapitalization.words,
                              validator: (v) => _validate('lastName', v),
                              onChanged: () => setState(() => _serverFieldErrors.remove('lastName')),
                            ),
                          ),
                        ],
                      ),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: _buildField(
                              icon: Icons.badge_outlined,
                              label: 'Cambodian NID',
                              hint: '9 digits',
                              controller: _nidController,
                              keyboardType: TextInputType.number,
                              maxLength: _nidNumberLength,
                              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                              validator: (v) => _validate('nidNumber', v, exactLength: _nidNumberLength),
                              onChanged: () => setState(() => _serverFieldErrors.remove('nidNumber')),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: _buildPickerField(
                              icon: Icons.calendar_today_outlined,
                              label: 'NID Expiry',
                              value: _nidExpiryDate,
                              errorText: _serverFieldErrors['nidExpiryDate'],
                              onTap: () => _pickDate(isExpiry: true),
                            ),
                          ),
                        ],
                      ),
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: _buildPickerField(
                              icon: Icons.cake_outlined,
                              label: 'Date of Birth',
                              value: _dateOfBirth,
                              errorText: _serverFieldErrors['dateOfBirth'],
                              onTap: () => _pickDate(isExpiry: false),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Row(
                                  children: [
                                    Icon(Icons.wc_outlined, size: 15, color: Colors.white70),
                                    SizedBox(width: 6),
                                    Text('Gender', style: TextStyle(fontSize: 12, color: Colors.white70)),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                DropdownButton<Gender>(
                                  value: _gender,
                                  isExpanded: true,
                                  hint: const Text(
                                    'Select',
                                    style: TextStyle(fontSize: 14, color: Colors.white24),
                                  ),
                                  dropdownColor: const Color(0xFF0A2B24),
                                  underline: const Divider(color: Colors.white24, height: 1),
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.white,
                                  ),
                                  items: const [
                                    DropdownMenuItem(value: Gender.female, child: Text('Female')),
                                    DropdownMenuItem(value: Gender.male, child: Text('Male')),
                                  ],
                                  onChanged: (val) => setState(() {
                                    _gender = val;
                                    _serverFieldErrors.remove('gender');
                                  }),
                                ),
                                if (_serverFieldErrors['gender'] != null)
                                  Text(
                                    _serverFieldErrors['gender']!,
                                    style: const TextStyle(color: AppTheme.primaryRedLight, fontSize: 11),
                                  ),
                                const SizedBox(height: 8),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      _buildField(
                        icon: Icons.lock_outline,
                        label: 'Account Password',
                        hint: 'At least $_minPasswordLength characters',
                        controller: _passwordController,
                        isPassword: true,
                        validator: (v) => _validate(
                          'password',
                          v,
                          minLength: _minPasswordLength,
                          label: 'Password',
                        ),
                        onChanged: () => setState(() => _serverFieldErrors.remove('password')),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    Expanded(
                      child: PrimaryActionButton(
                        title: 'BACK',
                        isSecondary: true,
                        onPressed: () => Navigator.pop(context),
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: PrimaryActionButton(
                        title: 'CONTINUE',
                        isLoading: _isLoading,
                        onPressed: _submitRegistration,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
