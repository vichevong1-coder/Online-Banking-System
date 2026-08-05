import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_models.dart';

// One client per auth endpoint (US-007/008/009/011) — thin wrappers, no business logic here
// beyond request/response shape, per CLAUDE.md's "no business logic in widgets" (this isn't a
// widget either, but the same rule keeps screens from talking to ApiClient directly).
class AuthApi {
  AuthApi({ApiClient? client}) : _client = client ?? ApiClient();

  final ApiClient _client;

  Future<void> register(RegisterRequest request) => _client.post('/auth/register', request.toJson());

  Future<void> verifyRegistrationOtp({required String phone, required String code}) =>
      _client.post('/auth/otp/verify', {'phone': phone, 'code': code});

  Future<void> resendRegistrationOtp({required String phone}) =>
      _client.post('/auth/otp/resend', {'phone': phone});

  Future<LoginResponse> login({required String phone, required String password}) async {
    final json = await _client.post('/auth/login', {'phone': phone, 'password': password});
    return LoginResponse.fromJson(json!);
  }

  Future<AuthTokenResponse> verifyTwoFactor({required String challengeToken, required String code}) async {
    final json = await _client.post('/auth/2fa/verify', {
      'challengeToken': challengeToken,
      'code': code,
    });
    return AuthTokenResponse.fromJson(json!);
  }

  Future<LoginResponse> resendTwoFactor({required String challengeToken}) async {
    final json = await _client.post('/auth/2fa/resend', {'challengeToken': challengeToken});
    return LoginResponse.fromJson(json!);
  }
}
