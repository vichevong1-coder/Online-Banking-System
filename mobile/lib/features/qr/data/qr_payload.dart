import 'package:mobile/features/account/data/account_models.dart';

/// Client-side reading of the payload format in `docs/qr-payments-spec.md`:
///
/// ```
/// OBS1:<type>:<target>[:<currency>:<amount>]
/// ```
///
/// This exists only so the confirmation screen can show what is about to be
/// paid — the spec makes that confirmation a Sprint 5 obligation, and
/// `GET /qr/resolve` is deferred, so the payload is all the client has. Every
/// decision that matters is still re-made server-side on `POST /qr/pay`.
class QrPayload {
  const QrPayload({
    required this.raw,
    required this.isPersonal,
    required this.target,
    this.currency,
    this.amount,
  });

  final String raw;

  /// `P` — [target] is an account number. `M` — [target] is a merchant code.
  final bool isPersonal;
  final String target;

  /// Set only when the payload fixes the price. The payer may then agree with
  /// it or send nothing; any other amount is a 400 QR_AMOUNT_MISMATCH.
  final Currency? currency;
  final double? amount;

  bool get hasFixedAmount => amount != null;

  /// What to show as the destination. Deliberately not a payee *name*: the
  /// client cannot verify one, and inventing it would undercut the whole point
  /// of the confirmation step.
  String get destinationLabel =>
      isPersonal ? 'Account $target' : 'Merchant $target';

  static QrPayload? tryParse(String value) {
    final raw = value.trim();
    final parts = raw.split(':');
    if (parts.length < 3 || parts[0] != 'OBS1') return null;

    final type = parts[1].toUpperCase();
    if (type != 'P' && type != 'M') return null;
    if (parts[2].isEmpty) return null;

    Currency? currency;
    double? amount;
    if (parts.length >= 5) {
      try {
        currency = Currency.fromJson(parts[3].toUpperCase());
      } catch (_) {
        return null;
      }
      amount = double.tryParse(parts[4]);
      if (amount == null || amount <= 0) return null;
    }

    return QrPayload(
      raw: raw,
      isPersonal: type == 'P',
      target: parts[2],
      currency: currency,
      amount: amount,
    );
  }
}
