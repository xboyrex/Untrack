package com.xboyrex.untrack

import android.app.Application

class UntrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageSettings.init(this)
        GlassSettings.init(this)
        GlassQuality.init(this)
        ThemeSettings.init(this)
        UntrackStore.init(this)
    }
}
