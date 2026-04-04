// flutter/lib/domain/model/product.dart
//
// Pure domain model — no generated proto types leak beyond the data layer.
class Product {
  const Product({
    required this.id,
    required this.name,
    required this.pricePaise,
    required this.imageUrl,
    required this.inStock,
    required this.category,
  });

  final String id;
  final String name;
  final int pricePaise;
  final String imageUrl;
  final bool inStock;
  final String category;

  /// Convenience: display price in rupees with paise
  String get displayPrice {
    final rupees = pricePaise ~/ 100;
    final paise = pricePaise % 100;
    return '₹$rupees.${paise.toString().padLeft(2, '0')}';
  }
}
