// flutter/lib/ui/catalog_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:grpc_demo/domain/model/price_update.dart';
import 'package:grpc_demo/domain/model/product.dart';
import 'catalog_notifier.dart';

class CatalogScreen extends ConsumerWidget {
  const CatalogScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state  = ref.watch(catalogProvider);
    final ticker = ref.watch(priceTickerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('gRPC Product Catalog'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Column(
        children: [
          if (ticker != null) _PriceTickerCard(ticker: ticker),
          Expanded(
            child: switch (state) {
              AsyncLoading() => const Center(child: CircularProgressIndicator()),
              AsyncError(:final error) => _ErrorView(
                  message: error.toString(),
                  onRetry: () => ref.read(catalogProvider.notifier).reload(),
                ),
              AsyncData(:final value) => _ProductList(
                  products: value,
                  watchingProductId: ticker?.productId,
                  onWatchPrice: (id) =>
                      ref.read(priceTickerProvider.notifier).watch(id),
                  onStopWatch: () =>
                      ref.read(priceTickerProvider.notifier).stop(),
                ),
              _ => const SizedBox.shrink(),
            },
          ),
        ],
      ),
    );
  }
}

// ── Sub-widgets ───────────────────────────────────────────

class _ProductList extends StatelessWidget {
  const _ProductList({
    required this.products,
    required this.watchingProductId,
    required this.onWatchPrice,
    required this.onStopWatch,
  });

  final List<Product> products;
  final String? watchingProductId;
  final ValueChanged<String> onWatchPrice;
  final VoidCallback onStopWatch;

  @override
  Widget build(BuildContext context) {
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: products.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (context, i) {
        final product = products[i];
        final isWatching = watchingProductId == product.id;
        return _ProductCard(
          product: product,
          isWatching: isWatching,
          onWatchPrice: () => onWatchPrice(product.id),
          onStopWatch: onStopWatch,
        );
      },
    );
  }
}

class _ProductCard extends StatelessWidget {
  const _ProductCard({
    required this.product,
    required this.isWatching,
    required this.onWatchPrice,
    required this.onStopWatch,
  });

  final Product product;
  final bool isWatching;
  final VoidCallback onWatchPrice;
  final VoidCallback onStopWatch;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(product.name,
                style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 4),
            Text(
              product.displayPrice,
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: Colors.grey[600]),
            ),
            Text(
              product.inStock ? 'In Stock' : 'Out of Stock',
              style: TextStyle(
                color: product.inStock ? const Color(0xFF2EC4B6) : Colors.red,
                fontSize: 13,
              ),
            ),
            const SizedBox(height: 8),
            FilledButton(
              onPressed: isWatching ? onStopWatch : onWatchPrice,
              style: FilledButton.styleFrom(
                backgroundColor:
                    isWatching ? Colors.red : Theme.of(context).colorScheme.primary,
              ),
              child: Text(isWatching ? 'Stop Live Price' : 'Watch Live Price'),
            ),
          ],
        ),
      ),
    );
  }
}

class _PriceTickerCard extends StatelessWidget {
  const _PriceTickerCard({required this.ticker});
  final PriceUpdate ticker;

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      color: const Color(0xFFE8F4F8),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('LIVE PRICE',
                  style: Theme.of(context)
                      .textTheme
                      .labelSmall
                      ?.copyWith(color: Colors.grey, fontWeight: FontWeight.bold)),
              Text(ticker.productId,
                  style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
          Text(
            '${ticker.currency} ${ticker.pricePaise ~/ 100}'
            '.${(ticker.pricePaise % 100).toString().padLeft(2, '0')}',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                  color: const Color(0xFF0D1B2A),
                ),
          ),
        ],
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message,
              style: const TextStyle(color: Colors.red),
              textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton(onPressed: onRetry, child: const Text('Retry')),
        ],
      ),
    );
  }
}
