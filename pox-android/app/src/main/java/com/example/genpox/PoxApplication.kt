package com.example.genpox

import android.app.Application
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.DataRepository
import com.example.genpox.data.DefaultDataRepository

class PoxApplication : Application() {
    lateinit var repository: DataRepository
        private set
    lateinit var synthManager: PoxSynthManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DefaultDataRepository(this)
        synthManager = PoxSynthManager().apply { initialize(this@PoxApplication) }
        com.example.genpox.theme.CyberTheme.loadPreset(this, "active_colors")
    }
}

