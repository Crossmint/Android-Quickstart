package com.crossmint.kotlin.checkoutdemo.model

import com.crossmint.kotlin.core.Environment

data class TokenNetwork(
    val chain: String,
    val network: String,
    val locator: String,
) {
    val title: String get() = "$chain ($network)"
}

data class TokenPreset(
    val symbol: String,
    val networks: List<TokenNetwork>,
) {
    companion object {
        private val stagingPresets =
            listOf(
                TokenPreset(
                    symbol = "USDC",
                    networks =
                        listOf(
                            TokenNetwork("Solana", "Devnet", "solana:4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"),
                            TokenNetwork("Base", "Sepolia", "base-sepolia:0x036CbD53842c5426634e7929541eC2318f3dCF7e"),
                            TokenNetwork(
                                "Stellar",
                                "Testnet",
                                "stellar:CBIELTK6YBZJU5UP2WWQEUCYKLPU6AUNZ2BQ4WWFEIE3USCIHMXQDAMA",
                            ),
                            TokenNetwork("Polygon", "Amoy", "polygon-amoy:0x41E94Eb019C0762f9Bfcf9Fb1E58725BfB0e7582"),
                        ),
                ),
                TokenPreset(
                    symbol = "EURC",
                    networks =
                        listOf(
                            TokenNetwork("Solana", "Devnet", "solana:HzwqbKZw8HxMN6bF2yFZNrht3c2iXXzpKcFu7uBEDKtr"),
                            TokenNetwork("Base", "Sepolia", "base-sepolia:0x808456652fdb597867f38412077A9182bf77359F"),
                            TokenNetwork(
                                "Stellar",
                                "Testnet",
                                "stellar:CCUUDM434BMZMYWYDITHFXHDMIVTGGD6T2I5UKNX5BSLXLW7HVR4MCGZ",
                            ),
                        ),
                ),
                TokenPreset(
                    symbol = "Wirex USDC",
                    networks =
                        listOf(
                            TokenNetwork(
                                "WirexStellar",
                                "Stellar Testnet",
                                "stellar:CAUGJT4GREIY3WHOUUU5RIUDGSPVREF5CDCYJOWMHOVT2GWQT5JEETGJ",
                            ),
                        ),
                ),
            )

        private val productionPresets =
            listOf(
                TokenPreset(
                    symbol = "USDC",
                    networks =
                        listOf(
                            TokenNetwork("Solana", "Mainnet", "solana:EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"),
                            TokenNetwork("Base", "Mainnet", "base:0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"),
                            TokenNetwork(
                                "Stellar",
                                "Mainnet",
                                "stellar:CCW67TSZV3SSS2HXMBQ5JFGCKJNXKZM7UQUWUZPUTHXSTZLEO7SJMI75",
                            ),
                            TokenNetwork("Polygon", "Mainnet", "polygon:0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"),
                        ),
                ),
                TokenPreset(
                    symbol = "EURC",
                    networks =
                        listOf(
                            TokenNetwork("Solana", "Mainnet", "solana:HzwqbKZw8HxMN6bF2yFZNrht3c2iXXzpKcFu7uBEDKtr"),
                            TokenNetwork("Base", "Mainnet", "base:0x60a3E35Cc302bFA44Cb288Bc5a4F316Fdb1adb42"),
                            TokenNetwork(
                                "Stellar",
                                "Mainnet",
                                "stellar:CDTKPWPLOURQA2SGTKTUQOWRCBZEORB4BWBOMJ3D3ZTQQSGE5F6JBQLV",
                            ),
                        ),
                ),
            )

        fun presets(environment: Environment): List<TokenPreset> =
            if (environment == Environment.PRODUCTION) productionPresets else stagingPresets

        fun findByLocator(
            environment: Environment,
            locator: String,
        ): Pair<TokenPreset, TokenNetwork>? {
            for (preset in presets(environment)) {
                val network = preset.networks.find { it.locator == locator }
                if (network != null) return preset to network
            }
            return null
        }
    }
}
