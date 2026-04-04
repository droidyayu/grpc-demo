// flutter/lib/domain/usecase/watch_price_use_case.dart
import 'package:grpc_demo/domain/model/price_update.dart';
import 'package:grpc_demo/domain/repository/catalog_repository.dart';

class WatchPriceUseCase {
  const WatchPriceUseCase(this._repository);
  final CatalogRepository _repository;

  Stream<PriceUpdate> call(String productId) =>
      _repository.watchPrice(productId);
}
