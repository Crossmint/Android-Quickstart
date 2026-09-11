package com.crossmint.kotlin.checkoutdemo.ui.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossmint.kotlin.checkoutdemo.model.DemoEvent
import com.crossmint.kotlin.checkoutdemo.utility.formatTimestamp

@Composable
fun EventsSectionView(
    events: List<DemoEvent>,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("No events yet. Create or use an order to get started.")
        }
        return
    }

    LazyColumn(modifier = modifier) {
        items(events.asReversed()) { event ->
            ListItem(
                headlineContent = { Text(event.message) },
                overlineContent = { Text(formatTimestamp(event.timestamp)) },
            )
            HorizontalDivider()
        }
    }
}
