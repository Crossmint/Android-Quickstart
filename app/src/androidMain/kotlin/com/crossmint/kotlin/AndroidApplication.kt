package com.crossmint.kotlin

import android.app.Application

class AndroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appInstance = this
    }
}
