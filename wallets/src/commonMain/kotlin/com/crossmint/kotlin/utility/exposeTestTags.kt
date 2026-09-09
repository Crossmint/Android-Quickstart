package com.crossmint.kotlin.utility

import androidx.compose.ui.Modifier

/**
 * Exposes Compose testTags as resource-ids for UI-automation frameworks (Maestro, UIAutomator).
 *
 * `testTagsAsResourceId` only affects the semantics subtree it is set on, and popup windows
 * (ModalBottomSheet, DropdownMenu, Dialog) host their own semantics roots — the flag set on the
 * app root does NOT propagate into them. Apply this modifier to the root content of every popup
 * whose descendants declare testTags that E2E flows match by id.
 *
 * No-op on non-Android targets where the property does not exist.
 */
expect fun Modifier.exposeTestTags(): Modifier
