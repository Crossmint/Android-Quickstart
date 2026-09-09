package com.crossmint.kotlin.checkoutdemo.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val checkoutDemoJson =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

fun createHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(checkoutDemoJson)
        }
    }
