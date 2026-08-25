class QrPayloadResponse {
  QrPayloadResponse({required this.payload});

  final String payload;

  factory QrPayloadResponse.fromJson(Map<String, dynamic> json) =>
      QrPayloadResponse(payload: json['payload'] as String);
}

class QrPayRequest {
  QrPayRequest({
    required this.payload,
    required this.fromAccountId,
    this.amount,
    this.currency,
    this.description,
  });

  final String payload;
  final String fromAccountId;
  final double? amount;
  final String? currency;
  final String? description;

  Map<String, dynamic> toJson() => {
    'payload': payload,
    'fromAccountId': fromAccountId,
    if (amount != null) 'amount': amount,
    if (currency != null) 'currency': currency,
    if (description != null && description!.isNotEmpty) 'description': description,
  };
}
