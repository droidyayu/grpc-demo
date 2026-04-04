// flutter/lib/main.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'ui/catalog_screen.dart';

void main() {
  runApp(
    // ProviderScope is the Flutter equivalent of @HiltAndroidApp —
    // it is the root DI container for all Riverpod providers.
    const ProviderScope(child: GrpcDemoApp()),
  );
}

class GrpcDemoApp extends StatelessWidget {
  const GrpcDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'gRPC Demo',
      theme: ThemeData(
        colorSchemeSeed: Colors.blue,
        useMaterial3: true,
      ),
      home: const CatalogScreen(),
    );
  }
}
