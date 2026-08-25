import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/transfer/data/transfer_models.dart';

class TransferApi {
  TransferApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  /// US-025. The backend names this `POST /transfers` — there is no
  /// `/transfers/internal`.
  Future<TransferResponse> internalTransfer(InternalTransferRequest request) async {
    final json = await _client.post('/transfers', request.toJson());
    return TransferResponse.fromJson(json!);
  }

  /// US-026, same-bank: pays another customer by account number.
  Future<TransferResponse> p2pTransfer(P2pTransferRequest request) async {
    final json = await _client.post('/transfers/p2p', request.toJson());
    return TransferResponse.fromJson(json!);
  }

  /// US-026, other banks.
  Future<TransferResponse> interbankTransfer(InterbankTransferRequest request) async {
    final json = await _client.post('/transfers/external', request.toJson());
    return TransferResponse.fromJson(json!);
  }

  Future<TransferResponse> getTransfer(String id) async {
    final json = await _client.get('/transfers/$id');
    return TransferResponse.fromJson(json as Map<String, dynamic>);
  }

  /// US-028's history, newest first.
  Future<List<TransferResponse>> listTransfers({int page = 0, int size = 20}) async {
    final json = await _client.get('/transfers', query: {
      'page': page.toString(),
      'size': size.toString(),
    });
    final content = (json as Map<String, dynamic>)['content'] as List<dynamic>;
    return content.map((e) => TransferResponse.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Paged on the backend, so the envelope has to be unwrapped.
  Future<List<Beneficiary>> listBeneficiaries({int page = 0, int size = 50}) async {
    final json = await _client.get('/beneficiaries', query: {
      'page': page.toString(),
      'size': size.toString(),
    });
    final content = (json as Map<String, dynamic>)['content'] as List<dynamic>;
    return content.map((e) => Beneficiary.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Beneficiary> createBeneficiary(CreateBeneficiaryRequest request) async {
    final json = await _client.post('/beneficiaries', request.toJson());
    return Beneficiary.fromJson(json!);
  }

  Future<Beneficiary> updateBeneficiary(String id, UpdateBeneficiaryRequest request) async {
    final json = await _client.patch('/beneficiaries/$id', request.toJson());
    return Beneficiary.fromJson(json!);
  }

  Future<void> deleteBeneficiary(String id) => _client.delete('/beneficiaries/$id');
}
