package com.aadil.liquidglass.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aadil.liquidglass.model.Wallpaper
import com.aadil.liquidglass.viewmodel.EditorViewModel
import com.kyant.liquidglass.GlassStyle
import com.kyant.liquidglass.liquidGlass
import com.kyant.liquidglass.liquidGlassProvider
import com.kyant.liquidglass.material.GlassMaterial
import com.kyant.liquidglass.refraction.InnerRefraction
import com.kyant.liquidglass.refraction.RefractionAmount
import com.kyant.liquidglass.refraction.RefractionHeight
import com.kyant.liquidglass.rememberLiquidGlassProviderState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.app.Activity
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.withFrameNanos
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: EditorViewModel = viewModel()) {
    val wallpapers by viewModel.wallpapers.collectAsState()
    val selectedWallpaper by viewModel.selectedWallpaper.collectAsState()
    val context = LocalContext.current

    if (selectedWallpaper == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Liquid Glass Wallpapers") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { paddingValues ->
            WallpaperGrid(
                wallpapers = wallpapers,
                onWallpaperClick = { wallpaper ->
                    viewModel.selectWallpaper(wallpaper)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    } else {
        FullScreenEditor(
            wallpaper = selectedWallpaper!!,
            viewModel = viewModel,
            onApply = { bitmap ->
                viewModel.setWallpaper(context, bitmap)
                viewModel.clearSelection()
            },
            onBack = {
                viewModel.clearSelection()
            }
        )
    }
}

@Composable
fun WallpaperGrid(
    wallpapers: List<Wallpaper>,
    onWallpaperClick: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(wallpapers) { wallpaper ->
            Image(
                painter = painterResource(id = wallpaper.resourceId),
                contentDescription = "Wallpaper ${wallpaper.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(),
                        onClick = { onWallpaperClick(wallpaper) }
                    )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenEditor(
    wallpaper: Wallpaper,
    viewModel: EditorViewModel,
    onApply: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val blurRadius by viewModel.blurRadius.collectAsState()
    val glassAlpha by viewModel.glassAlpha.collectAsState()
    val refractionHeight by viewModel.refractionHeight.collectAsState()
    val refractionAmount by viewModel.refractionAmount.collectAsState()
    val cornerRadius by viewModel.cornerRadius.collectAsState()

    var showControls by remember { mutableStateOf(true) }

    val glassStyle = remember(blurRadius, glassAlpha, refractionHeight, refractionAmount, cornerRadius) {
        GlassStyle(
            shape = RoundedCornerShape(cornerRadius.dp),
            innerRefraction = InnerRefraction(
                height = RefractionHeight(refractionHeight.dp),
                amount = RefractionAmount(refractionAmount.dp)
            ),
            material = GlassMaterial(
                blurRadius = blurRadius.dp,
                brush = SolidColor(Color.White),
                alpha = glassAlpha
            )
        )
    }

    var isCapturing by remember { mutableStateOf(false) }
    var canvasRect by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    canvasRect = Rect(
                        b.left.roundToInt(),
                        b.top.roundToInt(),
                        b.right.roundToInt(),
                        b.bottom.roundToInt()
                    )
                }
        ) {
            WallpaperCanvas(
                painter = painterResource(id = wallpaper.resourceId),
                glassStyle = glassStyle
            )
        }

        if (!isCapturing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text("Edit & Crop Effect", color = Color.White, fontSize = 20.sp)
                IconButton(onClick = {
                    scope.launch {
                        try {
                            isCapturing = true
                            withFrameNanos { }
                            withFrameNanos { }
                            val activity = context as? Activity
                            if (activity == null) {
                                Toast.makeText(
                                    context,
                                    "Unable to capture: No Activity.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                isCapturing = false
                                return@launch
                            }
                            val decor = activity.window.decorView
                            val windowRect = Rect(0, 0, decor.width, decor.height)
                            val src =
                                canvasRect?.let { intersectRect(it, windowRect) } ?: windowRect
                            val captured = pixelCopyWindow(activity.window, src)
                            onApply(captured)
                        } catch (t: Throwable) {
                            Toast.makeText(context, "Error capturing image.", Toast.LENGTH_SHORT)
                                .show()
                            t.printStackTrace()
                        } finally {
                            isCapturing = false
                        }
                    }
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Apply Wallpaper", tint = Color.White)
                }
            }
        }

        if (!isCapturing && showControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.25f),
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ControlsSheetContent(
                        onClose = { showControls = false },
                        blurRadius = blurRadius,
                        onBlurChange = { viewModel.blurRadius.value = it },
                        glassAlpha = glassAlpha,
                        onAlphaChange = { viewModel.glassAlpha.value = it },
                        refractionHeight = refractionHeight,
                        onRefractionHeightChange = { viewModel.refractionHeight.value = it },
                        refractionAmount = refractionAmount,
                        onRefractionAmountChange = { viewModel.refractionAmount.value = it },
                        cornerRadius = cornerRadius,
                        onCornerRadiusChange = { viewModel.cornerRadius.value = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperCanvas(
    painter: androidx.compose.ui.graphics.painter.Painter,
    glassStyle: GlassStyle
) {
    val density = LocalDensity.current
    val providerState = rememberLiquidGlassProviderState(backgroundColor = Color.Black)

    var selectionSize by remember { mutableStateOf(Size.Zero) }
    var selectionOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val constraintsWidth = with(density) { maxWidth.toPx() }
        val constraintsHeight = with(density) { maxHeight.toPx() }

        LaunchedEffect(Unit) {
            if (selectionSize == Size.Zero) {
                selectionSize = Size(constraintsWidth * 0.5f, constraintsHeight * 0.4f)
                selectionOffset = Offset(constraintsWidth * 0.25f, constraintsHeight * 0.3f)
            }
        }

        Image(
            painter = painter,
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassProvider(providerState)
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        selectionOffset.x.roundToInt(),
                        selectionOffset.y.roundToInt()
                    )
                }
                .size(
                    width = with(density) { selectionSize.width.toDp() },
                    height = with(density) { selectionSize.height.toDp() }
                )
                .clip(glassStyle.shape)
                .liquidGlass(
                    state = providerState,
                    style = glassStyle
                )
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.7f),
                    glassStyle.shape
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newOffsetX =
                            (selectionOffset.x + dragAmount.x)
                                .coerceIn(0f, constraintsWidth - selectionSize.width)
                        val newOffsetY =
                            (selectionOffset.y + dragAmount.y)
                                .coerceIn(0f, constraintsHeight - selectionSize.height)
                        selectionOffset = Offset(newOffsetX, newOffsetY)
                    }
                }
        ) {
            ResizeHandle(alignment = Alignment.TopStart) { dragAmount ->
                val newOffsetX = (selectionOffset.x + dragAmount.x).coerceIn(0f, selectionOffset.x + selectionSize.width - 100f)
                val newOffsetY = (selectionOffset.y + dragAmount.y).coerceIn(0f, selectionOffset.y + selectionSize.height - 100f)
                val newWidth = selectionSize.width - (newOffsetX - selectionOffset.x)
                val newHeight = selectionSize.height - (newOffsetY - selectionOffset.y)
                selectionOffset = Offset(newOffsetX, newOffsetY)
                selectionSize = Size(newWidth, newHeight)
            }
            ResizeHandle(alignment = Alignment.TopEnd) { dragAmount ->
                val newOffsetY = (selectionOffset.y + dragAmount.y).coerceIn(0f, selectionOffset.y + selectionSize.height - 100f)
                val newWidth = (selectionSize.width + dragAmount.x).coerceIn(100f, constraintsWidth - selectionOffset.x)
                val newHeight = selectionSize.height - (newOffsetY - selectionOffset.y)
                selectionOffset = Offset(selectionOffset.x, newOffsetY)
                selectionSize = Size(newWidth, newHeight)
            }
            ResizeHandle(alignment = Alignment.BottomStart) { dragAmount ->
                val newOffsetX = (selectionOffset.x + dragAmount.x).coerceIn(0f, selectionOffset.x + selectionSize.width - 100f)
                val newWidth = selectionSize.width - (newOffsetX - selectionOffset.x)
                val newHeight = (selectionSize.height + dragAmount.y).coerceIn(100f, constraintsHeight - selectionOffset.y)
                selectionOffset = Offset(newOffsetX, selectionOffset.y)
                selectionSize = Size(newWidth, newHeight)
            }
            ResizeHandle(alignment = Alignment.BottomEnd) { dragAmount ->
                val newWidth = (selectionSize.width + dragAmount.x).coerceIn(100f, constraintsWidth - selectionOffset.x)
                val newHeight = (selectionSize.height + dragAmount.y).coerceIn(100f, constraintsHeight - selectionOffset.y)
                selectionSize = Size(newWidth, newHeight)
            }
        }
    }
}


