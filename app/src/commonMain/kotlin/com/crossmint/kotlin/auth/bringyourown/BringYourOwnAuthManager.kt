package com.crossmint.kotlin.auth.bringyourown

import com.crossmint.kotlin.auth.AuthManager

class BringYourOwnAuthManager : AuthManager {
    private var jwt: String? = null

    override suspend fun getJWT(): String? = jwt

    override fun setJWT(jwt: String) {
        this.jwt = jwt
    }

    override suspend fun logout() {
        this.jwt = null
    }
}
