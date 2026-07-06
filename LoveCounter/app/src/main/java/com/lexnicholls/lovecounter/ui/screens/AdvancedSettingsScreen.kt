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
                text = "Test Notifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LovePink,
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
