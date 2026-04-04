package com.demo.grpcdemo.di

import com.demo.grpcdemo.BuildConfig
import com.demo.grpcdemo.data.GrpcCatalogDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideManagedChannel(): ManagedChannel {
        val builder = ManagedChannelBuilder
            .forAddress(BuildConfig.GRPC_HOST, BuildConfig.GRPC_PORT)
        // Use plaintext for debug builds; TLS for release
        if (BuildConfig.DEBUG) builder.usePlaintext()
        else builder.useTransportSecurity()
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideGrpcCatalogDataSource(channel: ManagedChannel): GrpcCatalogDataSource =
        GrpcCatalogDataSource(channel)

    @Provides
    @Named("restBaseUrl")
    fun provideRestBaseUrl(): String = BuildConfig.REST_BASE_URL
}
