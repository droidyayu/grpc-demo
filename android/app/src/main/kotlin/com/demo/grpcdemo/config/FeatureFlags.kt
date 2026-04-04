package com.demo.grpcdemo.config

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flag provider — thin wrapper so the source can be swapped without
 * changing call sites (local BuildConfig today, RemoteConfig / LaunchDarkly tomorrow).
 *
 * ── DEMO SLIDE NOTE ──────────────────────────────────────────────────────────
 * In production you would back this with Firebase Remote Config:
 *
 *   val remoteConfig = Firebase.remoteConfig
 *   fun useGrpcCatalog() = remoteConfig.getBoolean("use_grpc_catalog")
 *
 * The RepositoryModule reads this flag and binds either RestCatalogRepositoryImpl
 * or CatalogRepositoryImpl (gRPC) — zero ViewModel changes either way.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Singleton
class FeatureFlags @Inject constructor() {

    /**
     * `true`  → use gRPC transport  (CatalogRepositoryImpl)
     * `false` → use REST transport  (RestCatalogRepositoryImpl)
     *
     * Flip this to `false` to demo the REST → gRPC swap live on stage.
     */
    fun useGrpcCatalog(): Boolean = true
}
