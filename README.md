# Crossmint Android Quickstart

Two Android sample apps built on the [Crossmint Kotlin SDK](https://github.com/Crossmint/crossmint-kotlin-sdk), one for wallets and one for checkout. Both are Jetpack Compose apps that consume the published `com.crossmint:*` artifacts from Maven Central.

| Module | What it shows | SDK artifacts |
| --- | --- | --- |
| `wallets` | Email/phone OTP and bring-your-own-auth sign in, wallet creation on EVM/Solana/Stellar, balances, transfers, signers, passkeys | `crossmint-sdk`, `crossmint-compose` |
| `checkout` | Embedded checkout playground: create an order, tweak appearance and payment options, preview the checkout and identity verification (KYC) | `sdk-core`, `sdk-checkout`, `sdk-identity` |

The source of both apps lives in the SDK repo (`crossmintDemoApp` and `crossmintCheckoutDemoApp`) and is mirrored here on every stable SDK release. Open pull requests against the SDK repo for code changes, and against this repo only for build wiring.

## Prerequisites
- Android Studio (Ladybug or newer) with the Android SDK and an emulator or device running API 24+.
- JDK 17.
- A Crossmint client API key (see below).

## Get your Crossmint API key
1. Sign in to the [Crossmint staging console](https://staging.crossmint.com/console) and create a project.
2. Create a client API key. Pick `App type -> Mobile` and register the package name of the app you want to run (`com.crossmint.kotlin` for wallets, `com.crossmint.checkoutdemo` for checkout).
3. Enable the scopes the app needs: the wallet scopes (create, read, transactions, signers) for the wallets app, `orders.create` and `orders.read` for the checkout app.
4. For production, repeat the steps in the [production console](https://www.crossmint.com/console). The SDK picks the environment from the key prefix (`ck_staging_` or `ck_production_`).

## Configure local properties
1. Copy the template: `cp local.properties.example local.properties`.
2. Fill in the Android SDK path and your key:
   ```properties
   sdk.dir=/absolute/path/to/Android/sdk
   crossmint_api_key=ck_staging_...
   ```

`local.properties` is gitignored. The build exposes the key to both apps through `BuildKonfig.CROSSMINT_API_KEY`.

## Run the samples
Android Studio: open the repo root, let Gradle sync, then pick the `wallets` or `checkout` run configuration.

Command line, with a device or emulator connected:
```bash
./gradlew :wallets:installDebug
./gradlew :checkout:installDebug
```

## Where to look

### Wallets
- `wallets/src/androidMain/kotlin/com/crossmint/kotlin/AppRoot.kt` calls `CrossmintSDK.configure` with the key and hosts the app.
- `wallets/src/commonMain/kotlin/com/crossmint/kotlin/DemoApp.kt` holds the navigation between auth and wallet screens.
- `wallets/src/commonMain/kotlin/com/crossmint/kotlin/wallet/WalletViewModel.kt` covers `getWallet`, `createWallet`, balances, transfers and signer management.
- `wallets/src/commonMain/kotlin/com/crossmint/kotlin/auth/` shows both Crossmint managed OTP auth and bring-your-own-auth (`setJWT`).

### Checkout
- `checkout/src/commonMain/kotlin/com/crossmint/kotlin/checkoutdemo/ui/CheckoutPreviewHost.kt` embeds `CrossmintEmbeddedCheckout` and handles its events.
- `checkout/src/commonMain/kotlin/com/crossmint/kotlin/checkoutdemo/data/OrdersApi.kt` creates the order the checkout renders, using the Orders API with the same client key.
- `checkout/src/commonMain/kotlin/com/crossmint/kotlin/checkoutdemo/ui/sections/` holds one screen per playground option group (order, payment, appearance, fields, identity, events).

## Troubleshooting
- OTP emails not arriving: check the spam folder and confirm the email is allowed in your staging project.
- `WalletNotFound`: create a wallet first, or switch to a chain where the user already has one.
- Checkout shows an API error: make sure the key has the `orders.create` and `orders.read` scopes and matches the app's package name.
- Gradle sync issues: check `sdk.dir` in `local.properties` and that Android Studio is using JDK 17.