@Composable
fun BoxScope.ResizeHandle(
    alignment: Alignment,
    onDrag: (Offset) -> Unit
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .size(32.dp)
            .offset(
                x = if (alignment.toString().contains("End")) 16.dp else (-16).dp,
                y = if (alignment.toString().contains("Bottom")) 16.dp else (-16).dp
            )
            .background(Color.Transparent, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    )
}


@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Medium, color = Color.White)
            Text(String.format("%.1f", value), color = Color.White)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}

@Composable
private fun ControlsSheetContent(
    onClose: () -> Unit,
    blurRadius: Float,
    onBlurChange: (Float) -> Unit,
    glassAlpha: Float,
    onAlphaChange: (Float) -> Unit,
    refractionHeight: Float,
    onRefractionHeightChange: (Float) -> Unit,
    refractionAmount: Float,
    onRefractionAmountChange: (Float) -> Unit,
    cornerRadius: Float,
    onCornerRadiusChange: (Float) -> Unit
) {
    val accent = Color(0xFF3D5A40)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 312.dp)
    ) {
        Text(
            text = "Colors",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 12.dp, bottom = 8.dp)
        )
        IconButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.6f)),
            onClick = onClose
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
        }
    }

    Spacer(Modifier.height(8.dp))

    Column(
        Modifier
            .navigationBarsPadding()
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GlassSliderRow(
            icon = Icons.Default.Tune,
            label = "Blur radius",
            valueLabel = String.format("%.0f dp", blurRadius),
            value = blurRadius,
            range = 0f..50f,
            accent = accent,
            onValueChange = onBlurChange
        )
        GlassSliderRow(
            icon = Icons.Default.Tune,
            label = "Opacity",
            valueLabel = String.format("%.2f", glassAlpha),
            value = glassAlpha,
            range = 0f..1f,
            accent = accent,
            onValueChange = onAlphaChange
        )
        GlassSliderRow(
            icon = Icons.Default.Tune,
            label = "Refraction height",
            valueLabel = String.format("%.1f", refractionHeight),
            value = refractionHeight,
            range = 0f..50f,
            accent = accent,
            onValueChange = onRefractionHeightChange
        )
        GlassSliderRow(
            icon = Icons.Default.Tune,
            label = "Refraction amount",
            valueLabel = String.format("%.1f", refractionAmount),
            value = refractionAmount,
            range = -50f..0f,
            accent = accent,
            onValueChange = onRefractionAmountChange
        )
        GlassSliderRow(
            icon = Icons.Default.Tune,
            label = "Corner radius",
            valueLabel = String.format("%.0f dp", cornerRadius),
            value = cornerRadius,
            range = 0f..50f,
            accent = accent,
            onValueChange = onCornerRadiusChange
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GlassSliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Float) -> Unit
) {
    val progress = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(accent.copy(alpha = 0.6f))
        )
        Column(Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Medium)
                    Text(valueLabel, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

private suspend fun pixelCopyWindow(window: Window, srcRect: Rect): Bitmap =
    suspendCancellableCoroutine { cont ->
        val bitmap = Bitmap.createBitmap(srcRect.width(), srcRect.height(), Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            window,
            srcRect,
            bitmap,
            { result ->
                if (result == PixelCopy.SUCCESS) {
                    cont.resume(bitmap)
                } else {
                    cont.resumeWithException(IllegalStateException("PixelCopy failed: $result"))
                }
            },
            Handler(Looper.getMainLooper())
        )
    }

private fun intersectRect(a: Rect, b: Rect): Rect {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)
    return Rect(left, top, right, bottom)
}