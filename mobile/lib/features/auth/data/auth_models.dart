// Backend: com.obs.backend.feature.user.entity.User.Gender (US-007).
enum Gender {
  male,
  female;

  String toJson() => this == Gender.male ? 'MALE' : 'FEMALE';
}

// Java LocalDate wants exactly yyyy-MM-dd — DateTime.toIso8601String() includes a time
// component Jackson will reject, so this is formatted by hand.
String formatLocalDate(DateTime date) {
  final year = date.year.toString().padLeft(4, '0');
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '$year-$month-$day';
}

// Backend: com.obs.backend.feature.auth.dto.RegisterRequest (US-007).
class RegisterRequest {
  RegisterRequest({
    required this.firstName,
    required this.lastName,
    required this.password,
    required this.nidNumber,
    required this.nidExpiryDate,
    required this.dateOfBirth,
    required this.gender,
    required this.phone,
  });

  final String firstName;
  final String lastName;
  final String password;
  final String nidNumber;
  final DateTime nidExpiryDate;
  final DateTime dateOfBirth;
  final Gender gender;
  final String phone;

  Map<String, dynamic> toJson() => {
    'firstName': firstName,
    'lastName': lastName,
    'password': password,
    'nidNumber': nidNumber,
    'nidExpiryDate': formatLocalDate(nidExpiryDate),
    'dateOfBirth': formatLocalDate(dateOfBirth),
    'gender': gender.toJson(),
    'phone': phone,
  };
}

// Backend: com.obs.backend.feature.auth.dto.LoginResponse (US-009). 2FA is mandatory — login
// never returns real tokens directly, only a short-lived challenge token.
class LoginResponse {
  LoginResponse({required this.challengeToken});

  final String challengeToken;

  factory LoginResponse.fromJson(Map<String, dynamic> json) =>
      LoginResponse(challengeToken: json['challengeToken'] as String);
}

// Backend: com.obs.backend.feature.auth.dto.AuthTokenResponse (US-011).
class AuthTokenResponse {
  AuthTokenResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.userId,
    required this.firstName,
    required this.lastName,
    required this.role,
  });

  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final String userId;
  final String firstName;
  final String lastName;
  final String role;

  factory AuthTokenResponse.fromJson(Map<String, dynamic> json) => AuthTokenResponse(
    accessToken: json['accessToken'] as String,
    refreshToken: json['refreshToken'] as String,
    tokenType: json['tokenType'] as String,
    userId: json['userId'] as String,
    firstName: json['firstName'] as String,
    lastName: json['lastName'] as String,
    role: json['role'] as String,
  );
}
