import 'package:flutter_test/flutter_test.dart';

import 'package:mobile/app.dart';

void main() {
  testWidgets('app builds without error', (WidgetTester tester) async {
    await tester.pumpWidget(const ObsApp());

    expect(find.byType(ObsApp), findsOneWidget);
  });
}
