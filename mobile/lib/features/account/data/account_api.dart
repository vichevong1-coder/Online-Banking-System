import 'dart:typed_data' show Uint8List;

import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/account/data/account_models.dart';
import 'package:mobile/features/auth/data/auth_models.dart' show formatLocalDate;

class AccountApi {
  AccountApi({String? accessToken, ApiClient? client})
      : _client = client ?? ApiClient(accessToken: accessToken);

  final ApiClient _client;

  Future<List<Account>> listAccounts() async {
    final json = await _client.get('/accounts');
    return (json as List<dynamic>).map((e) => Account.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Account> openAccount(OpenAccountRequest request) async {
    final json = await _client.post('/accounts/requests', request.toJson());
    return Account.fromJson(json!);
  }

  Future<Balance> getBalance(String accountId) async {
    final json = await _client.get('/accounts/$accountId/balance');
    return Balance.fromJson(json as Map<String, dynamic>);
  }

  Future<TransactionPage> listTransactions(String accountId, TransactionQuery query) async {
    final json = await _client.get('/accounts/$accountId/transactions', query: query.toQuery());
    return TransactionPage.fromJson(json as Map<String, dynamic>);
  }

  Future<Uint8List> getStatement(String accountId, {DateTime? fromDate, DateTime? toDate}) {
    return _client.getBytes(
      '/accounts/$accountId/statement',
      query: {
        if (fromDate != null) 'fromDate': formatLocalDate(fromDate),
        if (toDate != null) 'toDate': formatLocalDate(toDate),
      },
    );
  }

  Future<void> emailStatement(String accountId, {DateTime? fromDate, DateTime? toDate}) {
    return _client.post(
      '/accounts/$accountId/statement/email',
      {
        if (fromDate != null) 'fromDate': formatLocalDate(fromDate),
        if (toDate != null) 'toDate': formatLocalDate(toDate),
      },
    );
  }
}
