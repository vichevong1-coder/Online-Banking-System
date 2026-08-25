class CardModel {
  CardModel({
    required this.id,
    required this.accountId,
    required this.accountNumber,
    required this.cardHolderName,
    required this.cardNumberMasked,
    required this.cardNumberLastFour,
    required this.cardType,
    required this.status,
    required this.expiryDate,
    required this.dailyLimit,
    required this.perTransactionLimit,
    required this.createdAt,
  });

  final String id;
  final String accountId;
  final String accountNumber;
  final String cardHolderName;
  final String cardNumberMasked;
  final String cardNumberLastFour;
  final String cardType;
  final String status;
  final String expiryDate;
  final double dailyLimit;
  final double perTransactionLimit;
  final String createdAt;

  bool get isBlocked => status == 'BLOCKED';

  factory CardModel.fromJson(Map<String, dynamic> json) => CardModel(
    id: json['id'] as String,
    accountId: json['accountId'] as String,
    accountNumber: json['accountNumber'] as String? ?? '',
    cardHolderName: json['cardHolderName'] as String,
    cardNumberMasked: json['cardNumberMasked'] as String,
    cardNumberLastFour: json['cardNumberLastFour'] as String,
    cardType: json['cardType'] as String? ?? 'DEBIT',
    status: json['status'] as String? ?? 'ACTIVE',
    expiryDate: json['expiryDate'] as String,
    dailyLimit: (json['dailyLimit'] as num).toDouble(),
    perTransactionLimit: (json['perTransactionLimit'] as num).toDouble(),
    createdAt: json['createdAt'] as String? ?? '',
  );
}

class CreateCardRequest {
  CreateCardRequest({
    required this.accountId,
    this.cardHolderName,
    this.pin,
  });

  final String accountId;
  final String? cardHolderName;
  final String? pin;

  Map<String, dynamic> toJson() => {
    'accountId': accountId,
    if (cardHolderName != null && cardHolderName!.isNotEmpty) 'cardHolderName': cardHolderName,
    if (pin != null && pin!.isNotEmpty) 'pin': pin,
  };
}

class UpdateCardRequest {
  UpdateCardRequest({
    this.pin,
    this.dailyLimit,
    this.perTransactionLimit,
  });

  final String? pin;
  final double? dailyLimit;
  final double? perTransactionLimit;

  Map<String, dynamic> toJson() => {
    if (pin != null && pin!.isNotEmpty) 'pin': pin,
    if (dailyLimit != null) 'dailyLimit': dailyLimit,
    if (perTransactionLimit != null) 'perTransactionLimit': perTransactionLimit,
  };
}
