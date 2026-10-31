import 'dart:typed_data';

import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image_picker/image_picker.dart';

/// A photo taken (or chosen) during signup. The bytes stay in memory until the
/// registration OTP is verified, because the backend only accepts uploads with
/// the token that verification returns.
class CapturedImage {
  const CapturedImage({required this.path, required this.bytes});

  final String path;
  final Uint8List bytes;
}

/// Takes the signup photos. Tests replace this so no camera is needed.
abstract class ImageCapturer {
  /// Returns null when the customer cancels. Throws when the camera or gallery
  /// cannot be opened (for example, permission denied).
  Future<CapturedImage?> capture({
    required bool selfie,
    bool fromGallery = false,
  });
}

class DeviceImageCapturer implements ImageCapturer {
  DeviceImageCapturer({ImagePicker? picker}) : _picker = picker ?? ImagePicker();

  final ImagePicker _picker;

  @override
  Future<CapturedImage?> capture({
    required bool selfie,
    bool fromGallery = false,
  }) async {
    // Capped at 1600px so the JPEG stays far below the backend's 5 MB limit
    // while the card text remains legible for OCR and for the reviewer.
    final file = await _picker.pickImage(
      source: fromGallery ? ImageSource.gallery : ImageSource.camera,
      preferredCameraDevice: selfie ? CameraDevice.front : CameraDevice.rear,
      maxWidth: 1600,
      maxHeight: 1600,
      imageQuality: 85,
    );
    if (file == null) return null;
    return CapturedImage(path: file.path, bytes: await file.readAsBytes());
  }
}

/// Reads the text on an ID card photo. Tests replace this.
abstract class NidTextReader {
  Future<String> read(String imagePath);
}

/// On-device ML Kit, Latin script only: the photo never leaves the phone for OCR.
class MlKitNidTextReader implements NidTextReader {
  @override
  Future<String> read(String imagePath) async {
    final recognizer = TextRecognizer(script: TextRecognitionScript.latin);
    try {
      final result = await recognizer.processImage(
        InputImage.fromFilePath(imagePath),
      );
      return result.text;
    } finally {
      await recognizer.close();
    }
  }
}
