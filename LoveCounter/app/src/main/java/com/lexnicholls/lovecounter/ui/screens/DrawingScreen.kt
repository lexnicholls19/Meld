package com.lexnicholls.lovecounter.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.ui.components.AdvancedColorPicker
import com.lexnicholls.lovecounter.ui.theme.LovePink
import com.lexnicholls.lovecounter.ui.theme.MeldTheme
import com.lexnicholls.lovecounter.util.t
import com.lexnicholls.lovecounter.util.getStringsForLanguage
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(userId: String, userName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val strings = t()

    var currentPath by remember { mutableStateOf<Path?>(null) }
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isEraserMode by remember { mutableStateOf(false) }
    var strokeSize by remember { mutableFloatStateOf(25f) }
    
    var showHistory by remember { mutableStateOf(false) }
    var drawingsHistory by remember { mutableStateOf<List<DrawingData>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    var selectedDrawingForView by remember { mutableStateOf<DrawingData?>(null) }

    val picture = remember { Picture() }
    val internalCanvasSize = 1024f

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("drawings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val latest = snapshot.documents[0].toObject(DrawingData::class.java)
                    latest?.base64Data?.let { base64 ->
                        val decodedString = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        backgroundBitmap = bitmap.asImageBitmap()
                    }
                }
            }
    }

    var showColorMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header: Title Left, History Right (Matches ImportantDates style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.drawing,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        loadDrawingHistory(db, userId) {
                            drawingsHistory = it
                            showHistory = true
                        }
                        showSizeMenu = false
                        showColorMenu = false
                    }
                ) {
                    Icon(Icons.Default.History, null, tint = LovePink)
                }
            }

            // Toolbar
            DrawingToolbar(
                isEraserMode = isEraserMode,
                onEraserModeChange = { isEraserMode = it },
                strokeSize = strokeSize,
                selectedColor = selectedColor,
                showSizeMenu = showSizeMenu,
                onShowSizeMenuChange = { 
                    showSizeMenu = it
                    if (it) showColorMenu = false
                },
                onUndo = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) },
                onClear = { 
                    paths.clear() 
                    backgroundBitmap = null
                },
                onMenuClose = {
                    showSizeMenu = false
                    showColorMenu = false
                }
            )

            // Canvas Area with Floating Overlays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // The Canvas "Sheet"
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (backgroundColor == Color.Transparent) Color.White.copy(alpha = 0.1f) else backgroundColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    showSizeMenu = false
                                    showColorMenu = false
                                })
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        showSizeMenu = false
                                        showColorMenu = false
                                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentPath?.lineTo(change.position.x, change.position.y)
                                        val p = currentPath
                                        currentPath = null
                                        currentPath = p
                                    },
                                    onDragEnd = {
                                        currentPath?.let {
                                            paths.add(DrawingPath(it, selectedColor, strokeWidth = strokeSize, isEraser = isEraserMode))
                                        }
                                        currentPath = null
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawIntoCanvas { canvas ->
                                val nativeCanvas = picture.beginRecording(size.width.toInt(), size.height.toInt())
                                
                                if (backgroundColor != Color.Transparent) {
                                    nativeCanvas.drawColor(backgroundColor.toArgb())
                                }
                                
                                backgroundBitmap?.let {
                                    val androidBitmap = it.asAndroidBitmap()
                                    val src = Rect(0, 0, androidBitmap.width, androidBitmap.height)
                                    val dst = Rect(0, 0, size.width.toInt(), size.height.toInt())
                                    nativeCanvas.drawBitmap(androidBitmap, src, dst, null)
                                }

                                val layerPaint = android.graphics.Paint()
                                val layerRect = RectF(0f, 0f, size.width, size.height)
                                nativeCanvas.saveLayer(layerRect, layerPaint)

                                paths.forEach { drawingPath ->
                                    val paint = android.graphics.Paint().apply {
                                        color = if (drawingPath.isEraser) android.graphics.Color.TRANSPARENT else drawingPath.color.toArgb()
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = drawingPath.strokeWidth
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                        isAntiAlias = true
                                        if (drawingPath.isEraser) {
                                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                        }
                                    }
                                    nativeCanvas.drawPath(drawingPath.path.asAndroidPath(), paint)
                                }

                                currentPath?.let {
                                    val paint = android.graphics.Paint().apply {
                                        color = if (isEraserMode) android.graphics.Color.TRANSPARENT else selectedColor.toArgb()
                                        style = android.graphics.Paint.Style.STROKE
                                        strokeWidth = strokeSize
                                        strokeCap = android.graphics.Paint.Cap.ROUND
                                        strokeJoin = android.graphics.Paint.Join.ROUND
                                        isAntiAlias = true
                                        if (isEraserMode) {
                                            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                        }
                                    }
                                    nativeCanvas.drawPath(it.asAndroidPath(), paint)
                                }
                                
                                nativeCanvas.restore()
                                picture.endRecording()
                                canvas.nativeCanvas.drawPicture(picture)
                            }
                        }
                    }
                }

                // Floating Size Slider
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSizeMenu,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = strokeSize,
                                onValueChange = { strokeSize = it },
                                valueRange = 5f..150f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = LovePink, activeTrackColor = LovePink)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = "${strokeSize.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LovePink)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }

        // FABs
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Color FAB (Bottom Left)
                Column(horizontalAlignment = Alignment.Start) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showColorMenu,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.padding(bottom = 12.dp).width(300.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(28.dp),
                            shadowElevation = 12.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = strings.tools,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LovePink,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                AdvancedColorPicker(
                                    initialColor = selectedColor,
                                    onColorChange = { 
                                        selectedColor = it
                                        isEraserMode = false
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Background Colors
                                Text(
                                    text = strings.backgroundColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatColorFill, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    listOf(Color.Transparent, Color.White, Color.Black, Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFF1F8E9)).forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if(color == Color.Transparent) Color.White.copy(alpha = 0.1f) else color)
                                                .border(1.dp, if(backgroundColor == color) LovePink else Color.LightGray.copy(alpha = 0.3f), CircleShape)
                                                .clickable { backgroundColor = color }
                                        ) {
                                            if (color == Color.Transparent) {
                                                Icon(Icons.Default.Block, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize().padding(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { 
                            val willBeOpen = !showColorMenu
                            showColorMenu = willBeOpen
                            if (willBeOpen) showSizeMenu = false
                        },
                        containerColor = if (showColorMenu) LovePink else MaterialTheme.colorScheme.surface,
                        contentColor = if (showColorMenu) Color.White else Color.Gray,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                    }
                }

                // Send FAB (Bottom Right)
                FloatingActionButton(
                    onClick = {
                        if (paths.isNotEmpty() || backgroundBitmap != null || backgroundColor != Color.Transparent) {
                            isSending = true
                            val standardSize = internalCanvasSize.toInt()
                            val resultBitmap = Bitmap.createBitmap(standardSize, standardSize, Bitmap.Config.ARGB_8888)
                            val resultCanvas = AndroidCanvas(resultBitmap)
                            val scaleX = standardSize.toFloat() / picture.width.toFloat()
                            val scaleY = standardSize.toFloat() / picture.height.toFloat()
                            resultCanvas.scale(scaleX, scaleY)
                            resultCanvas.drawPicture(picture)
                            
                            val outputStream = ByteArrayOutputStream()
                            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            val base64Str = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

                            saveCanvasDrawing(db, userId, userName, base64Str) {
                                isSending = false
                                paths.clear()
                                backgroundBitmap = null
                                sendInterpretedNotification(context, strings.drawing, strings.newDrawingNotification, userName)
                                Toast.makeText(context, strings.drawingSent, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = LovePink,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    if (isSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
            }
        }
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text(strings.todayDrawings) },
            text = {
                if (drawingsHistory.isEmpty()) {
                    Text(strings.noDrawingsToday)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(drawingsHistory) { drawing ->
                            DrawingHistoryItem(drawing) {
                                selectedDrawingForView = drawing
                                showHistory = false
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) { Text(strings.close) }
            }
        )
    }

    if (selectedDrawingForView != null) {
        val drawing = selectedDrawingForView!!
        val bitmap = remember(drawing) {
            val decodedString = Base64.decode(drawing.base64Data, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        }

        AlertDialog(
            onDismissRequest = { selectedDrawingForView = null },
            title = { Text(strings.drawingFrom.format(drawing.createdBy)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = strings.sentAt.format(formatDrawingTimestamp(drawing.timestamp)), fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveDrawingToGallery(context, bitmap, "Dibujo_${drawing.createdBy}_${System.currentTimeMillis()}")
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.download)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        backgroundBitmap = bitmap.asImageBitmap()
                        selectedDrawingForView = null 
                    }
                ) {
                    Text(strings.editAdd)
                }
            },
        )
    }
}

@Composable
fun ColorCircleHalo(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f))
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .shadow(if(color == Color.White) 2.dp else 0.dp, CircleShape)
        )
    }
}

