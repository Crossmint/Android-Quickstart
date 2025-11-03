# Crossmint Android Quickstart

An Android + Jetpack Compose sample that demonstrates how to authenticate with Crossmint, manage wallets, and submit EVM transactions using the Crossmint Kotlin SDK.

## Prerequisites
- Android Studio Hedgehog (or newer) with the Android SDK and an emulator or device running API 24+.
- JDK 11 (bundled with current Android Studio versions).
- A Crossmint developer account and API key from the [Crossmint console](https://www.crossmint.com/console).
- (Optional) An email inbox you can use for one-time-passcode (OTP) authentication during sign-in.

## Configure local properties
1. Copy the template: `cp local.properties.example local.properties`.
2. Update the paths and credentials inside `local.properties`:
   ```properties
   sdk.dir=/absolute/path/to/Android/sdk
   CROSSMINT_API_KEY=your_live_or_test_key
   ```
3. Never commit `local.properties` — it contains machine-specific secrets and is already gitignored.

The build reads `CROSSMINT_API_KEY` from `BuildConfig`, which the Compose UI injects into the Crossmint SDK during startup.

## Run the sample
- **Android Studio:** Open the project folder, let Gradle sync, then click *Run* and choose an emulator or USB device.
- **Command line:** From the repo root, install a debug build on a connected device/emulator:
  ```bash
  ./gradlew installDebug
  ```
  If you only need to build, use `./gradlew assembleDebug`.

On first launch you will be prompted for an email. Crossmint sends an OTP to that inbox, which the app uses to complete authentication before loading wallets.

## Crossmint SDK wiring

### Initialize the SDK with your API key
```kotlin
CrossmintSDKProvider
    .Builder(BuildConfig.CROSSMINT_API_KEY)
    .developmentMode()
    .onTEERequired { onOTPSubmit, onDismiss ->
        OTPDialog(onOTPSubmit = onOTPSubmit, onDismiss = onDismiss)
    }
    .build { QuickstartContent() }
```
Location: `app/src/main/java/com/crossmint/kotlin/quickstart/QuickstartApp.kt`

- `BuildConfig.CROSSMINT_API_KEY` resolves from `local.properties`.
- `.developmentMode()` points the SDK at Crossmint’s sandbox environment.
- `onTEERequired` is optional; include it only when your signer type requires OTP-based signing (email or phone) so the SDK can surface the verification dialog.

## Wallet operations in the sample

All wallet functionality lives in `WalletViewModel` (`app/src/main/java/com/crossmint/kotlin/quickstart/wallet/WalletViewModel.kt`). The snippets below highlight the most important calls.

### Fetch an existing wallet
```kotlin
viewModelScope.launch {
    when (val result = crossmintWallets.getWallet(chain)) {
        is Result.Success -> {
            _uiState.value = _uiState.value.copy(
                wallet = result.value,
                isLoading = false,
                errorMessage = null,
                isEmpty = false,
            )
        }
        is Result.Failure -> {
            val isEmpty = result.error is WalletError.WalletNotFound
            _uiState.value = _uiState.value.copy(
                wallet = null,
                isLoading = false,
                errorMessage = if (!isEmpty) result.error.message else null,
                isEmpty = isEmpty,
            )
        }
    }
}
```
- `chain` is an instance of `Chain`/`EVMChain` (the sample uses `EVMChain.BaseSepolia`).
- `Result.Failure` includes typed `WalletError` values (e.g., `WalletNotFound`).

### Create a wallet for the authenticated user
```kotlin
viewModelScope.launch {
    when (val result = crossmintWallets.createWallet(chain, signer)) {
        is Result.Success -> _uiState.value = _uiState.value.copy(wallet = result.value)
        is Result.Failure -> _uiState.value = _uiState.value.copy(errorMessage = result.error.message)
    }
}
```
- `signer` is a `SignerData` describing how the user will sign transactions (the sample uses a placeholder phone signer).
- On success, subsequent calls can reuse the returned `Wallet`.

### Create (submit) a transaction
```kotlin
viewModelScope.launch {
    val wallet = _uiState.value.wallet ?: return@launch
    when (val result = wallet.send(recipient, tokenLocator, amount)) {
        is Result.Success -> {
            _uiState.value = _uiState.value.copy(transaction = result.value)
            fetchTransaction(wallet, result.value.id)
        }
        is Result.Failure -> _uiState.value = _uiState.value.copy(transactionError = result.error)
    }
}
```
- `recipient` is the destination address.
- `tokenLocator` identifies the asset (e.g., `eth:base-sepolia`).
- `amount` is a `Double` representing the quantity to send.
- On success the sample immediately fetches full transaction details.

### Fetch a transaction by ID
```kotlin
private suspend fun fetchTransaction(wallet: Wallet, transactionId: String) {
    when (val result = wallet.getTransaction(transactionId)) {
        is Result.Success -> _uiState.value = _uiState.value.copy(transaction = result.value)
        is Result.Failure -> _uiState.value = _uiState.value.copy(transactionError = result.error)
    }
}
```
- Use the `Transaction.id` returned by `wallet.send`.
- Errors propagate as typed `TransactionError` values.

### Approve (sign) a pending transaction
```kotlin
viewModelScope.launch {
    val wallet = _uiState.value.wallet ?: return@launch
    when (val result = wallet.approve(transactionId)) {
        is Result.Success -> _uiState.value = _uiState.value.copy(transaction = result.value)
        is Result.Failure -> _uiState.value = _uiState.value.copy(errorMessage = "Signing failed: ${result.error.message}")
    }
}
```
- Only call `approve` after `fetchTransaction` succeeds so the transaction is fully loaded.
- On success, the transaction status in `_uiState` reflects the signed state.

## Troubleshooting
- **OTP emails not arriving:** Verify the email is registered with Crossmint’s sandbox and check spam folders.
- **`WalletNotFound` errors:** Create a wallet first (`createWallet`) or switch to a chain where the user already has one.
- **Gradle sync issues:** Make sure the Android SDK path in `local.properties` matches your local installation and that you are using JDK 11.

Happy hacking with Crossmint on Android!
