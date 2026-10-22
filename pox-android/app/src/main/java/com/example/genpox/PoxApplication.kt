package com.example.genpox

import android.app.Application
import com.example.genpox.audio.PoxSynthManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp(Application::class)
class PoxApplication : Hilt_PoxApplication() {
    @Inject
    lateinit var synthManager: PoxSynthManager

    override fun onCreate() {
        super.onCreate()
        com.example.genpox.theme.CyberTheme.loadPreset(this, "active_colors")
    }
}