@Composable
fun DrawingHistoryItem(drawing: DrawingData, onClick: () -> Unit) {
    val strings = t()
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Brush, contentDescription = null, tint = LovePink)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = "${strings.addedBy}: ${drawing.createdBy}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = "${strings.sentAt.split(" ")[0]}: ${formatDrawingTimestamp(drawing.timestamp)}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

fun loadDrawingHistory(db: FirebaseFirestore, userId: String, onResult: (List<DrawingData>) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar[Calendar.HOUR_OF_DAY] = 0
    calendar[Calendar.MINUTE] = 0
    calendar[Calendar.SECOND] = 0
    val todayStart = calendar.time

    db.collection("users").document(userId).collection("drawings")
        .whereGreaterThanOrEqualTo("timestamp", Timestamp(todayStart))
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { snapshot ->
            val list = snapshot.documents.mapNotNull { it.toObject(DrawingData::class.java) }
            onResult(list)
        }
}

fun saveCanvasDrawing(db: FirebaseFirestore, userId: String, userName: String, base64: String, onComplete: () -> Unit) {
    val data = hashMapOf(
        "createdBy" to userName,
        "timestamp" to Timestamp.now(),
        "base64Data" to base64,
    )
    db.collection("users").document(userId).collection("drawings").add(data)
        .addOnSuccessListener { onComplete() }
}

