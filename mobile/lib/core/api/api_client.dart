import 'dart:convert';
import 'dart:io' show Platform;
import 'dart:typed_data' show Uint8List;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;

String _resolveBaseUrl() {
  const override = String.fromEnvironment('API_BASE_URL');
  if (override.isNotEmpty) {
    return override;
  }
  // Android emulator's loopback to the host machine isn't localhost.
  if (!kIsWeb && Platform.isAndroid) {
    return 'http://10.0.2.2:8080';
  }
  return 'http://localhost:8080';
}

final String apiBaseUrl = _resolveBaseUrl();

class ApiException implements Exception {
  ApiException(this.statusCode, this.code, this.message, {this.fieldErrors});

  final int statusCode;
  final String code;
  final String message;
  final Map<String, String>? fieldErrors;
}

// Thin wrapper over the backend's REST endpoints. Auth endpoints (US-007/008/009/011) are all
// POST + JSON in, JSON (or 204 empty) out. Account-scoped endpoints (US-013+) additionally need
// an Authorization header, GET with query params, and raw bytes for the PDF statement.
class ApiClient {
  ApiClient({http.Client? client, this.accessToken}) : _client = client ?? http.Client();

  final http.Client _client;
  final String? accessToken;

  Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    if (accessToken != null) 'Authorization': 'Bearer $accessToken',
  };

  // Query params are joined onto the path rather than passed as a Map<String, String> to
  // Uri.parse(...).replace(queryParameters: ...), so callers can simply omit a key instead of
  // needing to know that an empty-string value 400s a numeric @RequestParam like minAmount.
  Uri _uri(String path, Map<String, String>? query) {
    final uri = Uri.parse('$apiBaseUrl$path');
    if (query == null || query.isEmpty) {
      return uri;
    }
    return uri.replace(queryParameters: query);
  }

  Future<Map<String, dynamic>?> post(String path, Map<String, dynamic> body) async {
    final http.Response response;
    try {
      response = await _client.post(_uri(path, null), headers: _headers, body: jsonEncode(body));
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body) as Map<String, dynamic>;
    }

    throw _toApiException(response);
  }

  // Returns dynamic because response shapes vary: GET /accounts is a top-level JSON array, while
  // GET /accounts/{id}/balance and the paginated transactions endpoint are objects. Callers cast.
  Future<dynamic> get(String path, {Map<String, String>? query}) async {
    final http.Response response;
    try {
      response = await _client.get(_uri(path, query), headers: _headers);
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body);
    }

    throw _toApiException(response);
  }

  Future<Uint8List> getBytes(String path, {Map<String, String>? query}) async {
    final http.Response response;
    try {
      response = await _client.get(_uri(path, query), headers: _headers);
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return response.bodyBytes;
    }

    throw _toApiException(response);
  }

  ApiException _toApiException(http.Response response) {
    Map<String, dynamic>? decoded;
    try {
      decoded = jsonDecode(response.body) as Map<String, dynamic>;
    } catch (_) {
      decoded = null;
    }

    final code = decoded?['error'] as String? ?? 'UNKNOWN_ERROR';
    final message = decoded?['message'] as String? ?? 'Something went wrong. Please try again.';
    final rawFieldErrors = decoded?['fieldErrors'] as Map<String, dynamic>?;
    final fieldErrors = rawFieldErrors?.map((key, value) => MapEntry(key, value.toString()));

    return ApiException(response.statusCode, code, message, fieldErrors: fieldErrors);
  }
}
