import 'package:mobile/features/auth/data/auth_models.dart';

/// What could be read off a Cambodian national ID card. Every field is
/// nullable: OCR is a convenience, and the customer confirms each value.
class NidFields {
  const NidFields({
    this.nidNumber,
    this.firstName,
    this.lastName,
    this.dateOfBirth,
    this.nidExpiryDate,
    this.gender,
  });

  final String? nidNumber;
  final String? firstName;
  final String? lastName;
  final DateTime? dateOfBirth;
  final DateTime? nidExpiryDate;
  final Gender? gender;

  bool get isEmpty =>
      nidNumber == null &&
      firstName == null &&
      lastName == null &&
      dateOfBirth == null &&
      nidExpiryDate == null &&
      gender == null;
}

final _nidNumberPattern = RegExp(r'(?<!\d)(\d{3})[ -]?(\d{3})[ -]?(\d{3})(?!\d)');
final _datePattern = RegExp(r'(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4})(?!\d)');
final _sexLabelPattern = RegExp(
  r'\bsex\s*[:.]?\s*([MF])\b',
  caseSensitive: false,
);
final _namePattern = RegExp(r"^[A-Z][A-Z' -]*[A-Z]$");

// Printed on every card, so an all-caps line containing one is not a name.
const _headerWords = {
  'KINGDOM',
  'CAMBODIA',
  'NATION',
  'RELIGION',
  'KING',
  'IDENTITY',
  'CARD',
  'NATIONAL',
  'ID',
};

/// Parses the text ML Kit read from the front and back of the card. Only the
/// Latin script is recognised, so Khmer text is ignored.
///
/// The card prints the surname first ("CHAN SOKHA"), so the first word becomes
/// the last name and the rest the first name.
NidFields parseNidText(String text, {DateTime? now}) {
  final today = now ?? DateTime.now();

  final numberMatch = _nidNumberPattern.firstMatch(text);
  final nidNumber = numberMatch == null
      ? null
      : '${numberMatch.group(1)}${numberMatch.group(2)}${numberMatch.group(3)}';

  final dates = <DateTime>[];
  for (final match in _datePattern.allMatches(text)) {
    final day = int.parse(match.group(1)!);
    final month = int.parse(match.group(2)!);
    final year = int.parse(match.group(3)!);
    final date = DateTime(year, month, day);
    // DateTime rolls 31/02 over to March; a date that changed is a misread.
    if (date.year == year && date.month == month && date.day == day) {
      dates.add(date);
    }
  }
  dates.sort();

  DateTime? dateOfBirth;
  DateTime? expiry;
  if (dates.isNotEmpty) {
    final latest = dates.last;
    if (latest.isAfter(today)) {
      expiry = latest;
    }
    // The card may also print an issue date, so the earliest date is the birth date.
    final earliest = dates.first;
    if (earliest != expiry) {
      dateOfBirth = earliest;
    }
  }

  Gender? gender;
  final sexLabel = _sexLabelPattern.firstMatch(text);
  if (RegExp(r'\bfemale\b', caseSensitive: false).hasMatch(text)) {
    gender = Gender.female;
  } else if (RegExp(r'\bmale\b', caseSensitive: false).hasMatch(text)) {
    gender = Gender.male;
  } else if (sexLabel != null) {
    gender = sexLabel.group(1)!.toUpperCase() == 'F'
        ? Gender.female
        : Gender.male;
  }

  String? firstName;
  String? lastName;
  for (final rawLine in text.split('\n')) {
    final line = rawLine.trim();
    if (!_namePattern.hasMatch(line)) continue;
    final words = line.split(RegExp(r"[ -]+"));
    if (words.length < 2 || words.length > 4) continue;
    if (words.any((w) => w.length < 2 || _headerWords.contains(w))) continue;
    lastName = _titleCase(words.first);
    firstName = words.skip(1).map(_titleCase).join(' ');
    break;
  }

  return NidFields(
    nidNumber: nidNumber,
    firstName: firstName,
    lastName: lastName,
    dateOfBirth: dateOfBirth,
    nidExpiryDate: expiry,
    gender: gender,
  );
}

String _titleCase(String word) =>
    word[0].toUpperCase() + word.substring(1).toLowerCase();
