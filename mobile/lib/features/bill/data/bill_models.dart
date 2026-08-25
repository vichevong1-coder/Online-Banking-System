import 'package:mobile/features/transfer/data/transfer_models.dart';
import 'package:mobile/features/account/data/account_models.dart';
class BillProvider {
  BillProvider({
    required this.id,
    required this.name,
    required this.category,
    this.accountNumberPattern,
    required this.active,
  });

  final String id;
  final String name;
  final String category;
  final String? accountNumberPattern;
  final bool active;

  factory BillProvider.fromJson(Map<String, dynamic> json) => BillProvider(
    id: json['id'] as String,
    name: json['name'] as String,
    category: json['category'] as String,
    accountNumberPattern: json['accountNumberPattern'] as String?,
    active: json['active'] as bool? ?? true,
  );
}

class BillPayment {
  BillPayment({
    required this.id,
    required this.providerId,
    required this.providerName,
    this.providerCategory,
    required this.accountId,
    required this.accountNumber,
    required this.billAccountNumber,
    required this.amount,
    required this.currency,
    required this.transferId,
    required this.reference,
    required this.status,
    required this.createdAt,
  });

  final String id;
  final String providerId;
  final String providerName;
  final String? providerCategory;
  final String accountId;
  final String accountNumber;
  final String billAccountNumber;
  final double amount;
  final String currency;
  final String transferId;
  final String reference;
  final String status;
  final String createdAt;

  factory BillPayment.fromJson(Map<String, dynamic> json) => BillPayment(
    id: json['id'] as String,
    providerId: json['providerId'] as String,
    providerName: json['providerName'] as String? ?? 'Provider',
    providerCategory: json['providerCategory'] as String?,
    accountId: json['accountId'] as String,
    accountNumber: json['accountNumber'] as String? ?? '',
    billAccountNumber: json['billAccountNumber'] as String,
    amount: (json['amount'] as num).toDouble(),
    currency: json['currency'] as String,
    transferId: json['transferId'] as String,
    reference: json['reference'] as String,
    status: json['status'] as String,
    createdAt: json['createdAt'] as String? ?? '',
  );

  /// A bill payment settles as a transfer, so it reuses US-028's receipt
  /// screen rather than growing a second one.
  TransferResponse asTransfer() => TransferResponse(
    id: transferId,
    reference: reference,
    fromAccountId: accountId,
    fromAccountNumber: accountNumber.isEmpty ? null : accountNumber,
    amount: amount,
    currency: Currency.fromJson(currency),
    status: TransferStatus.fromJson(status),
    description: 'Bill payment - $providerName ($billAccountNumber)',
    createdAt: createdAt,
  );
}

class RecurringBillPayment {
  RecurringBillPayment({
    required this.id,
    required this.providerId,
    required this.providerName,
    this.providerCategory,
    required this.accountId,
    required this.accountNumber,
    required this.billAccountNumber,
    required this.amount,
    required this.currency,
    required this.frequency,
    required this.nextPaymentDate,
    required this.active,
    required this.createdAt,
  });

  final String id;
  final String providerId;
  final String providerName;
  final String? providerCategory;
  final String accountId;
  final String accountNumber;
  final String billAccountNumber;
  final double amount;
  final String currency;
  final String frequency;
  final String nextPaymentDate;
  final bool active;
  final String createdAt;

  factory RecurringBillPayment.fromJson(Map<String, dynamic> json) => RecurringBillPayment(
    id: json['id'] as String,
    providerId: json['providerId'] as String,
    providerName: json['providerName'] as String? ?? 'Provider',
    providerCategory: json['providerCategory'] as String?,
    accountId: json['accountId'] as String,
    accountNumber: json['accountNumber'] as String? ?? '',
    billAccountNumber: json['billAccountNumber'] as String,
    amount: (json['amount'] as num).toDouble(),
    currency: json['currency'] as String,
    frequency: json['frequency'] as String,
    nextPaymentDate: json['nextPaymentDate'] as String,
    active: json['active'] as bool? ?? true,
    createdAt: json['createdAt'] as String? ?? '',
  );
}

class PayBillRequest {
  PayBillRequest({
    required this.fromAccountId,
    required this.providerId,
    required this.billAccountNumber,
    required this.amount,
    required this.currency,
  });

  final String fromAccountId;
  final String providerId;
  final String billAccountNumber;
  final double amount;
  final String currency;

  Map<String, dynamic> toJson() => {
    'fromAccountId': fromAccountId,
    'providerId': providerId,
    'billAccountNumber': billAccountNumber,
    'amount': amount,
    'currency': currency,
  };
}

class CreateRecurringBillRequest {
  CreateRecurringBillRequest({
    required this.fromAccountId,
    required this.providerId,
    required this.billAccountNumber,
    required this.amount,
    required this.currency,
    required this.frequency,
    required this.nextPaymentDate,
  });

  final String fromAccountId;
  final String providerId;
  final String billAccountNumber;
  final double amount;
  final String currency;
  final String frequency;
  final String nextPaymentDate;

  Map<String, dynamic> toJson() => {
    'fromAccountId': fromAccountId,
    'providerId': providerId,
    'billAccountNumber': billAccountNumber,
    'amount': amount,
    'currency': currency,
    'frequency': frequency,
    'nextPaymentDate': nextPaymentDate,
  };
}
