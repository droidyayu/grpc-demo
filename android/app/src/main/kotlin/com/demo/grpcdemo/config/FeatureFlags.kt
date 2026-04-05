package com.demo.grpcdemo.config

import com.demo.grpcdemo.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlags @Inject constructor() {
    fun useGrpcCatalog(): Boolean = BuildConfig.GRPC_ENABLED
}
