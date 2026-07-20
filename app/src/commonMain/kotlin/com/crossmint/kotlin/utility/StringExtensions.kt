package com.crossmint.kotlin.utility

internal fun String.truncateLocator(): String = if (length > 30) take(14) + "..." + takeLast(10) else this
