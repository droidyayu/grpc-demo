// flutter/lib/domain/usecase/list_products_use_case.dart
import 'package:grpc_demo/domain/model/product.dart';
import 'package:grpc_demo/domain/repository/catalog_repository.dart';

class ListProductsUseCase {
  const ListProductsUseCase(this._repository);
  final CatalogRepository _repository;

  Stream<Product> call({String category = '', int page = 1}) =>
      _repository.listProducts(category: category, page: page);
}
