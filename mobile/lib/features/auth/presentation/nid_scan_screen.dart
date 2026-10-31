import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/nid_parser.dart';
import 'package:mobile/features/auth/data/registration_draft.dart';
import 'package:mobile/features/auth/presentation/confirm_details_screen.dart';
import 'package:mobile/features/auth/presentation/onboarding_steps.dart';

/// Step 2 of registration: photograph both sides of the national ID card.
/// ML Kit reads the text on the phone to prefill the next step; the photos stay
/// in memory until the phone number is verified and are then uploaded for the
/// admin's review.
class NidScanScreen extends StatefulWidget {
  const NidScanScreen({
    super.key,
    required this.draft,
    this.capturer,
    this.reader,
  });

  final RegistrationDraft draft;

  /// Injected by tests; the device camera and ML Kit are used otherwise.
  final ImageCapturer? capturer;
  final NidTextReader? reader;

  @override
  State<NidScanScreen> createState() => _NidScanScreenState();
}

class _NidScanScreenState extends State<NidScanScreen> {
  late final ImageCapturer _capturer = widget.capturer ?? DeviceImageCapturer();
  late final NidTextReader _reader = widget.reader ?? MlKitNidTextReader();

  bool _isReading = false;
  String? _errorMessage;

  RegistrationDraft get _draft => widget.draft;
  bool get _hasBoth => _draft.nidFront != null && _draft.nidBack != null;

  Future<void> _capture({
    required bool front,
    bool fromGallery = false,
  }) async {
    setState(() => _errorMessage = null);
    try {
      final image = await _capturer.capture(
        selfie: false,
        fromGallery: fromGallery,
      );
      if (image == null || !mounted) return;
      setState(() {
        if (front) {
          _draft.nidFront = image;
        } else {
          _draft.nidBack = image;
        }
      });
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

  /// OCR is a convenience: any failure leaves the form empty rather than
  /// blocking the customer.
  Future<NidFields?> _scan() async {
    try {
      final texts = await Future.wait([
        _reader.read(_draft.nidFront!.path),
        _reader.read(_draft.nidBack!.path),
      ]);
      final fields = parseNidText(texts.join('\n'));
      return fields.isEmpty ? null : fields;
    } catch (_) {
      return null;
    }
  }

  Future<void> _continue() async {
    if (!_hasBoth) return;
    setState(() => _isReading = true);
    _draft.scanned = await _scan();
    if (!mounted) return;
    setState(() => _isReading = false);
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => ConfirmDetailsScreen(draft: _draft)),
    );
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
          'Scan your ID',
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
              const OnboardingSteps(currentStep: 2),
              const SizedBox(height: 28),
              const Text(
                'Scan your national ID',
                style: TextStyle(
                  fontSize: 26,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 10),
              const Text(
                'Photograph the front and back of your Cambodian ID card on a '
                'flat, well-lit surface. We read it on your phone to fill in '
                'your details, and a bank officer checks the photos before '
                'your account opens.',
                style: TextStyle(
                  fontSize: 13,
                  color: Colors.white70,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 24),
              _CaptureTile(
                label: 'Front of card',
                image: _draft.nidFront,
                onCamera: () => _capture(front: true),
                onGallery: () => _capture(front: true, fromGallery: true),
              ),
              const SizedBox(height: 16),
              _CaptureTile(
                label: 'Back of card',
                image: _draft.nidBack,
                onCamera: () => _capture(front: false),
                onGallery: () => _capture(front: false, fromGallery: true),
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
                isLoading: _isReading,
                onPressed: _hasBoth ? _continue : null,
              ),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }
}

class _CaptureTile extends StatelessWidget {
  const _CaptureTile({
    required this.label,
    required this.image,
    required this.onCamera,
    required this.onGallery,
  });

  final String label;
  final CapturedImage? image;
  final VoidCallback onCamera;
  final VoidCallback onGallery;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w700,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 12),
          AspectRatio(
            aspectRatio: 1.586,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: image == null
                  ? Container(
                      color: Colors.white10,
                      child: const Icon(
                        Icons.badge_outlined,
                        size: 40,
                        color: Colors.white38,
                      ),
                    )
                  : Image.memory(image!.bytes, fit: BoxFit.cover),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              TextButton.icon(
                onPressed: onCamera,
                icon: const Icon(
                  Icons.photo_camera_outlined,
                  size: 18,
                  color: AppTheme.emeraldLight,
                ),
                label: Text(
                  image == null ? 'Take photo' : 'Retake',
                  style: const TextStyle(color: AppTheme.emeraldLight),
                ),
              ),
              TextButton(
                onPressed: onGallery,
                child: const Text(
                  'Choose from gallery',
                  style: TextStyle(color: Colors.white70),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
