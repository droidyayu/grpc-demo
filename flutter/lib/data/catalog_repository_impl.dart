// flutter/lib/data/catalog_repository_impl.dart
//
// Implements the domain interface.
// Proto ↔ domain mapping is contained here — proto types never leave this file.
//
// NOTE: The `generated` imports below come from running protoc.
//       See lib/generated/.gitkeep for the generation command.

import 'package:grpc/grpc.dart';
// ignore: uri_does_not_exist
import 'package:grpc_demo/generated/product_catalog.pbgrpc.dart' as pb;
import 'package:grpc_demo/domain/exception/catalog_exception.dart';
import 'package:grpc_demo/domain/model/price_update.dart';
import 'package:grpc_demo/domain/model/product.dart';
import 'package:grpc_demo/domain/repository/catalog_repository.dart';

class CatalogRepositoryImpl implements CatalogRepository {
  CatalogRepositoryImpl(this._client);

  final pb.CatalogServiceClient _client;

  // ── Unary ─────────────────────────────────────────────────
  @override
  Future<Product> getProduct(String productId) async {
    try {
      final response = await _client.getProduct(
        pb.GetProductRequest()..productId = productId,
      );
      return response.toDomain();
    } on GrpcError catch (e) {
      throw _mapGrpcError(e);
    }
  }

  // ── Server-streaming ──────────────────────────────────────
  // Dart Stream<T> maps natively to gRPC server-streaming — no adapters needed.
  @override
  Stream<Product> listProducts({String category = '', int page = 1}) =>
      _client
          .listProducts(pb.ListProductsRequest()
            ..category = category
            ..page = page
            ..pageSize = 20)
          .map((p) => p.toDomain())
          .handleError((e) => throw _mapGrpcError(e));

  @override
  Stream<PriceUpdate> watchPrice(String productId) =>
      _client
          .watchPrice(pb.WatchPriceRequest()..productId = productId)
          .map((u) => u.toDomain())
          .handleError((e) => throw _mapGrpcError(e));
}

// ── Proto → domain mappers ─────────────────────────────────
extension on pb.Product {
  Product toDomain() => Product(
        id: id,
        name: name,
        pricePaise: pricePaise.toInt(),
        imageUrl: imageUrl,
        inStock: inStock,
        category: category,
      );
}

extension on pb.PriceUpdate {
  PriceUpdate toDomain() => PriceUpdate(
        productId: productId,
        pricePaise: pricePaise.toInt(),
        currency: currency,
      );
}

// ── gRPC status → domain exceptions ──────────────────────
CatalogException _mapGrpcError(Object e) {
  if (e is GrpcError) {
    return switch (e.code) {
      StatusCode.notFound      => NotFoundException(e.message ?? '?'),
      StatusCode.unavailable   => UnavailableException(e.message),
      _                        => UnknownException(e.message),
    };
  }
  return UnknownException(e);
}
