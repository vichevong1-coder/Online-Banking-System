import 'package:mobile/features/account/data/account_models.dart';

// KHR has no minor unit in everyday use (unlike USD cents), so it's formatted with zero decimal
// places. Never mixes currencies into one figure — see account_models.dart's Currency doc.
String formatMoney(double amount, Currency currency) {
  final isNegative = amount < 0;
  final fractionDigits = currency == Currency.usd ? 2 : 0;
  final fixed = amount.abs().toStringAsFixed(fractionDigits);
  final parts = fixed.split('.');

  final wholeDigits = parts[0];
  final grouped = StringBuffer();
  for (var i = 0; i < wholeDigits.length; i++) {
    if (i > 0 && (wholeDigits.length - i) % 3 == 0) {
      grouped.write(',');
    }
    grouped.write(wholeDigits[i]);
  }
  final whole = parts.length > 1 ? '$grouped.${parts[1]}' : grouped.toString();

  final symbol = currency == Currency.usd ? '\$' : '៛';
  return '${isNegative ? '-' : ''}$symbol$whole';
}
