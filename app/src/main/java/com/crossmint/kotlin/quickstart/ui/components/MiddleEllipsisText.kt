package com.crossmint.kotlin.quickstart.ui.components

fun middleEllipsisText(text: String, maxLength: Int = 12): String {
    if (text.length <= maxLength) return text

    val charsToShow = maxLength - 3
    val prefixLength = charsToShow / 2
    val suffixLength = charsToShow - prefixLength

    return "${text.take(prefixLength)}...${text.takeLast(suffixLength)}"
}
