import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/app.dart';

void main() {
  testWidgets('app builds and shows placeholder home', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const ObsApp());

    expect(find.text('Online Banking System'), findsWidgets);
    expect(find.text('Mobile app setup complete'), findsOneWidget);
  });
}
