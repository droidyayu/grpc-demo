// flutter/lib/domain/repository/catalog_repository.dart
import 'package:grpc_demo/domain/model/price_update.dart';
import 'package:grpc_demo/domain/model/product.dart';

abstract interface class CatalogRepository {
  Future<Product> getProduct(String productId);
  Stream<Product> listProducts({String category = '', int page = 1});
  Stream<PriceUpdate> watchPrice(String productId);
}
