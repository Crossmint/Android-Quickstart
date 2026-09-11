package com.crossmint.kotlin.checkoutdemo

import com.crossmint.kotlin.checkoutdemo.model.DemoEvent
import com.crossmint.kotlin.checkoutdemo.model.OrderDraft
import com.crossmint.kotlin.checkoutdemo.model.OrderSession
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundOptions
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundSection

data class PlaygroundUiState(
    val section: PlaygroundSection = PlaygroundSection.ORDER,
    val draft: OrderDraft = OrderDraft(),
    val options: PlaygroundOptions = PlaygroundOptions(),
    val existingOrderId: String = "",
    val existingClientSecret: String = "",
    val identityInquiryId: String = "",
    val identitySessionToken: String = "",
    val confirmedIdentityInquiryId: String = "",
    val confirmedIdentitySessionToken: String = "",
    val session: OrderSession? = null,
    val isCreatingOrder: Boolean = false,
    val orderErrorMessage: String? = null,
    val events: List<DemoEvent> = emptyList(),
) {
    val isPreviewingCheckout: Boolean
        get() = session != null

    val isPreviewingIdentity: Boolean
        get() = confirmedIdentityInquiryId.isNotBlank()
}
