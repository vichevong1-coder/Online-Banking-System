import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// US-024's session store.
///
/// The **refresh token is the only secret that is persisted**, and it lives in
/// `flutter_secure_storage` (Keystore / Keychain), mirroring the React admin's
/// choice. The access token is held in memory only: it is short-lived, and
/// writing it to disk would put a bearer credential in plain `SharedPreferences`
/// for no gain. `SharedPreferences` keeps only the display profile.
class SessionManager extends ChangeNotifier {
  static final SessionManager instance = SessionManager._internal();
  SessionManager._internal();

  /// Test seam — swapped for a mock so `init()` never touches the network.
  @visibleForTesting
  static http.Client Function() httpClientFactory = http.Client.new;

  @visibleForTesting
  // Defaults are already AES-GCM under a Keystore-wrapped key on Android and
  // the Keychain on iOS — no options needed.
  static FlutterSecureStorage secureStorage = const FlutterSecureStorage();

  static const String _keyRefreshToken = 'obs_refresh_token';
  static const String _keyUserId = 'obs_user_id';
  static const String _keyFirstName = 'obs_first_name';
  static const String _keyLastName = 'obs_last_name';
  static const String _keyPhone = 'obs_phone';
  static const String _keyEmail = 'obs_email';

  /// Legacy plaintext keys from before the move to secure storage. Read once so
  /// an already-installed demo build isn't silently logged out, then wiped.
  static const String _legacyKeyAccessToken = 'obs_access_token';
  static const String _legacyKeyRefreshToken = 'obs_refresh_token';

  String? _accessToken;
  String? _refreshToken;
  String? _userId;
  String? _firstName;
  String? _lastName;
  String? _phone;
  String? _email;
  bool _initialized = false;

  bool get isAuthenticated => _accessToken != null && _accessToken!.isNotEmpty;
  String? get accessToken => _accessToken;
  String? get refreshToken => _refreshToken;
  String? get userId => _userId;
  String? get firstName => _firstName;
  String? get lastName => _lastName;
  String? get phone => _phone;
  String? get email => _email;
  String get fullName => ('${_firstName ?? ''} ${_lastName ?? ''}').trim().isEmpty
      ? 'Customer'
      : ('${_firstName ?? ''} ${_lastName ?? ''}').trim();

  /// False until the stored refresh token has been exchanged (or found absent).
  /// `app.dart` shows a splash on this rather than flashing the welcome screen
  /// at a customer who is in fact signed in.
  bool get isInitialized => _initialized;

  /// Loads the profile, then trades the stored refresh token for a fresh access
  /// token. Bounded by a timeout: a backend that isn't running must not hold the
  /// app on a blank screen.
  Future<void> init({String baseUrl = '', Duration timeout = const Duration(seconds: 5)}) async {
    // The finally is load-bearing: app.dart holds a splash until this flag
    // flips, and main() does not await this call, so anything thrown here
    // would otherwise strand the app on that splash with no way out.
    try {
      final prefs = await SharedPreferences.getInstance();
      _userId = prefs.getString(_keyUserId);
      _firstName = prefs.getString(_keyFirstName);
      _lastName = prefs.getString(_keyLastName);
      _phone = prefs.getString(_keyPhone);
      _email = prefs.getString(_keyEmail);

      _refreshToken = await _readRefreshToken(prefs);

      if (_refreshToken != null && _refreshToken!.isNotEmpty && baseUrl.isNotEmpty) {
        await _exchangeRefreshToken(baseUrl, timeout);
      }
    } catch (_) {
      // Storage unavailable: treat it as "no stored session" and show sign-in.
    } finally {
      _initialized = true;
      notifyListeners();
    }
  }

  Future<String?> _readRefreshToken(SharedPreferences prefs) async {
    try {
      final stored = await secureStorage.read(key: _keyRefreshToken);
      if (stored != null && stored.isNotEmpty) {
        return stored;
      }
    } catch (_) {
      // Keystore unavailable (or a platform without it) — fall through.
    }

    final legacy = prefs.getString(_legacyKeyRefreshToken);
    await prefs.remove(_legacyKeyAccessToken);
    await prefs.remove(_legacyKeyRefreshToken);
    if (legacy != null && legacy.isNotEmpty) {
      await _writeRefreshToken(legacy);
      return legacy;
    }
    return null;
  }

  Future<void> _writeRefreshToken(String token) async {
    try {
      await secureStorage.write(key: _keyRefreshToken, value: token);
    } catch (_) {
      // Nothing to fall back to: a plaintext copy is exactly what this avoids.
    }
  }

  Future<void> _exchangeRefreshToken(String baseUrl, Duration timeout) async {
    final client = httpClientFactory();
    try {
      final response = await client
          .post(
            Uri.parse('$baseUrl/auth/refresh'),
            headers: const {'Content-Type': 'application/json'},
            body: jsonEncode({'refreshToken': _refreshToken}),
          )
          .timeout(timeout);

      if (response.statusCode >= 200 && response.statusCode < 300) {
        final decoded = jsonDecode(response.body) as Map<String, dynamic>;
        _accessToken = decoded['accessToken'] as String?;
        return;
      }

      // 401/403 means the token is spent or the account was suspended — the
      // session is genuinely over, so drop it rather than retrying forever.
      if (response.statusCode == 401 || response.statusCode == 403) {
        await clearSession();
      }
    } catch (_) {
      // Offline or the backend is down: keep the refresh token, stay signed
      // out for this launch, and let the next one try again.
    } finally {
      client.close();
    }
  }

  Future<void> saveSession({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String firstName,
    required String lastName,
    String? phone,
    String? email,
  }) async {
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _userId = userId;
    _firstName = firstName;
    _lastName = lastName;
    if (phone != null) _phone = phone;
    if (email != null) _email = email;

    await _writeRefreshToken(refreshToken);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_keyUserId, userId);
    await prefs.setString(_keyFirstName, firstName);
    await prefs.setString(_keyLastName, lastName);
    if (phone != null) await prefs.setString(_keyPhone, phone);
    if (email != null) await prefs.setString(_keyEmail, email);

    notifyListeners();
  }

  /// Memory only — see the class comment.
  Future<void> updateTokens(String accessToken) async {
    _accessToken = accessToken;
    notifyListeners();
  }

  Future<void> updateProfile({String? email, String? firstName, String? lastName}) async {
    if (email != null) _email = email;
    if (firstName != null) _firstName = firstName;
    if (lastName != null) _lastName = lastName;

    final prefs = await SharedPreferences.getInstance();
    if (email != null) await prefs.setString(_keyEmail, email);
    if (firstName != null) await prefs.setString(_keyFirstName, firstName);
    if (lastName != null) await prefs.setString(_keyLastName, lastName);

    notifyListeners();
  }

  Future<void> clearSession() async {
    _accessToken = null;
    _refreshToken = null;
    _userId = null;
    _firstName = null;
    _lastName = null;
    _phone = null;
    _email = null;

    try {
      await secureStorage.delete(key: _keyRefreshToken);
    } catch (_) {}

    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_keyUserId);
    await prefs.remove(_keyFirstName);
    await prefs.remove(_keyLastName);
    await prefs.remove(_keyPhone);
    await prefs.remove(_keyEmail);
    await prefs.remove(_legacyKeyAccessToken);
    await prefs.remove(_legacyKeyRefreshToken);

    notifyListeners();
  }
}
