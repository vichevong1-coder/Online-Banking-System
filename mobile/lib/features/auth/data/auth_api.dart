import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/features/auth/data/auth_models.dart';

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

  Future<void> forgotPassword(String identifier) =>
      _client.post('/auth/password/forgot', {'identifier': identifier});

  Future<void> resetPassword({
    required String identifier,
    required String code,
    required String newPassword,
  }) =>
      _client.post('/auth/password/reset', {
        'identifier': identifier,
        'code': code,
        'newPassword': newPassword,
      });

  Future<UserProfile> getProfile() async {
    final json = await _client.get('/me');
    return UserProfile.fromJson(json as Map<String, dynamic>);
  }

  Future<UserProfile> updateProfile({required String email}) async {
    final json = await _client.patch('/me', {'email': email});
    return UserProfile.fromJson(json as Map<String, dynamic>);
  }

  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) =>
      _client.post('/me/password', {
        'currentPassword': currentPassword,
        'newPassword': newPassword,
      });
}
