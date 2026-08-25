import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/card/data/card_models.dart';

class CardApi {
  CardApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<List<CardModel>> listCards() async {
    final json = await _client.get('/cards');
    return (json as List<dynamic>)
        .map((e) => CardModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<CardModel> getCard(String id) async {
    final json = await _client.get('/cards/$id');
    return CardModel.fromJson(json as Map<String, dynamic>);
  }

  Future<CardModel> requestCard(CreateCardRequest request) async {
    final json = await _client.post('/cards', request.toJson());
    return CardModel.fromJson(json!);
  }

  Future<CardModel> blockCard(String id) async {
    final json = await _client.post('/cards/$id/block', {});
    return CardModel.fromJson(json!);
  }

  Future<CardModel> unblockCard(String id) async {
    final json = await _client.post('/cards/$id/unblock', {});
    return CardModel.fromJson(json!);
  }

  Future<CardModel> updateCard(String id, UpdateCardRequest request) async {
    final json = await _client.patch('/cards/$id', request.toJson());
    return CardModel.fromJson(json!);
  }
}
