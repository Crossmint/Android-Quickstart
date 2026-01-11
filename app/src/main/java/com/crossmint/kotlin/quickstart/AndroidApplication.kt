package com.crossmint.kotlin.quickstart

import android.app.Application
import com.crossmint.kotlin.Crossmint

class AndroidApplication : Application() {
   override fun onCreate() {
      super.onCreate()

      Crossmint.initialize(
         apiKey = BuildConfig.CROSSMINT_API_KEY
      )
   }
}