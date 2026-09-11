package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.checkoutdemo.model.PlaygroundOptions

@Composable
fun AppearanceSectionView(
    options: PlaygroundOptions,
    onOptionsChange: (PlaygroundOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Colors (hex) are global appearance variables — they recolor every element at " +
                "once. Corner radii below are per element.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = options.textColor,
            onValueChange = { onOptionsChange(options.copy(textColor = it)) },
            label = { Text("Text color") },
            placeholder = { Text("#1C1C1E") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        OutlinedTextField(
            value = options.backgroundColor,
            onValueChange = { onOptionsChange(options.copy(backgroundColor = it)) },
            label = { Text("Background color") },
            placeholder = { Text("#FFFFFF") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        OutlinedTextField(
            value = options.accentColor,
            onValueChange = { onOptionsChange(options.copy(accentColor = it)) },
            label = { Text("Accent color") },
            placeholder = { Text("#00C853") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        OutlinedTextField(
            value = options.dangerColor,
            onValueChange = { onOptionsChange(options.copy(dangerColor = it)) },
            label = { Text("Danger color") },
            placeholder = { Text("#D32F2F") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        RadiusSlider(
            label = "Input corner radius",
            value = options.inputRadius,
            onValueChange = { onOptionsChange(options.copy(inputRadius = it)) },
        )

        RadiusSlider(
            label = "Tab corner radius",
            value = options.tabRadius,
            onValueChange = { onOptionsChange(options.copy(tabRadius = it)) },
        )

        RadiusSlider(
            label = "Button corner radius",
            value = options.buttonRadius,
            onValueChange = { onOptionsChange(options.copy(buttonRadius = it)) },
        )
    }
}

@Composable
private fun RadiusSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Text(
        "$label: ${value.toInt()}px",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 20.dp),
    )
    Slider(value = value, onValueChange = onValueChange, valueRange = 0f..24f)
}
