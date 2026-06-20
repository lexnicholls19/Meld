package com.lexnicholls.lovecounter

import androidx.compose.runtime.Composable

@Composable
fun RemindersScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val strings = t()
    BaseReminderScreen(
        title = strings.reminders,
        collectionName = "reminders",
        deviceId = deviceId,
        userName = userName,
        showAddDialog = showAddDialog,
        onDismissDialog = onDismissDialog,
        onCompletedViewToggled = onCompletedViewToggled
    )
}
