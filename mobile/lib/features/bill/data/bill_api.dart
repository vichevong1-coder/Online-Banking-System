import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/bill/data/bill_models.dart';

class BillApi {
  BillApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<List<BillProvider>> listProviders({String? category}) async {
    final json = await _client.get('/bill-providers', query: {
      if (category != null && category.isNotEmpty) 'category': category,
    });
    return (json as List<dynamic>)
        .map((e) => BillProvider.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<BillPayment> payBill(PayBillRequest request) async {
    final json = await _client.post('/bill-payments', request.toJson());
    return BillPayment.fromJson(json!);
  }

  Future<List<BillPayment>> getPaymentHistory({int page = 0, int size = 20}) async {
    final json = await _client.get('/bill-payments', query: {
      'page': page.toString(),
      'size': size.toString(),
    });
    final content = (json as Map<String, dynamic>)['content'] as List<dynamic>;
    return content.map((e) => BillPayment.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<BillPayment> getPaymentReceipt(String id) async {
    final json = await _client.get('/bill-payments/$id');
    return BillPayment.fromJson(json as Map<String, dynamic>);
  }

  Future<RecurringBillPayment> createRecurring(CreateRecurringBillRequest request) async {
    final json = await _client.post('/bill-payments/recurring', request.toJson());
    return RecurringBillPayment.fromJson(json!);
  }

  Future<List<RecurringBillPayment>> listRecurring() async {
    final json = await _client.get('/bill-payments/recurring');
    return (json as List<dynamic>)
        .map((e) => RecurringBillPayment.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> cancelRecurring(String id) => _client.delete('/bill-payments/recurring/$id');
}
