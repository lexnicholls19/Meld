package com.lexnicholls.lovecounter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BucketListScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val strings = t()
    BaseReminderScreen(
        title = strings.bucket,
        collectionName = "bucket_list",
        deviceId = deviceId,
        userName = userName,
        showAddDialog = showAddDialog,
        onDismissDialog = onDismissDialog,
        onCompletedViewToggled = onCompletedViewToggled,
        enableCompletedTab = false, // Desactivar la vista separada
        enableDescription = true, // Habilitar campo descripción
        headerContent = { items ->
            val childItems = items.filter { it.parentId.isNotEmpty() }
            val total = childItems.size
            val completed = childItems.count { it.isCompleted }
            val progress = if (total > 0) completed.toFloat() / total else 0f
            
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.adventureProgress,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$completed/$total",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
        }
    )
}
