package com.lexnicholls.lovecounter.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.ColorFilter
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.MainActivity
import com.lexnicholls.lovecounter.R
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import kotlinx.coroutines.withTimeoutOrNull

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        Log.d("LoveWidget", "RefreshAction triggered")
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[intPreferencesKey("current_page")] = 0
            }
        }
        LoveWidget().update(context, glanceId)
    }
}

val DeltaKey = androidx.glance.action.ActionParameters.Key<Int>("delta")

class TogglePageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        val delta = parameters[DeltaKey] ?: 1
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val configsStr = context.getSharedPreferences("prefs", Context.MODE_PRIVATE).getString("widget_configs", "Timer") ?: "Timer"
            val activeConfigs = configsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            
            val pages = mutableListOf<String>()
            if (activeConfigs.contains("Timer")) pages.add("Timer")
            if (activeConfigs.contains("Reminders")) pages.add("Reminders")
            if (activeConfigs.contains("Dates")) pages.add("Dates")
            
            val maxPages = pages.size
            if (maxPages <= 1) return@updateAppWidgetState prefs
            
            val currentPage = prefs[intPreferencesKey("current_page")] ?: 0
            val nextPage = (currentPage + delta + maxPages) % maxPages
            
            prefs.toMutablePreferences().apply {
                this[intPreferencesKey("current_page")] = nextPage
            }
        }
        LoveWidget().update(context, glanceId)
    }
}

data class ImportantDate(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val value: String = "",
    val isCompleted: Boolean = false
)

data class ImportantDateWithSource(
    val date: ImportantDate,
    val sourceCollection: String
)

val ConfigsKey = androidx.datastore.preferences.core.stringPreferencesKey("widget_configs_state")
val AutoRotateKey = androidx.datastore.preferences.core.booleanPreferencesKey("widget_auto_rotate_state")
val IntervalKey = androidx.datastore.preferences.core.intPreferencesKey("widget_interval_state")

class LoveWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val configsStr = sharedPrefs.getString("widget_configs", "Timer") ?: "Timer"
        val autoRotate = sharedPrefs.getBoolean("widget_auto_rotate", false)
        val intervalSeconds = sharedPrefs.getInt("widget_rotate_interval", 60).coerceAtLeast(1)
        
        val db = FirebaseFirestore.getInstance()
        var displayItems = emptyList<ImportantDateWithSource>()
        
        try {
            val configs = configsStr.split(",").map { it.trim() }.toSet()
            val collections = mutableListOf<String>()
            if (configs.contains("Reminders")) collections.add("reminders")
            if (configs.contains("Dates")) collections.add("important_dates")
            
            val allItems = mutableListOf<ImportantDateWithSource>()
            withTimeoutOrNull(5000) {
                for (coll in collections.distinct()) {
                    try {
                        val snapshot = db.collection(coll).get().await()
                        val items = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ImportantDate::class.java)?.copy(id = doc.id)
                        }.filter { !it.isCompleted }
                        items.forEach { allItems.add(ImportantDateWithSource(it, coll)) }
                    } catch (e: Exception) { }
                }
            }
            
            displayItems = allItems.sortedBy { wrapper ->
                val item = wrapper.date
                if (item.type == "date") {
                    try {
                        val date = LocalDate.parse(item.value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        val today = LocalDate.now()
                        var next = date.withYear(today.year)
                        if (next.isBefore(today)) next = next.plusYears(1)
                        ChronoUnit.DAYS.between(today, next)
                    } catch (e: Exception) { Long.MAX_VALUE }
                } else Long.MAX_VALUE
            }
        } catch (e: Exception) { }

        provideContent {
            val prefs = currentState<Preferences>()
            val manualPage = prefs[intPreferencesKey("current_page")] ?: 0
            
            // Usar directamente las variables leídas de SharedPreferences para garantizar sincronía total con la App
            val activeConfigs = configsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val pages = mutableListOf<String>()
            if (activeConfigs.contains("Timer")) pages.add("Timer")
            if (activeConfigs.contains("Reminders")) pages.add("Reminders")
            if (activeConfigs.contains("Dates")) pages.add("Dates")
            
            val totalPages = pages.size
            val effectivePage = if (totalPages <= 1) {
                0
            } else {
                val autoOffset = if (autoRotate) {
                    val totalSeconds = System.currentTimeMillis() / 1000
                    (totalSeconds / intervalSeconds).toInt()
                } else 0
                (manualPage + autoOffset) % totalPages
            }
            
            val currentPageType = if (pages.isNotEmpty()) pages[effectivePage] else "Timer"
            WidgetContent(effectivePage, totalPages, currentPageType, displayItems)
        }
    }

    @Composable
    fun WidgetContent(
        currentPageIndex: Int, 
        totalActivePages: Int,
        currentPageType: String,
        items: List<ImportantDateWithSource>
    ) {
        val size = LocalSize.current
        val titleFontSize = if (size.height < 80.dp) 11.sp else 14.sp
        val daysFontSize = if (size.height < 80.dp) 24.sp else 36.sp
        val itemFontSize = if (size.height < 80.dp) 10.sp else 12.sp
        
        val startDate = LocalDateTime.of(2021, 1, 4, 0, 0)
        val now = LocalDateTime.now()
        val duration = Duration.between(startDate, now)
        val totalDays = duration.toDays()

        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(ColorProvider(Color(0x33000000), Color(0x33000000)))
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_bg),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize()
            )

            Column(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 24.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                when (currentPageType) {
                    "Timer" -> {
                        Text(
                            text = "💚 Juntos 💛",
                            style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = titleFontSize, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "$totalDays días",
                            style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = daysFontSize, fontWeight = FontWeight.Bold)
                        )
                    }
                    "Reminders" -> {
                        val reminderItems = items.filter { it.sourceCollection == "reminders" }
                        Text(
                            text = "✨ Recordatorios ✨",
                            style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = titleFontSize, fontWeight = FontWeight.Bold)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        if (reminderItems.isEmpty()) {
                            Text(text = "¡Todo al día! ❤️", style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize))
                        } else {
                            reminderItems.take(3).forEach { wrapper ->
                                Text(
                                    text = "• ${wrapper.date.name}",
                                    maxLines = 1,
                                    style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize)
                                )
                            }
                        }
                    }
                    "Dates" -> {
                        val dateItems = items.filter { it.sourceCollection == "important_dates" }
                        Text(
                            text = "🗓️ Fechas Especiales 🗓️",
                            style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = titleFontSize, fontWeight = FontWeight.Bold)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        if (dateItems.isEmpty()) {
                            Text(text = "Sin fechas próximas", style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize))
                        } else {
                            dateItems.take(3).forEach { wrapper ->
                                val daysLeft = calculateDaysLeft(wrapper.date.value)
                                val suffix = if (daysLeft != null) " (${daysLeft}d)" else ""
                                Text(
                                    text = "• ${wrapper.date.name}$suffix",
                                    maxLines = 1,
                                    style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = GlanceModifier.fillMaxSize()) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .clickable(actionRunCallback<TogglePageAction>(androidx.glance.action.actionParametersOf(DeltaKey to -1))),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (totalActivePages > 1) {
                        Image(
                            provider = ImageProvider(android.R.drawable.arrow_up_float),
                            contentDescription = "Anterior",
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f))),
                            modifier = GlanceModifier.size(24.dp).padding(top = 4.dp)
                        )
                    }
                }
                
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .clickable(actionStartActivity<MainActivity>())
                ) {}
                
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                        .clickable(actionRunCallback<TogglePageAction>(androidx.glance.action.actionParametersOf(DeltaKey to 1))),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (totalActivePages > 1) {
                        Image(
                            provider = ImageProvider(android.R.drawable.arrow_down_float),
                            contentDescription = "Siguiente",
                            colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f))),
                            modifier = GlanceModifier.size(24.dp).padding(bottom = 4.dp)
                        )
                    }
                }
            }

            if (totalActivePages > 1) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(end = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        repeat(totalActivePages) { index ->
                            val dotColor = if (index == currentPageIndex) Color.White else Color.White.copy(alpha = 0.4f)
                            Box(
                                modifier = GlanceModifier.size(6.dp)
                                    .background(ColorProvider(dotColor, dotColor))
                                    .cornerRadius(3.dp)
                            ) {}
                            if (index < totalActivePages - 1) Spacer(GlanceModifier.height(6.dp))
                        }
                    }
                }
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "Refrescar",
                    colorFilter = ColorFilter.tint(ColorProvider(Color.White, Color.White)),
                    modifier = GlanceModifier
                        .padding(4.dp)
                        .size(48.dp)
                        .clickable(actionRunCallback<RefreshAction>())
                )
            }
        }
    }

    private fun calculateDaysLeft(value: String): Long? {
        return try {
            val date = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val today = LocalDate.now()
            var next = date.withYear(today.year)
            if (next.isBefore(today)) next = next.plusYears(1)
            ChronoUnit.DAYS.between(today, next)
        } catch (e: Exception) {
            null
        }
    }
}
