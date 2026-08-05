import 'dart:convert';

// The challenge token is a JWT — decoding it client-side (no signature check, this only drives
// the 2FA countdown UI) reads its real expiry instead of hardcoding the TTL, so the timer stays
// correct if JWT_CHALLENGE_TOKEN_TTL is ever changed. Mirrors web-admin's decodeJwtExpiryMs.
DateTime? decodeJwtExpiry(String token) {
  try {
    final parts = token.split('.');
    if (parts.length != 3) {
      return null;
    }
    final payload = jsonDecode(utf8.decode(base64Url.decode(base64Url.normalize(parts[1])))) as Map<String, dynamic>;
    final exp = payload['exp'];
    if (exp is! int) {
      return null;
    }
    return DateTime.fromMillisecondsSinceEpoch(exp * 1000);
  } catch (_) {
    return null;
  }
}
