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
import java.util.concurrent.TimeUnit

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
        LoveWidget().updateAll(context)
    }
}

class TogglePageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val currentPage = prefs[intPreferencesKey("current_page")] ?: 0
            val nextPage = if (currentPage == 0) 1 else 0
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

// Data Class local for widget or use the one from project
data class ImportantDateWithSource(
    val date: ImportantDate,
    val sourceCollection: String
)

class LoveWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        Log.d("LoveWidget", "provideGlance started")
        val sharedPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val configsStr = sharedPrefs.getString("widget_configs", "Timer") ?: "Timer"
        val configs = configsStr.split(",").toSet()
        
        val db = FirebaseFirestore.getInstance()
        var displayItems = emptyList<ImportantDateWithSource>()
        
        try {
            val collections = mutableListOf<String>()
            if (configs.contains("Reminders") || configs.contains("Dynamic")) collections.add("reminders")
            if (configs.contains("Dates") || configs.contains("Dynamic")) collections.add("important_dates")
            if (configs.contains("Dynamic")) collections.add("bucket_list")
            
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
            val currentPage = prefs[intPreferencesKey("current_page")] ?: 0
            
            // Si solo está seleccionado Timer, forzar página 0
            val onlyTimer = configs.size == 1 && configs.contains("Timer")
            val effectivePage = if (onlyTimer) 0 else currentPage
            
            WidgetContent(configsStr, effectivePage, displayItems)
        }
    }

    @Composable
    fun WidgetContent(config: String, currentPage: Int, items: List<ImportantDateWithSource>) {
        val size = LocalSize.current
        
        // Ajuste dinámico de fuentes
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
            // 1. Background Image
            Image(
                provider = ImageProvider(R.drawable.widget_bg),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())
            )

            // 2. Navigation / Page content
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                if (currentPage == 0) {
                    // PÁGINA 0: TIMER (FOCO EN DÍAS)
                    Text(
                        text = "💚 Juntos 💛",
                        style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = titleFontSize, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = "$totalDays días",
                        style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = daysFontSize, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                } else {
                    // PÁGINA 1: INFORMACIÓN ADICIONAL
                    Text(
                        text = "✨ Recordatorios ✨",
                        style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = titleFontSize, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    if (items.isEmpty()) {
                        Text(
                            text = "¡Todo al día! ❤️",
                            style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize),
                            maxLines = 1
                        )
                    } else {
                        val maxItems = if (size.height < 100.dp) 2 else 3
                        items.take(maxItems).forEach { wrapper ->
                            val item = wrapper.date
                            val daysLeft = if (item.type == "date") calculateDaysLeft(item.value) else null
                            val suffix = if (daysLeft != null) " (${daysLeft}d)" else ""
                            Text(
                                text = "• ${item.name}$suffix",
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(Color.White, Color.White), fontSize = itemFontSize)
                            )
                        }
                    }
                }
            }

            // 3. Paging Indicators (Dots) & Switch Action
            if (config != "Timer") {
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(bottom = 4.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = GlanceModifier.clickable(actionRunCallback<TogglePageAction>())
                            .padding(4.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        // Dot 0
                        val dot0Color = if (currentPage == 0) Color.White else Color.White.copy(alpha = 0.4f)
                        Box(
                            modifier = GlanceModifier.size(6.dp)
                                .background(ColorProvider(dot0Color, dot0Color))
                                .cornerRadius(3.dp)
                        ) {}
                        Spacer(GlanceModifier.width(6.dp))
                        // Dot 1
                        val dot1Color = if (currentPage == 1) Color.White else Color.White.copy(alpha = 0.4f)
                        Box(
                            modifier = GlanceModifier.size(6.dp)
                                .background(ColorProvider(dot1Color, dot1Color))
                                .cornerRadius(3.dp)
                        ) {}
                    }
                }
            }

            // 4. Refresh Button (Top End)
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "Refrescar",
                    colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.6f))),
                    modifier = GlanceModifier.padding(4.dp).size(20.dp).clickable(actionRunCallback<RefreshAction>())
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
