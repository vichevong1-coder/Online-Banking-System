import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;

// Backend: com.obs.backend.feature.account.entity.AccountType (US-013/014).
enum AccountType {
  savings,
  checking;

  String toJson() => this == AccountType.savings ? 'SAVINGS' : 'CHECKING';

  static AccountType fromJson(String value) => switch (value) {
    'SAVINGS' => AccountType.savings,
    'CHECKING' => AccountType.checking,
    _ => throw ArgumentError('Unknown account type: $value'),
  };

  String get label => this == AccountType.savings ? 'Savings' : 'Checking';
}

// Backend: com.obs.backend.feature.account.entity.Currency (US-016). One account, one currency —
// a customer goes multi-currency by holding several accounts, never by mixing currencies within one.
enum Currency {
  usd,
  khr;

  String toJson() => this == Currency.usd ? 'USD' : 'KHR';

  static Currency fromJson(String value) => switch (value) {
    'USD' => Currency.usd,
    'KHR' => Currency.khr,
    _ => throw ArgumentError('Unknown currency: $value'),
  };
}

// Backend: com.obs.backend.feature.account.entity.TransactionType (US-017/018).
enum TransactionType {
  deposit,
  withdrawal,
  transferIn,
  transferOut;

  String toJson() => switch (this) {
    TransactionType.deposit => 'DEPOSIT',
    TransactionType.withdrawal => 'WITHDRAWAL',
    TransactionType.transferIn => 'TRANSFER_IN',
    TransactionType.transferOut => 'TRANSFER_OUT',
  };

  static TransactionType fromJson(String value) => switch (value) {
    'DEPOSIT' => TransactionType.deposit,
    'WITHDRAWAL' => TransactionType.withdrawal,
    'TRANSFER_IN' => TransactionType.transferIn,
    'TRANSFER_OUT' => TransactionType.transferOut,
    _ => throw ArgumentError('Unknown transaction type: $value'),
  };

  String get label => switch (this) {
    TransactionType.deposit => 'Deposit',
    TransactionType.withdrawal => 'Withdrawal',
    TransactionType.transferIn => 'Transfer in',
    TransactionType.transferOut => 'Transfer out',
  };

  bool get isCredit => this == TransactionType.deposit || this == TransactionType.transferIn;
}

// Backend: com.obs.backend.feature.account.dto.AccountResponse (US-013).
class Account {
  Account({
    required this.id,
    required this.accountNumber,
    required this.accountType,
    required this.currency,
    required this.balance,
    required this.createdAt,
  });

  final String id;
  final String accountNumber;
  final AccountType accountType;
  final Currency currency;
  final double balance;
  final DateTime createdAt;

  factory Account.fromJson(Map<String, dynamic> json) => Account(
    id: json['id'] as String,
    accountNumber: json['accountNumber'] as String,
    accountType: AccountType.fromJson(json['accountType'] as String),
    currency: Currency.fromJson(json['currency'] as String),
    balance: (json['balance'] as num).toDouble(),
    createdAt: DateTime.parse(json['createdAt'] as String),
  );
}

// Backend: com.obs.backend.feature.account.dto.BalanceResponse (US-015).
class Balance {
  Balance({required this.accountId, required this.accountNumber, required this.currency, required this.balance});

  final String accountId;
  final String accountNumber;
  final Currency currency;
  final double balance;

  factory Balance.fromJson(Map<String, dynamic> json) => Balance(
    accountId: json['accountId'] as String,
    accountNumber: json['accountNumber'] as String,
    currency: Currency.fromJson(json['currency'] as String),
    balance: (json['balance'] as num).toDouble(),
  );
}

// Backend: com.obs.backend.feature.account.dto.TransactionResponse (US-017/018).
class BankTransaction {
  BankTransaction({
    required this.id,
    required this.type,
    required this.amount,
    required this.currency,
    required this.description,
    required this.balanceAfter,
    required this.createdAt,
  });

  final String id;
  final TransactionType type;
  final double amount;
  final Currency currency;
  final String? description;
  final double balanceAfter;
  final DateTime createdAt;

  factory BankTransaction.fromJson(Map<String, dynamic> json) => BankTransaction(
    id: json['id'] as String,
    type: TransactionType.fromJson(json['type'] as String),
    amount: (json['amount'] as num).toDouble(),
    currency: Currency.fromJson(json['currency'] as String),
    description: json['description'] as String?,
    balanceAfter: (json['balanceAfter'] as num).toDouble(),
    createdAt: DateTime.parse(json['createdAt'] as String),
  );
}

// Backend: com.obs.backend.common.dto.PageResponse<T> (generic Spring Data page wrapper).
class TransactionPage {
  TransactionPage({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<BankTransaction> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory TransactionPage.fromJson(Map<String, dynamic> json) => TransactionPage(
    content: (json['content'] as List<dynamic>)
        .map((e) => BankTransaction.fromJson(e as Map<String, dynamic>))
        .toList(),
    page: json['page'] as int,
    size: json['size'] as int,
    totalElements: json['totalElements'] as int,
    totalPages: json['totalPages'] as int,
  );

  bool get hasMore => page + 1 < totalPages;
}

// Backend: com.obs.backend.feature.account.dto.OpenAccountRequest (US-014).
class OpenAccountRequest {
  OpenAccountRequest({required this.accountType, required this.currency});

  final AccountType accountType;
  final Currency currency;

  Map<String, dynamic> toJson() => {'accountType': accountType.toJson(), 'currency': currency.toJson()};
}

// Backend: com.obs.backend.feature.account.dto.TransactionFilter (US-017/018), plus paging.
// Builds the query map for GET /accounts/{id}/transactions — keys are omitted rather than sent
// empty, since @RequestParam BigDecimal/LocalDate fields 400 on an empty string.
class TransactionQuery {
  TransactionQuery({this.type, this.fromDate, this.toDate, this.minAmount, this.maxAmount, this.page = 0, this.size = 20});

  final TransactionType? type;
  final DateTime? fromDate;
  final DateTime? toDate;
  final double? minAmount;
  final double? maxAmount;
  final int page;
  final int size;

  Map<String, String> toQuery() => {
    if (type != null) 'type': type!.toJson(),
    if (fromDate != null) 'fromDate': formatLocalDate(fromDate!),
    if (toDate != null) 'toDate': formatLocalDate(toDate!),
    if (minAmount != null) 'minAmount': minAmount!.toString(),
    if (maxAmount != null) 'maxAmount': maxAmount!.toString(),
    'page': page.toString(),
    'size': size.toString(),
  };
}

class AccountLookupResponse {
  AccountLookupResponse({required this.maskedName});
  
  final String maskedName;
  
  factory AccountLookupResponse.fromJson(Map<String, dynamic> json) => AccountLookupResponse(
    maskedName: json['maskedName'] as String,
  );
}
