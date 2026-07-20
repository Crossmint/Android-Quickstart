package com.crossmint.kotlin.utility

import android.widget.Toast
import com.crossmint.kotlin.appInstance

actual fun showToast(message: String) {
    Toast.makeText(appInstance, message, Toast.LENGTH_SHORT).show()
}
