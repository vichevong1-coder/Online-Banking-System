import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';
import 'package:mobile/features/auth/data/nid_parser.dart';

/// Everything collected across the signup screens. It lives only in memory and
/// is handed from screen to screen; nothing is sent until the selfie step.
class RegistrationDraft {
  RegistrationDraft({required this.phone});

  final String phone;
  CapturedImage? nidFront;
  CapturedImage? nidBack;
  CapturedImage? selfie;

  /// What OCR read from the card; null if reading was not attempted or failed.
  NidFields? scanned;

  /// The details the customer confirmed, with the password.
  RegisterRequest? details;
}
