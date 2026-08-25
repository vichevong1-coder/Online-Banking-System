import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/qr/data/qr_payload.dart';

// The payload grammar is docs/qr-payments-spec.md's: OBS1:<type>:<target>
// [:<currency>:<amount>]. These are the cases the scanner has to get right
// before anything is sent to POST /qr/pay.
void main() {
  group('QrPayload.tryParse', () {
    test('parses a personal payload with no amount', () {
      final payload = QrPayload.tryParse('OBS1:P:900000000001')!;

      expect(payload.isPersonal, isTrue);
      expect(payload.target, '900000000001');
      expect(payload.hasFixedAmount, isFalse);
      expect(payload.destinationLabel, 'Account 900000000001');
    });

    test('parses a merchant payload with a fixed amount', () {
      final payload = QrPayload.tryParse('OBS1:M:MERCH-ANGKOR:USD:12.50')!;

      expect(payload.isPersonal, isFalse);
      expect(payload.target, 'MERCH-ANGKOR');
      expect(payload.currency, Currency.usd);
      expect(payload.amount, 12.50);
      expect(payload.hasFixedAmount, isTrue);
      expect(payload.destinationLabel, 'Merchant MERCH-ANGKOR');
    });

    test('trims surrounding whitespace from a scan', () {
      expect(QrPayload.tryParse('  OBS1:P:900000000001 ')?.target, '900000000001');
    });

    test('rejects anything that is not an OBS1 payment code', () {
      expect(QrPayload.tryParse('https://example.com'), isNull);
      expect(QrPayload.tryParse('OBS2:P:900000000001'), isNull);
      expect(QrPayload.tryParse('OBS1:P'), isNull);
      expect(QrPayload.tryParse('OBS1:X:900000000001'), isNull);
      expect(QrPayload.tryParse('OBS1:P:'), isNull);
    });

    test('rejects a malformed currency or amount rather than paying blind', () {
      expect(QrPayload.tryParse('OBS1:M:MERCH-ANGKOR:GBP:12.50'), isNull);
      expect(QrPayload.tryParse('OBS1:M:MERCH-ANGKOR:USD:abc'), isNull);
      expect(QrPayload.tryParse('OBS1:M:MERCH-ANGKOR:USD:0'), isNull);
    });
  });
}
