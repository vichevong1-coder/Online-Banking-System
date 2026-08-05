import 'dart:convert';
import 'dart:io' show Platform;

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

// Thin wrapper over the backend's POST endpoints (US-007/008/009/011). Every auth endpoint is
// POST + JSON in, JSON (or 204 empty) out, so one helper covers all of them.
class ApiClient {
  ApiClient({http.Client? client}) : _client = client ?? http.Client();

  final http.Client _client;

  Future<Map<String, dynamic>?> post(String path, Map<String, dynamic> body) async {
    final http.Response response;
    try {
      response = await _client.post(
        Uri.parse('$apiBaseUrl$path'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
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
