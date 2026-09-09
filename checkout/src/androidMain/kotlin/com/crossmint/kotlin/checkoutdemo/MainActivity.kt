package com.crossmint.kotlin.checkoutdemo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.crossmint.checkoutdemo.BuildKonfig

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot(apiKey = BuildKonfig.CROSSMINT_API_KEY)
        }
    }
}
