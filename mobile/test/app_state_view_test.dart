import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_theme.dart';
import 'package:mobile/core/widgets/app_state_view.dart';

Widget _host(Widget child) {
  return MaterialApp(
    theme: AppTheme.darkTheme,
    home: Scaffold(body: child),
  );
}

void main() {
  testWidgets('AppErrorView shows the message and retries on tap', (tester) async {
    var retried = 0;
    await tester.pumpWidget(_host(
      AppErrorView(message: 'Couldn\'t load cards.', onRetry: () => retried++),
    ));

    expect(find.text('Couldn\'t load cards.'), findsOneWidget);
    await tester.tap(find.text('Try again'));
    await tester.pump();

    expect(retried, 1);
  });

  testWidgets('AppErrorView hides the retry action when no callback is given', (tester) async {
    await tester.pumpWidget(_host(const AppErrorView(message: 'Offline')));

    expect(find.text('Offline'), findsOneWidget);
    expect(find.text('Try again'), findsNothing);
  });

  testWidgets('AppEmptyView renders title and optional subtitle', (tester) async {
    await tester.pumpWidget(_host(
      const AppEmptyView(
        icon: Icons.receipt_long_outlined,
        title: 'No bill payment history yet',
        subtitle: 'Payments you make will appear here.',
      ),
    ));

    expect(find.text('No bill payment history yet'), findsOneWidget);
    expect(find.text('Payments you make will appear here.'), findsOneWidget);
  });

  testWidgets('PrimaryActionButton is dimmed when it has no handler', (tester) async {
    await tester.pumpWidget(_host(
      const PrimaryActionButton(title: 'SIGN IN', onPressed: null),
    ));

    final opacity = tester.widget<Opacity>(
      find.ancestor(of: find.text('SIGN IN'), matching: find.byType(Opacity)).first,
    );
    expect(opacity.opacity, lessThan(1));
  });

  testWidgets('PrimaryActionButton stays fully opaque while loading', (tester) async {
    await tester.pumpWidget(_host(
      const PrimaryActionButton(title: 'SIGN IN', onPressed: null, isLoading: true),
    ));

    final opacity = tester.widget<Opacity>(
      find.ancestor(of: find.byType(CircularProgressIndicator), matching: find.byType(Opacity)).first,
    );
    expect(opacity.opacity, 1);
  });
}
