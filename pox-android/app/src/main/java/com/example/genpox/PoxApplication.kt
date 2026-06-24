package com.example.genpox

import android.app.Application
import com.example.genpox.data.DataRepository
import com.example.genpox.data.DefaultDataRepository

class PoxApplication : Application() {
    lateinit var repository: DataRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DefaultDataRepository(this)
        com.example.genpox.theme.CyberTheme.loadPreset(this, "active_colors")
    }
}
