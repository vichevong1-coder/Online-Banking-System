import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile/core/api/api_client.dart';
import 'package:mobile/core/session/session_manager.dart';

import 'app.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
    ),
  );

  // Deliberately not awaited: restoring the session hits /auth/refresh, and a
  // backend that isn't running must not hold the app on a blank native splash.
  // ObsApp renders its own splash until SessionManager reports initialized.
  unawaited(SessionManager.instance.init(baseUrl: apiBaseUrl));

  runApp(const ObsApp());
}
