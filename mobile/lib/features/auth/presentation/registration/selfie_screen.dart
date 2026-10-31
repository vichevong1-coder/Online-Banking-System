import 'package:flutter/material.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/registration_draft.dart';
import 'package:mobile/features/auth/presentation/registration/onboarding_steps.dart';
import 'package:mobile/features/auth/presentation/registration/otp_verification_screen.dart';

/// Step 4 of registration: a selfie for the officer to compare with the ID
/// photo. Continuing creates the account (`/auth/register`) and sends the SMS
/// code; the images themselves are uploaded after the code is verified.
class SelfieScreen extends StatefulWidget {
  const SelfieScreen({
    super.key,
    required this.draft,
    this.capturer,
    this.authApi,
  });

  final RegistrationDraft draft;

  /// Injected by tests; the device camera and the real API are used otherwise.
  final ImageCapturer? capturer;
  final AuthApi? authApi;

  @override
  State<SelfieScreen> createState() => _SelfieScreenState();
}

class _SelfieScreenState extends State<SelfieScreen> {
  late final ImageCapturer _capturer = widget.capturer ?? DeviceImageCapturer();
  late final AuthApi _api = widget.authApi ?? AuthApi();

  bool _isLoading = false;
  String? _errorMessage;

  RegistrationDraft get _draft => widget.draft;

  Future<void> _capture({bool fromGallery = false}) async {
    setState(() => _errorMessage = null);
    try {
      final image = await _capturer.capture(
        selfie: true,
        fromGallery: fromGallery,
      );
      if (image == null || !mounted) return;
      setState(() => _draft.selfie = image);
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _errorMessage = fromGallery
            ? "Couldn't open your photos."
            : "Couldn't open the camera. Allow camera access in Settings, "
                  'or choose a photo from your gallery.',
      );
    }
  }

  Future<void> _register() async {
    final details = _draft.details;
    if (_draft.selfie == null || details == null) return;

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      await _api.register(details);
      if (!mounted) return;
      // pushReplacement, not push: going back from the OTP screen would land on
      // a button that re-POSTs /auth/register for a phone that now exists.
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => OtpVerificationScreen(draft: _draft)),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      final fields = e.fieldErrors;
      setState(
        () => _errorMessage = fields != null && fields.isNotEmpty
            ? 'Some details were rejected (${fields.values.join(', ')}). '
                  'Go back and correct them.'
            : e.message,
      );
    } catch (_) {
      if (!mounted) return;
      setState(
        () =>
            _errorMessage = "Couldn't complete registration. Please try again.",
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final selfie = _draft.selfie;
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
          'Take a selfie',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const OnboardingSteps(currentStep: 4),
              const SizedBox(height: 28),
              const Text(
                'Show us your face',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 10),
              const Text(
                'Face the camera in good light, without a hat or sunglasses. '
                'A bank officer compares it with your ID photo.',
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.white70,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 24),
              GlassCard(
                child: Column(
                  children: [
                    Center(
                      child: ClipOval(
                        child: SizedBox(
                          width: 200,
                          height: 200,
                          child: selfie == null
                              ? Container(
                                  color: Colors.white10,
                                  child: const Icon(
                                    Icons.face_outlined,
                                    size: 64,
                                    color: Colors.white38,
                                  ),
                                )
                              : Image.memory(selfie.bytes, fit: BoxFit.cover),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        TextButton.icon(
                          onPressed: _isLoading ? null : () => _capture(),
                          icon: const Icon(
                            Icons.photo_camera_front_outlined,
                            size: 18,
                            color: AppTheme.emeraldLight,
                          ),
                          label: Text(
                            selfie == null ? 'Take selfie' : 'Retake',
                            style: const TextStyle(
                              color: AppTheme.emeraldLight,
                            ),
                          ),
                        ),
                        TextButton(
                          onPressed: _isLoading
                              ? null
                              : () => _capture(fromGallery: true),
                          child: const Text(
                            'Choose from gallery',
                            style: TextStyle(color: Colors.white70),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              if (_errorMessage != null) ...[
                const SizedBox(height: 14),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(
                      Icons.error_outline,
                      color: AppTheme.primaryRedLight,
                      size: 16,
                    ),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: const TextStyle(
                          color: AppTheme.primaryRedLight,
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
              const SizedBox(height: 28),
              PrimaryActionButton(
                title: 'CONTINUE',
                isLoading: _isLoading,
                onPressed: selfie == null ? null : _register,
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}
