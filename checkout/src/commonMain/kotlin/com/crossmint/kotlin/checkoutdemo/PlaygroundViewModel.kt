package com.crossmint.kotlin.checkoutdemo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossmint.kotlin.checkoutdemo.data.OrdersApi
import com.crossmint.kotlin.checkoutdemo.model.DemoEvent
import com.crossmint.kotlin.checkoutdemo.model.OrderDraft
import com.crossmint.kotlin.checkoutdemo.model.OrderSession
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundOptions
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundSection
import com.crossmint.kotlin.checkoutdemo.utility.currentTimeMillis
import com.crossmint.kotlin.types.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaygroundViewModel(
    private val ordersApi: OrdersApi,
    private val apiKey: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaygroundUiState())
    val uiState: StateFlow<PlaygroundUiState> = _uiState.asStateFlow()

    fun selectSection(section: PlaygroundSection) {
        _uiState.value = _uiState.value.copy(section = section)
    }

    fun updateDraft(draft: OrderDraft) {
        _uiState.value = _uiState.value.copy(draft = draft)
    }

    fun updateOptions(options: PlaygroundOptions) {
        _uiState.value = _uiState.value.copy(options = options)
    }

    fun updateExistingOrder(
        orderId: String,
        clientSecret: String,
    ) {
        _uiState.value = _uiState.value.copy(existingOrderId = orderId, existingClientSecret = clientSecret)
    }

    fun updateIdentityCredentials(
        inquiryId: String,
        sessionToken: String,
    ) {
        _uiState.value = _uiState.value.copy(identityInquiryId = inquiryId, identitySessionToken = sessionToken)
    }

    fun startIdentityPreview() {
        val state = _uiState.value
        _uiState.value =
            state.copy(
                confirmedIdentityInquiryId = state.identityInquiryId,
                confirmedIdentitySessionToken = state.identitySessionToken,
            )
    }

    fun useExistingOrder() {
        val state = _uiState.value
        val orderId = state.existingOrderId.trim()
        val clientSecret = state.existingClientSecret.trim()
        if (orderId.isEmpty() || clientSecret.isEmpty()) return

        setSession(
            OrderSession(orderId = orderId, clientSecret = clientSecret, source = OrderSession.Source.EXISTING),
            "Using existing order $orderId",
        )
    }

    fun createOrder() {
        val draft = _uiState.value.draft
        _uiState.value = _uiState.value.copy(isCreatingOrder = true, orderErrorMessage = null)

        viewModelScope.launch {
            when (val result = ordersApi.createOrder(apiKey, draft)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isCreatingOrder = false)
                    setSession(result.value, "Order ${result.value.orderId} created")
                }

                is Result.Failure -> {
                    _uiState.value =
                        _uiState.value.copy(
                            isCreatingOrder = false,
                            orderErrorMessage = result.error.message,
                        )
                    logEvent("Order creation failed: ${result.error.message}")
                }
            }
        }
    }

    fun clearSession() {
        _uiState.value = _uiState.value.copy(session = null)
    }

    fun logExternalEvent(message: String) = logEvent(message)

    private fun setSession(
        session: OrderSession,
        eventMessage: String,
    ) {
        _uiState.value = _uiState.value.copy(session = session)
        logEvent(eventMessage)
    }

    private fun logEvent(message: String) {
        val event = DemoEvent(timestamp = currentTimeMillis(), message = message)
        _uiState.value = _uiState.value.copy(events = _uiState.value.events + event)
    }
}
