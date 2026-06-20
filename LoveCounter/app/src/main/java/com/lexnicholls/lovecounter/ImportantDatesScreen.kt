package com.lexnicholls.lovecounter

import androidx.compose.runtime.Composable

@Composable
fun ImportantDatesScreen(
    deviceId: String,
    userName: String,
    showAddDialog: Boolean,
    onDismissDialog: () -> Unit,
    onCompletedViewToggled: (Boolean) -> Unit
) {
    val strings = t()
    BaseReminderScreen(
        title = strings.dates,
        collectionName = "important_dates",
        deviceId = deviceId,
        userName = userName,
        showAddDialog = showAddDialog,
        onDismissDialog = onDismissDialog,
        onCompletedViewToggled = onCompletedViewToggled,
        enableCompletedTab = false,
        enableEdit = true,
        enableCompleteSwipe = false,
        showCountdown = true,
        useSimplifiedDialog = true
    )
}
