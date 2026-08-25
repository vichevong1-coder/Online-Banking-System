import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  testWidgets('app builds without error', (WidgetTester tester) async {
    await tester.pumpWidget(const ObsApp());
    expect(find.byType(ObsApp), findsOneWidget);
  });
}
