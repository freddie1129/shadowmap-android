package com.gooludou.shadowplanner.di

import com.gooludou.shadowplanner.purchase.billing.GooglePlayInAppPurchaseManager
import com.gooludou.shadowplanner.purchase.billing.InAppPurchaseManager
import com.gooludou.shadowplanner.purchase.remoteconfig.FirebaseRemoteConfigManager
import com.gooludou.shadowplanner.purchase.remoteconfig.RemoteConfigManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PurchaseModule {
    @Binds
    @Singleton
    abstract fun bindRemoteConfigManager(manager: FirebaseRemoteConfigManager): RemoteConfigManager

    @Binds
    @Singleton
    abstract fun bindInAppPurchaseManager(
        manager: GooglePlayInAppPurchaseManager
    ): InAppPurchaseManager
}