fun saveDrawingToGallery(context: android.content.Context, bitmap: Bitmap, filename: String) {
    val sharedPrefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
    val langCode = sharedPrefs.getString("app_language", "system") ?: "system"
    val strings = getStringsForLanguage(langCode)
    
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    imageUri?.let { uri ->
        val outputStream: OutputStream? = resolver.openOutputStream(uri)
        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, strings.drawingSaved, Toast.LENGTH_SHORT).show()
        }
    } ?: run {
        Toast.makeText(context, strings.drawingError, Toast.LENGTH_SHORT).show()
    }
}

fun formatDrawingTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

data class DrawingData(
    val createdBy: String = "",
    val timestamp: Timestamp? = null,
    val base64Data: String = "",
)

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float = 15f,
    val isEraser: Boolean = false,
)

@Composable
fun DrawingToolbar(
    isEraserMode: Boolean,
    onEraserModeChange: (Boolean) -> Unit,
    strokeSize: Float,
    selectedColor: Color,
    showSizeMenu: Boolean,
    onShowSizeMenuChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onMenuClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { 
            onEraserModeChange(false) 
            onMenuClose()
        }) {
            Icon(Icons.Default.Brush, null, tint = if (!isEraserMode) LovePink else Color.Gray)
        }
        IconButton(onClick = { 
            onEraserModeChange(true) 
            onMenuClose()
        }) {
            Icon(Icons.Default.AutoFixHigh, null, tint = if (isEraserMode) LovePink else Color.Gray)
        }
        
        // Size toggle (Circle with center point)
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable { 
                    onShowSizeMenuChange(!showSizeMenu)
                },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = CircleShape,
            border = BorderStroke(1.dp, if(showSizeMenu) LovePink else Color.LightGray.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((strokeSize / 150f * 24f).coerceAtLeast(3f).dp)
                        .clip(CircleShape)
                        .background(if(isEraserMode) Color.Gray else selectedColor)
                )
            }
        }

        IconButton(onClick = { 
            onUndo()
            onMenuClose()
        }) {
            Icon(Icons.AutoMirrored.Filled.Undo, null, tint = Color.Gray)
        }
        IconButton(onClick = { 
            onClear()
            onMenuClose()
        }) {
            Icon(Icons.Default.Delete, null, tint = Color.Gray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrawingToolbarPreview() {
    MeldTheme {
        DrawingToolbar(
            isEraserMode = false,
            onEraserModeChange = {},
            strokeSize = 25f,
            selectedColor = Color.Black,
            showSizeMenu = false,
            onShowSizeMenuChange = {},
            onUndo = {},
            onClear = {},
            onMenuClose = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawingHistoryItemPreview() {
    MeldTheme {
        DrawingHistoryItem(
            drawing = DrawingData(
                createdBy = "Lex",
                timestamp = Timestamp.now(),
                base64Data = ""
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ColorCircleHaloPreview() {
    MeldTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ColorCircleHalo(color = LovePink, isSelected = true, onClick = {})
            Spacer(Modifier.width(8.dp))
            ColorCircleHalo(color = Color.Blue, isSelected = false, onClick = {})
        }
    }
}
