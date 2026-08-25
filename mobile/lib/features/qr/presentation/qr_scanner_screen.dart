import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/features/qr/data/qr_payload.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// US-033's camera half. Pops the scanned payload string back to the caller —
/// it never pays anything itself, so the confirmation step the QR spec requires
/// stays on the screen that owns the source account.
///
/// Pushed rather than embedded in a tab on purpose: the camera then runs only
/// while this screen is on top, and is torn down when it is popped.
class QrScannerScreen extends StatefulWidget {
  const QrScannerScreen({super.key});

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen> {
  final MobileScannerController _controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.noDuplicates,
    formats: const [BarcodeFormat.qrCode],
  );

  bool _handled = false;
  String? _rejected;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onDetect(BarcodeCapture capture) {
    if (_handled) return;

    for (final barcode in capture.barcodes) {
      final value = barcode.rawValue;
      if (value == null || value.isEmpty) continue;

      final payload = QrPayload.tryParse(value);
      if (payload == null) {
        // Keep scanning — the customer may simply have framed a shop-loyalty
        // code. Telling them why is more useful than silently ignoring it.
        setState(() => _rejected = "That isn't an Online Banking payment code.");
        continue;
      }

      _handled = true;
      Navigator.pop(context, payload);
      return;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      extendBodyBehindAppBar: true,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.white),
        title: const Text('Scan to pay', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        actions: [
          IconButton(
            tooltip: 'Torch',
            icon: const Icon(Icons.flashlight_on_outlined, color: Colors.white),
            onPressed: () => _controller.toggleTorch(),
          ),
          IconButton(
            tooltip: 'Switch camera',
            icon: const Icon(Icons.cameraswitch_outlined, color: Colors.white),
            onPressed: () => _controller.switchCamera(),
          ),
        ],
      ),
      body: Stack(
        fit: StackFit.expand,
        children: [
          MobileScanner(
            controller: _controller,
            onDetect: _onDetect,
            errorBuilder: (context, error) => _CameraProblem(
              message: switch (error.errorCode) {
                MobileScannerErrorCode.permissionDenied =>
                  'Camera access is off. Turn it on in Settings, or type the code instead.',
                MobileScannerErrorCode.unsupported =>
                  "This device can't scan QR codes. Type the code instead.",
                _ => "The camera couldn't start. Type the code instead.",
              },
              onTypeInstead: () => Navigator.pop(context),
            ),
          ),

          // Reticle
          IgnorePointer(
            child: Center(
              child: Container(
                width: 240,
                height: 240,
                decoration: BoxDecoration(
                  border: Border.all(color: AppTheme.emeraldLight, width: 2),
                  borderRadius: BorderRadius.circular(24),
                ),
              ),
            ),
          ),

          Positioned(
            left: 24,
            right: 24,
            bottom: 48,
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  decoration: BoxDecoration(
                    color: Colors.black54,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Text(
                    _rejected ?? 'Point the camera at a personal or merchant QR code.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: _rejected != null ? AppTheme.primaryRedLight : Colors.white,
                      fontSize: 13,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                TextButton.icon(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.keyboard_alt_outlined, color: AppTheme.emeraldLight, size: 18),
                  label: const Text(
                    'Type the code instead',
                    style: TextStyle(color: AppTheme.emeraldLight, fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CameraProblem extends StatelessWidget {
  const _CameraProblem({required this.message, required this.onTypeInstead});

  final String message;
  final VoidCallback onTypeInstead;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.black,
      padding: const EdgeInsets.all(32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.no_photography_outlined, color: Colors.white54, size: 56),
          const SizedBox(height: 16),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.white70, fontSize: 14, height: 1.4),
          ),
          const SizedBox(height: 24),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: AppTheme.primaryEmerald),
            onPressed: onTypeInstead,
            child: const Text('Type the code instead', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
    );
  }
}
