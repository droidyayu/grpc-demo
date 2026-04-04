// flutter/lib/domain/model/price_update.dart
class PriceUpdate {
  const PriceUpdate({
    required this.productId,
    required this.pricePaise,
    required this.currency,
  });

  final String productId;
  final int pricePaise;
  final String currency;
}
