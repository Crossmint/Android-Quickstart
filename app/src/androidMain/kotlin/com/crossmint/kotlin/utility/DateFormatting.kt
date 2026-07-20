package com.crossmint.kotlin.utility

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatTimestamp(timestamp: Long): String {
    // Create a new instance each time for thread safety
    // SimpleDateFormat is not thread-safe, so we don't cache it
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
