package com.ace.hub.ui.games

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ace.hub.data.GameApp
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    apps: List<GameApp>,
    onAppSelected: (GameApp) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight(0.7f)) {
            Text("Select a Game", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(apps) { app ->
                    ListItem(
                        headlineContent = { Text(app.appName, style = MaterialTheme.typography.bodyLarge) },
                        leadingContent = {
                            if (app.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = app.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        },
                        modifier = Modifier.clickable { onAppSelected(app) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}
