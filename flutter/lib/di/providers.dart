// flutter/lib/di/providers.dart
//
// Riverpod providers — the Flutter equivalent of Hilt modules.
// All dependencies are declared here; widgets and notifiers receive them
// via ref.watch() / ref.read() — never construct dependencies themselves.

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:grpc/grpc.dart';
// ignore: uri_does_not_exist
import 'package:grpc_demo/generated/product_catalog.pbgrpc.dart' as pb;
import 'package:grpc_demo/data/catalog_repository_impl.dart';
import 'package:grpc_demo/domain/repository/catalog_repository.dart';
import 'package:grpc_demo/domain/usecase/list_products_use_case.dart';
import 'package:grpc_demo/domain/usecase/watch_price_use_case.dart';

// ── Transport ─────────────────────────────────────────────
// Change host/port here when pointing at a real deployment.
// On an Android emulator use 10.0.2.2; on iOS simulator use 127.0.0.1
final grpcChannelProvider = Provider<ClientChannel>((ref) {
  final channel = ClientChannel(
    '10.0.2.2',                          // host
    port: 50051,
    options: const ChannelOptions(
      credentials: ChannelCredentials.insecure(), // TLS in production
    ),
  );
  ref.onDispose(channel.shutdown);
  return channel;
});

final grpcClientProvider = Provider<pb.CatalogServiceClient>((ref) {
  return pb.CatalogServiceClient(ref.watch(grpcChannelProvider));
});

// ── Repository ────────────────────────────────────────────
final catalogRepositoryProvider = Provider<CatalogRepository>((ref) {
  return CatalogRepositoryImpl(ref.watch(grpcClientProvider));
});

// ── Use cases ─────────────────────────────────────────────
final listProductsUseCaseProvider = Provider((ref) =>
    ListProductsUseCase(ref.watch(catalogRepositoryProvider)));

final watchPriceUseCaseProvider = Provider((ref) =>
    WatchPriceUseCase(ref.watch(catalogRepositoryProvider)));
