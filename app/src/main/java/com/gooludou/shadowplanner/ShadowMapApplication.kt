package com.gooludou.shadowplanner

import android.app.Application
import com.google.android.filament.Filament
import com.google.android.filament.filamat.MaterialBuilder
import com.mapbox.common.MapboxOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShadowMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Filament.init()
        MaterialBuilder.init()
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
    }
}
