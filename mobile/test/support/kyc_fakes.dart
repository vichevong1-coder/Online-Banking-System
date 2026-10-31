import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/data/auth_api.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/data/kyc_capture.dart';

/// A valid 1x1 PNG so Image.memory can decode it.
final Uint8List tinyPng = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
);

class FakeCapturer implements ImageCapturer {
  FakeCapturer({this.fail = false});

  final bool fail;
  int calls = 0;

  @override
  Future<CapturedImage?> capture({
    required bool selfie,
    bool fromGallery = false,
  }) async {
    calls++;
    if (fail) throw Exception('camera denied');
    return CapturedImage(path: '/fake/$calls.png', bytes: tinyPng);
  }
}

/// Records calls and fails on demand; every method not listed here throws.
class FakeAuthApi extends Fake implements AuthApi {
  FakeAuthApi({
    this.registerError,
    this.uploadToken = 'upload-token',
    this.uploadFailures = 0,
  });

  final Object? registerError;

  /// What OTP verification returns; null means "nothing to upload".
  final String? uploadToken;

  /// How many upload calls fail before they start succeeding.
  int uploadFailures;

  int registerCalls = 0;
  int verifyCalls = 0;
  final List<({String token, KycDocumentType type})> uploads = [];

  @override
  Future<void> register(RegisterRequest request) async {
    registerCalls++;
    if (registerError != null) throw registerError!;
  }

  @override
  Future<String?> verifyRegistrationOtp({
    required String phone,
    required String code,
  }) async {
    verifyCalls++;
    return uploadToken;
  }

  @override
  Future<List<String>> uploadKycDocument({
    required String uploadToken,
    required KycDocumentType type,
    required Uint8List bytes,
  }) async {
    if (uploadFailures > 0) {
      uploadFailures--;
      throw Exception('network down');
    }
    uploads.add((token: uploadToken, type: type));
    return const [];
  }
}

RegisterRequest sampleRegisterRequest() => RegisterRequest(
  firstName: 'Sokha',
  lastName: 'Chan',
  password: 'Passw0rd!',
  nidNumber: '123456789',
  nidExpiryDate: DateTime(2030, 6, 10),
  dateOfBirth: DateTime(1995, 3, 5),
  gender: Gender.male,
  phone: '+85512000001',
);
