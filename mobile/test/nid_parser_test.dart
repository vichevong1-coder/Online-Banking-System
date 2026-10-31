import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/features/auth/data/auth_models.dart';
import 'package:mobile/features/auth/data/nid_parser.dart';

void main() {
  final today = DateTime(2026, 10, 31);

  const frontText = '''
KINGDOM OF CAMBODIA
NATION RELIGION KING
IDENTITY CARD
CHAN SOKHA
Date of birth 05/03/1995
Sex M
ID 123 456 789
''';
  const backText = 'Valid until 10/06/2030';

  test('reads the fields printed on a typical card', () {
    final fields = parseNidText('$frontText\n$backText', now: today);

    expect(fields.nidNumber, '123456789');
    expect(fields.lastName, 'Chan');
    expect(fields.firstName, 'Sokha');
    expect(fields.dateOfBirth, DateTime(1995, 3, 5));
    expect(fields.nidExpiryDate, DateTime(2030, 6, 10));
    expect(fields.gender, Gender.male);
    expect(fields.isEmpty, isFalse);
  });

  test('does not mistake the card header for a name', () {
    final fields = parseNidText(
      'KINGDOM OF CAMBODIA\nNATION RELIGION KING\nIDENTITY CARD',
      now: today,
    );
    expect(fields.firstName, isNull);
    expect(fields.lastName, isNull);
  });

  test('reads gender from the word female', () {
    final fields = parseNidText('Sex: Female\nCHAN SREY', now: today);
    expect(fields.gender, Gender.female);
  });

  test('reads gender from a SEX F label', () {
    final fields = parseNidText('SEX F', now: today);
    expect(fields.gender, Gender.female);
  });

  test('treats a single past date as the birth date, not an expiry', () {
    final fields = parseNidText('Born 01/02/1990', now: today);
    expect(fields.dateOfBirth, DateTime(1990, 2, 1));
    expect(fields.nidExpiryDate, isNull);
  });

  test('ignores impossible dates such as 31/02', () {
    final fields = parseNidText('31/02/1990', now: today);
    expect(fields.dateOfBirth, isNull);
    expect(fields.nidExpiryDate, isNull);
  });

  test('uses the earliest date as birth date when an issue date is printed', () {
    final fields = parseNidText(
      '05/03/1995\n12/01/2020\n10/06/2030',
      now: today,
    );
    expect(fields.dateOfBirth, DateTime(1995, 3, 5));
    expect(fields.nidExpiryDate, DateTime(2030, 6, 10));
  });

  test('returns an empty result for unreadable text', () {
    expect(parseNidText('', now: today).isEmpty, isTrue);
    expect(parseNidText('@@@ ### ???', now: today).isEmpty, isTrue);
  });
}
