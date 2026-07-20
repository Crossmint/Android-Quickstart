package com.crossmint.kotlin.wallet.playground

import android.content.Context
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import com.crossmint.kotlin.signers.DelegatedSigner
import java.math.BigInteger
import java.security.SecureRandom
import org.json.JSONObject

@Composable
actual fun rememberPasskeyCreator(): (suspend (name: String) -> DelegatedSigner.Passkey?)? {
    val context = LocalContext.current
    return { name -> createPasskeySigner(context, name) }
}

private suspend fun createPasskeySigner(
    context: Context,
    name: String,
): DelegatedSigner.Passkey? {
    val credentialManager = CredentialManager.create(context)

    val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val challengeB64 = Base64.encodeToString(challenge, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    val userId = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val userIdB64 = Base64.encodeToString(userId, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    val requestJson =
        buildString {
            append("""{"challenge":"$challengeB64",""")
            append(""""rp":{"id":"crossmint.com","name":"Crossmint"},""")
            append(""""user":{"id":"$userIdB64","name":"$name","displayName":"$name"},""")
            append(""""pubKeyCredParams":[{"type":"public-key","alg":-7}],""")
            append(
                """"authenticatorSelection":{"authenticatorAttachment":"platform","requireResidentKey":false,"residentKey":"preferred","userVerification":"preferred"},""",
            )
            append(""""timeout":60000,"attestation":"none"}""")
        }

    return try {
        val response =
            credentialManager.createCredential(
                context = context,
                request = CreatePublicKeyCredentialRequest(requestJson),
            ) as CreatePublicKeyCredentialResponse

        parseRegistrationResponse(response.registrationResponseJson, name)
    } catch (_: CreateCredentialCancellationException) {
        null
    }
}

private fun parseRegistrationResponse(
    json: String,
    name: String,
): DelegatedSigner.Passkey {
    val responseObj = JSONObject(json)
    val credentialId = responseObj.getString("id")
    val attestationB64 = responseObj.getJSONObject("response").getString("attestationObject")

    val attestationBytes = Base64.decode(attestationB64, Base64.URL_SAFE or Base64.NO_PADDING)
    val authData = extractAuthDataFromCbor(attestationBytes)

    // authData binary layout: rpIdHash(32) | flags(1) | signCount(4) | aaguid(16) | credIdLen(2) | credId | COSE key
    val credIdLen = ((authData[53].toInt() and 0xFF) shl 8) or (authData[54].toInt() and 0xFF)
    val coseKey = authData.copyOfRange(55 + credIdLen, authData.size)

    val (x, y) = extractXYFromCoseKey(coseKey)
    return DelegatedSigner.Passkey(id = credentialId, name = name, publicKeyX = x, publicKeyY = y)
}

// CBOR parsing — just enough to find "authData" bytes in the attestation object map.

private fun extractAuthDataFromCbor(cbor: ByteArray): ByteArray {
    var pos = 0
    val firstByte = cbor[pos++].toInt() and 0xFF
    check(firstByte shr 5 == 5) { "Expected CBOR map at start of attestation object" }
    var mapSize: Int
    val (ms, p1) = cborLength(cbor, pos, firstByte and 0x1F)
    mapSize = ms
    pos = p1

    repeat(mapSize) {
        // Key is always a text string in FIDO2 attestation objects
        val keyByte = cbor[pos++].toInt() and 0xFF
        check(keyByte shr 5 == 3) { "Expected CBOR text key" }
        val (keyLen, p2) = cborLength(cbor, pos, keyByte and 0x1F)
        pos = p2
        val key = String(cbor, pos, keyLen)
        pos += keyLen

        if (key == "authData") {
            val dataByte = cbor[pos++].toInt() and 0xFF
            check(dataByte shr 5 == 2) { "authData value must be CBOR bytes" }
            val (dataLen, p3) = cborLength(cbor, pos, dataByte and 0x1F)
            pos = p3
            return cbor.copyOfRange(pos, pos + dataLen)
        } else {
            pos = skipCborValue(cbor, pos)
        }
    }
    error("authData not found in attestation object")
}

/** Returns (length, positionAfterLengthBytes). Does not consume the initial header byte. */
private fun cborLength(
    cbor: ByteArray,
    pos: Int,
    additional: Int,
): Pair<Int, Int> =
    when {
        additional < 24 -> Pair(additional, pos)
        additional == 24 -> Pair(cbor[pos].toInt() and 0xFF, pos + 1)
        additional == 25 -> {
            val h = cbor[pos].toInt() and 0xFF
            val l = cbor[pos + 1].toInt() and 0xFF
            Pair((h shl 8) or l, pos + 2)
        }
        additional == 26 -> {
            val v =
                ((cbor[pos].toInt() and 0xFF) shl 24) or
                    ((cbor[pos + 1].toInt() and 0xFF) shl 16) or
                    ((cbor[pos + 2].toInt() and 0xFF) shl 8) or
                    (cbor[pos + 3].toInt() and 0xFF)
            Pair(v, pos + 4)
        }
        else -> error("Unsupported CBOR additional info: $additional")
    }

private fun skipCborValue(
    cbor: ByteArray,
    startPos: Int,
): Int {
    var pos = startPos
    val firstByte = cbor[pos++].toInt() and 0xFF
    val type = firstByte shr 5
    val additional = firstByte and 0x1F
    val (length, newPos) = cborLength(cbor, pos, additional)
    pos = newPos
    when (type) {
        2, 3 -> pos += length // bytes / text
        4 -> repeat(length) { pos = skipCborValue(cbor, pos) } // array
        5 -> repeat(length * 2) { pos = skipCborValue(cbor, pos) } // map (key + value each)
    }
    return pos
}

/**
 * Finds x and y in a COSE_Key for P-256 by scanning for the fixed CBOR byte patterns:
 *   0x21 0x58 0x20  →  CBOR key -2 (x), followed by bytes(32)
 *   0x22 0x58 0x20  →  CBOR key -3 (y), followed by bytes(32)
 * Returns (x, y) as decimal BigInteger strings.
 */
private fun extractXYFromCoseKey(coseKey: ByteArray): Pair<String, String> {
    var xBytes: ByteArray? = null
    var yBytes: ByteArray? = null
    var i = 0
    while (i <= coseKey.size - 35) {
        val b = coseKey[i].toInt() and 0xFF
        if ((b == 0x21 || b == 0x22) &&
            (coseKey[i + 1].toInt() and 0xFF) == 0x58 &&
            (coseKey[i + 2].toInt() and 0xFF) == 0x20
        ) {
            val coord = coseKey.copyOfRange(i + 3, i + 35)
            if (b == 0x21) xBytes = coord else yBytes = coord
            i += 35
        } else {
            i++
        }
    }
    checkNotNull(xBytes) { "P-256 x coordinate not found in COSE key" }
    checkNotNull(yBytes) { "P-256 y coordinate not found in COSE key" }
    return Pair(BigInteger(1, xBytes).toString(), BigInteger(1, yBytes).toString())
}
