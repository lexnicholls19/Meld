package com.lexnicholls.lovecounter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.viewmodel.LoveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    viewModel: LoveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val strings = t()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.advancedConfig, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Test Notifications (Topic: relation_${viewModel.sharedId.value})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "These buttons will send a push notification to all members of the current relationship via Firebase Cloud Functions.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            NotificationTestButton(
                label = strings.reminders,
                icon = Icons.Default.Notifications,
                onClick = { viewModel.sendTestNotification("reminders", context) }
            )

            NotificationTestButton(
                label = strings.dates,
                icon = Icons.Default.DateRange,
                onClick = { viewModel.sendTestNotification("dates", context) }
            )

            NotificationTestButton(
                label = strings.market,
                icon = Icons.Default.ShoppingCart,
                onClick = { viewModel.sendTestNotification("market", context) }
            )

            NotificationTestButton(
                label = strings.bucket,
                icon = Icons.Default.Star,
                onClick = { viewModel.sendTestNotification("bucket", context) }
            )

            NotificationTestButton(
                label = strings.drawing,
                icon = Icons.Default.Brush,
                onClick = { viewModel.sendTestNotification("drawing", context) }
            )

            NotificationTestButton(
                label = strings.quickActions,
                icon = Icons.Default.Favorite,
                onClick = { viewModel.sendTestNotification("quick_action", context) }
            )
            
            NotificationTestButton(
                label = "System Test",
                icon = Icons.Default.BugReport,
                onClick = { viewModel.sendTestNotification("system", context) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Troubleshooting",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val sharedPrefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
            val currentId = sharedPrefs.getString("device_id", "Unknown") ?: "Unknown"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device ID (Critical for Notifications):", fontSize = 12.sp, color = Color.Gray)
                    Text(currentId, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "If two phones have the same ID, they will IGNORE each other's notifications. This happens if you clone your phone data.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = {
                    val newId = java.util.UUID.randomUUID().toString()
                    sharedPrefs.edit().putString("device_id", newId).apply()
                    android.widget.Toast.makeText(context, "ID Reset! Please restart the app.", android.widget.Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Device ID")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NotificationTestButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(text = "Probar $label")
    }
}
