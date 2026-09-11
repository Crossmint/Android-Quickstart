package com.crossmint.kotlin.checkoutdemo.data

import com.crossmint.kotlin.checkout.CheckoutEnvironment
import com.crossmint.kotlin.checkoutdemo.model.OrderDraft
import com.crossmint.kotlin.checkoutdemo.model.OrderSession
import com.crossmint.kotlin.core.ApiKey
import com.crossmint.kotlin.core.Environment
import com.crossmint.kotlin.types.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class OrdersApi(
    private val httpClient: HttpClient,
    private val json: Json = checkoutDemoJson,
) {
    suspend fun createOrder(
        apiKey: String,
        draft: OrderDraft,
    ): Result<OrderSession, OrderApiError> {
        val parsedKey =
            try {
                ApiKey.fromString(apiKey)
            } catch (e: Exception) {
                return Result.Failure(OrderApiError.Api(e.message ?: "Invalid API key."))
            }

        val requestBody =
            CreateOrderRequest(
                payment =
                    OrderPayment(
                        receiptEmail = draft.receiptEmail.trim().ifEmpty { null },
                    ),
                lineItems =
                    OrderLineItems(
                        tokenLocator = draft.tokenLocator.trim(),
                        executionParameters = OrderExecutionParameters(amount = draft.amount.trim()),
                    ),
                recipient = OrderRecipient(walletAddress = draft.recipientWalletAddress.trim()),
            )

        return try {
            val response =
                httpClient.post("https://${parsedKey.host}/api/2022-06-09/orders") {
                    header("X-API-KEY", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
            val raw = response.bodyAsText()
            val parsed = json.decodeFromString(CreateOrderResponse.serializer(), raw)

            val orderId = parsed.order?.orderId
            val clientSecret = parsed.clientSecret
            if (orderId != null && clientSecret != null) {
                Result.Success(
                    OrderSession(
                        orderId = orderId,
                        clientSecret = clientSecret,
                        source = OrderSession.Source.CREATED_IN_APP,
                    ),
                )
            } else if (parsed.error == true) {
                Result.Failure(OrderApiError.Api(parsed.message ?: "The Orders API rejected the request."))
            } else {
                Result.Failure(OrderApiError.UnexpectedResponse())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            Result.Failure(OrderApiError.UnexpectedResponse())
        } catch (e: Exception) {
            Result.Failure(OrderApiError.Network(e))
        }
    }
}

internal val ApiKey.host: String
    get() = "${apiEnvironmentPathComponent}crossmint.com"

internal val ApiKey.checkoutEnvironment: CheckoutEnvironment
    get() =
        when (environment) {
            Environment.PRODUCTION -> CheckoutEnvironment.PRODUCTION
            Environment.STAGING, Environment.DEVELOPMENT -> CheckoutEnvironment.STAGING
        }

@Serializable
private data class CreateOrderRequest(
    val payment: OrderPayment,
    val lineItems: OrderLineItems,
    val recipient: OrderRecipient,
)

@Serializable
private data class OrderPayment(
    val method: String = "card",
    val currency: String = "usd",
    val receiptEmail: String? = null,
)

@Serializable
private data class OrderLineItems(
    val tokenLocator: String,
    val executionParameters: OrderExecutionParameters,
)

@Serializable
private data class OrderExecutionParameters(
    val mode: String = "exact-in",
    val amount: String,
)

@Serializable
private data class OrderRecipient(
    val walletAddress: String,
)

@Serializable
private data class CreateOrderResponse(
    val error: Boolean? = null,
    val message: String? = null,
    val clientSecret: String? = null,
    val order: CreateOrderResponseOrder? = null,
)

@Serializable
private data class CreateOrderResponseOrder(
    @SerialName("orderId")
    val orderId: String? = null,
)
