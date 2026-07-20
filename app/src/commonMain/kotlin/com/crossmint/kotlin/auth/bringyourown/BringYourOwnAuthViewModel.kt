package com.crossmint.kotlin.auth.bringyourown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossmint.kotlin.Deps
import com.crossmint.kotlin.auth.AuthManager
import com.crossmint.kotlin.utility.showToast
import kotlin.io.encoding.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class BringYourOwnAuthViewModel(
    private val goToWallets: () -> Unit,
    private val authManager: AuthManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BringYourOwnAuthUiState())
    val uiState: StateFlow<BringYourOwnAuthUiState> = _uiState.asStateFlow()

    fun clearMessages() {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = null,
                successMessage = null,
            )
    }

    fun updateJWT(token: String) {
        _uiState.value = _uiState.value.copy(jwt = token, errorMessage = null)
    }

    fun submitJWT() {
        viewModelScope.launch {
            val email = extractEmailFromJWT(_uiState.value.jwt)
            if (email == null) {
                showToast("JWT must contain an email address")
                return@launch
            }

            // This function doesn't actually validate the JWT against the JWKS endpoint, but my presumption is that it
            // would. Right now we are kind of implicitly relying on the first getWallet api call to validate our JWT token.
            authManager.setJWT(_uiState.value.jwt)

            showToast("Authentication successful")
            withContext(Dispatchers.Main) {
                Deps.authEmail = email
                goToWallets()
            }
        }
    }

    private fun extractEmailFromJWT(token: String): String? {
        return try {
            // Split the JWT into its three parts: header.payload.signature
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }

            // Decode the payload (second part)
            val payload = parts[1]

            // Base64 URL decode - need to handle padding
            val paddedPayload =
                when (payload.length % 4) {
                    2 -> payload + "=="
                    3 -> payload + "="
                    else -> payload
                }

            val decodedBytes = Base64.UrlSafe.decode(paddedPayload)
            val payloadJson = decodedBytes.decodeToString()

            // Parse JSON and extract email
            val jsonElement = Json.parseToJsonElement(payloadJson)
            jsonElement.jsonObject["email"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
    }
}

data class BringYourOwnAuthUiState(
    val jwt: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val userEmail: String? = null,
) {
    val hasError: Boolean
        get() = errorMessage != null
}
