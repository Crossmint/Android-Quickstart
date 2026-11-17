package com.crossmint.kotlin.wallet.playground

import com.crossmint.kotlin.types.SignerData
import com.crossmint.kotlin.wallet.SignerOption

internal data class SignerLabel(
    val headline: String,
    val detail: String,
)

internal fun signerLabel(signer: SignerOption): SignerLabel {
    val type = signer.type.takeUnless { it == "unknown" } ?: locatorType(signer.locator)
    return signerLabel(type, signer.locator, signer.name)
}

internal fun signerLabel(signer: SignerData): SignerLabel =
    signerLabel(locatorType(signer.locator), signer.locator, (signer as? SignerData.Passkey)?.name)

private fun signerLabel(
    type: String,
    locator: String,
    name: String?,
): SignerLabel {
    val typeLabel = signerTypeLabel(type)
    val value = signerValue(type, locator)
    return if (name.isNullOrBlank()) {
        SignerLabel(typeLabel, value)
    } else {
        SignerLabel(name, listOf(typeLabel, value).filter { it.isNotEmpty() }.joinToString(" · "))
    }
}

private fun locatorType(locator: String): String = if (':' in locator) locator.substringBefore(':') else "unknown"

private fun signerValue(
    type: String,
    locator: String,
): String {
    val value = locator.substringAfter(':')
    return when {
        type == "email" || type == "phone" -> value
        value.length > 18 -> value.take(8) + "…" + value.takeLast(7)
        else -> value
    }
}

private fun signerTypeLabel(type: String): String =
    when (type) {
        "external-wallet" -> "External Wallet"
        "api-key" -> "API Key"
        else -> type.replaceFirstChar { it.titlecase() }
    }
