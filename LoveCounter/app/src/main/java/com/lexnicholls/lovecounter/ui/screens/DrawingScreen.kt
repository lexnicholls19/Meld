package com.lexnicholls.lovecounter.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.lexnicholls.lovecounter.ui.theme.LovePink
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
    var backgroundColor by remember { mutableStateOf(Color.White) }
    var isEraserMode by remember { mutableStateOf(false) }
    var strokeSize by remember { mutableFloatStateOf(15f) }
    
    var showHistory by remember { mutableStateOf(false) }
    var drawingsHistory by remember { mutableStateOf<List<DrawingData>>(emptyList()) }
    var isSending by remember { mutableStateOf(false) }
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    var selectedDrawingForView by remember { mutableStateOf<DrawingData?>(null) }

    val picture = remember { Picture() }
    val internalCanvasSize = 1024f

    // Load latest drawing as background
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.drawing, fontWeight = FontWeight.Bold, color = LovePink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        loadHistory(db, userId) { 
                            drawingsHistory = it
                            showHistory = true 
                        }
                    }) {
                        Icon(Icons.Default.History, contentDescription = strings.todayDrawings)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Background Color Picker
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(strings.backgroundColor, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(Color.White, Color.Black, Color(0xFFFFEBEE), Color(0xFFE3F2FD), Color(0xFFF1F8E9), Color(0xFFFFFDE7)).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (backgroundColor == color) 2.dp else 1.dp,
                                    color = if (backgroundColor == color) LovePink else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { backgroundColor = color }
                        )
                    }
                }
            }

            // Brush Tools
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(strings.tools, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, LovePink).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor == color && !isEraserMode) 3.dp else 0.dp,
                                        color = if (selectedColor == color && !isEraserMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { 
                                        selectedColor = color 
                                        isEraserMode = false
                                    }
                            )
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isEraserMode = !isEraserMode }) {
                            Icon(
                                if (isEraserMode) Icons.Default.Brush else Icons.Default.AutoFixHigh,
                                contentDescription = strings.eraser,
                                tint = if (isEraserMode) LovePink else Color.Gray
                            )
                        }
                        IconButton(onClick = { if (paths.isNotEmpty()) paths.removeAt(paths.size - 1) }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = strings.undo, tint = Color.Gray)
                        }
                        IconButton(onClick = { 
                            paths.clear() 
                            backgroundBitmap = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.deleteAll, tint = Color.Gray)
                        }
                    }
                }
            }

            // Slider for size adjustment
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                val label = strings.strokeSize
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("${strokeSize.roundToInt()}", fontSize = 12.sp, color = LovePink, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = strokeSize,
                    onValueChange = { strokeSize = it },
                    valueRange = 5f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = LovePink,
                        activeTrackColor = LovePink,
                        inactiveTrackColor = LovePink.copy(alpha = 0.2f)
                    )
                )
            }

            // Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath?.lineTo(change.position.x, change.position.y)
                                // Trigger recomposition
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
                        
                        // 1. Draw background color
                        nativeCanvas.drawColor(backgroundColor.toArgb())
                        
                        // 2. Draw background image SCALED to fill current canvas size
                        backgroundBitmap?.let {
                            val androidBitmap = it.asAndroidBitmap()
                            val src = Rect(0, 0, androidBitmap.width, androidBitmap.height)
                            val dst = Rect(0, 0, size.width.toInt(), size.height.toInt())
                            nativeCanvas.drawBitmap(androidBitmap, src, dst, null)
                        }

                        // 3. Draw user paths
                        val layerPaint = android.graphics.Paint()
                        val layerRect = RectF(0f, 0f, size.width, size.height)
                        // This isolates the CLEAR mode so it only affects user strokes in this session
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (paths.isNotEmpty() || backgroundBitmap != null || backgroundColor != Color.White) {
                        isSending = true
                        
                        // CREATE STANDARD SIZE BITMAP (1024x1024)
                        val standardSize = internalCanvasSize.toInt()
                        val resultBitmap = Bitmap.createBitmap(standardSize, standardSize, Bitmap.Config.ARGB_8888)
                        val resultCanvas = Canvas(resultBitmap)
                        
                        // Scale the picture from current screen size to standard size
                        val scaleX = standardSize.toFloat() / picture.width.toFloat()
                        val scaleY = standardSize.toFloat() / picture.height.toFloat()
                        resultCanvas.scale(scaleX, scaleY)
                        resultCanvas.drawPicture(picture)
                        
                        val outputStream = ByteArrayOutputStream()
                        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

                        saveDrawing(db, userId, userName, base64) {
                            isSending = false
                            paths.clear()
                            backgroundBitmap = null
                            // Notificación
                            sendInterpretedNotification(context, strings.drawing, strings.newDrawingNotification, userName)
                            Toast.makeText(context, strings.drawingSent, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSending && (paths.isNotEmpty() || backgroundBitmap != null || backgroundColor != Color.White)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.sendDrawing)
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
                    Text(text = strings.sentAt.format(formatTimestamp(drawing.timestamp)), fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    saveImageToGallery(context, bitmap, "Dibujo_${drawing.createdBy}_${System.currentTimeMillis()}")
                }) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.download)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    backgroundBitmap = bitmap.asImageBitmap()
                    selectedDrawingForView = null 
                }) {
                    Text(strings.editAdd)
                }
            }
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
                Text(text = "${strings.sentAt.split(" ")[0]}: ${formatTimestamp(drawing.timestamp)}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

fun loadHistory(db: FirebaseFirestore, userId: String, onResult: (List<DrawingData>) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
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

fun saveDrawing(db: FirebaseFirestore, userId: String, userName: String, base64: String, onComplete: () -> Unit) {
    val data = hashMapOf(
        "createdBy" to userName,
        "timestamp" to Timestamp.now(),
        "base64Data" to base64
    )
    db.collection("users").document(userId).collection("drawings").add(data)
        .addOnSuccessListener { onComplete() }
}

fun saveImageToGallery(context: android.content.Context, bitmap: Bitmap, filename: String) {
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

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val date = timestamp.toDate()
    val sdf = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

data class DrawingData(
    val createdBy: String = "",
    val timestamp: Timestamp? = null,
    val base64Data: String = ""
)

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float = 15f,
    val isEraser: Boolean = false
)
