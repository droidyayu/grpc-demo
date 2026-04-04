// flutter/lib/ui/catalog_notifier.dart
//
// AsyncNotifier mirrors Android's CatalogViewModel:
//   - state holds AsyncValue<List<Product>> (equivalent of CatalogUiState)
//   - priceTickerProvider is a separate StateProvider (equivalent of priceTicker StateFlow)

import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:grpc_demo/di/providers.dart';
import 'package:grpc_demo/domain/exception/catalog_exception.dart';
import 'package:grpc_demo/domain/model/price_update.dart';
import 'package:grpc_demo/domain/model/product.dart';

// ── Catalog list notifier ──────────────────────────────────
class CatalogNotifier extends AutoDisposeAsyncNotifier<List<Product>> {
  @override
  Future<List<Product>> build() async {
    return _loadProducts();
  }

  Future<List<Product>> _loadProducts({String category = ''}) async {
    final useCase = ref.read(listProductsUseCaseProvider);
    final products = <Product>[];
    try {
      await for (final product in useCase(category: category)) {
        products.add(product);
        state = AsyncData(List.unmodifiable(products));
      }
    } on CatalogException catch (e) {
      state = AsyncError(e, StackTrace.current);
    }
    return products;
  }

  Future<void> reload({String category = ''}) async {
    state = const AsyncLoading();
    await _loadProducts(category: category);
  }
}

final catalogProvider =
    AsyncNotifierProvider.autoDispose<CatalogNotifier, List<Product>>(
        CatalogNotifier.new);

// ── Live price ticker ──────────────────────────────────────
class PriceTickerNotifier extends AutoDisposeNotifier<PriceUpdate?> {
  StreamSubscription<PriceUpdate>? _sub;

  @override
  PriceUpdate? build() => null;

  void watch(String productId) {
    _sub?.cancel();
    final useCase = ref.read(watchPriceUseCaseProvider);
    _sub = useCase(productId).listen(
      (update) => state = update,
      onError: (_) => _sub?.cancel(),
    );
    ref.onDispose(() => _sub?.cancel());
  }

  void stop() {
    _sub?.cancel();
    state = null;
  }
}

final priceTickerProvider =
    NotifierProvider.autoDispose<PriceTickerNotifier, PriceUpdate?>(
        PriceTickerNotifier.new);
