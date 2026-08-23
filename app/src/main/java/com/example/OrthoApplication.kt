package com.example

import android.app.Application
import com.example.di.AppModule

class OrthoApplication : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        appModule = AppModule(this)
    }
}
