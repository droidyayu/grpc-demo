package com.demo.grpcdemo.di

import com.demo.grpcdemo.config.FeatureFlags
import com.demo.grpcdemo.data.CatalogRepositoryImpl
import com.demo.grpcdemo.data.RestCatalogRepositoryImpl
import com.demo.grpcdemo.domain.repository.CatalogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * ── DEMO SLIDE: THE ONE-LINE SWAP ────────────────────────
     *
     * The feature flag decides which implementation is injected.
     * The ViewModel, use cases, and UI never change.
     *
     * useGrpcCatalog = true  → gRPC  (binary, streaming, efficient)
     * useGrpcCatalog = false → REST  (JSON polling, before state)
     *
     * In production: back FeatureFlags with Firebase Remote Config
     * so you can flip this for 1% of users without a release.
     * ─────────────────────────────────────────────────────────
     */
    @Provides
    @Singleton
    fun provideCatalogRepository(
        flags:    FeatureFlags,
        grpcImpl: CatalogRepositoryImpl,
        restImpl: RestCatalogRepositoryImpl,
    ): CatalogRepository =
        if (flags.useGrpcCatalog()) grpcImpl else restImpl
}
