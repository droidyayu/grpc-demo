// flutter/lib/domain/exception/catalog_exception.dart

sealed class CatalogException implements Exception {
  const CatalogException(this.message);
  final String message;
  @override
  String toString() => message;
}

final class NotFoundException extends CatalogException {
  const NotFoundException(String id) : super("Product '$id' not found");
}

final class UnavailableException extends CatalogException {
  const UnavailableException(Object? cause)
      : super("Service unavailable: $cause");
}

final class UnknownException extends CatalogException {
  const UnknownException(Object? cause) : super("Unknown error: $cause");
}
