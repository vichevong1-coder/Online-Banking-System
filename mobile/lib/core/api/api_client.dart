import 'dart:convert';
import 'dart:io' show Platform;
import 'dart:typed_data' show Uint8List;

import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:http/http.dart' as http;
import 'package:mobile/core/session/session_manager.dart';

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

  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({http.Client? client, this.accessToken}) : _client = client ?? http.Client();

  final http.Client _client;
  String? accessToken;

  String? get effectiveToken => accessToken ?? SessionManager.instance.accessToken;

  Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    if (effectiveToken != null) 'Authorization': 'Bearer $effectiveToken',
  };

  Uri _uri(String path, Map<String, String>? query) {
    final uri = Uri.parse('$apiBaseUrl$path');
    if (query == null || query.isEmpty) {
      return uri;
    }
    return uri.replace(queryParameters: query);
  }

  Future<bool> _tryRefreshToken() async {
    final refreshToken = SessionManager.instance.refreshToken;
    if (refreshToken == null || refreshToken.isEmpty) {
      return false;
    }

    try {
      final response = await _client.post(
        _uri('/auth/refresh', null),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'refreshToken': refreshToken}),
      );

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body) as Map<String, dynamic>;
        final newAccessToken = decoded['accessToken'] as String;
        accessToken = newAccessToken;
        await SessionManager.instance.updateTokens(newAccessToken);
        return true;
      }
    } catch (_) {}

    await SessionManager.instance.clearSession();
    return false;
  }

  Future<Map<String, dynamic>?> post(String path, Map<String, dynamic> body) async {
    http.Response response;
    try {
      response = await _client.post(_uri(path, null), headers: _headers, body: jsonEncode(body));
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode == 401 && path != '/auth/login' && path != '/auth/refresh') {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        try {
          response = await _client.post(_uri(path, null), headers: _headers, body: jsonEncode(body));
        } catch (_) {
          throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
        }
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body) as Map<String, dynamic>;
    }

    throw _toApiException(response);
  }

  Future<dynamic> get(String path, {Map<String, String>? query}) async {
    http.Response response;
    try {
      response = await _client.get(_uri(path, query), headers: _headers);
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        try {
          response = await _client.get(_uri(path, query), headers: _headers);
        } catch (_) {
          throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
        }
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body);
    }

    throw _toApiException(response);
  }

  Future<Map<String, dynamic>?> patch(String path, Map<String, dynamic> body) async {
    http.Response response;
    try {
      response = await _client.patch(_uri(path, null), headers: _headers, body: jsonEncode(body));
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        try {
          response = await _client.patch(_uri(path, null), headers: _headers, body: jsonEncode(body));
        } catch (_) {
          throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
        }
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) {
        return null;
      }
      return jsonDecode(response.body) as Map<String, dynamic>;
    }

    throw _toApiException(response);
  }

  Future<void> delete(String path) async {
    http.Response response;
    try {
      response = await _client.delete(_uri(path, null), headers: _headers);
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        try {
          response = await _client.delete(_uri(path, null), headers: _headers);
        } catch (_) {
          throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
        }
      }
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }

    throw _toApiException(response);
  }

  Future<Uint8List> getBytes(String path, {Map<String, String>? query}) async {
    http.Response response;
    try {
      response = await _client.get(_uri(path, query), headers: _headers);
    } catch (_) {
      throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
    }

    if (response.statusCode == 401) {
      final refreshed = await _tryRefreshToken();
      if (refreshed) {
        try {
          response = await _client.get(_uri(path, query), headers: _headers);
        } catch (_) {
          throw ApiException(0, 'NETWORK_ERROR', "Couldn't reach the server. Please try again.");
        }
      }
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
