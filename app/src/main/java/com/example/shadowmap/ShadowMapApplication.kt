package com.example.shadowmap

import android.app.Application
import com.google.android.filament.Filament
import com.google.android.filament.filamat.MaterialBuilder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShadowMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Filament.init()
        MaterialBuilder.init()
    }
}
