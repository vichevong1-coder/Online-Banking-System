import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/qr/data/qr_models.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';

class QrApi {
  QrApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<QrPayloadResponse> getMyPayload(String accountId) async {
    final json = await _client.get('/qr/me', query: {'accountId': accountId});
    return QrPayloadResponse.fromJson(json as Map<String, dynamic>);
  }

  Future<TransferResponse> payQr(QrPayRequest request) async {
    final json = await _client.post('/qr/pay', request.toJson());
    return TransferResponse.fromJson(json!);
  }
}
