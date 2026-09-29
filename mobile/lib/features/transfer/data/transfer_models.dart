import 'package:mobile/features/account/data/account_models.dart';

/// Mirrors the backend `TransferStatus` enum. Interbank transfers settle as
/// PENDING; internal and QR payments settle immediately as COMPLETED; a
/// rejection (a declining merchant, US-034) is written as FAILED.
enum TransferStatus {
  completed,
  pending,
  failed;

  static TransferStatus fromJson(String value) => switch (value.toUpperCase()) {
    'COMPLETED' => TransferStatus.completed,
    'PENDING' => TransferStatus.pending,
    _ => TransferStatus.failed,
  };

  String get label => switch (this) {
    TransferStatus.completed => 'Completed',
    TransferStatus.pending => 'Pending settlement',
    TransferStatus.failed => 'Failed',
  };
}

/// US-028's receipt payload, also one row of the caller's transfer history.
/// Field-for-field the backend's `TransferResponse` — the destination of an
/// interbank transfer lives in [externalRef] as "BANKCODE:ACCOUNTNUMBER",
/// because the receiving account is not a row this system holds.
class TransferResponse {
  TransferResponse({
    required this.id,
    required this.reference,
    required this.fromAccountId,
    this.fromAccountNumber,
    this.toAccountId,
    this.toAccountNumber,
    this.externalRef,
    required this.amount,
    required this.currency,
    required this.status,
    this.description,
    required this.createdAt,
  });

  final String id;
  final String reference;
  final String fromAccountId;
  final String? fromAccountNumber;
  final String? toAccountId;
  final String? toAccountNumber;
  final String? externalRef;
  final double amount;
  final Currency currency;
  final TransferStatus status;
  final String? description;
  final String createdAt;

  /// "ACLBKHPP:000123456789" → ("ACLBKHPP", "000123456789").
  String? get externalBankCode => externalRef?.split(':').first;
  String? get externalAccountNumber =>
      externalRef != null && externalRef!.contains(':') ? externalRef!.split(':').last : null;

  factory TransferResponse.fromJson(Map<String, dynamic> json) => TransferResponse(
    id: json['id'] as String,
    reference: json['reference'] as String,
    fromAccountId: json['fromAccountId'] as String,
    fromAccountNumber: json['fromAccountNumber'] as String?,
    toAccountId: json['toAccountId'] as String?,
    toAccountNumber: json['toAccountNumber'] as String?,
    externalRef: json['externalRef'] as String?,
    amount: (json['amount'] as num).toDouble(),
    currency: Currency.fromJson(json['currency'] as String),
    status: TransferStatus.fromJson(json['status'] as String),
    description: json['description'] as String?,
    createdAt: json['createdAt'] as String? ?? '',
  );
}

/// US-025: `POST /transfers`. No currency field — the backend takes it from the
/// source account and rejects a cross-currency pair outright.
class InternalTransferRequest {
  InternalTransferRequest({
    required this.fromAccountId,
    required this.toAccountId,
    required this.amount,
    this.description,
  });

  final String fromAccountId;
  final String toAccountId;
  final double amount;
  final String? description;

  Map<String, dynamic> toJson() => {
    'fromAccountId': fromAccountId,
    'toAccountId': toAccountId,
    'amount': amount,
    if (description != null && description!.isNotEmpty) 'description': description,
  };
}

/// US-026, same-bank half: `POST /transfers/p2p`. The destination is another
/// customer's account number — not an id, which a payer cannot know — and no
/// currency is sent: it is the source account's, and a mismatch is rejected.
class P2pTransferRequest {
  P2pTransferRequest({
    required this.fromAccountId,
    required this.toAccountNumber,
    required this.amount,
    this.description,
  });

  final String fromAccountId;
  final String toAccountNumber;
  final double amount;
  final String? description;

  Map<String, dynamic> toJson() => {
    'fromAccountId': fromAccountId,
    'toAccountNumber': toAccountNumber,
    'amount': amount,
    if (description != null && description!.isNotEmpty) 'description': description,
  };
}

/// US-029/US-030: a saved payee at another bank. Every beneficiary is external
/// — the backend requires a bank code, so there is no "own bank" variant.
class Beneficiary {
  Beneficiary({
    required this.id,
    required this.displayName,
    required this.bankCode,
    required this.accountNumber,
    required this.favorite,
    this.createdAt,
    this.updatedAt,
  });

  final String id;
  final String displayName;
  final String bankCode;
  final String accountNumber;
  final bool favorite;
  final String? createdAt;
  final String? updatedAt;

  factory Beneficiary.fromJson(Map<String, dynamic> json) => Beneficiary(
    id: json['id'] as String,
    displayName: json['displayName'] as String,
    bankCode: json['bankCode'] as String,
    accountNumber: json['accountNumber'] as String,
    favorite: json['favorite'] as bool? ?? false,
    createdAt: json['createdAt'] as String?,
    updatedAt: json['updatedAt'] as String?,
  );
}

class CreateBeneficiaryRequest {
  CreateBeneficiaryRequest({
    required this.displayName,
    required this.bankCode,
    required this.accountNumber,
  });

  final String displayName;
  final String bankCode;
  final String accountNumber;

  Map<String, dynamic> toJson() => {
    'displayName': displayName,
    'bankCode': bankCode,
    'accountNumber': accountNumber,
  };
}

/// Every field optional: PATCH only sends what changed, and `favorite` is
/// boxed on the backend so "not sent" stays distinct from "set to false".
class UpdateBeneficiaryRequest {
  UpdateBeneficiaryRequest({this.displayName, this.bankCode, this.accountNumber, this.favorite});

  final String? displayName;
  final String? bankCode;
  final String? accountNumber;
  final bool? favorite;

  Map<String, dynamic> toJson() => {
    if (displayName != null) 'displayName': displayName,
    if (bankCode != null) 'bankCode': bankCode,
    if (accountNumber != null) 'accountNumber': accountNumber,
    if (favorite != null) 'favorite': favorite,
  };
}

/// The banks the demo can name. Codes are SWIFT/BIC-shaped to satisfy the
/// backend's `^[A-Z0-9]{4,11}$`, and match `seed_demo_data.sql`'s beneficiaries.
class DestinationBank {
  const DestinationBank(this.code, this.name);

  final String code;
  final String name;

  static const List<DestinationBank> all = [
    DestinationBank('ABAAKHPP', 'ABA Bank'),
    DestinationBank('ACLBKHPP', 'ACLEDA Bank'),
    DestinationBank('CANAKHPP', 'Canadia Bank'),
    DestinationBank('WINGKHPP', 'Wing Bank'),
  ];

  static String nameFor(String code) =>
      all.where((b) => b.code == code).map((b) => b.name).firstOrNull ?? code;
}
