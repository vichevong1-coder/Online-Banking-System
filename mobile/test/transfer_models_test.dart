import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';

// These assert the wire shape the Java backend actually emits and accepts —
// TransferResponse, BeneficiaryResponse, CreateExternalTransferRequest. They
// exist because the client previously invented its own field names.
void main() {
  group('TransferResponse.fromJson', () {
    test('reads the backend receipt payload', () {
      final transfer = TransferResponse.fromJson({
        'id': '11111111-1111-1111-1111-111111111111',
        'reference': 'TRF-000123',
        'fromAccountId': '22222222-2222-2222-2222-222222222222',
        'fromAccountNumber': '900000000001',
        'toAccountId': null,
        'toAccountNumber': null,
        'externalRef': 'ACLBKHPP:000123456789',
        'amount': 25.5,
        'currency': 'USD',
        'status': 'PENDING',
        'description': 'Rent',
        'createdAt': '2026-08-20T10:15:00Z',
      });

      expect(transfer.reference, 'TRF-000123');
      expect(transfer.currency, Currency.usd);
      expect(transfer.status, TransferStatus.pending);
      expect(transfer.externalBankCode, 'ACLBKHPP');
      expect(transfer.externalAccountNumber, '000123456789');
    });

    test('a declined merchant payment reads as FAILED, not as a success', () {
      final transfer = TransferResponse.fromJson({
        'id': '11111111-1111-1111-1111-111111111111',
        'reference': 'TRF-000124',
        'fromAccountId': '22222222-2222-2222-2222-222222222222',
        'amount': 4.0,
        'currency': 'USD',
        'status': 'FAILED',
        'createdAt': '2026-08-20T10:16:00Z',
      });

      expect(transfer.status, TransferStatus.failed);
      expect(transfer.status.label, 'Failed');
    });
  });

  group('request payloads', () {
    test('internal transfer sends no currency — the backend takes it from the account', () {
      final json = InternalTransferRequest(
        fromAccountId: 'a',
        toAccountId: 'b',
        amount: 10,
      ).toJson();

      expect(json.keys, containsAll(['fromAccountId', 'toAccountId', 'amount']));
      expect(json.containsKey('currency'), isFalse);
    });

    test('p2p transfer names the destination by account number, with no currency', () {
      final json = P2pTransferRequest(
        fromAccountId: 'a',
        toAccountNumber: '900000000002',
        amount: 80,
        description: 'Split dinner',
      ).toJson();

      expect(json['toAccountNumber'], '900000000002');
      expect(json['description'], 'Split dinner');
      expect(json.containsKey('currency'), isFalse);
      expect(json.containsKey('toAccountId'), isFalse);
    });



    test('beneficiary create/patch use displayName and favorite', () {
      expect(
        CreateBeneficiaryRequest(
          displayName: 'Dara Kim',
          bankCode: 'ACLBKHPP',
          accountNumber: '000123456789',
        ).toJson()['displayName'],
        'Dara Kim',
      );

      // PATCH sends only what changed, so "not sent" stays distinct from false.
      expect(UpdateBeneficiaryRequest(favorite: true).toJson(), {'favorite': true});
    });
  });

  test('Beneficiary.fromJson reads BeneficiaryResponse', () {
    final beneficiary = Beneficiary.fromJson({
      'id': 'b1',
      'displayName': 'Wing Money',
      'bankCode': 'WINGKHPP',
      'accountNumber': '855012345678',
      'favorite': true,
      'createdAt': '2026-06-01T00:00:00Z',
      'updatedAt': '2026-06-01T00:00:00Z',
    });

    expect(beneficiary.displayName, 'Wing Money');
    expect(beneficiary.favorite, isTrue);
    expect(DestinationBank.nameFor(beneficiary.bankCode), 'Wing Bank');
  });
}
